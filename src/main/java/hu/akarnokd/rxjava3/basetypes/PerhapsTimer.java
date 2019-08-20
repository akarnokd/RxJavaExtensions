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

import java.util.concurrent.TimeUnit;

import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava3.basetypes.SoloTimer.TimerSubscriber;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

/**
 * Signal a 0L after the specified time delay.
 */
final class PerhapsTimer extends Perhaps<Long> {

    final long delay;

    final TimeUnit unit;

    final Scheduler scheduler;

    PerhapsTimer(long delay, TimeUnit unit, Scheduler scheduler) {
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
}
