/*
 * Copyright 2016-2017 David Karnok
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

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Maps each upstream value into a single value provided by a generated Publisher for that
 * input value and comines the original and generated single value into a final result item
 * to be emitted to downstream.
 * <p>Only the first item emitted by the inner Publisher's are considered. If
 * the inner Publisher is empty, no resulting item is generated for that input value.
 * @param <T> the input value type
 * @param <U> the intermediate value type
 * @param <R> the result value type
 *
 * @since 0.16.2
 */
final class FlowableMapAsync<T, U, R> extends Flowable<R> implements FlowableTransformer<T, R> {

    final Flowable<T> source;

    final Function<? super T, ? extends Publisher<? extends U>> mapper;

    final BiFunction<? super T, ? super U, ? extends R> combiner;

    final int bufferSize;

    FlowableMapAsync(Flowable<T> source, Function<? super T, ? extends Publisher<? extends U>> mapper,
            BiFunction<? super T, ? super U, ? extends R> combiner, int bufferSize) {
        this.source = source;
        this.mapper = mapper;
        this.combiner = combiner;
        this.bufferSize = bufferSize;
    }

    @Override
    public Publisher<R> apply(Flowable<T> upstream) {
        return new FlowableMapAsync<T, U, R>(upstream, mapper, combiner, bufferSize);
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new MapAsyncSubscriber<T, U, R>(s, mapper, combiner, bufferSize));
    }

    interface AsyncSupport<U> {

        void innerResult(U u);

        void innerError(Throwable ex);

        void innerComplete();
    }

    static final class MapAsyncSubscriber<T, U, R>
    extends AtomicReferenceArray<T>
    implements FlowableSubscriber<T>, Subscription, AsyncSupport<U> {

        private static final long serialVersionUID = -1557840206706079339L;

        final Subscriber<? super R> actual;

        final Function<? super T, ? extends Publisher<? extends U>> mapper;

        final BiFunction<? super T, ? super U, ? extends R> combiner;

        final int bufferSize;

        final AtomicThrowable error;

        final AtomicLong requested;

        final AtomicInteger wip;

        final AtomicReference<InnerSubscriber<U>> current;

        @SuppressWarnings({ "rawtypes", "unchecked" })
        static final InnerSubscriber INNER_CANCELLED = new InnerSubscriber(null);

        Subscription upstream;

        long producerIndex;

        long consumerIndex;

        int consumed;

        volatile boolean done;

        volatile boolean cancelled;

        U innerResult;

        long emitted;

        volatile int state;
        static final int STATE_FRESH = 0;
        static final int STATE_RUNNING = 1;
        static final int STATE_RESULT = 2;

        MapAsyncSubscriber(Subscriber<? super R> actual,
                Function<? super T, ? extends Publisher<? extends U>> mapper,
                BiFunction<? super T, ? super U, ? extends R> combiner, int bufferSize) {
            super(Pow2.roundToPowerOfTwo(bufferSize));
            this.actual = actual;
            this.mapper = mapper;
            this.combiner = combiner;
            this.bufferSize = bufferSize;
            this.error = new AtomicThrowable();
            this.requested = new AtomicLong();
            this.wip = new AtomicInteger();
            this.current = new AtomicReference<InnerSubscriber<U>>();
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
            InnerSubscriber<U> a = current.get();
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

                actual.onSubscribe(this);

                s.request(bufferSize);
            }
        }

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
            Subscriber<? super R> a = actual;

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
                        Publisher<? extends U> p;

                        try {
                            p = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null value");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            error.addThrowable(ex);
                            p = null;
                        }

                        if (p != null) {
                            if (p instanceof Callable) {
                                R v;

                                try {
                                    @SuppressWarnings("unchecked")
                                    U u = ((Callable<U>)p).call();
                                    if (u != null) {
                                        v = ObjectHelper.requireNonNull(combiner.apply(t, u), "The combiner returned a null value");
                                    } else {
                                        v = null;
                                    }
                                } catch (Throwable ex) {
                                    Exceptions.throwIfFatal(ex);
                                    error.addThrowable(ex);
                                    v = null;
                                }

                                if (v != null) {
                                    a.onNext(v);
                                    e++;
                                }
                            } else {
                                InnerSubscriber<U> inner = new InnerSubscriber<U>(this);
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
                        U u = innerResult;
                        innerResult = null;

                        if (u != null) {
                            R v;

                            try {
                                v = ObjectHelper.requireNonNull(combiner.apply(t, u), "The combiner returned a null value");
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                error.addThrowable(ex);
                                v = null;
                            }

                            if (v != null) {
                                a.onNext(v);
                                e++;
                            }
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
            InnerSubscriber<U> c = current.get();
            if (c != INNER_CANCELLED) {
                current.compareAndSet(c, null);
            }
        }

        @Override
        public void innerResult(U item) {
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

        static final class InnerSubscriber<U> extends AtomicReference<Subscription> implements Subscriber<U> {

            private static final long serialVersionUID = 6335578148970008248L;

            final AsyncSupport<U> parent;

            boolean done;

            InnerSubscriber(AsyncSupport<U> parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(U t) {
                if (!done) {
                    get().cancel();
                    done = true;
                    parent.innerResult(t);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (!done) {
                    done = true;
                    parent.innerError(t);
                } else {
                    RxJavaPlugins.onError(t);
                }
            }

            @Override
            public void onComplete() {
                if (!done) {
                    done = true;
                    parent.innerComplete();
                }
            }

            void cancel() {
                SubscriptionHelper.cancel(this);
            }
        }
    }
}
