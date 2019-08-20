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

import org.junit.Test;
import org.reactivestreams.*;

import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.CompositeException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class PerhapsTest implements Consumer<Object>, Action, LongConsumer, Cancellable {

    volatile int count;

    @Override
    public void accept(long t) throws Exception {
        count++;
    }

    @Override
    public void accept(Object t) throws Exception {
        count++;
    }

    @Override
    public void run() throws Exception {
        count++;
    }

    @Override
    public void cancel() throws Exception {
        count++;
    }

    @Test
    public void onAssembly() {
        assertNull(Perhaps.getOnAssemblyHandler());

        Perhaps.setOnAssemblyHandler(new Function<Perhaps<Object>, Perhaps<Object>>() {
            @Override
            public Perhaps<Object> apply(Perhaps<Object> sp) throws Exception {
                count++;
                return sp;
            }
        });

        Perhaps.just(1)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                return v + 1;
            }
        })
        .test()
        .assertResult(2);

        assertEquals(2, count);

        Perhaps.setOnAssemblyHandler(new Function<Perhaps<Object>, Perhaps<Object>>() {
            @Override
            public Perhaps<Object> apply(Perhaps<Object> sp) throws Exception {
                throw new IllegalStateException();
            }
        });

        try {
            Perhaps.just(1);
            fail("Should have thrown");
        } catch (IllegalStateException ex) {
            // expected
        }

        Perhaps.setOnAssemblyHandler(null);

        Perhaps.just(1)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                return v + 1;
            }
        })
        .test()
        .assertResult(2);

        assertEquals(2, count);
    }

    @Test
    public void fromFuture() {
        FutureTask<Integer> ft = new FutureTask<Integer>(Functions.EMPTY_RUNNABLE, 1);
        ft.run();

        Perhaps.fromFuture(ft)
        .test()
        .assertResult(1);
    }

    @Test
    public void fromFutureNull() {
        FutureTask<Integer> ft = new FutureTask<Integer>(Functions.EMPTY_RUNNABLE, null);
        ft.run();

        Perhaps.fromFuture(ft)
        .test()
        .assertResult();
    }

    @Test
    public void fromFutureTimeout() {
        FutureTask<Integer> ft = new FutureTask<Integer>(Functions.EMPTY_RUNNABLE, 1);

        Perhaps.fromFuture(ft, 1, TimeUnit.MILLISECONDS)
        .test()
        .assertFailure(TimeoutException.class);
    }

    @Test
    public void fromFutureCrash() {
        FutureTask<Integer> ft = new FutureTask<Integer>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw new IOException();
            }
        });
        ft.run();

        Perhaps.fromFuture(ft)
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void toFuture() throws Exception {
        assertEquals(1, Perhaps.just(1).toFuture().get().intValue());
    }

    @Test
    public void toFutureEmpty() throws Exception {
        assertNull(Perhaps.empty().toFuture().get());
    }

    @Test(expected = ExecutionException.class)
    public void toFutureError() throws Exception {
        Perhaps.error(new IOException()).toFuture().get();
    }

    @Test
    public void createSuccess() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Perhaps.create(new MaybeOnSubscribe<Object>() {
                @Override
                public void subscribe(MaybeEmitter<Object> e) throws Exception {
                    e.setCancellable(PerhapsTest.this);
                    e.onSuccess(1);
                    e.onSuccess(2);
                    e.onError(new IOException());
                }
            })
            .test()
            .assertResult(1);

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
            Perhaps.create(new MaybeOnSubscribe<Object>() {
                @Override
                public void subscribe(MaybeEmitter<Object> e) throws Exception {
                    e.onSuccess(1);
                    e.onSuccess(2);
                    e.onError(new IOException());
                }
            })
            .test()
            .assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void createError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Perhaps.create(new MaybeOnSubscribe<Object>() {
                @Override
                public void subscribe(MaybeEmitter<Object> e) throws Exception {
                    e.setCancellable(PerhapsTest.this);
                    e.onError(new IOException());
                    e.onSuccess(2);
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
            Perhaps.create(new MaybeOnSubscribe<Object>() {
                @Override
                public void subscribe(MaybeEmitter<Object> e) throws Exception {
                    e.onError(new IOException());
                    e.onSuccess(2);
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
    public void error() {
        Perhaps.error(new IOException())
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void errorSupplier() {
        Perhaps.error(new Supplier<Throwable>() {
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
        Perhaps.error(new Supplier<Throwable>() {
            @Override
            public Throwable get() throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void errorSupplierNull() {
        Perhaps.error(new Supplier<Throwable>() {
            @Override
            public Throwable get() throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void fromCallable() {
        Perhaps.fromCallable(Functions.justCallable(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void fromCallableNull() {
        Perhaps.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void fromCallableThrows() {
        Perhaps.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void never() {
        Perhaps.never()
        .test()
        .assertEmpty();
    }

    @Test
    public void defer() {
        Perhaps<Integer> sp = Perhaps.defer(new Supplier<Perhaps<Integer>>() {
            int i;

            @Override
            public Perhaps<Integer> get() throws Exception {
                return Perhaps.just(++i);
            }
        });

        sp.test().assertResult(1);

        sp.test().assertResult(2);
    }

    @Test
    public void deferThrows() {
        Perhaps<Integer> sp = Perhaps.defer(new Supplier<Perhaps<Integer>>() {
            @Override
            public Perhaps<Integer> get() throws Exception {
                throw new IOException();
            }
        });

        sp.test().assertFailure(IOException.class);

        sp.test().assertFailure(IOException.class);
    }

    @Test
    public void fromPublisher0() {
        Perhaps.fromPublisher(Flowable.empty())
        .test()
        .assertResult();
    }

    @Test
    public void fromPublisherPerhaps() {
        Perhaps.fromPublisher(Perhaps.just(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void fromPublisher1() {
        Perhaps.fromPublisher(Flowable.just(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void fromPublisher2() {
        Perhaps.fromPublisher(Flowable.range(1, 2))
        .test()
        .assertFailure(IndexOutOfBoundsException.class);
    }

    @Test
    public void fromPublisherError() {
        Perhaps.fromPublisher(Flowable.error(new IOException()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void fromSingle() {
        Perhaps.fromSingle(Single.just(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void fromSingleBackpressured() {
        Perhaps.fromSingle(Single.just(1))
        .test(0)
        .assertEmpty()
        .requestMore(1)
        .assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable1() {
        PerhapsProcessor<Integer> sp1 = PerhapsProcessor.create();
        PerhapsProcessor<Integer> sp2 = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = Perhaps.amb(Arrays.asList(sp1, sp2))
        .test();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        ts.assertEmpty();

        sp1.onNext(1);

        ts.assertResult(1);

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable2() {
        PerhapsProcessor<Integer> sp1 = PerhapsProcessor.create();
        PerhapsProcessor<Integer> sp2 = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = Perhaps.amb(Arrays.asList(sp1, sp2))
        .test();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        ts.assertEmpty();

        sp2.onNext(2);

        ts.assertResult(2);

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable1Error() {
        PerhapsProcessor<Integer> sp1 = PerhapsProcessor.create();
        PerhapsProcessor<Integer> sp2 = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = Perhaps.amb(Arrays.asList(sp1, sp2))
        .test();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        ts.assertEmpty();

        sp1.onError(new IOException());

        ts.assertFailure(IOException.class);

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable2Error() {
        PerhapsProcessor<Integer> sp1 = PerhapsProcessor.create();
        PerhapsProcessor<Integer> sp2 = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = Perhaps.amb(Arrays.asList(sp1, sp2))
        .test();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        ts.assertEmpty();

        sp2.onError(new IOException());

        ts.assertFailure(IOException.class);

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArray1() {
        PerhapsProcessor<Integer> sp1 = PerhapsProcessor.create();
        PerhapsProcessor<Integer> sp2 = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = Perhaps.ambArray(sp1, sp2)
        .test();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        ts.assertEmpty();

        sp1.onNext(1);

        ts.assertResult(1);

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArray2() {
        PerhapsProcessor<Integer> sp1 = PerhapsProcessor.create();
        PerhapsProcessor<Integer> sp2 = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = Perhaps.ambArray(sp1, sp2)
        .test();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        ts.assertEmpty();

        sp2.onNext(2);

        ts.assertResult(2);

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArray1Error() {
        PerhapsProcessor<Integer> sp1 = PerhapsProcessor.create();
        PerhapsProcessor<Integer> sp2 = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = Perhaps.ambArray(sp1, sp2)
        .test();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        ts.assertEmpty();

        sp1.onError(new IOException());

        ts.assertFailure(IOException.class);

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArray2Error() {
        PerhapsProcessor<Integer> sp1 = PerhapsProcessor.create();
        PerhapsProcessor<Integer> sp2 = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = Perhaps.ambArray(sp1, sp2)
        .test();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        ts.assertEmpty();

        sp2.onError(new IOException());

        ts.assertFailure(IOException.class);

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterable() {
        Perhaps.concat(Arrays.asList(Perhaps.just(1), Perhaps.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableError() {
        Perhaps.concat(Arrays.asList(Perhaps.just(1), Perhaps.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableError2() {
        Perhaps.concat(Arrays.asList(Perhaps.error(new IOException()), Perhaps.just(2)))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void concatPublisher() {
        Perhaps.concat(Flowable.just(Perhaps.just(1), Perhaps.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatPublisherPrefetch() {
        Perhaps.concat(Flowable.just(Perhaps.just(1), Perhaps.just(2)), 1)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatPublisherError() {
        Perhaps.concat(Flowable.just(Perhaps.just(1), Perhaps.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void concatPublisherError2() {
        Perhaps.concat(Flowable.just(Perhaps.error(new IOException()), Perhaps.just(2)))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void concatPublisherError3() {
        Perhaps.concat(
                Flowable.just(Perhaps.just(1))
                .concatWith(Flowable.<Perhaps<Integer>>error(new IOException()))
        )
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArray() {
        Perhaps.concatArray(Perhaps.just(1), Perhaps.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayError() {
        Perhaps.concatArray(Perhaps.just(1), Perhaps.error(new IOException()))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayError2() {
        Perhaps.concatArray(Perhaps.error(new IOException()), Perhaps.just(2))
        .test()
        .assertFailure(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableDelayError() {
        Perhaps.concatDelayError(Arrays.asList(Perhaps.just(1), Perhaps.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableDelayErrorError() {
        Perhaps.concatDelayError(Arrays.asList(Perhaps.just(1), Perhaps.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableDelayErrorError2() {
        Perhaps.concatDelayError(Arrays.asList(Perhaps.error(new IOException()), Perhaps.just(2)))
        .test()
        .assertFailure(IOException.class, 2);
    }

    @Test
    public void concatDelayErrorPublisher() {
        Perhaps.concatDelayError(Flowable.just(Perhaps.just(1), Perhaps.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatDelayErrorPublisherPrefetch() {
        Perhaps.concatDelayError(Flowable.just(Perhaps.just(1), Perhaps.just(2)), 1)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatDelayErrorPublisherError() {
        Perhaps.concatDelayError(Flowable.just(Perhaps.just(1), Perhaps.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void concatDelayErrorPublisherError2() {
        Perhaps.concatDelayError(Flowable.just(Perhaps.error(new IOException()), Perhaps.just(2)))
        .test()
        .assertFailure(IOException.class, 2);
    }

    @Test
    public void concatDelayErrorPublisherError3() {
        Perhaps.concatDelayError(
                Flowable.just(Perhaps.just(1))
                .concatWith(Flowable.<Perhaps<Integer>>error(new IOException()))
        )
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayDelayError() {
        Perhaps.concatArrayDelayError(Perhaps.just(1), Perhaps.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayDelayErrorError() {
        Perhaps.concatArrayDelayError(Perhaps.just(1), Perhaps.error(new IOException()))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayDelayErrorError2() {
        Perhaps.concatArrayDelayError(Perhaps.error(new IOException()), Perhaps.just(2))
        .test()
        .assertFailure(IOException.class, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterable() {
        Perhaps.merge(Arrays.asList(Perhaps.just(1), Perhaps.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableError() {
        Perhaps.merge(Arrays.asList(Perhaps.just(1), Perhaps.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableError2() {
        Perhaps.merge(Arrays.asList(Perhaps.error(new IOException()), Perhaps.just(2)))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void mergePublisher() {
        Perhaps.merge(Flowable.just(Perhaps.just(1), Perhaps.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void mergePublisherPrefetch() {
        Perhaps.merge(Flowable.just(Perhaps.just(1), Perhaps.just(2)), 1)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void mergePublisherError() {
        Perhaps.merge(Flowable.just(Perhaps.just(1), Perhaps.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void mergePublisherError2() {
        Perhaps.merge(Flowable.just(Perhaps.error(new IOException()), Perhaps.just(2)))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void mergePublisherError3() {
        Perhaps.merge(
                Flowable.just(Perhaps.just(1))
                .concatWith(Flowable.<Perhaps<Integer>>error(new IOException()))
        )
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArray() {
        Perhaps.mergeArray(Perhaps.just(1), Perhaps.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayError() {
        Perhaps.mergeArray(Perhaps.just(1), Perhaps.error(new IOException()))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayError2() {
        Perhaps.mergeArray(Perhaps.error(new IOException()), Perhaps.just(2))
        .test()
        .assertFailure(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableDelayError() {
        Perhaps.mergeDelayError(Arrays.asList(Perhaps.just(1), Perhaps.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableDelayErrorError() {
        Perhaps.mergeDelayError(Arrays.asList(Perhaps.just(1), Perhaps.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableDelayErrorError2() {
        Perhaps.mergeDelayError(Arrays.asList(Perhaps.error(new IOException()), Perhaps.just(2)))
        .test()
        .assertFailure(IOException.class, 2);
    }

    @Test
    public void mergeDelayErrorPublisher() {
        Perhaps.mergeDelayError(Flowable.just(Perhaps.just(1), Perhaps.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void mergeDelayErrorPublisherPrefetch() {
        Perhaps.mergeDelayError(Flowable.just(Perhaps.just(1), Perhaps.just(2)), 1)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void mergeDelayErrorPublisherError() {
        Perhaps.mergeDelayError(Flowable.just(Perhaps.just(1), Perhaps.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void mergeDelayErrorPublisherError2() {
        Perhaps.mergeDelayError(Flowable.just(Perhaps.error(new IOException()), Perhaps.just(2)))
        .test()
        .assertFailure(IOException.class, 2);
    }

    @Test
    public void mergeDelayErrorPublisherError3() {
        Perhaps.mergeDelayError(
                Flowable.just(Perhaps.just(1))
                .concatWith(Flowable.<Perhaps<Integer>>error(new IOException()))
        )
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayDelayError() {
        Perhaps.mergeArrayDelayError(Perhaps.just(1), Perhaps.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayDelayErrorError() {
        Perhaps.mergeArrayDelayError(Perhaps.just(1), Perhaps.error(new IOException()))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayDelayErrorError2() {
        Perhaps.mergeArrayDelayError(Perhaps.error(new IOException()), Perhaps.just(2))
        .test()
        .assertFailure(IOException.class, 2);
    }

    @Test
    public void timer() {
        Perhaps.timer(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0L);
    }

    @Test
    public void timerScheduler() {
        Perhaps.timer(100, TimeUnit.MILLISECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0L);
    }

    @Test
    public void usingEager() {
        Perhaps.using(Functions.justSupplier(1),
                Functions.justFunction(Perhaps.just(1)), this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void usingEagerError() {
        Perhaps.using(Functions.justSupplier(1),
                Functions.justFunction(Perhaps.error(new IOException())), this)
        .test()
        .assertFailure(IOException.class);

        assertEquals(1, count);
    }

    @Test
    public void usingNonEager() {
        Perhaps.using(Functions.justSupplier(1),
                Functions.justFunction(Perhaps.just(1)), this,
                false)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zip() {
        Perhaps.zip(Arrays.asList(Perhaps.just(1), Perhaps.just(2)), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return "" + a[0] + a[1];
            }
        })
        .test()
        .assertResult("12");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipError1() {
        Perhaps.zip(Arrays.asList(Perhaps.error(new IOException()), Perhaps.just(2)), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return "" + a[0] + a[1];
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipError2() {
        Perhaps.zip(Arrays.asList(Perhaps.just(1), Perhaps.error(new IOException())), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return "" + a[0] + a[1];
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipArray() {
        Perhaps.zipArray(new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return "" + a[0] + a[1];
            }
        }, Perhaps.just(1), Perhaps.just(2))
        .test()
        .assertResult("12");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipArrayError1() {
        Perhaps.zipArray(new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return "" + a[0] + a[1];
            }
        }, Perhaps.error(new IOException()), Perhaps.just(2))
        .test()
        .assertFailure(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipArrayError2() {
        Perhaps.zipArray(new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return "" + a[0] + a[1];
            }
        }, Perhaps.just(1), Perhaps.error(new IOException()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void ambWith1() {
        PerhapsProcessor<Integer> sp1 = PerhapsProcessor.create();
        PerhapsProcessor<Integer> sp2 = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = sp1.ambWith(sp2)
        .test();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        ts.assertEmpty();

        sp1.onNext(1);

        ts.assertResult(1);

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());
    }

    @Test
    public void ambWith2() {
        PerhapsProcessor<Integer> sp1 = PerhapsProcessor.create();
        PerhapsProcessor<Integer> sp2 = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = sp1.ambWith(sp2)
        .test();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        ts.assertEmpty();

        sp2.onNext(2);

        ts.assertResult(2);

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());
    }

    @Test
    public void ambWith1Error() {
        PerhapsProcessor<Integer> sp1 = PerhapsProcessor.create();
        PerhapsProcessor<Integer> sp2 = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = sp1.ambWith(sp2)
        .test();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        ts.assertEmpty();

        sp1.onError(new IOException());

        ts.assertFailure(IOException.class);

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());
    }

    @Test
    public void ambWith2Error() {
        PerhapsProcessor<Integer> sp1 = PerhapsProcessor.create();
        PerhapsProcessor<Integer> sp2 = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = sp1.ambWith(sp2)
        .test();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        ts.assertEmpty();

        sp2.onError(new IOException());

        ts.assertFailure(IOException.class);

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());
    }

    @Test
    public void andThenNono() {
        Perhaps.just(1).andThen(Nono.fromAction(this))
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void andThenNonoError() {
        Perhaps.just(1).andThen(Nono.error(new IOException()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void andThenErrorNono() {
        Perhaps.error(new IOException()).andThen(Nono.fromAction(this))
        .test()
        .assertFailure(IOException.class);

        assertEquals(0, count);
    }

    @Test
    public void andThenPublisher() {
        Perhaps.just(1).andThen(Flowable.just(2))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void andThenPublisherError() {
        Perhaps.just(1).andThen(Flowable.<Integer>error(new IOException()))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void andThenErrorPublisher() {
        Perhaps.error(new IOException()).andThen(Flowable.just(2))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void concatWith() {
        Perhaps.just(1).concatWith(Perhaps.just(2))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatWithError() {
        Perhaps.just(1).concatWith(Perhaps.<Integer>error(new IOException()))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void concatWithErrorPerhaps() {
        Perhaps.<Integer>error(new IOException()).concatWith(Perhaps.just(2))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void mergeWith() {
        Perhaps.just(1).mergeWith(Perhaps.just(2))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void mergeWithError() {
        Perhaps.just(1).mergeWith(Perhaps.<Integer>error(new IOException()))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void mergeWithErrorPerhaps() {
        Perhaps.<Integer>error(new IOException()).mergeWith(Perhaps.just(2))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void zipWith() {
        Perhaps.just(1).zipWith(Perhaps.just(2), new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return a + b;
            }
        })
        .test()
        .assertResult(3);
    }

    @Test
    public void map() {
        Perhaps.just(1).map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                return v + 1;
            }
        })
        .test()
        .assertResult(2);
    }

    @Test
    public void mapNull() {
        Perhaps.just(1).map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void mapThrows() {
        Perhaps.just(1).map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                throw new IllegalArgumentException();
            }
        })
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void mapError() {
        Perhaps.just(1)
        .mapError(new Function<Throwable, Throwable>() {
            @Override
            public Throwable apply(Throwable t) throws Exception {
                return new IllegalArgumentException();
            }
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void mapErrorWithError() {
        Perhaps.error(new IOException())
        .mapError(new Function<Throwable, Throwable>() {
            @Override
            public Throwable apply(Throwable t) throws Exception {
                return new IllegalArgumentException();
            }
        })
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void mapErrorThrows() {
        Perhaps.error(new IOException())
        .mapError(new Function<Throwable, Throwable>() {
            @Override
            public Throwable apply(Throwable t) throws Exception {
                throw new IllegalArgumentException();
            }
        })
        .test()
        .assertFailure(CompositeException.class);
    }

    @Test
    public void filter() {
        Perhaps.just(1).filter(Functions.alwaysTrue())
        .test()
        .assertResult(1);
    }

    @Test
    public void filterFalse() {
        Perhaps.just(1).filter(Functions.alwaysFalse())
        .test()
        .assertResult();
    }

    @Test
    public void filterError() {
        Perhaps.error(new IOException()).filter(Functions.alwaysTrue())
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void filterThrows() {
        Perhaps.just(1).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void timeoutNormal() {
        Perhaps.just(1).timeout(1, TimeUnit.MINUTES)
        .test()
        .assertResult(1);
    }

    @Test
    public void timeoutError() {
        Perhaps.error(new IOException()).timeout(1, TimeUnit.MINUTES)
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void timeout() {
        PerhapsProcessor<Integer> sp = PerhapsProcessor.create();

        sp.timeout(10, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void timeoutScheduler() {
        PerhapsProcessor<Integer> sp = PerhapsProcessor.create();

        sp.timeout(10, TimeUnit.MILLISECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void timeoutFallback() {
        PerhapsProcessor<Integer> sp = PerhapsProcessor.create();

        sp.timeout(10, TimeUnit.MILLISECONDS, Perhaps.just(1))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void timeoutFallbackScheduler() {
        PerhapsProcessor<Integer> sp = PerhapsProcessor.create();

        sp.timeout(10, TimeUnit.MILLISECONDS, Schedulers.single(), Perhaps.just(1))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void timeoutPublisher() {
        PerhapsProcessor<Integer> sp = PerhapsProcessor.create();

        sp.timeout(Flowable.empty())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void timeoutPublisherFallback() {
        PerhapsProcessor<Integer> sp = PerhapsProcessor.create();

        sp.timeout(Flowable.empty(), Perhaps.just(1))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void onErrorReturnItem() {
        Perhaps.just(1).onErrorReturnItem(2)
        .test()
        .assertResult(1);
    }

    @Test
    public void onErrorReturnItemError() {
        Perhaps.error(new IOException()).onErrorReturnItem(2)
        .test()
        .assertResult(2);
    }

    @Test
    public void onErrorResumeWith() {
        Perhaps.just(1).onErrorResumeWith(Perhaps.just(2))
        .test()
        .assertResult(1);
    }

    @Test
    public void onErrorResumeWithError() {
        Perhaps.<Integer>error(new IOException()).onErrorResumeWith(Perhaps.just(2))
        .test()
        .assertResult(2);
    }

    @Test
    public void onErrorResumeNext() {
        Perhaps.just(1).onErrorResumeNext(Functions.justFunction(Perhaps.just(2)))
        .test()
        .assertResult(1);
    }

    @Test
    public void onErrorResumeNextError() {
        Perhaps.<Integer>error(new IOException()).onErrorResumeNext(Functions.justFunction(Perhaps.just(2)))
        .test()
        .assertResult(2);
    }

    @Test
    public void onErrorResumeNextErrorThrows() {
        Perhaps.<Integer>error(new IOException())
        .onErrorResumeNext(new Function<Throwable, Perhaps<Integer>>() {
            @Override
            public Perhaps<Integer> apply(Throwable e) throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(CompositeException.class);
    }

    @Test
    public void flatMap() {
        Perhaps.just(1).flatMap(Functions.justFunction(Perhaps.just(2)))
        .test()
        .assertResult(2);
    }

    @Test
    public void flatMapError() {
        Perhaps.just(1).flatMap(Functions.justFunction(Perhaps.error(new IOException())))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void flatMapError2() {
        Perhaps.error(new IOException()).flatMap(Functions.justFunction(Perhaps.just(2)))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void flatMapSignal() {
        Perhaps.just(1).flatMap(
                Functions.justFunction(Perhaps.just(2)),
                Functions.justFunction(Perhaps.just(3)),
                Functions.justSupplier(Perhaps.just(4))
        )
        .test()
        .assertResult(2);
    }

    @Test
    public void flatMapSignalError() {
        Perhaps.error(new IOException()).flatMap(
                Functions.justFunction(Perhaps.just(2)),
                Functions.justFunction(Perhaps.just(3)),
                Functions.justSupplier(Perhaps.just(4))
        )
        .test()
        .assertResult(3);
    }

    @Test
    public void flatMapSignalError2() {
        Perhaps.just(1).flatMap(
                Functions.justFunction(Perhaps.error(new IOException())),
                Functions.justFunction(Perhaps.just(3)),
                Functions.justSupplier(Perhaps.just(4))
        )
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void flatMapSignalError3() {
        Perhaps.error(new IOException()).flatMap(
                Functions.justFunction(Perhaps.just(2)),
                Functions.justFunction(Perhaps.error(new IllegalArgumentException())),
                Functions.justSupplier(Perhaps.just(4))
        )
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void flatMapPublisher() {
        Perhaps.just(1).flatMapPublisher(Functions.justFunction(Flowable.range(1, 5)))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void flatMapPublisherBackpressure() {
        Perhaps.just(1).flatMapPublisher(Functions.justFunction(Flowable.range(1, 5)))
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void flatMapPublisherError() {
        Perhaps.error(new IOException()).flatMapPublisher(Functions.justFunction(Flowable.range(1, 5)))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void flatMapPublisherError2() {
        Perhaps.just(1)
        .flatMapPublisher(Functions.justFunction(Flowable.error(new IOException())))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void takeUnit() {
        Publisher<?> p = Perhaps.never();
        Perhaps.just(1).takeUntil(p)
        .test()
        .assertResult(1);
    }

    @Test
    public void takeUnitHot() {
        PerhapsProcessor<Integer> sp1 = PerhapsProcessor.create();
        PerhapsProcessor<Integer> sp2 = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = sp1.takeUntil(sp2)
        .test()
        .assertEmpty();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        sp1.onNext(1);
        sp1.onComplete();

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());

        ts.assertResult(1);
    }

    @Test
    public void takeUnitError() {
        Perhaps.error(new IOException()).takeUntil(Perhaps.never())
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void takeUnitSwitch() {
        Perhaps.just(1).takeUntil(Perhaps.just(1))
        .test()
        .assertResult();
    }

    @Test
    public void takeUnitSwitchNever() {
        Perhaps.just(1).takeUntil(Flowable.empty())
        .test()
        .assertResult();
    }

    @Test
    public void takeUnitHotSwitch() {
        PerhapsProcessor<Integer> sp1 = PerhapsProcessor.create();
        PerhapsProcessor<Integer> sp2 = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = sp1.takeUntil(sp2)
        .test()
        .assertEmpty();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        sp2.onNext(1);

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());

        ts.assertResult();
    }

    @Test
    public void delay() {
        Perhaps.just(1).delay(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delayScheduler() {
        Perhaps.just(1).delay(100, TimeUnit.MILLISECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delayError() {
        Perhaps.error(new IOException()).delay(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void delayPublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Perhaps.just(1).delay(pp).test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onNext(2);

        ts.assertResult(1);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void delayPublisherError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Perhaps.<Integer>error(new IOException())
                .delay(pp).test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onNext(2);

        ts.assertFailure(IOException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void delayPublisherError2() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Perhaps.<Integer>error(new IOException())
                .delay(pp).test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onError(new IllegalArgumentException());

        ts.assertFailure(CompositeException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void delaySubscription() {
        Perhaps.just(1)
        .delaySubscription(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delaySubscriptionScheduler() {
        PerhapsProcessor<Integer> sp = PerhapsProcessor.create();
        TestScheduler scheduler = new TestScheduler();

        TestSubscriber<Integer> ts = sp.delaySubscription(1, TimeUnit.SECONDS, scheduler)
        .test();

        assertFalse(sp.hasSubscribers());

        ts.assertEmpty();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        assertTrue(sp.hasSubscribers());

        sp.onNext(1);

        ts.assertResult(1);

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void delaySubscriptionPublisher() {
        PerhapsProcessor<Integer> sp = PerhapsProcessor.create();
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = sp.delaySubscription(pp)
        .test();

        assertFalse(sp.hasSubscribers());
        assertTrue(pp.hasSubscribers());

        ts.assertEmpty();

        pp.onNext(1);

        assertTrue(sp.hasSubscribers());
        assertFalse(pp.hasSubscribers());

        sp.onNext(1);

        ts.assertResult(1);

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void toFlowable() {
        Perhaps.just(1)
        .toFlowable()
        .test()
        .assertResult(1);
    }

    @Test
    public void toFlowableError() {
        Perhaps.error(new IOException())
        .toFlowable()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void toObservable() {
        Perhaps.just(1)
        .toObservable()
        .test()
        .assertResult(1);
    }

    @Test
    public void toObservableError() {
        Perhaps.error(new IOException())
        .toObservable()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void toMaybe() {
        Perhaps.just(1)
        .toMaybe()
        .test()
        .assertResult(1);
    }

    @Test
    public void toMaybeError() {
        Perhaps.error(new IOException())
        .toMaybe()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void toMaybeEmpty() {
        Perhaps.empty()
        .toMaybe()
        .test()
        .assertResult();
    }

    @Test
    public void doOnSubscribe() {
        Perhaps.just(1)
        .doOnSubscribe(this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void doOnRequest() {
        Perhaps.just(1)
        .doOnRequest(this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void doOnCancel() {
        PerhapsProcessor<Integer> sp = PerhapsProcessor.create();

        TestSubscriber<Integer> ts = sp.doOnCancel(this)
        .test()
        .assertEmpty();

        assertEquals(0, count);

        assertTrue(sp.hasSubscribers());

        ts.cancel();

        assertFalse(sp.hasSubscribers());

        assertEquals(1, count);
    }

    @Test
    public void doOnNext() {
        Perhaps.just(1)
        .doOnNext(this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void doOnAfterNext() {
        Perhaps.just(1)
        .doAfterNext(this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void doOnError() {
        Perhaps.error(new IOException())
        .doOnError(this)
        .test()
        .assertFailure(IOException.class);

        assertEquals(1, count);
    }

    @Test
    public void doOnComplete() {
        Perhaps.just(1)
        .doOnComplete(this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void doAfterTerminate() {
        Perhaps.just(1)
        .doAfterTerminate(this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void doAfterTerminateError() {
        Perhaps.error(new IOException())
        .doAfterTerminate(this)
        .test()
        .assertFailure(IOException.class);

        assertEquals(1, count);
    }

    @Test
    public void doFinally() {
        Perhaps.just(1)
        .doFinally(this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void doFinallyError() {
        Perhaps.error(new IOException())
        .doFinally(this)
        .test()
        .assertFailure(IOException.class);

        assertEquals(1, count);
    }

    @Test
    public void doFinallyCancel() {
        TestSubscriber<Object> ts = Perhaps.never()
        .doFinally(this)
        .test();

        assertEquals(0, count);

        ts.cancel();

        assertEquals(1, count);
    }

    @Test
    public void ignoreElement() {
        Perhaps.just(1)
        .ignoreElement()
        .test()
        .assertResult();
    }

    @Test
    public void ignoreElementError() {
        Perhaps.error(new IOException())
        .ignoreElement()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void hide() {
        assertFalse(Perhaps.just(1).hide() instanceof PerhapsJust);
    }

    @Test
    public void hideNormal() {
        Perhaps.just(1).hide()
        .test()
        .assertResult(1);
    }

    @Test
    public void hideError() {
        Perhaps.error(new IOException())
        .hide()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void hideCancel() {
        PerhapsProcessor<Object> sp = PerhapsProcessor.create();

        TestSubscriber<Object> ts = sp
        .hide()
        .test();

        assertTrue(sp.hasSubscribers());

        ts.cancel();

        assertFalse(sp.hasSubscribers());

        ts.assertEmpty();
    }

    @Test
    public void to() {
        Perhaps.just(1)
        .to(Functions.<Perhaps<Integer>>identity())
        .test()
        .assertResult(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toThrows() {
        Perhaps.just(1)
        .to(new Function<Perhaps<Integer>, Object>() {
            @Override
            public Object apply(Perhaps<Integer> sp) throws Exception {
                throw new IllegalArgumentException();
            }
        });
    }

    @Test
    public void compose() {
        Perhaps.just(1)
        .compose(new Function<Perhaps<Integer>, Perhaps<Object>>() {
            @Override
            public Perhaps<Object> apply(Perhaps<Integer> v) throws Exception {
                return v.map(new Function<Integer, Object>() {
                    @Override
                    public Object apply(Integer w) throws Exception {
                        return w + 1;
                    }
                });
            }
        })
        .test()
        .assertResult(2);
    }

    @Test
    public void lift() {
        Perhaps.just(1)
        .lift(new Function<Subscriber<? super Integer>, Subscriber<? super Integer>>() {
            @Override
            public Subscriber<? super Integer> apply(Subscriber<? super Integer> v) throws Exception {
                return v;
            }
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void repeat() {
        Perhaps.just(1)
        .repeat()
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatError() {
        Perhaps.error(new IOException())
        .repeat()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void repeatTimes() {
        Perhaps.just(1)
        .repeat(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void repeatTimesNegative() {
        Perhaps.just(1)
        .repeat(-5);
    }

    @Test
    public void repeatStop() {
        final int[] counts = { 0 };

        Perhaps.just(1)
        .repeat(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return ++counts[0] == 5;
            }
        })
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatStopThrows() {
        Perhaps.just(1)
        .repeat(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void repeatWhen() {
        Perhaps.just(1)
        .repeatWhen(new Function<Flowable<Object>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Object> f) throws Exception {
                return Flowable.range(1, 5);
            }
        })
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatWhenErrors() {
        Perhaps.just(1)
        .repeatWhen(new Function<Flowable<Object>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Object> f) throws Exception {
                return f.map(new Function<Object, Integer>() {
                    @Override
                    public Integer apply(Object v) throws Exception {
                        throw new IllegalArgumentException();
                    }
                });
            }
        })
        .test()
        .assertFailure(IllegalArgumentException.class, 1);
    }

    @Test
    public void retryNormal() {
        Perhaps.just(1)
        .retry()
        .test()
        .assertResult(1);
    }

    @Test
    public void retryTimes() {
        Perhaps.error(new IOException())
        .retry(5)
        .test()
        .assertFailure(IOException.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void retryTimesNegative() {
        Perhaps.just(1)
        .retry(-5);
    }

    @Test
    public void retryInfinite() {
        final int[] counter = { 0 };

        Perhaps.defer(new Supplier<Perhaps<Integer>>() {
            @Override
            public Perhaps<Integer> get() throws Exception {
                if (++counter[0] == 5) {
                    return Perhaps.just(1);
                }
                return Perhaps.<Integer>error(new IOException());
            }
        })
        .retry()
        .test()
        .assertResult(1);
    }

    @Test
    public void retryPredicate() {
        final int[] counter = { 0 };

        Perhaps.defer(new Supplier<Perhaps<Integer>>() {
            @Override
            public Perhaps<Integer> get() throws Exception {
                if (++counter[0] == 5) {
                    return Perhaps.<Integer>error(new IllegalArgumentException());
                }
                return Perhaps.<Integer>error(new IOException());
            }
        })
        .retry(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable e) throws Exception {
                return e instanceof IOException;
            }
        })
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void retryPredicateThrows() {
        Perhaps.error(new IOException())
        .retry(new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable e) throws Exception {
                throw new IllegalArgumentException();
            }
        })
        .test()
        .assertFailure(CompositeException.class);
    }

    @Test
    public void retryWhenNormal() {
        Perhaps.just(1)
        .retryWhen(Functions.<Flowable<Throwable>>identity())
        .test()
        .assertResult(1);
    }

    @Test
    public void retryWhen() {
        Perhaps.error(new IOException())
        .retryWhen(new Function<Flowable<Throwable>, Publisher<Throwable>>() {
            @Override
            public Publisher<Throwable> apply(Flowable<Throwable> f) throws Exception {
                return f.take(5);
            }
        })
        .test()
        .assertFailure(NoSuchElementException.class);
    }

    @Test
    public void subscribeOn() {
        String main = Thread.currentThread().getName();

        String other = Perhaps.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return Thread.currentThread().getName();
            }
        })
        .subscribeOn(Schedulers.single())
        .blockingGet();

        assertNotNull(other);
        assertNotEquals(main, other);
    }

    @Test
    public void subscribeOnError() {
        String main = Thread.currentThread().getName();
        final String[] other = { null };

        Perhaps.error(new IOException())
        .subscribeOn(Schedulers.single())
        .mapError(new Function<Throwable, Throwable>() {
            @Override
            public Throwable apply(Throwable e) throws Exception {
                other[0] = Thread.currentThread().getName();
                return e;
            }
        })
        .onErrorReturnItem(0)
        .blockingGet(5, TimeUnit.SECONDS);

        assertNotNull(other);
        assertNotEquals(main, other);
    }

    @Test
    public void observeOn() {
        String main = Thread.currentThread().getName();

        String other = Perhaps.just(1)
        .observeOn(Schedulers.single())
        .map(new Function<Integer, String>() {
            @Override
            public String apply(Integer v) throws Exception {
                return Thread.currentThread().getName();
            }
        })
        .blockingGet(5, TimeUnit.SECONDS);

        assertNotNull(other);
        assertNotEquals(main, other);
    }

    @Test
    public void observeOnHidden() {
        String main = Thread.currentThread().getName();

        String other = Perhaps.just(1).hide()
        .observeOn(Schedulers.single())
        .map(new Function<Integer, String>() {
            @Override
            public String apply(Integer v) throws Exception {
                return Thread.currentThread().getName();
            }
        })
        .blockingGet(5, TimeUnit.SECONDS);

        assertNotNull(other);
        assertNotEquals(main, other);
    }

    @Test
    public void observeOnError() {
        String main = Thread.currentThread().getName();
        final String[] other = { null };

        Perhaps.error(new IOException())
        .observeOn(Schedulers.single())
        .mapError(new Function<Throwable, Throwable>() {
            @Override
            public Throwable apply(Throwable e) throws Exception {
                other[0] = Thread.currentThread().getName();
                return e;
            }
        })
        .onErrorReturnItem(0)
        .blockingGet(5, TimeUnit.SECONDS);

        assertNotNull(other);
        assertNotEquals(main, other);
    }

    @Test
    public void unsubscribeOn() throws Exception {
        String main = Thread.currentThread().getName();

        final String[] other = { null };
        final CountDownLatch cdl = new CountDownLatch(1);

        Perhaps.never()
        .doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                other[0] = Thread.currentThread().getName();
                cdl.countDown();
            }
        })
        .unsubscribeOn(Schedulers.single())
        .test(true)
        ;

        assertTrue(cdl.await(5, TimeUnit.SECONDS));

        assertNotNull(other);
        assertNotEquals(main, other);
    }

    @Test
    public void unsubscribeOnNormal() {
        Perhaps.just(1)
        .unsubscribeOn(Schedulers.single())
        .test(false)
        .assertResult(1);
    }

    @Test
    public void unsubscribeOnError() {
        Perhaps.error(new IOException())
        .unsubscribeOn(Schedulers.single())
        .test()
        .assertFailure(IOException.class);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeActualError() {
        new Perhaps<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                throw new NullPointerException();
            }
        }
        .subscribe();
    }

    @Test
    public void subscribeActualError2() {
        try {
            new Perhaps<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    throw new IllegalArgumentException();
                }
            }
            .subscribe();
        } catch (NullPointerException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void subscribeWith() {
        TestSubscriber<Integer> ts = Perhaps.just(1)
                .subscribeWith(new TestSubscriber<Integer>());

        ts.assertResult(1);
    }

    @Test
    public void subscribeZeroArg() {
        Perhaps.just(1)
        .doOnNext(this)
        .subscribe();

        assertEquals(1, count);
    }

    @Test
    public void subscribeDispose() {
        PerhapsProcessor<Integer> sp = PerhapsProcessor.create();

        Disposable d = sp.subscribe();

        assertTrue(sp.hasSubscribers());

        d.dispose();

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void subscribeOneArg() {
        Perhaps.just(1).subscribe(this);

        assertEquals(1, count);
    }

    @Test
    public void subscribeTwoArg() {
        Perhaps.just(1).subscribe(this, this);

        assertEquals(1, count);
    }

    @Test
    public void subscribeTwoArgError() {
        Perhaps.error(new IOException()).subscribe(this, this);

        assertEquals(1, count);
    }

    @Test
    public void subscribeThreeArg() {
        Perhaps.just(1).subscribe(this, this, this);

        assertEquals(2, count);
    }

    @Test
    public void subscribeThreeArgError() {
        Perhaps.error(new IOException()).subscribe(this, this, this);

        assertEquals(1, count);
    }

    @Test
    public void blockingSubscribeZeroArg() {
        Perhaps.just(1)
        .doOnNext(this)
        .blockingSubscribe();

        assertEquals(1, count);
    }

    @Test
    public void blockingSubscribeOneArg() {
        Perhaps.just(1).blockingSubscribe(this);

        assertEquals(1, count);
    }

    @Test
    public void blockingSubscribeTwoArg() {
        Perhaps.just(1).blockingSubscribe(this, this);

        assertEquals(1, count);
    }

    @Test
    public void blockingSubscribeTwoArgError() {
        Perhaps.error(new IOException()).blockingSubscribe(this, this);

        assertEquals(1, count);
    }

    @Test
    public void blockingSubscribeThreeArg() {
        Perhaps.just(1).blockingSubscribe(this, this, this);

        assertEquals(2, count);
    }

    @Test
    public void blockingSubscribeThreeArgError() {
        Perhaps.error(new IOException()).blockingSubscribe(this, this, this);

        assertEquals(1, count);
    }

    @Test
    public void fromAction() {
        Perhaps.fromAction(this)
        .test()
        .assertResult();

        assertEquals(1, count);
    }

    @Test
    public void fromMaybe() {
        Perhaps.fromMaybe(Maybe.just(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void fromMaybeEmpty() {
        Perhaps.fromMaybe(Maybe.empty())
        .test()
        .assertResult();
    }

    @Test
    public void fromMaybeError() {
        Perhaps.fromMaybe(Maybe.error(new IOException()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void fromCompletableEmpty() {
        Perhaps.fromCompletable(Completable.complete())
        .test()
        .assertResult();
    }

    @Test
    public void fromCompletableError() {
        Perhaps.fromCompletable(Completable.error(new IOException()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void concatDelayErrorTill() {
        Perhaps.concatDelayError(Flowable.just(Perhaps.just(1), Perhaps.just(2)), 1, true)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void mergeMaxConcurrent() {
        Perhaps.merge(Flowable.just(Perhaps.just(1), Perhaps.just(2)), 1)
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableMaxConcurrent() {
        Perhaps.merge(Arrays.asList(Perhaps.just(1), Perhaps.just(2)), 1)
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayMaxConcurrent() {
        Perhaps.mergeArray(1, Perhaps.just(1), Perhaps.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayDelayErrorMaxConcurrent() {
        Perhaps.mergeArrayDelayError(1, Perhaps.just(1), Perhaps.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableDelayErrorMaxConcurrent() {
        Perhaps.mergeDelayError(Arrays.asList(Perhaps.just(1), Perhaps.just(2)), 1)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void onErrorComplete() {
        Perhaps.error(new IOException())
        .onErrorComplete()
        .test()
        .assertResult();
    }

    @Test
    public void defaultIfEmpty() {
        Perhaps.empty()
        .defaultIfEmpty(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void switchIfEmpty() {
        Perhaps.empty()
        .switchIfEmpty(Perhaps.just(2))
        .test()
        .assertResult(2);
    }

    @Test
    public void switchIfEmptyToEmpty() {
        Perhaps.empty()
        .switchIfEmpty(Perhaps.empty())
        .test()
        .assertResult();
    }

    @Test
    public void switchIfEmptyToError() {
        Perhaps.empty()
        .switchIfEmpty(Perhaps.error(new IOException()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void switchIfEmptyError() {
        Perhaps.error(new IOException())
        .switchIfEmpty(Perhaps.just(2))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void switchIfEmptyNotEmpty() {
        Perhaps.just(1)
        .switchIfEmpty(Perhaps.just(2))
        .test()
        .assertResult(1);
    }

    @Test
    public void cache() {
        Perhaps<Integer> np = Perhaps.just(1)
        .doOnSubscribe(this)
        .cache();

        assertEquals(0, count);

        np.test().assertResult(1);

        assertEquals(1, count);

        np.test().assertResult(1);

        assertEquals(1, count);

        np.test().assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void cacheError() {
        Perhaps<Integer> np = Perhaps.<Integer>error(new IOException())
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

    @Test
    public void cacheEmpty() {
        Perhaps<Integer> np = Perhaps.<Integer>empty()
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
}
