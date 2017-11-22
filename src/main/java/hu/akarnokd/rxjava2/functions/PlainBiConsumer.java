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

package hu.akarnokd.rxjava2.functions;

import io.reactivex.functions.BiConsumer;

/**
 * A {@link BiConsumer} with suppressed exception on its
 * {@link #accept(Object, Object)} method.
 *
 * @param <T1> the first argument type
 * @param <T2> the second argument type
 * @since 0.18.0
 */
public interface PlainBiConsumer<T1, T2>
extends BiConsumer<T1, T2> {

    @Override
    void accept(T1 t1, T2 t2);
}
