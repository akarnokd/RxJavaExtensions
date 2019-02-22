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

package hu.akarnokd.rxjava2.operators;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import org.junit.Test;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.Flowable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.internal.functions.Functions;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableCoalesceTest {

    Callable<List<Integer>> listSupplier = Functions.createArrayList(16);

    BiConsumer<List<Integer>, Integer> listAdd = new BiConsumer<List<Integer>, Integer>() {
        @Override
        public void accept(List<Integer> a, Integer b) throws Exception {
            a.add(b);
        }
    };

    BiConsumer<List<Integer>, Integer> listAddCrash = new BiConsumer<List<Integer>, Integer>() {
        @Override
        public void accept(List<Integer> a, Integer b) throws Exception {
            throw new IOException();
        }
    };

    BiConsumer<List<Integer>, Integer> listAddCrash100 = new BiConsumer<List<Integer>, Integer>() {
        @Override
        public void accept(List<Integer> a, Integer b) throws Exception {
            if (b == 100) {
                throw new IOException();
            }
            a.add(b);
        }
    };

    @Test
    @SuppressWarnings("unchecked")
    public void empty() {
        Flowable.<Integer>empty()
        .compose(FlowableTransformers.coalesce(listSupplier, listAdd))
        .test()
        .assertResult();
    }

    @Test
    public void never() {
        Flowable.<Integer>never()
        .compose(FlowableTransformers.coalesce(listSupplier, listAdd))
        .test()
        .assertEmpty()
        .cancel();;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void error() {
        Flowable.<Integer>error(new IOException())
        .compose(FlowableTransformers.coalesce(listSupplier, listAdd))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void coalescerCrash() {
        Flowable.just(1)
        .compose(FlowableTransformers.coalesce(listSupplier, listAddCrash))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void range() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.coalesce(listSupplier, listAdd))
        .test()
        .assertResult(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3), Arrays.asList(4), Arrays.asList(5));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void take() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.coalesce(listSupplier, listAdd))
        .take(2)
        .test()
        .assertResult(Arrays.asList(1), Arrays.asList(2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void rangeBackpressured() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.coalesce(listSupplier, listAdd))
        .test(0)
        .assertEmpty()
        .requestMore(1)
        .assertResult(Arrays.asList(1, 2, 3, 4, 5));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void rangeBackpressuredMixed() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.coalesce(listSupplier, listAdd))
        .test(1)
        .assertValue(Arrays.asList(1))
        .requestMore(1)
        .assertResult(Arrays.asList(1), Arrays.asList(2, 3, 4, 5));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void onNextRequestRace() {
        for (int i = 0; i < 1000; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<List<Integer>> ts =
                    pp.compose(FlowableTransformers.coalesce(listSupplier, listAdd))
                    .test(0);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            pp.onComplete();

            ts.assertResult(Arrays.asList(1));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void slowPathQueueUse() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>() {
            @Override
            public void onNext(List<Integer> t) {
                super.onNext(t);
                if (t.get(0) == 1) {
                    pp.onNext(100);
                }
            }
        };

        pp.compose(FlowableTransformers.coalesce(listSupplier, listAdd)).subscribe(ts);

        pp.onNext(1);
        pp.onComplete();

        ts.assertResult(Arrays.asList(1), Arrays.asList(100));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void slowPathQueueUseCrash() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>() {
            @Override
            public void onNext(List<Integer> t) {
                super.onNext(t);
                if (t.get(0) == 1) {
                    pp.onNext(100);
                }
            }
        };

        pp.compose(FlowableTransformers.coalesce(listSupplier, listAddCrash100)).subscribe(ts);

        pp.onNext(1);
        pp.onComplete();

        ts.assertFailure(IOException.class, Arrays.asList(1));
    }
}
