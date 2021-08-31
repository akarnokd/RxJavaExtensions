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

package hu.akarnokd.rxjava3.operators;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.operators.SimplePlainQueue;
import io.reactivex.rxjava3.operators.SpscLinkedArrayQueue;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.*;

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

    final boolean delayErrors;

    FlowableExpand(Flowable<T> source, Function<? super T, ? extends Publisher<? extends T>> expander,
            ExpandStrategy strategy, int capacityHint, boolean delayErrors) {
        this.source = source;
        this.expander = expander;
        this.strategy = strategy;
        this.capacityHint = capacityHint;
        this.delayErrors = delayErrors;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (strategy != ExpandStrategy.DEPTH_FIRST) {
            ExpandBreadthSubscriber<T> parent = new ExpandBreadthSubscriber<>(s, expander, capacityHint, delayErrors);
            parent.queue.offer(source);
            s.onSubscribe(parent);
            parent.drainQueue();
        } else {
            ExpandDepthSubscription<T> parent = new ExpandDepthSubscription<>(s, expander, capacityHint, delayErrors);
            parent.source = source;
            s.onSubscribe(parent);
        }
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableExpand<>(upstream, expander, strategy, capacityHint, delayErrors);
    }

    static final class ExpandBreadthSubscriber<T> extends SubscriptionArbiter implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -8200116117441115256L;

        final Subscriber<? super T> downstream;

        final Function<? super T, ? extends Publisher<? extends T>> expander;

        final SimplePlainQueue<Publisher<? extends T>> queue;

        final AtomicInteger wip;

        final boolean delayErrors;

        final AtomicThrowable errors;

        volatile boolean active;

        long produced;

        ExpandBreadthSubscriber(Subscriber<? super T> downstream,
                Function<? super T, ? extends Publisher<? extends T>> expander, int capacityHint, boolean delayErrors) {
            super(false);
            this.downstream = downstream;
            this.expander = expander;
            this.wip = new AtomicInteger();
            this.queue = new SpscLinkedArrayQueue<>(capacityHint);
            this.errors = new AtomicThrowable();
            this.delayErrors = delayErrors;
        }

        @Override
        public void onSubscribe(Subscription s) {
            setSubscription(s);
        }

        @Override
        public void onNext(T t) {
            produced++;
            downstream.onNext(t);

            Publisher<? extends T> p;
            try {
                p = Objects.requireNonNull(expander.apply(t), "The expander returned a null Publisher");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                super.cancel();
                downstream.onError(ex);
                drainQueue();
                return;
            }

            queue.offer(p);
        }

        @Override
        public void onError(Throwable t) {
            setSubscription(SubscriptionHelper.CANCELLED);
            if (delayErrors) {
                errors.tryAddThrowableOrReport(t);
                active = false;
            } else {
                super.cancel();
                downstream.onError(t);
            }
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
            errors.tryTerminateAndReport();
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
                                errors.tryTerminateConsumer(downstream);
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

        final Subscriber<? super T> downstream;

        final Function<? super T, ? extends Publisher<? extends T>> expander;

        final AtomicThrowable error;

        final AtomicInteger active;

        final AtomicLong requested;

        final AtomicReference<Object> current;

        final boolean delayErrors;

        ArrayDeque<ExpandDepthSubscriber> subscriptionStack;

        volatile boolean cancelled;

        Publisher<? extends T> source;

        long consumed;

        ExpandDepthSubscription(Subscriber<? super T> downstream,
                Function<? super T, ? extends Publisher<? extends T>> expander,
                        int capacityHint, boolean delayErrors) {
            this.downstream = downstream;
            this.expander = expander;
            this.subscriptionStack = new ArrayDeque<>();
            this.error = new AtomicThrowable();
            this.active = new AtomicInteger();
            this.requested = new AtomicLong();
            this.current = new AtomicReference<>();
            this.delayErrors = delayErrors;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drainQueue();
            }
        }

        @SuppressWarnings("unchecked")
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

                Object o = current.getAndSet(this);
                if (o != this && o != null) {
                    ((ExpandDepthSubscriber)o).dispose();
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

        boolean setCurrent(ExpandDepthSubscriber inner) {
            for (;;) {
                Object o = current.get();
                if (o == this) {
                    if (inner != null) {
                        inner.dispose();
                    }
                    return false;
                }
                if (current.compareAndSet(o, inner)) {
                    return true;
                }
            }
        }

        void drainQueue() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Subscriber<? super T> a = downstream;
            long e = consumed;
            AtomicInteger n = active;

            for (;;) {
                Object o = current.get();
                if (cancelled || o == this) {
                    source = null;
                    return;
                }

                @SuppressWarnings("unchecked")
                ExpandDepthSubscriber curr = (ExpandDepthSubscriber)o;
                Publisher<? extends T> p = source;

                if (curr == null && p != null) {
                    source = null;
                    n.getAndIncrement();

                    ExpandDepthSubscriber eds = new ExpandDepthSubscriber();
                    curr = eds;
                    if (setCurrent(eds)) {
                        p.subscribe(eds);
                    } else {
                        return;
                    }
                } else {

                    boolean currentDone = curr.done;

                    if (!delayErrors && error.get() != null) {
                        cancel();
                        error.tryTerminateConsumer(a);
                        return;
                    }

                    T v = curr.value;

                    boolean newSource = false;
                    if (v != null && e != requested.get()) {
                        curr.value = null;
                        a.onNext(v);
                        e++;

                        try {
                            p = Objects.requireNonNull(expander.apply(v), "The expander returned a null Publisher");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            p = null;
                            curr.dispose();
                            curr.done = true;
                            currentDone = true;
                            v = null;
                            error.tryAddThrowableOrReport(ex);
                        }

                        if (p != null) {
                            if (push(curr)) {
                                n.getAndIncrement();
                                curr = new ExpandDepthSubscriber();
                                if (setCurrent(curr)) {
                                    p.subscribe(curr);
                                    newSource = true;
                                } else {
                                    return;
                                }
                            }
                        }
                    }

                    if (!newSource) {
                        if (currentDone && v == null) {
                            if (n.decrementAndGet() == 0) {
                                error.tryTerminateConsumer(a);
                                return;
                            }
                            curr = pop();
                            if (curr != null && setCurrent(curr)) {
                                curr.requestOne();
                                continue;
                            } else {
                                return;
                            }
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
            error.tryAddThrowableOrReport(t);
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
                if (SubscriptionHelper.CANCELLED != get()) {
                    value = t;
                    innerNext(this, t);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (SubscriptionHelper.CANCELLED != get()) {
                    innerError(this, t);
                }
            }

            @Override
            public void onComplete() {
                if (SubscriptionHelper.CANCELLED != get()) {
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
