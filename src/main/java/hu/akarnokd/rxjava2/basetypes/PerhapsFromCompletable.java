/*
 * Copyright 2016-2018 David Karnok
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

package hu.akarnokd.rxjava2.basetypes;

import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.subscriptions.DeferredScalarSubscription;

/**
 * Wrap a Completable into a Perhaps.
 *
 * @param <T> the value type
 */
final class PerhapsFromCompletable<T> extends Perhaps<T> {

    final CompletableSource source;

    PerhapsFromCompletable(CompletableSource source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new FromCompletableObserver<T>(s));
    }

    static final class FromCompletableObserver<T> extends DeferredScalarSubscription<T> implements CompletableObserver {

        private static final long serialVersionUID = 1184208074074285424L;

        Disposable d;

        FromCompletableObserver(Subscriber<? super T> actual) {
            super(actual);
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }

        @Override
        public void cancel() {
            super.cancel();
            d.dispose();
        }
    }
}
