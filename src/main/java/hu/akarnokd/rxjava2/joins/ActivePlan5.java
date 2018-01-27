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

package hu.akarnokd.rxjava2.joins;

import hu.akarnokd.rxjava2.functions.Consumer5;
import io.reactivex.Notification;
import io.reactivex.functions.Action;

/**
 * Represents an active plan.
 * @param <T1> the first value type
 * @param <T2> the second value type
 * @param <T3> the third value type
 * @param <T4> the fourth value type
 * @param <T5> the fifth value type
 */
final class ActivePlan5<T1, T2, T3, T4, T5> extends ActivePlan0 {
    private final Consumer5<T1, T2, T3, T4, T5> onNext;
    private final Action onCompleted;
    private final JoinObserver1<T1> jo1;
    private final JoinObserver1<T2> jo2;
    private final JoinObserver1<T3> jo3;
    private final JoinObserver1<T4> jo4;
    private final JoinObserver1<T5> jo5;

    ActivePlan5(
            JoinObserver1<T1> jo1,
            JoinObserver1<T2> jo2,
            JoinObserver1<T3> jo3,
            JoinObserver1<T4> jo4,
            JoinObserver1<T5> jo5,
            Consumer5<T1, T2, T3, T4, T5> onNext,
            Action onCompleted) {
        this.onNext = onNext;
        this.onCompleted = onCompleted;
        this.jo1 = jo1;
        this.jo2 = jo2;
        this.jo3 = jo3;
        this.jo4 = jo4;
        this.jo5 = jo5;
        addJoinObserver(jo1);
        addJoinObserver(jo2);
        addJoinObserver(jo3);
        addJoinObserver(jo4);
        addJoinObserver(jo5);
    }

    @Override
    protected void match() throws Exception {
        if (!jo1.queue().isEmpty()
                && !jo2.queue().isEmpty()
                && !jo3.queue().isEmpty()
                && !jo4.queue().isEmpty()
                && !jo5.queue().isEmpty()
        ) {
            Notification<T1> n1 = jo1.queue().peek();
            Notification<T2> n2 = jo2.queue().peek();
            Notification<T3> n3 = jo3.queue().peek();
            Notification<T4> n4 = jo4.queue().peek();
            Notification<T5> n5 = jo5.queue().peek();

            if (n1.isOnComplete()
                    || n2.isOnComplete()
                    || n3.isOnComplete()
                    || n4.isOnComplete()
                    || n5.isOnComplete()
            ) {
                onCompleted.run();
            } else {
                dequeue();
                onNext.accept(
                        n1.getValue(),
                        n2.getValue(),
                        n3.getValue(),
                        n4.getValue(),
                        n5.getValue()
                );
            }
        }
    }

}
