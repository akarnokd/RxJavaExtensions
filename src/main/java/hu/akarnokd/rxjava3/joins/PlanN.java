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
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observer;
import io.reactivex.functions.*;

/**
 * Represents an execution plan for join patterns.
 * @param <R> the result type
 */
final class PlanN<R> extends Plan<R> {
    protected final PatternN expression;
    protected final Function<? super Object[], R> selector;

    PlanN(PatternN expression, Function<? super Object[], R> selector) {
        this.expression = expression;
        this.selector = selector;
    }

    @Override
    public ActivePlan0 activate(Map<Object, JoinObserver> externalSubscriptions,
            final Observer<R> observer, final Consumer<ActivePlan0> deactivate) {
        Consumer<Throwable> onError = onErrorFrom(observer);

        final List<JoinObserver1<? extends Object>> observers = new ArrayList<JoinObserver1<? extends Object>>();
        for (int i = 0; i < expression.size(); i++) {
            observers.add(createObserver(externalSubscriptions, expression.get(i), onError));
        }
        final AtomicReference<ActivePlanN> self = new AtomicReference<ActivePlanN>();

        ActivePlanN activePlan = new ActivePlanN(observers, new Consumer<Object[]>() {
                    @Override
                    public void accept(Object[]args) {
                        R result;
                        try {
                            result = selector.apply(args);
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
                        for (JoinObserver1<? extends Object> jo : observers) {
                            jo.removeActivePlan(self.get());
                        }
                        deactivate.accept(self.get());
                    }
                });

        self.set(activePlan);

        for (JoinObserver1<? extends Object> jo : observers) {
            jo.addActivePlan(activePlan);
        }

        return activePlan;
    }

}
