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

import org.reactivestreams.*;

import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Use a resource while the actual Nono is running.
 */
final class NonoUsing<R> extends Nono {

    final Callable<R> resourceSupplier;

    final Function<? super R, ? extends Nono> sourceSupplier;

    final Consumer<? super R> disposer;

    final boolean eager;

    NonoUsing(Callable<R> resourceSupplier,
            Function<? super R, ? extends Nono> sourceSupplier,
            Consumer<? super R> disposer, boolean eager) {
        this.resourceSupplier = resourceSupplier;
        this.sourceSupplier = sourceSupplier;
        this.disposer = disposer;
        this.eager = eager;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        R resource;

        try {
            resource = resourceSupplier.call();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        Nono np;

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

        np.subscribe(new UsingSubscriber<R>(s, resource, disposer, eager));
    }

    static final class UsingSubscriber<R> extends BasicNonoIntQueueSubscription
    implements Subscriber<Void> {

        final Subscriber<? super Void> actual;

        final Consumer<? super R> disposer;

        final boolean eager;

        Subscription s;

        R resource;

        private static final long serialVersionUID = 5500674592438910341L;

        UsingSubscriber(Subscriber<? super Void> actual, R resource, Consumer<? super R> disposer, boolean eager) {
            this.actual = actual;
            this.resource = resource;
            this.disposer = disposer;
            this.eager = eager;
        }

        @Override
        public void cancel() {
            if (compareAndSet(0, 1)) {
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
            }
        }

        @Override
        public void onNext(Void t) {
            // never called
        }

        @Override
        public void onError(Throwable t) {
            if (eager) {
                if (compareAndSet(0, 1)) {
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
                if (compareAndSet(0, 1)) {
                    disposeFinally();
                }
            }
        }

        @Override
        public void onComplete() {
            if (eager) {
                if (compareAndSet(0, 1)) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        actual.onError(ex);
                        return;
                    }
                }
            }

            actual.onComplete();

            if (!eager) {
                if (compareAndSet(0, 1)) {
                    disposeFinally();
                }
            }
        }
    }
}
