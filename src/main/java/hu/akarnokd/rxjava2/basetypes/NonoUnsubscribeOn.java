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

package hu.akarnokd.rxjava2.basetypes;

import org.reactivestreams.Subscriber;

import io.reactivex.Scheduler;

/**
 * Execute the downstream cancel call on the specified scheduler.
 */
final class NonoUnsubscribeOn extends Nono {

    final Nono source;

    final Scheduler scheduler;

    NonoUnsubscribeOn(Nono source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        source.subscribe(new UnsubscribeOnSubscriber(s, scheduler));
    }

    static final class UnsubscribeOnSubscriber extends BasicNonoSubscriber implements Runnable {

        final Scheduler scheduler;

        UnsubscribeOnSubscriber(Subscriber<? super Void> actual, Scheduler scheduler) {
            super(actual);
            this.scheduler = scheduler;
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }

        @Override
        public void cancel() {
            scheduler.scheduleDirect(this);
        }

        @Override
        public void run() {
            s.cancel();
        }
    }
}
