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

package hu.akarnokd.rxjava2.operators;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.*;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.*;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableSwitchFlatMapTest {

    @Test
    public void normal() {
        PublishProcessor<Integer> ps = PublishProcessor.create();

        @SuppressWarnings("unchecked")
        final PublishProcessor<Integer>[] pss = new PublishProcessor[3];
        for (int i = 0; i < pss.length; i++) {
            pss[i] = PublishProcessor.create();
        }

        TestSubscriber<Integer> ts = ps
        .compose(FlowableTransformers.switchFlatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return pss[v];
            }
        }, 2))
        .test();

        ps.onNext(0);
        ps.onNext(1);

        pss[0].onNext(1);
        pss[0].onNext(2);
        pss[0].onNext(3);

        pss[1].onNext(10);
        pss[1].onNext(11);
        pss[1].onNext(12);

        ps.onNext(2);

        assertFalse(pss[0].hasSubscribers());

        pss[0].onNext(4);

        pss[2].onNext(20);
        pss[2].onNext(21);
        pss[2].onNext(22);

        pss[1].onComplete();
        pss[2].onComplete();
        ps.onComplete();

        ts.assertResult(1, 2, 3, 10, 11, 12, 20, 21, 22);
    }

    @Test
    public void normalSimple() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(Flowable.just(1)), 1))
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void empty() {
        Flowable.empty()
        .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(Flowable.just(1)), 1))
        .test()
        .assertResult();
    }

    @Test
    public void emptyInner() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(Flowable.empty()), 1))
        .test()
        .assertResult();
    }

    @Test
    public void emptyBackpressured() {
        Flowable.empty()
        .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(Flowable.just(1)), 1))
        .test(0)
        .assertResult();
    }

    @Test
    public void emptyInnerBackpressured() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(Flowable.empty()), 1))
        .test(0)
        .assertResult();
    }

    @Test
    public void errorOuter() {
        Flowable.error(new IOException())
        .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(Flowable.just(1)), 1))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void errorInner() {
        Flowable.just(1)
        .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(Flowable.error(new IOException())), 1))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void backpressure() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(Flowable.just(1)), 1))
        .rebatchRequests(1)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void take() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(Flowable.just(1)), 1))
        .take(3)
        .test()
        .assertResult(1, 1, 1);
    }

    @Test
    public void mixed() {
        for (int i = 1; i < 33; i++) {
            Flowable.interval(2, TimeUnit.MILLISECONDS)
            .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(Flowable.interval(1, TimeUnit.MILLISECONDS)), i))
            .take(100)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValueCount(100)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test
    public void mixed2() {
        for (int i = 1; i < 33; i++) {
            Flowable.interval(2, TimeUnit.MILLISECONDS)
            .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(Flowable.interval(1, TimeUnit.MILLISECONDS).take(16)), i))
            .rebatchRequests(1)
            .take(100)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValueCount(100)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test
    public void mixed3() {
        for (int i = 1; i < 33; i++) {
            Flowable.interval(2, TimeUnit.MILLISECONDS)
            .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(Flowable.interval(1, TimeUnit.MILLISECONDS).take(16)), i, 16))
            .rebatchRequests(1)
            .take(100)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValueCount(100)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test
    public void innerErrorRace() {
        final Throwable ex = new IOException();

        for (int i = 0; i < 1000; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            TestSubscriber<Integer> ts = pp1
            .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(pp2), 2))
            .test();

            pp1.onNext(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp1.onComplete();
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp2.onError(ex);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            ts.assertFailure(IOException.class);
        }
    }

    @Test
    public void cancel() {
        final PublishProcessor<Integer> pp1 = PublishProcessor.create();
        final PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp1
        .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(pp2), 2))
        .test();

        pp1.onNext(1);

        ts.cancel();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());
    }

    @Test
    public void outerDoubleError() {
        List<Throwable> error = TestHelper.trackPluginErrors();
        try {
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onError(new IOException());
                    s.onError(new IllegalArgumentException());
                }
            }
            .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(pp2), 2))
            .test()
            .assertFailure(IOException.class);

            TestHelper.assertError(error, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void innerDoubleError() {
        List<Throwable> error = TestHelper.trackPluginErrors();
        try {
            Flowable.just(1)
            .compose(FlowableTransformers.switchFlatMap(Functions.justFunction(new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onError(new IOException());
                    s.onError(new IllegalArgumentException());
                }
            }), 2))
            .test()
            .assertFailure(IOException.class);

            TestHelper.assertError(error, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void mapperThrows() {
        BehaviorProcessor<Integer> bp = BehaviorProcessor.createDefault(1);

        bp
        .compose(FlowableTransformers.switchFlatMap(new Function<Object, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Object v) throws Exception {
                throw new IOException();
            }
        }, 2))
        .test()
        .assertFailure(IOException.class);

        assertFalse(bp.hasSubscribers());
    }
}
