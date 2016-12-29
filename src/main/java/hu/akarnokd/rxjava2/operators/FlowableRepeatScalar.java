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

import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * Repeats a scalar value indefinitely.
 *
 * @param <T> the value type
 * 
 * @since 0.14.2
 */
final class FlowableRepeatScalar<T> extends Flowable<T> {

    final T value;

    FlowableRepeatScalar(T value) {
        this.value = value;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            s.onSubscribe(new RepeatScalarConditionalSubscription<T>((ConditionalSubscriber<? super T>)s, value));
        } else {
            s.onSubscribe(new RepeatScalarSubscription<T>(s, value));
        }
    }

    static final class RepeatScalarSubscription<T> extends BasicQueueSubscription<T> {

        private static final long serialVersionUID = -231033913007168200L;

        final Subscriber<? super T> actual;

        T value;

        volatile boolean cancelled;

        RepeatScalarSubscription(Subscriber<? super T> actual, T value) {
            this.actual = actual;
            this.value = value;
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
            T v = value;
            for (;;) {
                if (cancelled) {
                    break;
                }
                actual.onNext(v);
            }
        }

        void slowpath(long r) {
            T v = value;

            long e = 0L;

            for (;;) {

                while (e != r) {
                    if (cancelled) {
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
            return value;
        }

        @Override
        public boolean isEmpty() {
            return value == null;
        }

        @Override
        public void clear() {
            value = null;
        }
    }

    static final class RepeatScalarConditionalSubscription<T> extends BasicQueueSubscription<T> {

        private static final long serialVersionUID = -231033913007168200L;

        final ConditionalSubscriber<? super T> actual;

        T value;

        volatile boolean cancelled;

        RepeatScalarConditionalSubscription(ConditionalSubscriber<? super T> actual, T value) {
            this.actual = actual;
            this.value = value;
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
            T v = value;
            for (;;) {
                if (cancelled) {
                    break;
                }
                actual.tryOnNext(v);
            }
        }

        void slowpath(long r) {
            T v = value;

            long e = 0L;

            for (;;) {

                while (e != r) {
                    if (cancelled) {
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
            return value;
        }

        @Override
        public boolean isEmpty() {
            return value == null;
        }

        @Override
        public void clear() {
            value = null;
        }
    }
}
