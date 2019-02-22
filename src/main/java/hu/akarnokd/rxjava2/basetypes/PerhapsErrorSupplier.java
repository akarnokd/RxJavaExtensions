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

package hu.akarnokd.rxjava2.basetypes;

import java.util.concurrent.Callable;

import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava2.util.SneakyThrows;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.EmptySubscription;

/**
 * Signals a Throwable returned by a callable.
 *
 * @param <T> the value type
 */
final class PerhapsErrorSupplier<T> extends Perhaps<T> implements Callable<T> {

    final Callable<? extends Throwable> errorSupplier;

    PerhapsErrorSupplier(Callable<? extends Throwable> errorSupplier) {
        this.errorSupplier = errorSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        Throwable ex;

        try {
            ex = ObjectHelper.requireNonNull(errorSupplier.call(), "The errorSupplier returned a null Throwable");
        } catch (Throwable exc) {
            Exceptions.throwIfFatal(exc);
            ex = exc;
        }

        EmptySubscription.error(ex, s);
    }

    @Override
    public T call() throws Exception {
        throw SneakyThrows.<RuntimeException>justThrow(ObjectHelper.requireNonNull(errorSupplier.call(), "The errorSupplier returned a null Throwable"));
    }
}
