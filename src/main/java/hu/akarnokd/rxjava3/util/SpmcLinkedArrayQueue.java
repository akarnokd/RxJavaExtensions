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

import java.util.Objects;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.operators.SimplePlainQueue;

/**
 * A single-producer multiple-conumer queue implementation with array islands.
 *
 * @param <T> the item type to be queued
 * @since 0.18.8
 */
public final class SpmcLinkedArrayQueue<T> implements SimplePlainQueue<T> {

    ARA producerArray;

    int producerIndex;

    final AtomicReference<ARA> consumerArray;

    public SpmcLinkedArrayQueue(int capacity) {
        ARA a = new ARA(Math.max(2, capacity) + 1);
        this.producerArray = a;
        this.consumerArray = new AtomicReference<>(a);
    }

    @Override
    public boolean offer(T value) {
        Objects.requireNonNull(value, "value is null");
        ARA pa = producerArray;
        int pi = producerIndex;

        if (pi == pa.length() - 1) {
            ARA next = new ARA(pa.length());
            producerArray = next;
            next.lazySet(0, value);
            pa.soNext(next);
            pi = 1;
        } else {
            pa.lazySet(pi, value);
            pi++;
        }
        producerIndex = pi;
        return true;
    }

    @Override
    public boolean offer(T v1, T v2) {
        Objects.requireNonNull(v1, "v1 is null");
        Objects.requireNonNull(v2, "v2 is null");

        ARA pa = producerArray;
        int pi = producerIndex;

        if (pi == pa.length() - 1) {
            ARA next = new ARA(pa.length());
            producerArray = next;
            next.lazySet(0, v1);
            next.lazySet(1, v2);
            pa.soNext(next);
            pi = 2;
        } else {
            pa.lazySet(pi + 1, v2);
            pa.lazySet(pi, v1);
            pi += 2;
        }
        producerIndex = pi;
        return true;
    }

    @Override
    public boolean isEmpty() {
        AtomicReference<ARA> ca = consumerArray;
        ARA a = ca.get();
        AtomicInteger index = a.index;

        for (;;) {
            int idx = index.get();
            if (idx < a.length() - 1) {
                Object o = a.get(idx);
                if (idx == index.get()) {
                    return o == null;
                }
            } else {
                ARA b = a.lvNext();
                if (b != null) {
                    a = b;
                    index = b.index;
                } else {
                    return true;
                }
            }
        }
    }

    @Override
    public void clear() {
        while (poll() != null) { }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T poll() {
        AtomicReference<ARA> ca = consumerArray;
        ARA a = ca.get();
        AtomicInteger index = a.index;

        for (;;) {
            int idx = index.get();
            if (idx < a.length() - 1) {
                Object o = a.get(idx);
                if (idx == index.get()) {
                    if (o == null) {
                        return null;
                    }
                    if (index.compareAndSet(idx, idx + 1)) {
                        a.lazySet(idx, null);
                        return (T)o;
                    }
                }
            } else {
                ARA b = a.lvNext();
                if (b != null) {
                    ca.compareAndSet(a, b);
                    a = ca.get();
                    index = a.index;
                } else {
                    return null;
                }
            }
        }
    }

    static final class ARA extends AtomicReferenceArray<Object> {

        private static final long serialVersionUID = 5627139329189102514L;

        final AtomicInteger index;

        ARA(int capacity) {
            super(capacity);
            this.index = new AtomicInteger();
        }

        ARA lvNext() {
            return (ARA)get(length() - 1);
        }

        void soNext(ARA next) {
            lazySet(length() - 1, next);
        }
    }
}
