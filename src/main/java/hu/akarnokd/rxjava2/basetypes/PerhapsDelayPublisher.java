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

import io.reactivex.exceptions.CompositeException;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Delay events until another Publisher signals an item or completes.
 * 
 * @param <T> the value type
 */
final class PerhapsDelayPublisher<T> extends Perhaps<T> {

    final Perhaps<T> source;

    final Publisher<?> other;

    PerhapsDelayPublisher(Perhaps<T> source, Publisher<?> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new DelaySubscriber<T>(s, other));
    }

    static final class DelaySubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -9119999967998769573L;

        final Publisher<?> other;

        final OtherSubscriber otherSubscriber;

        Subscription s;

        Throwable error;

        DelaySubscriber(Subscriber<? super T> actual, Publisher<?> other) {
            super(actual);
            this.other = other;
            this.otherSubscriber = new OtherSubscriber();
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
            this.value = t;
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            other.subscribe(otherSubscriber);
        }

        @Override
        public void onComplete() {
            other.subscribe(otherSubscriber);
        }

        void otherSignal() {
            Throwable ex = error;
            if (ex != null) {
                downstream.onError(ex);
                return;
            }
            T v = value;
            if (v != null) {
                complete(v);
            } else {
                downstream.onComplete();
            }
        }

        void otherError(Throwable t) {
            Throwable ex = error;
            if (ex != null) {
                t = new CompositeException(ex, t);
            }
            downstream.onError(t);
        }

        @Override
        public void cancel() {
            super.cancel();
            s.cancel();
            SubscriptionHelper.cancel(otherSubscriber);
        }

        final class OtherSubscriber extends AtomicReference<Subscription>
        implements Subscriber<Object> {

            private static final long serialVersionUID = -6651374802328276829L;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(Object t) {
                get().cancel();
                lazySet(SubscriptionHelper.CANCELLED);

                otherSignal();
            }

            @Override
            public void onError(Throwable t) {
                if (get() != SubscriptionHelper.CANCELLED) {
                    otherError(t);
                } else {
                    RxJavaPlugins.onError(t);
                }
            }

            @Override
            public void onComplete() {
                if (get() != SubscriptionHelper.CANCELLED) {
                    otherSignal();
                }
            }
        }
    }
}
