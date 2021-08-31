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

import java.util.*;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.*;

/**
 * Retry this solo if the Publisher returned by the handler signals an item
 * in response to the failure Throwable.
 *
 * @param <T> the value type
 */
final class SoloRetryWhen<T> extends Solo<T> {

    final Solo<T> source;

    final Function<? super Flowable<Throwable>, ? extends Publisher<?>> handler;

    SoloRetryWhen(Solo<T> source, Function<? super Flowable<Throwable>, ? extends Publisher<?>> handler) {
        this.source = source;
        this.handler = handler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        FlowableProcessor<Throwable> pp = PublishProcessor.<Throwable>create().toSerialized();

        Publisher<?> when;
        try {
            when = Objects.requireNonNull(handler.apply(pp), "The handler returned a null Publisher");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        RetrySubscriber<T> parent = new RetrySubscriber<>(s, pp, source);
        s.onSubscribe(parent);

        when.subscribe(parent.other);
        parent.subscribeNext();
    }

    static final class RetrySubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -1726278593241855499L;

        final AtomicInteger wip;

        final AtomicReference<Subscription> upstream;

        final Solo<T> source;

        final FlowableProcessor<Throwable> signal;

        final OtherSubscriber other;

        final AtomicBoolean once;

        volatile boolean active;

        RetrySubscriber(Subscriber<? super T> downstream, FlowableProcessor<Throwable> signal, Solo<T> source) {
            super(downstream);
            this.signal = signal;
            this.source = source;
            this.other = new OtherSubscriber();
            this.wip = new AtomicInteger();
            this.upstream = new AtomicReference<>();
            this.once = new AtomicBoolean();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.replace(this.upstream, s)) {
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            value = t;
        }

        @Override
        public void onError(Throwable t) {
            active = false;
            other.requestOne();
            signal.onNext(t);
        }

        void subscribeNext() {
            if (wip.getAndIncrement() == 0) {
                do {
                    if (SubscriptionHelper.CANCELLED == upstream.get()) {
                        return;
                    }

                    if (!active) {
                        active = true;
                        source.subscribe(this);
                    }
                } while (wip.decrementAndGet() != 0);
            }
        }

        @Override
        public void onComplete() {
            SubscriptionHelper.cancel(other);
            if (once.compareAndSet(false, true)) {
                T v = value;
                value = null;
                complete(v);
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            SubscriptionHelper.cancel(upstream);
            SubscriptionHelper.cancel(other);
        }

        void otherError(Throwable ex) {
            SubscriptionHelper.cancel(upstream);
            if (once.compareAndSet(false, true)) {
                downstream.onError(ex);
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        void otherComplete() {
            SubscriptionHelper.cancel(upstream);
            if (once.compareAndSet(false, true)) {
                downstream.onError(new NoSuchElementException());
            }
        }

        final class OtherSubscriber extends AtomicReference<Subscription>
        implements Subscriber<Object> {

            private static final long serialVersionUID = -790600520757208416L;

            final AtomicLong requested;

            OtherSubscriber() {
                this.requested = new AtomicLong();
            }

            @Override
            public void onSubscribe(Subscription s) {
                SubscriptionHelper.deferredSetOnce(this, requested, s);
            }

            @Override
            public void onNext(Object t) {
                subscribeNext();
            }

            @Override
            public void onError(Throwable t) {
                otherError(t);
            }

            @Override
            public void onComplete() {
                otherComplete();
            }

            void requestOne() {
                SubscriptionHelper.deferredRequest(this, requested, 1);
            }
        }
    }
}
