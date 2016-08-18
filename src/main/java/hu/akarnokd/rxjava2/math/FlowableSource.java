package hu.akarnokd.rxjava2.math;

import org.reactivestreams.Publisher;

import io.reactivex.Flowable;

abstract class FlowableSource<T, R> extends Flowable<R> {
    
    protected final Publisher<T> source;
    
    public FlowableSource(Publisher<T> source) {
        this.source = source;
    }
}
