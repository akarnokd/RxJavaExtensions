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

import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Run a Nono after another Nono.
 */
final class NonoAndThen extends Nono {

    final Nono before;

    final Nono after;

    NonoAndThen(Nono before, Nono after) {
        this.before = before;
        this.after = after;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        before.subscribe(new AndThenSubscriber(s, after));
    }

    static final class AndThenSubscriber extends BasicRefQueueSubscription<Void, Subscription>
    implements Subscriber<Void> {

        private static final long serialVersionUID = 5073982210916423158L;

        final Subscriber<? super Void> actual;

        final Nono after;

        AndThenSubscriber(Subscriber<? super Void> actual, Nono after) {
            this.actual = actual;
            this.after = after;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(Void t) {
            // never called
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            after.subscribe(new OtherSubscriber());
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(this);
        }

        void innerSubscribe(Subscription s) {
            SubscriptionHelper.replace(this, s);
        }

        final class OtherSubscriber implements Subscriber<Void> {

            @Override
            public void onSubscribe(Subscription s) {
                innerSubscribe(s);
            }

            @Override
            public void onNext(Void t) {
                // never called
            }

            @Override
            public void onError(Throwable t) {
                actual.onError(t);
            }

            @Override
            public void onComplete() {
                actual.onComplete();
            }
        }
    }
}
