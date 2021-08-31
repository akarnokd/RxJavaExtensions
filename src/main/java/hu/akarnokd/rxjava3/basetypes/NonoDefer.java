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

import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;

final class NonoDefer extends Nono {

    final Supplier<? extends Nono> supplier;

    NonoDefer(Supplier<? extends Nono> supplier) {
        this.supplier = supplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        Nono np;

        try {
            np = Objects.requireNonNull(supplier.get(), "The supplier returned a null Nono");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        np.subscribe(s);
    }

}
