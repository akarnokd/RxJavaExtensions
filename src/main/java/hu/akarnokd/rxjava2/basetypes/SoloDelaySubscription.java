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

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.*;

/**
 * Delay the subscription to this Solo until the other Publisher
 * signals a value or completes.
 *
 * @param <T> the value type
 */
final class SoloDelaySubscription<T> extends Solo<T> {

    final Solo<T> source;

    final Publisher<?> other;

    SoloDelaySubscription(Solo<T> source, Publisher<?> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        DelaySubscriptionSubscriber<T> parent = new DelaySubscriptionSubscriber<T>(s, source);
        s.onSubscribe(parent);

        other.subscribe(parent.other);
    }

    static final class DelaySubscriptionSubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = 2365899367123544974L;

        final Solo<T> source;

        final OtherSubscriber other;

        DelaySubscriptionSubscriber(Subscriber<? super T> downstream, Solo<T> source) {
            super(downstream);
            this.source = source;
            this.other = new OtherSubscriber();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.replace(other, s)) {
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
            if (v == null) {
                downstream.onComplete();
            } else {
                complete(v);
            }
        }

        @Override
        public void cancel() {
            super.cancel();
        }

        void otherError(Throwable t) {
            downstream.onError(t);
        }

        void otherComplete() {
            source.subscribe(this);
        }

        final class OtherSubscriber extends AtomicReference<Subscription>
        implements Subscriber<Object> {

            private static final long serialVersionUID = -4157815870217815859L;

            boolean once;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(Object t) {
                if (!once) {
                    once = true;
                    get().cancel();
                    otherComplete();
                }
            }

            @Override
            public void onError(Throwable t) {
                otherError(t);
            }

            @Override
            public void onComplete() {
                if (!once) {
                    once = true;
                    otherComplete();
                }
            }
        }
    }

}
