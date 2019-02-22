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

package hu.akarnokd.rxjava2.operators;

import java.util.Iterator;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;

final class FlowableSwitchIfEmptyMany<T> extends Flowable<T>
implements FlowableTransformer<T, T> {

    final Flowable<T> source;

    final Iterable<? extends Publisher<? extends T>> alternatives;

    FlowableSwitchIfEmptyMany(Flowable<T> source, Iterable<? extends Publisher<? extends T>> alternatives) {
        this.source = source;
        this.alternatives = alternatives;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        Iterator<? extends Publisher<? extends T>> it;
        try {
            it = alternatives.iterator();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }
        SwitchManySubscriber<T> parent = new SwitchManySubscriber<T>(s, it);
        s.onSubscribe(parent);
        parent.drain(source);
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableSwitchIfEmptyMany<T>(upstream, alternatives);
    }

    static final class SwitchManySubscriber<T>
    extends AtomicInteger
    implements Subscriber<T>, Subscription {

        private static final long serialVersionUID = -174718617614474267L;

        final Subscriber<? super T> downstream;

        final AtomicLong requested;

        final AtomicReference<Subscription> upstream;

        final Iterator<? extends Publisher<? extends T>> alternatives;

        boolean hasValue;

        volatile boolean active;

        SwitchManySubscriber(Subscriber<? super T> downstream, Iterator<? extends Publisher<? extends T>> alternatives) {
            this.downstream = downstream;
            this.alternatives = alternatives;
            this.requested = new AtomicLong();
            this.upstream = new AtomicReference<Subscription>();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                Subscription a = upstream.get();
                if (a != null) {
                    a.request(n);
                }
            }
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(upstream);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.replace(this.upstream, s)) {
                long n = requested.get();
                if (n != 0L) {
                    s.request(n);
                }
            }
        }

        @Override
        public void onNext(T t) {
            if (!hasValue) {
                hasValue = true;
            }
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (hasValue) {
                downstream.onComplete();
            } else {
                active = false;
                drain(null);
            }
        }

        void drain(Publisher<? extends T> source) {
            if (getAndIncrement() == 0) {
                do {
                    if (SubscriptionHelper.CANCELLED == upstream.get()) {
                        return;
                    }

                    if (!active) {
                        if (source == null) {
                            boolean b;
                            try {
                                b = alternatives.hasNext();
                                if (b) {
                                    source = alternatives.next();
                                }
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                downstream.onError(ex);
                                return;
                            }

                            if (!b) {
                                downstream.onComplete();
                                return;
                            }
                            if (source == null) {
                                downstream.onError(new NullPointerException("The alternative Publisher is null"));
                                return;
                            }
                        }
                        active = true;
                        source.subscribe(this);
                        source = null;
                    }
                } while (decrementAndGet() != 0);
            }
        }
    }
}
