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

import hu.akarnokd.rxjava2.basetypes.SoloHide.HideSubscriber;

/**
 * Hides the identity of the upstream Solo and its Subscription.
 *
 * @param <T> the value type
 */
final class PerhapsHide<T> extends Perhaps<T> {

    final Perhaps<T> source;

    PerhapsHide(Perhaps<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new HideSubscriber<T>(s));
    }
}
