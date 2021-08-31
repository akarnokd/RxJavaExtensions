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

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.schedulers.*;

public class StatementFlowableTest {
    TestScheduler scheduler;
    Supplier<Integer> func;
    Supplier<Integer> funcError;
    BooleanSupplier condition;
    BooleanSupplier conditionError;
    int numRecursion = 250;

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

    @SafeVarargs
    static <T> void observe(Flowable<T> source, T... values) {
        source.test().assertResult(values);
    }

    <T> void observeSequence(Flowable<T> source, Iterable<? extends T> values) {
        source.test().assertValueSequence(values).assertNoErrors().assertComplete();
    }

    @SafeVarargs
    static <T> void observeError(Flowable<T> source, Class<? extends Throwable> error, T... valuesBeforeError) {
        source.test().assertFailure(error, valuesBeforeError);
    }

    <T> void observeSequenceError(Flowable<T> source, Class<? extends Throwable> error, Iterable<? extends T> valuesBeforeError) {
        source.test().assertValueSequence(valuesBeforeError).assertError(error).assertNotComplete();
    }

    @Test
    public void testSimple() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);
        Flowable<Integer> source2 = Flowable.just(4, 5, 6);

        Map<Integer, Flowable<Integer>> map = new HashMap<>();
        map.put(1, source1);
        map.put(2, source2);

        Flowable<Integer> result = StatementFlowable.switchCase(func, map);

        observe(result, 1, 2, 3);
        observe(result, 4, 5, 6);
        observe(result);
    }

    @Test
    public void testDefaultCase() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);
        Flowable<Integer> source2 = Flowable.just(4, 5, 6);

        Map<Integer, Flowable<Integer>> map = new HashMap<>();
        map.put(1, source1);

        Flowable<Integer> result = StatementFlowable.switchCase(func, map, source2);

        observe(result, 1, 2, 3);
        observe(result, 4, 5, 6);
        observe(result, 4, 5, 6);
    }

    @Test
    public void testCaseSelectorThrows() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);

        Map<Integer, Flowable<Integer>> map = new HashMap<>();
        map.put(1, source1);

        Flowable<Integer> result = StatementFlowable.switchCase(funcError, map);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testMapGetThrows() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);
        Flowable<Integer> source2 = Flowable.just(4, 5, 6);

        Map<Integer, Flowable<Integer>> map = new HashMap<Integer, Flowable<Integer>>() {
            private static final long serialVersionUID = -4342868139960216388L;

            @Override
            public Flowable<Integer> get(Object key) {
                if (key.equals(2)) {
                    throw new RuntimeException("Forced failure!");
                }
                return super.get(key);
            }

        };
        map.put(1, source1);
        map.put(2, source2);

        Flowable<Integer> result = StatementFlowable.switchCase(func, map);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testMapContainsKeyThrows() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);

        Map<Integer, Flowable<Integer>> map = new HashMap<Integer, Flowable<Integer>>() {
            private static final long serialVersionUID = 1975411728567003983L;

            @Override
            public Flowable<Integer> get(Object key) {
                if (key.equals(2)) {
                    throw new RuntimeException("Forced failure!");
                }
                return super.get(key);
            }

        };
        map.put(1, source1);

        Flowable<Integer> result = StatementFlowable.switchCase(func, map);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testChosenFlowableThrows() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);
        Flowable<Integer> source2 = Flowable.error(new RuntimeException("Forced failure"));

        Map<Integer, Flowable<Integer>> map = new HashMap<>();
        map.put(1, source1);
        map.put(2, source2);

        Flowable<Integer> result = StatementFlowable.switchCase(func, map);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testIfThen() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);

        Flowable<Integer> result = StatementFlowable.ifThen(condition, source1);

        observe(result, 1, 2, 3);
        observe(result);
        observe(result, 1, 2, 3);
        observe(result);
    }

    @Test
    public void testIfThenElse() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);
        Flowable<Integer> source2 = Flowable.just(4, 5, 6);

        Flowable<Integer> result = StatementFlowable.ifThen(condition, source1, source2);

        observe(result, 1, 2, 3);
        observe(result, 4, 5, 6);
        observe(result, 1, 2, 3);
        observe(result, 4, 5, 6);
    }

    @Test
    public void testIfThenConditonThrows() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);

        Flowable<Integer> result = StatementFlowable.ifThen(conditionError, source1);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testIfThenFlowableThrows() {
        Flowable<Integer> source1 = Flowable.error(new RuntimeException("Forced failure!"));

        Flowable<Integer> result = StatementFlowable.ifThen(condition, source1);

        observeError(result, RuntimeException.class);
        observe(result);

        observeError(result, RuntimeException.class);
        observe(result);
    }

    @Test
    public void testIfThenElseFlowableThrows() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);
        Flowable<Integer> source2 = Flowable.error(new RuntimeException("Forced failure!"));

        Flowable<Integer> result = StatementFlowable.ifThen(condition, source1, source2);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testDoWhile() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);

        Flowable<Integer> result = StatementFlowable.doWhile(source1, condition);

        observe(result, 1, 2, 3, 1, 2, 3);
    }

    @Test
    public void testDoWhileOnce() throws Throwable {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);

        condition.getAsBoolean(); // toggle to false
        Flowable<Integer> result = StatementFlowable.doWhile(source1, condition);

        observe(result, 1, 2, 3);
    }

    @Test
    public void testDoWhileConditionThrows() throws Throwable {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);
        conditionError.getAsBoolean();
        Flowable<Integer> result = StatementFlowable.doWhile(source1, conditionError);

        observeError(result, RuntimeException.class, 1, 2, 3);
    }

    @Test
    public void testDoWhileSourceThrows() {
        Flowable<Integer> source1 = Flowable.concat(Flowable.just(1, 2, 3),
                Flowable.<Integer> error(new RuntimeException("Forced failure!")));

        Flowable<Integer> result = StatementFlowable.doWhile(source1, condition);

        observeError(result, RuntimeException.class, 1, 2, 3);
    }

    BooleanSupplier countdown(final int n) {
        return new BooleanSupplier() {
            int count = n;

            @Override
            public boolean getAsBoolean() {
                return count-- > 0;
            }
        };
    }

    @Test
    public void testDoWhileManyTimes() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3).subscribeOn(Schedulers.trampoline());

        List<Integer> expected = new ArrayList<>(numRecursion * 3);
        for (int i = 0; i < numRecursion; i++) {
            expected.add(1);
            expected.add(2);
            expected.add(3);
        }

        Flowable<Integer> result = StatementFlowable.doWhile(source1, countdown(numRecursion - 1));

        observeSequence(result, expected);
    }

    @Test
    public void testWhileDo() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);
        Flowable<Integer> result = StatementFlowable.whileDo(source1, countdown(2));

        observe(result, 1, 2, 3, 1, 2, 3);
    }

    @Test
    public void testWhileDoOnce() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);
        Flowable<Integer> result = StatementFlowable.whileDo(source1, countdown(1));

        observe(result, 1, 2, 3);
    }

    @Test
    public void testWhileDoZeroTimes() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);
        Flowable<Integer> result = StatementFlowable.whileDo(source1, countdown(0));

        observe(result);
    }

    @Test
    public void testWhileDoManyTimes() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3).subscribeOn(Schedulers.trampoline());

        List<Integer> expected = new ArrayList<>(numRecursion * 3);
        for (int i = 0; i < numRecursion; i++) {
            expected.add(1);
            expected.add(2);
            expected.add(3);
        }

        Flowable<Integer> result = StatementFlowable.whileDo(source1, countdown(numRecursion));

        observeSequence(result, expected);
    }

    @Test
    public void testWhileDoConditionThrows() {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);
        Flowable<Integer> result = StatementFlowable.whileDo(source1, conditionError);

        observeError(result, RuntimeException.class, 1, 2, 3);
    }

    @Test
    public void testWhileDoConditionThrowsImmediately() throws Throwable {
        Flowable<Integer> source1 = Flowable.just(1, 2, 3);
        conditionError.getAsBoolean();
        Flowable<Integer> result = StatementFlowable.whileDo(source1, conditionError);

        observeError(result, RuntimeException.class);
    }

    @Test
    public void testWhileDoSourceThrows() {
        Flowable<Integer> source1 = Flowable.concat(Flowable.just(1, 2, 3),
                Flowable.<Integer> error(new RuntimeException("Forced failure!")));

        Flowable<Integer> result = StatementFlowable.whileDo(source1, condition);

        observeError(result, RuntimeException.class, 1, 2, 3);
    }
}