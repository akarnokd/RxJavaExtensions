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

package hu.akarnokd.rxjava3.debug.validator;

import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava3.functions.PlainConsumer;
import io.reactivex.rxjava3.parallel.ParallelFlowable;

/**
 * Validates a ParallelFlowable.
 * @param <T> the value type
 * @since 0.17.4
 */
final class ParallelFlowableValidator<T> extends ParallelFlowable<T> {

    final ParallelFlowable<T> source;

    final PlainConsumer<ProtocolNonConformanceException> onViolation;

    ParallelFlowableValidator(ParallelFlowable<T> source, PlainConsumer<ProtocolNonConformanceException> onViolation) {
        this.source = source;
        this.onViolation = onViolation;
    }

    @Override
    public void subscribe(Subscriber<? super T>[] s) {
        validate(s);
        int n = source.parallelism();
        @SuppressWarnings("unchecked")
        Subscriber<? super T>[] actual = new Subscriber[n];
        for (int i = 0; i < n; i++) {
            actual[i] = new FlowableValidator.ValidatorConsumer<>(s[i], onViolation);
        }
        source.subscribe(actual);
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }
}
