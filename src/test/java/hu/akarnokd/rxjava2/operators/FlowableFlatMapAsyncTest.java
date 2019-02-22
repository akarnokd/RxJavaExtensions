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

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class FlowableFlatMapAsyncTest {

    @Test(timeout = 10000)
    public void normal() {
        for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
            Flowable.range(1, 1000)
            .compose(FlowableTransformers.flatMapAsync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n);
                }
            }, Schedulers.single()))
            .test()
            .awaitDone(9, TimeUnit.SECONDS)
            .assertValueCount(n * 1000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test(timeout = 10000)
    public void normalHidden() {
        for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
            Flowable.range(1, 1000)
            .compose(FlowableTransformers.flatMapAsync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n).hide();
                }
            }, Schedulers.single()))
            .test()
            .awaitDone(9, TimeUnit.SECONDS)
            .assertValueCount(n * 1000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test(timeout = 10000)
    public void normalAsync() {
        for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
            Flowable.range(1, 1000)
            .compose(FlowableTransformers.flatMapAsync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n).observeOn(Schedulers.computation());
                }
            }, Schedulers.single()))
            .test()
            .awaitDone(9, TimeUnit.SECONDS)
            .assertValueCount(n * 1000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test(timeout = 10000)
    public void normalAsyncHidden() {
        for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
            Flowable.range(1, 1000)
            .compose(FlowableTransformers.flatMapAsync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n).observeOn(Schedulers.computation()).hide();
                }
            }, Schedulers.single()))
            .test()
            .awaitDone(9, TimeUnit.SECONDS)
            .assertValueCount(n * 1000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test(timeout = 1000000)
    public void normalAsyncHidden2() {
        for (int m = 1; m < 1011; m = m < 16 ? m + 1 : m + 10) {
//            System.out.println(m);
            for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
//                System.out.println("  " + n);
                Flowable.range(1, m)
                .compose(FlowableTransformers.flatMapAsync(new Function<Integer, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Integer v) throws Exception {
                        return Flowable.range(v * 1000, n).observeOn(Schedulers.computation()).hide();
                    }
                }, Schedulers.single()))
                .test()
                .awaitDone(9, TimeUnit.SECONDS)
                .assertValueCount(n * m)
                .assertNoErrors()
                .assertComplete();
            }
        }
    }

    @Test(timeout = 10000)
    public void breadth() {
        for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
            Flowable.range(1, 1000)
            .compose(FlowableTransformers.flatMapAsync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n);
                }
            }, Schedulers.single()))
            .test()
            .awaitDone(9, TimeUnit.SECONDS)
            .assertValueCount(n * 1000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test(timeout = 10000)
    public void breadthHidden() {
        for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
            Flowable.range(1, 1000)
            .compose(FlowableTransformers.flatMapAsync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n).hide();
                }
            }, Schedulers.single(), false))
            .test()
            .awaitDone(9, TimeUnit.SECONDS)
            .assertValueCount(n * 1000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test(timeout = 10000)
    public void breadthAsync() {
        for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
            Flowable.range(1, 1000)
            .compose(FlowableTransformers.flatMapAsync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n).observeOn(Schedulers.computation());
                }
            }, Schedulers.single(), false))
            .test()
            .awaitDone(9, TimeUnit.SECONDS)
            .assertValueCount(n * 1000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test(timeout = 10000)
    public void breadthAsyncHidden() {
        for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
            Flowable.range(1, 1000)
            .compose(FlowableTransformers.flatMapAsync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n).observeOn(Schedulers.computation()).hide();
                }
            }, Schedulers.single(), false))
            .test()
            .awaitDone(9, TimeUnit.SECONDS)
            .assertValueCount(n * 1000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    //@Test//(timeout = 60000)
    public void breadthAsyncHidden3() {
        int m = 4;
        final int n = 10;
        for (int i = 0; i < 100000; i++) {
            Flowable.range(1, m)
            .compose(FlowableTransformers.flatMapAsync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n).observeOn(Schedulers.computation()).hide();
                }
            }, Schedulers.single(), false))
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertValueCount(n * m)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test(timeout = 60000)
    public void breadthAsyncHidden2() {
        for (int m = 1; m < 1011; m = m < 16 ? m + 1 : m + 10) {
//            System.out.println(m);
            for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
//                System.out.println("  " + n);
                Flowable.range(1, m)
                .compose(FlowableTransformers.flatMapAsync(new Function<Integer, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Integer v) throws Exception {
                        return Flowable.range(v * 1000, n).observeOn(Schedulers.computation()).hide();
                    }
                }, Schedulers.single(), false))
                .test()
                .awaitDone(9, TimeUnit.SECONDS)
                .assertValueCount(n * m)
                .assertNoErrors()
                .assertComplete();
            }
        }
    }
}
