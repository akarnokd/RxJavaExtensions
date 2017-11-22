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

import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava2.operators.BasicMergeSubscription;
import io.reactivex.Flowable;
import io.reactivex.parallel.ParallelFlowable;

/**
 * Merges the individual 'rails' of the source ParallelFlowable by picking
 * the next smallest available element from any 'rail' based on a comparator
 * into a single regular Publisher sequence (exposed as Flowable).
 * 
 * @param <T> the source value types
 * @author Simon Wimmesberger
 * @since 0.17.9
 */
final class ParallelOrderedMerge<T> extends Flowable<T> {

    final ParallelFlowable<T> source;

    final Comparator<? super T> comparator;

    final boolean delayErrors;

    final int prefetch;

    ParallelOrderedMerge(ParallelFlowable<T> source,
            Comparator<? super T> comparator,
            boolean delayErrors, int prefetch) {
        this.source = source;
        this.comparator = comparator;
        this.delayErrors = delayErrors;
        this.prefetch = prefetch;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        final BasicMergeSubscription<T> parent = new BasicMergeSubscription<T>(s, comparator, this.source.parallelism(), prefetch, delayErrors);
        s.onSubscribe(parent);
        parent.subscribe(this.source);
    }
}
