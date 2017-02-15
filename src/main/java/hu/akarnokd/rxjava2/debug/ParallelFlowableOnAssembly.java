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

package hu.akarnokd.rxjava2.debug;

import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava2.debug.FlowableOnAssembly.*;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.parallel.ParallelFlowable;

/**
 * Wrap a Parallel Flowable and inject the assembly info.
 *
 * @param <T> the value type
 * @since 0.15.2
 */
final class ParallelFlowableOnAssembly<T> extends ParallelFlowable<T> {

    final ParallelFlowable<T> source;

    final RxJavaAssemblyException assembled;

    ParallelFlowableOnAssembly(ParallelFlowable<T> source) {
        this.source = source;
        this.assembled = new RxJavaAssemblyException();
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }

    @Override
    public void subscribe(Subscriber<? super T>[] s) {
        if (validate(s)) {
            int n = s.length;
            @SuppressWarnings("unchecked")
            Subscriber<? super T>[] parents = new Subscriber[n];
            for (int i = 0; i < n; i++) {
                Subscriber<? super T> z = s[i];
                if (z instanceof ConditionalSubscriber) {
                    parents[i] = new OnAssemblyConditionalSubscriber<T>((ConditionalSubscriber<? super T>)z, assembled);
                } else {
                    parents[i] = new OnAssemblySubscriber<T>(z, assembled);
                }
            }

            source.subscribe(parents);
        }
    }
}
