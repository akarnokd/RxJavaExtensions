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

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.rxjava3.internal.subscriptions.*;

/**
 * If the upstream signals an error, switch over to the next Solo
 * and emits its signal instead.
 * @param <T> the value type
 */
final class SoloOnErrorResumeWith<T> extends Solo<T> {

    final Solo<T> source;

    final Solo<T> next;

    SoloOnErrorResumeWith(Solo<T> source, Solo<T> next) {
        this.source = source;
        this.next = next;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new OnErrorResumeWithSubscriber<T>(s, next));
    }

    static final class OnErrorResumeWithSubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -7631998337002592538L;

        final Solo<T> next;

        final NextSubscriber nextSubscriber;

        Subscription upstream;

        OnErrorResumeWithSubscriber(Subscriber<? super T> downstream, Solo<T> next) {
            super(downstream);
            this.next = next;
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
            value = t;
        }

        @Override
        public void onError(Throwable t) {
            next.subscribe(nextSubscriber);
        }

        @Override
        public void onComplete() {
            T v = value;
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
            SubscriptionHelper.cancel(nextSubscriber);
        }

        final class NextSubscriber extends AtomicReference<Subscription>
        implements Subscriber<T> {

            private static final long serialVersionUID = 5161815655607865861L;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(T t) {
                OnErrorResumeWithSubscriber.this.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                downstream.onError(t);
            }

            @Override
            public void onComplete() {
                OnErrorResumeWithSubscriber.this.onComplete();
            }
        }
    }
}
