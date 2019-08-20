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

package hu.akarnokd.rxjava3.subjects;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import hu.akarnokd.rxjava3.test.*;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DispatchWorkSubjectTest {

    @Test
    public void offline() {
        DispatchWorkSubject<Integer> dws = DispatchWorkSubject.create(Schedulers.trampoline());

        assertFalse(dws.hasComplete());
        assertFalse(dws.hasThrowable());
        assertNull(dws.getThrowable());
        assertFalse(dws.hasObservers());

        dws.onNext(1);
        dws.onNext(2);
        dws.onNext(3);
        dws.onNext(4);
        dws.onNext(5);
        dws.onComplete();

        assertTrue(dws.hasComplete());
        assertFalse(dws.hasThrowable());
        assertNull(dws.getThrowable());
        assertFalse(dws.hasObservers());

        dws.take(2).test().assertResult(1, 2);
        dws.take(2).test().assertResult(3, 4);
        dws.take(2).test().assertResult(5);
        dws.test().assertResult();

        assertFalse(dws.hasObservers());
    }

    @Test
    public void online() {
        DispatchWorkSubject<Integer> dws = DispatchWorkSubject.create(Schedulers.trampoline());

        TestObserver<Integer> to1 = dws.test();

        assertTrue(dws.hasObservers());

        to1.assertEmpty();

        dws.onNext(1);

        to1.assertValuesOnly(1);

        dws.onNext(2);

        to1.assertValuesOnly(1, 2);

        to1.dispose();

        assertFalse(dws.hasObservers());

        TestObserver<Integer> to2 = dws.test();

        assertTrue(dws.hasObservers());

        dws.onNext(3);

        to2.assertValuesOnly(3);

        dws.onNext(4);

        to2.assertValuesOnly(3, 4);

        dws.onNext(5);

        to2.assertValuesOnly(3, 4, 5);

        dws.onComplete();

        to2.assertResult(3, 4, 5);

        assertFalse(dws.hasObservers());
    }

    @Test
    public void disposedUpFront() {
        DispatchWorkSubject<Integer> dws = DispatchWorkSubject.create(Schedulers.trampoline());

        dws.test(true);

        assertFalse(dws.hasObservers());
    }

    @Test
    public void dispose() {
        DispatchWorkSubject<Integer> dws = DispatchWorkSubject.create(Schedulers.trampoline());

        assertFalse(dws.isDisposed());

        Disposable d = Disposables.empty();

        dws.onSubscribe(d);

        assertFalse(dws.isDisposed());
        assertFalse(d.isDisposed());

        dws.dispose();

        assertTrue(dws.isDisposed());
        assertTrue(d.isDisposed());
    }

    @Test
    public void errorDelayed() {
        DispatchWorkSubject<Integer> dws = DispatchWorkSubject.create(Schedulers.trampoline(), 32);

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
        DispatchWorkSubject<Integer> dws = DispatchWorkSubject.create(Schedulers.trampoline(), false);

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
        DispatchWorkSubject<Integer> dws = DispatchWorkSubject.create(Schedulers.trampoline(), true);

        TestObserver<Integer> to = dws.test();

        dws.onNext(1);
        dws.onNext(2);
        dws.onError(new TestException());

        to.assertFailure(TestException.class, 1, 2);

        dws.test().assertFailure(TestException.class);

        assertTrue(dws.hasThrowable());
        assertTrue(dws.getThrowable() instanceof TestException);
    }

    @Test
    public void addRemove() {
        DispatchWorkSubject<Integer> dws = DispatchWorkSubject.create(Schedulers.trampoline(), true);

        TestObserver<Integer> to = dws.test();

        dws.test();

        to.dispose();

        dws.test(true);
    }

    @Test
    public void noDelayErrorComplete() {
        DispatchWorkSubject<Integer> dws = DispatchWorkSubject.create(Schedulers.trampoline(), false);

        TestObserver<Integer> to = dws.test();

        dws.onComplete();

        to.assertResult();

        dws.test().assertResult();
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final DispatchWorkSubject<Integer> dws = DispatchWorkSubject.create(Schedulers.trampoline(), true);

            final TestObserver<Integer> to = dws.test();

            final TestObserver<Integer> to2 = new TestObserver<Integer>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    dws.subscribe(to2);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);

            assertTrue(dws.hasObservers());
        }
    }

    @Test
    public void doubleConsume() {
        final DispatchWorkSubject<Integer> dws = DispatchWorkSubject.create(Schedulers.computation(), true);

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
}
