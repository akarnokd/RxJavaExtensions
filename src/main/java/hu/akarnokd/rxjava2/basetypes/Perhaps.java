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
import io.reactivex.internal.operators.flowable.*;
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

    public static <T> Perhaps<T> create(MaybeOnSubscribe<T> onSubscribe) {
        // TODO implement
        throw new UnsupportedOperationException();
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

    public static <T> Perhaps<T> error(Throwable error) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Perhaps<T> error(Callable<? extends Throwable> errorSupplier) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a Perhaps that never signals any item or terminal event.
     * @param <T> the value type
     * @return the shared Perhaps instance
     */
    public static <T> Perhaps<T> never() {
        return onAssembly(PerhapsNever.<T>instance());
    }

    public static <T> Perhaps<T> fromCallable(Callable<? extends T> callable) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Perhaps<T> fromAction(Action action) {
        // TODO implement
        throw new UnsupportedOperationException();
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

    public static <T> Perhaps<T> fromPublisher(Publisher<? extends T> source) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Perhaps<T> fromObservable(ObservableSource<? extends T> source) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Perhaps<T> fromSingle(SingleSource<? extends T> source) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Perhaps<T> fromMaybe(MaybeSource<? extends T> source) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Perhaps<T> fromCompletable(CompletableSource source) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Perhaps<T> defer(Callable<? extends Perhaps<? extends T>> onDefer) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Perhaps<T> amb(Iterable<? extends Perhaps<? extends T>> sources) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T> Perhaps<T> ambArray(Perhaps<? extends T>... sources) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    // TODO javadoc
    public static <T> Flowable<T> concat(Iterable<? extends Perhaps<? extends T>> sources) {
        return Flowable.concat(sources);
    }

    // TODO javadoc
    public static <T> Flowable<T> concat(Publisher<? extends Perhaps<? extends T>> sources) {
        return Flowable.concat(sources);
    }

    // TODO javadoc
    public static <T> Flowable<T> concat(Publisher<? extends Perhaps<? extends T>> sources, int prefetch) {
        return Flowable.concat(sources, prefetch);
    }

    // TODO javadoc
    public static <T> Flowable<T> concatArray(Perhaps<? extends T>... sources) {
        return Flowable.concatArray(sources);
    }

    // TODO javadoc
    public static <T> Flowable<T> concatDelayError(Iterable<? extends Perhaps<? extends T>> sources) {
        return Flowable.concatDelayError(sources);
    }

    // TODO javadoc
    public static <T> Flowable<T> concatDelayError(Publisher<? extends Perhaps<? extends T>> sources) {
        return Flowable.concatDelayError(sources);
    }

    // TODO javadoc
    public static <T> Flowable<T> concatDelayError(Publisher<? extends Perhaps<? extends T>> sources, int prefetch) {
        return Flowable.concatDelayError(sources, prefetch, false);
    }

    // TODO javadoc
    public static <T> Flowable<T> concatDelayError(Publisher<? extends Perhaps<? extends T>> sources, int prefetch, boolean tillTheEnd) {
        return Flowable.concatDelayError(sources, prefetch, tillTheEnd);
    }

    // TODO javadoc
    public static <T> Flowable<T> concatArrayDelayError(Perhaps<? extends T>... sources) {
        return Flowable.concatArrayDelayError(sources);
    }

    // TODO javadoc
    public static <T> Flowable<T> merge(Iterable<? extends Perhaps<? extends T>> sources) {
        return Flowable.merge(sources);
    }

    // TODO javadoc
    public static <T> Flowable<T> merge(Iterable<? extends Perhaps<? extends T>> sources, int maxConcurrency) {
        return Flowable.merge(sources, maxConcurrency);
    }

    // TODO javadoc
    public static <T> Flowable<T> merge(Publisher<? extends Perhaps<? extends T>> sources) {
        return Flowable.merge(sources);
    }

    // TODO javadoc
    public static <T> Flowable<T> merge(Publisher<? extends Perhaps<? extends T>> sources, int maxConcurrency) {
        return Flowable.merge(sources, maxConcurrency);
    }

    // TODO javadoc
    public static <T> Flowable<T> mergeArray(Perhaps<? extends T>... sources) {
        return Flowable.mergeArray(sources);
    }

    // TODO javadoc
    public static <T> Flowable<T> mergeArray(int maxConcurrency, Perhaps<? extends T>... sources) {
        return Flowable.mergeArray(maxConcurrency, 1, sources);
    }

    // TODO javadoc
    public static <T> Flowable<T> mergeDelayError(Iterable<? extends Perhaps<? extends T>> sources) {
        return Flowable.mergeDelayError(sources);
    }

    // TODO javadoc
    public static <T> Flowable<T> mergeDelayError(Iterable<? extends Perhaps<? extends T>> sources, int maxConcurrency) {
        return Flowable.mergeDelayError(sources, maxConcurrency);
    }

    // TODO javadoc
    public static <T> Flowable<T> mergeDelayError(Publisher<? extends Perhaps<? extends T>> sources) {
        return Flowable.mergeDelayError(sources);
    }

    // TODO javadoc
    public static <T> Flowable<T> mergeDelayError(Publisher<? extends Perhaps<? extends T>> sources, int maxConcurrency) {
        return Flowable.mergeDelayError(sources, maxConcurrency);
    }

    // TODO javadoc
    public static <T> Flowable<T> mergeArrayDelayError(Perhaps<? extends T>... sources) {
        return Flowable.mergeArrayDelayError(sources);
    }

    // TODO javadoc
    public static <T> Flowable<T> mergeArrayDelayError(int maxConcurrency, Perhaps<? extends T>... sources) {
        return Flowable.mergeArrayDelayError(maxConcurrency, 1, sources);
    }

    public static Perhaps<Long> timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }

    public static Perhaps<Long> timer(long delay, TimeUnit unit, Scheduler scheduler) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    // TODO javadoc
    public static <T, R> Perhaps<T> using(Callable<R> resourceSupplier,
            Function<? super R, ? extends Perhaps<? extends T>> sourceSupplier,
            Consumer<? super R> disposer) {
        return using(resourceSupplier, sourceSupplier, disposer, true);
    }

    public static <T, R> Perhaps<T> using(Callable<R> resourceSupplier,
            Function<? super R, ? extends Perhaps<? extends T>> sourceSupplier,
            Consumer<? super R> disposer, boolean eager) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T, R> Perhaps<R> zip(Iterable<? extends Perhaps<? extends T>> sources, Function<? extends Object[], ? extends R> zipper) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static <T, R> Perhaps<R> zipArray(Function<? extends Object[], ? extends R> zipper, Perhaps<? extends T>... sources) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    // ----------------------------------------------------
    // Operators (stay)
    // ----------------------------------------------------

    public final Perhaps<T> ambWith(Perhaps<? extends T> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> andThen(Nono other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Flowable<T> andThen(Publisher<? extends T> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Flowable<T> concatWith(Publisher<? extends T> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Flowable<T> mergeWith(Publisher<? extends T> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final <U, R> Perhaps<R> zipWith(Perhaps<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final <R> Perhaps<R> map(Function<? super T, ? extends R> mapper) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> mapError(Function<? super Throwable, ? extends Throwable> errorMapper) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> filter(Predicate<? super T> predicate) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono ignoreElement() {
        // TODO implement
        throw new UnsupportedOperationException();
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

    public final <R> Perhaps<R> flatMap(Function<? super T, ? extends Perhaps<? extends R>> mapper) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final <R> Perhaps<R> flatMap(Function<? super T, ? extends Perhaps<? extends R>> onSuccessMapper,
            Function<? super Throwable, ? extends Perhaps<? extends R>> onErrorMapper,
            Callable<? extends Perhaps<? extends R>> onCompleteMapper) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final <R> Flowable<R> flatMapPublisher(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> onErrorComplete() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> onErrorReturnItem(T item) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> onErrorResumeWith(Perhaps<? extends T> fallback) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> onErrorResumeNext(Function<? super Throwable, ? extends Perhaps<? extends T>> fallbackSupplier) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> timeout(long timeout, TimeUnit unit) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> timeout(long timeout, TimeUnit unit, Perhaps<? extends T> fallback) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler, Perhaps<? extends T> fallback) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> timeout(Publisher<?> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> timeout(Publisher<?> other, Perhaps<? extends T> fallback) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> defaultIfEmpty(T item) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> switchIfEmpty(Perhaps<? extends T> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    /**
     * Repeats this Perhaps indefinitely.
     * @return the new Flowable instance
     */
    public final Flowable<T> repeat() {
        return RxJavaPlugins.onAssembly(new FlowableRepeat<T>(this, Long.MAX_VALUE));
    }

    /**
     * Repeats this Perhaps at most the given number of times.
     * @param times the number of times to repeat
     * @return the new Flowable instance
     */
    public final Flowable<T> repeat(long times) {
        if (times < 0L) {
            throw new IllegalArgumentException("times >= 0 required but it was " + times);
        }
        return RxJavaPlugins.onAssembly(new FlowableRepeat<T>(this, times));
    }

    /**
     * Repeats this Perhaps until the given boolean supplier returns true when an
     * repeat iteration of this Perhaps completes.
     * @param stop the supplier of a boolean value that should return true to
     * stop repeating.
     * @return the new Flowable instance
     */
    public final Flowable<T> repeat(BooleanSupplier stop) {
        ObjectHelper.requireNonNull(stop, "stop is null");
        return RxJavaPlugins.onAssembly(new FlowableRepeatUntil<T>(this, stop));
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
        ObjectHelper.requireNonNull(handler, "handler is null");
        return RxJavaPlugins.onAssembly(new FlowableRepeatWhen<T>(this, handler));
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

    public final Perhaps<T> subscribeOn(Scheduler scheduler) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> observeOn(Scheduler scheduler) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> unsubscribeOn(Scheduler scheduler) {
        // TODO implement
        throw new UnsupportedOperationException();
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

    public final Perhaps<T> delay(long delay, TimeUnit unit) {
        return delay(delay, unit, Schedulers.computation());
    }

    public final Perhaps<T> delay(long delay, TimeUnit unit, Scheduler scheduler) {
        return delay(timer(delay, unit, scheduler));
    }

    public final Perhaps<T> delay(Publisher<?> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> delaySubscription(long delay, TimeUnit unit) {
        return delaySubscription(timer(delay, unit));
    }

    public final Perhaps<T> delaySubscription(long delay, TimeUnit unit, Scheduler scheduler) {
        return delaySubscription(timer(delay, unit, scheduler));
    }

    public final Perhaps<T> delaySubscription(Publisher<?> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final <R> Perhaps<R> lift(Function<Subscriber<? super R>, Subscriber<? super T>> onLift) {
        // TODO implement
        throw new UnsupportedOperationException();
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

    public final Perhaps<T> takeUnit(Publisher<?> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Flowable<T> toFlowable() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Observable<T> toObservable() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Maybe<T> toMaybe() {
        // TODO implement
        throw new UnsupportedOperationException();
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
