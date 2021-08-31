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

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.schedulers.TestScheduler;

public class StatementMaybeTest {
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

    <T> void observe(Maybe<T> source) {
        source.test().assertNoValues().assertNoErrors();
    }

    static <T> void observe(Maybe<T> source, T value) {
        source.test().assertResult(value);
    }

    static <T> void observeError(Maybe<T> source, Class<? extends Throwable> error) {
        source.test().assertFailure(error);
    }

    @Test
    public void testSimple() {
        Maybe<Integer> source1 = Maybe.just(1);
        Maybe<Integer> source2 = Maybe.just(2);

        Map<Integer, Maybe<Integer>> map = new HashMap<>();
        map.put(1, source1);
        map.put(2, source2);

        Maybe<Integer> result = StatementMaybe.switchCase(func, map);

        observe(result, 1);
        observe(result, 2);
        observe(result);
    }

    @Test
    public void testDefaultCase() {
        Maybe<Integer> source1 = Maybe.just(1);
        Maybe<Integer> defaultSource = Maybe.just(2);

        Map<Integer, Maybe<Integer>> map = new HashMap<>();
        map.put(1, source1);

        Maybe<Integer> result = StatementMaybe.switchCase(func, map, defaultSource);

        observe(result, 1);
        observe(result, 2);
        observe(result, 2);
    }

    @Test
    public void testCaseSelectorThrows() {
        Maybe<Integer> source1 = Maybe.just(1);

        Map<Integer, Maybe<Integer>> map = new HashMap<>();
        map.put(1, source1);

        Maybe<Integer> result = StatementMaybe.switchCase(funcError, map);

        observe(result, 1);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testMapGetThrows() {
        Maybe<Integer> source1 = Maybe.just(1);
        Maybe<Integer> source2 = Maybe.just(2);

        Map<Integer, Maybe<Integer>> map = new HashMap<Integer, Maybe<Integer>>() {
            private static final long serialVersionUID = -4342868139960216388L;

            @Override
            public Maybe<Integer> get(Object key) {
                if (key.equals(2)) {
                    throw new RuntimeException("Forced failure!");
                }
                return super.get(key);
            }

        };
        map.put(1, source1);
        map.put(2, source2);

        Maybe<Integer> result = StatementMaybe.switchCase(func, map);

        observe(result, 1);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testMapContainsKeyThrows() {
        Maybe<Integer> source1 = Maybe.just(1);

        Map<Integer, Maybe<Integer>> map = new HashMap<Integer, Maybe<Integer>>() {
            private static final long serialVersionUID = 1975411728567003983L;

            @Override
            public Maybe<Integer> get(Object key) {
                if (key.equals(2)) {
                    throw new RuntimeException("Forced failure!");
                }
                return super.get(key);
            }

        };
        map.put(1, source1);

        Maybe<Integer> result = StatementMaybe.switchCase(func, map);

        observe(result, 1);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testChosenMaybeThrows() {
        Maybe<Integer> source1 = Maybe.just(1);
        Maybe<Integer> source2 = Maybe.error(new RuntimeException("Forced failure"));

        Map<Integer, Maybe<Integer>> map = new HashMap<>();
        map.put(1, source1);
        map.put(2, source2);

        Maybe<Integer> result = StatementMaybe.switchCase(func, map);

        observe(result, 1);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testIfThen() {
        Maybe<Integer> source1 = Maybe.just(1);

        Maybe<Integer> result = StatementMaybe.ifThen(condition, source1);

        observe(result, 1);
        observe(result);
        observe(result, 1);
        observe(result);
    }

    @Test
    public void testIfThenElse() {
        Maybe<Integer> source1 = Maybe.just(1);
        Maybe<Integer> source2 = Maybe.just(2);

        Maybe<Integer> result = StatementMaybe.ifThen(condition, source1, source2);

        observe(result, 1);
        observe(result, 2);
        observe(result, 1);
        observe(result, 2);
    }

    @Test
    public void testIfThenConditonThrows() {
        Maybe<Integer> source1 = Maybe.just(1);

        Maybe<Integer> result = StatementMaybe.ifThen(conditionError, source1);

        observe(result, 1);
        observeError(result, RuntimeException.class);
        observe(result, 1);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testIfThenMaybeThrows() {
        Maybe<Integer> source1 = Maybe.error(new RuntimeException("Forced failure!"));

        Maybe<Integer> result = StatementMaybe.ifThen(condition, source1);

        observeError(result, RuntimeException.class);
        observe(result);

        observeError(result, RuntimeException.class);
        observe(result);
    }

    @Test
    public void testIfThenElseMaybeThrows() {
        Maybe<Integer> source1 = Maybe.just(1);
        Maybe<Integer> source2 = Maybe.error(new RuntimeException("Forced failure!"));

        Maybe<Integer> result = StatementMaybe.ifThen(condition, source1, source2);

        observe(result, 1);
        observeError(result, RuntimeException.class);
        observe(result, 1);
        observeError(result, RuntimeException.class);
    }
}