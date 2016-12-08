/*
 * Copyright 2016 David Karnok
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

package hu.akarnokd.rxjava2.basetypes;

import java.util.concurrent.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.schedulers.Schedulers;

/**
 * Base class with fluent API for supporting a Publisher with
 * exactly 1 element or an error.
 *
 * @param <T> the value type
 */
public abstract class Solo<T> implements Publisher<T> {

    @SuppressWarnings("rawtypes")
    private static volatile Function<Solo, Solo> onAssembly;

    /**
     * Returns the current onAssembly handler.
     * @param <T> the target value type
     * @return the current handler, maybe null
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Function<Solo<T>, Solo<T>> getOnAssemblyHandler() {
        return (Function)onAssembly;
    }

    /**
     * Set the onAssembly handler.
     * @param <T> the target value type
     * @param handler the handler, null clears the handler
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> void setOnAssemblyHandler(Function<Solo<T>, Solo<T>> handler) {
        onAssembly = (Function)handler;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected static <T> Solo<T> onAssembly(Solo<T> source) {
        Function<Solo, Solo> f = onAssembly;
        if (f == null) {
            return source;
        }
        try {
            return f.apply(source);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    // ----------------------------------------------------
    // Factory methods (enter)
    // ----------------------------------------------------

    public static <T> Solo<T> just(T item) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Solo<T> error(Throwable error) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Solo<T> error(Callable<? extends Throwable> errorSupplier) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Solo<T> fromCallable(Callable<? extends T> callable) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Solo<T> never() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Solo<T> defer(Callable<? extends Solo<T>> supplier) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Solo<T> fromPublisher(Publisher<T> source) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Solo<T> fromSingle(Single<T> source) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Solo<T> amb(Iterable<? extends Solo<? extends T>> sources) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Solo<T> ambArray(Solo<T>... sources) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Flowable<T> concat(Iterable<? extends Solo<? extends T>> sources) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Flowable<T> concat(Publisher<? extends Solo<? extends T>> sources) {
        return concat(sources, 2);
    }

    public static <T> Flowable<T> concat(Publisher<? extends Solo<? extends T>> sources, int prefetch) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Flowable<T> concatArray(Solo<? extends T>... sources) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Flowable<T> concatDelayError(Iterable<? extends Solo<? extends T>> sources) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Flowable<T> concatDelayError(Publisher<? extends Solo<? extends T>> sources) {
        return concatDelayError(sources, 2, true);
    }

    public static <T> Flowable<T> concatDelayError(Publisher<? extends Solo<? extends T>> sources, int prefetch) {
        return concatDelayError(sources, prefetch, true);
    }

    public static <T> Flowable<T> concatDelayError(Publisher<? extends Solo<? extends T>> sources, int prefetch, boolean tillTheEnd) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Flowable<T> concatArrayDelayError(Solo<T>... sources) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Flowable<T> merge(Iterable<? extends Solo<? extends T>> sources) {
        return merge(sources, Integer.MAX_VALUE);
    }

    public static <T> Flowable<T> merge(Iterable<? extends Solo<? extends T>> sources, int maxConcurrency) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Flowable<T> merge(Publisher<? extends Solo<? extends T>> sources) {
        return merge(sources, Integer.MAX_VALUE);
    }

    public static <T> Flowable<T> merge(Publisher<? extends Solo<? extends T>> sources, int maxConcurrency) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Flowable<T> mergeArray(Solo<? extends T>... sources) {
        return mergeArray(Integer.MAX_VALUE, sources);
    }

    public static <T> Flowable<T> mergeArray(int maxConcurrency, Solo<? extends T>... sources) {
        // TODO implement
        throw new UnsupportedOperationException();
    }


    public static <T> Flowable<T> mergeDelayError(Iterable<? extends Solo<? extends T>> sources) {
        return mergeDelayError(sources, Integer.MAX_VALUE);
    }

    public static <T> Flowable<T> mergeDelayError(Iterable<? extends Solo<? extends T>> sources, int maxConcurrency) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Flowable<T> mergeDelayError(Publisher<? extends Solo<? extends T>> sources) {
        return mergeDelayError(sources, Integer.MAX_VALUE);
    }

    public static <T> Flowable<T> mergeDelayError(Publisher<? extends Solo<? extends T>> sources, int maxConcurrency) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Flowable<T> mergeArrayDelayError(Solo<? extends T>... sources) {
        return mergeArrayDelayError(Integer.MAX_VALUE, sources);
    }

    public static <T> Flowable<T> mergeArrayDelayError(int maxConcurrency, Solo<? extends T>... sources) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static Solo<Long> timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }

    public static Solo<Long> timer(long delay, TimeUnit unit, Scheduler scheduler) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    // ----------------------------------------------------
    // Instance operators (stay)
    // ----------------------------------------------------

    // ----------------------------------------------------
    // Consumers (leave)
    // ----------------------------------------------------

    @Override
    public final void subscribe(Subscriber<? super T> s) {
        ObjectHelper.requireNonNull(s, "s is null");
        try {
            subscribeActual(s);
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            NullPointerException npe = new NullPointerException();
            npe.initCause(ex);
            throw npe;
        }
    }

    /**
     * Implement this method to react to a Subscriber subscribing to this Solo.
     * @param s the downstream Subscriber, never null
     */
    protected abstract void subscribeActual(Subscriber<? super T> s);

    public final <E extends Subscriber<? super T>> E subscribeWith(E s) {
        subscribe(s);
        return s;
    }

    public final Disposable subscribe() {
        return subscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    public final Disposable subscribe(Consumer<? super T> onNext) {
        return subscribe(onNext, Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    public final Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError) {
        return subscribe(onNext, onError, Functions.EMPTY_ACTION);
    }

    public final Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Action onComplete) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final void blockingSubscribe() {
        blockingSubscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    public final void blockingSubscribe(Consumer<? super T> onNext) {
        blockingSubscribe(onNext, Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    public final void blockingSubscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError) {
        blockingSubscribe(onNext, onError, Functions.EMPTY_ACTION);
    }

    public final void blockingSubscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Action onComplete) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final T blockingGet() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final T blockingGet(long timeout, TimeUnit unit) {
        // TODO implement
        throw new UnsupportedOperationException();
    }
}
