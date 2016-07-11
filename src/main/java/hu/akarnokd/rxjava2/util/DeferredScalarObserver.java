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

package hu.akarnokd.rxjava2.util;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.fuseable.QueueDisposable;

/**
 * A subscriber that is unbounded-in and can generate 0 or 1 resulting value. 
 * @param <T> the input value type
 * @param <R> the output value type
 */
public abstract class DeferredScalarObserver<T, R> extends BaseQueueDisposable<R>
implements Observer<T> {
    protected final Observer<? super R> actual;
    
    protected Disposable s;
    
    protected boolean hasValue;
    
    protected R value;

    protected int fusionState;
    
    static final int NOT_FUSED = 0;
    static final int EMPTY = 1;
    static final int READY = 2;
    static final int CONSUMED = 3;
    
    volatile boolean disposed;
    
    public DeferredScalarObserver(Observer<? super R> actual) {
        this.actual = actual;
    }

    @Override
    public void onSubscribe(Disposable s) {
        if (DisposableHelper.validate(this.s, s)) {
            this.s = s;
            
            actual.onSubscribe(this);
        }
    }
    
    @Override
    public void onError(Throwable t) {
        value = null;
        actual.onError(t);
    }
    
    @Override
    public void onComplete() {
        if (hasValue) {
            complete(value);
        } else {
            actual.onComplete();
        }
    }
    
    protected final void complete(R value) {
        if (disposed) {
            return;
        }
        if (fusionState == EMPTY) {
            fusionState = READY;
            this.value = value;
        }
        actual.onNext(value);
        if (disposed) {
            return;
        }
        actual.onComplete();
    }
    
    @Override
    public final R poll() {
        if (fusionState == READY) {
            fusionState = CONSUMED;
            return value;
        }
        return null;
    }
    
    @Override
    public final boolean isDisposed() {
        return disposed;
    }
    
    @Override
    public final void dispose() {
        disposed = true;
        s.dispose();
    }
    
    @Override
    public final boolean isEmpty() {
        return fusionState != READY;
    }
    
    @Override
    public final int requestFusion(int mode) {
        return mode & QueueDisposable.ASYNC;
    }
    
    @Override
    public void clear() {
        value = null;
        fusionState = CONSUMED;
    }
}
