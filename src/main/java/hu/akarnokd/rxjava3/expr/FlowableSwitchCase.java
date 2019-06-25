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

package hu.akarnokd.rxjava3.expr;

import java.util.Map;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Supplier;
import io.reactivex.internal.subscriptions.EmptySubscription;

/**
 * For each Subscriber, it calls a keySelector for a key to lookup in the given Map for an Publisher
 * to subscribe to; otherwise subscribe the Subscriber to the default Publisher.
 *
 * @param <T> the output value type
 * @param <K> the key type
 */
final class FlowableSwitchCase<T, K> extends Flowable<T> {

    final Supplier<? extends K> caseSelector;

    final Map<? super K, ? extends Publisher<? extends T>> mapOfCases;

    final Publisher<? extends T> defaultCase;

    FlowableSwitchCase(Supplier<? extends K> caseSelector,
            Map<? super K, ? extends Publisher<? extends T>> mapOfCases,
                    Publisher<? extends T> defaultCase) {
        this.caseSelector = caseSelector;
        this.mapOfCases = mapOfCases;
        this.defaultCase = defaultCase;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        K key;
        Publisher<? extends T> source;

        try {
            key = caseSelector.get();

            source = mapOfCases.get(key);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        if (source == null) {
            source = defaultCase;
        }

        source.subscribe(s);
    }
}
