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

package hu.akarnokd.rxjava2.operators;

import java.util.concurrent.Callable;

import io.reactivex.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Additional operators in the form of {@link MaybeTransformer},
 * use {@link Maybe#compose(MaybeTransformer)}
 * to apply the operators to an existing sequence.
 * 
 * @see Maybes
 * @since 0.20.2
 */
public final class MaybeTransformers {

    /** Utility class. */
    private MaybeTransformers() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Maps the terminal signals of the upstream into {@link MaybeSource}s and
     * subscribes to it, relaying its terminal events to the downstream.
     * @param <T> the success type of the upstream
     * @param <R> the success type of the resulting Maybe
     * @param onSuccessHandler a function called with the upstream success value and should
     * return a MaybeSource to continue with.
     * @param onCompleteHandler called when the upstream completes normally and should return
     * the MaybeSource to continue with.
     * @param onErrorHandler called when the upstream fails and should return the
     * MaybeSource for the given Throwable instance to continue with.
     * @return the new MaybeTransformer instance
     * @since 0.20.2
     */
    public static <T, R> MaybeTransformer<T, R> flatMap(
            Function<? super T, ? extends MaybeSource<? extends R>> onSuccessHandler,
            Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorHandler,
            Callable<? extends MaybeSource<? extends R>> onCompleteHandler) {
        ObjectHelper.requireNonNull(onSuccessHandler, "onSuccessHandler is null");
        ObjectHelper.requireNonNull(onErrorHandler, "onErrorHandler is null");
        ObjectHelper.requireNonNull(onCompleteHandler, "onCompleteHandler is null");
        return new MaybeFlatMapSignalMaybe<T, R>(null, onSuccessHandler, onErrorHandler, onCompleteHandler);
    }
}
