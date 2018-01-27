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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class FlowableDebounceFirstTest {

    Function<Integer, Publisher<Integer>> delayByItself() {
        return new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(final Integer v) throws Exception {
                return Flowable.timer(v, TimeUnit.MILLISECONDS)
                        .map(new Function<Long, Integer>() {
                    @Override
                    public Integer apply(Long w) throws Exception {
                        return v;
                    }
                });
            }
        };
    }

    @Test
    public void normal() {
        Flowable.just(0, 50, 100, 150, 400, 500, 550, 1000)
        .flatMap(delayByItself())
        .compose(FlowableTransformers.debounceFirst(200, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0, 400, 1000);
    }

    @Test
    public void normalScheduler() {
        Flowable.just(0, 50, 100, 150, 400, 500, 550, 1000)
        .flatMap(delayByItself())
        .compose(FlowableTransformers.debounceFirst(200, TimeUnit.MILLISECONDS, Schedulers.single()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0, 400, 1000);
    }

    @Test
    public void empty() {
        Flowable.<Integer>empty()
        .flatMap(delayByItself())
        .compose(FlowableTransformers.debounceFirst(200, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void error() {
        Flowable.<Integer>error(new IOException())
        .flatMap(delayByItself())
        .compose(FlowableTransformers.debounceFirst(200, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void take() {
        Flowable.just(0, 50, 100, 150, 400, 500, 550, 1000)
        .flatMap(delayByItself())
        .compose(FlowableTransformers.debounceFirst(200, TimeUnit.MILLISECONDS))
        .take(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0, 400);
    }

    @Test
    public void backpressure() {
        Flowable.just(0, 50, 100, 150, 400, 500, 550, 1000)
        .flatMap(delayByItself())
        .compose(FlowableTransformers.debounceFirst(200, TimeUnit.MILLISECONDS))
        .rebatchRequests(1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0, 400, 1000);
    }
}
