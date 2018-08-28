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
 * Delay the emission of signals from the upstream until the
 * other Publisher signals an item or completes.
 *
 * @param <T> the value type
 */
final class PerhapsDelay<T> extends Perhaps<T> {

    final Perhaps<T> source;

    final Publisher<?> other;

    PerhapsDelay(Perhaps<T> source, Publisher<?> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new DelaySubscriber<T>(s, other));
    }

    static final class DelaySubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -7563209762781178448L;

        final Publisher<?> other;

        final OtherSubscriber inner;

        Subscription upstream;

        Throwable error;

        DelaySubscriber(Subscriber<? super T> downstream, Publisher<?> other) {
            super(downstream);
            this.other = other;
            this.inner = new OtherSubscriber();
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
            error = t;
            other.subscribe(inner);
        }

        @Override
        public void onComplete() {
            other.subscribe(inner);
        }

        @Override
        public void cancel() {
            super.cancel();
            upstream.cancel();
            SubscriptionHelper.cancel(inner);
        }

        void otherNext() {
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

        void otherError(Throwable ex) {
            Throwable ex0 = error;
            if (ex0 != null) {
                downstream.onError(new CompositeException(ex0, ex));
            } else {
                downstream.onError(ex);
            }
        }

        final class OtherSubscriber extends AtomicReference<Subscription>
        implements Subscriber<Object> {

            private static final long serialVersionUID = -2194292167160252795L;

            boolean done;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(Object t) {
                get().cancel();
                done = true;
                otherNext();
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
                    otherNext();
                }
            }
        }
    }
}
