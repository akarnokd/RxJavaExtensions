/*
 * Copyright 2016-2017 David Karnok
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

package hu.akarnokd.rxjava2.debug;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.functions.Action;
import io.reactivex.observers.TestObserver;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.subscribers.TestSubscriber;

/**
 * Named as such to avoid being filtered out in stacktraces of this test.
 */
public class RxJava2AssemblyTrackingTest {

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
        }).blockingGet();

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

            String st = RxJavaAssemblyException.find(ts.errors().get(0)).stacktrace();

            assertTrue(st, st.contains("RxJava2AssemblyTrackingTest.createFlowable"));

        } finally {
            RxJavaAssemblyTracking.disable();
        }

        Flowable<Integer> source = createFlowable();

        TestSubscriber<Integer> ts = source.test()
        .assertFailure(IOException.class, 1, 2, 3, 4, 5);

        assertNull(RxJavaAssemblyException.find(ts.errors().get(0)));
    }

    @Test
    public void observable() {
        RxJavaAssemblyTracking.enable();
        try {
            Observable<Integer> source = createObservable();

            TestObserver<Integer> ts = source.test()
            .assertFailure(IOException.class, 1, 2, 3, 4, 5);

            String st = RxJavaAssemblyException.find(ts.errors().get(0)).stacktrace();

            assertTrue(st, st.contains("RxJava2AssemblyTrackingTest.createObservable"));

        } finally {
            RxJavaAssemblyTracking.disable();
        }

        Flowable<Integer> source = createFlowable();

        TestSubscriber<Integer> ts = source.test()
        .assertFailure(IOException.class, 1, 2, 3, 4, 5);

        assertNull(RxJavaAssemblyException.find(ts.errors().get(0)));
    }

    @Test
    public void single() {
        RxJavaAssemblyTracking.enable();
        try {
            Single<Integer> source = createSingle();

            TestObserver<Integer> ts = source.test()
            .assertFailure(IOException.class);

            String st = RxJavaAssemblyException.find(ts.errors().get(0)).stacktrace();

            assertTrue(st, st.contains("RxJava2AssemblyTrackingTest.createSingle"));

        } finally {
            RxJavaAssemblyTracking.disable();
        }

        Single<Integer> source = createSingle();

        TestObserver<Integer> ts = source.test()
        .assertFailure(IOException.class);

        assertNull(RxJavaAssemblyException.find(ts.errors().get(0)));
    }

    @Test
    public void maybe() {
        RxJavaAssemblyTracking.enable();
        try {
            Maybe<Integer> source = createMaybe();

            TestObserver<Integer> ts = source.test()
            .assertFailure(IOException.class);

            String st = RxJavaAssemblyException.find(ts.errors().get(0)).stacktrace();

            assertTrue(st, st.contains("RxJava2AssemblyTrackingTest.createMaybe"));

        } finally {
            RxJavaAssemblyTracking.disable();
        }

        Maybe<Integer> source = createMaybe();

        TestObserver<Integer> ts = source.test()
        .assertFailure(IOException.class);

        assertNull(RxJavaAssemblyException.find(ts.errors().get(0)));
    }

    @Test
    public void completable() {
        RxJavaAssemblyTracking.enable();
        try {
            Completable source = createCompletable();

            TestObserver<Void> ts = source.test()
            .assertFailure(IOException.class);

            String st = RxJavaAssemblyException.find(ts.errors().get(0)).stacktrace();

            assertTrue(st, st.contains("RxJava2AssemblyTrackingTest.createCompletable"));

        } finally {
            RxJavaAssemblyTracking.disable();
        }

        Completable source = createCompletable();

        TestObserver<Void> ts = source.test()
        .assertFailure(IOException.class);

        assertNull(RxJavaAssemblyException.find(ts.errors().get(0)));
    }

    @Test
    public void parallelFlowable() {
        RxJavaAssemblyTracking.enable();
        try {
            ParallelFlowable<Integer> source = createParallelFlowable();

            TestSubscriber<Integer> ts = source.sequential().test()
            .assertFailure(IOException.class, 1, 2, 3, 4, 5);

            String st = RxJavaAssemblyException.find(ts.errors().get(0)).stacktrace();

            assertTrue(st, st.contains("RxJava2AssemblyTrackingTest.createParallelFlowable"));

        } finally {
            RxJavaAssemblyTracking.disable();
        }

        ParallelFlowable<Integer> source = createParallelFlowable();

        TestSubscriber<Integer> ts = source.sequential().test()
        .assertFailure(IOException.class, 1, 2, 3, 4, 5);

        assertNull(RxJavaAssemblyException.find(ts.errors().get(0)));
    }
}
