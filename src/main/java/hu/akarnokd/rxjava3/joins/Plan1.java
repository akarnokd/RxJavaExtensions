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

import io.reactivex.Observer;
import io.reactivex.functions.*;

/**
 * Represents an execution plan for join patterns.
 * @param <T1> the first value type
 * @param <R> the result type
 */
final class Plan1<T1, R> extends Plan<R> {
    protected final Pattern1<T1> expression;
    protected final Function<? super T1, ? extends R> selector;

    Plan1(Pattern1<T1> expression, Function<? super T1, ? extends R> selector) {
        this.expression = expression;
        this.selector = selector;
    }

    @Override
    public ActivePlan0 activate(Map<Object, JoinObserver> externalSubscriptions, final Observer<R> observer, final Consumer<ActivePlan0> deactivate) {
        Consumer<Throwable> onError = onErrorFrom(observer);

        final JoinObserver1<T1> firstJoinObserver = createObserver(externalSubscriptions, expression.o1(), onError);

        final AtomicReference<ActivePlan1<T1>> self = new AtomicReference<ActivePlan1<T1>>();

        ActivePlan1<T1> activePlan = new ActivePlan1<T1>(firstJoinObserver, new Consumer<T1>() {
            @Override
            public void accept(T1 t1) {
                R result;
                try {
                    result = selector.apply(t1);
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
                        firstJoinObserver.removeActivePlan(ap);
                        deactivate.accept(ap);
                    }
                });

        self.set(activePlan);

        firstJoinObserver.addActivePlan(activePlan);
        return activePlan;
    }

}
