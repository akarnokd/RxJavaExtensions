/*
 * Copyright 2016 David Karnok
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

import org.junit.Test;

import static hu.akarnokd.rxjava2.math.MathObservable.*;

import io.reactivex.*;
import io.reactivex.observers.TestObserver;

public class MathObservableTest {

    static <T> void assertResult(ObservableConsumable<T> source, T... array) {
        TestObserver<T> ts = new TestObserver<T>();
        
        source.subscribe(ts);
        
        ts.assertValues(array);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    static <T> Observable<T> values(T... array) {
        return Observable.fromArray(array);
    }
    

    @Test
    public void normalSumInt() {
        assertResult(sumInt(values(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)), 55);
    }
    
    @Test
    public void normalSumLong() {
        assertResult(sumLong(values(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)), 55L);
    }

    @Test
    public void normalSumFloat() {
        assertResult(sumFloat(values(1F, 2F, 3F, 4F, 5F, 6F, 7F, 8F, 9F, 10F)), 55F);
    }

    @Test
    public void normalSumDouble() {
        assertResult(sumDouble(values(1D, 2D, 3D, 4D, 5D, 6D, 7D, 8D, 9D, 10D)), 55D);
    }
    
    @Test
    public void normalMinInt() {
        assertResult(min(values(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)), 1);
    }
    
    @Test
    public void normalMinLong() {
        assertResult(min(values(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)), 1L);
    }

    @Test
    public void normalMinFloat() {
        assertResult(min(values(1F, 2F, 3F, 4F, 5F, 6F, 7F, 8F, 9F, 10F)), 1F);
    }

    @Test
    public void normalMinDouble() {
        assertResult(min(values(1D, 2D, 3D, 4D, 5D, 6D, 7D, 8D, 9D, 10D)), 1D);
    }
    
    @Test
    public void normalMaxInt() {
        assertResult(max(values(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)), 10);
    }
    
    @Test
    public void normalMaxLong() {
        assertResult(max(values(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L)), 10L);
    }

    @Test
    public void normalMaxFloat() {
        assertResult(max(values(1F, 2F, 3F, 4F, 5F, 6F, 7F, 8F, 9F, 10F)), 10F);
    }

    @Test
    public void normalMaxDouble() {
        assertResult(max(values(1D, 2D, 3D, 4D, 5D, 6D, 7D, 8D, 9D, 10D)), 10D);
    }
    
    @Test
    public void normalAverageFloat() {
        assertResult(averageFloat(values(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)), 5.5F);
    }
    
    @Test
    public void normalAverageDouble() {
        assertResult(averageDouble(values(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)), 5.5D);
    }
}
