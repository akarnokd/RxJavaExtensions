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

package hu.akarnokd.rxjava4.operators;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import hu.akarnokd.rxjava4.internal.*;
import hu.akarnokd.rxjava4.test.TestHelper;
import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.exceptions.MissingBackpressureException;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.schedulers.Schedulers;

public class FlowableRequestSampleTimeTest {

    @Test
    public void normal() {
        Flowables.repeatSupplier(Functions.justSupplier(1))
        .compose(FlowableTransformers.requestSample(1, TimeUnit.MILLISECONDS, Schedulers.single()))
        .take(5)
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void range() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.requestSample(1, TimeUnit.MILLISECONDS, Schedulers.single()))
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void error() {
        Flowable.error(new IOException())
        .compose(FlowableTransformers.requestSample(1, TimeUnit.SECONDS, Schedulers.single()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void badSourceNoRequests() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onNext(2);
                    s.onComplete();
                    s.onError(new IOException());
                }
            }
            .compose(FlowableTransformers.requestSample(5, TimeUnit.SECONDS, Schedulers.single()))
            .test(0L)
            .assertFailure(MissingBackpressureException.class);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
