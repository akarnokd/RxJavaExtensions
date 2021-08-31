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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import hu.akarnokd.rxjava3.operators.FlowableGenerateAsync.AtomicCancellable;
import hu.akarnokd.rxjava3.test.*;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class FlowableGenerateAsyncTest {

    static final class SyncRange {

        final int max;

        int index;

        SyncRange(int start, int size) {
            index = start;
            max = start + size;
        }

        public Completable nextValue(Consumer<? super Integer> onValue) {
            final int i = index;
            if (i == max) {
                return Completable.complete();
            }
            index = i + 1;
            try {
                onValue.accept(i);
            } catch (Throwable ex) {
                return Completable.error(ex);
            }
            return Completable.never();
        }
    }

    static final class AsyncRange {

        final int max;

        int index;

        AsyncRange(int start, int size) {
            index = start;
            max = start + size;
        }

        public Completable nextValue(final Consumer<? super Integer> onValue) {
            final int i = index;
            if (i == max) {
                return Completable.complete();
            }
            index = i + 1;
            return Completable.fromAction(new Action() {
                @Override
                public void run() throws Throwable {
                    onValue.accept(i);
                }
            })
            .subscribeOn(Schedulers.single())
            .mergeWith(Completable.never());
        }
    }

    @Test
    public void simple() {
        final AtomicInteger cleanup = new AtomicInteger();
        Flowables.generateAsync(
                new Supplier<SyncRange>() {
                    @Override
                    public SyncRange get() throws Throwable {
                        return new SyncRange(1, 5);
                    }
                },
                new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                    @Override
                    public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        Completable c = state.nextValue(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer v) throws Throwable {
                                emitter.onNext(v);
                            }
                        });

                        final Disposable d = c.subscribe(new Action() {
                            @Override
                            public void run() throws Throwable {
                                emitter.onComplete();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable e) throws Throwable {
                                emitter.onError(e);
                            }
                        });
                        emitter.replaceCancellable(new Cancellable() {
                            @Override
                            public void cancel() throws Throwable {
                                d.dispose();
                            }
                        });

                        return state;
                    }
                },
                new Consumer<SyncRange>() {
                    @Override
                    public void accept(SyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, cleanup.get());
    }

    @Test
    public void simpleEven() {
        final AtomicInteger cleanup = new AtomicInteger();
        Flowables.generateAsync(
                new Supplier<SyncRange>() {
                    @Override
                    public SyncRange get() throws Throwable {
                        return new SyncRange(1, 12);
                    }
                },
                new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                    @Override
                    public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        Completable c = state.nextValue(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer v) throws Throwable {
                                if (v % 2 == 0) {
                                    emitter.onNext(v);
                                } else if (v == 11) {
                                    emitter.onComplete();
                                } else {
                                    emitter.onNothing();
                                }
                            }
                        });

                        final Disposable d = c.subscribe(new Action() {
                            @Override
                            public void run() throws Throwable {
                                emitter.onComplete();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable e) throws Throwable {
                                emitter.onError(e);
                            }
                        });
                        emitter.replaceCancellable(new Cancellable() {
                            @Override
                            public void cancel() throws Throwable {
                                d.dispose();
                            }
                        });

                        return state;
                    }
                },
                new Consumer<SyncRange>() {
                    @Override
                    public void accept(SyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .test()
        .assertResult(2, 4, 6, 8, 10);

        assertEquals(1, cleanup.get());
    }

    @Test
    public void error() {
        final AtomicInteger cleanup = new AtomicInteger();
        Flowables.generateAsync(
                new Supplier<SyncRange>() {
                    @Override
                    public SyncRange get() throws Throwable {
                        return new SyncRange(1, 12);
                    }
                },
                new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                    @Override
                    public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        Completable c = state.nextValue(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer v) throws Throwable {
                                emitter.onError(new TestException());
                            }
                        });

                        final Disposable d = c.subscribe(new Action() {
                            @Override
                            public void run() throws Throwable {
                                emitter.onComplete();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable e) throws Throwable {
                                emitter.onError(e);
                            }
                        });
                        emitter.replaceCancellable(new Cancellable() {
                            @Override
                            public void cancel() throws Throwable {
                                d.dispose();
                            }
                        });

                        return state;
                    }
                },
                new Consumer<SyncRange>() {
                    @Override
                    public void accept(SyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, cleanup.get());
    }

    @Test
    public void take() {
        final AtomicBoolean cancelled1 = new AtomicBoolean();
        final AtomicReference<FlowableAsyncEmitter<Integer>> emitterRef = new AtomicReference<>();

        final AtomicInteger cleanup = new AtomicInteger();
        Flowables.generateAsync(
                new Supplier<SyncRange>() {
                    @Override
                    public SyncRange get() throws Throwable {
                        return new SyncRange(1, 5);
                    }
                },
                new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                    @Override
                    public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        cancelled1.set(emitter.isCancelled());
                        emitterRef.set(emitter);
                        Completable c = state.nextValue(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer v) throws Throwable {
                                emitter.onNext(v);
                            }
                        });

                        final Disposable d = c.subscribe(new Action() {
                            @Override
                            public void run() throws Throwable {
                                emitter.onComplete();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable e) throws Throwable {
                                emitter.onError(e);
                            }
                        });
                        emitter.replaceCancellable(new Cancellable() {
                            @Override
                            public void cancel() throws Throwable {
                                d.dispose();
                            }
                        });

                        return state;
                    }
                },
                new Consumer<SyncRange>() {
                    @Override
                    public void accept(SyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .take(3)
        .test()
        .assertResult(1, 2, 3);

        assertEquals(1, cleanup.get());

        assertFalse(cancelled1.get());
        assertTrue(emitterRef.get().isCancelled());
    }

    @Test
    public void initialStateCrash() {
        final AtomicInteger generator = new AtomicInteger();
        final AtomicInteger cleanup = new AtomicInteger();

        Flowables.generateAsync(
                new Supplier<SyncRange>() {
                    @Override
                    public SyncRange get() throws Throwable {
                        throw new TestException();
                    }
                },
                new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                    @Override
                    public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        generator.incrementAndGet();
                        return state;
                    }
                },
                new Consumer<SyncRange>() {
                    @Override
                    public void accept(SyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, generator.get());
        assertEquals(0, cleanup.get());
    }

    @Test
    public void cancelledCancel() throws Throwable {
        FlowableGenerateAsync.AtomicCancellable.CANCELLED.cancel();
    }

    @Test
    public void cancellableCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final AtomicInteger cleanup = new AtomicInteger();

            Flowables.generateAsync(
                    new Supplier<SyncRange>() {
                        @Override
                        public SyncRange get() throws Throwable {
                            return null;
                        }
                    },
                    new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                        @Override
                        public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                                throws Throwable {
                            emitter.setCancellable(new Cancellable() {
                                @Override
                                public void cancel() throws Throwable {
                                    throw new TestException();
                                }
                            });
                            emitter.onComplete();
                            return state;
                        }
                    },
                    new Consumer<SyncRange>() {
                        @Override
                        public void accept(SyncRange state) throws Throwable {
                            cleanup.incrementAndGet();
                        }
                    }
            )
            .test()
            .assertResult();

            assertEquals(1, cleanup.get());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cleanupCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Flowables.generateAsync(
                    new Supplier<SyncRange>() {
                        @Override
                        public SyncRange get() throws Throwable {
                            return null;
                        }
                    },
                    new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                        @Override
                        public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                                throws Throwable {
                            emitter.onComplete();
                            return state;
                        }
                    },
                    new Consumer<SyncRange>() {
                        @Override
                        public void accept(SyncRange state) throws Throwable {
                            throw new TestException();
                        }
                    }
            )
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void generatorCrash() {
        final AtomicInteger cleanup = new AtomicInteger();

        Flowables.generateAsync(
                new Supplier<SyncRange>() {
                    @Override
                    public SyncRange get() throws Throwable {
                        return null;
                    }
                },
                new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                    @Override
                    public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        throw new TestException();
                    }
                },
                new Consumer<SyncRange>() {
                    @Override
                    public void accept(SyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, cleanup.get());
    }

    @Test
    public void doubleOnError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final AtomicInteger cleanup = new AtomicInteger();

            Flowables.generateAsync(
                    new Supplier<SyncRange>() {
                        @Override
                        public SyncRange get() throws Throwable {
                            return null;
                        }
                    },
                    new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                        @Override
                        public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                                throws Throwable {
                            emitter.onError(new TestException("One"));
                            emitter.onError(new TestException("Two"));
                            return state;
                        }
                    },
                    new Consumer<SyncRange>() {
                        @Override
                        public void accept(SyncRange state) throws Throwable {
                            cleanup.incrementAndGet();
                        }
                    }
            )
            .test()
            .assertFailure(TestException.class)
            .assertError(TestHelper.assertErrorMessage("One"));

            assertEquals(1, cleanup.get());

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Two");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void never() {
        final AtomicInteger cleanup = new AtomicInteger();

        Flowables.generateAsync(
                new Supplier<SyncRange>() {
                    @Override
                    public SyncRange get() throws Throwable {
                        return null;
                    }
                },
                new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                    @Override
                    public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        return state;
                    }
                },
                new Consumer<SyncRange>() {
                    @Override
                    public void accept(SyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .test()
        .cancel();

        assertEquals(1, cleanup.get());
    }

    @Test
    public void onNextAlreadyAvailable() {
        final AtomicInteger cleanup = new AtomicInteger();

        Flowables.generateAsync(
                new Supplier<SyncRange>() {
                    @Override
                    public SyncRange get() throws Throwable {
                        return null;
                    }
                },
                new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                    @Override
                    public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        emitter.onNext(1);
                        return state;
                    }
                },
                new Consumer<SyncRange>() {
                    @Override
                    public void accept(SyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .test(0)
        .assertEmpty()
        .requestMore(1)
        .assertValue(1)
        .requestMore(1)
        .assertValues(1, 1)
        .cancel();

        assertEquals(1, cleanup.get());
    }

    @Test
    public void onNextAlreadyAvailableAndComplete() {
        final AtomicInteger cleanup = new AtomicInteger();

        Flowables.generateAsync(
                new Supplier<SyncRange>() {
                    @Override
                    public SyncRange get() throws Throwable {
                        return null;
                    }
                },
                new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                    @Override
                    public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        emitter.onNext(1);
                        emitter.onComplete();
                        return state;
                    }
                },
                new Consumer<SyncRange>() {
                    @Override
                    public void accept(SyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .test(0)
        .assertEmpty()
        .requestMore(1)
        .assertResult(1);

        assertEquals(1, cleanup.get());
    }

    @Test
    public void onNothingAndComplete() {
        final AtomicInteger cleanup = new AtomicInteger();

        Flowables.generateAsync(
                new Supplier<SyncRange>() {
                    @Override
                    public SyncRange get() throws Throwable {
                        return null;
                    }
                },
                new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                    @Override
                    public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        // in practice, there could be a request being serviced while
                        // the following emitter.onNothing() is executing
                        // after which the complete tags the state with it's done indicator
                        ((FlowableGenerateAsync.GenerateAsyncSubscription<?, ?>)emitter).itemState = 2;
                        emitter.onComplete();
                        return state;
                    }
                },
                new Consumer<SyncRange>() {
                    @Override
                    public void accept(SyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .test()
        .assertResult();

        assertEquals(1, cleanup.get());
    }

    @Test
    public void empty() {
        final AtomicInteger cleanup = new AtomicInteger();

        Flowables.generateAsync(
                new Supplier<SyncRange>() {
                    @Override
                    public SyncRange get() throws Throwable {
                        return null;
                    }
                },
                new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                    @Override
                    public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        emitter.onComplete();
                        return state;
                    }
                },
                new Consumer<SyncRange>() {
                    @Override
                    public void accept(SyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .test(0)
        .assertResult();

        assertEquals(1, cleanup.get());
    }

    @Test
    public void atomicCancellableSetCancelled() {
        final AtomicInteger calls = new AtomicInteger();

        AtomicCancellable ac = new AtomicCancellable();
        ac.cancel();

        ac.setCancellable(new Cancellable() {
            @Override
            public void cancel() throws Throwable {
                calls.incrementAndGet();
            }
        });

        assertEquals(1, calls.get());
    }

    @Test
    public void setCancelRace() {
        final AtomicCancellable ac = new AtomicCancellable();
        final Cancellable c1 = new Cancellable() {
            @Override
            public void cancel() throws Throwable {
            }
        };
        final Cancellable c2 = new Cancellable() {
            @Override
            public void cancel() throws Throwable {
            }
        };

        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            ac.set(null);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10; i++) {
                        ac.setCancellable(c1);
                    }
                }
            };
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 10; i++) {
                        ac.replaceCancellable(c2);
                    }
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void backpressured() {
        final AtomicInteger cleanup = new AtomicInteger();
        TestSubscriber<Integer> ts = Flowables.generateAsync(
                new Supplier<SyncRange>() {
                    @Override
                    public SyncRange get() throws Throwable {
                        return new SyncRange(1, 1024);
                    }
                },
                new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                    @Override
                    public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        Completable c = state.nextValue(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer v) throws Throwable {
                                emitter.onNext(v);
                            }
                        });

                        final Disposable d = c.subscribe(new Action() {
                            @Override
                            public void run() throws Throwable {
                                emitter.onComplete();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable e) throws Throwable {
                                emitter.onError(e);
                            }
                        });
                        emitter.replaceCancellable(new Cancellable() {
                            @Override
                            public void cancel() throws Throwable {
                                d.dispose();
                            }
                        });

                        return state;
                    }
                },
                new Consumer<SyncRange>() {
                    @Override
                    public void accept(SyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .test(0);

        for (int i = 0; i < 1024; i++) {
            ts.assertValueCount(i)
            .assertNoErrors()
            .assertNotComplete()
            .requestMore(1)
            .assertValueCount(i + 1)
            .assertValueAt(i, i + 1)
            .assertNoErrors();
        }

        ts.assertComplete();
        assertEquals(1, cleanup.get());
    }

    @Test
    public void asyncBbackpressured() {
        final AtomicInteger cleanup = new AtomicInteger();
        TestSubscriber<Integer> ts = Flowables.generateAsync(
                new Supplier<AsyncRange>() {
                    @Override
                    public AsyncRange get() throws Throwable {
                        return new AsyncRange(1, 256);
                    }
                },
                new BiFunction<AsyncRange, FlowableAsyncEmitter<Integer>, AsyncRange>() {
                    @Override
                    public AsyncRange apply(AsyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        Completable c = state.nextValue(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer v) throws Throwable {
                                emitter.onNext(v);
                            }
                        });

                        final Disposable d = c.subscribe(new Action() {
                            @Override
                            public void run() throws Throwable {
                                emitter.onComplete();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable e) throws Throwable {
                                emitter.onError(e);
                            }
                        });
                        emitter.replaceCancellable(new Cancellable() {
                            @Override
                            public void cancel() throws Throwable {
                                d.dispose();
                            }
                        });

                        return state;
                    }
                },
                new Consumer<AsyncRange>() {
                    @Override
                    public void accept(AsyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .test(0);

        for (int i = 0; i < 256; i++) {
            ts.assertValueCount(i)
            .assertNoErrors()
            .assertNotComplete()
            .requestMore(1)
            .awaitCount(i + 1)
            .assertValueAt(i, i + 1)
            .assertNoErrors();
        }

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertComplete();
        assertEquals(1, cleanup.get());
    }

    @Test
    public void nullItem() {
        final AtomicInteger cleanup = new AtomicInteger();

        Flowables.generateAsync(
                new Supplier<SyncRange>() {
                    @Override
                    public SyncRange get() throws Throwable {
                        return null;
                    }
                },
                new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                    @Override
                    public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        emitter.onNext(null);
                        return state;
                    }
                },
                new Consumer<SyncRange>() {
                    @Override
                    public void accept(SyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .test(0)
        .assertFailure(NullPointerException.class);

        assertEquals(1, cleanup.get());
    }

    @Test
    public void nullThrowable() {
        final AtomicInteger cleanup = new AtomicInteger();

        Flowables.generateAsync(
                new Supplier<SyncRange>() {
                    @Override
                    public SyncRange get() throws Throwable {
                        return null;
                    }
                },
                new BiFunction<SyncRange, FlowableAsyncEmitter<Integer>, SyncRange>() {
                    @Override
                    public SyncRange apply(SyncRange state, final FlowableAsyncEmitter<Integer> emitter)
                            throws Throwable {
                        emitter.onError(null);
                        return state;
                    }
                },
                new Consumer<SyncRange>() {
                    @Override
                    public void accept(SyncRange state) throws Throwable {
                        cleanup.incrementAndGet();
                    }
                }
        )
        .test(0)
        .assertFailure(NullPointerException.class);

        assertEquals(1, cleanup.get());
    }
}