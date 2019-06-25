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
 * Maps the signals of the upstream into Publishers and consumes them.
 * @since 0.20.2
 */
final class MaybeFlatMapSignalFlowable<T, R> extends Flowable<R>
implements MaybeConverter<T, Flowable<R>> {

    final Maybe<T> source;

    final Function<? super T, ? extends Publisher<? extends R>> onSuccessHandler;

    final Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler;

    final Supplier<? extends Publisher<? extends R>> onCompleteHandler;

    MaybeFlatMapSignalFlowable(Maybe<T> source,
            Function<? super T, ? extends Publisher<? extends R>> onSuccessHandler,
            Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler,
                    Supplier<? extends Publisher<? extends R>> onCompleteHandler) {
        this.source = source;
        this.onSuccessHandler = onSuccessHandler;
        this.onErrorHandler = onErrorHandler;
        this.onCompleteHandler = onCompleteHandler;
    }

    @Override
    public Flowable<R> apply(Maybe<T> t) {
        return new MaybeFlatMapSignalFlowable<T, R>(t, onSuccessHandler, onErrorHandler, onCompleteHandler);
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> subscriber) {
        source.subscribe(new FlatMapSignalConsumer<T, R>(subscriber, onSuccessHandler, onErrorHandler, onCompleteHandler));
    }

    static final class FlatMapSignalConsumer<T, R>
    implements MaybeObserver<T>, Subscription {

        final SignalConsumer<R> consumer;

        final Function<? super T, ? extends Publisher<? extends R>> onSuccessHandler;

        final Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler;

        final Supplier<? extends Publisher<? extends R>> onCompleteHandler;

        Disposable upstream;

        FlatMapSignalConsumer(
                Subscriber<? super R> downstream,
                Function<? super T, ? extends Publisher<? extends R>> onSuccessHandler,
                Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler,
                        Supplier<? extends Publisher<? extends R>> onCompleteHandler) {
            this.consumer = new SignalConsumer<R>(downstream);
            this.onSuccessHandler = onSuccessHandler;
            this.onErrorHandler = onErrorHandler;
            this.onCompleteHandler = onCompleteHandler;
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
        public void onSuccess(T t) {
            Publisher<? extends R> next;

            try {
                next = ObjectHelper.requireNonNull(onSuccessHandler.apply(t), "The onSuccessHandler returned a null Publisher");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                consumer.onError(ex);
                return;
            }

            next.subscribe(consumer);
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
