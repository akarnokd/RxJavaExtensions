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

package hu.akarnokd.rxjava3.operators;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.reactivestreams.*;

import hu.akarnokd.rxjava3.test.*;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.ProtocolViolationException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class FlowableZipLatestTest {

    Function<Object[], String> toString = new Function<Object[], String>() {
        @Override
        public String apply(Object[] a) throws Exception {
            return Arrays.toString(a);
        }
    };

    BiFunction<Object, Object, String> toString2 = new BiFunction<Object, Object, String>() {
        @Override
        public String apply(Object a, Object b) throws Exception {
            return "[" + a + ", " + b + "]";
        }
    };

    Function3<Object, Object, Object, String> toString3 = new Function3<Object, Object, Object, String>() {
        @Override
        public String apply(Object a, Object b, Object c) throws Exception {
            return "[" + a + ", " + b + ", " + c + "]";
        }
    };

    Function4<Object, Object, Object, Object, String> toString4 = new Function4<Object, Object, Object, Object, String>() {
        @Override
        public String apply(Object a, Object b, Object c, Object d) throws Exception {
            return "[" + a + ", " + b + ", " + c + ", " + d + "]";
        }
    };

    @Test
    public void zipLatest2a() {
        Flowables.zipLatest(Flowable.just(1), Flowable.just(2), toString2)
        .test()
        .assertResult("[1, 2]");
    }

    @Test
    public void zipLatest2b() {
        Flowables.zipLatest(Flowable.just(1), Flowable.just(2, 3), toString2)
        .test()
        .assertResult("[1, 2]");
    }

    @Test
    public void zipLatest2c() {
        Flowables.zipLatest(Flowable.just(1, 2), Flowable.just(3), toString2)
        .test()
        .assertResult("[2, 3]");
    }

    @Test
    public void zipLatest3a() {
        Flowables.zipLatest(Flowable.just(1), Flowable.just(2), Flowable.just(3), toString3)
        .test()
        .assertResult("[1, 2, 3]");
    }

    @Test
    public void zipLatest3b() {
        Flowables.zipLatest(Flowable.just(1), Flowable.just(2, 3), Flowable.just(4, 5), toString3)
        .test()
        .assertResult("[1, 3, 4]");
    }

    @Test
    public void zipLatest3c() {
        Flowables.zipLatest(Flowable.just(1, 2), Flowable.just(3), Flowable.just(4, 5, 6), toString3)
        .test()
        .assertResult("[2, 3, 4]");
    }

    @Test
    public void zipLatest4a() {
        Flowables.zipLatest(Flowable.just(1), Flowable.just(2), Flowable.just(3), Flowable.just(4), toString4)
        .test()
        .assertResult("[1, 2, 3, 4]");
    }

    @Test
    public void zipLatest4b() {
        Flowables.zipLatest(Flowable.just(1), Flowable.just(2, 3), Flowable.just(4, 5), Flowable.just(6, 7, 8), toString4)
        .test()
        .assertResult("[1, 3, 5, 6]");
    }

    @Test
    public void zipLatest4c() {
        Flowables.zipLatest(Flowable.just(1, 2), Flowable.just(3), Flowable.just(4, 5, 6), Flowable.just(7, 8), toString4)
        .test()
        .assertResult("[2, 3, 6, 7]");
    }

    @Test
    public void zipLatestArrayEmpty() {
        Flowables.zipLatest(toString)
        .test()
        .assertResult();
    }

    @Test
    public void zipLatestArrayEmptyScheduler() {
        Flowables.zipLatest(toString, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void zipLatestIterableEmpty() {
        Flowables.zipLatest(Collections.<Publisher<Object>>emptyList(), toString)
        .test()
        .assertResult();
    }

    @Test
    public void zipLatestIterableEmptyScheduler() {
        Flowables.zipLatest(Collections.<Publisher<Object>>emptyList(), toString, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void zipLatestArray() {
        TestScheduler scheduler = new TestScheduler();

        TestSubscriber<String> ts = Flowables.zipLatest(toString,
                Flowable.intervalRange(1, 6, 99, 100, TimeUnit.MILLISECONDS, scheduler),
                Flowable.intervalRange(4, 3, 200, 200, TimeUnit.MILLISECONDS, scheduler)
        )
        .test();

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        ts.assertValue("[2, 4]");

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        ts.assertValues("[2, 4]", "[4, 5]");

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        ts.assertResult("[2, 4]", "[4, 5]", "[6, 6]");
    }

    @Test
    public void zipLatestIterable() {
        TestScheduler scheduler = new TestScheduler();

        TestSubscriber<String> ts = Flowables.zipLatest(Arrays.asList(
                Flowable.intervalRange(1, 6, 99, 100, TimeUnit.MILLISECONDS, scheduler),
                Flowable.intervalRange(4, 3, 200, 200, TimeUnit.MILLISECONDS, scheduler)
                ), toString
        )
        .test();

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        ts.assertValue("[2, 4]");

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        ts.assertValues("[2, 4]", "[4, 5]");

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        ts.assertResult("[2, 4]", "[4, 5]", "[6, 6]");
    }

    @Test
    public void zipLatestIterableMany() {
        int n = 20;
        List<Flowable<Integer>> sources = new ArrayList<>();
        List<Integer> results = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            sources.add(Flowable.just(i));
            results.add(i);
        }

        Flowables.zipLatest(sources, toString)
        .test()
        .assertResult(results.toString());
    }

    @Test
    public void zipLatestTake2() {
        TestScheduler scheduler = new TestScheduler();

        TestSubscriber<String> ts = Flowables.zipLatest(toString,
                Flowable.intervalRange(1, 6, 99, 100, TimeUnit.MILLISECONDS, scheduler),
                Flowable.intervalRange(4, 3, 200, 200, TimeUnit.MILLISECONDS, scheduler)
        )
        .take(2)
        .test();

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        ts.assertValue("[2, 4]");

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        ts.assertResult("[2, 4]", "[4, 5]");

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        ts.assertResult("[2, 4]", "[4, 5]");
    }

    @Test
    public void firstErrors() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<String> ts = Flowables.zipLatest(pp1, pp2, toString2)
        .test();

        ts.assertEmpty();

        pp1.onNext(1);

        ts.assertEmpty();

        pp2.onNext(2);

        ts.assertValue("[1, 2]");

        pp1.onError(new IOException());

        Assert.assertFalse(pp2.hasSubscribers());

        ts.assertFailure(IOException.class, "[1, 2]");
    }

    @Test
    public void secondErrors() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<String> ts = Flowables.zipLatest(pp1, pp2, toString2)
        .test();

        ts.assertEmpty();

        pp1.onNext(1);

        ts.assertEmpty();

        pp2.onNext(2);

        ts.assertValue("[1, 2]");

        pp2.onError(new IOException());

        Assert.assertFalse(pp1.hasSubscribers());

        ts.assertFailure(IOException.class, "[1, 2]");
    }

    @Test
    public void firstErrorsBackpressured() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<String> ts = Flowables.zipLatest(pp1, pp2, toString2)
        .test(1);

        ts.assertEmpty();

        pp1.onNext(1);

        ts.assertEmpty();

        pp2.onNext(2);

        ts.assertValue("[1, 2]");

        pp1.onError(new IOException());

        Assert.assertFalse(pp2.hasSubscribers());

        ts.assertFailure(IOException.class, "[1, 2]");
    }

    @Test
    public void firstCompletesBackpressured() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<String> ts = Flowables.zipLatest(pp1, pp2, toString2)
        .test(1);

        ts.assertEmpty();

        pp1.onNext(1);

        ts.assertEmpty();

        pp2.onNext(2);

        ts.assertValue("[1, 2]");

        pp1.onComplete();

        Assert.assertFalse(pp2.hasSubscribers());

        ts.assertResult("[1, 2]");
    }

    @Test
    public void firstEmpty() {
        Flowables.zipLatest(Flowable.empty(), Flowable.just(1), toString2)
        .test()
        .assertResult();
    }

    @Test
    public void cancel() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<String> ts = Flowables.zipLatest(pp1, pp2, toString2)
        .test();

        ts.assertEmpty();

        pp1.onNext(1);

        ts.assertEmpty();

        pp2.onNext(2);

        ts.assertValue("[1, 2]");

        ts.cancel();

        Assert.assertFalse(pp1.hasSubscribers());
        Assert.assertFalse(pp2.hasSubscribers());

        ts.assertValue("[1, 2]")
        .assertNotComplete()
        .assertNoErrors();
    }

    @Test
    public void cancelBackpressure() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<String> ts = Flowables.zipLatest(pp1, pp2, toString2)
        .test(1);

        ts.assertEmpty();

        pp1.onNext(1);

        ts.assertEmpty();

        pp2.onNext(2);

        ts.assertValue("[1, 2]");

        ts.cancel();

        Assert.assertFalse(pp1.hasSubscribers());
        Assert.assertFalse(pp2.hasSubscribers());

        ts.assertValue("[1, 2]")
        .assertNotComplete()
        .assertNoErrors();
    }

    @Test
    public void cancelAfterOneBackpressured() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<String> ts = new TestSubscriber<String>(1) {
            @Override
            public void onNext(String t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowables.zipLatest(pp1, pp2, toString2).subscribe(ts);
        ts.assertEmpty();

        pp1.onNext(1);

        ts.assertEmpty();

        pp2.onNext(2);

        ts.assertResult("[1, 2]");
    }

    @Test
    public void firstCompleteBackpressure() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<String> ts = Flowables.zipLatest(pp1, pp2, toString2)
        .test(1);

        ts.assertEmpty();

        pp1.onNext(1);
        pp1.onComplete();

        Assert.assertTrue(pp2.hasSubscribers());

        ts.assertEmpty();

        pp2.onNext(2);

        ts.assertResult("[1, 2]");

        Assert.assertFalse(pp2.hasSubscribers());
    }

    @Test
    public void firstCompleteBackpressure2() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<String> ts = Flowables.zipLatest(pp1, pp2, toString2)
        .test(0);

        ts.assertEmpty();

        pp1.onNext(1);
        pp1.onComplete();

        Assert.assertTrue(pp2.hasSubscribers());

        ts.assertEmpty();

        pp2.onNext(2);

        ts.assertEmpty();

        ts.request(1);

        ts.assertResult("[1, 2]");

        Assert.assertFalse(pp2.hasSubscribers());
    }

    @Test
    public void combinerCrash() {
        Flowables.zipLatest(Flowable.just(1), Flowable.just(2), new BiFunction<Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b) throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void emissionRace() {
        for (int i = 0; i < 1000; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            TestSubscriber<String> ts = Flowables.zipLatest(pp1, pp2, toString2)
            .test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 50; j++) {
                        pp1.onNext(j);
                    }
                    pp1.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 50; j++) {
                        pp2.onNext(j);
                    }
                    pp2.onComplete();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            ts.assertNoErrors().assertComplete();
        }
    }

    @Test
    public void terminationRace() {
        for (int i = 0; i < 1000; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishProcessor<Integer> pp1 = PublishProcessor.create();
                final PublishProcessor<Integer> pp2 = PublishProcessor.create();

                TestSubscriberEx<String> ts = new TestSubscriberEx<>();

                Flowables.zipLatest(pp1, pp2, toString2).subscribe(ts);

                final Throwable ex = new IOException();

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

                if (ts.errors().size() != 0) {
                    ts.assertFailure(IOException.class);
                    Assert.assertTrue(errors.toString(), errors.isEmpty());
                } else {
                    ts.assertResult();
                    if (!errors.isEmpty()) {
                        TestHelper.assertUndeliverable(errors, 0, IOException.class);
                    }
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void badRequest() {
        final PublishProcessor<Integer> pp1 = PublishProcessor.create();
        final PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestHelper.assertBadRequestReported(Flowables.zipLatest(pp1, pp2, toString2));
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final Flowable<Integer> pp2 = new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    BooleanSubscription bs1 = new BooleanSubscription();
                    s.onSubscribe(bs1);

                    BooleanSubscription bs2 = new BooleanSubscription();
                    s.onSubscribe(bs2);

                    Assert.assertFalse(bs1.isCancelled());
                    Assert.assertTrue(bs2.isCancelled());
                }
            };

            Flowables.zipLatest(pp1, pp2, toString2).test();

            TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
