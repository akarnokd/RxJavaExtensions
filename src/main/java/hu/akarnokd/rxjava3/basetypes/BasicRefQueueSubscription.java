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

package hu.akarnokd.rxjava3.basetypes;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.internal.fuseable.QueueSubscription;

/**
 * Base class extending AtomicReference (resource tracking) and QueueSubscription (fusion).
 *
 * @param <T> the value type
 * @param <R> the reference type
 */
abstract class BasicRefQueueSubscription<T, R> extends AtomicReference<R> implements QueueSubscription<T> {

    private static final long serialVersionUID = -6671519529404341862L;

    @Override
    public final boolean offer(T e) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public final boolean offer(T v1, T v2) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public int requestFusion(int mode) {
        return mode & ASYNC;
    }

    @Override
    public T poll() throws Exception {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public void clear() {
        // no-op
    }

    @Override
    public void request(long n) {
        // ignored
    }
}