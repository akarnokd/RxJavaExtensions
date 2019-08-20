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

package hu.akarnokd.rxjava3.operators;

import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.*;

import hu.akarnokd.rxjava3.test.*;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.MissingBackpressureException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class FlowablePartialCollectTest {

    @Test
    public void simplePassthrough() {
        Flowable.range(1, 1024)
        .compose(FlowableTransformers.partialCollect(new Consumer<PartialCollectEmitter<Integer, Object, Object, Integer>>() {
            @Override
            public void accept(
                    PartialCollectEmitter<Integer, Object, Object, Integer> emitter)
                    throws Exception {
                boolean d = emitter.isComplete();
                boolean empty = emitter.size() == 0;
                if (d && empty) {
                    emitter.complete();
                    return;
                }
                if (empty || emitter.demand() == 0) {
                    return;
                }
                while (emitter.size() != 0 && emitter.demand() != 0) {
                    emitter.next(emitter.getItem(0));
                    emitter.dropItems(1);
                }
            }
        }, Functions.emptyConsumer(), 128))
        .test()
        .assertValueCount(1024)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void simplePassthroughShort() {
        Flowable.range(1, 10)
        .compose(FlowableTransformers.partialCollect(new Consumer<PartialCollectEmitter<Integer, Object, Object, Integer>>() {
            @Override
            public void accept(
                    PartialCollectEmitter<Integer, Object, Object, Integer> emitter)
                    throws Exception {
                boolean d = emitter.isComplete();
                boolean empty = emitter.size() == 0;
                if (d && empty) {
                    emitter.complete();
                    return;
                }
                if (empty || emitter.demand() == 0) {
                    return;
                }
                while (emitter.size() != 0 && emitter.demand() != 0) {
                    emitter.next(emitter.getItem(0));
                    emitter.dropItems(1);
                }
            }
        }, Functions.emptyConsumer(), 128))
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void simpleBackpressured() {
        TestSubscriber<Integer> ts = Flowable.range(1, 1024)
        .compose(FlowableTransformers.partialCollect(new Consumer<PartialCollectEmitter<Integer, Object, Object, Integer>>() {
            @Override
            public void accept(
                    PartialCollectEmitter<Integer, Object, Object, Integer> emitter)
                    throws Exception {
                boolean d = emitter.isComplete();
                boolean empty = emitter.size() == 0;
                if (d && empty) {
                    emitter.complete();
                    return;
                }
                if (empty || emitter.demand() == 0) {
                    return;
                }
                while (emitter.size() != 0 && emitter.demand() != 0) {
                    Integer item = emitter.getItem(0);
                    if (item == 960) {
                        System.out.println(item);
                    }
                    emitter.next(item);
                    emitter.dropItems(1);
                }
            }
        }, Functions.emptyConsumer(), 128))
        .test(0L);

        for (int i = 0; i < 1024; i++) {
            ts.assertValueCount(i)
            .requestMore(1)
            .assertValueCount(i + 1)
            .assertValueAt(i, i + 1)
            .assertNoErrors();
        }

        ts.assertComplete();
    }

    @Test
    public void error() {
        Flowable.<Integer>error(new TestException())
        .compose(FlowableTransformers.partialCollect(new Consumer<PartialCollectEmitter<Integer, Object, Object, Integer>>() {
            @Override
            public void accept(
                    PartialCollectEmitter<Integer, Object, Object, Integer> emitter)
                    throws Exception {
            }
        }, Functions.emptyConsumer(), 128))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> f)
                    throws Exception {
                return f.compose(FlowableTransformers.partialCollect(new Consumer<PartialCollectEmitter<Integer, Object, Object, Integer>>() {
                    @Override
                    public void accept(
                            PartialCollectEmitter<Integer, Object, Object, Integer> emitter)
                            throws Exception {
                    }
                }, Functions.emptyConsumer(), 128));
            }
        });
    }

    @Test
    public void doubleError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onError(new TestException("first"));
                    s.onError(new TestException("second"));
                }
            }
            .compose(FlowableTransformers.partialCollect(new Consumer<PartialCollectEmitter<Integer, Object, Object, Integer>>() {
                @Override
                public void accept(
                        PartialCollectEmitter<Integer, Object, Object, Integer> emitter)
                        throws Exception {
                }
            }, Functions.emptyConsumer(), 128))
            .test()
            .assertFailure(TestException.class)
            .assertError(TestHelper.assertErrorMessage("first"));

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void handlerCrash() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.compose(FlowableTransformers.partialCollect(new Consumer<PartialCollectEmitter<Integer, Object, Object, Integer>>() {
            @Override
            public void accept(
                    PartialCollectEmitter<Integer, Object, Object, Integer> emitter)
                    throws Exception {
                throw new TestException();
            }
        }, Functions.emptyConsumer(), 128))
        .test();

        ts.assertFailure(TestException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void cancel() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.compose(FlowableTransformers.partialCollect(new Consumer<PartialCollectEmitter<Integer, Object, Object, Integer>>() {
            @Override
            public void accept(
                    PartialCollectEmitter<Integer, Object, Object, Integer> emitter)
                    throws Exception {
                if (emitter.isCancelled()) {
                    return;
                }
                if (emitter.size() != 0) {
                    emitter.next(emitter.getItem(0));
                    emitter.dropItems(1);
                }
            }
        }, Functions.emptyConsumer(), 128))
        .take(2)
        .test();

        pp.onNext(1);
        pp.onNext(2);

        ts.assertResult(1, 2);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void handlerMissingBackpressure() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.compose(FlowableTransformers.partialCollect(new Consumer<PartialCollectEmitter<Integer, Object, Object, Integer>>() {
            @Override
            public void accept(
                    PartialCollectEmitter<Integer, Object, Object, Integer> emitter)
                    throws Exception {
                emitter.next(1);
            }
        }, Functions.emptyConsumer(), 128))
        .test(0L);

        pp.onNext(1);

        ts.assertFailure(MissingBackpressureException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void cleanupCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            PublishProcessor<Integer> pp = PublishProcessor.create();

            TestSubscriber<Integer> ts = pp.compose(FlowableTransformers.partialCollect(new Consumer<PartialCollectEmitter<Integer, Object, Object, Integer>>() {
                @Override
                public void accept(
                        PartialCollectEmitter<Integer, Object, Object, Integer> emitter)
                        throws Exception {
                    emitter.dropItems(1);
                }
            }, new Consumer<Integer>() {
                @Override
                public void accept(Integer t) throws Exception {
                    throw new TestException();
                }
            }, 128))
            .test();

            pp.onNext(1);

            ts.assertEmpty();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void stringSplit() {
        Flowable.just("ab|cdef", "gh|ijkl|", "mno||pqr|s", "|", "tuv|xy", "|z")
        .compose(FlowableTransformers.partialCollect(new Consumer<PartialCollectEmitter<String, Integer, StringBuilder, String>>() {
            @Override
            public void accept(
                    PartialCollectEmitter<String, Integer, StringBuilder, String> emitter)
                    throws Exception {
                Integer idx = emitter.getIndex();
                if (idx == null) {
                    idx = 0;
                }
                StringBuilder sb = emitter.getAccumulator();
                if (sb == null) {
                    sb = new StringBuilder();
                    emitter.setAccumulator(sb);
                }

                if (emitter.demand() != 0) {

                    boolean d = emitter.isComplete();
                    if (emitter.size() != 0) {
                        String str = emitter.getItem(0);

                        int j = str.indexOf('|', idx);

                        if (j >= 0) {
                            sb.append(str.substring(idx, j));
                            emitter.next(sb.toString());
                            sb.setLength(0);
                            idx = j + 1;
                        } else {
                            sb.append(str.substring(idx));
                            emitter.dropItems(1);
                            idx = 0;
                        }
                    } else if (d) {
                        if (sb.length() != 0) {
                            emitter.next(sb.toString());
                        }
                        emitter.complete();
                        return;
                    }
                }

                emitter.setIndex(idx);
            }
        }, Functions.emptyConsumer(), 128))
        .test()
        .assertResult(
                "ab",
                "cdefgh",
                "ijkl",
                "mno",
                "",
                "pqr",
                "s",
                "tuv",
                "xy",
                "z"
        );
    }

    @Test
    public void stringSplitBackpressured() {
        Flowable.just("ab|cdef", "gh|ijkl|", "mno||pqr|s", "|", "tuv|xy", "|z")
        .compose(FlowableTransformers.partialCollect(new Consumer<PartialCollectEmitter<String, Integer, StringBuilder, String>>() {
            @Override
            public void accept(
                    PartialCollectEmitter<String, Integer, StringBuilder, String> emitter)
                    throws Exception {
                Integer idx = emitter.getIndex();
                if (idx == null) {
                    idx = 0;
                }
                StringBuilder sb = emitter.getAccumulator();
                if (sb == null) {
                    sb = new StringBuilder();
                    emitter.setAccumulator(sb);
                }

                if (emitter.demand() != 0) {

                    boolean d = emitter.isComplete();
                    if (emitter.size() != 0) {
                        String str = emitter.getItem(0);

                        int j = str.indexOf('|', idx);

                        if (j >= 0) {
                            sb.append(str.substring(idx, j));
                            emitter.next(sb.toString());
                            sb.setLength(0);
                            idx = j + 1;
                        } else {
                            sb.append(str.substring(idx));
                            emitter.dropItems(1);
                            idx = 0;
                        }
                    } else if (d) {
                        if (sb.length() != 0) {
                            emitter.next(sb.toString());
                        }
                        emitter.complete();
                        return;
                    }
                }

                emitter.setIndex(idx);
            }
        }, Functions.emptyConsumer(), 128))
        .rebatchRequests(1)
        .test()
        .assertResult(
                "ab",
                "cdefgh",
                "ijkl",
                "mno",
                "",
                "pqr",
                "s",
                "tuv",
                "xy",
                "z"
        );
    }

    @Test
    public void stringSplitBackpressuredAsync() {
        Flowable.just("ab|cdef", "gh|ijkl|", "mno||pqr|s", "|", "tuv|xy", "|z")
        .compose(FlowableTransformers.partialCollect(new Consumer<PartialCollectEmitter<String, Integer, StringBuilder, String>>() {
            @Override
            public void accept(
                    PartialCollectEmitter<String, Integer, StringBuilder, String> emitter)
                    throws Exception {
                Integer idx = emitter.getIndex();
                if (idx == null) {
                    idx = 0;
                }
                StringBuilder sb = emitter.getAccumulator();
                if (sb == null) {
                    sb = new StringBuilder();
                    emitter.setAccumulator(sb);
                }

                if (emitter.demand() != 0) {

                    boolean d = emitter.isComplete();
                    if (emitter.size() != 0) {
                        String str = emitter.getItem(0);

                        int j = str.indexOf('|', idx);

                        if (j >= 0) {
                            sb.append(str.substring(idx, j));
                            emitter.next(sb.toString());
                            sb.setLength(0);
                            idx = j + 1;
                        } else {
                            sb.append(str.substring(idx));
                            emitter.dropItems(1);
                            idx = 0;
                        }
                    } else if (d) {
                        if (sb.length() != 0) {
                            emitter.next(sb.toString());
                        }
                        emitter.complete();
                        return;
                    }
                }

                emitter.setIndex(idx);
            }
        }, Functions.emptyConsumer(), 128))
        .doOnRequest(new LongConsumer() {
            @Override
            public void accept(long l) throws Exception {
                System.out.println("request(" + l + ")");
            }
        })
        .doOnNext(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                System.out.println("Item: " + s);
            }
        })
        .observeOn(Schedulers.computation(), false, 1)
        .test()
        .awaitDone(10, TimeUnit.SECONDS)
        .assertResult(
                "ab",
                "cdefgh",
                "ijkl",
                "mno",
                "",
                "pqr",
                "s",
                "tuv",
                "xy",
                "z"
        );
    }
}
