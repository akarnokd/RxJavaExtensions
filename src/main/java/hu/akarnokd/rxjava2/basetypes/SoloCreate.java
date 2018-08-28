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

package hu.akarnokd.rxjava2.basetypes;

import java.util.concurrent.atomic.*;

import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Cancellable;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.subscriptions.DeferredScalarSubscription;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Create a Solo that for each incoming Subscriber calls a callback to
 * emit a sync or async events in a thread-safe, backpressure-aware and
 * cancellation-safe manner.
 * @param <T> the value type
 */
final class SoloCreate<T> extends Solo<T> {

    final SingleOnSubscribe<T> onCreate;

    SoloCreate(SingleOnSubscribe<T> onCreate) {
        this.onCreate = onCreate;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        SoloEmitter<T> parent = new SoloEmitter<T>(s);
        s.onSubscribe(parent);

        try {
            onCreate.subscribe(parent);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            parent.onError(ex);
        }
    }

    static final class SoloEmitter<T> extends DeferredScalarSubscription<T> implements SingleEmitter<T> {

        private static final long serialVersionUID = -7149477775653368644L;

        final AtomicReference<Disposable> resource;

        SoloEmitter(Subscriber<? super T> downstream) {
            super(downstream);
            this.resource = new AtomicReference<Disposable>();
        }

        @Override
        public void onSuccess(T t) {
            Disposable d = resource.getAndSet(DisposableHelper.DISPOSED);
            if (d != DisposableHelper.DISPOSED) {

                complete(t);

                if (d != null) {
                    d.dispose();
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!tryOnError(t)) {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public boolean tryOnError(Throwable t) {
            Disposable d = resource.getAndSet(DisposableHelper.DISPOSED);
            if (d != DisposableHelper.DISPOSED) {
                downstream.onError(t);

                if (d != null) {
                    d.dispose();
                }
                return true;
            }
            return false;
        }

        @Override
        public void setDisposable(Disposable d) {
            DisposableHelper.set(resource, d);
        }

        @Override
        public void setCancellable(Cancellable c) {
            setDisposable(new CancellableDisposable(c));
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(resource.get());
        }
    }
}
