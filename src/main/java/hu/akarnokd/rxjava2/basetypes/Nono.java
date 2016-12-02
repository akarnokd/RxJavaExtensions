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
import io.reactivex.annotations.SchedulerSupport;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.util.*;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

/**
 * Represents the base reactive class with fluent API for Publisher-based, 
 * no-item just onError or onComplete source.
 * <p>
 * Since this type never emits any value, the implementations ignore
 * the downstream request and emit the terminal events even if there was
 * no request (which is allowed by the Reactive-Streams specification).
 * <p>
 * Since there is no bottom type in Java (that is T is a subtype of all other types),
 * Nono implements the Publisher interface via the Void type parameter.
 * @since 0.11.0
 */
public abstract class Nono implements Publisher<Void> {

    private static volatile Function<Nono, Nono> onAssemblyHandler;
    
    /**
     * Returns the default buffer or prefetch size.
     * @return the buffer or prefetch size
     */
    public static int bufferSize() {
        return Flowable.bufferSize();
    }
    
    /**
     * Optionally apply a function to the raw source and return a
     * potentially modified Nono instance.
     * @param source the source to apply to
     * @return the possibly wrapped Nono instance
     */
    protected static Nono onAssembly(Nono source) {
        Function<Nono, Nono> f = onAssemblyHandler;
        if (f != null) {
            try {
                return ObjectHelper.requireNonNull(f.apply(source), "The onAssemblyHandler returned a null Nono");
            } catch (Throwable ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }
        return source;
    }

    public static Function<Nono, Nono> getOnAssemblyHandler() {
        return onAssemblyHandler;
    }

    public static void setOnAssemblyHandler(Function<Nono, Nono> handler) {
        onAssemblyHandler = handler;
    }
    
    // -----------------------------------------------------------
    // Static factories (enter)
    // -----------------------------------------------------------
    
    public static Nono complete() {
        return onAssembly(NonoComplete.INSTANCE);
    }

    public static Nono error(Throwable ex) {
        ObjectHelper.requireNonNull(ex, "ex is null");
        return onAssembly(new NonoError(ex));
    }

    public static Nono error(Callable<? extends Throwable> errorSupplier) {
        ObjectHelper.requireNonNull(errorSupplier, "errorSupplier is null");
        return onAssembly(new NonoErrorSupplier(errorSupplier));
    }

    public static Nono defer(Callable<? extends Nono> supplier) {
        ObjectHelper.requireNonNull(supplier, "supplier is null");
        return onAssembly(new NonoDefer(supplier));
    }

    public static Nono fromAction(Action action) {
        ObjectHelper.requireNonNull(action, "action is null");
        return onAssembly(new NonoFromAction(action));
    }
    
    public static Nono fromFuture(Future<?> future) {
        ObjectHelper.requireNonNull(future, "future is null");
        return onAssembly(new NonoFromFuture(future, 0L, TimeUnit.NANOSECONDS));
    }

    public static Nono fromFuture(Future<?> future, long timeout, TimeUnit unit) {
        ObjectHelper.requireNonNull(future, "future is null");
        ObjectHelper.requireNonNull(unit, "unit is null");
        return onAssembly(new NonoFromFuture(future, timeout, unit));
    }
    
    public static Nono amb(Iterable<? extends Nono> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return onAssembly(new NonoAmbIterable(sources));
    }

    public static Nono ambArray(Nono... sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return onAssembly(new NonoAmbArray(sources));
    }

    public static Nono concat(Iterable<? extends Nono> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return onAssembly(new NonoConcatIterable(sources, false));
    }

    public static Nono concat(Publisher<? extends Nono> sources) {
        return concat(sources, 2);
    }

    public static Nono concat(Publisher<? extends Nono> sources, int prefetch) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return onAssembly(new NonoConcat(sources, prefetch, ErrorMode.IMMEDIATE));
    }

    public static Nono concatArray(Nono... sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return onAssembly(new NonoConcatArray(sources, false));
    }
    
    public static Nono concatDelayError(Iterable<? extends Nono> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return onAssembly(new NonoConcatIterable(sources, true));
    }

    public static Nono concatDelayError(Publisher<? extends Nono> sources) {
        return concatDelayError(sources, 2, true);
    }

    public static Nono concatDelayError(Publisher<? extends Nono> sources, int prefetch, boolean tillTheEnd) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        return onAssembly(new NonoConcat(sources, prefetch, tillTheEnd ? ErrorMode.END : ErrorMode.BOUNDARY));
    }
    
    public static Nono concatArrayDelayError(Nono... sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return onAssembly(new NonoConcatArray(sources, true));
    }

    public static Nono merge(Iterable<? extends Nono> sources) {
        return merge(sources, Integer.MAX_VALUE);
    }

    public static Nono merge(Iterable<? extends Nono> sources, int maxConcurrency) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static Nono merge(Publisher<? extends Nono> sources) {
        return merge(sources, Integer.MAX_VALUE);
    }

    public static Nono merge(Publisher<? extends Nono> sources, int maxConcurrency) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static Nono mergeArray(Nono... sources) {
        return mergeArray(Integer.MAX_VALUE, sources);
    }

    public static Nono mergeArray(int maxConcurrency, Nono... sources) {
        // TODO implement
        throw new UnsupportedOperationException();
    }


    public static Nono mergeDelayError(Iterable<? extends Nono> sources) {
        return mergeDelayError(sources, Integer.MAX_VALUE);
    }

    public static Nono mergeDelayError(Iterable<? extends Nono> sources, int maxConcurrency) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static Nono mergeDelayError(Publisher<? extends Nono> sources) {
        return mergeDelayError(sources, Integer.MAX_VALUE);
    }

    public static Nono mergeDelayError(Publisher<? extends Nono> sources, int maxConcurrency) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static Nono mergeArrayDelayError(Nono... sources) {
        return mergeArrayDelayError(bufferSize(), sources);
    }

    public static Nono mergeArrayDelayError(int maxConcurrency, Nono... sources) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static Nono timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }

    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static Nono timer(long delay, TimeUnit unit, Scheduler scheduler) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return onAssembly(new NonoTimer(delay, unit, scheduler));
    }
    
    public static <R> Nono using(Callable<R> resourceSupplier, Function<? super R, ? extends Nono> sourceSupplier,
            Consumer<? super R> disposer) {
        return using(resourceSupplier, sourceSupplier, disposer, true);
    }

    public static <R> Nono using(Callable<R> resourceSupplier, Function<? super R, ? extends Nono> sourceSupplier,
            Consumer<? super R> disposer, boolean eager) {
        // TODO implement
        throw new UnsupportedOperationException();
    }
    
    public static Nono fromPublisher(Publisher<?> source) {
        // TODO implement
        throw new UnsupportedOperationException();
    }
    
    public static Nono fromSingle(SingleSource<?> source) {
        // TODO implement
        throw new UnsupportedOperationException();
    }
    
    public static Nono fromMaybe(MaybeSource<?> source) {
        // TODO implement
        throw new UnsupportedOperationException();
    }
    
    public static Nono fromCompletable(Completable source) {
        // TODO implement
        throw new UnsupportedOperationException();
    }
    
    public static Nono fromObservable(ObservableSource<?> source) {
        // TODO implement
        throw new UnsupportedOperationException();
    }
    
    // -----------------------------------------------------------
    // Instance operators (stay)
    // -----------------------------------------------------------
    
    public final <T> Flowable<T> andThen(Publisher<? extends T> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono andThen(Nono other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono delay(long delay, TimeUnit unit) {
        return delay(delay, unit, Schedulers.computation());
    }

    public final Nono delay(long delay, TimeUnit unit, Scheduler scheduler) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono delaySubscription(Publisher<?> other) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono delaySubscription(long delay, TimeUnit unit) {
        return delaySubscription(timer(delay, unit));
    }

    public final Nono delaySubscription(long delay, TimeUnit unit, Scheduler scheduler) {
        return delaySubscription(timer(delay, unit, scheduler));
    }
    
    public final Nono timeout(long delay, TimeUnit unit) {
        return timeout(delay, unit, Schedulers.computation());
    }
    
    public final Nono timeout(long delay, TimeUnit unit, Nono fallback) {
        return timeout(delay, unit, Schedulers.computation(), fallback);
    }

    public final Nono timeout(long delay, TimeUnit unit, Scheduler scheduler) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono timeout(long delay, TimeUnit unit, Scheduler scheduler, Nono fallback) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono onErrorComplete() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono onErrorResumeNext(Function<? super Throwable, ? extends Nono> errorHandler) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono mapError(Function<? super Throwable, ? extends Throwable> mapper) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final <T> Flowable<T> flatMap(Function<? super Throwable, ? extends Publisher<? extends T>> onErrorMapper,
            Callable<? extends Publisher<? extends T>> onCompleteMapper) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono compose(Function<? super Nono, ? extends Nono> composer) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final <R> R to(Function<? super Nono, R> converter) {
        try {
            return converter.apply(this);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }
    
    public final Nono lift(Function<Subscriber<? super Void>, Subscriber<? super Void>> lifter) {
        // TODO implement
        throw new UnsupportedOperationException();
    }
    
    @SuppressWarnings("unchecked")
    public final <T> Flowable<T> toFlowable() {
        if (this instanceof FuseToFlowable) {
            return ((FuseToFlowable<T>)this).fuseToFlowable();
        }
        // TODO implement
        throw new UnsupportedOperationException();
    }
    
    @SuppressWarnings("unchecked")
    public final <T> Observable<T> toObservable() {
        if (this instanceof FuseToObservable) {
            return ((FuseToObservable<T>)this).fuseToObservable();
        }
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Completable toCompletable() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final <T> Maybe<T> toMaybe() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono subscribeOn(Scheduler scheduler) {
        return onAssembly(new NonoSubscribeOn(this, scheduler));
    }
    
    public final Nono observeOn(Scheduler scheduler) {
        return onAssembly(new NonoObserveOn(this, scheduler));
    }
    
    public final Nono unsubscribeOn(Scheduler scheduler) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono doOnComplete(Action action) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono doOnError(Consumer<? super Throwable> error) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono doAfterComplete(Action action) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono doAfterTerminate(Action action) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono doFinally(Action action) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final Nono doOnCancel(Action action) {
        // TODO implement
        throw new UnsupportedOperationException();
    }
    // -----------------------------------------------------------
    // Consumers and subscribers (leave)
    // -----------------------------------------------------------
    
    @Override
    public final void subscribe(Subscriber<? super Void> s) {
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
     * Implement this method to signal the terminal events to the given subscriber.
     * @param s the downstream subscriber, not null
     */
    protected abstract void subscribeActual(Subscriber<? super Void> s);
    
    /**
     * Subscribe with the given subscriber and return the same subscriber, allowing
     * chaining methods on it or fluently reusing the instance.
     * @param <T> the target value type of the subscriber
     * @param <E> the subscriber's (sub)type
     * @param subscriber the subscriber to subscribe with, not null
     * @return the subscriber
     */
    @SuppressWarnings("unchecked")
    public final <T, E extends Subscriber<T>> E subscribeWith(E subscriber) {
        subscribe((Subscriber<Object>)subscriber);
        return subscriber;
    }
    
    public final TestSubscriber<Void> test() {
        TestSubscriber<Void> ts = new TestSubscriber<Void>();
        subscribe(ts);
        return ts;
    }
    
    public final TestSubscriber<Void> test(boolean cancelled) {
        TestSubscriber<Void> ts = new TestSubscriber<Void>();
        if (cancelled) {
            ts.cancel();
        }
        subscribe(ts);
        return ts;
    }
    
    public final Throwable blockingAwait() {
        if (this instanceof Callable) {
            try {
                ((Callable<?>)this).call();
                return null;
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                return ex;
            }
        }
        // TODO implement
        throw new UnsupportedOperationException();
    }
    
    public final Throwable blockingAwait(long timeout, TimeUnit unit) {
        if (this instanceof Callable) {
            try {
                ((Callable<?>)this).call();
                return null;
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                return ex;
            }
        }
        // TODO implement
        throw new UnsupportedOperationException();
    }
    
    public final Disposable subscribe(Action onComplete) {
        return subscribe(onComplete, Functions.ERROR_CONSUMER);
    }

    public final Disposable subscribe(Action onComplete, Consumer<? super Throwable> onError) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final void blockingSubscribe(Action onComplete) {
        blockingSubscribe(onComplete, Functions.ERROR_CONSUMER);
    }

    public final void blockingSubscribe(Action onComplete, Consumer<? super Throwable> onError) {
        // TODO implement
        throw new UnsupportedOperationException();
    }
}
