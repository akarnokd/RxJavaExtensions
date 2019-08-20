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

package hu.akarnokd.rxjava3.basetypes;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.internal.subscriptions.*;

/**
 * Retries the source Solo at most the given number of times.
 *
 * @param <T> the value type
 */
final class SoloRetryWhile<T> extends Solo<T> {

    final Solo<T> source;

    final Predicate<? super Throwable> predicate;

    SoloRetryWhile(Solo<T> source, Predicate<? super Throwable> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        RetrySubscriber<T> parent = new RetrySubscriber<T>(s, predicate, source);
        s.onSubscribe(parent);
        parent.subscribeNext();
    }

    static final class RetrySubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -1726278593241855499L;

        final AtomicInteger wip;

        final AtomicReference<Subscription> upstream;

        final Solo<T> source;

        final Predicate<? super Throwable> predicate;

        volatile boolean active;

        RetrySubscriber(Subscriber<? super T> downstream, Predicate<? super Throwable> predicate, Solo<T> source) {
            super(downstream);
            this.predicate = predicate;
            this.source = source;
            this.wip = new AtomicInteger();
            this.upstream = new AtomicReference<Subscription>();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.replace(this.upstream, s)) {
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            value = t;
        }

        @Override
        public void onError(Throwable t) {

            boolean b;

            try {
                b = predicate.test(t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(new CompositeException(t, ex));
                return;
            }

            if (b) {
                active = false;
                subscribeNext();
            } else {
                downstream.onError(t);
            }
        }

        void subscribeNext() {
            if (wip.getAndIncrement() == 0) {
                do {
                    if (SubscriptionHelper.CANCELLED == upstream.get()) {
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
                downstream.onComplete();
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            SubscriptionHelper.cancel(upstream);
        }
    }
}
