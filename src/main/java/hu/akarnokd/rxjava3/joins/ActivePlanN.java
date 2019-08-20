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

import io.reactivex.rxjava3.core.Notification;
import io.reactivex.rxjava3.functions.*;

/**
 * Represents an active plan.
 */
final class ActivePlanN extends ActivePlan0 {
    private final Consumer<? super Object[]> onNext;
    private final Action onCompleted;
    private final List<JoinObserver1<? extends Object>> observers;

    ActivePlanN(List<JoinObserver1<? extends Object>> observers,
            Consumer<? super Object[]> onNext,
            Action onCompleted) {
        this.onNext = onNext;
        this.onCompleted = onCompleted;
        this.observers = new ArrayList<JoinObserver1<? extends Object>>(observers);
        for (JoinObserver1<? extends Object> jo : this.observers) {
            addJoinObserver(jo);
        }
    }

    @Override
    protected void match() throws Throwable {
        Object[] notifications = new Object[this.observers.size()];
        int j = 0;
        int completedCount = 0;
        for (JoinObserver1<? extends Object> jo : this.observers) {
            if (jo.queue().isEmpty()) {
                return;
            }
            Notification<? extends Object> n = jo.queue().peek();
            if (n.isOnComplete()) {
                completedCount++;
            }
            notifications[j] = n.getValue();
            j++;
        }

        if (completedCount == j) {
            onCompleted.run();
        } else {
            dequeue();
            onNext.accept(notifications);
        }
    }

}
