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

import hu.akarnokd.rxjava3.functions.Consumer3;
import io.reactivex.Notification;
import io.reactivex.functions.Action;

/**
 * Represents an active plan.
 * @param <T1> the first value type
 * @param <T2> the second value type
 * @param <T3> the third value type
 */
final class ActivePlan3<T1, T2, T3> extends ActivePlan0 {
    private final Consumer3<T1, T2, T3> onNext;
    private final Action onCompleted;
    private final JoinObserver1<T1> first;
    private final JoinObserver1<T2> second;
    private final JoinObserver1<T3> third;

    ActivePlan3(JoinObserver1<T1> first,
            JoinObserver1<T2> second,
            JoinObserver1<T3> third,
            Consumer3<T1, T2, T3> onNext,
            Action onCompleted) {
        this.onNext = onNext;
        this.onCompleted = onCompleted;
        this.first = first;
        this.second = second;
        this.third = third;
        addJoinObserver(first);
        addJoinObserver(second);
        addJoinObserver(third);
    }

    @Override
    protected void match() throws Throwable {
        if (!first.queue().isEmpty()
                && !second.queue().isEmpty()
                && !third.queue().isEmpty()) {
            Notification<T1> n1 = first.queue().peek();
            Notification<T2> n2 = second.queue().peek();
            Notification<T3> n3 = third.queue().peek();

            if (n1.isOnComplete() || n2.isOnComplete() || n3.isOnComplete()) {
                onCompleted.run();
            } else {
                dequeue();
                onNext.accept(n1.getValue(), n2.getValue(), n3.getValue());
            }
        }
    }

}
