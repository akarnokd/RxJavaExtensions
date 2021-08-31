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

import java.util.Objects;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.operators.QueueSubscription;
import io.reactivex.rxjava3.internal.subscribers.BasicFuseableSubscriber;

/**
 * Maps the error from upstream into another Throwable.
 *
 * @param <T> the value type
 */
final class SoloMapError<T> extends Solo<T> {

    final Solo<T> source;

    final Function<? super Throwable, ? extends Throwable> errorMapper;

    SoloMapError(Solo<T> source, Function<? super Throwable, ? extends Throwable> errorMapper) {
        this.source = source;
        this.errorMapper = errorMapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new MapErrorSubscriber<T>(s, errorMapper));
    }

    static final class MapErrorSubscriber<T> extends BasicFuseableSubscriber<T, T> {

        final Function<? super Throwable, ? extends Throwable> errorMapper;

        MapErrorSubscriber(Subscriber<? super T> downstream,
                Function<? super Throwable, ? extends Throwable> errorMapper) {
            super(downstream);
            this.errorMapper = errorMapper;
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            Throwable ex;

            try {
                ex = Objects.requireNonNull(errorMapper.apply(t), "The errorMapper returned a null Throwable");
            } catch (Throwable exc) {
                Exceptions.throwIfFatal(exc);
                ex = new CompositeException(t, exc);
            }

            super.onError(ex);
        }

        @Override
        public int requestFusion(int mode) {
            QueueSubscription<T> qs = this.qs;
            return qs != null ? qs.requestFusion(mode) : NONE;
        }

        @Override
        public T poll() throws Throwable {
            return qs.poll();
        }
    }
}
