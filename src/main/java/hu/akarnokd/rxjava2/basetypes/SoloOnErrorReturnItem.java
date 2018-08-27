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

package hu.akarnokd.rxjava2.basetypes;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.*;

/**
 * If the upstream signals an error, signal an item instead.
 *
 * @param <T> the value type
 */
final class SoloOnErrorReturnItem<T> extends Solo<T> {

    final Solo<T> source;

    final T item;

    SoloOnErrorReturnItem(Solo<T> source, T item) {
        this.source = source;
        this.item = item;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new OnErrorReturnItemSubscriber<T>(s, item));
    }

    static final class OnErrorReturnItemSubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -7631998337002592538L;

        final T item;

        Subscription upstream;

        OnErrorReturnItemSubscriber(Subscriber<? super T> actual, T item) {
            super(actual);
            this.item = item;
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
        public void onNext(T t) {
            value = t;
        }

        @Override
        public void onError(Throwable t) {
            complete(item);
        }

        @Override
        public void onComplete() {
            complete(value);
        }

        @Override
        public void cancel() {
            super.cancel();
            upstream.cancel();
        }
    }
}
