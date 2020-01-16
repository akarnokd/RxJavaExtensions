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

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FlowableSwitchIfEmptyManyArrayTest {

    @SuppressWarnings("unchecked")
    @Test
    public void normalNonEmpty() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.switchIfEmptyArray(new Publisher[] { Flowable.range(10, 5) }))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void error() {
        Flowable.<Integer>error(new IOException())
        .compose(FlowableTransformers.<Integer>switchIfEmptyArray(new Publisher[] { Flowable.range(10, 5) }))
        .test()
        .assertFailure(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nullAlternative() {
        Flowable.<Integer>empty()
        .compose(FlowableTransformers.<Integer>switchIfEmptyArray(new Publisher[] { null }))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void normalNonEmptyBackpressured() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.switchIfEmptyArray(new Publisher[] { Flowable.range(10, 5) }))
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normalNonEmptyBackpressured2() {
        @SuppressWarnings("unchecked")
        FlowableTransformer<Integer, Integer> t = FlowableTransformers.<Integer>switchIfEmptyArray(new Publisher[] { Flowable.range(10, 5) });

        Flowable.range(1, 5)
        .compose(t)
        .test(0)
        .assertEmpty()
        .requestMore(1)
        .assertValue(1)
        .requestMore(2)
        .assertValues(1, 2, 3)
        .requestMore(2)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normalEmpty() {
        for (int i = 1; i < 10; i++) {
            @SuppressWarnings("unchecked")
            Publisher<Integer>[] alt = new Publisher[i];
            Arrays.fill(alt, Flowable.<Integer>empty());
            alt[i - 1] = Flowable.range(1, 5);

            Flowable.<Integer>empty()
            .compose(FlowableTransformers.<Integer>switchIfEmptyArray(alt))
            .test()
            .assertResult(1, 2, 3, 4, 5);
        }
    }

    @Test
    public void normalEmptyAsync() {
        for (int i = 1; i < 10; i++) {
            @SuppressWarnings("unchecked")
            Publisher<Integer>[] alt = new Publisher[i];
            Arrays.fill(alt, Flowable.<Integer>empty().observeOn(Schedulers.computation()));
            alt[i - 1] = Flowable.range(1, 5);

            Flowable.<Integer>empty()
            .compose(FlowableTransformers.<Integer>switchIfEmptyArray(alt))
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5);
        }
    }

    @Test
    public void normalBackpressured() {
        for (int i = 1; i < 10; i++) {
            @SuppressWarnings("unchecked")
            Publisher<Integer>[] alt = new Publisher[i];
            Arrays.fill(alt, Flowable.<Integer>empty());
            alt[i - 1] = Flowable.range(1, 5);

            Flowable.<Integer>empty()
            .compose(FlowableTransformers.<Integer>switchIfEmptyArray(alt))
            .rebatchRequests(1)
            .test()
            .assertResult(1, 2, 3, 4, 5);
        }
    }

    @Test
    public void normalBackpressuredAsync() {
        for (int i = 1; i < 10; i++) {
            @SuppressWarnings("unchecked")
            Publisher<Integer>[] alt = new Publisher[i];
            Arrays.fill(alt, Flowable.<Integer>empty().observeOn(Schedulers.computation()));
            alt[i - 1] = Flowable.range(1, 5);

            Flowable.<Integer>empty()
            .compose(FlowableTransformers.<Integer>switchIfEmptyArray(alt))
            .rebatchRequests(1)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5);
        }
    }

    @Test
    public void normalBackpressured2() {
        for (int i = 1; i < 10; i++) {
            @SuppressWarnings("unchecked")
            Publisher<Integer>[] alt = new Publisher[i];
            Arrays.fill(alt, Flowable.<Integer>empty());
            alt[i - 1] = Flowable.range(1, 5);

            Flowable.<Integer>empty()
            .compose(FlowableTransformers.<Integer>switchIfEmptyArray(alt))
            .test(0)
            .assertEmpty()
            .requestMore(1)
            .assertValue(1)
            .requestMore(2)
            .assertValues(1, 2, 3)
            .requestMore(2)
            .assertResult(1, 2, 3, 4, 5);
        }
    }

    @Test
    public void take() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.switchIfEmptyArray(Flowable.range(10, 5)))
        .take(3)
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void take2() {
        Flowable.<Integer>empty()
        .compose(FlowableTransformers.switchIfEmptyArray(Flowable.range(10, 5)))
        .take(3)
        .test()
        .assertResult(10, 11, 12);
    }

    @Test
    public void cancel() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>switchIfEmptyArray(Flowable.range(10, 5)))
        .test(Long.MAX_VALUE, true)
        .assertEmpty();
    }

    @Test
    public void emptyEmpty() {
        Flowable.<Integer>empty()
        .compose(FlowableTransformers.<Integer>switchIfEmptyArray())
        .test()
        .assertResult();
    }
}
