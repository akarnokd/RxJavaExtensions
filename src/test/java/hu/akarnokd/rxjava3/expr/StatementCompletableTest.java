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

package hu.akarnokd.rxjava3.expr;

import java.util.*;

import org.junit.*;
import org.mockito.MockitoAnnotations;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.schedulers.TestScheduler;

@SuppressWarnings("rawtypes")
public class StatementCompletableTest {
    TestScheduler scheduler;
    Supplier func;
    Supplier funcError;
    BooleanSupplier condition;
    BooleanSupplier conditionError;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        scheduler = new TestScheduler();
        func = new Supplier() {
            int count = 1;

            @Override
            public Integer get() {
                return count++;
            }
        };
        funcError = new Supplier() {
            int count = 1;

            @Override
            public Integer get() {
                if (count == 2) {
                    throw new RuntimeException("Forced failure!");
                }
                return count++;
            }
        };
        condition = new BooleanSupplier() {
            boolean r;

            @Override
            public boolean getAsBoolean() {
                r = !r;
                return r;
            }

        };
        conditionError = new BooleanSupplier() {
            boolean r;

            @Override
            public boolean getAsBoolean() {
                r = !r;
                if (!r) {
                    throw new RuntimeException("Forced failure!");
                }
                return r;
            }

        };
    }

     void observe(Completable source) {
        source.test().assertComplete();
    }

     void observeError(Completable source, Class<? extends Throwable> error) {
        source.test().assertFailure(error);
    }

    @Test
    public void testSimple() {
        Completable source1 = Completable.complete();
        Completable source2 = Completable.complete();
        Completable defaultSource = Completable.error(new RuntimeException("Forced Failure"));

        Map<Integer, Completable> map = new HashMap<>();
        map.put(1, source1);
        map.put(2, source2);

        @SuppressWarnings("unchecked")
        Completable result = StatementCompletable.switchCase(func, map, defaultSource);

        observe(result);
        observe(result);
    }

    @Test
    public void testDefaultCase() {
        Completable source1 = Completable.error(new RuntimeException("Forced Failure"));
        Completable source2 = Completable.complete();

        Map<Integer, Completable> map = new HashMap<>();
        map.put(1, source1);

        @SuppressWarnings("unchecked")
        Completable result = StatementCompletable.switchCase(func, map, source2);

        observeError(result, RuntimeException.class);
        observe(result);
    }

    @Test
    public void testCaseSelectorThrows() {
        Completable source1 = Completable.complete();
        Completable defaultSource = Completable.complete();

        Map<Integer, Completable> map = new HashMap<>();
        map.put(1, source1);

        @SuppressWarnings("unchecked")
        Completable result = StatementCompletable.switchCase(funcError, map, defaultSource);

        observe(result);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testMapGetThrows() {
        Completable source1 = Completable.complete();
        Completable source2 = Completable.complete();
        Completable defaultSource = Completable.complete();

        Map<Integer, Completable> map = new HashMap<Integer, Completable>() {
            private static final long serialVersionUID = -4342868139960216388L;

            @Override
            public Completable get(Object key) {
                if (key.equals(2)) {
                    throw new RuntimeException("Forced failure!");
                }
                return super.get(key);
            }

        };
        map.put(1, source1);
        map.put(2, source2);

        @SuppressWarnings("unchecked")
        Completable result = StatementCompletable.switchCase(func, map, defaultSource);

        observe(result);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testMapContainsKeyThrows() {
        Completable source1 = Completable.complete();
        Completable defaultSource = Completable.complete();

        Map<Integer, Completable> map = new HashMap<Integer, Completable>() {
            private static final long serialVersionUID = 1975411728567003983L;

            @Override
            public Completable get(Object key) {
                if (key.equals(2)) {
                    throw new RuntimeException("Forced failure!");
                }
                return super.get(key);
            }

        };
        map.put(1, source1);

        @SuppressWarnings("unchecked")
        Completable result = StatementCompletable.switchCase(func, map, defaultSource);

        observe(result);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testChosenCompletableThrows() {
        Completable source1 = Completable.complete();
        Completable source2 = Completable.error(new RuntimeException("Forced failure"));
        Completable defaultSource = Completable.complete();

        Map<Integer, Completable> map = new HashMap<>();
        map.put(1, source1);
        map.put(2, source2);

        @SuppressWarnings("unchecked")
        Completable result = StatementCompletable.switchCase(func, map, defaultSource);

        observe(result);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testIfThenElse() {
        Completable source1 = Completable.complete();
        Completable source2 = Completable.error(new RuntimeException("Forced failure"));

        Completable result = StatementCompletable.ifThen(condition, source1, source2);

        observe(result);
        observeError(result, RuntimeException.class);
        observe(result);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testIfThenConditionThrows() {
        Completable source1 = Completable.complete();
        Completable source2 = Completable.complete();

        Completable result = StatementCompletable.ifThen(conditionError, source1, source2);

        observe(result);
        observeError(result, RuntimeException.class);
        observe(result);
        observeError(result, RuntimeException.class);
    }
}