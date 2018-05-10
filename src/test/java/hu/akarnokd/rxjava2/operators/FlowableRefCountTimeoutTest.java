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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.*;

import org.junit.*;
import org.reactivestreams.Subscription;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

@SuppressWarnings("deprecation")
public class FlowableRefCountTimeoutTest {

    @Test
    public void byCount() {
        final int[] subscriptions = { 0 };

        Flowable<Integer> source = Flowable.range(1, 5)
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                subscriptions[0]++;
            }
        })
        .publish()
        .compose(FlowableTransformers.<Integer>refCount(2));

        for (int i = 0; i < 3; i++) {
            TestSubscriber<Integer> ts1 = source.test();

            ts1.assertEmpty();

            TestSubscriber<Integer> ts2 = source.test();

            ts1.assertResult(1, 2, 3, 4, 5);
            ts2.assertResult(1, 2, 3, 4, 5);
        }

        Assert.assertEquals(3, subscriptions[0]);
    }

    @Test
    public void resubscribeBeforeTimeout() throws Exception {
        final int[] subscriptions = { 0 };

        PublishProcessor<Integer> pp = PublishProcessor.create();

        Flowable<Integer> source = pp
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                subscriptions[0]++;
            }
        })
        .publish()
        .compose(FlowableTransformers.<Integer>refCount(500, TimeUnit.MILLISECONDS));

        TestSubscriber<Integer> ts1 = source.test(0);

        Assert.assertEquals(1, subscriptions[0]);

        ts1.cancel();

        Thread.sleep(100);

        ts1 = source.test(0);

        Assert.assertEquals(1, subscriptions[0]);

        Thread.sleep(500);

        Assert.assertEquals(1, subscriptions[0]);

        pp.onNext(1);
        pp.onNext(2);
        pp.onNext(3);
        pp.onNext(4);
        pp.onNext(5);
        pp.onComplete();

        ts1.requestMore(5)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void letitTimeout() throws Exception {
        final int[] subscriptions = { 0 };

        PublishProcessor<Integer> pp = PublishProcessor.create();

        Flowable<Integer> source = pp
        .doOnSubscribe(new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                subscriptions[0]++;
            }
        })
        .publish()
        .compose(FlowableTransformers.<Integer>refCount(1, 100, TimeUnit.MILLISECONDS));

        TestSubscriber<Integer> ts1 = source.test(0);

        Assert.assertEquals(1, subscriptions[0]);

        ts1.cancel();

        Assert.assertTrue(pp.hasSubscribers());

        Thread.sleep(200);

        Assert.assertFalse(pp.hasSubscribers());
    }

    @Test
    public void error() {
        Flowable.<Integer>error(new IOException())
        .publish()
        .compose(FlowableTransformers.<Integer>refCount(500, TimeUnit.MILLISECONDS))
        .test()
        .assertFailure(IOException.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void badUpstream() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>refCount(500, TimeUnit.MILLISECONDS, Schedulers.single()))
        ;
    }

    @Test
    public void comeAndGo() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        Flowable<Integer> source = pp
        .publish()
        .compose(FlowableTransformers.<Integer>refCount(1));

        TestSubscriber<Integer> ts1 = source.test(0);

        Assert.assertTrue(pp.hasSubscribers());

        for (int i = 0; i < 3; i++) {
            TestSubscriber<Integer> ts2 = source.test();
            ts1.cancel();
            ts1 = ts2;
        }

        ts1.cancel();

        Assert.assertFalse(pp.hasSubscribers());
    }

    @Test
    public void unsubscribeSubscribeRace() {
        for (int i = 0; i < 1000; i++) {

            final Flowable<Integer> source = Flowable.range(1, 5)
                    .replay()
                    .compose(FlowableTransformers.<Integer>refCount(1))
                    ;

            final TestSubscriber<Integer> ts1 = source.test(0);

            final TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>(0);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    source.subscribe(ts2);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            ts2.requestMore(6) // FIXME RxJava replay() doesn't issue onComplete without reqest
            .withTag("Round: " + i)
            .assertResult(1, 2, 3, 4, 5);
        }
    }

    Flowable<Object> source;

    @Test
    public void replayNoLeak() throws Exception {
        System.gc();
        Thread.sleep(100);

        long start = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = Flowable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new byte[100 * 1000 * 1000];
            }
        })
        .replay(1)
        .compose(FlowableTransformers.<Object>refCount(1));

        source.subscribe();

        System.gc();
        Thread.sleep(100);

        long after = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = null;
        assertTrue(String.format("%,3d -> %,3d%n", start, after), start + 20 * 1000 * 1000 > after);
    }

    @Test
    public void replayNoLeak2() throws Exception {
        System.gc();
        Thread.sleep(100);

        long start = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = Flowable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new byte[100 * 1000 * 1000];
            }
        }).concatWith(Flowable.never())
        .replay(1)
        .compose(FlowableTransformers.<Object>refCount(1));

        Disposable s1 = source.subscribe();
        Disposable s2 = source.subscribe();

        s1.dispose();
        s2.dispose();

        s1 = null;
        s2 = null;

        System.gc();
        Thread.sleep(100);

        long after = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = null;
        assertTrue(String.format("%,3d -> %,3d%n", start, after), start + 20 * 1000 * 1000 > after);
    }
}
