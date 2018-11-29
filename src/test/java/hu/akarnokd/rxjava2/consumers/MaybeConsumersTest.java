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

package hu.akarnokd.rxjava2.consumers;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;

import org.junit.Test;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.*;
import io.reactivex.observers.LambdaConsumerIntrospection;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.MaybeSubject;

public class MaybeConsumersTest implements Consumer<Object>, Action {

    final CompositeDisposable composite = new CompositeDisposable();

    final MaybeSubject<Integer> processor = MaybeSubject.create();

    final List<Object> events = new ArrayList<Object>();

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
        TestHelper.checkUtilityClass(MaybeConsumers.class);
    }

    @Test
    public void onSuccessNormal() {

        Disposable d = MaybeConsumers.subscribeAutoDispose(processor, composite, this);

        assertFalse(d.getClass().toString(), ((LambdaConsumerIntrospection)d).hasCustomOnError());

        assertTrue(composite.size() > 0);

        assertTrue(events.toString(), events.isEmpty());

        processor.onSuccess(1);

        assertEquals(0, composite.size());

        assertEquals(Arrays.<Object>asList(1), events);

    }

    @Test
    public void onErrorNormal() {

        MaybeConsumers.subscribeAutoDispose(processor, composite, this, this);

        assertTrue(composite.size() > 0);

        assertTrue(events.toString(), events.isEmpty());

        processor.onSuccess(1);

        assertEquals(0, composite.size());

        assertEquals(Arrays.<Object>asList(1), events);

    }

    @Test
    public void onErrorError() {

        Disposable d = MaybeConsumers.subscribeAutoDispose(processor, composite, this, this);

        assertTrue(d.getClass().toString(), ((LambdaConsumerIntrospection)d).hasCustomOnError());

        assertTrue(composite.size() > 0);

        assertTrue(events.toString(), events.isEmpty());

        processor.onError(new IOException());

        assertTrue(events.toString(), events.get(0) instanceof IOException);

        assertEquals(0, composite.size());
    }

    @Test
    public void onCompleteNormal() {

        MaybeConsumers.subscribeAutoDispose(processor, composite, this, this, this);

        assertTrue(composite.size() > 0);

        assertTrue(events.toString(), events.isEmpty());

        processor.onComplete();

        assertEquals(0, composite.size());

        assertEquals(Arrays.<Object>asList("OnComplete"), events);

    }

    @Test
    public void onCompleteError() {

        MaybeConsumers.subscribeAutoDispose(processor, composite, this, this, this);

        assertTrue(composite.size() > 0);

        assertTrue(events.toString(), events.isEmpty());

        processor.onError(new IOException());

        assertTrue(events.toString(), events.get(0) instanceof IOException);

        assertEquals(0, composite.size());
    }

    @Test
    public void onCompleteDispose() {

        Disposable d = MaybeConsumers.subscribeAutoDispose(processor, composite, this, this, this);

        assertTrue(composite.size() > 0);

        assertTrue(events.toString(), events.isEmpty());

        assertFalse(d.isDisposed());

        d.dispose();
        d.dispose();

        assertTrue(d.isDisposed());

        assertEquals(0, composite.size());

        assertFalse(processor.hasObservers());
    }

    @Test
    public void onSuccessCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            MaybeConsumers.subscribeAutoDispose(processor, composite, new Consumer<Object>() {
                @Override
                public void accept(Object t) throws Exception {
                    throw new IOException();
                }
            }, this, this);

            processor.onSuccess(1);

            assertTrue(events.toString(), events.isEmpty());

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onErrorCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            MaybeConsumers.subscribeAutoDispose(processor, composite, this, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable t) throws Exception {
                    throw new IOException(t);
                }
            }, this);

            processor.onError(new IllegalArgumentException());

            assertTrue(events.toString(), events.isEmpty());

            TestHelper.assertError(errors, 0, CompositeException.class);
            List<Throwable> inners = TestHelper.compositeList(errors.get(0));
            TestHelper.assertError(inners, 0, IllegalArgumentException.class);
            TestHelper.assertError(inners, 1, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            MaybeConsumers.subscribeAutoDispose(processor, composite, this, this, new Action() {
                @Override
                public void run() throws Exception {
                    throw new IOException();
                }
            });

            processor.onComplete();

            assertTrue(events.toString(), events.isEmpty());

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            MaybeConsumers.subscribeAutoDispose(
                    new Maybe<Integer>() {
                        @Override
                        protected void subscribeActual(
                                MaybeObserver<? super Integer> observer) {
                            observer.onSubscribe(Disposables.empty());
                            observer.onComplete();

                            observer.onSubscribe(Disposables.empty());
                            observer.onSuccess(2);
                            observer.onComplete();
                            observer.onError(new IOException());
                        }
                    }, composite, this, this, this
                );

            assertEquals(Arrays.<Object>asList("OnComplete"), events);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
