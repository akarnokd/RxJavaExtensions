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

package hu.akarnokd.rxjava2.basetypes;

import org.reactivestreams.*;

import io.reactivex.internal.subscribers.DeferredScalarSubscriber;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Signals the 0 or 1 value of the source Publisher.
 *
 * @param <T> the value type
 */
final class PerhapsFromPublisher<T> extends Perhaps<T> {

    final Publisher<T> source;

    PerhapsFromPublisher(Publisher<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new FromPublisherSubscriber<T>(s));
    }

    static final class FromPublisherSubscriber<T> extends DeferredScalarSubscriber<T, T> {

        private static final long serialVersionUID = 1473656799413159020L;

        boolean done;

        FromPublisherSubscriber(Subscriber<? super T> actual) {
            super(actual);
        }

        @Override
        public void onNext(T t) {
            if (!done) {
                if (hasValue) {
                    s.cancel();
                    onError(new IndexOutOfBoundsException());
                } else {
                    hasValue = true;
                    value = t;
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
            } else {
                value = null;
                done = true;
                actual.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                if (hasValue) {
                    done = true;
                    complete(value);
                } else {
                    actual.onComplete();
                }
            }
        }
    }
 }
