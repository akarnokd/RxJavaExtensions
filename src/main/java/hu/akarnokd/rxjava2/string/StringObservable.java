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

package hu.akarnokd.rxjava2.string;

import io.reactivex.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Utility class for String operations with {@link Observable}s.
 */
public final class StringObservable {
    /** Utility class. */
    private StringObservable() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Signals each character of the given string CharSequence as Integers.
     * @param string the source of characters
     * @return the new Observable instance
     */
    public static Observable<Integer> characters(CharSequence string) {
        return RxJavaPlugins.onAssembly(new ObservableCharSequence(string));
    }
}
