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

import java.util.Objects;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Wait for the very first item and if it matches a predicate, switch to
 * an alternative, generated follow-up sequence; otherwise resume with the old sequence.
 *
 * @param <T> the source element type to test
 * @since 0.20.7
 */
final class FlowableSwitchOnFirst<T> extends Flowable<T> implements FlowableTransformer<T, T> {

    final Flowable<T> source;

    final Predicate<? super T> predicate;

    final Function<? super T, ? extends Publisher<? extends T>> selector;

    FlowableSwitchOnFirst(Flowable<T> source, Predicate<? super T> predicate,
            Function<? super T, ? extends Publisher<? extends T>> selector) {
        super();
        this.source = source;
        this.predicate = predicate;
        this.selector = selector;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new SwitchOnFirstSubscriber<>(s, predicate, selector));
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableSwitchOnFirst<>(upstream, predicate, selector);
    }

    static final class SwitchOnFirstSubscriber<T> extends AtomicBoolean
    implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -3008387388867141204L;

        final Subscriber<? super T> downstream;

        final Predicate<? super T> predicate;

        final Function<? super T, ? extends Publisher<? extends T>> selector;

        final SwitchOnSecondarySubscriber<T> secondary;

        Subscription upstream;

        boolean isMainSequence;

        boolean once;

        SwitchOnFirstSubscriber(Subscriber<? super T> downstream, Predicate<? super T> predicate,
                Function<? super T, ? extends Publisher<? extends T>> selector) {
            this.downstream = downstream;
            this.predicate = predicate;
            this.selector = selector;
            this.secondary = new SwitchOnSecondarySubscriber<>(downstream);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(upstream, s)) {
                upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!once) {
                once = true;

                Publisher<? extends T> resume = null;

                try {
                    if (predicate.test(t)) {
                        resume = Objects.requireNonNull(selector.apply(t), "The selector returned a null Publisher");
                    }
                } catch (Throwable ex) {
                    upstream.cancel();
                    downstream.onError(ex);
                    return;
                }
                if (resume != null) {
                    upstream.cancel();
                    upstream = SubscriptionHelper.CANCELLED;
                    resume.subscribe(secondary);
                } else {
                    isMainSequence = true;
                }
            }
            if (isMainSequence) {
                downstream.onNext(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!once || isMainSequence) {
                downstream.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!once || isMainSequence) {
                downstream.onComplete();
            }
        }

        @Override
        public void request(long n) {
            if (compareAndSet(false, true)) {
                n--;
                upstream.request(1L);
                if (n == 0L) {
                    return;
                }
            }
            if (isMainSequence) {
                upstream.request(n);
            } else {
                SwitchOnSecondarySubscriber<T> secondary = this.secondary;
                if (secondary.upstream.get() == null) {
                    upstream.request(n);
                }
                SubscriptionHelper.deferredRequest(secondary.upstream, secondary, n);
            }
        }

        @Override
        public void cancel() {
            upstream.cancel();
            SubscriptionHelper.cancel(secondary.upstream);
        }

        static final class SwitchOnSecondarySubscriber<T> extends AtomicLong
        implements FlowableSubscriber<T> {

            private static final long serialVersionUID = 6866823891735850338L;

            final Subscriber<? super T> downstream;

            final AtomicReference<Subscription> upstream;

            SwitchOnSecondarySubscriber(Subscriber<? super T> downstream) {
                this.downstream = downstream;
                this.upstream = new AtomicReference<>();
            }

            @Override
            public void onSubscribe(Subscription s) {
                SubscriptionHelper.deferredSetOnce(upstream, this, s);
            }

            @Override
            public void onNext(T t) {
                downstream.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                downstream.onError(t);
            }

            @Override
            public void onComplete() {
                downstream.onComplete();
            }
        }
    }
}
