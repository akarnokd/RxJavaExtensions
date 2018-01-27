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

package hu.akarnokd.rxjava2.debug.multihook;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

import org.junit.Test;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class OnScheduleMultiHandlerManagerTest {

    int calls;

    OnScheduleMultiHandlerManager manager = new OnScheduleMultiHandlerManager();

    @Test
    public void normal() {
        manager.enable();
        try {
            Disposable d1 = manager.register(new Function<Runnable, Runnable>() {
                @Override
                public Runnable apply(Runnable r) throws Exception {
                    calls++;
                    return r;
                }
            });
            manager.register(new Function<Runnable, Runnable>() {
                @Override
                public Runnable apply(Runnable r) throws Exception {
                    calls++;
                    return r;
                }
            });

            RxJavaPlugins.onSchedule(Functions.EMPTY_RUNNABLE);

            assertFalse(d1.isDisposed());
            d1.dispose();
            assertTrue(d1.isDisposed());
            d1.dispose();
            assertTrue(d1.isDisposed());

            assertEquals(2, calls);

            RxJavaPlugins.onSchedule(Functions.EMPTY_RUNNABLE);

            assertEquals(3, calls);
        } finally {
            manager.disable();
        }

        RxJavaPlugins.onSchedule(Functions.EMPTY_RUNNABLE);

        assertEquals(3, calls);

        manager.forEach(new Consumer<Function<Runnable, Runnable>>() {
            @Override
            public void accept(Function<Runnable, Runnable> f)
                    throws Exception {
                        calls++;
                    }
        });

        assertEquals(4, calls);

        manager.clear();

        manager.forEach(new Consumer<Function<Runnable, Runnable>>() {
            @Override
            public void accept(Function<Runnable, Runnable> f)
                    throws Exception {
                        calls++;
                    }
        });

        assertEquals(4, calls);
    }

    @Test
    public void handlerCrash() throws Exception {
        manager.register(new Function<Runnable, Runnable>() {
            @Override
            public Runnable apply(Runnable r) throws Exception {
                throw new IOException();
            }
        });
        manager.register(new Function<Runnable, Runnable>() {
            @Override
            public Runnable apply(Runnable r) throws Exception {
                calls++;
                return r;
            }
        });

        Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (e instanceof IOException) {
                    calls++;
                }
            }
        });

        manager.apply(Functions.EMPTY_RUNNABLE);

        assertEquals(2, calls);

        manager.forEach(new Consumer<Function<Runnable, Runnable>>() {
            @Override
            public void accept(Function<Runnable, Runnable> f)
                    throws Exception {
                throw new IOException();
            }
        });

        assertEquals(4, calls);
    }

    @Test
    public void append() {
        try {
            manager.append();
            manager.disable();

            manager.enable();
            manager.append();
            manager.disable();

            assertNull(RxJavaPlugins.getScheduleHandler());

            RxJavaPlugins.setScheduleHandler(new Function<Runnable, Runnable>() {
                @Override
                public Runnable apply(Runnable r) throws Exception {
                    calls++;
                    return r;
                }
            });

            manager.append();
            manager.register(new Function<Runnable, Runnable>() {
                @Override
                public Runnable apply(Runnable r) throws Exception {
                    calls++;
                    return r;
                }
            });

            RxJavaPlugins.onSchedule(Functions.EMPTY_RUNNABLE);

            assertEquals(2, calls);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void forEachConsumerRace() {
        for (int i = 0; i < 1000; i++) {
            final Disposable d = manager.register(Functions.<Runnable>identity());

            assertTrue(manager.hasHandlers());

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    d.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    manager.forEach(Functions.<Function<Runnable, Runnable>>emptyConsumer());
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            assertFalse(manager.hasHandlers());
        }
    }

    @Test
    public void forEachBiConsumerRace() {
        for (int i = 0; i < 1000; i++) {
            final Disposable d = manager.register(Functions.<Runnable>identity());

            assertTrue(manager.hasHandlers());

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    d.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    manager.forEach(0, new BiConsumer<Integer, Function<Runnable, Runnable>>() {
                        @Override
                        public void accept(Integer s, Function<Runnable, Runnable> f) throws Exception {
                            // no op
                        }
                    });
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            assertFalse(manager.hasHandlers());
        }
    }
}
