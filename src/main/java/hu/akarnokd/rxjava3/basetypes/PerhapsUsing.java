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

import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava3.basetypes.SoloUsing.UsingSubscriber;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

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
}
