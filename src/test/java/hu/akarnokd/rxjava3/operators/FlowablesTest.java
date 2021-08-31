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

package hu.akarnokd.rxjava3.operators;

import org.junit.Test;

import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.rxjava3.internal.functions.Functions;

public class FlowablesTest {

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(Flowables.class);
    }

    @Test
    public void repeatScalar() {
        Flowables.repeat(1)
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatScalarSlowPath() {
        Flowables.repeat(1)
        .rebatchRequests(1)
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatScalarConditional() {
        Flowables.repeat(1)
        .filter(Functions.alwaysTrue())
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatScalarSlowPathConditional() {
        Flowables.repeat(1)
        .filter(Functions.alwaysTrue())
        .rebatchRequests(1)
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatCallable() {
        Flowables.repeatSupplier(Functions.justSupplier(1))
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatCallableSlowPath() {
        Flowables.repeatSupplier(Functions.justSupplier(1))
        .rebatchRequests(1)
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatCallableConditional() {
        Flowables.repeatSupplier(Functions.justSupplier(1))
        .filter(Functions.alwaysTrue())
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void repeatCallableSlowPathConditional() {
        Flowables.repeatSupplier(Functions.justSupplier(1))
        .filter(Functions.alwaysTrue())
        .rebatchRequests(1)
        .take(5)
        .test()
        .assertResult(1, 1, 1, 1, 1);
    }
}
