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

import java.util.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.operators.ConditionalSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Buffer into the same buffer while the predicate returns true or
 * buffer into the same buffer until predicate returns true.
 *
 * @param <T> the source value type
 * @param <C> the buffer type
 *
 * @since 0.8.0
 */
final class FlowableBufferPredicate<T, C extends Collection<? super T>> extends Flowable<C> implements FlowableTransformer<T, C> {

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

    final Supplier<C> bufferSupplier;

    FlowableBufferPredicate(Publisher<T> source, Predicate<? super T> predicate, Mode mode,
            Supplier<C> bufferSupplier) {
        this.source = source;
        this.predicate = predicate;
        this.mode = mode;
        this.bufferSupplier = bufferSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super C> s) {
        C buffer;

        try {
            buffer = Objects.requireNonNull(bufferSupplier.get(), "The bufferSupplier returned a null buffer");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        source.subscribe(new BufferPredicateSubscriber<>(s, buffer, predicate, mode, bufferSupplier));
    }

    @Override
    public Publisher<C> apply(Flowable<T> upstream) {
        return new FlowableBufferPredicate<>(upstream, predicate, mode, bufferSupplier);
    }

    static final class BufferPredicateSubscriber<T, C extends Collection<? super T>>
    implements ConditionalSubscriber<T>, Subscription {

        final Subscriber<? super C> downstream;

        final Predicate<? super T> predicate;

        final Mode mode;

        final Supplier<C> bufferSupplier;

        C buffer;

        Subscription upstream;

        int count;

        BufferPredicateSubscriber(Subscriber<? super C> downstream,
                C buffer,
                Predicate<? super T> predicate, Mode mode,
                Supplier<C> bufferSupplier) {
            this.downstream = downstream;
            this.predicate = predicate;
            this.mode = mode;
            this.buffer = buffer;
            this.bufferSupplier = bufferSupplier;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                upstream.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            C buf = buffer;
            if (buf != null) {
                boolean b;

                try {
                    b = predicate.test(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    upstream.cancel();
                    buffer = null;
                    downstream.onError(ex);
                    return true;
                }

                switch (mode) {
                case AFTER: {
                    buf.add(t);
                    if (b) {
                        downstream.onNext(buf);

                        try {
                            buffer = bufferSupplier.get();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            upstream.cancel();
                            onError(ex);
                            return true;
                        }

                        count = 0;
                    } else {
                        count++;
                        return false;
                    }
                    break;
                }
                case BEFORE: {
                    if (b) {
                        buf.add(t);
                        count++;
                        return false;
                    } else {
                        downstream.onNext(buf);
                        try {
                            buf = bufferSupplier.get();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            upstream.cancel();
                            onError(ex);
                            return true;
                        }

                        buf.add(t);
                        buffer = buf;
                        count = 1;
                    }
                    break;
                }
                default:
                    if (b) {
                        downstream.onNext(buf);

                        try {
                            buffer = bufferSupplier.get();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            upstream.cancel();
                            onError(ex);
                            return true;
                        }

                        count = 0;
                    } else {
                        buf.add(t);
                        count++;
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public void onError(Throwable t) {
            if (buffer != null) {
                buffer = null;
                downstream.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            C b = buffer;
            if (b != null) {
                buffer = null;
                if (count != 0) {
                    downstream.onNext(b);
                }
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
