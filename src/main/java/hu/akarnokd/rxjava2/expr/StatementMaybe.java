/*
 * Copyright 2016-2018 David Karnok
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

package hu.akarnokd.rxjava2.expr;

import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Scheduler;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Imperative statements expressed as Maybe operators.
 */
public final class StatementMaybe {

    /** Factory class. */
    private StatementMaybe() { throw new IllegalStateException("No instances!"); }
    /**
     * Return a particular one of several possible Maybes based on a case
     * selector.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchCase.png" alt="">
     *
     * @param <K>
     *            the case key type
     * @param <R>
     *            the result value type
     * @param caseSelector
     *            the function that produces a case key when an
     *            MaybeObserver subscribes
     * @param mapOfCases
     *            a map that maps a case key to a Maybe
     * @return a particular Maybe chosen by key from the map of
     *         Maybes, or an empty Maybe if no Maybe matches the
     *         key
     */
    public static <K, R> Maybe<R> switchCase(Callable<? extends K> caseSelector,
            Map<? super K, ? extends MaybeSource<? extends R>> mapOfCases) {
        return switchCase(caseSelector, mapOfCases, Maybe.<R> empty());
    }

    /**
     * Return a particular one of several possible Maybes based on a case
     * selector and run it on the designated scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/switchCase.s.png" alt="">
     *
     * @param <K>
     *            the case key type
     * @param <R>
     *            the result value type
     * @param caseSelector
     *            the function that produces a case key when an
     *            MaybeObserver subscribes
     * @param mapOfCases
     *            a map that maps a case key to a Maybe
     * @param scheduler
     *            the scheduler where the empty maybe is observed
     * @return a particular Maybe chosen by key from the map of
     *         Maybes, or an empty Maybe if no Maybe matches the
     *         key, but one that runs on the designated scheduler in either case
     */
    public static <K, R> Maybe<R> switchCase(Callable<? extends K> caseSelector,
            Map<? super K, ? extends MaybeSource<? extends R>> mapOfCases, Scheduler scheduler) {
        return switchCase(caseSelector, mapOfCases, Maybe.<R> empty().subscribeOn(scheduler));
    }

    /**
     * Return a particular one of several possible Maybes based on a case
     * selector, or a default Maybe if the case selector does not map to
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
     *            MaybeObserver subscribes
     * @param mapOfCases
     *            a map that maps a case key to a Maybe
     * @param defaultCase
     *            the default Maybe if the {@code mapOfCases} doesn't contain a value for the key returned by the {@code caseSelector}
     * @return a particular Maybe chosen by key from the map of
     *         Maybes, or the default case if no Maybe matches the key
     */
    public static <K, R> Maybe<R> switchCase(Callable<? extends K> caseSelector,
            Map<? super K, ? extends MaybeSource<? extends R>> mapOfCases,
                    MaybeSource<? extends R> defaultCase) {
        ObjectHelper.requireNonNull(caseSelector, "caseSelector is null");
        ObjectHelper.requireNonNull(mapOfCases, "mapOfCases is null");
        ObjectHelper.requireNonNull(defaultCase, "defaultCase is null");
        return RxJavaPlugins.onAssembly(new MaybeSwitchCase<R, K>(caseSelector, mapOfCases, defaultCase));
    }

    /**
     * Return a Maybe that emits the emissions from a specified Maybe
     * if a condition evaluates to true, otherwise return an empty Maybe.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ifThen.png" alt="">
     *
     * @param <R>
     *            the result value type
     * @param condition
     *            the condition that decides whether to emit the emissions
     *            from the <code>then</code> Maybe
     * @param then
     *            the Maybe sequence to emit to if {@code condition} is {@code true}
     * @return a Maybe that mimics the {@code then} Maybe if the {@code condition} function evaluates to true, or an empty
     *         Maybe otherwise
     */
    public static <R> Maybe<R> ifThen(BooleanSupplier condition, MaybeSource<? extends R> then) {
        return ifThen(condition, then, Maybe.<R> empty());
    }

    /**
     * Return a Maybe that emits the emissions from a specified Maybe
     * if a condition evaluates to true, otherwise return an empty Maybe
     * that runs on a specified Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ifThen.s.png" alt="">
     *
     * @param <R>
     *            the result value type
     * @param condition
     *            the condition that decides whether to emit the emissions
     *            from the <code>then</code> Maybe
     * @param then
     *            the Maybe sequence to emit to if {@code condition} is {@code true}
     * @param scheduler
     *            the Scheduler on which the empty Maybe runs if the
     *            in case the condition returns false
     * @return a Maybe that mimics the {@code then} Maybe if the {@code condition} function evaluates to true, or an empty
     *         Maybe running on the specified Scheduler otherwise
     */
    public static <R> Maybe<R> ifThen(BooleanSupplier condition, MaybeSource<? extends R> then, Scheduler scheduler) {
        return ifThen(condition, then, Maybe.<R> empty().subscribeOn(scheduler));
    }

    /**
     * Return a Maybe that emits the emissions from one specified
     * Maybe if a condition evaluates to true, or from another specified
     * Maybe otherwise.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ifThen.e.png" alt="">
     *
     * @param <R>
     *            the result value type
     * @param condition
     *            the condition that decides which Maybe to emit the
     *            emissions from
     * @param then
     *            the Maybe sequence to emit to if {@code condition} is {@code true}
     * @param orElse
     *            the Maybe sequence to emit to if {@code condition} is {@code false}
     * @return a Maybe that mimics either the {@code then} or {@code orElse} Maybes depending on a condition function
     */
    public static <R> Maybe<R> ifThen(BooleanSupplier condition, MaybeSource<? extends R> then,
            Maybe<? extends R> orElse) {
        ObjectHelper.requireNonNull(condition, "condition is null");
        ObjectHelper.requireNonNull(then, "then is null");
        ObjectHelper.requireNonNull(orElse, "orElse is null");
        return RxJavaPlugins.onAssembly(new MaybeIfThen<R>(condition, then, orElse));
    }
}