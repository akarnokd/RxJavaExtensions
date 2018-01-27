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

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Switch to another Perhaps if this doesn't signal events before the other
 * Publisher does.
 *
 * @param <T> the value type
 */
final class PerhapsTimeout<T> extends Perhaps<T> {

    final Perhaps<T> source;

    final Publisher<?> other;

    final Perhaps<? extends T> fallback;

    PerhapsTimeout(Perhaps<T> source, Publisher<?> other, Perhaps<? extends T> fallback) {
        this.source = source;
        this.other = other;
        this.fallback = fallback;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        TimeoutSubscriber<T> parent = new TimeoutSubscriber<T>(s, fallback);
        s.onSubscribe(parent);

        other.subscribe(parent.other);
        source.subscribe(parent);
    }

    static final class TimeoutSubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -2613153829201889588L;

        final AtomicReference<Subscription> s;

        final Perhaps<? extends T> fallback;

        final OtherSubscriber other;

        final FallbackSubscriber fallbackSubscriber;

        final AtomicBoolean once;

        TimeoutSubscriber(Subscriber<? super T> actual, Perhaps<? extends T> fallback) {
            super(actual);
            this.s = new AtomicReference<Subscription>();
            this.fallback = fallback;
            this.once = new AtomicBoolean();
            this.other = new OtherSubscriber();
            this.fallbackSubscriber = fallback != null ? new FallbackSubscriber() : null;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this.s, s)) {
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            value = t;
        }

        @Override
        public void onError(Throwable t) {
            if (once.compareAndSet(false, true)) {
                SubscriptionHelper.cancel(other);

                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (once.compareAndSet(false, true)) {
                SubscriptionHelper.cancel(other);

                T v = value;
                if (v != null) {
                    complete(value);
                } else {
                    actual.onComplete();
                }
            }
        }

        void otherComplete() {
            SubscriptionHelper.cancel(s);
            if (once.compareAndSet(false, true)) {
                Perhaps<? extends T> f = fallback;
                if (f != null) {
                    f.subscribe(fallbackSubscriber);
                } else {
                    actual.onError(new TimeoutException());
                }
            }
        }

        void otherError(Throwable ex) {
            if (once.compareAndSet(false, true)) {
                SubscriptionHelper.cancel(s);

                actual.onError(ex);
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            SubscriptionHelper.cancel(s);
            SubscriptionHelper.cancel(other);
            AtomicReference<Subscription> fs = this.fallbackSubscriber;
            if (fs != null) {
                SubscriptionHelper.cancel(fs);
            }
        }

        void fallbackError(Throwable ex) {
            actual.onError(ex);
        }

        void fallbackComplete(T v) {
            if (v != null) {
                complete(v);
            } else {
                actual.onComplete();
            }
        }

        final class OtherSubscriber extends AtomicReference<Subscription>
        implements Subscriber<Object> {

            private static final long serialVersionUID = -8725214806550415150L;

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
                if (!once) {
                    once = true;
                    otherError(t);
                } else {
                    RxJavaPlugins.onError(t);
                }
            }

            @Override
            public void onComplete() {
                if (!once) {
                    once = true;
                    otherComplete();
                }
            }
        }

        final class FallbackSubscriber extends AtomicReference<Subscription>
        implements Subscriber<T> {

            private static final long serialVersionUID = -1360947483517311225L;

            T v;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(T t) {
                v = t;
            }

            @Override
            public void onError(Throwable t) {
                fallbackError(t);
            }

            @Override
            public void onComplete() {
                T val = v;
                v = null;
                fallbackComplete(val);
            }
        }
    }
}
