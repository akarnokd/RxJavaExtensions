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

import java.util.Arrays;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.ObjectHelper;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Takes only the latest value from the sources in each zip round and applies a transformer function
 * on the array of values.
 * <p>
 * This operator acts similar to zip but only the latest value is buffered from each source. The
 * emission rate is determined by the availability of the slowest source and the consumption rate
 * of the downstream.
 * <p>
 * This operator is lossy; non-consumed items from sources are overwrittern with newer values.
 * @param <T> the element type of the sources
 * @param <R> the result type
 * @since 0.17.3
 */
final class FlowableZipLatest<T, R> extends Flowable<R> {

    final Publisher<? extends T>[] sources;

    final Iterable<? extends Publisher<? extends T>> sourcesIterable;

    final Function<? super Object[], ? extends R> combiner;

    final Scheduler scheduler;

    FlowableZipLatest(Publisher<? extends T>[] sources,
            Iterable<? extends Publisher<? extends T>> sourcesIterable,
            Function<? super Object[], ? extends R> combiner,
            Scheduler scheduler) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
        this.combiner = combiner;
        this.scheduler = scheduler;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        Publisher<? extends T>[] srcs = sources;
        int n;
        if (srcs == null) {
            srcs = new Publisher[8];
            n = 0;
            for (Publisher<? extends T> p : sourcesIterable) {
                if (n == srcs.length) {
                    srcs = Arrays.copyOf(srcs, n + (n >> 1));
                }
                srcs[n++] = p;
            }
        } else {
            n = srcs.length;
        }

        if (n == 0) {
            Flowable.<R>empty().observeOn(scheduler).subscribe(s);
        } else {
            ZipLatestCoordinator<T, R> zc = new ZipLatestCoordinator<T, R>(s, n, scheduler.createWorker(), combiner);
            s.onSubscribe(zc);

            zc.subscribe(srcs, n);
        }
    }

    static final class ZipLatestCoordinator<T, R> extends AtomicReferenceArray<T> implements Subscription, Runnable {

        private static final long serialVersionUID = -8321911708267957704L;

        final Subscriber<? super R> downstream;

        final InnerSubscriber<T>[] subscribers;

        final AtomicInteger wip;

        final AtomicLong requested;

        final Worker worker;

        final AtomicThrowable errors;

        final Function<? super Object[], ? extends R> combiner;

        volatile boolean cancelled;

        long emitted;

        @SuppressWarnings("unchecked")
        ZipLatestCoordinator(Subscriber<? super R> downstream, int n, Worker worker, Function<? super Object[], ? extends R> combiner) {
            super(n);
            this.downstream = downstream;
            this.subscribers = new InnerSubscriber[n];
            this.wip = new AtomicInteger();
            this.requested = new AtomicLong();
            this.errors = new AtomicThrowable();
            this.worker = worker;
            for (int i = 0; i < n; i++) {
                subscribers[i] = new InnerSubscriber<T>(this, i);
            }
            this.combiner = combiner;
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
            cancelAll();
            if (wip.getAndIncrement() == 0) {
                clear();
            }
        }

        void cancelAll() {
            for (InnerSubscriber<?> inner : subscribers) {
                inner.cancel();
            }
        }

        void clear() {
            int n = length();
            for (int i = 0; i < n; i++) {
                lazySet(i, null);
            }
        }

        void drain() {
            if (wip.getAndIncrement() == 0) {
                worker.schedule(this);
            }
        }

        @Override
        public void run() {
            int missed = 1;
            long e = emitted;
            InnerSubscriber<T>[] subs = subscribers;
            int n = subs.length;
            Subscriber<? super R> a = downstream;

            for (;;) {

                long r = requested.get();

                while (e != r) {
                    if (cancelled) {
                        clear();
                        return;
                    }
                    boolean someEmpty = false;
                    for (int i = 0; i < n; i++) {
                        boolean d = subs[i].done;
                        Object o = get(i);
                        if (d && o == null) {
                            cancelled = true;
                            cancelAll();
                            clear();
                            Throwable ex = errors.terminate();
                            if (ex == null) {
                                a.onComplete();
                            } else {
                                a.onError(ex);
                            }
                            worker.dispose();
                            return;
                        }
                        if (o == null) {
                            someEmpty = true;
                        }
                    }

                    if (someEmpty) {
                        break;
                    }
                    Object[] array = new Object[n];
                    for (int i = 0; i < n; i++) {
                        array[i] = getAndSet(i, null);
                    }

                    R v;

                    try {
                        v = ObjectHelper.requireNonNull(combiner.apply(array), "The combiner returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        errors.addThrowable(ex);
                        cancelled = true;
                        cancelAll();
                        clear();
                        a.onError(errors.terminate());
                        worker.dispose();
                        return;
                    }

                    a.onNext(v);

                    e++;
                }

                if (e == r) {
                    if (cancelled) {
                        clear();
                        return;
                    }

                    for (int i = 0; i < n; i++) {
                        if (subs[i].done && get(i) == null) {
                            cancelled = true;
                            cancelAll();
                            clear();
                            Throwable ex = errors.terminate();
                            if (ex == null) {
                                a.onComplete();
                            } else {
                                a.onError(ex);
                            }
                            worker.dispose();
                            return;
                        }
                    }
                }

                emitted = e;
                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        void subscribe(Publisher<? extends T>[] sources, int n) {
            for (int i = 0; i < n; i++) {
                if (cancelled) {
                    return;
                }
                sources[i].subscribe(subscribers[i]);
            }
        }

        static final class InnerSubscriber<T> extends AtomicReference<Subscription> implements FlowableSubscriber<T> {

            private static final long serialVersionUID = -5384962852497888461L;

            final ZipLatestCoordinator<T, ?> parent;

            final int index;

            volatile boolean done;

            InnerSubscriber(ZipLatestCoordinator<T, ?> parent, int index) {
                this.index = index;
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(T t) {
                ZipLatestCoordinator<T, ?> p = parent;
                p.lazySet(index, t);
                p.drain();
            }

            @Override
            public void onError(Throwable t) {
                ZipLatestCoordinator<T, ?> p = parent;
                if (p.errors.addThrowable(t)) {
                    lazySet(SubscriptionHelper.CANCELLED);
                    done = true;
                    p.drain();
                } else {
                    RxJavaPlugins.onError(t);
                }
            }

            @Override
            public void onComplete() {
                lazySet(SubscriptionHelper.CANCELLED);
                done = true;
                parent.drain();
            }

            void cancel() {
                SubscriptionHelper.cancel(this);
            }
        }
    }
}
