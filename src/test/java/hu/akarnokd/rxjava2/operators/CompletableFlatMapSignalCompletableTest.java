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

public class CompletableFlatMapSignalCompletableTest {

    @Test
    public void normal() {
        final int[] counts = { 0, 0 };

        Completable.complete()
        .compose(CompletableTransformers.flatMap(
                new Callable<CompletableSource>() {
                    @Override
                    public CompletableSource call() throws Exception {
                        counts[0]++;
                        return Completable.complete();
                    }
                },
                new Function<Throwable, CompletableSource>() {
                    @Override
                    public CompletableSource apply(Throwable e)
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
    }

    @Test
    public void error() {
        final int[] counts = { 0, 0 };

        Completable.error(new TestException())
        .compose(CompletableTransformers.flatMap(
                new Callable<CompletableSource>() {
                    @Override
                    public CompletableSource call() throws Exception {
                        counts[0]++;
                        return Completable.complete();
                    }
                },
                new Function<Throwable, CompletableSource>() {
                    @Override
                    public CompletableSource apply(Throwable e)
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
    }

    @Test
    public void normalCrash() {
        final int[] counts = { 0, 0 };

        Completable.complete()
        .compose(CompletableTransformers.flatMap(
                new Callable<CompletableSource>() {
                    @Override
                    public CompletableSource call() throws Exception {
                        counts[0]++;
                        throw new TestException("onComplete");
                    }
                },
                new Function<Throwable, CompletableSource>() {
                    @Override
                    public CompletableSource apply(Throwable e)
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
        .compose(CompletableTransformers.flatMap(
                new Callable<CompletableSource>() {
                    @Override
                    public CompletableSource call() throws Exception {
                        counts[0]++;
                        throw new TestException("onComplete");
                    }
                },
                new Function<Throwable, CompletableSource>() {
                    @Override
                    public CompletableSource apply(Throwable e)
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
                .compose(CompletableTransformers.flatMap(
                        new Callable<CompletableSource>() {
                            @Override
                            public CompletableSource call() throws Exception {
                                return Completable.complete();
                            }
                        },
                        new Function<Throwable, CompletableSource>() {
                            @Override
                            public CompletableSource apply(Throwable e)
                                    throws Exception {
                                return Completable.error(e);
                            }
                        }
                ))
        );
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeCompletable(new Function<Completable, CompletableSource>() {
            @Override
            public CompletableSource apply(Completable c) throws Exception {
                return c.compose(CompletableTransformers.flatMap(
                        new Callable<CompletableSource>() {
                            @Override
                            public CompletableSource call() throws Exception {
                                return Completable.complete();
                            }
                        },
                        new Function<Throwable, CompletableSource>() {
                            @Override
                            public CompletableSource apply(Throwable e)
                                    throws Exception {
                                return Completable.error(e);
                            }
                        }
                ));
            }
        });
    }
}
