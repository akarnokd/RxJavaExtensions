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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import hu.akarnokd.rxjava2.test.*;
import io.reactivex.*;
import io.reactivex.functions.Function;
import io.reactivex.subjects.SingleSubject;

public class SingleFlatMapSignalCompletableTest {

    @Test
    public void normalSuccess() {
        final int[] counts = { 0, 0, 0 };

        Single.just(0)
        .as(Singles.flatMapCompletable(
                new Function<Integer, Completable>() {
                    @Override
                    public Completable apply(Integer e)
                            throws Exception {
                        counts[0]++;
                        return Completable.complete();
                    }
                },
                new Function<Throwable, Completable>() {
                    @Override
                    public Completable apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        return Completable.error(e);
                    }
                }
        ))
        .test()
        .assertResult();

        assertEquals(1, counts[0]);
        assertEquals(0, counts[1]);
        assertEquals(0, counts[2]);
    }

    @Test
    public void error() {
        final int[] counts = { 0, 0, 0 };

        Single.<Integer>error(new TestException())
        .as(Singles.flatMapCompletable(
                new Function<Integer, Completable>() {
                    @Override
                    public Completable apply(Integer e)
                            throws Exception {
                        counts[0]++;
                        return Completable.complete();
                    }
                },
                new Function<Throwable, Completable>() {
                    @Override
                    public Completable apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        return Completable.error(e);
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
        .as(Singles.flatMapCompletable(
                new Function<Integer, Completable>() {
                    @Override
                    public Completable apply(Integer e)
                            throws Exception {
                        counts[0]++;
                        throw new TestException("onSuccess");
                    }
                },
                new Function<Throwable, Completable>() {
                    @Override
                    public Completable apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        throw new TestException("onError");
                    }
                }
        ))
        .test()
        .assertFailureAndMessage(TestException.class, "onSuccess");

        assertEquals(1, counts[0]);
        assertEquals(0, counts[1]);
        assertEquals(0, counts[2]);
    }

    @Test
    public void errorCrash() {
        final int[] counts = { 0, 0, 0 };

        Single.<Integer>error(new TestException("main"))
        .as(Singles.flatMapCompletable(
                new Function<Integer, Completable>() {
                    @Override
                    public Completable apply(Integer e)
                            throws Exception {
                        counts[0]++;
                        throw new TestException("onSuccess");
                    }
                },
                new Function<Throwable, Completable>() {
                    @Override
                    public Completable apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        throw new TestException("onError");
                    }
                }
        ))
        .test()
        .assertFailureAndMessage(TestException.class, "onError");

        assertEquals(0, counts[0]);
        assertEquals(1, counts[1]);
        assertEquals(0, counts[2]);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(SingleSubject.<Integer>create()
                .as(Singles.flatMapCompletable(
                        new Function<Integer, Completable>() {
                            @Override
                            public Completable apply(Integer e)
                                    throws Exception {
                                return Completable.complete();
                            }
                        },
                        new Function<Throwable, Completable>() {
                            @Override
                            public Completable apply(Throwable e)
                                    throws Exception {
                                return Completable.error(e);
                            }
                        }
                ))
        );
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingleToCompletable(new Function<Single<Integer>, Completable>() {
            @Override
            public Completable apply(Single<Integer> c) throws Exception {
                return c.as(Singles.flatMapCompletable(
                        new Function<Integer, Completable>() {
                            @Override
                            public Completable apply(Integer e)
                                    throws Exception {
                                return Completable.complete();
                            }
                        },
                        new Function<Throwable, Completable>() {
                            @Override
                            public Completable apply(Throwable e)
                                    throws Exception {
                                return Completable.error(e);
                            }
                        }
                ));
            }
        });
    }
}
