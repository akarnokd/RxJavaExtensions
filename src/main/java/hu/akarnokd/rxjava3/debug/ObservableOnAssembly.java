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

package hu.akarnokd.rxjava3.debug;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.operators.QueueDisposable;
import io.reactivex.rxjava3.internal.observers.BasicFuseableObserver;

/**
 * Wraps a ObservableSource and inject the assembly info.
 *
 * @param <T> the value type
 */
final class ObservableOnAssembly<T> extends Observable<T> {

    final ObservableSource<T> source;

    final RxJavaAssemblyException assembled;

    ObservableOnAssembly(ObservableSource<T> source) {
        this.source = source;
        this.assembled = new RxJavaAssemblyException();
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new OnAssemblyObserver<T>(observer, assembled));
    }

    static final class OnAssemblyObserver<T> extends BasicFuseableObserver<T, T> {

        final RxJavaAssemblyException assembled;

        OnAssemblyObserver(Observer<? super T> downstream, RxJavaAssemblyException assembled) {
            super(downstream);
            this.assembled = assembled;
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(assembled.appendLast(t));
        }

        @Override
        public int requestFusion(int mode) {
            QueueDisposable<T> qd = this.qd;
            if (qd != null) {
                int m = qd.requestFusion(mode);
                sourceMode = m;
                return m;
            }
            return NONE;
        }

        @Override
        public T poll() throws Throwable {
            return qd.poll();
        }
    }
}
