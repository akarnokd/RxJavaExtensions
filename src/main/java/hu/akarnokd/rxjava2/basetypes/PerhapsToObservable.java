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

package hu.akarnokd.rxjava2.basetypes;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.observers.DeferredScalarDisposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Convert the Perhaps into an Observable.
 *
 * @param <T> the value type
 */
final class PerhapsToObservable<T> extends Observable<T> {

    final Perhaps<T> source;

    PerhapsToObservable(Perhaps<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Observer<? super T> s) {
        source.subscribe(new ToFlowableSubscriber<T>(s));
    }

    static final class ToFlowableSubscriber<T> extends DeferredScalarDisposable<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -1626180231890768109L;

        Subscription s;

        ToFlowableSubscriber(Observer<? super T> actual) {
            super(actual);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            value = t;
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            T v = value;
            if (v != null) {
                complete(v);
            } else {
                actual.onComplete();
            }
        }
    }
}
