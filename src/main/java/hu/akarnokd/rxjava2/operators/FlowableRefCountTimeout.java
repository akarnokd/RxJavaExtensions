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

package hu.akarnokd.rxjava2.operators;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * A refCount implementation that allows connecting to the source after the specified
 * number of Subscribers subscribed and allows disconnecting after a specified
 * grace period.
 * @param <T> the input and output element type
 * @since 0.17.0
 */
final class FlowableRefCountTimeout<T> extends Flowable<T> implements FlowableTransformer<T, T> {

    final ConnectableFlowable<T> source;

    final int n;

    final long timeout;

    final TimeUnit unit;

    final Scheduler scheduler;

    RefConnection connection;

    FlowableRefCountTimeout(ConnectableFlowable<T> source, int n, long timeout, TimeUnit unit,
            Scheduler scheduler) {
        this.source = source;
        this.n = n;
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        if (upstream instanceof ConnectableFlowable) {
            return new FlowableRefCountTimeout<T>((ConnectableFlowable<T>)upstream, n, timeout, unit, scheduler);
        }
        throw new IllegalArgumentException("This transformer requires an upstream ConnectableFlowable");
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {

        for (;;) {
            RefConnection conn;

            boolean connect = false;
            synchronized (this) {
                conn = connection;
                if (conn == null || conn.terminated) {
                    conn = new RefConnection(this);
                    connection = conn;
                }

                long c = conn.subscriberCount;
                if (c == 0L && conn.timer != null) {
                    conn.timer.dispose();
                }
                conn.subscriberCount = c + 1;
                if (!conn.connected && c + 1 == n) {
                    connect = true;
                    conn.connected = true;
                }
            }

            source.subscribe(new RefCountSubscriber<T>(s, this, conn));

            if (connect) {
                source.connect(conn);
            }

            break;
        }
    }

    void cancel(RefConnection rc) {
        SequentialDisposable sd;
        synchronized (this) {
            if (rc.terminated) {
                return;
            }
            long c = rc.subscriberCount - 1;
            rc.subscriberCount = c;
            if (c != 0L || !rc.connected) {
                return;
            }
            if (timeout == 0L) {
                timeout(rc);
                return;
            }
            sd = new SequentialDisposable();
            rc.timer = sd;
        }

        sd.replace(scheduler.scheduleDirect(rc, timeout, unit));
    }

    void terminated(RefConnection rc) {
        synchronized (this) {
            if (!rc.terminated) {
                rc.terminated = true;
                if (source instanceof Disposable) {
                    ((Disposable)source).dispose();
                }
                connection = null;
            }
        }
    }

    void timeout(RefConnection rc) {
        synchronized (this) {
            if (rc.subscriberCount == 0 && rc == connection) {
                DisposableHelper.dispose(rc);
                if (source instanceof Disposable) {
                    ((Disposable)source).dispose();
                }
                connection = null;
            }
        }
    }

    static final class RefConnection extends AtomicReference<Disposable>
    implements Runnable, Consumer<Disposable> {

        private static final long serialVersionUID = -4552101107598366241L;

        final FlowableRefCountTimeout<?> parent;

        Disposable timer;

        long subscriberCount;

        boolean connected;

        boolean terminated;

        RefConnection(FlowableRefCountTimeout<?> parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            parent.timeout(this);
        }

        @Override
        public void accept(Disposable t) throws Exception {
            DisposableHelper.replace(this, t);
        }
    }

    static final class RefCountSubscriber<T>
    extends AtomicBoolean implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -7419642935409022375L;

        final Subscriber<? super T> actual;

        final FlowableRefCountTimeout<T> parent;

        final RefConnection connection;

        Subscription upstream;

        RefCountSubscriber(Subscriber<? super T> actual, FlowableRefCountTimeout<T> parent, RefConnection connection) {
            this.actual = actual;
            this.parent = parent;
            this.connection = connection;
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (compareAndSet(false, true)) {
                parent.terminated(connection);
            }
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (compareAndSet(false, true)) {
                parent.terminated(connection);
            }
            actual.onComplete();
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
            if (compareAndSet(false, true)) {
                parent.cancel(connection);
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(upstream, s)) {
                this.upstream = s;

                actual.onSubscribe(this);
            }
        }
    }
}
