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

public class ObservableSumLong extends ObservableWithSource<Long, Long> {

    public ObservableSumLong(ObservableSource<Long> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Observer<? super Long> observer) {
        source.subscribe(new SumLongObserver(observer));
    }
    
    static final class SumLongObserver extends DeferredScalarObserver<Long, Long> {

        /** */
        private static final long serialVersionUID = 8645575082613773782L;

        long accumulator;
        
        boolean hasValue;
        
        public SumLongObserver(Observer<? super Long> actual) {
            super(actual);
        }

        @Override
        public void onNext(Long value) {
            if (!hasValue) {
                hasValue = true;
            }
            accumulator += value.longValue();
        }
        
        @Override
        public void onComplete() {
            if (hasValue) {
                complete(accumulator);
            } else {
                actual.onComplete();
            }
        }

    }
}
