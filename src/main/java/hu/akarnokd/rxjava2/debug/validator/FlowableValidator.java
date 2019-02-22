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

package hu.akarnokd.rxjava2.debug.validator;

import org.reactivestreams.*;

import hu.akarnokd.rxjava2.functions.PlainConsumer;
import io.reactivex.*;

/**
 * Validates a Flowable.
 * @param <T> the value type
 * @since 0.17.4
 */
final class FlowableValidator<T> extends Flowable<T> {

    final Flowable<T> source;

    final PlainConsumer<ProtocolNonConformanceException> onViolation;

    FlowableValidator(Flowable<T> source, PlainConsumer<ProtocolNonConformanceException> onViolation) {
        this.source = source;
        this.onViolation = onViolation;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new ValidatorConsumer<T>(s, onViolation));
    }

    static final class ValidatorConsumer<T> implements FlowableSubscriber<T>, Subscription {

        final Subscriber<? super T> downstream;

        final PlainConsumer<ProtocolNonConformanceException> onViolation;

        Subscription upstream;

        boolean done;

        ValidatorConsumer(Subscriber<? super T> downstream,
                PlainConsumer<ProtocolNonConformanceException> onViolation) {
            super();
            this.downstream = downstream;
            this.onViolation = onViolation;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (s == null) {
                onViolation.accept(new NullOnSubscribeParameterException());
            }
            Subscription u = upstream;
            if (u != null) {
                onViolation.accept(new MultipleOnSubscribeCallsException());
            }
            upstream = s;
            downstream.onSubscribe(this);
        }

        @Override
        public void onNext(T t) {
            if (t == null) {
                onViolation.accept(new NullOnNextParameterException());
            }
            if (upstream == null) {
                onViolation.accept(new OnSubscribeNotCalledException());
            }
            if (done) {
                onViolation.accept(new OnNextAfterTerminationException());
            } else {
                downstream.onNext(t);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (e == null) {
                onViolation.accept(new NullOnErrorParameterException());
            }
            if (upstream == null) {
                onViolation.accept(new OnSubscribeNotCalledException(e));
            }
            if (done) {
                onViolation.accept(new MultipleTerminationsException(e));
            } else {
                done = true;
                downstream.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (upstream == null) {
                OnSubscribeNotCalledException ex = new OnSubscribeNotCalledException();
                onViolation.accept(ex);
            }
            if (done) {
                onViolation.accept(new MultipleTerminationsException());
            } else {
                done = true;
                downstream.onComplete();
            }
        }

        @Override
        public void cancel() {
            upstream.cancel();
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }
    }
}
