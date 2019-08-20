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

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FlowableFlatMapSyncTest {

    @Test(timeout = 10000)
    public void normal() {
        for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
            Flowable.range(1, 1000)
            .compose(FlowableTransformers.flatMapSync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n);
                }
            }))
            .test()
            .assertValueCount(n * 1000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test(timeout = 10000)
    public void normalHidden() {
        for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
            Flowable.range(1, 1000)
            .compose(FlowableTransformers.flatMapSync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n).hide();
                }
            }))
            .test()
            .assertValueCount(n * 1000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test(timeout = 10000)
    public void normalAsync() {
        for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
            Flowable.range(1, 1000)
            .compose(FlowableTransformers.flatMapSync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n).observeOn(Schedulers.computation());
                }
            }))
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
            .compose(FlowableTransformers.flatMapSync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n).observeOn(Schedulers.computation()).hide();
                }
            }))
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
                .compose(FlowableTransformers.flatMapSync(new Function<Integer, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Integer v) throws Exception {
                        return Flowable.range(v * 1000, n).observeOn(Schedulers.computation()).hide();
                    }
                }))
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
            .compose(FlowableTransformers.flatMapSync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n);
                }
            }))
            .test()
            .assertValueCount(n * 1000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test(timeout = 10000)
    public void breadthHidden() {
        for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
            Flowable.range(1, 1000)
            .compose(FlowableTransformers.flatMapSync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n).hide();
                }
            }, false))
            .test()
            .assertValueCount(n * 1000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test(timeout = 10000)
    public void breadthAsync() {
        for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
            Flowable.range(1, 1000)
            .compose(FlowableTransformers.flatMapSync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n).observeOn(Schedulers.computation());
                }
            }, false))
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
            .compose(FlowableTransformers.flatMapSync(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return Flowable.range(v * 1000, n).observeOn(Schedulers.computation()).hide();
                }
            }, false))
            .test()
            .awaitDone(9, TimeUnit.SECONDS)
            .assertValueCount(n * 1000)
            .assertNoErrors()
            .assertComplete();
        }
    }

    @Test(timeout = 1000000)
    public void breadthAsyncHidden2() {
        for (int m = 1; m < 1011; m = m < 16 ? m + 1 : m + 10) {
//            System.out.println(m);
            for (final int n : new int[] { 0, 1, 2, 10, 100, 1000 }) {
//                System.out.println("  " + n);
                Flowable.range(1, m)
                .compose(FlowableTransformers.flatMapSync(new Function<Integer, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Integer v) throws Exception {
                        return Flowable.range(v * 1000, n).observeOn(Schedulers.computation()).hide();
                    }
                }, false))
                .test()
                .awaitDone(9, TimeUnit.SECONDS)
                .assertValueCount(n * m)
                .assertNoErrors()
                .assertComplete();
            }
        }
    }
}
