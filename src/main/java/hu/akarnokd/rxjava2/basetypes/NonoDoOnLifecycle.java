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

import org.reactivestreams.*;

import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Execute callbacks at various points in the lifecycle.
 */
final class NonoDoOnLifecycle extends Nono {

    final Nono source;

    final Consumer<? super Throwable> onError;

    final Action onComplete;

    final Action onAfterTerminate;

    final Consumer<? super Subscription> onSubscribe;

    final LongConsumer onRequest;

    final Action onCancel;

    NonoDoOnLifecycle(
            Nono source,
            Consumer<? super Throwable> onError,
            Action onComplete,
            Action onAfterTerminate,
            Consumer<? super Subscription> onSubscribe,
            LongConsumer onRequest,
            Action onCancel) {
        this.source = source;
        this.onError = onError;
        this.onComplete = onComplete;
        this.onAfterTerminate = onAfterTerminate;
        this.onSubscribe = onSubscribe;
        this.onRequest = onRequest;
        this.onCancel = onCancel;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        source.subscribe(new DoOnSubscriber(s));
    }

    final class DoOnSubscriber extends BasicNonoSubscriber {

        DoOnSubscriber(Subscriber<? super Void> actual) {
            super(actual);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                try {
                    onSubscribe.accept(s);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    EmptySubscription.error(ex, actual);
                    return;
                }

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onError(Throwable t) {
            try {
                onError.accept(t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                t = new CompositeException(t, ex);
            }

            actual.onError(t);

            doAfter();
        }

        @Override
        public void onComplete() {
            try {
                onComplete.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(ex);
                return;
            }

            actual.onComplete();

            doAfter();
        }

        void doAfter() {
            try {
                onAfterTerminate.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void cancel() {
            try {
                onCancel.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
            s.cancel();
        }

        @Override
        public void request(long n) {
            try {
                onRequest.accept(n);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
            s.request(n);
        }
    }
}
