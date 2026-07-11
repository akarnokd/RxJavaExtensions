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

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;

/**
 * Utility methods to work with numerical Observable sources: sum, min, max and average.
 */
public final class MathObservable {
    /** Utility class. */
    private MathObservable() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Returns a {@code Observable} that will emit the sum of integers of the upstream.
     * @param source the source sequence
     * @return the new {@code Observable} instance
     */
    public static Observable<Integer> sumInt(ObservableSource<Integer> source) {
        return RxJavaPlugins.onAssembly(new ObservableSumInt(source));
    }

    /**
     * Returns a {@code Observable} that will emit the sum of longs of the upstream.
     * @param source the source sequence
     * @return the new {@code Observable} instance
     */
    public static Observable<Long> sumLong(ObservableSource<Long> source) {
        return RxJavaPlugins.onAssembly(new ObservableSumLong(source));
    }

    /**
     * Returns a {@code Observable} that will emit the sum of floats of the upstream.
     * @param source the source sequence
     * @return the new {@code Observable} instance
     */
    public static Observable<Float> sumFloat(ObservableSource<Float> source) {
        return RxJavaPlugins.onAssembly(new ObservableSumFloat(source));
    }

    /**
     * Returns a {@code Observable} that will emit the sum of doubles of the upstream.
     * @param source the source sequence
     * @return the new {@code Observable} instance
     */
    public static Observable<Double> sumDouble(ObservableSource<Double> source) {
        return RxJavaPlugins.onAssembly(new ObservableSumDouble(source));
    }

    /**
     * Returns a {@code Observable} that will find the largest comparable value in the upstream.
     * @param <T> the element type
     * @param source the source sequence
     * @return the new {@code Observable} instance
     */
    public static <T extends Comparable<? super T>> Observable<T> max(ObservableSource<T> source) {
        return max(source, Comparator.naturalOrder());
    }

    /**
     * Returns a {@code Observable} that will find the largest value via a comparator in the upstream.
     * @param <T> the element type
     * @param source the source sequence
     * @param comparator the comparator to compare items with
     * @return the new {@code Observable} instance
     */
    public static <T> Observable<T> max(ObservableSource<T> source, Comparator<? super T> comparator) {
        return RxJavaPlugins.onAssembly(new ObservableMinMax<>(source, comparator, -1));
    }

    /**
     * Returns a {@code Observable} that will find the smallest comparable value in the upstream.
     * @param <T> the element type
     * @param source the source sequence
     * @return the new {@code Observable} instance
     */
    public static <T extends Comparable<? super T>> Observable<T> min(ObservableSource<T> source) {
        return min(source, Comparator.naturalOrder());
    }

    /**
     * Returns a {@code Observable} that will find the smallest value via a comparator in the upstream.
     * @param <T> the element type
     * @param source the source sequence
     * @param comparator the comparator to compare items with
     * @return the new {@code Observable} instance
     */
    public static <T> Observable<T> min(ObservableSource<T> source, Comparator<? super T> comparator) {
        return RxJavaPlugins.onAssembly(new ObservableMinMax<>(source, comparator, 1));
    }

    /**
     * Returns a {@code Observable} that calculates the average of the numeric values in the upstream
     * as a float.
     * @param source the source sequence
     * @return the new {@code Observable} instance
     */
    @SuppressWarnings("unchecked")
    public static Observable<Float> averageFloat(ObservableSource<? extends Number> source) {
        return RxJavaPlugins.onAssembly(new ObservableAverageFloat((ObservableSource<Number>)source));
    }

    /**
     * Returns a {@code Observable} that calculates the average of the numeric values in the upstream
     * as a double.
     * @param source the source sequence
     * @return the new {@code Observable} instance
     */
    @SuppressWarnings("unchecked")
    public static Observable<Double> averageDouble(ObservableSource<? extends Number> source) {
        return RxJavaPlugins.onAssembly(new ObservableAverageDouble((ObservableSource<Number>)source));
    }

}
