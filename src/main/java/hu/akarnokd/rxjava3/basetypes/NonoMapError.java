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

import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.ObjectHelper;

/**
 * Map an upstream error into another Throwable.
 */
final class NonoMapError extends Nono {

    final Nono source;

    final Function<? super Throwable, ? extends Throwable> mapper;

    NonoMapError(Nono source, Function<? super Throwable, ? extends Throwable> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        source.subscribe(new MapErrorSubscriber(s, mapper));
    }

    static final class MapErrorSubscriber extends BasicNonoSubscriber {

        final Function<? super Throwable, ? extends Throwable> mapper;

        MapErrorSubscriber(Subscriber<? super Void> downstream, Function<? super Throwable, ? extends Throwable> mapper) {
            super(downstream);
            this.mapper = mapper;
        }

        @Override
        public void onError(Throwable t) {
            Throwable ex;
            try {
                ex = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null Throwable");
            } catch (Throwable exc) {
                Exceptions.throwIfFatal(exc);
                ex = new CompositeException(t, exc);
            }
            downstream.onError(ex);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

    }
}
