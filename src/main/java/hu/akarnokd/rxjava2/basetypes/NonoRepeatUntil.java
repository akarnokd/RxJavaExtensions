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

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Repeat until the supplier returns true.
 */
final class NonoRepeatUntil extends Nono {

    final Nono source;

    final BooleanSupplier stop;

    NonoRepeatUntil(Nono source, BooleanSupplier stop) {
        this.source = source;
        this.stop = stop;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        source.subscribe(new RepeatUntilSubscriber(s, stop, source));
    }

    static final class RepeatUntilSubscriber extends BasicNonoIntQueueSubscription
    implements Subscriber<Void> {

        private static final long serialVersionUID = -3208438978515192633L;

        protected final Subscriber<? super Void> actual;

        final Nono source;

        final BooleanSupplier stop;

        final AtomicReference<Subscription> s;

        volatile boolean active;

        boolean once;

        RepeatUntilSubscriber(Subscriber<? super Void> actual,
                BooleanSupplier stop, Nono source) {
            this.actual = actual;
            this.stop = stop;
            this.source = source;
            this.s = new AtomicReference<Subscription>();
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(s);
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.replace(this.s, s);
            if (!once) {
                once = true;
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
            active = false;
            subscribeNext();
        }

        void subscribeNext() {
            boolean b;

            try {
                b = stop.getAsBoolean();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(ex);
                return;
            }

            if (b) {
                actual.onComplete();
            } else {
                if (getAndIncrement() == 0) {
                    do {
                        if (SubscriptionHelper.isCancelled(s.get())) {
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
    }
}
