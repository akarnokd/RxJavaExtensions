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

package hu.akarnokd.rxjava3.basetypes;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * Subscribe to the upstream on the specified scheduler.
 *
 * @param <T> the value type
 */
final class SoloSubscribeOn<T> extends Solo<T> {

    final Solo<T> source;

    final Scheduler scheduler;

    SoloSubscribeOn(Solo<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        Worker worker = scheduler.createWorker();

        SubscribeOnSubscriber<T> parent = new SubscribeOnSubscriber<>(s, worker, source);
        s.onSubscribe(parent);

        DisposableHelper.replace(parent.task, worker.schedule(parent));
    }

    static final class SubscribeOnSubscriber<T>
    extends AtomicReference<Subscription>
    implements Subscriber<T>, Runnable, Subscription {

        private static final long serialVersionUID = 2047863608816341143L;

        final Subscriber<? super T> downstream;

        final Worker worker;

        final AtomicReference<Disposable> task;

        final Publisher<T> source;

        final AtomicBoolean requested;

        SubscribeOnSubscriber(Subscriber<? super T> downstream, Worker worker, Publisher<T> source) {
            this.downstream = downstream;
            this.worker = worker;
            this.source = source;
            this.task = new AtomicReference<>();
            this.requested = new AtomicBoolean();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {
                if (requested.getAndSet(false)) {
                    scheduleRequest();
                }
            }
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
            worker.dispose();
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
            worker.dispose();
        }

        @Override
        public void run() {
            source.subscribe(this);
        }

        void scheduleRequest() {
            worker.schedule(new Runnable() {
                @Override
                public void run() {
                    get().request(Long.MAX_VALUE);
                }
            });
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                Subscription s = get();
                if (s != null) {
                    scheduleRequest();
                } else {
                    requested.set(true);
                    s = get();
                    if (s != null) {
                        if (requested.getAndSet(false)) {
                            scheduleRequest();
                        }
                    }
                }
            }
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(this);
            DisposableHelper.dispose(task);
            worker.dispose();
        }
    }
}
