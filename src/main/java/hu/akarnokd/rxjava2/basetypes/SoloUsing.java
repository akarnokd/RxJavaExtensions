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

package hu.akarnokd.rxjava2.basetypes;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.*;

import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Use a resource while the actual Solo is running.
 */
final class SoloUsing<T, R> extends Solo<T> {

    final Callable<R> resourceSupplier;

    final Function<? super R, ? extends Solo<T>> sourceSupplier;

    final Consumer<? super R> disposer;

    final boolean eager;

    SoloUsing(Callable<R> resourceSupplier, Function<? super R, ? extends Solo<T>> sourceSupplier,
            Consumer<? super R> disposer, boolean eager) {
        this.resourceSupplier = resourceSupplier;
        this.sourceSupplier = sourceSupplier;
        this.disposer = disposer;
        this.eager = eager;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        R resource;

        try {
            resource = resourceSupplier.call();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        Solo<T> np;

        try {
            np = ObjectHelper.requireNonNull(sourceSupplier.apply(resource), "The sourceSupplier returned a null Nono");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            if (eager) {
                try {
                    disposer.accept(resource);
                } catch (Throwable exc) {
                    Exceptions.throwIfFatal(exc);
                    ex = new CompositeException(ex, exc);
                }

                EmptySubscription.error(ex, s);
            } else {
                EmptySubscription.error(ex, s);
                try {
                    disposer.accept(resource);
                } catch (Throwable exc) {
                    Exceptions.throwIfFatal(exc);
                    RxJavaPlugins.onError(exc);
                }
            }
            return;
        }

        np.subscribe(new UsingSubscriber<T, R>(s, resource, disposer, eager));
    }

    static final class UsingSubscriber<T, R> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = 5500674592438910341L;

        final Consumer<? super R> disposer;

        final boolean eager;

        final AtomicBoolean once;

        Subscription s;

        R resource;

        UsingSubscriber(Subscriber<? super T> actual, R resource, Consumer<? super R> disposer, boolean eager) {
            super(actual);
            this.resource = resource;
            this.disposer = disposer;
            this.eager = eager;
            this.once = new AtomicBoolean();
        }

        @Override
        public void cancel() {
            if (once.compareAndSet(false, true)) {
                disposeFinally();
            }
        }

        void disposeFinally() {
            try {
                disposer.accept(resource);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            value = t;
        }

        @Override
        public void onError(Throwable t) {
            if (eager) {
                if (once.compareAndSet(false, true)) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        t = new CompositeException(t, ex);
                    }
                }
            }

            actual.onError(t);

            if (!eager) {
                if (once.compareAndSet(false, true)) {
                    disposeFinally();
                }
            }
        }

        @Override
        public void onComplete() {
            if (eager) {
                if (once.compareAndSet(false, true)) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        actual.onError(ex);
                        return;
                    }
                }
            }

            T v = value;
            if (v == null) {
                actual.onComplete();
            } else {
                complete(v);
            }

            if (!eager) {
                if (once.compareAndSet(false, true)) {
                    disposeFinally();
                }
            }
        }
    }
}
