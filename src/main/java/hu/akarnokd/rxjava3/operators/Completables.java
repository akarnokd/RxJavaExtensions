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

package hu.akarnokd.rxjava3.operators;

import java.util.Objects;

import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;

/**
 * Additional operators in the form of {@link CompletableConverter}s,
 * use {@link Completable#to(CompletableConverter)}
 * to apply the operators to an existing sequence.
 * 
 * @see CompletableTransformers
 * @since 0.20.2
 */
public final class Completables {

    /** Utility class. */
    private Completables() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Maps the terminal signals of the upstream into {@link MaybeSource}s and
     * subscribes to it, relaying its terminal events to the downstream.
     * @param <R> the element type of the mapped-in MaybeSource
     * @param onCompleteHandler called when the upstream completes normally and should return
     * the MaybeSource to continue with.
     * @param onErrorHandler called when the upstream fails and should return the
     * MaybeSource for the given Throwable instance to continue with.
     * @return the new CompletableConverter instance
     * @since 0.20.2
     */
    public static <R> CompletableConverter<Maybe<R>> flatMapMaybe(
            Supplier<? extends MaybeSource<? extends R>> onCompleteHandler,
            Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorHandler) {
        Objects.requireNonNull(onCompleteHandler, "onCompleteHandler is null");
        Objects.requireNonNull(onErrorHandler, "onErrorHandler is null");
        return new CompletableFlatMapSignalMaybe<>(null, onCompleteHandler, onErrorHandler);
    }

    /**
     * Maps the terminal signals of the upstream into {@link SingleSource}s and
     * subscribes to it, relaying its terminal events to the downstream.
     * @param <R> the element type of the mapped-in SingleSource
     * @param onCompleteHandler called when the upstream completes normally and should return
     * the SingleSource to continue with.
     * @param onErrorHandler called when the upstream fails and should return the
     * SingleSource for the given Throwable instance to continue with.
     * @return the new CompletableConverter instance
     * @since 0.20.2
     */
    public static <R> CompletableConverter<Single<R>> flatMapSingle(
            Supplier<? extends SingleSource<? extends R>> onCompleteHandler,
            Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorHandler) {
        Objects.requireNonNull(onCompleteHandler, "onCompleteHandler is null");
        Objects.requireNonNull(onErrorHandler, "onErrorHandler is null");
        return new CompletableFlatMapSignalSingle<>(null, onCompleteHandler, onErrorHandler);
    }

    /**
     * Maps the terminal signals of the upstream into {@link ObservableSource}s and
     * subscribes to it, relaying its terminal events to the downstream.
     * @param <R> the element type of the mapped-in ObservableSource
     * @param onCompleteHandler called when the upstream completes normally and should return
     * the SingleSource to continue with.
     * @param onErrorHandler called when the upstream fails and should return the
     * ObservableSource for the given Throwable instance to continue with.
     * @return the new CompletableConverter instance
     * @since 0.20.2
     */
    public static <R> CompletableConverter<Observable<R>> flatMapObservable(
            Supplier<? extends ObservableSource<? extends R>> onCompleteHandler,
            Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorHandler) {
        Objects.requireNonNull(onCompleteHandler, "onCompleteHandler is null");
        Objects.requireNonNull(onErrorHandler, "onErrorHandler is null");
        return new CompletableFlatMapSignalObservable<>(null, onCompleteHandler, onErrorHandler);
    }

    /**
     * Maps the terminal signals of the upstream into {@link Publisher}s and
     * subscribes to it, relaying its terminal events to the downstream.
     * @param <R> the element type of the mapped-in ObservableSource
     * @param onCompleteHandler called when the upstream completes normally and should return
     * the Publisher to continue with.
     * @param onErrorHandler called when the upstream fails and should return the
     * Publisher for the given Throwable instance to continue with.
     * @return the new CompletableConverter instance
     * @since 0.20.2
     */
    public static <R> CompletableConverter<Flowable<R>> flatMapFlowable(
            Supplier<? extends Publisher<? extends R>> onCompleteHandler,
            Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler) {
        Objects.requireNonNull(onCompleteHandler, "onCompleteHandler is null");
        Objects.requireNonNull(onErrorHandler, "onErrorHandler is null");
        return new CompletableFlatMapSignalFlowable<>(null, onCompleteHandler, onErrorHandler);
    }
}
