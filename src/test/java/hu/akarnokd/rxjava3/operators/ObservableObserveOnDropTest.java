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

package hu.akarnokd.rxjava3.operators;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.*;
import io.reactivex.functions.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;

public class ObservableObserveOnDropTest {

    @Test
    public void normal() {
        TestObserver<Object> to = Observable.range(1, 1000000)
        .compose(ObservableTransformers.observeOnDrop(Schedulers.computation()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertComplete();

        assertTrue("" + to.values().size(),
                to.values().size() >= 1 && to.values().size() <= 1000000);
    }

    @Test
    public void normalTrampoline() {
        Observable.range(1, 1000000)
        .compose(ObservableTransformers.observeOnDrop(Schedulers.trampoline()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1000000)
        .assertComplete()
        ;
    }

    @Test
    public void error() {
        Observable.error(new IOException())
        .compose(ObservableTransformers.observeOnDrop(Schedulers.computation()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void take() {
        Observable.range(1, 10)
        .compose(ObservableTransformers.observeOnDrop(Schedulers.trampoline()))
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5)
        ;
    }

    @Test
    public void badSource() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o)
                    throws Exception {
                return o.compose(ObservableTransformers.observeOnDrop(Schedulers.trampoline()));
            }
        });
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Observable.range(1, 10)
        .compose(ObservableTransformers.observeOnDrop(Schedulers.trampoline())));
    }
}
