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

import java.util.*;

import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.functions.Function9;

/**
 * Represents a join pattern over observable sequences.
 * @param <T1> the first value type
 * @param <T2> the second value type
 * @param <T3> the third value type
 * @param <T4> the fourth value type
 * @param <T5> the fifth value type
 * @param <T6> the sixth value type
 * @param <T7> the seventh value type
 * @param <T8> the eighth value type
 * @param <T9> the ninth value type
 * @param o1 the first source
 * @param o2 the second source
 * @param o3 the third source
 * @param o4 the fourth source
 * @param o5 the fifth source
 * @param o6 the sixth source
 * @param o7 the seventh source
 * @param o8 the eight source
 * @param o9 the ninth source
 */
public record Pattern9<T1, T2, T3, T4, T5, T6, T7, T8, T9>(
        Observable<T1> o1,
        Observable<T2> o2,
        Observable<T3> o3,
        Observable<T4> o4,
        Observable<T5> o5,
        Observable<T6> o6,
        Observable<T7> o7,
        Observable<T8> o8,
        Observable<T9> o9
        ) {

    /**
     * Creates a pattern that matches when all nine observable sequences have an available element.
     *
     * @param other
     *            Observable sequence to match with the eight previous sequences.
     * @return Pattern object that matches when all observable sequences have an available element.
     */
    public PatternN and(Observable<? extends Object> other) {
        if (other == null) {
            throw new NullPointerException();
        }
        List<Observable<? extends Object>> list = new ArrayList<>();
        list.add(o1);
        list.add(o2);
        list.add(o3);
        list.add(o4);
        list.add(o5);
        list.add(o6);
        list.add(o7);
        list.add(o8);
        list.add(o9);
        list.add(other);
        return new PatternN(list);
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
    public <R> Plan<R> then(Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> selector) {
        Objects.requireNonNull(selector, "selector is null");
        return new Plan9<>(this, selector);
    }
}
