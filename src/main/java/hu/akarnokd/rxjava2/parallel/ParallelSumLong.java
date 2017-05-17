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

import org.reactivestreams.*;

import io.reactivex.FlowableSubscriber;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.parallel.ParallelTransformer;

/**
 * Sums numbers as longs on each parallel rail, or empty if the rail doesn't produce any data.
 * @since 0.16.3
 */
final class ParallelSumLong<T extends Number> extends ParallelFlowable<Long> implements ParallelTransformer<T, Long> {

    final ParallelFlowable<? extends Number> source;

    ParallelSumLong(ParallelFlowable<? extends Number> source) {
        this.source = source;
    }

    @Override
    public ParallelFlowable<Long> apply(ParallelFlowable<T> t) {
        return new ParallelSumLong<T>(t);
    }

    @Override
    public void subscribe(Subscriber<? super Long>[] subscribers) {
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

    static final class SumIntSubscriber extends DeferredScalarSubscription<Long>
    implements FlowableSubscriber<Number> {
        private static final long serialVersionUID = -1502296701568087162L;

        long sum;
        boolean hasValue;

        Subscription upstream;

        SumIntSubscriber(Subscriber<? super Long> actual) {
            super(actual);
        }

        @Override
        public void onNext(Number t) {
            if (!hasValue) {
                hasValue = true;
            }
            sum += t.longValue();
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (hasValue) {
                complete(sum);
            } else {
                actual.onComplete();
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(upstream, s)) {
                upstream = s;

                actual.onSubscribe(this);

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
