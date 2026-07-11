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

package hu.akarnokd.rxjava4.joins;

import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.functions.Function;

/**
 * Represents a join pattern over {@link Observable} sequences.
 * @param <T1> the first value type
 * @param o1 the first {@code Observable}
 */
public record Pattern1<T1>(Observable<T1> o1) {

    /**
     * Matches when all observable sequences have an available
     * element and projects the elements by invoking the selector function.
     *
     * @param <R> the result type
     * @param selector
     *            the function that will be invoked for elements in the source sequences.
     * @return the plan for the matching
     * @throws NullPointerException
     *             if selector is null
     */
    public <R> Plan<R> then(Function<? super T1, ? extends R> selector) {
        if (selector == null) {
            throw new NullPointerException();
        }
        return new Plan1<>(this, selector);
    }
}
