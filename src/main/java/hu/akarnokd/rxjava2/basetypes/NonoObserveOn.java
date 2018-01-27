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

import org.reactivestreams.Subscriber;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * Emit the terminal events on the specified Scheduler.
 */
final class NonoObserveOn extends Nono {

    final Nono source;

    final Scheduler scheduler;

    NonoObserveOn(Nono source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        source.subscribe(new ObserveOnSubscriber(s, scheduler));
    }

    static final class ObserveOnSubscriber extends BasicRefNonoSubscriber<Disposable> implements Runnable {

        private static final long serialVersionUID = -7575632829277450540L;

        final Scheduler scheduler;

        Throwable error;

        ObserveOnSubscriber(Subscriber<? super Void> actual, Scheduler scheduler) {
            super(actual);
            this.scheduler = scheduler;
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            DisposableHelper.replace(this, scheduler.scheduleDirect(this));
        }

        @Override
        public void onComplete() {
            DisposableHelper.replace(this, scheduler.scheduleDirect(this));
        }

        @Override
        public void run() {
            Throwable ex = error;
            if (ex != null) {
                error = null;
                actual.onError(ex);
            } else {
                actual.onComplete();
            }
        }

        @Override
        public void cancel() {
            s.cancel();
            DisposableHelper.dispose(this);
        }
    }
}
