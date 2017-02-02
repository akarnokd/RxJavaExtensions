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

package hu.akarnokd.rxjava2.debug;

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
    protected void subscribeActual(CompletableObserver s) {
        source.subscribe(new OnAssemblyCompletableObserver(s, assembled));
    }

    static final class OnAssemblyCompletableObserver implements CompletableObserver, Disposable {

        final CompletableObserver actual;

        final RxJavaAssemblyException assembled;

        Disposable d;

        OnAssemblyCompletableObserver(CompletableObserver actual, RxJavaAssemblyException assembled) {
            this.actual = actual;
            this.assembled = assembled;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(assembled.appendLast(t));
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }

        @Override
        public boolean isDisposed() {
            return d.isDisposed();
        }

        @Override
        public void dispose() {
            // don't break the link here
            d.dispose();
        }
    }
}
