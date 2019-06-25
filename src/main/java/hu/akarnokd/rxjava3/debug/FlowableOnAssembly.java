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

package hu.akarnokd.rxjava3.debug;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.subscribers.*;

/**
 * Wraps a Publisher and inject the assembly info.
 *
 * @param <T> the value type
 */
final class FlowableOnAssembly<T> extends Flowable<T> {

    final Publisher<T> source;

    final RxJavaAssemblyException assembled;

    FlowableOnAssembly(Publisher<T> source) {
        this.source = source;
        this.assembled = new RxJavaAssemblyException();
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            source.subscribe(new OnAssemblyConditionalSubscriber<T>((ConditionalSubscriber<? super T>)s, assembled));
        } else {
            source.subscribe(new OnAssemblySubscriber<T>(s, assembled));
        }
    }

    static final class OnAssemblySubscriber<T> extends BasicFuseableSubscriber<T, T> {

        final RxJavaAssemblyException assembled;

        OnAssemblySubscriber(Subscriber<? super T> downstream, RxJavaAssemblyException assembled) {
            super(downstream);
            this.assembled = assembled;
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(assembled.appendLast(t));
        }

        @Override
        public int requestFusion(int mode) {
            QueueSubscription<T> qs = this.qs;
            if (qs != null) {
                int m = qs.requestFusion(mode);
                sourceMode = m;
                return m;
            }
            return NONE;
        }

        @Override
        public T poll() throws Throwable {
            return qs.poll();
        }
    }

    static final class OnAssemblyConditionalSubscriber<T> extends BasicFuseableConditionalSubscriber<T, T> {

        final RxJavaAssemblyException assembled;

        OnAssemblyConditionalSubscriber(ConditionalSubscriber<? super T> downstream, RxJavaAssemblyException assembled) {
            super(downstream);
            this.assembled = assembled;
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public boolean tryOnNext(T t) {
            return downstream.tryOnNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(assembled.appendLast(t));
        }

        @Override
        public int requestFusion(int mode) {
            QueueSubscription<T> qs = this.qs;
            if (qs != null) {
                int m = qs.requestFusion(mode);
                sourceMode = m;
                return m;
            }
            return NONE;
        }

        @Override
        public T poll() throws Throwable {
            return qs.poll();
        }
    }
}
