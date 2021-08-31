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

package hu.akarnokd.rxjava3.operators;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.SequentialDisposable;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.subscribers.SerializedSubscriber;

/**
 * Emit the very last item if the source ends or the timeout
 * happens after the last upstream item's arrival or from
 * the start of the sequence.
 *
 * @param <T> the value type
 */
final class FlowableTimeoutLast<T> extends Flowable<T>
implements FlowableTransformer<T, T> {

    final Publisher<T> source;

    final long timeout;

    final TimeUnit unit;

    final Scheduler scheduler;

    final boolean fromStart;

    FlowableTimeoutLast(Publisher<T> source, long timeout, TimeUnit unit, Scheduler scheduler, boolean fromStart) {
        this.source = source;
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.fromStart = fromStart;
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableTimeoutLast<>(upstream, timeout, unit, scheduler, fromStart);
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        SerializedSubscriber<T> serial = new SerializedSubscriber<>(s);
        if (fromStart) {
            source.subscribe(new TimeoutStartLast<>(serial, timeout, unit, scheduler));
        } else {
            source.subscribe(new TimeoutLast<>(serial, timeout, unit, scheduler.createWorker()));
        }
    }

    static final class TimeoutLast<T>
    extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = 7744982114753543953L;

        final long timeout;

        final TimeUnit unit;

        final Scheduler.Worker worker;

        final SequentialDisposable task;

        final AtomicLong index;

        final AtomicReference<T> value;

        Subscription upstream;

        TimeoutLast(Subscriber<? super T> downstream, long timeout, TimeUnit unit, Worker worker) {
            super(downstream);
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
            this.task = new SequentialDisposable();
            this.index = new AtomicLong();
            this.value = new AtomicReference<>();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);

                scheduleTimeout(0L);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            final long idx = index.incrementAndGet();

            if (idx >= 0L) {
                value.lazySet(t);

                Disposable d = task.get();
                if (d != null) {
                    d.dispose();
                }

                scheduleTimeout(idx);
            }
        }

        void scheduleTimeout(final long idx) {
            task.replace(worker.schedule(new Runnable() {
                @Override
                public void run() {
                    if (index.compareAndSet(idx, Long.MIN_VALUE)) {
                        upstream.cancel();
                        emitLast();
                    }
                }
            }, timeout, unit));
        }

        @Override
        public void onError(Throwable t) {
            index.getAndSet(Long.MIN_VALUE);
            downstream.onError(t);
            worker.dispose();
            value.lazySet(null);
        }

        @Override
        public void onComplete() {
            if (index.getAndSet(Long.MIN_VALUE) != Long.MIN_VALUE) {
                emitLast();
            }
        }

        void emitLast() {
            T v = value.get();
            value.lazySet(null);
            if (v != null) {
                complete(v);
            } else {
                downstream.onComplete();
            }
            worker.dispose();
        }

        @Override
        public void cancel() {
            if (index.getAndSet(Long.MIN_VALUE) != Long.MIN_VALUE) {
                upstream.cancel();
                worker.dispose();
                value.lazySet(null);
            }
        }
    }

    static final class TimeoutStartLast<T>
    extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = 7744982114753543953L;

        final long timeout;

        final TimeUnit unit;

        final Scheduler scheduler;

        final SequentialDisposable task;

        final AtomicBoolean once;

        final AtomicReference<T> value;

        Subscription upstream;

        TimeoutStartLast(Subscriber<? super T> downstream, long timeout, TimeUnit unit, Scheduler scheduler) {
            super(downstream);
            this.timeout = timeout;
            this.unit = unit;
            this.scheduler = scheduler;
            this.task = new SequentialDisposable();
            this.once = new AtomicBoolean();
            this.value = new AtomicReference<>();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);

                scheduleTimeout(0L);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            value.lazySet(t);
        }

        void scheduleTimeout(final long idx) {
            task.replace(scheduler.scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    if (once.compareAndSet(false, true)) {
                        upstream.cancel();
                        emitLast();
                    }
                }
            }, timeout, unit));
        }

        @Override
        public void onError(Throwable t) {
            if (once.compareAndSet(false, true)) {
                downstream.onError(t);
                task.dispose();
                value.lazySet(null);
            }
        }

        @Override
        public void onComplete() {
            if (once.compareAndSet(false, true)) {
                emitLast();
            }
        }

        void emitLast() {
            T v = value.get();
            value.lazySet(null);
            if (v != null) {
                complete(v);
            } else {
                downstream.onComplete();
            }
            task.dispose();
        }

        @Override
        public void cancel() {
            if (once.compareAndSet(false, true)) {
                upstream.cancel();
                task.dispose();
                value.lazySet(null);
            }
        }
    }
}
