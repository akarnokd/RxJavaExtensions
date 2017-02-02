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

package hu.akarnokd.rxjava2.math;

import static hu.akarnokd.rxjava2.math.MathObservable.*;

import org.junit.Test;

import hu.akarnokd.rxjava2.test.BaseTest;
import io.reactivex.Observable;

public class MathObservableTest extends BaseTest {

    static Observable<Integer> intEmpty() {
        return Observable.empty();
    }

    static Observable<Long> longEmpty() {
        return Observable.empty();
    }

    static Observable<Float> floatEmpty() {
        return Observable.empty();
    }

    static Observable<Double> doubleEmpty() {
        return Observable.empty();
    }

    @Test
    public void normalSumInt() {
        assertResult(sumInt(observe(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)), 55);
    }

    @Test
    public void normalSumLong() {
        assertResult(sumLong(observe(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)), 55L);
    }

    @Test
    public void normalSumFloat() {
        assertResult(sumFloat(observe(1F, 2F, 3F, 4F, 5F, 6F, 7F, 8F, 9F, 10F)), 55F);
    }

    @Test
    public void normalSumDouble() {
        assertResult(sumDouble(observe(1D, 2D, 3D, 4D, 5D, 6D, 7D, 8D, 9D, 10D)), 55D);
    }

    @Test
    public void normalMinInt() {
        assertResult(min(observe(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)), 1);
    }

    @Test
    public void normalMinLong() {
        assertResult(min(observe(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)), 1L);
    }

    @Test
    public void normalMinFloat() {
        assertResult(min(observe(1F, 2F, 3F, 4F, 5F, 6F, 7F, 8F, 9F, 10F)), 1F);
    }

    @Test
    public void normalMinDouble() {
        assertResult(min(observe(1D, 2D, 3D, 4D, 5D, 6D, 7D, 8D, 9D, 10D)), 1D);
    }

    @Test
    public void normalMaxInt() {
        assertResult(max(observe(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)), 10);
    }

    @Test
    public void normalMaxLong() {
        assertResult(max(observe(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)), 10L);
    }

    @Test
    public void normalMaxFloat() {
        assertResult(max(observe(1F, 2F, 3F, 4F, 5F, 6F, 7F, 8F, 9F, 10F)), 10F);
    }

    @Test
    public void normalMaxDouble() {
        assertResult(max(observe(1D, 2D, 3D, 4D, 5D, 6D, 7D, 8D, 9D, 10D)), 10D);
    }

    @Test
    public void normalAverageFloat() {
        assertResult(averageFloat(observe(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)), 5.5F);
    }

    @Test
    public void normalAverageDouble() {
        assertResult(averageDouble(observe(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)), 5.5D);
    }

    @Test
    public void emptySumInt() {
        assertResult(sumInt(intEmpty()));
    }

    @Test
    public void emptySumLong() {
        assertResult(sumLong(longEmpty()));
    }

    @Test
    public void emptySumFloat() {
        assertResult(sumFloat(floatEmpty()));
    }

    @Test
    public void emptySumDouble() {
        assertResult(sumDouble(doubleEmpty()));
    }

    @Test
    public void emptyMinInt() {
        assertResult(min(intEmpty()));
    }

    @Test
    public void emptyMinLong() {
        assertResult(min(longEmpty()));
    }

    @Test
    public void emptyMinFloat() {
        assertResult(min(floatEmpty()));
    }

    @Test
    public void emptyMinDouble() {
        assertResult(min(doubleEmpty()));
    }

    @Test
    public void emptyMaxInt() {
        assertResult(max(intEmpty()));
    }

    @Test
    public void emptyMaxLong() {
        assertResult(max(longEmpty()));
    }

    @Test
    public void emptyMaxFloat() {
        assertResult(max(floatEmpty()));
    }

    @Test
    public void emptyMaxDouble() {
        assertResult(max(doubleEmpty()));
    }

    @Test
    public void emptyAverageFloat() {
        assertResult(averageFloat(floatEmpty()));
    }

    @Test
    public void emptyAverageDouble() {
        assertResult(averageDouble(doubleEmpty()));
    }
}
