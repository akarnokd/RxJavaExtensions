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

package hu.akarnokd.rxjava2.operators;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * Periodically try to emit an ever increasing long value (starting from 0L)
 * or hold onto them without physical buffering until the downstream catches up.
 * @since 0.15.0
 */
final class FlowableIntervalBackpressure extends Flowable<Long> {

    final long initialDelay;

    final long period;

    final TimeUnit unit;

    final Scheduler scheduler;

    FlowableIntervalBackpressure(long initialDelay, long period, TimeUnit unit, Scheduler scheduler) {
        this.initialDelay = initialDelay;
        this.period = period;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Long> s) {
        IntervalBackpressureSubscription ibs = new IntervalBackpressureSubscription(s);
        s.onSubscribe(ibs);
        ibs.task.replace(scheduler.schedulePeriodicallyDirect(ibs, initialDelay, period, unit));
    }

    static final class IntervalBackpressureSubscription extends AtomicInteger
    implements Subscription, Runnable {

        private static final long serialVersionUID = -3871976901922172519L;

        final Subscriber<? super Long> actual;

        final SequentialDisposable task;

        final AtomicLong requested;

        final AtomicLong available;

        long emitted;

        IntervalBackpressureSubscription(Subscriber<? super Long> actual) {
            this.actual = actual;
            this.task = new SequentialDisposable();
            this.requested = new AtomicLong();
            this.available = new AtomicLong(-1L);
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            task.dispose();
        }

        @Override
        public void run() {
            available.getAndIncrement();
            drain();
        }

        void drain() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                SequentialDisposable t = task;
                AtomicLong v = available;
                long produced = emitted;
                Subscriber<? super Long> a = actual;

                for (;;) {
                    long r = requested.get();

                    while (produced != r) {
                        if (t.isDisposed()) {
                            return;
                        }
                        if (v.get() >= produced) {
                            a.onNext(produced);
                            produced++;
                        } else {
                            break;
                        }
                    }

                    if (t.isDisposed()) {
                        return;
                    }

                    emitted = produced;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                }
            }
        }
    }
}
