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

package hu.akarnokd.rxjava2.basetypes;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.*;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.AtomicThrowable;

/**
 * Subscribe to the Nono sources one at a time and complete if all of them complete.
 */
final class NonoConcatIterable extends Nono {

    final Iterable<? extends Nono> sources;

    final boolean delayError;

    NonoConcatIterable(Iterable<? extends Nono> sources, boolean delayError) {
        this.sources = sources;
        this.delayError = delayError;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        Iterator<? extends Nono> it;

        try {
            it = ObjectHelper.requireNonNull(sources.iterator(), "The sources Iterable returned a null Iterator");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        ConcatSubscriber parent = new ConcatSubscriber(s, it, delayError);
        parent.drain();
    }

    static final class ConcatSubscriber extends BasicRefQueueSubscription<Void, Subscription> implements Subscriber<Void> {

        private static final long serialVersionUID = -4926738846855955051L;

        final Subscriber<? super Void> actual;

        final AtomicThrowable errors;

        final Iterator<? extends Nono> iterator;

        final AtomicInteger wip;

        volatile boolean active;

        ConcatSubscriber(Subscriber<? super Void> actual, Iterator<? extends Nono> iterator, boolean delayError) {
            this.actual = actual;
            this.iterator = iterator;
            this.errors = delayError ? new AtomicThrowable() : null;
            this.wip = new AtomicInteger();
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(this);
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.replace(this, s);
        }

        @Override
        public void onNext(Void t) {
            // not called
        }

        @Override
        public void onError(Throwable t) {
            AtomicThrowable err = errors;
            if (err != null) {
                err.addThrowable(t);
                active = false;
                drain();
            } else {
                actual.onError(t);
            }
        }

        @Override
        public void onComplete() {
            active = false;
            drain();
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            do {
                if (SubscriptionHelper.isCancelled(this.get())) {
                    return;
                }

                if (!active) {
                    boolean b;
                    Nono np = null;

                    try {
                        b = iterator.hasNext();
                        if (b) {
                            np = ObjectHelper.requireNonNull(iterator.next(), "The iterator returned a null Nono");
                        }
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        if (errors != null) {
                            errors.addThrowable(ex);
                            actual.onError(errors.terminate());
                        } else {
                            actual.onError(ex);
                        }
                        return;
                    }
                    if (!b) {
                        Throwable ex = errors != null ? errors.terminate() : null;
                        if (ex != null) {
                            actual.onError(ex);
                        } else {
                            actual.onComplete();
                        }
                        return;
                    }
                    active = true;
                    np.subscribe(this);
                }
            } while (wip.decrementAndGet() != 0);
        }
    }
}
