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

package hu.akarnokd.rxjava3.operators;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

/**
 * Maps the terminal signals of the upstream into SingleSources and consumes them.
 * @since 0.20.2
 */
final class CompletableFlatMapSignalSingle<R> extends Single<R>
implements CompletableConverter<Single<R>> {

    final Completable source;

    final Supplier<? extends SingleSource<? extends R>> onCompleteHandler;

    final Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorHandler;

    CompletableFlatMapSignalSingle(Completable source,
            Supplier<? extends SingleSource<? extends R>> onCompleteHandler,
            Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorHandler) {
        this.source = source;
        this.onCompleteHandler = onCompleteHandler;
        this.onErrorHandler = onErrorHandler;
    }

    @Override
    public Single<R> apply(Completable t) {
        return new CompletableFlatMapSignalSingle<>(t, onCompleteHandler, onErrorHandler);
    }

    @Override
    protected void subscribeActual(SingleObserver<? super R> observer) {
        source.subscribe(new FlatMapSignalConsumer<>(observer, onCompleteHandler, onErrorHandler));
    }

    static final class FlatMapSignalConsumer<R>
    implements CompletableObserver, Disposable {

        final SignalConsumer<R> consumer;

        final Supplier<? extends SingleSource<? extends R>> onCompleteHandler;

        final Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorHandler;

        FlatMapSignalConsumer(SingleObserver<? super R> downstream,
                Supplier<? extends SingleSource<? extends R>> onCompleteHandler,
                Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorHandler) {
            this.consumer = new SignalConsumer<>(downstream);
            this.onCompleteHandler = onCompleteHandler;
            this.onErrorHandler = onErrorHandler;
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(consumer);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(consumer.get());
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(consumer.get(), d)) {
                consumer.lazySet(d);
                consumer.downstream.onSubscribe(this);
            }
        }

        @Override
        public void onComplete() {
            SingleSource<? extends R> next;

            try {
                next = Objects.requireNonNull(onCompleteHandler.get(), "The onCompleteHandler returned a null SingleSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                consumer.onError(ex);
                return;
            }

            next.subscribe(consumer);
        }

        @Override
        public void onError(Throwable e) {
            SingleSource<? extends R> next;

            try {
                next = Objects.requireNonNull(onErrorHandler.apply(e), "The onErrorHandler returned a null SingleSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                consumer.onError(ex);
                return;
            }

            next.subscribe(consumer);
        }

        static final class SignalConsumer<R> extends AtomicReference<Disposable>
        implements SingleObserver<R> {

            private static final long serialVersionUID = 314442824941893429L;

            final SingleObserver<? super R> downstream;

            SignalConsumer(SingleObserver<? super R> downstream) {
                this.downstream = downstream;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.replace(this, d);
            }

            @Override
            public void onSuccess(R t) {
                downstream.onSuccess(t);
            }

            @Override
            public void onError(Throwable e) {
                downstream.onError(e);
            }
        }
    }
}
