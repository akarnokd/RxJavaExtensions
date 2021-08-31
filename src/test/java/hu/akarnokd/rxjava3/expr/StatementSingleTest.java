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

package hu.akarnokd.rxjava3.expr;

import java.util.*;

import org.junit.*;
import org.mockito.MockitoAnnotations;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.schedulers.TestScheduler;

public class StatementSingleTest {
    TestScheduler scheduler;
    Supplier<Integer> func;
    Supplier<Integer> funcError;
    BooleanSupplier condition;
    BooleanSupplier conditionError;

    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);
        scheduler = new TestScheduler();
        func = new Supplier<Integer>() {
            int count = 1;

            @Override
            public Integer get() {
                return count++;
            }
        };
        funcError = new Supplier<Integer>() {
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

    <T> void observe(Single<T> source, T value) {
        source.test().assertResult(value);
    }

    <T> void observeError(Single<T> source, Class<? extends Throwable> error) {
        source.test().assertFailure(error);
    }

    @Test
    public void testSimple() {
        Single<Integer> source1 = Single.just(1);
        Single<Integer> source2 = Single.just(2);
        Single<Integer> defaultSource = Single.just(Integer.MAX_VALUE);

        Map<Integer, Single<Integer>> map = new HashMap<>();
        map.put(1, source1);
        map.put(2, source2);

        Single<Integer> result = StatementSingle.switchCase(func, map, defaultSource);

        observe(result, 1);
        observe(result, 2);
    }

    @Test
    public void testDefaultCase() {
        Single<Integer> source1 = Single.just(1);
        Single<Integer> source2 = Single.just(2);

        Map<Integer, Single<Integer>> map = new HashMap<>();
        map.put(1, source1);

        Single<Integer> result = StatementSingle.switchCase(func, map, source2);

        observe(result, 1);
        observe(result, 2);
        observe(result, 2);
    }

    @Test
    public void testCaseSelectorThrows() {
        Single<Integer> source1 = Single.just(1);
        Single<Integer> defaultSource = Single.just(Integer.MAX_VALUE);

        Map<Integer, Single<Integer>> map = new HashMap<>();
        map.put(1, source1);

        Single<Integer> result = StatementSingle.switchCase(funcError, map, defaultSource);

        observe(result, 1);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testMapGetThrows() {
        Single<Integer> source1 = Single.just(1);
        Single<Integer> source2 = Single.just(2);
        Single<Integer> defaultSource = Single.just(Integer.MAX_VALUE);

        Map<Integer, Single<Integer>> map = new HashMap<Integer, Single<Integer>>() {
            private static final long serialVersionUID = -4342868139960216388L;

            @Override
            public Single<Integer> get(Object key) {
                if (key.equals(2)) {
                    throw new RuntimeException("Forced failure!");
                }
                return super.get(key);
            }

        };
        map.put(1, source1);
        map.put(2, source2);

        Single<Integer> result = StatementSingle.switchCase(func, map, defaultSource);

        observe(result, 1);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testMapContainsKeyThrows() {
        Single<Integer> source1 = Single.just(1);
        Single<Integer> defaultSource = Single.just(Integer.MAX_VALUE);

        Map<Integer, Single<Integer>> map = new HashMap<Integer, Single<Integer>>() {
            private static final long serialVersionUID = 1975411728567003983L;

            @Override
            public Single<Integer> get(Object key) {
                if (key.equals(2)) {
                    throw new RuntimeException("Forced failure!");
                }
                return super.get(key);
            }

        };
        map.put(1, source1);

        Single<Integer> result = StatementSingle.switchCase(func, map, defaultSource);

        observe(result, 1);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testChosenSingleThrows() {
        Single<Integer> source1 = Single.just(1);
        Single<Integer> source2 = Single.error(new RuntimeException("Forced failure"));
        Single<Integer> defaultSource = Single.just(Integer.MAX_VALUE);

        Map<Integer, Single<Integer>> map = new HashMap<>();
        map.put(1, source1);
        map.put(2, source2);

        Single<Integer> result = StatementSingle.switchCase(func, map, defaultSource);

        observe(result, 1);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testIfThenElse() {
        Single<Integer> source1 = Single.just(1);
        Single<Integer> source2 = Single.just(2);

        Single<Integer> result = StatementSingle.ifThen(condition, source1, source2);

        observe(result, 1);
        observe(result, 2);
        observe(result, 1);
        observe(result, 2);
    }

    @Test
    public void testIfThenConditionThrows() {
        Single<Integer> source1 = Single.just(1);
        Single<Integer> source2 = Single.just(2);

        Single<Integer> result = StatementSingle.ifThen(conditionError, source1, source2);

        observe(result, 1);
        observeError(result, RuntimeException.class);
        observe(result, 1);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testIfThenElseSingleThrows() {
        Single<Integer> source1 = Single.just(1);
        Single<Integer> source2 = Single.error(new RuntimeException("Forced failure!"));

        Single<Integer> result = StatementSingle.ifThen(condition, source1, source2);

        observe(result, 1);
        observeError(result, RuntimeException.class);
        observe(result, 1);
        observeError(result, RuntimeException.class);
    }
}