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

package hu.akarnokd.rxjava2.expr;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.internal.subscriptions.EmptySubscription;

/**
 * When an Subscriber subscribes, the condition is evaluated and the appropriate
 * Publisher is subscribed to.
 *
 * @param <T> the common value type of the Observables
 */
final class FlowableIfThen<T> extends Flowable<T> {

    final BooleanSupplier condition;

    final Publisher<? extends T> then;

    final Publisher<? extends T> orElse;

    FlowableIfThen(BooleanSupplier condition, Publisher<? extends T> then,
            Publisher<? extends T> orElse) {
        this.condition = condition;
        this.then = then;
        this.orElse = orElse;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        boolean b;

        try {
            b = condition.getAsBoolean();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        if (b) {
            then.subscribe(s);
        } else {
            orElse.subscribe(s);
        }
    }
}
