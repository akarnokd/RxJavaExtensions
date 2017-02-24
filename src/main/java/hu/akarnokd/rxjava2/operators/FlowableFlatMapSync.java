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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.SimpleQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * FlatMap a bounded number of inner, non-trivial flows (unbound not supported).
 *
 * @param <T> the input value type
 * @param <R> the result value type
 */
final class FlowableFlatMapSync<T, R> extends Flowable<R> {

    final Publisher<T> source;

    final Function<? super T, ? extends Publisher<? extends R>> mapper;

    final int maxConcurrency;

    final int bufferSize;

    FlowableFlatMapSync(Publisher<T> source, Function<? super T, ? extends Publisher<? extends R>> mapper,
            int maxConcurrency, int bufferSize) {
        this.source = source;
        this.mapper = mapper;
        this.maxConcurrency = maxConcurrency;
        this.bufferSize = bufferSize;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new FlatMapOuterSubscriber<T, R>(s, mapper, maxConcurrency, bufferSize));
    }

    static final class FlatMapOuterSubscriber<T, R> extends AtomicInteger
    implements Subscriber<T>, Subscription {

        private static final long serialVersionUID = -208456984819517117L;

        final Subscriber<? super R> actual;

        final Function<? super T, ? extends Publisher<? extends R>> mapper;

        final int maxConcurrency;

        final int bufferSize;

        final AtomicLong requested;

        final AtomicReferenceArray<FlatMapInnerSubscriber<T, R>> subscribers;

        final AtomicIntegerArray freelist;

        final AtomicThrowable error;

        volatile boolean done;

        volatile boolean cancelled;

        Subscription upstream;

        FlatMapOuterSubscriber(Subscriber<? super R> actual,
                Function<? super T, ? extends Publisher<? extends R>> mapper, int maxConcurrency, int bufferSize) {
            this.actual = actual;
            this.mapper = mapper;
            this.maxConcurrency = maxConcurrency;
            this.bufferSize = bufferSize;
            this.requested = new AtomicLong();
            this.error = new AtomicThrowable();

            int c = Pow2.roundToPowerOfTwo(maxConcurrency);
            this.subscribers = new AtomicReferenceArray<FlatMapInnerSubscriber<T, R>>(c);
            this.freelist = new AtomicIntegerArray(c + 2);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                actual.onSubscribe(this);

                s.request(maxConcurrency);
            }
        }

        @Override
        public void onNext(T t) {

            Publisher<? extends R> p;

            try {
                p = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.cancel();
                if (error.addThrowable(ex)) {
                    done = true;
                    drain();
                } else {
                    RxJavaPlugins.onError(ex);
                }
                return;
            }

            if (!cancelled) {
                AtomicIntegerArray fl = freelist;
                AtomicReferenceArray<FlatMapInnerSubscriber<T, R>> s = subscribers;
                int m = s.length();

                int ci = fl.get(m);
                int idx = fl.get(ci);
                if (idx == 0) {
                    idx = ci + 1;
                }

                FlatMapInnerSubscriber<T, R> inner = new FlatMapInnerSubscriber<T, R>(this, bufferSize, idx);
                s.lazySet(idx - 1, inner);
                fl.lazySet(m, (ci + 1) & (m - 1));
                if (cancelled) {
                    s.lazySet(idx - 1, null);
                } else {
                    p.subscribe(inner);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (error.addThrowable(t)) {
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
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
                cancelInners();
            }
        }

        void drain() {
            // TODO implement
        }

        void remove(FlatMapInnerSubscriber<T, R> inner) {
            AtomicIntegerArray fl = freelist;
            AtomicReferenceArray<FlatMapInnerSubscriber<T, R>> s = subscribers;
            int m = s.length();
            int idx = inner.index;
            int pi = fl.get(m + 1);
            s.lazySet(idx - 1, null);
            fl.lazySet(pi, idx);
            fl.lazySet(m + 1, (pi + 1) & (m - 1));
        }

        void cancelInners() {
            AtomicReferenceArray<FlatMapInnerSubscriber<T, R>> s = subscribers;
            int m = s.length();
            for (int i = 0; i < m; i++) {
                FlatMapInnerSubscriber<T, R> inner = s.get(i);
                if (inner != null) {
                    s.lazySet(i, null);
                    inner.cancel();
                }
            }
        }

        void innerNext(FlatMapInnerSubscriber<T, R> inner, R item) {
            // TODO implement
        }

        void innerError(FlatMapInnerSubscriber<T, R> inner, Throwable ex) {
            // TODO implement
        }

        void innerComplete(FlatMapInnerSubscriber<T, R> inner) {
            // TODO implement
        }

        static final class FlatMapInnerSubscriber<T, R> extends AtomicReference<Subscription>
        implements Subscriber<R> {

            private static final long serialVersionUID = -4991009168975207961L;

            final FlatMapOuterSubscriber<T, R> parent;

            final int bufferSize;

            final int limit;

            final int index;

            int produced;

            volatile boolean done;

            volatile SimpleQueue<R> queue;

            FlatMapInnerSubscriber(FlatMapOuterSubscriber<T, R> parent, int bufferSize, int index) {
                this.parent = parent;
                this.bufferSize = bufferSize;
                this.limit = bufferSize - (bufferSize >> 2);
                this.index = index;
            }

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    // TODO fusion?!
                    s.request(bufferSize);
                }
            }

            @Override
            public void onNext(R t) {
                parent.innerNext(this, t);
            }

            @Override
            public void onError(Throwable t) {
                parent.innerError(this, t);
            }

            @Override
            public void onComplete() {
                parent.innerComplete(this);
            }

            void cancel() {
                SubscriptionHelper.cancel(this);
            }

            void producedOne() {
                int p = produced + 1;
                if (p == limit) {
                    produced = 0;
                    get().request(p);
                } else {
                    produced = p;
                }
            }
        }
    }
}
