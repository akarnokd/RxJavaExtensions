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

package hu.akarnokd.rxjava2.processors;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.processors.FlowableProcessor;

final class FlowableProcessorWrap<T> extends FlowableProcessor<T> {

    final Processor<T, T> source;

    volatile boolean done;
    Throwable error;

    FlowableProcessorWrap(Processor<T, T> source) {
        this.source = source;
    }

    @Override
    public void onSubscribe(Subscription s) {
        source.onSubscribe(s);
    }

    @Override
    public void onNext(T t) {
        source.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        error = t;
        done = true;
        source.onError(t);
    }

    @Override
    public void onComplete() {
        done = true;
        source.onComplete();
    }

    @Override
    public boolean hasSubscribers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasThrowable() {
        return done && error != null;
    }

    @Override
    public boolean hasComplete() {
        return done && error == null;
    }

    @Override
    public Throwable getThrowable() {
        return done ? error : null;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new WrapSubscriber(s));
    }

    final class WrapSubscriber extends AtomicBoolean implements Subscriber<T>, Subscription {

        private static final long serialVersionUID = -6891177704330298695L;

        final Subscriber<? super T> actual;

        Subscription s;

        WrapSubscriber(Subscriber<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.s = s;

            actual.onSubscribe(this);
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            s = SubscriptionHelper.CANCELLED;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            s = SubscriptionHelper.CANCELLED;
            actual.onComplete();
        }

        @Override
        public void request(long n) {
            s.request(n);
        }

        @Override
        public void cancel() {
            if (compareAndSet(false, true)) {
                s.cancel();
                s = SubscriptionHelper.CANCELLED;
            }
        }
    }
}
