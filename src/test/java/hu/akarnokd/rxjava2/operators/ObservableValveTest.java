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

package hu.akarnokd.rxjava2.operators;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class ObservableValveTest {

    @Test
    public void passthrough() {
        Observable.range(1, 10)
        .compose(ObservableTransformers.<Integer>valve(Observable.<Boolean>never()))
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void gatedoff() {
        Observable.range(1, 10)
        .compose(ObservableTransformers.<Integer>valve(Observable.<Boolean>never(), false))
        .test()
        .assertEmpty();
    }

    @Test
    public void syncGating() {
        PublishSubject<Boolean> pp = PublishSubject.create();

        TestObserver<Integer> ts = Observable.range(1, 10)
        .compose(ObservableTransformers.<Integer>valve(pp, false))
        .test();

        ts.assertEmpty();

        pp.onNext(true);

        ts.assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        assertFalse(pp.hasObservers());
    }

    @Test
    public void gating() {
        Observable.intervalRange(1, 10, 17, 17, TimeUnit.MILLISECONDS)
        .compose(ObservableTransformers.<Long>valve(
                Observable.interval(50, TimeUnit.MILLISECONDS).map(new Function<Long, Boolean>() {
            @Override
            public Boolean apply(Long v) throws Exception {
                return (v & 1) == 0;
            }
        }), true, 16))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
    }

    @Test
    public void mainError() {
        Observable.<Integer>error(new IOException())
        .compose(ObservableTransformers.<Integer>valve(Observable.<Boolean>never()))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void otherError() {
        Observable.just(1)
        .compose(ObservableTransformers.<Integer>valve(Observable.<Boolean>error(new IOException())))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void otherCompletes() {
        Observable.just(1)
        .compose(ObservableTransformers.<Integer>valve(Observable.<Boolean>empty()))
        .test()
        .assertFailure(IllegalStateException.class);
    }

    @Test
    public void bothError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Observable.<Integer>error(new IllegalArgumentException())
            .compose(ObservableTransformers.<Integer>valve(Observable.<Boolean>error(new IOException())))
            .test()
            .assertFailure(IOException.class);

            TestHelper.assertError(errors, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void take() {
        Observable.range(1, 10)
        .compose(ObservableTransformers.<Integer>valve(Observable.<Boolean>never()))
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void openCloseRace() {
        for (int i = 0; i < 1000; i++) {
            final PublishSubject<Integer> pp1 = PublishSubject.create();
            final PublishSubject<Boolean> pp2 = PublishSubject.create();

            TestObserver<Integer> ts = pp1.compose(ObservableTransformers.<Integer>valve(pp2, false))
            .test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp1.onNext(1);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp2.onNext(true);
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());

            ts.assertValue(1).assertNoErrors().assertNotComplete();
        }
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(PublishSubject.<Integer>create().compose(ObservableTransformers.<Integer>valve(Observable.<Boolean>never())));
    }
}
