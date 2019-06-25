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

package hu.akarnokd.rxjava3.subjects;

import io.reactivex.annotations.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.subjects.Subject;

/**
 * Utility methods to work with RxJava 2 Subjects.
 * @since 0.18.2
 */
public final class Subjects {
    /** Utility class. */
    private Subjects() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Wraps a Subject and makes sure if all observers dispose
     * their disposables, the upstream's Disposable gets disposed as well.
     * <p>
     * This operator is similar to {@link io.reactivex.observables.ConnectableObservable#refCount()}
     * except the first Observer doesn't trigger any sort of connection; that happens
     * when the resulting Subject is subscribed to an Observable manually.
     * @param <T> the input and output value type
     * @param subject the subject to wrap, not null
     * @return the wrapped and reference-counted Subject
     * @since 2.1.8 - experimental
     */
    @NonNull
    @CheckReturnValue
    public static <T> Subject<T> refCount(Subject<T> subject) {
        if (subject instanceof RefCountSubject) {
            return subject;
        }
        return new RefCountSubject<T>(ObjectHelper.requireNonNull(subject, "subject is null"));
    }
}
