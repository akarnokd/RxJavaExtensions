/*
 * Copyright 2016-2019 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.rxjava3.basetypes;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.queue.*;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.*;

/**
 * Concatenate sources emitted by a Publisher one after another and complete after each complete.
 */
final class NonoConcat extends Nono {

    final Publisher<? extends Nono> sources;

    final int prefetch;

    final ErrorMode errorMode;

    NonoConcat(Publisher<? extends Nono> sources, int prefetch, ErrorMode errorMode) {
        this.sources = sources;
        this.prefetch = prefetch;
        this.errorMode = errorMode;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        if (errorMode == ErrorMode.IMMEDIATE) {
            sources.subscribe(new ConcatImmediateSubscriber(s, prefetch));
        } else {
            sources.subscribe(new ConcatDelayedSubscriber(s, prefetch, errorMode == ErrorMode.END));
        }
    }

    abstract static class AbstractConcatSubscriber extends BasicIntQueueSubscription<Void>
    implements Subscriber<Nono> {

        private static final long serialVersionUID = -2273338080908719181L;

        final Subscriber<? super Void> downstream;

        final int prefetch;

        final int limit;

        final AtomicThrowable errors;

        final InnerSubscriber inner;

        Subscription upstream;

        SimpleQueue<Nono> queue;

        int sourceMode;

        int consumed;

        volatile boolean active;

        volatile boolean done;

        volatile boolean cancelled;

        AbstractConcatSubscriber(Subscriber<? super Void> downstream, int prefetch) {
            this.downstream = downstream;
            this.prefetch = prefetch;
            this.limit = prefetch - (prefetch >> 2);
            this.errors = new AtomicThrowable();
            this.inner = new InnerSubscriber();
        }

        @Override
        public final void request(long n) {
            // no-op
        }

        @Override
        public final int requestFusion(int mode) {
            return mode & ASYNC;
        }

        @Override
        public final void clear() {
            // no-op
        }

        @Override
        public final Void poll() throws Exception {
            return null;
        }

        @Override
        public final boolean isEmpty() {
            return true;
        }

        @Override
        public final void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<Nono> qs = (QueueSubscription<Nono>) s;

                    int m = qs.requestFusion(ANY);
                    if (m == SYNC) {
                        sourceMode = m;
                        queue = qs;
                        done = true;

                        downstream.onSubscribe(this);

                        drain();
                        return;
                    }
                    if (m == ASYNC) {
                        sourceMode = m;
                        queue = qs;

                        downstream.onSubscribe(this);

                        s.request(prefetch == Integer.MAX_VALUE ? Long.MAX_VALUE : prefetch);

                        return;
                    }
                }

                if (prefetch == Integer.MAX_VALUE) {
                    queue = new SpscLinkedArrayQueue<Nono>(bufferSize());

                    downstream.onSubscribe(this);

                    s.request(Long.MAX_VALUE);
                } else {
                    queue = new SpscArrayQueue<Nono>(prefetch);

                    downstream.onSubscribe(this);

                    s.request(prefetch);
                }
            }
        }

        final void requestOne() {
            if (sourceMode != SYNC && prefetch != Integer.MAX_VALUE) {
                int c = consumed + 1;
                if (c == limit) {
                    consumed = 0;
                    upstream.request(c);
                } else {
                    consumed = c;
                }
            }
        }

        @Override
        public final void onNext(Nono t) {
            if (sourceMode == NONE) {
                if (!queue.offer(t)) {
                    upstream.cancel();
                    onError(new MissingBackpressureException());
                    return;
                }
            }
            drain();
        }

        final void innerComplete() {
            active = false;
            drain();
        }

        abstract void drain();

        abstract void innerError(Throwable t);

        final class InnerSubscriber extends AtomicReference<Subscription> implements Subscriber<Void> {

            private static final long serialVersionUID = -1235060320533681511L;

            @Override
            public void onSubscribe(Subscription s) {
                SubscriptionHelper.replace(this, s);
            }

            @Override
            public void onNext(Void t) {
                // not called
            }

            @Override
            public void onError(Throwable t) {
                innerError(t);
            }

            @Override
            public void onComplete() {
                active = false;
                drain();
            }

            void dispose() {
                SubscriptionHelper.cancel(this);
            }
        }
    }

    static final class ConcatImmediateSubscriber extends AbstractConcatSubscriber {

        private static final long serialVersionUID = 6000895759062406410L;

        final AtomicInteger wip;

        ConcatImmediateSubscriber(Subscriber<? super Void> downstream, int prefetch) {
            super(downstream, prefetch);
            this.wip = new AtomicInteger();
        }

        @Override
        public void onError(Throwable t) {
            cancelIf(true);
            HalfSerializer.onError(downstream, t, this, errors);
        }

        @Override
        public void innerError(Throwable t) {
            cancelIf(true);
            HalfSerializer.onError(downstream, t, this, errors);
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @Override
        public void cancel() {
            cancelIf(false);
        }
        
        void cancelIf(boolean error) {
            cancelled = true;
            upstream.cancel();
            inner.dispose();
            if (!error) {
                errors.tryTerminateAndReport();
            }

            if (wip.getAndIncrement() == 0) {
                queue.clear();
            }

        }

        @Override
        public void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            do {
                if (cancelled) {
                    queue.clear();
                    return;
                }

                if (!active) {
                    boolean d = done;
                    Nono np;

                    try {
                        np = queue.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        upstream.cancel();
                        queue.clear();
                        HalfSerializer.onError(downstream, ex, this, errors);
                        return;
                    }

                    boolean empty = np == null;

                    if (d && empty) {
                        HalfSerializer.onComplete(downstream, this, errors);
                        return;
                    }

                    if (!empty) {
                        requestOne();

                        active = true;
                        np.subscribe(inner);
                    }
                }
            } while (wip.decrementAndGet() != 0);
        }
    }

    static final class ConcatDelayedSubscriber extends AbstractConcatSubscriber {

        private static final long serialVersionUID = -3402839602492103389L;

        final boolean tillTheEnd;

        ConcatDelayedSubscriber(Subscriber<? super Void> downstream, int prefetch, boolean tillTheEnd) {
            super(downstream, prefetch);
            this.tillTheEnd = tillTheEnd;
        }

        @Override
        public void onError(Throwable t) {
            if (errors.tryAddThrowableOrReport(t)) {
                done = true;
                drain();
            }
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @Override
        public void cancel() {
            cancelled = true;
            upstream.cancel();
            inner.dispose();
            errors.tryTerminateAndReport();

            if (getAndIncrement() == 0) {
                queue.clear();
            }
        }

        @Override
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            do {
                if (cancelled) {
                    queue.clear();
                    return;
                }

                if (!active) {
                    if (!tillTheEnd && errors.get() != null) {
                        queue.clear();
                        errors.tryTerminateConsumer(downstream);
                        return;
                    }

                    boolean d = done;

                    Nono np;

                    try {
                        np = queue.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        upstream.cancel();
                        queue.clear();
                        errors.tryAddThrowableOrReport(ex);

                        errors.tryTerminateConsumer(downstream);
                        return;
                    }

                    boolean empty = np == null;

                    if (d && empty) {
                        errors.tryTerminateConsumer(downstream);
                        return;
                    }

                    if (!empty) {
                        requestOne();

                        active = true;
                        np.subscribe(inner);
                    }
                }
            } while (decrementAndGet() != 0);
        }

        @Override
        void innerError(Throwable t) {
            if (errors.tryAddThrowableOrReport(t)) {
                if (!tillTheEnd) {
                    upstream.cancel();
                }
                active = false;
                drain();
            }
        }
    }
}
