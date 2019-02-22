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

package hu.akarnokd.rxjava2.basetypes;

import java.util.concurrent.*;

import org.reactivestreams.*;

import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Blocks until the source terminates and returns the (last) value it produced if any.
 *
 * @param <T> the value type
 */
final class BlockingGetSubscriber<T> extends CountDownLatch implements Subscriber<T> {

    volatile boolean cancelled;

    Subscription upstream;

    T value;
    Throwable error;

    BlockingGetSubscriber() {
        super(1);
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validate(this.upstream, s)) {
            this.upstream = s;
            if (cancelled) {
                s.cancel();
            } else {
                s.request(Long.MAX_VALUE);
            }
        }
    }

    @Override
    public void onNext(T t) {
        value = t;
    }

    @Override
    public void onError(Throwable t) {
        error = t;
        countDown();
    }

    @Override
    public void onComplete() {
        countDown();
    }

    void dispose() {
        cancelled = true;
        Subscription a = upstream;
        if (a != null) {
            a.cancel();
        }
    }

    T blockingGet() {
        if (getCount() != 0) {
            try {
                await();
            } catch (InterruptedException ex) {
                dispose();
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }
        Throwable ex = error;
        if (ex != null) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
        return value;
    }

    T blockingGet(long timeout, TimeUnit unit) {
        if (getCount() != 0) {
            try {
                if (!await(timeout, unit)) {
                    dispose();
                    throw ExceptionHelper.wrapOrThrow(new TimeoutException());
                }
            } catch (InterruptedException ex) {
                dispose();
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }
        Throwable ex = error;
        if (ex != null) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
        return value;
    }

    void blockingCall(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Action onComplete) {
        if (getCount() != 0) {
            try {
                await();
            } catch (InterruptedException ex) {
                dispose();
                try {
                    onError.accept(ex);
                } catch (Throwable exc) {
                    Exceptions.throwIfFatal(exc);
                    RxJavaPlugins.onError(new CompositeException(ex, exc));
                }
                return;
            }
        }
        Throwable ex = error;
        if (ex != null) {
            try {
                onError.accept(ex);
            } catch (Throwable exc) {
                Exceptions.throwIfFatal(exc);
                RxJavaPlugins.onError(new CompositeException(ex, exc));
            }
            return;
        }
        T v = value;
        if (v != null) {
            try {
                onNext.accept(v);
            } catch (Throwable exc) {
                Exceptions.throwIfFatal(exc);

                try {
                    onError.accept(exc);
                } catch (Throwable excc) {
                    Exceptions.throwIfFatal(excc);
                    RxJavaPlugins.onError(new CompositeException(exc, excc));
                }

                return;
            }
        }
        try {
            onComplete.run();
        } catch (Throwable exc) {
            Exceptions.throwIfFatal(exc);
            RxJavaPlugins.onError(exc);
        }
    }
}
