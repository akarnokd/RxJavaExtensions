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

import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.ObjectHelper;

/**
 * Additional operators in the form of {@link SingleConverter}s,
 * use {@link Single#to(SingleConverter)}
 * to apply the operators to an existing sequence.
 * 
 * @see SingleTransformers
 * @since 0.20.2
 */
public final class Singles {

    /** Utility class. */
    private Singles() {
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
     * @return the new SingleConverter instance
     * @since 0.20.2
     */
    public static <T> SingleConverter<T, Completable> flatMapCompletable(
            Function<? super T, ? extends CompletableSource> onSuccessHandler,
            Function<? super Throwable, ? extends CompletableSource> onErrorHandler) {
        ObjectHelper.requireNonNull(onSuccessHandler, "onSuccessHandler is null");
        ObjectHelper.requireNonNull(onErrorHandler, "onErrorHandler is null");
        return new SingleFlatMapSignalCompletable<T>(null, onSuccessHandler, onErrorHandler);
    }

    /**
     * Maps the terminal signals of the upstream into {@link MaybeSource}s and
     * subscribes to it, relaying its terminal events to the downstream.
     * @param <T> the upstream success value type
     * @param <R> the element type of the mapped-in MaybeSource
     * @param onSuccessHandler a function called with the upstream success value and should
     * return a MaybeSource to continue with.
     * @param onErrorHandler called when the upstream fails and should return the
     * MaybeSource for the given Throwable instance to continue with.
     * @return the new SingleConverter instance
     * @since 0.20.2
     */
    public static <T, R> SingleConverter<T, Maybe<R>> flatMapMaybe(
            Function<? super T, ? extends MaybeSource<? extends R>> onSuccessHandler,
            Function<? super Throwable, ? extends MaybeSource<? extends R>> onErrorHandler) {
        ObjectHelper.requireNonNull(onSuccessHandler, "onSuccessHandler is null");
        ObjectHelper.requireNonNull(onErrorHandler, "onErrorHandler is null");
        return new SingleFlatMapSignalMaybe<T, R>(null, onSuccessHandler, onErrorHandler);
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
     * @return the new SingleConverter instance
     * @since 0.20.2
     */
    public static <T, R> SingleConverter<T, Observable<R>> flatMapObservable(
            Function<? super T, ? extends ObservableSource<? extends R>> onSuccessHandler,
            Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorHandler) {
        ObjectHelper.requireNonNull(onSuccessHandler, "onSuccessHandler is null");
        ObjectHelper.requireNonNull(onErrorHandler, "onErrorHandler is null");
        return new SingleFlatMapSignalObservable<T, R>(null, onSuccessHandler, onErrorHandler);
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
     * @return the new SingleConverter instance
     * @since 0.20.2
     */
    public static <T, R> SingleConverter<T, Flowable<R>> flatMapFlowable(
            Function<? super T, ? extends Publisher<? extends R>> onSuccessHandler,
            Function<? super Throwable, ? extends Publisher<? extends R>> onErrorHandler) {
        ObjectHelper.requireNonNull(onSuccessHandler, "onSuccessHandler is null");
        ObjectHelper.requireNonNull(onErrorHandler, "onErrorHandler is null");
        return new SingleFlatMapSignalFlowable<T, R>(null, onSuccessHandler, onErrorHandler);
    }
}
