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

import hu.akarnokd.rxjava3.util.BiFunctionSecondIdentity;
import io.reactivex.*;
import io.reactivex.annotations.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Additional operators in the form of {@link ObservableTransformer},
 * use {@link Observable#compose(ObservableTransformer)}
 * to apply the operators to an existing sequence.
 * 
 * @see Observables
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
     * <li>If the other ObservableSource completes, the sequence terminates with an {@code IllegalStateException}.</li>
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

    /**
     * Maps each upstream value into a single value provided by a generated ObservableSource for that
     * input value, which is then emitted to the downstream.
     * <p>Only the first item emitted by the inner ObservableSource are considered. If
     * the inner ObservableSource is empty, no resulting item is generated for that input value.
     * <p>The inner ObservableSources are consumed in order and one at a time.
     * @param <T> the input value type
     * @param <R> the result value type
     * @param mapper the function that receives the upstream value and returns a ObservableSource
     * that should emit a single value to be emitted.
     * @return the new ObservableTransformer instance
     * @since 0.20.4
     */
    public static <T, R> ObservableTransformer<T, R> mapAsync(Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        return mapAsync(mapper, BiFunctionSecondIdentity.<T, R>instance(), Flowable.bufferSize());
    }
    /**
     * Maps each upstream value into a single value provided by a generated ObservableSource for that
     * input value, which is then emitted to the downstream.
     * <p>Only the first item emitted by the inner ObservableSource are considered. If
     * the inner ObservableSource is empty, no resulting item is generated for that input value.
     * <p>The inner ObservableSources are consumed in order and one at a time.
     * @param <T> the input value type
     * @param <R> the result value type
     * @param mapper the function that receives the upstream value and returns a ObservableSource
     * that should emit a single value to be emitted.
     * @param capacityHint the number of items expected from the upstream to be buffered while each
     * inner ObservableSource is executing.
     * @return the new ObservableTransformer instance
     * @since 0.20.4
     */
    public static <T, R> ObservableTransformer<T, R> mapAsync(Function<? super T, ? extends ObservableSource<? extends R>> mapper, int capacityHint) {
        return mapAsync(mapper, BiFunctionSecondIdentity.<T, R>instance(), capacityHint);
    }
    /**
     * Maps each upstream value into a single value provided by a generated ObservableSource for that
     * input value and combines the original and generated single value into a final result item
     * to be emitted to downstream.
     * <p>Only the first item emitted by the inner ObservableSource are considered. If
     * the inner ObservableSource is empty, no resulting item is generated for that input value.
     * <p>The inner ObservableSources are consumed in order and one at a time.
     * @param <T> the input value type
     * @param <U> the intermediate value type
     * @param <R> the result value type
     * @param mapper the function that receives the upstream value and returns a ObservableSource
     * that should emit a single value to be emitted.
     * @param combiner the bi-function that receives the original upstream value and the
     * single value emitted by the ObservableSource and returns a result value to be emitted to
     * downstream.
     * @return the new ObservableTransformer instance
     * @since 0.20.4
     */
    public static <T, U, R> ObservableTransformer<T, R> mapAsync(Function<? super T, ? extends ObservableSource<? extends U>> mapper, BiFunction<? super T, ? super U, ? extends R> combiner) {
        return mapAsync(mapper, combiner, Flowable.bufferSize());
    }

    /**
     * Maps each upstream value into a single value provided by a generated ObservableSource for that
     * input value and combines the original and generated single value into a final result item
     * to be emitted to downstream.
     * <p>Only the first item emitted by the inner ObservableSource are considered. If
     * the inner ObservableSource is empty, no resulting item is generated for that input value.
     * <p>The inner ObservableSources are consumed in order and one at a time.
     * @param <T> the input value type
     * @param <U> the intermediate value type
     * @param <R> the result value type
     * @param mapper the function that receives the upstream value and returns a ObservableSource
     * that should emit a single value to be emitted.
     * @param combiner the bi-function that receives the original upstream value and the
     * single value emitted by the ObservableSource and returns a result value to be emitted to
     * downstream.
     * @param capacityHint the number of items expected from the upstream to be buffered while each
     * inner ObservableSource is executing.
     * @return the new ObservableTransformer instance
     * @since 0.20.4
     */
    public static <T, U, R> ObservableTransformer<T, R> mapAsync(Function<? super T, ? extends ObservableSource<? extends U>> mapper, BiFunction<? super T, ? super U, ? extends R> combiner, int capacityHint) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        ObjectHelper.requireNonNull(combiner, "combiner is null");
        ObjectHelper.verifyPositive(capacityHint, "capacityHint");
        return new ObservableMapAsync<T, U, R>(null, mapper, combiner, capacityHint);
    }

    /**
     * Maps each upstream value into a single {@code true} or {@code false} value provided by a generated ObservableSource for that
     * input value and emits the input value if the inner ObservableSource returned {@code true}.
     * <p>Only the first item emitted by the inner ObservableSource's are considered. If
     * the inner ObservableSource is empty, no resulting item is generated for that input value.
     * <p>The inner ObservableSources are consumed in order and one at a time.
     * @param <T> the input and output value type
     * @param asyncPredicate the function that receives the upstream value and returns
     * a ObservableSource that should emit a single true to indicate the original value should pass.
     * @return the new ObservableTransformer instance
     * @since 0.20.4
     */
    public static <T> ObservableTransformer<T, T> filterAsync(Function<? super T, ? extends ObservableSource<Boolean>> asyncPredicate) {
        return filterAsync(asyncPredicate, Flowable.bufferSize());
    }

    /**
     * Maps each upstream value into a single {@code true} or {@code false} value provided by a generated ObservableSource for that
     * input value and emits the input value if the inner ObservableSource returned {@code true}.
     * <p>Only the first item emitted by the inner ObservableSource's are considered. If
     * the inner ObservableSource is empty, no resulting item is generated for that input value.
     * <p>The inner ObservableSources are consumed in order and one at a time.
     * @param <T> the input and output value type
     * @param asyncPredicate the function that receives the upstream value and returns
     * a ObservableSource that should emit a single true to indicate the original value should pass.
     * @param bufferSize the internal buffer size and prefetch amount to buffer items from
     * upstream until their turn comes up
     * @return the new ObservableTransformer instance
     * @since 0.20.4
     */
    public static <T> ObservableTransformer<T, T> filterAsync(Function<? super T, ? extends ObservableSource<Boolean>> asyncPredicate, int bufferSize) {
        ObjectHelper.requireNonNull("asyncPredicate", "asyncPredicate is null");
        ObjectHelper.verifyPositive(bufferSize, "capacityHint");
        return new ObservableFilterAsync<T>(null, asyncPredicate, bufferSize);
    }
}
