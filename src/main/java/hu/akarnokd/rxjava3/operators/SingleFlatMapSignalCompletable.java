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
 * Maps the signals of the upstream into CompletableSources and consumes them.
 * @since 0.20.2
 */
final class SingleFlatMapSignalCompletable<T> extends Completable
implements SingleConverter<T, Completable> {

    final Single<T> source;

    final Function<? super T, ? extends CompletableSource> onSuccessHandler;

    final Function<? super Throwable, ? extends CompletableSource> onErrorHandler;

    SingleFlatMapSignalCompletable(Single<T> source,
            Function<? super T, ? extends CompletableSource> onSuccessHandler,
            Function<? super Throwable, ? extends CompletableSource> onErrorHandler) {
        this.source = source;
        this.onSuccessHandler = onSuccessHandler;
        this.onErrorHandler = onErrorHandler;
    }

    @Override
    public Completable apply(Single<T> t) {
        return new SingleFlatMapSignalCompletable<T>(t, onSuccessHandler, onErrorHandler);
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        source.subscribe(new FlatMapSignalConsumer<T>(observer, onSuccessHandler, onErrorHandler));
    }

    static final class FlatMapSignalConsumer<T>
    implements SingleObserver<T>, Disposable {

        final SignalConsumer consumer;

        final Function<? super T, ? extends CompletableSource> onSuccessHandler;

        final Function<? super Throwable, ? extends CompletableSource> onErrorHandler;

        FlatMapSignalConsumer(
                CompletableObserver downstream,
                Function<? super T, ? extends CompletableSource> onSuccessHandler,
                Function<? super Throwable, ? extends CompletableSource> onErrorHandler) {
            this.consumer = new SignalConsumer(downstream);
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
            CompletableSource next;

            try {
                next = ObjectHelper.requireNonNull(onSuccessHandler.apply(t), "The onSuccessHandler returned a null CompletableSource");
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
                next = ObjectHelper.requireNonNull(onErrorHandler.apply(e), "The onErrorHandler returned a null CompletableSource");
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
