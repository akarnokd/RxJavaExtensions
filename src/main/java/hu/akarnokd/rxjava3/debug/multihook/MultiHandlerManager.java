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

package hu.akarnokd.rxjava3.debug.multihook;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Manages an array of handlers.
 *
 * @param <H> the handler type
 * @since 0.18.0
 */
public class MultiHandlerManager<H> {

    /**
     * The copy-on-write registry of handlers wrapped into a HandlerRegistration container
     * to support disposing/unregistering a particular registration.
     */
    protected final CopyOnWriteArrayList<HandlerRegistration<H>> handlers;

    /**
     * Constructs an empty handler manager.
     */
    public MultiHandlerManager() {
        handlers = new CopyOnWriteArrayList<HandlerRegistration<H>>();
    }

    /**
     * Registers the specified handler instance with this MultiHandlerManager.
     * <p>
     * Handlers don't have to be unique instances, if the same handler is
     * registered multiple times, it will be invoked multiple times as well.
     * <p>
     * This method is threadsafe.
     * @param handler the handler to register
     * @return the Disposable instance to unregister the handler.
     */
    @NonNull
    public final Disposable register(@NonNull H handler) {
        ObjectHelper.requireNonNull(handler, "handler is null");
        HandlerRegistration<H> hr = new HandlerRegistration<H>(this, handler);
        handlers.add(hr);
        return hr;
    }

    final void unregister(HandlerRegistration<H> handler) {
        handlers.remove(handler);
    }

    /**
     * The given consumer is invoked with each registered handler instance.
     * <p>
     * Exceptions raised by the invocation of the consumer for a particular
     * handler are printed to the console and the current thread's
     * uncaught exception handler is notified.
     * <p>
     * This method is threadsafe.
     * @param consumer the consumer to invoke
     */
    public final void forEach(@NonNull Consumer<H> consumer) {
        ObjectHelper.requireNonNull(consumer, "consumer is null");
        Iterator<HandlerRegistration<H>> it = handlers.iterator();
        while (it.hasNext()) {
            try {
                HandlerRegistration<H> hr = it.next();
                H h = hr.get();
                if (h != null) {
                    consumer.accept(h);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                Thread t = Thread.currentThread();
                t.getUncaughtExceptionHandler().uncaughtException(t, ex);
            }
        }
    }

    /**
     * The given consumer is invoked with each registered handler instance.
     * <p>
     * Exceptions raised by the invocation of the consumer for a particular
     * handler are printed to the console and the current thread's
     * uncaught exception handler is notified.
     * <p>
     * This method is threadsafe.
     * @param <S> the type of the extra state provided to the consumer
     * @param state the extra state provided to the consumer
     * @param consumer the consumer to invoke
     */
    public final <S> void forEach(S state, @NonNull BiConsumer<S, H> consumer) {
        ObjectHelper.requireNonNull(consumer, "consumer is null");
        Iterator<HandlerRegistration<H>> it = handlers.iterator();
        while (it.hasNext()) {
            try {
                HandlerRegistration<H> hr = it.next();
                H h = hr.get();
                if (h != null) {
                    consumer.accept(state, h);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                Thread t = Thread.currentThread();
                t.getUncaughtExceptionHandler().uncaughtException(t, ex);
            }
        }
    }

    /**
     * Removes all handlers from this MultiHandlerManager.
     * <p>
     * This method is threadsafe.
     */
    public final void clear() {
        handlers.clear();
    }

    /**
     * Returns true if this manager has any handlers registered.
     * <p>
     * This method is threadsafe.
     * @return true if this manager has any handlers registered.
     */
    public final boolean hasHandlers() {
        return !handlers.isEmpty();
    }

    static final class HandlerRegistration<H> extends AtomicReference<H> implements Disposable {

        private static final long serialVersionUID = -3761960052630027297L;

        final MultiHandlerManager<H> parent;

        HandlerRegistration(MultiHandlerManager<H> parent, H handler) {
            this.parent = parent;
            lazySet(handler);
        }

        @Override
        public void dispose() {
            H h = getAndSet(null);
            if (h != null) {
                parent.unregister(this);
            }
        }

        @Override
        public boolean isDisposed() {
            return get() == null;
        }
    }
}
