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

package hu.akarnokd.rxjava2.parallel;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.reactivestreams.Publisher;

import hu.akarnokd.rxjava2.util.SelfComparator;
import io.reactivex.*;
import io.reactivex.functions.*;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

@SuppressWarnings("deprecation")
public class ParallelFlowableTest {

    static <T> Function<Flowable<Integer>, ParallelFlowable<Integer>> parallel() {
        return new Function<Flowable<Integer>, ParallelFlowable<Integer>>() {
            @Override
            public ParallelFlowable<Integer> apply(Flowable<Integer> f) throws Exception {
                return ParallelFlowable.from(f);
            }
        };
    }

    static <T> Function<Flowable<Integer>, ParallelFlowable<Integer>> parallel(final int level) {
        return new Function<Flowable<Integer>, ParallelFlowable<Integer>>() {
            @Override
            public ParallelFlowable<Integer> apply(Flowable<Integer> f) throws Exception {
                return ParallelFlowable.from(f, level);
            }
        };
    }

    static <T> Function<Flowable<Long>, ParallelFlowable<Long>> parallelLong(final int level) {
        return new Function<Flowable<Long>, ParallelFlowable<Long>>() {
            @Override
            public ParallelFlowable<Long> apply(Flowable<Long> f) throws Exception {
                return ParallelFlowable.from(f, level);
            }
        };
    }

    @Test
    public void sequentialMode() {
        Flowable<Integer> source = Flowable.range(1, 1000000).hide();
        for (int i = 1; i < 33; i++) {
            Flowable<Integer> result = ParallelFlowable.from(source, i)
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {
                    return v + 1;
                }
            })
            .sequential()
            ;

            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

            result.subscribe(ts);

            ts
            .assertSubscribed()
            .assertValueCount(1000000)
            .assertComplete()
            .assertNoErrors()
            ;
        }

    }

    @Test
    public void sequentialModeFused() {
        Flowable<Integer> source = Flowable.range(1, 1000000);
        for (int i = 1; i < 33; i++) {
            Flowable<Integer> result = ParallelFlowable.from(source, i)
            .map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer v) throws Exception {
                    return v + 1;
                }
            })
            .sequential()
            ;

            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

            result.subscribe(ts);

            ts
            .assertSubscribed()
            .assertValueCount(1000000)
            .assertComplete()
            .assertNoErrors()
            ;
        }

    }

    @Test
    public void parallelMode() {
        Flowable<Integer> source = Flowable.range(1, 1000000).hide();
        int ncpu = Math.max(8, Runtime.getRuntime().availableProcessors());
        for (int i = 1; i < ncpu + 1; i++) {

            ExecutorService exec = Executors.newFixedThreadPool(i);

            Scheduler scheduler = Schedulers.from(exec);

            try {
                Flowable<Integer> result = ParallelFlowable.from(source, i)
                .runOn(scheduler)
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) throws Exception {
                        return v + 1;
                    }
                })
                .sequential()
                ;

                TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

                result.subscribe(ts);

                ts.awaitDone(10, TimeUnit.SECONDS);

                ts
                .assertSubscribed()
                .assertValueCount(1000000)
                .assertComplete()
                .assertNoErrors()
                ;
            } finally {
                exec.shutdown();
            }
        }

    }

    @Test
    public void parallelModeFused() {
        Flowable<Integer> source = Flowable.range(1, 1000000);
        int ncpu = Math.max(8, Runtime.getRuntime().availableProcessors());
        for (int i = 1; i < ncpu + 1; i++) {

            ExecutorService exec = Executors.newFixedThreadPool(i);

            Scheduler scheduler = Schedulers.from(exec);

            try {
                Flowable<Integer> result = ParallelFlowable.from(source, i)
                .runOn(scheduler)
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) throws Exception {
                        return v + 1;
                    }
                })
                .sequential()
                ;

                TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

                result.subscribe(ts);

                ts.awaitDone(10, TimeUnit.SECONDS);

                ts
                .assertSubscribed()
                .assertValueCount(1000000)
                .assertComplete()
                .assertNoErrors()
                ;
            } finally {
                exec.shutdown();
            }
        }

    }

    @Test
    public void reduceFull() {
        for (int i = 1; i <= Runtime.getRuntime().availableProcessors() * 2; i++) {
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

            Flowable.range(1, 10)
            .to(parallel(i))
            .reduce(new BiFunction<Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b) throws Exception {
                    return a + b;
                }
            })
            .subscribe(ts);

            ts.assertResult(55);
        }
    }

    @Test
    public void parallelReduceFull() {
        int m = 100000;
        for (int n = 1; n <= m; n *= 10) {
//            System.out.println(n);
            for (int i = 1; i <= Runtime.getRuntime().availableProcessors(); i++) {
//                System.out.println("  " + i);

                ExecutorService exec = Executors.newFixedThreadPool(i);

                Scheduler scheduler = Schedulers.from(exec);

                try {
                    TestSubscriber<Long> ts = new TestSubscriber<Long>();

                    Flowable.range(1, n)
                    .map(new Function<Integer, Long>() {
                        @Override
                        public Long apply(Integer v) throws Exception {
                            return (long)v;
                        }
                    })
                    .to(parallelLong(i))
                    .runOn(scheduler)
                    .reduce(new BiFunction<Long, Long, Long>() {
                        @Override
                        public Long apply(Long a, Long b) throws Exception {
                            return a + b;
                        }
                    })
                    .subscribe(ts);

                    ts.awaitDone(500, TimeUnit.SECONDS);

                    long e = ((long)n) * (1 + n) / 2;

                    ts.assertResult(e);
                } finally {
                    exec.shutdown();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void toSortedList() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        Flowable.fromArray(10, 9, 8, 7, 6, 5, 4, 3, 2, 1)
        .to(parallel())
        .toSortedList(SelfComparator.INSTANCE)
        .subscribe(ts);

        ts.assertResult(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void sorted() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);

        Flowable.fromArray(10, 9, 8, 7, 6, 5, 4, 3, 2, 1)
        .to(parallel())
        .sorted(SelfComparator.INSTANCE)
        .subscribe(ts);

        ts.assertNoValues();

        ts.request(2);

        ts.assertValues(1, 2);

        ts.request(5);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7);

        ts.request(3);

        ts.assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void collect() {
        Callable<List<Integer>> as = new Callable<List<Integer>>() {
            @Override
            public List<Integer> call() throws Exception {
                return new ArrayList<Integer>();
            }
        };

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.range(1, 10)
        .to(parallel())
        .collect(as, new BiConsumer<List<Integer>, Integer>() {
            @Override
            public void accept(List<Integer> a, Integer b) throws Exception {
                a.add(b);
            }
        })
        .sequential()
        .flatMapIterable(new Function<List<Integer>, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(List<Integer> v) throws Exception {
                return v;
            }
        })
        .subscribe(ts);

        ts.assertValueSet(new HashSet<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
        .assertNoErrors()
        .assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void from() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        ParallelFlowable.fromArray(Flowable.range(1, 5), Flowable.range(6, 5))
        .sequential()
        .subscribe(ts);

        ts.assertValueSet(new HashSet<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void concatMapUnordered() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(1, 5)
        .to(parallel())
        .concatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.range(v * 10 + 1, 3);
            }
        })
        .sequential()
        .subscribe(ts);

        ts.assertValueSet(new HashSet<Integer>(Arrays.asList(11, 12, 13, 21, 22, 23, 31, 32, 33, 41, 42, 43, 51, 52, 53)))
        .assertNoErrors()
        .assertComplete();

    }

    @Test
    public void flatMapUnordered() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        Flowable.range(1, 5)
        .to(parallel())
        .flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.range(v * 10 + 1, 3);
            }
        })
        .sequential()
        .subscribe(ts);

        ts.assertValueSet(new HashSet<Integer>(Arrays.asList(11, 12, 13, 21, 22, 23, 31, 32, 33, 41, 42, 43, 51, 52, 53)))
        .assertNoErrors()
        .assertComplete();

    }

    @Test
    public void collectAsyncFused() {
        ExecutorService exec = Executors.newFixedThreadPool(3);

        Scheduler s = Schedulers.from(exec);

        try {
            Callable<List<Integer>> as = new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return new ArrayList<Integer>();
                }
            };
            TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

            Flowable.range(1, 100000)
            .to(parallel(3))
            .runOn(s)
            .collect(as, new BiConsumer<List<Integer>, Integer>() {
                @Override
                public void accept(List<Integer> a, Integer b) throws Exception {
                    a.add(b);
                }
            })
            .doOnNext(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> v) throws Exception {
                    System.out.println(v.size());
                }
            })
            .sequential()
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(3)
            .assertNoErrors()
            .assertComplete()
            ;

            List<List<Integer>> list = ts.values();

            Assert.assertEquals(100000, list.get(0).size() + list.get(1).size() + list.get(2).size());
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void collectAsync() {
        ExecutorService exec = Executors.newFixedThreadPool(3);

        Scheduler s = Schedulers.from(exec);

        try {
            Callable<List<Integer>> as = new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return new ArrayList<Integer>();
                }
            };
            TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

            Flowable.range(1, 100000).hide()
            .to(parallel(3))
            .runOn(s)
            .collect(as, new BiConsumer<List<Integer>, Integer>() {
                @Override
                public void accept(List<Integer> a, Integer b) throws Exception {
                    a.add(b);
                }
            })
            .doOnNext(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> v) throws Exception {
                    System.out.println(v.size());
                }
            })
            .sequential()
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(3)
            .assertNoErrors()
            .assertComplete()
            ;

            List<List<Integer>> list = ts.values();

            Assert.assertEquals(100000, list.get(0).size() + list.get(1).size() + list.get(2).size());
        } finally {
            exec.shutdown();
        }
    }


    @Test
    public void collectAsync2() {
        ExecutorService exec = Executors.newFixedThreadPool(3);

        Scheduler s = Schedulers.from(exec);

        try {
            Callable<List<Integer>> as = new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return new ArrayList<Integer>();
                }
            };
            TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

            Flowable.range(1, 100000).hide()
            .observeOn(s)
            .to(parallel(3))
            .runOn(s)
            .collect(as, new BiConsumer<List<Integer>, Integer>() {
                @Override
                public void accept(List<Integer> a, Integer b) throws Exception {
                    a.add(b);
                }
            })
            .doOnNext(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> v) throws Exception {
                    System.out.println(v.size());
                }
            })
            .sequential()
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(3)
            .assertNoErrors()
            .assertComplete()
            ;

            List<List<Integer>> list = ts.values();

            Assert.assertEquals(100000, list.get(0).size() + list.get(1).size() + list.get(2).size());
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void collectAsync3() {
        ExecutorService exec = Executors.newFixedThreadPool(3);

        Scheduler s = Schedulers.from(exec);

        try {
            Callable<List<Integer>> as = new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return new ArrayList<Integer>();
                }
            };
            TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

            Flowable.range(1, 100000).hide()
            .observeOn(s)
            .to(parallel(3))
            .runOn(s)
            .collect(as, new BiConsumer<List<Integer>, Integer>() {
                @Override
                public void accept(List<Integer> a, Integer b) throws Exception {
                    a.add(b);
                }
            })
            .doOnNext(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> v) throws Exception {
                    System.out.println(v.size());
                }
            })
            .sequential()
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(3)
            .assertNoErrors()
            .assertComplete()
            ;

            List<List<Integer>> list = ts.values();

            Assert.assertEquals(100000, list.get(0).size() + list.get(1).size() + list.get(2).size());
        } finally {
            exec.shutdown();
        }
    }


    @Test
    public void collectAsync3Fused() {
        ExecutorService exec = Executors.newFixedThreadPool(3);

        Scheduler s = Schedulers.from(exec);

        try {
            Callable<List<Integer>> as = new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return new ArrayList<Integer>();
                }
            };
            TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

            Flowable.range(1, 100000)
            .observeOn(s)
            .to(parallel(3))
            .runOn(s)
            .collect(as, new BiConsumer<List<Integer>, Integer>() {
                @Override
                public void accept(List<Integer> a, Integer b) throws Exception {
                    a.add(b);
                }
            })
            .doOnNext(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> v) throws Exception {
                    System.out.println(v.size());
                }
            })
            .sequential()
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(3)
            .assertNoErrors()
            .assertComplete()
            ;

            List<List<Integer>> list = ts.values();

            Assert.assertEquals(100000, list.get(0).size() + list.get(1).size() + list.get(2).size());
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void collectAsync3Take() {
        ExecutorService exec = Executors.newFixedThreadPool(4);

        Scheduler s = Schedulers.from(exec);

        try {
            Callable<List<Integer>> as = new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return new ArrayList<Integer>();
                }
            };
            TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

            Flowable.range(1, 100000)
            .take(1000)
            .observeOn(s)
            .to(parallel(3))
            .runOn(s)
            .collect(as, new BiConsumer<List<Integer>, Integer>() {
                @Override
                public void accept(List<Integer> a, Integer b) throws Exception {
                    a.add(b);
                }
            })
            .doOnNext(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> v) throws Exception {
                    System.out.println(v.size());
                }
            })
            .sequential()
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(3)
            .assertNoErrors()
            .assertComplete()
            ;

            List<List<Integer>> list = ts.values();

            Assert.assertEquals(1000, list.get(0).size() + list.get(1).size() + list.get(2).size());
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void collectAsync4Take() {
        ExecutorService exec = Executors.newFixedThreadPool(3);

        Scheduler s = Schedulers.from(exec);

        try {
            Callable<List<Integer>> as = new Callable<List<Integer>>() {
                @Override
                public List<Integer> call() throws Exception {
                    return new ArrayList<Integer>();
                }
            };
            TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

            UnicastProcessor<Integer> up = UnicastProcessor.create();

            for (int i = 0; i < 1000; i++) {
                up.onNext(i);
            }

            up
            .take(1000)
            .observeOn(s)
            .to(parallel(3))
            .runOn(s)
            .collect(as, new BiConsumer<List<Integer>, Integer>() {
                @Override
                public void accept(List<Integer> a, Integer b) throws Exception {
                    a.add(b);
                }
            })
            .doOnNext(new Consumer<List<Integer>>() {
                @Override
                public void accept(List<Integer> v) throws Exception {
                    System.out.println(v.size());
                }
            })
            .sequential()
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(3)
            .assertNoErrors()
            .assertComplete()
            ;

            List<List<Integer>> list = ts.values();

            Assert.assertEquals(1000, list.get(0).size() + list.get(1).size() + list.get(2).size());
        } finally {
            exec.shutdown();
        }
    }

    @Test
    public void emptySourceZeroRequest() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>(0);

        Flowable.range(1, 3).to(parallel(3)).sequential().subscribe(ts);

        ts.request(1);

        ts.assertValue(1);
    }
}
