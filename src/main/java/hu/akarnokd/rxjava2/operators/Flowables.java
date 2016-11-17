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

package hu.akarnokd.rxjava2.operators;

import java.util.Comparator;

import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Utility class to create Flowable sources.
 */
public final class Flowables {
    /** Utility class. */
    private Flowables() {
        throw new IllegalStateException("No instances!");
    }

    public static <T> Flowable<T> orderedMerge(Comparator<? super T> comparator, Publisher<T>... sources) {
        return orderedMerge(sources, comparator, false, Flowable.bufferSize());
    }

    public static <T> Flowable<T> orderedMerge(Comparator<? super T> comparator, boolean delayErrors, Publisher<T>... sources) {
        return orderedMerge(sources, comparator, delayErrors, Flowable.bufferSize());
    }

    public static <T> Flowable<T> orderedMerge(Comparator<? super T> comparator, boolean delayErrors, int prefetch, Publisher<T>... sources) {
        return orderedMerge(sources, comparator, delayErrors, prefetch);
    }

    public static <T> Flowable<T> orderedMerge(Publisher<T>[] sources, Comparator<? super T> comparator) {
        return orderedMerge(sources, comparator, false, Flowable.bufferSize());
    }

    public static <T> Flowable<T> orderedMerge(Publisher<T>[] sources, Comparator<? super T> comparator, boolean delayErrors) {
        return orderedMerge(sources, comparator, delayErrors, Flowable.bufferSize());
    }

    public static <T> Flowable<T> orderedMerge(Publisher<T>[] sources, Comparator<? super T> comparator, boolean delayErrors, int prefetch) {
        ObjectHelper.requireNonNull(comparator, "comparator is null");
        ObjectHelper.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new FlowableOrderedMerge<T>(sources, null, comparator, delayErrors, prefetch));
    }
    public static <T> Flowable<T> orderedMerge(Iterable<? extends Publisher<T>> sources, Comparator<? super T> comparator) {
        return orderedMerge(sources, comparator, false, Flowable.bufferSize());
    }

    public static <T> Flowable<T> orderedMerge(Iterable<? extends Publisher<T>> sources, Comparator<? super T> comparator, boolean delayErrors) {
        return orderedMerge(sources, comparator, delayErrors, Flowable.bufferSize());
    }

    public static <T> Flowable<T> orderedMerge(Iterable<? extends Publisher<T>> sources, Comparator<? super T> comparator, boolean delayErrors, int prefetch) {
        ObjectHelper.requireNonNull(comparator, "comparator is null");
        ObjectHelper.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return RxJavaPlugins.onAssembly(new FlowableOrderedMerge<T>(null, sources, comparator, delayErrors, prefetch));
    }
}
