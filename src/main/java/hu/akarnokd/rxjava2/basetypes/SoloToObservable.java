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

package hu.akarnokd.rxjava2.basetypes;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.observers.DeferredScalarDisposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Converts a Solo into an Observable.
 *
 * @param <T> the value type
 */
final class SoloToObservable<T> extends Observable<T> {

    final Solo<T> source;

    SoloToObservable(Solo<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new ToObservableSubscriber<T>(observer));
    }

    static final class ToObservableSubscriber<T> extends DeferredScalarDisposable<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = 3162354714056564295L;

        Subscription upstream;

        ToObservableSubscriber(Observer<? super T> downstream) {
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
        public void onNext(T t) {
            value = t;
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            complete(value);
        }

        @Override
        public void dispose() {
            upstream.cancel();
            super.dispose();
        }
    }
}
