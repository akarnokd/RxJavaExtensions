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

package hu.akarnokd.rxjava2.operators;

import java.io.IOException;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.subscriptions.BooleanSubscription;

public class FlowableIndexOfTest {

    @Test
    public void empty() {
        Flowable.<Integer>empty()
        .compose(FlowableTransformers.indexOf(new Predicate<Integer>() {
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
        Flowable.range(1, 10)
        .compose(FlowableTransformers.indexOf(new Predicate<Integer>() {
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
        Flowable.range(1, 10)
        .compose(FlowableTransformers.indexOf(new Predicate<Integer>() {
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
        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                s.onNext(10);
                s.onComplete();
            }
        }
        .compose(FlowableTransformers.indexOf(new Predicate<Integer>() {
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
        Flowable.range(1, 10)
        .compose(FlowableTransformers.indexOf(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                throw new IOException();
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }
}
