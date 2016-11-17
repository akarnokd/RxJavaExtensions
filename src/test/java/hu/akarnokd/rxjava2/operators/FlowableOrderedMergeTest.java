/*
 * Copyright 2016 David Karnok
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

import java.io.IOException;
import java.util.*;

import org.junit.Test;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.Flowable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableOrderedMergeTest {

    @SuppressWarnings("unchecked")
    @Test
    public void normal1() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1), Flowable.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void normal2() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1, 3, 5, 7), Flowable.just(2, 4, 6, 8))
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void normal3() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1, 3, 5, 7), Flowable.just(2, 4, 6))
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void normal4() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1, 3, 5, 7), Flowable.just(1, 3, 5, 7))
        .test()
        .assertResult(1, 1, 3, 3, 5, 5, 7, 7);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void normal1Hidden() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1).hide(), Flowable.just(2).hide())
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void normal2Hidden() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1, 3, 5, 7).hide(), Flowable.just(2, 4, 6, 8).hide())
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void normal3Hidden() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1, 3, 5, 7).hide(), Flowable.just(2, 4, 6).hide())
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void normal4Hidden() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1, 3, 5, 7).hide(), Flowable.just(1, 3, 5, 7).hide())
        .test()
        .assertResult(1, 1, 3, 3, 5, 5, 7, 7);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void backpressure1() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1, 3, 5, 7), Flowable.just(2, 4, 6, 8))
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void backpressure2() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1), Flowable.just(2))
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void backpressure3() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(), false, 1,
                Flowable.just(1, 3, 5, 7), Flowable.just(2, 4, 6, 8))
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void take() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1, 3, 5, 7), Flowable.just(2, 4, 6, 8))
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void firstErrors() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.<Integer>error(new IOException()),
                Flowable.just(2, 4, 6, 8))
        .test()
        .assertFailure(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void firstErrorsBackpressured() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.<Integer>error(new IOException()),
                Flowable.just(2, 4, 6, 8))
        .test(0L)
        .assertFailure(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void secondErrors() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1, 3, 5, 7),
                Flowable.<Integer>error(new IOException())
        )
        .test()
        .assertFailure(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void secondErrorsBackpressured() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1, 3, 5, 7),
                Flowable.<Integer>error(new IOException())
        )
        .test(0L)
        .assertFailure(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bothError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                    Flowable.<Integer>error(new IOException("first")),
                    Flowable.<Integer>error(new IOException("second"))
            )
            .test()
            .assertFailureAndMessage(IOException.class, "first");

            TestHelper.assertError(errors, 0, IOException.class, "second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bothErrorDelayed() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                true,
                Flowable.<Integer>error(new IOException("first")),
                Flowable.<Integer>error(new IOException("second"))
        )
        .test()
        .assertFailure(CompositeException.class)
        .assertOf(new Consumer<TestSubscriber<Integer>>() {
            @Override
            public void accept(TestSubscriber<Integer> ts) throws Exception {
                List<Throwable> list = TestHelper.compositeList(ts.errors().get(0));

                TestHelper.assertError(list, 0, IOException.class, "first");
                TestHelper.assertError(list, 1, IOException.class, "second");
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bothErrorDelayedBackpressured() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                true,
                Flowable.<Integer>error(new IOException("first")),
                Flowable.<Integer>error(new IOException("second"))
        )
        .test(0L)
        .assertFailure(CompositeException.class)
        .assertOf(new Consumer<TestSubscriber<Integer>>() {
            @Override
            public void accept(TestSubscriber<Integer> ts) throws Exception {
                List<Throwable> list = TestHelper.compositeList(ts.errors().get(0));

                TestHelper.assertError(list, 0, IOException.class, "first");
                TestHelper.assertError(list, 1, IOException.class, "second");
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nonEmptyBothErrorDelayed() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                true,
                Flowable.just(1).concatWith(Flowable.<Integer>error(new IOException("first"))),
                Flowable.just(2).concatWith(Flowable.<Integer>error(new IOException("second")))
        )
        .test()
        .assertFailure(CompositeException.class, 1, 2)
        .assertOf(new Consumer<TestSubscriber<Integer>>() {
            @Override
            public void accept(TestSubscriber<Integer> ts) throws Exception {
                List<Throwable> list = TestHelper.compositeList(ts.errors().get(0));

                TestHelper.assertError(list, 0, IOException.class, "first");
                TestHelper.assertError(list, 1, IOException.class, "second");
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nonEmptyBothErrorDelayed2() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                true,
                Flowable.just(1).concatWith(Flowable.<Integer>error(new IOException("first"))),
                Flowable.just(2).concatWith(Flowable.<Integer>error(new IOException("second")))
        )
        .rebatchRequests(1)
        .test()
        .assertFailure(CompositeException.class, 1, 2)
        .assertOf(new Consumer<TestSubscriber<Integer>>() {
            @Override
            public void accept(TestSubscriber<Integer> ts) throws Exception {
                List<Throwable> list = TestHelper.compositeList(ts.errors().get(0));

                TestHelper.assertError(list, 0, IOException.class, "first");
                TestHelper.assertError(list, 1, IOException.class, "second");
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void never() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.<Integer>never(), Flowable.<Integer>never())
        .test()
        .assertOf(new Consumer<TestSubscriber<Integer>>() {
            @Override
            public void accept(TestSubscriber<Integer> ts) throws Exception {
                ts.cancel();
            }
        })
        .assertEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fusedThrowsInDrainLoop() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1).map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) throws Exception {
                        throw new IllegalArgumentException();
                    }
                }),
                Flowable.just(2, 3))
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fusedThrowsInDrainLoopDelayed() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                true,
                Flowable.just(4).map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) throws Exception {
                        throw new IllegalArgumentException();
                    }
                }),
                Flowable.just(2, 3))
        .test()
        .assertFailure(IllegalArgumentException.class, 2, 3);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fusedThrowsInPostEmissionCheck() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1).map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) throws Exception {
                        throw new IllegalArgumentException();
                    }
                }),
                Flowable.just(2, 3))
        .test(0L)
        .assertFailure(IllegalArgumentException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fusedThrowsInPostEmissionCheckErrorDelayed() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                true,
                Flowable.just(1).map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) throws Exception {
                        throw new IllegalArgumentException();
                    }
                }),
                Flowable.just(2, 3))
        .test(0L)
        .requestMore(2)
        .assertFailure(IllegalArgumentException.class, 2, 3);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void iterable() {
        Flowables.orderedMerge(Arrays.asList(Flowable.just(1, 3, 5, 7), Flowable.just(2, 4, 6, 8)),
                Functions.<Integer>naturalComparator()
        )
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    public void iterableEmpty() {
        Flowables.orderedMerge(Collections.<Flowable<Integer>>emptyList(),
                Functions.<Integer>naturalComparator()
        )
        .test()
        .assertResult();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void iterableSingleton() {
        Flowables.orderedMerge(Arrays.asList(Flowable.just(1, 3, 5, 7)),
                Functions.<Integer>naturalComparator()
        )
        .test()
        .assertResult(1, 3, 5, 7);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void iterableDelayErrors() {
        Flowables.orderedMerge(Arrays.asList(Flowable.just(1, 3, 5, 7), Flowable.just(2, 4, 6, 8)),
                Functions.<Integer>naturalComparator(), true
        )
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void iterableDelayErrorsAndPrefetch() {
        Flowables.orderedMerge(Arrays.asList(Flowable.just(1, 3, 5, 7), Flowable.just(2, 4, 6, 8)),
                Functions.<Integer>naturalComparator(), true, 1
        )
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    public void iterableMany() {
        List<Flowable<Integer>> sources = new ArrayList<Flowable<Integer>>();

        for (int i = 0; i < 32; i++) {
            sources.add(Flowable.just(i));
        }

        Flowables.orderedMerge(sources,
                Functions.<Integer>naturalComparator()
        )
        .test()
        .assertResult(0, 1, 2, 3, 4, 5, 6, 7,
                8, 9, 10, 11, 12, 13, 14, 15,
                16, 17, 18, 19, 20, 21, 22, 23,
                24, 25, 26, 27, 28, 29, 30, 31);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void iterableNull() {
        Flowables.orderedMerge(Arrays.asList(Flowable.just(1, 3, 5, 7), null),
                Functions.<Integer>naturalComparator()
        )
        .test()
        .assertFailure(NullPointerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nullSecond() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                Flowable.just(1), null)
        .test()
        .assertFailure(NullPointerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nullSecondDelayErrors() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                true,
                Flowable.just(1), null)
        .test()
        .assertFailure(NullPointerException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nullFirst() {
        Flowables.orderedMerge(Functions.<Integer>naturalComparator(),
                null, Flowable.just(1), null)
        .test()
        .assertFailure(NullPointerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void comparatorThrows() {
        Flowables.orderedMerge(new Comparator<Integer>() {
                    @Override
                    public int compare(Integer a, Integer b) {
                        throw new IllegalArgumentException();
                    }
                },
                Flowable.just(1, 3), Flowable.just(2, 4))
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void selfComparableArray() {
        Flowables.orderedMerge(
                Flowable.just(1), Flowable.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void selfComparableArrayDelayError() {
        Flowables.orderedMerge(true,
                Flowable.just(1), Flowable.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void selfComparableArrayDelayErrorPrefetch() {
        Flowables.orderedMerge(true, 1,
                Flowable.just(1), Flowable.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void selfComparableIterable() {
        Flowables.orderedMerge(
                Arrays.asList(Flowable.just(1), Flowable.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void selfComparableIterableDelayError() {
        Flowables.orderedMerge(
                Arrays.asList(Flowable.just(1), Flowable.just(2))
                , true
        )
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void selfComparableIterableDelayErrorPrefetch() {
        Flowables.orderedMerge(
                Arrays.asList(Flowable.just(1), Flowable.just(2))
                , true, 1
        )
        .test()
        .assertResult(1, 2);
    }
}
