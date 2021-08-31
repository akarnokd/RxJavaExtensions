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

import io.reactivex.rxjava3.core.Flowable;

public class FlowableEveryTest {

    @Test
    public void every1() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>every(1))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void every1AndError() {
        Flowable.range(1, 5).concatWith(Flowable.<Integer>error(new IOException()))
        .compose(FlowableTransformers.<Integer>every(1))
        .test()
        .assertFailure(IOException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void every1Rebatch() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>every(1))
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void every1Take() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>every(1))
        .take(3)
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void every2() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>every(2))
        .test()
        .assertResult(2, 4);
    }

    @Test
    public void every6() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.<Integer>every(6))
        .test()
        .assertResult();
    }

    @Test
    public void every1000() {
        Flowable.range(1, 5000)
        .compose(FlowableTransformers.<Integer>every(1000))
        .test()
        .assertResult(1000, 2000, 3000, 4000, 5000);
    }
}
