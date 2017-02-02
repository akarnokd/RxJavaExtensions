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

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Signal a TimeoutException or switch to a fallback Mono if the
 * other publisher signals a value or completes.
 */
final class NonoTimeout extends Nono {

    final Nono source;

    final Publisher<?> other;

    final Nono fallback;

    NonoTimeout(Nono source, Publisher<?> other, Nono fallback) {
        this.source = source;
        this.other = other;
        this.fallback = fallback;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        MainSubscriber parent = new MainSubscriber(s, fallback);
        s.onSubscribe(parent);

        other.subscribe(parent.other);
        source.subscribe(parent);
    }

    static final class MainSubscriber extends BasicRefQueueSubscription<Void, Subscription>
    implements Subscriber<Void> {

        private static final long serialVersionUID = 5699062216456523328L;

        final Subscriber<? super Void> actual;

        final Nono fallback;

        final OtherSubscriber other;

        final AtomicBoolean once;

        MainSubscriber(Subscriber<? super Void> actual, Nono fallback) {
            this.actual = actual;
            this.fallback = fallback;
            this.other = new OtherSubscriber();
            this.once = new AtomicBoolean();
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(this);
            SubscriptionHelper.cancel(other);
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
            SubscriptionHelper.cancel(other);
            if (once.compareAndSet(false, true)) {
                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            SubscriptionHelper.cancel(other);
            if (once.compareAndSet(false, true)) {
                actual.onComplete();
            }
        }

        void otherError(Throwable t) {
            SubscriptionHelper.cancel(this);
            if (once.compareAndSet(false, true)) {
                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        void otherComplete() {
            SubscriptionHelper.cancel(this);
            if (once.compareAndSet(false, true)) {
                Nono f = fallback;
                if (f == null) {
                    actual.onError(new TimeoutException());
                } else {
                    f.subscribe(new FallbackSubscriber());
                }
            }
        }

        void fallbackSubscribe(Subscription s) {
            SubscriptionHelper.replace(this, s);
        }

        final class FallbackSubscriber implements Subscriber<Void> {

            @Override
            public void onSubscribe(Subscription s) {
                fallbackSubscribe(s);
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

        final class OtherSubscriber extends AtomicReference<Subscription> implements Subscriber<Object> {

            private static final long serialVersionUID = -7257274632636068061L;

            boolean done;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(Object t) {
                if (!done) {
                    SubscriptionHelper.cancel(this);
                    onComplete();
                }
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
