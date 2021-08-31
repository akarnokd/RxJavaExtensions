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

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Signal a NoSuchElementException if the other signals before this
 * Solo signals.
 *
 * @param <T> the value type
 */
final class SoloTakeUntil<T> extends Solo<T> {

    final Solo<T> source;

    final Publisher<?> other;

    SoloTakeUntil(Solo<T> source, Publisher<?> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        TakeUntilSubscriber<T> parent = new TakeUntilSubscriber<>(s);
        s.onSubscribe(parent);

        other.subscribe(parent.other);
        source.subscribe(parent);
    }

    static final class TakeUntilSubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -3094876274753374720L;

        final AtomicReference<Subscription> upstream;

        final OtherSubscriber other;

        final AtomicBoolean once;

        TakeUntilSubscriber(Subscriber<? super T> downstream) {
            super(downstream);
            this.upstream = new AtomicReference<>();
            this.other = new OtherSubscriber();
            this.once = new AtomicBoolean();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this.upstream, s)) {
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            value = t;
        }

        @Override
        public void onError(Throwable t) {
            SubscriptionHelper.cancel(other);
            if (once.compareAndSet(false, true)) {
                value = null;
                downstream.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            SubscriptionHelper.cancel(other);
            if (once.compareAndSet(false, true)) {
                T v = value;
                if (v != null) {
                    value = null;
                    complete(v);
                } else {
                    downstream.onComplete();
                }
            }
        }

        void otherError(Throwable t) {
            SubscriptionHelper.cancel(upstream);
            if (once.compareAndSet(false, true)) {
                value = null;
                downstream.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        final class OtherSubscriber
        extends AtomicReference<Subscription>
        implements Subscriber<Object> {

            private static final long serialVersionUID = -7055801798042780544L;

            boolean done;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(Object t) {
                if (!done) {
                    get().cancel();
                    onComplete();
                }
            }

            @Override
            public void onError(Throwable t) {
                if (done) {
                    RxJavaPlugins.onError(t);
                } else {
                    done = true;
                    otherError(t);
                }
            }

            @Override
            public void onComplete() {
                if (!done) {
                    done = true;
                    otherError(new NoSuchElementException());
                }
            }
        }
    }
}
