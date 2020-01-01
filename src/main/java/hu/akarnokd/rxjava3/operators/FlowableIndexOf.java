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

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.internal.subscribers.DeferredScalarSubscriber;

/**
 * Returns the first index of an element that matches a predicate or -1L if no elements match.
 *
 * @param <T> the upstream value type
 * @since 0.18.2
 */
final class FlowableIndexOf<T> extends Flowable<Long>
implements FlowableTransformer<T, Long> {

    final Flowable<T> source;

    final Predicate<? super T> predicate;

    FlowableIndexOf(Flowable<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    public Publisher<Long> apply(Flowable<T> upstream) {
        return new FlowableIndexOf<>(upstream, predicate);
    }

    @Override
    protected void subscribeActual(Subscriber<? super Long> s) {
        source.subscribe(new IndexOfSubscriber<T>(s, predicate));
    }

    static final class IndexOfSubscriber<T> extends DeferredScalarSubscriber<T, Long> {

        private static final long serialVersionUID = 4809092721669178986L;

        final Predicate<? super T> predicate;

        long index;
        boolean found;

        IndexOfSubscriber(Subscriber<? super Long> downstream,
                Predicate<? super T> predicate) {
            super(downstream);
            this.predicate = predicate;
        }

        @Override
        public void onNext(T t) {
            try {
                long idx = index;
                if (predicate.test(t)) {
                    found = true;
                    upstream.cancel();
                    complete(idx);
                    return;
                }
                index = idx + 1;
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                found = true;
                upstream.cancel();
                onError(ex);
                return;
            }
        }

        @Override
        public void onComplete() {
            if (!found) {
                complete(-1L);
            }
        }
    }
}
