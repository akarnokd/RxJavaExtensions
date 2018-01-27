/*
 * Copyright 2016-2018 David Karnok
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

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.disposables.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.observers.LambdaConsumerIntrospection;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Wraps lambda callbacks and when the upstream terminates or the observer gets disposed,
 * removes itself from a {@link io.reactivex.disposables.CompositeDisposable}.
 * @since 0.18.0
 */
abstract class AbstractDisposableAutoRelease
extends AtomicReference<Disposable>
implements Disposable, LambdaConsumerIntrospection {

    private static final long serialVersionUID = 8924480688481408726L;

    final AtomicReference<CompositeDisposable> composite;

    final Consumer<? super Throwable> onError;

    final Action onComplete;

    AbstractDisposableAutoRelease(
            CompositeDisposable composite,
            Consumer<? super Throwable> onError,
            Action onComplete
    ) {
        this.onError = onError;
        this.onComplete = onComplete;
        this.composite = new AtomicReference<CompositeDisposable>(composite);
    }

    public final void onError(Throwable t) {
        if (get() != DisposableHelper.DISPOSED) {
            lazySet(DisposableHelper.DISPOSED);
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

    public final void onComplete() {
        if (get() != DisposableHelper.DISPOSED) {
            lazySet(DisposableHelper.DISPOSED);
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
    public final void dispose() {
        DisposableHelper.dispose(this);
        removeSelf();
    }

    final void removeSelf() {
        CompositeDisposable c = composite.getAndSet(null);
        if (c != null) {
            c.delete(this);
        }
    }

    @Override
    public final boolean isDisposed() {
        return DisposableHelper.isDisposed(get());
    }

    public final void onSubscribe(Disposable s) {
        DisposableHelper.setOnce(this, s);
    }

    @Override
    public final boolean hasCustomOnError() {
        return onError != null;
    }

}
