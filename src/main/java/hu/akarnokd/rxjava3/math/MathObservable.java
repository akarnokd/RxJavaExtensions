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

package hu.akarnokd.rxjava3.math;

import java.util.Comparator;

import hu.akarnokd.rxjava3.util.SelfComparator;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Utility methods to work with numerical Observable sources: sum, min, max and average.
 */
public final class MathObservable {
    /** Utility class. */
    private MathObservable() {
        throw new IllegalStateException("No instances!");
    }

    public static Observable<Integer> sumInt(ObservableSource<Integer> source) {
        return RxJavaPlugins.onAssembly(new ObservableSumInt(source));
    }

    public static Observable<Long> sumLong(ObservableSource<Long> source) {
        return RxJavaPlugins.onAssembly(new ObservableSumLong(source));
    }

    public static Observable<Float> sumFloat(ObservableSource<Float> source) {
        return RxJavaPlugins.onAssembly(new ObservableSumFloat(source));
    }

    public static Observable<Double> sumDouble(ObservableSource<Double> source) {
        return RxJavaPlugins.onAssembly(new ObservableSumDouble(source));
    }

    public static <T extends Comparable<? super T>> Observable<T> max(ObservableSource<T> source) {
        Comparator<T> comp = SelfComparator.instance();
        return max(source, comp);
    }

    public static <T> Observable<T> max(ObservableSource<T> source, Comparator<? super T> comparator) {
        return RxJavaPlugins.onAssembly(new ObservableMinMax<>(source, comparator, -1));
    }

    public static <T extends Comparable<? super T>> Observable<T> min(ObservableSource<T> source) {
        Comparator<T> comp = SelfComparator.instance();
        return min(source, comp);
    }

    public static <T> Observable<T> min(ObservableSource<T> source, Comparator<? super T> comparator) {
        return RxJavaPlugins.onAssembly(new ObservableMinMax<>(source, comparator, 1));
    }

    @SuppressWarnings("unchecked")
    public static Observable<Float> averageFloat(ObservableSource<? extends Number> source) {
        return RxJavaPlugins.onAssembly(new ObservableAverageFloat((ObservableSource<Number>)source));
    }

    @SuppressWarnings("unchecked")
    public static Observable<Double> averageDouble(ObservableSource<? extends Number> source) {
        return RxJavaPlugins.onAssembly(new ObservableAverageDouble((ObservableSource<Number>)source));
    }

}
