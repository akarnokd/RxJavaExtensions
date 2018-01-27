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
 * Execute a Nono after the source Perhaps.
 *
 * @param <T> the value type
 */
final class PerhapsAndThenNono<T> extends Perhaps<T> {

    final Perhaps<T> source;

    final Nono other;

    PerhapsAndThenNono(Perhaps<T> source, Nono other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new AndThenNonoSubscriber<T>(s, other));
    }

    static final class AndThenNonoSubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -7603167128015174475L;

        final Nono other;

        final OtherSubscriber otherSubscriber;

        Subscription s;

        AndThenNonoSubscriber(Subscriber<? super T> actual, Nono other) {
            super(actual);
            this.other = other;
            this.otherSubscriber = new OtherSubscriber();
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
            other.subscribe(otherSubscriber);
        }

        @Override
        public void cancel() {
            super.cancel();
            s.cancel();
            SubscriptionHelper.cancel(otherSubscriber);
        }

        void otherError(Throwable t) {
            actual.onError(t);
        }

        void otherComplete() {
            T v = value;
            if (v != null) {
                complete(v);
            } else {
                actual.onComplete();
            }
        }

        final class OtherSubscriber extends AtomicReference<Subscription>
        implements Subscriber<Object> {

            private static final long serialVersionUID = -2562437629991690939L;

            @Override
            public void onSubscribe(Subscription s) {
                SubscriptionHelper.setOnce(this, s);
            }

            @Override
            public void onNext(Object t) {
                // not called
            }

            @Override
            public void onError(Throwable t) {
                otherError(t);
            }

            @Override
            public void onComplete() {
                otherComplete();
            }
        }
    }

}
