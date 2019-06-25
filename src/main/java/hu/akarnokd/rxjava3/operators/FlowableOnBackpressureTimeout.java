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

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * If the downstream doesn't request, it buffers events and times out
 * old elements from the front of the buffer.
 * @param <T> the input and output element type
 */
final class FlowableOnBackpressureTimeout<T> extends Flowable<T>
implements FlowableTransformer<T, T> {

    final Publisher<T> source;

    final int maxSize;

    final long timeout;

    final TimeUnit unit;

    final Scheduler scheduler;

    final Consumer<? super T> onEvict;

    FlowableOnBackpressureTimeout(Publisher<T> source, int maxSize, long timeout, TimeUnit unit,
            Scheduler scheduler, Consumer<? super T> onEvict) {
        this.source = source;
        this.maxSize = maxSize;
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.onEvict = onEvict;
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableOnBackpressureTimeout<T>(upstream, maxSize, timeout, unit, scheduler, onEvict);
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new OnBackpressureTimeoutSubscriber<T>(s, maxSize, timeout, unit, scheduler.createWorker(), onEvict));
    }

    static final class OnBackpressureTimeoutSubscriber<T>
    extends AtomicInteger
    implements Subscriber<T>, Subscription, Runnable {

        private static final long serialVersionUID = 2264324530873250941L;

        final Subscriber<? super T> downstream;

        final AtomicLong requested;

        final int maxSizeDouble;

        final long timeout;

        final TimeUnit unit;

        final Worker worker;

        final Consumer<? super T> onEvict;

        Subscription upstream;

        final ArrayDeque<Object> queue;

        volatile boolean done;
        Throwable error;

        volatile boolean cancelled;

        OnBackpressureTimeoutSubscriber(Subscriber<? super T> downstream, int maxSize, long timeout, TimeUnit unit,
                Worker worker, Consumer<? super T> onEvict) {
            this.downstream = downstream;
            this.maxSizeDouble = maxSize << 1;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
            this.onEvict = onEvict;
            this.requested = new AtomicLong();
            this.queue = new ArrayDeque<Object>();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            upstream.cancel();
            worker.dispose();

            if (getAndIncrement() == 0) {
                clearQueue();
            }
        }

        @SuppressWarnings("unchecked")
        void clearQueue() {
            for (;;) {
                T evicted;
                synchronized (this) {
                    if (queue.isEmpty()) {
                        break;
                    }

                    queue.poll();
                    evicted = (T)queue.poll();
                }

                evict(evicted);
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);

                s.request(Long.MAX_VALUE);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onNext(T t) {
            T evicted = null;
            synchronized (this) {
                if (queue.size() == maxSizeDouble) {
                    queue.poll();
                    evicted = (T)queue.poll();
                }
                queue.offer(worker.now(unit));
                queue.offer(t);
            }
            evict(evicted);
            worker.schedule(this, timeout, unit);
            drain();
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            for (;;) {
                if (cancelled) {
                    break;
                }

                boolean d = done;
                boolean empty;
                T evicted = null;

                synchronized (this) {
                    Long ts = (Long)queue.peek();
                    empty = ts == null;
                    if (!empty) {
                        if (ts.longValue() <= worker.now(unit) - timeout) {
                            queue.poll();
                            evicted = (T)queue.poll();
                        } else {
                            break;
                        }
                    }
                }

                evict(evicted);

                if (empty) {
                    if (d) {
                        drain();
                    }
                    break;
                }
            }
        }

        void evict(T evicted) {
            if (evicted != null) {
                try {
                    onEvict.accept(evicted);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaPlugins.onError(ex);
                }
            }
        }

        @SuppressWarnings("unchecked")
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;

            for (;;) {
                long r = requested.get();
                long e = 0;

                while (e != r) {
                    if (cancelled) {
                        clearQueue();
                        return;
                    }

                    boolean d = done;
                    T v;

                    synchronized (this) {
                        if (queue.poll() != null) {
                            v = (T)queue.poll();
                        } else {
                            v = null;
                        }
                    }

                    boolean empty = v == null;

                    if (d && empty) {
                        Throwable ex = error;
                        if (ex != null) {
                            downstream.onError(ex);
                        } else {
                            downstream.onComplete();
                        }

                        worker.dispose();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    downstream.onNext(v);

                    e++;
                }

                if (e == r) {
                    if (cancelled) {
                        clearQueue();
                        return;
                    }

                    boolean d = done;
                    boolean empty;
                    synchronized (this) {
                        empty = queue.isEmpty();
                    }

                    if (d && empty) {
                        Throwable ex = error;
                        if (ex != null) {
                            downstream.onError(ex);
                        } else {
                            downstream.onComplete();
                        }

                        worker.dispose();
                        return;
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
