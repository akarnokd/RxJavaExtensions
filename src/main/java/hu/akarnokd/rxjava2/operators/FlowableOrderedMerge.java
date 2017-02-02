/*
 * Copyright 2016-2017 David Karnok
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

package hu.akarnokd.rxjava2.operators;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.SimpleQueue;
import io.reactivex.internal.subscribers.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Merges a fixed set of sources by picking the next smallest
 * available element from any of the sources based on a comparator.
 * 
 * @param <T> the source value types
 * 
 * @since 0.8.0
 */
final class FlowableOrderedMerge<T> extends Flowable<T> {

    final Publisher<T>[] sources;

    final Iterable<? extends Publisher<T>> sourcesIterable;

    final Comparator<? super T> comparator;

    final boolean delayErrors;

    final int prefetch;

    FlowableOrderedMerge(Publisher<T>[] sources, Iterable<? extends Publisher<T>> sourcesIterable,
            Comparator<? super T> comparator,
            boolean delayErrors, int prefetch) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
        this.comparator = comparator;
        this.delayErrors = delayErrors;
        this.prefetch = prefetch;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        Publisher<T>[] array = sources;
        int n;

        if (array == null) {
            array = new Publisher[8];
            n = 0;
            try {
                for (Publisher<T> p : sourcesIterable) {
                    if (n == array.length) {
                        array = Arrays.copyOf(array, n << 1);
                    }
                    array[n++] = ObjectHelper.requireNonNull(p, "a source is null");
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptySubscription.error(ex, s);
                return;
            }
        } else {
            n = array.length;
        }

        if (n == 0) {
            EmptySubscription.complete(s);
            return;
        }

        if (n == 1) {
            array[0].subscribe(s);
            return;
        }

        MergeCoordinator<T> parent = new MergeCoordinator<T>(s, comparator, n, prefetch, delayErrors);
        s.onSubscribe(parent);
        parent.subscribe(array, n);
    }

    static final class MergeCoordinator<T>
    extends AtomicInteger
    implements Subscription, InnerQueuedSubscriberSupport<T> {
        private static final long serialVersionUID = -8467324377226330554L;

        final Subscriber<? super T> actual;

        final Comparator<? super T> comparator;

        final InnerQueuedSubscriber<T>[] subscribers;

        final boolean delayErrors;

        final AtomicThrowable errors;

        final AtomicLong requested;

        final Object[] latest;

        volatile boolean cancelled;

        @SuppressWarnings("unchecked")
        MergeCoordinator(Subscriber<? super T> actual, Comparator<? super T> comparator, int n, int prefetch, boolean delayErrors) {
            this.actual = actual;
            this.comparator = comparator;
            this.delayErrors = delayErrors;
            InnerQueuedSubscriber<T>[] subs = new InnerQueuedSubscriber[n];
            for (int i = 0; i < n; i++) {
                subs[i] = new InnerQueuedSubscriber<T>(this, prefetch);
            }
            this.subscribers = subs;
            this.requested = new AtomicLong();
            this.errors = new AtomicThrowable();
            this.latest = new Object[n];
        }

        void subscribe(Publisher<T>[] sources, int n) {
            InnerQueuedSubscriber<T>[] subs = subscribers;
            for (int i = 0; i < n && !cancelled; i++) {
                Publisher<T> p = sources[i];
                if (p != null) {
                    p.subscribe(subs[i]);
                } else {
                    EmptySubscription.error(new NullPointerException("The " + i + "th source is null"), subs[i]);
                    if (!delayErrors) {
                        break;
                    }
                }
            }
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        void cancelSources() {
            for (InnerQueuedSubscriber<T> d : subscribers) {
                d.cancel();
            }
        }

        void clearSources() {
            Arrays.fill(latest, this);
            for (InnerQueuedSubscriber<T> d : subscribers) {
                SimpleQueue<T> q = d.queue();
                if (q != null) {
                    q.clear();
                }
            }
        }

        void cancelAndClearSources() {
            Arrays.fill(latest, this);
            for (InnerQueuedSubscriber<T> d : subscribers) {
                d.cancel();
                SimpleQueue<T> q = d.queue();
                if (q != null) {
                    q.clear();
                }
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                cancelSources();
                if (getAndIncrement() == 0) {
                    clearSources();
                }
            }
        }

        @Override
        public void innerNext(InnerQueuedSubscriber<T> inner, T value) {
            inner.queue().offer(value);
            drain();
        }

        @Override
        public void innerError(InnerQueuedSubscriber<T> inner, Throwable e) {
            if (errors.addThrowable(e)) {
                if (!delayErrors) {
                    cancelSources();
                } else {
                    inner.setDone();
                }
                drain();
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void innerComplete(InnerQueuedSubscriber<T> inner) {
            inner.setDone();
            drain();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;

            Subscriber<? super T> a = actual;
            AtomicThrowable err = errors;
            InnerQueuedSubscriber<T>[] subs = subscribers;
            int n = subs.length;
            Object[] latest = this.latest;
            Comparator<? super T> comp = comparator;

            for (;;) {

                long r = requested.get();
                long e = 0L;

                while (e != r) {
                    if (cancelled) {
                        clearSources();
                        return;
                    }

                    if (!delayErrors && err.get() != null) {
                        cancelAndClearSources();
                        a.onError(err.terminate());
                        return;
                    }

                    boolean d = true;
                    int hasValue = 0;
                    boolean empty = true;

                    T smallest = null;
                    int pick = -1;

                    for (int i = 0; i < n; i++) {
                        InnerQueuedSubscriber<T> inner = subs[i];
                        boolean innerDone = inner.isDone();
                        if (!innerDone) {
                            d = false;
                        }
                        Object v = latest[i];
                        if (v == null) {
                            SimpleQueue<T> q = inner.queue();
                            try {
                                v = q != null ? q.poll() : null;
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                err.addThrowable(ex);
                                inner.setDone();
                                if (!delayErrors) {
                                    cancelAndClearSources();
                                    a.onError(err.terminate());
                                    return;
                                }
                                v = this;
                            }

                            if (v != null) {
                                latest[i] = v;
                                hasValue++;
                                empty = false;
                            } else
                            if (innerDone) {
                                latest[i] = this;
                                hasValue++;
                            }
                        } else {
                            hasValue++;
                            if (v != this) {
                                empty = false;
                            }
                        }

                        if (v != null && v != this) {
                            boolean smaller;
                            try {
                                if (smallest != null) {
                                    smaller = comp.compare(smallest, (T)v) > 0;
                                } else {
                                    smaller = true;
                                }
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                err.addThrowable(ex);
                                cancelAndClearSources();
                                a.onError(err.terminate());
                                return;
                            }
                            if (smaller) {
                                smallest = (T)v;
                                pick = i;
                            }
                        }
                    }

                    if (hasValue == n && pick >= 0) {
                        a.onNext(smallest);
                        latest[pick] = null;
                        subs[pick].requestOne();

                        e++;
                    } else {
                        if (d && empty) {
                            if (err.get() != null) {
                                a.onError(err.terminate());
                            } else {
                                a.onComplete();
                            }
                            return;
                        }
                        break;
                    }
                }

                if (e == r) {
                    if (cancelled) {
                        clearSources();
                        return;
                    }

                    if (!delayErrors && err.get() != null) {
                        cancelAndClearSources();
                        a.onError(err.terminate());
                        return;
                    }

                    boolean d = true;
                    boolean empty = true;

                    for (int i = 0; i < subs.length; i++) {
                        InnerQueuedSubscriber<T> inner = subs[i];
                        if (!inner.isDone()) {
                            d = false;
                            break;
                        }
                        Object o = latest[i];
                        SimpleQueue<T> q = inner.queue();
                        if (o == null && q != null) {
                            try {
                                o = q.poll();
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                err.addThrowable(ex);
                                if (!delayErrors) {
                                    cancelAndClearSources();
                                    a.onError(ex);
                                    return;
                                }
                                o = this;
                            }

                            latest[i] = o;
                        }
                        if (o != null && o != this) {
                            empty = false;
                            break;
                        }
                    }

                    if (d && empty) {
                        if (err.get() != null) {
                            a.onError(err.terminate());
                        } else {
                            a.onComplete();
                        }
                        return;
                    }
                }

                if (e != 0L) {
                    BackpressureHelper.produced(requested, e);
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }
}
