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

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import hu.akarnokd.rxjava3.schedulers.ParallelScheduler.TrackingParallelWorker.TrackedAction;
import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.schedulers.RxThreadFactory;
import io.reactivex.schedulers.Schedulers;

public class ParallelSchedulerTest implements Runnable {

    final AtomicInteger calls = new AtomicInteger();

    @Override
    public void run() {
        calls.getAndIncrement();
    }

    @Test
    public void normalNonTracking() {
        Scheduler s = new ParallelScheduler(2, false);

        for (int i = 0; i < 100; i++) {
            Flowable.range(1, 10).hide()
            .observeOn(s, false, 4)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
    }

    @Test
    public void delayedNonTracking() {
        Scheduler s = new ParallelScheduler(2, false);

        try {
            for (int i = 0; i < 100; i++) {
                Flowable.range(1, 10).hide()
                .delay(50, TimeUnit.MILLISECONDS, s)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            }
        } finally {
            s.shutdown();
        }
    }

    @Test
    public void normalTracking() {
        Scheduler s = new ParallelScheduler(2, true);

        try {
            for (int i = 0; i < 100; i++) {
                Flowable.range(1, 10).hide()
                .observeOn(s, false, 4)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            }
        } finally {
            s.shutdown();
        }
    }

    @Test
    public void delayedTracking() {
        Scheduler s = new ParallelScheduler(2, true);

        try {
            for (int i = 0; i < 100; i++) {
                Flowable.range(1, 10).hide()
                .delay(50, TimeUnit.MILLISECONDS, s)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            }
        } finally {
            s.shutdown();
        }
    }

    @Test
    public void shutdownNonTracking() throws Exception {
        Scheduler s = new ParallelScheduler(2, false);

        shutdown(s);
    }

    @Test
    public void shutdownTracking() throws Exception {
        Scheduler s = new ParallelScheduler(2, true);

        shutdown(s);
    }

    protected void shutdown(Scheduler s) throws InterruptedException {
        try {
            Worker w = s.createWorker();

            w.dispose();

            assertSame(Disposables.disposed(), w.schedule(this));

            assertSame(Disposables.disposed(), w.schedule(this, 100, TimeUnit.MILLISECONDS));

            assertSame(Disposables.disposed(), w.schedulePeriodically(this, 100, 100, TimeUnit.MILLISECONDS));

            s.shutdown();

            assertSame(Disposables.disposed(), s.scheduleDirect(this));

            assertSame(Disposables.disposed(), s.scheduleDirect(this, 100, TimeUnit.MILLISECONDS));

            assertSame(Disposables.disposed(), s.schedulePeriodicallyDirect(this, 100, 100, TimeUnit.MILLISECONDS));

            w = s.createWorker();

            assertSame(Disposables.disposed(), w.schedule(this));

            assertSame(Disposables.disposed(), w.schedule(this, 100, TimeUnit.MILLISECONDS));

            assertSame(Disposables.disposed(), w.schedulePeriodically(this, 100, 100, TimeUnit.MILLISECONDS));

            assertEquals(0, calls.get());

            s.start();

            s.scheduleDirect(this);

            s.scheduleDirect(this, 100, TimeUnit.MILLISECONDS);

            s.schedulePeriodicallyDirect(this, 100, 100, TimeUnit.MILLISECONDS);

            w = s.createWorker();

            w.schedule(this);

            w.schedule(this, 100, TimeUnit.MILLISECONDS);

            w.schedulePeriodically(this, 100, 100, TimeUnit.MILLISECONDS);

            Thread.sleep(1000);

            int c = calls.get();
            assertTrue("" + c, c > 6);
        } finally {
            s.shutdown();
        }
    }

    @Test(timeout = 5000)
    public void taskThrowsNonTracking() throws Exception {
        Scheduler s = new ParallelScheduler(2, false);
        taskThrows(s);
    }

    @Test(timeout = 5000)
    public void taskThrowsTracking() throws Exception {
        Scheduler s = new ParallelScheduler(2, true);
        taskThrows(s);
    }

    protected void taskThrows(Scheduler s) throws InterruptedException {
        try {
            List<Throwable> errors = TestHelper.trackPluginErrors();

            Worker w = s.createWorker();

            w.schedule(new Runnable() {
                @Override
                public void run() {
                    calls.getAndIncrement();
                    throw new IllegalStateException();
                }
            });

            while (errors.isEmpty()) {
                Thread.sleep(20);
            }

            TestHelper.assertError(errors, 0, IllegalStateException.class);
        } finally {
            s.shutdown();
        }
    }

    @Test
    public void cancelledTaskNotTracking() throws Exception {
        Scheduler s = new ParallelScheduler(2, false);
        cancelledTask(s);
    }

    @Test
    public void cancelledTaskTracking() throws Exception {
        Scheduler s = new ParallelScheduler(2, true);
        cancelledTask(s);
    }

    void cancelledTask(Scheduler s) throws InterruptedException {
        try {
            Worker w = s.createWorker();

            try {
                assertFalse(w.isDisposed());

                Disposable d = w.schedule(this, 200, TimeUnit.MILLISECONDS);

                assertFalse(d.isDisposed());

                d.dispose();

                assertTrue(d.isDisposed());

                Thread.sleep(300);

                assertEquals(0, calls.get());
                w.dispose();

                assertTrue(w.isDisposed());
            } finally {
                w.dispose();
            }
        } finally {
            s.shutdown();
        }
    }

    @Test
    public void constructors() {
        startStop(new ParallelScheduler());
        startStop(new ParallelScheduler(1));
        startStop(new ParallelScheduler(1, new RxThreadFactory("Test")));
        startStop(new ParallelScheduler("Test"));
    }

    private void startStop(Scheduler s) {
        s.start();
        s.shutdown();
        s.shutdown();
    }

    @Test
    public void shutdownBackingTracking() {
        ParallelScheduler s = new ParallelScheduler(2, true);

        shutdownBacking(s);
    }

    @Test
    public void shutdownBackingNonTracking() {
        ParallelScheduler s = new ParallelScheduler(2, false);

        shutdownBacking(s);
    }

    private void shutdownBacking(ParallelScheduler s) {
        for (ScheduledExecutorService exec : s.pool.get()) {
            exec.shutdown();
        }

        assertSame(Disposables.disposed(), s.scheduleDirect(this));

        assertSame(Disposables.disposed(), s.scheduleDirect(this, 100, TimeUnit.MILLISECONDS));

        assertSame(Disposables.disposed(), s.schedulePeriodicallyDirect(this, 100, 100, TimeUnit.MILLISECONDS));

        Worker w = s.createWorker();

        assertSame(Disposables.disposed(), w.schedule(this));

        assertSame(Disposables.disposed(), w.schedule(this, 100, TimeUnit.MILLISECONDS));

        assertSame(Disposables.disposed(), w.schedulePeriodically(this, 100, 100, TimeUnit.MILLISECONDS));

        assertEquals(0, calls.get());
    }

    @Test
    public void startRace() {
        for (int i = 0; i < 1000; i++) {
            final Scheduler s = new ParallelScheduler(2);
            s.shutdown();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    s.start();
                }
            };

            TestHelper.race(r, r, Schedulers.single());
        }
    }

    @Test
    public void setFutureRace() {
        final Scheduler s = new ParallelScheduler(2, true);
        try {
            for (int i = 0; i < 1000; i++) {
                final Worker w = s.createWorker();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        w.schedule(ParallelSchedulerTest.this);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        w.dispose();
                    }
                };
                TestHelper.race(r1, r2, Schedulers.single());
            }
        } finally {
            s.shutdown();
        }
    }

    @Test
    public void setFutureRace2() {
        final Scheduler s = new ParallelScheduler(2, true);
        try {
            for (int i = 0; i < 1000; i++) {
                final CompositeDisposable cd = new CompositeDisposable();
                final TrackedAction tt = new TrackedAction(this, cd);
                final FutureTask<Object> ft = new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null);

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        tt.setFuture(ft);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        tt.future.set(TrackedAction.FINISHED);
                    }
                };
                TestHelper.race(r1, r2, Schedulers.single());
            }
        } finally {
            s.shutdown();
        }
    }

    @Test
    public void setFutureRace3() {
        final Scheduler s = new ParallelScheduler(2, true);
        try {
            for (int i = 0; i < 1000; i++) {
                final CompositeDisposable cd = new CompositeDisposable();
                final TrackedAction tt = new TrackedAction(this, cd);
                final FutureTask<Object> ft = new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null);

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        tt.setFuture(ft);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        tt.future.set(TrackedAction.DISPOSED);
                    }
                };
                TestHelper.race(r1, r2, Schedulers.single());
            }
        } finally {
            s.shutdown();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalParallelism() {
        new ParallelScheduler(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalPriority() {
        new ParallelScheduler(2, true, -1);
    }
}
