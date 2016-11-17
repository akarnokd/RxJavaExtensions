/*
 * Copyright 2016 David Karnok
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

package hu.akarnokd.rxjava2.operators;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.Flowable;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.plugins.RxJavaPlugins;

public class FlowableBufferPredicateTest {

    @SuppressWarnings("unchecked")
    @Test
    public void whileNormal() {
        Flowable.just(1, 2, -1, 3, 4, 5, -1, -1, 6)
        .compose(FlowableTransformers.bufferWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v != -1;
            }
        }))
        .test()
        .assertResult(
                Arrays.asList(1, 2),
                Arrays.asList(-1, 3, 4, 5),
                Arrays.asList(-1),
                Arrays.asList(-1, 6)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void whileNormalHidden() {
        Flowable.just(1, 2, -1, 3, 4, 5, -1, -1, 6).hide()
        .compose(FlowableTransformers.bufferWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v != -1;
            }
        }))
        .test()
        .assertResult(
                Arrays.asList(1, 2),
                Arrays.asList(-1, 3, 4, 5),
                Arrays.asList(-1),
                Arrays.asList(-1, 6)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void whileNormalBackpressured() {
        Flowable.just(1, 2, -1, 3, 4, 5, -1, -1, 6)
        .compose(FlowableTransformers.bufferWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v != -1;
            }
        }))
        .test(0)
        .assertNoValues()
        .requestMore(1)
        .assertValue(Arrays.asList(1, 2))
        .requestMore(2)
        .assertValues(Arrays.asList(1, 2),
                Arrays.asList(-1, 3, 4, 5),
                Arrays.asList(-1))
        .requestMore(1)
        .assertResult(
                Arrays.asList(1, 2),
                Arrays.asList(-1, 3, 4, 5),
                Arrays.asList(-1),
                Arrays.asList(-1, 6)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void untilNormal() {
        Flowable.just(1, 2, -1, 3, 4, 5, -1, -1, 6)
        .compose(FlowableTransformers.bufferUntil(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == -1;
            }
        }))
        .test()
        .assertResult(
                Arrays.asList(1, 2, -1),
                Arrays.asList(3, 4, 5, -1),
                Arrays.asList(-1),
                Arrays.asList(6)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void untilNormalHidden() {
        Flowable.just(1, 2, -1, 3, 4, 5, -1, -1, 6).hide()
        .compose(FlowableTransformers.bufferUntil(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == -1;
            }
        }))
        .test()
        .assertResult(
                Arrays.asList(1, 2, -1),
                Arrays.asList(3, 4, 5, -1),
                Arrays.asList(-1),
                Arrays.asList(6)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void untilNormalBackpressured() {
        Flowable.just(1, 2, -1, 3, 4, 5, -1, -1, 6)
        .compose(FlowableTransformers.bufferUntil(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == -1;
            }
        }))
        .test(0)
        .assertNoValues()
        .requestMore(1)
        .assertValue(Arrays.asList(1, 2, -1))
        .requestMore(2)
        .assertValues(Arrays.asList(1, 2, -1),
                Arrays.asList(3, 4, 5, -1),
                Arrays.asList(-1))
        .requestMore(1)
        .assertResult(
                Arrays.asList(1, 2, -1),
                Arrays.asList(3, 4, 5, -1),
                Arrays.asList(-1),
                Arrays.asList(6)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void emptyWhile() {
        Flowable.<Integer>empty()
        .compose(FlowableTransformers.bufferWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v != -1;
            }
        }))
        .test()
        .assertResult();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void emptyUntil() {
        Flowable.<Integer>empty()
        .compose(FlowableTransformers.bufferUntil(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == -1;
            }
        }))
        .test()
        .assertResult();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorWhile() {
        Flowable.<Integer>error(new IOException())
        .compose(FlowableTransformers.bufferWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v != -1;
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorUntil() {
        Flowable.<Integer>error(new IOException())
        .compose(FlowableTransformers.bufferUntil(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == -1;
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void whileTake() {
        Flowable.just(1, 2, -1, 3, 4, 5, -1, -1, 6)
        .compose(FlowableTransformers.bufferWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v != -1;
            }
        }))
        .take(2)
        .test()
        .assertResult(
                Arrays.asList(1, 2),
                Arrays.asList(-1, 3, 4, 5)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void untilTake() {
        Flowable.just(1, 2, -1, 3, 4, 5, -1, -1, 6)
        .compose(FlowableTransformers.bufferUntil(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == -1;
            }
        }))
        .take(2)
        .test()
        .assertResult(
                Arrays.asList(1, 2, -1),
                Arrays.asList(3, 4, 5, -1)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void predicateCrash() {
        Flowable.just(1, 2, -1, 3, 4, 5, -1, -1, 6)
        .compose(FlowableTransformers.bufferUntil(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                throw new IllegalArgumentException();
            }
        }))
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferSupplierCrash0() {
        Flowable.just(1, 2, -1, 3, 4, 5, -1, -1, 6)
        .compose(FlowableTransformers.bufferUntil(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == -1;
            }
        }, new Callable<List<Integer>>() {
            @Override
            public List<Integer> call() throws Exception {
                throw new IllegalArgumentException();
            }
        }))
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferSupplierCrash1() {
        Flowable.just(1, 2, -1, 3, 4, 5, -1, -1, 6)
        .compose(FlowableTransformers.bufferUntil(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == -1;
            }
        }, new Callable<List<Integer>>() {
            int c;
            @Override
            public List<Integer> call() throws Exception {
                if (c++ == 1) {
                    throw new IllegalArgumentException();
                }
                return new ArrayList<Integer>();
            }
        }))
        .test()
        .assertFailure(IllegalArgumentException.class, Arrays.asList(1, 2, -1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferSupplierCrash2() {
        Flowable.just(1, 2, -1, 3, 4, 5, -1, -1, 6)
        .compose(FlowableTransformers.bufferWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v != -1;
            }
        }, new Callable<List<Integer>>() {
            int c;
            @Override
            public List<Integer> call() throws Exception {
                if (c++ == 1) {
                    throw new IllegalArgumentException();
                }
                return new ArrayList<Integer>();
            }
        }))
        .test()
        .assertFailure(IllegalArgumentException.class, Arrays.asList(1, 2));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void doubleError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onError(new IllegalArgumentException());
                    s.onError(new IOException());
                }
            }
            .compose(FlowableTransformers.bufferWhile(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
                    return v != -1;
                }
            }))
            .test()
            .assertFailure(IllegalArgumentException.class);

            TestHelper.assertError(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
