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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FlowableCacheLastTest implements Consumer<Object> {

    volatile int calls;

    @Override
    public void accept(Object t) throws Exception {
        calls++;
    }

    @Test
    public void normal() {
        Flowable<Integer> f = Flowable.range(1, 5)
        .doOnSubscribe(this)
        .compose(FlowableTransformers.<Integer>cacheLast())
        ;

        assertEquals(0, calls);

        f.test().assertResult(5);

        assertEquals(1, calls);

        f.test().assertResult(5);

        assertEquals(1, calls);
    }

    @Test
    public void empty() {
        Flowable<Integer> f = Flowable.<Integer>empty()
        .doOnSubscribe(this)
        .compose(FlowableTransformers.<Integer>cacheLast())
        ;

        assertEquals(0, calls);

        f.test().assertResult();

        assertEquals(1, calls);

        f.test().assertResult();

        assertEquals(1, calls);
    }

    @Test
    public void error() {
        Flowable<Integer> f = Flowable.<Integer>error(new IOException())
        .doOnSubscribe(this)
        .compose(FlowableTransformers.<Integer>cacheLast())
        ;

        assertEquals(0, calls);

        f.test().assertFailure(IOException.class);

        assertEquals(1, calls);

        f.test().assertFailure(IOException.class);

        assertEquals(1, calls);
    }

    @Test
    public void subscriptionRace() {
        for (int i = 0; i < 1000; i++) {
            calls = 0;

            final Flowable<Integer> f = Flowable.just(1)
                    .doOnSubscribe(this)
                    .compose(FlowableTransformers.<Integer>cacheLast())
                    ;

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    f.test().awaitDone(5, TimeUnit.SECONDS).assertResult(1);
                }
            };

            TestHelper.race(r, r, Schedulers.single());

            assertEquals(1, calls);
        }
    }
}
