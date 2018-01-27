/*
 * Copyright 2016-2018 David Karnok
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

package hu.akarnokd.rxjava2.expr;

import java.util.*;
import java.util.concurrent.Callable;

import org.junit.*;
import org.mockito.MockitoAnnotations;

import io.reactivex.Observable;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.schedulers.*;

public class StatementObservableTest {
    TestScheduler scheduler;
    Callable<Integer> func;
    Callable<Integer> funcError;
    BooleanSupplier condition;
    BooleanSupplier conditionError;
    int numRecursion = 250;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        scheduler = new TestScheduler();
        func = new Callable<Integer>() {
            int count = 1;

            @Override
            public Integer call() {
                return count++;
            }
        };
        funcError = new Callable<Integer>() {
            int count = 1;

            @Override
            public Integer call() {
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

    <T> void observe(Observable<T> source, T... values) {
        source.test().assertResult(values);
    }

    <T> void observeSequence(Observable<T> source, Iterable<? extends T> values) {
        source.test().assertValueSequence(values).assertNoErrors().assertComplete().assertSubscribed();
    }

    <T> void observeError(Observable<T> source, Class<? extends Throwable> error, T... valuesBeforeError) {
        source.test().assertFailure(error, valuesBeforeError);
    }

    <T> void observeSequenceError(Observable<T> source, Class<? extends Throwable> error, Iterable<? extends T> valuesBeforeError) {
        source.test().assertValueSequence(valuesBeforeError).assertError(error).assertNotComplete().assertSubscribed();
    }

    @Test
    public void testSimple() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);
        Observable<Integer> source2 = Observable.just(4, 5, 6);

        Map<Integer, Observable<Integer>> map = new HashMap<Integer, Observable<Integer>>();
        map.put(1, source1);
        map.put(2, source2);

        Observable<Integer> result = StatementObservable.switchCase(func, map);

        observe(result, 1, 2, 3);
        observe(result, 4, 5, 6);
        observe(result);
    }

    @Test
    public void testDefaultCase() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);
        Observable<Integer> source2 = Observable.just(4, 5, 6);

        Map<Integer, Observable<Integer>> map = new HashMap<Integer, Observable<Integer>>();
        map.put(1, source1);

        Observable<Integer> result = StatementObservable.switchCase(func, map, source2);

        observe(result, 1, 2, 3);
        observe(result, 4, 5, 6);
        observe(result, 4, 5, 6);
    }

    @Test
    public void testCaseSelectorThrows() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);

        Map<Integer, Observable<Integer>> map = new HashMap<Integer, Observable<Integer>>();
        map.put(1, source1);

        Observable<Integer> result = StatementObservable.switchCase(funcError, map);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testMapGetThrows() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);
        Observable<Integer> source2 = Observable.just(4, 5, 6);

        Map<Integer, Observable<Integer>> map = new HashMap<Integer, Observable<Integer>>() {
            private static final long serialVersionUID = -4342868139960216388L;

            @Override
            public Observable<Integer> get(Object key) {
                if (key.equals(2)) {
                    throw new RuntimeException("Forced failure!");
                }
                return super.get(key);
            }

        };
        map.put(1, source1);
        map.put(2, source2);

        Observable<Integer> result = StatementObservable.switchCase(func, map);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testMapContainsKeyThrows() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);

        Map<Integer, Observable<Integer>> map = new HashMap<Integer, Observable<Integer>>() {
            private static final long serialVersionUID = 1975411728567003983L;

            @Override
            public Observable<Integer> get(Object key) {
                if (key.equals(2)) {
                    throw new RuntimeException("Forced failure!");
                }
                return super.get(key);
            }

        };
        map.put(1, source1);

        Observable<Integer> result = StatementObservable.switchCase(func, map);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testChosenObservableThrows() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);
        Observable<Integer> source2 = Observable.error(new RuntimeException("Forced failure"));

        Map<Integer, Observable<Integer>> map = new HashMap<Integer, Observable<Integer>>();
        map.put(1, source1);
        map.put(2, source2);

        Observable<Integer> result = StatementObservable.switchCase(func, map);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testIfThen() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);

        Observable<Integer> result = StatementObservable.ifThen(condition, source1);

        observe(result, 1, 2, 3);
        observe(result);
        observe(result, 1, 2, 3);
        observe(result);
    }

    @Test
    public void testIfThenElse() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);
        Observable<Integer> source2 = Observable.just(4, 5, 6);

        Observable<Integer> result = StatementObservable.ifThen(condition, source1, source2);

        observe(result, 1, 2, 3);
        observe(result, 4, 5, 6);
        observe(result, 1, 2, 3);
        observe(result, 4, 5, 6);
    }

    @Test
    public void testIfThenConditonThrows() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);

        Observable<Integer> result = StatementObservable.ifThen(conditionError, source1);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testIfThenObservableThrows() {
        Observable<Integer> source1 = Observable.error(new RuntimeException("Forced failure!"));

        Observable<Integer> result = StatementObservable.ifThen(condition, source1);

        observeError(result, RuntimeException.class);
        observe(result);

        observeError(result, RuntimeException.class);
        observe(result);
    }

    @Test
    public void testIfThenElseObservableThrows() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);
        Observable<Integer> source2 = Observable.error(new RuntimeException("Forced failure!"));

        Observable<Integer> result = StatementObservable.ifThen(condition, source1, source2);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testDoWhile() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);

        Observable<Integer> result = StatementObservable.doWhile(source1, condition);

        observe(result, 1, 2, 3, 1, 2, 3);
    }

    @Test
    public void testDoWhileOnce() throws Exception {
        Observable<Integer> source1 = Observable.just(1, 2, 3);

        condition.getAsBoolean(); // toggle to false
        Observable<Integer> result = StatementObservable.doWhile(source1, condition);

        observe(result, 1, 2, 3);
    }

    @Test
    public void testDoWhileConditionThrows() throws Exception {
        Observable<Integer> source1 = Observable.just(1, 2, 3);
        conditionError.getAsBoolean();
        Observable<Integer> result = StatementObservable.doWhile(source1, conditionError);

        observeError(result, RuntimeException.class, 1, 2, 3);
    }

    @Test
    public void testDoWhileSourceThrows() {
        Observable<Integer> source1 = Observable.concat(Observable.just(1, 2, 3),
                Observable.<Integer> error(new RuntimeException("Forced failure!")));

        Observable<Integer> result = StatementObservable.doWhile(source1, condition);

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
        Observable<Integer> source1 = Observable.just(1, 2, 3).subscribeOn(Schedulers.trampoline());

        List<Integer> expected = new ArrayList<Integer>(numRecursion * 3);
        for (int i = 0; i < numRecursion; i++) {
            expected.add(1);
            expected.add(2);
            expected.add(3);
        }

        Observable<Integer> result = StatementObservable.doWhile(source1, countdown(numRecursion - 1));

        observeSequence(result, expected);
    }

    @Test
    public void testWhileDo() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);
        Observable<Integer> result = StatementObservable.whileDo(source1, countdown(2));

        observe(result, 1, 2, 3, 1, 2, 3);
    }

    @Test
    public void testWhileDoOnce() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);
        Observable<Integer> result = StatementObservable.whileDo(source1, countdown(1));

        observe(result, 1, 2, 3);
    }

    @Test
    public void testWhileDoZeroTimes() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);
        Observable<Integer> result = StatementObservable.whileDo(source1, countdown(0));

        observe(result);
    }

    @Test
    public void testWhileDoManyTimes() {
        Observable<Integer> source1 = Observable.just(1, 2, 3).subscribeOn(Schedulers.trampoline());

        List<Integer> expected = new ArrayList<Integer>(numRecursion * 3);
        for (int i = 0; i < numRecursion; i++) {
            expected.add(1);
            expected.add(2);
            expected.add(3);
        }

        Observable<Integer> result = StatementObservable.whileDo(source1, countdown(numRecursion));

        observeSequence(result, expected);
    }

    @Test
    public void testWhileDoConditionThrows() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);
        Observable<Integer> result = StatementObservable.whileDo(source1, conditionError);

        observeError(result, RuntimeException.class, 1, 2, 3);
    }

    @Test
    public void testWhileDoConditionThrowsImmediately() throws Exception {
        Observable<Integer> source1 = Observable.just(1, 2, 3);
        conditionError.getAsBoolean();
        Observable<Integer> result = StatementObservable.whileDo(source1, conditionError);

        observeError(result, RuntimeException.class);
    }

    @Test
    public void testWhileDoSourceThrows() {
        Observable<Integer> source1 = Observable.concat(Observable.just(1, 2, 3),
                Observable.<Integer> error(new RuntimeException("Forced failure!")));

        Observable<Integer> result = StatementObservable.whileDo(source1, condition);

        observeError(result, RuntimeException.class, 1, 2, 3);
    }
}