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

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;

/**
 * Completes the subscriber immediately.
 */
final class PerhapsEmpty extends Perhaps<Object> implements Supplier<Object> {

    static final PerhapsEmpty INSTANCE = new PerhapsEmpty();

    @SuppressWarnings("unchecked")
    static <T> Perhaps<T> instance() {
        return (Perhaps<T>)INSTANCE;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Object> s) {
        EmptySubscription.complete(s);
    }

    @Override
    public Object get() throws Exception {
        return null;
    }
}
