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

import org.reactivestreams.Publisher;

import hu.akarnokd.rxjava3.util.SelfComparator;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Utility methods to work with numerical Flowable sources: sum, min, max and average.
 */
public final class MathFlowable {
    /** Utility class. */
    private MathFlowable() {
        throw new IllegalStateException("No instances!");
    }

    public static Flowable<Integer> sumInt(Publisher<Integer> source) {
        return RxJavaPlugins.onAssembly(new FlowableSumInt(source));
    }

    public static Flowable<Long> sumLong(Publisher<Long> source) {
        return RxJavaPlugins.onAssembly(new FlowableSumLong(source));
    }

    public static Flowable<Float> sumFloat(Publisher<Float> source) {
        return RxJavaPlugins.onAssembly(new FlowableSumFloat(source));
    }

    public static Flowable<Double> sumDouble(Publisher<Double> source) {
        return RxJavaPlugins.onAssembly(new FlowableSumDouble(source));
    }

    public static <T extends Comparable<? super T>> Flowable<T> max(Publisher<T> source) {
        Comparator<T> comp = SelfComparator.instance();
        return max(source, comp);
    }

    public static <T> Flowable<T> max(Publisher<T> source, Comparator<? super T> comparator) {
        return RxJavaPlugins.onAssembly(new FlowableMinMax<>(source, comparator, -1));
    }

    public static <T extends Comparable<? super T>> Flowable<T> min(Publisher<T> source) {
        Comparator<T> comp = SelfComparator.instance();
        return min(source, comp);
    }

    public static <T> Flowable<T> min(Publisher<T> source, Comparator<? super T> comparator) {
        return RxJavaPlugins.onAssembly(new FlowableMinMax<>(source, comparator, 1));
    }

    @SuppressWarnings("unchecked")
    public static Flowable<Float> averageFloat(Publisher<? extends Number> source) {
        return RxJavaPlugins.onAssembly(new FlowableAverageFloat((Publisher<Number>)source));
    }

    @SuppressWarnings("unchecked")
    public static Flowable<Double> averageDouble(Publisher<? extends Number> source) {
        return RxJavaPlugins.onAssembly(new FlowableAverageDouble((Publisher<Number>)source));
    }

}
