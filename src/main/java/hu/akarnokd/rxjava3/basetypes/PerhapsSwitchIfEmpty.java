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

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Switch to another Perhaps if the main is empty.
 * 
 * @param <T> the value type
 */
final class PerhapsSwitchIfEmpty<T> extends Perhaps<T> {

    final Perhaps<T> source;

    final Perhaps<? extends T> other;

    PerhapsSwitchIfEmpty(Perhaps<T> source, Perhaps<? extends T> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new SwitchIfEmptySubscriber<T>(s, other));
    }

    static final class SwitchIfEmptySubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -9119999967998769573L;

        final Perhaps<? extends T> other;

        final OtherSubscriber otherSubscriber;

        Subscription upstream;

        SwitchIfEmptySubscriber(Subscriber<? super T> downstream, Perhaps<? extends T> other) {
            super(downstream);
            this.other = other;
            this.otherSubscriber = new OtherSubscriber();
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
            this.value = t;
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            T v = value;
            if (v != null) {
                complete(v);
            } else {
                other.subscribe(otherSubscriber);
            }
        }

        void otherSignal(T v) {
            complete(v);
        }

        void otherError(Throwable t) {
            downstream.onError(t);
        }

        void otherComplete() {
            downstream.onComplete();
        }

        @Override
        public void cancel() {
            super.cancel();
            upstream.cancel();
            SubscriptionHelper.cancel(otherSubscriber);
        }

        final class OtherSubscriber extends AtomicReference<Subscription>
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
                get().cancel();
                lazySet(SubscriptionHelper.CANCELLED);
                otherSignal(t);
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
                    otherComplete();
                }
            }
        }
    }
}
