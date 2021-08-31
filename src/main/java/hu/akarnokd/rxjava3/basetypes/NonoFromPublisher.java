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

package hu.akarnokd.rxjava3.basetypes;

import org.reactivestreams.*;

import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Convert a Publisher into a Nono.
 */
final class NonoFromPublisher extends Nono {

    final Publisher<?> source;

    NonoFromPublisher(Publisher<?> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        source.subscribe(new FromPublisherSubscriber(s));
    }

    static final class FromPublisherSubscriber extends BasicNonoSubscriber {

        FromPublisherSubscriber(Subscriber<? super Void> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);

                s.request(Long.MAX_VALUE);
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
    }
}
