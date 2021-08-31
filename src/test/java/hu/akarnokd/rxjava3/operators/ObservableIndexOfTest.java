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

package hu.akarnokd.rxjava3.operators;

import java.io.IOException;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Predicate;

public class ObservableIndexOfTest {

    @Test
    public void empty() {
        Observable.<Integer>empty()
        .compose(ObservableTransformers.indexOf(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == 5;
            }
        }))
        .test()
        .assertResult(-1L);
    }

    @Test
    public void found() {
        Observable.range(1, 10)
        .compose(ObservableTransformers.indexOf(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == 5;
            }
        }))
        .test()
        .assertResult(4L);
    }

    @Test
    public void notfound() {
        Observable.range(1, 10)
        .compose(ObservableTransformers.indexOf(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == 15;
            }
        }))
        .test()
        .assertResult(-1L);
    }

    @Test
    public void foundWithUnconditionalOnCompleteAfter() {
        new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposable.empty());
                observer.onNext(10);
                observer.onComplete();
            }
        }
        .compose(ObservableTransformers.indexOf(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == 10;
            }
        }))
        .test()
        .assertResult(0L);
    }

    @Test
    public void predicateCrash() {
        Observable.range(1, 10)
        .compose(ObservableTransformers.indexOf(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                throw new IOException();
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }
}
