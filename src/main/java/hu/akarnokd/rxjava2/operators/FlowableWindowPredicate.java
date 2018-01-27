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

package hu.akarnokd.rxjava2.operators;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.processors.UnicastProcessor;

/**
 * Emit into the same window while the predicate returns true or
 * emit into the same window until the predicate returns true.
 *
 * @param <T> the source value type
 * @author Martin Nowak
 * @since 0.17.7
 */
final class FlowableWindowPredicate<T> extends Flowable<Flowable<T>> implements FlowableTransformer<T, Flowable<T>> {

    enum Mode {
        /** The item triggering the new buffer will be part of the new buffer. */
        BEFORE,
        /** The item triggering the new buffer will be part of the old buffer. */
        AFTER,
        /** The item won't be part of any buffers. */
        SPLIT
    }

    final Publisher<T> source;

    final Predicate<? super T> predicate;

    final Mode mode;

    final int bufferSize;

    FlowableWindowPredicate(Publisher<T> source, Predicate<? super T> predicate, Mode mode,
            int bufferSize) {
        this.source = source;
        this.predicate = predicate;
        this.mode = mode;
        this.bufferSize = bufferSize;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Flowable<T>> s) {
        source.subscribe(new WindowPredicateSubscriber<T>(s, predicate, mode, bufferSize));
    }

    @Override
    public Publisher<Flowable<T>> apply(Flowable<T> upstream) {
        return new FlowableWindowPredicate<T>(upstream, predicate, mode, bufferSize);
    }

    static final class WindowPredicateSubscriber<T>
    extends AtomicInteger
    implements ConditionalSubscriber<T>, Subscription, Runnable {

        private static final long serialVersionUID = 2749959965593866309L;

        final Subscriber<? super Flowable<T>> actual;

        final Predicate<? super T> predicate;

        final Mode mode;

        final int bufferSize;

        final AtomicBoolean cancelled = new AtomicBoolean();

        Subscription s;

        UnicastProcessor<T> window;

        final AtomicLong requestedWindows;

        final AtomicReference<UnicastProcessor<T>> pending; // 1-element drain queue for Mode.BEFORE

        WindowPredicateSubscriber(Subscriber<? super Flowable<T>> actual,
                Predicate<? super T> predicate, Mode mode,
                int bufferSize) {
            super(1);
            this.actual = actual;
            this.predicate = predicate;
            this.mode = mode;
            this.bufferSize = bufferSize;
            // In Mode.BEFORE windows are opened earlier and added to the 1-element drain "queue"
            if (mode == Mode.BEFORE) {
                requestedWindows = new AtomicLong();
                pending = new AtomicReference<UnicastProcessor<T>>();
            } else {
                requestedWindows = null;
                pending = null;
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            UnicastProcessor<T> w = window;
            if (w == null) {
                // ignore additional items after last window is completed
                if (cancelled.get()) {
                    return true;
                }
                // emit next window
                w = UnicastProcessor.<T>create(bufferSize, this);
                window = w;
                getAndIncrement();
                if (mode == Mode.BEFORE) {
                    requestedWindows.getAndDecrement();
                }
                actual.onNext(w);
            }

            boolean b;

            try {
                // negate predicate for windowWhile
                b = predicate.test(t) ^ mode == Mode.BEFORE;
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.cancel();
                actual.onError(ex);
                w.onError(ex);
                window = null;
                return true;
            }

            if (b) {
                // element goes into current window
                if (mode == Mode.AFTER) {
                    w.onNext(t);
                }
                // finish current window
                w.onComplete();
                // element goes into the next requested window
                if (mode == Mode.BEFORE) {
                    w = UnicastProcessor.<T>create(bufferSize, this);
                    window = w;
                    w.onNext(t);
                    // add window to drain queue
                    pending.set(w);
                    // try emitting right away
                    drain();
                } else {
                    // new window emitted on next upstream item
                    window = null;
                }
            } else {
                w.onNext(t);
            }
            return b;
        }

        @Override
        public void onError(Throwable t) {
            Processor<T, T> w = window;
            if (w != null) {
                window = null;
                w.onError(t);
            }

            actual.onError(t);
        }

        @Override
        public void onComplete() {
            Processor<T, T> w = window;
            if (w != null) {
                window = null;
                w.onComplete();
            }

            actual.onComplete();
        }

        @Override
        public void request(long n) {
            if (mode == Mode.BEFORE && SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requestedWindows, n);
                drain();
            }
            s.request(n);
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                run();
            }
        }

        @Override
        public void run() {
            if (decrementAndGet() == 0) {
                s.cancel();
            }
        }

        private void drain() {
            if (requestedWindows.get() <= 0) {
                return;
            }
            UnicastProcessor<T> w = pending.getAndSet(null);
            if (w == null) {
                return;
            }
            getAndIncrement();
            requestedWindows.getAndDecrement();
            actual.onNext(w);
        }
    }
}
