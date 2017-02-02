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

import java.util.Arrays;

import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava2.basetypes.SoloMap.MapSubscriber;
import hu.akarnokd.rxjava2.basetypes.SoloZipArray.ZipCoordinator;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.EmptySubscription;

/**
 * Combines the solo values of all the sources via a zipper function into a
 * single resulting value.
 * @param <T> the common input base type
 * @param <R> the result type
 */
final class SoloZipIterable<T, R> extends Solo<R> implements Function<T, R> {

    final Iterable<? extends Solo<? extends T>> sources;

    final Function<? super Object[], ? extends R> zipper;

    SoloZipIterable(Iterable<? extends Solo<? extends T>> sources, Function<? super Object[], ? extends R> zipper) {
        this.sources = sources;
        this.zipper = zipper;
    }

    @Override
    public R apply(T t) throws Exception {
        return zipper.apply(new Object[] { t });
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        int n = 0;
        @SuppressWarnings("unchecked")
        Solo<? extends T>[] array = new Solo[8];

        try {
            for (Solo<? extends T> inner : sources) {
                if (n == array.length) {
                    array = Arrays.copyOf(array, n + (n >> 1));
                }
                array[n++] = ObjectHelper.requireNonNull(inner, "One of the source Solo is null");
            }
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        if (n == 0) {
            EmptySubscription.complete(s);
            return;
        } else
        if (n == 1) {
            array[0].subscribe(new MapSubscriber<T, R>(s, this));
            return;
        }

        ZipCoordinator<T, R> parent = new ZipCoordinator<T, R>(s, zipper, n);
        s.onSubscribe(parent);

        parent.subscribe(array, n);
    }

}
