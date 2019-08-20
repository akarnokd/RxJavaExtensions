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
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Delay events until another Publisher signals an item or completes.
 * 
 * @param <T> the value type
 */
final class PerhapsDelaySubscription<T> extends Perhaps<T> {

    final Perhaps<T> source;

    final Publisher<?> other;

    PerhapsDelaySubscription(Perhaps<T> source, Publisher<?> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        other.subscribe(new DelaySubscriber<T>(s, source));
    }

    static final class DelaySubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<Object> {

        private static final long serialVersionUID = -9119999967998769573L;

        final Perhaps<T> source;

        final SourceSubscriber sourceSubscriber;

        Subscription upstream;

        DelaySubscriber(Subscriber<? super T> downstream, Perhaps<T> source) {
            super(downstream);
            this.source = source;
            this.sourceSubscriber = new SourceSubscriber();
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
        public void onNext(Object t) {
            if (upstream != SubscriptionHelper.CANCELLED) {
                upstream.cancel();
                upstream = SubscriptionHelper.CANCELLED;

                source.subscribe(sourceSubscriber);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (upstream == SubscriptionHelper.CANCELLED) {
                RxJavaPlugins.onError(t);
            } else {
                downstream.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (upstream != SubscriptionHelper.CANCELLED) {
                upstream = SubscriptionHelper.CANCELLED;

                source.subscribe(sourceSubscriber);
            }
        }

        void otherSignal(T v) {
            value = v;
        }

        void otherError(Throwable t) {
            downstream.onError(t);
        }

        void otherComplete() {
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
            SubscriptionHelper.cancel(sourceSubscriber);
        }

        final class SourceSubscriber extends AtomicReference<Subscription>
        implements Subscriber<T> {

            private static final long serialVersionUID = -6651374802328276829L;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(T t) {
                otherSignal(t);
            }

            @Override
            public void onError(Throwable t) {
                otherError(t);
            }

            @Override
            public void onComplete() {
                otherComplete();
            }
        }
    }
}
