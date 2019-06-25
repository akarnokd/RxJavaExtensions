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
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * FlatMap a bounded number of inner, non-trivial flows (unbound not supported).
 *
 * @param <T> the input value type
 * @param <R> the result value type
 *
 * @since 0.16.0
 */
final class FlowableFlatMapSync<T, R> extends Flowable<R> implements FlowableTransformer<T, R> {

    final Publisher<T> source;

    final Function<? super T, ? extends Publisher<? extends R>> mapper;

    final int maxConcurrency;

    final int bufferSize;

    final boolean depthFirst;

    FlowableFlatMapSync(Publisher<T> source, Function<? super T, ? extends Publisher<? extends R>> mapper,
            int maxConcurrency, int bufferSize, boolean depthFirst) {
        this.source = source;
        this.mapper = mapper;
        this.maxConcurrency = maxConcurrency;
        this.bufferSize = bufferSize;
        this.depthFirst = depthFirst;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new FlatMapOuterSubscriber<T, R>(s, mapper, maxConcurrency, bufferSize, depthFirst));
    }

    @Override
    public Publisher<R> apply(Flowable<T> upstream) {
        return new FlowableFlatMapSync<T, R>(upstream, mapper, maxConcurrency, bufferSize, depthFirst);
    }

    interface FlatMapInnerSubscriberSupport<T, R> {

        void innerNext(FlatMapInnerSubscriber<T, R> inner, R value);

        void innerError(FlatMapInnerSubscriber<T, R> inner, Throwable ex);

        void innerComplete(FlatMapInnerSubscriber<T, R> inner);

        void drain();
    }

    abstract static class BaseFlatMapOuterSubscriber<T, R> extends AtomicInteger
    implements Subscriber<T>, Subscription, FlatMapInnerSubscriberSupport<T, R> {

        private static final long serialVersionUID = -208456984819517117L;

        final Subscriber<? super R> downstream;

        final Function<? super T, ? extends Publisher<? extends R>> mapper;

        final int maxConcurrency;

        final int bufferSize;

        final AtomicLong requested;

        final AtomicReferenceArray<FlatMapInnerSubscriber<T, R>> subscribers;

        final AtomicIntegerArray freelist;

        final AtomicThrowable error;

        final boolean depthFirst;

        final AtomicLong active;

        volatile boolean done;

        volatile boolean cancelled;

        Subscription upstream;

        long emitted;

        long finished;

        static final int PRODUCER_INDEX = 16;
        static final int CONSUMER_INDEX = 32;

        BaseFlatMapOuterSubscriber(Subscriber<? super R> downstream,
                Function<? super T, ? extends Publisher<? extends R>> mapper,
                        int maxConcurrency, int bufferSize,
                        boolean depthFirst) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.maxConcurrency = maxConcurrency;
            this.bufferSize = bufferSize;
            this.requested = new AtomicLong();
            this.error = new AtomicThrowable();
            this.depthFirst = depthFirst;
            this.active = new AtomicLong();

            int c = Pow2.roundToPowerOfTwo(maxConcurrency);
            this.subscribers = new AtomicReferenceArray<FlatMapInnerSubscriber<T, R>>(c);
            this.freelist = new AtomicIntegerArray(c + CONSUMER_INDEX + 16);
        }

        @Override
        public final void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);

                s.request(maxConcurrency);
            }
        }

        @Override
        public final void onNext(T t) {

            Publisher<? extends R> p;

            try {
                p = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.cancel();
                cancelInners();
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

                int ci = fl.get(m + CONSUMER_INDEX);
                int idx = fl.get(ci);
                if (idx == 0) {
                    idx = ci + 1;
                }

                FlatMapInnerSubscriber<T, R> inner = new FlatMapInnerSubscriber<T, R>(this, bufferSize, idx);
                s.lazySet(idx - 1, inner);
                fl.lazySet(m + CONSUMER_INDEX, (ci + 1) & (m - 1));

                AtomicLong act = active;
                act.lazySet(act.get() + 1);

                if (cancelled) {
                    s.lazySet(idx - 1, null);
                } else {
                    p.subscribe(inner);
                }
            }
        }

        @Override
        public final void onError(Throwable t) {
            if (error.addThrowable(t)) {
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public final void onComplete() {
            done = true;
            drain();
        }

        @Override
        public final void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        @Override
        public final void cancel() {
            if (!cancelled) {
                cancelled = true;
                upstream.cancel();
                cancelInners();
                cleanupAfter();
            }
        }

        abstract void cleanupAfter();

        final void depthFirst() {
            int missed = 1;
            long e = emitted;
            AtomicReferenceArray<FlatMapInnerSubscriber<T, R>> s = subscribers;
            int m = s.length();
            Subscriber<? super R> a = downstream;
            AtomicLong act = active;

            for (;;) {

                long r = requested.get();

                while (e != r) {
                    if (cancelled) {
                        return;
                    }

                    boolean d = done;

                    if (d) {
                        Throwable ex = error.get();
                        if (ex != null) {
                            a.onError(error.terminate());
                            cleanupAfter();
                            return;
                        }
                    }

                    long n = act.get();
                    long f = finished;
                    int innerEmpty = 0;

                    for (int i = 0, j = 0; i < m && j + f < n; i++) {
                        FlatMapInnerSubscriber<T, R> inner = s.get(i);
                        if (inner != null) {
                            j++;
                            boolean innerDone = inner.done;
                            SimpleQueue<R> q = inner.queue;

                            if (innerDone && (q == null || q.isEmpty())) {
                                remove(inner);
                                finished++;
                                innerEmpty++;
                                upstream.request(1);
                            } else
                            if (q != null) {
                                while (e != r) {
                                    if (cancelled) {
                                        return;
                                    }

                                    if (d) {
                                        Throwable ex = error.get();
                                        if (ex != null) {
                                            a.onError(error.terminate());
                                            cleanupAfter();
                                            return;
                                        }
                                    }

                                    R v;

                                    try {
                                        v = q.poll();
                                    } catch (Throwable ex) {
                                        Exceptions.throwIfFatal(ex);
                                        error.addThrowable(ex);
                                        upstream.cancel();
                                        cancelInners();
                                        a.onError(error.terminate());
                                        cleanupAfter();
                                        return;
                                    }

                                    boolean empty = v == null;

                                    if (innerDone && empty) {
                                        remove(inner);
                                        finished++;
                                        innerEmpty++;
                                        upstream.request(1);
                                        break;
                                    }

                                    if (empty) {
                                        innerEmpty++;
                                        break;
                                    }

                                    a.onNext(v);

                                    e++;

                                    inner.producedOne();
                                }
                            } else {
                                innerEmpty++;
                            }
                        }
                    }

                    n = act.get();
                    f = finished;
                    if (d) {
                        if (n == f) {
                            a.onComplete();
                            cleanupAfter();
                            return;
                        }
                    }

                    if (innerEmpty + f == n) {
                        break;
                    }
                }

                if (e == r) {
                    if (cancelled) {
                        return;
                    }

                    boolean d = done;

                    if (d) {

                        Throwable ex = error.get();
                        if (ex != null) {
                            a.onError(error.terminate());
                            cleanupAfter();
                            return;
                        }

                        long n = act.get();

                        if (n == finished) {
                            a.onComplete();
                            cleanupAfter();
                            return;
                        }
                    }
                }

                int w = get();
                if (w == missed) {
                    emitted = e;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }

        final void breadthFirst() {
            int missed = 1;
            long e = emitted;
            AtomicReferenceArray<FlatMapInnerSubscriber<T, R>> s = subscribers;
            int m = s.length();
            Subscriber<? super R> a = downstream;
            AtomicLong act = active;

            for (;;) {

                for (;;) {

                    long r = requested.get();
                    long alive = act.get() - finished;
                    int innerEmpty = 0;

                    int j = 0;
                    for (int i = 0; i < m && j < alive; i++) {
                        if (cancelled) {
                            return;
                        }

                        if (done) {
                            Throwable ex = error.get();
                            if (ex != null) {
                                a.onError(error.terminate());
                                cleanupAfter();
                                return;
                            }
                        }

                        FlatMapInnerSubscriber<T, R> inner = s.get(i);
                        if (inner != null) {
                            j++;
                            boolean innerDone = inner.done;
                            SimpleQueue<R> q = inner.queue;

                            if (innerDone && (q == null || q.isEmpty())) {
                                remove(inner);
                                finished++;
                                innerEmpty++;
                                upstream.request(1);
                            } else
                            if (q != null) {
                                if (e != r) {
                                    R v;

                                    try {
                                        v = q.poll();
                                    } catch (Throwable ex) {
                                        Exceptions.throwIfFatal(ex);
                                        error.addThrowable(ex);
                                        upstream.cancel();
                                        cancelInners();
                                        a.onError(error.terminate());
                                        cleanupAfter();
                                        return;
                                    }

                                    if (v == null) {
                                        innerEmpty++;
                                    } else {
                                        a.onNext(v);
                                        e++;
                                        inner.producedOne();
                                    }
                                }
                            } else {
                                innerEmpty++;
                            }
                        }
                    }

                    if (e == r) {
                        if (cancelled) {
                            return;
                        }

                        if (done) {
                            Throwable ex = error.get();
                            if (ex != null) {
                                a.onError(error.terminate());
                                cleanupAfter();
                                return;
                            }

                            if (finished == act.get()) {
                                a.onComplete();
                                cleanupAfter();
                                return;
                            }
                        }
                        break;
                    }

                    long f = finished;

                    long in2 = act.get();
                    if (done && in2 == f) {
                        a.onComplete();
                        cleanupAfter();
                        return;
                    }

                    if (innerEmpty == alive) {
                        break;
                    }
                }

                int w = get();
                if (w == missed) {
                    emitted = e;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }

        final void remove(FlatMapInnerSubscriber<T, R> inner) {
            AtomicIntegerArray fl = freelist;
            AtomicReferenceArray<FlatMapInnerSubscriber<T, R>> s = subscribers;
            int m = s.length();
            int idx = inner.index;
            int pi = fl.get(m + PRODUCER_INDEX);
            s.lazySet(idx - 1, null);
            fl.lazySet(pi, idx);
            fl.lazySet(m + PRODUCER_INDEX, (pi + 1) & (m - 1));
        }

        final void cancelInners() {
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
    }

    static final class FlatMapOuterSubscriber<T, R> extends BaseFlatMapOuterSubscriber<T, R> {
        private static final long serialVersionUID = -5109342841608286301L;

        FlatMapOuterSubscriber(Subscriber<? super R> downstream,
                Function<? super T, ? extends Publisher<? extends R>> mapper, int maxConcurrency, int bufferSize,
                boolean depthFirst) {
            super(downstream, mapper, maxConcurrency, bufferSize, depthFirst);
        }

        @Override
        public void drain() {
            if (getAndIncrement() == 0) {
                drainLoop();
            }
        }

        void drainLoop() {
            if (depthFirst) {
                depthFirst();
            } else {
                breadthFirst();
            }
        }

        @Override
        void cleanupAfter() {
        }

        @Override
        public void innerNext(FlatMapInnerSubscriber<T, R> inner, R item) {
            if (get() == 0 && compareAndSet(0, 1)) {
                long r = requested.get();
                long e = emitted;
                if (e != r) {
                    downstream.onNext(item);
                    emitted = e + 1;
                    inner.producedOne();
                } else {
                    SimpleQueue<R> q = inner.queue();
                    q.offer(item);
                }
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SimpleQueue<R> q = inner.queue();
                q.offer(item);
                if (getAndIncrement() != 0) {
                    return;
                }
            }
            drainLoop();
        }

        @Override
        public void innerError(FlatMapInnerSubscriber<T, R> inner, Throwable ex) {
            remove(inner);
            if (error.addThrowable(ex)) {
                inner.done = true;
                done = true;
                upstream.cancel();
                cancelInners();
                drain();
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void innerComplete(FlatMapInnerSubscriber<T, R> inner) {
            inner.done = true;
            drain();
        }
    }

    static final class FlatMapInnerSubscriber<T, R> extends AtomicReference<Subscription>
    implements Subscriber<R> {

        private static final long serialVersionUID = -4991009168975207961L;

        final FlatMapInnerSubscriberSupport<T, R> parent;

        final int bufferSize;

        final int limit;

        final int index;

        int produced;

        int fusionMode;

        volatile boolean done;

        volatile SimpleQueue<R> queue;

        FlatMapInnerSubscriber(FlatMapInnerSubscriberSupport<T, R> parent, int bufferSize, int index) {
            this.parent = parent;
            this.bufferSize = bufferSize;
            this.limit = bufferSize - (bufferSize >> 2);
            this.index = index;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {
                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<R> qs = (QueueSubscription<R>) s;

                    int m = qs.requestFusion(QueueFuseable.ANY | QueueFuseable.BOUNDARY);

                    if (m == QueueFuseable.SYNC) {
                        fusionMode = m;
                        queue = qs;
                        done = true;
                        parent.drain();
                        return;
                    }
                    if (m == QueueFuseable.ASYNC) {
                        fusionMode = m;
                        queue = qs;
                        s.request(bufferSize);
                        return;
                    }
                }
                s.request(bufferSize);
            }
        }

        @Override
        public void onNext(R t) {
            if (fusionMode == QueueFuseable.NONE) {
                parent.innerNext(this, t);
            } else {
                parent.drain();
            }
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
            if (fusionMode != QueueFuseable.SYNC) {
                int p = produced + 1;
                if (p == limit) {
                    produced = 0;
                    get().request(p);
                } else {
                    produced = p;
                }
            }
        }

        SimpleQueue<R> queue() {
            SimpleQueue<R> q = queue;
            if (q == null) {
                q = new SpscArrayQueue<R>(bufferSize);
                queue = q;
            }
            return q;
        }
    }
}
