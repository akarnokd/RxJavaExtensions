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

package hu.akarnokd.rxjava4.basetypes;

import java.io.Serial;
import java.util.concurrent.Flow.*;
import java.util.concurrent.atomic.*;

import hu.akarnokd.rxjava4.internal.rxcopy.*;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.Scheduler.Worker;

/**
 * Subscribe to the upstream on the specified scheduler.
 *
 * @param <T> the value type
 */
final class PerhapsSubscribeOn<T> extends Perhaps<T> {

    final Perhaps<T> source;

    final Scheduler scheduler;

    PerhapsSubscribeOn(Perhaps<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        Worker worker = scheduler.createWorker();

        SubscribeOnSubscriber<T> parent = new SubscribeOnSubscriber<>(s, worker, source, false);
        s.onSubscribe(parent);

        worker.schedule(parent);
    }

    static final class SubscribeOnSubscriber<T> extends AtomicReference<Thread>
    implements FlowableSubscriber<T>, Subscription, Runnable {

        @Serial
        private static final long serialVersionUID = 8094547886072529208L;

        final Subscriber<? super T> downstream;

        final Scheduler.Worker worker;

        final AtomicReference<Subscription> upstream;

        final AtomicLong requested;

        final boolean nonScheduledRequests;

        Publisher<T> source;

        SubscribeOnSubscriber(Subscriber<? super T> downstream, Scheduler.Worker worker, Publisher<T> source, boolean requestOn) {
            this.downstream = downstream;
            this.worker = worker;
            this.source = source;
            this.upstream = new AtomicReference<>();
            this.requested = new AtomicLong();
            this.nonScheduledRequests = !requestOn;
        }

        @Override
        public void run() {
            lazySet(Thread.currentThread());
            Publisher<T> src = source;
            source = null;
            src.subscribe(this);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this.upstream, s)) {
                long r = requested.getAndSet(0L);
                if (r != 0L) {
                    requestUpstream(r, s);
                }
            }
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
            worker.dispose();
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
            worker.dispose();
        }

        @Override
        public void request(final long n) {
            if (SubscriptionHelper.validate(n)) {
                Subscription s = this.upstream.get();
                if (s != null) {
                    requestUpstream(n, s);
                } else {
                    BackpressureHelper.add(requested, n);
                    s = this.upstream.get();
                    if (s != null) {
                        long r = requested.getAndSet(0L);
                        if (r != 0L) {
                            requestUpstream(r, s);
                        }
                    }
                }
            }
        }

        void requestUpstream(final long n, final Subscription s) {
            if (nonScheduledRequests || Thread.currentThread() == get()) {
                s.request(n);
            } else {
                worker.schedule(new Request(s, n));
            }
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(upstream);
            worker.dispose();
        }

        record Request(Subscription upstream, long n) implements Runnable {

            @Override
                    public void run() {
                        upstream.request(n);
                    }
                }
    }}
