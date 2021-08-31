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

package hu.akarnokd.rxjava3.debug;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.operators.ScalarSupplier;
import io.reactivex.rxjava3.observables.ConnectableObservable;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Utility class to enable and disable tracking of operator application ({@code source.map().filter()})
 * by capturing the current stacktrace (warning: very expensive!), have it in a debug-time accessible
 * field (when walking the references in a debugger) and append it to exceptions passing by the
 * regular {@code onError}.
 */
public final class RxJavaAssemblyTracking {

    /** Simply lock out concurrent state changes. */
    static final AtomicBoolean lock = new AtomicBoolean();

    /** Utility class. */
    private RxJavaAssemblyTracking() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Enable the assembly tracking.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void enable() {
        if (lock.compareAndSet(false, true)) {

            RxJavaPlugins.setOnFlowableAssembly(new Function<Flowable, Flowable>() {
                @Override
                public Flowable apply(Flowable f) throws Exception {
                    if (f instanceof Supplier) {
                        if (f instanceof ScalarSupplier) {
                            return new FlowableOnAssemblyScalarSupplier(f);
                        }
                        return new FlowableOnAssemblySupplier(f);
                    }
                    return new FlowableOnAssembly(f);
                }
            });

            RxJavaPlugins.setOnConnectableFlowableAssembly(new Function<ConnectableFlowable, ConnectableFlowable>() {
                @Override
                public ConnectableFlowable apply(ConnectableFlowable f) throws Exception {
                    return new FlowableOnAssemblyConnectable(f);
                }
            });

            RxJavaPlugins.setOnObservableAssembly(new Function<Observable, Observable>() {
                @Override
                public Observable apply(Observable f) throws Exception {
                    if (f instanceof Supplier) {
                        if (f instanceof ScalarSupplier) {
                            return new ObservableOnAssemblyScalarSupplier(f);
                        }
                        return new ObservableOnAssemblySupplier(f);
                    }
                    return new ObservableOnAssembly(f);
                }
            });

            RxJavaPlugins.setOnConnectableObservableAssembly(new Function<ConnectableObservable, ConnectableObservable>() {
                @Override
                public ConnectableObservable apply(ConnectableObservable f) throws Exception {
                    return new ObservableOnAssemblyConnectable(f);
                }
            });

            RxJavaPlugins.setOnSingleAssembly(new Function<Single, Single>() {
                @Override
                public Single apply(Single f) throws Exception {
                    if (f instanceof Supplier) {
                        if (f instanceof ScalarSupplier) {
                            return new SingleOnAssemblyScalarSupplier(f);
                        }
                        return new SingleOnAssemblySupplier(f);
                    }
                    return new SingleOnAssembly(f);
                }
            });

            RxJavaPlugins.setOnCompletableAssembly(new Function<Completable, Completable>() {
                @Override
                public Completable apply(Completable f) throws Exception {
                    if (f instanceof Supplier) {
                        if (f instanceof ScalarSupplier) {
                            return new CompletableOnAssemblyScalarSupplier(f);
                        }
                        return new CompletableOnAssemblySupplier(f);
                    }
                    return new CompletableOnAssembly(f);
                }
            });

            RxJavaPlugins.setOnMaybeAssembly(new Function<Maybe, Maybe>() {
                @Override
                public Maybe apply(Maybe f) throws Exception {
                    if (f instanceof Supplier) {
                        if (f instanceof ScalarSupplier) {
                            return new MaybeOnAssemblyScalarSupplier(f);
                        }
                        return new MaybeOnAssemblySupplier(f);
                    }
                    return new MaybeOnAssembly(f);
                }
            });

            RxJavaPlugins.setOnParallelAssembly(new Function<ParallelFlowable, ParallelFlowable>() {
                @Override
                public ParallelFlowable apply(ParallelFlowable t) throws Exception {
                    return new ParallelFlowableOnAssembly(t);
                }
            });

            lock.set(false);
        }
    }

    /**
     * Disable the assembly tracking.
     */
    public static void disable() {
        if (lock.compareAndSet(false, true)) {

            RxJavaPlugins.setOnCompletableAssembly(null);
            RxJavaPlugins.setOnSingleAssembly(null);
            RxJavaPlugins.setOnMaybeAssembly(null);

            RxJavaPlugins.setOnObservableAssembly(null);
            RxJavaPlugins.setOnFlowableAssembly(null);
            RxJavaPlugins.setOnConnectableObservableAssembly(null);
            RxJavaPlugins.setOnConnectableFlowableAssembly(null);

            RxJavaPlugins.setOnParallelAssembly(null);

            lock.set(false);
        }
    }
}
