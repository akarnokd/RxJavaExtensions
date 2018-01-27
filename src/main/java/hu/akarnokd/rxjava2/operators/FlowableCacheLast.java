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

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.processors.AsyncProcessor;

/**
 * Cache only the very last value and relay/replay it to Subscribers.
 *
 * @param <T> the value type
 * @since 0.15.0
 */
final class FlowableCacheLast<T> extends Flowable<T>
implements FlowableTransformer<T, T> {

    final Publisher<T> source;

    final AsyncProcessor<T> processor;

    final AtomicBoolean once;

    FlowableCacheLast(Publisher<T> source) {
        this.source = source;
        this.processor = AsyncProcessor.create();
        this.once = new AtomicBoolean();
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableCacheLast<T>(upstream);
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        processor.subscribe(s);
        AtomicBoolean o = once;
        if (!o.get() && o.compareAndSet(false, true)) {
            source.subscribe(processor);
        }
    }
}
