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

import java.util.concurrent.Callable;

import org.junit.Test;

import hu.akarnokd.rxjava2.test.*;
import io.reactivex.*;
import io.reactivex.functions.Function;
import io.reactivex.subjects.CompletableSubject;
import io.reactivex.subscribers.TestSubscriber;

public class CompletableFlatMapSignalFlowableTest {

    @Test
    public void success() {
        final int[] counts = { 0, 0 };

        Completable.complete()
        .as(Completables.flatMapFlowable(
                new Callable<Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> call() throws Exception {
                        counts[0]++;
                        return Flowable.just(1);
                    }
                },
                new Function<Throwable, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        return Flowable.error(e);
                    }
                }
        ))
        .test()
        .assertResult(1);

        assertEquals(1, counts[0]);
        assertEquals(0, counts[1]);
    }

    @Test
    public void empty() {
        final int[] counts = { 0, 0 };

        Completable.complete()
        .as(Completables.flatMapFlowable(
                new Callable<Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> call() throws Exception {
                        counts[0]++;
                        return Flowable.empty();
                    }
                },
                new Function<Throwable, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        return Flowable.error(e);
                    }
                }
        ))
        .test()
        .assertResult();

        assertEquals(1, counts[0]);
        assertEquals(0, counts[1]);
    }

    @Test
    public void error() {
        final int[] counts = { 0, 0 };

        Completable.error(new TestException())
        .as(Completables.flatMapFlowable(
                new Callable<Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> call() throws Exception {
                        counts[0]++;
                        return Flowable.empty();
                    }
                },
                new Function<Throwable, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        return Flowable.error(e);
                    }
                }
        ))
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, counts[0]);
        assertEquals(1, counts[1]);
    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = Completable.complete()
        .as(Completables.flatMapFlowable(new Callable<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> call() throws Exception {
                return Flowable.range(1, 5);
            }
        },
        new Function<Throwable, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Throwable e)
                    throws Exception {
                return Flowable.error(e);
            }
        }))
        .test(0L);

        ts.assertEmpty();

        ts.request(1);

        ts.assertValuesOnly(1);

        ts.request(2);

        ts.assertValuesOnly(1, 2, 3);

        ts.request(2);

        ts.assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normalCrash() {
        final int[] counts = { 0, 0 };

        Completable.complete()
        .as(Completables.flatMapFlowable(
                new Callable<Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> call() throws Exception {
                        counts[0]++;
                        throw new TestException("onComplete");
                    }
                },
                new Function<Throwable, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        throw new TestException("onError");
                    }
                }
        ))
        .test()
        .assertFailureAndMessage(TestException.class, "onComplete");

        assertEquals(1, counts[0]);
        assertEquals(0, counts[1]);
    }

    @Test
    public void errorCrash() {
        final int[] counts = { 0, 0 };

        Completable.error(new TestException("main"))
        .as(Completables.flatMapFlowable(
                new Callable<Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> call() throws Exception {
                        counts[0]++;
                        throw new TestException("onComplete");
                    }
                },
                new Function<Throwable, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Throwable e)
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
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(CompletableSubject.create()
                .as(Completables.flatMapFlowable(
                        new Callable<Flowable<Integer>>() {
                            @Override
                            public Flowable<Integer> call() throws Exception {
                                return Flowable.empty();
                            }
                        },
                        new Function<Throwable, Flowable<Integer>>() {
                            @Override
                            public Flowable<Integer> apply(Throwable e)
                                    throws Exception {
                                return Flowable.error(e);
                            }
                        }
                ))
        );
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeCompletableToFlowable(new Function<Completable, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Completable c) throws Exception {
                return c.as(Completables.flatMapFlowable(
                        new Callable<Flowable<Integer>>() {
                            @Override
                            public Flowable<Integer> call() throws Exception {
                                return Flowable.empty();
                            }
                        },
                        new Function<Throwable, Flowable<Integer>>() {
                            @Override
                            public Flowable<Integer> apply(Throwable e)
                                    throws Exception {
                                return Flowable.error(e);
                            }
                        }
                ));
            }
        });
    }
}
