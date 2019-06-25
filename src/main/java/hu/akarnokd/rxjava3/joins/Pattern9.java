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

package hu.akarnokd.rxjava3.joins;

import java.util.*;

import io.reactivex.Observable;
import io.reactivex.functions.Function9;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Represents a join pattern over observable sequences.
 * @param <T1> the first value type
 * @param <T2> the second value type
 * @param <T3> the third value type
 * @param <T4> the fourth value type
 * @param <T5> the fifth value type
 * @param <T6> the sixth value type
 * @param <T7> the sevent value type
 * @param <T8> the eighth value type
 * @param <T9> the ninth value type
 */
public final class Pattern9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {
    private final Observable<T1> o1;
    private final Observable<T2> o2;
    private final Observable<T3> o3;
    private final Observable<T4> o4;
    private final Observable<T5> o5;
    private final Observable<T6> o6;
    private final Observable<T7> o7;
    private final Observable<T8> o8;
    private final Observable<T9> o9;

    public Pattern9(
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
        this.o1 = o1;
        this.o2 = o2;
        this.o3 = o3;
        this.o4 = o4;
        this.o5 = o5;
        this.o6 = o6;
        this.o7 = o7;
        this.o8 = o8;
        this.o9 = o9;
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

    Observable<T7> o7() {
        return o7;
    }

    Observable<T8> o8() {
        return o8;
    }

    Observable<T9> o9() {
        return o9;
    }

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
        List<Observable<? extends Object>> list = new ArrayList<Observable<? extends Object>>();
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
        ObjectHelper.requireNonNull(selector, "selector is null");
        return new Plan9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R>(this, selector);
    }
}
