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

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.*;

import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.MissingBackpressureException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FlowableRequestSampleTest {

    @Test
    public void normal() {
        Flowables.repeatSupplier(Functions.justSupplier(1))
        .compose(FlowableTransformers.requestSample(Flowable.interval(1, TimeUnit.MILLISECONDS, Schedulers.single())))
        .take(5)
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void normal2() {
        Flowables.repeatSupplier(Functions.justSupplier(1))
        .compose(FlowableTransformers.requestSample(
            Flowable.fromArray(10, 50, 100, 200, 500)
            .concatMap(new Function<Integer, Publisher<? extends Object>>() {
                @Override
                public Publisher<? extends Object> apply(Integer delay)
                        throws Exception {
                    return Flowable.timer(delay, TimeUnit.MILLISECONDS);
                }
            })
        ))
        .take(5)
        .test()
        .awaitDone(7, TimeUnit.SECONDS)
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void range() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.requestSample(Flowable.interval(1, TimeUnit.MILLISECONDS, Schedulers.single())))
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void error() {
        Flowable.error(new IOException())
        .compose(FlowableTransformers.requestSample(Flowable.interval(1, TimeUnit.SECONDS, Schedulers.single())))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void error2() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        Flowable.error(new IOException())
        .compose(FlowableTransformers.requestSample(pp))
        .test()
        .assertFailure(IOException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void otherError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp
        .compose(FlowableTransformers.requestSample(Flowable.error(new IOException())))
        .test()
        .assertFailure(IOException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void otherComplete() {
        Flowable.never()
        .compose(FlowableTransformers.requestSample(Flowable.error(new IOException())))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void otherComplete2() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp
        .compose(FlowableTransformers.requestSample(Flowable.empty()))
        .test()
        .assertResult();

        assertFalse(pp.hasSubscribers());
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
            .compose(FlowableTransformers.requestSample(Flowable.interval(5, TimeUnit.SECONDS, Schedulers.single())))
            .test(0L)
            .assertFailure(MissingBackpressureException.class);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cancelledUpfront() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp
        .compose(FlowableTransformers.requestSample(Flowable.interval(1, TimeUnit.MILLISECONDS, Schedulers.single())))
        .test(0L, true)
        .assertEmpty();

        assertFalse(pp.hasSubscribers());
    }
}
