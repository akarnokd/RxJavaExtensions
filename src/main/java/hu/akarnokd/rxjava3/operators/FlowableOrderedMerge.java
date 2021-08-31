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

package hu.akarnokd.rxjava3.operators;

import java.util.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;

/**
 * Merges a fixed set of sources by picking the next smallest
 * available element from any of the sources based on a comparator.
 * 
 * @param <T> the source value types
 * 
 * @since 0.8.0
 */
final class FlowableOrderedMerge<T> extends Flowable<T> {

    final Publisher<T>[] sources;

    final Iterable<? extends Publisher<T>> sourcesIterable;

    final Comparator<? super T> comparator;

    final boolean delayErrors;

    final int prefetch;

    FlowableOrderedMerge(Publisher<T>[] sources, Iterable<? extends Publisher<T>> sourcesIterable,
            Comparator<? super T> comparator,
            boolean delayErrors, int prefetch) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
        this.comparator = comparator;
        this.delayErrors = delayErrors;
        this.prefetch = prefetch;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        Publisher<T>[] array = sources;
        int n;

        if (array == null) {
            array = new Publisher[8];
            n = 0;
            try {
                for (Publisher<T> p : sourcesIterable) {
                    if (n == array.length) {
                        array = Arrays.copyOf(array, n << 1);
                    }
                    array[n++] = Objects.requireNonNull(p, "a source is null");
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptySubscription.error(ex, s);
                return;
            }
        } else {
            n = array.length;
        }

        if (n == 0) {
            EmptySubscription.complete(s);
            return;
        }

        if (n == 1) {
            array[0].subscribe(s);
            return;
        }

        BasicMergeSubscription<T> parent = new BasicMergeSubscription<>(s, comparator, n, prefetch, delayErrors);
        s.onSubscribe(parent);
        parent.subscribe(array, n);
    }
}
