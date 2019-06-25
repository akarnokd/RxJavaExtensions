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
 * Maps the terminal signals of the upstream into CompletableSources and consumes them.
 * @since 0.20.2
 */
final class CompletableFlatMapSignalCompletable extends Completable
implements CompletableTransformer {

    final Completable source;

    final Supplier<? extends CompletableSource> onCompleteHandler;

    final Function<? super Throwable, ? extends CompletableSource> onErrorHandler;

    CompletableFlatMapSignalCompletable(Completable source,
            Supplier<? extends CompletableSource> onCompleteHandler,
            Function<? super Throwable, ? extends CompletableSource> onErrorHandler) {
        this.source = source;
        this.onCompleteHandler = onCompleteHandler;
        this.onErrorHandler = onErrorHandler;
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        return new CompletableFlatMapSignalCompletable(upstream, onCompleteHandler, onErrorHandler);
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        source.subscribe(new FlatMapSignalConsumer(observer, onCompleteHandler, onErrorHandler));
    }

    static final class FlatMapSignalConsumer
    implements CompletableObserver, Disposable {

        final SignalConsumer consumer;

        final Supplier<? extends CompletableSource> onCompleteHandler;

        final Function<? super Throwable, ? extends CompletableSource> onErrorHandler;

        FlatMapSignalConsumer(CompletableObserver downstream,
                Supplier<? extends CompletableSource> onCompleteHandler,
                Function<? super Throwable, ? extends CompletableSource> onErrorHandler) {
            this.consumer = new SignalConsumer(downstream);
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
            CompletableSource next;

            try {
                next = ObjectHelper.requireNonNull(onCompleteHandler.get(), "The onCompleteHandler returned a null CompletableSource");
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
