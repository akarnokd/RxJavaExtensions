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

package hu.akarnokd.rxjava3.operators;

import java.util.Objects;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.operators.ConditionalSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.BackpressureHelper;

/**
 * Repeatedly calls a Supplier indefinitely.
 *
 * @param <T> the value type
 * 
 * @since 0.14.2
 */
final class FlowableRepeatSupplier<T> extends Flowable<T> {

    final Supplier<T> supplier;

    FlowableRepeatSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            s.onSubscribe(new RepeatCallableConditionalSubscription<>((ConditionalSubscriber<? super T>)s, supplier));
        } else {
            s.onSubscribe(new RepeatCallableSubscription<>(s, supplier));
        }
    }

    static final class RepeatCallableSubscription<T> extends BasicQueueSubscription<T> {

        private static final long serialVersionUID = -231033913007168200L;

        final Subscriber<? super T> downstream;

        final Supplier<T> supplier;

        volatile boolean cancelled;

        RepeatCallableSubscription(Subscriber<? super T> downstream, Supplier<T> supplier) {
            this.downstream = downstream;
            this.supplier = supplier;
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
            Supplier<T> c = supplier;
            for (;;) {
                if (cancelled) {
                    break;
                }

                T v;

                try {
                    v = Objects.requireNonNull(c.get(), "The supplier returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    downstream.onError(ex);
                    break;
                }

                downstream.onNext(v);
            }
        }

        void slowpath(long r) {
            Supplier<T> c = supplier;

            long e = 0L;

            for (;;) {

                while (e != r) {
                    if (cancelled) {
                        return;
                    }

                    T v;

                    try {
                        v = Objects.requireNonNull(c.get(), "The supplier returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        downstream.onError(ex);
                        return;
                    }

                    downstream.onNext(v);

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
                    e = 0L;
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & BOUNDARY) == 0) {
                return mode & SYNC;
            }
            return NONE;
        }

        @Override
        public T poll() throws Throwable {
            return Objects.requireNonNull(supplier.get(), "The supplier returned a null value");
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

        final ConditionalSubscriber<? super T> downstream;

        final Supplier<T> supplier;

        volatile boolean cancelled;

        RepeatCallableConditionalSubscription(ConditionalSubscriber<? super T> downstream, Supplier<T> supplier) {
            this.downstream = downstream;
            this.supplier = supplier;
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
            Supplier<T> c = supplier;
            for (;;) {
                if (cancelled) {
                    break;
                }

                T v;

                try {
                    v = Objects.requireNonNull(c.get(), "The supplier returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    downstream.onError(ex);
                    break;
                }

                downstream.tryOnNext(v);
            }
        }

        void slowpath(long r) {
            Supplier<T> c = supplier;

            long e = 0L;

            for (;;) {

                while (e != r) {
                    if (cancelled) {
                        return;
                    }

                    T v;

                    try {
                        v = Objects.requireNonNull(c.get(), "The supplier returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        downstream.onError(ex);
                        return;
                    }

                    if (downstream.tryOnNext(v)) {
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
                    e = 0L;
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & BOUNDARY) == 0) {
                return mode & SYNC;
            }
            return NONE;
        }

        @Override
        public T poll() throws Throwable {
            return Objects.requireNonNull(supplier.get(), "The supplier returned a null value");
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
