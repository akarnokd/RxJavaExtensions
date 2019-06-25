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

package hu.akarnokd.rxjava3.expr;

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.internal.subscriptions.*;

final class FlowableWhileDoWhile<T> extends Flowable<T> {

    final Publisher<? extends T> source;

    final BooleanSupplier preCondition;

    final BooleanSupplier postCondition;

    FlowableWhileDoWhile(Publisher<? extends T> source, BooleanSupplier preCondition,
            BooleanSupplier postCondition) {
        this.source = source;
        this.preCondition = preCondition;
        this.postCondition = postCondition;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        boolean b;

        try {
            b = preCondition.getAsBoolean();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        if (b) {
            WhileDoWhileObserver<T> parent = new WhileDoWhileObserver<T>(s, postCondition, source);
            s.onSubscribe(parent);
            parent.subscribeNext();
        } else {
            EmptySubscription.complete(s);
        }
    }

    static final class WhileDoWhileObserver<T>
    extends SubscriptionArbiter
    implements Subscriber<T>, Subscription {

        private static final long serialVersionUID = -5255585317630843019L;

        final Subscriber<? super T> downstream;

        final AtomicInteger wip;

        final BooleanSupplier postCondition;

        final Publisher<? extends T> source;

        volatile boolean active;

        long produced;

        WhileDoWhileObserver(Subscriber<? super T> downstream, BooleanSupplier postCondition, Publisher<? extends T> source) {
            super(false);
            this.downstream = downstream;
            this.wip = new AtomicInteger();
            this.postCondition = postCondition;
            this.source = source;
        }

        @Override
        public void onSubscribe(Subscription s) {
            super.setSubscription(s);
        }

        @Override
        public void onNext(T value) {
            produced++;
            downstream.onNext(value);
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void onComplete() {

            boolean b;

            try {
                b = postCondition.getAsBoolean();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }

            if (b) {
                long p = produced;
                if (p != 0L) {
                    produced(p);
                }

                active = false;
                subscribeNext();
            } else {
                downstream.onComplete();
            }
        }

        void subscribeNext() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            for (;;) {

                if (isCancelled()) {
                    return;
                }

                if (!active) {
                    active = true;
                    source.subscribe(this);
                }

                if (wip.decrementAndGet() == 0) {
                    break;
                }
            }
        }
    }
}
