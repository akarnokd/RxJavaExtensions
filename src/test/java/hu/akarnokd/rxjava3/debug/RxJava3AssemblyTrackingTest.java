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

package hu.akarnokd.rxjava3.debug;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.*;

import org.junit.Test;

import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

/**
 * Named as such to avoid being filtered out in stacktraces of this test.
 */
public class RxJava3AssemblyTrackingTest {

//    @Test
    public void printFilter() {
        System.err.println(new RxJavaAssemblyException().stacktrace());
    }

    public static void main(String[] args) throws Exception {
        System.err.println(new RxJavaAssemblyException().stacktrace());

        Completable.fromAction(new Action() {
            @Override
            public void run() {
                System.err.println(new RxJavaAssemblyException().stacktrace());
            }
        }).blockingAwait();

        ExecutorService exec = Executors.newSingleThreadExecutor();

        exec.submit(new Runnable() {
            @Override
            public void run() {
                System.err.println(new RxJavaAssemblyException().stacktrace());
            }
        }).get();
    }

    static Flowable<Integer> createFlowable() {
        return Flowable.range(1, 5).concatWith(Flowable.<Integer>error(new IOException()));
    }

    static Observable<Integer> createObservable() {
        return Observable.range(1, 5).concatWith(Observable.<Integer>error(new IOException()));
    }

    static final Single<Integer> createSingle() {
        return Single.<Integer>error(new IOException());
    }

    static Maybe<Integer> createMaybe() {
        return Maybe.<Integer>error(new IOException());
    }

    static Completable createCompletable() {
        return Completable.error(new IOException());
    }

    static ParallelFlowable<Integer> createParallelFlowable() {
        return Flowable.range(1, 5).concatWith(Flowable.<Integer>error(new IOException())).parallel();
    }

    @Test
    public void flowable() {
        RxJavaAssemblyTracking.enable();
        try {
            Flowable<Integer> source = createFlowable();

            TestSubscriber<Integer> ts = source.test()
            .assertFailure(IOException.class, 1, 2, 3, 4, 5);

            String st = RxJavaAssemblyException.find(TestHelper.errors(ts).get(0)).stacktrace();

            assertTrue(st, st.contains("RxJava3AssemblyTrackingTest.createFlowable"));

        } finally {
            RxJavaAssemblyTracking.disable();
        }

        Flowable<Integer> source = createFlowable();

        TestSubscriber<Integer> ts = source.test()
        .assertFailure(IOException.class, 1, 2, 3, 4, 5);

        assertNull(RxJavaAssemblyException.find(TestHelper.errors(ts).get(0)));
    }

    @Test
    public void observable() {
        RxJavaAssemblyTracking.enable();
        try {
            Observable<Integer> source = createObservable();

            TestObserver<Integer> to = source.test()
            .assertFailure(IOException.class, 1, 2, 3, 4, 5);

            String st = RxJavaAssemblyException.find(TestHelper.errors(to).get(0)).stacktrace();

            assertTrue(st, st.contains("RxJava3AssemblyTrackingTest.createObservable"));

        } finally {
            RxJavaAssemblyTracking.disable();
        }

        Flowable<Integer> source = createFlowable();

        TestSubscriber<Integer> ts = source.test()
        .assertFailure(IOException.class, 1, 2, 3, 4, 5);

        assertNull(RxJavaAssemblyException.find(TestHelper.errors(ts).get(0)));
    }

    @Test
    public void single() {
        RxJavaAssemblyTracking.enable();
        try {
            Single<Integer> source = createSingle();

            TestObserver<Integer> to = source.test()
            .assertFailure(IOException.class);

            String st = RxJavaAssemblyException.find(TestHelper.errors(to).get(0)).stacktrace();

            assertTrue(st, st.contains("RxJava3AssemblyTrackingTest.createSingle"));

        } finally {
            RxJavaAssemblyTracking.disable();
        }

        Single<Integer> source = createSingle();

        TestObserver<Integer> to = source.test()
        .assertFailure(IOException.class);

        assertNull(RxJavaAssemblyException.find(TestHelper.errors(to).get(0)));
    }

    @Test
    public void maybe() {
        RxJavaAssemblyTracking.enable();
        try {
            Maybe<Integer> source = createMaybe();

            TestObserver<Integer> to = source.test()
            .assertFailure(IOException.class);

            String st = RxJavaAssemblyException.find(TestHelper.errors(to).get(0)).stacktrace();

            assertTrue(st, st.contains("RxJava3AssemblyTrackingTest.createMaybe"));

        } finally {
            RxJavaAssemblyTracking.disable();
        }

        Maybe<Integer> source = createMaybe();

        TestObserver<Integer> to = source.test()
        .assertFailure(IOException.class);

        assertNull(RxJavaAssemblyException.find(TestHelper.errors(to).get(0)));
    }

    @Test
    public void completable() {
        RxJavaAssemblyTracking.enable();
        try {
            Completable source = createCompletable();

            TestObserver<Void> to = source.test()
            .assertFailure(IOException.class);

            String st = RxJavaAssemblyException.find(TestHelper.errors(to).get(0)).stacktrace();

            assertTrue(st, st.contains("RxJava3AssemblyTrackingTest.createCompletable"));

        } finally {
            RxJavaAssemblyTracking.disable();
        }

        Completable source = createCompletable();

        TestObserver<Void> to = source.test()
        .assertFailure(IOException.class);

        assertNull(RxJavaAssemblyException.find(TestHelper.errors(to).get(0)));
    }

    @Test
    public void parallelFlowable() {
        RxJavaAssemblyTracking.enable();
        try {
            ParallelFlowable<Integer> source = createParallelFlowable();

            TestSubscriber<Integer> ts = source.sequential().test()
            .assertFailure(IOException.class, 1, 2, 3, 4, 5);

            String st = RxJavaAssemblyException.find(TestHelper.errors(ts).get(0)).stacktrace();

            assertTrue(st, st.contains("RxJava3AssemblyTrackingTest.createParallelFlowable"));

        } finally {
            RxJavaAssemblyTracking.disable();
        }

        ParallelFlowable<Integer> source = createParallelFlowable();

        TestSubscriber<Integer> ts = source.sequential().test()
        .assertFailure(IOException.class, 1, 2, 3, 4, 5);

        assertNull(RxJavaAssemblyException.find(TestHelper.errors(ts).get(0)));
    }

    @Test
    public void observableReplayResetWorks() {

        RxJavaAssemblyTracking.enable();
        try {
            PublishSubject<Integer> ps = PublishSubject.create();

            Observable<Integer> source = ps.replay(1).refCount();

            Disposable d1 = source.subscribe();
            Disposable d2 = source.subscribe();

            ps.onNext(1);

            d1.dispose();
            d2.dispose();

            TestObserver<Integer> to1 = source.test();
            TestObserver<Integer> to2 = source.test();

            ps.onNext(2);
            ps.onNext(3);
            ps.onNext(4);

            to1.assertValuesOnly(2, 3, 4);
            to2.assertValuesOnly(2, 3, 4);
        } finally {
            RxJavaAssemblyTracking.disable();
        }
    }

    @Test
    public void flowableReplayResetWorks() {

        RxJavaAssemblyTracking.enable();
        try {
            PublishProcessor<Integer> pp = PublishProcessor.create();

            Flowable<Integer> source = pp.replay(1).refCount();

            Disposable d1 = source.subscribe();
            Disposable d2 = source.subscribe();

            pp.onNext(1);

            d1.dispose();
            d2.dispose();

            TestSubscriber<Integer> ts1 = source.test();
            TestSubscriber<Integer> ts2 = source.test();

            pp.onNext(2);
            pp.onNext(3);
            pp.onNext(4);

            ts1.assertValuesOnly(2, 3, 4);
            ts2.assertValuesOnly(2, 3, 4);
        } finally {
            RxJavaAssemblyTracking.disable();
        }
    }

    @Test
    public void observablePublishResetWorks() {

        RxJavaAssemblyTracking.enable();
        try {
            PublishSubject<Integer> ps = PublishSubject.create();

            Observable<Integer> source = ps.publish().refCount();

            Disposable d1 = source.subscribe();
            Disposable d2 = source.subscribe();

            ps.onNext(1);

            d1.dispose();
            d2.dispose();

            TestObserver<Integer> to1 = source.test();
            TestObserver<Integer> to2 = source.test();

            ps.onNext(2);
            ps.onNext(3);
            ps.onNext(4);

            to1.assertValuesOnly(2, 3, 4);
            to2.assertValuesOnly(2, 3, 4);
        } finally {
            RxJavaAssemblyTracking.disable();
        }
    }

    @Test
    public void flowablePublishResetWorks() {

        RxJavaAssemblyTracking.enable();
        try {
            PublishProcessor<Integer> pp = PublishProcessor.create();

            Flowable<Integer> source = pp.publish().refCount();

            Disposable d1 = source.subscribe();
            Disposable d2 = source.subscribe();

            pp.onNext(1);

            d1.dispose();
            d2.dispose();

            TestSubscriber<Integer> ts1 = source.test();
            TestSubscriber<Integer> ts2 = source.test();

            pp.onNext(2);
            pp.onNext(3);
            pp.onNext(4);

            ts1.assertValuesOnly(2, 3, 4);
            ts2.assertValuesOnly(2, 3, 4);
        } finally {
            RxJavaAssemblyTracking.disable();
        }
    }
}
