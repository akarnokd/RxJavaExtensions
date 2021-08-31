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

package hu.akarnokd.rxjava3.basetypes;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.reactivestreams.*;

import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.exceptions.CompositeException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.operators.QueueSubscription;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class NonoTest implements Action, Consumer<Object>, LongConsumer, Cancellable {

    volatile int count;

    final Nono ioError = Nono.error(new IOException());

    @Override
    public void run() throws Exception {
        count++;
    }

    @Override
    public void accept(Object t) throws Exception {
        count++;
    }

    @Override
    public void cancel() throws Exception {
        count++;
    }

    @Override
    public void accept(long t) throws Exception {
        count++;
    }

    @Test
    public void complete() {
        Nono.complete()
        .test()
        .assertResult();
    }

    @Test
    public void never() {
        Nono.never()
        .test()
        .assertEmpty();
    }

    @Test
    public void error() {
        Nono.error(new IOException())
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void errorSupplier() {
        Nono.error(new Supplier<Throwable>() {
            @Override
            public Throwable get() throws Exception {
                return new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void errorSupplierThrows() {
        Nono.error(new Supplier<Throwable>() {
            @Override
            public Throwable get() throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void defer() {
        final int[] counter = { 0 };

        Nono np = Nono.defer(new Supplier<Nono>() {
            @Override
            public Nono get() throws Exception {
                if (counter[0]++ == 0) {
                    return Nono.error(new IOException());
                }
                return Nono.error(new IllegalArgumentException());
            }
        });

        Assert.assertEquals(0, counter[0]);

        np.test().assertFailure(IOException.class);

        Assert.assertEquals(1, counter[0]);

        np.test().assertFailure(IllegalArgumentException.class);

        Assert.assertEquals(2, counter[0]);

        np.test().assertFailure(IllegalArgumentException.class);

        Assert.assertEquals(3, counter[0]);
    }

    @Test
    public void deferThrows() {
        Nono np = Nono.defer(new Supplier<Nono>() {
            @Override
            public Nono get() throws Exception {
                throw new IOException();
            }
        });

        np.test().assertFailure(IOException.class);
    }

    @Test
    public void fromAction() {
        final int[] counter = { 0 };

        Nono np = Nono.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                counter[0]++;
            }
        });

        Assert.assertEquals(0, counter[0]);

        np.test().assertResult();

        Assert.assertEquals(1, counter[0]);

        np.test().assertResult();

        Assert.assertEquals(2, counter[0]);
    }

    @Test
    public void fromActionThrows() {
        Nono np = Nono.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                throw new IOException();
            }
        });

        np.test().assertFailure(IOException.class);
    }

    @Test
    public void fromFuture() {
        final int[] counter = { 0 };

        FutureTask<Void> ft = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                counter[0]++;
                return null;
            }
        });
        Schedulers.single().scheduleDirect(ft, 200, TimeUnit.MILLISECONDS);

        Nono.fromFuture(ft)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();

        Assert.assertEquals(1, counter[0]);
    }

    @Test
    public void fromFutureThrows() {
        final int[] counter = { 0 };

        FutureTask<Void> ft = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                counter[0]++;
                throw new IOException();
            }
        });
        Schedulers.single().scheduleDirect(ft, 200, TimeUnit.MILLISECONDS);

        Nono.fromFuture(ft)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);

        Assert.assertEquals(1, counter[0]);
    }

    @Test
    public void fromFutureWithTimeout() {
        final int[] counter = { 0 };

        FutureTask<Void> ft = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                counter[0]++;
                return null;
            }
        });
        Schedulers.single().scheduleDirect(ft, 200, TimeUnit.MILLISECONDS);

        Nono.fromFuture(ft, 2, TimeUnit.SECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();

        Assert.assertEquals(1, counter[0]);
    }

    @Test
    public void fromFutureWithTimeoutError() {
        final int[] counter = { 0 };

        FutureTask<Void> ft = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                counter[0]++;
                throw new IOException();
            }
        });
        Schedulers.single().scheduleDirect(ft, 200, TimeUnit.MILLISECONDS);

        Nono.fromFuture(ft, 2, TimeUnit.SECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);

        Assert.assertEquals(1, counter[0]);
    }

    @Test
    public void fromFutureWithTimeoutDoTimeout() {
        final int[] counter = { 0 };

        FutureTask<Void> ft = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                counter[0]++;
                return null;
            }
        });
        Schedulers.single().scheduleDirect(ft, 1, TimeUnit.SECONDS);

        Nono.fromFuture(ft, 1, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);

        Assert.assertEquals(0, counter[0]);
    }

    @Test
    public void amb1() {
        Nono.amb(Arrays.asList(Nono.complete(), Nono.never()))
        .test()
        .assertResult();
    }

    @Test
    public void amb2() {
        Nono.amb(Arrays.asList(Nono.never(), Nono.complete()))
        .test()
        .assertResult();
    }

    @Test
    public void amb1Error() {
        Nono.amb(Arrays.asList(Nono.error(new IOException()), Nono.never(), Nono.never()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void amb2Error() {
        Nono.amb(Arrays.asList(Nono.never(), Nono.error(new IOException()), Nono.never()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void ambArray1() {
        Nono.ambArray(Nono.complete(), Nono.never())
        .test()
        .assertResult();
    }

    @Test
    public void ambArray2() {
        Nono.ambArray(Nono.never(), Nono.complete())
        .test()
        .assertResult();
    }

    @Test
    public void ambArray1Error() {
        Nono.ambArray(Nono.error(new IOException()), Nono.never(), Nono.never())
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void ambArray2Error() {
        Nono.ambArray(Nono.never(), Nono.error(new IOException()), Nono.never())
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void concatIterable() {
        Nono.concat(Arrays.asList(Nono.fromAction(this), Nono.fromAction(this)))
        .test()
        .assertResult();

        Assert.assertEquals(2, count);
    }

    @Test
    public void concatPublisher() {
        Nono.concat(Flowable.fromArray(Nono.fromAction(this), Nono.fromAction(this)))
        .test()
        .assertResult();

        Assert.assertEquals(2, count);
    }

    @Test
    public void concatPublisher2() {
        Nono.concat(Flowable.fromArray(
                Nono.fromAction(this), Nono.fromAction(this)), 2)
        .test()
        .assertResult();

        Assert.assertEquals(2, count);
    }

    @Test
    public void concatArray() {
        Nono.concatArray(Nono.fromAction(this), Nono.fromAction(this))
        .test()
        .assertResult();

        Assert.assertEquals(2, count);
    }

    @Test
    public void concatIterableError() {
        Nono.concat(Arrays.asList(Nono.fromAction(this), Nono.error(new IOException())))
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void concatPublisherError() {
        Nono.concat(Flowable.fromArray(Nono.fromAction(this), Nono.error(new IOException())))
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void concatPublisherMainError() {
        Nono.concat(Flowable.<Nono>error(new IOException()))
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(0, count);
    }

    @Test
    public void concatArrayError() {
        Nono.concatArray(Nono.fromAction(this), Nono.error(new IOException()))
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void concatIterableDelayError() {
        Nono.concatDelayError(
                Arrays.asList(Nono.fromAction(this),
                        Nono.error(new IOException()),
                        Nono.fromAction(this)))
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(2, count);
    }

    @Test
    public void concatPublisherDelayError() {
        Nono.concatDelayError(
                Flowable.fromArray(Nono.fromAction(this),
                        Nono.error(new IOException()),
                        Nono.fromAction(this)))
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(2, count);
    }

    @Test
    public void concatPublisherDelayErrorMain() {
        Nono.concatDelayError(
                Flowable.fromArray(
                        Nono.fromAction(this),
                        Nono.fromAction(this))
                .concatWith(Flowable.<Nono>error(new IOException()))
                )
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(2, count);
    }

    @Test
    public void concatPublisherDelayErrorMainBoundary() {
        Nono.concatDelayError(
                Flowable.fromArray(Nono.fromAction(this),
                        Nono.error(new IOException()),
                        Nono.fromAction(this)), 1, false)
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void concatArrayDelayError() {
        Nono.concatArrayDelayError(
                Nono.fromAction(this),
                Nono.error(new IOException()),
                Nono.fromAction(this))
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(2, count);
    }

    @Test
    public void mergeIterable() {
        Nono.merge(Arrays.asList(
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(100, TimeUnit.MILLISECONDS)
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void mergeIterableMaxConcurrent() {
        Nono.merge(Arrays.asList(
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(100, TimeUnit.MILLISECONDS)
        ), 1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void mergeIterablePublisher() {
        Nono.merge(Flowable.range(1, 3)
                .map(new Function<Integer, Nono>() {
                    @Override
                    public Nono apply(Integer v) throws Exception {
                        return Nono.complete().delay(50, TimeUnit.MILLISECONDS);
                    }
                }))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void mergeIterablePublisherMaxConcurrent() {
        Nono.merge(Flowable.range(1, 3)
                .map(new Function<Integer, Nono>() {
                    @Override
                    public Nono apply(Integer v) throws Exception {
                        return Nono.complete().delay(50, TimeUnit.MILLISECONDS);
                    }
                }), 1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void mergeArray() {
        Nono.mergeArray(
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(100, TimeUnit.MILLISECONDS)
        )
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void mergeArrayMaxConcurrent() {
        Nono.mergeArray(1,
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(100, TimeUnit.MILLISECONDS)
        )
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void mergeIterableDelayError() {
        Nono.mergeDelayError(Arrays.asList(
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(100, TimeUnit.MILLISECONDS)
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void mergeIterableMaxConcurrentDelayError() {
        Nono.mergeDelayError(Arrays.asList(
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(100, TimeUnit.MILLISECONDS)
        ), 1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void mergeIterablePublisherDelayError() {
        Nono.mergeDelayError(Flowable.range(1, 3)
                .map(new Function<Integer, Nono>() {
                    @Override
                    public Nono apply(Integer v) throws Exception {
                        return Nono.complete().delay(50, TimeUnit.MILLISECONDS);
                    }
                }))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void mergeIterablePublisherMaxConcurrentDelayError() {
        Nono.mergeDelayError(Flowable.range(1, 3)
                .map(new Function<Integer, Nono>() {
                    @Override
                    public Nono apply(Integer v) throws Exception {
                        return Nono.complete().delay(50, TimeUnit.MILLISECONDS);
                    }
                }), 1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void mergeArrayDelayError() {
        Nono.mergeArrayDelayError(
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(100, TimeUnit.MILLISECONDS)
        )
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void mergeArrayMaxConcurrentDelayError() {
        Nono.mergeArrayDelayError(1,
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(100, TimeUnit.MILLISECONDS)
        )
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void mergeIterableDelayErrorError() {
        Nono.mergeDelayError(Arrays.asList(
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                ioError.delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(100, TimeUnit.MILLISECONDS)
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void mergeIterableMaxConcurrentDelayErrorError() {
        Nono.mergeDelayError(Arrays.asList(
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                ioError.delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(100, TimeUnit.MILLISECONDS)
        ), 1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void mergeIterablePublisherDelayErrorError() {
        Nono.mergeDelayError(Flowable.range(1, 3)
                .map(new Function<Integer, Nono>() {
                    @Override
                    public Nono apply(Integer v) throws Exception {
                        if (v == 2) {
                            return ioError;
                        }
                        return Nono.complete().delay(50, TimeUnit.MILLISECONDS);
                    }
                }))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void mergeIterablePublisherMaxConcurrentDelayErrorError() {
        Nono.mergeDelayError(Flowable.range(1, 3)
                .map(new Function<Integer, Nono>() {
                    @Override
                    public Nono apply(Integer v) throws Exception {
                        if (v == 2) {
                            return ioError;
                        }
                        return Nono.complete().delay(50, TimeUnit.MILLISECONDS);
                    }
                }), 1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void mergeArrayDelayErrorError() {
        Nono.mergeArrayDelayError(
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                ioError.delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(100, TimeUnit.MILLISECONDS)
        )
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void mergeArrayMaxConcurrentDelayErrorError() {
        Nono.mergeArrayDelayError(1,
                Nono.complete().delay(50, TimeUnit.MILLISECONDS),
                ioError.delay(50, TimeUnit.MILLISECONDS),
                Nono.complete().delay(100, TimeUnit.MILLISECONDS)
        )
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void timer() {
        Nono.timer(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void timerScheduler() {
        Nono.timer(100, TimeUnit.MILLISECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void using() {
        Nono.using(
                Functions.justSupplier(1),
                Functions.justFunction(Nono.complete()),
                this
        )
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void usingError() {
        Nono.using(
                Functions.justSupplier(1),
                Functions.justFunction(ioError),
                this
        )
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void usingNonEager() {
        Nono.using(
                Functions.justSupplier(1),
                Functions.justFunction(Nono.complete()),
                this, false
        )
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void usingNonEagerError() {
        Nono.using(
                Functions.justSupplier(1),
                Functions.justFunction(ioError),
                this, false
        )
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void usingResourceThrows() {
        Nono.using(new Supplier<Object>() {
                @Override
                public Object get() throws Exception {
                    throw new IllegalArgumentException();
                }
            },
            Functions.justFunction(Nono.complete()),
            this
        )
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void usingSourceThrows() {
        Nono.using(Functions.justSupplier(0),
                new Function<Integer, Nono>() {
                    @Override
                    public Nono apply(Integer v) throws Exception {
                        throw new IllegalArgumentException();
                    }
                },
                this
        )
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void usingDisposerThrows1() {
        Nono.using(Functions.justSupplier(0),
                Functions.justFunction(Nono.complete()),
                new Consumer<Integer>() {
                    @Override
                    public void accept(Integer t) throws Exception {
                        throw new IllegalArgumentException();
                    }
                }
        )
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void usingDisposerThrows2() {
        TestSubscriber<Void> ts = Nono.using(Functions.justSupplier(0),
                Functions.justFunction(ioError),
                new Consumer<Integer>() {
                    @Override
                    public void accept(Integer t) throws Exception {
                        throw new IllegalArgumentException();
                    }
                }
        )
        .test()
        .assertFailure(CompositeException.class)
        ;

        TestHelper.assertCompositeExceptions(ts, IOException.class, IllegalArgumentException.class);
    }

    @Test
    public void usingDisposerThrows3() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Nono.using(Functions.justSupplier(0),
                    Functions.justFunction(Nono.complete()),
                    new Consumer<Integer>() {
                        @Override
                        public void accept(Integer t) throws Exception {
                            throw new IllegalArgumentException();
                        }
                    }, false
            )
            .test()
            .assertResult();

            TestHelper.assertError(errors, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void usingDisposerThrows4() {
        TestSubscriber<Void> ts = Nono.using(Functions.justSupplier(0),
                new Function<Integer, Nono>() {
                    @Override
                    public Nono apply(Integer v) throws Exception {
                        throw new IOException();
                    }
                },
                new Consumer<Integer>() {
                    @Override
                    public void accept(Integer t) throws Exception {
                        throw new IllegalArgumentException();
                    }
                }
        )
        .test()
        .assertFailure(CompositeException.class)
        ;

        TestHelper.assertCompositeExceptions(ts,
                IOException.class, IllegalArgumentException.class);
    }

    @Test
    public void usingDisposerThrows5() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Nono.using(Functions.justSupplier(0),
                    new Function<Integer, Nono>() {
                        @Override
                        public Nono apply(Integer v) throws Exception {
                            throw new IOException();
                        }
                    },
                    new Consumer<Integer>() {
                        @Override
                        public void accept(Integer t) throws Exception {
                            throw new IllegalArgumentException();
                        }
                    }, false
            )
            .test()
            .assertFailure(IOException.class);

            TestHelper.assertError(errors, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void fromPublisher1() {
        Nono.fromPublisher(Flowable.empty())
        .test()
        .assertResult();
    }

    @Test
    public void fromPublisher2() {
        Nono.fromPublisher(Flowable.just(1))
        .test()
        .assertResult();
    }

    @Test
    public void fromPublisher3() {
        Nono.fromPublisher(Flowable.range(1, 3))
        .test()
        .assertResult();
    }

    @Test
    public void fromPublisher4() {
        Nono.fromPublisher(Flowable.never())
        .test()
        .assertEmpty();
    }

    @Test
    public void fromPublisher5() {
        Nono.fromPublisher(Flowable.error(new IOException()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void fromPublisher6() {
        Nono np = Nono.fromPublisher(Nono.complete());
        np
        .test()
        .assertResult();

        Assert.assertSame(np, Nono.complete());
    }

    @Test
    public void fromSingle1() {
        Nono.fromSingle(Single.just(1))
        .test()
        .assertResult();
    }

    @Test
    public void fromSingle2() {
        Nono.fromSingle(Single.error(new IOException()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void fromMaybe1() {
        Nono.fromMaybe(Maybe.just(1))
        .test()
        .assertResult();
    }

    @Test
    public void fromMaybe2() {
        Nono.fromMaybe(Maybe.error(new IOException()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void fromMaybe3() {
        Nono.fromMaybe(Maybe.empty())
        .test()
        .assertResult();
    }

    @Test
    public void fromCompletable1() {
        Nono.fromCompletable(Completable.complete())
        .test()
        .assertResult();
    }

    @Test
    public void fromCompletable2() {
        Nono.fromCompletable(Completable.error(new IOException()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void fromObservable1() {
        Nono.fromObservable(Observable.empty())
        .test()
        .assertResult();
    }

    @Test
    public void fromObservable2() {
        Nono.fromObservable(Observable.just(1))
        .test()
        .assertResult();
    }

    @Test
    public void fromObservable3() {
        Nono.fromObservable(Observable.range(1, 3))
        .test()
        .assertResult();
    }

    @Test
    public void fromObservable4() {
        Nono.fromObservable(Observable.never())
        .test()
        .assertEmpty();
    }

    @Test
    public void fromObservable5() {
        Nono.fromObservable(Observable.error(new IOException()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void andThen() {
        Nono.complete().andThen(Flowable.range(1, 5))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void andThenError() {
        ioError.andThen(Flowable.range(1, 5))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void andThenBackpressure() {
        Nono.complete().andThen(Flowable.range(1, 5))
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void andThenNono1() {
        Nono.fromAction(this).andThen(Nono.fromAction(this))
        .test()
        .assertResult();

        Assert.assertEquals(2, count);
    }

    @Test
    public void andThenNono2() {
        Nono.fromAction(this).andThen(ioError)
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void andThenNono3() {
        ioError.andThen(Nono.fromAction(this))
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(0, count);
    }

    @Test
    public void delaySubscription1() {
        Nono.fromAction(this).delaySubscription(Nono.complete())
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void delaySubscription2() {
        Nono.fromAction(this).delaySubscription(Flowable.range(1, 2))
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void delaySubscription3() {
        Nono.fromAction(this).delaySubscription(Flowable.just(1))
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void delaySubscription4() {
        Nono.fromAction(this).delaySubscription(Flowable.error(new IOException()))
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(0, count);
    }

    @Test
    public void delaySubscription5() {
        ioError.delaySubscription(Flowable.just(1))
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(0, count);
    }

    @Test
    public void delaySubscriptionTime1() {
        Nono.fromAction(this).delaySubscription(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void delaySubscriptionTime2() {
        Nono.fromAction(this).delaySubscription(100, TimeUnit.MILLISECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void delaySubscriptionTime3() {
        ioError.delaySubscription(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void timeout1() {
        Nono.fromAction(this).timeout(Flowable.never())
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void timeout2() {
        Nono.fromAction(this).timeout(Flowable.empty(), Nono.fromAction(this))
        .test()
        .assertResult();

        Assert.assertEquals(2, count);
    }

    @Test
    public void timeout3() {
        Nono.fromAction(this).timeout(Flowable.error(new IOException()))
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void timeout4() {
        Nono.fromAction(this).timeout(Flowable.empty())
        .test()
        .assertFailure(TimeoutException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void timeout5() {
        Nono.fromAction(this)
        .delay(10, TimeUnit.MILLISECONDS)
        .timeout(1, TimeUnit.SECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void timeout6() {
        Nono.fromAction(this)
        .delay(1, TimeUnit.SECONDS)
        .timeout(10, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void timeout7() {
        Nono.fromAction(this)
        .delay(1, TimeUnit.SECONDS)
        .timeout(10, TimeUnit.MILLISECONDS, Nono.fromAction(this))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();

        Assert.assertEquals(2, count);
    }

    @Test
    public void timeout8() {
        Nono.fromAction(this)
        .delay(1, TimeUnit.SECONDS)
        .timeout(10, TimeUnit.MILLISECONDS, Schedulers.single(),
                Nono.fromAction(this))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();

        Assert.assertEquals(2, count);
    }

    @Test
    public void timeout9() {
        Nono.fromAction(this)
        .delay(10, TimeUnit.MILLISECONDS)
        .timeout(1, TimeUnit.SECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void timeout10() {
        Nono.fromAction(this)
        .delay(1, TimeUnit.SECONDS)
        .timeout(10, TimeUnit.MILLISECONDS, ioError)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void timeout11() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        ioError.timeout(pp)
        .test()
        .assertFailure(IOException.class);

        Assert.assertFalse(pp.hasSubscribers());
    }

    @Test
    public void timeout12() {
        ioError.timeout(1, TimeUnit.SECONDS)
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void onErrorComplete() {
        Nono.fromAction(this).onErrorComplete()
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void onErrorCompleteError() {
        ioError.onErrorComplete()
        .test()
        .assertResult();
    }

    @Test
    public void mapError() {
        Nono.fromAction(this).mapError(Functions.justFunction(new IllegalArgumentException()))
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void mapErrorError() {
        ioError.mapError(Functions.justFunction(new IllegalArgumentException()))
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void onErrorResumeNext() {
        Nono.fromAction(this).onErrorResumeNext(Functions.justFunction(Nono.fromAction(this)))
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void onErrorResumeNextError1() {
        ioError.onErrorResumeNext(Functions.justFunction(Nono.fromAction(this)))
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void onErrorResumeNextError2() {
        ioError.onErrorResumeNext(Functions.justFunction(Nono.error(new IllegalArgumentException())))
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void flatMap1() {
        Nono.fromAction(this).flatMap(Functions.justFunction(Flowable.just(1)), Functions.justSupplier(Flowable.just(2)))
        .test()
        .assertResult(2);
    }

    @Test
    public void flatMap2() {
        Nono.fromAction(this).flatMap(Functions.justFunction(Flowable.just(1)), Functions.justSupplier(Flowable.range(10, 5)))
        .rebatchRequests(1)
        .test()
        .assertResult(10, 11, 12, 13, 14);

        Assert.assertEquals(1, count);
    }

    @Test
    public void flatMap3() {
        ioError.flatMap(Functions.justFunction(Flowable.just(1)), Functions.justSupplier(Flowable.just(2)))
        .test()
        .assertResult(1);
    }

    @Test
    public void flatMap4() {
        ioError.flatMap(
                Functions.justFunction(Flowable.error(new IllegalArgumentException())),
                Functions.justSupplier(Flowable.just(2)))
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void flatMap5() {
        Nono.fromAction(this).flatMap(
                Functions.justFunction(Flowable.just(1)),
                Functions.justSupplier(Flowable.error(new IllegalArgumentException()))
        )
        .test()
        .assertFailure(IllegalArgumentException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void compose() {
        Nono.fromAction(this).compose(Functions.justFunction(Nono.complete()))
        .test()
        .assertResult();

        Assert.assertEquals(0, count);
    }

    @Test
    public void toError() {
        try {
            Nono.fromAction(this).to(new Function<Nono, Object>() {
                @Override
                public Object apply(Nono np) throws Exception {
                    throw new IOException();
                }
            });
        } catch (RuntimeException ex) {
            Assert.assertTrue(ex.toString(), ex.getCause() instanceof IOException);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void toError2() {
        Nono.fromAction(this).to(new Function<Nono, Object>() {
            @Override
            public Object apply(Nono np) throws Exception {
                throw new IllegalArgumentException();
            }
        });
    }

    @Test
    public void lift() {
        Nono.fromAction(this).lift(Functions.<Subscriber<? super Void>>identity())
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void toFlowable() {
        Nono.fromAction(this).toFlowable()
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void toFlowableError() {
        ioError.toFlowable()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void toObservable() {
        Nono.fromAction(this).toObservable()
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void toObservableError() {
        ioError.toObservable()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void toMaybe() {
        Nono.fromAction(this).toMaybe()
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void toMaybeError() {
        ioError.toMaybe()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void toCompletable() {
        Nono.fromAction(this).toCompletable()
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void toCompletableError() {
        ioError.toCompletable()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void subscribeOn() {
        String main = Thread.currentThread().getName();
        final String[] on = { null };

        Nono.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                on[0] = Thread.currentThread().getName();
            }
        }).subscribeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();

        Assert.assertNotNull(on[0]);
        Assert.assertNotEquals(main, on[0]);
    }

    @Test
    public void subscribeOnError() {
        String main = Thread.currentThread().getName();
        final String[] on = { null };

        Nono.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                on[0] = Thread.currentThread().getName();
                throw new IOException();
            }
        }).subscribeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);

        Assert.assertNotNull(on[0]);
        Assert.assertNotEquals(main, on[0]);
    }

    @Test
    public void observeOn() {
        String main = Thread.currentThread().getName();
        final String[] on = { null };

        Nono.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                on[0] = Thread.currentThread().getName();
            }
        }).observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();

        Assert.assertNotNull(on[0]);
        Assert.assertEquals(main, on[0]);
    }

    @Test
    public void observeOnError() {
        String main = Thread.currentThread().getName();
        final String[] on = { null };

        Nono.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                on[0] = Thread.currentThread().getName();
                throw new IOException();
            }
        }).observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);

        Assert.assertNotNull(on[0]);
        Assert.assertEquals(main, on[0]);
    }

    @Test
    public void unsubscribeOn() throws Exception {
        String main = Thread.currentThread().getName();
        final String[] on = { null };

        final CountDownLatch cdl = new CountDownLatch(1);

        Nono.never()
        .doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                on[0] = Thread.currentThread().getName();
                cdl.countDown();
            }
        })
        .unsubscribeOn(Schedulers.single())
        .test(true);

        cdl.await();

        Assert.assertNotNull(on[0]);
        Assert.assertNotEquals(main, on[0]);
    }

    @Test
    public void doOnComplete1() {
        Nono.complete()
        .doOnComplete(this)
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void doOnComplete2() {
        ioError
        .doOnComplete(this)
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(0, count);
    }

    @Test
    public void doOnComplete3() {
        Nono.complete()
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(0, count);
    }

    @Test
    public void doOnError1() {
        Nono.complete()
        .doOnError(this)
        .test()
        .assertResult();

        Assert.assertEquals(0, count);
    }

    @Test
    public void doOnError2() {
        ioError
        .doOnError(this)
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void doOnError3() {
        ioError
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable t) throws Exception {
                count++;
                throw new IllegalArgumentException();
            }
        })
        .test()
        .assertFailure(CompositeException.class)
        .assertError(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable error) throws Throwable {
                List<Throwable> list = TestHelper.compositeList(error);
                TestHelper.assertError(list, 0, IOException.class);
                TestHelper.assertError(list, 1, IllegalArgumentException.class);
                return true;
            }
        })
        ;

        Assert.assertEquals(1, count);
    }

    @Test
    public void doOnSubscribe() {
        Nono.complete()
        .doOnSubscribe(this)
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void doOnSubscribeError() {
        ioError
        .doOnSubscribe(this)
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void doOnSubscribeThrows() {
        Nono.complete()
        .doOnSubscribe(new Consumer<Object>() {
            @Override
            public void accept(Object t) throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void doOnRequest() {
        Nono.complete()
        .doOnRequest(this)
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void doOnRequestError() {
        ioError
        .doOnRequest(this)
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void doOnRequestThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Nono.complete()
            .doOnRequest(new LongConsumer() {
                @Override
                public void accept(long t) throws Exception {
                    count++;
                    throw new IOException();
                }
            })
            .test()
            .assertResult();

            Assert.assertEquals(1, count);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void doAfterTerminate() {
        Nono.complete()
        .doAfterTerminate(this)
        .test()
        .assertResult();
    }

    @Test
    public void doAfterTerminateError() {
        ioError
        .doAfterTerminate(this)
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void doAfterTerminateThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Nono.complete()
            .doAfterTerminate(new Action() {
                @Override
                public void run() throws Exception {
                    count++;
                    throw new IOException();
                }
            })
            .test()
            .assertResult();

            Assert.assertEquals(1, count);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void doAfterTerminateThrowsError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ioError
            .doAfterTerminate(new Action() {
                @Override
                public void run() throws Exception {
                    count++;
                    throw new IllegalArgumentException();
                }
            })
            .test()
            .assertFailure(IOException.class);

            Assert.assertEquals(1, count);

            TestHelper.assertError(errors, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void doFinally1() {
        Nono.complete()
        .doFinally(this)
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void doFinally2() {
        ioError
        .doFinally(this)
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void doFinally3() {
        TestSubscriber<Void> ts = Nono.never()
        .doFinally(this)
        .test()
        ;

        ts.cancel();

        ts.assertEmpty();

        Assert.assertEquals(1, count);
    }

    @Test
    public void doFinally4() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ioError
            .doFinally(new Action() {
                @Override
                public void run() throws Exception {
                    count++;
                    throw new IllegalArgumentException();
                }
            })
            .test()
            .assertFailure(IOException.class);

            Assert.assertEquals(1, count);

            TestHelper.assertError(errors, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test(timeout = 5000)
    public void repeat() {
        final int[] counter = { 0 };

        Nono.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                if (counter[0]++ == 5) {
                    throw new CancellationException();
                }
            }
        }).repeat()
        .test()
        .assertFailure(CancellationException.class);

        Assert.assertEquals(6, counter[0]);
    }

    @Test
    public void repeatLimit() {
        Nono.fromAction(this)
        .repeat(5)
        .test()
        .assertResult();

        Assert.assertEquals(5, count);
    }

    @Test
    public void repeatBoolean() {
        Nono.fromAction(this)
        .repeat(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return count == 5;
            }
        })
        .test()
        .assertResult();

        Assert.assertEquals(5, count);
    }

    @Test
    public void repeatBooleanError() {
        ioError
        .repeat(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return count == 5;
            }
        })
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(0, count);
    }

    @Test
    public void repeatBooleanThrows() {
        Nono.fromAction(this)
        .repeat(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);

        Assert.assertEquals(1, count);
    }

    @Test
    public void repeatWhen() {
        Nono.fromAction(this)
        .repeatWhen(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f) throws Exception {
                return f.takeWhile(new Predicate<Object>() {
                    @Override
                    public boolean test(Object v) throws Exception {
                        return count == 5;
                    }
                });
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void repeatWhenError() {
        ioError
        .repeatWhen(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f) throws Exception {
                return f.takeWhile(new Predicate<Object>() {
                    @Override
                    public boolean test(Object v) throws Exception {
                        return count == 5;
                    }
                });
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void repeatWhenThrows() {
        Nono.fromAction(this)
        .repeatWhen(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f) throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void repeatWhenSignalError() {
        Nono.fromAction(this)
        .repeatWhen(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f) throws Exception {
                return f.map(new Function<Object, Object>() {
                    @Override
                    public Object apply(Object v) throws Exception {
                        throw new IOException();
                    }
                });
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test(timeout = 5000)
    public void retry() {
        Nono.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                if (count++ != 5) {
                    throw new IOException();
                }
            }
        }).retry()
        .test()
        .assertResult();
    }

    @Test(timeout = 5000)
    public void retryNoError() {
        Nono.fromAction(this)
        .retry()
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void retryLimit() {
        Nono.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                throw new IOException();
            }
        }).retry(5)
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void retryLimit2() {
        Nono.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                if (count++ != 4) {
                    throw new IOException();
                }
            }
        }).retry(5)
        .test()
        .assertResult();
    }

    @Test(timeout = 5000)
    public void retryLimitNoError() {
        Nono.fromAction(this)
        .retry(5)
        .test()
        .assertResult();

        Assert.assertEquals(1, count);
    }

    @Test
    public void retryPredicateNoError() {
        Nono.fromAction(this)
        .retry(Functions.alwaysTrue())
        .test()
        .assertResult();
    }

    @Test(timeout = 5000)
    public void retryPredicateNoRetry() {
        ioError
        .retry(Functions.alwaysFalse())
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void retryPredicate() {
        Nono.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                if (count++ != 4) {
                    throw new IOException();
                }
            }
        }).retry(Functions.alwaysTrue())
        .test()
        .assertResult();
    }

    @Test
    public void retryPredicateLimit() {
        Nono.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                count++;
                throw new IOException();
            }
        }).retry(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable e) throws Exception {
                return count != 5;
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void retryPredicateThrows() {
        ioError
        .retry(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable e) throws Exception {
                throw new IllegalArgumentException();
            }
        })
        .test()
        .assertFailure(CompositeException.class)
        .assertError(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable error) throws Throwable {
                List<Throwable> errors = TestHelper.compositeList(error);
                TestHelper.assertError(errors, 0, IOException.class);
                TestHelper.assertError(errors, 1, IllegalArgumentException.class);
                return true;
            }
        });
    }

    @Test
    public void retryWhenNoError() {
        Nono.fromAction(this)
        .retryWhen(Functions.<Flowable<Throwable>>identity())
        .test()
        .assertResult();
    }

    @Test
    public void retryWhen() {
        ioError
        .retryWhen(new Function<Flowable<Throwable>, Publisher<Throwable>>() {
            @Override
            public Publisher<Throwable> apply(Flowable<Throwable> f) throws Exception {
                return f.takeWhile(new Predicate<Throwable>() {
                    @Override
                    public boolean test(Throwable v) throws Exception {
                        return count++ != 5;
                    }
                });
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void retryWhenThrows() {
        Nono.complete()
        .retryWhen(new Function<Flowable<Throwable>, Publisher<Throwable>>() {
            @Override
            public Publisher<Throwable> apply(Flowable<Throwable> f) throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void retryWhenSignalError() {
        ioError
        .retryWhen(new Function<Flowable<Throwable>, Publisher<Throwable>>() {
            @Override
            public Publisher<Throwable> apply(Flowable<Throwable> f) throws Exception {
                return f.map(new Function<Throwable, Throwable>() {
                    @Override
                    public Throwable apply(Throwable v) throws Exception {
                        throw new IllegalArgumentException();
                    }
                });
            }
        })
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeActual() {
        new Nono() {
            @Override
            protected void subscribeActual(Subscriber<? super Void> s) {
                throw new NullPointerException();
            }
        }.test();
    }

    @Test
    public void subscribeActual2() {
        try {
            new Nono() {
                @Override
                protected void subscribeActual(Subscriber<? super Void> s) {
                    throw new IllegalArgumentException();
                }
            }.test(false);
        } catch (NullPointerException ex) {
            Assert.assertTrue(ex.toString(), ex.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void subscribeWith() {
        TestSubscriber<Void> ts = new TestSubscriber<>();

        Assert.assertSame(ts, Nono.complete().subscribeWith(ts));
    }

    @Test
    public void onAssembly() {
        Assert.assertNull(Nono.getOnAssemblyHandler());
        try {
            Nono.setOnAssemblyHandler(new Function<Nono, Nono>() {
                @Override
                public Nono apply(Nono f) throws Exception {
                    count++;
                    return f;
                }
            });
            Assert.assertNotNull(Nono.getOnAssemblyHandler());

            Nono.complete().delay(1, TimeUnit.MILLISECONDS);

            Assert.assertEquals(2, count);
        } finally {
            Nono.setOnAssemblyHandler(null);
        }
        Assert.assertNull(Nono.getOnAssemblyHandler());

        Nono.complete().delay(1, TimeUnit.MILLISECONDS);

        Assert.assertEquals(2, count);
    }

    @Test
    public void onAssemblyThrows() {
        Assert.assertNull(Nono.getOnAssemblyHandler());
        try {
            Nono.setOnAssemblyHandler(new Function<Nono, Nono>() {
                @Override
                public Nono apply(Nono f) throws Exception {
                    throw new IllegalArgumentException();
                }
            });
            Assert.assertNotNull(Nono.getOnAssemblyHandler());

            try {
                Nono.complete().delay(1, TimeUnit.MILLISECONDS);
                Assert.fail("Should have thrown");
            } catch (IllegalArgumentException ex) {
                // expected
            }

        } finally {
            Nono.setOnAssemblyHandler(null);
        }
        Assert.assertNull(Nono.getOnAssemblyHandler());

        Nono.complete().delay(1, TimeUnit.MILLISECONDS);

        Assert.assertEquals(0, count);
    }

    @Test
    public void subscribe1() {
        Nono.complete()
        .subscribe(this);

        Assert.assertEquals(1, count);
    }

    @Test
    public void subscribe1Error() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ioError
            .subscribe(this);

            Assert.assertEquals(0, count);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void subscribe2() {
        Nono.complete()
        .subscribe(this, this);

        Assert.assertEquals(1, count);
    }

    @Test
    public void subscribe2Error() {
        ioError
        .subscribe(this, this);

        Assert.assertEquals(1, count);
    }

    @Test
    public void blockingAwaitScalar() {
        Assert.assertNull(Nono.complete().blockingAwait());
    }

    @Test
    public void blockingAwait() {
        Assert.assertNull(Nono.complete().doOnComplete(this).blockingAwait());

        Assert.assertEquals(1, count);
    }

    @Test
    public void blockingAwaitErrorScalar() {
        Assert.assertNotNull(ioError.blockingAwait());
    }

    @Test
    public void blockingAwaitError() {
        Assert.assertNotNull(ioError.doOnError(this).blockingAwait());

        Assert.assertEquals(1, count);
    }

    @Test
    public void blockingAwaitDelayed() {
        Assert.assertNull(Nono.complete().delay(10, TimeUnit.MILLISECONDS).blockingAwait());
    }

    @Test
    public void blockingAwaitDelayedError() {
        Assert.assertNotNull(ioError.delay(10, TimeUnit.MILLISECONDS).blockingAwait());
    }

    @Test
    public void blockingAwaitScalarWithTimeout() {
        Assert.assertNull(Nono.complete().blockingAwait(5, TimeUnit.SECONDS));
    }

    @Test
    public void blockingAwaitWithTimeoutDoTimeout() {
        Throwable t = Nono.never().blockingAwait(200, TimeUnit.MILLISECONDS);
        Assert.assertTrue(t.toString(), t instanceof TimeoutException);
    }

    @Test
    public void blockingAwaitErrorScalarWithTimeout() {
        Throwable t = ioError.blockingAwait(5, TimeUnit.SECONDS);
        Assert.assertTrue(t.toString(), t instanceof IOException);
    }

    @Test
    public void blockingAwaitErrorWithTimeout() {
        Assert.assertNotNull(ioError.doOnError(this).blockingAwait(5, TimeUnit.SECONDS));

        Assert.assertEquals(1, count);
    }

    @Test
    public void blockingAwaitDelayedWithTimeout() {
        Assert.assertNull(Nono.complete().delay(10, TimeUnit.MILLISECONDS).blockingAwait(5, TimeUnit.SECONDS));
    }

    @Test
    public void blockingAwaitDelayedErrorWithTimeout() {
        Assert.assertNotNull(ioError.delay(10, TimeUnit.MILLISECONDS).blockingAwait(5, TimeUnit.SECONDS));
    }

    @Test
    public void blockingAwaitInterrupt() {
        try {
            Thread.currentThread().interrupt();
            Throwable t = Nono.never().blockingAwait();
            Assert.assertTrue(String.valueOf(t), t instanceof InterruptedException);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void blockingAwaitInterruptTiemout() {
        try {
            Thread.currentThread().interrupt();
            Throwable t = Nono.never().blockingAwait(5, TimeUnit.SECONDS);
            Assert.assertTrue(String.valueOf(t), t instanceof InterruptedException);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void blockingSubscribe1() {
        Nono.complete()
        .blockingSubscribe(this);

        Assert.assertEquals(1, count);
    }

    @Test
    public void blockingSubscribe1Error() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ioError
            .blockingSubscribe(this);

            Assert.assertEquals(0, count);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void blockingSubscribe2() {
        Nono.complete()
        .blockingSubscribe(this, this);

        Assert.assertEquals(1, count);
    }

    @Test
    public void blockingSubscribe2Error() {
        ioError
        .blockingSubscribe(this, this);

        Assert.assertEquals(1, count);
    }

    @Test
    public void blockingSubscribeCompleteThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Nono.complete()
            .blockingSubscribe(new Action() {
                @Override
                public void run() throws Exception {
                    throw new IllegalArgumentException();
                }
            }, this);

            TestHelper.assertError(errors, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void blockingSubscribeErrorThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ioError
            .blockingSubscribe(this, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable ex) throws Exception {
                    throw new IllegalArgumentException();
                }
            });
            TestHelper.assertError(errors, 0, CompositeException.class);

            List<Throwable> ce = TestHelper.compositeList(errors.get(0));

            TestHelper.assertError(ce, 0, IOException.class);
            TestHelper.assertError(ce, 1, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void hide() {
        Subscriber<Void> s = new Subscriber<Void>() {

            @Override
            public void onSubscribe(Subscription s) {
                Assert.assertFalse(s instanceof QueueSubscription);
            }

            @Override
            public void onNext(Void t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        };
        Nono.complete().hide().subscribe(s);
        ioError.hide().subscribe(s);
    }

    void checkNoNext(Function<Nono, Nono> mapper) {
        try {
            mapper.apply(new Nono() {
                @Override
                protected void subscribeActual(Subscriber<? super Void> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(null);
                    s.onComplete();
                }
            })
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult();
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    @Test
    public void noNext() {
        checkNoNext(new Function<Nono, Nono>() {
            @Override
            public Nono apply(Nono np) throws Exception {
                return np.andThen(Nono.complete());
            }
        });

        checkNoNext(new Function<Nono, Nono>() {
            @Override
            public Nono apply(Nono np) throws Exception {
                return Nono.fromPublisher(np.andThen(Flowable.empty()));
            }
        });

        checkNoNext(new Function<Nono, Nono>() {
            @Override
            public Nono apply(Nono np) throws Exception {
                return np.delay(50, TimeUnit.MILLISECONDS);
            }
        });

        checkNoNext(new Function<Nono, Nono>() {
            @Override
            public Nono apply(Nono np) throws Exception {
                return np.delaySubscription(50, TimeUnit.MILLISECONDS);
            }
        });

        checkNoNext(new Function<Nono, Nono>() {
            @Override
            public Nono apply(Nono np) throws Exception {
                return np.delaySubscription(Flowable.timer(50, TimeUnit.MILLISECONDS));
            }
        });

        checkNoNext(new Function<Nono, Nono>() {
            @Override
            public Nono apply(Nono np) throws Exception {
                return np.doFinally(NonoTest.this);
            }
        });

        checkNoNext(new Function<Nono, Nono>() {
            @Override
            public Nono apply(Nono np) throws Exception {
                return np.repeat(1);
            }
        });

        checkNoNext(new Function<Nono, Nono>() {
            @Override
            public Nono apply(Nono np) throws Exception {
                return np.retry(1);
            }
        });
    }

    @Test
    public void takeUntil1() {
        Nono.complete().takeUntil(Nono.never())
        .test()
        .assertResult();
    }

    @Test
    public void takeUntil2() {
        Nono.complete().takeUntil(Nono.complete())
        .test()
        .assertResult();
    }

    @Test
    public void takeUntil3() {
        ioError.takeUntil(Nono.never())
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void takeUntil4() {
        Nono.never().takeUntil(ioError)
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void takeUntil5() {
        Nono.never().takeUntil(Flowable.range(1, 2))
        .test()
        .assertResult();
    }

    @Test
    public void takeUntil6() {
        Nono.never().takeUntil(Nono.never())
        .test()
        .assertEmpty()
        .cancel();
    }

    @Test
    public void subscribe() {
        Nono.fromAction(this).subscribe();

        Assert.assertEquals(1, count);
    }

    @Test
    public void subscribeError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ioError.subscribe();

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void blockingSubscribe() {
        Nono.fromAction(this)
        .delay(100, TimeUnit.MILLISECONDS)
        .blockingSubscribe();
    }

    @Test
    public void blockingSubscribeError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ioError
            .delay(100, TimeUnit.MILLISECONDS)
            .blockingSubscribe();

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void createSuccess() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Nono.create(new CompletableOnSubscribe() {
                @Override
                public void subscribe(CompletableEmitter e) throws Exception {
                    e.setCancellable(NonoTest.this);
                    e.onComplete();
                    e.onError(new IOException());
                    e.onComplete();
                }
            })
            .test()
            .assertResult();

            assertEquals(1, count);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void createSuccessNoResource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Nono.create(new CompletableOnSubscribe() {
                @Override
                public void subscribe(CompletableEmitter e) throws Exception {
                    e.onComplete();
                    e.onError(new IOException());
                    e.onComplete();
                }
            })
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void createError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Nono.create(new CompletableOnSubscribe() {
                @Override
                public void subscribe(CompletableEmitter e) throws Exception {
                    e.setCancellable(NonoTest.this);
                    e.onError(new IOException());
                    e.onComplete();
                    e.onError(new IOException());
                }
            })
            .test()
            .assertFailure(IOException.class);

            assertEquals(1, count);
            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void createErrorNoResource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Nono.create(new CompletableOnSubscribe() {
                @Override
                public void subscribe(CompletableEmitter e) throws Exception {
                    e.onError(new IOException());
                    e.onComplete();
                    e.onError(new IOException());
                }
            })
            .test()
            .assertFailure(IOException.class);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void toFutureEmpty() throws Exception {
        assertNull(Nono.complete().toFuture().get());
    }

    @Test(expected = ExecutionException.class)
    public void toFutureError() throws Exception {
        Nono.error(new IOException()).toFuture().get();
    }

    @Test
    public void cache() {
        Nono np = Nono.complete()
        .doOnSubscribe(this)
        .cache();

        assertEquals(0, count);

        np.test().assertResult();

        assertEquals(1, count);

        np.test().assertResult();

        assertEquals(1, count);

        np.test().assertResult();

        assertEquals(1, count);
    }

    @Test
    public void cacheError() {
        Nono np = Nono.error(new IOException())
        .doOnSubscribe(this)
        .cache();

        assertEquals(0, count);

        np.test().assertFailure(IOException.class);

        assertEquals(1, count);

        np.test().assertFailure(IOException.class);

        assertEquals(1, count);

        np.test().assertFailure(IOException.class);

        assertEquals(1, count);
    }
}
