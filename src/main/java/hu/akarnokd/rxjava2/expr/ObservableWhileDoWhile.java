/*
 * Copyright 2016 David Karnok
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

package hu.akarnokd.rxjava2.expr;

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.internal.disposables.*;

final class ObservableWhileDoWhile<T> extends Observable<T> {

    final ObservableSource<? extends T> source;

    final BooleanSupplier preCondition;

    final BooleanSupplier postCondition;

    ObservableWhileDoWhile(ObservableSource<? extends T> source, BooleanSupplier preCondition,
            BooleanSupplier postCondition) {
        this.source = source;
        this.preCondition = preCondition;
        this.postCondition = postCondition;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        boolean b;

        try {
            b = preCondition.getAsBoolean();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, observer);
            return;
        }

        if (b) {
            WhileDoWhileObserver<T> parent = new WhileDoWhileObserver<T>(observer, postCondition, source);
            observer.onSubscribe(parent);
            parent.subscribeNext();
        } else {
            EmptyDisposable.complete(observer);
        }
    }

    static final class WhileDoWhileObserver<T>
    extends AtomicReference<Disposable>
    implements Observer<T>, Disposable {

        private static final long serialVersionUID = -5255585317630843019L;

        final Observer<? super T> actual;

        final AtomicInteger wip;

        final BooleanSupplier postCondition;

        final ObservableSource<? extends T> source;

        volatile boolean active;

        WhileDoWhileObserver(Observer<? super T> actual, BooleanSupplier postCondition, ObservableSource<? extends T> source) {
            this.actual = actual;
            this.wip = new AtomicInteger();
            this.postCondition = postCondition;
            this.source = source;
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.replace(this, d);
        }

        @Override
        public void onNext(T value) {
            actual.onNext(value);
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }

        @Override
        public void onComplete() {

            boolean b;

            try {
                b = postCondition.getAsBoolean();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                actual.onError(ex);
                return;
            }

            if (b) {
                active = false;
                subscribeNext();
            } else {
                actual.onComplete();
            }
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        void subscribeNext() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            for (;;) {

                if (isDisposed()) {
                    return;
                }

                if (!active) {
                    active = true;
                    source.subscribe(this);
                }

                if (wip.decrementAndGet() == 0) {
                    break;
                }
            }
        }
    }
}
