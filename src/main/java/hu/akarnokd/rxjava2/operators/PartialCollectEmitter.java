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

/**
 * Provides callbacks and methods to support the partial collect operator:
 * access to cached elements, save and retrieve an input index and accumulator and
 * produce the output instance.
 * @param <T> the upstream value type
 * @param <I> the type that indicates where the first cached item should be read from
 * @param <A> the accumulator type used to collect up partial data
 * @param <R> the output type
 * @since 0.18.9
 */
public interface PartialCollectEmitter<T, I, A, R> {

    /**
     * The downstream requested cancellation.
     * @return true if the downstream cancelled
     */
    boolean isCancelled();

    /**
     * The upstream completed sending new items.
     * @return true if the upstream completed
     */
    boolean isComplete();

    /**
     * The number of items cached and accessible via {@link #getItem(int)}.
     * @return the number of items cached
     */
    int size();

    /**
     * Access a cached item based on an index less than {@link #size()}.
     * @param index the index
     * @return the item
     */
    T getItem(int index);

    /**
     * Remove the first {@code count} items from the cache, sending them
     * to the cleanup handler of the operator as well as possibly
     * triggering more requests to the upstream to replenish the buffer.
     * @param count the number of items to drop
     */
    void dropItems(int count);

    /**
     * Reads an optional, user-defined index that can be used to store a read pointer into
     * the very first upstream item accessible via {@link #getItem(int)} to
     * indicate from where to resume.
     * @return the index object
     */
    I getIndex();

    /**
     * Sets an optional, user-defined index that can be used as a read poitner into
     * the very first upstream item.
     * @param newIndex the index object to set
     */
    void setIndex(I newIndex);

    /**
     * Returns an optional, user-defined accumulator that can be used to aggregate
     * partial items from upstream until enough data has been gathered.
     * @return the accumulator object
     */
    A getAccumulator();

    /**
     * Sets an optional, user-defined accumulator that can be used to aggregate
     * partial items from upstream.
     * @param newAccumulator the new accumulator object
     */
    void setAccumulator(A newAccumulator);

    /**
     * Signal the next output item.
     * <p>
     * This can be called as many times as {@link #demand()}.
     * @param item the item to signal
     */
    void next(R item);

    /**
     * Indicate that no further output items will be produced.
     */
    void complete();

    /**
     * Call the cleanup handler of the operator for a specific upstream
     * instance.
     * @param item the item to clean up
     */
    void cleanupItem(T item);

    /**
     * Returns the number of items that can be emitted via {@link #next(Object)}
     * without overflowing the downstream.
     * @return the outstanding downstream demand
     */
    long demand();
}
