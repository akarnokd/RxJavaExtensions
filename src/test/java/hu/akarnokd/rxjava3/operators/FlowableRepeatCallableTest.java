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

import java.io.IOException;

import org.junit.Test;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class FlowableRepeatCallableTest {

    @Test
    public void fastPathCrash() {
        Flowables.repeatSupplier(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                throw new IOException();
            }
        })
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void slowPathCrash() {
        Flowables.repeatSupplier(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                throw new IOException();
            }
        })
        .test(5)
        .assertFailure(IOException.class);
    }

    @Test
    public void requestLimited() {
        Flowables.repeatSupplier(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                return 1;
            }
        })
        .test(5)
        .assertValues(1, 1, 1, 1, 1)
        .assertNoErrors()
        .assertNotComplete();
    }

    @Test
    public void take() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>(2) {
            @Override
            public void onNext(Object t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowables.repeatSupplier(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                return 1;
            }
        })
        .subscribe(ts);

        ts.assertResult(1);
    }

    @Test
    public void fused() {
        Flowables.repeatSupplier(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                return 1;
            }
        })
        .concatMap(Functions.justFunction(Flowable.just(2)))
        .take(5)
        .test()
        .assertResult(2, 2, 2, 2, 2);
    }

    @Test
    public void fastPathCrashConditional() {
        Flowables.repeatSupplier(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                throw new IOException();
            }
        })
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void slowPathCrashConditional() {
        Flowables.repeatSupplier(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                throw new IOException();
            }
        })
        .filter(Functions.alwaysTrue())
        .test(5)
        .assertFailure(IOException.class);
    }

    @Test
    public void requestLimitedConditional() {
        Flowables.repeatSupplier(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                return 1;
            }
        })
        .filter(Functions.alwaysTrue())
        .test(5)
        .assertValues(1, 1, 1, 1, 1)
        .assertNoErrors()
        .assertNotComplete();
    }

    @Test
    public void takeConditional() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>(2) {
            @Override
            public void onNext(Object t) {
                super.onNext(t);
                cancel();
                onComplete();
            }
        };

        Flowables.repeatSupplier(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                return 1;
            }
        })
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertResult(1);
    }

    @Test
    public void fusedConditional() {
        Flowables.repeatSupplier(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                return 1;
            }
        })
        .filter(Functions.alwaysTrue())
        .concatMap(Functions.justFunction(Flowable.just(2)))
        .take(5)
        .test()
        .assertResult(2, 2, 2, 2, 2);
    }
}
