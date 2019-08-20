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

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import hu.akarnokd.rxjava3.functions.Consumer7;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.functions.*;

/**
 * Represents an execution plan for join patterns.
 * @param <T1> the first value type
 * @param <T2> the second value type
 * @param <T3> the third value type
 * @param <T4> the fourth value type
 * @param <T5> the fifth value type
 * @param <T6> the sixth value type
 * @param <T7> the sevent value type
 * @param <R> the result type
 */
final class Plan7<T1, T2, T3, T4, T5, T6, T7, R> extends Plan<R> {
    protected final Pattern7<T1, T2, T3, T4, T5, T6, T7> expression;
    protected final Function7<T1, T2, T3, T4, T5, T6, T7, R> selector;

    Plan7(Pattern7<T1, T2, T3, T4, T5, T6, T7> expression, Function7<T1, T2, T3, T4, T5, T6, T7, R> selector) {
        this.expression = expression;
        this.selector = selector;
    }

    @Override
    public ActivePlan0 activate(Map<Object, JoinObserver> externalSubscriptions,
            final Observer<R> observer, final Consumer<ActivePlan0> deactivate) {
        Consumer<Throwable> onError = onErrorFrom(observer);

        final JoinObserver1<T1> jo1 = createObserver(externalSubscriptions, expression.o1(), onError);
        final JoinObserver1<T2> jo2 = createObserver(externalSubscriptions, expression.o2(), onError);
        final JoinObserver1<T3> jo3 = createObserver(externalSubscriptions, expression.o3(), onError);
        final JoinObserver1<T4> jo4 = createObserver(externalSubscriptions, expression.o4(), onError);
        final JoinObserver1<T5> jo5 = createObserver(externalSubscriptions, expression.o5(), onError);
        final JoinObserver1<T6> jo6 = createObserver(externalSubscriptions, expression.o6(), onError);
        final JoinObserver1<T7> jo7 = createObserver(externalSubscriptions, expression.o7(), onError);

        final AtomicReference<ActivePlan0> self = new AtomicReference<ActivePlan0>();

        ActivePlan0 activePlan = new ActivePlan7<T1, T2, T3, T4, T5, T6, T7>(
                jo1, jo2, jo3, jo4, jo5, jo6, jo7,
                new Consumer7<T1, T2, T3, T4, T5, T6, T7>() {
                    @Override
                    public void accept(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
                        R result;
                        try {
                            result = selector.apply(t1, t2, t3, t4, t5, t6, t7);
                        } catch (Throwable t) {
                            observer.onError(t);
                            return;
                        }
                        observer.onNext(result);
                    }
                },
                new Action() {
                    @Override
                    public void run() throws Throwable  {
                        ActivePlan0 ap = self.get();
                        jo1.removeActivePlan(ap);
                        jo2.removeActivePlan(ap);
                        jo3.removeActivePlan(ap);
                        jo4.removeActivePlan(ap);
                        jo5.removeActivePlan(ap);
                        jo6.removeActivePlan(ap);
                        jo7.removeActivePlan(ap);
                        deactivate.accept(ap);
                    }
                });

        self.set(activePlan);

        jo1.addActivePlan(activePlan);
        jo2.addActivePlan(activePlan);
        jo3.addActivePlan(activePlan);
        jo4.addActivePlan(activePlan);
        jo5.addActivePlan(activePlan);
        jo6.addActivePlan(activePlan);
        jo7.addActivePlan(activePlan);

        return activePlan;
    }

}
