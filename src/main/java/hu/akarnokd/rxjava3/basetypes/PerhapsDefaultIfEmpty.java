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

import io.reactivex.rxjava3.internal.subscriptions.*;

/**
 * Return an item if the upstream is empty.
 *
 * @param <T> the value type
 */
final class PerhapsDefaultIfEmpty<T> extends Perhaps<T> {

    final Perhaps<T> source;

    final T item;

    PerhapsDefaultIfEmpty(Perhaps<T> source, final T item) {
        this.source = source;
        this.item = item;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new OnErrorReturnItemSubscriber<>(s, item));
    }

    static final class OnErrorReturnItemSubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = 4223622176096519295L;

        final T item;

        Subscription upstream;

        OnErrorReturnItemSubscriber(Subscriber<? super T> downstream, T item) {
            super(downstream);
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
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            T v = value;
            if (v != null) {
                complete(v);
            } else {
                complete(item);
            }
        }
    }
}
