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

/**
 * Convert a CompletableSource into a Nono.
 */
final class NonoFromCompletable extends Nono {

    final CompletableSource source;

    NonoFromCompletable(CompletableSource source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        source.subscribe(new FromCompletableObserver(s));
    }

    static final class FromCompletableObserver extends BasicEmptyQueueSubscription
    implements CompletableObserver {

        final Subscriber<? super Void> downstream;

        Disposable upstream;

        FromCompletableObserver(Subscriber<? super Void> downstream) {
            this.downstream = downstream;
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
            downstream.onError(t);
        }

        @Override
        public void cancel() {
            upstream.dispose();
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
