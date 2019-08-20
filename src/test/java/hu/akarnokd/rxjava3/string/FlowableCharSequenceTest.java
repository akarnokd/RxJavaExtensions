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

package hu.akarnokd.rxjava3.string;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class FlowableCharSequenceTest {

    @Test
    public void normal() {
        StringFlowable.characters("abcdefgh")
        .test()
        .assertResult((int)'a', (int)'b', (int)'c', (int)'d', (int)'e', (int)'f', (int)'g', (int)'h');
    }

    @Test
    public void take() {
        StringFlowable.characters("abcdefgh")
        .take(3)
        .test()
        .assertResult((int)'a', (int)'b', (int)'c');
    }

    @Test
    public void take1Of1() {
        StringFlowable.characters("a")
        .take(1)
        .test()
        .assertResult((int)'a');
    }

    @Test
    public void backpressure() {
        StringFlowable.characters("abcdefgh")
        .test(0)
        .assertEmpty()
        .requestMore(3)
        .assertValues((int)'a', (int)'b', (int)'c')
        .requestMore(3)
        .assertValues((int)'a', (int)'b', (int)'c', (int)'d', (int)'e', (int)'f')
        .requestMore(2)
        .assertResult((int)'a', (int)'b', (int)'c', (int)'d', (int)'e', (int)'f', (int)'g', (int)'h');
        ;
    }

    @Test
    public void backpressure2() {
        StringFlowable.characters("a")
        .test(0)
        .assertEmpty()
        .requestMore(1)
        .assertResult((int)'a');
    }

    @Test
    public void backpressure3() {
        StringFlowable.characters("abc")
        .rebatchRequests(1)
        .test(0)
        .assertEmpty()
        .requestMore(3)
        .assertResult((int)'a', (int)'b', (int)'c');
    }

    @Test
    public void backpressure3Hidden() {
        StringFlowable.characters("abc")
        .hide()
        .rebatchRequests(1)
        .test(0)
        .assertEmpty()
        .requestMore(3)
        .assertResult((int)'a', (int)'b', (int)'c');
    }

    @Test
    public void backpressure4() {
        StringFlowable.characters("abcdefgh")
        .test(8)
        .assertResult((int)'a', (int)'b', (int)'c', (int)'d', (int)'e', (int)'f', (int)'g', (int)'h');
    }

    @Test
    public void empty() {
        StringFlowable.characters("")
        .test()
        .assertResult();
    }

    @Test
    public void emptyBackpressured() {
        StringFlowable.characters("")
        .test(1)
        .assertResult();
    }

    @Test
    public void concatMap() {
        StringFlowable.characters("abcdefgh")
        .concatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v)
                    throws Exception {
                return Flowable.just(v);
            }
        })
        .test()
        .assertResult((int)'a', (int)'b', (int)'c', (int)'d', (int)'e', (int)'f', (int)'g', (int)'h');
    }

    @Test
    public void concatMapTake() {
        StringFlowable.characters("abcdefgh")
        .concatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v)
                    throws Exception {
                return Flowable.just(v);
            }
        })
        .take(3)
        .test()
        .assertResult((int)'a', (int)'b', (int)'c');
    }

    @Test
    public void slowPathCancel() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(2) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };
        StringFlowable.characters("abcdefgh")
        .subscribe(ts);

        ts.assertResult((int)'a');
    }

    @Test
    public void fusedCancel() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };
        StringFlowable.characters("abcdefgh")
        .rebatchRequests(2)
        .subscribe(ts);

        ts.assertResult((int)'a');
    }

    @Test
    public void slowPathRequestMore() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(2) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                request(1);
            }
        };
        StringFlowable.characters("abcdefgh")
        .subscribe(ts);

        ts.assertResult((int)'a', (int)'b', (int)'c', (int)'d', (int)'e', (int)'f', (int)'g', (int)'h');
    }
}
