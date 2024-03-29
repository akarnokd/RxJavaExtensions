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

package hu.akarnokd.rxjava3.consumers;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Wraps lambda callbacks and when the upstream terminates or this (Single | Maybe | Completable)
 * observer gets disposed, removes itself from a {@link io.reactivex.rxjava3.disposables.CompositeDisposable}.
 * @param <T> the element type consumed
 * @since 0.18.0
 */
final class DisposableAutoReleaseMultiObserver<T>
extends AbstractDisposableAutoRelease
implements SingleObserver<T>, MaybeObserver<T>, CompletableObserver {

    private static final long serialVersionUID = 8924480688481408726L;

    final Consumer<? super T> onSuccess;

    DisposableAutoReleaseMultiObserver(
            CompositeDisposable composite,
            Consumer<? super T> onSuccess,
            Consumer<? super Throwable> onError,
            Action onComplete
    ) {
        super(composite, onError, onComplete);
        this.onSuccess = onSuccess;
    }

    @Override
    public void onSuccess(T t) {
        if (get() != DisposableHelper.DISPOSED) {
            lazySet(DisposableHelper.DISPOSED);
            try {
                onSuccess.accept(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaPlugins.onError(e);
            }
        }
        removeSelf();
    }

}
