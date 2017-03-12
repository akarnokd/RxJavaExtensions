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

package hu.akarnokd.rxjava2.operators;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.*;

/**
 * Emit and recursively expand elements from upstream.
 * @param <T> the value type
 * @since 0.16.1
 */
final class FlowableExpand<T> extends Flowable<T> implements FlowableTransformer<T, T> {

    final Flowable<T> source;

    final Function<? super T, ? extends Publisher<? extends T>> expander;

    final ExpandStrategy strategy;

    final int capacityHint;

    FlowableExpand(Flowable<T> source, Function<? super T, ? extends Publisher<? extends T>> expander,
            ExpandStrategy strategy, int capacityHint) {
        this.source = source;
        this.expander = expander;
        this.strategy = strategy;
        this.capacityHint = capacityHint;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (strategy == ExpandStrategy.BREATH_FIRST) {
            ExpandBreathSubscriber<T> parent = new ExpandBreathSubscriber<T>(s, expander, capacityHint);
            parent.queue.offer(source);
            s.onSubscribe(parent);
            parent.drainQueue();
        } else {
            ExpandDepthSubscription<T> parent = new ExpandDepthSubscription<T>(s, expander, capacityHint);
            parent.source = source;
            s.onSubscribe(parent);
        }
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableExpand<T>(upstream, expander, strategy, capacityHint);
    }

    static final class ExpandBreathSubscriber<T> extends SubscriptionArbiter implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -8200116117441115256L;

        final Subscriber<? super T> actual;

        final Function<? super T, ? extends Publisher<? extends T>> expander;

        final SimplePlainQueue<Publisher<? extends T>> queue;

        final AtomicInteger wip;

        volatile boolean active;

        long produced;

        ExpandBreathSubscriber(Subscriber<? super T> actual,
                Function<? super T, ? extends Publisher<? extends T>> expander, int capacityHint) {
            this.actual = actual;
            this.expander = expander;
            this.wip = new AtomicInteger();
            this.queue = new SpscLinkedArrayQueue<Publisher<? extends T>>(capacityHint);
        }

        @Override
        public void onSubscribe(Subscription s) {
            setSubscription(s);
        }

        @Override
        public void onNext(T t) {
            produced++;
            actual.onNext(t);

            Publisher<? extends T> p;
            try {
                p = ObjectHelper.requireNonNull(expander.apply(t), "The expander returned a null Publisher");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                super.cancel();
                actual.onError(ex);
                drainQueue();
                return;
            }

            queue.offer(p);
        }

        @Override
        public void onError(Throwable t) {
            setSubscription(SubscriptionHelper.CANCELLED);
            super.cancel();
            actual.onError(t);
            drainQueue();
        }

        @Override
        public void onComplete() {
            active = false;
            drainQueue();
        }

        @Override
        public void cancel() {
            super.cancel();
            drainQueue();
        }

        void drainQueue() {
            if (wip.getAndIncrement() == 0) {
                do {
                    SimplePlainQueue<Publisher<? extends T>> q = queue;
                    if (isCancelled()) {
                        q.clear();
                    } else {
                        if (!active) {
                            if (q.isEmpty()) {
                                setSubscription(SubscriptionHelper.CANCELLED);
                                super.cancel();
                                actual.onComplete();
                            } else {
                                Publisher<? extends T> p = q.poll();
                                long c = produced;
                                if (c != 0L) {
                                    produced = 0L;
                                    produced(c);
                                }
                                active = true;
                                p.subscribe(this);
                            }
                        }
                    }
                } while (wip.decrementAndGet() != 0);
            }
        }
    }

    static final class ExpandDepthSubscription<T>
    extends AtomicInteger
    implements Subscription {

        private static final long serialVersionUID = -2126738751597075165L;

        final Subscriber<? super T> actual;

        final Function<? super T, ? extends Publisher<? extends T>> expander;

        final AtomicThrowable error;

        final AtomicInteger active;

        final AtomicLong requested;

        ArrayDeque<ExpandDepthSubscriber> subscriptionStack;

        volatile boolean cancelled;

        Publisher<? extends T> source;

        long consumed;

        ExpandDepthSubscriber current;

        ExpandDepthSubscription(Subscriber<? super T> actual,
                Function<? super T, ? extends Publisher<? extends T>> expander,
                        int capacityHint) {
            this.actual = actual;
            this.expander = expander;
            this.subscriptionStack = new ArrayDeque<ExpandDepthSubscriber>();
            this.error = new AtomicThrowable();
            this.active = new AtomicInteger();
            this.requested = new AtomicLong();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drainQueue();
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                ArrayDeque<ExpandDepthSubscriber> q;
                synchronized (this) {
                    q = subscriptionStack;
                    subscriptionStack = null;
                }

                if (q != null) {
                    while (!q.isEmpty()) {
                        q.poll().dispose();
                    }
                }
            }
        }

        ExpandDepthSubscriber pop() {
            synchronized (this) {
                ArrayDeque<ExpandDepthSubscriber> q = subscriptionStack;
                return q != null ? q.pollFirst() : null;
            }
        }

        boolean push(ExpandDepthSubscriber subscriber) {
            synchronized (this) {
                ArrayDeque<ExpandDepthSubscriber> q = subscriptionStack;
                if (q != null) {
                    q.offerFirst(subscriber);
                    return true;
                }
                return false;
            }
        }

        void drainQueue() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Subscriber<? super T> a = actual;
            long e = consumed;
            AtomicInteger n = active;

            for (;;) {
                if (cancelled) {
                    source = null;
                    current = null;
                    return;
                }

                ExpandDepthSubscriber curr = current;
                Publisher<? extends T> p = source;

                if (curr == null && p != null) {
                    source = null;
                    n.getAndIncrement();

                    ExpandDepthSubscriber eds = new ExpandDepthSubscriber();
                    curr = eds;
                    current = eds;

                    p.subscribe(eds);
                } else {

                    boolean currentDone = curr.done;
                    T v = curr.value;

                    boolean newSource = false;
                    if (v != null && e != requested.get()) {
                        curr.value = null;
                        a.onNext(v);
                        e++;

                        try {
                            p = ObjectHelper.requireNonNull(expander.apply(v), "The expander returned a null Publisher");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            p = null;
                            curr.dispose();
                            curr.done = true;
                            currentDone = true;
                            v = null;
                            error.addThrowable(ex);
                        }

                        if (p != null) {
                            if (push(curr)) {
                                n.getAndIncrement();
                                curr = new ExpandDepthSubscriber();
                                current = curr;

                                p.subscribe(curr);
                                newSource = true;
                            }
                        }
                    }

                    if (!newSource) {
                        if (currentDone && v == null) {
                            if (n.decrementAndGet() == 0) {
                                Throwable ex = error.terminate();
                                if (ex != null) {
                                    a.onError(ex);
                                } else {
                                    a.onComplete();
                                }
                                return;
                            }
                            curr = pop();
                            current = curr;
                            curr.requestOne();
                            continue;
                        }
                    }
                }
                int w = get();
                if (missed == w) {
                    consumed = e;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }

        void innerNext(ExpandDepthSubscriber inner, T t) {
            drainQueue();
        }

        void innerError(ExpandDepthSubscriber inner, Throwable t) {
            error.addThrowable(t);
            inner.done = true;
            drainQueue();
        }

        void innerComplete(ExpandDepthSubscriber inner) {
            inner.done = true;
            drainQueue();
        }

        final class ExpandDepthSubscriber
        extends AtomicReference<Subscription>
        implements FlowableSubscriber<T> {

            private static final long serialVersionUID = 4198645419772153739L;

            volatile boolean done;

            volatile T value;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(1);
                }
            }

            @Override
            public void onNext(T t) {
                if (!SubscriptionHelper.isCancelled(get())) {
                    value = t;
                    innerNext(this, t);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (!SubscriptionHelper.isCancelled(get())) {
                    innerError(this, t);
                }
            }

            @Override
            public void onComplete() {
                if (!SubscriptionHelper.isCancelled(get())) {
                    innerComplete(this);
                }
            }

            public void requestOne() {
                get().request(1);
            }

            public void dispose() {
                SubscriptionHelper.cancel(this);
            }
        }
    }
}
