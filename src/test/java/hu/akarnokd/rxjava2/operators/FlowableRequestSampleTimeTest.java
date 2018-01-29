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

package hu.akarnokd.rxjava2.operators;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.Flowable;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class FlowableRequestSampleTimeTest {

    @Test
    public void normal() {
        Flowables.repeatCallable(Functions.justCallable(1))
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
