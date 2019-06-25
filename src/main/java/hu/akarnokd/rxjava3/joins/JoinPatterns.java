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

package hu.akarnokd.rxjava3.joins;

import java.util.*;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.*;
import io.reactivex.functions.*;
import io.reactivex.observers.SafeObserver;

/**
 * Join patterns  And, Then and When.
 */
final class JoinPatterns {

    private JoinPatterns() { throw new IllegalStateException("No instances!"); }
    /**
     * Creates a pattern that matches when both observable sequences have an available element.
     * @param left the left source
     * @param right the right source
     * @param <T1> the left value type
     * @param <T2> the right value type
     * @return the pattern with two sources 'and'-ed
     */
    public static <T1, T2> Pattern2<T1, T2> and(/* this */Observable<T1> left, Observable<T2> right) {
        if (left == null) {
            throw new NullPointerException("left");
        }
        if (right == null) {
            throw new NullPointerException("right");
        }
        return new Pattern2<T1, T2>(left, right);
    }

    /**
     * Matches when the observable sequence has an available element and projects the element by invoking the selector function.
     * @param source the source Observable
     * @param selector the selector to map the source values into result values
     * @param <T1> the source value type
     * @param <R> the result value type
     * @return the new Plan0 representing the mapping
     */
    public static <T1, R> Plan<R> then(/* this */Observable<T1> source, Function<? super T1, ? extends R> selector) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        if (selector == null) {
            throw new NullPointerException("selector");
        }
        return new Pattern1<T1>(source).then(selector);
    }

    /**
     * Joins together the results from several patterns.
     * @param plans the array of plans with the common result type
     * @param <R> the result type
     * @return the Observable coining the plans
     */
    public static <R> Observable<R> when(Plan<R>... plans) {
        if (plans == null) {
            throw new NullPointerException("plans");
        }
        return when(Arrays.asList(plans));
    }

    /**
     * Joins together the results from several patterns.
     * @param plans the iterable sequence of plans
     * @param <R> the common result type
     * @return the Observable joining the plans
     */
    public static <R> Observable<R> when(final Iterable<? extends Plan<R>> plans) {
        if (plans == null) {
            throw new NullPointerException("plans");
        }
        return new Observable<R>() {
            @Override
            protected void subscribeActual(final Observer<? super R> t1) {
                final Map<Object, JoinObserver> externalSubscriptions = new HashMap<Object, JoinObserver>();
                final Object gate = new Object();
                final List<ActivePlan0> activePlans = new ArrayList<ActivePlan0>();

                final Observer<R> out = new SafeObserver<R>(new Observer<R>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(R args) {
                        t1.onNext(args);
                    }

                    @Override
                    public void onError(Throwable e) {
                        for (JoinObserver po : externalSubscriptions.values()) {
                            po.dispose();
                        }
                        t1.onError(e);
                    }

                    @Override
                    public void onComplete() {
                        t1.onComplete();
                    }
                });

                out.onSubscribe(Disposables.empty());

                try {
                    for (Plan<R> plan : plans) {
                        activePlans.add(plan.activate(externalSubscriptions, out, new Consumer<ActivePlan0>() {
                            @Override
                            public void accept(ActivePlan0 activePlan) {
                                activePlans.remove(activePlan);
                                if (activePlans.isEmpty()) {
                                    out.onComplete();
                                }
                            }
                        }));
                    }
                } catch (Throwable t) {
                    Observable.<R> error(t).subscribe(t1);
                    return;
                }
                CompositeDisposable group = new CompositeDisposable();
                t1.onSubscribe(group);
                for (JoinObserver jo : externalSubscriptions.values()) {
                    jo.subscribe(gate);
                    group.add(jo);
                }
            }
        };
    }
}
