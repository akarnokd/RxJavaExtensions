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

package hu.akarnokd.rxjava3.operators;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * Coalesces items into a container if the downstream is not ready to receive items.
 *
 * @param <T> the upstream element type
 * @param <R> the container type emitted to downstream
 *
 * @since 0.17.3
 */
final class FlowableCoalesce<T, R> extends Flowable<R> implements FlowableTransformer<T, R> {

    final Publisher<T> source;

    final Supplier<R> containerSupplier;

    final BiConsumer<R, T> coalescer;

    final int bufferSize;

    FlowableCoalesce(Publisher<T> source, Supplier<R> containerSupplier, BiConsumer<R, T> coalescer, int bufferSize) {
        this.source = source;
        this.containerSupplier = containerSupplier;
        this.coalescer = coalescer;
        this.bufferSize = bufferSize;
    }

    @Override
    public Publisher<R> apply(Flowable<T> upstream) {
        return new FlowableCoalesce<T, R>(upstream, containerSupplier, coalescer, bufferSize);
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new CoalesceSubscriber<T, R>(s, containerSupplier, coalescer, bufferSize));
    }

    static final class CoalesceSubscriber<T, R> extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -6157179110480235565L;

        final Subscriber<? super R> downstream;

        final Supplier<R> containerSupplier;

        final BiConsumer<R, T> coalescer;

        final AtomicLong requested;

        final int bufferSize;

        volatile SimplePlainQueue<T> queue;

        Subscription upstream;

        R container;

        volatile boolean done;

        volatile boolean cancelled;

        Throwable error;

        long emitted;

        CoalesceSubscriber(Subscriber<? super R> downstream, Supplier<R> containerSupplier,
                BiConsumer<R, T> coalescer, int bufferSize) {
            this.downstream = downstream;
            this.containerSupplier = containerSupplier;
            this.coalescer = coalescer;
            this.requested = new AtomicLong();
            this.bufferSize = bufferSize;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(upstream, s)) {
                upstream = s;
                downstream.onSubscribe(this);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (get() == 0 && compareAndSet(0, 1)) {
                SimplePlainQueue<T> q = queue;
                if (q == null || q.isEmpty()) {
                    R c = container;
                    try {
                        if (c == null) {
                            c = containerSupplier.get();
                            container = c;
                        }
                        coalescer.accept(c, t);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        upstream.cancel();
                        container = null;
                        downstream.onError(ex);
                        return;
                    }
                    long r = requested.get();
                    long e = emitted;
                    if (e != r) {
                        container = null;
                        downstream.onNext(c);
                        emitted = e + 1;
                    }
                    if (decrementAndGet() == 0) {
                        return;
                    }
                }
            } else {
                SimplePlainQueue<T> q = queue;
                if (q == null) {
                    q = new SpscLinkedArrayQueue<T>(bufferSize);
                    queue = q;
                }
                q.offer(t);
                if (getAndIncrement() != 0) {
                    return;
                }
            }
            drainLoop();
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            upstream.cancel();
            if (getAndIncrement() == 0) {
                container = null;
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            drainLoop();
        }

        void drainLoop() {
            int missed = 1;
            long e = emitted;
            R c = container;
            Subscriber<? super R> a = downstream;

            for (;;) {
                if (cancelled) {
                    container = null;
                    return;
                }
                boolean d = done;
                SimplePlainQueue<T> q = queue;
                boolean empty = q == null || q.isEmpty();

                if (!empty) {
                    try {
                        if (c == null) {
                            c = containerSupplier.get();
                            container = c;
                        }

                        for (;;) {
                            T v = q.poll();
                            if (v == null) {
                                break;
                            }
                            coalescer.accept(c, v);
                        }
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        container = null;
                        a.onError(ex);
                        return;
                    }
                }

                if (c != null && e != requested.get()) {
                    a.onNext(c);
                    c = null;
                    container = null;
                    e++;
                }

                if (d && c == null) {
                    Throwable ex = error;
                    container = null;
                    if (ex != null) {
                        a.onError(ex);
                    } else {
                        a.onComplete();
                    }
                    return;
                }

                emitted = e;
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }
}
