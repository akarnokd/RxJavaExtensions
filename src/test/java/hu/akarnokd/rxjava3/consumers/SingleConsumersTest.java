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

package hu.akarnokd.rxjava3.consumers;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;

import org.junit.Test;

import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.CompositeException;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.observers.LambdaConsumerIntrospection;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.SingleSubject;

public class SingleConsumersTest implements Consumer<Object> {

    final CompositeDisposable composite = new CompositeDisposable();

    final SingleSubject<Integer> processor = SingleSubject.create();

    final List<Object> events = new ArrayList<>();

    @Override
    public void accept(Object t) throws Exception {
        events.add(t);
    }

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(SingleConsumers.class);
    }

    @Test
    public void onSuccessNormal() {

        Disposable d = SingleConsumers.subscribeAutoDispose(processor, composite, this);

        assertFalse(d.getClass().toString(), ((LambdaConsumerIntrospection)d).hasCustomOnError());

        assertTrue(composite.size() > 0);

        assertTrue(events.toString(), events.isEmpty());

        processor.onSuccess(1);

        assertEquals(0, composite.size());

        assertEquals(Arrays.<Object>asList(1), events);

    }

    @Test
    public void onErrorNormal() {

        SingleConsumers.subscribeAutoDispose(processor, composite, this, this);

        assertTrue(composite.size() > 0);

        assertTrue(events.toString(), events.isEmpty());

        processor.onSuccess(1);

        assertEquals(0, composite.size());

        assertEquals(Arrays.<Object>asList(1), events);

    }

    @Test
    public void onErrorError() {

        Disposable d = SingleConsumers.subscribeAutoDispose(processor, composite, this, this);

        assertTrue(d.getClass().toString(), ((LambdaConsumerIntrospection)d).hasCustomOnError());

        assertTrue(composite.size() > 0);

        assertTrue(events.toString(), events.isEmpty());

        processor.onError(new IOException());

        assertTrue(events.toString(), events.get(0) instanceof IOException);

        assertEquals(0, composite.size());
    }

    @Test
    public void onSuccessCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            SingleConsumers.subscribeAutoDispose(processor, composite, new Consumer<Object>() {
                @Override
                public void accept(Object t) throws Exception {
                    throw new IOException();
                }
            }, this);

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
            SingleConsumers.subscribeAutoDispose(processor, composite, this, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable t) throws Exception {
                    throw new IOException(t);
                }
            });

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
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            SingleConsumers.subscribeAutoDispose(
                    new Single<Integer>() {
                        @Override
                        protected void subscribeActual(
                                SingleObserver<? super Integer> observer) {
                            observer.onSubscribe(Disposable.empty());
                            observer.onSuccess(1);

                            observer.onSubscribe(Disposable.empty());
                            observer.onSuccess(2);
                            observer.onError(new IOException());
                        }
                    }, composite, this, this
                );

            assertEquals(Arrays.<Object>asList(1), events);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
