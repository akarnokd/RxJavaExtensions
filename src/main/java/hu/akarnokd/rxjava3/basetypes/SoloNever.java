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

package hu.akarnokd.rxjava3.basetypes;

import org.reactivestreams.Subscriber;

import io.reactivex.internal.subscriptions.EmptySubscription;

/**
 * Never signals an event other than onSubscribe.
 */
final class SoloNever extends Solo<Object> {

    static final SoloNever INSTANCE = new SoloNever();

    @SuppressWarnings("unchecked")
    static <T> Solo<T> instance() {
        return (Solo<T>)INSTANCE;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Object> s) {
        s.onSubscribe(EmptySubscription.INSTANCE);
    }

}
