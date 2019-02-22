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

package hu.akarnokd.rxjava2.debug.validator;

import java.io.IOException;
import java.util.List;

import org.junit.*;
import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava2.debug.*;
import hu.akarnokd.rxjava2.functions.PlainConsumer;
import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.*;
import io.reactivex.subscribers.TestSubscriber;

public class RxJavaProtocolValidatorTest implements PlainConsumer<ProtocolNonConformanceException> {

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(RxJavaProtocolValidator.class);
    }

    List<Throwable> errors;

    @Before
    public void before() {
        errors = TestHelper.trackPluginErrors();
    }

    @After
    public void after() {
        RxJavaPlugins.setErrorHandler(null);
    }

    @Override
    public void accept(ProtocolNonConformanceException t) {
        errors.add(t);
    }

    @Test
    public void completable() {
        Completable source = new Completable() {

            @Override
            protected void subscribeActual(CompletableObserver observer) {
                observer.onComplete();
                observer.onError(null);
                observer.onError(new IOException());
                observer.onSubscribe(null);
                observer.onSubscribe(Disposables.empty());
                observer.onSubscribe(Disposables.empty());
                observer.onComplete();
            }
        };

        RxJavaProtocolValidator.enable();
        Assert.assertTrue(RxJavaProtocolValidator.isEnabled());

        try {
            Completable.complete().test().assertResult();
            Completable.error(new IOException()).test().assertFailure(IOException.class);
            TestHelper.checkDisposed(RxJavaPlugins.onAssembly(CompletableSubject.create()));

            Completable c = RxJavaPlugins.onAssembly(source);

            c.test();

            Assert.assertEquals(9, errors.size());
            TestHelper.assertError(errors, 0, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 1, NullOnErrorParameterException.class);
            TestHelper.assertError(errors, 2, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 3, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 4, OnSubscribeNotCalledException.class);
            Assert.assertTrue("" + errors.get(4).getCause(), errors.get(4).getCause() instanceof IOException);
            TestHelper.assertError(errors, 5, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 6, NullOnSubscribeParameterException.class);
            TestHelper.assertError(errors, 7, MultipleOnSubscribeCallsException.class);
            TestHelper.assertError(errors, 8, MultipleTerminationsException.class);
        } finally {
            RxJavaProtocolValidator.disable();
        }
    }

    @Test
    public void maybe() {
        Maybe<Integer> source = new Maybe<Integer>() {

            @Override
            protected void subscribeActual(MaybeObserver<? super Integer> observer) {
                observer.onComplete();
                observer.onError(null);
                observer.onError(new IOException());
                observer.onSuccess(null);
                observer.onSuccess(1);
                observer.onSubscribe(null);
                observer.onSubscribe(Disposables.empty());
                observer.onSubscribe(Disposables.empty());
                observer.onComplete();
                observer.onSuccess(2);
            }
        };

        RxJavaProtocolValidator.setOnViolationHandler(this);
        Assert.assertSame(this, RxJavaProtocolValidator.getOnViolationHandler());

        SavedHooks h = RxJavaProtocolValidator.enableAndChain();
        Assert.assertTrue(RxJavaProtocolValidator.isEnabled());

        try {
            Maybe.just(1).test().assertResult(1);
            Maybe.empty().test().assertResult();
            Maybe.error(new IOException()).test().assertFailure(IOException.class);
            TestHelper.checkDisposed(RxJavaPlugins.onAssembly(MaybeSubject.create()));

            Maybe<Integer> c = RxJavaPlugins.onAssembly(source);

            c.test();

            Assert.assertEquals(15, errors.size());
            TestHelper.assertError(errors, 0, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 1, NullOnErrorParameterException.class);
            TestHelper.assertError(errors, 2, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 3, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 4, OnSubscribeNotCalledException.class);
            Assert.assertTrue("" + errors.get(4).getCause(), errors.get(4).getCause() instanceof IOException);
            TestHelper.assertError(errors, 5, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 6, NullOnSuccessParameterException.class);
            TestHelper.assertError(errors, 7, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 8, OnSuccessAfterTerminationException.class);
            TestHelper.assertError(errors, 9, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 10, OnSuccessAfterTerminationException.class);
            TestHelper.assertError(errors, 11, NullOnSubscribeParameterException.class);
            TestHelper.assertError(errors, 12, MultipleOnSubscribeCallsException.class);
            TestHelper.assertError(errors, 13, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 14, OnSuccessAfterTerminationException.class);
        } finally {
            h.restore();
            RxJavaProtocolValidator.setOnViolationHandler(null);
        }
    }

    @Test
    public void single() {
        Single<Integer> source = new Single<Integer>() {

            @Override
            protected void subscribeActual(SingleObserver<? super Integer> observer) {
                observer.onError(null);
                observer.onError(new IOException());
                observer.onSuccess(null);
                observer.onSuccess(1);
                observer.onSubscribe(null);
                observer.onSubscribe(Disposables.empty());
                observer.onSubscribe(Disposables.empty());
                observer.onSuccess(2);
            }
        };

        RxJavaProtocolValidator.setOnViolationHandler(this);
        Assert.assertSame(this, RxJavaProtocolValidator.getOnViolationHandler());

        SavedHooks h = RxJavaProtocolValidator.enableAndChain();
        Assert.assertTrue(RxJavaProtocolValidator.isEnabled());

        try {
            Single.just(1).test().assertResult(1);
            Single.error(new IOException()).test().assertFailure(IOException.class);
            TestHelper.checkDisposed(RxJavaPlugins.onAssembly(SingleSubject.create()));

            Single<Integer> c = RxJavaPlugins.onAssembly(source);

            c.test();

            Assert.assertEquals(12, errors.size());
            TestHelper.assertError(errors, 0, NullOnErrorParameterException.class);
            TestHelper.assertError(errors, 1, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 2, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 3, MultipleTerminationsException.class);
            Assert.assertTrue("" + errors.get(3).getCause(), errors.get(3).getCause() instanceof IOException);
            TestHelper.assertError(errors, 4, NullOnSuccessParameterException.class);
            TestHelper.assertError(errors, 5, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 6, OnSuccessAfterTerminationException.class);
            TestHelper.assertError(errors, 7, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 8, OnSuccessAfterTerminationException.class);
            TestHelper.assertError(errors, 9, NullOnSubscribeParameterException.class);
            TestHelper.assertError(errors, 10, MultipleOnSubscribeCallsException.class);
            TestHelper.assertError(errors, 11, OnSuccessAfterTerminationException.class);
        } finally {
            h.restore();
            RxJavaProtocolValidator.setOnViolationHandler(null);
        }
    }

    @Test
    public void observable() {
        Observable<Integer> source = new Observable<Integer>() {

            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onComplete();
                observer.onError(null);
                observer.onError(new IOException());
                observer.onNext(null);
                observer.onNext(1);
                observer.onSubscribe(null);
                observer.onSubscribe(Disposables.empty());
                observer.onSubscribe(Disposables.empty());
                observer.onComplete();
                observer.onNext(2);
            }
        };

        RxJavaProtocolValidator.setOnViolationHandler(this);
        Assert.assertSame(this, RxJavaProtocolValidator.getOnViolationHandler());

        SavedHooks h = RxJavaProtocolValidator.enableAndChain();
        Assert.assertTrue(RxJavaProtocolValidator.isEnabled());

        try {
            Observable.just(1).test().assertResult(1);
            Observable.empty().test().assertResult();
            Observable.error(new IOException()).test().assertFailure(IOException.class);
            TestHelper.checkDisposed(RxJavaPlugins.onAssembly(PublishSubject.create()));

            Observable<Integer> o = RxJavaPlugins.onAssembly(source);

            o.test();

            Assert.assertEquals(15, errors.size());
            TestHelper.assertError(errors, 0, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 1, NullOnErrorParameterException.class);
            TestHelper.assertError(errors, 2, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 3, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 4, OnSubscribeNotCalledException.class);
            Assert.assertTrue("" + errors.get(4).getCause(), errors.get(4).getCause() instanceof IOException);
            TestHelper.assertError(errors, 5, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 6, NullOnNextParameterException.class);
            TestHelper.assertError(errors, 7, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 8, OnNextAfterTerminationException.class);
            TestHelper.assertError(errors, 9, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 10, OnNextAfterTerminationException.class);
            TestHelper.assertError(errors, 11, NullOnSubscribeParameterException.class);
            TestHelper.assertError(errors, 12, MultipleOnSubscribeCallsException.class);
            TestHelper.assertError(errors, 13, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 14, OnNextAfterTerminationException.class);
        } finally {
            h.restore();
            RxJavaProtocolValidator.setOnViolationHandler(null);
        }
    }

    @Test
    public void flowable() {
        Flowable<Integer> source = new Flowable<Integer>() {

            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onComplete();
                s.onError(null);
                s.onError(new IOException());
                s.onNext(null);
                s.onNext(1);
                s.onSubscribe(null);
                s.onSubscribe(new BooleanSubscription());
                s.onSubscribe(new BooleanSubscription());
                s.onComplete();
                s.onNext(2);
            }
        };

        RxJavaProtocolValidator.setOnViolationHandler(this);
        Assert.assertSame(this, RxJavaProtocolValidator.getOnViolationHandler());

        SavedHooks h = RxJavaProtocolValidator.enableAndChain();
        Assert.assertTrue(RxJavaProtocolValidator.isEnabled());

        try {
            Flowable.just(1).test().assertResult(1);
            Flowable.empty().test().assertResult();
            Flowable.error(new IOException()).test().assertFailure(IOException.class);

            Flowable<Integer> c = RxJavaPlugins.onAssembly(source);

            c.test(0);

            Assert.assertEquals(15, errors.size());
            TestHelper.assertError(errors, 0, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 1, NullOnErrorParameterException.class);
            TestHelper.assertError(errors, 2, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 3, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 4, OnSubscribeNotCalledException.class);
            Assert.assertTrue("" + errors.get(4).getCause(), errors.get(4).getCause() instanceof IOException);
            TestHelper.assertError(errors, 5, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 6, NullOnNextParameterException.class);
            TestHelper.assertError(errors, 7, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 8, OnNextAfterTerminationException.class);
            TestHelper.assertError(errors, 9, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 10, OnNextAfterTerminationException.class);
            TestHelper.assertError(errors, 11, NullOnSubscribeParameterException.class);
            TestHelper.assertError(errors, 12, MultipleOnSubscribeCallsException.class);
            TestHelper.assertError(errors, 13, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 14, OnNextAfterTerminationException.class);
        } finally {
            h.restore();
            RxJavaProtocolValidator.setOnViolationHandler(null);
        }
    }

    @Test
    public void connectableFlowable() {
        ConnectableFlowable<Integer> source = new ConnectableFlowable<Integer>() {

            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onComplete();
                s.onError(null);
                s.onError(new IOException());
                s.onNext(null);
                s.onNext(1);
                s.onSubscribe(null);
                s.onSubscribe(new BooleanSubscription());
                s.onSubscribe(new BooleanSubscription());
                s.onComplete();
                s.onNext(2);
            }

            @Override
            public void connect(Consumer<? super Disposable> connection) {
            }
        };

        RxJavaProtocolValidator.setOnViolationHandler(this);
        Assert.assertSame(this, RxJavaProtocolValidator.getOnViolationHandler());

        SavedHooks h = RxJavaProtocolValidator.enableAndChain();
        Assert.assertTrue(RxJavaProtocolValidator.isEnabled());

        try {
            Flowable.just(1).publish().autoConnect().test().assertResult(1);
            Flowable.empty().publish().autoConnect().test().assertResult();
            Flowable.error(new IOException()).test().assertFailure(IOException.class);

            ConnectableFlowable<Integer> c = RxJavaPlugins.onAssembly(source);

            c.test(0);

            c.connect();

            Assert.assertEquals(15, errors.size());
            TestHelper.assertError(errors, 0, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 1, NullOnErrorParameterException.class);
            TestHelper.assertError(errors, 2, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 3, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 4, OnSubscribeNotCalledException.class);
            Assert.assertTrue("" + errors.get(4).getCause(), errors.get(4).getCause() instanceof IOException);
            TestHelper.assertError(errors, 5, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 6, NullOnNextParameterException.class);
            TestHelper.assertError(errors, 7, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 8, OnNextAfterTerminationException.class);
            TestHelper.assertError(errors, 9, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 10, OnNextAfterTerminationException.class);
            TestHelper.assertError(errors, 11, NullOnSubscribeParameterException.class);
            TestHelper.assertError(errors, 12, MultipleOnSubscribeCallsException.class);
            TestHelper.assertError(errors, 13, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 14, OnNextAfterTerminationException.class);
        } finally {
            h.restore();
            RxJavaProtocolValidator.setOnViolationHandler(null);
        }
    }

    @Test
    public void connectableObservable() {
        ConnectableObservable<Integer> source = new ConnectableObservable<Integer>() {

            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onComplete();
                observer.onError(null);
                observer.onError(new IOException());
                observer.onNext(null);
                observer.onNext(1);
                observer.onSubscribe(null);
                observer.onSubscribe(Disposables.empty());
                observer.onSubscribe(Disposables.empty());
                observer.onComplete();
                observer.onNext(2);
            }

            @Override
            public void connect(Consumer<? super Disposable> connection) {
            }
        };

        RxJavaProtocolValidator.setOnViolationHandler(this);
        Assert.assertSame(this, RxJavaProtocolValidator.getOnViolationHandler());

        SavedHooks h = RxJavaProtocolValidator.enableAndChain();
        Assert.assertTrue(RxJavaProtocolValidator.isEnabled());

        try {
            Observable.just(1).test().assertResult(1);
            Observable.empty().test().assertResult();
            Observable.error(new IOException()).test().assertFailure(IOException.class);
            TestHelper.checkDisposed(RxJavaPlugins.onAssembly(PublishSubject.create()));

            ConnectableObservable<Integer> co = RxJavaPlugins.onAssembly(source);

            co.test();

            co.connect();

            Assert.assertEquals(15, errors.size());
            TestHelper.assertError(errors, 0, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 1, NullOnErrorParameterException.class);
            TestHelper.assertError(errors, 2, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 3, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 4, OnSubscribeNotCalledException.class);
            Assert.assertTrue("" + errors.get(4).getCause(), errors.get(4).getCause() instanceof IOException);
            TestHelper.assertError(errors, 5, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 6, NullOnNextParameterException.class);
            TestHelper.assertError(errors, 7, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 8, OnNextAfterTerminationException.class);
            TestHelper.assertError(errors, 9, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 10, OnNextAfterTerminationException.class);
            TestHelper.assertError(errors, 11, NullOnSubscribeParameterException.class);
            TestHelper.assertError(errors, 12, MultipleOnSubscribeCallsException.class);
            TestHelper.assertError(errors, 13, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 14, OnNextAfterTerminationException.class);
        } finally {
            h.restore();
            RxJavaProtocolValidator.setOnViolationHandler(null);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void parallelFlowable() {
        ParallelFlowable<Integer> source = new ParallelFlowable<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer>[] s) {
                validate(s);
                s[0].onComplete();
                s[0].onError(null);
                s[0].onError(new IOException());
                s[0].onNext(null);
                s[0].onNext(1);
                s[0].onSubscribe(null);
                s[0].onSubscribe(new BooleanSubscription());
                s[0].onSubscribe(new BooleanSubscription());
                s[0].onComplete();
                s[0].onNext(2);
            }

            @Override
            public int parallelism() {
                return 1;
            }
        };

        RxJavaProtocolValidator.setOnViolationHandler(this);
        Assert.assertSame(this, RxJavaProtocolValidator.getOnViolationHandler());

        SavedHooks h = RxJavaProtocolValidator.enableAndChain();
        Assert.assertTrue(RxJavaProtocolValidator.isEnabled());

        try {
            Flowable.just(1).publish().autoConnect().test().assertResult(1);
            Flowable.empty().publish().autoConnect().test().assertResult();
            Flowable.error(new IOException()).test().assertFailure(IOException.class);

            ParallelFlowable<Integer> c = RxJavaPlugins.onAssembly(source);

            c.subscribe(new Subscriber[] { new TestSubscriber<Integer>(0) });

            Assert.assertEquals(15, errors.size());
            TestHelper.assertError(errors, 0, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 1, NullOnErrorParameterException.class);
            TestHelper.assertError(errors, 2, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 3, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 4, OnSubscribeNotCalledException.class);
            Assert.assertTrue("" + errors.get(4).getCause(), errors.get(4).getCause() instanceof IOException);
            TestHelper.assertError(errors, 5, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 6, NullOnNextParameterException.class);
            TestHelper.assertError(errors, 7, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 8, OnNextAfterTerminationException.class);
            TestHelper.assertError(errors, 9, OnSubscribeNotCalledException.class);
            TestHelper.assertError(errors, 10, OnNextAfterTerminationException.class);
            TestHelper.assertError(errors, 11, NullOnSubscribeParameterException.class);
            TestHelper.assertError(errors, 12, MultipleOnSubscribeCallsException.class);
            TestHelper.assertError(errors, 13, MultipleTerminationsException.class);
            TestHelper.assertError(errors, 14, OnNextAfterTerminationException.class);
        } finally {
            h.restore();
            RxJavaProtocolValidator.setOnViolationHandler(null);
        }
    }

    @Test
    public void withAssemblyTracking() {
        RxJavaAssemblyTracking.enable();
        try {
            Object o1 = RxJavaPlugins.getOnCompletableAssembly();
            Object o2 = RxJavaPlugins.getOnSingleAssembly();
            Object o3 = RxJavaPlugins.getOnMaybeAssembly();
            Object o4 = RxJavaPlugins.getOnObservableAssembly();
            Object o5 = RxJavaPlugins.getOnFlowableAssembly();
            Object o6 = RxJavaPlugins.getOnConnectableFlowableAssembly();
            Object o7 = RxJavaPlugins.getOnConnectableObservableAssembly();
            Object o8 = RxJavaPlugins.getOnParallelAssembly();

            SavedHooks h = RxJavaProtocolValidator.enableAndChain();

            h.restore();

            Assert.assertSame(o1, RxJavaPlugins.getOnCompletableAssembly());
            Assert.assertSame(o2, RxJavaPlugins.getOnSingleAssembly());
            Assert.assertSame(o3, RxJavaPlugins.getOnMaybeAssembly());
            Assert.assertSame(o4, RxJavaPlugins.getOnObservableAssembly());
            Assert.assertSame(o5, RxJavaPlugins.getOnFlowableAssembly());
            Assert.assertSame(o6, RxJavaPlugins.getOnConnectableFlowableAssembly());
            Assert.assertSame(o7, RxJavaPlugins.getOnConnectableObservableAssembly());
            Assert.assertSame(o8, RxJavaPlugins.getOnParallelAssembly());
        } finally {
            RxJavaAssemblyTracking.disable();
        }
    }

    @Test
    public void withAssemblyTrackingOverride() {
        RxJavaAssemblyTracking.enable();
        try {
            RxJavaProtocolValidator.enable();

            RxJavaProtocolValidator.disable();

            Assert.assertNull(RxJavaPlugins.getOnCompletableAssembly());
            Assert.assertNull(RxJavaPlugins.getOnSingleAssembly());
            Assert.assertNull(RxJavaPlugins.getOnMaybeAssembly());
            Assert.assertNull(RxJavaPlugins.getOnObservableAssembly());
            Assert.assertNull(RxJavaPlugins.getOnFlowableAssembly());
            Assert.assertNull(RxJavaPlugins.getOnConnectableFlowableAssembly());
            Assert.assertNull(RxJavaPlugins.getOnConnectableObservableAssembly());
            Assert.assertNull(RxJavaPlugins.getOnParallelAssembly());
        } finally {
            RxJavaAssemblyTracking.disable();
        }
    }

    @Test
    public void protocolNonConformanceException() {
        Assert.assertNotNull(new ProtocolNonConformanceException() {
            private static final long serialVersionUID = -1400755866355428747L;
        });

        Assert.assertNotNull(new ProtocolNonConformanceException("Message") {
            private static final long serialVersionUID = -1400755866355428747L;
        });

        Assert.assertNotNull(new ProtocolNonConformanceException(new IOException()) {
            private static final long serialVersionUID = -1400755866355428747L;
        });

        Assert.assertNotNull(new ProtocolNonConformanceException("Message", new IOException()) {
            private static final long serialVersionUID = -1400755866355428747L;
        });
    }
}
