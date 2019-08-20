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

package hu.akarnokd.rxjava3.operators;

import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.functions.ObjectHelper;
import io.reactivex.rxjava3.internal.util.AtomicThrowable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * FlatMap only one {@link ObservableSource} at a time and keep the latest upstream value until it terminates
 * and resume with the {@code ObservableSource} mapped for that latest upstream value.
 * @param <T> the upstream value type
 * @param <R> the output type
 * @since 0.19.0
 */
final class ObservableFlatMapLatest<T, R> extends Observable<R> implements ObservableTransformer<T, R> {

    final Observable<T> source;

    final Function<? super T, ? extends ObservableSource<? extends R>> mapper;

    ObservableFlatMapLatest(Observable<T> source,
            Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public ObservableSource<R> apply(Observable<T> upstream) {
        return new ObservableFlatMapLatest<T, R>(upstream, mapper);
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) {
        source.subscribe(new FlatMapLatestObserver<T, R>(observer, mapper));
    }

    static final class FlatMapLatestObserver<T, R>
    extends AtomicInteger
    implements Observer<T>, Disposable {

        private static final long serialVersionUID = 1251911925259779985L;

        final Observer<? super R> downstream;

        final Function<? super T, ? extends ObservableSource<? extends R>> mapper;

        final FlatMapLatestInnerObserver innerObserver;

        final AtomicThrowable errors;

        final AtomicReference<T> latest;

        Disposable upstream;

        volatile boolean active;

        volatile boolean done;

        volatile boolean disposed;

        FlatMapLatestObserver(Observer<? super R> downstream,
                Function<? super T, ? extends ObservableSource<? extends R>> mapper) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.innerObserver = new FlatMapLatestInnerObserver();
            this.errors = new AtomicThrowable();
            this.latest = new AtomicReference<T>();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            latest.set(t);
            drain();
        }

        @Override
        public void onError(Throwable e) {
            if (errors.addThrowable(e)) {
                onComplete();
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @Override
        public void dispose() {
            disposed = true;
            upstream.dispose();
            DisposableHelper.dispose(innerObserver);
            if (getAndIncrement() == 0) {
                latest.lazySet(null);
            }
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        void innerNext(R item) {
            downstream.onNext(item);
        }

        void innerError(Throwable e) {
            if (errors.addThrowable(e)) {
                innerComplete();
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        void innerComplete() {
            active = false;
            drain();
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            do {
                if (disposed) {
                    latest.lazySet(null);
                    return;
                }
                if (!active) {
                    boolean d = done;
                    T v = latest.getAndSet(null);
                    if (d && v == null) {
                        Throwable ex = errors.terminate();
                        if (ex == null) {
                            downstream.onComplete();
                        } else {
                            downstream.onError(ex);
                        }
                        return;
                    }
                    if (v != null) {
                        ObservableSource<? extends R> o;

                        try {
                            o = ObjectHelper.requireNonNull(mapper.apply(v), "The mapper returned a null ObservableSource");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            upstream.dispose();
                            errors.addThrowable(ex);
                            ex = errors.terminate();
                            downstream.onError(ex);
                            return;
                        }

                        active = true;
                        o.subscribe(innerObserver);
                    }
                }
            } while (decrementAndGet() != 0);
        }

        final class FlatMapLatestInnerObserver
        extends AtomicReference<Disposable>
        implements Observer<R> {

            private static final long serialVersionUID = -3707363807296094399L;

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.replace(this, d);
            }

            @Override
            public void onNext(R t) {
                innerNext(t);
            }

            @Override
            public void onError(Throwable e) {
                innerError(e);
            }

            @Override
            public void onComplete() {
                innerComplete();
            }
        }
    }
}
