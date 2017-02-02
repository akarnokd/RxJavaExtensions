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

import hu.akarnokd.rxjava2.basetypes.SoloUnsubscribeOn.UnsubscribeOnSubscriber;
import io.reactivex.Scheduler;

/**
 * Call cancel from downstream on the specified Scheduler.
 *
 * @param <T> the value type
 */
final class PerhapsUnsubscribeOn<T> extends Perhaps<T> {

    final Perhaps<T> source;

    final Scheduler scheduler;

    PerhapsUnsubscribeOn(Perhaps<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new UnsubscribeOnSubscriber<T>(s, scheduler));
    }
}
