/*
 * Copyright 2016-2017 David Karnok
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

package hu.akarnokd.rxjava2.operators;

import java.util.*;
import java.util.concurrent.*;

import org.reactivestreams.Publisher;

import io.reactivex.*;
import io.reactivex.annotations.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;
import io.reactivex.schedulers.Schedulers;

/**
 * Additional operators in the form of {@link FlowableTransformer},
 * use {@link Flowable#compose(FlowableTransformer)}
 * to apply the operators to an existing sequence.
 * 
 * @since 0.7.2
 */
public final class FlowableTransformers {
    /** Utility class. */
    private FlowableTransformers() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Relays values until the other Publisher signals false and resumes if the other
     * Publisher signals true again, like closing and opening a valve and not losing
     * any items from the main source.
     * <p>Properties:
     * <ul>
     * <li>The operator starts with an open valve.</li>
     * <li>If the other Publisher completes, the sequence terminates with an {@code IllegalStateException}.</li>
     * <li>The operator doesn't run on any particular {@link io.reactivex.Scheduler Scheduler}.</li>
     * <li>The operator is a pass-through for backpressure and uses an internal unbounded buffer
     * of size {@link Flowable#bufferSize()} to hold onto values if the valve is closed.</li>
     * </ul>
     * @param <T> the value type of the main source
     * @param other the other source
     * @return the new FlowableTransformer instance
     * @throws NullPointerException if {@code other} is null
     * 
     * @since 0.7.2
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public static <T> FlowableTransformer<T, T> valve(Publisher<Boolean> other) {
        return valve(other, true, Flowable.bufferSize());
    }

    /**
     * Relays values until the other Publisher signals false and resumes if the other
     * Publisher signals true again, like closing and opening a valve and not losing
     * any items from the main source and starts with the specivied valve state.
     * <p>Properties:
     * <ul>
     * <li>If the other Publisher completes, the sequence terminates with an {@code IllegalStateException}.</li>
     * <li>The operator doesn't run on any particular {@link io.reactivex.Scheduler Scheduler}.</li>
     * <li>The operator is a pass-through for backpressure and uses an internal unbounded buffer
     * of size {@link Flowable#bufferSize()} to hold onto values if the valve is closed.</li>
     * </ul>
     * @param <T> the value type of the main source
     * @param other the other source
     * @param defaultOpen should the valve start as open?
     * @return the new FlowableTransformer instance
     * @throws NullPointerException if {@code other} is null
     * 
     * @since 0.7.2
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public static <T> FlowableTransformer<T, T> valve(Publisher<Boolean> other, boolean defaultOpen) {
        return valve(other, defaultOpen, Flowable.bufferSize());
    }

    /**
     * Relays values until the other Publisher signals false and resumes if the other
     * Publisher signals true again, like closing and opening a valve and not losing
     * any items from the main source and starts with the specivied valve state and the specified
     * buffer size hint.
     * <p>Properties:
     * <ul>
     * <li>If the other Publisher completes, the sequence terminates with an {@code IllegalStateException}.</li>
     * <li>The operator doesn't run on any particular {@link io.reactivex.Scheduler Scheduler}.</li>
     * </ul>
     * @param <T> the value type of the main source
     * @param other the other source
     * @param defaultOpen should the valve start as open?
     * @param bufferSize the buffer size hint (the chunk size of the underlying unbounded buffer)
     * @return the new FlowableTransformer instance
     * @throws IllegalArgumentException if bufferSize &lt;= 0
     * @throws NullPointerException if {@code other} is null
     * @since 0.7.2
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    public static <T> FlowableTransformer<T, T> valve(Publisher<Boolean> other, boolean defaultOpen, int bufferSize) {
        ObjectHelper.requireNonNull(other, "other is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return new FlowableValve<T>(null, other, defaultOpen, bufferSize);
    }

    /**
     * Buffers elements into a List while the given predicate returns true; if the
     * predicate returns false for an item, a new buffer is created with the specified item.
     * @param <T> the source value type
     * @param predicate the predicate receiving the current value and if returns false,
     *                  a new buffer is created with the specified item
     * @return the new FlowableTransformer instance
     *
     * @since 0.8.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <T> FlowableTransformer<T, List<T>> bufferWhile(Predicate<? super T> predicate) {
        return bufferWhile(predicate, Functions.<T>createArrayList(16));
    }

    /**
     * Buffers elements into a custom collection while the given predicate returns true; if the
     * predicate returns false for an item, a new collection is created with the specified item.
     * @param <T> the source value type
     * @param <C> the collection type
     * @param predicate the predicate receiving the current value and if returns false,
     *                  a new collection is created with the specified item
     * @param bufferSupplier the callable that returns a fresh collection
     * @return the new FlowableTransformer instance
     *
     * @since 0.8.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <T, C extends Collection<? super T>> FlowableTransformer<T, C> bufferWhile(Predicate<? super T> predicate, Callable<C> bufferSupplier) {
        return new FlowableBufferPredicate<T, C>(null, predicate, FlowableBufferPredicate.Mode.BEFORE, bufferSupplier);
    }

    /**
     * Buffers elements into a List until the given predicate returns true at which
     * point a new empty buffer is started.
     * @param <T> the source value type
     * @param predicate the predicate receiving the current item and if returns true,
     *                  the current buffer is emitted and a fresh empty buffer is created
     * @return the new FlowableTransformer instance
     *
     * @since 0.8.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <T> FlowableTransformer<T, List<T>> bufferUntil(Predicate<? super T> predicate) {
        return bufferUntil(predicate, Functions.<T>createArrayList(16));
    }


    /**
     * Buffers elements into a custom collection until the given predicate returns true at which
     * point a new empty custom collection is started.
     * @param <T> the source value type
     * @param <C> the collection type
     * @param predicate the predicate receiving the current item and if returns true,
     *                  the current collection is emitted and a fresh empty collection is created
     * @param bufferSupplier the callable that returns a fresh collection
     * @return the new Flowable instance
     *
     * @since 0.8.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <T, C extends Collection<? super T>> FlowableTransformer<T, C> bufferUntil(Predicate<? super T> predicate, Callable<C> bufferSupplier) {
        return new FlowableBufferPredicate<T, C>(null, predicate, FlowableBufferPredicate.Mode.AFTER, bufferSupplier);
    }

    /**
     * Buffers elements into a List until the given predicate returns true at which
     * point a new empty buffer is started; the particular item will be dropped.
     * @param <T> the source value type
     * @param predicate the predicate receiving the current item and if returns true,
     *                  the current buffer is emitted and a fresh empty buffer is created
     * @return the new FlowableTransformer instance
     *
     * @since 0.14.3
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <T> FlowableTransformer<T, List<T>> bufferSplit(Predicate<? super T> predicate) {
        return bufferSplit(predicate, Functions.<T>createArrayList(16));
    }


    /**
     * Buffers elements into a custom collection until the given predicate returns true at which
     * point a new empty custom collection is started; the particular item will be dropped.
     * @param <T> the source value type
     * @param <C> the collection type
     * @param predicate the predicate receiving the current item and if returns true,
     *                  the current collection is emitted and a fresh empty collection is created
     * @param bufferSupplier the callable that returns a fresh collection
     * @return the new Flowable instance
     *
     * @since 0.14.3
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @BackpressureSupport(BackpressureKind.FULL)
    public static <T, C extends Collection<? super T>> FlowableTransformer<T, C> bufferSplit(Predicate<? super T> predicate, Callable<C> bufferSupplier) {
        return new FlowableBufferPredicate<T, C>(null, predicate, FlowableBufferPredicate.Mode.SPLIT, bufferSupplier);
    }

    /**
     * Inserts a time delay between emissions from the upstream source.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses the computation {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static <T> FlowableTransformer<T, T> spanout(long betweenDelay, TimeUnit unit) {
        return spanout(0L, betweenDelay, unit, Schedulers.computation(), false);
    }


    /**
     * Inserts a time delay between emissions from the upstream source.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses a custom {@link Scheduler} you provide.</dd>
     * </dl>
     * @param <T> the value type
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @param scheduler the scheduler to delay and emit the values on
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> FlowableTransformer<T, T> spanout(long betweenDelay, TimeUnit unit, Scheduler scheduler) {
        return spanout(0L, betweenDelay, unit, scheduler, false);
    }


    /**
     * Inserts a time delay between emissions from the upstream source, including an initial delay.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses the computation {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param initialDelay the initial delay
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static <T> FlowableTransformer<T, T> spanout(long initialDelay, long betweenDelay, TimeUnit unit) {
        return spanout(initialDelay, betweenDelay, unit, Schedulers.computation(), false);
    }

    /**
     * Inserts a time delay between emissions from the upstream source, including an initial delay.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses a custom {@link Scheduler} you provide.</dd>
     * </dl>
     * @param <T> the value type
     * @param initialDelay the initial delay
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @param scheduler the scheduler to delay and emit the values on
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> FlowableTransformer<T, T> spanout(long initialDelay, long betweenDelay, TimeUnit unit, Scheduler scheduler) {
        return spanout(initialDelay, betweenDelay, unit, scheduler, false);
    }

    /**
     * Inserts a time delay between emissions from the upstream source, including an initial delay.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses the computation {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @param delayError delay the onError event from upstream
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static <T> FlowableTransformer<T, T> spanout(long betweenDelay, TimeUnit unit, boolean delayError) {
        return spanout(0L, betweenDelay, unit, Schedulers.computation(), delayError);
    }


    /**
     * Inserts a time delay between emissions from the upstream source, including an initial delay.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses a custom {@link Scheduler} you provide.</dd>
     * </dl>
     * @param <T> the value type
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @param scheduler the scheduler to delay and emit the values on
     * @param delayError delay the onError event from upstream
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> FlowableTransformer<T, T> spanout(long betweenDelay, TimeUnit unit, Scheduler scheduler, boolean delayError) {
        return spanout(0L, betweenDelay, unit, scheduler, delayError);
    }


    /**
     * Inserts a time delay between emissions from the upstream source, including an initial delay.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses the computation {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param initialDelay the initial delay
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @param delayError delay the onError event from upstream
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static <T> FlowableTransformer<T, T> spanout(long initialDelay, long betweenDelay, TimeUnit unit, boolean delayError) {
        return spanout(initialDelay, betweenDelay, unit, Schedulers.computation(), delayError);
    }

    /**
     * Inserts a time delay between emissions from the upstream source, including an initial delay.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator itself doesn't interfere with backpressure and uses an unbounded
     *  internal buffer to store elements that need delay.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator uses a custom {@link Scheduler} you provide.</dd>
     * </dl>
     * @param <T> the value type
     * @param initialDelay the initial delay
     * @param betweenDelay the (minimum) delay time between elements
     * @param unit the time unit of the initial delay and the between delay values
     * @param scheduler the scheduler to delay and emit the values on
     * @param delayError delay the onError event from upstream
     * @return the new FlowableTransformer instance
     * 
     * @since 0.9.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> FlowableTransformer<T, T> spanout(long initialDelay, long betweenDelay, TimeUnit unit, Scheduler scheduler, boolean delayError) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return new FlowableSpanout<T>(null, initialDelay, betweenDelay, unit, scheduler, delayError, Flowable.bufferSize());
    }

    /**
     * Allows mapping or filtering an upstream value through an emitter.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param consumer the consumer that is called for each upstream value and should call one of the doXXX methods
     * on the BasicEmitter it receives (individual to each Subscriber).
     * @return the new FlowableTransformer instance
     * 
     * @since 0.10.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, R> FlowableTransformer<T, R> mapFilter(BiConsumer<? super T, ? super BasicEmitter<R>> consumer) {
        ObjectHelper.requireNonNull(consumer, "consumer is null");
        return new FlowableMapFilter<T, R>(null, consumer);
    }

    /**
     * Buffers the incoming values from upstream up to a maximum timeout if
     * the downstream can't keep up.
     * @param <T> the value type
     * @param timeout the maximum age of an element in the buffer
     * @param unit the time unit of the timeout
     * @return the new FlowableTransformer instance
     * @see #onBackpressureTimeout(int, long, TimeUnit, Scheduler, Consumer) for more options
     *
     * @since 0.13.0
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static <T> FlowableTransformer<T, T> onBackpressureTimeout(long timeout, TimeUnit unit) {
        return onBackpressureTimeout(timeout, unit, Schedulers.computation());
    }

    /**
     * Buffers the incoming values from upstream up to a maximum size or timeout if
     * the downstream can't keep up.
     * @param <T> the value type
     * @param timeout the maximum age of an element in the buffer
     * @param unit the time unit of the timeout
     * @param scheduler the scheduler to be used as time source and to trigger the timeout &amp; eviction
     * @param onEvict called when an element is evicted, maybe concurrently
     * @return the new FlowableTransformer instance
     *
     * @since 0.13.0
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> FlowableTransformer<T, T> onBackpressureTimeout(long timeout, TimeUnit unit, Scheduler scheduler, Consumer<? super T> onEvict) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        ObjectHelper.requireNonNull(onEvict, "onEvict is null");

        return new FlowableOnBackpressureTimeout<T>(null, Integer.MAX_VALUE, timeout, unit, scheduler, onEvict);
    }

    /**
     * Buffers the incoming values from upstream up to a maximum timeout if
     * the downstream can't keep up, running on a custom scheduler.
     * @param <T> the value type
     * @param timeout the maximum age of an element in the buffer
     * @param unit the time unit of the timeout
     * @param scheduler the scheduler to be used as time source and to trigger the timeout &amp; eviction
     * @return the new FlowableTransformer instance
     * @see #onBackpressureTimeout(int, long, TimeUnit, Scheduler, Consumer) for more options
     *
     * @since 0.13.0
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> FlowableTransformer<T, T> onBackpressureTimeout(long timeout, TimeUnit unit, Scheduler scheduler) {
        return onBackpressureTimeout(Integer.MAX_VALUE, timeout, unit, scheduler, Functions.emptyConsumer());
    }

    /**
     * Buffers the incoming values from upstream up to a maximum size or timeout if
     * the downstream can't keep up.
     * @param <T> the value type
     * @param maxSize the maximum number of elements in the buffer, beyond that,
     *                the oldest element is evicted
     * @param timeout the maximum age of an element in the buffer
     * @param unit the time unit of the timeout
     * @param scheduler the scheduler to be used as time source and to trigger the timeout &amp; eviction
     * @param onEvict called when an element is evicted, maybe concurrently
     * @return the new FlowableTransformer instance
     *
     * @since 0.13.0
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> FlowableTransformer<T, T> onBackpressureTimeout(int maxSize, long timeout, TimeUnit unit, Scheduler scheduler, Consumer<? super T> onEvict) {
        ObjectHelper.verifyPositive(maxSize, "maxSize");
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        ObjectHelper.requireNonNull(onEvict, "onEvict is null");

        return new FlowableOnBackpressureTimeout<T>(null, maxSize, timeout, unit, scheduler, onEvict);
    }

    /**
     * Relays every Nth item from upstream.
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator requests keep {@code times} what the downstream requests and skips @code keep-1} items.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>The operator doesn't run on any particular {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the value type
     * @param keep the period of items to keep, i.e., this minus one items will be dropped
     * before emitting an item directly
     * @return the new FlowableTransformer instance
     *
     * @since 0.14.2
     */
    @BackpressureSupport(BackpressureKind.SPECIAL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> FlowableTransformer<T, T> every(long keep) {
        ObjectHelper.verifyPositive(keep, "keep");
        return new FlowableEvery<T>(null, keep);
    }

    /**
     * Cache the very last value of the flow and relay/replay it to Subscribers.
     * <p>
     * The operator subscribes to the upstream when the first downstream Subscriber
     * arrives. Once connected, the upstream can't be stopped from the
     * downstream even if all Subscribers cancel.
     * <p>
     * A difference from {@code replay(1)} is that {@code replay()} is likely
     * holding onto 2 references due to continuity requirements whereas this
     * operator is guaranteed to hold only the very last item.
     * @param <T> the value type emitted
     * @return the new FlowableTransformer instance
     * 
     * @since 0.15.0
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> FlowableTransformer<T, T> cacheLast() {
        return new FlowableCacheLast<T>(null);
    }

    /**
     * Emit the last item when the upstream completes or the
     * the latest received if the specified timeout elapses since
     * the last received item.
     * @param <T> the value type
     * @param timeout the timeout value
     * @param unit the timeout time unit
     * @return the new Flowable type
     * 
     * @since 0.15.0
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static <T> FlowableTransformer<T, T> timeoutLast(long timeout, TimeUnit unit) {
        return timeoutLast(timeout, unit, Schedulers.computation());
    }

    /**
     * Emit the last item when the upstream completes or the
     * the latest received if the specified timeout elapses since
     * the last received item.
     * @param <T> the value type
     * @param timeout the timeout value
     * @param unit the timeout time unit
     * @param scheduler the scheduler to run the timeout and possible emit the last/latest
     * @return the new Flowable type
     * 
     * @since 0.15.0
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> FlowableTransformer<T, T> timeoutLast(long timeout, TimeUnit unit, Scheduler scheduler) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return new FlowableTimeoutLast<T>(null, timeout, unit, scheduler, false);
    }

    /**
     * Emit the last item when the upstream completes or the
     * the latest received if the specified timeout elapses
     * since the start of the sequence.
     * @param <T> the value type
     * @param timeout the timeout value
     * @param unit the timeout time unit
     * @return the new Flowable type
     * 
     * @since 0.15.0
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static <T> FlowableTransformer<T, T> timeoutLastAbsolute(long timeout, TimeUnit unit) {
        return timeoutLastAbsolute(timeout, unit, Schedulers.computation());
    }

    /**
     * Emit the last item when the upstream completes or the
     * the latest received if the specified timeout elapses
     * since the start of the sequence.
     * @param <T> the value type
     * @param timeout the timeout value
     * @param unit the timeout time unit
     * @param scheduler the scheduler to run the timeout and possible emit the last/latest
     * @return the new Flowable type
     * 
     * @since 0.15.0
     */
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> FlowableTransformer<T, T> timeoutLastAbsolute(long timeout, TimeUnit unit, Scheduler scheduler) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return new FlowableTimeoutLast<T>(null, timeout, unit, scheduler, true);
    }

    /**
     * Debounces the upstream by taking an item and dropping subsequent items until
     * the specified amount of time elapses after the last item, after which the
     * process repeats.
     * <p>
     * Note that the operator uses the {@code computation} {@link Scheduler} for
     * the source of time but doesn't use it to emit non-dropped items or terminal events.
     * The operator uses calculation with the current time to decide if an upstream
     * item may pass or not.
     * @param <T> the value type
     * @param timeout the timeout
     * @param unit the unit of measure of the timeout parameter
     * @return the new FlowableTransformer instance
     *
     * @since 0.15.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.COMPUTATION)
    public static <T> FlowableTransformer<T, T> debounceFirst(long timeout, TimeUnit unit) {
        return debounceFirst(timeout, unit, Schedulers.computation());
    }

    /**
     * Debounces the upstream by taking an item and dropping subsequent items until
     * the specified amount of time elapses after the last item, after which the
     * process repeats.
     * <p>
     * Note that the operator uses the {@code computation} {@link Scheduler} for
     * the source of time but doesn't use it to emit non-dropped items or terminal events.
     * The operator uses calculation with the current time to decide if an upstream
     * item may pass or not.
     * @param <T> the value type
     * @param timeout the timeout
     * @param unit the unit of measure of the timeout parameter
     * @param scheduler the scheduler used for getting the current time when
     * evaluating upstream items
     * @return the new FlowableTransformer instance
     *
     * @since 0.15.0
     */
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    public static <T> FlowableTransformer<T, T> debounceFirst(long timeout, TimeUnit unit, Scheduler scheduler) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return new FlowableDebounceFirst<T>(null, timeout, unit, scheduler);
    }

    /**
     * Combination of switchMap and flatMap where there is a limit on the number of
     * concurrent sources to be flattened into a single sequence and if the operator is at
     * the given maximum active count, a newer source Publisher will switch out the oldest
     * active source Publisher being merged.
     * @param <T> the source value type
     * @param <R> the result value type
     * @param mapper the function that maps an upstream value into a Publisher to be merged/switched
     * @param maxActive the maximum number of active inner Publishers
     * @return the new FlowableTransformer instance
     * 
     * @since 0.15.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, R> FlowableTransformer<T, R> switchFlatMap(Function<? super T, ? extends Publisher<? extends R>> mapper, int maxActive) {
        return switchFlatMap(mapper, maxActive, Flowable.bufferSize());
    }

    /**
     * Combination of switchMap and flatMap where there is a limit on the number of
     * concurrent sources to be flattened into a single sequence and if the operator is at
     * the given maximum active count, a newer source Publisher will switch out the oldest
     * active source Publisher being merged.
     * @param <T> the source value type
     * @param <R> the result value type
     * @param mapper the function that maps an upstream value into a Publisher to be merged/switched
     * @param maxActive the maximum number of active inner Publishers
     * @param bufferSize the number of items to prefetch from each inner source
     * @return the new FlowableTransformer instance
     * 
     * @since 0.15.0
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T, R> FlowableTransformer<T, R> switchFlatMap(Function<? super T, ? extends Publisher<? extends R>> mapper, int maxActive, int bufferSize) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        ObjectHelper.verifyPositive(maxActive, "maxActive");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return new FlowableSwitchFlatMap<T, R>(null, mapper, maxActive, bufferSize);
    }

    /**
     * Maps the upstream values into Publisher and merges at most 32 of them at once,
     * optimized for mainly synchronous sources.
     * @param <T> the input value type
     * @param <R> the result value type
     * @param mapper the function mapping from a value into a Publisher
     * @return the new FlowableTransformer instance
     *
     * @since 0.16.0
     */
    public static <T, R> FlowableTransformer<T, R> flatMapSync(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return flatMapSync(mapper, 32, Flowable.bufferSize(), true);
    }

    /**
     * Maps the upstream values into Publisher and merges at most maxConcurrency of them at once,
     * optimized for mainly synchronous sources.
     * @param <T> the input value type
     * @param <R> the result value type
     * @param mapper the function mapping from a value into a Publisher
     * @param depthFirst if true, the inner sources are drained as much as possible
     *                   if false, the inner sources are consumed in a round-robin fashion
     * @return the new FlowableTransformer instance
     *
     * @since 0.16.0
     */
    public static <T, R> FlowableTransformer<T, R> flatMapSync(Function<? super T, ? extends Publisher<? extends R>> mapper, boolean depthFirst) {
        return flatMapSync(mapper, 32, Flowable.bufferSize(), depthFirst);
    }

    /**
     * Maps the upstream values into Publisher and merges at most maxConcurrency of them at once,
     * optimized for mainly synchronous sources.
     * @param <T> the input value type
     * @param <R> the result value type
     * @param mapper the function mapping from a value into a Publisher
     * @param maxConcurrency the maximum number of sources merged at once
     * @param bufferSize the prefetch on each inner source
     * @param depthFirst if true, the inner sources are drained as much as possible
     *                   if false, the inner sources are consumed in a round-robin fashion
     * @return the new FlowableTransformer instance
     *
     * @since 0.16.0
     */
    public static <T, R> FlowableTransformer<T, R> flatMapSync(Function<? super T, ? extends Publisher<? extends R>> mapper, int maxConcurrency, int bufferSize, boolean depthFirst) {
        return new FlowableFlatMapSync<T, R>(null, mapper, maxConcurrency, bufferSize, depthFirst);
    }

    /**
     * Maps the upstream values into Publisher and merges at most 32 of them at once,
     * collects and emits the items on the specified scheduler.
     * <p>This operator can be considered as a fusion between a flatMapSync
     * and observeOn.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param mapper the function mapping from a value into a Publisher
     * @param scheduler the Scheduler to use to collect and emit merged items
     * @return the new FlowableTransformer instance
     *
     * @since 0.16.0
     */
    public static <T, R> FlowableTransformer<T, R> flatMapAsync(Function<? super T, ? extends Publisher<? extends R>> mapper, Scheduler scheduler) {
        return flatMapAsync(mapper, scheduler, 32, Flowable.bufferSize(), true);
    }

    /**
     * Maps the upstream values into Publisher and merges at most 32 of them at once,
     * collects and emits the items on the specified scheduler.
     * <p>This operator can be considered as a fusion between a flatMapSync
     * and observeOn.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param mapper the function mapping from a value into a Publisher
     * @param scheduler the Scheduler to use to collect and emit merged items
     * @param depthFirst if true, the inner sources are drained as much as possible
     *                   if false, the inner sources are consumed in a round-robin fashion
     * @return the new FlowableTransformer instance
     *
     * @since 0.16.0
     */
    public static <T, R> FlowableTransformer<T, R> flatMapAsync(Function<? super T, ? extends Publisher<? extends R>> mapper, Scheduler scheduler, boolean depthFirst) {
        return flatMapAsync(mapper, scheduler, 32, Flowable.bufferSize(), depthFirst);
    }

    /**
     * Maps the upstream values into Publisher and merges at most 32 of them at once,
     * collects and emits the items on the specified scheduler.
     * <p>This operator can be considered as a fusion between a flatMapSync
     * and observeOn.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param mapper the function mapping from a value into a Publisher
     * @param scheduler the Scheduler to use to collect and emit merged items
     * @param maxConcurrency the maximum number of sources merged at once
     * @param bufferSize the prefetch on each inner source
     * @param depthFirst if true, the inner sources are drained as much as possible
     *                   if false, the inner sources are consumed in a round-robin fashion
     * @return the new FlowableTransformer instance
     *
     * @since 0.16.0
     */
    public static <T, R> FlowableTransformer<T, R> flatMapAsync(Function<? super T, ? extends Publisher<? extends R>> mapper, Scheduler scheduler, int maxConcurrency, int bufferSize, boolean depthFirst) {
        return new FlowableFlatMapAsync<T, R>(null, mapper, maxConcurrency, bufferSize, depthFirst, scheduler);
    }

    /**
     * If the upstream turns out to be empty, it keeps switching to the alternative sources until
     * one of them is non-empty or there are no more alternatives remaining.
     * @param <T> the input and output value type
     * @param alternatives the array of alternative Publishers.
     * @return the new FlowableTransformer instance
     *
     * @since 0.16.0
     */
    public static <T> FlowableTransformer<T, T> switchIfEmptyArray(Publisher<? extends T>... alternatives) {
        return new FlowableSwitchIfEmptyManyArray<T>(null, alternatives);
    }

    /**
     * If the upstream turns out to be empty, it keeps switching to the alternative sources until
     * one of them is non-empty or there are no more alternatives remaining.
     * @param <T> the input and output value type
     * @param alternatives the Iterable of alternative Publishers.
     * @return the new FlowableTransformer instance
     *
     * @since 0.16.0
     */
    public static <T> FlowableTransformer<T, T> switchIfEmpty(Iterable<? extends Publisher<? extends T>> alternatives) {
        return new FlowableSwitchIfEmptyMany<T>(null, alternatives);
    }
}
