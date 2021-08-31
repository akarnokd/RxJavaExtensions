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

import java.util.concurrent.TimeUnit;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.operators.SimplePlainQueue;
import io.reactivex.rxjava3.operators.SpscLinkedArrayQueue;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Makes sure there is at least the given amount of time between emissions of the upstream elements.
 * <pre><code>
 * a-b-c-d-e--f----g-|
 * spanout(---)
 * a---b---c---d---e---f----g-|
 * </code></pre>
 * @param <T> the upstream value type
 * @since 0.9.0
 */
final class FlowableSpanout<T> extends Flowable<T> implements FlowableTransformer<T, T> {

    final Publisher<T> source;

    final long initialSpan;

    final long betweenSpan;

    final Scheduler scheduler;

    final boolean delayError;

    final int bufferSize;

    FlowableSpanout(Publisher<T> source,
            long initialSpan, long betweenSpan, TimeUnit unit,
            Scheduler scheduler, boolean delayError,
            int bufferSize) {
        this.source = source;
        this.initialSpan = unit.toNanos(initialSpan);
        this.betweenSpan = unit.toNanos(betweenSpan);
        this.scheduler = scheduler;
        this.delayError = delayError;
        this.bufferSize = bufferSize;
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableSpanout<>(upstream, initialSpan, betweenSpan, TimeUnit.NANOSECONDS, scheduler, delayError, bufferSize);
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new SpanoutSubscriber<T>(s, initialSpan, betweenSpan,
                scheduler.createWorker(), delayError, bufferSize));
    }

    static final class SpanoutSubscriber<T> implements Subscriber<T>, Subscription, Runnable {

        final Subscriber<? super T> downstream;

        final long initialSpan;

        final long betweenSpan;

        final Worker worker;

        final boolean delayError;

        final SimplePlainQueue<T> queue;

        long lastEvent;

        Subscription upstream;

        volatile Object terminalEvent;

        SpanoutSubscriber(Subscriber<? super T> downstream, long initialSpan, long betweenSpan,
                Worker worker, boolean delayError, int bufferSize) {
            this.downstream = downstream;
            this.initialSpan = initialSpan;
            this.betweenSpan = betweenSpan;
            this.worker = worker;
            this.delayError = delayError;
            this.lastEvent = -1L;
            this.queue = new SpscLinkedArrayQueue<>(bufferSize);
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
            queue.offer(t);

            long now = worker.now(TimeUnit.NANOSECONDS);
            long last = lastEvent;
            long between = betweenSpan;

            if (last == -1L) {
                lastEvent = now + between + initialSpan;
                worker.schedule(this, initialSpan, TimeUnit.NANOSECONDS);
            } else {
                if (last < now) {
                    lastEvent = now + between;
                    worker.schedule(this);
                } else {
                    lastEvent = last + between;
                    long next = last - now;
                    worker.schedule(this, next, TimeUnit.NANOSECONDS);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            terminalEvent = t;
            if (delayError) {
                long now = worker.now(TimeUnit.NANOSECONDS);
                worker.schedule(this, lastEvent - now - betweenSpan, TimeUnit.NANOSECONDS);
            } else {
                worker.schedule(this);
            }
        }

        @Override
        public void onComplete() {
            terminalEvent = this;
            long now = worker.now(TimeUnit.NANOSECONDS);
            worker.schedule(this, lastEvent - now - betweenSpan, TimeUnit.NANOSECONDS);
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            worker.dispose();
            upstream.cancel();
        }

        @Override
        public void run() {
            Object o = terminalEvent;
            if (o != null && o != this && !delayError) {
                queue.clear();
                downstream.onError((Throwable)o);
                worker.dispose();
                return;
            }

            T v = queue.poll();

            boolean empty = v == null;

            if (o != null && empty) {
                if (o == this) {
                    downstream.onComplete();
                } else {
                    downstream.onError((Throwable)o);
                }
                worker.dispose();
                return;
            }

            if (!empty) {
                downstream.onNext(v);
            }
        }
    }
}
