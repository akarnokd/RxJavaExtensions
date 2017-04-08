/*
 * Copyright 2016-2017 David Karnok
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

package hu.akarnokd.rxjava2.processors;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.FlowableProcessor;

/**
 * A {@code Processor} implementation that coordinates downstream requrests through
 * a front-buffer and stable-prefetching, optionally cancelling the upstream if all
 * subscribers have cancelled.
 * @param <T> the input and output value type
 * @since 0.16.4
 */
public final class MulticastProcessor<T> extends FlowableProcessor<T> {

    final AtomicInteger wip;

    final AtomicReference<Subscription> upstream;

    final AtomicReference<MulticastSubscription<T>[]> subscribers;

    final AtomicBoolean once;

    final int bufferSize;

    final int limit;

    final boolean refcount;

    volatile SimpleQueue<T> queue;

    boolean done;
    Throwable error;

    int consumed;

    int fusionMode;

    @SuppressWarnings("rawtypes")
    static final MulticastSubscription[] EMPTY = new MulticastSubscription[0];

    @SuppressWarnings("rawtypes")
    static final MulticastSubscription[] TERMINATED = new MulticastSubscription[0];

    /**
     * Constructs a fresh instance with the default Flowable.bufferSize() prefetch
     * amount and no refCount-behavior.
     */
    public MulticastProcessor() {
        this(bufferSize(), false);
    }

    /**
     * Constructs a fresh instance with the given prefetch amount and no refCount behavior.
     * @param bufferSize the prefetch amount
     */
    public MulticastProcessor(int bufferSize) {
        this(bufferSize, false);
    }

    /**
     * Constructs a fres instance with the given prefetch amount and the optional
     * refCount-behavior.
     * @param bufferSize the prefech amount
     * @param refCount if true and if all Subscribers have unsubscribed, the upstream
     * is cancelled
     */
    @SuppressWarnings("unchecked")
    public MulticastProcessor(int bufferSize, boolean refCount) {
        this.bufferSize = bufferSize;
        this.limit = bufferSize - (bufferSize >> 2);
        this.wip = new AtomicInteger();
        this.subscribers = new AtomicReference<MulticastSubscription<T>[]>(EMPTY);
        this.upstream = new AtomicReference<Subscription>();
        this.refcount = refCount;
        this.once = new AtomicBoolean();
    }

    public void start() {
        if (SubscriptionHelper.setOnce(upstream, EmptySubscription.INSTANCE)) {
            queue = new SpscArrayQueue<T>(bufferSize);
        }
    }

    public void startUnbounded() {
        if (SubscriptionHelper.setOnce(upstream, EmptySubscription.INSTANCE)) {
            queue = new SpscLinkedArrayQueue<T>(bufferSize);
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(upstream, s)) {
            if (s instanceof QueueSubscription) {
                @SuppressWarnings("unchecked")
                QueueSubscription<T> qs = (QueueSubscription<T>)s;

                int m = qs.requestFusion(QueueSubscription.ANY);
                if (m == QueueSubscription.SYNC) {
                    fusionMode = m;
                    queue = qs;
                    done = true;
                    drain();
                    return;
                }
                if (m == QueueSubscription.ASYNC) {
                    fusionMode = m;
                    queue = qs;

                    s.request(bufferSize);
                    return;
                }
            }

            queue = new SpscArrayQueue<T>(bufferSize);

            s.request(bufferSize);
        }
    }

    @Override
    public void onNext(T t) {
        if (once.get()) {
            return;
        }
        if (t == null) {
            throw new NullPointerException("t is null");
        }
        if (fusionMode == QueueSubscription.NONE) {
            if (!queue.offer(t)) {
                SubscriptionHelper.cancel(upstream);
                onError(new MissingBackpressureException());
                return;
            }
        }
        drain();
    }

    @Override
    public void onError(Throwable t) {
        if (t == null) {
            throw new NullPointerException("t is null");
        }
        if (once.compareAndSet(false, true)) {
            error = t;
            done = true;
            drain();
        } else {
            RxJavaPlugins.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (once.compareAndSet(false, true)) {
            done = true;
            drain();
        }
    }

    @Override
    public boolean hasSubscribers() {
        return subscribers.get().length != 0;
    }

    @Override
    public boolean hasThrowable() {
        return once.get() && error != null;
    }

    @Override
    public boolean hasComplete() {
        return once.get() && error == null;
    }

    @Override
    public Throwable getThrowable() {
        return once.get() ? error : null;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        MulticastSubscription<T> ms = new MulticastSubscription<T>(s, this);
        s.onSubscribe(ms);
        if (add(ms)) {
            if (ms.get() == Long.MIN_VALUE) {
                remove(ms);
            } else {
                drain();
            }
        } else {
            if (once.get() || !refcount) {
                Throwable ex = error;
                if (ex != null) {
                    s.onError(ex);
                    return;
                }
            }
            s.onComplete();
        }
    }

    boolean add(MulticastSubscription<T> inner) {
        // TODO Auto-generated method stub
        return false;
    }

    void remove(MulticastSubscription<T> inner) {
        // TODO Auto-generated method stub
        
    }

    void drain() {
        // TODO Auto-generated method stub
        
    }

    static final class MulticastSubscription<T> extends AtomicLong implements Subscription {

        private static final long serialVersionUID = -363282618957264509L;

        final Subscriber<? super T> actual;

        final MulticastProcessor<T> parent;

        long emitted;

        MulticastSubscription(Subscriber<? super T> actual, MulticastProcessor<T> parent) {
            this.actual = actual;
            this.parent = parent;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                for (;;) {
                    long r = get();
                    if (r == Long.MIN_VALUE || r == Long.MAX_VALUE) {
                        break;
                    }
                    long u = r + n;
                    if (u < 0L) {
                        u = Long.MAX_VALUE;
                    }
                    if (compareAndSet(r, u)) {
                        parent.drain();
                        break;
                    }
                }
            }
        }

        @Override
        public void cancel() {
            if (getAndSet(Long.MIN_VALUE) != Long.MIN_VALUE) {
                parent.remove(this);
            }
        }

        void onNext(T t) {
            if (get() != Long.MIN_VALUE) {
                actual.onNext(t);
            }
        }

        void onError(Throwable t) {
            if (get() != Long.MIN_VALUE) {
                actual.onError(t);
            }
        }

        void onComplete() {
            if (get() != Long.MIN_VALUE) {
                actual.onComplete();
            }
        }
    }
}
