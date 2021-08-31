/*
 * Copyright 2016-present David Karnok
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

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.internal.observers.DeferredScalarObserver;

final class ObservableSumInt extends ObservableWithSource<Integer, Integer> {

    ObservableSumInt(ObservableSource<Integer> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Observer<? super Integer> observer) {
        source.subscribe(new SumIntObserver(observer));
    }

    static final class SumIntObserver extends DeferredScalarObserver<Integer, Integer> {

        private static final long serialVersionUID = 5434323279399190675L;

        int accumulator;

        boolean hasValue;

        SumIntObserver(Observer<? super Integer> downstream) {
            super(downstream);
        }

        @Override
        public void onNext(Integer value) {
            if (!hasValue) {
                hasValue = true;
            }
            accumulator += value.intValue();
        }

        @Override
        public void onComplete() {
            if (hasValue) {
                complete(accumulator);
            } else {
                downstream.onComplete();
            }
        }

    }
}
