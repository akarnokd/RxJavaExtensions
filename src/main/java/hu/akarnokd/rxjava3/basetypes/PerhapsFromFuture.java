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

import java.util.concurrent.*;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.internal.subscriptions.DeferredScalarSubscription;

/**
 * Await a Future's result.
 *
 * @param <T> the value type
 *
 * @since 0.14.0
 */
final class PerhapsFromFuture<T> extends Perhaps<T> {

    final Future<? extends T> future;

    final long timeout;

    final TimeUnit unit;

    PerhapsFromFuture(Future<? extends T> future, long timeout, TimeUnit unit) {
        this.future = future;
        this.timeout = timeout;
        this.unit = unit;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        DeferredScalarSubscription<T> dss = new DeferredScalarSubscription<>(s);
        s.onSubscribe(dss);

        T v;

        try {
            if (timeout <= 0L) {
                v = future.get();
            } else {
                v = future.get(timeout, unit);
            }
        } catch (InterruptedException ex) {
            s.onError(ex);
            return;
        } catch (ExecutionException ex) {
            s.onError(ex.getCause());
            return;
        } catch (TimeoutException ex) {
            s.onError(ex);
            return;
        }

        if (v != null) {
            dss.complete(v);
        } else {
            s.onComplete();
        }
    }

}
