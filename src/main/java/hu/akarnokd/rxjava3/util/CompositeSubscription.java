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

package hu.akarnokd.rxjava3.util;

import org.reactivestreams.Subscription;

import io.reactivex.internal.util.OpenHashSet;

/**
 * Container for Subscriptions and atomic operations on them.
 */
public final class CompositeSubscription implements Subscription {

    OpenHashSet<Subscription> set;

    volatile boolean cancelled;

    public boolean add(Subscription s) {
        if (!cancelled) {
            synchronized (this) {
                if (!cancelled) {
                    OpenHashSet<Subscription> h = set;
                    if (h == null) {
                        h = new OpenHashSet<Subscription>();
                        set = h;
                    }
                    h.add(s);
                    return true;
                }
            }
        }
        s.cancel();
        return false;
    }

    public void delete(Subscription s) {
        if (!cancelled) {
            synchronized (this) {
                if (!cancelled) {
                    OpenHashSet<Subscription> h = set;
                    if (h != null) {
                        h.remove(s);
                    }
                }
            }
        }
    }

    @Override
    public void request(long n) {
        // ignored
    }

    @Override
    public void cancel() {
        if (!cancelled) {
            OpenHashSet<Subscription> h;
            synchronized (this) {
                if (cancelled) {
                    return;
                }
                h = set;
                set = null;
                cancelled = true;
            }

            if (h != null) {
                Object[] array = h.keys();
                for (Object o : array) {
                    if (o != null) {
                        ((Subscription)o).cancel();
                    }
                }
            }
        }
    }
}
