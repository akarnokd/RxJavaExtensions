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

package hu.akarnokd.rxjava2.math;

import java.util.Comparator;

import hu.akarnokd.rxjava2.util.SelfComparator;
import io.reactivex.*;

/**
 * Utility methods to work with numerical Observable sources: sum, min, max and average.
 */
public final class MathObservable {
    /** Utility class. */
    private MathObservable() {
        throw new IllegalStateException("No instances!");
    }
    
    public static Observable<Integer> sumInt(ObservableConsumable<Integer> source) {
        return new ObservableSumInt(source);
    }

    public static Observable<Long> sumLong(ObservableConsumable<Long> source) {
        return new ObservableSumLong(source);
    }

    public static Observable<Float> sumFloat(ObservableConsumable<Float> source) {
        return new ObservableSumFloat(source);
    }

    public static Observable<Double> sumDouble(ObservableConsumable<Double> source) {
        return new ObservableSumDouble(source);
    }

    public static <T extends Comparable<? super T>> Observable<T> max(ObservableConsumable<T> source) {
        Comparator<T> comp = SelfComparator.instance();
        return max(source, comp);
    }

    public static <T> Observable<T> max(ObservableConsumable<T> source, Comparator<? super T> comparator) {
        return new ObservableMinMax<T>(source, comparator, -1);
    }

    public static <T extends Comparable<? super T>> Observable<T> min(ObservableConsumable<T> source) {
        Comparator<T> comp = SelfComparator.instance();
        return min(source, comp);
    }

    public static <T> Observable<T> min(ObservableConsumable<T> source, Comparator<? super T> comparator) {
        return new ObservableMinMax<T>(source, comparator, 1);
    }

    @SuppressWarnings("unchecked")
    public static Observable<Float> averageFloat(ObservableConsumable<? extends Number> source) {
        return new ObservableAverageFloat((ObservableConsumable<Number>)source);
    }

    @SuppressWarnings("unchecked")
    public static Observable<Double> averageDouble(ObservableConsumable<? extends Number> source) {
        return new ObservableAverageDouble((ObservableConsumable<Number>)source);
    }

}
