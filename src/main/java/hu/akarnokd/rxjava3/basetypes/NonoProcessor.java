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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.plugins.RxJavaPlugins;

/**
 * A hot Nono that signals the terminal event to Subscribers.
 * <p>
 * NonoProcessor is thread-safe and naturally serialized on its onXXX methods.
 * 
 * @since 0.12.0
 */
public final class NonoProcessor extends Nono implements Processor<Void, Void> {

    static final NonoSubscription[] EMPTY = new NonoSubscription[0];

    static final NonoSubscription[] TERMINATED = new NonoSubscription[0];

    Throwable error;

    final AtomicReference<NonoSubscription[]> subscribers;

    final AtomicBoolean once;

    /**
     * Creates a NonoProcessor instance ready to receive events and Subscribers.
     * @return the new NonoProcessor instance
     */
    public static NonoProcessor create() {
        return new NonoProcessor();
    }

    NonoProcessor() {
        subscribers = new AtomicReference<NonoSubscription[]>(EMPTY);
        once = new AtomicBoolean();
    }

    boolean add(NonoSubscription inner) {
        for (;;) {
            NonoSubscription[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }
            int n = a.length;
            NonoSubscription[] b = new NonoSubscription[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = inner;
            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    void delete(NonoSubscription inner) {
        for (;;) {
            NonoSubscription[] a = subscribers.get();
            int n = a.length;
            if (n == 0) {
                return;
            }
            int j = -1;
            for (int i = 0; i < a.length; i++) {
                NonoSubscription ns = a[i];
                if (ns == inner) {
                    j = i;
                    break;
                }
            }

            if (j < 0) {
                return;
            }

            NonoSubscription[] b;
            if (n == 1) {
                b = EMPTY;
            } else {
                b = new NonoSubscription[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (subscribers.compareAndSet(a, b)) {
                return;
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

    @Override
    public void onNext(Void t) {
        throw new NullPointerException();
    }

    @Override
    public void onError(Throwable t) {
        if (once.compareAndSet(false, true)) {
            if (t == null) {
                t = new NullPointerException();
            }
            error = t;
            for (NonoSubscription ns : subscribers.getAndSet(TERMINATED)) {
                ns.doError(t);
            }
        } else {
            RxJavaPlugins.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (once.compareAndSet(false, true)) {
            for (NonoSubscription ns : subscribers.getAndSet(TERMINATED)) {
                ns.doComplete();
            }
        }
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        NonoSubscription ns = new NonoSubscription(s, this);
        s.onSubscribe(ns);
        if (add(ns)) {
            if (ns.get() != 0) {
                delete(ns);
            }
        } else {
            Throwable ex = error;
            if (ex != null) {
                ns.doError(ex);
            } else {
                ns.doComplete();
            }
        }
    }

    /**
     * Returns true if this NonoProcessor currently has Subscribers.
     * @return true if there are subscribers
     */
    public boolean hasSubscribers() {
        return subscribers.get().length != 0;
    }

    /**
     * Returns true if this NonoProcessor has completed normally.
     * @return true if completed normally
     */
    public boolean hasComplete() {
        return subscribers.get() == TERMINATED && error == null;
    }

    /**
     * Returns true if this NonoProcessor has terminated with an error.
     * @return true if terminated with an error
     * @see #getThrowable()
     */
    public boolean hasThrowable() {
        return subscribers.get() == TERMINATED && error != null;
    }

    /**
     * Returns the error that terminated this NonoProcessor if
     * {@link #hasThrowable()} returns true.
     * @return the error Throwable that terminated this NonoProcessor
     */
    public Throwable getThrowable() {
        return subscribers.get() == TERMINATED ? error : null;
    }

    /**
     * Returns the current number of subscribers.
     * @return the current number of subscribers
     */
    /* test */ int subscriberCount() {
        return subscribers.get().length;
    }

    /**
     * Fuseable subscription handed to the subscribers.
     */
    static final class NonoSubscription extends BasicNonoIntQueueSubscription {

        private static final long serialVersionUID = 8377121611843740196L;

        final Subscriber<? super Void> downstream;

        final NonoProcessor parent;

        NonoSubscription(Subscriber<? super Void> downstream, NonoProcessor parent) {
            this.downstream = downstream;
            this.parent = parent;
        }

        @Override
        public void cancel() {
            if (compareAndSet(0, 1)) {
                parent.delete(this);
            }
        }

        void doError(Throwable t) {
            if (compareAndSet(0, 1)) {
                downstream.onError(t);
            }
        }

        void doComplete() {
            if (compareAndSet(0, 1)) {
                downstream.onComplete();
            }
        }
    }
}
