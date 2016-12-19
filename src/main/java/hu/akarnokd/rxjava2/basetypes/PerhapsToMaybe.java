/*
 * Copyright 2016 David Karnok
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
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Convert the Perhaps into an Maybe.
 *
 * @param <T> the value type
 */
final class PerhapsToMaybe<T> extends Maybe<T> {

    final Perhaps<T> source;

    PerhapsToMaybe(Perhaps<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> s) {
        source.subscribe(new ToFlowableSubscriber<T>(s));
    }

    static final class ToFlowableSubscriber<T>
    implements Subscriber<T>, Disposable {

        final MaybeObserver<? super T> actual;

        Subscription s;

        T value;

        ToFlowableSubscriber(MaybeObserver<? super T> actual) {
            this.actual = actual;
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
                actual.onSuccess(v);
            } else {
                actual.onComplete();
            }
        }

        @Override
        public boolean isDisposed() {
            return s == SubscriptionHelper.CANCELLED;
        }

        @Override
        public void dispose() {
            s.cancel();
            s = SubscriptionHelper.CANCELLED;
        }
    }
}
