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

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.*;

import hu.akarnokd.rxjava3.test.*;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class FlowableSwitchOnFirstTest {

    @Test
    public void noSwitch() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.<Integer>switchOnFirst(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return false;
            }
        }, new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v) throws Exception {
                return null;
            }
        }))
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void noSwitchBackpressured() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.<Integer>switchOnFirst(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return false;
            }
        }, new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v) throws Exception {
                return null;
            }
        }))
        .test(0L)
        .assertEmpty()
        .requestMore(1)
        .assertValuesOnly(1)
        .requestMore(5)
        .assertValuesOnly(1, 2, 3, 4, 5, 6)
        .requestMore(4)
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void doSwitch() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
        .compose(FlowableTransformers.<Integer>switchOnFirst(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        }, new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v) throws Exception {
                return Flowable.range(2, 9);
            }
        }))
        .test();

        assertTrue(pp.hasSubscribers());

        assertTrue(pp.offer(1));

        assertFalse(pp.hasSubscribers());

        ts
        .assertResult(2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void doSwitchBackpressured() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
        .compose(FlowableTransformers.<Integer>switchOnFirst(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        }, new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v) throws Exception {
                return Flowable.range(2, 9);
            }
        }))
        .test(0);

        assertTrue(pp.hasSubscribers());

        assertFalse(pp.offer(1));

        ts.request(1);

        assertTrue(pp.offer(1));

        assertFalse(pp.hasSubscribers());

        ts.assertEmpty()
        .requestMore(5)
        .assertValuesOnly(2, 3, 4, 5, 6)
        .requestMore(4)
        .assertResult(2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void doSwitchBackpressured2() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
        .compose(FlowableTransformers.<Integer>switchOnFirst(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        }, new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v) throws Exception {
                return Flowable.range(2, 9);
            }
        }))
        .test(0);

        assertTrue(pp.hasSubscribers());

        assertFalse(pp.offer(1));

        ts.request(2);

        assertTrue(pp.offer(1));

        assertFalse(pp.hasSubscribers());

        ts.assertValuesOnly(2)
        .requestMore(4)
        .assertValuesOnly(2, 3, 4, 5, 6)
        .requestMore(4)
        .assertResult(2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void noSwitchEmpty() {
        final AtomicInteger predicateCalled = new AtomicInteger();
        final AtomicInteger selectorCalled = new AtomicInteger();

        Flowable.<Integer>empty()
        .compose(FlowableTransformers.<Integer>switchOnFirst(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                predicateCalled.incrementAndGet();
                return true;
            }
        }, new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v) throws Exception {
                selectorCalled.incrementAndGet();
                return Flowable.range(2, 9);
            }
        }))
        .test()
        .assertResult();

        assertEquals(0, predicateCalled.get());
        assertEquals(0, selectorCalled.get());
    }

    @Test
    public void noSwitchError() {
        final AtomicInteger predicateCalled = new AtomicInteger();
        final AtomicInteger selectorCalled = new AtomicInteger();

        Flowable.<Integer>error(new TestException())
        .compose(FlowableTransformers.<Integer>switchOnFirst(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                predicateCalled.incrementAndGet();
                return true;
            }
        }, new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v) throws Exception {
                selectorCalled.incrementAndGet();
                return Flowable.range(2, 9);
            }
        }))
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, predicateCalled.get());
        assertEquals(0, selectorCalled.get());
    }

    @Test
    public void doSwitchError() {
        final AtomicInteger predicateCalled = new AtomicInteger();
        final AtomicInteger selectorCalled = new AtomicInteger();

        Flowable.just(1)
        .compose(FlowableTransformers.<Integer>switchOnFirst(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                predicateCalled.incrementAndGet();
                return true;
            }
        }, new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v) throws Exception {
                selectorCalled.incrementAndGet();
                return Flowable.error(new TestException());
            }
        }))
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, predicateCalled.get());
        assertEquals(1, selectorCalled.get());
    }

    @Test
    public void noSwitchItemThenError() {
        final AtomicInteger predicateCalled = new AtomicInteger();
        final AtomicInteger selectorCalled = new AtomicInteger();

        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .compose(FlowableTransformers.<Integer>switchOnFirst(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                predicateCalled.incrementAndGet();
                return false;
            }
        }, new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v) throws Exception {
                selectorCalled.incrementAndGet();
                return Flowable.range(2, 9);
            }
        }))
        .test()
        .assertFailure(TestException.class, 1);

        assertEquals(1, predicateCalled.get());
        assertEquals(0, selectorCalled.get());
    }

    @Test
    public void predicateCrash() {
        final AtomicInteger predicateCalled = new AtomicInteger();
        final AtomicInteger selectorCalled = new AtomicInteger();
        final AtomicInteger cancelled = new AtomicInteger();

        Flowable.just(1)
        .doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                cancelled.incrementAndGet();
            }
        })
        .compose(FlowableTransformers.<Integer>switchOnFirst(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                predicateCalled.incrementAndGet();
                throw new TestException();
            }
        }, new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v) throws Exception {
                selectorCalled.incrementAndGet();
                return Flowable.range(2, 9);
            }
        }))
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, predicateCalled.get());
        assertEquals(0, selectorCalled.get());
        assertEquals(1, cancelled.get());
    }

    @Test
    public void selectorCrash() {
        final AtomicInteger predicateCalled = new AtomicInteger();
        final AtomicInteger selectorCalled = new AtomicInteger();
        final AtomicInteger cancelled = new AtomicInteger();

        Flowable.just(1)
        .doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                cancelled.incrementAndGet();
            }
        })
        .compose(FlowableTransformers.<Integer>switchOnFirst(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                predicateCalled.incrementAndGet();
                return true;
            }
        }, new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v) throws Exception {
                selectorCalled.incrementAndGet();
                throw new TestException();
            }
        }))
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, predicateCalled.get());
        assertEquals(1, selectorCalled.get());
        assertEquals(1, cancelled.get());
    }

    @Test
    public void doSwitchCancel() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Flowable.just(1)
        .compose(FlowableTransformers.<Integer>switchOnFirst(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        }, new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v) throws Exception {
                return pp;
            }
        }))
        .test();

        assertTrue(pp.hasSubscribers());

        ts.cancel();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void mainIgnoresCancelCompletes() {
        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                s.onNext(1);
                s.onNext(2);
                s.onComplete();
            }
        }
        .compose(FlowableTransformers.<Integer>switchOnFirst(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return true;
            }
        }, new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v) throws Exception {
                return Flowable.range(2, 9);
            }
        }))
        .test()
        .assertResult(2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void mainIgnoresCancelErrors() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onNext(2);
                    s.onError(new TestException());
                }
            }
            .compose(FlowableTransformers.<Integer>switchOnFirst(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
                    return true;
                }
            }, new Function<Integer, Publisher<? extends Integer>>() {
                @Override
                public Publisher<? extends Integer> apply(Integer v) throws Exception {
                    return Flowable.range(2, 9);
                }
            }))
            .test()
            .assertResult(2, 3, 4, 5, 6, 7, 8, 9, 10);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f) throws Exception {
                return f.compose(FlowableTransformers.<Object>switchOnFirst(new Predicate<Object>() {
                        @Override
                        public boolean test(Object v) throws Exception {
                            return true;
                        }
                    }, new Function<Object, Publisher<? extends Object>>() {
                        @Override
                        public Publisher<? extends Object> apply(Object v) throws Exception {
                            return Flowable.range(2, 9);
                        }
                    }));
            }
        });
    }
}
