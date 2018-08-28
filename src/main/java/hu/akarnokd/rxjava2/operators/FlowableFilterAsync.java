/*
 * Copyright 2016-2018 David Karnok
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

package hu.akarnokd.rxjava2.operators;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import hu.akarnokd.rxjava2.operators.FlowableMapAsync.AsyncSupport;
import hu.akarnokd.rxjava2.operators.FlowableMapAsync.MapAsyncSubscriber.InnerSubscriber;
import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;

/**
 * Maps each upstream value into a single {@code true} or {@code false} value provided by a generated Publisher for that
 * input value and emits the input value if the inner Publisher returned {@code true}.
 * <p>Only the first item emitted by the inner Publisher's are considered. If
 * the inner Publisher is empty, no resulting item is generated for that input value.
 * @param <T> the input value type
 *
 * @since 0.16.2
 */
final class FlowableFilterAsync<T> extends Flowable<T> implements FlowableTransformer<T, T> {

    final Flowable<T> source;

    final Function<? super T, ? extends Publisher<Boolean>> asyncPredicate;

    final int bufferSize;

    FlowableFilterAsync(Flowable<T> source, Function<? super T, ? extends Publisher<Boolean>> asyncPredicate,
            int bufferSize) {
        this.source = source;
        this.asyncPredicate = asyncPredicate;
        this.bufferSize = bufferSize;
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableFilterAsync<T>(upstream, asyncPredicate, bufferSize);
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new FilterAsyncSubscriber<T>(s, asyncPredicate, bufferSize));
    }

    static final class FilterAsyncSubscriber<T>
    extends AtomicReferenceArray<T>
    implements FlowableSubscriber<T>, Subscription, AsyncSupport<Boolean> {

        private static final long serialVersionUID = -1557840206706079339L;

        final Subscriber<? super T> downstream;

        final Function<? super T, ? extends Publisher<Boolean>> asyncPredicate;

        final int bufferSize;

        final AtomicThrowable error;

        final AtomicLong requested;

        final AtomicInteger wip;

        final AtomicReference<InnerSubscriber<Boolean>> current;

        @SuppressWarnings({ "rawtypes", "unchecked" })
        static final InnerSubscriber INNER_CANCELLED = new InnerSubscriber(null);

        Subscription upstream;

        long producerIndex;

        long consumerIndex;

        int consumed;

        volatile boolean done;

        volatile boolean cancelled;

        Boolean innerResult;

        long emitted;

        volatile int state;
        static final int STATE_FRESH = 0;
        static final int STATE_RUNNING = 1;
        static final int STATE_RESULT = 2;

        FilterAsyncSubscriber(Subscriber<? super T> downstream,
                Function<? super T, ? extends Publisher<Boolean>> asyncPredicate,
                int bufferSize) {
            super(Pow2.roundToPowerOfTwo(bufferSize));
            this.downstream = downstream;
            this.asyncPredicate = asyncPredicate;
            this.bufferSize = bufferSize;
            this.error = new AtomicThrowable();
            this.requested = new AtomicLong();
            this.wip = new AtomicInteger();
            this.current = new AtomicReference<InnerSubscriber<Boolean>>();
        }

        @Override
        public void onNext(T t) {
            long pi = producerIndex;
            int m = length() - 1;

            int offset = (int)pi & m;
            lazySet(offset, t);
            producerIndex = pi + 1;
            drain();
        }

        @Override
        public void onError(Throwable t) {
            error.addThrowable(t);
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
            if (!cancelled) {
                cancelled = true;
                upstream.cancel();
                cancelInner();
                if (wip.getAndIncrement() == 0) {
                    clear();
                }
            }
        }

        @SuppressWarnings("unchecked")
        void cancelInner() {
            InnerSubscriber<Boolean> a = current.get();
            if (a != INNER_CANCELLED) {
                a = current.getAndSet(INNER_CANCELLED);
                if (a != null && a != INNER_CANCELLED) {
                    a.cancel();
                }
            }
        }

        void clear() {
            int n = length();
            for (int i = 0; i < n; i++) {
                lazySet(i, null);
            }
            innerResult = null;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(upstream, s)) {
                upstream = s;

                downstream.onSubscribe(this);

                s.request(bufferSize);
            }
        }

        @SuppressWarnings("unchecked")
        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            int limit = bufferSize - (bufferSize >> 2);
            long e = emitted;
            long ci = consumerIndex;
            int f = consumed;
            int m = length() - 1;
            Subscriber<? super T> a = downstream;

            for (;;) {
                long r = requested.get();

                while (e != r) {
                    if (cancelled) {
                        clear();
                        return;
                    }

                    boolean d = done;

                    int offset = (int)ci & m;
                    T t = get(offset);
                    boolean empty = t == null;

                    if (d && empty) {
                        Throwable ex = error.terminate();
                        if (ex == null) {
                            a.onComplete();
                        } else {
                            a.onError(ex);
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    int s = state;
                    if (s == STATE_FRESH) {
                        Publisher<Boolean> p;

                        try {
                            p = ObjectHelper.requireNonNull(asyncPredicate.apply(t), "The asyncPredicate returned a null value");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            error.addThrowable(ex);
                            p = null;
                        }

                        if (p != null) {
                            if (p instanceof Callable) {
                                Boolean u;

                                try {
                                    u = ((Callable<Boolean>)p).call();
                                } catch (Throwable ex) {
                                    Exceptions.throwIfFatal(ex);
                                    error.addThrowable(ex);
                                    u = null;
                                }

                                if (u != null && u) {
                                    a.onNext(t);
                                    e++;
                                }
                            } else {
                                InnerSubscriber<Boolean> inner = new InnerSubscriber<Boolean>(this);
                                if (current.compareAndSet(null, inner)) {
                                    state = STATE_RUNNING;
                                    p.subscribe(inner);
                                    break;
                                }
                            }
                        }

                        lazySet(offset, null);
                        ci++;
                        if (++f == limit) {
                            f = 0;
                            upstream.request(limit);
                        }
                    } else
                    if (s == STATE_RESULT) {
                        Boolean u = innerResult;
                        innerResult = null;

                        if (u != null && u) {
                            e++;
                            a.onNext(t);
                        }

                        lazySet(offset, null);
                        ci++;
                        if (++f == limit) {
                            f = 0;
                            upstream.request(limit);
                        }
                        state = STATE_FRESH;
                    } else {
                        break;
                    }
                }

                if (e == r) {
                    if (cancelled) {
                        clear();
                        return;
                    }

                    boolean d = done;

                    int offset = (int)ci & m;
                    T t = get(offset);
                    boolean empty = t == null;

                    if (d && empty) {
                        Throwable ex = error.terminate();
                        if (ex == null) {
                            a.onComplete();
                        } else {
                            a.onError(ex);
                        }
                        return;
                    }
                }

                int w = wip.get();
                if (missed == w) {
                    consumed = f;
                    consumerIndex = ci;
                    emitted = e;
                    missed = wip.addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }

        void clearCurrent() {
            InnerSubscriber<Boolean> c = current.get();
            if (c != INNER_CANCELLED) {
                current.compareAndSet(c, null);
            }
        }

        @Override
        public void innerResult(Boolean item) {
            innerResult = item;
            state = STATE_RESULT;
            clearCurrent();
            drain();
        }

        @Override
        public void innerError(Throwable ex) {
            error.addThrowable(ex);
            state = STATE_RESULT;
            clearCurrent();
            drain();
        }

        @Override
        public void innerComplete() {
            state = STATE_RESULT;
            clearCurrent();
            drain();
        }
    }
}
