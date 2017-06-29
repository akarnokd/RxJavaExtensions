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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.reactivestreams.*;

import hu.akarnokd.rxjava2.basetypes.*;
import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.Flowable;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableMapAsyncTest {

    enum DelayedMappers implements Function<Integer, Flowable<String>>, BiFunction<Integer, Object, String> {
        INSTANCE;
        @Override
        public Flowable<String> apply(Integer t) throws Exception {
            return Flowable.just(Integer.toString(t + 1)).delay(100, TimeUnit.MILLISECONDS);
        }

        @Override
        public String apply(Integer t1, Object t2) throws Exception {
            return t1 + "-" + t2;
        }
    }

    enum NonDelayedMappers implements Function<Integer, Flowable<String>>, BiFunction<Integer, Object, String> {
        INSTANCE;
        @Override
        public Flowable<String> apply(Integer t) throws Exception {
            return Flowable.just(Integer.toString(t + 1)).hide();
        }

        @Override
        public String apply(Integer t1, Object t2) throws Exception {
            return t1 + "-" + t2;
        }
    }

    enum NonDelayedFusedMappers implements Function<Integer, Flowable<String>>, BiFunction<Integer, Object, String> {
        INSTANCE;
        @Override
        public Flowable<String> apply(Integer t) throws Exception {
            return Flowable.just(Integer.toString(t + 1));
        }

        @Override
        public String apply(Integer t1, Object t2) throws Exception {
            return t1 + "-" + t2;
        }
    }

    @Test
    public void normal() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapAsync(DelayedMappers.INSTANCE))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("2", "3", "4", "5", "6");
    }


    @Test
    public void normalCombiner() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapAsync(DelayedMappers.INSTANCE, DelayedMappers.INSTANCE))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1-2", "2-3", "3-4", "4-5", "5-6");
    }

    @Test
    public void nonDelayed() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapAsync(NonDelayedMappers.INSTANCE))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("2", "3", "4", "5", "6");
    }


    @Test
    public void nonDelayedCombiner() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapAsync(NonDelayedMappers.INSTANCE, NonDelayedMappers.INSTANCE))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1-2", "2-3", "3-4", "4-5", "5-6");
    }

    @Test
    public void nonDelayedFused() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapAsync(NonDelayedFusedMappers.INSTANCE))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("2", "3", "4", "5", "6");
    }

    @Test
    public void nonDelayedFusedCombiner() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapAsync(NonDelayedFusedMappers.INSTANCE, NonDelayedFusedMappers.INSTANCE))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1-2", "2-3", "3-4", "4-5", "5-6");
    }

    @Test
    public void mainError() {
        Flowable.<Integer>error(new IOException())
        .compose(FlowableTransformers.mapAsync(NonDelayedMappers.INSTANCE))
        .test()
        .assertFailure(IOException.class);
    }


    @Test
    public void mainErrorBackpressured() {
        Flowable.<Integer>error(new IOException())
        .compose(FlowableTransformers.mapAsync(NonDelayedMappers.INSTANCE))
        .test(0)
        .assertFailure(IOException.class);
    }

    @Test
    public void innerError() {
        Flowable.just(1)
        .compose(FlowableTransformers.mapAsync(new Function<Object, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Object v) throws Exception {
                return Flowable.error(new IOException());
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void empty() {
        Flowable.<Integer>empty()
        .compose(FlowableTransformers.mapAsync(NonDelayedMappers.INSTANCE))
        .test()
        .assertResult();
    }


    @Test
    public void emptyBackpressured() {
        Flowable.<Integer>empty()
        .compose(FlowableTransformers.mapAsync(NonDelayedMappers.INSTANCE))
        .test(0)
        .assertResult();
    }

    @Test
    public void allInnerEmpty() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.mapAsync(new Function<Object, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Object v) throws Exception {
                return Flowable.empty().hide();
            }
        }))
        .test()
        .assertResult();
    }

    @Test
    public void allInnerEmptyFused() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.mapAsync(new Function<Object, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Object v) throws Exception {
                return Flowable.empty();
            }
        }))
        .test()
        .assertResult();
    }

    @Test
    public void mapperThrows() {
        Flowable.just(1)
        .compose(FlowableTransformers.mapAsync(new Function<Object, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Object v) throws Exception {
                throw new IOException();
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void mapperNull() {
        Flowable.just(1)
        .compose(FlowableTransformers.mapAsync(new Function<Object, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Object v) throws Exception {
                return null;
            }
        }))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void combinerThrows() {
        Flowable.just(1)
        .compose(FlowableTransformers.mapAsync(new Function<Object, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Object v) throws Exception {
                return Flowable.just(v).hide();
            }
        }, new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object a, Object b) throws Exception {
                throw new IOException();
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void combinerNull() {
        Flowable.just(1)
        .compose(FlowableTransformers.mapAsync(new Function<Object, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Object v) throws Exception {
                return Flowable.just(v).hide();
            }
        }, new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object a, Object b) throws Exception {
                return null;
            }
        }))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void combinerThrowsFusedMap() {
        Flowable.just(1)
        .compose(FlowableTransformers.mapAsync(new Function<Object, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Object v) throws Exception {
                return Flowable.just(v);
            }
        }, new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object a, Object b) throws Exception {
                throw new IOException();
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void combinerNullFusedMap() {
        Flowable.just(1)
        .compose(FlowableTransformers.mapAsync(new Function<Object, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Object v) throws Exception {
                return Flowable.just(v);
            }
        }, new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object a, Object b) throws Exception {
                return null;
            }
        }))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void take() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapAsync(NonDelayedMappers.INSTANCE))
        .take(3)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("2", "3", "4");
    }

    @Test
    public void cancel() {
        final PublishProcessor<Object> pp = PublishProcessor.create();

        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapAsync(new Function<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Integer v) throws Exception {
                return pp;
            }
        }))
        .test()
        .cancel();

        Assert.assertFalse(pp.hasSubscribers());
    }

    @Test
    public void longSource() {
        Flowable.range(1, 1000)
        .compose(FlowableTransformers.mapAsync(NonDelayedMappers.INSTANCE))
        .test()
        .assertValueCount(1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void longSourceFused() {
        Flowable.range(1, 1000)
        .compose(FlowableTransformers.mapAsync(NonDelayedFusedMappers.INSTANCE))
        .test()
        .assertValueCount(1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void take1Cancel() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>() {
            @Override
            public void onNext(Object t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.range(1, 1000)
        .compose(FlowableTransformers.mapAsync(NonDelayedFusedMappers.INSTANCE))
        .subscribe(ts);

        ts.assertResult("2");
    }

    @Test
    public void take1CancelBackpressured() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>(1) {
            @Override
            public void onNext(Object t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowable.range(1, 1000)
        .compose(FlowableTransformers.mapAsync(NonDelayedFusedMappers.INSTANCE))
        .subscribe(ts);

        ts.assertResult("2");
    }

    @Test
    public void backpressureExactlyOne() {
        Flowable.just(1)
        .compose(FlowableTransformers.mapAsync(NonDelayedFusedMappers.INSTANCE))
        .test(1)
        .assertResult("2");
    }

    @Test
    public void longSourceSingleStep() {
        Flowable.range(1, 1000)
        .compose(FlowableTransformers.mapAsync(NonDelayedMappers.INSTANCE))
        .rebatchRequests(1)
        .test()
        .assertValueCount(1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void oneAndErrorInner() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowable.just(1)
            .compose(FlowableTransformers.mapAsync(new Function<Object, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Object v) throws Exception {
                    return new Flowable<Integer>() {
                        @Override
                        public void subscribeActual(Subscriber<? super Integer> s) {
                            s.onSubscribe(new BooleanSubscription());
                            s.onNext(1);
                            s.onError(new IOException());
                        }
                    };
                }
            }, 16))
            .test()
            .assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void filterAllOut() {
        final int[] calls = { 0 };

        Flowable.range(1, 1000)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                calls[0]++;
            }
        })
        .compose(FlowableTransformers.mapAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Perhaps.empty();
            }
        }, 16))
        .flatMap(Functions.justFunction(Flowable.just(0)))
        .test()
        .assertResult();

        Assert.assertEquals(1000, calls[0]);
    }
}
