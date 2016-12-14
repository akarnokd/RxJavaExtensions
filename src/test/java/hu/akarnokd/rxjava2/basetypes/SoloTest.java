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

package hu.akarnokd.rxjava2.basetypes;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import org.junit.Test;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.*;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class SoloTest implements Consumer<Object>, Action, LongConsumer, Cancellable {

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
        assertNull(Solo.getOnAssemblyHandler());

        Solo.setOnAssemblyHandler(new Function<Solo<Object>, Solo<Object>>() {
            @Override
            public Solo<Object> apply(Solo<Object> sp) throws Exception {
                count++;
                return sp;
            }
        });

        Solo.just(1)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                return v + 1;
            }
        })
        .test()
        .assertResult(2);

        assertEquals(2, count);

        Solo.setOnAssemblyHandler(new Function<Solo<Object>, Solo<Object>>() {
            @Override
            public Solo<Object> apply(Solo<Object> sp) throws Exception {
                throw new IllegalStateException();
            }
        });

        try {
            Solo.just(1);
            fail("Should have thrown");
        } catch (IllegalStateException ex) {
            // expected
        }

        Solo.setOnAssemblyHandler(null);

        Solo.just(1)
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
    public void createSuccess() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Solo.create(new SingleOnSubscribe<Object>() {
                @Override
                public void subscribe(SingleEmitter<Object> e) throws Exception {
                    e.setCancellable(SoloTest.this);
                    e.onSuccess(1);
                    e.onSuccess(2);
                    e.onError(new IOException());
                }
            })
            .test()
            .assertResult(1);

            assertEquals(1, count);

            TestHelper.assertError(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void createSuccessNoResource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Solo.create(new SingleOnSubscribe<Object>() {
                @Override
                public void subscribe(SingleEmitter<Object> e) throws Exception {
                    e.onSuccess(1);
                    e.onSuccess(2);
                    e.onError(new IOException());
                }
            })
            .test()
            .assertResult(1);

            TestHelper.assertError(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void createError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Solo.create(new SingleOnSubscribe<Object>() {
                @Override
                public void subscribe(SingleEmitter<Object> e) throws Exception {
                    e.setCancellable(SoloTest.this);
                    e.onError(new IOException());
                    e.onSuccess(2);
                    e.onError(new IOException());
                }
            })
            .test()
            .assertFailure(IOException.class);

            assertEquals(1, count);
            TestHelper.assertError(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void createErrorNoResource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Solo.create(new SingleOnSubscribe<Object>() {
                @Override
                public void subscribe(SingleEmitter<Object> e) throws Exception {
                    e.onError(new IOException());
                    e.onSuccess(2);
                    e.onError(new IOException());
                }
            })
            .test()
            .assertFailure(IOException.class);

            TestHelper.assertError(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void error() {
        Solo.error(new IOException())
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void errorSupplier() {
        Solo.error(new Callable<Throwable>() {
            @Override
            public Throwable call() throws Exception {
                return new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void errorSupplierThrows() {
        Solo.error(new Callable<Throwable>() {
            @Override
            public Throwable call() throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void errorSupplierNull() {
        Solo.error(new Callable<Throwable>() {
            @Override
            public Throwable call() throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void fromCallable() {
        Solo.fromCallable(Functions.justCallable(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void fromCallableNull() {
        Solo.fromCallable(new Callable<Object>() {
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
        Solo.fromCallable(new Callable<Object>() {
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
        Solo.never()
        .test()
        .assertEmpty();
    }

    @Test
    public void defer() {
        Solo<Integer> sp = Solo.defer(new Callable<Solo<Integer>>() {
            int i;

            @Override
            public Solo<Integer> call() throws Exception {
                return Solo.just(++i);
            }
        });

        sp.test().assertResult(1);

        sp.test().assertResult(2);
    }

    @Test
    public void deferThrows() {
        Solo<Integer> sp = Solo.defer(new Callable<Solo<Integer>>() {
            @Override
            public Solo<Integer> call() throws Exception {
                throw new IOException();
            }
        });

        sp.test().assertFailure(IOException.class);

        sp.test().assertFailure(IOException.class);
    }

    @Test
    public void fromPublisher0() {
        Solo.fromPublisher(Flowable.empty())
        .test()
        .assertFailure(NoSuchElementException.class);
    }

    @Test
    public void fromPublisherSolo() {
        Solo.fromPublisher(Solo.just(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void fromPublisher1() {
        Solo.fromPublisher(Flowable.just(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void fromPublisher2() {
        Solo.fromPublisher(Flowable.range(1, 2))
        .test()
        .assertFailure(IndexOutOfBoundsException.class);
    }

    @Test
    public void fromPublisherError() {
        Solo.fromPublisher(Flowable.error(new IOException()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void fromSingle() {
        Solo.fromSingle(Single.just(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void fromSingleBackpressured() {
        Solo.fromSingle(Single.just(1))
        .test(0)
        .assertEmpty()
        .requestMore(1)
        .assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable1() {
        SoloProcessor<Integer> sp1 = SoloProcessor.create();
        SoloProcessor<Integer> sp2 = SoloProcessor.create();

        TestSubscriber<Integer> ts = Solo.amb(Arrays.asList(sp1, sp2))
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
        SoloProcessor<Integer> sp1 = SoloProcessor.create();
        SoloProcessor<Integer> sp2 = SoloProcessor.create();

        TestSubscriber<Integer> ts = Solo.amb(Arrays.asList(sp1, sp2))
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
        SoloProcessor<Integer> sp1 = SoloProcessor.create();
        SoloProcessor<Integer> sp2 = SoloProcessor.create();

        TestSubscriber<Integer> ts = Solo.amb(Arrays.asList(sp1, sp2))
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
        SoloProcessor<Integer> sp1 = SoloProcessor.create();
        SoloProcessor<Integer> sp2 = SoloProcessor.create();

        TestSubscriber<Integer> ts = Solo.amb(Arrays.asList(sp1, sp2))
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
        SoloProcessor<Integer> sp1 = SoloProcessor.create();
        SoloProcessor<Integer> sp2 = SoloProcessor.create();

        TestSubscriber<Integer> ts = Solo.ambArray(sp1, sp2)
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
        SoloProcessor<Integer> sp1 = SoloProcessor.create();
        SoloProcessor<Integer> sp2 = SoloProcessor.create();

        TestSubscriber<Integer> ts = Solo.ambArray(sp1, sp2)
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
        SoloProcessor<Integer> sp1 = SoloProcessor.create();
        SoloProcessor<Integer> sp2 = SoloProcessor.create();

        TestSubscriber<Integer> ts = Solo.ambArray(sp1, sp2)
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
        SoloProcessor<Integer> sp1 = SoloProcessor.create();
        SoloProcessor<Integer> sp2 = SoloProcessor.create();

        TestSubscriber<Integer> ts = Solo.ambArray(sp1, sp2)
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
        Solo.concat(Arrays.asList(Solo.just(1), Solo.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableError() {
        Solo.concat(Arrays.asList(Solo.just(1), Solo.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableError2() {
        Solo.concat(Arrays.asList(Solo.error(new IOException()), Solo.just(2)))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void concatPublisher() {
        Solo.concat(Flowable.just(Solo.just(1), Solo.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatPublisherPrefetch() {
        Solo.concat(Flowable.just(Solo.just(1), Solo.just(2)), 1)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatPublisherError() {
        Solo.concat(Flowable.just(Solo.just(1), Solo.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void concatPublisherError2() {
        Solo.concat(Flowable.just(Solo.error(new IOException()), Solo.just(2)))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void concatPublisherError3() {
        Solo.concat(
                Flowable.just(Solo.just(1))
                .concatWith(Flowable.<Solo<Integer>>error(new IOException()))
        )
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArray() {
        Solo.concatArray(Solo.just(1), Solo.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayError() {
        Solo.concatArray(Solo.just(1), Solo.error(new IOException()))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayError2() {
        Solo.concatArray(Solo.error(new IOException()), Solo.just(2))
        .test()
        .assertFailure(IOException.class);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableDelayError() {
        Solo.concatDelayError(Arrays.asList(Solo.just(1), Solo.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableDelayErrorError() {
        Solo.concatDelayError(Arrays.asList(Solo.just(1), Solo.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableDelayErrorError2() {
        Solo.concatDelayError(Arrays.asList(Solo.error(new IOException()), Solo.just(2)))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void concatDelayErrorPublisher() {
        Solo.concatDelayError(Flowable.just(Solo.just(1), Solo.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatDelayErrorPublisherPrefetch() {
        Solo.concatDelayError(Flowable.just(Solo.just(1), Solo.just(2)), 1)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatDelayErrorPublisherError() {
        Solo.concatDelayError(Flowable.just(Solo.just(1), Solo.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void concatDelayErrorPublisherError2() {
        Solo.concatDelayError(Flowable.just(Solo.error(new IOException()), Solo.just(2)))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void concatDelayErrorPublisherError3() {
        Solo.concatDelayError(
                Flowable.just(Solo.just(1))
                .concatWith(Flowable.<Solo<Integer>>error(new IOException()))
        )
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayDelayError() {
        Solo.concatArrayDelayError(Solo.just(1), Solo.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayDelayErrorError() {
        Solo.concatArrayDelayError(Solo.just(1), Solo.error(new IOException()))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayDelayErrorError2() {
        Solo.concatArrayDelayError(Solo.error(new IOException()), Solo.just(2))
        .test()
        .assertFailure(IOException.class, 2);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterable() {
        Solo.merge(Arrays.asList(Solo.just(1), Solo.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableError() {
        Solo.merge(Arrays.asList(Solo.just(1), Solo.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableError2() {
        Solo.merge(Arrays.asList(Solo.error(new IOException()), Solo.just(2)))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void mergePublisher() {
        Solo.merge(Flowable.just(Solo.just(1), Solo.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void mergePublisherPrefetch() {
        Solo.merge(Flowable.just(Solo.just(1), Solo.just(2)), 1)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void mergePublisherError() {
        Solo.merge(Flowable.just(Solo.just(1), Solo.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void mergePublisherError2() {
        Solo.merge(Flowable.just(Solo.error(new IOException()), Solo.just(2)))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void mergePublisherError3() {
        Solo.merge(
                Flowable.just(Solo.just(1))
                .concatWith(Flowable.<Solo<Integer>>error(new IOException()))
        )
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArray() {
        Solo.mergeArray(Solo.just(1), Solo.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayError() {
        Solo.mergeArray(Solo.just(1), Solo.error(new IOException()))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayError2() {
        Solo.mergeArray(Solo.error(new IOException()), Solo.just(2))
        .test()
        .assertFailure(IOException.class);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableDelayError() {
        Solo.mergeDelayError(Arrays.asList(Solo.just(1), Solo.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableDelayErrorError() {
        Solo.mergeDelayError(Arrays.asList(Solo.just(1), Solo.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableDelayErrorError2() {
        Solo.mergeDelayError(Arrays.asList(Solo.error(new IOException()), Solo.just(2)))
        .test()
        .assertFailure(IOException.class, 2);
    }

    @Test
    public void mergeDelayErrorPublisher() {
        Solo.mergeDelayError(Flowable.just(Solo.just(1), Solo.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void mergeDelayErrorPublisherPrefetch() {
        Solo.mergeDelayError(Flowable.just(Solo.just(1), Solo.just(2)), 1)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void mergeDelayErrorPublisherError() {
        Solo.mergeDelayError(Flowable.just(Solo.just(1), Solo.error(new IOException())))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void mergeDelayErrorPublisherError2() {
        Solo.mergeDelayError(Flowable.just(Solo.error(new IOException()), Solo.just(2)))
        .test()
        .assertFailure(IOException.class, 2);
    }

    @Test
    public void mergeDelayErrorPublisherError3() {
        Solo.mergeDelayError(
                Flowable.just(Solo.just(1))
                .concatWith(Flowable.<Solo<Integer>>error(new IOException()))
        )
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayDelayError() {
        Solo.mergeArrayDelayError(Solo.just(1), Solo.just(2))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayDelayErrorError() {
        Solo.mergeArrayDelayError(Solo.just(1), Solo.error(new IOException()))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayDelayErrorError2() {
        Solo.mergeArrayDelayError(Solo.error(new IOException()), Solo.just(2))
        .test()
        .assertFailure(IOException.class, 2);
    }

    @Test
    public void timer() {
        Solo.timer(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0L);
    }

    @Test
    public void timerScheduler() {
        Solo.timer(100, TimeUnit.MILLISECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0L);
    }

    @Test
    public void usingEager() {
        Solo.using(Functions.justCallable(1),
                Functions.justFunction(Solo.just(1)), this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void usingEagerError() {
        Solo.using(Functions.justCallable(1),
                Functions.justFunction(Solo.error(new IOException())), this)
        .test()
        .assertFailure(IOException.class);

        assertEquals(1, count);
    }

    @Test
    public void usingNonEager() {
        Solo.using(Functions.justCallable(1),
                Functions.justFunction(Solo.just(1)), this,
                false)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zip() {
        Solo.zip(Arrays.asList(Solo.just(1), Solo.just(2)), new Function<Object[], Object>() {
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
        Solo.zip(Arrays.asList(Solo.error(new IOException()), Solo.just(2)), new Function<Object[], Object>() {
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
        Solo.zip(Arrays.asList(Solo.just(1), Solo.error(new IOException())), new Function<Object[], Object>() {
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
        Solo.zipArray(new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return "" + a[0] + a[1];
            }
        }, Solo.just(1), Solo.just(2))
        .test()
        .assertResult("12");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipArrayError1() {
        Solo.zipArray(new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return "" + a[0] + a[1];
            }
        }, Solo.error(new IOException()), Solo.just(2))
        .test()
        .assertFailure(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipArrayError2() {
        Solo.zipArray(new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return "" + a[0] + a[1];
            }
        }, Solo.just(1), Solo.error(new IOException()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void ambWith1() {
        SoloProcessor<Integer> sp1 = SoloProcessor.create();
        SoloProcessor<Integer> sp2 = SoloProcessor.create();

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
        SoloProcessor<Integer> sp1 = SoloProcessor.create();
        SoloProcessor<Integer> sp2 = SoloProcessor.create();

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
        SoloProcessor<Integer> sp1 = SoloProcessor.create();
        SoloProcessor<Integer> sp2 = SoloProcessor.create();

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
        SoloProcessor<Integer> sp1 = SoloProcessor.create();
        SoloProcessor<Integer> sp2 = SoloProcessor.create();

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
        Solo.just(1).andThen(Nono.fromAction(this))
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void andThenNonoError() {
        Solo.just(1).andThen(Nono.error(new IOException()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void andThenErrorNono() {
        Solo.error(new IOException()).andThen(Nono.fromAction(this))
        .test()
        .assertFailure(IOException.class);

        assertEquals(0, count);
    }

    @Test
    public void andThenPublisher() {
        Solo.just(1).andThen(Flowable.just(2))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void andThenPublisherError() {
        Solo.just(1).andThen(Flowable.<Integer>error(new IOException()))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void andThenErrorPublisher() {
        Solo.error(new IOException()).andThen(Flowable.just(2))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void concatWith() {
        Solo.just(1).concatWith(Solo.just(2))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatWithError() {
        Solo.just(1).concatWith(Solo.<Integer>error(new IOException()))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void concatWithErrorSolo() {
        Solo.<Integer>error(new IOException()).concatWith(Solo.just(2))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void mergeWith() {
        Solo.just(1).mergeWith(Solo.just(2))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void mergeWithError() {
        Solo.just(1).mergeWith(Solo.<Integer>error(new IOException()))
        .test()
        .assertFailure(IOException.class, 1);
    }

    @Test
    public void mergeWithErrorSolo() {
        Solo.<Integer>error(new IOException()).mergeWith(Solo.just(2))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void zipWith() {
        Solo.just(1).zipWith(Solo.just(2), new BiFunction<Integer, Integer, Integer>() {
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
        Solo.just(1).map(new Function<Integer, Object>() {
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
        Solo.just(1).map(new Function<Integer, Object>() {
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
        Solo.just(1).map(new Function<Integer, Object>() {
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
        Solo.just(1)
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
        Solo.error(new IOException())
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
        Solo.error(new IOException())
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
        Solo.just(1).filter(Functions.alwaysTrue())
        .test()
        .assertResult(1);
    }

    @Test
    public void filterFalse() {
        Solo.just(1).filter(Functions.alwaysFalse())
        .test()
        .assertResult();
    }

    @Test
    public void filterError() {
        Solo.error(new IOException()).filter(Functions.alwaysTrue())
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void filterThrows() {
        Solo.just(1).filter(new Predicate<Integer>() {
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
        Solo.just(1).timeout(1, TimeUnit.MINUTES)
        .test()
        .assertResult(1);
    }

    @Test
    public void timeoutError() {
        Solo.error(new IOException()).timeout(1, TimeUnit.MINUTES)
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void timeout() {
        SoloProcessor<Integer> sp = SoloProcessor.create();

        sp.timeout(10, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void timeoutScheduler() {
        SoloProcessor<Integer> sp = SoloProcessor.create();

        sp.timeout(10, TimeUnit.MILLISECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void timeoutFallback() {
        SoloProcessor<Integer> sp = SoloProcessor.create();

        sp.timeout(10, TimeUnit.MILLISECONDS, Solo.just(1))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void timeoutFallbackScheduler() {
        SoloProcessor<Integer> sp = SoloProcessor.create();

        sp.timeout(10, TimeUnit.MILLISECONDS, Schedulers.single(), Solo.just(1))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void timeoutPublisher() {
        SoloProcessor<Integer> sp = SoloProcessor.create();

        sp.timeout(Flowable.empty())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void timeoutPublisherFallback() {
        SoloProcessor<Integer> sp = SoloProcessor.create();

        sp.timeout(Flowable.empty(), Solo.just(1))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void onErrorReturnItem() {
        Solo.just(1).onErrorReturnItem(2)
        .test()
        .assertResult(1);
    }

    @Test
    public void onErrorReturnItemError() {
        Solo.error(new IOException()).onErrorReturnItem(2)
        .test()
        .assertResult(2);
    }

    @Test
    public void onErrorResumeWith() {
        Solo.just(1).onErrorResumeWith(Solo.just(2))
        .test()
        .assertResult(1);
    }

    @Test
    public void onErrorResumeWithError() {
        Solo.<Integer>error(new IOException()).onErrorResumeWith(Solo.just(2))
        .test()
        .assertResult(2);
    }

    @Test
    public void onErrorResumeNext() {
        Solo.just(1).onErrorResumeNext(Functions.justFunction(Solo.just(2)))
        .test()
        .assertResult(1);
    }

    @Test
    public void onErrorResumeNextError() {
        Solo.<Integer>error(new IOException()).onErrorResumeNext(Functions.justFunction(Solo.just(2)))
        .test()
        .assertResult(2);
    }

    @Test
    public void onErrorResumeNextErrorThrows() {
        Solo.<Integer>error(new IOException())
        .onErrorResumeNext(new Function<Throwable, Solo<Integer>>() {
            @Override
            public Solo<Integer> apply(Throwable e) throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(CompositeException.class);
    }

    @Test
    public void flatMap() {
        Solo.just(1).flatMap(Functions.justFunction(Solo.just(2)))
        .test()
        .assertResult(2);
    }

    @Test
    public void flatMapError() {
        Solo.just(1).flatMap(Functions.justFunction(Solo.error(new IOException())))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void flatMapError2() {
        Solo.error(new IOException()).flatMap(Functions.justFunction(Solo.just(2)))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void flatMapSignal() {
        Solo.just(1).flatMap(
                Functions.justFunction(Solo.just(2)),
                Functions.justFunction(Solo.just(3))
        )
        .test()
        .assertResult(2);
    }

    @Test
    public void flatMapSignalError() {
        Solo.error(new IOException()).flatMap(
                Functions.justFunction(Solo.just(2)),
                Functions.justFunction(Solo.just(3))
        )
        .test()
        .assertResult(3);
    }

    @Test
    public void flatMapSignalError2() {
        Solo.just(1).flatMap(
                Functions.justFunction(Solo.error(new IOException())),
                Functions.justFunction(Solo.just(3))
        )
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void flatMapSignalError3() {
        Solo.error(new IOException()).flatMap(
                Functions.justFunction(Solo.just(2)),
                Functions.justFunction(Solo.error(new IllegalArgumentException()))
        )
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void flatMapPublisher() {
        Solo.just(1).flatMapPublisher(Functions.justFunction(Flowable.range(1, 5)))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void flatMapPublisherBackpressure() {
        Solo.just(1).flatMapPublisher(Functions.justFunction(Flowable.range(1, 5)))
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void flatMapPublisherError() {
        Solo.error(new IOException()).flatMapPublisher(Functions.justFunction(Flowable.range(1, 5)))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void flatMapPublisherError2() {
        Solo.just(1)
        .flatMapPublisher(Functions.justFunction(Flowable.error(new IOException())))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void takeUnit() {
        Solo.just(1).takeUntil(Solo.never())
        .test()
        .assertResult(1);
    }

    @Test
    public void takeUnitHot() {
        SoloProcessor<Integer> sp1 = SoloProcessor.create();
        SoloProcessor<Integer> sp2 = SoloProcessor.create();

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
        Solo.error(new IOException()).takeUntil(Solo.never())
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void takeUnitSwitch() {
        Solo.just(1).takeUntil(Solo.just(1))
        .test()
        .assertFailure(NoSuchElementException.class);
    }

    @Test
    public void takeUnitSwitchNever() {
        Solo.just(1).takeUntil(Flowable.empty())
        .test()
        .assertFailure(NoSuchElementException.class);
    }

    @Test
    public void takeUnitHotSwitch() {
        SoloProcessor<Integer> sp1 = SoloProcessor.create();
        SoloProcessor<Integer> sp2 = SoloProcessor.create();

        TestSubscriber<Integer> ts = sp1.takeUntil(sp2)
        .test()
        .assertEmpty();

        assertTrue(sp1.hasSubscribers());
        assertTrue(sp2.hasSubscribers());

        sp2.onNext(1);

        assertFalse(sp1.hasSubscribers());
        assertFalse(sp2.hasSubscribers());

        ts.assertFailure(NoSuchElementException.class);
    }

    @Test
    public void delay() {
        Solo.just(1).delay(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delayScheduler() {
        Solo.just(1).delay(100, TimeUnit.MILLISECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delayError() {
        Solo.error(new IOException()).delay(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void delayPublisher() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Solo.just(1).delay(pp).test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onNext(2);

        ts.assertResult(1);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void delayPublisherError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = Solo.<Integer>error(new IOException())
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

        TestSubscriber<Integer> ts = Solo.<Integer>error(new IOException())
                .delay(pp).test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onError(new IllegalArgumentException());

        ts.assertFailure(CompositeException.class);

        assertFalse(pp.hasSubscribers());
    }
}

