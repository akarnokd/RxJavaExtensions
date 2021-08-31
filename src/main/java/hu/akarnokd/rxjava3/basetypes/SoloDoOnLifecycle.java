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

import org.reactivestreams.*;

import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.operators.QueueSubscription;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Execute callbacks at various points in the lifecycle.
 * 
 * @param <T> the value type
 */
final class SoloDoOnLifecycle<T> extends Solo<T> {

    final Solo<T> source;

    final Consumer<? super T> onNext;

    final Consumer<? super T> onAfterNext;

    final Consumer<? super Throwable> onError;

    final Action onComplete;

    final Action onAfterTerminate;

    final Consumer<? super Subscription> onSubscribe;

    final LongConsumer onRequest;

    final Action onCancel;

    boolean done;

    SoloDoOnLifecycle(
            Solo<T> source,
            Consumer<? super T> onNext,
            Consumer<? super T> onAfterNext,
            Consumer<? super Throwable> onError,
            Action onComplete,
            Action onAfterTerminate,
            Consumer<? super Subscription> onSubscribe,
            LongConsumer onRequest,
            Action onCancel) {
        this.source = source;
        this.onNext = onNext;
        this.onAfterNext = onAfterNext;
        this.onError = onError;
        this.onComplete = onComplete;
        this.onAfterTerminate = onAfterTerminate;
        this.onSubscribe = onSubscribe;
        this.onRequest = onRequest;
        this.onCancel = onCancel;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new DoOnSubscriber(s));
    }

    final class DoOnSubscriber extends BasicSoloQueueSubscription<T> implements Subscriber<T> {

        final Subscriber<? super T> downstream;

        Subscription upstream;

        QueueSubscription<T> queue;

        boolean done;

        int sourceMode;

        DoOnSubscriber(Subscriber<? super T> downstream) {
            this.downstream = downstream;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                if (s instanceof QueueSubscription) {
                    queue = (QueueSubscription<T>)s;
                }

                try {
                    onSubscribe.accept(s);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    s.cancel();
                    downstream.onSubscribe(EmptySubscription.INSTANCE);
                    onError(ex);
                    return;
                }

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!done) {
                if (sourceMode != NONE) {
                    downstream.onNext(null);
                    return;
                }

                try {
                    onNext.accept(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    upstream.cancel();
                    onError(ex);
                    return;
                }

                downstream.onNext(t);

                try {
                    onAfterNext.accept(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    upstream.cancel();
                    onError(ex);
                    return;
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            try {
                onError.accept(t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                t = new CompositeException(t, ex);
            }

            downstream.onError(t);

            doAfter();
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                try {
                    onComplete.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    downstream.onError(ex);
                    return;
                }

                downstream.onComplete();

                doAfter();
            }
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
            upstream.cancel();
        }

        @Override
        public void request(long n) {
            try {
                onRequest.accept(n);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
            upstream.request(n);
        }

        @Override
        public int requestFusion(int mode) {
            QueueSubscription<T> qs = queue;
            if (qs != null) {
                int m = qs.requestFusion(mode);
                this.sourceMode = m;
                return m;
            }
            return NONE;
        }

        @Override
        public T poll() throws Throwable {
            T v = queue.poll();

            if (v != null) {
                onNext.accept(v);
                onAfterNext.accept(v);
            } else {
                if (sourceMode == SYNC) {
                    onComplete.run();
                    onAfterTerminate.run();
                }
            }

            return v;
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }

        @Override
        public void clear() {
            queue.clear();
        }
    }
}
