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

package hu.akarnokd.rxjava3.expr;

import java.util.Map;

import io.reactivex.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Imperative statements expressed as Completable operators.
 */
public final class StatementCompletable {

    /** Factory class. */
    private StatementCompletable() { throw new IllegalStateException("No instances!"); }

    /**
     * Return a particular one of several possible Completables based on a case
     * selector, or a default Completable if the case selector does not map to
     * a particular one.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchCase.png" alt="">
     *
     * @param <K>
     *            the case key type
     * @param caseSelector
     *            the function that produces a case key when an
     *            CompletableObserver subscribes
     * @param mapOfCases
     *            a map that maps a case key to a Completable
     * @param defaultCase
     *            the default Completable if the {@code mapOfCases} doesn't contain a value for the key returned by the {@code caseSelector}
     * @return a particular Completable chosen by key from the map of
     *         Completables, or the default case if no Completable matches the key
     */
    public static <K> Completable switchCase(Supplier<? extends K> caseSelector,
            Map<? super K, ? extends CompletableSource> mapOfCases,
                    CompletableSource defaultCase) {
        ObjectHelper.requireNonNull(caseSelector, "caseSelector is null");
        ObjectHelper.requireNonNull(mapOfCases, "mapOfCases is null");
        ObjectHelper.requireNonNull(defaultCase, "defaultCase is null");
        return RxJavaPlugins.onAssembly(new CompletableSwitchCase<K>(caseSelector, mapOfCases, defaultCase));
    }

    /**
     * Return a Completable that emits the emissions from one specified
     * Completable if a condition evaluates to true, or from another specified
     * Completable otherwise.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ifThen.e.png" alt="">
     *
     * @param condition
     *            the condition that decides which Completable to emit the
     *            emissions from
     * @param then
     *            the Completable sequence to emit to if {@code condition} is {@code true}
     * @param orElse
     *            the Completable sequence to emit to if {@code condition} is {@code false}
     * @return a Completable that mimics either the {@code then} or {@code orElse} Completables depending on a condition function
     */
    public static Completable ifThen(BooleanSupplier condition, CompletableSource then,
            Completable orElse) {
        ObjectHelper.requireNonNull(condition, "condition is null");
        ObjectHelper.requireNonNull(then, "then is null");
        ObjectHelper.requireNonNull(orElse, "orElse is null");
        return RxJavaPlugins.onAssembly(new CompletableIfThen(condition, then, orElse));
    }
}