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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.Subject;

/**
 * A {@link Subject} that holds an unbounded queue of items and relays/replays it to
 * a single {@link Observer} at a time, making sure that when the {@code Observer} disposes,
 * any unconsumed items are available for the next {@code Observer}.
 * <p>
 * This {@link Subject} doesn't allow more than one {@link Observer}s at a time.
 * <p>
 * The {@code UnicastWorkSubject} also allows disconnecting from the optional upstream
 * via {@link #dispose()}.
 *
 * @param <T> the input and output value type
 * @since 0.18.8
 */
public final class UnicastWorkSubject<T> extends Subject<T> implements Disposable {

    /**
     * Constructs an empty {@link UnicastWorkSubject} with the default capacity hint
     * (expected number of cached items) of {@link Flowable#bufferSize()} and error delaying behavior.
     * @param <T> the input and output value type
     * @return the new UnicastWorkSubject instance
     * @see #create(int, boolean)
     */
    public static <T> UnicastWorkSubject<T> create() {
        return create(Flowable.bufferSize(), true);
    }

    /**
     * Constructs an empty {@link UnicastWorkSubject} with the given capacity hint
     * (expected number of cached items) and error delaying behavior.
     * @param <T> the input and output value type
     * @param capacityHint the number of items expected to be cached, larger number
     *                     reduces the internal allocation count if the consumer is slow
     * @return the new UnicastWorkSubject instance
     * @see #create(int, boolean)
     */
    public static <T> UnicastWorkSubject<T> create(int capacityHint) {
        return create(capacityHint, true);
    }

    /**
     * Constructs an empty {@link UnicastWorkSubject} with the given capacity hint
     * (expected number of cached items) of {@link Flowable#bufferSize()} and
     * optional error delaying behavior.
     * @param <T> the input and output value type
     * @param delayErrors if true, errors are emitted last
     * @return the new UnicastWorkSubject instance
     * @see #create(int, boolean)
     */
    public static <T> UnicastWorkSubject<T> create(boolean delayErrors) {
        return create(Flowable.bufferSize(), delayErrors);
    }

    /**
     * Constructs an empty {@link UnicastWorkSubject} with the given capacity hint
     * (expected number of cached items) and optional error delaying behavior.
     * @param <T> the input and output value type
     * @param capacityHint the number of items expected to be cached, larger number
     *                     reduces the internal allocation count if the consumer is slow
     * @param delayErrors if true, errors are emitted last
     * @return the new UnicastWorkSubject instance
     */
    public static <T> UnicastWorkSubject<T> create(int capacityHint, boolean delayErrors) {
        return new UnicastWorkSubject<T>(capacityHint, delayErrors);
    }

    final SimplePlainQueue<T> queue;

    final boolean delayErrors;

    final AtomicInteger wip;

    final AtomicReference<Disposable> upstream;

    final AtomicReference<Throwable> error;

    final AtomicReference<WorkDisposable> consumer;

    T item;

    UnicastWorkSubject(int capacityHint, boolean delayErrors) {
        this.queue = new SpscLinkedArrayQueue<T>(capacityHint);
        this.delayErrors = delayErrors;
        this.consumer = new AtomicReference<WorkDisposable>();
        this.upstream = new AtomicReference<Disposable>();
        this.wip = new AtomicInteger();
        this.error = new AtomicReference<Throwable>();
    }

    @Override
    public void onSubscribe(Disposable d) {
        DisposableHelper.setOnce(upstream, d);
    }

    @Override
    public void onNext(T t) {
        ObjectHelper.requireNonNull(t, "t is null");
        if (error.get() == null) {
            queue.offer(t);
            drain();
        }
    }

    @Override
    public void onError(Throwable e) {
        ObjectHelper.requireNonNull(e, "e is null");
        if (error.compareAndSet(null, e)) {
            drain();
        } else {
            RxJavaPlugins.onError(e);
        }
    }

    @Override
    public void onComplete() {
        if (error.compareAndSet(null, ExceptionHelper.TERMINATED)) {
            drain();
        }
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        WorkDisposable w = new WorkDisposable(observer);
        observer.onSubscribe(w);

        if (consumer.compareAndSet(null, w)) {
            if (w.get()) {
                consumer.compareAndSet(w, null);
            } else {
                drain();
            }
        } else {
            observer.onError(new IllegalStateException("Only one Observer allowed at a time"));
        }
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(upstream);
        if (error.compareAndSet(null, ExceptionHelper.TERMINATED)) {
            drain();
        }
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
        return consumer.get() != null;
    }

    void remove(WorkDisposable d) {
        consumer.compareAndSet(d, null);
    }

    void drain() {
        if (wip.getAndIncrement() != 0) {
            return;
        }

        int missed = 1;
        AtomicReference<Throwable> error = this.error;
        AtomicReference<WorkDisposable> consumer = this.consumer;
        boolean delayErrors = this.delayErrors;

        for (;;) {

            for (;;) {
                WorkDisposable a = consumer.get();
                if (a != null) {
                    Throwable ex = error.get();
                    boolean d = ex != null;
                    if (d && !delayErrors) {
                        if (ex != ExceptionHelper.TERMINATED) {
                            queue.clear();
                            item = null;
                            if (consumer.compareAndSet(a, null)) {
                                a.downstream.onError(ex);
                            }
                            break;
                        }
                    }

                    T v = item;
                    if (v == null) {
                        v = queue.poll();
                    }
                    boolean empty = v == null;

                    if (d && empty) {
                        if (ex != ExceptionHelper.TERMINATED) {
                            if (consumer.compareAndSet(a, null)) {
                                a.downstream.onError(ex);
                            }
                        } else {
                            if (consumer.compareAndSet(a, null)) {
                                a.downstream.onComplete();
                            }
                        }
                        break;
                    }

                    if (empty) {
                        break;
                    }

                    if (a == consumer.get()) {
                        item = null;
                        a.downstream.onNext(v);
                    }
                } else {
                    break;
                }
            }

            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    final class WorkDisposable extends AtomicBoolean implements Disposable {

        private static final long serialVersionUID = -3574708954225968389L;

        final Observer<? super T> downstream;

        WorkDisposable(Observer<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void dispose() {
            if (compareAndSet(false, true)) {
                remove(this);
            }
        }

        @Override
        public boolean isDisposed() {
            return get();
        }
    }
}
