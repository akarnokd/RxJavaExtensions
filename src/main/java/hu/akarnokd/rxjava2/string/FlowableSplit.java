/*
 * Copyright 2016 David Karnok
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

package hu.akarnokd.rxjava2.string;

import java.util.concurrent.atomic.*;
import java.util.regex.Pattern;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Consider a sequence of CharSequence as one and split it based on
 * a pattern.
 * 
 * @since 0.13.0
 */
final class FlowableSplit extends Flowable<String> implements FlowableTransformer<String, String> {

    final Publisher<String> source;

    final Pattern pattern;

    final int bufferSize;

    FlowableSplit(Publisher<String> source, Pattern pattern, int bufferSize) {
        this.source = source;
        this.pattern = pattern;
        this.bufferSize = bufferSize;
    }

    @Override
    public Publisher<String> apply(Flowable<String> upstream) {
        return new FlowableSplit(upstream, pattern, bufferSize);
    }

    @Override
    protected void subscribeActual(Subscriber<? super String> s) {
        source.subscribe(new SplitSubscriber(s, pattern, bufferSize));
    }

    static final class SplitSubscriber
    extends AtomicInteger
    implements ConditionalSubscriber<String>, Subscription {

        static final String[] EMPTY = new String[0];

        private static final long serialVersionUID = -5022617259701794064L;

        final Subscriber<? super String> actual;

        final Pattern pattern;

        final SimplePlainQueue<String[]> queue;

        final AtomicLong requested;

        final int bufferSize;

        final int limit;

        Subscription s;

        volatile boolean cancelled;

        String leftOver;

        String[] current;

        int index;

        int produced;

        volatile boolean done;
        Throwable error;

        SplitSubscriber(Subscriber<? super String> actual, Pattern pattern, int bufferSize) {
            this.actual = actual;
            this.pattern = pattern;
            this.bufferSize = bufferSize;
            this.limit = bufferSize - (bufferSize >> 2);
            this.queue = new SpscArrayQueue<String[]>(bufferSize);
            this.requested = new AtomicLong();
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
            s.cancel();

            if (getAndIncrement() == 0) {
                current = null;
                queue.clear();
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);

                s.request(bufferSize);
            }
        }

        @Override
        public void onNext(String t) {
            if (!tryOnNext(t)) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(String t) {
            String lo = leftOver;
            String[] a;
            try {
                if (lo == null || lo.isEmpty()) {
                    a = pattern.split(t);
                } else {
                    a = pattern.split(lo + t);
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                this.s.cancel();
                onError(ex);
                return true;
            }

            if (a.length == 0) {
                leftOver = null;
                return false;
            } else
            if (a.length == 1) {
                leftOver = a[0];
                return false;
            }
            leftOver = a[a.length - 1];
            queue.offer(a);
            drain();
            return true;
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            String lo = leftOver;
            if (lo != null && !lo.isEmpty()) {
                leftOver = null;
                queue.offer(new String[] { lo, null });
            }
            error = t;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                String lo = leftOver;
                if (lo != null && !lo.isEmpty()) {
                    leftOver = null;
                    queue.offer(new String[] { lo, null });
                }
                drain();
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            SimplePlainQueue<String[]> q = queue;

            int missed = 1;
            int consumed = produced;
            String[] array = current;
            int idx = index;

            Subscriber<? super String> a = actual;

            for (;;) {
                long r = requested.get();
                long e = 0;

                while (e != r) {
                    if (cancelled) {
                        current = null;
                        q.clear();
                        return;
                    }

                    boolean d = done;

                    if (array == null) {
                        array = q.poll();
                        if (array != null) {
                            current = array;
                            if (++consumed == limit) {
                                consumed = 0;
                                s.request(limit);
                            }
                        }
                    }

                    boolean empty = array == null;

                    if (d && empty) {
                        Throwable ex = error;
                        if (ex != null) {
                            a.onError(ex);
                        } else {
                            a.onComplete();
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    if (array.length == idx + 1) {
                        array = null;
                        current = null;
                        idx = 0;
                        continue;
                    }

                    a.onNext(array[idx]);

                    e++;
                    idx++;
                }

                if (e == r) {
                    if (cancelled) {
                        current = null;
                        q.clear();
                        return;
                    }

                    boolean d = done;

                    if (array == null) {
                        array = q.poll();
                        if (array != null) {
                            current = array;
                            if (++consumed == limit) {
                                consumed = 0;
                                s.request(limit);
                            }
                        }
                    }

                    boolean empty = array == null;

                    if (d && empty) {
                        Throwable ex = error;
                        if (ex != null) {
                            a.onError(ex);
                        } else {
                            a.onComplete();
                        }
                        return;
                    }
                }

                if (e != 0L) {
                    BackpressureHelper.produced(requested, e);
                }

                produced = consumed;
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }
}
