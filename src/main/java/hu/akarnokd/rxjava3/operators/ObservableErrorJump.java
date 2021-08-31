/*
 * Copyright 2016-present David Karnok
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

package hu.akarnokd.rxjava3.operators;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;

/**
 * Makes sure an upstream error skips the flow created with a
 * transformer and then re-emitted when the inner flow terminates.
 * <p>
 * This operator is like an external delay-error behavior, allowing
 * an inner transformed flow to complete normally but still fail the
 * flow further down.
 * @param <T> the upstream value type
 * @param <R> the downstream value type
 * @since 0.19.1
 */
final class ObservableErrorJump<T, R> extends Observable<R> implements ObservableTransformer<T, R> {

    final Observable<T> source;

    final ObservableTransformer<T, R> transformer;

    ObservableErrorJump(Observable<T> source, ObservableTransformer<T, R> transformer) {
        this.source = source;
        this.transformer = transformer;
    }

    @Override
    public ObservableSource<R> apply(Observable<T> upstream) {
        return new ObservableErrorJump<>(upstream, transformer);
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) {

        ErrorJumpFront<T, R> front = new ErrorJumpFront<>(source, observer);

        ObservableSource<R> downstream;

        try {
            downstream = Objects.requireNonNull(
                    transformer.apply(front),
                    "The transformer returned a null Publisher");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, observer);
            return;
        }

        downstream.subscribe(front.end);
    }

    static final class ErrorJumpFront<T, R> extends Observable<T> implements Observer<T>, Disposable {

        final Observable<T> source;

        final AtomicReference<Observer<? super T>> middle;

        final EndSubscriber end;

        Disposable upstream;

        ErrorJumpFront(Observable<T> source, Observer<? super R> downstream) {
            this.source = source;
            this.middle = new AtomicReference<>();
            this.end = new EndSubscriber(downstream);
        }

        @Override
        protected void subscribeActual(Observer<? super T> observer) {
            if (middle.compareAndSet(null, observer)) {
                source.subscribe(this);
            } else {
                EmptyDisposable.error(new IllegalStateException("Only one Subscriber allowed"), observer);
            }
        }

        @Override
        public void onSubscribe(Disposable d) {
            upstream = d;
            middle.get().onSubscribe(this);
        }

        @Override
        public void onNext(T t) {
            middle.get().onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            end.set(t);
            middle.get().onComplete();
        }

        @Override
        public void onComplete() {
            middle.get().onComplete();
        }

        @Override
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        final class EndSubscriber
        extends AtomicReference<Throwable>
        implements Observer<R>, Disposable {

            private static final long serialVersionUID = -5718512540714037078L;

            final Observer<? super R> downstream;

            Disposable upstream;

            EndSubscriber(Observer<? super R> downstream) {
                this.downstream = downstream;
            }

            @Override
            public void onSubscribe(Disposable d) {
                upstream = d;
                downstream.onSubscribe(this);
            }

            @Override
            public void onNext(R t) {
                downstream.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                Throwable ex = get();
                if (ex != null) {
                    t = new CompositeException(ex, t);
                }
                downstream.onError(t);
            }

            @Override
            public void onComplete() {
                Throwable ex = get();
                if (ex != null) {
                    downstream.onError(ex);
                } else {
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
}
