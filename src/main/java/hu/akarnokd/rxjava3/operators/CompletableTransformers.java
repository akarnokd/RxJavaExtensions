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

import java.util.Objects;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;

/**
 * Additional operators in the form of {@link CompletableTransformer},
 * use {@link Completable#compose(CompletableTransformer)}
 * to apply the operators to an existing sequence.
 *
 * @see Completables
 * @since 0.20.2
 */
public final class CompletableTransformers {

    /** Utility class. */
    private CompletableTransformers() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Maps the terminal signals of the upstream into {@link CompletableSource}s and
     * subscribes to it, relaying its terminal events to the downstream.
     * @param onCompleteHandler called when the upstream completes normally and should return
     * the CompletableSource to continue with.
     * @param onErrorHandler called when the upstream fails and should return the
     * CompletableSource for the given Throwable instance to continue with.
     * @return the new CompletableTransformer instance
     * @since 0.20.2
     */
    public static CompletableTransformer flatMap(
            Supplier<? extends CompletableSource> onCompleteHandler,
            Function<? super Throwable, ? extends CompletableSource> onErrorHandler) {
        Objects.requireNonNull(onCompleteHandler, "onCompleteHandler is null");
        Objects.requireNonNull(onErrorHandler, "onErrorHandler is null");
        return new CompletableFlatMapSignalCompletable(null, onCompleteHandler, onErrorHandler);
    }
}
