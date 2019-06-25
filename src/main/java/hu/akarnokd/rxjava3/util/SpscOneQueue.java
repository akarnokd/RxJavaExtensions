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

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.SimplePlainQueue;

/**
 * A single-producer single-consumer queue with exactly one slot of value.
 *
 * @param <T> the value type
 */
public final class SpscOneQueue<T> extends AtomicReference<T>
implements  SimplePlainQueue<T> {

    private static final long serialVersionUID = -8766520133280966316L;

    @Override
    public boolean offer(T value) {
        ObjectHelper.requireNonNull(value, "value is null");
        if (get() == null) {
            lazySet(value);
            return true;
        }
        return false;
    }

    @Override
    public boolean offer(T v1, T v2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return get() == null;
    }

    @Override
    public void clear() {
        lazySet(null);
    }

    @Override
    public T poll() {
        T v = get();
        if (v != null) {
            lazySet(null);
        }
        return v;
    }
}
