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

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.internal.functions.ObjectHelper;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;

/**
 * Signals a Throwable returned by a Supplier.
 *
 * @param <T> the value type
 */
final class PerhapsErrorSupplier<T> extends Perhaps<T> implements Supplier<T> {

    final Supplier<? extends Throwable> errorSupplier;

    PerhapsErrorSupplier(Supplier<? extends Throwable> errorSupplier) {
        this.errorSupplier = errorSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        Throwable ex;

        try {
            ex = ObjectHelper.requireNonNull(errorSupplier.get(), "The errorSupplier returned a null Throwable");
        } catch (Throwable exc) {
            Exceptions.throwIfFatal(exc);
            ex = exc;
        }

        EmptySubscription.error(ex, s);
    }

    @Override
    public T get() throws Throwable {
        throw ObjectHelper.requireNonNull(errorSupplier.get(), "The errorSupplier returned a null Throwable");
    }
}
