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
 * Wraps a MaybeSource and inject the assembly info.
 *
 * @param <T> the value type
 */
final class MaybeOnAssembly<T> extends Maybe<T> {

    final MaybeSource<T> source;

    final RxJavaAssemblyException assembled;

    MaybeOnAssembly(MaybeSource<T> source) {
        this.source = source;
        this.assembled = new RxJavaAssemblyException();
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new OnAssemblyMaybeObserver<T>(observer, assembled));
    }

    static final class OnAssemblyMaybeObserver<T> implements MaybeObserver<T>, Disposable {

        final MaybeObserver<? super T> downstream;

        final RxJavaAssemblyException assembled;

        Disposable upstream;

        OnAssemblyMaybeObserver(MaybeObserver<? super T> downstream, RxJavaAssemblyException assembled) {
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
        public void onSuccess(T value) {
            downstream.onSuccess(value);
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
