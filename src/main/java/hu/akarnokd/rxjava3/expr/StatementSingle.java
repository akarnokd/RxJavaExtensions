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

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.ObjectHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Imperative statements expressed as Single operators.
 */
public final class StatementSingle {

    /** Factory class. */
    private StatementSingle() { throw new IllegalStateException("No instances!"); }

    /**
     * Return a particular one of several possible Singles based on a case
     * selector, or a default Single if the case selector does not map to
     * a particular one.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchCase.png" alt="">
     *
     * @param <K>
     *            the case key type
     * @param <R>
     *            the result value type
     * @param caseSelector
     *            the function that produces a case key when an
     *            SingleObserver subscribes
     * @param mapOfCases
     *            a map that maps a case key to a Single
     * @param defaultCase
     *            the default Single if the {@code mapOfCases} doesn't contain a value for the key returned by the {@code caseSelector}
     * @return a particular Single chosen by key from the map of
     *         Singles, or the default case if no Single matches the key
     */
    public static <K, R> Single<R> switchCase(Supplier<? extends K> caseSelector,
            Map<? super K, ? extends SingleSource<? extends R>> mapOfCases,
                    SingleSource<? extends R> defaultCase) {
        ObjectHelper.requireNonNull(caseSelector, "caseSelector is null");
        ObjectHelper.requireNonNull(mapOfCases, "mapOfCases is null");
        ObjectHelper.requireNonNull(defaultCase, "defaultCase is null");
        return RxJavaPlugins.onAssembly(new SingleSwitchCase<R, K>(caseSelector, mapOfCases, defaultCase));
    }

    /**
     * Return a Single that emits the emissions from one specified
     * Single if a condition evaluates to true, or from another specified
     * Single otherwise.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ifThen.e.png" alt="">
     *
     * @param <R>
     *            the result value type
     * @param condition
     *            the condition that decides which Single to emit the
     *            emissions from
     * @param then
     *            the Single sequence to emit to if {@code condition} is {@code true}
     * @param orElse
     *            the Single sequence to emit to if {@code condition} is {@code false}
     * @return a Single that mimics either the {@code then} or {@code orElse} Singles depending on a condition function
     */
    public static <R> Single<R> ifThen(BooleanSupplier condition, SingleSource<? extends R> then,
            Single<? extends R> orElse) {
        ObjectHelper.requireNonNull(condition, "condition is null");
        ObjectHelper.requireNonNull(then, "then is null");
        ObjectHelper.requireNonNull(orElse, "orElse is null");
        return RxJavaPlugins.onAssembly(new SingleIfThen<R>(condition, then, orElse));
    }
}