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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.internal.subscriptions.DeferredScalarSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * A Processor based on the Perhaps type that emits an onNext+onComplete, only onComplete or an onError.
 * <p>
 * Calling the onNext multiple times has no effect.
 * 
 * @param <T> the input and output value type
 * 
 * @since 0.14.0
 */
public final class PerhapsProcessor<T> extends Perhaps<T> implements Processor<T, T> {

    @SuppressWarnings("rawtypes")
    static final InnerSubscription[] EMPTY = new InnerSubscription[0];

    @SuppressWarnings("rawtypes")
    static final InnerSubscription[] TERMINATED = new InnerSubscription[0];

    final AtomicReference<InnerSubscription<T>[]> subscribers;

    final AtomicBoolean once;

    T value;
    Throwable error;

    /**
     * Creates a fresh PerhapsProcessor instance.
     * @param <T> the input and output value type
     * @return the new PerhapsProcessor instance
     */
    public static <T> PerhapsProcessor<T> create() {
        return new PerhapsProcessor<>();
    }

    /**
     * Private: processors traditionally are created via create().
     */
    @SuppressWarnings("unchecked")
    private PerhapsProcessor() {
        subscribers = new AtomicReference<>(EMPTY);
        once = new AtomicBoolean();
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        InnerSubscription<T> inner = new InnerSubscription<>(s, this);
        s.onSubscribe(inner);
        if (add(inner)) {
            if (inner.isCancelled()) {
                remove(inner);
            }
        } else {
            Throwable ex = error;
            if (ex != null) {
                inner.error(ex);
            } else {
                T v = value;
                if (v != null) {
                    inner.complete(v);
                } else {
                    s.onComplete();
                }
            }
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (subscribers.get() == TERMINATED) {
            s.cancel();
        } else {
            s.request(Long.MAX_VALUE);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onNext(T t) {
        if (once.compareAndSet(false, true)) {
            value = t;
            for (InnerSubscription<T> inner : subscribers.getAndSet(TERMINATED)) {
                inner.complete(t);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(Throwable t) {
        if (once.compareAndSet(false, true)) {
            error = t;
            for (InnerSubscription<T> inner : subscribers.getAndSet(TERMINATED)) {
                inner.error(t);
            }
        } else {
            RxJavaPlugins.onError(t);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onComplete() {
        if (!once.get() && once.compareAndSet(false, true)) {
            for (InnerSubscription<T> inner : subscribers.getAndSet(TERMINATED)) {
                inner.complete();
            }
        }
    }

    /**
     * Returns true if this Processor has Subscribers.
     * @return true if this Processor has Subscribers
     */
    public boolean hasSubscribers() {
        return subscribers.get().length != 0;
    }

    /**
     * Test support: return the current number of Subscribers.
     * @return the current number of Subscribers
     */
    int subscriberCount() {
        return subscribers.get().length;
    }

    /**
     * Returns true if this PerhapsProcessor received an onNext value.
     * @return true if this PerhapsProcessor received an onNext value
     */
    public boolean hasValue() {
        return subscribers.get() == TERMINATED && value != null;
    }

    /**
     * Returns the received onNext value if hasValue() is true, null otherwise.
     * @return the received onNext value if hasValue() is true, null otherwise
     */
    public T getValue() {
        return subscribers.get() == TERMINATED ? value : null;
    }

    /**
     * Returns true if this PerhapsProcessor received an onError Throwable.
     * @return true if this PerhapsProcessor received an onError Throwable
     */
    public boolean hasThrowable() {
        return subscribers.get() == TERMINATED && error != null;
    }

    /**
     * Returns the received onError Throwable if hasThrowable() is true, null otherwise.
     * @return the received onError Throwable if hasThrowable() is true, null otherwise
     */
    public Throwable getThrowable() {
        return subscribers.get() == TERMINATED ? error : null;
    }

    /**
     * Returns true if this PerhapsProcessor completed without any value.
     * @return true if this PerhapsProcessor completed without any value
     */
    public boolean hasComplete() {
        return subscribers.get() == TERMINATED && value == null;
    }

    boolean add(InnerSubscription<T> inner) {
        for (;;) {
            InnerSubscription<T>[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }
            int n = a.length;
            @SuppressWarnings("unchecked")
            InnerSubscription<T>[] b = new InnerSubscription[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = inner;
            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(InnerSubscription<T> inner) {
        for (;;) {
            InnerSubscription<T>[] a = subscribers.get();
            int n = a.length;
            if (n == 0) {
                return;
            }
            int j = -1;
            for (int i = 0; i < n; i++) {
                if (a[i] == inner) {
                    j = i;
                    break;
                }
            }

            if (j < 0) {
                return;
            }
            InnerSubscription<T>[] b;

            if (n == 1) {
                b = EMPTY;
            } else {
                b = new InnerSubscription[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (subscribers.compareAndSet(a, b)) {
                break;
            }
        }
    }

    static final class InnerSubscription<T> extends DeferredScalarSubscription<T> {

        final PerhapsProcessor<T> parent;

        private static final long serialVersionUID = -8241863418761502064L;

        InnerSubscription(Subscriber<? super T> downstream, PerhapsProcessor<T> parent) {
            super(downstream);
            this.parent = parent;
        }

        @Override
        public void cancel() {
            super.cancel();
            parent.remove(this);
        }

        void error(Throwable ex) {
            downstream.onError(ex);
        }

        void complete() {
            downstream.onComplete();
        }
    }
}
