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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Run the main Nono until a Publisher signals an item or completes.
 */
final class NonoTakeUntil extends Nono {

    final Nono source;

    final Publisher<?> other;

    NonoTakeUntil(Nono source, Publisher<?> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        TakeUntilSubscriber parent = new TakeUntilSubscriber(s);
        s.onSubscribe(parent);

        other.subscribe(parent.inner);
        source.subscribe(parent);
    }

    static final class TakeUntilSubscriber extends BasicRefQueueSubscription<Void, Subscription>
    implements Subscriber<Void> {

        private static final long serialVersionUID = 5812459132190733401L;

        final Subscriber<? super Void> actual;

        final AtomicBoolean once;

        final OtherSubscriber inner;

        TakeUntilSubscriber(Subscriber<? super Void> actual) {
            this.actual = actual;
            this.once = new AtomicBoolean();
            this.inner = new OtherSubscriber();
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(this);
            SubscriptionHelper.cancel(inner);
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.setOnce(this, s);
        }

        @Override
        public void onNext(Void t) {
            // never called
        }

        @Override
        public void onError(Throwable t) {
            if (once.compareAndSet(false, true)) {
                SubscriptionHelper.cancel(inner);
                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (once.compareAndSet(false, true)) {
                SubscriptionHelper.cancel(inner);
                actual.onComplete();
            }
        }

        void innerComplete() {
            if (once.compareAndSet(false, true)) {
                SubscriptionHelper.cancel(this);
                actual.onComplete();
            }
        }

        void innerError(Throwable t) {
            if (once.compareAndSet(false, true)) {
                SubscriptionHelper.cancel(this);
                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        final class OtherSubscriber extends AtomicReference<Subscription>
        implements Subscriber<Object> {

            private static final long serialVersionUID = 9056087023210091030L;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(Object t) {
                get().cancel();
                innerComplete();
            }

            @Override
            public void onError(Throwable t) {
                innerError(t);
            }

            @Override
            public void onComplete() {
                innerComplete();
            }
        }
    }
}
