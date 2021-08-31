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

package hu.akarnokd.rxjava3.string;

import java.util.Objects;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.internal.functions.ObjectHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Utility class for String operations with {@link Flowable}s.
 */
public final class StringFlowable {
    /** Utility class. */
    private StringFlowable() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Signals each character of the given string CharSequence as Integers.
     * @param string the source of characters
     * @return the new Flowable instance
     */
    public static Flowable<Integer> characters(CharSequence string) {
        Objects.requireNonNull(string, "string is null");
        return RxJavaPlugins.onAssembly(new FlowableCharSequence(string));
    }

    /**
     * Splits the input sequence of strings based on a pattern even across subsequent
     * elements if needed.
     * @param pattern the Rexexp pattern to split along
     * @return the new FlowableTransformer instance
     *
     * @since 0.13.0
     */
    public static FlowableTransformer<String, String> split(Pattern pattern) {
        return split(pattern, Flowable.bufferSize());
    }

    /**
     * Splits the input sequence of strings based on a pattern even across subsequent
     * elements if needed.
     * @param pattern the Rexexp pattern to split along
     * @param bufferSize the number of items to prefetch from the upstream
     * @return the new FlowableTransformer instance
     *
     * @since 0.13.0
     */
    public static FlowableTransformer<String, String> split(Pattern pattern, int bufferSize) {
        Objects.requireNonNull(pattern, "pattern is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return new FlowableSplit(null, pattern, bufferSize);
    }

    /**
     * Splits the input sequence of strings based on a pattern even across subsequent
     * elements if needed.
     * @param pattern the Rexexp pattern to split along
     * @return the new FlowableTransformer instance
     *
     * @since 0.13.0
     */
    public static FlowableTransformer<String, String> split(String pattern) {
        return split(pattern, Flowable.bufferSize());
    }

    /**
     * Splits the input sequence of strings based on a pattern even across subsequent
     * elements if needed.
     * @param pattern the Rexexp pattern to split along
     * @param bufferSize the number of items to prefetch from the upstream
     * @return the new FlowableTransformer instance
     *
     * @since 0.13.0
     */
    public static FlowableTransformer<String, String> split(String pattern, int bufferSize) {
        return split(Pattern.compile(pattern), bufferSize);
    }

}
