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

package hu.akarnokd.rxjava3.basetypes;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Cancellable;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.subscriptions.DeferredScalarSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Create a Perhaps that for each incoming Subscriber calls a callback to
 * emit a sync or async events in a thread-safe, backpressure-aware and
 * cancellation-safe manner.
 * @param <T> the value type
 */
final class PerhapsCreate<T> extends Perhaps<T> {

    final MaybeOnSubscribe<T> onCreate;

    PerhapsCreate(MaybeOnSubscribe<T> onCreate) {
        this.onCreate = onCreate;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        PerhapsEmitter<T> parent = new PerhapsEmitter<>(s);
        s.onSubscribe(parent);

        try {
            onCreate.subscribe(parent);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            parent.onError(ex);
        }
    }

    static final class PerhapsEmitter<T> extends DeferredScalarSubscription<T> implements MaybeEmitter<T> {

        private static final long serialVersionUID = -7149477775653368644L;

        final AtomicReference<Disposable> resource;

        PerhapsEmitter(Subscriber<? super T> downstream) {
            super(downstream);
            this.resource = new AtomicReference<>();
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
        public void onComplete() {
            Disposable d = resource.getAndSet(DisposableHelper.DISPOSED);
            if (d != DisposableHelper.DISPOSED) {

                downstream.onComplete();

                if (d != null) {
                    d.dispose();
                }
            }
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
