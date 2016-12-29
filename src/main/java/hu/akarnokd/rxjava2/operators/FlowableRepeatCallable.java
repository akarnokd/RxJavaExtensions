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

package hu.akarnokd.rxjava2.operators;

import java.util.concurrent.Callable;

import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * Repeatedly calls a Callable indefinitely.
 *
 * @param <T> the value type
 * 
 * @since 0.14.2
 */
final class FlowableRepeatCallable<T> extends Flowable<T> {

    final Callable<T> callable;

    FlowableRepeatCallable(Callable<T> callable) {
        this.callable = callable;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            s.onSubscribe(new RepeatCallableConditionalSubscription<T>((ConditionalSubscriber<? super T>)s, callable));
        } else {
            s.onSubscribe(new RepeatCallableSubscription<T>(s, callable));
        }
    }

    static final class RepeatCallableSubscription<T> extends BasicQueueSubscription<T> {

        private static final long serialVersionUID = -231033913007168200L;

        final Subscriber<? super T> actual;

        final Callable<T> callable;

        volatile boolean cancelled;

        RepeatCallableSubscription(Subscriber<? super T> actual, Callable<T> callable) {
            this.actual = actual;
            this.callable = callable;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                if (BackpressureHelper.add(this, n) == 0) {
                    if (n == Long.MAX_VALUE) {
                        fastpath();
                    } else {
                        slowpath(n);
                    }
                }
            }
        }

        void fastpath() {
            Callable<T> c = callable;
            for (;;) {
                if (cancelled) {
                    break;
                }

                T v;

                try {
                    v = ObjectHelper.requireNonNull(c.call(), "The callable returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    actual.onError(ex);
                    break;
                }

                actual.onNext(v);
            }
        }

        void slowpath(long r) {
            Callable<T> c = callable;

            long e = 0L;

            for (;;) {

                while (e != r) {
                    if (cancelled) {
                        return;
                    }


                    T v;

                    try {
                        v = ObjectHelper.requireNonNull(c.call(), "The callable returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        actual.onError(ex);
                        return;
                    }

                    actual.onNext(v);

                    e++;
                }

                if (cancelled) {
                    return;
                }

                r = get();
                if (e == r) {
                    r = addAndGet(-e);
                    if (r == 0L) {
                        break;
                    }
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public int requestFusion(int mode) {
            return mode & SYNC;
        }

        @Override
        public T poll() throws Exception {
            return ObjectHelper.requireNonNull(callable.call(), "The callable returned a null value");
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void clear() {
            // no-op
        }
    }

    static final class RepeatCallableConditionalSubscription<T> extends BasicQueueSubscription<T> {

        private static final long serialVersionUID = -231033913007168200L;

        final ConditionalSubscriber<? super T> actual;

        final Callable<T> callable;

        volatile boolean cancelled;

        RepeatCallableConditionalSubscription(ConditionalSubscriber<? super T> actual, Callable<T> callable) {
            this.actual = actual;
            this.callable = callable;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                if (BackpressureHelper.add(this, n) == 0) {
                    if (n == Long.MAX_VALUE) {
                        fastpath();
                    } else {
                        slowpath(n);
                    }
                }
            }
        }

        void fastpath() {
            Callable<T> c = callable;
            for (;;) {
                if (cancelled) {
                    break;
                }

                T v;

                try {
                    v = ObjectHelper.requireNonNull(c.call(), "The callable returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    actual.onError(ex);
                    break;
                }

                actual.tryOnNext(v);
            }
        }

        void slowpath(long r) {
            Callable<T> c = callable;

            long e = 0L;

            for (;;) {

                while (e != r) {
                    if (cancelled) {
                        return;
                    }

                    T v;

                    try {
                        v = ObjectHelper.requireNonNull(c.call(), "The callable returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        actual.onError(ex);
                        return;
                    }

                    if (actual.tryOnNext(v)) {
                        e++;
                    }
                }

                if (cancelled) {
                    return;
                }

                r = get();
                if (e == r) {
                    r = addAndGet(-e);
                    if (r == 0L) {
                        break;
                    }
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public int requestFusion(int mode) {
            return mode & SYNC;
        }

        @Override
        public T poll() throws Exception {
            return ObjectHelper.requireNonNull(callable.call(), "The callable returned a null value");
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void clear() {
            // no-op
        }
    }
}
