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

package hu.akarnokd.rxjava2.test;

import org.reactivestreams.Publisher;

import io.reactivex.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;

public abstract class BaseTest {

    public static <T> void assertResult(ObservableSource<T> source, T... array) {
        TestObserver<T> ts = new TestObserver<T>();

        source.subscribe(ts);

        ts.assertValues(array);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    public static <T> void assertResult(Publisher<T> source, T... array) {
        TestSubscriber<T> ts = new TestSubscriber<T>();

        source.subscribe(ts);

        ts.assertValues(array)
        .assertNoErrors()
        .assertComplete();
    }


    public static <T> Observable<T> observe(T... array) {
        return Observable.fromArray(array);
    }

    public static <T> Flowable<T> flow(T... array) {
        return Flowable.fromArray(array);
    }
}
