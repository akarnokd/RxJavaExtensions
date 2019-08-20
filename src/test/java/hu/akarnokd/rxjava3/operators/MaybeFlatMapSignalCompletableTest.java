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
import io.reactivex.rxjava3.subjects.MaybeSubject;

public class MaybeFlatMapSignalCompletableTest {

    @Test
    public void normalEmpty() {
        final int[] counts = { 0, 0, 0 };

        Maybe.<Integer>empty()
        .to(Maybes.flatMapCompletable(
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
                },
                new Supplier<Completable>() {
                    @Override
                    public Completable get() throws Exception {
                        counts[2]++;
                        return Completable.complete();
                    }
                }
        ))
        .test()
        .assertResult();

        assertEquals(0, counts[0]);
        assertEquals(0, counts[1]);
        assertEquals(1, counts[2]);
    }

    @Test
    public void normalSuccess() {
        final int[] counts = { 0, 0, 0 };

        Maybe.just(0)
        .to(Maybes.flatMapCompletable(
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
                },
                new Supplier<Completable>() {
                    @Override
                    public Completable get() throws Exception {
                        counts[2]++;
                        return Completable.complete();
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
    public void empty() {
        final int[] counts = { 0, 0, 0 };

        Maybe.<Integer>empty()
        .to(Maybes.flatMapCompletable(
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
                },
                new Supplier<Completable>() {
                    @Override
                    public Completable get() throws Exception {
                        counts[2]++;
                        return Completable.complete();
                    }
                }
        ))
        .test()
        .assertResult();

        assertEquals(0, counts[0]);
        assertEquals(0, counts[1]);
        assertEquals(1, counts[2]);
    }

    @Test
    public void error() {
        final int[] counts = { 0, 0, 0 };

        Maybe.<Integer>error(new TestException())
        .to(Maybes.flatMapCompletable(
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
                },
                new Supplier<Completable>() {
                    @Override
                    public Completable get() throws Exception {
                        counts[2]++;
                        return Completable.complete();
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

        Maybe.just(1)
        .to(Maybes.flatMapCompletable(
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
                },
                new Supplier<Completable>() {
                    @Override
                    public Completable get() throws Exception {
                        counts[2]++;
                        throw new TestException("onComplete");
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
    public void normalEmptyCrash() {
        final int[] counts = { 0, 0, 0 };

        Maybe.<Integer>empty()
        .to(Maybes.flatMapCompletable(
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
                },
                new Supplier<Completable>() {
                    @Override
                    public Completable get() throws Exception {
                        counts[2]++;
                        throw new TestException("onComplete");
                    }
                }
        ))
        .test()
        .assertFailure(TestException.class)
        .assertError(TestHelper.assertErrorMessage("onComplete"));

        assertEquals(0, counts[0]);
        assertEquals(0, counts[1]);
        assertEquals(1, counts[2]);
    }

    @Test
    public void errorCrash() {
        final int[] counts = { 0, 0, 0 };

        Maybe.<Integer>error(new TestException("main"))
        .to(Maybes.flatMapCompletable(
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
                },
                new Supplier<Completable>() {
                    @Override
                    public Completable get() throws Exception {
                        counts[2]++;
                        throw new TestException("onComplete");
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
        TestHelper.checkDisposed(MaybeSubject.<Integer>create()
                .to(Maybes.flatMapCompletable(
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
                        },
                        new Supplier<Completable>() {
                            @Override
                            public Completable get() throws Exception {
                                return Completable.complete();
                            }
                        }
                ))
        );
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybeToCompletable(new Function<Maybe<Integer>, Completable>() {
            @Override
            public Completable apply(Maybe<Integer> c) throws Exception {
                return c.to(Maybes.flatMapCompletable(
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
                        },
                        new Supplier<Completable>() {
                            @Override
                            public Completable get() throws Exception {
                                return Completable.complete();
                            }
                        }
                ));
            }
        });
    }
}
