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

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableValveTest {

    @Test
    public void passthrough() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.<Integer>valve(Flowable.<Boolean>never()))
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void gatedoff() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.<Integer>valve(Flowable.<Boolean>never(), false))
        .test()
        .assertEmpty();
    }

    @Test
    public void syncGating() {
        PublishProcessor<Boolean> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Flowable.range(1, 10)
        .compose(FlowableTransformers.<Integer>valve(pp, false))
        .test();

        ts.assertEmpty();

        pp.onNext(true);

        ts.assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void syncGatingBackpressured() {
        PublishProcessor<Boolean> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Flowable.range(1, 10)
        .compose(FlowableTransformers.<Integer>valve(pp, false))
        .test(5);

        ts.assertEmpty();

        pp.onNext(true);

        ts.assertValues(1, 2, 3, 4, 5).assertNotComplete().assertNoErrors();

        pp.onNext(false);

        ts.request(5);

        ts.assertValues(1, 2, 3, 4, 5).assertNotComplete().assertNoErrors();

        pp.onNext(true);

        ts.assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void gating() {
        Flowable.intervalRange(1, 10, 17, 17, TimeUnit.MILLISECONDS)
        .compose(FlowableTransformers.<Long>valve(
                Flowable.interval(50, TimeUnit.MILLISECONDS).map(new Function<Long, Boolean>() {
            @Override
            public Boolean apply(Long v) throws Exception {
                return (v & 1) == 0;
            }
        }), true, 16))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
    }

    @Test
    public void mainError() {
        Flowable.<Integer>error(new IOException())
        .compose(FlowableTransformers.<Integer>valve(Flowable.<Boolean>never()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void otherError() {
        Flowable.just(1)
        .compose(FlowableTransformers.<Integer>valve(Flowable.<Boolean>error(new IOException())))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void otherCompletes() {
        Flowable.just(1)
        .compose(FlowableTransformers.<Integer>valve(Flowable.<Boolean>empty()))
        .test()
        .assertFailure(IllegalStateException.class);
    }

    @Test
    public void bothError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.<Integer>error(new IllegalArgumentException())
            .compose(FlowableTransformers.<Integer>valve(Flowable.<Boolean>error(new IOException())))
            .test()
            .assertFailure(IOException.class);

            TestHelper.assertError(errors, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void take() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.<Integer>valve(Flowable.<Boolean>never()))
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void openCloseRace() {
        for (int i = 0; i < 1000; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Boolean> pp2 = PublishProcessor.create();

            TestSubscriber<Integer> ts = pp1.compose(FlowableTransformers.<Integer>valve(pp2, false))
            .test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp1.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp2.onNext(true);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            ts.assertValue(1).assertNoErrors().assertNotComplete();
        }
    }
}
