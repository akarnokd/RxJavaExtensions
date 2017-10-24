package hu.akarnokd.rxjava2.parallel;

import java.util.Comparator;

import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava2.basetypes.BasicMergeSubscription;
import io.reactivex.Flowable;
import io.reactivex.parallel.ParallelFlowable;

public class ParallelOrderedMerge<T> extends Flowable<T> {

  final ParallelFlowable<T> source;

  final Comparator<? super T> comparator;

  final boolean delayErrors;

  final int prefetch;

  ParallelOrderedMerge(ParallelFlowable<T> source,
          Comparator<? super T> comparator,
          boolean delayErrors, int prefetch) {
      this.source = source;
      this.comparator = comparator;
      this.delayErrors = delayErrors;
      this.prefetch = prefetch;
  }

  @Override
  protected void subscribeActual(Subscriber<? super T> s) {
      final BasicMergeSubscription<T> parent = new BasicMergeSubscription<T>(s, comparator, this.source.parallelism(), prefetch, delayErrors);
      s.onSubscribe(parent);
      parent.subscribe(this.source);
  }
}
