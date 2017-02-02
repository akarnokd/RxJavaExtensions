/*
 * Copyright 2016-2017 David Karnok
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

import org.reactivestreams.Subscriber;

import io.reactivex.internal.fuseable.ScalarCallable;
import io.reactivex.internal.subscriptions.ScalarSubscription;

/**
 * Signals exactly one value.
 *
 * @param <T> the value type
 */
final class PerhapsJust<T> extends Perhaps<T> implements ScalarCallable<T> {

    final T value;

    PerhapsJust(T value) {
        this.value = value;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        s.onSubscribe(new ScalarSubscription<T>(s, value));
    }

    @Override
    public T call() {
        return value;
    }
}
