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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.*;

/**
 * Retries the source Solo at most the given number of times.
 *
 * @param <T> the value type
 */
final class SoloRetry<T> extends Solo<T> {

    final Solo<T> source;

    final long times;

    SoloRetry(Solo<T> source, long times) {
        this.source = source;
        this.times = times;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        RetrySubscriber<T> parent = new RetrySubscriber<T>(s, times, source);
        s.onSubscribe(parent);
        parent.subscribeNext();
    }

    static final class RetrySubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -1726278593241855499L;

        final AtomicInteger wip;

        final AtomicReference<Subscription> s;

        final Solo<T> source;

        long times;

        volatile boolean active;

        RetrySubscriber(Subscriber<? super T> actual, long times, Solo<T> source) {
            super(actual);
            this.times = times;
            this.source = source;
            this.wip = new AtomicInteger();
            this.s = new AtomicReference<Subscription>();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.replace(this.s, s)) {
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            value = t;
        }

        @Override
        public void onError(Throwable t) {
            long c = times;
            if (c != Long.MAX_VALUE) {
                if (--c == 0L) {
                    actual.onError(t);
                    return;
                } else {
                    times = c;
                }
            }

            active = false;
            subscribeNext();
        }

        void subscribeNext() {
            if (wip.getAndIncrement() == 0) {
                do {
                    if (SubscriptionHelper.isCancelled(s.get())) {
                        return;
                    }

                    if (!active) {
                        active = true;
                        source.subscribe(this);
                    }
                } while (wip.decrementAndGet() != 0);
            }
        }

        @Override
        public void onComplete() {
            T v = value;
            if (v != null) {
                value = null;
                complete(v);
            } else {
                actual.onComplete();
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            SubscriptionHelper.cancel(s);
        }
    }
}
