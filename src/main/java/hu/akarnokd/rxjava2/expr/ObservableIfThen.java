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

package hu.akarnokd.rxjava2.expr;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.internal.disposables.EmptyDisposable;

/**
 * When an Observer subscribes, the condition is evaluated and the appropriate
 * ObservableSource is subscribed to.
 *
 * @param <T> the common value type of the Observables
 */
final class ObservableIfThen<T> extends Observable<T> {

    final BooleanSupplier condition;

    final ObservableSource<? extends T> then;

    final ObservableSource<? extends T> orElse;

    ObservableIfThen(BooleanSupplier condition, ObservableSource<? extends T> then,
            ObservableSource<? extends T> orElse) {
        this.condition = condition;
        this.then = then;
        this.orElse = orElse;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        boolean b;

        try {
            b = condition.getAsBoolean();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, observer);
            return;
        }

        if (b) {
            then.subscribe(observer);
        } else {
            orElse.subscribe(observer);
        }
    }
}
