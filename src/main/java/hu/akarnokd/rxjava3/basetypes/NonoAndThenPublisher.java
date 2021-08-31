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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Run the Nono before switching to the Publisher and relaying its values.
 *
 * @param <T> the value type
 */
final class NonoAndThenPublisher<T> extends Flowable<T> {

    final Nono before;

    final Publisher<? extends T> after;

    NonoAndThenPublisher(Nono before, Publisher<? extends T> after) {
        this.before = before;
        this.after = after;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        before.subscribe(new AndThenSubscriber<T>(s, after));
    }

    static final class AndThenSubscriber<T> extends AtomicReference<Subscription>
    implements Subscriber<Void>, Subscription {

        private static final long serialVersionUID = -1295251708496517979L;

        final Subscriber<? super T> downstream;

        final AtomicLong requested;

        final Publisher<? extends T> after;

        Subscription upstream;

        AndThenSubscriber(Subscriber<? super T> downstream, Publisher<? extends T> after) {
            this.downstream = downstream;
            this.after = after;
            this.requested = new AtomicLong();
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(this, requested, n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
            SubscriptionHelper.cancel(this);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(Void t) {
            // not called
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            after.subscribe(new AfterSubscriber());
        }

        void innerOnSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(this, requested, s);
        }

        final class AfterSubscriber implements Subscriber<T> {

            final Subscriber<? super T> a = downstream;

            @Override
            public void onSubscribe(Subscription s) {
                innerOnSubscribe(s);
            }

            @Override
            public void onNext(T t) {
                a.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                a.onError(t);
            }

            @Override
            public void onComplete() {
                a.onComplete();
            }
        }
    }
}
