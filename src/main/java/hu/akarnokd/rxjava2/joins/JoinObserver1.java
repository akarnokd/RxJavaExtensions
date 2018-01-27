/*
 * Copyright 2016-2018 David Karnok
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

package hu.akarnokd.rxjava2.joins;

import java.util.*;
import java.util.concurrent.atomic.*;

import io.reactivex.Notification;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Default implementation of a join observer.
 */
final class JoinObserver1<T> implements Observer<Notification<T>>, JoinObserver {
    private Object gate;
    private final Observable<T> source;
    private final Consumer<Throwable> onError;
    private final List<ActivePlan0> activePlans;
    private final Queue<Notification<T>> queue;
    private final AtomicBoolean subscribed = new AtomicBoolean(false);
    private final InnerObserver safeObserver;

    JoinObserver1(Observable<T> source, Consumer<Throwable> onError) {
        this.source = source;
        this.onError = onError;
        queue = new LinkedList<Notification<T>>();
        activePlans = new ArrayList<ActivePlan0>();
        safeObserver = new InnerObserver();
        // add this subscription so it gets unsubscribed when the parent does
//        add(safeObserver);
    }

    public Queue<Notification<T>> queue() {
        return queue;
    }

    public void addActivePlan(ActivePlan0 activePlan) {
        activePlans.add(activePlan);
    }

    @Override
    public void subscribe(Object gate) {
        if (subscribed.compareAndSet(false, true)) {
            this.gate = gate;
            source.materialize().subscribe(this);
        } else {
            throw new IllegalStateException("Can only be subscribed to once.");
        }
    }

    @Override
    public void dequeue() {
        queue.remove();
    }


    @Override
    public void onNext(Notification<T> args) {
        safeObserver.onNext(args);
    }

    @Override
    public void onError(Throwable e) {
        safeObserver.onError(e);
    }

    @Override
    public void onComplete() {
        safeObserver.onComplete();
    }

    void removeActivePlan(ActivePlan0 activePlan) {
        activePlans.remove(activePlan);
        if (activePlans.isEmpty()) {
            dispose();
        }
    }

    @Override
    public void onSubscribe(Disposable d) {
        safeObserver.onSubscribe(d);
    }

    @Override
    public boolean isDisposed() {
        return DisposableHelper.isDisposed(safeObserver.get());
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(safeObserver);
    }

    final class InnerObserver
    extends AtomicReference<Disposable>
    implements Observer<Notification<T>> {

        private static final long serialVersionUID = -1466017793444404254L;

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }

        @Override
        public void onNext(Notification<T> args) {
            synchronized (gate) {
                if (!DisposableHelper.isDisposed(get())) {
                    if (args.isOnError()) {
                        try {
                            onError.accept(args.getError());
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            RxJavaPlugins.onError(new CompositeException(args.getError(), ex));
                        }
                        return;
                    }
                    queue.add(args);

                    // remark: activePlans might change while iterating
                    for (ActivePlan0 a : new ArrayList<ActivePlan0>(activePlans)) {
                        try {
                            a.match();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            try {
                                onError.accept(ex);
                            } catch (Throwable ex2) {
                                Exceptions.throwIfFatal(ex2);
                                RxJavaPlugins.onError(new CompositeException(ex, ex2));
                                return;
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            // not expected
        }

        @Override
        public void onComplete() {
            // not expected or ignored
        }
    }

}