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

import hu.akarnokd.rxjava3.debug.CompletableOnAssembly.OnAssemblyCompletableObserver;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Supplier;

/**
 * Wraps a CompletableSource and inject the assembly info.
 */
final class CompletableOnAssemblySupplier extends Completable implements Supplier<Object> {

    final CompletableSource source;

    final RxJavaAssemblyException assembled;

    CompletableOnAssemblySupplier(CompletableSource source) {
        this.source = source;
        this.assembled = new RxJavaAssemblyException();
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        source.subscribe(new OnAssemblyCompletableObserver(observer, assembled));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object get() throws Throwable {
        try {
            return ((Supplier<Object>)source).get();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            throw assembled.appendLast(ex);
        }
    }
}
