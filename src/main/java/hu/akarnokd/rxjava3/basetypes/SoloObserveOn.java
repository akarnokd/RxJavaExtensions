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

package hu.akarnokd.rxjava3.basetypes;

import org.reactivestreams.*;

import hu.akarnokd.rxjava3.util.SpscOneQueue;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.operators.*;
import io.reactivex.rxjava3.internal.subscriptions.*;

/**
 * Delivers the upstream's onNext, onError and onComplete on the specified
 * scheduler.
 *
 * @param <T> the value type
 */
final class SoloObserveOn<T> extends Solo<T> {

    final Solo<T> source;

    final Scheduler scheduler;

    SoloObserveOn(Solo<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new ObserveOnSubscriber<T>(s, scheduler.createWorker()));
    }

    static final class ObserveOnSubscriber<T>
    extends BasicIntQueueSubscription<T>
    implements Subscriber<T>, Runnable {

        private static final long serialVersionUID = -658564450611526565L;

        final Subscriber<? super T> downstream;

        final Worker worker;

        Subscription upstream;

        volatile boolean done;
        Throwable error;

        SimpleQueue<T> queue;

        volatile boolean cancelled;

        volatile boolean requested;

        int sourceMode;

        boolean outputFused;

        ObserveOnSubscriber(Subscriber<? super T> downstream, Worker worker) {
            this.downstream = downstream;
            this.worker = worker;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<T> qs = (QueueSubscription<T>) s;

                    int m = qs.requestFusion(ANY | BOUNDARY);
                    if (m == SYNC) {
                        sourceMode = m;
                        queue = qs;
                        done = true;

                        downstream.onSubscribe(this);

                        return;
                    }
                    if (m == ASYNC) {
                        sourceMode = m;
                        queue = qs;

                        downstream.onSubscribe(this);

                        s.request(Long.MAX_VALUE);
                        return;
                    }
                }

                queue = new SpscOneQueue<>();

                downstream.onSubscribe(this);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (sourceMode == NONE) {
                queue.offer(t);
            }
            trySchedule();
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            trySchedule();
        }

        @Override
        public void onComplete() {
            done = true;
            trySchedule();
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }

        @Override
        public void clear() {
            queue.clear();
        }

        @Override
        public T poll() throws Throwable {
            return queue.poll();
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                upstream.cancel();
                worker.dispose();

                if (getAndIncrement() == 0) {
                    queue.clear();
                }
            }
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                requested = true;
                trySchedule();
            }
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & ASYNC) != 0) {
                outputFused = true;
                return ASYNC;
            }
            return NONE;
        }

        void trySchedule() {
            if (getAndIncrement() == 0) {
                worker.schedule(this);
            }
        }

        @Override
        public void run() {
            SimpleQueue<T> q = queue;
            int missed = 1;

            for (;;) {
                if (cancelled) {
                    q.clear();
                    return;
                }

                boolean d = done;

                if (requested) {
                    boolean empty;

                    if (outputFused) {
                        empty = q.isEmpty();
                        if (!empty) {
                            downstream.onNext(null);
                            empty = true;
                        }
                    } else {
                        T v;

                        try {
                            v = q.poll();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            q.clear();
                            downstream.onError(ex);
                            worker.dispose();
                            return;
                        }

                        empty = v == null;

                        if (!empty) {
                            downstream.onNext(v);
                            empty = true;
                        }
                    }
                }

                if (d) {
                    Throwable ex = error;
                    if (ex != null) {
                        downstream.onError(ex);
                        worker.dispose();
                        return;
                    } else
                    if (q.isEmpty()) {
                        downstream.onComplete();
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
