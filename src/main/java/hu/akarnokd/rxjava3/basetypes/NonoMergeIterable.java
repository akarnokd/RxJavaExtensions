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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava3.basetypes.NonoMergeArray.*;
import hu.akarnokd.rxjava3.util.CompositeSubscription;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.AtomicThrowable;

/**
 * Run Nono sources in parallel and complete when all complete.
 */
final class NonoMergeIterable extends Nono {

    final Iterable<? extends Nono> sources;

    final boolean delayErrors;

    final int maxConcurrency;

    NonoMergeIterable(Iterable<? extends Nono> sources, boolean delayErrors, int maxConcurrency) {
        this.sources = sources;
        this.delayErrors = delayErrors;
        this.maxConcurrency = maxConcurrency;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {

        Iterator<? extends Nono> it;
        try {
            it = Objects.requireNonNull(sources.iterator(), "The source Iterable returned a null Iterator");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }
        MergeSubscriber parent = new MergeSubscriber(s, delayErrors, maxConcurrency, it);
        s.onSubscribe(parent);
        parent.subscribe(maxConcurrency);
    }

    static final class MergeSubscriber extends BasicIntQueueSubscription<Void> implements NonoInnerSupport {

        private static final long serialVersionUID = -58058606508277827L;

        final Subscriber<? super Void> downstream;

        final AtomicThrowable errors;

        final boolean delayErrors;

        final Iterator<? extends Nono> iterator;

        final CompositeSubscription set;

        final AtomicInteger wip;

        volatile boolean cancelled;

        MergeSubscriber(Subscriber<? super Void> downstream, boolean delayErrors, int maxConcurrency, Iterator<? extends Nono> iterator) {
            this.downstream = downstream;
            this.delayErrors = delayErrors;
            this.errors = new AtomicThrowable();
            this.iterator = iterator;
            this.set = new CompositeSubscription();
            this.wip = new AtomicInteger();
            this.lazySet(1);
        }

        @Override
        public int requestFusion(int mode) {
            return mode & ASYNC;
        }

        @Override
        public Void poll() throws Exception {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void clear() {
            // no-op
        }

        @Override
        public void request(long n) {
            // no-op
        }

        @Override
        public void cancel() {
            cancelled = true;
            set.cancel();
            errors.tryTerminateAndReport();
        }

        void subscribe(int n) {
            for (;;) {
                int c = wip.get();
                if (c == Integer.MAX_VALUE) {
                    return;
                }
                int u = c + n;
                if (u < 0) {
                    u = Integer.MAX_VALUE;
                }
                if (wip.compareAndSet(c, u)) {
                    if (c != 0) {
                        return;
                    }
                    break;
                }
            }

            Iterator<? extends Nono> srcs = iterator;
            int e = 0;

            for (;;) {

                while (e != n) {
                    if (cancelled) {
                        return;
                    }
                    boolean hasNext;
                    Nono np = null;

                    try {
                        hasNext = srcs.hasNext();
                        if (hasNext) {
                            np = Objects.requireNonNull(srcs.next(), "The iterator returned a null Nono");
                        }
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        errors.tryAddThrowableOrReport(ex);
                        complete();
                        return;
                    }

                    if (hasNext) {
                        InnerSubscriber inner = new InnerSubscriber(this);
                        set.add(inner);
                        getAndIncrement();

                        np.subscribe(inner);
                        e++;
                    } else {
                        complete();
                        return;
                    }
                }

                n = get();
                if (e == n) {
                    n = addAndGet(-e);
                    if (n == 0) {
                        break;
                    }
                    e = 0;
                }
            }
        }

        @Override
        public void innerError(InnerSubscriber inner, Throwable ex) {
            set.delete(inner);
            if (errors.tryAddThrowableOrReport(ex)) {
                if (!delayErrors) {
                    set.cancel();

                    errors.tryTerminateConsumer(downstream);
                } else {
                    subscribe(1);
                    complete();
                }
            }
        }

        @Override
        public void innerComplete(InnerSubscriber inner) {
            set.delete(inner);
            subscribe(1);
            complete();
        }

        void complete() {
            if (decrementAndGet() == 0) {
                Throwable ex = errors.terminate();
                if (ex != null) {
                    downstream.onError(ex);
                } else {
                    downstream.onComplete();
                }
            }
        }
    }
}
