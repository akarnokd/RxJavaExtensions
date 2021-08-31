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
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.subjects.MaybeSubject;

public class MaybeFlatMapSignalFlowableTest {

    @Test
    public void normalEmpty() {
        final int[] counts = { 0, 0, 0 };

        Maybe.<Integer>empty()
        .to(Maybes.flatMapFlowable(
                new Function<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Integer e)
                            throws Exception {
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
                },
                new Supplier<Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> get() throws Exception {
                        counts[2]++;
                        return Flowable.just(1);
                    }
                }
        ))
        .test()
        .assertResult(1);

        assertEquals(0, counts[0]);
        assertEquals(0, counts[1]);
        assertEquals(1, counts[2]);
    }

    @Test
    public void normalSuccess() {
        final int[] counts = { 0, 0, 0 };

        Maybe.just(0)
        .to(Maybes.flatMapFlowable(
                new Function<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Integer e)
                            throws Exception {
                        counts[0]++;
                        return Flowable.just(2);
                    }
                },
                new Function<Throwable, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        return Flowable.error(e);
                    }
                },
                new Supplier<Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> get() throws Exception {
                        counts[2]++;
                        return Flowable.just(1);
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
    public void backpressure() {
        final int[] counts = { 0, 0, 0 };

        Maybe.just(0)
        .to(Maybes.flatMapFlowable(
                new Function<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Integer e)
                            throws Exception {
                        counts[0]++;
                        return Flowable.range(1, 5);
                    }
                },
                new Function<Throwable, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        return Flowable.error(e);
                    }
                },
                new Supplier<Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> get() throws Exception {
                        counts[2]++;
                        return Flowable.just(1);
                    }
                }
        ))
        .test(0L)
        .assertEmpty()
        .requestMore(1)
        .assertValuesOnly(1)
        .requestMore(2)
        .assertValuesOnly(1, 2, 3)
        .requestMore(2)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, counts[0]);
        assertEquals(0, counts[1]);
        assertEquals(0, counts[2]);
    }

    @Test
    public void empty() {
        final int[] counts = { 0, 0, 0 };

        Maybe.<Integer>empty()
        .to(Maybes.flatMapFlowable(
                new Function<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Integer e)
                            throws Exception {
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
                },
                new Supplier<Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> get() throws Exception {
                        counts[2]++;
                        return Flowable.empty();
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
        .to(Maybes.flatMapFlowable(
                new Function<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Integer e)
                            throws Exception {
                        counts[0]++;
                        return Flowable.just(2);
                    }
                },
                new Function<Throwable, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        return Flowable.error(e);
                    }
                },
                new Supplier<Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> get() throws Exception {
                        counts[2]++;
                        return Flowable.empty();
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
        .to(Maybes.flatMapFlowable(
                new Function<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Integer e)
                            throws Exception {
                        counts[0]++;
                        throw new TestException("onSuccess");
                    }
                },
                new Function<Throwable, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        throw new TestException("onError");
                    }
                },
                new Supplier<Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> get() throws Exception {
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
        .to(Maybes.flatMapFlowable(
                new Function<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Integer e)
                            throws Exception {
                        counts[0]++;
                        throw new TestException("onSuccess");
                    }
                },
                new Function<Throwable, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        throw new TestException("onError");
                    }
                },
                new Supplier<Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> get() throws Exception {
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
        .to(Maybes.flatMapFlowable(
                new Function<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Integer e)
                            throws Exception {
                        counts[0]++;
                        throw new TestException("onSuccess");
                    }
                },
                new Function<Throwable, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Throwable e)
                            throws Exception {
                        counts[1]++;
                        throw new TestException("onError");
                    }
                },
                new Supplier<Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> get() throws Exception {
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
                .to(Maybes.flatMapFlowable(
                        new Function<Integer, Flowable<Integer>>() {
                            @Override
                            public Flowable<Integer> apply(Integer e)
                                    throws Exception {
                                return Flowable.just(2);
                            }
                        },
                        new Function<Throwable, Flowable<Integer>>() {
                            @Override
                            public Flowable<Integer> apply(Throwable e)
                                    throws Exception {
                                return Flowable.error(e);
                            }
                        },
                        new Supplier<Flowable<Integer>>() {
                            @Override
                            public Flowable<Integer> get() throws Exception {
                                return Flowable.empty();
                            }
                        }
                ))
        );
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybeToFlowable(new Function<Maybe<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Maybe<Integer> c) throws Exception {
                return c.to(Maybes.flatMapFlowable(
                        new Function<Integer, Flowable<Integer>>() {
                            @Override
                            public Flowable<Integer> apply(Integer e)
                                    throws Exception {
                                return Flowable.just(2);
                            }
                        },
                        new Function<Throwable, Flowable<Integer>>() {
                            @Override
                            public Flowable<Integer> apply(Throwable e)
                                    throws Exception {
                                return Flowable.error(e);
                            }
                        },
                        new Supplier<Flowable<Integer>>() {
                            @Override
                            public Flowable<Integer> get() throws Exception {
                                return Flowable.empty();
                            }
                        }
                ));
            }
        });
    }
}
