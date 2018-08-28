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

import io.reactivex.Scheduler;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.internal.subscribers.BasicFuseableSubscriber;

/**
 * Call cancel from downstream on the specified Scheduler.
 *
 * @param <T> the value type
 */
final class SoloUnsubscribeOn<T> extends Solo<T> {

    final Solo<T> source;

    final Scheduler scheduler;

    SoloUnsubscribeOn(Solo<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new UnsubscribeOnSubscriber<T>(s, scheduler));
    }

    static final class UnsubscribeOnSubscriber<T> extends BasicFuseableSubscriber<T, T>
    implements Runnable {

        final Scheduler scheduler;

        UnsubscribeOnSubscriber(Subscriber<? super T> downstream, Scheduler scheduler) {
            super(downstream);
            this.scheduler = scheduler;
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public int requestFusion(int mode) {
            QueueSubscription<T> qs = this.qs;
            if (qs != null) {
                int m = qs.requestFusion(mode);
                sourceMode = m;
                return m;
            }
            return NONE;
        }

        @Override
        public T poll() throws Exception {
            return qs.poll();
        }

        @Override
        public void cancel() {
            scheduler.scheduleDirect(this);
        }

        @Override
        public void run() {
            super.cancel();
        }
    }
}
