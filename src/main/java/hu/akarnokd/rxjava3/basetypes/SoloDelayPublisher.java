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

import io.reactivex.rxjava3.exceptions.CompositeException;
import io.reactivex.rxjava3.operators.QueueSubscription;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Delay signals till the other signals an item or completes.
 *
 * @param <T> the value type
 */
final class SoloDelayPublisher<T> extends Solo<T> {

    final Solo<T> source;

    final Publisher<?> other;

    SoloDelayPublisher(Solo<T> source, Publisher<?> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new DelaySubscriber<T>(s, other));
    }

    static final class DelaySubscriber<T> extends AtomicReference<Subscription>
    implements QueueSubscription<T>, Subscriber<T> {

        private static final long serialVersionUID = 511073038536312798L;

        final Subscriber<? super T> downstream;

        final Publisher<?> other;

        Subscription upstream;

        T value;
        Throwable error;
        volatile boolean available;

        boolean outputFused;

        DelaySubscriber(Subscriber<? super T> downstream, Publisher<?> other) {
            this.downstream = downstream;
            this.other = other;
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & ASYNC) != 0) {
                outputFused = true;
                return ASYNC;
            }
            return NONE;
        }

        @Override
        public T poll() throws Exception {
            if (available) {
                T v = value;
                value = null;
                return v;
            }
            return null;
        }

        @Override
        public boolean isEmpty() {
            return !available || value == null;
        }

        @Override
        public void clear() {
            value = null;
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
            SubscriptionHelper.cancel(this);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            this.value = t;
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
            onComplete();
        }

        @Override
        public void onComplete() {
            other.subscribe(new OtherSubscriber());
        }

        void run() {
            Subscriber<? super T> a = downstream;
            Throwable ex = error;
            if (ex != null) {
                a.onError(ex);
            } else {
                if (outputFused) {
                    available = true;
                    a.onNext(null);
                } else {
                    T v = value;
                    value = null;
                    if (v != null) {
                        a.onNext(v);
                    }
                }
                a.onComplete();
            }
        }

        @Override
        public boolean offer(T value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean offer(T v1, T v2) {
            throw new UnsupportedOperationException();
        }

        boolean innerSubscribe(Subscription s) {
            return SubscriptionHelper.setOnce(this, s);
        }

        void innerCancel() {
            SubscriptionHelper.cancel(this);
        }

        void innerError(Throwable ex) {
            Throwable e = error;
            if (e == null) {
                error = ex;
            } else {
                error = new CompositeException(e, ex);
            }
            run();
        }

        final class OtherSubscriber implements Subscriber<Object> {

            boolean done;

            @Override
            public void onSubscribe(Subscription s) {
                if (innerSubscribe(s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(Object t) {
                SubscriptionHelper.cancel(DelaySubscriber.this);
                onComplete();
            }

            @Override
            public void onError(Throwable t) {
                if (done) {
                    RxJavaPlugins.onError(t);
                } else {
                    innerError(t);
                }
            }

            @Override
            public void onComplete() {
                if (!done) {
                    done = true;
                    run();
                }
            }
        }
    }
}
