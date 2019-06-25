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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Issues {@link Subscription#request(long)}(1) after the initial delay and then at each period.
 *
 * @param <T> the item type
 * @since 0.18.6
 */
final class FlowableRequestSampleTime<T> extends Flowable<T> implements FlowableTransformer<T, T> {

    final Flowable<T> source;

    final long initialDelay;

    final long period;

    final TimeUnit unit;

    final Scheduler scheduler;

    FlowableRequestSampleTime(Flowable<T> source, long initialDelay,
            long period, TimeUnit unit, Scheduler scheduler) {
        super();
        this.source = source;
        this.initialDelay = initialDelay;
        this.period = period;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableRequestSampleTime<T>(upstream, initialDelay, period, unit, scheduler);
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        RequestSample<T> parent = new RequestSample<T>(s);
        s.onSubscribe(parent);
        DisposableHelper.setOnce(parent.timer, scheduler.schedulePeriodicallyDirect(parent, initialDelay, period, unit));
        source.subscribe(parent);
    }

    static final class RequestSample<T>
    implements FlowableSubscriber<T>, Subscription, Runnable {

        final Subscriber<? super T> downstream;

        final AtomicLong downstreamRequests;

        final AtomicLong upstreamRequests;

        final AtomicReference<Subscription> upstream;

        final AtomicReference<Disposable> timer;

        long emitted;

        boolean done;

        RequestSample(Subscriber<? super T> downstream) {
            this.downstream = downstream;
            this.downstreamRequests = new AtomicLong();
            this.upstreamRequests = new AtomicLong();
            this.upstream = new AtomicReference<Subscription>();
            this.timer = new AtomicReference<Disposable>();
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
                    downstream.onNext(t);
                } else {
                    done = true;
                    cancel();
                    downstream.onError(new MissingBackpressureException("Downstream is not ready to receive the next upstream item."));
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!done) {
                done = true;
                downstream.onError(t);
                DisposableHelper.dispose(timer);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                downstream.onComplete();
                DisposableHelper.dispose(timer);
            }
        }

        @Override
        public void run() {
            SubscriptionHelper.deferredRequest(upstream, upstreamRequests, 1);
        }

        @Override
        public void request(long n) {
            BackpressureHelper.add(downstreamRequests, n);
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(upstream);
            DisposableHelper.dispose(timer);
        }
    }
}
