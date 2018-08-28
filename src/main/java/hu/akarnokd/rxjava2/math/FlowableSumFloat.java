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

import org.reactivestreams.*;

import io.reactivex.internal.subscribers.DeferredScalarSubscriber;

final class FlowableSumFloat extends FlowableSource<Float, Float> {

    FlowableSumFloat(Publisher<Float> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Subscriber<? super Float> subscriber) {
        source.subscribe(new SumFloatSubscriber(subscriber));
    }

    static final class SumFloatSubscriber extends DeferredScalarSubscriber<Float, Float> {

        private static final long serialVersionUID = 600979972678601618L;

        float accumulator;

        SumFloatSubscriber(Subscriber<? super Float> downstream) {
            super(downstream);
        }

        @Override
        public void onNext(Float value) {
            if (!hasValue) {
                hasValue = true;
            }
            accumulator += value.floatValue();
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
