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
 * Maps the signals of the upstream into CompletableSources and consumes them.
 * @since 0.20.2
 */
final class MaybeFlatMapSignalCompletable<T> extends Completable
implements MaybeConverter<T, Completable> {

    final Maybe<T> source;

    final Function<? super T, ? extends CompletableSource> onSuccessHandler;

    final Function<? super Throwable, ? extends CompletableSource> onErrorHandler;

    final Supplier<? extends CompletableSource> onCompleteHandler;

    MaybeFlatMapSignalCompletable(Maybe<T> source,
            Function<? super T, ? extends CompletableSource> onSuccessHandler,
            Function<? super Throwable, ? extends CompletableSource> onErrorHandler,
            Supplier<? extends CompletableSource> onCompleteHandler) {
        this.source = source;
        this.onSuccessHandler = onSuccessHandler;
        this.onErrorHandler = onErrorHandler;
        this.onCompleteHandler = onCompleteHandler;
    }

    @Override
    public Completable apply(Maybe<T> t) {
        return new MaybeFlatMapSignalCompletable<>(t, onSuccessHandler, onErrorHandler, onCompleteHandler);
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        source.subscribe(new FlatMapSignalConsumer<T>(observer, onSuccessHandler, onErrorHandler, onCompleteHandler));
    }

    static final class FlatMapSignalConsumer<T>
    implements MaybeObserver<T>, Disposable {

        final SignalConsumer consumer;

        final Function<? super T, ? extends CompletableSource> onSuccessHandler;

        final Function<? super Throwable, ? extends CompletableSource> onErrorHandler;

        final Supplier<? extends CompletableSource> onCompleteHandler;

        FlatMapSignalConsumer(
                CompletableObserver downstream,
                Function<? super T, ? extends CompletableSource> onSuccessHandler,
                Function<? super Throwable, ? extends CompletableSource> onErrorHandler,
                Supplier<? extends CompletableSource> onCompleteHandler) {
            this.consumer = new SignalConsumer(downstream);
            this.onSuccessHandler = onSuccessHandler;
            this.onErrorHandler = onErrorHandler;
            this.onCompleteHandler = onCompleteHandler;
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
        public void onSuccess(T t) {
            CompletableSource next;

            try {
                next = Objects.requireNonNull(onSuccessHandler.apply(t), "The onSuccessHandler returned a null CompletableSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                consumer.onError(ex);
                return;
            }

            next.subscribe(consumer);
        }

        @Override
        public void onComplete() {
            CompletableSource next;

            try {
                next = Objects.requireNonNull(onCompleteHandler.get(), "The onCompleteHandler returned a null CompletableSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                consumer.onError(ex);
                return;
            }

            next.subscribe(consumer);
        }

        @Override
        public void onError(Throwable e) {
            CompletableSource next;

            try {
                next = Objects.requireNonNull(onErrorHandler.apply(e), "The onErrorHandler returned a null CompletableSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                consumer.onError(ex);
                return;
            }

            next.subscribe(consumer);
        }

        static final class SignalConsumer extends AtomicReference<Disposable>
        implements CompletableObserver {

            private static final long serialVersionUID = 314442824941893429L;

            final CompletableObserver downstream;

            SignalConsumer(CompletableObserver downstream) {
                this.downstream = downstream;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.replace(this, d);
            }

            @Override
            public void onComplete() {
                downstream.onComplete();
            }

            @Override
            public void onError(Throwable e) {
                downstream.onError(e);
            }
        }
    }
}
