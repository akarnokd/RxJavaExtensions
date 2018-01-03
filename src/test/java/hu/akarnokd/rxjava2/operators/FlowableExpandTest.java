/*
 * Copyright 2016-2017 David Karnok
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.reactivestreams.*;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableExpandTest {

    Function<Integer, Publisher<Integer>> countDown = new Function<Integer, Publisher<Integer>>() {
        @Override
        public Publisher<Integer> apply(Integer v) throws Exception {
            return v == 0 ? Flowable.<Integer>empty() : Flowable.just(v - 1);
        }
    };

    @Test
    public void recursiveCountdownDepth() {
        Flowable.just(10)
        .compose(FlowableTransformers.<Integer>expand(countDown, ExpandStrategy.DEPTH_FIRST))
        .test()
        .assertResult(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0);
    }

    @Test
    public void recursiveCountdownDefault() {
        Flowable.just(10)
        .compose(FlowableTransformers.<Integer>expand(countDown))
        .test()
        .assertResult(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0);
    }

    @Test
    public void recursiveCountdownBreadth() {
        Flowable.just(10)
        .compose(FlowableTransformers.<Integer>expand(countDown, ExpandStrategy.BREADTH_FIRST))
        .test()
        .assertResult(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0);
    }

    @Test
    public void error() {
        for (ExpandStrategy strategy : ExpandStrategy.values()) {
            Flowable.<Integer>error(new IOException())
            .compose(FlowableTransformers.expand(countDown, strategy))
            .test()
            .withTag(strategy.toString())
            .assertFailure(IOException.class);
        }
    }

    @Test
    public void errorDelayed() {
        for (ExpandStrategy strategy : ExpandStrategy.values()) {
            Flowable.<Integer>error(new IOException())
            .compose(FlowableTransformers.expandDelayError(countDown, strategy))
            .test()
            .withTag(strategy.toString())
            .assertFailure(IOException.class);
        }
    }

    @Test
    public void empty() {
        for (ExpandStrategy strategy : ExpandStrategy.values()) {
            Flowable.<Integer>empty()
            .compose(FlowableTransformers.expand(countDown, strategy))
            .test()
            .assertResult();
        }
    }

    @Test
    public void recursiveCountdown() {
        for (ExpandStrategy strategy : ExpandStrategy.values()) {
            for (int i = 0; i < 1000; i = (i < 100 ? i + 1 : i + 50)) {
                String tag = "i = " + i + ", strategy = " + strategy;

                TestSubscriber<Integer> ts = Flowable.just(i)
                .compose(FlowableTransformers.<Integer>expand(countDown, strategy))
                .test()
                .withTag(tag)
                .assertSubscribed()
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(i + 1);

                List<Integer> list = ts.values();
                for (int j = 0; j <= i; j++) {
                    Assert.assertEquals(tag + ", " + list, i - j, list.get(j).intValue());
                }
            }
        }
    }

    @Test
    public void recursiveCountdownTake() {
        for (ExpandStrategy strategy : ExpandStrategy.values()) {
            Flowable.just(10)
            .compose(FlowableTransformers.<Integer>expand(countDown, strategy))
            .take(5)
            .test()
            .withTag(strategy.toString())
            .assertResult(10, 9, 8, 7, 6);
        }
    }

    @Test
    public void recursiveCountdownBackpressure() {
        for (ExpandStrategy strategy : ExpandStrategy.values()) {
            Flowable.just(10)
            .compose(FlowableTransformers.<Integer>expand(countDown, strategy))
            .test(0L)
            .withTag(strategy.toString())
            .requestMore(1)
            .assertValues(10)
            .requestMore(3)
            .assertValues(10, 9, 8, 7)
            .requestMore(4)
            .assertValues(10, 9, 8, 7, 6, 5, 4, 3)
            .requestMore(3)
            .assertResult(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0);
        }
    }

    @Test
    public void expanderThrows() {
        for (ExpandStrategy strategy : ExpandStrategy.values()) {
            Flowable.just(10)
            .compose(FlowableTransformers.<Integer>expand(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    throw new IOException();
                }
            }, strategy))
            .test()
            .withTag(strategy.toString())
            .assertFailure(IOException.class, 10);
        }
    }

    @Test
    public void expanderReturnsNull() {
        for (ExpandStrategy strategy : ExpandStrategy.values()) {
            Flowable.just(10)
            .compose(FlowableTransformers.<Integer>expand(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    return null;
                }
            }, strategy))
            .test()
            .withTag(strategy.toString())
            .assertFailure(NullPointerException.class, 10);
        }
    }

    static final class Node {
        final String name;
        final List<Node> children;

        Node(String name, Node... nodes) {
            this.name = name;
            this.children = new ArrayList<Node>();
            for (Node n : nodes) {
                children.add(n);
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }

    Node createTest() {
        return new Node("root",
            new Node("1",
                new Node("11")
            ),
            new Node("2",
                new Node("21"),
                new Node("22",
                    new Node("221")
                )
            ),
            new Node("3",
                new Node("31"),
                new Node("32",
                    new Node("321")
                ),
                new Node("33",
                    new Node("331"),
                    new Node("332",
                        new Node("3321")
                    )
                )
            ),
            new Node("4",
                new Node("41"),
                new Node("42",
                    new Node("421")
                ),
                new Node("43",
                    new Node("431"),
                    new Node("432",
                        new Node("4321")
                    )
                ),
                new Node("44",
                    new Node("441"),
                    new Node("442",
                        new Node("4421")
                    ),
                    new Node("443",
                        new Node("4431"),
                        new Node("4432")
                    )
                )
            )
        );
    }

    @Test(timeout = 5000)
    public void depthFirst() {
        Node root = createTest();

        Flowable.just(root)
        .compose(FlowableTransformers.<Node>expand(new Function<Node, Publisher<Node>>() {
            @Override
            public Publisher<Node> apply(Node v) throws Exception {
                return Flowable.fromIterable(v.children);
            }
        }, ExpandStrategy.DEPTH_FIRST))
        .map(new Function<Node, String>() {
            @Override
            public String apply(Node v) throws Exception {
                return v.name;
            }
        })
        .test()
        .assertResult(
                "root",
                "1", "11",
                "2", "21", "22", "221",
                "3", "31", "32", "321", "33", "331", "332", "3321",
                "4", "41", "42", "421", "43", "431", "432", "4321",
                    "44", "441", "442", "4421", "443", "4431", "4432"
        );
    }

    @Test(timeout = 5000)
    public void depthFirstAsync() {
        Node root = createTest();

        Flowable.just(root)
        .compose(FlowableTransformers.<Node>expand(new Function<Node, Publisher<Node>>() {
            @Override
            public Publisher<Node> apply(Node v) throws Exception {
                return Flowable.fromIterable(v.children).subscribeOn(Schedulers.computation());
            }
        }, ExpandStrategy.DEPTH_FIRST))
        .map(new Function<Node, String>() {
            @Override
            public String apply(Node v) throws Exception {
                return v.name;
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(
                "root",
                "1", "11",
                "2", "21", "22", "221",
                "3", "31", "32", "321", "33", "331", "332", "3321",
                "4", "41", "42", "421", "43", "431", "432", "4321",
                    "44", "441", "442", "4421", "443", "4431", "4432"
        );
    }

    @Test(timeout = 5000)
    public void breadthFirst() {
        Node root = createTest();

        Flowable.just(root)
        .compose(FlowableTransformers.<Node>expand(new Function<Node, Publisher<Node>>() {
            @Override
            public Publisher<Node> apply(Node v) throws Exception {
                return Flowable.fromIterable(v.children);
            }
        }, ExpandStrategy.BREADTH_FIRST))
        .map(new Function<Node, String>() {
            @Override
            public String apply(Node v) throws Exception {
                return v.name;
            }
        })
        .test()
        .assertResult(
                "root",
                "1", "2", "3", "4",
                "11", "21", "22", "31", "32", "33", "41", "42", "43", "44",
                "221", "321", "331", "332", "421", "431", "432", "441", "442", "443",
                "3321", "4321", "4421", "4431", "4432"
        );
    }

    @Test(timeout = 5000)
    public void breadthFirstAsync() {
        Node root = createTest();

        Flowable.just(root)
        .compose(FlowableTransformers.<Node>expand(new Function<Node, Publisher<Node>>() {
            @Override
            public Publisher<Node> apply(Node v) throws Exception {
                return Flowable.fromIterable(v.children).subscribeOn(Schedulers.computation());
            }
        }, ExpandStrategy.BREADTH_FIRST))
        .map(new Function<Node, String>() {
            @Override
            public String apply(Node v) throws Exception {
                return v.name;
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(
                "root",
                "1", "2", "3", "4",
                "11", "21", "22", "31", "32", "33", "41", "42", "43", "44",
                "221", "321", "331", "332", "421", "431", "432", "441", "442", "443",
                "3321", "4321", "4421", "4431", "4432"
        );
    }

    @Test
    public void depthFirstCancel() {

        final PublishProcessor<Integer> pp = PublishProcessor.create();

        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        FlowableSubscriber<Integer> s = new FlowableSubscriber<Integer>() {

            Subscription upstream;

            @Override
            public void onSubscribe(Subscription s) {
                upstream = s;
                ts.onSubscribe(s);
            }

            @Override
            public void onNext(Integer t) {
                ts.onNext(t);
                upstream.cancel();
                upstream.request(1);
                onComplete();
            }

            @Override
            public void onError(Throwable t) {
                ts.onError(t);
            }

            @Override
            public void onComplete() {
                ts.onComplete();
            }
        };

        Flowable.just(1)
        .compose(FlowableTransformers.expand(Functions.justFunction(pp), ExpandStrategy.DEPTH_FIRST))
        .subscribe(s);

        Assert.assertFalse(pp.hasSubscribers());

        ts.assertResult(1);
    }

    @Test
    public void depthCancelRace() {
        for (int i = 0; i < 1000; i++) {
            final TestSubscriber<Integer> ts = Flowable.just(0)
            .compose(FlowableTransformers.<Integer>expand(countDown, ExpandStrategy.DEPTH_FIRST))
            .test(0);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.request(1);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void depthEmitCancelRace() {
        for (int i = 0; i < 1000; i++) {

            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = Flowable.just(0)
            .compose(FlowableTransformers.<Integer>expand(Functions.justFunction(pp), ExpandStrategy.DEPTH_FIRST))
            .test(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onNext(1);
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void depthCompleteCancelRace() {
        for (int i = 0; i < 1000; i++) {

            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = Flowable.just(0)
            .compose(FlowableTransformers.<Integer>expand(Functions.justFunction(pp), ExpandStrategy.DEPTH_FIRST))
            .test(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void depthCancelRace2() throws Exception {
        for (int i = 0; i < 1000; i++) {

            final PublishProcessor<Integer> pp = PublishProcessor.create();

            Flowable<Integer> source = Flowable.just(0)
            .compose(FlowableTransformers.<Integer>expand(Functions.justFunction(pp), ExpandStrategy.DEPTH_FIRST));

            final CountDownLatch cdl = new CountDownLatch(1);

            TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
                final AtomicInteger sync = new AtomicInteger(2);

                @Override
                public void onNext(Integer t) {
                    super.onNext(t);
                    Schedulers.single().scheduleDirect(new Runnable() {
                        @Override
                        public void run() {
                            if (sync.decrementAndGet() != 0) {
                                while (sync.get() != 0) { }
                            }
                            cancel();
                            cdl.countDown();
                        }
                    });
                    if (sync.decrementAndGet() != 0) {
                        while (sync.get() != 0) { }
                    }
                }
            };

            source.subscribe(ts);

            Assert.assertTrue(cdl.await(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void multipleRoots() {
        Flowable.just(10, 5)
        .compose(FlowableTransformers.expand(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v)
                    throws Exception {
                        if (v == 9) {
                            return Flowable.error(new IOException("error"));
                        } else if (v == 0) {
                            return Flowable.empty();
                        } else {
                            return Flowable.just(v - 1);
                        }
                    }
        }, ExpandStrategy.DEPTH_FIRST))
        .test()
        .assertFailureAndMessage(IOException.class, "error", 10, 9);
    }

    @Test
    public void multipleRootsDelayError() {
        Flowable.just(10, 5)
        .compose(FlowableTransformers.expandDelayError(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v)
                    throws Exception {
                        if (v == 9) {
                            return Flowable.error(new IOException("error"));
                        } else if (v == 0) {
                            return Flowable.empty();
                        } else {
                            return Flowable.just(v - 1);
                        }
                    }
        }, ExpandStrategy.DEPTH_FIRST))
        .test()
        .assertFailureAndMessage(IOException.class, "error", 10, 9, 5, 4, 3, 2, 1, 0);
    }

    @Test
    public void multipleRootsBreadth() {
        Flowable.just(10, 5)
        .compose(FlowableTransformers.expand(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v)
                    throws Exception {
                        if (v == 9) {
                            return Flowable.error(new IOException("error"));
                        } else if (v == 0) {
                            return Flowable.empty();
                        } else {
                            return Flowable.just(v - 1);
                        }
                    }
        }, ExpandStrategy.BREADTH_FIRST))
        .test()
        .assertFailureAndMessage(IOException.class, "error", 10, 5, 9, 4);
    }

    @Test
    public void multipleRootsDelayErrorBreadth() {
        Flowable.just(10, 5)
        .compose(FlowableTransformers.expandDelayError(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v)
                    throws Exception {
                        if (v == 9) {
                            return Flowable.error(new IOException("error"));
                        } else if (v == 0) {
                            return Flowable.empty();
                        } else {
                            return Flowable.just(v - 1);
                        }
                    }
        }, ExpandStrategy.BREADTH_FIRST))
        .test()
        .assertFailureAndMessage(IOException.class, "error", 10, 5, 9, 4, 3, 2, 1, 0);
    }
}
