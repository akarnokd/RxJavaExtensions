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

package hu.akarnokd.rxjava3.basetypes;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.*;

/**
 * Map the signals of the upstream into a Perhaps and emit
 * its signals.
 *
 * @param <T> the upstream value type
 * @param <R> the result value type
 */
final class PerhapsFlatMapSignal<T, R> extends Perhaps<R> {

    final Perhaps<T> source;

    final Function<? super T, ? extends Perhaps<? extends R>> onSuccessMapper;

    final Function<? super Throwable, ? extends Perhaps<? extends R>> onErrorMapper;

    final Supplier<? extends Perhaps<? extends R>> onCompleteMapper;

    PerhapsFlatMapSignal(Perhaps<T> source,
            Function<? super T, ? extends Perhaps<? extends R>> onSuccessMapper,
            Function<? super Throwable, ? extends Perhaps<? extends R>> onErrorMapper,
                    Supplier<? extends Perhaps<? extends R>> onCompleteMapper
   ) {
        this.source = source;
        this.onSuccessMapper = onSuccessMapper;
        this.onErrorMapper = onErrorMapper;
        this.onCompleteMapper = onCompleteMapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new FlatMapSubscriber<T, R>(s, onSuccessMapper, onErrorMapper, onCompleteMapper));
    }

    static final class FlatMapSubscriber<T, R> extends DeferredScalarSubscription<R>
    implements Subscriber<T> {

        private static final long serialVersionUID = 1417117475410404413L;

        final Function<? super T, ? extends Perhaps<? extends R>> onSuccessMapper;

        final Function<? super Throwable, ? extends Perhaps<? extends R>> onErrorMapper;

        final Supplier<? extends Perhaps<? extends R>> onCompleteMapper;

        final InnerSubscriber inner;

        Subscription upstream;

        boolean hasValue;

        FlatMapSubscriber(Subscriber<? super R> downstream,
                Function<? super T, ? extends Perhaps<? extends R>> onSuccessMapper,
                        Function<? super Throwable, ? extends Perhaps<? extends R>> onErrorMapper,
                                Supplier<? extends Perhaps<? extends R>> onCompleteMapper) {
            super(downstream);
            this.onSuccessMapper = onSuccessMapper;
            this.onErrorMapper = onErrorMapper;
            this.onCompleteMapper = onCompleteMapper;
            this.inner = new InnerSubscriber();
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
            Perhaps<? extends R> ph;

            try {
                ph = Objects.requireNonNull(onSuccessMapper.apply(t), "The onSuccessMapper returned a null Perhaps");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }

            ph.subscribe(inner);
        }

        @Override
        public void onError(Throwable t) {
            Perhaps<? extends R> ph;
            try {
                ph = Objects.requireNonNull(onErrorMapper.apply(t), "The onErrorMapper returned a null Perhaps");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }
            ph.subscribe(inner);
        }

        @Override
        public void onComplete() {
            if (!hasValue) {
                Perhaps<? extends R> ph;
                try {
                    ph = Objects.requireNonNull(onCompleteMapper.get(), "The onCompleteMapper returned a null Perhaps");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    downstream.onError(ex);
                    return;
                }
                ph.subscribe(inner);
            }
        }

        void innerNext(R t) {
            value = t;
        }

        void innerError(Throwable ex) {
            downstream.onError(ex);
        }

        void innerComplete() {
            R v = value;
            if (v != null) {
                complete(v);
            } else {
                downstream.onComplete();
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            upstream.cancel();
            SubscriptionHelper.cancel(inner);
        }

        final class InnerSubscriber extends AtomicReference<Subscription>
        implements Subscriber<R> {

            private static final long serialVersionUID = -7349825169192389387L;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(R t) {
                innerNext(t);
            }

            @Override
            public void onError(Throwable t) {
                innerError(t);
            }

            @Override
            public void onComplete() {
                innerComplete();
            }
        }
    }
}
