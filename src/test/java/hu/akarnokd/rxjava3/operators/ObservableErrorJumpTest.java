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

import java.util.*;

import org.junit.Test;

import hu.akarnokd.rxjava3.test.*;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.exceptions.CompositeException;
import io.reactivex.rxjava3.observers.TestObserver;

public class ObservableErrorJumpTest {

    @Test
    public void normalDirect() {
        Observable.range(1, 5)
        .compose(ObservableTransformers.<Integer, Integer>errorJump(new ObservableTransformer<Integer, Integer>() {
            @Override
            public ObservableSource<Integer> apply(Observable<Integer> v) {
                return v;
            }
        }))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void errorDirect() {
        Observable.<Integer>error(new TestException())
        .compose(ObservableTransformers.<Integer, Integer>errorJump(new ObservableTransformer<Integer, Integer>() {
            @Override
            public ObservableSource<Integer> apply(Observable<Integer> v) {
                return v;
            }
        }))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void oneSubscriber() {
        Observable.range(1, 5)
        .compose(ObservableTransformers.<Integer, Integer>errorJump(new ObservableTransformer<Integer, Integer>() {
            @Override
            public ObservableSource<Integer> apply(Observable<Integer> v) {
                return v.mergeWith(v);
            }
        }))
        .test()
        .assertFailure(IllegalStateException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void errorJump() {
        Observable.range(1, 5)
        .concatWith(Observable.<Integer>error(new TestException()))
        .compose(ObservableTransformers.errorJump(new ObservableTransformer<Integer, List<Integer>>() {
            @Override
            public ObservableSource<List<Integer>> apply(Observable<Integer> v) {
                return v.buffer(3);
            }
        }))
        .test()
        .assertFailure(TestException.class, Arrays.asList(1, 2, 3), Arrays.asList(4, 5));
    }

    @Test
    public void take() {
        Observable.range(1, 5)
        .compose(ObservableTransformers.<Integer, Integer>errorJump(new ObservableTransformer<Integer, Integer>() {
            @Override
            public ObservableSource<Integer> apply(Observable<Integer> v) {
                return v;
            }
        }))
        .take(2)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void innerTransformerCrash() {
        Observable.range(1, 5)
        .compose(ObservableTransformers.<Integer, Integer>errorJump(new ObservableTransformer<Integer, Integer>() {
            @Override
            public ObservableSource<Integer> apply(Observable<Integer> v) {
                throw new TestException();
            }
        }))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void outerInnerCrash() {
        TestObserver<Integer> to = Observable.range(1, 5).concatWith(Observable.<Integer>error(new TestException("main")))
        .compose(ObservableTransformers.<Integer, Integer>errorJump(new ObservableTransformer<Integer, Integer>() {
            @Override
            public ObservableSource<Integer> apply(Observable<Integer> v) {
                return v.concatWith(Observable.<Integer>error(new TestException("inner")));
            }
        }))
        .test();

        to
        .assertFailure(CompositeException.class, 1, 2, 3, 4, 5);

        TestHelper.assertCompositeExceptions(to,
                TestException.class, "main",
                TestException.class, "inner"
        );
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Observable.range(1, 5)
                .compose(ObservableTransformers.<Integer, Integer>errorJump(new ObservableTransformer<Integer, Integer>() {
                    @Override
                    public ObservableSource<Integer> apply(Observable<Integer> v) {
                        return v;
                    }
                })));
    }
}
