/*
 * Copyright 2016-2017 David Karnok
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

package hu.akarnokd.rxjava2.operators;

import java.util.concurrent.TimeUnit;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Debounce the upstream by taking an item and then blocking out subsequent
 * items until a specified time elapsed after the last item.
 *
 * @param <T> the value type
 *
 * @since 0.15.0
 */
final class FlowableDebounceFirst<T> extends Flowable<T>
implements FlowableTransformer<T, T> {

    final Publisher<T> source;

    final long timeout;

    final TimeUnit unit;

    final Scheduler scheduler;

    FlowableDebounceFirst(Publisher<T> source, long timeout, TimeUnit unit, Scheduler scheduler) {
        this.source = source;
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableDebounceFirst<T>(upstream, timeout, unit, scheduler);
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new DebounceFirstSubscriber<T>(s, timeout, unit, scheduler));
    }

    static final class DebounceFirstSubscriber<T>
    implements ConditionalSubscriber<T>, Subscription {

        final Subscriber<? super T> actual;

        final long timeout;

        final TimeUnit unit;

        final Scheduler scheduler;

        Subscription s;

        long timestamp;

        long gate;

        DebounceFirstSubscriber(Subscriber<? super T> actual, long timeout, TimeUnit unit, Scheduler scheduler) {
            super();
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                this.gate = scheduler.now(unit);

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            long now = scheduler.now(unit);
            long g = gate;
            gate = now + timeout;
            if (now < g) {
                return false;
            }
            actual.onNext(t);
            return true;
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }

        @Override
        public void request(long n) {
            s.request(n);
        }

        @Override
        public void cancel() {
            s.cancel();
        }
    }
}
