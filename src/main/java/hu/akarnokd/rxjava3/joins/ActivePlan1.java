/*
 * Copyright 2016-present David Karnok
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

import io.reactivex.rxjava3.core.Notification;
import io.reactivex.rxjava3.functions.*;

/**
 * Represents an active plan.
 * @param <T1> the first value type
 */
final class ActivePlan1<T1> extends ActivePlan0 {
    private final Consumer<T1> onNext;
    private final Action onCompleted;
    private final JoinObserver1<T1> jo1;

    ActivePlan1(JoinObserver1<T1> jo1, Consumer<T1> onNext, Action onCompleted) {
        this.onNext = onNext;
        this.onCompleted = onCompleted;
        this.jo1 = jo1;
        addJoinObserver(jo1);
    }

    @Override
    protected void match() throws Throwable {
        if (!jo1.queue().isEmpty()) {
            Notification<T1> n1 = jo1.queue().peek();
            if (n1.isOnComplete()) {
                onCompleted.run();
            } else {
                dequeue();
                onNext.accept(n1.getValue());
            }
        }
    }

}
