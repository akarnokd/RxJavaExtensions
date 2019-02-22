/*
 * Copyright 2016-2019 David Karnok
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

import hu.akarnokd.rxjava2.basetypes.SoloSubscribeOn.SubscribeOnSubscriber;
import io.reactivex.Scheduler;
import io.reactivex.Scheduler.Worker;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * Subscribe to the upstream on the specified scheduler.
 *
 * @param <T> the value type
 */
final class PerhapsSubscribeOn<T> extends Perhaps<T> {

    final Perhaps<T> source;

    final Scheduler scheduler;

    PerhapsSubscribeOn(Perhaps<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        Worker worker = scheduler.createWorker();

        SubscribeOnSubscriber<T> parent = new SubscribeOnSubscriber<T>(s, worker, source);
        s.onSubscribe(parent);

        DisposableHelper.replace(parent.task, worker.schedule(parent));
    }
}
