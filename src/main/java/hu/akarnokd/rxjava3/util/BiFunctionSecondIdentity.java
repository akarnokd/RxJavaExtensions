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

package hu.akarnokd.rxjava3.util;

import io.reactivex.functions.BiFunction;

/**
 * A BiFunction that returns its second parameter; use {@link #instance()} to
 * get a correctly typed (shared) instance.
 *
 * @since 0.16.2
 */
public enum BiFunctionSecondIdentity implements BiFunction<Object, Object, Object> {
    /** The singleton instance. */
    INSTANCE;

    /**
     * Returns a correctly typed instance of the enum's singleton instance.
     * @param <T> the first input value type.
     * @param <U> the second input and result type
     * @return the typed BiFunction (shared) instance.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T, U> BiFunction<T, U, U> instance() {
        return (BiFunction)INSTANCE;
    }

    @Override
    public Object apply(Object t1, Object t2) throws Exception {
        return t2;
    }
}
