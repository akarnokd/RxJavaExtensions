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

package hu.akarnokd.rxjava2.basetypes;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * If the upstream fails, use the Throwable to get a fallback Perhaps and resume with it.
 *
 * @param <T> the value type
 */
final class PerhapsOnErrorResumeNext<T> extends Perhaps<T> {

    final Perhaps<T> source;

    final Function<? super Throwable, ? extends Perhaps<? extends T>> fallbackSupplier;

    PerhapsOnErrorResumeNext(Perhaps<T> source, Function<? super Throwable, ? extends Perhaps<? extends T>> fallbackSupplier) {
        this.source = source;
        this.fallbackSupplier = fallbackSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new OnErrorResumeNextSubscriber<T>(s, fallbackSupplier));
    }

    static final class OnErrorResumeNextSubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -9119999967998769573L;

        final Function<? super Throwable, ? extends Perhaps<? extends T>> fallbackSupplier;

        final OtherSubscriber otherSubscriber;

        Subscription upstream;

        OnErrorResumeNextSubscriber(Subscriber<? super T> downstream, Function<? super Throwable, ? extends Perhaps<? extends T>> fallbackSupplier) {
            super(downstream);
            this.fallbackSupplier = fallbackSupplier;
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
            Perhaps<? extends T> ph;

            try {
                ph = ObjectHelper.requireNonNull(fallbackSupplier.apply(t), "The fallbackSupplier returned a null Perhaps");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(new CompositeException(t, ex));
                return;
            }

            ph.subscribe(otherSubscriber);
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
