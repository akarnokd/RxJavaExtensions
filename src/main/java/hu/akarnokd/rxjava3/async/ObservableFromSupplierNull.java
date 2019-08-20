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

package hu.akarnokd.rxjava3.async;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.internal.observers.DeferredScalarDisposable;

/**
 * Runs a supplier and treats a null result as completion.
 *
 * @param <T> the value type
 */
final class ObservableFromSupplierNull<T> extends Observable<T> implements Supplier<T> {

    final Supplier<? extends T> supplier;

    ObservableFromSupplierNull(Supplier<? extends T> supplier) {
        this.supplier = supplier;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        SupplierNullDisposable<T> deferred = new SupplierNullDisposable<T>(observer);
        observer.onSubscribe(deferred);

        if (!deferred.isDisposed()) {

            T v;

            try {
                v = supplier.get();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                if (!deferred.isDisposed()) {
                    observer.onError(ex);
                }
                return;
            }

            if (!deferred.isDisposed()) {
                if (v == null) {
                    observer.onComplete();
                } else {
                    deferred.complete(v);
                }
            }
        }
    }

    @Override
    public T get() throws Throwable {
        return supplier.get();
    }

    static final class SupplierNullDisposable<T> extends DeferredScalarDisposable<T> {

        private static final long serialVersionUID = -7088349936918117528L;

        SupplierNullDisposable(Observer<? super T> downstream) {
            super(downstream);
        }

    }
}
