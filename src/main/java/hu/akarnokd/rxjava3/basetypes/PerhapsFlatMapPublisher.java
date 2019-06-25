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

package hu.akarnokd.rxjava3.basetypes;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Map the success value of the upstream into a Publisher and emit
 * all of its signals.
 *
 * @param <T> the upstream value type
 * @param <R> the result value type
 */
final class PerhapsFlatMapPublisher<T, R> extends Flowable<R> {

    final Perhaps<T> source;

    final Function<? super T, ? extends Publisher<? extends R>> mapper;

    PerhapsFlatMapPublisher(Perhaps<T> source, Function<? super T, ? extends Publisher<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new FlatMapSubscriber<T, R>(s, mapper));
    }

    static final class FlatMapSubscriber<T, R> extends AtomicLong
    implements Subscriber<T>, Subscription {

        private static final long serialVersionUID = 1417117475410404413L;

        final Subscriber<? super R> downstream;

        final Function<? super T, ? extends Publisher<? extends R>> mapper;

        final InnerSubscriber inner;

        Subscription upstream;

        boolean hasValue;

        FlatMapSubscriber(Subscriber<? super R> downstream,
                Function<? super T, ? extends Publisher<? extends R>> mapper) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.inner = new InnerSubscriber(downstream);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            hasValue = true;
            Publisher<? extends R> ph;

            try {
                ph = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null Publisher");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }

            ph.subscribe(inner);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (!hasValue) {
                downstream.onComplete();
            }
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(inner, this, n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
            SubscriptionHelper.cancel(inner);
        }

        final class InnerSubscriber extends AtomicReference<Subscription>
        implements Subscriber<R> {

            private static final long serialVersionUID = -7407027791505806997L;

            final Subscriber<? super R> downstream;

            InnerSubscriber(Subscriber<? super R> downstream) {
                this.downstream = downstream;
            }

            @Override
            public void onSubscribe(Subscription s) {
                SubscriptionHelper.deferredSetOnce(this, FlatMapSubscriber.this, s);
            }

            @Override
            public void onNext(R t) {
                downstream.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                downstream.onError(t);
            }

            @Override
            public void onComplete() {
                downstream.onComplete();
            }
        }
    }
}
