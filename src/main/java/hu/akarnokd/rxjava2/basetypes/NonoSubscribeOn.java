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

import org.reactivestreams.*;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Subscribe to the source Nono on a specified scheduler.
 */
final class NonoSubscribeOn extends Nono {

    final Nono source;

    final Scheduler scheduler;

    NonoSubscribeOn(Nono source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        SubscribeOnSubscriber parent = new SubscribeOnSubscriber(s, source);
        s.onSubscribe(parent);

        parent.setTask(scheduler.scheduleDirect(parent));
    }

    static final class SubscribeOnSubscriber extends BasicRefQueueSubscription<Void, Subscription>
    implements Subscriber<Void>, Runnable {

        private static final long serialVersionUID = -6761773996344047676L;

        final Subscriber<? super Void> actual;

        final SequentialDisposable task;

        final Nono source;

        SubscribeOnSubscriber(Subscriber<? super Void> actual, Nono source) {
            this.actual = actual;
            this.source = source;
            this.task = new SequentialDisposable();
        }

        void setTask(Disposable d) {
            task.replace(d);
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.setOnce(this, s);
        }

        @Override
        public void onNext(Void t) {
            // not present
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
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
            SubscriptionHelper.cancel(this);
            task.dispose();
        }

        @Override
        public void run() {
            source.subscribe(this);
        }
    }
}
