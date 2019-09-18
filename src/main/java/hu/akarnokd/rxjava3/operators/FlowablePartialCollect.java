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

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Allows converting upstream items into output objects where an upstream item
 * may represent such output objects partially or may represent more than one
 * output object.
 * <p>
 * For example, given a stream of {@code byte[]} where each array could contain part
 * of a larger object, and thus more than one subsequent arrays are required to construct
 * the output object. The same array could also contain more than one output items, therefore,
 * it should be kept around in case the output is backpressured.
 * @param <T> the upstream value type
 * @param <I> the type that indicates where the first cached item should be read from
 * @param <A> the accumulator type used to collect up partial data
 * @param <R> the output type
 * @since 0.18.9
 */
final class FlowablePartialCollect<T, I, A, R> extends Flowable<R>
implements FlowableTransformer<T, R> {

    final Flowable<T> source;

    final Consumer<? super PartialCollectEmitter<T, I, A, R>> handler;

    final Consumer<? super T> cleaner;

    final int prefetch;

    FlowablePartialCollect(Flowable<T> source,
            Consumer<? super PartialCollectEmitter<T, I, A, R>> handler,
            Consumer<? super T> cleaner,
            int prefetch) {
        this.source = source;
        this.handler = handler;
        this.cleaner = cleaner;
        this.prefetch = prefetch;
    }

    @Override
    public Publisher<R> apply(Flowable<T> upstream) {
        return new FlowablePartialCollect<T, I, A, R>(upstream, handler, cleaner, prefetch);
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new PartialCollectSubscriber<T, I, A, R>(s, handler, cleaner, prefetch));
    }

    static final class PartialCollectSubscriber<T, I, A, R>
    extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription, PartialCollectEmitter<T, I, A, R> {

        private static final long serialVersionUID = -2029240720070492688L;

        final Subscriber<? super R> downstream;

        final Consumer<? super PartialCollectEmitter<T, I, A, R>> handler;

        final Consumer<? super T> cleaner;

        final int prefetch;

        final int limit;

        final AtomicThrowable errors;

        final AtomicReferenceArray<T> queue;

        final AtomicLong producerIndex;

        final AtomicLong requested;

        long consumerIndex;

        Subscription upstream;

        volatile boolean cancelled;

        volatile boolean done;

        I index;

        A accumulator;

        long emitted;

        int consumed;

        boolean handlerDone;

        PartialCollectSubscriber(Subscriber<? super R> downstream,
                Consumer<? super PartialCollectEmitter<T, I, A, R>> handler,
                Consumer<? super T> cleaner,
                int prefetch) {
            this.downstream = downstream;
            this.handler = handler;
            this.cleaner = cleaner;
            this.prefetch = prefetch;
            this.errors = new AtomicThrowable();
            this.queue = new AtomicReferenceArray<T>(Pow2.roundToPowerOfTwo(prefetch));
            this.producerIndex = new AtomicLong();
            this.requested = new AtomicLong();
            this.limit = prefetch - (prefetch >> 2);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
                s.request(prefetch);
            }
        }

        @Override
        public void onNext(T t) {
            AtomicReferenceArray<T> q = queue;
            int mask = q.length() - 1;
            long pi = producerIndex.get();
            int offset = (int)pi & mask;
            q.lazySet(offset, t);
            producerIndex.lazySet(pi + 1);
            drain();
        }

        @Override
        public void onError(Throwable t) {
            if (errors.tryAddThrowableOrReport(t)) {
                done = true;
                drain();
            }
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @Override
        public void cancel() {
            cancelled = true;
            upstream.cancel();
            errors.tryTerminateAndReport();
            drain();
        }

        @Override
        public void request(long n) {
            BackpressureHelper.add(requested, n);
            drain();
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isComplete() {
            return done;
        }

        @Override
        public int size() {
            return (int)(producerIndex.get() - consumerIndex);
        }

        @Override
        public T getItem(int index) {
            long ci = consumerIndex;
            AtomicReferenceArray<T> q = queue;
            int mask = q.length() - 1;
            int offset = (int)(ci + index) & mask;
            return q.get(offset);
        }

        @Override
        public void dropItems(int count) {
            int replenish = 0;
            long ci = consumerIndex;
            long pi = producerIndex.get();

            AtomicReferenceArray<T> q = queue;
            int mask = q.length() - 1;

            while (pi != ci && count != replenish) {
                int offset = (int)ci & mask;
                cleanupItem(q.get(offset));
                q.lazySet(offset, null);
                ci++;
                replenish++;
            }
            consumerIndex = ci;

            int c = consumed + replenish;
            if (c >= limit) {
                consumed = 0;
                upstream.request(c);
            } else {
                consumed = c;
            }
        }

        @Override
        public I getIndex() {
            return index;
        }

        @Override
        public void setIndex(I newIndex) {
            index = newIndex;
        }

        @Override
        public A getAccumulator() {
            return accumulator;
        }

        @Override
        public void setAccumulator(A newAccumulator) {
            this.accumulator = newAccumulator;
        }

        @Override
        public void next(R item) {
            long e = emitted;
            if (e != requested.get()) {
                emitted = e + 1;
                downstream.onNext(item);
            } else {
                handlerDone = true;
                throw new MissingBackpressureException();
            }
        }

        @Override
        public void complete() {
            handlerDone = true;
        }

        @Override
        public void cleanupItem(T item) {
            try {
                cleaner.accept(item);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public long demand() {
            return requested.get() - emitted;
        }

        void cleanup() {
            long ci = consumerIndex;
            long pi = producerIndex.get();

            AtomicReferenceArray<T> q = queue;
            int mask = q.length() - 1;

            while (pi != ci) {
                int offset = (int)ci & mask;
                cleanupItem(q.get(offset));
                q.lazySet(offset, null);
                ci++;
            }
            consumerIndex = ci;

            index = null;
            accumulator = null;
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;

            for (;;) {

                if (cancelled) {
                    cleanup();
                } else {
                    for (;;) {
                        boolean d = this.done;

                        if (d && errors.get() != null) {
                            errors.tryTerminateConsumer(downstream);
                            cleanup();
                            cancelled = true;
                            break;
                        }

                        if (this.handlerDone) {
                            errors.tryTerminateConsumer(downstream);
                            cleanup();
                            cancelled = true;
                            break;
                        }

                        long e = emitted;
                        long ci = consumerIndex;

                        try {
                            handler.accept(this);
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            upstream.cancel();
                            errors.tryAddThrowableOrReport(ex);
                            this.handlerDone = true;
                            continue;
                        }

                        if (this.handlerDone) {
                            continue;
                        }
                        // if there was no emission and no consumption, quit
                        if (e == emitted && ci == consumerIndex) {
                            break;
                        }
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }
}
