/*
 * Copyright 2016 David Karnok
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

import org.reactivestreams.*;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.subscribers.TestSubscriber;

/**
 * A 0-1-error base reactive type, similar to Maybe, implementing the Reactive-Streams Publisher
 * interface.
 *
 * @param <T> the value type
 * 
 * @since 0.14.0
 */
public abstract class Perhaps<T> implements Publisher<T> {

    /**
     * Hook called when assembling Perhaps sequences.
     */
    @SuppressWarnings("rawtypes")
    private static volatile Function<Perhaps, Perhaps> onAssembly;

    /**
     * Returns the current onAssembly handler.
     * @param <T> the target value type
     * @return the current handler, maybe null
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Function<Perhaps<T>, Perhaps<T>> getOnAssemblyHandler() {
        return (Function)onAssembly;
    }

    /**
     * Set the onAssembly handler.
     * @param <T> the target value type
     * @param handler the handler, null clears the handler
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> void setOnAssemblyHandler(Function<Perhaps<T>, Perhaps<T>> handler) {
        onAssembly = (Function)handler;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected static <T> Perhaps<T> onAssembly(Perhaps<T> source) {
        Function<Perhaps, Perhaps> f = onAssembly;
        if (f == null) {
            return source;
        }
        try {
            return f.apply(source);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    // ----------------------------------------------------
    // Factory methods (enter)
    // ----------------------------------------------------

    @Override
    public final void subscribe(Subscriber<? super T> s) {
        ObjectHelper.requireNonNull(s, "s is null");
        try {
            subscribeActual(s);
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            NullPointerException npe = new NullPointerException();
            npe.initCause(ex);
            throw npe;
        }
    }

    /**
     * Implement this method to react to a Subscriber subscribing to this Perhaps.
     * @param s the downstream Subscriber, never null
     */
    protected abstract void subscribeActual(Subscriber<? super T> s);

    /**
     * Creates a TestSubscriber and subscribes it to this Perhaps.
     * @return the new TestSubscriber instance
     */
    public final TestSubscriber<T> test() {
        return test(Long.MAX_VALUE, false);
    }

    /**
     * Creates a TestSubscriber, optionally cancels it, and subscribes
     * it to this Perhaps.
     * @param cancel if true, the TestSubscriber will be cancelled before
     * subscribing to this Perhaps
     * @return the new TestSubscriber instance
     */
    public final TestSubscriber<T> test(boolean cancel) {
        return test(Long.MAX_VALUE, cancel);
    }

    /**
     * Creates a TestSubscriber with the given initial request and
     * subscribes it to this Perhaps.
     * @param initialRequest the initial request amount, non-negative
     * @return the new TestSubscriber
     */
    public final TestSubscriber<T> test(long initialRequest) {
        return test(initialRequest, false);
    }

    /**
     * Creates a TestSubscriber with the given initial request,
     * optionally cancels it, and subscribes it to this Perhaps.
     * @param initialRequest the initial request amount, non-negative
     * @param cancel if true, the TestSubscriber will be cancelled before
     * subscribing to this Perhaps
     * @return the new TestSubscriber
     */
    public final TestSubscriber<T> test(long initialRequest, boolean cancel) {
        TestSubscriber<T> ts = new TestSubscriber<T>(initialRequest);
        if (cancel) {
            ts.cancel();
        }
        subscribe(ts);
        return ts;
    }

}
