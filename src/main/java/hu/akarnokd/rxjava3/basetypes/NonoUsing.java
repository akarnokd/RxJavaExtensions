/*
 * Copyright 2016-present David Karnok
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

import java.util.Objects;

import org.reactivestreams.*;

import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Use a resource while the actual Nono is running.
 * @param <R> the resource type
 */
final class NonoUsing<R> extends Nono {

    final Supplier<R> resourceSupplier;

    final Function<? super R, ? extends Nono> sourceSupplier;

    final Consumer<? super R> disposer;

    final boolean eager;

    NonoUsing(Supplier<R> resourceSupplier,
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
            resource = resourceSupplier.get();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        Nono np;

        try {
            np = Objects.requireNonNull(sourceSupplier.apply(resource), "The sourceSupplier returned a null Nono");
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

        np.subscribe(new UsingSubscriber<>(s, resource, disposer, eager));
    }

    static final class UsingSubscriber<R> extends BasicNonoIntQueueSubscription
    implements Subscriber<Void> {

        final Subscriber<? super Void> downstream;

        final Consumer<? super R> disposer;

        final boolean eager;

        Subscription upstream;

        R resource;

        private static final long serialVersionUID = 5500674592438910341L;

        UsingSubscriber(Subscriber<? super Void> downstream, R resource, Consumer<? super R> disposer, boolean eager) {
            this.downstream = downstream;
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
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);
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

            downstream.onError(t);

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
                        downstream.onError(ex);
                        return;
                    }
                }
            }

            downstream.onComplete();

            if (!eager) {
                if (compareAndSet(0, 1)) {
                    disposeFinally();
                }
            }
        }
    }
}
