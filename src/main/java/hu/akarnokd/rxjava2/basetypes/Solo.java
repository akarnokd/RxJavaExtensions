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
import io.reactivex.internal.subscribers.LambdaSubscriber;
import io.reactivex.internal.util.ExceptionHelper;
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
    public static <T> Solo<T> error(Callable<? extends Throwable> errorSupplier) {
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
        return onAssembly(new SoloCallable<T>(callable));
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
    public static <T> Solo<T> defer(Callable<? extends Solo<T>> supplier) {
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

    public static <T, R> Solo<T> using(
            Callable<R> resourceSupplier,
            Function<? super R, ? extends Solo<T>> sourceSupplier,
            Consumer<? super R> disposer
    ) {
        return using(resourceSupplier, sourceSupplier, disposer, true);
    }

    public static <T, R> Solo<T> using(
            Callable<R> resourceSupplier,
            Function<? super R, ? extends Solo<T>> sourceSupplier,
            Consumer<? super R> disposer,
            boolean eager
    ) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T, R> Solo<R> zip(
            Iterable<? extends Solo<? extends T>> sources,
            Function<? super Object[], ? extends R> zipper
    ) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T, R> Solo<R> zipArray(
            Function<? super Object[], ? extends R> zipper,
            Solo<? extends T>... sources
    ) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    // ----------------------------------------------------
    // Instance operators (stay)
    // ----------------------------------------------------

    public final Solo<T> ambWith(Solo<T> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> andThen(Nono other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Flowable<T> andThen(Publisher<? extends T> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Flowable<T> concatWith(Solo<T> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Flowable<T> mergeWith(Solo<T> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final <U, R> Solo<R> zipWith(Solo<U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        // TODO implement
        throw new UnsupportedOperationException();
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
        // TODO implement
        throw new UnsupportedOperationException();
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
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> onErrorReturnItem(T item) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> onErrorResumeWith(Solo<T> next) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> onErrorResumeNext(Function<? super Throwable, ? extends Solo<T>> errorHandler) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final <R> Solo<T> flatMap(
            Function<? super T, ? extends Solo<? extends R>> mapper) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final <R> Solo<T> flatMap(
            Function<? super T, ? extends Solo<? extends R>> onNextMapper,
            Function<? super Throwable, ? extends Solo<? extends R>> onErrorMapper,
            Callable<? extends Solo<? extends R>> onCompleteMapper
                    ) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final <R> Flowable<R> flatMapPublisher(
            Function<? super T, ? extends Publisher<? extends R>> mapper
    ) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> takeUntil(Publisher<?> other) {
        // TODO implement
        throw new UnsupportedOperationException();
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
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> delaySubscription(Publisher<?> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> delaySubscription(long delay, TimeUnit unit) {
        return delaySubscription(timer(delay, unit));
    }

    public final Solo<T> delaySubscription(long delay, TimeUnit unit, Scheduler scheduler) {
        return delaySubscription(timer(delay, unit, scheduler));
    }

    public final Flowable<T> toFlowable() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Flowable<T> toObservabe() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Single<T> toSingle() {
        // TODO implement
        throw new UnsupportedOperationException();
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
                Functions.ERROR_CONSUMER,
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
                Functions.ERROR_CONSUMER,
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
     * @return the new Nono instance
     */
    public final Solo<T> doOnCancel(Action onCancel) {
        ObjectHelper.requireNonNull(onCancel, "onCancel is null");
        return onAssembly(new SoloDoOnLifecycle<T>(this,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.ERROR_CONSUMER,
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
                Functions.ERROR_CONSUMER,
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
                Functions.ERROR_CONSUMER,
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
                Functions.ERROR_CONSUMER,
                onComplete,
                Functions.EMPTY_ACTION,
                Functions.emptyConsumer(),
                Functions.EMPTY_LONG_CONSUMER,
                Functions.EMPTY_ACTION
        ));
    }


    /**
     * Executes the callback after this Nono terminates and the downstream
     * is notified.
     * @param onAfterTerminate the callback to call after the downstream is notified
     * @return the new Sono instance
     */
    public final Solo<T> doAfterTerminate(Action onAfterTerminate) {
        ObjectHelper.requireNonNull(onAfterTerminate, "onAfterTerminate is null");
        return onAssembly(new SoloDoOnLifecycle<T>(this,
                Functions.emptyConsumer(),
                Functions.emptyConsumer(),
                Functions.ERROR_CONSUMER,
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
     * @return the new Sono instance
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

    public final Flowable<T> repeat() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Flowable<T> repeat(long times) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Flowable<T> repeat(BooleanSupplier stop) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Flowable<T> repeatWhen(Function<? super Flowable<Object>, ? extends Publisher<?>> handler) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> retry() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> retry(long times) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> retry(Predicate<? super Throwable> predicate) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> retryWhen(Function<? super Flowable<Throwable>, ? extends Publisher<?>> handler) {
        // TODO implement
        throw new UnsupportedOperationException();
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
        LambdaSubscriber<T> s = new LambdaSubscriber<T>(onNext, onError, onComplete, Functions.<Subscription>emptyConsumer());
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
}
