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

package hu.akarnokd.rxjava2.expr;

import java.util.Map;
import java.util.concurrent.Callable;

import org.reactivestreams.Publisher;

import hu.akarnokd.rxjava2.util.AlwaysTrueBooleanSupplier;
import io.reactivex.*;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Imperative statements expressed as Flowable operators.
 */
public final class StatementFlowable {

    /** Factory class. */
    private StatementFlowable() { throw new IllegalStateException("No instances!"); }
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
     *            a map that maps a case key to an Flowable
     * @return a particular Flowable chosen by key from the map of
     *         Observables, or an empty Flowable if no Flowable matches the
     *         key
     */
    public static <K, R> Flowable<R> switchCase(Callable<? extends K> caseSelector,
            Map<? super K, ? extends Publisher<? extends R>> mapOfCases) {
        return switchCase(caseSelector, mapOfCases, Flowable.<R> empty());
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
     *            a map that maps a case key to an Flowable
     * @param scheduler
     *            the scheduler where the empty observable is observed
     * @return a particular Flowable chosen by key from the map of
     *         Observables, or an empty Flowable if no Flowable matches the
     *         key, but one that runs on the designated scheduler in either case
     */
    public static <K, R> Flowable<R> switchCase(Callable<? extends K> caseSelector,
            Map<? super K, ? extends Publisher<? extends R>> mapOfCases, Scheduler scheduler) {
        return switchCase(caseSelector, mapOfCases, Flowable.<R> empty().subscribeOn(scheduler));
    }

    /**
     * Return a particular one of several possible Observables based on a case
     * selector, or a default Flowable if the case selector does not map to
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
     *            a map that maps a case key to an Flowable
     * @param defaultCase
     *            the default Flowable if the {@code mapOfCases} doesn't contain a value for the key returned by the {@code caseSelector}
     * @return a particular Flowable chosen by key from the map of
     *         Observables, or the default case if no Flowable matches the key
     */
    public static <K, R> Flowable<R> switchCase(Callable<? extends K> caseSelector,
            Map<? super K, ? extends Publisher<? extends R>> mapOfCases,
                    Publisher<? extends R> defaultCase) {
        ObjectHelper.requireNonNull(caseSelector, "caseSelector is null");
        ObjectHelper.requireNonNull(mapOfCases, "mapOfCases is null");
        ObjectHelper.requireNonNull(defaultCase, "defaultCase is null");
        return RxJavaPlugins.onAssembly(new FlowableSwitchCase<R, K>(caseSelector, mapOfCases, defaultCase));
    }

    /**
     * Return an Flowable that re-emits the emissions from the source
     * Flowable, and then re-subscribes to the source long as a condition is
     * true.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/doWhile.png" alt="">
     * 
     * @param <T> the value type
     * @param source the source Flowable to work with
     * @param postCondition
     *            the post condition to test after the source
     *            Flowable completes
     * @return an Flowable that replays the emissions from the source
     *         Flowable, and then continues to replay them so long as the post
     *         condition is true
     */
    public static <T> Flowable<T> doWhile(Publisher<? extends T> source, BooleanSupplier postCondition) {
        ObjectHelper.requireNonNull(source, "source is null");
        ObjectHelper.requireNonNull(postCondition, "postCondition is null");
        return RxJavaPlugins.onAssembly(new FlowableWhileDoWhile<T>(source, AlwaysTrueBooleanSupplier.INSTANCE, postCondition));
    }

    /**
     * Return an Flowable that re-emits the emissions from the source
     * Flowable as long as the condition is true before the first or subsequent subscribe() calls.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/whileDo.png" alt="">
     * 
     * @param <T> the value type
     * @param source the source Flowable to work with
     * @param preCondition
     *            the condition to evaluate before subscribing to or
     *            replaying the source Flowable
     * @return an Flowable that replays the emissions from the source
     *         Flowable so long as <code>preCondition</code> is true
     */
    public static <T> Flowable<T> whileDo(Publisher<? extends T> source, BooleanSupplier preCondition) {
        ObjectHelper.requireNonNull(source, "source is null");
        ObjectHelper.requireNonNull(preCondition, "preCondition is null");
        return RxJavaPlugins.onAssembly(new FlowableWhileDoWhile<T>(source, preCondition, preCondition));
    }

    /**
     * Return an Flowable that emits the emissions from a specified Flowable
     * if a condition evaluates to true, otherwise return an empty Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ifThen.png" alt="">
     * 
     * @param <R>
     *            the result value type
     * @param condition
     *            the condition that decides whether to emit the emissions
     *            from the <code>then</code> Flowable
     * @param then
     *            the Flowable sequence to emit to if {@code condition} is {@code true}
     * @return an Flowable that mimics the {@code then} Flowable if the {@code condition} function evaluates to true, or an empty
     *         Flowable otherwise
     */
    public static <R> Flowable<R> ifThen(BooleanSupplier condition, Publisher<? extends R> then) {
        return ifThen(condition, then, Flowable.<R> empty());
    }

    /**
     * Return an Flowable that emits the emissions from a specified Flowable
     * if a condition evaluates to true, otherwise return an empty Flowable
     * that runs on a specified Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ifThen.s.png" alt="">
     * 
     * @param <R>
     *            the result value type
     * @param condition
     *            the condition that decides whether to emit the emissions
     *            from the <code>then</code> Flowable
     * @param then
     *            the Flowable sequence to emit to if {@code condition} is {@code true}
     * @param scheduler
     *            the Scheduler on which the empty Flowable runs if the
     *            in case the condition returns false
     * @return an Flowable that mimics the {@code then} Flowable if the {@code condition} function evaluates to true, or an empty
     *         Flowable running on the specified Scheduler otherwise
     */
    public static <R> Flowable<R> ifThen(BooleanSupplier condition, Publisher<? extends R> then, Scheduler scheduler) {
        return ifThen(condition, then, Flowable.<R> empty().subscribeOn(scheduler));
    }

    /**
     * Return an Flowable that emits the emissions from one specified
     * Flowable if a condition evaluates to true, or from another specified
     * Flowable otherwise.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/ifThen.e.png" alt="">
     * 
     * @param <R>
     *            the result value type
     * @param condition
     *            the condition that decides which Flowable to emit the
     *            emissions from
     * @param then
     *            the Flowable sequence to emit to if {@code condition} is {@code true}
     * @param orElse
     *            the Flowable sequence to emit to if {@code condition} is {@code false}
     * @return an Flowable that mimics either the {@code then} or {@code orElse} Observables depending on a condition function
     */
    public static <R> Flowable<R> ifThen(BooleanSupplier condition, Publisher<? extends R> then,
            Flowable<? extends R> orElse) {
        ObjectHelper.requireNonNull(condition, "condition is null");
        ObjectHelper.requireNonNull(then, "then is null");
        ObjectHelper.requireNonNull(orElse, "orElse is null");
        return RxJavaPlugins.onAssembly(new FlowableIfThen<R>(condition, then, orElse));
    }
}