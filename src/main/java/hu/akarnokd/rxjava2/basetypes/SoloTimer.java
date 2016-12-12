/*
 * Copyright 2016 David Karnok
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.subscriptions.DeferredScalarSubscription;

/**
 * Signal a 0L after the specified time delay.
 */
final class SoloTimer extends Solo<Long> {

    final long delay;

    final TimeUnit unit;

    final Scheduler scheduler;

    SoloTimer(long delay, TimeUnit unit, Scheduler scheduler) {
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Long> s) {
        TimerSubscriber parent = new TimerSubscriber(s);
        s.onSubscribe(parent);

        Disposable d = scheduler.scheduleDirect(parent, delay, unit);
        DisposableHelper.replace(parent.task, d);
    }

    static final class TimerSubscriber extends DeferredScalarSubscription<Long>
    implements Runnable {

        private static final long serialVersionUID = -4937102843159363918L;

        final AtomicReference<Disposable> task;

        TimerSubscriber(Subscriber<? super Long> actual) {
            super(actual);
            this.task = new AtomicReference<Disposable>();
        }

        @Override
        public void run() {
            complete(0L);
        }
    }
}
