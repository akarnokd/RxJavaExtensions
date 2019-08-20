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

package hu.akarnokd.rxjava3.schedulers;

import static org.junit.Assert.*;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import hu.akarnokd.rxjava3.schedulers.SharedScheduler.SharedWorker.SharedAction;
import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.schedulers.*;

public class SharedSchedulerTest implements Runnable {

    volatile int calls;

    @Override
    public void run() {
        calls++;
    }

    @Test(timeout = 5000)
    public void normal() {
        SharedScheduler scheduler = new SharedScheduler(Schedulers.io());
        try {
            final Set<String> threads = new HashSet<String>();

            for (int i = 0; i < 100; i++) {
                Flowable.just(1).subscribeOn(scheduler)
                .map(new Function<Integer, Object>() {
                    @Override
                    public Object apply(Integer v) throws Exception {
                        return threads.add(Thread.currentThread().getName());
                    }
                })
                .blockingLast();
            }

            assertEquals(1, threads.size());
        } finally {
            scheduler.shutdown();
        }
    }

    @Test(timeout = 5000)
    public void delay() {
        SharedScheduler scheduler = new SharedScheduler(Schedulers.io());
        try {
            final Set<String> threads = new HashSet<String>();

            for (int i = 0; i < 100; i++) {
                Flowable.just(1).delay(1, TimeUnit.MILLISECONDS, scheduler)
                .map(new Function<Integer, Object>() {
                    @Override
                    public Object apply(Integer v) throws Exception {
                        return threads.add(Thread.currentThread().getName());
                    }
                })
                .blockingLast();
            }

            assertEquals(1, threads.size());
        } finally {
            scheduler.shutdown();
        }
    }

    long memoryUsage() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    @Test
    public void noleak() throws Exception {
        SharedScheduler scheduler = new SharedScheduler(Schedulers.io());
        try {
            Worker worker = scheduler.createWorker();

            worker.schedule(Functions.EMPTY_RUNNABLE);

            System.gc();
            Thread.sleep(500);

            long before = memoryUsage();
            System.out.printf("Start: %.1f%n", before / 1024.0 / 1024.0);

            for (int i = 0; i < 200 * 1000; i++) {
                worker.schedule(Functions.EMPTY_RUNNABLE, 1, TimeUnit.DAYS);
            }

            long middle = memoryUsage();

            System.out.printf("Middle: %.1f -> %.1f%n", before / 1024.0 / 1024.0, middle / 1024.0 / 1024.0);

            worker.dispose();

            System.gc();

            Thread.sleep(100);

            int wait = 400;

            long after = memoryUsage();

            while (wait-- > 0) {
                System.out.printf("Usage: %.1f -> %.1f -> %.1f%n", before / 1024.0 / 1024.0, middle / 1024.0 / 1024.0, after / 1024.0 / 1024.0);

                if (middle > after * 2) {
                    return;
                }

                Thread.sleep(100);

                System.gc();

                Thread.sleep(100);

                after = memoryUsage();
            }

            fail(String.format("Looks like there is a memory leak: %.1f -> %.1f -> %.1f", before / 1024.0 / 1024.0, middle / 1024.0 / 1024.0, after / 1024.0 / 1024.0));

        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    public void now() {
        TestScheduler test = new TestScheduler();

        SharedScheduler scheduler = new SharedScheduler(test);

        assertEquals(0L, scheduler.now(TimeUnit.MILLISECONDS));

        assertEquals(0L, scheduler.createWorker().now(TimeUnit.MILLISECONDS));
    }

    @Test
    public void direct() {
        TestScheduler test = new TestScheduler();

        SharedScheduler scheduler = new SharedScheduler(test);

        scheduler.scheduleDirect(this);

        test.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        assertEquals(1, calls);

        scheduler.scheduleDirect(this, 1, TimeUnit.MILLISECONDS);

        test.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        assertEquals(2, calls);

        scheduler.schedulePeriodicallyDirect(this, 1, 1, TimeUnit.MILLISECONDS);

        test.advanceTimeBy(10, TimeUnit.MILLISECONDS);

        assertEquals(12, calls);

        Worker worker = scheduler.createWorker();
        worker.dispose();

        assertSame(Disposables.disposed(), worker.schedule(this));

        assertSame(Disposables.disposed(), worker.schedule(this, 1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void taskCrash() {
        TestScheduler test = new TestScheduler();

        SharedScheduler scheduler = new SharedScheduler(test);

        Disposable d = scheduler.createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                throw new IllegalArgumentException();
            }
        });

        assertFalse(d.isDisposed());

        try {
            test.triggerActions();
        } catch (IllegalArgumentException ex) {
            // expected
        }

        assertTrue(d.isDisposed());
    }

    @Test(timeout = 5000)
    public void futureDisposeRace() throws Exception {
        SharedScheduler scheduler = new SharedScheduler(Schedulers.computation());
        try {
            Worker w = scheduler.createWorker();
            for (int i = 0; i < 1000; i++) {
               w.schedule(this);
            }

            while (calls != 1000) {
                Thread.sleep(100);
            }
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    public void disposeSetFutureRace() {
        for (int i = 0; i < 1000; i++) {
            final SharedAction sa = new SharedAction(this, new CompositeDisposable());

            final Disposable d = Disposables.empty();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    sa.setFuture(d);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    sa.dispose();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            assertTrue("Future not disposed", d.isDisposed());
        }
    }

    @Test
    public void runSetFutureRace() {
        for (int i = 0; i < 1000; i++) {
            final SharedAction sa = new SharedAction(this, new CompositeDisposable());

            final Disposable d = Disposables.empty();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    sa.setFuture(d);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    sa.run();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            assertFalse("Future disposed", d.isDisposed());
            assertEquals(i + 1, calls);
        }
    }
}
