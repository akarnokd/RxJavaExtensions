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
import io.reactivex.rxjava3.functions.Function5;

/**
 * Represents a join pattern over observable sequences.
 * @param <T1> the first value type
 * @param <T2> the second value type
 * @param <T3> the third value type
 * @param <T4> the fourth value type
 * @param <T5> the fifth value type
 */
public final class Pattern5<T1, T2, T3, T4, T5> {
    private final Observable<T1> o1;
    private final Observable<T2> o2;
    private final Observable<T3> o3;
    private final Observable<T4> o4;
    private final Observable<T5> o5;

    public Pattern5(
            Observable<T1> o1,
            Observable<T2> o2,
            Observable<T3> o3,
            Observable<T4> o4,
            Observable<T5> o5
            ) {
        this.o1 = o1;
        this.o2 = o2;
        this.o3 = o3;
        this.o4 = o4;
        this.o5 = o5;
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

    /**
     * Creates a pattern that matches when all five observable sequences have an available element.
     *
     * @param <T6> the value type of the extra Observable
     * @param other
     *            Observable sequence to match with the four previous sequences.
     * @return Pattern object that matches when all observable sequences have an available element.
     */
    public <T6> Pattern6<T1, T2, T3, T4, T5, T6> and(Observable<T6> other) {
        if (other == null) {
            throw new NullPointerException();
        }
        return new Pattern6<>(o1, o2, o3, o4, o5, other);
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
    public <R> Plan<R> then(Function5<T1, T2, T3, T4, T5, R> selector) {
        if (selector == null) {
            throw new NullPointerException();
        }
        return new Plan5<>(this, selector);
    }
}
