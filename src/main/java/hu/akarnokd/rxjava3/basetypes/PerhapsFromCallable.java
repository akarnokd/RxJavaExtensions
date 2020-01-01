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

import java.util.Objects;
import java.util.concurrent.Callable;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.internal.subscriptions.DeferredScalarSubscription;

/**
 * Executes a callable and signals its resulting value.
 *
 * @param <T> the value type
 */
final class PerhapsFromCallable<T> extends Perhaps<T> implements Callable<T> {

    final Callable<T> callable;

    PerhapsFromCallable(Callable<T> callable) {
        this.callable = callable;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        DeferredScalarSubscription<T> dss = new DeferredScalarSubscription<>(s);
        s.onSubscribe(dss);

        T v;

        try {
            v = call();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            s.onError(ex);
            return;
        }

        if (v != null) {
            dss.complete(v);
        } else {
            s.onComplete();
        }
    }

    @Override
    public T call() throws Exception {
        return Objects.requireNonNull(callable.call(), "The callable returned a null value");
    }
}
