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
