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

package hu.akarnokd.rxjava4.basetypes;

import java.util.Objects;
import java.util.concurrent.Flow.Subscriber;

import hu.akarnokd.rxjava4.internal.rxcopy.EmptySubscription;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.functions.Function;

/**
 * Transform the downstream Subscriber into an upstream Subscriber.
 */
final class NonoLift extends Nono {

    final Nono source;

    final Function<Subscriber<? super Void>, Subscriber<? super Void>> lifter;

    NonoLift(Nono source, Function<Subscriber<? super Void>, Subscriber<? super Void>> lifter) {
        this.source = source;
        this.lifter = lifter;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        Subscriber<? super Void> z;

        try {
            z = Objects.requireNonNull(lifter.apply(s), "The lifter returned a null Subscriber");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        source.subscribe(z);
    }

}
