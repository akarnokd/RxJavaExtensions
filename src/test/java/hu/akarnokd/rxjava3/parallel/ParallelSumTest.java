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

package hu.akarnokd.rxjava3.parallel;

import java.io.IOException;

import org.junit.*;

import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ParallelSumTest {

    @Test
    public void normalInteger() {
        Flowable.range(1, 5)
        .parallel(1)
        .compose(ParallelTransformers.<Integer>sumInteger())
        .sequential()
        .test()
        .assertResult(15);
    }

    @Test
    public void normalLong() {
        Flowable.rangeLong(1, 5)
        .parallel(1)
        .compose(ParallelTransformers.<Long>sumLong())
        .sequential()
        .test()
        .assertResult(15L);
    }

    @Test
    public void normalDouble() {
        Flowable.range(1, 5)
        .map(new Function<Integer, Double>() {
            @Override
            public Double apply(Integer v) throws Exception {
                return v + 0.5d;
            }
        })
        .parallel(1)
        .compose(ParallelTransformers.<Double>sumDouble())
        .sequential()
        .test()
        .assertResult(17.5d);
    }

    @Test
    public void emptyInteger() {
        Flowable.<Integer>empty()
        .parallel(1)
        .compose(ParallelTransformers.<Integer>sumInteger())
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void errorInteger() {
        Flowable.<Integer>error(new IOException())
        .parallel(1)
        .compose(ParallelTransformers.<Integer>sumInteger())
        .sequential()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void emptyLong() {
        Flowable.<Long>empty()
        .parallel(1)
        .compose(ParallelTransformers.<Long>sumLong())
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void errorLong() {
        Flowable.<Long>error(new IOException())
        .parallel(1)
        .compose(ParallelTransformers.<Long>sumLong())
        .sequential()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void emptyDouble() {
        Flowable.<Double>empty()
        .parallel(1)
        .compose(ParallelTransformers.<Double>sumDouble())
        .sequential()
        .test()
        .assertResult();
    }

    @Test
    public void errorDouble() {
        Flowable.<Double>error(new IOException())
        .parallel(1)
        .compose(ParallelTransformers.<Double>sumDouble())
        .sequential()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void invalidRailsInteger() {
        TestHelper.checkInvalidParallelSubscribers(Flowable.range(1, 5)
                .parallel(1)
                .compose(ParallelTransformers.<Integer>sumInteger()));
    }

    @Test
    public void invalidRailsLong() {
        TestHelper.checkInvalidParallelSubscribers(Flowable.range(1, 5)
                .parallel(1)
                .compose(ParallelTransformers.<Integer>sumLong()));
    }

    @Test
    public void invalidRailsDouble() {
        TestHelper.checkInvalidParallelSubscribers(Flowable.range(1, 5)
                .parallel(1)
                .compose(ParallelTransformers.<Integer>sumDouble()));
    }

    @Test
    public void sumALot() {
        Long[] array = new Long[1000 * 1000];
        for (int i = 0; i < array.length; i++) {
            array[i] = (long)i;
        }

        long n = Flowable.fromArray(array)
        .parallel()
        .runOn(Schedulers.computation())
        .compose(ParallelTransformers.<Long>sumLong())
        .reduce(new BiFunction<Long, Long, Long>() {
            @Override
            public Long apply(Long a, Long b) throws Exception {
                return a + b;
            }
        })
        .blockingLast();

        Assert.assertEquals(999999L * 500000L, n);
    }
}
