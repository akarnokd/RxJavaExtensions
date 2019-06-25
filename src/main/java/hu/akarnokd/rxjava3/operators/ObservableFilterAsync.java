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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Filters elements of the upstream by mapping each of them, one after the other, onto a
 * an inner ObservableSource which should signal a boolean true if the associated item
 * should be passed to the downstream.
 *
 * @param <T> the upstream value type
 * @param <U> the inner Observable's element type
 * @param <R> the result element type
 * @since 0.20.4
 */
final class ObservableFilterAsync<T> extends Observable<T>
implements ObservableTransformer<T, T> {

    final ObservableSource<T> source;

    final Function<? super T, ? extends ObservableSource<Boolean>> asyncPredicate;

    final int capacityHint;

    ObservableFilterAsync(ObservableSource<T> source,
            Function<? super T, ? extends ObservableSource<Boolean>> asyncPredicate,
            int capacityHint) {
        super();
        this.source = source;
        this.asyncPredicate = asyncPredicate;
        this.capacityHint = capacityHint;
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return new ObservableFilterAsync<T>(upstream, asyncPredicate, capacityHint);
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new FilterAsyncObserver<T>(observer, asyncPredicate, capacityHint));
    }

    static final class FilterAsyncObserver<T> extends AtomicInteger implements Observer<T>, Disposable {

        private static final long serialVersionUID = -204261674817426393L;

        final Observer<? super T> downstream;

        final Function<? super T, ? extends ObservableSource<Boolean>> asyncPredicate;

        final SpscLinkedArrayQueue<T> queue;

        final AtomicThrowable errors;

        final AtomicReference<Disposable> innerDisposable;

        Disposable upstream;

        volatile boolean done;
        volatile boolean disposed;

        T current;

        volatile int state;

        static final int STATE_FRESH = 0;
        static final int STATE_RUNNING = 1;
        static final int STATE_TRUE = 2;
        static final int STATE_FALSE = 3;

        FilterAsyncObserver(
                Observer<? super T> downstream,
                Function<? super T, ? extends ObservableSource<Boolean>> asyncPredicate,
                int capacityHint) {
            this.downstream = downstream;
            this.asyncPredicate = asyncPredicate;
            this.queue = new SpscLinkedArrayQueue<T>(capacityHint);
            this.errors = new AtomicThrowable();
            this.innerDisposable = new AtomicReference<Disposable>();
        }

        @Override
        public void dispose() {
            disposed = true;
            upstream.dispose();
            DisposableHelper.dispose(innerDisposable);
            drain();
        }

        @Override
        public boolean isDisposed() {
            return disposed;
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
            queue.offer(t);
            drain();
        }

        @Override
        public void onError(Throwable e) {
            DisposableHelper.dispose(innerDisposable);
            if (errors.addThrowable(e)) {
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        public void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;

            for (;;) {
                if (disposed) {
                    current = null;
                    queue.clear();
                } else {
                    if (errors.get() != null) {
                        Throwable ex = errors.terminate();
                        disposed = true;
                        downstream.onError(ex);
                        continue;
                    }
                    int s = state;
                    if (s == STATE_FRESH) {
                        boolean d = done;
                        T item = queue.poll();
                        boolean empty = item == null;

                        if (d && empty) {
                            downstream.onComplete();
                        } else
                        if (!empty) {
                            current = item;

                            ObservableSource<Boolean> innerSource;

                            try {
                                innerSource = ObjectHelper.requireNonNull(asyncPredicate.apply(item), "The mapper returned a null ObservableSource");
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                upstream.dispose();
                                errors.addThrowable(ex);
                                ex = errors.terminate();
                                disposed = true;
                                downstream.onError(ex);
                                continue;
                            }

                            state = STATE_RUNNING;
                            innerSource.subscribe(new InnerObserver());
                        }
                    } else
                    if (s == STATE_TRUE) {
                        T mainItem = current;
                        current = null;

                        downstream.onNext(mainItem);
                        state = STATE_FRESH;
                        continue;
                    } else
                    if (s == STATE_FALSE) {
                        current = null;
                        state = STATE_FRESH;
                        continue;
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        void innerSuccess(boolean value) {
            state = value ? STATE_TRUE : STATE_FALSE;
            DisposableHelper.replace(innerDisposable, null);
            drain();
        }

        void innerError(Throwable ex) {
            if (errors.addThrowable(ex)) {
                state = STATE_FALSE;
                DisposableHelper.replace(innerDisposable, null);
                upstream.dispose();
                drain();
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        void innerComplete() {
            state = STATE_FALSE;
            DisposableHelper.replace(innerDisposable, null);
            drain();
        }

        final class InnerObserver implements Observer<Boolean> {

            boolean once;

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(innerDisposable, d);
            }

            @Override
            public void onNext(Boolean t) {
                if (!once) {
                    once = true;
                    innerDisposable.get().dispose();
                    innerSuccess(t);
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!once) {
                    innerError(e);
                } else {
                    RxJavaPlugins.onError(e);
                }
            }

            @Override
            public void onComplete() {
                if (!once) {
                    innerComplete();
                }
            }
        }
    }
}
