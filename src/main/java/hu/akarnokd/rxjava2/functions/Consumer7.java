/*
 * Copyright 2016 David Karnok
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

package hu.akarnokd.rxjava2.functions;

/**
 * Functional interface for a callback that consumes multipe values at the same
 * and may throw a checked exception.
 *
 * @param <T1> the first value type
 * @param <T2> the second value type
 * @param <T3> the third value type
 * @param <T4> the fourth value type
 * @param <T5> the fifth value type
 * @param <T6> the sixth value type
 * @param <T7> the seventh value type
 */
public interface Consumer7<T1, T2, T3, T4, T5, T6, T7> {

    /**
     * Consum the input parameters.
     * @param t1 the first parameter
     * @param t2 the second parameter
     * @param t3 the third parameter
     * @param t4 the fourth parameter
     * @param t5 the fifth parameter
     * @param t6 the sixth parameter
     * @param t7 the seventh parameter
     * @throws Exception on error
     */
    void accept(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) throws Exception;
}
