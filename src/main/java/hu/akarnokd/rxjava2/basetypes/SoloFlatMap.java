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

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.*;

/**
 * Maps the success value of this Solo into another Solo and
 * emits its signals.
 * @param <T> the value type
 * @param <R> the output type
 */
final class SoloFlatMap<T, R> extends Solo<R> {

    final Solo<T> source;

    final Function<? super T, ? extends Solo<? extends R>> mapper;

    SoloFlatMap(Solo<T> source, Function<? super T, ? extends Solo<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new FlatMapSubscriber<T, R>(s, mapper));
    }

    static final class FlatMapSubscriber<T, R> extends DeferredScalarSubscription<R>
    implements Subscriber<T> {

        private static final long serialVersionUID = -7631998337002592538L;

        final Function<? super T, ? extends Solo<? extends R>> mapper;

        final NextSubscriber nextSubscriber;

        Subscription upstream;

        FlatMapSubscriber(Subscriber<? super R> downstream, Function<? super T, ? extends Solo<? extends R>> mapper) {
            super(downstream);
            this.mapper = mapper;
            this.nextSubscriber = new NextSubscriber();
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
            Solo<? extends R> sp;

            try {
                sp = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null Solo");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }

            sp.subscribe(nextSubscriber);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            // ignored
        }

        @Override
        public void cancel() {
            super.cancel();
            upstream.cancel();
            SubscriptionHelper.cancel(nextSubscriber);
        }

        void nextComplete() {
            complete(value);
        }

        final class NextSubscriber extends AtomicReference<Subscription>
        implements Subscriber<R> {

            private static final long serialVersionUID = 5161815655607865861L;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(R t) {
                FlatMapSubscriber.this.value = t;
            }

            @Override
            public void onError(Throwable t) {
                downstream.onError(t);
            }

            @Override
            public void onComplete() {
                nextComplete();
            }
        }
    }
}
