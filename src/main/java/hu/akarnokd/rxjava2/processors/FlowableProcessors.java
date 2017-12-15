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

package hu.akarnokd.rxjava2.processors;

import org.reactivestreams.Processor;

import io.reactivex.annotations.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.processors.*;

/**
 * Utility methods to work with Reactive-Streams Processors and RxJava 2 FlowableProcessors.
 */
public final class FlowableProcessors {
    /** Utility class. */
    private FlowableProcessors() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Wraps an arbitrary Reactive-Streams {@link Processor} into a {@link FlowableProcessor}, relaying
     * the onXXX and subscribe() calls to it and providing a rich fluent API on top.
     * <p>Note that RxJava 2 doesn't support a FlowableProcessor with different input
     * and output types.
     * @param <T> the input and output type
     * @param processor the processor to wrap (or return if already a FlowableProcessor), not null
     * @return the FlowableProcessor instance possible wrapping the input processor
     */
    public static <T> FlowableProcessor<T> wrap(Processor<T, T> processor) {
        if (processor instanceof FlowableProcessor) {
            return (FlowableProcessor<T>)processor;
        }
        return new FlowableProcessorWrap<T>(ObjectHelper.requireNonNull(processor, "processor is null"));
    }

    /**
     * Wraps a FlowableProcessor and makes sure if all subscribers cancel
     * their subscriptions, the upstream's Subscription gets cancelled as well.
     * <p>
     * This operator is similar to {@link io.reactivex.flowables.ConnectableFlowable#refCount()}
     * except the first Subscriber doesn't trigger any sort of connection; that happens
     * when the resulting FlowableProcessor is subscribed to a Publisher manually.
     * @param <T> the input and output value type
     * @param processor the processor to wrap, not null
     * @return the wrapped and reference-counted FlowableProcessor
     * @since 0.18.2
     */
    @NonNull
    @CheckReturnValue
    public static <T> FlowableProcessor<T> refCount(FlowableProcessor<T> processor) {
        if (processor instanceof RefCountProcessor) {
            return processor;
        }
        return new RefCountProcessor<T>(ObjectHelper.requireNonNull(processor, "processor is null"));
    }
}
