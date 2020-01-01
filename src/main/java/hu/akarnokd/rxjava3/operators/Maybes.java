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

import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;

/**
 * Additional operators in the form of {@link MaybeConverter}s,
 * use {@link Maybe#to(MaybeConverter)}
 * to apply the operators to an existing sequence.
 * 
 * @see MaybeTransformers
 * @since 0.20.2
 */
public final class Maybes {

    /** Utility class. */
    private Maybes() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Maps the terminal signals of the upstream into {@link CompletableSource}s and
     * subscribes to it, relaying its terminal events to the downstream.
     * @param <T> the upstream success value type
     * @param onSuccessHandler a function called with the upstream success value and should
     * return a CompletableSource to continue with.
     * @param onErrorHandler called when the upstream fails and should return the
     * CompletableSource for the given Throwable instance to continue with.
     * @param onCompleteHandler called when the upstream completes normally and should return
     * the CompletableSource to continue with.
     * @return the new MaybeConverter instance
     * @since 0.20.2
     */
    public static <T> MaybeConverter<T, Completable> flatMapCompletable(
            Function<? super T, ? extends CompletableSource> onSuccessHandler,
            Function<? super Throwable, ? extends CompletableSource> onErrorHandler,
            Supplier<? extends CompletableSource> onCompleteHandler) {
        Objects.requireNonNull(onSuccessHandler, "onSuccessHandler is null");
        Objects.requireNonNull(onErrorHandler, "onErrorHandler is null");
        Objects.requireNonNull(onCompleteHandler, "onCompleteHandler is null");
        return new MaybeFlatMapSignalCompletable<>(null, onSuccessHandler, onErrorHandler, onCompleteHandler);
    }

    /**
     * Maps the terminal signals of the upstream into {@link SingleSource}s and
     * subscribes to it, relaying its terminal events to the downstream.
     * @param <T> the upstream success value type
     * @param <R> the element type of the mapped-in SingleSource
     * @param onSuccessHandler a function called with the upstream success value and should
     * return a SingleSource to continue with.
     * @param onErrorHandler called when the upstream fails and should return the
     * SingleSource for the given Throwable instance to continue with.
     * @param onCompleteHandler called when the upstream completes normally and should return
     * the SingleSource to continue with.
     * @return the new MaybeConverter instance
     * @since 0.20.2
     */
    public static <T, R> MaybeConverter<T, Single<R>> flatMapSingle(
            Function<? super T, ? extends SingleSource<? extends R>> onSuccessHandler,
            Function<? super Throwable, ? extends SingleSource<? extends R>> onErrorHandler,
                    Supplier<? extends SingleSource<? extends R>> onCompleteHandler) {
        Objects.requireNonNull(onSuccessHandler, "onSuccessHandler is null");
        Objects.requireNonNull(onErrorHandler, "onErrorHandler is null");
        Objects.requireNonNull(onCompleteHandler, "onCompleteHandler is null");
        return new MaybeFlatMapSignalSingle<>(null, onSuccessHandler, onErrorHandler, onCompleteHandler);
    }

    /**
     * Maps the terminal signals of the upstream into {@link ObservableSource}s and
     * subscribes to it, relaying its terminal events to the downstream.
     * @param <T> the upstream success value type
     * @param <R> the element type of the mapped-in ObservableSource
     * @param onSuccessHandler a function called with the upstream success value and should
     * return a ObservableSource to continue with.
     * @param onErrorHandler called when the upstream fails and should return the
     * ObservableSource for the given Throwable instance to continue with.
     * @param onCompleteHandler called when the upstream completes normally and should return
     * the ObservableSource to continue with.
     * @return the new MaybeConverter instance
     * @since 0.20.2
     */
    public static <T, R> MaybeConverter<T, Observable<R>> flatMapObservable(
            Function<? super T, ? extends ObservableSource<? extends R>> onSuccessHandler,
            Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorHandler,
                    Supplier<? extends ObservableSource<? extends R>> onCompleteHandler) {
        Objects.requireNonNull(onSuccessHandler, "onSuccessHandler is null");
        Objects.requireNonNull(onErrorHandler, "onErrorHandler is null");
        Objects.requireNonNull(onCompleteHandler, "onCompleteHandler is null");
        return new MaybeFlatMapSignalObservable<>(null, onSuccessHandler, onErrorHandler, onCompleteHandler);
    }

    /**
     * Maps the terminal signals of the upstream into {@link Publisher}s and
     * subscribes to it, relaying its terminal events to the downstream.
     * @param <T> the upstream success value type
     * @param <R> the element type of the mapped-in Publisher
     * @param onSuccessHandler a function called with the upstream success value and should
     * return a Publisher to continue with.
     * @param onErrorHandler called when the upstream fails and should return the
     * Publisher for the given Throwable instance to continue with.
     * @param onCompleteHandler called when the upstream completes normally and should return
     * the Publisher to continue with.
     * @return the new MaybeConverter instance
     * @since 0.20.2
     */
    public static <T, R> MaybeConverter<T, Flowable<R>> flatMapFlowable(
            Function<? super T, ? extends Publisher<? extends R>> onSuccessHandler,
            Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler,
                    Supplier<? extends Publisher<? extends R>> onCompleteHandler) {
        Objects.requireNonNull(onSuccessHandler, "onSuccessHandler is null");
        Objects.requireNonNull(onErrorHandler, "onErrorHandler is null");
        Objects.requireNonNull(onCompleteHandler, "onCompleteHandler is null");
        return new MaybeFlatMapSignalFlowable<>(null, onSuccessHandler, onErrorHandler, onCompleteHandler);
    }
}
