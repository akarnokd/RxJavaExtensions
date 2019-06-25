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

package hu.akarnokd.rxjava3.math;

import java.util.Comparator;

import io.reactivex.internal.observers.DeferredScalarObserver;
import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;

final class ObservableMinMax<T> extends ObservableWithSource<T, T> {

    final Comparator<? super T> comparator;

    final int flag;

    ObservableMinMax(ObservableSource<T> source, Comparator<? super T> comparator, int flag) {
        super(source);
        this.comparator = comparator;
        this.flag = flag;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new MinMaxSubscriber<T>(observer, comparator, flag));
    }

    static final class MinMaxSubscriber<T> extends DeferredScalarObserver<T, T> {

        private static final long serialVersionUID = -4484454790848904397L;

        final Comparator<? super T> comparator;

        final int flag;

        MinMaxSubscriber(Observer<? super T> downstream, Comparator<? super T> comparator, int flag) {
            super(downstream);
            this.comparator = comparator;
            this.flag = flag;
        }

        @Override
        public void onNext(T value) {
            try {
                T v = this.value;
                if (v != null) {
                    if (comparator.compare(v, value) * flag > 0) {
                        this.value = value;
                    }
                } else {
                    this.value = value;
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.dispose();
                downstream.onError(ex);
            }
        }

    }
}
