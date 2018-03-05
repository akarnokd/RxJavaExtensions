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

package hu.akarnokd.rxjava2.subjects;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import hu.akarnokd.rxjava2.test.*;
import io.reactivex.disposables.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;

public class UnicastWorkSubjectTest {

    @Test
    public void offline() {
        UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create();

        uws.onNext(1);
        uws.onNext(2);
        uws.onNext(3);
        uws.onNext(4);
        uws.onNext(5);
        uws.onComplete();

        uws.take(2).test().assertResult(1, 2);

        uws.take(2).test().assertResult(3, 4);

        uws.take(2).test().assertResult(5);

        uws.test().assertResult();
    }

    @Test
    public void online() {
        UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create();

        assertFalse(uws.hasComplete());
        assertFalse(uws.hasThrowable());
        assertNull(uws.getThrowable());

        TestObserver<Integer> ts1 = uws.test();

        assertTrue(uws.hasObservers());

        ts1.assertEmpty();

        uws.onNext(1);

        ts1.assertValuesOnly(1);

        uws.onNext(2);

        ts1.assertValuesOnly(1, 2);

        ts1.dispose();

        assertFalse(uws.hasObservers());

        uws.onNext(3);
        uws.onNext(4);

        TestObserver<Integer> ts2 = uws.test();

        assertTrue(uws.hasObservers());

        ts2.assertValuesOnly(3, 4);

        uws.onNext(5);

        ts2.assertValuesOnly(3, 4, 5);

        uws.onComplete();

        ts2.assertResult(3, 4, 5);

        assertFalse(uws.hasObservers());

        assertTrue(uws.hasComplete());
        assertFalse(uws.hasThrowable());
        assertNull(uws.getThrowable());
    }

    @Test(expected = NullPointerException.class)
    public void nullOnNext() {
        UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create();

        uws.onNext(null);
    }

    @Test(expected = NullPointerException.class)
    public void nullOnError() {
        UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create();

        uws.onError(null);
    }

    @Test
    public void disposeUpfront() {
        UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create();

        uws.onNext(1);

        uws.test(true).assertEmpty();

        assertFalse(uws.hasObservers());
    }


    @Test
    public void twoSubscribers() {
        UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create();

        TestObserver<Integer> to = uws.test();

        uws.test().assertFailure(IllegalStateException.class);

        assertTrue(uws.hasObservers());

        uws.onComplete();

        to.assertResult();
    }

    @Test
    public void errorDelayed() {
        UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create();

        uws.onNext(1);
        uws.onError(new IOException());

        uws.test().assertFailure(IOException.class, 1);

        uws.test().assertFailure(IOException.class);
    }

    @Test
    public void errorEager() {
        UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create(false);

        uws.onNext(1);
        uws.onError(new IOException());

        uws.test().assertFailure(IOException.class);

        uws.test().assertFailure(IOException.class);

        assertTrue(uws.hasThrowable());
        assertTrue(uws.getThrowable() instanceof IOException);
        assertFalse(uws.hasComplete());
        assertFalse(uws.hasObservers());
    }

    @Test
    public void dispose() {
        UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create();

        Disposable d = Disposables.empty();

        uws.onSubscribe(d);

        assertFalse(uws.isDisposed());
        assertFalse(d.isDisposed());

        uws.dispose();

        assertTrue(uws.isDisposed());
        assertTrue(d.isDisposed());
    }

    @Test
    public void onXXXAfterTermination() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create(16);
            uws.onComplete();

//            assertTrue(uws.isDisposed());

            uws.onNext(1);
            uws.onError(new IOException());
            uws.onComplete();
            uws.dispose();

//            assertTrue(uws.isDisposed());

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void emptyNoDelayError() {
        UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create(false);
        uws.onComplete();

        uws.test().assertResult();
    }

    @Test
    public void checkDispose() {
        TestHelper.checkDisposed(UnicastWorkSubject.create());
    }

    @Test
    public void onNextDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create();
            final TestObserver<Integer> to = uws.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    uws.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);

            assertFalse(uws.hasObservers());
        }
    }

    @Test
    public void onNextDisposeRace2() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create();
            final TestObserver<Integer> to = uws.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    uws.onNext(1);
                    uws.onNext(2);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);

            assertFalse(uws.hasObservers());

            uws.test().assertNotTerminated();
        }
    }

    @Test
    public void onCompleteDisposeRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create();
            final TestObserver<Integer> to = uws.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    uws.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);

            assertFalse(uws.hasObservers());
        }
    }

    @Test
    public void onErrorDisposeRace() {
        final TestException ex = new TestException();
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create();
            final TestObserver<Integer> to = uws.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    uws.onError(ex);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);

            assertFalse(uws.hasObservers());
        }
    }

    @Test
    public void onErrorDisposeRaceNoDelay() {
        final TestException ex = new TestException();
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create(false);
            final TestObserver<Integer> to = uws.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    uws.onError(ex);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);

            assertFalse(uws.hasObservers());
        }
    }

    @Test
    public void onNextSubscribeRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create();
            final TestObserver<Integer> to = new TestObserver<Integer>();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    uws.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    uws.subscribe(to);
                }
            };

            TestHelper.race(r1, r2);

            assertTrue(uws.hasObservers());

            to.assertValuesOnly(1);
        }
    }

    @Test
    public void peekRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final UnicastWorkSubject<Integer> uws = UnicastWorkSubject.create();
            final TestObserver<Integer> to = uws.test();
            uws.item = 1;

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    uws.drain();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);

            assertFalse(uws.hasObservers());

            uws.test().assertNotTerminated();
        }
    }
}
