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

package hu.akarnokd.rxjava3.subjects;

import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.subjects.Subject;

/**
 * A Subject wrapper that disposes the Disposable set via
 * onSubscribe if the number of observers reaches zero.
 *
 * @param <T> the upstream and downstream value type
 * @since 0.18.2
 */
/* public */final class RefCountSubject<T> extends Subject<T> implements Disposable {

    final Subject<T> actual;

    final AtomicReference<Disposable> upstream;

    final AtomicReference<RefCountObserver<T>[]> observers;

    @SuppressWarnings("rawtypes")
    static final RefCountObserver[] EMPTY = new RefCountObserver[0];

    @SuppressWarnings("rawtypes")
    static final RefCountObserver[] TERMINATED = new RefCountObserver[0];

    @SuppressWarnings("unchecked")
    RefCountSubject(Subject<T> actual) {
        this.actual = actual;
        this.upstream = new AtomicReference<>();
        this.observers = new AtomicReference<>(EMPTY);
    }

    @Override
    public void onSubscribe(Disposable d) {
        if (DisposableHelper.setOnce(upstream, d)) {
            actual.onSubscribe(this);
        }
    }

    @Override
    public void onNext(T t) {
        actual.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        upstream.lazySet(DisposableHelper.DISPOSED);
        actual.onError(t);
    }

    @Override
    public void onComplete() {
        upstream.lazySet(DisposableHelper.DISPOSED);
        actual.onComplete();
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        RefCountObserver<T> rcs = new RefCountObserver<>(observer, this);
        if (!add(rcs)) {
            EmptyDisposable.error(new IllegalStateException("RefCountSubject terminated"), observer);
            return;
        }
        actual.subscribe(rcs);
    }

    @Override
    public boolean hasComplete() {
        return actual.hasComplete();
    }

    @Override
    public boolean hasThrowable() {
        return actual.hasThrowable();
    }

    @Override
    public Throwable getThrowable() {
        return actual.getThrowable();
    }

    @Override
    public boolean hasObservers() {
        return actual.hasObservers();
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(upstream);
    }

    @Override
    public boolean isDisposed() {
        return DisposableHelper.isDisposed(upstream.get());
    }

    boolean add(RefCountObserver<T> rcs) {
        for (;;) {
            RefCountObserver<T>[] a = observers.get();
            if (a == TERMINATED) {
                return false;
            }
            int n = a.length;
            @SuppressWarnings("unchecked")
            RefCountObserver<T>[] b = new RefCountObserver[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = rcs;
            if (observers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    void remove(RefCountObserver<T> rcs) {
        for (;;) {
            RefCountObserver<T>[] a = observers.get();
            int n = a.length;
            if (n == 0) {
                break;
            }
            int j = -1;

            for (int i = 0; i < n; i++) {
                if (rcs == a[i]) {
                    j = i;
                    break;
                }
            }

            if (j < 0) {
                break;
            }

            RefCountObserver<T>[] b;
            if (n == 1) {
                b = TERMINATED;
            } else {
                b = new RefCountObserver[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (observers.compareAndSet(a, b)) {
                if (b == TERMINATED) {
                    dispose();
                }
                break;
            }
        }
    }

    static final class RefCountObserver<T> extends AtomicBoolean implements Observer<T>, Disposable {

        private static final long serialVersionUID = -4317488092687530631L;

        final Observer<? super T> downstream;

        final RefCountSubject<T> parent;

        Disposable upstream;

        RefCountObserver(Observer<? super T> downstream, RefCountSubject<T> parent) {
            this.downstream = downstream;
            this.parent = parent;
        }

        @Override
        public void dispose() {
            lazySet(true);
            upstream.dispose();
            parent.remove(this);
        }

        @Override
        public void onSubscribe(Disposable d) {
            this.upstream = d;
            downstream.onSubscribe(this);
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }
    }
}
