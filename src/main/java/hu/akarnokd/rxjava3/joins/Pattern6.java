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

package hu.akarnokd.rxjava3.joins;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function6;

/**
 * Represents a join pattern over observable sequences.
 * @param <T1> the first value type
 * @param <T2> the second value type
 * @param <T3> the third value type
 * @param <T4> the fourth value type
 * @param <T5> the fifth value type
 * @param <T6> the sixth value type
 */
public final class Pattern6<T1, T2, T3, T4, T5, T6> {
    private final Observable<T1> o1;
    private final Observable<T2> o2;
    private final Observable<T3> o3;
    private final Observable<T4> o4;
    private final Observable<T5> o5;
    private final Observable<T6> o6;

    public Pattern6(
            Observable<T1> o1,
            Observable<T2> o2,
            Observable<T3> o3,
            Observable<T4> o4,
            Observable<T5> o5,
            Observable<T6> o6
            ) {
        this.o1 = o1;
        this.o2 = o2;
        this.o3 = o3;
        this.o4 = o4;
        this.o5 = o5;
        this.o6 = o6;
    }

    Observable<T1> o1() {
        return o1;
    }

    Observable<T2> o2() {
        return o2;
    }

    Observable<T3> o3() {
        return o3;
    }

    Observable<T4> o4() {
        return o4;
    }

    Observable<T5> o5() {
        return o5;
    }

    Observable<T6> o6() {
        return o6;
    }

    /**
     * Creates a pattern that matches when all six observable sequences have an available element.
     *
     * @param <T7> the value type of the extra Observable
     * @param other
     *            Observable sequence to match with the five previous sequences.
     * @return Pattern object that matches when all observable sequences have an available element.
     */
    public <T7> Pattern7<T1, T2, T3, T4, T5, T6, T7> and(Observable<T7> other) {
        if (other == null) {
            throw new NullPointerException();
        }
        return new Pattern7<>(o1, o2, o3, o4, o5, o6, other);
    }
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
    public <R> Plan<R> then(Function6<T1, T2, T3, T4, T5, T6, R> selector) {
        if (selector == null) {
            throw new NullPointerException();
        }
        return new Plan6<>(this, selector);
    }
}
