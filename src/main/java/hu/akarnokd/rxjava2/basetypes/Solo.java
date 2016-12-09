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

    public final <R> Solo<R> map(Function<? super T, ? extends R> mapper) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Perhaps<T> filter(Predicate<? super T> predicate) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> timeout(long timeout, TimeUnit unit) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> timeout(long timeout, TimeUnit unit, Solo<T> fallback) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler, Solo<T> fallback) {
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

    public final Solo<T> delay(long delay, TimeUnit unit) {
        return delay(timer(delay, unit));
    }

    public final Solo<T> delay(long delay, TimeUnit unit, Scheduler scheduler) {
        return delay(timer(delay, unit, scheduler));
    }

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

    public final Solo<T> doOnSubscribe(Consumer<? super Subscription> onSubscribe) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> doOnRequest(LongConsumer onRequest) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> doOnCancel(Action onCancel) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> doOnNext(Consumer<? super T> onNext) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> doAfterNext(Consumer<? super T> onAfterNext) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> doOnError(Consumer<? super Throwable> onError) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> doOnComplete(Action onComplete) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> doAfterTerminate(Action onAfterTerminate) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> doFinally(Action onFinally) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono ignoreElement() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> hide() {
        // TODO implement
        throw new UnsupportedOperationException();
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

    public final <R> Solo<R> lift(Function<Subscriber<? super R>, Subscriber<? super T>> onLift) {
        // TODO implement
        throw new UnsupportedOperationException();
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

    public final Solo<T> mapError(Function<? super Throwable, ? extends Throwable> errorMapper) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> subscribeOn(Scheduler scheduler) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> observeOn(Scheduler scheduler) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Solo<T> unsubscribeOn(Scheduler scheduler) {
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

    public final TestSubscriber<T> test() {
        return test(Long.MAX_VALUE, false);
    }

    public final TestSubscriber<T> test(boolean cancel) {
        return test(Long.MAX_VALUE, cancel);
    }

    public final TestSubscriber<T> test(long initialRequest) {
        return test(initialRequest, false);
    }

    public final TestSubscriber<T> test(long initialRequest, boolean cancel) {
        TestSubscriber<T> ts = new TestSubscriber<T>(initialRequest);
        if (cancel) {
            ts.cancel();
        }
        subscribe(ts);
        return ts;
    }
}
