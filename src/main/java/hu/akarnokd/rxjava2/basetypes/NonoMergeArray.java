/*
 * Copyright 2016-2018 David Karnok
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

package hu.akarnokd.rxjava2.basetypes;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import hu.akarnokd.rxjava2.util.CompositeSubscription;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Run Nono sources in parallel and complete when all complete.
 */
final class NonoMergeArray extends Nono {

    final Nono[] sources;

    final boolean delayErrors;

    final int maxConcurrency;

    NonoMergeArray(Nono[] sources, boolean delayErrors, int maxConcurrency) {
        this.sources = sources;
        this.delayErrors = delayErrors;
        this.maxConcurrency = maxConcurrency;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        MergeSubscriber parent = new MergeSubscriber(s, delayErrors, maxConcurrency, sources);
        s.onSubscribe(parent);
        parent.subscribe(maxConcurrency);
    }

    static final class MergeSubscriber extends BasicIntQueueSubscription<Void> implements NonoInnerSupport {

        private static final long serialVersionUID = -58058606508277827L;

        final Subscriber<? super Void> actual;

        final AtomicThrowable errors;

        final boolean delayErrors;

        final Nono[] sources;

        final CompositeSubscription set;

        final AtomicInteger wip;

        int index;

        volatile boolean cancelled;

        MergeSubscriber(Subscriber<? super Void> actual, boolean delayErrors, int maxConcurrency, Nono[] sources) {
            this.actual = actual;
            this.delayErrors = delayErrors;
            this.errors = new AtomicThrowable();
            this.sources = sources;
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

            Nono[] srcs = sources;
            int f = srcs.length;
            int e = 0;

            for (;;) {
                int i = index;

                while (e != n && i != f) {
                    if (cancelled) {
                        return;
                    }

                    Nono np = srcs[i];

                    if (np == null) {
                        errors.addThrowable(new NullPointerException("A source is null"));
                        if (delayErrors) {
                            i = f;
                            break;
                        }
                        set.cancel();
                        Throwable ex = errors.terminate();
                        if (ex != ExceptionHelper.TERMINATED) {
                            actual.onError(ex);
                        }
                        return;
                    }

                    InnerSubscriber inner = new InnerSubscriber(this);
                    set.add(inner);
                    getAndIncrement();

                    np.subscribe(inner);

                    i++;
                    e++;
                }

                if (cancelled) {
                    return;
                }
                if (i == f) {
                    complete();
                    break;
                }

                index = i;

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
            if (errors.addThrowable(ex)) {
                if (!delayErrors) {
                    set.cancel();

                    ex = errors.terminate();
                    if (ex != ExceptionHelper.TERMINATED) {
                        actual.onError(ex);
                    }
                } else {
                    subscribe(1);
                    complete();
                }
            } else {
                RxJavaPlugins.onError(ex);
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
                    actual.onError(ex);
                } else {
                    actual.onComplete();
                }
            }
        }
    }

    interface NonoInnerSupport {
        void innerError(InnerSubscriber inner, Throwable t);

        void innerComplete(InnerSubscriber inner);
    }

    static final class InnerSubscriber extends AtomicReference<Subscription> implements Subscriber<Void>, Subscription {

        private static final long serialVersionUID = -7172670778151490886L;

        final NonoInnerSupport parent;

        InnerSubscriber(NonoInnerSupport parent) {
            this.parent = parent;
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.setOnce(this, s);
        }

        @Override
        public void onNext(Void t) {
            // not called
        }

        @Override
        public void onError(Throwable t) {
            parent.innerError(this, t);
        }

        @Override
        public void onComplete() {
            parent.innerComplete(this);
        }

        @Override
        public void request(long n) {
            // not used
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(this);
        }
    }
}
