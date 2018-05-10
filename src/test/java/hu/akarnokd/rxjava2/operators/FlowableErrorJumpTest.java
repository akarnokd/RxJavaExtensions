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

import java.util.*;

import org.junit.Test;
import org.reactivestreams.Publisher;

import hu.akarnokd.rxjava2.test.*;
import io.reactivex.*;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableErrorJumpTest {

    @Test
    public void normalDirect() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer, Integer>errorJump(new FlowableTransformer<Integer, Integer>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> v) {
                return v;
            }
        }))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void errorDirect() {
        Flowable.<Integer>error(new TestException())
        .compose(FlowableTransformers.<Integer, Integer>errorJump(new FlowableTransformer<Integer, Integer>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> v) {
                return v;
            }
        }))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void oneSubscriber() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer, Integer>errorJump(new FlowableTransformer<Integer, Integer>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> v) {
                return v.mergeWith(v);
            }
        }))
        .test()
        .assertFailure(IllegalStateException.class, 1, 2, 3, 4, 5);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void errorJump() {
        Flowable.range(1, 5)
        .concatWith(Flowable.<Integer>error(new TestException()))
        .compose(FlowableTransformers.errorJump(new FlowableTransformer<Integer, List<Integer>>() {
            @Override
            public Publisher<List<Integer>> apply(Flowable<Integer> v) {
                return v.buffer(3);
            }
        }))
        .test()
        .assertFailure(TestException.class, Arrays.asList(1, 2, 3), Arrays.asList(4, 5));
    }

    @Test
    public void take() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer, Integer>errorJump(new FlowableTransformer<Integer, Integer>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> v) {
                return v;
            }
        }))
        .take(2)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void innerTransformerCrash() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer, Integer>errorJump(new FlowableTransformer<Integer, Integer>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> v) {
                throw new TestException();
            }
        }))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void outerInnerCrash() {
        TestSubscriber<Integer> ts = Flowable.range(1, 5).concatWith(Flowable.<Integer>error(new TestException("main")))
        .compose(FlowableTransformers.<Integer, Integer>errorJump(new FlowableTransformer<Integer, Integer>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> v) {
                return v.concatWith(Flowable.<Integer>error(new TestException("inner")));
            }
        }))
        .test();

        ts
        .assertFailure(CompositeException.class, 1, 2, 3, 4, 5);

        TestHelper.assertCompositeExceptions(ts,
                TestException.class, "main",
                TestException.class, "inner"
        );
    }
}
