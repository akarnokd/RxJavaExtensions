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

package hu.akarnokd.rxjava2.debug;

import org.reactivestreams.*;

import hu.akarnokd.rxjava2.debug.FlowableOnAssembly.*;
import io.reactivex.Flowable;
import io.reactivex.internal.fuseable.*;

/**
 * Wraps a Publisher and inject the assembly info.
 *
 * @param <T> the value type
 */
final class FlowableOnAssemblyScalarCallable<T> extends Flowable<T> implements ScalarCallable<T> {

    final Publisher<T> source;

    final RxJavaAssemblyException assembled;

    FlowableOnAssemblyScalarCallable(Publisher<T> source) {
        this.source = source;
        this.assembled = new RxJavaAssemblyException();
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            source.subscribe(new OnAssemblyConditionalSubscriber<T>((ConditionalSubscriber<? super T>)s, assembled));
        } else {
            source.subscribe(new OnAssemblySubscriber<T>(s, assembled));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T call() {
        return ((ScalarCallable<T>)source).call();
    }
}
