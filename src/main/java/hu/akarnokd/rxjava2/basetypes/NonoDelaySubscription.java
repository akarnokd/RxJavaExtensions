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

import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Delay subscribing to the main Nono until the other Publisher signals
 * a value or completes.
 */
final class NonoDelaySubscription extends Nono {

    final Nono source;

    final Publisher<?> other;

    NonoDelaySubscription(Nono source, Publisher<?> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        other.subscribe(new DelaySubscriptionSubscriber(s, source));
    }

    static final class DelaySubscriptionSubscriber extends BasicRefQueueSubscription<Object, Subscription>
    implements Subscriber<Object> {

        private static final long serialVersionUID = 7914910659996431449L;

        final Subscriber<? super Void> actual;

        final Nono source;

        boolean done;

        DelaySubscriptionSubscriber(Subscriber<? super Void> actual, Nono source) {
            this.actual = actual;
            this.source = source;
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(this);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {

                actual.onSubscribe(this);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(Object t) {
            if (!done) {
                get().cancel();
                onComplete();
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
            } else {
                actual.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                source.subscribe(new MainSubscriber());
            }
        }

        void innerSubscribe(Subscription s) {
            SubscriptionHelper.replace(this, s);
        }

        final class MainSubscriber implements Subscriber<Void> {

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
