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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import hu.akarnokd.rxjava3.test.*;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.subjects.SingleSubject;

public class SingleFlatMapSignalSingleTest {

    @Test
    public void normalSuccess() {
        final int[] counts = { 0, 0, 0 };

        Single.just(0)
        .compose(SingleTransformers.flatMap(
                new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer e)
                            throws Exception {
                        counts[0]++;
                        return Single.just(2);
                    }
                },
                new Function<Throwable, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        return Single.error(e);
                    }
                }
        ))
        .test()
        .assertResult(2);

        assertEquals(1, counts[0]);
        assertEquals(0, counts[1]);
        assertEquals(0, counts[2]);
    }

    @Test
    public void error() {
        final int[] counts = { 0, 0, 0 };

        Single.<Integer>error(new TestException())
        .compose(SingleTransformers.flatMap(
                new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer e)
                            throws Exception {
                        counts[0]++;
                        return Single.just(2);
                    }
                },
                new Function<Throwable, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        return Single.error(e);
                    }
                }
        ))
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, counts[0]);
        assertEquals(1, counts[1]);
        assertEquals(0, counts[2]);
    }

    @Test
    public void normalSuccessCrash() {
        final int[] counts = { 0, 0, 0 };

        Single.just(1)
        .compose(SingleTransformers.flatMap(
                new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer e)
                            throws Exception {
                        counts[0]++;
                        throw new TestException("onSuccess");
                    }
                },
                new Function<Throwable, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        throw new TestException("onError");
                    }
                }
        ))
        .test()
        .assertFailure(TestException.class)
        .assertError(TestHelper.assertErrorMessage("onSuccess"));

        assertEquals(1, counts[0]);
        assertEquals(0, counts[1]);
        assertEquals(0, counts[2]);
    }

    @Test
    public void errorCrash() {
        final int[] counts = { 0, 0, 0 };

        Single.<Integer>error(new TestException("main"))
        .compose(SingleTransformers.flatMap(
                new Function<Integer, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Integer e)
                            throws Exception {
                        counts[0]++;
                        throw new TestException("onSuccess");
                    }
                },
                new Function<Throwable, Single<Integer>>() {
                    @Override
                    public Single<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        throw new TestException("onError");
                    }
                }
        ))
        .test()
        .assertFailure(TestException.class)
        .assertError(TestHelper.assertErrorMessage("onError"));

        assertEquals(0, counts[0]);
        assertEquals(1, counts[1]);
        assertEquals(0, counts[2]);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(SingleSubject.<Integer>create()
                .compose(SingleTransformers.flatMap(
                        new Function<Integer, Single<Integer>>() {
                            @Override
                            public Single<Integer> apply(Integer e)
                                    throws Exception {
                                return Single.just(2);
                            }
                        },
                        new Function<Throwable, Single<Integer>>() {
                            @Override
                            public Single<Integer> apply(Throwable e)
                                    throws Exception {
                                return Single.error(e);
                            }
                        }
                ))
        );
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle(new Function<Single<Integer>, Single<Integer>>() {
            @Override
            public Single<Integer> apply(Single<Integer> c) throws Exception {
                return c.compose(SingleTransformers.flatMap(
                        new Function<Integer, Single<Integer>>() {
                            @Override
                            public Single<Integer> apply(Integer e)
                                    throws Exception {
                                return Single.just(2);
                            }
                        },
                        new Function<Throwable, Single<Integer>>() {
                            @Override
                            public Single<Integer> apply(Throwable e)
                                    throws Exception {
                                return Single.error(e);
                            }
                        }
                ));
            }
        });
    }
}
