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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import hu.akarnokd.rxjava3.test.*;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.subjects.CompletableSubject;

public class CompletableFlatMapSignalObservableTest {

    @Test
    public void success() {
        final int[] counts = { 0, 0 };

        Completable.complete()
        .to(Completables.flatMapObservable(
                new Supplier<Observable<Integer>>() {
                    @Override
                    public Observable<Integer> get() throws Exception {
                        counts[0]++;
                        return Observable.just(1);
                    }
                },
                new Function<Throwable, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        return Observable.error(e);
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
        .to(Completables.flatMapObservable(
                new Supplier<Observable<Integer>>() {
                    @Override
                    public Observable<Integer> get() throws Exception {
                        counts[0]++;
                        return Observable.empty();
                    }
                },
                new Function<Throwable, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        return Observable.error(e);
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
        .to(Completables.flatMapObservable(
                new Supplier<Observable<Integer>>() {
                    @Override
                    public Observable<Integer> get() throws Exception {
                        counts[0]++;
                        return Observable.empty();
                    }
                },
                new Function<Throwable, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        return Observable.error(e);
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
        .to(Completables.flatMapObservable(
                new Supplier<Observable<Integer>>() {
                    @Override
                    public Observable<Integer> get() throws Exception {
                        counts[0]++;
                        throw new TestException("onComplete");
                    }
                },
                new Function<Throwable, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        throw new TestException("onError");
                    }
                }
        ))
        .test()
        .assertFailure(TestException.class)
        .assertError(TestHelper.assertErrorMessage("onComplete"));

        assertEquals(1, counts[0]);
        assertEquals(0, counts[1]);
    }

    @Test
    public void errorCrash() {
        final int[] counts = { 0, 0 };

        Completable.error(new TestException("main"))
        .to(Completables.flatMapObservable(
                new Supplier<Observable<Integer>>() {
                    @Override
                    public Observable<Integer> get() throws Exception {
                        counts[0]++;
                        throw new TestException("onComplete");
                    }
                },
                new Function<Throwable, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(Throwable e)
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
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(CompletableSubject.create()
                .to(Completables.flatMapObservable(
                        new Supplier<Observable<Integer>>() {
                            @Override
                            public Observable<Integer> get() throws Exception {
                                return Observable.empty();
                            }
                        },
                        new Function<Throwable, Observable<Integer>>() {
                            @Override
                            public Observable<Integer> apply(Throwable e)
                                    throws Exception {
                                return Observable.error(e);
                            }
                        }
                ))
        );
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeCompletableToObservable(new Function<Completable, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Completable c) throws Exception {
                return c.to(Completables.flatMapObservable(
                        new Supplier<Observable<Integer>>() {
                            @Override
                            public Observable<Integer> get() throws Exception {
                                return Observable.empty();
                            }
                        },
                        new Function<Throwable, Observable<Integer>>() {
                            @Override
                            public Observable<Integer> apply(Throwable e)
                                    throws Exception {
                                return Observable.error(e);
                            }
                        }
                ));
            }
        });
    }
}
