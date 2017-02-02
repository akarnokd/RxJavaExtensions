/*
 * Copyright 2016-2017 David Karnok
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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.*;

/**
 * Repeat when the Publisher signals an item.
 */
final class NonoRepeatWhen extends Nono {

    final Nono source;

    final Function<? super Flowable<Object>, ? extends Publisher<?>> handler;

    NonoRepeatWhen(Nono source, Function<? super Flowable<Object>, ? extends Publisher<?>> handler) {
        this.source = source;
        this.handler = handler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        FlowableProcessor<Object> processor = PublishProcessor.create().toSerialized();

        Publisher<?> p;
        try {
            p = handler.apply(processor);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        RepeatWhenMainSubscriber parent = new RepeatWhenMainSubscriber(s, processor, source);
        s.onSubscribe(parent);

        p.subscribe(parent.inner);

        source.subscribe(parent);
    }

    interface RedoSupport {

        void innerNext();

        void innerError(Throwable ex);

        void innerComplete();
    }

    static final class RepeatWhenMainSubscriber extends BasicNonoIntQueueSubscription
    implements Subscriber<Void>, RedoSupport {

        private static final long serialVersionUID = 6463015514357680572L;

        final Subscriber<? super Void> actual;

        final AtomicReference<Subscription> s;

        final RedoInnerSubscriber inner;

        final AtomicBoolean once;

        final FlowableProcessor<Object> processor;

        final Nono source;

        volatile boolean active;

        RepeatWhenMainSubscriber(Subscriber<? super Void> actual, FlowableProcessor<Object> processor, Nono source) {
            this.actual = actual;
            this.s = new AtomicReference<Subscription>();
            this.inner = new RedoInnerSubscriber(this);
            this.once = new AtomicBoolean();
            this.processor = processor;
            this.source = source;
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(s);
            inner.cancel();
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.setOnce(this.s, s);
        }

        @Override
        public void onNext(Void t) {
            // never called
        }

        @Override
        public void onError(Throwable t) {
            inner.cancel();
            if (once.compareAndSet(false, true)) {
                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            active = false;
            if (getAndIncrement() == 0) {
                do {
                    if (SubscriptionHelper.isCancelled(s.get())) {
                        return;
                    }

                    if (!active) {
                        active = true;
                        inner.request(1);
                        processor.onNext(0);
                    }
                } while (decrementAndGet() != 0);
            }
        }

        @Override
        public void innerNext() {
            source.subscribe(this);
        }

        @Override
        public void innerError(Throwable ex) {
            SubscriptionHelper.cancel(s);
            if (once.compareAndSet(false, true)) {
                actual.onError(ex);
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void innerComplete() {
            SubscriptionHelper.cancel(s);
            if (once.compareAndSet(false, true)) {
                actual.onComplete();
            }
        }

    }

    static final class RedoInnerSubscriber extends AtomicReference<Subscription>
    implements Subscriber<Object>, Subscription {

        private static final long serialVersionUID = 3973630610536953229L;

        final RedoSupport parent;

        final AtomicLong requested = new AtomicLong();

        RedoInnerSubscriber(RedoSupport parent) {
            this.parent = parent;
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(this, requested, s);
        }

        @Override
        public void onNext(Object t) {
            parent.innerNext();
        }

        @Override
        public void onError(Throwable t) {
            parent.innerError(t);
        }

        @Override
        public void onComplete() {
            parent.innerComplete();
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(this, requested, n);
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(this);
        }
    }

}
