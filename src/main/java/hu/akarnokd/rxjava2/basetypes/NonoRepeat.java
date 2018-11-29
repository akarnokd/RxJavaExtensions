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

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Repeatedly re-subscribe to the source Nono.
 */
final class NonoRepeat extends Nono {

    final Nono source;

    final long times;

    NonoRepeat(Nono source, long times) {
        this.source = source;
        this.times = times;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        source.subscribe(new RepeatSubscriber(s, times, source));
    }

    abstract static class RedoSubscriber extends BasicNonoIntQueueSubscription
    implements Subscriber<Void> {

        private static final long serialVersionUID = -3208438978515192633L;

        protected final Subscriber<? super Void> downstream;

        final Nono source;

        long times;

        final AtomicReference<Subscription> upstream;

        protected volatile boolean active;

        boolean once;

        RedoSubscriber(Subscriber<? super Void> downstream, long times, Nono source) {
            this.downstream = downstream;
            this.times = times;
            this.source = source;
            this.upstream = new AtomicReference<Subscription>();
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

        protected final void subscribeNext(Throwable ex) {
            long p = times;
            if (p == 1) {
                if (ex != null) {
                    downstream.onError(ex);
                } else {
                    downstream.onComplete();
                }
            } else {
                if (p != Long.MAX_VALUE) {
                    times = p - 1;
                }
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
    }

    static final class RepeatSubscriber extends RedoSubscriber {

        private static final long serialVersionUID = 3432411068139897716L;

        RepeatSubscriber(Subscriber<? super Void> downstream, long times, Nono source) {
            super(downstream, times, source);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            active = false;
            subscribeNext(null);
        }
    }
}
