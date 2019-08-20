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

package hu.akarnokd.rxjava3.basetypes;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.ObjectHelper;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;

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
            z = ObjectHelper.requireNonNull(lifter.apply(s), "The lifter returned a null Subscriber");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        source.subscribe(z);
    }

}
