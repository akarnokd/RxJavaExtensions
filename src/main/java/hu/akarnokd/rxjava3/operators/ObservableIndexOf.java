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

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.observers.DeferredScalarObserver;

/**
 * Returns the first index of an element that matches a predicate or -1L if no elements match.
 *
 * @param <T> the upstream value type
 * @since 0.18.2
 */
final class ObservableIndexOf<T> extends Observable<Long>
implements ObservableTransformer<T, Long> {

    final Observable<T> source;

    final Predicate<? super T> predicate;

    ObservableIndexOf(Observable<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    public ObservableSource<Long> apply(Observable<T> upstream) {
        return new ObservableIndexOf<T>(upstream, predicate);
    }

    @Override
    protected void subscribeActual(Observer<? super Long> observer) {
        source.subscribe(new IndexOfObserver<T>(observer, predicate));
    }

    static final class IndexOfObserver<T> extends DeferredScalarObserver<T, Long> {

        private static final long serialVersionUID = 4809092721669178986L;

        final Predicate<? super T> predicate;

        long index;
        boolean found;

        IndexOfObserver(Observer<? super Long> downstream,
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
                    upstream.dispose();
                    complete(idx);
                    return;
                }
                index = idx + 1;
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                found = true;
                upstream.dispose();
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
