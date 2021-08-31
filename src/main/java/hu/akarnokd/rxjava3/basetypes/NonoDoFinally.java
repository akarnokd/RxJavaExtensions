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

package hu.akarnokd.rxjava3.basetypes;

import org.reactivestreams.*;

import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Execute an action exactly once after the upstream terminates or the
 * downstream cancelles.
 */
final class NonoDoFinally extends Nono {

    final Nono source;

    final Action onFinally;

    NonoDoFinally(Nono source, Action onFinally) {
        this.source = source;
        this.onFinally = onFinally;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        source.subscribe(new DoFinallySubscriber(s, onFinally));
    }

    static final class DoFinallySubscriber extends BasicNonoIntQueueSubscription
    implements Subscriber<Void> {

        private static final long serialVersionUID = -2447716698732984984L;

        final Subscriber<? super Void> downstream;

        final Action onFinally;

        Subscription upstream;

        DoFinallySubscriber(Subscriber<? super Void> downstream, Action onFinally) {
            this.downstream = downstream;
            this.onFinally = onFinally;
        }

        @Override
        public void cancel() {
            upstream.cancel();
            runFinally();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(Void t) {
            // never called
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
    }
}
