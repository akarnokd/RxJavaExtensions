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

import java.util.Comparator;

import io.reactivex.Flowable;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.parallel.ParallelTransformer;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Transformers for RxJava 2 ParallelFlowable sequences.
 * @since 0.16.3
 */
public final class ParallelTransformers {

    private ParallelTransformers() {
        throw new IllegalStateException("No instances!");
    }
    
    /**
     * Merges the source ParallelFlowable rails in an ordered fashion picking the smallest of the available value from
     * them (determined by their natural order).
     * @param <T> the value type of all sources
     * @param source the source ParallelFlowable
     * @return the new Flowable instance
     */
    public static <T extends Comparable<? super T>> Flowable<T> orderedMerge(ParallelFlowable<T> source) {
        return orderedMerge(source, Functions.naturalOrder(), false, Flowable.bufferSize());
    }


    /**
     * Merges the source ParallelFlowable rails in an ordered fashion picking the smallest of the available value from
     * them (determined by their natural order) and allows delaying any error they may signal.
     * @param <T> the value type of all sources
     * @param source the source ParallelFlowable
     * @param delayErrors if true, source errors are delayed until all sources terminate in some way
     * @return the new Flowable instance
     */
    public static <T extends Comparable<? super T>> Flowable<T> orderedMerge(ParallelFlowable<T> source, boolean delayErrors) {
        return orderedMerge(source, Functions.naturalOrder(), delayErrors, Flowable.bufferSize());
    }


    /**
     * Merges the source ParallelFlowable rails in an ordered fashion picking the smallest of the available value from
     * them (determined by their natural order), allows delaying any error they may signal and sets the prefetch
     * amount when requesting from these Publishers.
     * @param <T> the value type of all sources
     * @param source the source ParallelFlowable
     * @param delayErrors if true, source errors are delayed until all sources terminate in some way
     * @param prefetch the number of items to prefetch from the sources
     * @return the new Flowable instance
     */
    public static <T extends Comparable<? super T>> Flowable<T> orderedMerge(ParallelFlowable<T> source, boolean delayErrors, int prefetch) {
        return orderedMerge(source, Functions.naturalOrder(), delayErrors, prefetch);
    }


    /**
     * Merges the source ParallelFlowable rails in an ordered fashion picking the smallest of the available value from
     * them (determined by the Comparator).
     * @param <T> the value type of all sources
     * @param source the source ParallelFlowable
     * @param comparator the comparator to use for comparing items;
     *                   it is called with the last known smallest in its first argument
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> orderedMerge(ParallelFlowable<T> source, Comparator<? super T> comparator) {
        return orderedMerge(source, comparator, false, Flowable.bufferSize());
    }


    /**
     * Merges the source ParallelFlowable rails in an ordered fashion picking the smallest of the available value from
     * them (determined by the Comparator) and allows delaying any error they may signal.
     * @param <T> the value type of all sources
     * @param source the source ParallelFlowable
     * @param comparator the comparator to use for comparing items;
     *                   it is called with the last known smallest in its first argument
     * @param delayErrors if true, source errors are delayed until all sources terminate in some way
     * @return the new Flowable instance
     * 
     */
    public static <T> Flowable<T> orderedMerge(ParallelFlowable<T> source, Comparator<? super T> comparator, boolean delayErrors) {
        return orderedMerge(source, comparator, delayErrors, Flowable.bufferSize());
    }


    /**
     * Merges the source ParallelFlowable rails in an ordered fashion picking the smallest of the available value from
     * them (determined by the Comparator), allows delaying any error they may signal and sets the prefetch
     * amount when requesting from these Publishers.
     * @param <T> the value type of all sources
     * @param source the source ParallelFlowable
     * @param comparator the comparator to use for comparing items;
     *                   it is called with the last known smallest in its first argument
     * @param delayErrors if true, source errors are delayed until all sources terminate in some way
     * @param prefetch the number of items to prefetch from the sources
     * @return the new Flowable instance
     * 
     */
    public static <T> Flowable<T> orderedMerge(ParallelFlowable<T> source, Comparator<? super T> comparator, boolean delayErrors, int prefetch) {
        ObjectHelper.requireNonNull(comparator, "comparator is null");
        ObjectHelper.requireNonNull(source, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new ParallelOrderedMerge<T>(source, comparator, delayErrors, prefetch));
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
