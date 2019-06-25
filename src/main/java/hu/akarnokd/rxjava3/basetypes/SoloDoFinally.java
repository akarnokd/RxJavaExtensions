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

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Execute an action exactly once after the upstream terminates or the
 * downstream cancels.
 * @param <T> the input and output element type
 */
final class SoloDoFinally<T> extends Solo<T> {

    final Solo<T> source;

    final Action onFinally;

    SoloDoFinally(Solo<T> source, Action onFinally) {
        this.source = source;
        this.onFinally = onFinally;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new DoFinallySubscriber<T>(s, onFinally));
    }

    static final class DoFinallySubscriber<T> extends BasicIntQueueSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -2447716698732984984L;

        final Subscriber<? super T> downstream;

        final Action onFinally;

        Subscription upstream;

        QueueSubscription<T> queue;

        int sourceMode;

        DoFinallySubscriber(Subscriber<? super T> downstream, Action onFinally) {
            this.downstream = downstream;
            this.onFinally = onFinally;
        }

        @Override
        public void cancel() {
            upstream.cancel();
            runFinally();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                if (s instanceof QueueSubscription) {
                    queue = (QueueSubscription<T>)s;
                }

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
            runFinally();
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
            runFinally();
        }

        void runFinally() {
            if (compareAndSet(0, 1)) {
                try {
                    onFinally.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaPlugins.onError(ex);
                }
            }
        }

        @Override
        public int requestFusion(int mode) {
            QueueSubscription<T> qs = queue;
            if (qs != null && (mode & BOUNDARY) == 0) {
                int m = qs.requestFusion(mode);
                sourceMode = m;
                return m;
            }
            return NONE;
        }

        @Override
        public T poll() throws Throwable {
            T v = queue.poll();
            if (sourceMode == SYNC && v == null) {
                runFinally();
            }
            return v;
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
        public void request(long n) {
            upstream.request(n);
        }
    }
}
