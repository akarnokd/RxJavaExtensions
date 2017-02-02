/*
 * Copyright 2016-2017 David Karnok
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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

/**
 * Cache the terminal event of the upstream source.
 *
 * @since 0.14.1
 */
final class NonoCache extends Nono implements Subscriber<Void> {

    static final CacheSubscription[] EMPTY = new CacheSubscription[0];

    static final CacheSubscription[] TERMINATED = new CacheSubscription[0];

    final Nono source;

    final AtomicBoolean once;

    final AtomicReference<CacheSubscription[]> subscribers;

    Throwable error;

    NonoCache(Nono source) {
        this.source = source;
        this.once = new AtomicBoolean();
        this.subscribers = new AtomicReference<CacheSubscription[]>(EMPTY);
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        CacheSubscription inner = new CacheSubscription(s);
        s.onSubscribe(inner);

        if (add(inner)) {
            if (inner.get() != 0) {
                remove(inner);
            }
            if (once.compareAndSet(false, true)) {
                source.subscribe(this);
            }
        } else {
            if (inner.get() == 0) {
                Throwable ex = error;
                if (ex != null) {
                    inner.actual.onError(ex);
                } else {
                    inner.actual.onComplete();
                }
            }
        }
    }

    boolean add(CacheSubscription inner) {
        for (;;) {
            CacheSubscription[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }
            int n = a.length;

            CacheSubscription[] b = new CacheSubscription[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = inner;
            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    void remove(CacheSubscription inner) {
        for (;;) {
            CacheSubscription[] a = subscribers.get();
            int n = a.length;
            if (n == 0) {
                break;
            }

            int j = -1;

            for (int i = 0; i < n; i++) {
                if (a[i] == inner) {
                    j = i;
                    break;
                }
            }

            if (j < 0) {
                break;
            }

            CacheSubscription[] b;
            if (n == 1) {
                b = EMPTY;
            } else {
                b = new CacheSubscription[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (subscribers.compareAndSet(a, b)) {
                break;
            }
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        // not used
    }

    @Override
    public void onNext(Void t) {
        // not called
    }

    @Override
    public void onError(Throwable t) {
        error = t;
        for (CacheSubscription inner : subscribers.getAndSet(TERMINATED)) {
            if (inner.get() == 0) {
                inner.actual.onError(t);
            }
        }
    }

    @Override
    public void onComplete() {
        for (CacheSubscription inner : subscribers.getAndSet(TERMINATED)) {
            if (inner.get() == 0) {
                inner.actual.onComplete();
            }
        }
    }

    final class CacheSubscription extends BasicNonoIntQueueSubscription {

        private static final long serialVersionUID = -5746624477415417500L;

        final Subscriber<? super Void> actual;

        CacheSubscription(Subscriber<? super Void> actual) {
            this.actual = actual;
        }

        @Override
        public void cancel() {
            if (compareAndSet(0, 1)) {
                remove(this);
            }
        }
    }
}
