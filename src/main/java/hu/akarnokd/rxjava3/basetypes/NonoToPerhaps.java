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

package hu.akarnokd.rxjava3.basetypes;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Convert a Nono into an empty Perhaps.
 *
 * @param <T> the value type
 */
final class NonoToPerhaps<T> extends Perhaps<T> {

    final Nono source;

    NonoToPerhaps(Nono source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new ToPerhapsSubcriber<T>(s));
    }

    static final class ToPerhapsSubcriber<T> extends BasicEmptyQueueSubscription
    implements Subscriber<Object> {

        final Subscriber<? super T> downstream;

        Subscription upstream;

        ToPerhapsSubcriber(Subscriber<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void cancel() {
            upstream.cancel();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(Object t) {
            // not called
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
