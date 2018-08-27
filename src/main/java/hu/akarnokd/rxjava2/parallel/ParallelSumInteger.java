/*
 * Copyright 2016-2018 David Karnok
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

import org.reactivestreams.*;

import io.reactivex.FlowableSubscriber;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.parallel.ParallelTransformer;

/**
 * Sums numbers as integers on each parallel rail, or empty if the rail doesn't produce any data.
 * @param <T> the input element type extending Number
 * @since 0.16.3
 */
final class ParallelSumInteger<T extends Number> extends ParallelFlowable<Integer> implements ParallelTransformer<T, Integer> {

    final ParallelFlowable<? extends Number> source;

    ParallelSumInteger(ParallelFlowable<? extends Number> source) {
        this.source = source;
    }

    @Override
    public ParallelFlowable<Integer> apply(ParallelFlowable<T> t) {
        return new ParallelSumInteger<T>(t);
    }

    @Override
    public void subscribe(Subscriber<? super Integer>[] subscribers) {
        if (!validate(subscribers)) {
            return;
        }

        int n = subscribers.length;
        @SuppressWarnings("unchecked")
        Subscriber<Number>[] parents = new Subscriber[n];
        for (int i = 0; i < n; i++) {
            parents[i] = new SumIntSubscriber(subscribers[i]);
        }

        source.subscribe(parents);
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }

    static final class SumIntSubscriber extends DeferredScalarSubscription<Integer>
    implements FlowableSubscriber<Number> {
        private static final long serialVersionUID = -1502296701568087162L;

        int sum;
        boolean hasValue;

        Subscription upstream;

        SumIntSubscriber(Subscriber<? super Integer> actual) {
            super(actual);
        }

        @Override
        public void onNext(Number t) {
            if (!hasValue) {
                hasValue = true;
            }
            sum += t.intValue();
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (hasValue) {
                complete(sum);
            } else {
                downstream.onComplete();
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(upstream, s)) {
                upstream = s;

                downstream.onSubscribe(this);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            upstream.cancel();
        }
    }

}
