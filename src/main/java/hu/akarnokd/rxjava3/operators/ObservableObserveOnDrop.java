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
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * Schedules the event emission on a Scheduler and drops upstream values while
 * the {@code onNext} with the current item is executing on the given scheduler.
 * 
 * @param <T> the item type
 * @since 0.18.7
 */
final class ObservableObserveOnDrop<T> extends Observable<T> implements ObservableTransformer<T, T> {

    final Observable<T> source;

    final Scheduler scheduler;

    ObservableObserveOnDrop(Observable<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return new ObservableObserveOnDrop<T>(upstream, scheduler);
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new ObserveOnDropObserver<T>(observer, scheduler.createWorker()));
    }

    static final class ObserveOnDropObserver<T> extends AtomicInteger
    implements Observer<T>, Disposable, Runnable {

        private static final long serialVersionUID = -11696478502477044L;

        final Observer<? super T> downstream;

        final Worker worker;

        final AtomicReference<Object> item;

        static final Object WORKING = new Object();

        Disposable upstream;

        volatile boolean disposed;

        volatile boolean done;

        Throwable error;

        ObserveOnDropObserver(Observer<? super T> downstream, Worker worker) {
            this.downstream = downstream;
            this.worker = worker;
            this.item = new AtomicReference<Object>();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(upstream, d)) {
                upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (item.compareAndSet(null, t)) {
                drain();
            }
        }

        @Override
        public void onError(Throwable e) {
            error = e;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public void dispose() {
            disposed = true;
            upstream.dispose();
            worker.dispose();
            if (getAndIncrement() == 0) {
                item.lazySet(WORKING);
            }
        }

        void drain() {
            if (getAndIncrement() == 0) {
                worker.schedule(this);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            int missed = 1;
            Observer<? super T> a = downstream;

            for (;;) {

                for (;;) {
                    if (disposed) {
                        item.lazySet(WORKING);
                        return;
                    }

                    boolean d = done;
                    Object v = item.get();
                    boolean empty = v == null;

                    if (d && empty) {
                        Throwable ex = error;
                        if (ex == null) {
                            a.onComplete();
                        } else {
                            a.onError(ex);
                        }
                        worker.dispose();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    v = item.getAndSet(WORKING);

                    a.onNext((T)v);

                    item.set(null);
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }
}
