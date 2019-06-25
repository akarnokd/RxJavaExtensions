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

package hu.akarnokd.rxjava3.consumers;

import io.reactivex.Completable;
import io.reactivex.disposables.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;

/**
 * Utility methods for creating and using consumers {@link io.reactivex.Completable}s.
 * @since 0.18.0
 */
public final class CompletableConsumers {

    /** Utility class. */
    private CompletableConsumers() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Wraps the given onXXX callbacks into a {@code Disposable} {@code CompletableObserver},
     * adds it to the given {@code CompositeDisposable} and ensures, that if the upstream
     * completes or this particlular Disposable is disposed, the CompletableObserver is removed
     * from the given composite.
     * <p>
     * The CompletableObserver will be removed after the callback for the terminal event has been invoked.
     * @param source the source Completable to subscribe to.
     * @param composite the composite Disposable to add and remove the created Disposable CompletableObserver
     * @param onComplete the callback for when the upstream completes
     * @return the Disposable that allows disposing the particular subscription.
     */
    public static Disposable subscribeAutoDispose(
            Completable source,
            CompositeDisposable composite,
            Action onComplete) {
        ObjectHelper.requireNonNull(source, "source is null");
        ObjectHelper.requireNonNull(composite, "composite is null");
        ObjectHelper.requireNonNull(onComplete, "onComplete is null");

        DisposableAutoReleaseMultiObserver<Void> observer = new DisposableAutoReleaseMultiObserver<Void>(
                composite, Functions.emptyConsumer(), null, onComplete);
        composite.add(observer);
        source.subscribe(observer);
        return observer;
    }

    /**
     * Wraps the given onXXX callbacks into a {@code Disposable} {@code CompletableObserver},
     * adds it to the given {@code CompositeDisposable} and ensures, that if the upstream
     * completes or this particlular Disposable is disposed, the CompletableObserver is removed
     * from the given composite.
     * <p>
     * The CompletableObserver will be removed after the callback for the terminal event has been invoked.
     * @param source the source Completable to subscribe to.
     * @param composite the composite Disposable to add and remove the created Disposable CompletableObserver
     * @param onComplete the callback for when the upstream completes
     * @param onError the callback for an upstream error if any
     * @return the Disposable that allows disposing the particular subscription.
     */
    public static Disposable subscribeAutoDispose(
            Completable source,
            CompositeDisposable composite,
            Action onComplete,
            Consumer<? super Throwable> onError) {
        ObjectHelper.requireNonNull(source, "source is null");
        ObjectHelper.requireNonNull(composite, "composite is null");
        ObjectHelper.requireNonNull(onComplete, "onSuccess is null");
        ObjectHelper.requireNonNull(onError, "onError is null");

        DisposableAutoReleaseMultiObserver<Void> observer = new DisposableAutoReleaseMultiObserver<Void>(
                composite, Functions.emptyConsumer(), onError, onComplete);
        composite.add(observer);
        source.subscribe(observer);
        return observer;
    }

}
