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
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.Consumer;

/**
 * Validates a ConnectableFlowable.
 * @param <T> the value type
 * @since 0.17.4
 */
final class ConnectableFlowableValidator<T> extends ConnectableFlowable<T> {

    final ConnectableFlowable<T> source;

    final PlainConsumer<ProtocolNonConformanceException> onViolation;

    ConnectableFlowableValidator(ConnectableFlowable<T> source, PlainConsumer<ProtocolNonConformanceException> onViolation) {
        this.source = source;
        this.onViolation = onViolation;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new FlowableValidator.ValidatorConsumer<T>(s, onViolation));
    }

    @Override
    public void connect(Consumer<? super Disposable> connection) {
        source.connect(connection);
    }

    @Override
    public void reset() {
        source.reset();
    }
}
