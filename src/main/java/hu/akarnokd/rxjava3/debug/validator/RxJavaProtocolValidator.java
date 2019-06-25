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

package hu.akarnokd.rxjava3.debug.validator;

import hu.akarnokd.rxjava3.debug.SavedHooks;
import hu.akarnokd.rxjava3.functions.PlainConsumer;
import io.reactivex.*;
import io.reactivex.annotations.Nullable;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Installs assembly hooks that validate the Reactive-Streams and derived
 * protocols are honored.
 *
 * @since 0.17.4
 * @see #setOnViolationHandler(PlainConsumer)
 * @see #enable()
 */
public final class RxJavaProtocolValidator {

    /** Utility class. */
    private RxJavaProtocolValidator() {
        throw new IllegalStateException("No instances!");
    }

    static volatile boolean enabled;

    static volatile PlainConsumer<ProtocolNonConformanceException> onViolation;

    static final PlainConsumer<ProtocolNonConformanceException> DEFAULT = new PlainConsumer<ProtocolNonConformanceException>() {
        @Override
        public void accept(ProtocolNonConformanceException e) {
            RxJavaPlugins.onError(e);
        }
    };

    /**
     * Enable the protocol violation hooks.
     * @see #enableAndChain()
     * @see #disable()
     */
    public static void enable() {
        enable(false);
    }

    /**
     * Enable the protocol violation hooks by chaining it
     * before any existing hook.
     * @return the SavedHooks instance that allows restoring the previous assembly
     * hook handlers overridden by this method
     * @see #enable()
     */
    public static SavedHooks enableAndChain() {
        return enable(true);
    }

    @SuppressWarnings("rawtypes")
    static SavedHooks enable(boolean chain) {
        PlainConsumer<ProtocolNonConformanceException> h = onViolation;
        if (h == null) {
            h = DEFAULT;
        }
        final PlainConsumer<ProtocolNonConformanceException> handler = h;

        // ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

        final Function<? super Completable, ? extends Completable> saveC = RxJavaPlugins.getOnCompletableAssembly();
        Function<? super Completable, ? extends Completable> oldCompletable = saveC;
        if (oldCompletable == null || !chain) {
            oldCompletable = Functions.identity();
        }
        final Function<? super Completable, ? extends Completable> oldC = oldCompletable;

        RxJavaPlugins.setOnCompletableAssembly(new Function<Completable, Completable>() {
            @Override
            public Completable apply(Completable c) throws Throwable {
                return oldC.apply(new CompletableValidator(c, handler));
            }
        });

        // ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

        final Function<? super Maybe, ? extends Maybe> saveM = RxJavaPlugins.getOnMaybeAssembly();
        Function<? super Maybe, ? extends Maybe> oldMaybe = saveM;
        if (oldMaybe == null || !chain) {
            oldMaybe = Functions.identity();
        }
        final Function<? super Maybe, ? extends Maybe> oldM = oldMaybe;

        RxJavaPlugins.setOnMaybeAssembly(new Function<Maybe, Maybe>() {
            @SuppressWarnings("unchecked")
            @Override
            public Maybe apply(Maybe c) throws Throwable {
                return oldM.apply(new MaybeValidator(c, handler));
            }
        });

        // ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

        final Function<? super Single, ? extends Single> saveS = RxJavaPlugins.getOnSingleAssembly();
        Function<? super Single, ? extends Single> oldSingle = saveS;
        if (oldSingle == null || !chain) {
            oldSingle = Functions.identity();
        }
        final Function<? super Single, ? extends Single> oldS = oldSingle;

        RxJavaPlugins.setOnSingleAssembly(new Function<Single, Single>() {
            @SuppressWarnings("unchecked")
            @Override
            public Single apply(Single c) throws Throwable {
                return oldS.apply(new SingleValidator(c, handler));
            }
        });

        // ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

        final Function<? super Observable, ? extends Observable> saveO = RxJavaPlugins.getOnObservableAssembly();
        Function<? super Observable, ? extends Observable> oldObservable = saveO;
        if (oldObservable == null || !chain) {
            oldObservable = Functions.identity();
        }
        final Function<? super Observable, ? extends Observable> oldO = oldObservable;

        RxJavaPlugins.setOnObservableAssembly(new Function<Observable, Observable>() {
            @SuppressWarnings("unchecked")
            @Override
            public Observable apply(Observable c) throws Throwable {
                return oldO.apply(new ObservableValidator(c, handler));
            }
        });

        // ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

        final Function<? super Flowable, ? extends Flowable> saveF = RxJavaPlugins.getOnFlowableAssembly();
        Function<? super Flowable, ? extends Flowable> oldFlowable = saveF;
        if (oldFlowable == null || !chain) {
            oldFlowable = Functions.identity();
        }
        final Function<? super Flowable, ? extends Flowable> oldF = oldFlowable;

        RxJavaPlugins.setOnFlowableAssembly(new Function<Flowable, Flowable>() {
            @SuppressWarnings("unchecked")
            @Override
            public Flowable apply(Flowable c) throws Throwable {
                return oldF.apply(new FlowableValidator(c, handler));
            }
        });

        // ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

        final Function<? super ConnectableFlowable, ? extends ConnectableFlowable> saveCF = RxJavaPlugins.getOnConnectableFlowableAssembly();
        Function<? super ConnectableFlowable, ? extends ConnectableFlowable> oldConnFlow = saveCF;
        if (oldConnFlow == null || !chain) {
            oldConnFlow = Functions.identity();
        }
        final Function<? super ConnectableFlowable, ? extends ConnectableFlowable> oldCF = oldConnFlow;

        RxJavaPlugins.setOnConnectableFlowableAssembly(new Function<ConnectableFlowable, ConnectableFlowable>() {
            @SuppressWarnings("unchecked")
            @Override
            public ConnectableFlowable apply(ConnectableFlowable c) throws Throwable {
                return oldCF.apply(new ConnectableFlowableValidator(c, handler));
            }
        });

        // ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

        final Function<? super ConnectableObservable, ? extends ConnectableObservable> saveCO = RxJavaPlugins.getOnConnectableObservableAssembly();
        Function<? super ConnectableObservable, ? extends ConnectableObservable> oldConnObs = saveCO;
        if (oldConnObs == null || !chain) {
            oldConnObs = Functions.identity();
        }
        final Function<? super ConnectableObservable, ? extends ConnectableObservable> oldCO = oldConnObs;

        RxJavaPlugins.setOnConnectableObservableAssembly(new Function<ConnectableObservable, ConnectableObservable>() {
            @SuppressWarnings("unchecked")
            @Override
            public ConnectableObservable apply(ConnectableObservable c) throws Throwable {
                return oldCO.apply(new ConnectableObservableValidator(c, handler));
            }
        });

        // ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo

        final Function<? super ParallelFlowable, ? extends ParallelFlowable> savePF = RxJavaPlugins.getOnParallelAssembly();
        Function<? super ParallelFlowable, ? extends ParallelFlowable> oldParFlow = savePF;
        if (oldParFlow == null || !chain) {
            oldParFlow = Functions.identity();
        }
        final Function<? super ParallelFlowable, ? extends ParallelFlowable> oldPF = oldParFlow;

        RxJavaPlugins.setOnParallelAssembly(new Function<ParallelFlowable, ParallelFlowable>() {
            @SuppressWarnings("unchecked")
            @Override
            public ParallelFlowable apply(ParallelFlowable c) throws Throwable {
                return oldPF.apply(new ParallelFlowableValidator(c, handler));
            }
        });

        enabled = true;

        return new SavedHooks() {
            @Override
            public void restore() {
                RxJavaPlugins.setOnCompletableAssembly(saveC);
                RxJavaPlugins.setOnSingleAssembly(saveS);
                RxJavaPlugins.setOnMaybeAssembly(saveM);
                RxJavaPlugins.setOnObservableAssembly(saveO);
                RxJavaPlugins.setOnFlowableAssembly(saveF);

                RxJavaPlugins.setOnConnectableObservableAssembly(saveCO);
                RxJavaPlugins.setOnConnectableFlowableAssembly(saveCF);

                RxJavaPlugins.setOnParallelAssembly(savePF);
            }
        };
    }

    /**
     * Disables the validation hooks be resetting the assembly hooks
     * to none.
     */
    public static void disable() {
        RxJavaPlugins.setOnCompletableAssembly(null);
        RxJavaPlugins.setOnSingleAssembly(null);
        RxJavaPlugins.setOnMaybeAssembly(null);
        RxJavaPlugins.setOnObservableAssembly(null);
        RxJavaPlugins.setOnFlowableAssembly(null);

        RxJavaPlugins.setOnConnectableObservableAssembly(null);
        RxJavaPlugins.setOnConnectableFlowableAssembly(null);

        RxJavaPlugins.setOnParallelAssembly(null);
        enabled = false;
    }

    /**
     * Returns true if the validation hooks have been installed.
     * @return true if the validation hooks have been installed
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Set a custom violation callback handler.
     * <p>
     * Call this method before enabling the validation hooks via {@link #enable()}.
     * <p>
     * Calling with {@code null} restores the default handler which forwards the exception
     * to the global {@link RxJavaPlugins#onError(Throwable)} handler.
     * @param handler the handler that will receive the {@link ProtocolNonConformanceException}
     * violation exceptions
     */
    public static void setOnViolationHandler(@Nullable PlainConsumer<ProtocolNonConformanceException> handler) {
        onViolation = handler;
    }

    /**
     * Returns the current custom violation callback handler.
     * @return the current custom violation callback handler or null if not set
     */
    @Nullable
    public static PlainConsumer<ProtocolNonConformanceException> getOnViolationHandler() {
        return onViolation;
    }
}
