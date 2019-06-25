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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.AtomicThrowable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Allows stopping and resuming the flow of the main source when a secondary flow
 * signals false and true respectively.
 *
 * @param <T> the main source's value type
 * 
 * @since 0.7.2
 */
final class FlowableValve<T> extends Flowable<T> implements FlowableOperator<T, T>, FlowableTransformer<T, T> {

    final Publisher<? extends T> source;

    final Publisher<Boolean> other;

    final boolean defaultOpen;

    final int bufferSize;

    FlowableValve(Publisher<? extends T> source, Publisher<Boolean> other, boolean defaultOpen, int bufferSize) {
        this.source = source;
        this.other = other;
        this.defaultOpen = defaultOpen;
        this.bufferSize = bufferSize;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(apply(s));
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> subscriber) {
        ValveMainSubscriber<T> parent = new ValveMainSubscriber<T>(subscriber, bufferSize, defaultOpen);
        subscriber.onSubscribe(parent);
        other.subscribe(parent.other);
        return parent;
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableValve<T>(upstream, other, defaultOpen, bufferSize);
    }

    static final class ValveMainSubscriber<T>
    extends AtomicInteger
    implements Subscriber<T>, Subscription {

        private static final long serialVersionUID = -2233734924340471378L;

        final Subscriber<? super T> downstream;

        final AtomicReference<Subscription> upstream;

        final AtomicLong requested;

        final SimplePlainQueue<T> queue;

        final OtherSubscriber other;

        final AtomicThrowable error;

        volatile boolean done;

        volatile boolean gate;

        volatile boolean cancelled;

        ValveMainSubscriber(Subscriber<? super T> downstream, int bufferSize, boolean defaultOpen) {
            this.downstream = downstream;
            this.queue = new SpscLinkedArrayQueue<T>(bufferSize);
            this.gate = defaultOpen;
            this.other = new OtherSubscriber();
            this.requested = new AtomicLong();
            this.error = new AtomicThrowable();
            this.upstream = new AtomicReference<Subscription>();
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(this.upstream, requested, s);
        }

        @Override
        public void onNext(T t) {
            queue.offer(t);
            drain();
        }

        @Override
        public void onError(Throwable t) {
            if (error.addThrowable(t)) {
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(upstream, requested, n);
        }

        @Override
        public void cancel() {
            cancelled = true;
            SubscriptionHelper.cancel(upstream);
            SubscriptionHelper.cancel(other);
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;

            SimplePlainQueue<T> q = queue;
            Subscriber<? super T> a = downstream;
            AtomicThrowable error = this.error;

            for (;;) {
                for (;;) {
                    if (cancelled) {
                        q.clear();
                        return;
                    }

                    if (error.get() != null) {
                        Throwable ex = error.terminate();
                        q.clear();
                        SubscriptionHelper.cancel(upstream);
                        SubscriptionHelper.cancel(other);
                        a.onError(ex);
                        return;
                    }

                    if (!gate) {
                        break;
                    }

                    boolean d = done;
                    T v = q.poll();
                    boolean empty = v == null;

                    if (d && empty) {
                        SubscriptionHelper.cancel(other);
                        a.onComplete();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(v);
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        void change(boolean state) {
            gate = state;
            if (state) {
                drain();
            }
        }

        void innerError(Throwable ex) {
            onError(ex);
        }

        void innerComplete() {
            innerError(new IllegalStateException("The valve source completed unexpectedly."));
        }

        final class OtherSubscriber extends AtomicReference<Subscription> implements FlowableSubscriber<Boolean> {

            private static final long serialVersionUID = -3076915855750118155L;

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(Boolean t) {
                change(t);
            }

            @Override
            public void onError(Throwable t) {
                innerError(t);
            }

            @Override
            public void onComplete() {
                innerComplete();
            }
        }
    }
}
