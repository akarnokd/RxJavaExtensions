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
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Maps the signals of the upstream into Publishers and consumes them.
 * @since 0.20.2
 */
final class SingleFlatMapSignalFlowable<T, R> extends Flowable<R>
implements SingleConverter<T, Flowable<R>> {

    final Single<T> source;

    final Function<? super T, ? extends Publisher<? extends R>> onSuccessHandler;

    final Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler;

    SingleFlatMapSignalFlowable(Single<T> source,
            Function<? super T, ? extends Publisher<? extends R>> onSuccessHandler,
            Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler) {
        this.source = source;
        this.onSuccessHandler = onSuccessHandler;
        this.onErrorHandler = onErrorHandler;
    }

    @Override
    public Flowable<R> apply(Single<T> t) {
        return new SingleFlatMapSignalFlowable<>(t, onSuccessHandler, onErrorHandler);
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> subscriber) {
        source.subscribe(new FlatMapSignalConsumer<T, R>(subscriber, onSuccessHandler, onErrorHandler));
    }

    static final class FlatMapSignalConsumer<T, R>
    implements SingleObserver<T>, Subscription {

        final SignalConsumer<R> consumer;

        final Function<? super T, ? extends Publisher<? extends R>> onSuccessHandler;

        final Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler;

        Disposable upstream;

        FlatMapSignalConsumer(
                Subscriber<? super R> downstream,
                Function<? super T, ? extends Publisher<? extends R>> onSuccessHandler,
                Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler) {
            this.consumer = new SignalConsumer<>(downstream);
            this.onSuccessHandler = onSuccessHandler;
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
        public void onSuccess(T t) {
            Publisher<? extends R> next;

            try {
                next = Objects.requireNonNull(onSuccessHandler.apply(t), "The onSuccessHandler returned a null Publisher");
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
                next = Objects.requireNonNull(onErrorHandler.apply(e), "The onErrorHandler returned a null Publisher");
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
