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

import hu.akarnokd.rxjava3.basetypes.SoloDoFinally.DoFinallySubscriber;
import io.reactivex.functions.Action;

/**
 * Execute an action exactly once after the upstream terminates or the
 * downstream cancels.
 * @param <T> the input and output element type
 */
final class PerhapsDoFinally<T> extends Perhaps<T> {

    final Perhaps<T> source;

    final Action onFinally;

    PerhapsDoFinally(Perhaps<T> source, Action onFinally) {
        this.source = source;
        this.onFinally = onFinally;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new DoFinallySubscriber<T>(s, onFinally));
    }
}
