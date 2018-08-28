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

package hu.akarnokd.rxjava2.processors;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.reactivestreams.*;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.processors.*;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableProcessorsTest {

    static final class SimpleProcessor implements Processor<Integer, Integer>, Subscription {
        Subscriber<? super Integer> subscriber;

        Subscription upstream;

        @Override
        public void onSubscribe(Subscription s) {
            this.upstream = s;
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(Integer t) {
            Subscriber<? super Integer> s = subscriber;
            if (s != null) {
                s.onNext(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            Subscriber<? super Integer> s = subscriber;
            if (s != null) {
                s.onError(t);
                subscriber = null;
            }
        }

        @Override
        public void onComplete() {
            Subscriber<? super Integer> s = subscriber;
            if (s != null) {
                s.onComplete();
                subscriber = null;
            }
        }

        @Override
        public void subscribe(Subscriber<? super Integer> s) {
            subscriber = s;
            s.onSubscribe(this);
        }

        @Override
        public void cancel() {
            subscriber = null;
        }

        @Override
        public void request(long n) {
            // ignored
        }
    }

    SimpleProcessor sp = new SimpleProcessor();

    @Test
    public void normal() {
        FlowableProcessor<Integer> fp = FlowableProcessors.wrap(sp);

        fp.onSubscribe(new BooleanSubscription());

        assertFalse(fp.hasComplete());
        assertFalse(fp.hasThrowable());
        assertNull(fp.getThrowable());

        TestSubscriber<Integer> ts = fp.test();

        assertFalse(fp.hasComplete());
        assertFalse(fp.hasThrowable());
        assertNull(fp.getThrowable());

        fp.onNext(1);
        fp.onNext(2);

        ts.assertValues(1, 2);

        assertFalse(fp.hasComplete());
        assertFalse(fp.hasThrowable());
        assertNull(fp.getThrowable());

        fp.onComplete();

        assertTrue(fp.hasComplete());
        assertFalse(fp.hasThrowable());
        assertNull(fp.getThrowable());

        ts.assertResult(1, 2);
    }

    @Test
    public void error() {
        FlowableProcessor<Integer> fp = FlowableProcessors.wrap(sp);

        assertFalse(fp.hasComplete());
        assertFalse(fp.hasThrowable());
        assertNull(fp.getThrowable());

        TestSubscriber<Integer> ts = fp.test();

        assertFalse(fp.hasComplete());
        assertFalse(fp.hasThrowable());
        assertNull(fp.getThrowable());

        fp.onNext(1);
        fp.onNext(2);

        ts.assertValues(1, 2);

        assertFalse(fp.hasComplete());
        assertFalse(fp.hasThrowable());
        assertNull(fp.getThrowable());

        fp.onError(new IOException());

        assertFalse(fp.hasComplete());
        assertTrue(fp.hasThrowable());
        assertNotNull(fp.getThrowable());

        ts.assertFailure(IOException.class, 1, 2);
    }

    @Test
    public void take() {
        FlowableProcessor<Integer> fp = FlowableProcessors.wrap(sp);

        TestSubscriber<Integer> ts = fp.take(1).test();

        fp.onNext(1);
        fp.onNext(2);
        fp.onNext(3);
        fp.onComplete();

        ts.assertResult(1);
    }

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(FlowableProcessors.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void hasSubscribers() {
        FlowableProcessors.wrap(sp).hasSubscribers();
    }

    @Test
    public void alreadyFlowableProcessor() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        assertSame(pp, FlowableProcessors.wrap(pp));
    }
}
