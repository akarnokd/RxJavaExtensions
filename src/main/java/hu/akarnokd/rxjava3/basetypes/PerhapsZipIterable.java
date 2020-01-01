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

import java.util.Arrays;

import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava3.basetypes.PerhapsZipArray.ZipCoordinator;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;

/**
 * Waits till all sources produce a value and calls a function
 * to generate the only resulting value or terminates if
 * one of the sources terminates without value.
 *
 * @param <T> the shared base source value type
 * @param <R> the result value type
 */
final class PerhapsZipIterable<T, R> extends Perhaps<R> {

    final Iterable<? extends Perhaps<? extends T>> sources;

    final Function<? super Object[], ? extends R> zipper;

    PerhapsZipIterable(Iterable<? extends Perhaps<? extends T>> sources, Function<? super Object[], ? extends R> zipper) {
        this.sources = sources;
        this.zipper = zipper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        @SuppressWarnings("unchecked")
        Perhaps<? extends T>[] srcs = new Perhaps[8];
        int n = 0;

        try {
            for (Perhaps<? extends T> ph : sources) {
                if (srcs.length == n) {
                    srcs = Arrays.copyOf(srcs, n + (n >> 1));
                }
                srcs[n++] = ph;
            }
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        ZipCoordinator<T, R> parent = new ZipCoordinator<>(s, zipper, n);
        s.onSubscribe(parent);

        parent.subscribe(srcs, n);
    }
}
