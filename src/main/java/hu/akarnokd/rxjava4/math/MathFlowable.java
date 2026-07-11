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

package hu.akarnokd.rxjava4.math;

import java.util.Comparator;
import java.util.concurrent.Flow.Publisher;

import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;

/**
 * Utility methods to work with numerical Flowable sources: sum, min, max and average.
 */
public final class MathFlowable {
    /** Utility class. */
    private MathFlowable() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Returns a {@code Flowable} that will emit the sum of integers of the upstream.
     * @param source the source sequence
     * @return the new {@code Flowable} instance
     */
    public static Flowable<Integer> sumInt(Publisher<Integer> source) {
        return RxJavaPlugins.onAssembly(new FlowableSumInt(source));
    }

    /**
     * Returns a {@code Flowable} that will emit the sum of longs of the upstream.
     * @param source the source sequence
     * @return the new {@code Flowable} instance
     */
    public static Flowable<Long> sumLong(Publisher<Long> source) {
        return RxJavaPlugins.onAssembly(new FlowableSumLong(source));
    }

    /**
     * Returns a {@code Flowable} that will emit the sum of floats of the upstream.
     * @param source the source sequence
     * @return the new {@code Flowable} instance
     */
    public static Flowable<Float> sumFloat(Publisher<Float> source) {
        return RxJavaPlugins.onAssembly(new FlowableSumFloat(source));
    }

    /**
     * Returns a {@code Flowable} that will emit the sum of doubles of the upstream.
     * @param source the source sequence
     * @return the new {@code Flowable} instance
     */
    public static Flowable<Double> sumDouble(Publisher<Double> source) {
        return RxJavaPlugins.onAssembly(new FlowableSumDouble(source));
    }

    /**
     * Returns a {@code Flowable} that will find the largest comparable value in the upstream.
     * @param <T> the element type
     * @param source the source sequence
     * @return the new {@code Flowable} instance
     */
    public static <T extends Comparable<? super T>> Flowable<T> max(Publisher<T> source) {
        return max(source, Comparator.naturalOrder());
    }

    /**
     * Returns a {@code Flowable} that will find the largest value via a comparator in the upstream.
     * @param <T> the element type
     * @param source the source sequence
     * @param comparator the comparator to compare items with
     * @return the new {@code Flowable} instance
     */
    public static <T> Flowable<T> max(Publisher<T> source, Comparator<? super T> comparator) {
        return RxJavaPlugins.onAssembly(new FlowableMinMax<>(source, comparator, -1));
    }

    /**
     * Returns a {@code Flowable} that will find the smallest comparable value in the upstream.
     * @param <T> the element type
     * @param source the source sequence
     * @return the new {@code Flowable} instance
     */
    public static <T extends Comparable<? super T>> Flowable<T> min(Publisher<T> source) {
        return min(source, Comparator.naturalOrder());
    }

    /**
     * Returns a {@code Flowable} that will find the smallest value via a comparator in the upstream.
     * @param <T> the element type
     * @param source the source sequence
     * @param comparator the comparator to compare items with
     * @return the new {@code Flowable} instance
     */
    public static <T> Flowable<T> min(Publisher<T> source, Comparator<? super T> comparator) {
        return RxJavaPlugins.onAssembly(new FlowableMinMax<>(source, comparator, 1));
    }

    /**
     * Returns a {@code Flowable} that calculates the average of the numeric values in the upstream
     * as a float.
     * @param source the source sequence
     * @return the new {@code Flowable} instance
     */
    @SuppressWarnings("unchecked")
    public static Flowable<Float> averageFloat(Publisher<? extends Number> source) {
        return RxJavaPlugins.onAssembly(new FlowableAverageFloat((Publisher<Number>)source));
    }

    /**
     * Returns a {@code Flowable} that calculates the average of the numeric values in the upstream
     * as a double.
     * @param source the source sequence
     * @return the new {@code Flowable} instance
     */
    @SuppressWarnings("unchecked")
    public static Flowable<Double> averageDouble(Publisher<? extends Number> source) {
        return RxJavaPlugins.onAssembly(new FlowableAverageDouble((Publisher<Number>)source));
    }

}
