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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * Wraps a CompletableSource and inject the assembly info.
 */
final class CompletableOnAssembly extends Completable {

    final CompletableSource source;

    final RxJavaAssemblyException assembled;

    CompletableOnAssembly(CompletableSource source) {
        this.source = source;
        this.assembled = new RxJavaAssemblyException();
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        source.subscribe(new OnAssemblyCompletableObserver(observer, assembled));
    }

    static final class OnAssemblyCompletableObserver implements CompletableObserver, Disposable {

        final CompletableObserver downstream;

        final RxJavaAssemblyException assembled;

        Disposable upstream;

        OnAssemblyCompletableObserver(CompletableObserver downstream, RxJavaAssemblyException assembled) {
            this.downstream = downstream;
            this.assembled = assembled;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(assembled.appendLast(t));
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void dispose() {
            // don't break the link here
            upstream.dispose();
        }
    }
}
