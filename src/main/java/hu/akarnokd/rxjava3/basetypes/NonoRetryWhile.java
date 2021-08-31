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

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Retry while the predicate returns true.
 */
final class NonoRetryWhile extends Nono {

    final Nono source;

    final Predicate<? super Throwable> predicate;

    NonoRetryWhile(Nono source, Predicate<? super Throwable> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        source.subscribe(new RetryUntilSubscriber(s, predicate, source));
    }

    static final class RetryUntilSubscriber extends BasicNonoIntQueueSubscription
    implements Subscriber<Void> {

        private static final long serialVersionUID = -3208438978515192633L;

        protected final Subscriber<? super Void> downstream;

        final Nono source;

        final Predicate<? super Throwable> predicate;

        final AtomicReference<Subscription> upstream;

        volatile boolean active;

        boolean once;

        RetryUntilSubscriber(Subscriber<? super Void> downstream,
                Predicate<? super Throwable> predicate, Nono source) {
            this.downstream = downstream;
            this.predicate = predicate;
            this.source = source;
            this.upstream = new AtomicReference<>();
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(upstream);
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.replace(this.upstream, s);
            if (!once) {
                once = true;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(Void t) {
            // never called
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

            if (!b) {
                downstream.onError(t);
            } else {
                active = false;
                if (getAndIncrement() == 0) {
                    do {
                        if (SubscriptionHelper.CANCELLED == upstream.get()) {
                            return;
                        }

                        if (!active) {
                            active = true;
                            source.subscribe(this);
                        }
                    } while (decrementAndGet() != 0);
                }
            }
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
