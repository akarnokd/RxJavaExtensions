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

package hu.akarnokd.rxjava2.math;

import io.reactivex.internal.observers.DeferredScalarObserver;
import io.reactivex.*;

public class ObservableSumDouble extends ObservableWithSource<Double, Double> {

    public ObservableSumDouble(ObservableSource<Double> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Observer<? super Double> observer) {
        source.subscribe(new SumDoubleObserver(observer));
    }

    static final class SumDoubleObserver extends DeferredScalarObserver<Double, Double> {

        private static final long serialVersionUID = -769098775594601087L;

        double accumulator;

        boolean hasValue;

        SumDoubleObserver(Observer<? super Double> downstream) {
            super(downstream);
        }

        @Override
        public void onNext(Double value) {
            if (!hasValue) {
                hasValue = true;
            }
            accumulator += value.doubleValue();
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
