/*
 * Copyright 2016-2018 David Karnok
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

import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava2.basetypes.SoloMap.MapSubscriber;
import io.reactivex.functions.Function;

/**
 * Map the value of upstream via a function.
 *
 * @param <T> the source value type
 * @param <R> the result value type
 */
final class PerhapsMap<T, R> extends Perhaps<R> {

    final Perhaps<T> source;

    final Function<? super T, ? extends R> mapper;

    PerhapsMap(Perhaps<T> source, Function<? super T, ? extends R> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new MapSubscriber<T, R>(s, mapper));
    }
}
