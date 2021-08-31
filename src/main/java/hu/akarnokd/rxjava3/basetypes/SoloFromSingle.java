/*
 * Copyright 2016-present David Karnok
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

package hu.akarnokd.rxjava3.basetypes;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.subscriptions.DeferredScalarSubscription;

/**
 * Wrap a Single into a Solo.
 *
 * @param <T> the value type
 */
final class SoloFromSingle<T> extends Solo<T> {

    final SingleSource<T> source;

    SoloFromSingle(SingleSource<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new FromSingleObserver<T>(s));
    }

    static final class FromSingleObserver<T> extends DeferredScalarSubscription<T> implements SingleObserver<T> {

        private static final long serialVersionUID = 1184208074074285424L;

        Disposable upstream;

        FromSingleObserver(Subscriber<? super T> downstream) {
            super(downstream);
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
            complete(value);
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void cancel() {
            super.cancel();
            upstream.dispose();
        }
    }
}
