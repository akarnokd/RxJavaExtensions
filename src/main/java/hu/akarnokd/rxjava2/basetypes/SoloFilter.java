/*
 * Copyright 2016-2018 David Karnok
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

import org.reactivestreams.Subscriber;

import io.reactivex.functions.Predicate;
import io.reactivex.internal.subscribers.BasicFuseableSubscriber;

/**
 * Tests the single value from upstream and emits it or completes.
 *
 * @param <T> the value type
 */
final class SoloFilter<T> extends Perhaps<T> {

    final Solo<T> source;

    final Predicate<? super T> predicate;

    SoloFilter(Solo<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new FilterSubscriber<T>(s, predicate));
    }

    static final class FilterSubscriber<T> extends BasicFuseableSubscriber<T, T> {

        final Predicate<? super T> predicate;

        FilterSubscriber(Subscriber<? super T> actual, Predicate<? super T> predicate) {
            super(actual);
            this.predicate = predicate;
        }

        @Override
        public void onNext(T t) {
            if (sourceMode == NONE) {
                boolean b;

                try {
                b = predicate.test(t);
                } catch (Throwable ex) {
                    fail(ex);
                    return;
                }

                if (b) {
                    downstream.onNext(t);
                }
            } else {
                downstream.onNext(null);
            }
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Override
        public T poll() throws Exception {
            T v = qs.poll();
            if (v != null) {
                if (predicate.test(v)) {
                    return v;
                }
            }
            return null;
        }

    }
}
