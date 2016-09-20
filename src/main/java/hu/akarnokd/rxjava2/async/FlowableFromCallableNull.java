/*
 * Copyright 2016 David Karnok
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

package hu.akarnokd.rxjava2.async;

import java.util.concurrent.Callable;

import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.subscriptions.DeferredScalarSubscription;

/**
 * Runs a callable and treats a null result as completion.
 *
 * @param <T> the value type
 */
final class FlowableFromCallableNull<T> extends Flowable<T> implements Callable<T> {

    final Callable<? extends T> callable;

    FlowableFromCallableNull(Callable<? extends T> callable) {
        this.callable = callable;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        CallableNullSubscription<T> deferred = new CallableNullSubscription<T>(s);
        s.onSubscribe(deferred);

        if (!deferred.isCancelled()) {

            T v;

            try {
                v = callable.call();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                if (!deferred.isCancelled()) {
                    s.onError(ex);
                }
                return;
            }

            if (!deferred.isCancelled()) {
                if (v == null) {
                    s.onComplete();
                } else {
                    deferred.complete(v);
                }
            }
        }
    }

    @Override
    public T call() throws Exception {
        return callable.call();
    }

    static final class CallableNullSubscription<T> extends DeferredScalarSubscription<T> {

        private static final long serialVersionUID = -7088349936918117528L;

        CallableNullSubscription(Subscriber<? super T> actual) {
            super(actual);
        }

    }
}
