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

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * Requests one-by-one and emits those items on the given {@link Scheduler}, allowing an
 * interleaved usage of the {@code Worker} of the {@code Scheduler} by other tasks (aka "fair").
 *
 * @param <T> the item type
 * @since 0.18.6
 */
final class FlowableRequestObserveOn<T> extends Flowable<T> implements FlowableTransformer<T, T> {

    final Flowable<T> source;

    final Scheduler scheduler;

    FlowableRequestObserveOn(Flowable<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableRequestObserveOn<T>(upstream, scheduler);
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new RequestObserveOnSubscriber<T>(s, scheduler.createWorker()));
    }

    static final class RequestObserveOnSubscriber<T>
    extends AtomicLong
    implements FlowableSubscriber<T>, Subscription, Runnable {

        private static final long serialVersionUID = 3167152788131496136L;

        final Subscriber<? super T> downstream;

        final Worker worker;

        final Runnable requestOne;

        Subscription upstream;

        volatile T item;
        Throwable error;
        volatile boolean done;

        long emitted;
        boolean terminated;

        RequestObserveOnSubscriber(Subscriber<? super T> downstream, Scheduler.Worker worker) {
            this.downstream = downstream;
            this.worker = worker;
            this.requestOne = new Runnable() {
                @Override
                public void run() {
                    upstream.request(1L);
                }
            };
        }

        @Override
        public void onSubscribe(Subscription s) {
            upstream = s;
            downstream.onSubscribe(this);
            worker.schedule(requestOne);
        }

        @Override
        public void onNext(T t) {
            item = t;
            worker.schedule(this);
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            worker.schedule(this);
        }

        @Override
        public void onComplete() {
            done = true;
            worker.schedule(this);
        }

        @Override
        public void run() {
            if (terminated) {
                return;
            }
            for (;;) {
                boolean d = done;
                T v = item;
                boolean empty = v == null;

                if (d && empty) {
                    Throwable ex = error;
                    if (ex != null) {
                        downstream.onError(ex);
                    } else {
                        downstream.onComplete();
                    }
                    worker.dispose();
                    terminated = true;
                    return;
                }
                long e = emitted;
                if (!empty && e != get()) {
                    item = null;
                    downstream.onNext(v);
                    emitted = e + 1;
                    worker.schedule(requestOne);
                } else {
                    break;
                }
            }
        }

        @Override
        public void request(long n) {
            BackpressureHelper.add(this, n);
            worker.schedule(this);
        }

        @Override
        public void cancel() {
            upstream.cancel();
            worker.dispose();
            item = null;
        }
    }
}
