/*
 * Copyright 2016-2017 David Karnok
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

public class FlowableTimeoutLastTest {

    @Test
    public void normalNoTimeout() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.timeoutLast(5, TimeUnit.SECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(10);
    }

    @Test
    public void normalNoTimeoutScheduler() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.timeoutLast(5, TimeUnit.SECONDS, Schedulers.single()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(10);
    }

    @Test
    public void absoluteNoTimeout() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.timeoutLastAbsolute(5, TimeUnit.SECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(10);
    }

    @Test
    public void absoluteNoTimeoutScheduler() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.timeoutLastAbsolute(5, TimeUnit.SECONDS, Schedulers.single()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(10);
    }

    @Test
    public void normalEmpty() {
        Flowable.<Integer>empty()
        .compose(FlowableTransformers.timeoutLast(5, TimeUnit.SECONDS, Schedulers.single()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void normalError() {
        Flowable.<Integer>error(new IOException())
        .compose(FlowableTransformers.timeoutLast(5, TimeUnit.SECONDS, Schedulers.single()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void absoluteEmpty() {
        Flowable.<Integer>empty()
        .compose(FlowableTransformers.timeoutLastAbsolute(5, TimeUnit.SECONDS, Schedulers.single()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void absoluteError() {
        Flowable.<Integer>error(new IOException())
        .compose(FlowableTransformers.timeoutLastAbsolute(5, TimeUnit.SECONDS, Schedulers.single()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void timeout() {
        Flowable.just(0, 50, 100, 400)
        .flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(final Integer v) throws Exception {
                return Flowable.timer(v, TimeUnit.MILLISECONDS).map(new Function<Long, Integer>() {
                    @Override
                    public Integer apply(Long w) throws Exception {
                        return v;
                    }
                });
            }
        })
        .compose(FlowableTransformers.timeoutLast(200, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(500, TimeUnit.SECONDS)
        .assertResult(100);
    }

    @Test
    public void timeoutAbsolute() {
        Flowable.just(0, 50, 100, 150, 400)
        .flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(final Integer v) throws Exception {
                return Flowable.timer(v, TimeUnit.MILLISECONDS).map(new Function<Long, Integer>() {
                    @Override
                    public Integer apply(Long w) throws Exception {
                        return v;
                    }
                });
            }
        })
        .compose(FlowableTransformers.timeoutLastAbsolute(200, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(150);
    }

    @Test
    public void cancel() {
        Flowable.timer(500, TimeUnit.MILLISECONDS)
        .compose(FlowableTransformers.timeoutLast(200, TimeUnit.MILLISECONDS))
        .takeUntil(Flowable.timer(100, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void cancelAbsolute() {
        Flowable.timer(500, TimeUnit.MILLISECONDS)
        .compose(FlowableTransformers.timeoutLastAbsolute(200, TimeUnit.MILLISECONDS))
        .takeUntil(Flowable.timer(100, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }
}
