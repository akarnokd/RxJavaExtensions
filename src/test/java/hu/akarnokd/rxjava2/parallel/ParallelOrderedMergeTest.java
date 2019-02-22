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

package hu.akarnokd.rxjava2.parallel;

import java.util.Random;

import org.junit.Test;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class ParallelOrderedMergeTest {
  @Test
  public void testBasicOrder() throws InterruptedException {
    final Random rnd = new Random();
    ParallelTransformers.orderedMerge(Flowable.range(1, 5).parallel(4).runOn(Schedulers.computation()).map(new Function<Integer, Integer>() {
      @Override
      public Integer apply(Integer t) throws Exception {
        Thread.sleep(0, 50 + rnd.nextInt(1000));
        return t;
      }
    })).test().await().assertResult(1, 2, 3, 4, 5);
  }
}
