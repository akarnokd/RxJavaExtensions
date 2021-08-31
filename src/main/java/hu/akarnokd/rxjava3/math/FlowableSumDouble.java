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

import org.reactivestreams.*;

import io.reactivex.rxjava3.internal.subscribers.DeferredScalarSubscriber;

final class FlowableSumDouble extends FlowableSource<Double, Double> {

    FlowableSumDouble(Publisher<Double> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Subscriber<? super Double> subscriber) {
        source.subscribe(new SumDoubleSubscriber(subscriber));
    }

    static final class SumDoubleSubscriber extends DeferredScalarSubscriber<Double, Double> {

        private static final long serialVersionUID = 600979972678601618L;

        double accumulator;

        SumDoubleSubscriber(Subscriber<? super Double> downstream) {
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
