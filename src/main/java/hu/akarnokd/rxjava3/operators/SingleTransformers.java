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

package hu.akarnokd.rxjava3.operators;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.ObjectHelper;

/**
 * Additional operators in the form of {@link SingleTransformer},
 * use {@link Single#compose(SingleTransformer)}
 * to apply the operators to an existing sequence.
 * 
 * @see Singles
 * @since 0.20.2
 */
public final class SingleTransformers {

    /** Utility class. */
    private SingleTransformers() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Maps the terminal signals of the upstream into {@link SingleSource}s and
     * subscribes to it, relaying its terminal events to the downstream.
     * @param <T> the success type of the upstream
     * @param <R> the success type of the resulting Maybe
     * @param onSuccessHandler a function called with the upstream success value and should
     * return a SingleSource to continue with.
     * @param onErrorHandler called when the upstream fails and should return the
     * SingleSource for the given Throwable instance to continue with.
     * @return the new SingleTransformer instance
     * @since 0.20.2
     */
    public static <T, R> SingleTransformer<T, R> flatMap(
            Function<? super T, ? extends SingleSource<? extends R>> onSuccessHandler,
            Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorHandler) {
        ObjectHelper.requireNonNull(onSuccessHandler, "onSuccessHandler is null");
        ObjectHelper.requireNonNull(onErrorHandler, "onErrorHandler is null");
        return new SingleFlatMapSignalSingle<T, R>(null, onSuccessHandler, onErrorHandler);
    }
}
