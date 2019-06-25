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

package hu.akarnokd.rxjava3.processors;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import hu.akarnokd.rxjava3.test.*;
import io.reactivex.*;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class DispatchWorkProcessorTest {

    @Test
    public void offline() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.trampoline());

        assertFalse(dws.hasComplete());
        assertFalse(dws.hasThrowable());
        assertNull(dws.getThrowable());
        assertFalse(dws.hasSubscribers());

        dws.onNext(1);
        dws.onNext(2);
        dws.onNext(3);
        dws.onNext(4);
        dws.onNext(5);
        dws.onComplete();

        assertTrue(dws.hasComplete());
        assertFalse(dws.hasThrowable());
        assertNull(dws.getThrowable());
        assertFalse(dws.hasSubscribers());

        dws.take(2).test().assertResult(1, 2);
        dws.take(2).test().assertResult(3, 4);
        dws.take(2).test().assertResult(5);
        dws.test().assertResult();

        assertFalse(dws.hasSubscribers());
    }

    @Test
    public void online() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.trampoline());

        TestSubscriber<Integer> ts1 = dws.test();

        assertTrue(dws.hasSubscribers());

        ts1.assertEmpty();

        dws.onNext(1);

        ts1.assertValuesOnly(1);

        dws.onNext(2);

        ts1.assertValuesOnly(1, 2);

        ts1.cancel();

        assertFalse(dws.hasSubscribers());

        TestSubscriber<Integer> ts2 = dws.test();

        assertTrue(dws.hasSubscribers());

        dws.onNext(3);

        ts2.assertValuesOnly(3);

        dws.onNext(4);

        ts2.assertValuesOnly(3, 4);

        dws.onNext(5);

        ts2.assertValuesOnly(3, 4, 5);

        dws.onComplete();

        ts2.assertResult(3, 4, 5);

        assertFalse(dws.hasSubscribers());
    }

    @Test
    public void disposedUpFront() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.trampoline());

        dws.test(1L, true);

        assertFalse(dws.hasSubscribers());
    }

    @Test
    public void dispose() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.trampoline());

        assertFalse(dws.isDisposed());

        BooleanSubscription bs = new BooleanSubscription();

        dws.onSubscribe(bs);

        assertFalse(dws.isDisposed());
        assertFalse(bs.isCancelled());

        dws.dispose();

        assertTrue(dws.isDisposed());
        assertTrue(bs.isCancelled());
    }

    @Test
    public void errorDelayed() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.trampoline(), 32);

        dws.onNext(1);
        dws.onNext(2);
        dws.onError(new TestException());

        dws.test().assertFailure(TestException.class, 1, 2);

        dws.test().assertFailure(TestException.class);

        assertTrue(dws.hasThrowable());
        assertTrue(dws.getThrowable() instanceof TestException);

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            dws.onNext(3);
            dws.onError(new IOException());
            dws.onComplete();

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void errorEager() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.trampoline(), false);

        dws.onNext(1);
        dws.onNext(2);
        dws.onError(new TestException());

        dws.test().assertFailure(TestException.class);

        dws.test().assertFailure(TestException.class);

        assertTrue(dws.hasThrowable());
        assertTrue(dws.getThrowable() instanceof TestException);
    }

    @Test
    public void errorLive() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.trampoline(), true);

        TestSubscriber<Integer> ts = dws.test();

        dws.onNext(1);
        dws.onNext(2);
        dws.onError(new TestException());

        ts.assertFailure(TestException.class, 1, 2);

        dws.test().assertFailure(TestException.class);

        assertTrue(dws.hasThrowable());
        assertTrue(dws.getThrowable() instanceof TestException);
    }

    @Test
    public void addRemove() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.trampoline(), true);

        TestSubscriber<Integer> ts = dws.test();

        dws.test();

        ts.cancel();

        dws.test(1L, true);
    }

    @Test
    public void noDelayErrorComplete() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.trampoline(), false);

        TestSubscriber<Integer> ts = dws.test();

        dws.onComplete();

        ts.assertResult();

        dws.test().assertResult();
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.trampoline(), true);

            final TestSubscriber<Integer> ts = dws.test();

            final TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    dws.subscribe(ts2);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2);

            assertTrue(dws.hasSubscribers());
        }
    }

    @Test
    public void doubleConsume() {
        final DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.computation(), true);

        Single<List<Integer>> o = dws.toList();

        TestObserver<HashSet<Integer>> to = Single.zip(o, o, new BiFunction<List<Integer>, List<Integer>, HashSet<Integer>>() {
            @Override
            public HashSet<Integer> apply(List<Integer> a, List<Integer> b)
                    throws Exception {
                HashSet<Integer> set = new HashSet<Integer>();
                set.addAll(a);
                set.addAll(b);
                return set;
            }
        })
                .test();

        int n = 1000000;

        for (int i = 0; i < n; i++) {
            dws.onNext(i);
        }
        dws.onComplete();

        to.awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        HashSet<Integer> set = to.values().get(0);

        assertEquals(n, set.size());

        for (int i = 0; i < n; i++) {
            assertTrue(set.remove(i));
        }

        assertTrue(set.isEmpty());
    }

    @Test
    public void unbounded() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.createUnbounded(Schedulers.trampoline());

        assertFalse(dws.hasComplete());
        assertFalse(dws.hasThrowable());
        assertNull(dws.getThrowable());
        assertFalse(dws.hasSubscribers());

        dws.onNext(1);
        dws.onNext(2);
        dws.onNext(3);
        dws.onNext(4);
        dws.onNext(5);
        dws.onComplete();

        assertTrue(dws.hasComplete());
        assertFalse(dws.hasThrowable());
        assertNull(dws.getThrowable());
        assertFalse(dws.hasSubscribers());

        dws.take(2).test().assertResult(1, 2);
        dws.take(2).test().assertResult(3, 4);
        dws.take(2).test().assertResult(5);
        dws.test().assertResult();

        assertFalse(dws.hasSubscribers());
    }

    @Test
    public void unboundedCapacityHint() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.createUnbounded(Schedulers.trampoline(), 2, false);

        assertFalse(dws.hasComplete());
        assertFalse(dws.hasThrowable());
        assertNull(dws.getThrowable());
        assertFalse(dws.hasSubscribers());

        dws.onNext(1);
        dws.onNext(2);
        dws.onNext(3);
        dws.onNext(4);
        dws.onNext(5);
        dws.onComplete();

        assertTrue(dws.hasComplete());
        assertFalse(dws.hasThrowable());
        assertNull(dws.getThrowable());
        assertFalse(dws.hasSubscribers());

        dws.take(2).test().assertResult(1, 2);
        dws.take(2).test().assertResult(3, 4);
        dws.take(2).test().assertResult(5);
        dws.test().assertResult();

        assertFalse(dws.hasSubscribers());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.doubleOnSubscribe(DispatchWorkProcessor.create(Schedulers.trampoline()));
    }

    @Test
    public void longBackpressured() {
        final DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.computation(), true);

        Single<List<Integer>> o = dws.toList();

        TestObserver<HashSet<Integer>> to = Single.zip(o, o, new BiFunction<List<Integer>, List<Integer>, HashSet<Integer>>() {
            @Override
            public HashSet<Integer> apply(List<Integer> a, List<Integer> b)
                    throws Exception {
                HashSet<Integer> set = new HashSet<Integer>();
                set.addAll(a);
                set.addAll(b);
                return set;
            }
        })
        .test();

        int n = 1000000;

        Flowable.range(0, n).subscribe(dws);

        to.awaitDone(30, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        HashSet<Integer> set = to.values().get(0);

        assertEquals(n, set.size());

        for (int i = 0; i < n; i++) {
            assertTrue("" + i, set.remove(i));
        }

        assertTrue(set.isEmpty());
    }

    @Test
    public void longBackpressured2() {
        final DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.computation(), true);

        Single<List<Integer>> o = dws.toList();

        TestObserver<HashSet<Integer>> to = Single.zip(o, o, new BiFunction<List<Integer>, List<Integer>, HashSet<Integer>>() {
            @Override
            public HashSet<Integer> apply(List<Integer> a, List<Integer> b)
                    throws Exception {
                HashSet<Integer> set = new HashSet<Integer>();
                set.addAll(a);
                set.addAll(b);
                return set;
            }
        })
        .test();

        int n = 1000000;

        Flowable.range(0, n).subscribeOn(Schedulers.single()).subscribe(dws);

        to.awaitDone(30, TimeUnit.SECONDS)
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        HashSet<Integer> set = to.values().get(0);

        assertEquals(n, set.size());

        for (int i = 0; i < n; i++) {
            assertTrue("" + i, set.remove(i));
        }

        assertTrue(set.isEmpty());
    }

    @Test
    public void longBackpressured3() {
        final DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.computation(), true);

        final AtomicInteger counter = new AtomicInteger();

        Single<List<Integer>> o = dws
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer v) throws Exception {
                        counter.incrementAndGet();
                    }
                })
                .toList();

        TestObserver<HashSet<Integer>> to = Single.zip(o, o, new BiFunction<List<Integer>, List<Integer>, HashSet<Integer>>() {
            @Override
            public HashSet<Integer> apply(List<Integer> a, List<Integer> b)
                    throws Exception {
                HashSet<Integer> set = new HashSet<Integer>();
                set.addAll(a);
                set.addAll(b);
                return set;
            }
        })
        .test();

        int n = 1000000;

        Flowable.range(0, n).subscribeOn(Schedulers.single()).subscribe(dws);

        to.awaitDone(30, TimeUnit.SECONDS)
        .withTag("Received: " + counter.get())
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();

        HashSet<Integer> set = to.values().get(0);

        assertEquals(n, set.size());

        for (int i = 0; i < n; i++) {
            assertTrue("" + i, set.remove(i));
        }

        assertTrue(set.isEmpty());
    }

    @Test
    public void emptyBackpressured() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.createUnbounded(Schedulers.trampoline(), 2, false);

        dws.onComplete();

        dws.test(0L).assertResult();

        assertFalse(dws.hasSubscribers());
    }

    @Test
    public void limit() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.createUnbounded(Schedulers.trampoline(), 2, false);

        dws.onNext(1);
        dws.onNext(2);
        dws.onComplete();

        dws.limit(1).test().assertResult(1);

        assertFalse(dws.hasSubscribers());
    }

    @Test
    public void errorBackpressured() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.createUnbounded(Schedulers.trampoline(), 2, true);

        dws.onError(new TestException());

        dws.test(0L).assertFailure(TestException.class);

        assertFalse(dws.hasSubscribers());
    }

    @Test
    public void errorBackpressuredNoDelayError() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.createUnbounded(Schedulers.trampoline(), 2, false);

        dws.onError(new TestException());

        dws.test(0L).assertFailure(TestException.class);

        assertFalse(dws.hasSubscribers());
    }

    @Test
    public void initialRequest0() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.createUnbounded(Schedulers.trampoline(), 2, false);

        dws.onNext(1);
        dws.onNext(2);
        dws.onComplete();

        dws.test(0L).assertEmpty();

        assertFalse(dws.hasSubscribers());
    }

    @Test
    public void initialRequest0NotComplete() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.createUnbounded(Schedulers.trampoline(), 2, false);

        dws.onNext(1);
        dws.onNext(2);

        dws.test(0L).assertEmpty();
    }

    @Test
    public void drainMoreThanLimit() {
        DispatchWorkProcessor<Integer> dws = DispatchWorkProcessor.create(Schedulers.trampoline(), 2, false);

        dws.onSubscribe(new BooleanSubscription());

        dws.requestMore(6);
    }
}
