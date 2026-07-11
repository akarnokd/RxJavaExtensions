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

package hu.akarnokd.rxjava4.basetypes;

import java.io.Serial;
import java.util.Objects;
import java.util.concurrent.Flow.*;
import java.util.concurrent.atomic.AtomicBoolean;

import hu.akarnokd.rxjava4.internal.rxcopy.*;
import io.reactivex.rxjava4.core.FlowableSubscriber;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;

/**
 * Use a resource while the actual Perhaps is running.
 * @param <T> the element type of the supplied Perhaps
 * @param <R> the resource type
 */
final class PerhapsUsing<T, R> extends Perhaps<T> {

    final Supplier<R> resourceSupplier;

    final Function<? super R, ? extends Perhaps<? extends T>> sourceSupplier;

    final Consumer<? super R> disposer;

    final boolean eager;

    PerhapsUsing(Supplier<R> resourceSupplier,
            Function<? super R, ? extends Perhaps<? extends T>> sourceSupplier,
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
            resource = resourceSupplier.get();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        Perhaps<? extends T> np;

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

        np.subscribe(new UsingSubscriber<T, R>(s, resource, disposer, eager));
    }

    static final class UsingSubscriber<T, D> extends AtomicBoolean implements FlowableSubscriber<T>, Subscription {

        @Serial
        private static final long serialVersionUID = 5904473792286235046L;

        final Subscriber<? super T> downstream;
        final D resource;
        final Consumer<? super D> disposer;
        final boolean eager;

        Subscription upstream;

        UsingSubscriber(Subscriber<? super T> downstream, D resource, Consumer<? super D> disposer, boolean eager) {
            this.downstream = downstream;
            this.resource = resource;
            this.disposer = disposer;
            this.eager = eager;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (eager) {
                Throwable innerError = null;
                if (compareAndSet(false, true)) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable e) {
                        Exceptions.throwIfFatal(e);
                        innerError = e;
                    }
                }

                if (innerError != null) {
                    downstream.onError(new CompositeException(t, innerError));
                } else {
                    downstream.onError(t);
                }
            } else {
                downstream.onError(t);
                disposeResource();
            }
        }

        @Override
        public void onComplete() {
            if (eager) {
                if (compareAndSet(false, true)) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable e) {
                        Exceptions.throwIfFatal(e);
                        downstream.onError(e);
                        return;
                    }
                }

                downstream.onComplete();
            } else {
                downstream.onComplete();
                disposeResource();
            }
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            if (eager) {
                disposeResource();
                upstream.cancel();
                upstream = SubscriptionHelper.CANCELLED;
            } else {
                upstream.cancel();
                upstream = SubscriptionHelper.CANCELLED;
                disposeResource();
            }
        }

        void disposeResource() {
            if (compareAndSet(false, true)) {
                try {
                    disposer.accept(resource);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    // can't call actual.onError unless it is serialized, which is expensive
                    RxJavaPlugins.onError(e);
                }
            }
        }
    }
}
