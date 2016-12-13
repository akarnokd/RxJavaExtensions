/*
 * Copyright 2016 David Karnok
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

package hu.akarnokd.rxjava2.basetypes;

import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Cancellable;
import io.reactivex.internal.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Call a create action with an abstraction of the incoming subscriber to allow
 * thread-safe and cancellation-safe terminal event emission.
 */
final class NonoCreate extends Nono {

    final CompletableOnSubscribe onCreate;

    NonoCreate(CompletableOnSubscribe onCreate) {
        this.onCreate = onCreate;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        NonoEmitter parent = new NonoEmitter(s);
        s.onSubscribe(parent);

        try {
            onCreate.subscribe(parent);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            parent.onError(ex);
        }
    }

    static final class NonoEmitter extends BasicRefQueueSubscription<Void, Disposable> implements CompletableEmitter {

        private static final long serialVersionUID = -7351447810798891941L;

        final Subscriber<? super Void> actual;

        NonoEmitter(Subscriber<? super Void> actual) {
            this.actual = actual;
        }

        @Override
        public void cancel() {
            DisposableHelper.dispose(this);
        }

        @Override
        public void onComplete() {
            Disposable d = getAndSet(DisposableHelper.DISPOSED);
            if (d != DisposableHelper.DISPOSED) {

                actual.onComplete();

                if (d != null) {
                    d.dispose();
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            Disposable d = getAndSet(DisposableHelper.DISPOSED);
            if (d != DisposableHelper.DISPOSED) {
                actual.onError(t);

                if (d != null) {
                    d.dispose();
                }
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void setDisposable(Disposable d) {
            DisposableHelper.set(this, d);
        }

        @Override
        public void setCancellable(Cancellable c) {
            setDisposable(new CancellableDisposable(c));
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }
    }
}
