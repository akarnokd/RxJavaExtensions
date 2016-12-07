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

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.*;

import org.junit.*;

import io.reactivex.Flowable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

public class NonoTest implements Action {

    volatile int count;

    @Override
    public void run() throws Exception {
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
        Nono.error(new Callable<Throwable>() {
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
        Nono.error(new Callable<Throwable>() {
            @Override
            public Throwable call() throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void defer() {
        final int[] counter = { 0 };

        Nono np = Nono.defer(new Callable<Nono>() {
            @Override
            public Nono call() throws Exception {
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
        Nono np = Nono.defer(new Callable<Nono>() {
            @Override
            public Nono call() throws Exception {
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

        FutureTask<Void> ft = new FutureTask<Void>(new Callable<Void>() {
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

        FutureTask<Void> ft = new FutureTask<Void>(new Callable<Void>() {
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

        FutureTask<Void> ft = new FutureTask<Void>(new Callable<Void>() {
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

        FutureTask<Void> ft = new FutureTask<Void>(new Callable<Void>() {
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

        FutureTask<Void> ft = new FutureTask<Void>(new Callable<Void>() {
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
}
