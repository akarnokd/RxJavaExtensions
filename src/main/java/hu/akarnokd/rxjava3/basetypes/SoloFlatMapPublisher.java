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

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.ObjectHelper;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Maps the success value of this Solo into a Publisher and
 * emits its signals.
 *
 * @param <T> the upstream value type
 * @param <R> the downstream value type
 */
final class SoloFlatMapPublisher<T, R> extends Flowable<R> {

    final Solo<T> source;

    final Function<? super T, ? extends Publisher<? extends R>> mapper;

    SoloFlatMapPublisher(Solo<T> source, Function<? super T, ? extends Publisher<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new FlatMapPublisherSubscriber<T, R>(s, mapper));
    }

    static final class FlatMapPublisherSubscriber<T, R>
    extends AtomicReference<Subscription>
    implements Subscriber<T>, Subscription {

        private static final long serialVersionUID = -3958458353930920644L;

        final Subscriber<? super R> downstream;

        final InnerSubscriber requested;

        final Function<? super T, ? extends Publisher<? extends R>> mapper;

        Subscription upstream;

        FlatMapPublisherSubscriber(Subscriber<? super R> downstream, Function<? super T, ? extends Publisher<? extends R>> mapper) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.requested = new InnerSubscriber(downstream);
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
            Publisher<? extends R> p;

            try {
                p = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null Publisher");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }

            p.subscribe(requested);
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
            upstream.cancel();
            SubscriptionHelper.cancel(this);
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(this, requested, n);
        }

        final class InnerSubscriber extends AtomicLong implements Subscriber<R> {

            private static final long serialVersionUID = 2003600104149898338L;

            final Subscriber<? super R> downstream;

            InnerSubscriber(Subscriber<? super R> downstream) {
                this.downstream = downstream;
            }

            @Override
            public void onSubscribe(Subscription s) {
                SubscriptionHelper.deferredSetOnce(FlatMapPublisherSubscriber.this, requested, s);
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
