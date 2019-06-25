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

package hu.akarnokd.rxjava3.subjects;

import java.util.concurrent.atomic.*;

import hu.akarnokd.rxjava3.util.SpmcLinkedArrayQueue;
import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.Subject;

/**
 * A {@link Subject} variant that queues up items and dispatches them asynchronously
 * so that one of the currently subscribed {@link Observer}s can pick it up one by
 * one, otherwise* cache it until at least an {@code Observer} subscribes.
 *
 * @param <T> the input and output value type
 * @since 0.18.8
 */
public final class DispatchWorkSubject<T> extends Subject<T> implements Disposable {

    /**
     * Create an empty {@link DispatchWorkSubject} instance with the given scheduler,
     * default capacity hint ({@link Flowable#bufferSize()}, expected number of items
     * cached until consumption) and delaying errors.
     * @param <T> the input and output value type
     * @param scheduler the scheduler to use for the {@link Observer}s to be notified on
     * @return the new DispatchWorkSubject instance
     */
    public static <T> DispatchWorkSubject<T> create(Scheduler scheduler) {
        return create(scheduler, Flowable.bufferSize(), true);
    }

    /**
     * Create an empty {@link DispatchWorkSubject} instance with the given scheduler,
     * capacity hint (expected number of items cached until consumption) and delaying
     * errors.
     * @param <T> the input and output value type
     * @param scheduler the scheduler to use for the {@link Observer}s to be notified on
     * @param capacityHint the expected number of items to be cached until consumption
     * @return the new DispatchWorkSubject instance
     */
    public static <T> DispatchWorkSubject<T> create(Scheduler scheduler, int capacityHint) {
        return create(scheduler, capacityHint, true);
    }

    /**
     * Create an empty {@link DispatchWorkSubject} instance with the given scheduler,
     * default capacity hint ({@link Flowable#bufferSize()}, expected number of items
     * cached until consumption) and if an error should be delayed.
     * @param <T> the input and output value type
     * @param scheduler the scheduler to use for the {@link Observer}s to be notified on
     * @param delayErrors if true, errors are delivered after items have been consumed
     * @return the new DispatchWorkSubject instance
     */
    public static <T> DispatchWorkSubject<T> create(Scheduler scheduler, boolean delayErrors) {
        return create(scheduler, Flowable.bufferSize(), delayErrors);
    }

    /**
     * Create an empty {@link DispatchWorkSubject} instance with the given scheduler,
     * capacity hint (expected number of items cached until consumption) and if an
     * error should be delayed.
     * @param <T> the input and output value type
     * @param scheduler the scheduler to use for the {@link Observer}s to be notified on
     * @param capacityHint the expected number of items to be cached until consumption
     * @param delayErrors if true, errors are delivered after items have been consumed
     * @return the new DispatchWorkSubject instance
     */
    public static <T> DispatchWorkSubject<T> create(Scheduler scheduler, int capacityHint, boolean delayErrors) {
        return new DispatchWorkSubject<T>(capacityHint, delayErrors, scheduler);
    }

    final SimplePlainQueue<T> queue;

    final AtomicInteger wip;

    final AtomicReference<Disposable> upstream;

    final AtomicReference<Throwable> error;

    final boolean delayErrors;

    final AtomicReference<WorkDisposable<T>[]> observers;

    final Scheduler scheduler;

    @SuppressWarnings("rawtypes")
    static final WorkDisposable[] EMPTY = new WorkDisposable[0];
    @SuppressWarnings("rawtypes")
    static final WorkDisposable[] TERMINATED = new WorkDisposable[0];

    @SuppressWarnings("unchecked")
    DispatchWorkSubject(int capacityHint, boolean delayErrors, Scheduler scheduler) {
        this.queue = new SpmcLinkedArrayQueue<T>(capacityHint);
        this.delayErrors = delayErrors;
        this.wip = new AtomicInteger();
        this.upstream = new AtomicReference<Disposable>();
        this.error = new AtomicReference<Throwable>();
        this.observers = new AtomicReference<WorkDisposable<T>[]>(EMPTY);
        this.scheduler = scheduler;
    }

    @Override
    public void onSubscribe(Disposable d) {
        DisposableHelper.setOnce(upstream, d);
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
    protected void subscribeActual(Observer<? super T> observer) {
        WorkDisposable<T> wd = new WorkDisposable<T>(observer, this, scheduler.createWorker(), delayErrors);
        observer.onSubscribe(wd);
        if (add(wd)) {
            if (wd.isDisposed()) {
                remove(wd);
                return;
            }
        }
        wd.drain();
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(upstream);
    }

    @Override
    public boolean isDisposed() {
        return DisposableHelper.isDisposed(upstream.get());
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
    public boolean hasObservers() {
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

    static final class WorkDisposable<T> extends AtomicInteger implements Disposable, Runnable {

        private static final long serialVersionUID = 7597704795244221647L;

        final Observer<? super T> downstream;

        final DispatchWorkSubject<T> parent;

        final Worker worker;

        final boolean delayErrors;

        volatile boolean disposed;

        WorkDisposable(Observer<? super T> downstream, DispatchWorkSubject<T> parent, Worker worker, boolean delayErrors) {
            this.downstream = downstream;
            this.parent = parent;
            this.worker = worker;
            this.delayErrors = delayErrors;
        }

        @Override
        public void dispose() {
            disposed = true;
            parent.remove(this);
            worker.dispose();
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public void run() {
            int missed = 1;

            SimplePlainQueue<T> q = parent.queue;
            Observer<? super T> downstream = this.downstream;
            AtomicReference<Throwable> error = parent.error;
            boolean delayErrors = this.delayErrors;

            for (;;) {
                for (;;) {
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
                }

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
