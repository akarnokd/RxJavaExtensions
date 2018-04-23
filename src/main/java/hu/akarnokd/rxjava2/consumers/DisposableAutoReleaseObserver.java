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

import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * Wraps lambda callbacks and when the upstream terminates or this observer gets disposed,
 * removes itself from a {@link io.reactivex.disposables.CompositeDisposable}.
 * @param <T> the element type consumed
 * @since 0.18.0
 */
final class DisposableAutoReleaseObserver<T>
extends AbstractDisposableAutoRelease
implements Observer<T> {

    private static final long serialVersionUID = 8924480688481408726L;

    final Consumer<? super T> onNext;

    DisposableAutoReleaseObserver(
            CompositeDisposable composite,
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Action onComplete
    ) {
        super(composite, onError, onComplete);
        this.onNext = onNext;
    }

    @Override
    public void onNext(T t) {
        if (get() != DisposableHelper.DISPOSED) {
            try {
                onNext.accept(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                get().dispose();
                onError(e);
            }
        }
    }

}
