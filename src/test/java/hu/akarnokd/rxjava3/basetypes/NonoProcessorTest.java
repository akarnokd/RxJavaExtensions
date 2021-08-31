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

package hu.akarnokd.rxjava3.basetypes;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.reactivestreams.*;

import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class NonoProcessorTest {

    @Test
    public void once() {
        NonoProcessor ms = NonoProcessor.create();

        TestSubscriber<Void> ts = ms.test();

        ms.onComplete();

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ms.onError(new IOException());

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
        ms.onComplete();

        ts.assertResult();
    }

    @Test
    public void error() {
        NonoProcessor ms = NonoProcessor.create();

        assertFalse(ms.hasComplete());
        assertFalse(ms.hasThrowable());
        assertNull(ms.getThrowable());
        assertFalse(ms.hasSubscribers());
        assertEquals(0, ms.subscriberCount());

        TestSubscriber<Void> ts = ms.test();

        ts.assertEmpty();

        assertTrue(ms.hasSubscribers());
        assertEquals(1, ms.subscriberCount());

        ms.onError(new IOException());

        assertFalse(ms.hasComplete());
        assertTrue(ms.hasThrowable());
        assertTrue(ms.getThrowable().toString(), ms.getThrowable() instanceof IOException);
        assertFalse(ms.hasSubscribers());
        assertEquals(0, ms.subscriberCount());

        ts.assertFailure(IOException.class);

        ms.test().assertFailure(IOException.class);

        assertFalse(ms.hasComplete());
        assertTrue(ms.hasThrowable());
        assertTrue(ms.getThrowable().toString(), ms.getThrowable() instanceof IOException);
        assertFalse(ms.hasSubscribers());
        assertEquals(0, ms.subscriberCount());
    }

    @Test
    public void complete() {
        NonoProcessor ms = NonoProcessor.create();

        assertFalse(ms.hasComplete());
        assertFalse(ms.hasThrowable());
        assertNull(ms.getThrowable());
        assertFalse(ms.hasSubscribers());
        assertEquals(0, ms.subscriberCount());

        TestSubscriber<Void> ts = ms.test();

        ts.assertEmpty();

        assertTrue(ms.hasSubscribers());
        assertEquals(1, ms.subscriberCount());

        ms.onComplete();

        assertTrue(ms.hasComplete());
        assertFalse(ms.hasThrowable());
        assertNull(ms.getThrowable());
        assertFalse(ms.hasSubscribers());
        assertEquals(0, ms.subscriberCount());

        ts.assertResult();

        ms.test().assertResult();

        assertTrue(ms.hasComplete());
        assertFalse(ms.hasThrowable());
        assertNull(ms.getThrowable());
        assertFalse(ms.hasSubscribers());
        assertEquals(0, ms.subscriberCount());
    }

    @Test
    public void nullThrowable() {
        NonoProcessor ms = NonoProcessor.create();

        TestSubscriber<Void> ts = ms.test();

        ms.onError(null);

        ts.assertFailure(NullPointerException.class);
    }

    @Test
    public void cancelOnArrival() {
        NonoProcessor.create()
        .test(true)
        .assertEmpty();
    }

    @Test
    public void cancelOnArrival2() {
        NonoProcessor ms = NonoProcessor.create();

        ms.test();

        ms
        .test(true)
        .assertEmpty();
    }

    @Test
    public void disposeTwice() {
        NonoProcessor.create()
        .subscribe(new Subscriber<Void>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.cancel();
                s.cancel();
            }

            @Override
            public void onNext(Void t) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    @Test
    public void onSubscribeDispose() {
        NonoProcessor ms = NonoProcessor.create();

        BooleanSubscription bs = new BooleanSubscription();

        ms.onSubscribe(bs);

        assertFalse(bs.isCancelled());

        ms.onComplete();

        bs = new BooleanSubscription();

        ms.onSubscribe(bs);

        assertTrue(bs.isCancelled());
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < 500; i++) {
            final NonoProcessor ms = NonoProcessor.create();

            final TestSubscriber<Void> ts = ms.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ms.test();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };
            TestHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test(expected = NullPointerException.class)
    public void onNextNpe() {
        NonoProcessor.create().onNext(null);
    }

    @Test
    public void crossCancelComplete() {
        final TestSubscriber<Void> ts1 = new TestSubscriber<>();

        TestSubscriber<Void> ts2 = new TestSubscriber<Void>() {
            @Override
            public void onComplete() {
                super.onComplete();
                ts1.cancel();
            }
        };

        NonoProcessor np = NonoProcessor.create();
        np.subscribe(ts2);
        np.subscribe(ts1);

        np.onComplete();

        ts1.assertEmpty();
        ts2.assertResult();
    }

    @Test
    public void crossCancelError() {
        final TestSubscriber<Void> ts1 = new TestSubscriber<>();

        TestSubscriber<Void> ts2 = new TestSubscriber<Void>() {
            @Override
            public void onError(Throwable t) {
                super.onError(t);
                ts1.cancel();
            }
        };

        NonoProcessor np = NonoProcessor.create();
        np.subscribe(ts2);
        np.subscribe(ts1);

        np.onError(new IOException());

        ts1.assertEmpty();
        ts2.assertFailure(IOException.class);
    }
}
