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

package hu.akarnokd.rxjava3.async;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * A Future implementation that can be terminated externally.
 *
 * @param <T> the returned value type
 */
final class FutureCompletable<T> extends CountDownLatch implements Future<T> {

    /** The future is still going on. */
    static final int STATE_ACTIVE = 0;

    /** The future has been completed normally. */
    static final int STATE_DONE = 1;

    /** The future has been completed exceptionally. */
    static final int STATE_ERROR = 2;

    /** The future has been cancelled. */
    static final int STATE_CANCELLED = 3;

    final AtomicInteger once;

    Disposable onCancel;

    T value;

    Throwable error;

    FutureCompletable() {
        this(null);
    }

    FutureCompletable(Disposable onCancel) {
        super(1);
        this.onCancel = onCancel;
        this.once = new AtomicInteger();
    }

    /**
     * Complete this Future with the given value (null allowed) and unblock
     * any waiters in {@link Future#get()}.
     * @param value the value to complete
     */
    public void complete(T value) {
        if (once.compareAndSet(0, STATE_DONE)) {
            this.onCancel = null;
            this.value = value;
            countDown();
        }
    }

    /**
     * Complete this Future with the given throwable (not null) and unblock
     * any waiters in {@link Future#get()}.
     * @param error the error to complete exceptionally
     */
    public void completeExceptionally(Throwable error) {
        Objects.requireNonNull(error, "error is null");
        if (once.compareAndSet(0, STATE_ERROR)) {
            this.onCancel = null;
            this.error = error;
            countDown();
        } else {
            RxJavaPlugins.onError(error);
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (once.compareAndSet(0, STATE_CANCELLED)) {
            this.error = new CancellationException();
            countDown();
            Disposable d = this.onCancel;
            this.onCancel = null;
            if (d != null) {
                d.dispose();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        return once.get() == STATE_CANCELLED;
    }

    @Override
    public boolean isDone() {
        return getCount() == 0;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        if (getCount() != 0) {
            await();
        }

        Throwable ex = error;
        if (ex != null) {
            throw new ExecutionException(ex);
        }

        return value;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (getCount() != 0) {
            if (!await(timeout, unit)) {
                throw new TimeoutException();
            }
        }

        Throwable ex = error;
        if (ex != null) {
            throw new ExecutionException(ex);
        }

        return value;
    }
}
