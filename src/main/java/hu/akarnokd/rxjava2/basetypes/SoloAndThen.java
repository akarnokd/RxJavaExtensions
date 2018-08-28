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

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.*;

/**
 * Run the given Nono after this Solo completes successfully and
 * emit that original success value only if the Nono completes normally.
 *
 * @param <T> the value type
 */
final class SoloAndThen<T> extends Solo<T> {

    final Solo<T> source;

    final Nono other;

    SoloAndThen(Solo<T> source, Nono other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new AndThenSubscriber<T>(s, other));
    }

    static final class AndThenSubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = 2538648456345135486L;

        final OtherSubscriber otherSubscriber;

        final Nono other;

        Subscription upstream;

        AndThenSubscriber(Subscriber<? super T> downstream, Nono other) {
            super(downstream);
            this.otherSubscriber = new OtherSubscriber();
            this.other = other;
        }

        @Override
        public void cancel() {
            super.cancel();
            SubscriptionHelper.cancel(otherSubscriber);
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
            value = null;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            other.subscribe(otherSubscriber);
        }

        final class OtherSubscriber extends AtomicReference<Subscription>
        implements Subscriber<Object> {

            private static final long serialVersionUID = 1987312061510219761L;

            @Override
            public void onSubscribe(Subscription s) {
                SubscriptionHelper.setOnce(this, s);
            }

            @Override
            public void onNext(Object t) {
                // ignored
            }

            @Override
            public void onError(Throwable t) {
                downstream.onError(t);
            }

            @Override
            public void onComplete() {
                complete(value);
            }

        }
    }
}
