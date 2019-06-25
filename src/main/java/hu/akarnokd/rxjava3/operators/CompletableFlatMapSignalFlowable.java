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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Maps the terminal signals of the upstream into ObservableSources and consumes them.
 * @since 0.20.2
 */
final class CompletableFlatMapSignalFlowable<R> extends Flowable<R>
implements CompletableConverter<Flowable<R>> {

    final Completable source;

    final Supplier<? extends Publisher<? extends R>> onCompleteHandler;

    final Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler;

    CompletableFlatMapSignalFlowable(Completable source,
            Supplier<? extends Publisher<? extends R>> onCompleteHandler,
            Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler) {
        this.source = source;
        this.onCompleteHandler = onCompleteHandler;
        this.onErrorHandler = onErrorHandler;
    }

    @Override
    public Flowable<R> apply(Completable t) {
        return new CompletableFlatMapSignalFlowable<R>(t, onCompleteHandler, onErrorHandler);
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> subscriber) {
        source.subscribe(new FlatMapSignalConsumer<R>(subscriber, onCompleteHandler, onErrorHandler));
    }

    static final class FlatMapSignalConsumer<R>
    implements CompletableObserver, Subscription {

        final SignalConsumer<R> consumer;

        final Supplier<? extends Publisher<? extends R>> onCompleteHandler;

        final Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler;

        Disposable upstream;

        FlatMapSignalConsumer(Subscriber<? super R> downstream,
                Supplier<? extends Publisher<? extends R>> onCompleteHandler,
                Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler) {
            this.consumer = new SignalConsumer<R>(downstream);
            this.onCompleteHandler = onCompleteHandler;
            this.onErrorHandler = onErrorHandler;
        }

        @Override
        public void cancel() {
            upstream.dispose();
            SubscriptionHelper.cancel(consumer);
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(consumer, consumer.requested, n);
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(upstream, d)) {
                upstream = d;
                consumer.downstream.onSubscribe(this);
            }
        }

        @Override
        public void onComplete() {
            Publisher<? extends R> next;

            try {
                next = ObjectHelper.requireNonNull(onCompleteHandler.get(), "The onCompleteHandler returned a null Publisher");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                consumer.onError(ex);
                return;
            }

            next.subscribe(consumer);
        }

        @Override
        public void onError(Throwable e) {
            Publisher<? extends R> next;

            try {
                next = ObjectHelper.requireNonNull(onErrorHandler.apply(e), "The onErrorHandler returned a null Publisher");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                consumer.onError(ex);
                return;
            }

            next.subscribe(consumer);
        }

        static final class SignalConsumer<R> extends AtomicReference<Subscription>
        implements FlowableSubscriber<R> {

            private static final long serialVersionUID = 314442824941893429L;

            final Subscriber<? super R> downstream;

            final AtomicLong requested;

            SignalConsumer(Subscriber<? super R> downstream) {
                this.downstream = downstream;
                this.requested = new AtomicLong();
            }

            @Override
            public void onSubscribe(Subscription s) {
                SubscriptionHelper.deferredSetOnce(this, requested, s);
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
