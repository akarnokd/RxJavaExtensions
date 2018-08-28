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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

import org.junit.*;
import org.reactivestreams.*;

import hu.akarnokd.rxjava2.basetypes.Solo;
import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.Flowable;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableFilterAsyncTest {

    @Test
    public void normal() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.just(v % 2 == 0).delay(100, TimeUnit.MILLISECONDS);
            }
        }))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void normalSync() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.just(v % 2 == 0).hide();
            }
        }))
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void normalSyncFused() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.just(v % 2 == 0);
            }
        }))
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void allEmpty() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.<Boolean>empty().hide();
            }
        }))
        .test()
        .assertResult();
    }

    @Test
    public void allEmptyFused() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.<Boolean>empty();
            }
        }))
        .test()
        .assertResult();
    }

    @Test
    public void empty() {
        Flowable.<Integer>empty()
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.<Boolean>just(true);
            }
        }))
        .test()
        .assertResult();
    }

    @Test
    public void emptyBackpressured() {
        Flowable.<Integer>empty()
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.<Boolean>just(true);
            }
        }))
        .test(0L)
        .assertResult();
    }

    @Test
    public void error() {
        Flowable.<Integer>error(new IOException())
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.<Boolean>just(true);
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void errorBackpressured() {
        Flowable.<Integer>error(new IOException())
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.<Boolean>just(true);
            }
        }))
        .test(0L)
        .assertFailure(IOException.class);
    }

    @Test
    public void backpressureExactlyOne() {
        Flowable.just(1)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.<Boolean>just(true);
            }
        }))
        .test(1)
        .assertResult(1);
    }

    @Test
    public void longSourceSingleStep() {
        Flowable.range(1, 1000)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.<Boolean>just(true);
            }
        }))
        .rebatchRequests(1)
        .test()
        .assertValueCount(1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void longSource() {
        Flowable.range(1, 1000)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.<Boolean>just(true).hide();
            }
        }))
        .test()
        .assertValueCount(1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void longSourceFused() {
        Flowable.range(1, 1000)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.<Boolean>just(true);
            }
        }))
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
            .compose(FlowableTransformers.filterAsync(new Function<Object, Publisher<Boolean>>() {
                @Override
                public Publisher<Boolean> apply(Object v) throws Exception {
                    return new Flowable<Boolean>() {
                        @Override
                        public void subscribeActual(Subscriber<? super Boolean> s) {
                            s.onSubscribe(new BooleanSubscription());
                            s.onNext(true);
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
    public void predicateThrows() {
        Flowable.just(1)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                throw new IOException();
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void predicateNull() {
        Flowable.just(1)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return null;
            }
        }))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void predicateError() {
        Flowable.just(1)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.<Boolean>error(new IOException()).hide();
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void predicateErrorFused() {
        Flowable.just(1)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.fromCallable(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        throw new IOException();
                    }
                });
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void take() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.just(v % 2 == 0).hide();
            }
        }))
        .take(1)
        .test()
        .assertResult(2);
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
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.just(true).hide();
            }
        }))
        .subscribe(ts);

        ts.assertResult(1);
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
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Flowable.just(true).hide();
            }
        }))
        .subscribe(ts);

        ts.assertResult(1);
    }

    @Test
    public void cancel() {
        final PublishProcessor<Boolean> pp = PublishProcessor.create();

        Flowable.range(1, 5)
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return pp;
            }
        }, 16))
        .test()
        .cancel();

        Assert.assertFalse(pp.hasSubscribers());
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
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Solo.just(false);
            }
        }, 16))
        .flatMap(Functions.justFunction(Flowable.just(0)))
        .test()
        .assertResult();

        Assert.assertEquals(1000, calls[0]);
    }

    @Test
    public void filterAllOutHidden() {
        final int[] calls = { 0 };

        Flowable.range(1, 1000)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                calls[0]++;
            }
        })
        .compose(FlowableTransformers.filterAsync(new Function<Integer, Publisher<Boolean>>() {
            @Override
            public Publisher<Boolean> apply(Integer v) throws Exception {
                return Solo.just(false).hide();
            }
        }, 16))
        .flatMap(Functions.justFunction(Flowable.just(0)))
        .test()
        .assertResult();

        Assert.assertEquals(1000, calls[0]);
    }
}
