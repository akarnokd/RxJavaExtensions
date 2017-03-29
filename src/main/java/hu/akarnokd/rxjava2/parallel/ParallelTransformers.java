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

package hu.akarnokd.rxjava2.parallel;

import io.reactivex.parallel.ParallelTransformer;

/**
 * Transformers for RxJava 2 ParallelFlowable sequences.
 * @since 0.16.3
 */
public final class ParallelTransformers {

    private ParallelTransformers() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Sums the numbers as integers on each rail.
     * @param <T> the numerical type of the input values
     * @return the new ParallelTransformer type
     * @since 0.16.3
     */
    public static <T extends Number> ParallelTransformer<T, Integer> sumInteger() {
        return new ParallelSumInteger<T>(null);
    }

    /**
     * Sums the numbers as longs on each rail.
     * @param <T> the numerical type of the input values
     * @return the new ParallelTransformer type
     * @since 0.16.3
     */
    public static <T extends Number> ParallelTransformer<T, Long> sumLong() {
        return new ParallelSumLong<T>(null);
    }

    /**
     * Sums the numbers as longs on each rail.
     * @param <T> the numerical type of the input values
     * @return the new ParallelTransformer type
     * @since 0.16.3
     */
    public static <T extends Number> ParallelTransformer<T, Double> sumDouble() {
        return new ParallelSumDouble<T>(null);
    }
}
