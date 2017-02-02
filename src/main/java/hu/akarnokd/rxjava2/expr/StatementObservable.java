/*
 * Copyright 2016-2017 David Karnok
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

import java.util.Map;
import java.util.concurrent.Callable;

import hu.akarnokd.rxjava2.util.AlwaysTrueBooleanSupplier;
import io.reactivex.*;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.internal.functions.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Imperative statements expressed as Observable operators.
 */
public final class StatementObservable {

    /** Factory class. */
    private StatementObservable() { throw new IllegalStateException("No instances!"); }
    /**
     * Return a particular one of several possible Observables based on a case
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
     *            Observer subscribes
     * @param mapOfCases
     *            a map that maps a case key to an Observable
     * @return a particular Observable chosen by key from the map of
     *         Observables, or an empty Observable if no Observable matches the
     *         key
     */
    public static <K, R> Observable<R> switchCase(Callable<? extends K> caseSelector,
            Map<? super K, ? extends ObservableSource<? extends R>> mapOfCases) {
        return switchCase(caseSelector, mapOfCases, Observable.<R> empty());
    }

    /**
     * Return a particular one of several possible Observables based on a case
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
     *            Observer subscribes
     * @param mapOfCases
     *            a map that maps a case key to an Observable
     * @param scheduler
     *            the scheduler where the empty observable is observed
     * @return a particular Observable chosen by key from the map of
     *         Observables, or an empty Observable if no Observable matches the
     *         key, but one that runs on the designated scheduler in either case
     */
    public static <K, R> Observable<R> switchCase(Callable<? extends K> caseSelector,
            Map<? super K, ? extends ObservableSource<? extends R>> mapOfCases, Scheduler scheduler) {
        return switchCase(caseSelector, mapOfCases, Observable.<R> empty().subscribeOn(scheduler));
    }

    /**
     * Return a particular one of several possible Observables based on a case
     * selector, or a default Observable if the case selector does not map to
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
     *            Observer subscribes
     * @param mapOfCases
     *            a map that maps a case key to an Observable
     * @param defaultCase
     *            the default Observable if the {@code mapOfCases} doesn't contain a value for the key returned by the {@code caseSelector}
     * @return a particular Observable chosen by key from the map of
     *         Observables, or the default case if no Observable matches the key
     */
    public static <K, R> Observable<R> switchCase(Callable<? extends K> caseSelector,
            Map<? super K, ? extends ObservableSource<? extends R>> mapOfCases,
                    ObservableSource<? extends R> defaultCase) {
        ObjectHelper.requireNonNull(caseSelector, "caseSelector is null");
        ObjectHelper.requireNonNull(mapOfCases, "mapOfCases is null");
        ObjectHelper.requireNonNull(defaultCase, "defaultCase is null");
        return RxJavaPlugins.onAssembly(new ObservableSwitchCase<R, K>(caseSelector, mapOfCases, defaultCase));
    }

    /**
     * Return an Observable that re-emits the emissions from the source
     * Observable, and then re-subscribes to the source long as a condition is
     * true.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doWhile.png" alt="">
     * 
     * @param <T> the value type
     * @param source the source Observable to work with
     * @param postCondition
     *            the post condition to test after the source
     *            Observable completes
     * @return an Observable that replays the emissions from the source
     *         Observable, and then continues to replay them so long as the post
     *         condition is true
     */
    public static <T> Observable<T> doWhile(ObservableSource<? extends T> source, BooleanSupplier postCondition) {
        ObjectHelper.requireNonNull(source, "source is null");
        ObjectHelper.requireNonNull(postCondition, "postCondition is null");
        return RxJavaPlugins.onAssembly(new ObservableWhileDoWhile<T>(source, AlwaysTrueBooleanSupplier.INSTANCE, postCondition));
    }

    /**
     * Return an Observable that re-emits the emissions from the source
     * Observable as long as the condition is true before the first or subsequent subscribe() calls.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/whileDo.png" alt="">
     * 
     * @param <T> the value type
     * @param source the source Observable to work with
     * @param preCondition
     *            the condition to evaluate before subscribing to or
     *            replaying the source Observable
     * @return an Observable that replays the emissions from the source
     *         Observable so long as <code>preCondition</code> is true
     */
    public static <T> Observable<T> whileDo(ObservableSource<? extends T> source, BooleanSupplier preCondition) {
        ObjectHelper.requireNonNull(source, "source is null");
        ObjectHelper.requireNonNull(preCondition, "preCondition is null");
        return RxJavaPlugins.onAssembly(new ObservableWhileDoWhile<T>(source, preCondition, preCondition));
    }

    /**
     * Return an Observable that emits the emissions from a specified Observable
     * if a condition evaluates to true, otherwise return an empty Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ifThen.png" alt="">
     * 
     * @param <R>
     *            the result value type
     * @param condition
     *            the condition that decides whether to emit the emissions
     *            from the <code>then</code> Observable
     * @param then
     *            the Observable sequence to emit to if {@code condition} is {@code true}
     * @return an Observable that mimics the {@code then} Observable if the {@code condition} function evaluates to true, or an empty
     *         Observable otherwise
     */
    public static <R> Observable<R> ifThen(BooleanSupplier condition, ObservableSource<? extends R> then) {
        return ifThen(condition, then, Observable.<R> empty());
    }

    /**
     * Return an Observable that emits the emissions from a specified Observable
     * if a condition evaluates to true, otherwise return an empty Observable
     * that runs on a specified Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ifThen.s.png" alt="">
     * 
     * @param <R>
     *            the result value type
     * @param condition
     *            the condition that decides whether to emit the emissions
     *            from the <code>then</code> Observable
     * @param then
     *            the Observable sequence to emit to if {@code condition} is {@code true}
     * @param scheduler
     *            the Scheduler on which the empty Observable runs if the
     *            in case the condition returns false
     * @return an Observable that mimics the {@code then} Observable if the {@code condition} function evaluates to true, or an empty
     *         Observable running on the specified Scheduler otherwise
     */
    public static <R> Observable<R> ifThen(BooleanSupplier condition, ObservableSource<? extends R> then, Scheduler scheduler) {
        return ifThen(condition, then, Observable.<R> empty().subscribeOn(scheduler));
    }

    /**
     * Return an Observable that emits the emissions from one specified
     * Observable if a condition evaluates to true, or from another specified
     * Observable otherwise.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ifThen.e.png" alt="">
     * 
     * @param <R>
     *            the result value type
     * @param condition
     *            the condition that decides which Observable to emit the
     *            emissions from
     * @param then
     *            the Observable sequence to emit to if {@code condition} is {@code true}
     * @param orElse
     *            the Observable sequence to emit to if {@code condition} is {@code false}
     * @return an Observable that mimics either the {@code then} or {@code orElse} Observables depending on a condition function
     */
    public static <R> Observable<R> ifThen(BooleanSupplier condition, ObservableSource<? extends R> then,
            Observable<? extends R> orElse) {
        ObjectHelper.requireNonNull(condition, "condition is null");
        ObjectHelper.requireNonNull(then, "then is null");
        ObjectHelper.requireNonNull(orElse, "orElse is null");
        return RxJavaPlugins.onAssembly(new ObservableIfThen<R>(condition, then, orElse));
    }
}