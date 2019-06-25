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

package hu.akarnokd.rxjava3.operators;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Maps the signals of the upstream into MaybeSources and consumes them.
 * @since 0.20.2
 */
final class SingleFlatMapSignalMaybe<T, R> extends Maybe<R>
implements SingleConverter<T, Maybe<R>> {

    final Single<T> source;

    final Function<? super T, ? extends MaybeSource<? extends R>> onSuccessHandler;

    final Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorHandler;

    SingleFlatMapSignalMaybe(Single<T> source,
            Function<? super T, ? extends MaybeSource<? extends R>> onSuccessHandler,
            Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorHandler) {
        this.source = source;
        this.onSuccessHandler = onSuccessHandler;
        this.onErrorHandler = onErrorHandler;
    }

    @Override
    public Maybe<R> apply(Single<T> t) {
        return new SingleFlatMapSignalMaybe<T, R>(t, onSuccessHandler, onErrorHandler);
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super R> observer) {
        source.subscribe(new FlatMapSignalConsumer<T, R>(observer, onSuccessHandler, onErrorHandler));
    }

    static final class FlatMapSignalConsumer<T, R>
    implements SingleObserver<T>, Disposable {

        final SignalConsumer<R> consumer;

        final Function<? super T, ? extends MaybeSource<? extends R>> onSuccessHandler;

        final Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorHandler;

        FlatMapSignalConsumer(
                MaybeObserver<? super R> downstream,
                Function<? super T, ? extends MaybeSource<? extends R>> onSuccessHandler,
                Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorHandler) {
            this.consumer = new SignalConsumer<R>(downstream);
            this.onSuccessHandler = onSuccessHandler;
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
        public void onSuccess(T t) {
            MaybeSource<? extends R> next;

            try {
                next = ObjectHelper.requireNonNull(onSuccessHandler.apply(t), "The onSuccessHandler returned a null MaybeSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                consumer.onError(ex);
                return;
            }

            next.subscribe(consumer);
        }

        @Override
        public void onError(Throwable e) {
            MaybeSource<? extends R> next;

            try {
                next = ObjectHelper.requireNonNull(onErrorHandler.apply(e), "The onErrorHandler returned a null MaybeSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                consumer.onError(ex);
                return;
            }

            next.subscribe(consumer);
        }

        static final class SignalConsumer<R> extends AtomicReference<Disposable>
        implements MaybeObserver<R> {

            private static final long serialVersionUID = 314442824941893429L;

            final MaybeObserver<? super R> downstream;

            SignalConsumer(MaybeObserver<? super R> downstream) {
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

            @Override
            public void onComplete() {
                downstream.onComplete();
            }
        }
    }
}
