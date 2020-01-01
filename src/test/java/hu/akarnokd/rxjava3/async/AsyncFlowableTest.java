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

package hu.akarnokd.rxjava3.async;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava3.functions.*;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.processors.UnicastProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AsyncFlowableTest {

    @Test
    public void startDefault() {

        final AtomicInteger counter = new AtomicInteger();

        Flowable<Integer> source = AsyncFlowable.start(new Supplier<Integer>() {
            @Override
            public Integer get() throws Exception {
                return counter.incrementAndGet();
            }
        });

        for (int i = 0; i < 5; i++) {
            source.test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1);

            assertEquals(1, counter.get());
        }
    }

    @Test
    public void startCustomScheduler() {
        final AtomicInteger counter = new AtomicInteger();

        Flowable<Integer> source = AsyncFlowable.start(new Supplier<Integer>() {
            @Override
            public Integer get() throws Exception {
                return counter.incrementAndGet();
            }
        }, Schedulers.single());

        for (int i = 0; i < 5; i++) {
            source.test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1);

            assertEquals(1, counter.get());
        }
    }

    @SuppressWarnings("rawtypes")
    static final class MultiFunction
    implements Action, Consumer, BiConsumer, Consumer3, Consumer4, Consumer5,
    Consumer6, Consumer7, Consumer8, Consumer9,
    SimpleCallable, Function, BiFunction, Function3, Function4, Function5,
    Function6, Function7, Function8, Function9, Supplier {

        final AtomicReference<String> state = new AtomicReference<>();

        void setState(String s) {
            state.set(s);
        }

        String getState() {
            return state.get();
        }

        @Override
        public void accept(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7, Object t8,
                Object t9) throws Exception {
            setState("" + t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8 + t9);
        }

        @Override
        public void accept(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7, Object t8)
                throws Exception {
            setState("" + t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8);
        }

        @Override
        public void accept(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7)
                throws Exception {
            setState("" + t1 + t2 + t3 + t4 + t5 + t6 + t7);
        }

        @Override
        public void accept(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6) throws Exception {
            setState("" + t1 + t2 + t3 + t4 + t5 + t6);
        }

        @Override
        public void accept(Object t1, Object t2, Object t3, Object t4, Object t5) throws Exception {
            setState("" + t1 + t2 + t3 + t4 + t5);
        }

        @Override
        public void accept(Object t1, Object t2, Object t3, Object t4) throws Exception {
            setState("" + t1 + t2 + t3 + t4);
        }

        @Override
        public void accept(Object t1, Object t2, Object t3) throws Exception {
            setState("" + t1 + t2 + t3);
        }

        @Override
        public void accept(Object t1, Object t2) throws Exception {
            setState("" + t1 + t2);
        }

        @Override
        public void accept(Object t) throws Exception {
            if (t instanceof Object[]) {
                setState(Arrays.toString((Object[])t));
            } else {
                setState("" + t);
            }
        }

        @Override
        public void run() throws Exception {
            setState("-");
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7, Object t8,
                Object t9) throws Exception {
            return "" + t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8 + t9;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7, Object t8)
                throws Exception {
            return "" + t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7)
                throws Exception {
            return "" + t1 + t2 + t3 + t4 + t5 + t6 + t7;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6) throws Exception {
            return "" + t1 + t2 + t3 + t4 + t5 + t6;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4, Object t5) throws Exception {
            return "" + t1 + t2 + t3 + t4 + t5;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3, Object t4) throws Exception {
            return "" + t1 + t2 + t3 + t4;
        }

        @Override
        public Object apply(Object t1, Object t2, Object t3) throws Exception {
            return "" + t1 + t2 + t3;
        }

        @Override
        public Object apply(Object t1, Object t2) throws Exception {
            return "" + t1 + t2;
        }

        @Override
        public Object apply(Object t) throws Exception {
            if (t instanceof Object[]) {
                return Arrays.toString((Object[])t);
            }
            return "" + t;
        }

        @Override
        public Object call() {
            return "-";
        }

        @Override
        public Object get() {
            return "-";
        }
}

    @Test
    public void toAsyncConsumer0() {
        final AtomicInteger counter = new AtomicInteger();

        AsyncFlowable.toAsync(new Action() {
            @Override
            public void run() throws Exception {
                counter.getAndIncrement();
            }
        })
        .call()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer1() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer<Object>)f)
        .apply(1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "1");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer2() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((BiConsumer<Object, Object>)f)
        .apply(1, 2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "12");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer3() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer3<Object, Object, Object>)f)
        .apply(1, 2, 3)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "123");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer4() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer4<Object, Object, Object, Object>)f)
        .apply(1, 2, 3, 4)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "1234");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer5() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer5<Object, Object, Object, Object, Object>)f)
        .apply(1, 2, 3, 4, 5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "12345");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer6() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer6<Object, Object, Object, Object, Object, Object>)f)
        .apply(1, 2, 3, 4, 5, 6)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "123456");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer7() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer7<Object, Object, Object, Object, Object, Object, Object>)f)
        .apply(1, 2, 3, 4, 5, 6, 7)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "1234567");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer8() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer8<Object, Object, Object, Object, Object, Object, Object, Object>)f)
        .apply(1, 2, 3, 4, 5, 6, 7, 8)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "12345678");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer9() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer9<Object, Object, Object, Object, Object, Object, Object, Object, Object>)f)
        .apply(1, 2, 3, 4, 5, 6, 7, 8, 9)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "123456789");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumerN() {
        MultiFunction f = new MultiFunction();
        Consumer<Object[]> f2 = f;

        AsyncFlowable.toAsyncArray(f2)
        .apply(new Object[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncCallableFunction0() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsyncCallable((SimpleCallable<Object>)f)
        .call()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("-");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncSupplierFunction0() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsyncSupplier((Supplier<Object>)f)
        .call()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("-");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction1() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function<Object, Object>)f)
        .apply(1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction2() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((BiFunction<Object, Object, Object>)f)
        .apply(1, 2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("12");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction3() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function3<Object, Object, Object, Object>)f)
        .apply(1, 2, 3)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("123");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction4() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function4<Object, Object, Object, Object, Object>)f)
        .apply(1, 2, 3, 4)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1234");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction5() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function5<Object, Object, Object, Object, Object, Object>)f)
        .apply(1, 2, 3, 4, 5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("12345");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction6() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function6<Object, Object, Object, Object, Object, Object, Object>)f)
        .apply(1, 2, 3, 4, 5, 6)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("123456");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction7() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function7<Object, Object, Object, Object, Object, Object, Object, Object>)f)
        .apply(1, 2, 3, 4, 5, 6, 7)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1234567");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction8() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function8<Object, Object, Object, Object, Object, Object, Object, Object, Object>)f)
        .apply(1, 2, 3, 4, 5, 6, 7, 8)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("12345678");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction9() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function9<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>)f)
        .apply(1, 2, 3, 4, 5, 6, 7, 8, 9)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("123456789");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunctionN() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function<Object[], Object>)f)
        .apply(new Object[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]");
    }

    @Test
    public void toAsyncConsumer0Scheduler() {
        final AtomicInteger counter = new AtomicInteger();

        AsyncFlowable.toAsync(new Action() {
            @Override
            public void run() throws Exception {
                counter.getAndIncrement();
            }
        }, Schedulers.single())
        .call()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer1Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer<Object>)f, Schedulers.single())
        .apply(1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "1");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer2Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((BiConsumer<Object, Object>)f, Schedulers.single())
        .apply(1, 2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "12");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer3Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer3<Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2, 3)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "123");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer4Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer4<Object, Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2, 3, 4)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "1234");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer5Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer5<Object, Object, Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2, 3, 4, 5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "12345");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer6Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer6<Object, Object, Object, Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2, 3, 4, 5, 6)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "123456");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer7Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer7<Object, Object, Object, Object, Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2, 3, 4, 5, 6, 7)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "1234567");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer8Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer8<Object, Object, Object, Object, Object, Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2, 3, 4, 5, 6, 7, 8)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "12345678");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumer9Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Consumer9<Object, Object, Object, Object, Object, Object, Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2, 3, 4, 5, 6, 7, 8, 9)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "123456789");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncConsumerNScheduler() {
        MultiFunction f = new MultiFunction();
        Consumer<Object[]> f2 = f;

        AsyncFlowable.toAsyncArray(f2, Schedulers.single())
        .apply(new Object[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(AnyValue.INSTANCE);

        assertEquals(f.getState(), "[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncCallableFunction0Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsyncCallable((SimpleCallable<Object>)f, Schedulers.single())
        .call()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("-");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncSupplierFunction0Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsyncSupplier((Supplier<Object>)f, Schedulers.single())
        .call()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("-");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction1Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function<Object, Object>)f, Schedulers.single())
        .apply(1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction2Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((BiFunction<Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("12");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction3Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function3<Object, Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2, 3)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("123");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction4Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function4<Object, Object, Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2, 3, 4)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1234");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction5Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function5<Object, Object, Object, Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2, 3, 4, 5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("12345");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction6Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function6<Object, Object, Object, Object, Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2, 3, 4, 5, 6)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("123456");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction7Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function7<Object, Object, Object, Object, Object, Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2, 3, 4, 5, 6, 7)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1234567");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction8Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function8<Object, Object, Object, Object, Object, Object, Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2, 3, 4, 5, 6, 7, 8)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("12345678");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunction9Scheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function9<Object, Object, Object, Object, Object, Object, Object, Object, Object, Object>)f, Schedulers.single())
        .apply(1, 2, 3, 4, 5, 6, 7, 8, 9)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("123456789");
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void toAsyncFunctionNScheduler() {
        MultiFunction f = new MultiFunction();

        AsyncFlowable.toAsync((Function<Object[], Object>)f, Schedulers.single())
        .apply(new Object[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]");
    }

    @Test
    public void startFuture() {
        final FutureTask<Integer> ft = new FutureTask<>(Functions.EMPTY_RUNNABLE, 1);
        ft.run();
        AsyncFlowable.startFuture(new Supplier<Future<Integer>>() {
            @Override
            public Future<Integer> get() throws Exception {
                return ft;
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void startFutureNull() {
        final FutureTask<Integer> ft = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
        ft.run();
        AsyncFlowable.startFuture(new Supplier<Future<Integer>>() {
            @Override
            public Future<Integer> get() throws Exception {
                return ft;
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void startFutureCustomScheduler() {
        final FutureTask<Integer> ft = new FutureTask<>(Functions.EMPTY_RUNNABLE, 1);
        ft.run();
        AsyncFlowable.startFuture(new Supplier<Future<Integer>>() {
            @Override
            public Future<Integer> get() throws Exception {
                return ft;
            }
        }, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void deferFuture() {
        final FutureTask<Flowable<Integer>> ft = new FutureTask<>(Functions.EMPTY_RUNNABLE, Flowable.just(1));
        ft.run();

        AsyncFlowable.deferFuture(new Supplier<Future<Flowable<Integer>>>() {
            @Override
            public Future<Flowable<Integer>> get() throws Exception {
                return ft;
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void deferFutureCustomScheduler() {
        final FutureTask<Flowable<Integer>> ft = new FutureTask<>(Functions.EMPTY_RUNNABLE, Flowable.just(1));
        ft.run();

        AsyncFlowable.deferFuture(new Supplier<Future<Flowable<Integer>>>() {
            @Override
            public Future<Flowable<Integer>> get() throws Exception {
                return ft;
            }
        }, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void forEachFutureC1() throws Exception {
        final List<Integer> list = new ArrayList<>();

        assertNull(AsyncFlowable.forEachFuture(Flowable.range(1, 5), new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        })
        .get());

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void forEachFutureC1Error() throws Exception {
        final List<Integer> list = new ArrayList<>();

        try {
            AsyncFlowable.forEachFuture(Flowable.<Integer>error(new IOException()), new Consumer<Integer>() {
                @Override
                public void accept(Integer v) throws Exception {
                    list.add(v);
                }
            })
            .get();

            fail("Should have thrown");
        } catch (ExecutionException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof IOException);
        }

        assertTrue(list.isEmpty());
    }

    @Test
    public void forEachFutureC1C2() throws Exception {
        final List<Integer> list = new ArrayList<>();

        assertNull(AsyncFlowable.forEachFuture(Flowable.range(1, 5), new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                list.add(100);
            }
        })
        .get());

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void forEachFutureC1C2Error() throws Exception {
        final List<Integer> list = new ArrayList<>();
        try {
            AsyncFlowable.forEachFuture(Flowable.<Integer>error(new IOException()), new Consumer<Integer>() {
                @Override
                public void accept(Integer v) throws Exception {
                    list.add(v);
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    list.add(100);
                }
            })
            .get();

            fail("Should have thrown");
        } catch (ExecutionException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof IOException);
        }

        assertEquals(Arrays.asList(100), list);
    }

    @Test
    public void forEachFutureC1C2A3() throws Exception {
        final List<Integer> list = new ArrayList<>();

        assertNull(AsyncFlowable.forEachFuture(Flowable.range(1, 5), new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                list.add(100);
            }
        }, new Action() {
            @Override
            public void run() throws Exception {
                list.add(200);
            }
        })
        .get());

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 200), list);
    }

    @Test
    public void forEachFutureC1C2A3Error() throws Exception {
        final List<Integer> list = new ArrayList<>();
        try {
            AsyncFlowable.forEachFuture(Flowable.<Integer>error(new IOException()), new Consumer<Integer>() {
                @Override
                public void accept(Integer v) throws Exception {
                    list.add(v);
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    list.add(100);
                }
            }, new Action() {
                @Override
                public void run() throws Exception {
                    list.add(200);
                }
            })
            .get();

            fail("Should have thrown");
        } catch (ExecutionException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof IOException);
        }

        assertEquals(Arrays.asList(100), list);
    }

    @Test
    public void forEachFutureC1Scheduler() throws Exception {
        final List<Integer> list = new ArrayList<>();

        assertNull(AsyncFlowable.forEachFuture(Flowable.range(1, 5), new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        }, Schedulers.single())
        .get());

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void forEachFutureC1ErrorScheduler() throws Exception {
        final List<Integer> list = new ArrayList<>();

        try {
            AsyncFlowable.forEachFuture(Flowable.<Integer>error(new IOException()), new Consumer<Integer>() {
                @Override
                public void accept(Integer v) throws Exception {
                    list.add(v);
                }
            }, Schedulers.single())
            .get();

            fail("Should have thrown");
        } catch (ExecutionException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof IOException);
        }

        assertTrue(list.isEmpty());
    }

    @Test
    public void forEachFutureC1C2Scheduler() throws Exception {
        final List<Integer> list = new ArrayList<>();

        assertNull(AsyncFlowable.forEachFuture(Flowable.range(1, 5), new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                list.add(100);
            }
        }, Schedulers.single())
        .get());

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void forEachFutureC1C2ErrorScheduler() throws Exception {
        final List<Integer> list = new ArrayList<>();
        try {
            AsyncFlowable.forEachFuture(Flowable.<Integer>error(new IOException()), new Consumer<Integer>() {
                @Override
                public void accept(Integer v) throws Exception {
                    list.add(v);
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    list.add(100);
                }
            }, Schedulers.single())
            .get();

            fail("Should have thrown");
        } catch (ExecutionException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof IOException);
        }

        assertEquals(Arrays.asList(100), list);
    }

    @Test
    public void forEachFutureC1C2A3Scheduler() throws Exception {
        final List<Integer> list = new ArrayList<>();

        assertNull(AsyncFlowable.forEachFuture(Flowable.range(1, 5), new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                list.add(v);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                list.add(100);
            }
        }, new Action() {
            @Override
            public void run() throws Exception {
                list.add(200);
            }
        }, Schedulers.single())
        .get());

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 200), list);
    }

    @Test
    public void forEachFutureC1C2A3ErrorScheduler() throws Exception {
        final List<Integer> list = new ArrayList<>();
        try {
            AsyncFlowable.forEachFuture(Flowable.<Integer>error(new IOException()), new Consumer<Integer>() {
                @Override
                public void accept(Integer v) throws Exception {
                    list.add(v);
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) throws Exception {
                    list.add(100);
                }
            }, new Action() {
                @Override
                public void run() throws Exception {
                    list.add(200);
                }
            }, Schedulers.single())
            .get();

            fail("Should have thrown");
        } catch (ExecutionException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof IOException);
        }

        assertEquals(Arrays.asList(100), list);
    }

    @Test
    public void runAsync() {

        AsyncFlowable.runAsync(Schedulers.single(), new BiConsumer<Subscriber<? super Object>, Disposable>() {
            @Override
            public void accept(Subscriber<? super Object> s, Disposable d) throws Exception {
                Thread.sleep(200);
                s.onNext(1);
                s.onNext(2);
                s.onNext(3);
                Thread.sleep(200);
                s.onNext(4);
                s.onNext(5);
                s.onComplete();
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void runAsyncProcessor() {
        AsyncFlowable.runAsync(Schedulers.single(),
            UnicastProcessor.<Object>create(),
        new BiConsumer<Subscriber<? super Object>, Disposable>() {
            @Override
            public void accept(Subscriber<? super Object> s, Disposable d) throws Exception {
                s.onNext(1);
                s.onNext(2);
                s.onNext(3);
                Thread.sleep(200);
                s.onNext(4);
                s.onNext(5);
                s.onComplete();
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }
}
