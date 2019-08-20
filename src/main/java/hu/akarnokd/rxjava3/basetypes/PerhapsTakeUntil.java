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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Take until the other Publisher signals an item or completes, completing this Perhaps.
 *
 * @param <T> the value type
 */
final class PerhapsTakeUntil<T> extends Perhaps<T> {

    final Perhaps<T> source;

    final Publisher<?> other;

    PerhapsTakeUntil(Perhaps<T> source, Publisher<?> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        TakeUntilSubscriber<T> parent = new TakeUntilSubscriber<T>(s);
        s.onSubscribe(parent);

        other.subscribe(parent.other);
        source.subscribe(parent);
    }

    static final class TakeUntilSubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = 8414575379623209938L;

        final AtomicBoolean once;

        final AtomicReference<Subscription> upstream;

        final OtherSubscriber other;

        TakeUntilSubscriber(Subscriber<? super T> downstream) {
            super(downstream);
            this.other = new OtherSubscriber();
            this.upstream = new AtomicReference<Subscription>();
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
            if (once.compareAndSet(false, true)) {
                SubscriptionHelper.cancel(other);
                complete(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (once.compareAndSet(false, true)) {
                SubscriptionHelper.cancel(other);
                downstream.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!once.get() && once.compareAndSet(false, true)) {
                SubscriptionHelper.cancel(other);
                downstream.onComplete();
            }
        }

        void otherSignal() {
            if (once.compareAndSet(false, true)) {
                SubscriptionHelper.cancel(upstream);
                downstream.onComplete();
            }
        }

        void otherError(Throwable e) {
            if (once.compareAndSet(false, true)) {
                SubscriptionHelper.cancel(upstream);
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            SubscriptionHelper.cancel(upstream);
            SubscriptionHelper.cancel(other);
        }

        final class OtherSubscriber extends AtomicReference<Subscription>
        implements Subscriber<Object> {

            private static final long serialVersionUID = 8999579172944042558L;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(Object t) {
                get().cancel();
                otherSignal();
            }

            @Override
            public void onError(Throwable t) {
                otherError(t);
            }

            @Override
            public void onComplete() {
                otherSignal();
            }
        }
    }
}
