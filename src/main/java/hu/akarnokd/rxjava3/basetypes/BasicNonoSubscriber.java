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

import org.reactivestreams.*;

import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Basic subscriber that supports queue fusion and defaults onSubscribe, onNext and cancel.
 */
abstract class BasicNonoSubscriber extends BasicEmptyQueueSubscription implements Subscriber<Object> {

    protected final Subscriber<? super Void> downstream;

    protected Subscription upstream;

    BasicNonoSubscriber(Subscriber<? super Void> downstream) {
        this.downstream = downstream;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validate(this.upstream, s)) {
            this.upstream = s;

            downstream.onSubscribe(this);
        }
    }

    @Override
    public final void onNext(Object t) {
        // never called
    }

    @Override
    public void cancel() {
        upstream.cancel();
    }

}
