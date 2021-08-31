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

package hu.akarnokd.rxjava3.expr;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.BooleanSupplier;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;

/**
 * When an Observer subscribes, the condition is evaluated and the appropriate
 * CompletableSource is subscribed to.
 */
final class CompletableIfThen extends Completable {

    final BooleanSupplier condition;

    final CompletableSource then;

    final CompletableSource orElse;

    CompletableIfThen(BooleanSupplier condition, CompletableSource then,
                      CompletableSource orElse) {
        this.condition = condition;
        this.then = then;
        this.orElse = orElse;
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
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
