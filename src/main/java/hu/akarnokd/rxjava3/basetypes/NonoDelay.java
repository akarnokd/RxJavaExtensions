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

import java.util.concurrent.TimeUnit;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

/**
 * Emit the terminal events on the specified Scheduler.
 */
final class NonoDelay extends Nono {

    final Nono source;

    final long delay;

    final TimeUnit unit;

    final Scheduler scheduler;

    NonoDelay(Nono source, long delay, TimeUnit unit, Scheduler scheduler) {
        this.source = source;
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        source.subscribe(new ObserveOnSubscriber(s, delay, unit, scheduler));
    }

    static final class ObserveOnSubscriber extends BasicRefNonoSubscriber<Disposable> implements Runnable {

        private static final long serialVersionUID = -7575632829277450540L;

        final long delay;

        final TimeUnit unit;

        final Scheduler scheduler;

        Throwable error;

        ObserveOnSubscriber(Subscriber<? super Void> downstream, long delay, TimeUnit unit, Scheduler scheduler) {
            super(downstream);
            this.delay = delay;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            DisposableHelper.replace(this, scheduler.scheduleDirect(this, delay, unit));
        }

        @Override
        public void onComplete() {
            DisposableHelper.replace(this, scheduler.scheduleDirect(this, delay, unit));
        }

        @Override
        public void run() {
            Throwable ex = error;
            if (ex != null) {
                error = null;
                downstream.onError(ex);
            } else {
                downstream.onComplete();
            }
        }

        @Override
        public void cancel() {
            upstream.cancel();
            DisposableHelper.dispose(this);
        }
    }
}
