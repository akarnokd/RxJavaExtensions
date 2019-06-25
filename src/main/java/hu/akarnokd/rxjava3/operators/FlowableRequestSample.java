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
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Issues {@link Subscription#request(long)}(1) when the other source Publisher signals an onNext item.
 *
 * @param <T> the item type
 * @since 0.18.6
 */
final class FlowableRequestSample<T> extends Flowable<T> implements FlowableTransformer<T, T> {

    final Flowable<T> source;

    final Publisher<?> other;

    FlowableRequestSample(Flowable<T> source, Publisher<?> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableRequestSample<T>(upstream, other);
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        RequestSample<T> parent = new RequestSample<T>(s);
        s.onSubscribe(parent);
        other.subscribe(parent.otherConsumer);
        source.subscribe(parent);
    }

    static final class RequestSample<T>
    implements FlowableSubscriber<T>, Subscription {

        final Subscriber<? super T> downstream;

        final AtomicLong downstreamRequests;

        final AtomicLong upstreamRequests;

        final AtomicReference<Subscription> upstream;

        final OtherConsumer otherConsumer;

        final AtomicInteger wip;

        final AtomicThrowable error;

        long emitted;

        boolean done;

        RequestSample(Subscriber<? super T> downstream) {
            this.downstream = downstream;
            this.downstreamRequests = new AtomicLong();
            this.upstreamRequests = new AtomicLong();
            this.upstream = new AtomicReference<Subscription>();
            this.otherConsumer = new OtherConsumer(this);
            this.error = new AtomicThrowable();
            this.wip = new AtomicInteger();
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(upstream, upstreamRequests, s);
        }

        @Override
        public void onNext(T t) {
            if (!done) {
                long e = emitted;
                if (downstreamRequests.get() != e) {
                    emitted = e + 1;
                    HalfSerializer.onNext(downstream, t, wip, error);
                } else {
                    done = true;
                    cancel();
                    HalfSerializer.onError(downstream, new MissingBackpressureException("Downstream is not ready to receive the next upstream item."), wip, error);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!done) {
                done = true;
                otherConsumer.cancel();

                HalfSerializer.onError(downstream, t, wip, error);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                otherConsumer.cancel();
                HalfSerializer.onComplete(downstream, wip, error);
            }
        }

        void otherNext() {
            SubscriptionHelper.deferredRequest(upstream, upstreamRequests, 1);
        }

        void otherError(Throwable t) {
            SubscriptionHelper.cancel(upstream);
            HalfSerializer.onError(downstream, t, wip, error);
        }

        void otherComplete() {
            SubscriptionHelper.cancel(upstream);
            HalfSerializer.onComplete(downstream, wip, error);
        }

        @Override
        public void request(long n) {
            BackpressureHelper.add(downstreamRequests, n);
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(upstream);
            otherConsumer.cancel();
        }

        static final class OtherConsumer extends AtomicReference<Subscription> implements FlowableSubscriber<Object> {

            private static final long serialVersionUID = -9069889200779269650L;

            final RequestSample<?> parent;

            OtherConsumer(RequestSample<?> parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(Object t) {
                parent.otherNext();
            }

            @Override
            public void onError(Throwable t) {
                parent.otherError(t);
            }

            @Override
            public void onComplete() {
                parent.otherComplete();
            }

            void cancel() {
                SubscriptionHelper.cancel(this);
            }
        }
    }
}
