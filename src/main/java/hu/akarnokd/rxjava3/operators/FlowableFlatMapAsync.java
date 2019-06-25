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

import org.reactivestreams.*;

import hu.akarnokd.rxjava3.operators.FlowableFlatMapSync.*;
import io.reactivex.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.fuseable.SimpleQueue;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * FlatMap a bounded number of inner, non-trivial flows (unbound not supported).
 *
 * @param <T> the input value type
 * @param <R> the result value type
 *
 * @since 0.16.0
 */
final class FlowableFlatMapAsync<T, R> extends Flowable<R> implements FlowableTransformer<T, R> {

    final Publisher<T> source;

    final Function<? super T, ? extends Publisher<? extends R>> mapper;

    final int maxConcurrency;

    final int bufferSize;

    final boolean depthFirst;

    final Scheduler scheduler;

    FlowableFlatMapAsync(Publisher<T> source, Function<? super T, ? extends Publisher<? extends R>> mapper,
            int maxConcurrency, int bufferSize, boolean depthFirst, Scheduler scheduler) {
        this.source = source;
        this.mapper = mapper;
        this.maxConcurrency = maxConcurrency;
        this.bufferSize = bufferSize;
        this.depthFirst = depthFirst;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new FlatMapOuterSubscriber<T, R>(s, mapper, maxConcurrency, bufferSize, depthFirst, scheduler.createWorker()));
    }

    @Override
    public Publisher<R> apply(Flowable<T> upstream) {
        return new FlowableFlatMapAsync<T, R>(upstream, mapper, maxConcurrency, bufferSize, depthFirst, scheduler);
    }

    static final class FlatMapOuterSubscriber<T, R> extends BaseFlatMapOuterSubscriber<T, R> implements Runnable {
        private static final long serialVersionUID = -5109342841608286301L;

        final Scheduler.Worker worker;

        FlatMapOuterSubscriber(Subscriber<? super R> downstream,
                Function<? super T, ? extends Publisher<? extends R>> mapper, int maxConcurrency, int bufferSize,
                boolean depthFirst, Scheduler.Worker worker) {
            super(downstream, mapper, maxConcurrency, bufferSize, depthFirst);
            this.worker = worker;
        }

        @Override
        public void drain() {
            if (getAndIncrement() == 0) {
                worker.schedule(this);
            }
        }

        @Override
        public void run() {
            if (depthFirst) {
                depthFirst();
            } else {
                breadthFirst();
            }
        }

        @Override
        void cleanupAfter() {
            worker.dispose();
        }

        @Override
        public void innerNext(FlatMapInnerSubscriber<T, R> inner, R item) {
            SimpleQueue<R> q = inner.queue();
            q.offer(item);
            drain();
        }

        @Override
        public void innerError(FlatMapInnerSubscriber<T, R> inner, Throwable ex) {
            remove(inner);
            if (error.addThrowable(ex)) {
                inner.done = true;
                done = true;
                upstream.cancel();
                cancelInners();
                drain();
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void innerComplete(FlatMapInnerSubscriber<T, R> inner) {
            inner.done = true;
            drain();
        }
    }
}
