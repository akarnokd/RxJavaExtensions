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

import hu.akarnokd.rxjava2.functions.PlainConsumer;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;

/**
 * Validates a Completable.
 * @since 0.17.4
 */
final class CompletableValidator extends Completable {

    final Completable source;

    final PlainConsumer<ProtocolNonConformanceException> onViolation;

    CompletableValidator(Completable source, PlainConsumer<ProtocolNonConformanceException> onViolation) {
        this.source = source;
        this.onViolation = onViolation;
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        source.subscribe(new ValidatorConsumer(observer, onViolation));
    }

    static final class ValidatorConsumer implements CompletableObserver, Disposable {

        final CompletableObserver downstream;

        final PlainConsumer<ProtocolNonConformanceException> onViolation;

        Disposable upstream;

        boolean done;

        ValidatorConsumer(CompletableObserver downstream,
                PlainConsumer<ProtocolNonConformanceException> onViolation) {
            super();
            this.downstream = downstream;
            this.onViolation = onViolation;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (d == null) {
                onViolation.accept(new NullOnSubscribeParameterException());
            }
            Disposable u = upstream;
            if (u != null) {
                onViolation.accept(new MultipleOnSubscribeCallsException());
            }
            upstream = d;
            downstream.onSubscribe(this);
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
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }
    }
}
