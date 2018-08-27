/*
 * Copyright 2016-2018 David Karnok
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
 * Map the success value of the upstream into a Perhaps and emit
 * its signals.
 *
 * @param <T> the upstream value type
 * @param <R> the result value type
 */
final class PerhapsFlatMap<T, R> extends Perhaps<R> {

    final Perhaps<T> source;

    final Function<? super T, ? extends Perhaps<? extends R>> mapper;

    PerhapsFlatMap(Perhaps<T> source, Function<? super T, ? extends Perhaps<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new FlatMapSubscriber<T, R>(s, mapper));
    }

    static final class FlatMapSubscriber<T, R> extends DeferredScalarSubscription<R>
    implements Subscriber<T> {

        private static final long serialVersionUID = 1417117475410404413L;

        final Function<? super T, ? extends Perhaps<? extends R>> mapper;

        final InnerSubscriber inner;

        Subscription s;

        boolean hasValue;

        FlatMapSubscriber(Subscriber<? super R> actual,
                Function<? super T, ? extends Perhaps<? extends R>> mapper) {
            super(actual);
            this.mapper = mapper;
            this.inner = new InnerSubscriber();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                downstream.onSubscribe(this);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            hasValue = true;
            Perhaps<? extends R> ph;

            try {
                ph = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null Perhaps");
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
            s.cancel();
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
