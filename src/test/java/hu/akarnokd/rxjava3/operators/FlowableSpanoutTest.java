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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableSpanoutTest {

    @Test
    public void normal() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
            .compose(FlowableTransformers.<Integer>spanout(100, TimeUnit.MILLISECONDS, scheduler))
            .test();

        ts.assertEmpty();

        pp.onNext(1);
        pp.onNext(2);

        ts.assertEmpty();

        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);

        ts.assertValue(1);

        pp.onNext(3);

        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);

        ts.assertValues(1, 2);

        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);

        ts.assertValues(1, 2);

        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);

        ts.assertValues(1, 2, 3);

        pp.onComplete();

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        ts.assertResult(1, 2, 3);
    }

    @Test
    public void normalInitialDelay() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
            .compose(FlowableTransformers.<Integer>spanout(100, 100, TimeUnit.MILLISECONDS, scheduler))
            .test();

        ts.assertEmpty();

        pp.onNext(1);
        pp.onNext(2);

        ts.assertEmpty();

        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);

        ts.assertEmpty();

        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);

        ts.assertValue(1);

        pp.onNext(3);

        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);

        ts.assertValue(1);

        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);

        ts.assertValues(1, 2);

        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);

        ts.assertValues(1, 2);

        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);

        ts.assertValues(1, 2, 3);

        pp.onComplete();

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        ts.assertResult(1, 2, 3);
    }

    @Test
    public void errorImmediate() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
            .compose(FlowableTransformers.<Integer>spanout(100, TimeUnit.MILLISECONDS, scheduler))
            .test();

        pp.onNext(1);
        pp.onError(new IOException());

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        ts.assertFailure(IOException.class);
    }

    @Test
    public void errorDelayed() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
            .compose(FlowableTransformers.<Integer>spanout(100L, TimeUnit.MILLISECONDS, scheduler, true))
            .test();

        pp.onNext(1);
        pp.onError(new IOException());

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        ts.assertFailure(IOException.class, 1);
    }

    @Test
    public void normal2() {
        Flowable.range(1, 3)
        .compose(FlowableTransformers.<Integer>spanout(100L, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);
    }

    @Test
    public void normal3() {
        Flowable.range(1, 3)
        .compose(FlowableTransformers.<Integer>spanout(100L, TimeUnit.MILLISECONDS, true))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);
    }

    @Test
    public void normal4() {
        Flowable.range(1, 3)
        .compose(FlowableTransformers.<Integer>spanout(100L, TimeUnit.MILLISECONDS, Schedulers.io()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);
    }

    @Test
    public void normal5() {
        Flowable.range(1, 3)
        .compose(FlowableTransformers.<Integer>spanout(100L, TimeUnit.MILLISECONDS, Schedulers.io(), true))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);
    }

    @Test
    public void normalInitial2() {
        Flowable.range(1, 3)
        .compose(FlowableTransformers.<Integer>spanout(50L, 100L, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);
    }

    @Test
    public void normalInitial3() {
        Flowable.range(1, 3)
        .compose(FlowableTransformers.<Integer>spanout(50L, 100L, TimeUnit.MILLISECONDS, true))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);
    }

    @Test
    public void normalInitial4() {
        Flowable.range(1, 3)
        .compose(FlowableTransformers.<Integer>spanout(50L, 100L, TimeUnit.MILLISECONDS, Schedulers.io()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);
    }

    @Test
    public void normalInitial5() {
        Flowable.range(1, 3)
        .compose(FlowableTransformers.<Integer>spanout(50L, 100L, TimeUnit.MILLISECONDS, Schedulers.io(), true))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);
    }

    @Test
    public void take() {
        Flowable.range(1, 3)
        .compose(FlowableTransformers.<Integer>spanout(100L, TimeUnit.MILLISECONDS))
        .take(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2);
    }

    @Test
    public void emitWayAfterPrevious() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
            .compose(FlowableTransformers.<Integer>spanout(100, TimeUnit.MILLISECONDS, scheduler))
            .test();

        pp.onNext(1);

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        ts.assertValue(1);

        scheduler.advanceTimeBy(50, TimeUnit.MILLISECONDS);

        pp.onNext(2);

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        ts.assertValues(1, 2);
    }

    @Test
    public void empty() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
            .compose(FlowableTransformers.<Integer>spanout(100, TimeUnit.MILLISECONDS, scheduler))
            .test();

        pp.onComplete();

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        ts.assertResult();
    }

    @Test
    public void errorImmediateWithoutValue() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
            .compose(FlowableTransformers.<Integer>spanout(100, TimeUnit.MILLISECONDS, scheduler))
            .test();

        pp.onError(new IOException());

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        ts.assertFailure(IOException.class);
    }

    @Test
    public void errorDelayErrorWithoutValue() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
            .compose(FlowableTransformers.<Integer>spanout(100, TimeUnit.MILLISECONDS, scheduler, true))
            .test();

        pp.onError(new IOException());

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        ts.assertFailure(IOException.class);
    }
}
