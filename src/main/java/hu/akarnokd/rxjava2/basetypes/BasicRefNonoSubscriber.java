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

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Basic subscriber that supports queue fusion, has an atomic reference and defaults onSubscribe, onNext and cancel.
 * 
 * @param <R> the reference type
 */
public abstract class BasicRefNonoSubscriber<R> extends BasicRefQueueSubscription<Void, R> implements Subscriber<Void> {
    private static final long serialVersionUID = -3157015053656142804L;

    protected final Subscriber<? super Void> actual;

    Subscription s;

    public BasicRefNonoSubscriber(Subscriber<? super Void> actual) {
        this.actual = actual;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validate(this.s, s)) {
            this.s = s;

            actual.onSubscribe(this);
        }
    }

    @Override
    public final void onNext(Void t) {
        // never called
    }

    @Override
    public void cancel() {
        s.cancel();
    }

    
    @Override
    public final void clear() {
        // no-op
    }
    
    @Override
    public final boolean isEmpty() {
        return true;
    }
    
    @Override
    public final Void poll() throws Exception {
        return null;
    }
    
    @Override
    public final void request(long n) {
        // no-op
    }
    
    @Override
    public final int requestFusion(int mode) {
        return mode & ASYNC;
    }

}
