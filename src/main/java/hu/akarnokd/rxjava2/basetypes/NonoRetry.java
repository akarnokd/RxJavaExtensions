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

import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava2.basetypes.NonoRepeat.RedoSubscriber;

/**
 * Repeatedly re-subscribe to the source Nono if it fails.
 */
final class NonoRetry extends Nono {

    final Nono source;

    final long times;

    NonoRetry(Nono source, long times) {
        this.source = source;
        this.times = times;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        source.subscribe(new RetrySubscriber(s, times, source));
    }

    static final class RetrySubscriber extends RedoSubscriber {

        private static final long serialVersionUID = 3432411068139897716L;

        RetrySubscriber(Subscriber<? super Void> actual, long times, Nono source) {
            super(actual, times, source);
        }

        @Override
        public void onError(Throwable t) {
            active = false;
            subscribeNext(t);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }
    }
}
