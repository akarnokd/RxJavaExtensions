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

package hu.akarnokd.rxjava2.basetypes;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Map the terminal events of a Nono into Publisher and stream its events.
 *
 * @param <T> the value type
 */
final class NonoFlatMapSignal<T> extends Flowable<T> {

    final Nono source;

    final Function<? super Throwable, ? extends Publisher<? extends T>> onErrorMapper;

    final Callable<? extends Publisher<? extends T>> onCompleteMapper;

    NonoFlatMapSignal(Nono source, Function<? super Throwable, ? extends Publisher<? extends T>> onErrorMapper,
            Callable<? extends Publisher<? extends T>> onCompleteMapper) {
        this.source = source;
        this.onErrorMapper = onErrorMapper;
        this.onCompleteMapper = onCompleteMapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new FlatMapSignalSubscriber<T>(s, onErrorMapper, onCompleteMapper));
    }

    static final class FlatMapSignalSubscriber<T> extends AtomicReference<Subscription>
    implements Subscriber<Void>, Subscription {

        private static final long serialVersionUID = -1838187298176717779L;

        final Subscriber<? super T> downstream;

        final Function<? super Throwable, ? extends Publisher<? extends T>> onErrorMapper;

        final Callable<? extends Publisher<? extends T>> onCompleteMapper;

        final AtomicLong requested;

        Subscription upstream;

        FlatMapSignalSubscriber(Subscriber<? super T> downstream,
                Function<? super Throwable, ? extends Publisher<? extends T>> onErrorMapper,
                Callable<? extends Publisher<? extends T>> onCompleteMapper) {
            this.downstream = downstream;
            this.onErrorMapper = onErrorMapper;
            this.onCompleteMapper = onCompleteMapper;
            this.requested = new AtomicLong();
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(this, requested, n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
            SubscriptionHelper.cancel(this);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(Void t) {
            // never called
        }

        @Override
        public void onError(Throwable t) {
            Publisher<? extends T> p;
            try {
                p = ObjectHelper.requireNonNull(onErrorMapper.apply(t), "The onErrorMapper returned a null Nono");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }

            p.subscribe(new InnerSubscriber());
        }

        @Override
        public void onComplete() {
            Publisher<? extends T> p;
            try {
                p = ObjectHelper.requireNonNull(onCompleteMapper.call(), "The onCompleteMapper returned a null Nono");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }

            p.subscribe(new InnerSubscriber());
        }

        void innerSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(this, requested, s);
        }

        final class InnerSubscriber implements Subscriber<T> {

            final Subscriber<? super T> a = downstream;

            @Override
            public void onSubscribe(Subscription s) {
                innerSubscribe(s);
            }

            @Override
            public void onNext(T t) {
                a.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                a.onError(t);
            }

            @Override
            public void onComplete() {
                a.onComplete();
            }
        }
    }
}
