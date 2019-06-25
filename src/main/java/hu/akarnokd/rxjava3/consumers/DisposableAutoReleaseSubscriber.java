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

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;

import io.reactivex.FlowableSubscriber;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.observers.LambdaConsumerIntrospection;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Wraps lambda callbacks and when the upstream terminates or this subscriber gets disposed,
 * removes itself from a {@link io.reactivex.disposables.CompositeDisposable}.
 * @param <T> the element type consumed
 * @since 0.18.0
 */
final class DisposableAutoReleaseSubscriber<T>
extends AtomicReference<Subscription>
implements FlowableSubscriber<T>, Disposable, LambdaConsumerIntrospection {

    private static final long serialVersionUID = 8924480688481408726L;

    final AtomicReference<CompositeDisposable> composite;

    final Consumer<? super T> onNext;

    final Consumer<? super Throwable> onError;

    final Action onComplete;

    DisposableAutoReleaseSubscriber(
            CompositeDisposable composite,
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Action onComplete
    ) {
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
        this.composite = new AtomicReference<CompositeDisposable>(composite);
    }

    @Override
    public void onNext(T t) {
        if (get() != SubscriptionHelper.CANCELLED) {
            try {
                onNext.accept(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                get().cancel();
                onError(e);
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        if (get() != SubscriptionHelper.CANCELLED) {
            lazySet(SubscriptionHelper.CANCELLED);
            if (onError != null) {
                try {
                    onError.accept(t);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    RxJavaPlugins.onError(new CompositeException(t, e));
                }
            } else {
                RxJavaPlugins.onError(new OnErrorNotImplementedException(t));
            }
        } else {
            RxJavaPlugins.onError(t);
        }
        removeSelf();
    }

    @Override
    public void onComplete() {
        if (get() != SubscriptionHelper.CANCELLED) {
            lazySet(SubscriptionHelper.CANCELLED);
            try {
                onComplete.run();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaPlugins.onError(e);
            }
        }
        removeSelf();
    }

    @Override
    public void dispose() {
        SubscriptionHelper.cancel(this);
        removeSelf();
    }

    void removeSelf() {
        CompositeDisposable c = composite.getAndSet(null);
        if (c != null) {
            c.delete(this);
        }
    }

    @Override
    public boolean isDisposed() {
        return SubscriptionHelper.CANCELLED == get();
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(this, s)) {
            s.request(Long.MAX_VALUE);
        }
    }

    @Override
    public boolean hasCustomOnError() {
        return onError != null;
    }

}
