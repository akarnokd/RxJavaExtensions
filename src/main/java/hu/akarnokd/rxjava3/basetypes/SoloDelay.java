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
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.operators.QueueSubscription;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Delay signals by the given time amount.
 *
 * @param <T> the value type
 */
final class SoloDelay<T> extends Solo<T> {

    final Solo<T> source;

    final long delay;

    final TimeUnit unit;

    final Scheduler scheduler;

    SoloDelay(Solo<T> source, long delay, TimeUnit unit, Scheduler scheduler) {
        this.source = source;
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new DelaySubscriber<T>(s, delay, unit, scheduler));
    }

    static final class DelaySubscriber<T> extends AtomicReference<Disposable>
    implements QueueSubscription<T>, Subscriber<T>, Runnable {

        private static final long serialVersionUID = 511073038536312798L;

        final Subscriber<? super T> downstream;

        final long delay;

        final TimeUnit unit;

        final Scheduler scheduler;

        Subscription upstream;

        T value;
        Throwable error;
        volatile boolean available;

        boolean outputFused;

        DelaySubscriber(Subscriber<? super T> downstream, long delay, TimeUnit unit, Scheduler scheduler) {
            this.downstream = downstream;
            this.delay = delay;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & ASYNC) != 0) {
                outputFused = true;
                return ASYNC;
            }
            return NONE;
        }

        @Override
        public T poll() throws Exception {
            if (available) {
                T v = value;
                value = null;
                return v;
            }
            return null;
        }

        @Override
        public boolean isEmpty() {
            return !available || value == null;
        }

        @Override
        public void clear() {
            value = null;
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
            DisposableHelper.dispose(this);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            this.value = t;
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
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
                downstream.onError(ex);
            } else {
                if (outputFused) {
                    available = true;
                    downstream.onNext(null);
                } else {
                    T v = value;
                    value = null;
                    downstream.onNext(v);
                }
                downstream.onComplete();
            }
        }

        @Override
        public boolean offer(T value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean offer(T v1, T v2) {
            throw new UnsupportedOperationException();
        }
    }
}
