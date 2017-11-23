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

package hu.akarnokd.rxjava2.consumers;

import io.reactivex.Flowable;
import io.reactivex.disposables.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;

/**
 * Utility methods for creating and using consumers {@link io.reactivex.Flowable}s.
 * @since 0.18.0
 */
public final class FlowableConsumers {

    /** Utility class. */
    private FlowableConsumers() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Wraps the given onXXX callbacks into a {@code Disposable} {@code Subscriber},
     * adds it to the given {@code CompositeDisposable} and ensures, that if the upstream
     * completes or this particlular Disposable is disposed, the Subscriber is removed
     * from the given composite.
     * <p>
     * The Subscriber will be removed after the callback for the terminal event has been invoked.
     * @param <T> the value type
     * @param source the source Flowable to subscribe to.
     * @param composite the composite Disposable to add and remove the created Disposable Subscriber
     * @param onNext the callback for upstream items
     * @return the Disposable that allows disposing the particular subscription.
     */
    public static <T> Disposable subscribeAutoDispose(
            Flowable<T> source,
            CompositeDisposable composite,
            Consumer<? super T> onNext) {
        ObjectHelper.requireNonNull(source, "source is null");
        ObjectHelper.requireNonNull(composite, "composite is null");
        ObjectHelper.requireNonNull(onNext, "onNext is null");

        DisposableAutoReleaseSubscriber<T> subscriber = new DisposableAutoReleaseSubscriber<T>(
                composite, onNext, null, Functions.EMPTY_ACTION);
        composite.add(subscriber);
        source.subscribe(subscriber);
        return subscriber;
    }

    /**
     * Wraps the given onXXX callbacks into a {@code Disposable} {@code Subscriber},
     * adds it to the given {@code CompositeDisposable} and ensures, that if the upstream
     * completes or this particlular Disposable is disposed, the Subscriber is removed
     * from the given composite.
     * <p>
     * The Subscriber will be removed after the callback for the terminal event has been invoked.
     * @param <T> the value type
     * @param source the source Flowable to subscribe to.
     * @param composite the composite Disposable to add and remove the created Disposable Subscriber
     * @param onNext the callback for upstream items
     * @param onError the callback for an upstream error if any
     * @return the Disposable that allows disposing the particular subscription.
     */
    public static <T> Disposable subscribeAutoDispose(
            Flowable<T> source,
            CompositeDisposable composite,
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError) {
        ObjectHelper.requireNonNull(source, "source is null");
        ObjectHelper.requireNonNull(composite, "composite is null");
        ObjectHelper.requireNonNull(onNext, "onNext is null");
        ObjectHelper.requireNonNull(onError, "onError is null");

        DisposableAutoReleaseSubscriber<T> subscriber = new DisposableAutoReleaseSubscriber<T>(
                composite, onNext, onError, Functions.EMPTY_ACTION);
        composite.add(subscriber);
        source.subscribe(subscriber);
        return subscriber;
    }

    /**
     * Wraps the given onXXX callbacks into a {@code Disposable} {@code Subscriber},
     * adds it to the given {@code CompositeDisposable} and ensures, that if the upstream
     * completes or this particlular Disposable is disposed, the Subscriber is removed
     * from the given composite.
     * <p>
     * The Subscriber will be removed after the callback for the terminal event has been invoked.
     * @param <T> the value type
     * @param source the source Flowable to subscribe to.
     * @param composite the composite Disposable to add and remove the created Disposable Subscriber
     * @param onNext the callback for upstream items
     * @param onError the callback for an upstream error if any
     * @param onComplete the callback for the upstream completion if any
     * @return the Disposable that allows disposing the particular subscription.
     */
    public static <T> Disposable subscribeAutoDispose(
            Flowable<T> source,
            CompositeDisposable composite,
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Action onComplete) {
        ObjectHelper.requireNonNull(source, "source is null");
        ObjectHelper.requireNonNull(composite, "composite is null");
        ObjectHelper.requireNonNull(onNext, "onNext is null");
        ObjectHelper.requireNonNull(onError, "onError is null");
        ObjectHelper.requireNonNull(onComplete, "onComplete is null");

        DisposableAutoReleaseSubscriber<T> subscriber = new DisposableAutoReleaseSubscriber<T>(
                composite, onNext, onError, onComplete);
        composite.add(subscriber);
        source.subscribe(subscriber);
        return subscriber;
    }
}
