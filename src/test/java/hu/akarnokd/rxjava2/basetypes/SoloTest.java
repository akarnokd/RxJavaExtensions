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

package hu.akarnokd.rxjava2.basetypes;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import org.junit.Test;
import org.reactivestreams.*;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.*;
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

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
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

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
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
            TestHelper.assertUndeliverable(errors, 0, IOException.class);
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

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
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

    @Test
    public void delaySubscription() {
        Solo.just(1)
        .delaySubscription(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delaySubscriptionScheduler() {
        SoloProcessor<Integer> sp = SoloProcessor.create();
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
        SoloProcessor<Integer> sp = SoloProcessor.create();
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
        Solo.just(1)
        .toFlowable()
        .test()
        .assertResult(1);
    }

    @Test
    public void toFlowableError() {
        Solo.error(new IOException())
        .toFlowable()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void toObservable() {
        Solo.just(1)
        .toObservable()
        .test()
        .assertResult(1);
    }

    @Test
    public void toObservableError() {
        Solo.error(new IOException())
        .toObservable()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void toSingle() {
        Solo.just(1)
        .toSingle()
        .test()
        .assertResult(1);
    }

    @Test
    public void toSingleError() {
        Solo.error(new IOException())
        .toSingle()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void doOnSubscribe() {
        Solo.just(1)
        .doOnSubscribe(this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void doOnRequest() {
        Solo.just(1)
        .doOnRequest(this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void doOnCancel() {
        SoloProcessor<Integer> sp = SoloProcessor.create();

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
        Solo.just(1)
        .doOnNext(this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void doOnAfterNext() {
        Solo.just(1)
        .doAfterNext(this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void doOnError() {
        Solo.error(new IOException())
        .doOnError(this)
        .test()
        .assertFailure(IOException.class);

        assertEquals(1, count);
    }

    @Test
    public void doOnComplete() {
        Solo.just(1)
        .doOnComplete(this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void doAfterTerminate() {
        Solo.just(1)
        .doAfterTerminate(this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void doAfterTerminateError() {
        Solo.error(new IOException())
        .doAfterTerminate(this)
        .test()
        .assertFailure(IOException.class);

        assertEquals(1, count);
    }

    @Test
    public void doFinally() {
        Solo.just(1)
        .doFinally(this)
        .test()
        .assertResult(1);

        assertEquals(1, count);
    }

    @Test
    public void doFinallyError() {
        Solo.error(new IOException())
        .doFinally(this)
        .test()
        .assertFailure(IOException.class);

        assertEquals(1, count);
    }

    @Test
    public void doFinallyCancel() {
        TestSubscriber<Object> ts = Solo.never()
        .doFinally(this)
        .test();

        assertEquals(0, count);

        ts.cancel();

        assertEquals(1, count);
    }

    @Test
    public void ignoreElement() {
        Solo.just(1)
        .ignoreElement()
        .test()
        .assertResult();
    }

    @Test
    public void ignoreElementError() {
        Solo.error(new IOException())
        .ignoreElement()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void hide() {
        assertFalse(Solo.just(1).hide() instanceof SoloJust);
    }

    @Test
    public void hideNormal() {
        Solo.just(1).hide()
        .test()
        .assertResult(1);
    }

    @Test
    public void hideError() {
        Solo.error(new IOException())
        .hide()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void hideCancel() {
        SoloProcessor<Object> sp = SoloProcessor.create();

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
        Solo.just(1)
        .to(Functions.<Solo<Integer>>identity())
        .test()
        .assertResult(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toThrows() {
        Solo.just(1)
        .to(new Function<Solo<Integer>, Object>() {
            @Override
            public Object apply(Solo<Integer> sp) throws Exception {
                throw new IllegalArgumentException();
            }
        });
    }

    @Test
    public void compose() {
        Solo.just(1)
        .compose(new Function<Solo<Integer>, Solo<Object>>() {
            @Override
            public Solo<Object> apply(Solo<Integer> v) throws Exception {
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
        Solo.just(1)
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
        Solo.just(1)
        .repeat()
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatError() {
        Solo.error(new IOException())
        .repeat()
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void repeatTimes() {
        Solo.just(1)
        .repeat(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void repeatTimesNegative() {
        Solo.just(1)
        .repeat(-5);
    }

    @Test
    public void repeatStop() {
        final int[] counts = { 0 };

        Solo.just(1)
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
        Solo.just(1)
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
        Solo.just(1)
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
        Solo.just(1)
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
        Solo.just(1)
        .retry()
        .test()
        .assertResult(1);
    }

    @Test
    public void retryTimes() {
        Solo.error(new IOException())
        .retry(5)
        .test()
        .assertFailure(IOException.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void retryTimesNegative() {
        Solo.just(1)
        .retry(-5);
    }

    @Test
    public void retryInfinite() {
        final int[] counter = { 0 };

        Solo.defer(new Callable<Solo<Integer>>() {
            @Override
            public Solo<Integer> call() throws Exception {
                if (++counter[0] == 5) {
                    return Solo.just(1);
                }
                return Solo.<Integer>error(new IOException());
            }
        })
        .retry()
        .test()
        .assertResult(1);
    }

    @Test
    public void retryPredicate() {
        final int[] counter = { 0 };

        Solo.defer(new Callable<Solo<Integer>>() {
            @Override
            public Solo<Integer> call() throws Exception {
                if (++counter[0] == 5) {
                    return Solo.<Integer>error(new IllegalArgumentException());
                }
                return Solo.<Integer>error(new IOException());
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
        Solo.error(new IOException())
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
        Solo.just(1)
        .retryWhen(Functions.<Flowable<Throwable>>identity())
        .test()
        .assertResult(1);
    }

    @Test
    public void retryWhen() {
        Solo.error(new IOException())
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

        String other = Solo.fromCallable(new Callable<String>() {
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

        Solo.error(new IOException())
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

        String other = Solo.just(1)
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

        String other = Solo.just(1).hide()
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

        Solo.error(new IOException())
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

        Solo.never()
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
        Solo.just(1)
        .unsubscribeOn(Schedulers.single())
        .test(false)
        .assertResult(1);
    }

    @Test
    public void unsubscribeOnError() {
        Solo.error(new IOException())
        .unsubscribeOn(Schedulers.single())
        .test()
        .assertFailure(IOException.class);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeActualError() {
        new Solo<Integer>() {
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
            new Solo<Integer>() {
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
        TestSubscriber<Integer> ts = Solo.just(1)
                .subscribeWith(new TestSubscriber<Integer>());

        ts.assertResult(1);
    }

    @Test
    public void subscribeZeroArg() {
        Solo.just(1)
        .doOnNext(this)
        .subscribe();

        assertEquals(1, count);
    }

    @Test
    public void subscribeDispose() {
        SoloProcessor<Integer> sp = SoloProcessor.create();

        Disposable d = sp.subscribe();

        assertTrue(sp.hasSubscribers());

        d.dispose();

        assertFalse(sp.hasSubscribers());
    }

    @Test
    public void subscribeOneArg() {
        Solo.just(1).subscribe(this);

        assertEquals(1, count);
    }

    @Test
    public void subscribeTwoArg() {
        Solo.just(1).subscribe(this, this);

        assertEquals(1, count);
    }

    @Test
    public void subscribeTwoArgError() {
        Solo.error(new IOException()).subscribe(this, this);

        assertEquals(1, count);
    }

    @Test
    public void subscribeThreeArg() {
        Solo.just(1).subscribe(this, this, this);

        assertEquals(2, count);
    }

    @Test
    public void subscribeThreeArgError() {
        Solo.error(new IOException()).subscribe(this, this, this);

        assertEquals(1, count);
    }

    @Test
    public void blockingSubscribeZeroArg() {
        Solo.just(1)
        .doOnNext(this)
        .blockingSubscribe();

        assertEquals(1, count);
    }

    @Test
    public void blockingSubscribeOneArg() {
        Solo.just(1).blockingSubscribe(this);

        assertEquals(1, count);
    }

    @Test
    public void blockingSubscribeTwoArg() {
        Solo.just(1).blockingSubscribe(this, this);

        assertEquals(1, count);
    }

    @Test
    public void blockingSubscribeTwoArgError() {
        Solo.error(new IOException()).blockingSubscribe(this, this);

        assertEquals(1, count);
    }

    @Test
    public void blockingSubscribeThreeArg() {
        Solo.just(1).blockingSubscribe(this, this, this);

        assertEquals(2, count);
    }

    @Test
    public void blockingSubscribeThreeArgError() {
        Solo.error(new IOException()).blockingSubscribe(this, this, this);

        assertEquals(1, count);
    }

    @Test
    public void fromFuture() {
        FutureTask<Integer> ft = new FutureTask<Integer>(Functions.EMPTY_RUNNABLE, 1);
        ft.run();

        Solo.fromFuture(ft)
        .test()
        .assertResult(1);
    }

    @Test
    public void fromFutureNull() {
        FutureTask<Integer> ft = new FutureTask<Integer>(Functions.EMPTY_RUNNABLE, null);
        ft.run();

        Solo.fromFuture(ft)
        .test()
        .assertFailure(NoSuchElementException.class);
    }

    @Test
    public void fromFutureTimeout() {
        FutureTask<Integer> ft = new FutureTask<Integer>(Functions.EMPTY_RUNNABLE, 1);

        Solo.fromFuture(ft, 1, TimeUnit.MILLISECONDS)
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

        Solo.fromFuture(ft)
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void toFuture() throws Exception {
        assertEquals(1, Solo.just(1).toFuture().get().intValue());
    }

    @Test(expected = ExecutionException.class)
    public void toFutureError() throws Exception {
        Solo.error(new IOException()).toFuture().get();
    }

    @Test
    public void cache() {
        Solo<Integer> np = Solo.just(1)
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
        Solo<Integer> np = Solo.<Integer>error(new IOException())
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

