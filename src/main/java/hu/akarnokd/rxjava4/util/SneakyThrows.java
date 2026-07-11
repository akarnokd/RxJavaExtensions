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

package hu.akarnokd.rxjava4.util;

/**
 * Utility class to throw arbitrary {@link Throwable}s.
 */
public final class SneakyThrows {

    private SneakyThrows() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Sneakily throw.
     * @param <E> the throwable subclass
     * @param error the error to throw
     * @return never
     * @throws E the exception type
     */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> E justThrowX(Throwable error) throws E {
        throw (E)error;
    }

}
