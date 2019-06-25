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

package hu.akarnokd.rxjava3.basetypes;

import java.util.concurrent.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.subscribers.LambdaSubscriber;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

/**
 * Base class with fluent API for supporting a Publisher with
 * exactly 1 element or an error.
 *
 * @param <T> the value type
 * 
 * @since 0.13.0
 */
public abstract class Solo<T> implements Publisher<T> {

    /**
     * Hook called when assembling Solo sequences.
     */
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

    /**
     * Create a Solo that for each incoming Subscriber calls a callback to
     * emit a sync or async events in a thread-safe, backpressure-aware and
     * cancellation-safe manner.
     * @param <T> the value type emitted
     * @param onCreate the callback called for each individual subscriber with an
     * abstraction of the incoming Subscriber.
     * @return th new Solo instance
     */
    public static <T> Solo<T> create(SingleOnSubscribe<T> onCreate) {
        ObjectHelper.requireNonNull(onCreate, "onCreate is null");
        return onAssembly(new SoloCreate<T>(onCreate));
    }

    /**
     * Returns a Solo that signals the given item and completes.
     * @param <T> the value type
     * @param item the item, not null
     * @return the new Solo instance
     */
    public static <T> Solo<T> just(T item) {
        ObjectHelper.requireNonNull(item, "item is null");
        return onAssembly(new SoloJust<T>(item));
    }

    /**
     * Returns a Solo that signals the given error to Subscribers.
     * @param <T> the value type
     * @param error the error to signal, not null
     * @return the new Solo instance
     */
    public static <T> Solo<T> error(Throwable error) {
        ObjectHelper.requireNonNull(error, "error is null");
        return onAssembly(new SoloError<T>(error));
    }

    /**
     * Returns a Solo that signals the error returned from
     * the errorSupplier to each individual Subscriber.
     * @param <T> the value type
     * @param errorSupplier the supplier called for each Subscriber to
     * return a Throwable to be signalled
     * @return the new Solo instance
     */
    public static <T> Solo<T> error(Supplier<? extends Throwable> errorSupplier) {
        ObjectHelper.requireNonNull(errorSupplier, "errorSupplier is null");
        return onAssembly(new SoloErrorSupplier<T>(errorSupplier));
    }

    /**
     * Returns a Solo that calls the callable and emits its value or error.
     * @param <T> the value type
     * @param callable the callable to call
     * @return the new Solo instance
     */
    public static <T> Solo<T> fromCallable(Callable<T> callable) {
        ObjectHelper.requireNonNull(callable, "callable is null");
        return onAssembly(new SoloFromCallable<T>(callable));
    }

    /**
     * Returns a Solo that never signals an item or terminal event.
     * @param <T> the value type
     * @return the new Solo instance
     */
    public static <T> Solo<T> never() {
        return onAssembly(SoloNever.<T>instance());
    }

    /**
     * Defers the creation of the actual Solo to the time when a Subscriber
     * subscribes to the returned Solo.
     * @param <T> the value type
     * @param supplier the supplier of the actual Solo
     * @return the new Solo instance
     */
    public static <T> Solo<T> defer(Supplier<? extends Solo<T>> supplier) {
        ObjectHelper.requireNonNull(supplier, "supplier is null");
        return onAssembly(new SoloDefer<T>(supplier));
    }

    /**
     * Wraps a Publisher into a Solo and signals its only value,
     * NoSuchElementException if empty or IndexOutOfBoundsException if it has
     * more than one element.
     * @param <T> the value type
     * @param source the source Publisher
     * @return the new Solo instance
     */
    public static <T> Solo<T> fromPublisher(Publisher<T> source) {
        if (source instanceof Solo) {
            return (Solo<T>)source;
        }
        ObjectHelper.requireNonNull(source, "source is null");
        return onAssembly(new SoloFromPublisher<T>(source));
    }

    /**
     * Wraps a Single into a Solo and signals its events.
     * @param <T> the value type
     * @param source the source Single
     * @return the new Solo instance
     */
    public static <T> Solo<T> fromSingle(SingleSource<T> source) {
        ObjectHelper.requireNonNull(source, "source is null");
        return onAssembly(new SoloFromSingle<T>(source));
    }

    /**
     * When subscribed, the future is awaited blockingly and
     * indefinitely for its result value; null result
     * will yield a NoSuchElementException.
     * @param <T> the value type
     * @param future the future to await
     * @return the new Solo instance
     *
     * @since 0.14.0
     */
    public static <T> Solo<T> fromFuture(Future<? extends T> future) {
        ObjectHelper.requireNonNull(future, "future is null");
        return onAssembly(new SoloFromFuture<T>(future, 0, null));
    }

    /**
     * When subscribed, the future is awaited blockingly for
     * a given amount of time for its result value; null result
     * will yield a NoSuchElementException and a timeout
     * will yield a TimeoutException.
     * @param <T> the value type
     * @param future the future to await
     * @param timeout the timeout value
     * @param unit the time unit
     * @return the new Solo instance
     *
     * @since 0.14.0
     */
    public static <T> Solo<T> fromFuture(Future<? extends T> future, long timeout, TimeUnit unit) {
        ObjectHelper.requireNonNull(future, "future is null");
        ObjectHelper.requireNonNull(unit, "unit is null");
        return onAssembly(new SoloFromFuture<T>(future, timeout, unit));
    }

    /**
     * Emit the events of the Solo that reacts first.
     * @param <T> the common value type
     * @param sources the Iterable sequence of Solo sources
     * @return the new Solo instance
     */
    public static <T> Solo<T> amb(Iterable<? extends Solo<? extends T>> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return onAssembly(new SoloAmbIterable<T>(sources));
    }

    /**
     * Emit the events of the Solo that reacts first.
     * @param <T> the common value type
     * @param sources the array of Solo sources
     * @return the new Solo instance
     */
    public static <T> Solo<T> ambArray(Solo<? extends T>... sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return onAssembly(new SoloAmbArray<T>(sources));
    }

    /**
     * Concatenate the values in order from a sequence of Solo sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concat(Iterable<? extends Solo<? extends T>> sources) {
        return Flowable.concat(sources);
    }

    /**
     * Concatenate the values in order from a sequence of Solo sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concat(Publisher<? extends Solo<? extends T>> sources) {
        return concat(sources, 2);
    }

    /**
     * Concatenate the values in order from a sequence of Solo sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param prefetch the number of sources to prefetch from upstream
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concat(Publisher<? extends Solo<? extends T>> sources, int prefetch) {
        return Flowable.concat(sources, prefetch);
    }

    /**
     * Concatenate the values in order from a sequence of Solo sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concatArray(Solo<? extends T>... sources) {
        return Flowable.concatArray(sources);
    }

    /**
     * Concatenate the values in order from a sequence of Solo sources, delaying
     * errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concatDelayError(Iterable<? extends Solo<? extends T>> sources) {
        return Flowable.concatDelayError(sources);
    }

    /**
     * Concatenate the values in order from a sequence of Solo sources, delaying
     * errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concatDelayError(Publisher<? extends Solo<? extends T>> sources) {
        return concatDelayError(sources, 2, true);
    }

    /**
     * Concatenate the values in order from a sequence of Solo sources, delaying
     * errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param prefetch the number of sources to prefetch from upstream
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concatDelayError(Publisher<? extends Solo<? extends T>> sources, int prefetch) {
        return concatDelayError(sources, prefetch, true);
    }

    /**
     * Concatenate the values in order from a sequence of Solo sources, delaying
     * errors till a source terminates or the whole sequence terminates.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param prefetch the number of sources to prefetch from upstream
     * @param tillTheEnd if true, errors are delayed to the very end;
     * if false, an error will be signalled at the end of one source
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concatDelayError(Publisher<? extends Solo<? extends T>> sources, int prefetch, boolean tillTheEnd) {
        return Flowable.concatDelayError(sources, prefetch, tillTheEnd);
    }

    /**
     * Concatenate the values in order from a sequence of Solo sources, delaying
     * errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concatArrayDelayError(Solo<? extends T>... sources) {
        return Flowable.concatArrayDelayError(sources);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Solo sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> merge(Iterable<? extends Solo<? extends T>> sources) {
        return merge(sources, Integer.MAX_VALUE);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Solo sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param maxConcurrency the maximum number of active subscriptions
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> merge(Iterable<? extends Solo<? extends T>> sources, int maxConcurrency) {
        return Flowable.merge(sources, maxConcurrency);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Solo sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> merge(Publisher<? extends Solo<? extends T>> sources) {
        return merge(sources, Integer.MAX_VALUE);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Solo sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param maxConcurrency the maximum number of active subscriptions
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> merge(Publisher<? extends Solo<? extends T>> sources, int maxConcurrency) {
        return Flowable.merge(sources, maxConcurrency);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Solo sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeArray(Solo<? extends T>... sources) {
        return mergeArray(Integer.MAX_VALUE, sources);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Solo sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param maxConcurrency the maximum number of active subscriptions
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeArray(int maxConcurrency, Solo<? extends T>... sources) {
        return Flowable.mergeArray(maxConcurrency, 1, sources);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Solo sources,
     * delaying errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeDelayError(Iterable<? extends Solo<? extends T>> sources) {
        return mergeDelayError(sources, Integer.MAX_VALUE);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Solo sources,
     * delaying errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param maxConcurrency the maximum number of active subscriptions
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeDelayError(Iterable<? extends Solo<? extends T>> sources, int maxConcurrency) {
        return Flowable.mergeDelayError(sources, maxConcurrency);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Solo sources,
     * delaying errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeDelayError(Publisher<? extends Solo<? extends T>> sources) {
        return mergeDelayError(sources, Integer.MAX_VALUE);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Solo sources,
     * delaying errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param maxConcurrency the maximum number of active subscriptions
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeDelayError(Publisher<? extends Solo<? extends T>> sources, int maxConcurrency) {
        return Flowable.mergeDelayError(sources, maxConcurrency);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Solo sources,
     * delaying errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeArrayDelayError(Solo<? extends T>... sources) {
        return mergeArrayDelayError(Integer.MAX_VALUE, sources);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Solo sources,
     * delaying errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param maxConcurrency the maximum number of active subscriptions
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeArrayDelayError(int maxConcurrency, Solo<? extends T>... sources) {
        return Flowable.mergeArrayDelayError(maxConcurrency, 1, sources);
    }

    /**
     * Signals a 0L after the specified amount of time has passed since
     * subscription.
     * @param delay the delay time
     * @param unit the time unit
     * @return the new Solo instance
     */
    public static Solo<Long> timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }

    /**
     * Signals a 0L after the specified amount of time has passed since
     * subscription on the specified scheduler.
     * @param delay the delay time
     * @param unit the time unit
     * @param scheduler the scheduler to wait on
     * @return the new Solo instance
     */
    public static Solo<Long> timer(long delay, TimeUnit unit, Scheduler scheduler) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return onAssembly(new SoloTimer(delay, unit, scheduler));
    }

    /**
     * Generate a resource and a Solo based on that resource and then
     * dispose that resource eagerly when the Solo terminates or the
     * downstream cancels the sequence.
     * @param <T> the value type
     * @param <R> the resource type
     * @param resourceSupplier the callback to get a resource for each subscriber
     * @param sourceSupplier the function that returns a Solo for the generated resource
     * @param disposer the consumer of the resource once the upstream terminates or the
     * downstream cancels
     * @return the new Solo instance
     */
    public static <T, R> Solo<T> using(
            Supplier<R> resourceSupplier,
            Function<? super R, ? extends Solo<T>> sourceSupplier,
            Consumer<? super R> disposer
    ) {
        return using(resourceSupplier, sourceSupplier, disposer, true);
    }

    /**
     * Generate a resource and a Solo based on that resource and then
     * dispose that resource eagerly when the Solo terminates or the
     * downstream cancels the sequence.
     * @param <T> the value type
     * @param <R> the resource type
     * @param resourceSupplier the callback to get a resource for each subscriber
     * @param sourceSupplier the function that returns a Solo for the generated resource
     * @param disposer the consumer of the resource once the upstream terminates or the
     * downstream cancels
     * @param eager if true, the resource is disposed before the terminal event is emitted
     *              if false, the resource is disposed after the terminal event has been emitted
     * @return the new Solo instance
     */
    public static <T, R> Solo<T> using(
            Supplier<R> resourceSupplier,
            Function<? super R, ? extends Solo<T>> sourceSupplier,
            Consumer<? super R> disposer,
            boolean eager
    ) {
        ObjectHelper.requireNonNull(resourceSupplier, "resourceSupplier is null");
        ObjectHelper.requireNonNull(sourceSupplier, "sourceSupplier is null");
        ObjectHelper.requireNonNull(disposer, "disposer is null");
        return onAssembly(new SoloUsing<T, R>(resourceSupplier, sourceSupplier, disposer, eager));
    }

    /**
     * Combines the solo values of all the sources via a zipper function into a
     * single resulting value.
     * @param <T> the common input base type
     * @param <R> the result type
     * @param sources the sequence of Solo sources
     * @param zipper the function takin in an array of values and returns a solo value
     * @return the new Solo instance
     */
    public static <T, R> Solo<R> zip(
            Iterable<? extends Solo<? extends T>> sources,
            Function<? super Object[], ? extends R> zipper
    ) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        ObjectHelper.requireNonNull(zipper, "zipper is null");
        return onAssembly(new SoloZipIterable<T, R>(sources, zipper));
    }

    /**
     * Combines the solo values of all the sources via a zipper function into a
     * single resulting value.
     * @param <T> the common input base type
     * @param <R> the result type
     * @param sources the sequence of Solo sources
     * @param zipper the function takin in an array of values and returns a solo value
     * @return the new Solo instance
     */
    public static <T, R> Solo<R> zipArray(
            Function<? super Object[], ? extends R> zipper,
            Solo<? extends T>... sources
    ) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        ObjectHelper.requireNonNull(zipper, "zipper is null");
        return onAssembly(new SoloZipArray<T, R>(sources, zipper));
    }

    // ----------------------------------------------------
    // Instance operators (stay)
    // ----------------------------------------------------

    /**
     * Signal the events of this or the other Solo whichever
     * signals first.
     * @param other the other Solo
     * @return the new Solo instance
     */
    @SuppressWarnings("unchecked")
    public final Solo<T> ambWith(Solo<? extends T> other) {
        return ambArray(this, other);
    }

    /**
     * Run the given Nono after this Solo completes successfully and
     * emit that original success value only if the Nono completes normally.
     * @param other the other Nono to execute
     * @return the new Solo instance
     */
    public final Solo<T> andThen(Nono other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return onAssembly(new SoloAndThen<T>(this, other));
    }

    /**
     * Emits the success value of this Solo and then continues with
     * the events of the other Publisher.
     * @param other the other Publisher instance
     * @return the new Flowable instance
     */
    public final Flowable<T> andThen(Publisher<? extends T> other) {
        return Flowable.concat(this, other);
    }

    /**
     * Emits the success value of this Solo followed by the event of
     * the other Solo.
     * @param other the other Solo instance
     * @return the new Flowable instance
     */
    public final Flowable<T> concatWith(Solo<T> other) {
        return Flowable.concat(this, other);
    }

    /**
     * Merges the values of this Solo and the other Solo into a
     * Flowable sequence.
     * @param other the other Solo instance
     * @return the new Flowable instance
     */
    public final Flowable<T> mergeWith(Solo<T> other) {
        return Flowable.merge(this, other);
    }

    /**
     * Combines the values of this and the other Solo via a BiFunction.
     * @param <U> the other value type
     * @param <R> the result value type
     * @param other the other Solo source
     * @param zipper the bi-function taking the success value from this and
     * the other Solo and returns a solo value to be emitted.
     * @return the new Solo instance
     */
    @SuppressWarnings("unchecked")
    public final <U, R> Solo<R> zipWith(Solo<U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        return Solo.zipArray(Functions.toFunction(zipper), this, other);
    }

    /**
     * Maps the value of this Solo into another value via function.
     * @param <R> the output value type
     * @param mapper the function that receives the success value of this Solo
     * and returns a replacement value.
     * @return the new Solo instance
     */
    public final <R> Solo<R> map(Function<? super T, ? extends R> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return onAssembly(new SoloMap<T, R>(this, mapper));
    }

    /**
     * Maps the Throwable error of this Solo into another Throwable error type.
     * @param errorMapper the function that receives the Throwable and should
     * return a Throwable to be emitted.
     * @return the new Solo instance
     */
    public final Solo<T> mapError(Function<? super Throwable, ? extends Throwable> errorMapper) {
        ObjectHelper.requireNonNull(errorMapper, "errorMapper is null");
        return onAssembly(new SoloMapError<T>(this, errorMapper));
    }

    /**
     * Applies a predicate to the value and emits it if the predicate
     * returns true, completes otherwise.
     * @param predicate the predicate called with the solo value
     * @return the new Perhaps instance
     */
    public final Perhaps<T> filter(Predicate<? super T> predicate) {
        ObjectHelper.requireNonNull(predicate, "predicate is null");
        return Perhaps.onAssembly(new SoloFilter<T>(this, predicate));
    }

    /**
     * Signal a TimeoutException if there is no item from this Solo within
     * the given timeout time.
     * @param timeout the timeout value
     * @param unit the time unit
     * @return the new Solo instance
     */
    public final Solo<T> timeout(long timeout, TimeUnit unit) {
        return timeout(timer(timeout, unit));
    }

    /**
     * Signal a TimeoutException if there is no item from this Solo within
     * the given timeout time, running on the specified scheduler.
     * @param timeout the timeout value
     * @param unit the time unit
     * @param scheduler the scheduler to wait on
     * @return the new Solo instance
     */
    public final Solo<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler) {
        return timeout(timer(timeout, unit, scheduler));
    }

    /**
     * Fall back to another Solo if this Solo doesn't signal within the given
     * time period.
     * @param timeout the timeout value
     * @param unit the time unit
     * @param fallback the other Solo to subscribe to
     * @return the new Solo instance
     */
    public final Solo<T> timeout(long timeout, TimeUnit unit, Solo<T> fallback) {
        return timeout(timer(timeout, unit), fallback);
    }

    /**
     * Fall back to another Solo if this Solo doesn't signal within the given
     * time period, waiting on the specified scheduler.
     * @param timeout the timeout value
     * @param unit the time unit
     * @param scheduler the scheduler to wait on
     * @param fallback the fallback Solo to subscribe to
     * @return the new Solo instance
     */
    public final Solo<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler, Solo<T> fallback) {
        return timeout(timer(timeout, unit, scheduler), fallback);
    }

    /**
     * Signal a TimeoutException if the other Publisher signals or completes
     * before this Solo signals a value.
     * @param other the other Publisher triggers the TimeoutException when
     * it signals its first item or completes.
     * @return the new Solo instance
     */
    public final Solo<T> timeout(Publisher<?> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return onAssembly(new SoloTimeout<T>(this, other, null));
    }

    /**
     * Fall back to another Solo if the other Publisher signals or completes
     * before this Solo signals a value.
     * @param other the other Publisher triggers the TimeoutException when
     * it signals its first item or completes.
     * @param fallback the fallback Solo to subscribe to in case of a timeout
     * @return the new Solo instance
     */
    public final Solo<T> timeout(Publisher<?> other, Solo<T> fallback) {
        ObjectHelper.requireNonNull(other, "other is null");
        ObjectHelper.requireNonNull(fallback, "fallback is null");
        return onAssembly(new SoloTimeout<T>(this, other, fallback));
    }

    /**
     * If the upstream signals an error, signal an item instead.
     * @param item the item to signal if the upstream fails
     * @return the new Solo instance
     */
    public final Solo<T> onErrorReturnItem(T item) {
        ObjectHelper.requireNonNull(item, "item is null");
        return onAssembly(new SoloOnErrorReturnItem<T>(this, item));
    }

    /**
     * If the upstream signals an error, switch over to the next Solo
     * and emits its signal instead.
     * @param next the other Solo to switch to in case of an upstream error
     * @return the new Solo instance
     */
    public final Solo<T> onErrorResumeWith(Solo<T> next) {
        ObjectHelper.requireNonNull(next, "next is null");
        return onAssembly(new SoloOnErrorResumeWith<T>(this, next));
    }

    /**
     * If the upstream signals an error, call a function and subscribe to
     * the Solo it returns.
     * @param errorHandler the function receiving the upstream error and
     * returns a Solo to resume with.
     * @return the new Solo instance
     */
    public final Solo<T> onErrorResumeNext(Function<? super Throwable, ? extends Solo<T>> errorHandler) {
        ObjectHelper.requireNonNull(errorHandler, "errorHandler is null");
        return onAssembly(new SoloOnErrorResumeNext<T>(this, errorHandler));
    }

    /**
     * Maps the success value of this Solo into another Solo and
     * emits its signals.
     * @param <R> the result type
     * @param mapper the function that receives the success value and returns
     * another Solo to subscribe to
     * @return the new Solo instance
     */
    public final <R> Solo<R> flatMap(
            Function<? super T, ? extends Solo<? extends R>> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return onAssembly(new SoloFlatMap<T, R>(this, mapper));
    }

    /**
     * Maps the success value or the failure of this Solo into another
     * Solo and emits its signal.
     * @param <R> the result type
     * @param onSuccessMapper the function that receives the success value and return
     * another Solo to subscribe to
     * @param onErrorMapper the function that receives the Throwable error and
     * return another Solo to subscribe to
     * @return th new Solo instance
     */
    public final <R> Solo<R> flatMap(
            Function<? super T, ? extends Solo<? extends R>> onSuccessMapper,
            Function<? super Throwable, ? extends Solo<? extends R>> onErrorMapper
    ) {
        ObjectHelper.requireNonNull(onSuccessMapper, "onSuccessMapper is null");
        ObjectHelper.requireNonNull(onErrorMapper, "onErrorMapper is null");
        return onAssembly(new SoloFlatMapSignal<T, R>(this, onSuccessMapper, onErrorMapper));
    }

    /**
     * Maps the success value of this Solo into a Publisher and
     * emits its signals.
     * @param <R> the result type
     * @param mapper the function that takes the success value of this Solo
     * and returns a Publisher
     * @return the new Flowable instance
     */
    public final <R> Flowable<R> flatMapPublisher(
            Function<? super T, ? extends Publisher<? extends R>> mapper
    ) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new SoloFlatMapPublisher<T, R>(this, mapper));
    }

    /**
     * Signal a NoSuchElementException if the other signals before this
     * Solo signals.
     * @param other the other Publisher
     * @return the new Solo instance
     */
    public final Solo<T> takeUntil(Publisher<?> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return onAssembly(new SoloTakeUntil<T>(this, other));
    }

    /**
     * Delay the emission of the signal of this Solo with the specified
     * time amount.
     * @param delay the delay time
     * @param unit the delay time unit
     * @return the new Solo instance
     */
    public final Solo<T> delay(long delay, TimeUnit unit) {
        return delay(delay, unit, Schedulers.computation());
    }

    /**
     * Delay the emission of the signal of this Solo with the specified
     * time amount on the specified scheduler.
     * @param delay the delay time
     * @param unit the delay time unit
     * @param scheduler the scheduler to wait on
     * @return the new Solo instance
     */
    public final Solo<T> delay(long delay, TimeUnit unit, Scheduler scheduler) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return delay(timer(delay, unit, scheduler));
    }

    /**
     * Delay the emission of the signal until the other Publisher signals
     * an item or completes.
     * @param other the other Publisher that has to signal to emit the origina
     * signal from this Solo
     * @return the new Solo instance
     */
    public final Solo<T> delay(Publisher<?> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return onAssembly(new SoloDelayPublisher<T>(this, other));
    }

    /**
     * Delay the subscription to this Solo until the other Publisher
     * signals a value or completes.
     * @param other the other Publisher to trigger the actual subscription
     * @return the new Solo type
     */
    public final Solo<T> delaySubscription(Publisher<?> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return onAssembly(new SoloDelaySubscription<T>(this, other));
    }

    /**
     * Delay the subscription to this Solo until the specified delay elapses.
     * @param delay the delay time
     * @param unit the delay unit
     * @return the new Solo type
     */
    public final Solo<T> delaySubscription(long delay, TimeUnit unit) {
        return delaySubscription(timer(delay, unit));
    }

    /**
     * Delay the subscription to this Solo until the specified delay elapses.
     * @param delay the delay time
     * @param unit the delay unit
     * @param scheduler the scheduler to wait on
     * @return the new Solo type
     */
    public final Solo<T> delaySubscription(long delay, TimeUnit unit, Scheduler scheduler) {
        return delaySubscription(timer(delay, unit, scheduler));
    }

    /**
     * Converts this Solo into a Flowable.
     * @return the new Flowable instance
     */
    public final Flowable<T> toFlowable() {
        return RxJavaPlugins.onAssembly(new SoloToFlowable<T>(this));
    }

    /**
     * Converts this Solo into an Observable.
     * @return the new Observable instance
     */
    public final Observable<T> toObservable() {
        return RxJavaPlugins.onAssembly(new SoloToObservable<T>(this));
    }

    /**
     * Converts this Soli into a Single.
     * @return the new Single instance
     */
    public final Single<T> toSingle() {
        return RxJavaPlugins.onAssembly(new SoloToSingle<T>(this));
    }

    /**
     * Executes a callback when the upstream calls onSubscribe.
     * @param onSubscribe the consumer called with the upstream Subscription
     * @return the new Solo instance
     */
    public final Solo<T> doOnSubscribe(Consumer<? super Subscription> onSubscribe) {
        ObjectHelper.requireNonNull(onSubscribe, "onSubscribe is null");
        return onAssembly(new SoloDoOnLifecycle<T>(this,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION,
                onSubscribe,
                Functions.EMPTY_LONG_CONSUMER,
                Functions.EMPTY_ACTION
        ));
    }

    /**
     * Executes the callback when the downstream requests from this Solo.
     * @param onRequest the callback called with the request amount
     * @return the new Solo instance
     */
    public final Solo<T> doOnRequest(LongConsumer onRequest) {
        ObjectHelper.requireNonNull(onRequest, "onRequest is null");
        return onAssembly(new SoloDoOnLifecycle<T>(this,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION,
                Functions.emptyConsumer(),
                onRequest,
                Functions.EMPTY_ACTION
        ));
    }

    /**
     * Executes the callback if the downstream cancels the sequence.
     * @param onCancel the action to call
     * @return the new Solo instance
     */
    public final Solo<T> doOnCancel(Action onCancel) {
        ObjectHelper.requireNonNull(onCancel, "onCancel is null");
        return onAssembly(new SoloDoOnLifecycle<T>(this,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION,
                Functions.emptyConsumer(),
                Functions.EMPTY_LONG_CONSUMER,
                onCancel
        ));
    }

    /**
     * Executes a callback before the value is emitted to downstream.
     * @param onNext the consumer called with the current value before it is
     * is emitted to downstream.
     * @return the new Solo instance
     */
    public final Solo<T> doOnNext(Consumer<? super T> onNext) {
        ObjectHelper.requireNonNull(onNext, "onNext is null");
        return onAssembly(new SoloDoOnLifecycle<T>(this,
                onNext,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION,
                Functions.emptyConsumer(),
                Functions.EMPTY_LONG_CONSUMER,
                Functions.EMPTY_ACTION
        ));
    }

    /**
     * Executes a callback after the value is emitted to downstream.
     * @param onAfterNext the consumer called with the current value after it is
     * is emitted to downstream.
     * @return the new Solo instance
     */
    public final Solo<T> doAfterNext(Consumer<? super T> onAfterNext) {
        ObjectHelper.requireNonNull(onAfterNext, "onAfterNext is null");
        return onAssembly(new SoloDoOnLifecycle<T>(this,
                Functions.emptyConsumer(),
                onAfterNext,
                Functions.emptyConsumer(),
                Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION,
                Functions.emptyConsumer(),
                Functions.EMPTY_LONG_CONSUMER,
                Functions.EMPTY_ACTION
        ));
    }

    /**
     * Executes a callback when the upstream signals an error.
     * @param onError the consumer called before the error is emitted to
     * the downstream
     * @return the new Solo instance
     */
    public final Solo<T> doOnError(Consumer<? super Throwable> onError) {
        ObjectHelper.requireNonNull(onError, "onError is null");
        return onAssembly(new SoloDoOnLifecycle<T>(this,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                onError,
                Functions.EMPTY_ACTION,
                Functions.EMPTY_ACTION,
                Functions.emptyConsumer(),
                Functions.EMPTY_LONG_CONSUMER,
                Functions.EMPTY_ACTION
        ));
    }

    /**
     * Executes a callback when the upstream completes normally.
     * @param onComplete the consumer called before the completion event
     * is emitted to the downstream.
     * @return the new Solo instance
     */
    public final Solo<T> doOnComplete(Action onComplete) {
        ObjectHelper.requireNonNull(onComplete, "onComplete is null");
        return onAssembly(new SoloDoOnLifecycle<T>(this,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                onComplete,
                Functions.EMPTY_ACTION,
                Functions.emptyConsumer(),
                Functions.EMPTY_LONG_CONSUMER,
                Functions.EMPTY_ACTION
        ));
    }

    /**
     * Executes the callback after this Solo terminates and the downstream
     * is notified.
     * @param onAfterTerminate the callback to call after the downstream is notified
     * @return the new Solo instance
     */
    public final Solo<T> doAfterTerminate(Action onAfterTerminate) {
        ObjectHelper.requireNonNull(onAfterTerminate, "onAfterTerminate is null");
        return onAssembly(new SoloDoOnLifecycle<T>(this,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.EMPTY_ACTION,
                onAfterTerminate,
                Functions.emptyConsumer(),
                Functions.EMPTY_LONG_CONSUMER,
                Functions.EMPTY_ACTION
        ));
    }

    /**
     * Executes the callback exactly if the upstream terminates or
     * the downstream cancels the sequence.
     * @param onFinally the action to call
     * @return the new Solo instance
     */
    public final Solo<T> doFinally(Action onFinally) {
        ObjectHelper.requireNonNull(onFinally, "onFinally is null");
        return onAssembly(new SoloDoFinally<T>(this, onFinally));
    }

    /**
     * Ignore the solo value of this Solo and only signal the terminal events.
     * @return the new Nono instance
     */
    public final Nono ignoreElement() {
        return Nono.onAssembly(new NonoFromPublisher(this));
    }

    /**
     * Hides the identity of this Solo, including its Subscription and
     * allows preventing fusion and other optimizations for diagnostic
     * purposes.
     * @return the new Solo instance
     */
    public final Solo<T> hide() {
        return onAssembly(new SoloHide<T>(this));
    }

    /**
     * Applies the function, fluently to this Solo and returns the value it returns.
     * @param <R> the result type
     * @param converter the function receiving this Solo and returns a value to be returned
     * @return the value returned by the function
     */
    public final <R> R to(Function<? super Solo<T>, R> converter) {
        try {
            return converter.apply(this);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /**
     * Applies a function to this Solo and returns the Solo it returned.
     * @param <R> the result type
     * @param composer the function that receives this Solo and should return a Solo
     * @return the new Solo instance
     */
    public final <R> Solo<R> compose(Function<? super Solo<T>, ? extends Solo<R>> composer) {
        return to(composer);
    }

    /**
     * Map the downstream Subscriber into an upstream Subscriber.
     * @param <R> the downstream value type
     * @param onLift the function called with the downstream's Subscriber and
     * should return a Subscriber to be subscribed to this Solo.
     * @return the new Solo type
     */
    public final <R> Solo<R> lift(Function<Subscriber<? super R>, Subscriber<? super T>> onLift) {
        ObjectHelper.requireNonNull(onLift, "onLift is null");
        return onAssembly(new SoloLift<T, R>(this, onLift));
    }

    /**
     * Repeats this Solo indefinitely.
     * @return the new Flowable instance
     */
    public final Flowable<T> repeat() {
        return Flowable.fromPublisher(this).repeat();
    }

    /**
     * Repeats this Solo at most the given number of times.
     * @param times the number of times to repeat
     * @return the new Flowable instance
     */
    public final Flowable<T> repeat(long times) {
        return Flowable.fromPublisher(this).repeat(times);
    }

    /**
     * Repeats this Solo until the given boolean supplier returns true when an
     * repeat iteration of this Solo completes.
     * @param stop the supplier of a boolean value that should return true to
     * stop repeating.
     * @return the new Flowable instance
     */
    public final Flowable<T> repeat(BooleanSupplier stop) {
        return Flowable.fromPublisher(this).repeatUntil(stop);
    }

    /**
     * Repeats this Solo when the Publisher returned by the handler function emits
     * an item or terminates if this Publisher terminates.
     * @param handler the function that receives Flowable that emits an item
     * when this Solo completes and returns a Publisher that should emit an item
     * to trigger a repeat or terminate to trigger a termination.
     * @return the new Flowable instance
     */
    public final Flowable<T> repeatWhen(Function<? super Flowable<Object>, ? extends Publisher<?>> handler) {
        return Flowable.fromPublisher(this).repeatWhen(handler);
    }

    /**
     * Retry this Solo indefinitely if it fails.
     * @return the new Solo instance
     */
    public final Solo<T> retry() {
        return onAssembly(new SoloRetry<T>(this, Long.MAX_VALUE));
    }

    /**
     * Retry this Solo at most the given number of times if it fails.
     * @param times the number of times, Long.MAX_VALUE means indefinitely
     * @return the new Solo instance
     */
    public final Solo<T> retry(long times) {
        if (times < 0L) {
            throw new IllegalArgumentException("times >= 0 required but it was " + times);
        }
        return onAssembly(new SoloRetry<T>(this, times));
    }

    /**
     * Retry this Solo if the predicate returns true for the latest failure
     * Throwable.
     * @param predicate the predicate receiving the latest Throwable and
     * if returns true, the Solo is retried.
     * @return the new Solo instance
     */
    public final Solo<T> retry(Predicate<? super Throwable> predicate) {
        ObjectHelper.requireNonNull(predicate, "predicate is null");
        return onAssembly(new SoloRetryWhile<T>(this, predicate));
    }

    /**
     * Retry this solo if the Publisher returned by the handler signals an item
     * in response to the failure Throwable.
     * @param handler the function that receives a Flowable that signals the
     * failure Throwable of this Solo and returns a Publisher which triggers a retry
     * or termination.
     * @return the new Solo instance
     */
    public final Solo<T> retryWhen(Function<? super Flowable<Throwable>, ? extends Publisher<?>> handler) {
        ObjectHelper.requireNonNull(handler, "handler is null");
        return onAssembly(new SoloRetryWhen<T>(this, handler));
    }

    /**
     * Returns a Solo that subscribes to this Solo on the specified scheduler
     * and makes sure downstream requests are forwarded there as well.
     * @param scheduler the scheduler to subscribe on
     * @return the new Solo instance
     */
    public final Solo<T> subscribeOn(Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return onAssembly(new SoloSubscribeOn<T>(this, scheduler));
    }

    /**
     * Returns a Solo that delivers the onNext, onError and onComplete signals
     * from this Solo on the specified scheduler.
     * @param scheduler the scheduler to emit the events on
     * @return the new Solo instance
     */
    public final Solo<T> observeOn(Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return onAssembly(new SoloObserveOn<T>(this, scheduler));
    }

    /**
     * Returns a Solo which when cancelled, cancels this Solo on the
     * specified scheduler.
     * @param scheduler the scheduler to cancel this Solo
     * @return the new Solo instance
     */
    public final Solo<T> unsubscribeOn(Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return onAssembly(new SoloUnsubscribeOn<T>(this, scheduler));
    }

    /**
     * Caches the value or error event of the upstream Solo
     * and relays/replays it to Subscribers.
     * @return the new Solo instance
     *
     * @since 0.14.1
     */
    public final Solo<T> cache() {
        return onAssembly(new SoloCache<T>(this));
    }

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

    /**
     * Subscribe with a Subscriber (subclass) and return it as is.
     * @param <E> the Subscriber subclass type
     * @param s the soubscriber, not null
     * @return the {@code s} as is
     */
    public final <E extends Subscriber<? super T>> E subscribeWith(E s) {
        subscribe(s);
        return s;
    }

    /**
     * Subscribe to this Solo and ignore any signal it produces.
     * @return the Disposable that allows cancelling the subscription
     */
    public final Disposable subscribe() {
        return subscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to this Solo and calls the onNext if this Solo succeeds.
     * @param onNext called when this Solo succeeds
     * @return the Disposable that allows cancelling the subscription
     */
    public final Disposable subscribe(Consumer<? super T> onNext) {
        return subscribe(onNext, Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to this Solo and calls the appropriate callback for the resulting signal.
     * @param onNext called when this Solo succeeds
     * @param onError called when this Solo fails
     * @return the Disposable that allows cancelling the subscription
     */
    public final Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError) {
        return subscribe(onNext, onError, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to this Solo and calls the appropriate callback for the resulting signal.
     * @param onNext called when this Solo succeeds
     * @param onError called when this Solo fails
     * @param onComplete called when this Solo succeeds after the call to onNext
     * @return the Disposable that allows cancelling the subscription
     */
    public final Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Action onComplete) {
        LambdaSubscriber<T> s = new LambdaSubscriber<T>(onNext, onError, onComplete, Functions.REQUEST_MAX);
        subscribe(s);
        return s;
    }

    /**
     * Blocks until this Solo terminates and ignores the signals it produced.
     */
    public final void blockingSubscribe() {
        blockingSubscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    /**
     * Blocks until this Solo terminates and calls the onNext with the success value.
     * @param onNext the callback to call when this Solo completes with a success value
     */
    public final void blockingSubscribe(Consumer<? super T> onNext) {
        blockingSubscribe(onNext, Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    /**
     * Blocks until this solo terminates and calls the onNext with the success value
     * or calls the onError with the error Throwable.
     * @param onNext the callback to call when this Solo completes with a success value
     * @param onError the callback to call when this Solo fails with an error
     */
    public final void blockingSubscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError) {
        blockingSubscribe(onNext, onError, Functions.EMPTY_ACTION);
    }

    /**
     * Blocks until this Solo produces its terminal signal and calls the
     * appropriate callback(s) based on the signal type.
     * @param onNext called when the Solo succeeds
     * @param onError called when the Solo fails
     * @param onComplete called when the Solo succeeds after the call to onNext.
     */
    public final void blockingSubscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Action onComplete) {
        ObjectHelper.requireNonNull(onNext, "onNext is null");
        ObjectHelper.requireNonNull(onError, "onError is null");
        ObjectHelper.requireNonNull(onComplete, "onComplete is null");
        BlockingGetSubscriber<T> s = new BlockingGetSubscriber<T>();
        subscribe(s);
        s.blockingCall(onNext, onError, onComplete);
    }

    /**
     * Blockingly awaits indefinitely the success value of this Solo or rethrows
     * its error (wrapped into a RuntimeException if necessary).
     * @return the success value of this Solo
     */
    public final T blockingGet() {
        BlockingGetSubscriber<T> s = new BlockingGetSubscriber<T>();
        subscribe(s);
        return s.blockingGet();
    }

    /**
     * Blockingly awaits at most the given timeout for the success
     * value of this Solo or rethrows
     * its error (wrapped into a RuntimeException if necessary).
     * @param timeout the time to wait for a success value
     * @param unit the time unit of the timeout
     * @return the success value of this Solo
     */
    public final T blockingGet(long timeout, TimeUnit unit) {
        BlockingGetSubscriber<T> s = new BlockingGetSubscriber<T>();
        subscribe(s);
        return s.blockingGet(timeout, unit);
    }

    /**
     * Creates a TestSubscriber and subscribes it to this Solo.
     * @return the new TestSubscriber instance
     */
    public final TestSubscriber<T> test() {
        return test(Long.MAX_VALUE, false);
    }

    /**
     * Creates a TestSubscriber, optionally cancels it, and subscribes
     * it to this Solo.
     * @param cancel if true, the TestSubscriber will be cancelled before
     * subscribing to this Solo
     * @return the new TestSubscriber instance
     */
    public final TestSubscriber<T> test(boolean cancel) {
        return test(Long.MAX_VALUE, cancel);
    }

    /**
     * Creates a TestSubscriber with the given initial request and
     * subscribes it to this Solo.
     * @param initialRequest the initial request amount, non-negative
     * @return the new TestSubscriber
     */
    public final TestSubscriber<T> test(long initialRequest) {
        return test(initialRequest, false);
    }

    /**
     * Creates a TestSubscriber with the given initial request,
     * optionally cancels it, and subscribes it to this Solo.
     * @param initialRequest the initial request amount, non-negative
     * @param cancel if true, the TestSubscriber will be cancelled before
     * subscribing to this Solo
     * @return the new TestSubscriber
     */
    public final TestSubscriber<T> test(long initialRequest, boolean cancel) {
        TestSubscriber<T> ts = new TestSubscriber<T>(initialRequest);
        if (cancel) {
            ts.cancel();
        }
        subscribe(ts);
        return ts;
    }

    /**
     * Converts this Solo into a Future and signals its single
     * value.
     * @return the new Future instance
     */
    public final Future<T> toFuture() {
        FuturePerhapsSubscriber<T> fs = new FuturePerhapsSubscriber<T>();
        subscribe(fs);
        return fs;
    }
}
