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

package hu.akarnokd.rxjava4.consumers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Flow.Subscriber;

import org.junit.jupiter.api.Test;

import hu.akarnokd.rxjava4.internal.BooleanSubscription;
import hu.akarnokd.rxjava4.test.TestHelper;
import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.observers.LambdaConsumerIntrospection;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.processors.PublishProcessor;

public class FlowableConsumersTest implements Consumer<Object>, Action {

    final CompositeDisposable composite = new CompositeDisposable();

    final PublishProcessor<Integer> processor = PublishProcessor.create();

    final List<Object> events = new ArrayList<>();

    @Override
    public void run() throws Exception {
        events.add("OnComplete");
    }

    @Override
    public void accept(Object t) throws Exception {
        events.add(t);
    }

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(FlowableConsumers.class);
    }

    @Test
    public void onNextNormal() {

        Disposable d = FlowableConsumers.subscribeAutoDispose(processor, composite, this);

        assertFalse(((LambdaConsumerIntrospection)d).hasCustomOnError(), d.getClass().toString());

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        processor.onNext(1);

        assertTrue(composite.size() > 0);

        assertEquals(Arrays.<Object>asList(1), events);

        processor.onComplete();

        assertEquals(Arrays.<Object>asList(1), events);

        assertEquals(0, composite.size());
    }

    @Test
    public void onErrorNormal() {

        FlowableConsumers.subscribeAutoDispose(processor, composite, this, this);

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        processor.onNext(1);

        assertTrue(composite.size() > 0);

        assertEquals(Arrays.<Object>asList(1), events);

        processor.onComplete();

        assertEquals(Arrays.<Object>asList(1), events);

        assertEquals(0, composite.size());
    }

    @Test
    public void onErrorError() {

        Disposable d = FlowableConsumers.subscribeAutoDispose(processor, composite, this, this);

        assertTrue(((LambdaConsumerIntrospection)d).hasCustomOnError(), d.getClass().toString());

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        processor.onNext(1);

        assertTrue(composite.size() > 0);

        assertEquals(Arrays.<Object>asList(1), events);

        processor.onError(new IOException());

        assertEquals(1, events.get(0), events.toString());
        assertTrue(events.get(1) instanceof IOException, events.toString());

        assertEquals(0, composite.size());
    }

    @Test
    public void onCompleteNormal() {

        FlowableConsumers.subscribeAutoDispose(processor, composite, this, this, this);

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        processor.onNext(1);

        assertTrue(composite.size() > 0);

        assertEquals(Arrays.<Object>asList(1), events);

        processor.onComplete();

        assertEquals(Arrays.<Object>asList(1, "OnComplete"), events);

        assertEquals(0, composite.size());
    }

    @Test
    public void onCompleteError() {

        FlowableConsumers.subscribeAutoDispose(processor, composite, this, this, this);

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        processor.onNext(1);

        assertTrue(composite.size() > 0);

        assertEquals(Arrays.<Object>asList(1), events);

        processor.onError(new IOException());

        assertEquals(1, events.get(0), events.toString());
        assertTrue(events.get(1) instanceof IOException, events.toString());

        assertEquals(0, composite.size());
    }

    @Test
    public void onCompleteDispose() {

        Disposable d = FlowableConsumers.subscribeAutoDispose(processor, composite, this, this, this);

        assertTrue(composite.size() > 0);

        assertTrue(events.isEmpty(), events.toString());

        assertFalse(d.isDisposed());

        d.dispose();
        d.dispose();

        assertTrue(d.isDisposed());

        assertEquals(0, composite.size());

        assertFalse(processor.hasSubscribers());
    }

    @Test
    public void onNextCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            FlowableConsumers.subscribeAutoDispose(processor, composite, new Consumer<Object>() {
                @Override
                public void accept(Object t) throws Exception {
                    throw new IOException();
                }
            }, this, this);

            processor.onNext(1);

            assertTrue(errors.isEmpty(), errors.toString());

            assertTrue(events.get(0) instanceof IOException, events.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onNextCrashOnError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            FlowableConsumers.subscribeAutoDispose(processor, composite, this, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable t) throws Exception {
                    throw new IOException(t);
                }
            }, this);

            processor.onError(new IllegalArgumentException());

            assertTrue(events.isEmpty(), events.toString());

            TestHelper.assertError(errors, 0, CompositeException.class);
            List<Throwable> inners = TestHelper.compositeList(errors.get(0));
            TestHelper.assertError(inners, 0, IllegalArgumentException.class);
            TestHelper.assertError(inners, 1, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onNextCrashNoError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            FlowableConsumers.subscribeAutoDispose(processor, composite, new Consumer<Object>() {
                @Override
                public void accept(Object t) throws Exception {
                    throw new IOException();
                }
            });

            processor.onNext(1);

            assertTrue(events.isEmpty(), events.toString());

            TestHelper.assertError(errors, 0, OnErrorNotImplementedException.class);
            assertTrue(errors.get(0).getCause() instanceof IOException);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            FlowableConsumers.subscribeAutoDispose(processor, composite, this, this, new Action() {
                @Override
                public void run() throws Exception {
                    throw new IOException();
                }
            });

            processor.onNext(1);
            processor.onComplete();

            assertEquals(Arrays.asList(1), events);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            FlowableConsumers.subscribeAutoDispose(
                    new Flowable<Integer>() {
                        @Override
                        protected void subscribeActual(
                                Subscriber<? super Integer> s) {
                            s.onSubscribe(new BooleanSubscription());
                            s.onNext(1);
                            s.onComplete();

                            s.onSubscribe(new BooleanSubscription());
                            s.onNext(2);
                            s.onComplete();
                            s.onError(new IOException());
                        }
                    }, composite, this, this, this
                );

            assertEquals(Arrays.<Object>asList(1, "OnComplete"), events);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
