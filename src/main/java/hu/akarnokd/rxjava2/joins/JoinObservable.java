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

package hu.akarnokd.rxjava2.joins;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Represents an observable that supports join operations.
 *
 * @param <T> the value type joined
 */
public final class JoinObservable<T> {

    private final Observable<T> o;

    private JoinObservable(Observable<T> o) {
        this.o = o;
    }

    /**
     * Creates a JoinObservable from a regular Observable.
     * @param <T> the value type
     * @param o the observable to wrap
     * @return the created JoinObservable instance
     */
    public static <T> JoinObservable<T> from(Observable<T> o) {
        return new JoinObservable<T>(RxJavaPlugins.onAssembly(o));
    }

    /**
     * Returns a Pattern that matches when both Observables emit an item.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/and_then_when.png" alt="">
     *
     * @param <T2> the value type of the right Observable
     * @param right
     *            an Observable to match with the source Observable
     * @return a Pattern object that matches when both Observables emit an item
     * @throws NullPointerException
     *             if {@code right} is null
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: and()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229153.aspx">MSDN: Observable.And</a>
     */
    public <T2> Pattern2<T, T2> and(Observable<T2> right) {
        return JoinPatterns.and(o, right);
    }

    /**
     * Joins together the results from several patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/and_then_when.png" alt="">
     *
     * @param <R> the result type
     * @param plans
     *            a series of plans created by use of the {@link #then} Observer on patterns
     * @return an Observable that emits the results from matching several patterns
     * @throws NullPointerException
     *             if {@code plans} is null
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229558.aspx">MSDN: Observable.When</a>
     */
    public static <R> JoinObservable<R> when(Iterable<? extends Plan<R>> plans) {
        if (plans == null) {
            throw new NullPointerException("plans");
        }
        return from(JoinPatterns.when(plans));
    }

    /**
     * Joins together the results from several patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/and_then_when.png" alt="">
     *
     * @param <R> the result type
     * @param plans
     *            a series of plans created by use of the {@link #then} Observer on patterns
     * @return an Observable that emits the results from matching several patterns
     * @throws NullPointerException
     *             if {@code plans} is null
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    public static <R> JoinObservable<R> when(Plan<R>... plans) {
        return from(JoinPatterns.when(plans));
    }

    /**
     * Joins the results from a pattern via its plan.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/and_then_when.png" alt="">
     *
     * @param <R> the result type
     * @param p1
     *            the plan to join, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching a pattern
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public static <R> JoinObservable<R> when(Plan<R> p1) {
        return from(JoinPatterns.when(p1));
    }

    /**
     * Joins together the results from two patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/and_then_when.png" alt="">
     *
     * @param <R> the result type
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching two patterns
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public static <R> JoinObservable<R> when(Plan<R> p1, Plan<R> p2) {
        return from(JoinPatterns.when(p1, p2));
    }

    /**
     * Joins together the results from three patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/and_then_when.png" alt="">
     *
     * @param <R> the result type
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p3
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching three patterns
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public static <R> JoinObservable<R> when(Plan<R> p1, Plan<R> p2, Plan<R> p3) {
        return from(JoinPatterns.when(p1, p2, p3));
    }

    /**
     * Joins together the results from four patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/and_then_when.png" alt="">
     *
     * @param <R> the result type
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p3
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p4
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching four patterns
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public static <R> JoinObservable<R> when(Plan<R> p1, Plan<R> p2, Plan<R> p3, Plan<R> p4) {
        return from(JoinPatterns.when(p1, p2, p3, p4));
    }

    /**
     * Joins together the results from five patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/and_then_when.png" alt="">
     *
     * @param <R> the result type
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p3
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p4
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p5
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching five patterns
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public static <R> JoinObservable<R> when(Plan<R> p1, Plan<R> p2, Plan<R> p3, Plan<R> p4, Plan<R> p5) {
        return from(JoinPatterns.when(p1, p2, p3, p4, p5));
    }

    /**
     * Joins together the results from six patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/and_then_when.png" alt="">
     *
     * @param <R> the result type
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p3
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p4
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p5
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p6
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching six patterns
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public static <R> JoinObservable<R> when(Plan<R> p1, Plan<R> p2, Plan<R> p3, Plan<R> p4, Plan<R> p5, Plan<R> p6) {
        return from(JoinPatterns.when(p1, p2, p3, p4, p5, p6));
    }

    /**
     * Joins together the results from seven patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/and_then_when.png" alt="">
     *
     * @param <R> the result type
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p3
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p4
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p5
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p6
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p7
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching seven patterns
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public static <R> JoinObservable<R> when(Plan<R> p1, Plan<R> p2, Plan<R> p3, Plan<R> p4, Plan<R> p5, Plan<R> p6, Plan<R> p7) {
        return from(JoinPatterns.when(p1, p2, p3, p4, p5, p6, p7));
    }

    /**
     * Joins together the results from eight patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/and_then_when.png" alt="">
     *
     * @param <R> the result type
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p3
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p4
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p5
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p6
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p7
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p8
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching eight patterns
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public static <R> JoinObservable<R> when(Plan<R> p1, Plan<R> p2, Plan<R> p3, Plan<R> p4, Plan<R> p5, Plan<R> p6, Plan<R> p7, Plan<R> p8) {
        return from(JoinPatterns.when(p1, p2, p3, p4, p5, p6, p7, p8));
    }

    /**
     * Joins together the results from nine patterns via their plans.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/and_then_when.png" alt="">
     *
     * @param <R> the result type
     * @param p1
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p2
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p3
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p4
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p5
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p6
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p7
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p8
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @param p9
     *            a plan, created by use of the {@link #then} Observer on a pattern
     * @return an Observable that emits the results from matching nine patterns
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: when()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229889.aspx">MSDN: Observable.When</a>
     */
    @SuppressWarnings("unchecked")
    public static <R> JoinObservable<R> when(Plan<R> p1, Plan<R> p2, Plan<R> p3, Plan<R> p4, Plan<R> p5, Plan<R> p6, Plan<R> p7, Plan<R> p8, Plan<R> p9) {
        return from(JoinPatterns.when(p1, p2, p3, p4, p5, p6, p7, p8, p9));
    }

    /**
     * Matches when the Observable has an available item and projects the item by invoking the selector
     * function.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/and_then_when.png" alt="">
     *
     * @param <R> the result type
     * @param selector
     *            selector that will be invoked for items emitted by the source Observable
     * @return a {@link Plan} that produces the projected results, to be fed (with other Plans) to the {@link #when} Observer
     * @throws NullPointerException
     *             if {@code selector} is null
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Combining-Observables#wiki-and-then-and-when">RxJava Wiki: then()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211662.aspx">MSDN: Observable.Then</a>
     */
    public <R> Plan<R> then(Function<? super T, ? extends R> selector) {
        return JoinPatterns.then(o, selector);
    }

    public Observable<T> toObservable() {
        return o;
    }
}