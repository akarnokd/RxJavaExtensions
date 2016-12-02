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

import org.reactivestreams.Subscriber;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * Signal onComplete after the specified delay on the given scheduler.
 */
final class NonoTimer extends Nono {

    final long delay;
    
    final TimeUnit unit;
    
    final Scheduler scheduler;
    
    NonoTimer(long delay, TimeUnit unit, Scheduler scheduler) {
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        TimerSubscription parent = new TimerSubscription(s);
        s.onSubscribe(parent);

        DisposableHelper.replace(parent, scheduler.scheduleDirect(parent, delay, unit));
    }

    static final class TimerSubscription extends BasicRefQueueSubscription<Void, Disposable>
    implements Runnable {
        private static final long serialVersionUID = 3940118717227297027L;

        final Subscriber<? super Void> actual;
        
        TimerSubscription(Subscriber<? super Void> actual) {
            this.actual = actual;
        }

        @Override
        public int requestFusion(int mode) {
            return mode & ASYNC;
        }

        @Override
        public Void poll() throws Exception {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void clear() {
            // no-op
        }

        @Override
        public void request(long n) {
            // no-op
        }

        @Override
        public void cancel() {
            DisposableHelper.dispose(this);
        }

        @Override
        public void run() {
            actual.onComplete();
        }
        
        
    }
}
