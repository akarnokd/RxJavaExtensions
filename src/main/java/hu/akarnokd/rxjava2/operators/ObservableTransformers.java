/*
 * Copyright 2016-2018 David Karnok
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

import io.reactivex.*;
import io.reactivex.annotations.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Additional operators in the form of {@link ObservableTransformer},
 * use {@link Observable#compose(ObservableTransformer)}
 * to apply the operators to an existing sequence.
 * 
 * @since 0.18.2
 */
public final class ObservableTransformers {

    /**
     * Utility class.
     */
    private ObservableTransformers() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Returns the first index of an element that matches a predicate or -1L if no elements match.
     * @param <T> the upstream element type
     * @param predicate the predicate called to test each item, returning true will
     * stop the sequence and return the current item index
     * @return the new ObservableTransformer instance
     * 
     * @since 0.18.2
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @CheckReturnValue
    @NonNull
    public static <T> ObservableTransformer<T, Long> indexOf(@NonNull Predicate<? super T> predicate) {
        ObjectHelper.requireNonNull(predicate, "predicate is null");
        return new ObservableIndexOf<T>(null, predicate);
    }

    /**
     * Schedules the event emission on a {@link Scheduler} and drops upstream values while
     * the {@code onNext} with the current item is executing on that scheduler.
     * <p>
     * Errors are delayed until all items that weren't dropped have been delivered.
     * @param <T> the element type
     * @param scheduler the scheduler to use for emitting events on
     * @return the new ObservableTransformer instance
     * @see #observeOnLatest(Scheduler)
     */
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @CheckReturnValue
    @NonNull
    public static <T> ObservableTransformer<T, T> observeOnDrop(@NonNull Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return new ObservableObserveOnDrop<T>(null, scheduler);
    }

    /**
     * Schedules the event emission on a {@link Scheduler} and keeps the latest upstream item
     * while the downstream's {@code onNext} is executing so that it will resume
     * with that latest value.
     * <p>
     * Errors are delayed until the very last item has been delivered.
     * @param <T> the element type
     * @param scheduler the scheduler to use for emitting events on
     * @return the new ObservableTransformer instance
     * @see #observeOnLatest(Scheduler)
     */
    @SchedulerSupport(SchedulerSupport.CUSTOM)
    @CheckReturnValue
    @NonNull
    public static <T> ObservableTransformer<T, T> observeOnLatest(@NonNull Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return new ObservableObserveOnLatest<T>(null, scheduler);
    }

    /**
     * FlatMap only one {@link ObservableSource} at a time and ignore upstream values until it terminates.
     * <p>
     * Errors are delayed until both the upstream and the active inner {@code ObservableSource} terminate.
     * @param <T> the upstream value type
     * @param <R> the output type
     * @param mapper the function that takes an upstream item and returns a {@link ObservableSource}
     * to be run exclusively until it finishes
     * @return the new ObservableTransformer instance
     * @since 0.19.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @CheckReturnValue
    @NonNull
    public static <T, R> ObservableTransformer<T, R> flatMapDrop(Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return new ObservableFlatMapDrop<T, R>(null, mapper);
    }

    /**
 * FlatMap only one {@link ObservableSource} at a time and keep the latest upstream value until it terminates
 * and resume with the {@code ObservableSource} mapped for that latest upstream value.
     * <p>
     * Errors are delayed until both the upstream and the active inner {@code ObservableSource} terminate.
     * @param <T> the upstream value type
     * @param <R> the output type
     * @param mapper the function that takes an upstream item and returns a {@link ObservableSource}
     * to be run exclusively until it finishes
     * @return the new ObservableTransformer instance
     * @since 0.19.0
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    @CheckReturnValue
    @NonNull
    public static <T, R> ObservableTransformer<T, R> flatMapLatest(Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return new ObservableFlatMapLatest<T, R>(null, mapper);
    }

    /**
     * Allows an upstream error to jump over an inner transformation and is
     * then reapplied once the inner transformation's returned Flowable terminates.
     * @param <T> the upstream value type
     * @param <R> the downstream value type
     * @param transformer the transformation applied to the flow on a per-Subscriber basis
     * @return the new FlowableTransformer instance
     * @since 0.19.1
     */
    public static <T, R> ObservableTransformer<T, R> errorJump(ObservableTransformer<T, R> transformer) {
        ObjectHelper.requireNonNull(transformer, "transformer");
        return new ObservableErrorJump<T, R>(null, transformer);
    }

    /**
     * Relays values until the other ObservableSource signals false and resumes if the other
     * ObservableSource signals true again, like closing and opening a valve and not losing
     * any items from the main source.
     * <p>Properties:
     * <ul>
     * <li>The operator starts with an open valve.</li>
     * <li>If the other ObservableSource completes, the sequence terminates with an {@code IllegalStateException}.</li>
     * <li>The operator doesn't run on any particular {@link io.reactivex.Scheduler Scheduler}.</li>
     * <li>The operator uses an internal unbounded buffer
     * of size {@link Flowable#bufferSize()} to hold onto values if the valve is closed.</li>
     * </ul>
     * @param <T> the value type of the main source
     * @param other the other source
     * @return the new ObservableTransformer instance
     * @throws NullPointerException if {@code other} is null
     * 
     * @since 0.20.2
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> ObservableTransformer<T, T> valve(ObservableSource<Boolean> other) {
        return valve(other, true, Flowable.bufferSize());
    }

    /**
     * Relays values until the other ObservableSource signals false and resumes if the other
     * ObservableSource signals true again, like closing and opening a valve and not losing
     * any items from the main source and starts with the specified valve state.
     * <p>Properties:
     * <ul>
     * <li>If the other ObservableSource completes, the sequence terminates with an {@code IllegalStateException}.</li>
     * <li>The operator doesn't run on any particular {@link io.reactivex.Scheduler Scheduler}.</li>
     * <li>The operator uses an internal unbounded buffer
     * of size {@link Flowable#bufferSize()} to hold onto values if the valve is closed.</li>
     * </ul>
     * @param <T> the value type of the main source
     * @param other the other source
     * @param defaultOpen should the valve start as open?
     * @return the new ObservableTransformer instance
     * @throws NullPointerException if {@code other} is null
     * 
     * @since 0.20.2
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> ObservableTransformer<T, T> valve(ObservableSource<Boolean> other, boolean defaultOpen) {
        return valve(other, defaultOpen, Flowable.bufferSize());
    }

    /**
     * Relays values until the other ObservableSource signals false and resumes if the other
     * ObservableSource signals true again, like closing and opening a valve and not losing
     * any items from the main source and starts with the specified valve state and the specified
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
     * @return the new ObservableTransformer instance
     * @throws IllegalArgumentException if bufferSize &lt;= 0
     * @throws NullPointerException if {@code other} is null
     * @since 0.20.2
     */
    @SchedulerSupport(SchedulerSupport.NONE)
    public static <T> ObservableTransformer<T, T> valve(ObservableSource<Boolean> other, boolean defaultOpen, int bufferSize) {
        ObjectHelper.requireNonNull(other, "other is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return new ObservableValve<T>(null, other, defaultOpen, bufferSize);
    }
}
