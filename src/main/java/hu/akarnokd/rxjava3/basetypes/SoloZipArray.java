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
import io.reactivex.rxjava3.internal.functions.ObjectHelper;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Combines the solo values of all the sources via a zipper function into a
 * single resulting value.
 * @param <T> the common input base type
 * @param <R> the result type
 */
final class SoloZipArray<T, R> extends Solo<R> {

    final Solo<? extends T>[] sources;

    final Function<? super Object[], ? extends R> zipper;

    SoloZipArray(Solo<? extends T>[] sources, Function<? super Object[], ? extends R> zipper) {
        this.sources = sources;
        this.zipper = zipper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        Solo<? extends T>[] a = sources;
        int n = a.length;
        ZipCoordinator<T, R> parent = new ZipCoordinator<T, R>(s, zipper, n);
        s.onSubscribe(parent);

        parent.subscribe(a, n);
    }

    static final class ZipCoordinator<T, R> extends DeferredScalarSubscription<R> {

        private static final long serialVersionUID = -4130106888008958190L;

        final Function<? super Object[], ? extends R> zipper;

        final Object[] values;

        final ZipSubscriber<T, R>[] subscribers;

        final AtomicInteger wip;

        ZipCoordinator(Subscriber<? super R> downstream, Function<? super Object[], ? extends R> zipper, int n) {
            super(downstream);
            this.zipper = zipper;
            this.values = new Object[n];
            @SuppressWarnings("unchecked")
            ZipSubscriber<T, R>[] a = new ZipSubscriber[n];
            for (int i = 0; i < n; i++) {
                a[i] = new ZipSubscriber<T, R>(i, this);
            }
            this.subscribers = a;
            this.wip = new AtomicInteger(n);
        }

        void subscribe(Solo<? extends T>[] sources, int n) {
            AtomicInteger w = wip;
            ZipSubscriber<T, R>[] a = subscribers;
            for (int i = 0; i < n; i++) {
                if (w.get() > 0) {
                    Solo<? extends T> solo = sources[i];
                    if (solo == null) {
                        onError(i, new NullPointerException("One of the source Solo is null"));
                        break;
                    } else {
                        solo.subscribe(a[i]);
                    }
                }
            }
        }

        void onSuccess(int index, T value) {
            values[index] = value;
            if (wip.decrementAndGet() == 0) {
                R v;
                try {
                    v = ObjectHelper.requireNonNull(zipper.apply(values), "The zipper returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    Arrays.fill(values, null);
                    downstream.onError(ex);
                    return;
                }

                Arrays.fill(values, null);
                complete(v);
            }
        }

        void onError(int index, Throwable t) {
            if (wip.getAndSet(0) > 0) {
                Arrays.fill(values, null);
                for (ZipSubscriber<T, R> inner : subscribers) {
                    inner.cancel();
                }
                downstream.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            if (wip.getAndSet(0) > 0) {
                Arrays.fill(values, null);
                for (ZipSubscriber<T, R> inner : subscribers) {
                    inner.cancel();
                }
            }
        }

        static final class ZipSubscriber<T, R> extends AtomicReference<Subscription>
        implements Subscriber<T> {
            private static final long serialVersionUID = -4715238780191248967L;

            final int index;

            final ZipCoordinator<T, R> parent;

            ZipSubscriber(int index, ZipCoordinator<T, R> parent) {
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
                parent.onSuccess(index, t);
            }

            @Override
            public void onError(Throwable t) {
                parent.onError(index, t);
            }

            @Override
            public void onComplete() {
                // ignored here
            }

            void cancel() {
                SubscriptionHelper.cancel(this);
            }
        }
    }
}
