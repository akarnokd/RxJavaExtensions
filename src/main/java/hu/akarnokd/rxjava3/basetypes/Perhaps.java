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
 * A 0-1-error base reactive type, similar to Maybe, implementing the Reactive-Streams Publisher
 * interface.
 *
 * @param <T> the value type
 * 
 * @since 0.14.0
 */
public abstract class Perhaps<T> implements Publisher<T> {

    /**
     * Hook called when assembling Perhaps sequences.
     */
    @SuppressWarnings("rawtypes")
    private static volatile Function<Perhaps, Perhaps> onAssembly;

    /**
     * Returns the current onAssembly handler.
     * @param <T> the target value type
     * @return the current handler, maybe null
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Function<Perhaps<T>, Perhaps<T>> getOnAssemblyHandler() {
        return (Function)onAssembly;
    }

    /**
     * Set the onAssembly handler.
     * @param <T> the target value type
     * @param handler the handler, null clears the handler
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> void setOnAssemblyHandler(Function<Perhaps<T>, Perhaps<T>> handler) {
        onAssembly = (Function)handler;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected static <T> Perhaps<T> onAssembly(Perhaps<T> source) {
        Function<Perhaps, Perhaps> f = onAssembly;
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
     * Create a Perhaps that for each incoming Subscriber calls a callback to
     * emit a sync or async events in a thread-safe, backpressure-aware and
     * cancellation-safe manner.
     * @param <T> the value type emitted
     * @param onCreate the callback called for each individual subscriber with an
     * abstraction of the incoming Subscriber.
     * @return th new Perhaps instance
     */
    public static <T> Perhaps<T> create(MaybeOnSubscribe<T> onCreate) {
        ObjectHelper.requireNonNull(onCreate, "onCreate is null");
        return onAssembly(new PerhapsCreate<T>(onCreate));
    }

    /**
     * Returns a Perhaps that signals the given item.
     * @param <T> the value type
     * @param item the item to signal, not null
     * @return the new Perhaps instance
     */
    public static <T> Perhaps<T> just(T item) {
        ObjectHelper.requireNonNull(item, "item is null");
        return onAssembly(new PerhapsJust<T>(item));
    }

    /**
     * Returns an empty Perhaps.
     * @param <T> the value type
     * @return the shared Perhaps instance
     */
    public static <T> Perhaps<T> empty() {
        return onAssembly(PerhapsEmpty.<T>instance());
    }

    /**
     * Returns a Perhaps that signals the given error to Subscribers.
     * @param <T> the value type
     * @param error the error to signal, not null
     * @return the new Perhaps instance
     */
    public static <T> Perhaps<T> error(Throwable error) {
        ObjectHelper.requireNonNull(error, "error is null");
        return onAssembly(new PerhapsError<T>(error));
    }

    /**
     * Returns a Perhaps that signals the error returned from
     * the errorSupplier to each individual Subscriber.
     * @param <T> the value type
     * @param errorSupplier the supplier called for each Subscriber to
     * return a Throwable to be signalled
     * @return the new Perhaps instance
     */
    public static <T> Perhaps<T> error(Supplier<? extends Throwable> errorSupplier) {
        ObjectHelper.requireNonNull(errorSupplier, "errorSupplier is null");
        return onAssembly(new PerhapsErrorSupplier<T>(errorSupplier));
    }

    /**
     * Returns a Perhaps that never signals any item or terminal event.
     * @param <T> the value type
     * @return the shared Perhaps instance
     */
    public static <T> Perhaps<T> never() {
        return onAssembly(PerhapsNever.<T>instance());
    }

    /**
     * Runs a Callable and emits its resulting value or its
     * exception; null is considered to be an indication for an empty Perhaps.
     * @param <T> the value type
     * @param callable the callable to call for each individual Subscriber
     * @return the new Perhaps instance
     */
    public static <T> Perhaps<T> fromCallable(Callable<T> callable) {
        ObjectHelper.requireNonNull(callable, "callable is null");
        return onAssembly(new PerhapsFromCallable<T>(callable));
    }

    /**
     * Run an action for each individual Subscriber and terminate.
     * @param <T> the value type
     * @param action the action to call for each individual Subscriber
     * @return the new Perhaps instance
     */
    public static <T> Perhaps<T> fromAction(Action action) {
        ObjectHelper.requireNonNull(action, "action is null");
        return onAssembly(new PerhapsFromAction<T>(action));
    }

    /**
     * When subscribed, the future is awaited blockingly and
     * indefinitely for its result value; null result
     * will yield a NoSuchElementException.
     * @param <T> the value type
     * @param future the future to await
     * @return the new Perhaps instance
     */
    public static <T> Perhaps<T> fromFuture(Future<? extends T> future) {
        ObjectHelper.requireNonNull(future, "future is null");
        return onAssembly(new PerhapsFromFuture<T>(future, 0, null));
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
     * @return the new Perhaps instance
     */
    public static <T> Perhaps<T> fromFuture(Future<? extends T> future, long timeout, TimeUnit unit) {
        ObjectHelper.requireNonNull(future, "future is null");
        ObjectHelper.requireNonNull(unit, "unit is null");
        return onAssembly(new PerhapsFromFuture<T>(future, timeout, unit));
    }

    /**
     * Wraps a Publisher and signals its single value or completion signal or
     * signals IndexOutOfBoundsException if the Publisher has more than one element.
     * @param <T> the value type
     * @param source the source Publisher
     * @return the new Perhaps instance
     */
    public static <T> Perhaps<T> fromPublisher(Publisher<T> source) {
        ObjectHelper.requireNonNull(source, "source is null");
        return onAssembly(new PerhapsFromPublisher<T>(source));
    }

    /**
     * Wraps a Single and signals its events.
     * @param <T> the value type
     * @param source the source
     * @return the new Perhaps instance
     */
    public static <T> Perhaps<T> fromSingle(SingleSource<T> source) {
        ObjectHelper.requireNonNull(source, "source is null");
        return onAssembly(new PerhapsFromSingle<T>(source));
    }

    /**
     * Wraps a Maybe and signals its events.
     * @param <T> the value type
     * @param source the source
     * @return the new Perhaps instance
     */
    public static <T> Perhaps<T> fromMaybe(MaybeSource<T> source) {
        ObjectHelper.requireNonNull(source, "source is null");
        return onAssembly(new PerhapsFromMaybe<T>(source));
    }

    /**
     * Wraps a Completable and signals its terminal events.
     * @param <T> the value type
     * @param source the source
     * @return the new Perhaps instance
     */
    public static <T> Perhaps<T> fromCompletable(CompletableSource source) {
        ObjectHelper.requireNonNull(source, "source is null");
        return onAssembly(new PerhapsFromCompletable<T>(source));
    }

    /**
     * Defers the creation of the actual Perhaps instance until
     * subscription time and for each downstream Subscriber the given
     * Supplier is called.
     * @param <T> the value type
     * @param supplier the Supplier called for each individual Subscriber
     * to return a Perhaps to be subscribe to.
     * @return the new Perhaps instance
     */
    public static <T> Perhaps<T> defer(Supplier<? extends Perhaps<? extends T>> supplier) {
        ObjectHelper.requireNonNull(supplier, "supplier is null");
        return onAssembly(new PerhapsDefer<T>(supplier));
    }

    /**
     * Emit the events of the Perhaps that reacts first.
     * @param <T> the common value type
     * @param sources the Iterable sequence of Perhaps sources
     * @return the new Perhaps instance
     */
    public static <T> Perhaps<T> amb(Iterable<? extends Perhaps<? extends T>> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return onAssembly(new PerhapsAmbIterable<T>(sources));
    }

    /**
     * Emit the events of the Perhaps that reacts first.
     * @param <T> the common value type
     * @param sources the array of Perhaps sources
     * @return the new Perhaps instance
     */
    public static <T> Perhaps<T> ambArray(Perhaps<? extends T>... sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return onAssembly(new PerhapsAmbArray<T>(sources));
    }

    /**
     * Concatenate the values in order from a sequence of Perhaps sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concat(Iterable<? extends Perhaps<? extends T>> sources) {
        return Flowable.concat(sources);
    }

    /**
     * Concatenate the values in order from a sequence of Perhaps sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concat(Publisher<? extends Perhaps<? extends T>> sources) {
        return Flowable.concat(sources);
    }

    /**
     * Concatenate the values in order from a sequence of Perhaps sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param prefetch the number of sources to prefetch from upstream
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concat(Publisher<? extends Perhaps<? extends T>> sources, int prefetch) {
        return Flowable.concat(sources, prefetch);
    }

    /**
     * Concatenate the values in order from a sequence of Perhaps sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concatArray(Perhaps<? extends T>... sources) {
        return Flowable.concatArray(sources);
    }

    /**
     * Concatenate the values in order from a sequence of Perhaps sources, delaying
     * errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concatDelayError(Iterable<? extends Perhaps<? extends T>> sources) {
        return Flowable.concatDelayError(sources);
    }

    /**
     * Concatenate the values in order from a sequence of Perhaps sources, delaying
     * errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concatDelayError(Publisher<? extends Perhaps<? extends T>> sources) {
        return Flowable.concatDelayError(sources);
    }

    /**
     * Concatenate the values in order from a sequence of Perhaps sources, delaying
     * errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param prefetch the number of sources to prefetch from upstream
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concatDelayError(Publisher<? extends Perhaps<? extends T>> sources, int prefetch) {
        return Flowable.concatDelayError(sources, prefetch, false);
    }

    /**
     * Concatenate the values in order from a sequence of Perhaps sources, delaying
     * errors till a source terminates or the whole sequence terminates.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param prefetch the number of sources to prefetch from upstream
     * @param tillTheEnd if true, errors are delayed to the very end;
     * if false, an error will be signalled at the end of one source
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concatDelayError(Publisher<? extends Perhaps<? extends T>> sources, int prefetch, boolean tillTheEnd) {
        return Flowable.concatDelayError(sources, prefetch, tillTheEnd);
    }

    /**
     * Concatenate the values in order from a sequence of Perhaps sources, delaying
     * errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> concatArrayDelayError(Perhaps<? extends T>... sources) {
        return Flowable.concatArrayDelayError(sources);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Perhaps sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> merge(Iterable<? extends Perhaps<? extends T>> sources) {
        return Flowable.merge(sources);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Perhaps sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param maxConcurrency the maximum number of active subscriptions
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> merge(Iterable<? extends Perhaps<? extends T>> sources, int maxConcurrency) {
        return Flowable.merge(sources, maxConcurrency);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Perhaps sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> merge(Publisher<? extends Perhaps<? extends T>> sources) {
        return Flowable.merge(sources);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Perhaps sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param maxConcurrency the maximum number of active subscriptions
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> merge(Publisher<? extends Perhaps<? extends T>> sources, int maxConcurrency) {
        return Flowable.merge(sources, maxConcurrency);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Perhaps sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeArray(Perhaps<? extends T>... sources) {
        return Flowable.mergeArray(sources);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Perhaps sources.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param maxConcurrency the maximum number of active subscriptions
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeArray(int maxConcurrency, Perhaps<? extends T>... sources) {
        return Flowable.mergeArray(maxConcurrency, 1, sources);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Perhaps sources,
     * delaying errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeDelayError(Iterable<? extends Perhaps<? extends T>> sources) {
        return Flowable.mergeDelayError(sources);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Perhaps sources,
     * delaying errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param maxConcurrency the maximum number of active subscriptions
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeDelayError(Iterable<? extends Perhaps<? extends T>> sources, int maxConcurrency) {
        return Flowable.mergeDelayError(sources, maxConcurrency);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Perhaps sources,
     * delaying errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeDelayError(Publisher<? extends Perhaps<? extends T>> sources) {
        return Flowable.mergeDelayError(sources);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Perhaps sources,
     * delaying errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param maxConcurrency the maximum number of active subscriptions
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeDelayError(Publisher<? extends Perhaps<? extends T>> sources, int maxConcurrency) {
        return Flowable.mergeDelayError(sources, maxConcurrency);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Perhaps sources,
     * delaying errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeArrayDelayError(Perhaps<? extends T>... sources) {
        return Flowable.mergeArrayDelayError(sources);
    }

    /**
     * Merge the values in arbitrary order from a sequence of Perhaps sources,
     * delaying errors till all sources terminate.
     * @param <T> the common base value type
     * @param sources the sequence of sources
     * @param maxConcurrency the maximum number of active subscriptions
     * @return the new Flowable instance
     */
    public static <T> Flowable<T> mergeArrayDelayError(int maxConcurrency, Perhaps<? extends T>... sources) {
        return Flowable.mergeArrayDelayError(maxConcurrency, 1, sources);
    }

    /**
     * Signals a 0L after the specified amount of time has passed since
     * subscription.
     * @param delay the delay time
     * @param unit the time unit
     * @return the new Perhaps instance
     */
    public static Perhaps<Long> timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }

    /**
     * Signals a 0L after the specified amount of time has passed since
     * subscription on the specified scheduler.
     * @param delay the delay time
     * @param unit the time unit
     * @param scheduler the scheduler to wait on
     * @return the new Perhaps instance
     */
    public static Perhaps<Long> timer(long delay, TimeUnit unit, Scheduler scheduler) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return onAssembly(new PerhapsTimer(delay, unit, scheduler));
    }

    /**
     * Generate a resource and a Perhaps based on that resource and then
     * dispose that resource eagerly when the Perhaps terminates or the
     * downstream cancels the sequence.
     * @param <T> the value type
     * @param <R> the resource type
     * @param resourceSupplier the callback to get a resource for each subscriber
     * @param sourceSupplier the function that returns a Perhaps for the generated resource
     * @param disposer the consumer of the resource once the upstream terminates or the
     * downstream cancels
     * @return the new Perhaps instance
     */
    public static <T, R> Perhaps<T> using(Supplier<R> resourceSupplier,
            Function<? super R, ? extends Perhaps<? extends T>> sourceSupplier,
            Consumer<? super R> disposer) {
        return using(resourceSupplier, sourceSupplier, disposer, true);
    }

    /**
     * Generate a resource and a Perhaps based on that resource and then
     * dispose that resource eagerly when the Perhaps terminates or the
     * downstream cancels the sequence.
     * @param <T> the value type
     * @param <R> the resource type
     * @param resourceSupplier the callback to get a resource for each subscriber
     * @param sourceSupplier the function that returns a Perhaps for the generated resource
     * @param disposer the consumer of the resource once the upstream terminates or the
     * downstream cancels
     * @param eager if true, the resource is disposed before the terminal event is emitted
     *              if false, the resource is disposed after the terminal event has been emitted
     * @return the new Perhaps instance
     */
    public static <T, R> Perhaps<T> using(Supplier<R> resourceSupplier,
            Function<? super R, ? extends Perhaps<? extends T>> sourceSupplier,
            Consumer<? super R> disposer, boolean eager) {
        ObjectHelper.requireNonNull(resourceSupplier, "resourceSupplier is null");
        ObjectHelper.requireNonNull(sourceSupplier, "sourceSupplier is null");
        ObjectHelper.requireNonNull(disposer, "disposer is null");
        return onAssembly(new PerhapsUsing<T, R>(resourceSupplier, sourceSupplier, disposer, eager));
    }

    /**
     * Combines the Perhaps values of all the sources via a zipper function into a
     * single resulting value.
     * @param <T> the common input base type
     * @param <R> the result type
     * @param sources the sequence of Perhaps sources
     * @param zipper the function takin in an array of values and returns a Perhaps value
     * @return the new Perhaps instance
     */
    public static <T, R> Perhaps<R> zip(Iterable<? extends Perhaps<? extends T>> sources, Function<? super Object[], ? extends R> zipper) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        ObjectHelper.requireNonNull(zipper, "zipper is null");
        return onAssembly(new PerhapsZipIterable<T, R>(sources, zipper));
    }

    /**
     * Combines the Perhaps values of all the sources via a zipper function into a
     * single resulting value.
     * @param <T> the common input base type
     * @param <R> the result type
     * @param sources the sequence of Perhaps sources
     * @param zipper the function takin in an array of values and returns a Perhaps value
     * @return the new Perhaps instance
     */
    public static <T, R> Perhaps<R> zipArray(Function<? super Object[], ? extends R> zipper, Perhaps<? extends T>... sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        ObjectHelper.requireNonNull(zipper, "zipper is null");
        return onAssembly(new PerhapsZipArray<T, R>(sources, zipper));
    }

    // ----------------------------------------------------
    // Operators (stay)
    // ----------------------------------------------------

    /**
     * Signals the events of this or the other Perhaps whichever
     * signals first.
     * @param other the other Perhaps instance
     * @return the new Perhaps instance
     */
    @SuppressWarnings("unchecked")
    public final Perhaps<T> ambWith(Perhaps<? extends T> other) {
        return ambArray(this, other);
    }

    /**
     * Runs this Perhaps and then runs the other Nono source, only
     * emitting this Perhaps' success value if the other Nono source
     * completes normally.
     * @param other the other Nono source to run after this
     * @return the new Perhaps instance
     */
    public final Perhaps<T> andThen(Nono other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return onAssembly(new PerhapsAndThenNono<T>(this, other));
    }

    /**
     * Runs this Perhaps and emits its value followed by running
     * the other Publisher and emitting its values.
     * @param other the other Publisher to run after this
     * @return the new Flowable instance
     */
    public final Flowable<T> andThen(Publisher<? extends T> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return Flowable.concat(this, other);
    }

    /**
     * Runs this Perhaps and emits its value followed by running
     * the other Publisher and emitting its values.
     * @param other the other Publisher to run after this
     * @return the new Flowable instance
     */
    public final Flowable<T> concatWith(Publisher<? extends T> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return Flowable.concat(this, other);
    }

    /**
     * Merges this Perhaps with another Publisher and emits all their
     * values.
     * @param other the other Publisher source instance
     * @return the new Flowable instance
     */
    public final Flowable<T> mergeWith(Publisher<? extends T> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return Flowable.merge(this, other);
    }

    /**
     * Zips the value of this Perhaps with the other Perhaps through
     * a BiFunction.
     * @param <U> the value type of the other source
     * @param <R> the result type
     * @param other the other Perhaps source
     * @param zipper the function receiving each source value and should
     * return a value to be emitted
     * @return the new Perhaps instance
     */
    @SuppressWarnings("unchecked")
    public final <U, R> Perhaps<R> zipWith(Perhaps<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        ObjectHelper.requireNonNull(other, "other is null");
        ObjectHelper.requireNonNull(zipper, "zipper is null");
        return zipArray(Functions.toFunction(zipper), this, other);
    }

    /**
     * Maps the value of this Perhaps into another value (of possibly different
     * type).
     * @param <R> the result value type
     * @param mapper the function that receives the onNext value from this Perhaps
     * and returns another value
     * @return the new Perhaps instance
     */
    public final <R> Perhaps<R> map(Function<? super T, ? extends R> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return onAssembly(new PerhapsMap<T, R>(this, mapper));
    }

    /**
     * Maps the error from upstream into another Throwable error.
     * @param errorMapper the function that receives the upstream error and
     * returns a Throwable
     * @return the new Perhaps instance
     */
    public final Perhaps<T> mapError(Function<? super Throwable, ? extends Throwable> errorMapper) {
        ObjectHelper.requireNonNull(errorMapper, "errorMapper is null");
        return onAssembly(new PerhapsMapError<T>(this, errorMapper));
    }

    /**
     * Filters the value from upstream with a predicate and completes
     * if the filter didn't match it.
     * @param predicate the predicate receiving the upstream value and
     * returns true if it should be passed along.
     * @return the new Perhaps instance
     */
    public final Perhaps<T> filter(Predicate<? super T> predicate) {
        ObjectHelper.requireNonNull(predicate, "predicate is null");
        return onAssembly(new PerhapsFilter<T>(this, predicate));
    }

    /**
     * Ignore the element of this Perhaps.
     * @return the new Nono instance.
     */
    public final Nono ignoreElement() {
        return Nono.fromPublisher(this);
    }

    /**
     * Hides the identity of this Perhaps instance, including
     * its subscriber.
     * <p>This allows preventing cerain optimizations as well
     * for diagnostic purposes.
     * @return the new Perhaps instance
     */
    public final Perhaps<T> hide() {
        return onAssembly(new PerhapsHide<T>(this));
    }

    /**
     * Maps the upstream's value into another Perhaps and emits its
     * resulting events.
     * @param <R> the output value type
     * @param mapper the function that receives the upstream's value
     * and returns a Perhaps to be consumed.
     * @return the new Perhaps instance
     */
    public final <R> Perhaps<R> flatMap(Function<? super T, ? extends Perhaps<? extends R>> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return onAssembly(new PerhapsFlatMap<T, R>(this, mapper));
    }

    /**
     * Maps the upstream events into other Perhaps instances and emits
     * its resulting events.
     * <p>Note that only one of the onXXXMapper is called based on what
     * the upstream signals, i.e., the usual onNext + onComplete will pick the
     * Perhaps of the onSuccessMapper only and never the onCompleteMapper.
     * @param <R> the result value type
     * @param onSuccessMapper the function called for the upstream value
     * @param onErrorMapper the function called for the upstream error
     * @param onCompleteMapper the function called when the upstream is empty
     * @return the new Perhaps instance
     */
    public final <R> Perhaps<R> flatMap(Function<? super T, ? extends Perhaps<? extends R>> onSuccessMapper,
            Function<? super Throwable, ? extends Perhaps<? extends R>> onErrorMapper,
                    Supplier<? extends Perhaps<? extends R>> onCompleteMapper) {
        ObjectHelper.requireNonNull(onSuccessMapper, "onSuccessMapper is null");
        ObjectHelper.requireNonNull(onErrorMapper, "onErrorMapper is null");
        ObjectHelper.requireNonNull(onCompleteMapper, "onCompleteMapper is null");
        return onAssembly(new PerhapsFlatMapSignal<T, R>(this, onSuccessMapper, onErrorMapper, onCompleteMapper));
    }

    /**
     * Maps the upstream value into a Publisher and emits all of its events.
     * @param <R> the result value type
     * @param mapper the function that maps the success value into a Publisher that
     * gets subscribed to and streamed further
     * @return the new Flowable type
     */
    public final <R> Flowable<R> flatMapPublisher(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new PerhapsFlatMapPublisher<T, R>(this, mapper));
    }

    /**
     * Completes in case the upstream signals an error.
     * @return the new Perhaps instance
     */
    public final Perhaps<T> onErrorComplete() {
        return onAssembly(new PerhapsOnErrorReturnItem<T>(this, null));
    }

    /**
     * If the upstream signals an error, it is replaced by a signal
     * of the given item and normal completion.
     * @param item the item to signal in case of an error
     * @return the new Perhaps instance
     */
    public final Perhaps<T> onErrorReturnItem(T item) {
        ObjectHelper.requireNonNull(item, "item is null");
        return onAssembly(new PerhapsOnErrorReturnItem<T>(this, item));
    }

    /**
     * If the upstream signals an error, switch to the given fallback
     * Perhaps.
     * @param fallback the fallback to switch to in case of an error
     * @return the new Perhaps instance
     */
    public final Perhaps<T> onErrorResumeWith(Perhaps<? extends T> fallback) {
        ObjectHelper.requireNonNull(fallback, "fallback is null");
        return onAssembly(new PerhapsOnErrorResumeWith<T>(this, fallback));
    }

    /**
     * If the upstream signals an error, apply the given function to that
     * Throwable error and resume with the returned Perhaps.
     * @param fallbackSupplier the function that receives the upstream Throwable
     * and should return the fallback Perhaps that will be subscribed to as
     * a resumptions
     * @return the new Perhaps instance
     */
    public final Perhaps<T> onErrorResumeNext(Function<? super Throwable, ? extends Perhaps<? extends T>> fallbackSupplier) {
        ObjectHelper.requireNonNull(fallbackSupplier, "fallbackSupplier is null");
        return onAssembly(new PerhapsOnErrorResumeNext<T>(this, fallbackSupplier));
    }

    /**
     * Signals a TimeoutException if the Perhaps doesn't signal an item
     * within the specified time.
     * @param timeout the time to wait for an item
     * @param unit the unit of time
     * @return the new Perhaps instance
     */
    public final Perhaps<T> timeout(long timeout, TimeUnit unit) {
        return timeout(timeout, unit, Schedulers.computation());
    }

    /**
     * Signals a TimeoutException if the Perhaps doesn't signal an item
     * or (terminates) within the specified time.
     * @param timeout the time to wait for an item
     * @param unit the unit of time
     * @param scheduler the scheduler to wait on
     * @return the new Perhaps instance
     */
    public final Perhaps<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler) {
        return timeout(timer(timeout, unit, scheduler));
    }

    /**
     * Switch to the fallback Perhaps if this Perhaps doesn't signal an
     * item (or terminates) within the specified time.
     * @param timeout the time to wait for an item
     * @param unit the unit of time
     * @param fallback the Perhaps to switch to if this Perhaps times out
     * @return the new Perhaps instance
     */
    public final Perhaps<T> timeout(long timeout, TimeUnit unit, Perhaps<? extends T> fallback) {
        return timeout(timeout, unit, Schedulers.computation(), fallback);
    }

    /**
     * Switch to the fallback Perhaps if this Perhaps doesn't signal an
     * item (or terminates) within the specified time.
     * @param timeout the time to wait for an item
     * @param unit the unit of time
     * @param scheduler the scheduler to wait on
     * @param fallback the Perhaps to switch to if this Perhaps times out
     * @return the new Perhaps instance
     */
    public final Perhaps<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler, Perhaps<? extends T> fallback) {
        return timeout(timer(timeout, unit, scheduler), fallback);
    }

    /**
     * Signal a TimeoutException if the other Publisher signals an item or
     * completes before this Perhaps does.
     * @param other the other Publisher that signals the timeout
     * @return the new Perhaps instance
     */
    public final Perhaps<T> timeout(Publisher<?> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return onAssembly(new PerhapsTimeout<T>(this, other, null));
    }

    /**
     * Switch to the fallback Perhaps if the other Publisher signals an item or
     * completes before this Perhaps does.
     * @param other the other Publisher that signals the timeout
     * @param fallback the Perhaps to switch to in case of a timeout
     * @return the new Perhaps instance
     */
    public final Perhaps<T> timeout(Publisher<?> other, Perhaps<? extends T> fallback) {
        ObjectHelper.requireNonNull(other, "other is null");
        ObjectHelper.requireNonNull(fallback, "fallback is null");
        return onAssembly(new PerhapsTimeout<T>(this, other, fallback));
    }

    /**
     * Signal the given item if this Perhaps is empty.
     * @param item the item to signal
     * @return the new Perhaps instance
     */
    public final Perhaps<T> defaultIfEmpty(T item) {
        ObjectHelper.requireNonNull(item, "item is null");
        return onAssembly(new PerhapsDefaultIfEmpty<T>(this, item));
    }

    /**
     * Switch to the other Perhaps if this Perhaps is empty.
     * @param other the other Perhaps to switch to
     * @return the new Perhaps instance
     */
    public final Perhaps<T> switchIfEmpty(Perhaps<? extends T> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return onAssembly(new PerhapsSwitchIfEmpty<T>(this, other));
    }

    /**
     * Repeats this Perhaps indefinitely.
     * @return the new Flowable instance
     */
    public final Flowable<T> repeat() {
        return Flowable.fromPublisher(this).repeat();
    }

    /**
     * Repeats this Perhaps at most the given number of times.
     * @param times the number of times to repeat
     * @return the new Flowable instance
     */
    public final Flowable<T> repeat(long times) {
        return Flowable.fromPublisher(this).repeat(times);
    }

    /**
     * Repeats this Perhaps until the given boolean supplier returns true when an
     * repeat iteration of this Perhaps completes.
     * @param stop the supplier of a boolean value that should return true to
     * stop repeating.
     * @return the new Flowable instance
     */
    public final Flowable<T> repeat(BooleanSupplier stop) {
        return Flowable.fromPublisher(this).repeatUntil(stop);
    }

    /**
     * Repeats this Perhaps when the Publisher returned by the handler function emits
     * an item or terminates if this Publisher terminates.
     * @param handler the function that receives Flowable that emits an item
     * when this Perhaps completes and returns a Publisher that should emit an item
     * to trigger a repeat or terminate to trigger a termination.
     * @return the new Flowable instance
     */
    public final Flowable<T> repeatWhen(Function<? super Flowable<Object>, ? extends Publisher<?>> handler) {
        return Flowable.fromPublisher(this).repeatWhen(handler);
    }

    /**
     * Retry this Perhaps indefinitely if it fails.
     * @return the new Perhaps instance
     */
    public final Perhaps<T> retry() {
        return onAssembly(new PerhapsRetry<T>(this, Long.MAX_VALUE));
    }

    /**
     * Retry this Perhaps at most the given number of times if it fails.
     * @param times the number of times, Long.MAX_VALUE means indefinitely
     * @return the new Perhaps instance
     */
    public final Perhaps<T> retry(long times) {
        if (times < 0L) {
            throw new IllegalArgumentException("times >= 0 required but it was " + times);
        }
        return onAssembly(new PerhapsRetry<T>(this, times));
    }

    /**
     * Retry this Perhaps if the predicate returns true for the latest failure
     * Throwable.
     * @param predicate the predicate receiving the latest Throwable and
     * if returns true, the Perhaps is retried.
     * @return the new Perhaps instance
     */
    public final Perhaps<T> retry(Predicate<? super Throwable> predicate) {
        ObjectHelper.requireNonNull(predicate, "predicate is null");
        return onAssembly(new PerhapsRetryWhile<T>(this, predicate));
    }

    /**
     * Retry this Perhaps if the Publisher returned by the handler signals an item
     * in response to the failure Throwable.
     * @param handler the function that receives a Flowable that signals the
     * failure Throwable of this Perhaps and returns a Publisher which triggers a retry
     * or termination.
     * @return the new Perhaps instance
     */
    public final Perhaps<T> retryWhen(Function<? super Flowable<Throwable>, ? extends Publisher<?>> handler) {
        ObjectHelper.requireNonNull(handler, "handler is null");
        return onAssembly(new PerhapsRetryWhen<T>(this, handler));
    }

    /**
     * Subscribes to the upstream Perhaps and requests on the
     * specified Scheduler.
     * @param scheduler the scheduler to subscribe on
     * @return the new Perhaps instance
     */
    public final Perhaps<T> subscribeOn(Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return onAssembly(new PerhapsSubscribeOn<T>(this, scheduler));
    }

    /**
     * Observe the events of this Perhaps on the specified scheduler.
     * @param scheduler the scheduler to observe events on
     * @return the new Perhaps instance
     */
    public final Perhaps<T> observeOn(Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return onAssembly(new PerhapsObserveOn<T>(this, scheduler));
    }

    /**
     * If the downstream cancels, the upstream is cancelled on
     * the specified scheduler.
     * <p>Note that normal termination don't trigger cancellation.
     * @param scheduler the scheduler to unsubscribe on
     * @return the new Perhaps instance
     */
    public final Perhaps<T> unsubscribeOn(Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return onAssembly(new PerhapsUnsubscribeOn<T>(this, scheduler));
    }

    /**
     * Executes a callback when the upstream calls onSubscribe.
     * @param onSubscribe the consumer called with the upstream Subscription
     * @return the new Perhaps instance
     */
    public final Perhaps<T> doOnSubscribe(Consumer<? super Subscription> onSubscribe) {
        ObjectHelper.requireNonNull(onSubscribe, "onSubscribe is null");
        return onAssembly(new PerhapsDoOnLifecycle<T>(this,
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
     * Executes the callback when the downstream requests from this Perhaps.
     * @param onRequest the callback called with the request amount
     * @return the new Perhaps instance
     */
    public final Perhaps<T> doOnRequest(LongConsumer onRequest) {
        ObjectHelper.requireNonNull(onRequest, "onRequest is null");
        return onAssembly(new PerhapsDoOnLifecycle<T>(this,
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
     * @return the new Perhaps instance
     */
    public final Perhaps<T> doOnCancel(Action onCancel) {
        ObjectHelper.requireNonNull(onCancel, "onCancel is null");
        return onAssembly(new PerhapsDoOnLifecycle<T>(this,
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
     * @return the new Perhaps instance
     */
    public final Perhaps<T> doOnNext(Consumer<? super T> onNext) {
        ObjectHelper.requireNonNull(onNext, "onNext is null");
        return onAssembly(new PerhapsDoOnLifecycle<T>(this,
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
     * @return the new Perhaps instance
     */
    public final Perhaps<T> doAfterNext(Consumer<? super T> onAfterNext) {
        ObjectHelper.requireNonNull(onAfterNext, "onAfterNext is null");
        return onAssembly(new PerhapsDoOnLifecycle<T>(this,
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
     * @return the new Perhaps instance
     */
    public final Perhaps<T> doOnError(Consumer<? super Throwable> onError) {
        ObjectHelper.requireNonNull(onError, "onError is null");
        return onAssembly(new PerhapsDoOnLifecycle<T>(this,
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
     * @return the new Perhaps instance
     */
    public final Perhaps<T> doOnComplete(Action onComplete) {
        ObjectHelper.requireNonNull(onComplete, "onComplete is null");
        return onAssembly(new PerhapsDoOnLifecycle<T>(this,
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
     * Executes the callback after this Perhaps terminates and the downstream
     * is notified.
     * @param onAfterTerminate the callback to call after the downstream is notified
     * @return the new Perhaps instance
     */
    public final Perhaps<T> doAfterTerminate(Action onAfterTerminate) {
        ObjectHelper.requireNonNull(onAfterTerminate, "onAfterTerminate is null");
        return onAssembly(new PerhapsDoOnLifecycle<T>(this,
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
     * @return the new Perhaps instance
     */
    public final Perhaps<T> doFinally(Action onFinally) {
        ObjectHelper.requireNonNull(onFinally, "onFinally is null");
        return onAssembly(new PerhapsDoFinally<T>(this, onFinally));
    }

    /**
     * Delay the emission of the signals of this Perhaps by the
     * given amount of time.
     * @param delay the delay amount
     * @param unit the time unit of the delay
     * @return the new Perhaps instance
     */
    public final Perhaps<T> delay(long delay, TimeUnit unit) {
        return delay(delay, unit, Schedulers.computation());
    }

    /**
     * Delay the emission of the signals of this Perhaps by the
     * given amount of time.
     * @param delay the delay amount
     * @param unit the time unit of the delay
     * @param scheduler the scheduler to delay on
     * @return the new Perhaps instance
     */
    public final Perhaps<T> delay(long delay, TimeUnit unit, Scheduler scheduler) {
        return delay(timer(delay, unit, scheduler));
    }

    /**
     * Delay the emission of the signals of this Perhaps till
     * the other Publisher signals an item or completes.
     * @param other the other Publisher to delay with
     * @return the new Perhaps instance
     */
    public final Perhaps<T> delay(Publisher<?> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return onAssembly(new PerhapsDelayPublisher<T>(this, other));
    }

    /**
     * Delay the subscription to this Perhaps by the given time amount.
     * @param delay the amount to delay the subscription
     * @param unit the delay time unit
     * @return the new Perhaps instance
     */
    public final Perhaps<T> delaySubscription(long delay, TimeUnit unit) {
        return delaySubscription(timer(delay, unit));
    }

    /**
     * Delay the subscription to this Perhaps by the given time amount,
     * running on the specified Scheduler.
     * @param delay the amount to delay the subscription
     * @param unit the delay time unit
     * @param scheduler the scheduler to wait on
     * @return the new Perhaps instance
     */
    public final Perhaps<T> delaySubscription(long delay, TimeUnit unit, Scheduler scheduler) {
        return delaySubscription(timer(delay, unit, scheduler));
    }

    /**
     * Delay the subscription to this Perhaps until the other Publisher
     * signals an item or completes.
     * @param other the other Publisher that will trigger the actual
     * subscription to this Perhaps.
     * @return the new Perhaps instance
     */
    public final Perhaps<T> delaySubscription(Publisher<?> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return onAssembly(new PerhapsDelaySubscription<T>(this, other));
    }

    /**
     * Map the downstream Subscriber into an upstream Subscriber.
     * @param <R> the downstream value type
     * @param onLift the function called with the downstream's Subscriber and
     * should return a Subscriber to be subscribed to this Perhaps.
     * @return the new Perhaps type
     */
    public final <R> Perhaps<R> lift(Function<Subscriber<? super R>, Subscriber<? super T>> onLift) {
        ObjectHelper.requireNonNull(onLift, "onLift is null");
        return onAssembly(new PerhapsLift<T, R>(this, onLift));
    }

    /**
     * Applies the function, fluently to this Perhaps and returns the value it returns.
     * @param <R> the result type
     * @param converter the function receiving this Perhaps and returns a value to be returned
     * @return the value returned by the function
     */
    public final <R> R to(Function<? super Perhaps<T>, R> converter) {
        try {
            return converter.apply(this);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /**
     * Applies a function to this Perhaps and returns the Perhaps it returned.
     * @param <R> the result type
     * @param composer the function that receives this Perhaps and should return a Perhaps
     * @return the new Perhaps instance
     */
    public final <R> Perhaps<R> compose(Function<? super Perhaps<T>, ? extends Perhaps<R>> composer) {
        return to(composer);
    }

    /**
     * Try consuming this Perhaps until the other Publisher signals an item
     * or completes which then completes the Perhaps.
     * @param other the other Publisher instance
     * @return the new Perhaps instance
     */
    public final Perhaps<T> takeUntil(Publisher<?> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return onAssembly(new PerhapsTakeUntil<T>(this, other));
    }

    /**
     * Convert this Perhaps into a Flowable.
     * @return the new Flowable instance
     */
    public final Flowable<T> toFlowable() {
        return RxJavaPlugins.onAssembly(new PerhapsToFlowable<T>(this));
    }

    /**
     * Convert this Perhaps into an Observable.
     * @return the new Observable instance
     */
    public final Observable<T> toObservable() {
        return RxJavaPlugins.onAssembly(new PerhapsToObservable<T>(this));
    }

    /**
     * Convert this Perhaps into a Maybe.
     * @return the new Maybe instance
     */
    public final Maybe<T> toMaybe() {
        return RxJavaPlugins.onAssembly(new PerhapsToMaybe<T>(this));
    }

    /**
     * Caches the value or error event of the upstream Perhaps
     * and relays/replays it to Subscribers.
     * @return the new Perhaps instance
     *
     * @since 0.14.1
     */
    public final Perhaps<T> cache() {
        return onAssembly(new PerhapsCache<T>(this));
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
     * Implement this method to react to a Subscriber subscribing to this Perhaps.
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
     * Subscribe to this Perhaps and ignore any signal it produces.
     * @return the Disposable that allows cancelling the subscription
     */
    public final Disposable subscribe() {
        return subscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to this Perhaps and calls the onNext if this Perhaps succeeds.
     * @param onNext called when this Perhaps succeeds
     * @return the Disposable that allows cancelling the subscription
     */
    public final Disposable subscribe(Consumer<? super T> onNext) {
        return subscribe(onNext, Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to this Perhaps and calls the appropriate callback for the resulting signal.
     * @param onNext called when this Perhaps succeeds
     * @param onError called when this Perhaps fails
     * @return the Disposable that allows cancelling the subscription
     */
    public final Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError) {
        return subscribe(onNext, onError, Functions.EMPTY_ACTION);
    }

    /**
     * Subscribes to this Perhaps and calls the appropriate callback for the resulting signal.
     * @param onNext called when this Perhaps succeeds
     * @param onError called when this Perhaps fails
     * @param onComplete called when this Perhaps succeeds after the call to onNext
     * @return the Disposable that allows cancelling the subscription
     */
    public final Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Action onComplete) {
        LambdaSubscriber<T> s = new LambdaSubscriber<T>(onNext, onError, onComplete, Functions.REQUEST_MAX);
        subscribe(s);
        return s;
    }

    /**
     * Blocks until this Perhaps terminates and ignores the signals it produced.
     */
    public final void blockingSubscribe() {
        blockingSubscribe(Functions.emptyConsumer(), Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    /**
     * Blocks until this Perhaps terminates and calls the onNext with the success value.
     * @param onNext the callback to call when this Perhaps completes with a success value
     */
    public final void blockingSubscribe(Consumer<? super T> onNext) {
        blockingSubscribe(onNext, Functions.ERROR_CONSUMER, Functions.EMPTY_ACTION);
    }

    /**
     * Blocks until this Perhaps terminates and calls the onNext with the success value
     * or calls the onError with the error Throwable.
     * @param onNext the callback to call when this Perhaps completes with a success value
     * @param onError the callback to call when this Perhaps fails with an error
     */
    public final void blockingSubscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError) {
        blockingSubscribe(onNext, onError, Functions.EMPTY_ACTION);
    }

    /**
     * Blocks until this Perhaps produces its terminal signal and calls the
     * appropriate callback(s) based on the signal type.
     * @param onNext called when the Perhaps succeeds
     * @param onError called when the Perhaps fails
     * @param onComplete called when the Perhaps succeeds after the call to onNext.
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
     * Blockingly awaits indefinitely the success value of this Perhaps or rethrows
     * its error (wrapped into a RuntimeException if necessary).
     * @return the success value of this Perhaps
     */
    public final T blockingGet() {
        BlockingGetSubscriber<T> s = new BlockingGetSubscriber<T>();
        subscribe(s);
        return s.blockingGet();
    }

    /**
     * Blockingly awaits at most the given timeout for the success
     * value of this Perhaps or rethrows
     * its error (wrapped into a RuntimeException if necessary).
     * @param timeout the time to wait for a success value
     * @param unit the time unit of the timeout
     * @return the success value of this Perhaps
     */
    public final T blockingGet(long timeout, TimeUnit unit) {
        BlockingGetSubscriber<T> s = new BlockingGetSubscriber<T>();
        subscribe(s);
        return s.blockingGet(timeout, unit);
    }

    /**
     * Creates a TestSubscriber and subscribes it to this Perhaps.
     * @return the new TestSubscriber instance
     */
    public final TestSubscriber<T> test() {
        return test(Long.MAX_VALUE, false);
    }

    /**
     * Creates a TestSubscriber, optionally cancels it, and subscribes
     * it to this Perhaps.
     * @param cancel if true, the TestSubscriber will be cancelled before
     * subscribing to this Perhaps
     * @return the new TestSubscriber instance
     */
    public final TestSubscriber<T> test(boolean cancel) {
        return test(Long.MAX_VALUE, cancel);
    }

    /**
     * Creates a TestSubscriber with the given initial request and
     * subscribes it to this Perhaps.
     * @param initialRequest the initial request amount, non-negative
     * @return the new TestSubscriber
     */
    public final TestSubscriber<T> test(long initialRequest) {
        return test(initialRequest, false);
    }

    /**
     * Creates a TestSubscriber with the given initial request,
     * optionally cancels it, and subscribes it to this Perhaps.
     * @param initialRequest the initial request amount, non-negative
     * @param cancel if true, the TestSubscriber will be cancelled before
     * subscribing to this Perhaps
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
     * Converts this Perhaps into a Future and signals its single
     * value or null if this Perhaps is empty.
     * @return the new Future instance
     */
    public final Future<T> toFuture() {
        FuturePerhapsSubscriber<T> fs = new FuturePerhapsSubscriber<T>();
        subscribe(fs);
        return fs;
    }
}
