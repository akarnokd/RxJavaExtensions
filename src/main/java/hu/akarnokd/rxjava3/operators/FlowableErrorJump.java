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

package hu.akarnokd.rxjava3.operators;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.internal.functions.ObjectHelper;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;

/**
 * Makes sure an upstream error skips the flow created with a
 * transformer and then re-emitted when the inner flow terminates.
 * <p>
 * This operator is like an external delay-error behavior, allowing
 * an inner transformed flow to complete normally but still fail the
 * flow further down.
 * @param <T> the upstream value type
 * @param <R> the downstream value type
 * @since 0.19.1
 */
final class FlowableErrorJump<T, R> extends Flowable<R> implements FlowableTransformer<T, R> {

    final Flowable<T> source;

    final FlowableTransformer<T, R> transformer;

    FlowableErrorJump(Flowable<T> source, FlowableTransformer<T, R> transformer) {
        this.source = source;
        this.transformer = transformer;
    }

    @Override
    public Publisher<R> apply(Flowable<T> upstream) {
        return new FlowableErrorJump<T, R>(upstream, transformer);
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {

        ErrorJumpFront<T, R> front = new ErrorJumpFront<T, R>(source, s);

        Publisher<R> downstream;

        try {
            downstream = ObjectHelper.requireNonNull(
                    transformer.apply(front),
                    "The transformer returned a null Publisher");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        downstream.subscribe(front.end);
    }

    static final class ErrorJumpFront<T, R> extends Flowable<T> implements FlowableSubscriber<T>, Subscription {

        final Flowable<T> source;

        final AtomicReference<Subscriber<? super T>> middle;

        final EndSubscriber end;

        Subscription upstream;

        ErrorJumpFront(Flowable<T> source, Subscriber<? super R> downstream) {
            this.source = source;
            this.middle = new AtomicReference<Subscriber<? super T>>();
            this.end = new EndSubscriber(downstream);
        }

        @Override
        protected void subscribeActual(Subscriber<? super T> s) {
            if (middle.compareAndSet(null, s)) {
                source.subscribe(this);
            } else {
                EmptySubscription.error(new IllegalStateException("Only one Subscriber allowed"), s);
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            upstream = s;
            middle.get().onSubscribe(this);
        }

        @Override
        public void onNext(T t) {
            middle.get().onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            end.set(t);
            middle.get().onComplete();
        }

        @Override
        public void onComplete() {
            middle.get().onComplete();
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
        }

        final class EndSubscriber
        extends AtomicReference<Throwable>
        implements FlowableSubscriber<R>, Subscription {

            private static final long serialVersionUID = -5718512540714037078L;

            final Subscriber<? super R> downstream;

            Subscription upstream;

            EndSubscriber(Subscriber<? super R> downstream) {
                this.downstream = downstream;
            }

            @Override
            public void onSubscribe(Subscription s) {
                upstream = s;
                downstream.onSubscribe(this);
            }

            @Override
            public void onNext(R t) {
                downstream.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                Throwable ex = get();
                if (ex != null) {
                    t = new CompositeException(ex, t);
                }
                downstream.onError(t);
            }

            @Override
            public void onComplete() {
                Throwable ex = get();
                if (ex != null) {
                    downstream.onError(ex);
                } else {
                    downstream.onComplete();
                }
            }

            @Override
            public void request(long n) {
                upstream.request(n);
            }

            @Override
            public void cancel() {
                upstream.cancel();
            }
        }
    }
}
