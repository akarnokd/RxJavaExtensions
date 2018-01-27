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
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.Flowable;
import io.reactivex.internal.schedulers.ImmediateThinScheduler;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableRequestObserveOnTest {

    @Test
    public void normal() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.requestObserveOn(Schedulers.computation()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void take() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.requestObserveOn(Schedulers.computation()))
        .take(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void error() {
        Flowable.error(new IOException())
        .compose(FlowableTransformers.requestObserveOn(Schedulers.computation()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void backpressured() throws Exception {
        TestScheduler testSched = new TestScheduler();
        TestSubscriber<Integer> ts = Flowable.range(1, 2)
        .compose(FlowableTransformers.<Integer>requestObserveOn(testSched))
        .test(0L);

        testSched.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        ts.assertEmpty();

        ts.requestMore(1);

        testSched.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        ts.assertValue(1);

        ts.requestMore(1);

        testSched.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        ts.assertResult(1, 2);

        ts.requestMore(1);

        testSched.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        ts.assertResult(1, 2);
    }


    @Test
    public void requestAfterCompleteImmediate() throws Exception {
        TestSubscriber<Integer> ts = Flowable.range(1, 2)
        .compose(FlowableTransformers.<Integer>requestObserveOn(ImmediateThinScheduler.INSTANCE))
        .test(0L);

        ts.assertEmpty();

        ts.requestMore(1);

        ts.assertValue(1);

        ts.requestMore(1);

        ts.assertResult(1, 2);

        ts.requestMore(1);

        ts.assertResult(1, 2);
    }
}
