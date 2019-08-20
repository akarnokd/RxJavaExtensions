/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package hu.akarnokd.rxjava3.subjects;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.*;

public class RefCountSubjectTest {

    @Test
    public void normal() {
        Subject<Integer> rcp = Subjects.refCount(PublishSubject.<Integer>create());

        PublishSubject<Integer> source = PublishSubject.create();

        assertFalse(source.hasObservers());

        source.subscribe(rcp);

        assertFalse(((Disposable)rcp).isDisposed());

        assertTrue(source.hasObservers());

        TestObserver<Integer> to = rcp.test();

        source.onNext(1);

        to.assertValue(1);

        source.onNext(2);

        to.assertValues(1, 2);

        to.dispose();

        assertFalse(source.hasObservers());

        assertTrue(((Disposable)rcp).isDisposed());
    }

    @Test
    public void complete() {
        Subject<Integer> rcp = Subjects.refCount(PublishSubject.<Integer>create());

        PublishSubject<Integer> source = PublishSubject.create();

        assertFalse(source.hasObservers());

        source.subscribe(rcp);

        assertTrue(source.hasObservers());

        TestObserver<Integer> to = rcp.test();

        assertFalse(rcp.hasComplete());
        assertFalse(rcp.hasThrowable());
        assertNull(rcp.getThrowable());
        assertTrue(rcp.hasObservers());

        source.onNext(1);

        to.assertValue(1);

        source.onNext(2);

        to.assertValues(1, 2);

        source.onComplete();

        assertFalse(source.hasObservers());

        to.assertResult(1, 2);

        rcp.test().assertResult();

        assertTrue(rcp.hasComplete());
        assertFalse(rcp.hasThrowable());
        assertNull(rcp.getThrowable());
        assertFalse(rcp.hasObservers());
    }

    @Test
    public void error() {
        Subject<Integer> rcp = Subjects.refCount(PublishSubject.<Integer>create());

        PublishSubject<Integer> source = PublishSubject.create();

        assertFalse(source.hasObservers());

        source.subscribe(rcp);

        assertTrue(source.hasObservers());

        TestObserver<Integer> to = rcp.test();

        assertFalse(rcp.hasComplete());
        assertFalse(rcp.hasThrowable());
        assertNull(rcp.getThrowable());
        assertTrue(rcp.hasObservers());

        source.onNext(1);

        to.assertValue(1);

        source.onNext(2);

        to.assertValues(1, 2);

        source.onError(new IOException());

        assertFalse(source.hasObservers());

        to.assertFailure(IOException.class, 1, 2);

        rcp.test().assertFailure(IOException.class);

        assertFalse(rcp.hasComplete());
        assertTrue(rcp.hasThrowable());
        assertNotNull(rcp.getThrowable());
        assertFalse(rcp.hasObservers());
    }

    @Test
    public void multipleSubscribers() {
        Subject<Integer> rcp = Subjects.refCount(PublishSubject.<Integer>create());

        PublishSubject<Integer> source = PublishSubject.create();

        source.subscribe(rcp);

        assertTrue(source.hasObservers());

        TestObserver<Integer> to1 = rcp.test();
        TestObserver<Integer> to2 = rcp.test();

        to1.dispose();

        assertTrue(source.hasObservers());

        to2.dispose();

        assertFalse(source.hasObservers());

        rcp.test()
        .assertFailure(IllegalStateException.class)
        .assertError(TestHelper.assertErrorMessage("RefCountSubject terminated"));
    }

    @Test
    public void immediatelyCancelled() {
        Subject<Integer> rcp = Subjects.refCount(PublishSubject.<Integer>create());

        PublishSubject<Integer> source = PublishSubject.create();

        source.subscribe(rcp);

        assertTrue(source.hasObservers());

        rcp.test(true);

        assertFalse(source.hasObservers());
    }

    @Test
    public void cancelTwice() {
        Subject<Integer> rcp = Subjects.refCount(PublishSubject.<Integer>create());

        PublishSubject<Integer> source = PublishSubject.create();

        source.subscribe(rcp);

        assertTrue(source.hasObservers());

        final TestObserver<Integer> to = new TestObserver<Integer>();

        rcp.subscribe(new Observer<Integer>() {

            @Override
            public void onNext(Integer t) {
                to.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                to.onError(t);
            }

            @Override
            public void onComplete() {
                to.onComplete();
            }

            @Override
            public void onSubscribe(Disposable d) {
                to.onSubscribe(d);
                assertFalse(d.isDisposed());
                d.dispose();
                assertTrue(d.isDisposed());
                d.dispose();
                assertTrue(d.isDisposed());
            }
        });

        assertFalse(source.hasObservers());

        to.assertEmpty();
    }

    @Test
    public void cancelTwiceDontCancelUp() {
        Subject<Integer> rcp = Subjects.refCount(PublishSubject.<Integer>create());

        PublishSubject<Integer> source = PublishSubject.create();

        source.subscribe(rcp);

        assertTrue(source.hasObservers());

        TestObserver<Integer> to0 = rcp.test();

        final TestObserver<Integer> to = new TestObserver<Integer>();

        rcp.subscribe(new Observer<Integer>() {

            @Override
            public void onNext(Integer t) {
                to.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                to.onError(t);
            }

            @Override
            public void onComplete() {
                to.onComplete();
            }

            @Override
            public void onSubscribe(Disposable d) {
                to.onSubscribe(d);
                d.dispose();
                d.dispose();
            }
        });

        assertTrue(source.hasObservers());

        to.assertEmpty();

        source.onNext(1);
        source.onComplete();

        to0.assertResult(1);
    }

    @Test
    public void addRemoveRace() {
        for (int i = 0; i < 1000; i++) {
            final Subject<Integer> rcp = Subjects.refCount(PublishSubject.<Integer>create());

            PublishSubject<Integer> source = PublishSubject.create();

            source.subscribe(rcp);

            assertTrue(source.hasObservers());

            final TestObserver<Integer> to1 = rcp.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to1.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    rcp.test().dispose();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());
        }
    }

    @Test
    public void doubleOnSubscribe() {
        final Subject<Integer> rcp = Subjects.refCount(PublishSubject.<Integer>create());

        Disposable bs1 = Disposables.empty();

        rcp.onSubscribe(bs1);

        Disposable bs2 = Disposables.empty();

        rcp.onSubscribe(bs2);

        assertFalse(bs1.isDisposed());
        assertTrue(bs2.isDisposed());
    }

    @Test
    public void doubleRefCount() {
        final Subject<Integer> rcp = Subjects.refCount(PublishSubject.<Integer>create());

        assertSame(rcp, Subjects.refCount(rcp));
    }
}
