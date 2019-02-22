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

import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.disposables.EmptyDisposable;

/**
 * For each Observer, it calls a keySelector for a key to lookup in the given Map for an ObservableSource
 * to subscribe to; otherwise subscribe the Observer to the default ObservableSource.
 *
 * @param <T> the output value type
 * @param <K> the key type
 */
final class ObservableSwitchCase<T, K> extends Observable<T> {

    final Callable<? extends K> caseSelector;

    final Map<? super K, ? extends ObservableSource<? extends T>> mapOfCases;

    final ObservableSource<? extends T> defaultCase;

    ObservableSwitchCase(Callable<? extends K> caseSelector,
            Map<? super K, ? extends ObservableSource<? extends T>> mapOfCases,
            ObservableSource<? extends T> defaultCase) {
        this.caseSelector = caseSelector;
        this.mapOfCases = mapOfCases;
        this.defaultCase = defaultCase;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        K key;
        ObservableSource<? extends T> source;

        try {
            key = caseSelector.call();

            source = mapOfCases.get(key);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, observer);
            return;
        }

        if (source == null) {
            source = defaultCase;
        }

        source.subscribe(observer);
    }
}
