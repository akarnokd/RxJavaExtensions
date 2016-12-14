/*
 * Copyright 2016 David Karnok
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

/**
 * If the upstream signals an error, switch over to the next Solo
 * and emits its signal instead.
 * @param <T> the value type
 */
final class SoloOnErrorResumeNext<T> extends Solo<T> {

    final Solo<T> source;

    final Function<? super Throwable, ? extends Solo<T>> errorHandler;

    SoloOnErrorResumeNext(Solo<T> source, Function<? super Throwable, ? extends Solo<T>> errorHandler) {
        this.source = source;
        this.errorHandler = errorHandler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new OnErrorReturnItemSubscriber<T>(s, errorHandler));
    }

    static final class OnErrorReturnItemSubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -7631998337002592538L;

        final Function<? super Throwable, ? extends Solo<T>> errorHandler;

        final NextSubscriber nextSubscriber;

        Subscription s;

        OnErrorReturnItemSubscriber(Subscriber<? super T> actual, Function<? super Throwable, ? extends Solo<T>> errorHandler) {
            super(actual);
            this.errorHandler = errorHandler;
            this.nextSubscriber = new NextSubscriber();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            value = t;
        }

        @Override
        public void onError(Throwable t) {
            Solo<T> sp;

            try {
                sp = ObjectHelper.requireNonNull(errorHandler.apply(t), "The errorHandler returned a null Solo");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(new CompositeException(t, ex));
                return;
            }

            sp.subscribe(nextSubscriber);
        }

        @Override
        public void onComplete() {
            T v = value;
            if (v != null) {
                complete(v);
            } else {
                actual.onComplete();
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            s.cancel();
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
                OnErrorReturnItemSubscriber.this.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                actual.onError(t);
            }

            @Override
            public void onComplete() {
                OnErrorReturnItemSubscriber.this.onComplete();
            }
        }
    }
}
