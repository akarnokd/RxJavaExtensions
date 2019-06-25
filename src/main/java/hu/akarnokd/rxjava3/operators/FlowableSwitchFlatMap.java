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

import java.util.*;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

final class FlowableSwitchFlatMap<T, R> extends Flowable<R>
implements FlowableTransformer<T, R> {

    final Publisher<T> source;

    final Function<? super T, ? extends Publisher<? extends R>> mapper;

    final int maxActive;

    final int bufferSize;

    FlowableSwitchFlatMap(Publisher<T> source, Function<? super T, ? extends Publisher<? extends R>> mapper,
            int maxActive, int bufferSize) {
        super();
        this.source = source;
        this.mapper = mapper;
        this.maxActive = maxActive;
        this.bufferSize = bufferSize;
    }

    @Override
    public Publisher<R> apply(Flowable<T> upstream) {
        return new FlowableSwitchFlatMap<T, R>(upstream, mapper, maxActive, bufferSize);
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new SwitchFlatMapSubscriber<T, R>(s, mapper, maxActive, bufferSize));
    }

    static final class SwitchFlatMapSubscriber<T, R>
    extends AtomicInteger
    implements Subscriber<T>, Subscription {

        private static final long serialVersionUID = 6801374887555723721L;

        final Subscriber<? super R> downstream;

        final Function<? super T, ? extends Publisher<? extends R>> mapper;

        final int maxActive;

        final int bufferSize;

        final ArrayDeque<SfmInnerSubscriber<T, R>> active;

        final AtomicLong requested;

        final AtomicThrowable error;

        Subscription upstream;

        volatile boolean done;

        volatile boolean cancelled;

        volatile long version;

        final SfmInnerSubscriber<T, R>[] activeCache;
        long versionCache;

        @SuppressWarnings("unchecked")
        SwitchFlatMapSubscriber(Subscriber<? super R> downstream,
                Function<? super T, ? extends Publisher<? extends R>> mapper, int maxActive,
                        int bufferSize) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.maxActive = maxActive;
            this.bufferSize = bufferSize;
            this.active = new ArrayDeque<SfmInnerSubscriber<T, R>>();
            this.requested = new AtomicLong();
            this.error = new AtomicThrowable();
            this.activeCache = new SfmInnerSubscriber[maxActive];
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            Publisher<? extends R> p;
            try {
                p = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null Publisher");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.cancel();
                onError(ex);
                return;
            }

            SfmInnerSubscriber<T, R> inner = new SfmInnerSubscriber<T, R>(this, bufferSize);
            if (add(inner)) {
                p.subscribe(inner);
            }
        }

        boolean add(SfmInnerSubscriber<T, R> inner) {
            SfmInnerSubscriber<T, R> evicted = null;
            synchronized (this) {
                if (cancelled) {
                    return false;
                }

                if (active.size() == maxActive) {
                    evicted = active.poll();
                }
                active.offer(inner);
                version++;
            }

            if (evicted != null) {
                evicted.cancel();
            }
            return true;
        }

        void remove(SfmInnerSubscriber<T, R> inner) {
            synchronized (this) {
                active.remove(inner);
                version++;
            }
        }

        @Override
        public void onError(Throwable t) {
            if (error.compareAndSet(null, t)) {
                cancelInners();
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
                if (getAndIncrement() == 0) {
                    clearCache();
                }
            }
        }

        void clearCache() {
            Arrays.fill(activeCache, null);
        }

        void cancelInners() {
            List<SfmInnerSubscriber<T, R>> subscribers = new ArrayList<SfmInnerSubscriber<T, R>>();
            synchronized (this) {
                subscribers.addAll(active);
                active.clear();
            }
            for (SfmInnerSubscriber<T, R> inner : subscribers) {
                inner.cancel();
            }
        }

        void innerError(Throwable t) {
            if (error.compareAndSet(null, t)) {
                upstream.cancel();
                cancelInners();
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        void updateInners() {
            SfmInnerSubscriber<T, R>[] a = activeCache;
            if (versionCache != version) {
                synchronized (this) {
                    int i = 0;
                    Iterator<SfmInnerSubscriber<T, R>> it = active.iterator();
                    while (it.hasNext()) {
                        a[i++] = it.next();
                    }
                    for (int j = i; j < a.length; j++) {
                        a[j] = null;
                    }
                    versionCache = version;
                }
            }
        }

        void drain() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                Subscriber<? super R> a = downstream;
                SfmInnerSubscriber<T, R>[] inners = activeCache;
                AtomicThrowable err = error;

                outer:
                for (;;) {
                    long r = requested.get();
                    long e = 0;

                    for (;;) {
                        if (cancelled) {
                            clearCache();
                            return;
                        }

                        boolean d = done;

                        updateInners();
                        long ver = versionCache;

                        if (d) {
                            Throwable ex = err.get();
                            if (ex != null) {
                                clearCache();

                                a.onError(err.terminate());
                                return;
                            } else
                            if (inners[0] == null) {
                                a.onComplete();
                                return;
                            }
                        }

                        int becameEmpty = 0;
                        int activeCount = 0;

                        draining:
                        for (SfmInnerSubscriber<T, R> inner : inners) {
                            if (cancelled) {
                                clearCache();
                                return;
                            }

                            if (inner == null) {
                                break;
                            }
                            if (ver != version) {
                                if (e != 0) {
                                    BackpressureHelper.produced(requested, e);
                                }
                                continue outer;
                            }

                            activeCount++;

                            long f = 0;

                            SimplePlainQueue<R> q = inner.queue;

                            while (e != r) {
                                if (cancelled) {
                                    clearCache();
                                    return;
                                }

                                Throwable ex = err.get();
                                if (ex != null) {
                                    clearCache();

                                    a.onError(err.terminate());
                                    return;
                                }

                                if (ver != version) {
                                    if (e != 0) {
                                        BackpressureHelper.produced(requested, e);
                                    }
                                    if (f != 0L) {
                                        inner.produced(f);
                                    }
                                    continue outer;
                                }

                                boolean d2 = inner.done;
                                R v = q.poll();
                                boolean empty = v == null;

                                if (d2 && empty) {
                                    remove(inner);
                                    continue draining;
                                }

                                if (empty) {
                                    if (f != 0L) {
                                        inner.produced(f);
                                        f = 0L;
                                    }
                                    becameEmpty++;
                                    break;
                                }

                                a.onNext(v);
                                e++;
                                f++;
                            }

                            if (inner.done && q.isEmpty()) {
                                remove(inner);
                            } else
                            if (f != 0L) {
                                inner.produced(f);
                            }
                        }

                        if (becameEmpty == activeCount || e == r) {
                            break;
                        }
                    }

                    if (e != 0) {
                        BackpressureHelper.produced(requested, e);
                    }

                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                }
            }
        }

        static final class SfmInnerSubscriber<T, R> extends AtomicReference<Subscription>
        implements Subscriber<R> {

            private static final long serialVersionUID = 4011255448052082638L;

            final SwitchFlatMapSubscriber<T, R> parent;

            final int bufferSize;

            final int limit;

            final SimplePlainQueue<R> queue;

            long produced;

            volatile boolean done;

            SfmInnerSubscriber(SwitchFlatMapSubscriber<T, R> parent, int bufferSize) {
                this.parent = parent;
                this.bufferSize = bufferSize;
                this.limit = bufferSize - (bufferSize >> 2);
                this.queue = new SpscArrayQueue<R>(bufferSize);
            }

            void cancel() {
                SubscriptionHelper.cancel(this);
            }

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(bufferSize);
                }
            }

            @Override
            public void onNext(R t) {
                queue.offer(t);
                parent.drain();
            }

            @Override
            public void onError(Throwable t) {
                parent.innerError(t);
            }

            @Override
            public void onComplete() {
                done = true;
                parent.drain();
            }

            void produced(long f) {
                long p = produced + f;
                if (p >= limit) {
                    produced = 0;
                    get().request(p);
                } else {
                    produced = p;
                }
            }
        }
    }
}
