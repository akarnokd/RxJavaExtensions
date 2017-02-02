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

package hu.akarnokd.rxjava2.joins;

import java.util.Map;

import io.reactivex.*;
import io.reactivex.functions.Consumer;

/**
 * Represents an execution plan for join patterns.
 * @param <R> the result type
 */
public abstract class Plan<R> {
    abstract ActivePlan0 activate(Map<Object, JoinObserver> externalSubscriptions,
            Observer<R> observer, Consumer<ActivePlan0> deactivate);

    @SuppressWarnings("unchecked")
    static final <T> JoinObserver1<T> createObserver(
            Map<Object, JoinObserver> externalSubscriptions,
            Observable<T> observable,
            Consumer<Throwable> onError
            ) {
        JoinObserver1<T> observer;
        JoinObserver nonGeneric = externalSubscriptions.get(observable);
        if (nonGeneric == null) {
            observer = new JoinObserver1<T>(observable, onError);
            externalSubscriptions.put(observable, observer);
        } else {
            observer = (JoinObserver1<T>) nonGeneric;
        }
        return observer;
    }

    /**
     * Extracts a method reference to the Observer's {@link Observer#onError(java.lang.Throwable) onError}
     * method in the form of an {@link Consumer}.
     * <p>Java 8: observer::onError</p>
     *
     * @param <T> the value type
     * @param observer
     *            the {@link Observer} to use
     * @return an action which calls observer's {@code onError} method.
     */
    protected static <T> Consumer<Throwable> onErrorFrom(final Observer<T> observer) {
        return new Consumer<Throwable>() {
            @Override
            public void accept(Throwable t1) {
                observer.onError(t1);
            }
        };
    }

}
