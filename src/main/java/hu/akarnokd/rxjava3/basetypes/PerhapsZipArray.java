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

import java.util.Arrays;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Waits till all sources produce a value and calls a function
 * to generate the only resulting value or terminates if
 * one of the sources terminates without value.
 *
 * @param <T> the shared base source value type
 * @param <R> the result value type
 */
final class PerhapsZipArray<T, R> extends Perhaps<R> {

    final Perhaps<? extends T>[] sources;

    final Function<? super Object[], ? extends R> zipper;

    PerhapsZipArray(Perhaps<? extends T>[] sources, Function<? super Object[], ? extends R> zipper) {
        this.sources = sources;
        this.zipper = zipper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        Perhaps<? extends T>[] srcs = sources;
        int n = srcs.length;
        ZipCoordinator<T, R> parent = new ZipCoordinator<>(s, zipper, n);
        s.onSubscribe(parent);

        parent.subscribe(srcs, n);
    }

    static final class ZipCoordinator<T, R> extends DeferredScalarSubscription<R> {

        private static final long serialVersionUID = 278835184144033561L;

        final Function<? super Object[], ? extends R> zipper;

        final AtomicInteger wip;

        final ZipInnerSubscriber<T, R>[] subscribers;

        final Object[] values;

        @SuppressWarnings("unchecked")
        ZipCoordinator(Subscriber<? super R> downstream, Function<? super Object[], ? extends R> zipper, int n) {
            super(downstream);
            this.zipper = zipper;
            this.wip = new AtomicInteger(n);
            this.subscribers = new ZipInnerSubscriber[n];
            for (int i = 0; i < n; i++) {
                subscribers[i] = new ZipInnerSubscriber<>(i, this);
            }
            this.values = new Object[n];
        }

        void subscribe(Perhaps<? extends T>[] sources, int n) {
            ZipInnerSubscriber<T, R>[] subs = subscribers;
            AtomicInteger w = wip;
            for (int i = 0; i < n; i++) {
                if (w.get() <= 0) {
                    break;
                }
                sources[i].subscribe(subs[i]);
            }
        }

        void innerNext(int index, T value) {
            values[index] = value;
            if (wip.decrementAndGet() == 0) {
                R v;

                try {
                    v = zipper.apply(values);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    downstream.onError(ex);
                    return;
                }

                Arrays.fill(values, this);

                complete(v);
            }
        }

        void innerError(int index, Throwable error) {
            if (wip.getAndSet(0) > 0) {
                cancel(index);
                Arrays.fill(values, this);
                downstream.onError(error);
            } else {
                RxJavaPlugins.onError(error);
            }
        }

        void innerComplete(int index) {
            if (values[index] == null) {
                if (wip.getAndSet(0) > 0) {
                    cancel(index);

                    Arrays.fill(values, this);

                    downstream.onComplete();
                }
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            cancel(-1);
        }

        void cancel(int index) {
            ZipInnerSubscriber<T, R>[] subs = subscribers;
            for (int i = 0; i < subs.length; i++) {
                if (i != index) {
                    SubscriptionHelper.cancel(subs[i]);
                }
            }
        }

        static final class ZipInnerSubscriber<T, R> extends AtomicReference<Subscription>
        implements Subscriber<T> {

            private static final long serialVersionUID = 2125487621013035317L;

            final ZipCoordinator<T, R> parent;

            final int index;

            ZipInnerSubscriber(int index, ZipCoordinator<T, R> parent) {
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
                parent.innerNext(index, t);
            }

            @Override
            public void onError(Throwable t) {
                parent.innerError(index, t);
            }

            @Override
            public void onComplete() {
                parent.innerComplete(index);
            }
        }
    }
}
