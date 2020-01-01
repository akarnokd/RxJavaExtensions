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

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.subscribers.BasicFuseableSubscriber;

/**
 * Map the success value to another value.
 *
 * @param <T> the source value type
 * @param <R> the result value type
 */
final class SoloMap<T, R> extends Solo<R> {

    final Solo<T> source;

    final Function<? super T, ? extends R> mapper;

    SoloMap(Solo<T> source, Function<? super T, ? extends R> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new MapSubscriber<T, R>(s, mapper));
    }

    static final class MapSubscriber<T, R> extends BasicFuseableSubscriber<T, R> {

        final Function<? super T, ? extends R> mapper;

        MapSubscriber(Subscriber<? super R> downstream, Function<? super T, ? extends R> mapper) {
            super(downstream);
            this.mapper = mapper;
        }

        @Override
        public void onNext(T t) {
            if (!done) {
                if (sourceMode == NONE) {
                    R v;

                    try {
                        v = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null value");
                    } catch (Throwable ex) {
                        fail(ex);
                        return;
                    }

                    downstream.onNext(v);
                } else {
                    downstream.onNext(null);
                }
            }
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Override
        public R poll() throws Throwable {
            T v = qs.poll();
            if (v != null) {
                return Objects.requireNonNull(mapper.apply(v), "The mapper returned a null value");
            }
            return null;
        }
    }
}
