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

import java.util.*;

/**
 * Represents an activated plan.
 */
abstract class ActivePlan0 {
    protected final Map<JoinObserver, JoinObserver> joinObservers = new HashMap<>();

    protected abstract void match() throws Throwable;

    protected void addJoinObserver(JoinObserver joinObserver) {
        joinObservers.put(joinObserver, joinObserver);
    }

    protected void dequeue() {
        for (JoinObserver jo : joinObservers.values()) {
            jo.dequeue();
        }
    }
}
