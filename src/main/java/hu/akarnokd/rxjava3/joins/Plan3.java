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

import hu.akarnokd.rxjava3.functions.Consumer3;
import io.reactivex.Observer;
import io.reactivex.functions.*;

/**
 * Represents an execution plan for join patterns.
 * @param <T1> the first value type
 * @param <T2> the second value type
 * @param <T3> the third value type
 * @param <R> the result type
 */
final class Plan3<T1, T2, T3, R> extends Plan<R> {
    protected final Pattern3<T1, T2, T3> expression;
    protected final Function3<T1, T2, T3, R> selector;

    Plan3(Pattern3<T1, T2, T3> expression, Function3<T1, T2, T3, R> selector) {
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

        final AtomicReference<ActivePlan3<T1, T2, T3>> self = new AtomicReference<ActivePlan3<T1, T2, T3>>();

        ActivePlan3<T1, T2, T3> activePlan = new ActivePlan3<T1, T2, T3>(
                jo1, jo2, jo3,
            new Consumer3<T1, T2, T3>() {
                    @Override
                    public void accept(T1 t1, T2 t2, T3 t3) {
                        R result;
                        try {
                            result = selector.apply(t1, t2, t3);
                        } catch (Throwable t) {
                            observer.onError(t);
                            return;
                        }
                        observer.onNext(result);
                    }
                },
                new Action() {
                    @Override
                    public void run() throws Throwable {
                        ActivePlan0 ap = self.get();
                        jo1.removeActivePlan(ap);
                        jo2.removeActivePlan(ap);
                        jo3.removeActivePlan(ap);
                        deactivate.accept(ap);
                    }
                });

        self.set(activePlan);

        jo1.addActivePlan(activePlan);
        jo2.addActivePlan(activePlan);
        jo3.addActivePlan(activePlan);

        return activePlan;
    }

}
