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

import java.util.concurrent.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Block until the upstream terminates.
 */
final class NonoBlockingAwaitSubscriber extends CountDownLatch implements Subscriber<Void> {

    Throwable error;

    Subscription upstream;

    volatile boolean cancelled;

    NonoBlockingAwaitSubscriber() {
        super(1);
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validate(this.upstream, s)) {
            this.upstream = s;
            if (cancelled) {
                s.cancel();
            }
        }
    }

    void cancel() {
        cancelled = true;
        Subscription s = this.upstream;
        if (s != null) {
            s.cancel();
        }
    }

    @Override
    public void onNext(Void t) {
        // not received
    }

    @Override
    public void onError(Throwable t) {
        error = t;
        countDown();
    }

    @Override
    public void onComplete() {
        countDown();
    }

    Throwable blockingAwait() {
        if (getCount() != 0) {
            try {
                await();
            } catch (InterruptedException ex) {
                cancel();
                return ex;
            }
        }
        return error;
    }

    Throwable blockingAwait(long timeout, TimeUnit unit) {
        if (getCount() != 0) {
            try {
                if (!await(timeout, unit)) {
                    cancel();
                    return new TimeoutException();
                }
            } catch (InterruptedException ex) {
                cancel();
                return ex;
            }
        }
        return error;
    }
}
