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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

final class FlowableSwitchIfEmptyManyArray<T> extends Flowable<T>
implements FlowableTransformer<T, T> {

    final Flowable<T> source;

    final Publisher<? extends T>[] alternatives;

    FlowableSwitchIfEmptyManyArray(Flowable<T> source, Publisher<? extends T>[] alternatives) {
        this.source = source;
        this.alternatives = alternatives;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        SwitchManySubscriber<T> parent = new SwitchManySubscriber<T>(s, alternatives);
        s.onSubscribe(parent);
        parent.drain(source);
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableSwitchIfEmptyManyArray<T>(upstream, alternatives);
    }

    static final class SwitchManySubscriber<T>
    extends AtomicInteger
    implements Subscriber<T>, Subscription {

        private static final long serialVersionUID = -174718617614474267L;

        final Subscriber<? super T> downstream;

        final AtomicLong requested;

        final AtomicReference<Subscription> upstream;

        final Publisher<? extends T>[] alternatives;

        int index;

        boolean hasValue;

        volatile boolean active;

        SwitchManySubscriber(Subscriber<? super T> downstream, Publisher<? extends T>[] alternatives) {
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
                            int idx = index;
                            Publisher<? extends T>[] a = alternatives;
                            if (idx == a.length) {
                                downstream.onComplete();
                                return;
                            }
                            source = a[idx];
                            if (source == null) {
                                downstream.onError(new NullPointerException("The " + idx + "th alternative Publisher is null"));
                                return;
                            }
                            index = idx + 1;
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
