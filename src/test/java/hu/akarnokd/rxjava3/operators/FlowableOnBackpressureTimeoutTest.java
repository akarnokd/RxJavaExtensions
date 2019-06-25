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
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableOnBackpressureTimeoutTest implements Consumer<Object> {

    final List<Object> evicted = Collections.synchronizedList(new ArrayList<Object>());

    @Override
    public void accept(Object t) throws Exception {
        evicted.add(t);
    }

    @Test
    public void empty() {
        Flowable.<Integer>empty()
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, TimeUnit.MINUTES))
        .test()
        .assertResult();
    }

    @Test
    public void error() {
        Flowable.<Integer>error(new IOException())
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, TimeUnit.MINUTES))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void errorDelayed() {
        TestSubscriber<Integer> ts = Flowable.just(1).concatWith(Flowable.<Integer>error(new IOException()))
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, TimeUnit.MINUTES))
        .test(0);

        ts
        .assertEmpty()
        .requestMore(1)
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void normal1() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, TimeUnit.MINUTES))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normal1SingleStep() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, TimeUnit.MINUTES))
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normal2() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, TimeUnit.MINUTES, Schedulers.single()))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normal2SingleStep() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, TimeUnit.MINUTES, Schedulers.single()))
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normal3() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(10, 1, TimeUnit.MINUTES, Schedulers.single(), this))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normal3SingleStep() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(10, 1, TimeUnit.MINUTES, Schedulers.single(), this))
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normal4() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, TimeUnit.MINUTES, Schedulers.single(), this))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normal4SingleStep() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, TimeUnit.MINUTES, Schedulers.single(), this))
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void bufferLimit() {
        TestSubscriber<Integer> ts = Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, 1, TimeUnit.MINUTES, Schedulers.single(), this))
        .test(0);

        ts
        .assertEmpty()
        .requestMore(1)
        .assertResult(5);

        Assert.assertEquals(Arrays.asList(1, 2, 3, 4), evicted);
    }

    @Test
    public void timeoutLimit() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, 1, TimeUnit.SECONDS, scheduler, this))
        .test(0);

        ts.assertEmpty();

        pp.onNext(1);

        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        pp.onNext(2);

        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        pp.onNext(3);

        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        pp.onNext(4);

        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        pp.onNext(5);

        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        pp.onComplete();

        ts
        .assertEmpty()
        .requestMore(1)
        .assertResult(5);

        Assert.assertEquals(Arrays.asList(1, 2, 3, 4), evicted);
    }

    @Test
    public void take() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, TimeUnit.MINUTES))
        .take(2)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void cancelEvictAll() {
        TestSubscriber<Integer> ts = Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, TimeUnit.MINUTES, Schedulers.single(), this))
        .test(0);

        ts.cancel();

        Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5), evicted);
    }

    @Test
    public void timeoutEvictAll() {
        TestScheduler scheduler = new TestScheduler();

        TestSubscriber<Integer> ts = Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, TimeUnit.SECONDS, scheduler, this))
        .test(0);

        scheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        ts
        .requestMore(1)
        .assertResult();

        Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5), evicted);
    }

    @Test
    public void evictCancels() {
        TestScheduler scheduler = new TestScheduler();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

        pp
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(10, 1, TimeUnit.SECONDS, scheduler, new Consumer<Integer>() {
            @Override
            public void accept(Integer e) throws Exception {
                evicted.add(e);
                ts.cancel();
            }
        }))
        .subscribe(ts);

        TestHelper.emit(pp, 1, 2, 3, 4, 5);

        scheduler.advanceTimeBy(1, TimeUnit.MINUTES);

        Assert.assertEquals(Arrays.asList(1, 2, 3, 4, 5), evicted);
    }

    @Test
    public void evictThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestScheduler scheduler = new TestScheduler();

            PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

            pp
            .compose(FlowableTransformers.<Integer>onBackpressureTimeout(10, 1, TimeUnit.SECONDS, scheduler, new Consumer<Integer>() {
                @Override
                public void accept(Integer e) throws Exception {
                    throw new IOException(e.toString());
                }
            }))
            .subscribe(ts);

            TestHelper.emit(pp, 1, 2, 3, 4, 5);

            scheduler.advanceTimeBy(1, TimeUnit.MINUTES);

            ts.requestMore(1).assertResult();

            TestHelper.assertUndeliverable(errors, 0, IOException.class, "1");
            TestHelper.assertUndeliverable(errors, 1, IOException.class, "2");
            TestHelper.assertUndeliverable(errors, 2, IOException.class, "3");
            TestHelper.assertUndeliverable(errors, 3, IOException.class, "4");
            TestHelper.assertUndeliverable(errors, 4, IOException.class, "5");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cancelAndRequest() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>onBackpressureTimeout(1, TimeUnit.MINUTES, Schedulers.single(), this))
        .subscribe(new TestSubscriber<Integer>(1) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
            }
        });
    }

    @Test
    public void example() {
        Flowable.intervalRange(1, 5, 100, 100, TimeUnit.MILLISECONDS)
        .compose(FlowableTransformers
            .<Long>onBackpressureTimeout(2, 100, TimeUnit.MILLISECONDS,
                 Schedulers.single(), this))
        .test(0)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();

        Assert.assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), evicted);
    }
}
