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

package hu.akarnokd.rxjava2.async;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import org.junit.Test;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

public class AsyncFlowableTest {

    @Test
    public void startDefault() {

        final AtomicInteger counter = new AtomicInteger();

        Flowable<Integer> source = AsyncFlowable.start(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return counter.incrementAndGet();
            }
        });

        for (int i = 0; i < 5; i++) {
            source.test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1);

            assertEquals(1, counter.get());
        }
    }

    @Test
    public void startCustomScheduler() {
        final AtomicInteger counter = new AtomicInteger();

        Flowable<Integer> source = AsyncFlowable.start(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return counter.incrementAndGet();
            }
        }, Schedulers.single());

        for (int i = 0; i < 5; i++) {
            source.test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1);

            assertEquals(1, counter.get());
        }
    }
}
