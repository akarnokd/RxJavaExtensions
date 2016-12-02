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

package hu.akarnokd.rxjava2.basetypes;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Calls the appropriate function when this Subscriber terminates
 */
final class NonoLambdaSubscriber extends AtomicReference<Subscription> implements Subscriber<Void>, Disposable {

    private static final long serialVersionUID = 2347769328526232927L;

    final Action onComplete;

    final Consumer<? super Throwable> onError;

    NonoLambdaSubscriber(Action onComplete, Consumer<? super Throwable> onError) {
        this.onComplete = onComplete;
        this.onError = onError;
    }

    @Override
    public void onSubscribe(Subscription s) {
        SubscriptionHelper.setOnce(this, s);
    }

    @Override
    public void onNext(Void t) {
        // never called
    }

    @Override
    public void onError(Throwable t) {
        try {
            onError.accept(t);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(new CompositeException(t, ex));
        }
    }

    @Override
    public void onComplete() {
        try {
            onComplete.run();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        }
    }

    @Override
    public boolean isDisposed() {
        return SubscriptionHelper.isCancelled(get());
    }

    @Override
    public void dispose() {
        SubscriptionHelper.cancel(this);
    }
}
