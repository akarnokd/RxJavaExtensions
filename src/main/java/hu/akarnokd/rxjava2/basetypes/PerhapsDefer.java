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

import org.reactivestreams.Subscriber;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.EmptySubscription;

/**
 * Defers the creation of the actual Perhaps until subscription time.
 *
 * @param <T> the value type
 */
final class PerhapsDefer<T> extends Perhaps<T> {

    final Callable<? extends Perhaps<? extends T>> supplier;

    PerhapsDefer(Callable<? extends Perhaps<? extends T>> supplier) {
        this.supplier = supplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        Perhaps<? extends T> sp;

        try {
            sp = ObjectHelper.requireNonNull(supplier.call(), "The supplier returned a null Solo");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        sp.subscribe(s);
    }

}
