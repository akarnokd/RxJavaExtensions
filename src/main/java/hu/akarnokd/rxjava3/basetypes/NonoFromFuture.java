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

import java.util.concurrent.*;

import org.reactivestreams.Subscriber;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Supplier;
import io.reactivex.internal.subscriptions.EmptySubscription;

/**
 * Block until the Future completes or times out.
 */
final class NonoFromFuture extends Nono implements Supplier<Void> {

    final Future<?> future;

    final long timeout;

    final TimeUnit unit;

    NonoFromFuture(Future<?> future, long timeout, TimeUnit unit) {
        this.future = future;
        this.timeout = timeout;
        this.unit = unit;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        s.onSubscribe(EmptySubscription.INSTANCE);
        try {
            if (timeout <= 0L) {
                future.get();
            } else {
                future.get(timeout, unit);
            }
        } catch (ExecutionException ex) {
            s.onError(ex.getCause());
            return;
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            s.onError(ex);
            return;
        }

        s.onComplete();
    }

    @Override
    public Void get() throws Throwable {
        if (timeout > 0L) {
            future.get(timeout, unit);
        } else {
            future.get();
        }
        return null;
    }
}
