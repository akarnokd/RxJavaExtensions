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

package hu.akarnokd.rxjava2.basetypes;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.disposables.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Run Nonos at the same time and terminate when one or all terminate.
 */
final class NonoMerge extends Nono {

    final Publisher<? extends Nono> sources;

    final boolean delayErrors;

    final int maxConcurrency;

    NonoMerge(Publisher<? extends Nono> sources, boolean delayErrors, int maxConcurrency) {
        this.sources = sources;
        this.delayErrors = delayErrors;
        this.maxConcurrency = maxConcurrency;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        sources.subscribe(new MergeSubscriber(s, delayErrors, maxConcurrency));
    }

    static final class MergeSubscriber extends BasicNonoIntQueueSubscription implements Subscriber<Nono> {

        private static final long serialVersionUID = 1247749138466245004L;

        final Subscriber<? super Void> actual;

        final CompositeDisposable set;

        final boolean delayErrors;

        final int maxConcurrency;

        final AtomicThrowable errors;

        Subscription s;

        MergeSubscriber(Subscriber<? super Void> actual, boolean delayErrors, int maxConcurrency) {
            this.actual = actual;
            this.delayErrors = delayErrors;
            this.maxConcurrency = maxConcurrency;
            this.set = new CompositeDisposable();
            this.errors = new AtomicThrowable();
            this.lazySet(1);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);

                int m = maxConcurrency;
                if (m == Integer.MAX_VALUE) {
                    s.request(Long.MAX_VALUE);
                } else {
                    s.request(m);
                }
            }
        }

        @Override
        public void onNext(Nono t) {
            getAndIncrement();
            MergeInnerSubscriber inner = new MergeInnerSubscriber();
            set.add(inner);
            t.subscribe(inner);
        }

        @Override
        public void onError(Throwable t) {
            if (errors.addThrowable(t)) {
                if (!delayErrors) {
                    set.dispose();

                    Throwable ex = errors.terminate();
                    if (ex != ExceptionHelper.TERMINATED) {
                        actual.onError(ex);
                    }
                } else {
                    onComplete();
                }
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (decrementAndGet() == 0) {
                Throwable ex = errors.terminate();
                if (ex != null) {
                    actual.onError(ex);
                } else {
                    actual.onComplete();
                }
            }
        }

        void innerComplete(Disposable inner) {
            set.delete(inner);
            complete();
        }

        void complete() {
            if (decrementAndGet() == 0) {
                Throwable ex = errors.terminate();
                if (ex != null) {
                    actual.onError(ex);
                } else {
                    actual.onComplete();
                }
            } else {
                s.request(1);
            }
        }

        void innerError(Disposable inner, Throwable error) {
            set.delete(inner);
            if (errors.addThrowable(error)) {
                if (!delayErrors) {
                    set.dispose();

                    Throwable ex = errors.terminate();
                    if (ex != ExceptionHelper.TERMINATED) {
                        actual.onError(ex);
                    }
                } else {
                    complete();
                }
            } else {
                RxJavaPlugins.onError(error);
            }
        }

        @Override
        public void cancel() {
            s.cancel();
            set.dispose();
        }

        final class MergeInnerSubscriber extends AtomicReference<Subscription> implements Subscriber<Void>, Disposable {

            private static final long serialVersionUID = -2042478764098922486L;

            @Override
            public void dispose() {
                SubscriptionHelper.cancel(this);
            }

            @Override
            public boolean isDisposed() {
                return SubscriptionHelper.isCancelled(get());
            }

            @Override
            public void onSubscribe(Subscription s) {
                SubscriptionHelper.setOnce(this, s);
            }

            @Override
            public void onNext(Void t) {
                // never called
            }

            @Override
            public void onError(Throwable t) {
                innerError(this, t);
            }

            @Override
            public void onComplete() {
                innerComplete(this);
            }
        }
    }
}
