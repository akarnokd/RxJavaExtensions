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

package hu.akarnokd.rxjava3.processors;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import hu.akarnokd.rxjava3.util.SpmcLinkedArrayQueue;
import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.FlowableProcessor;

/**
 * A {@link FlowableProcessor} variant that queues up items and dispatches them asynchronously
 * so that one of the currently subscribed {@link Subscriber}s can pick it up one by
 * one, otherwise* cache it until at least an {@code Subscriber} subscribes.
 *
 * @param <T> the input and output value type
 * @since 0.18.8
 */
public final class DispatchWorkProcessor<T> extends FlowableProcessor<T> implements Disposable {

    /**
     * Create an empty {@link DispatchWorkProcessor} instance with the given scheduler,
     * default capacity hint ({@link Flowable#bufferSize()}, expected number of items
     * cached until consumption) and delaying errors.
     * @param <T> the input and output value type
     * @param scheduler the scheduler to use for the {@link Observer}s to be notified on
     * @return the new DispatchWorkSubject instance
     */
    public static <T> DispatchWorkProcessor<T> create(Scheduler scheduler) {
        return create(scheduler, Flowable.bufferSize(), true);
    }

    /**
     * Create an empty, unbounded {@link DispatchWorkProcessor} instance with the given scheduler,
     * default capacity hint ({@link Flowable#bufferSize()}, expected number of items
     * cached until consumption) and delaying errors.
     * @param <T> the input and output value type
     * @param scheduler the scheduler to use for the {@link Observer}s to be notified on
     * @return the new DispatchWorkSubject instance
     */
    public static <T> DispatchWorkProcessor<T> createUnbounded(Scheduler scheduler) {
        return createUnbounded(scheduler, Flowable.bufferSize(), true);
    }

    /**
     * Create an empty {@link DispatchWorkProcessor} instance with the given scheduler,
     * capacity hint (expected number of items cached until consumption) and delaying
     * errors.
     * @param <T> the input and output value type
     * @param scheduler the scheduler to use for the {@link Observer}s to be notified on
     * @param capacityHint the expected number of items to be cached until consumption
     * @return the new DispatchWorkSubject instance
     */
    public static <T> DispatchWorkProcessor<T> create(Scheduler scheduler, int capacityHint) {
        return create(scheduler, capacityHint, true);
    }

    /**
     * Create an empty {@link DispatchWorkProcessor} instance with the given scheduler,
     * default capacity hint ({@link Flowable#bufferSize()}, expected number of items
     * cached until consumption) and if an error should be delayed.
     * @param <T> the input and output value type
     * @param scheduler the scheduler to use for the {@link Observer}s to be notified on
     * @param delayErrors if true, errors are delivered after items have been consumed
     * @return the new DispatchWorkSubject instance
     */
    public static <T> DispatchWorkProcessor<T> create(Scheduler scheduler, boolean delayErrors) {
        return create(scheduler, Flowable.bufferSize(), delayErrors);
    }

    /**
     * Create an empty {@link DispatchWorkProcessor} instance with the given scheduler,
     * capacity hint (expected number of items cached until consumption) and if an
     * error should be delayed.
     * @param <T> the input and output value type
     * @param scheduler the scheduler to use for the {@link Observer}s to be notified on
     * @param capacityHint the expected number of items to be cached until consumption
     * @param delayErrors if true, errors are delivered after items have been consumed
     * @return the new DispatchWorkSubject instance
     */
    public static <T> DispatchWorkProcessor<T> create(Scheduler scheduler, int capacityHint, boolean delayErrors) {
        return new DispatchWorkProcessor<T>(capacityHint, delayErrors, scheduler, false);
    }

    /**
     * Create an empty and unbounded {@link DispatchWorkProcessor} instance with the given scheduler,
     * capacity hint (expected number of items cached until consumption) and if an
     * error should be delayed.
     * @param <T> the input and output value type
     * @param scheduler the scheduler to use for the {@link Observer}s to be notified on
     * @param capacityHint the expected number of items to be cached until consumption
     * @param delayErrors if true, errors are delivered after items have been consumed
     * @return the new DispatchWorkSubject instance
     */
    public static <T> DispatchWorkProcessor<T> createUnbounded(Scheduler scheduler, int capacityHint, boolean delayErrors) {
        return new DispatchWorkProcessor<T>(capacityHint, delayErrors, scheduler, true);
    }

    final SimplePlainQueue<T> queue;

    final AtomicInteger wip;

    final AtomicReference<Subscription> upstream;

    final AtomicReference<Throwable> error;

    final boolean delayErrors;

    final AtomicReference<WorkDisposable<T>[]> observers;

    final Scheduler scheduler;

    final long prefetch;

    final AtomicLong requestedDownstream;

    final AtomicLong requestedUpstream;

    @SuppressWarnings("rawtypes")
    static final WorkDisposable[] EMPTY = new WorkDisposable[0];
    @SuppressWarnings("rawtypes")
    static final WorkDisposable[] TERMINATED = new WorkDisposable[0];

    @SuppressWarnings("unchecked")
    DispatchWorkProcessor(int capacityHint, boolean delayErrors, Scheduler scheduler, boolean unbounded) {
        this.queue = new SpmcLinkedArrayQueue<T>(capacityHint);
        this.delayErrors = delayErrors;
        this.wip = new AtomicInteger();
        this.upstream = new AtomicReference<Subscription>();
        this.error = new AtomicReference<Throwable>();
        this.observers = new AtomicReference<WorkDisposable<T>[]>(EMPTY);
        this.scheduler = scheduler;
        this.prefetch = unbounded ? Long.MAX_VALUE : capacityHint;
        this.requestedUpstream = new AtomicLong();
        this.requestedDownstream = new AtomicLong();
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(upstream, s)) {
            s.request(prefetch);
        }
    }

    @Override
    public void onNext(T t) {
        if (error.get() == null) {
            queue.offer(t);
            for (WorkDisposable<T> wd : observers.get()) {
                wd.drain();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(Throwable e) {
        ObjectHelper.requireNonNull(e, "e is null");
        if (error.compareAndSet(null, e)) {
            for (WorkDisposable<T> wd : observers.getAndSet(TERMINATED)) {
                wd.drain();
            }
        } else {
            RxJavaPlugins.onError(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onComplete() {
        if (error.compareAndSet(null, ExceptionHelper.TERMINATED)) {
            for (WorkDisposable<T> wd : observers.getAndSet(TERMINATED)) {
                wd.drain();
            }
        }
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> subscriber) {
        WorkDisposable<T> wd = new WorkDisposable<T>(subscriber, this, scheduler.createWorker(), delayErrors);
        subscriber.onSubscribe(wd);
        if (add(wd)) {
            if (wd.isCancelled()) {
                remove(wd);
                return;
            }
        }
        wd.drain();
    }

    @Override
    public void dispose() {
        SubscriptionHelper.cancel(upstream);
    }

    @Override
    public boolean isDisposed() {
        return SubscriptionHelper.CANCELLED == upstream.get();
    }

    @Override
    public boolean hasComplete() {
        return error.get() == ExceptionHelper.TERMINATED;
    }

    @Override
    public boolean hasThrowable() {
        Throwable ex = error.get();
        return ex != null && ex != ExceptionHelper.TERMINATED;
    }

    @Override
    public Throwable getThrowable() {
        Throwable ex = error.get();
        return ex != ExceptionHelper.TERMINATED ? ex : null;
    }

    @Override
    public boolean hasSubscribers() {
        return observers.get().length != 0;
    }

    boolean add(WorkDisposable<T> wd) {
        for (;;) {
            WorkDisposable<T>[] a = observers.get();
            if (a == TERMINATED) {
                return false;
            }
            int n = a.length;
            @SuppressWarnings("unchecked")
            WorkDisposable<T>[] b = new WorkDisposable[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = wd;
            if (observers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(WorkDisposable<T> wd) {
        for (;;) {
            WorkDisposable<T>[] a = observers.get();
            int n = a.length;
            if (n == 0) {
                break;
            }

            int j = -1;
            for (int i = 0; i < n; i++) {
                if (a[i] == wd) {
                    j = i;
                    break;
                }
            }

            if (j < 0) {
                break;
            }
            WorkDisposable<T>[] b;
            if (n == 1) {
                b = EMPTY;
            } else {
                b = new WorkDisposable[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (observers.compareAndSet(a, b)) {
                break;
            }
        }
    }

    void requestMore(long n) {
        long pf = prefetch;
        Subscription s = upstream.get();
        if (pf != Long.MAX_VALUE && s != null) {
            long limit = pf - (pf >> 2);

            AtomicLong requestedDownstream = this.requestedDownstream;
            BackpressureHelper.add(requestedDownstream, n);

            AtomicLong requestedUpstream = this.requestedUpstream;
            for (;;) {
                long rd = requestedDownstream.get();
                long ru = requestedUpstream.get();
                if (rd - ru >= limit) {
                    long next = BackpressureHelper.addCap(ru, limit);
                    if (requestedUpstream.compareAndSet(ru, next)) {
                        s.request(limit);
                    }
                } else {
                    break;
                }
            }
        }
    }

    static final class WorkDisposable<T> extends AtomicInteger implements Subscription, Runnable {

        private static final long serialVersionUID = 7597704795244221647L;

        final Subscriber<? super T> downstream;

        final DispatchWorkProcessor<T> parent;

        final Worker worker;

        final boolean delayErrors;

        final AtomicLong requested;

        long emitted;

        volatile boolean disposed;

        WorkDisposable(Subscriber<? super T> downstream, DispatchWorkProcessor<T> parent, Worker worker, boolean delayErrors) {
            this.downstream = downstream;
            this.parent = parent;
            this.worker = worker;
            this.delayErrors = delayErrors;
            this.requested = new AtomicLong();
        }

        @Override
        public void cancel() {
            disposed = true;
            parent.remove(this);
            worker.dispose();
        }

        boolean isCancelled() {
            return disposed;
        }

        @Override
        public void request(long n) {
            BackpressureHelper.add(requested, n);
            drain();
        }

        @Override
        public void run() {
            int missed = 1;

            DispatchWorkProcessor<T> parent = this.parent;
            SimplePlainQueue<T> q = parent.queue;
            Subscriber<? super T> downstream = this.downstream;
            AtomicReference<Throwable> error = parent.error;
            boolean delayErrors = this.delayErrors;
            long e = emitted;
            AtomicLong requested = this.requested;

            for (;;) {

                long r = requested.get();
                long c = 0;
                while (e != r) {
                    if (disposed) {
                        return;
                    }

                    Throwable ex = error.get();
                    boolean d = ex != null;
                    if (d && !delayErrors) {
                        if (ex != ExceptionHelper.TERMINATED) {
                            q.clear();
                            downstream.onError(ex);
                            worker.dispose();
                            return;
                        }
                    }

                    T v = q.poll();
                    boolean empty = v == null;

                    if (d && empty) {
                        if (ex == ExceptionHelper.TERMINATED) {
                            downstream.onComplete();
                        } else {
                            downstream.onError(ex);
                        }
                        worker.dispose();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    downstream.onNext(v);

                    e++;
                    c++;
                }

                if (c != 0L) {
                    parent.requestMore(c);
                }

                if (e == r) {
                    if (disposed) {
                        return;
                    }

                    Throwable ex = error.get();
                    boolean d = ex != null;
                    if (d && !delayErrors) {
                        if (ex != ExceptionHelper.TERMINATED) {
                            q.clear();
                            downstream.onError(ex);
                            worker.dispose();
                            return;
                        }
                    }

                    boolean empty = q.isEmpty();

                    if (d && empty) {
                        if (ex == ExceptionHelper.TERMINATED) {
                            downstream.onComplete();
                        } else {
                            downstream.onError(ex);
                        }
                        worker.dispose();
                        return;
                    }
                }

                emitted = e;
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        void drain() {
            if (getAndIncrement() == 0) {
                worker.schedule(this);
            }
        }
    }
}
