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

package hu.akarnokd.rxjava3.debug;

import hu.akarnokd.rxjava3.debug.MaybeOnAssembly.OnAssemblyMaybeObserver;
import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Supplier;

/**
 * Wraps a MaybeSource and inject the assembly info.
 *
 * @param <T> the value type
 */
final class MaybeOnAssemblySupplier<T> extends Maybe<T> implements Supplier<T> {

    final MaybeSource<T> source;

    final RxJavaAssemblyException assembled;

    MaybeOnAssemblySupplier(MaybeSource<T> source) {
        this.source = source;
        this.assembled = new RxJavaAssemblyException();
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new OnAssemblyMaybeObserver<T>(observer, assembled));
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() throws Throwable {
        try {
            return ((Supplier<T>)source).get();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            throw (Exception)assembled.appendLast(ex);
        }
    }
}
