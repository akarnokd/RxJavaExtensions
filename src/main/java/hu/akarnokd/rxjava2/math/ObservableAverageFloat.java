/*
 * Copyright 2016 David Karnok
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

import io.reactivex.internal.subscribers.observable.DeferredScalarObserver;
import io.reactivex.*;

public class ObservableAverageFloat extends ObservableWithSource<Number, Float> {

    public ObservableAverageFloat(ObservableSource<Number> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Observer<? super Float> observer) {
        source.subscribe(new AverageFloatObserver(observer));
    }
    
    static final class AverageFloatObserver extends DeferredScalarObserver<Number, Float> {

        /** */
        private static final long serialVersionUID = -4845767048730060914L;

        float accumulator;
        
        int count;
        
        public AverageFloatObserver(Observer<? super Float> actual) {
            super(actual);
        }

        @Override
        public void onNext(Number value) {
            count++;
            accumulator += value.floatValue();
        }
        
        @Override
        public void onComplete() {
            int c = count;
            if (c != 0) {
                complete(accumulator / c);
            } else {
                actual.onComplete();
            }
        }

    }
}
