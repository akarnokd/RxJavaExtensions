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

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class FlowableIntervalBackpressureTest {

    @Test
    public void period() {
        Flowables.intervalBackpressure(1, TimeUnit.MILLISECONDS)
        .rebatchRequests(1)
        .take(10)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
    }

    @Test
    public void periodScheduler() {
        Flowables.intervalBackpressure(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .rebatchRequests(1)
        .take(10)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
    }

    @Test
    public void initial() {
        Flowables.intervalBackpressure(100, 1, TimeUnit.MILLISECONDS)
        .rebatchRequests(1)
        .take(10)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
    }

    @Test
    public void initialScheduler() {
        Flowables.intervalBackpressure(100, 1, TimeUnit.MILLISECONDS, Schedulers.single())
        .rebatchRequests(1)
        .take(10)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
    }

    @Test
    public void backpressure() {
        TestScheduler scheduler = new TestScheduler();

        TestSubscriber<Long> ts = Flowables.intervalBackpressure(1, TimeUnit.MILLISECONDS, scheduler)
        .test(0L);

        ts.assertEmpty();

        scheduler.advanceTimeBy(3, TimeUnit.MILLISECONDS);

        ts.assertEmpty();

        scheduler.advanceTimeBy(2, TimeUnit.MILLISECONDS);

        ts.request(2);

        ts.assertValues(0L, 1L);

        scheduler.advanceTimeBy(3, TimeUnit.MILLISECONDS);

        ts.assertValues(0L, 1L);

        ts.request(5);

        ts.assertValues(0L, 1L, 2L, 3L, 4L, 5L, 6L);

        ts.request(10);

        ts.assertValues(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L);

        ts.assertNoErrors()
        .assertNotComplete()
        ;
    }

    @Test
    public void syncCancel() {
        TestScheduler scheduler = new TestScheduler();

        TestSubscriber<Long> ts = new TestSubscriber<Long>() {
            @Override
            public void onNext(Long t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowables.intervalBackpressure(1, TimeUnit.MILLISECONDS, scheduler)
        .subscribe(ts);

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        ts.assertResult(0L);
    }
}
