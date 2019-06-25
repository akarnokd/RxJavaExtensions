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
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Maps the terminal signals of the upstream into ObservableSources and consumes them.
 * @since 0.20.2
 */
final class CompletableFlatMapSignalObservable<R> extends Observable<R>
implements CompletableConverter<Observable<R>> {

    final Completable source;

    final Supplier<? extends ObservableSource<? extends R>> onCompleteHandler;

    final Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorHandler;

    CompletableFlatMapSignalObservable(Completable source,
            Supplier<? extends ObservableSource<? extends R>> onCompleteHandler,
            Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorHandler) {
        this.source = source;
        this.onCompleteHandler = onCompleteHandler;
        this.onErrorHandler = onErrorHandler;
    }

    @Override
    public Observable<R> apply(Completable t) {
        return new CompletableFlatMapSignalObservable<R>(t, onCompleteHandler, onErrorHandler);
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) {
        source.subscribe(new FlatMapSignalConsumer<R>(observer, onCompleteHandler, onErrorHandler));
    }

    static final class FlatMapSignalConsumer<R>
    implements CompletableObserver, Disposable {

        final SignalConsumer<R> consumer;

        final Supplier<? extends ObservableSource<? extends R>> onCompleteHandler;

        final Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorHandler;

        FlatMapSignalConsumer(Observer<? super R> downstream,
                Supplier<? extends ObservableSource<? extends R>> onCompleteHandler,
                Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorHandler) {
            this.consumer = new SignalConsumer<R>(downstream);
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
            ObservableSource<? extends R> next;

            try {
                next = ObjectHelper.requireNonNull(onCompleteHandler.get(), "The onCompleteHandler returned a null ObservableSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                consumer.onError(ex);
                return;
            }

            next.subscribe(consumer);
        }

        @Override
        public void onError(Throwable e) {
            ObservableSource<? extends R> next;

            try {
                next = ObjectHelper.requireNonNull(onErrorHandler.apply(e), "The onErrorHandler returned a null ObservableSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                consumer.onError(ex);
                return;
            }

            next.subscribe(consumer);
        }

        static final class SignalConsumer<R> extends AtomicReference<Disposable>
        implements Observer<R> {

            private static final long serialVersionUID = 314442824941893429L;

            final Observer<? super R> downstream;

            SignalConsumer(Observer<? super R> downstream) {
                this.downstream = downstream;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.replace(this, d);
            }

            @Override
            public void onNext(R t) {
                downstream.onNext(t);
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
