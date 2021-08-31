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

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.*;

import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.AtomicThrowable;

/**
 * Subscribe to the Nono sources one at a time and complete if all of them complete.
 */
final class NonoConcatArray extends Nono {

    final Nono[] sources;

    final boolean delayError;

    NonoConcatArray(Nono[] sources, boolean delayError) {
        this.sources = sources;
        this.delayError = delayError;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        ConcatSubscriber parent = new ConcatSubscriber(s, sources, delayError);
        s.onSubscribe(parent);
        parent.drain();
    }

    static final class ConcatSubscriber extends BasicRefQueueSubscription<Void, Subscription> implements Subscriber<Void> {

        private static final long serialVersionUID = -4926738846855955051L;

        final Subscriber<? super Void> downstream;

        final AtomicThrowable errors;

        final Nono[] sources;

        final AtomicInteger wip;

        int index;

        volatile boolean active;

        ConcatSubscriber(Subscriber<? super Void> downstream, Nono[] sources, boolean delayError) {
            this.downstream = downstream;
            this.sources = sources;
            this.errors = delayError ? new AtomicThrowable() : null;
            this.wip = new AtomicInteger();
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(this);
            if (errors != null) {
                errors.tryTerminateAndReport();
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.replace(this, s);
        }

        @Override
        public void onNext(Void t) {
            // not called
        }

        @Override
        public void onError(Throwable t) {
            AtomicThrowable err = errors;
            if (err != null) {
                if (err.tryAddThrowableOrReport(t)) {
                    active = false;
                    drain();
                }
            } else {
                downstream.onError(t);
            }
        }

        @Override
        public void onComplete() {
            active = false;
            drain();
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            do {
                if (SubscriptionHelper.CANCELLED == this.get()) {
                    return;
                }

                if (!active) {
                    int idx = index;
                    if (idx == sources.length) {
                        if (errors != null) {
                            errors.tryTerminateConsumer(downstream);
                        } else {
                            downstream.onComplete();
                        }
                        return;
                    }

                    Nono np = sources[idx];

                    index = idx + 1;

                    if (np == null) {
                        NullPointerException npe = new NullPointerException("One of the sources is null");
                        if (errors != null) {
                            errors.tryAddThrowableOrReport(npe);
                            errors.tryTerminateConsumer(downstream);
                        } else {
                            downstream.onError(npe);
                        }
                        return;
                    }

                    active = true;
                    np.subscribe(this);
                }
            } while (wip.decrementAndGet() != 0);
        }
    }
}
