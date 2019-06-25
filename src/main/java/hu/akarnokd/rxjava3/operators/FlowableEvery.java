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

package hu.akarnokd.rxjava3.operators;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * Emits only every Nth item.
 *
 * @param <T> the value type
 *
 * @since 0.14.2
 */
final class FlowableEvery<T> extends Flowable<T> implements FlowableTransformer<T, T> {

    final Publisher<T> source;

    final long keep;

    FlowableEvery(Publisher<T> source, long keep) {
        this.source = source;
        this.keep = keep;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new EverySubscriber<T>(s, keep));
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableEvery<T>(upstream, keep);
    }

    static final class EverySubscriber<T> implements Subscriber<T>, Subscription {

        final Subscriber<? super T> downstream;

        final long keep;

        long index;

        Subscription upstream;

        EverySubscriber(Subscriber<? super T> downstream, long keep) {
            this.downstream = downstream;
            this.keep = keep;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            long i = index + 1;
            if (i == keep) {
                index = 0;
                downstream.onNext(t);
            } else {
                index = i;
            }
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                long u = BackpressureHelper.multiplyCap(n, keep);
                upstream.request(u);
            }
        }

        @Override
        public void cancel() {
            upstream.cancel();
        }
    }
}
