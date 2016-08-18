package hu.akarnokd.rxjava2.math;

import io.reactivex.*;

abstract class ObservableWithSource<T, R> extends Observable<R> {
    
    protected final ObservableSource<T> source;
    
    public ObservableWithSource(ObservableSource<T> source) {
        this.source = source;
    }
}
