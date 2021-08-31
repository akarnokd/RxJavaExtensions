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

import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;

import hu.akarnokd.rxjava3.test.*;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.BiConsumer;
import io.reactivex.rxjava3.operators.QueueFuseable;
import io.reactivex.rxjava3.processors.*;

public class FlowableMapFilterTest {

    @Test
    public void map() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                e.doNext(t * 2);
            }
        }))
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void take() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                e.doNext(t * 2);
            }
        }))
        .take(3)
        .test()
        .assertResult(2, 4, 6);
    }

    @Test
    public void filter() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                if (t % 2 == 0) {
                    e.doNext(t * 2);
                }
            }
        }))
        .test()
        .assertResult(4, 8);
    }

    @Test
    public void mapAndComplete() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                e.doNext(t * 2);
                e.doComplete();
            }
        }))
        .test()
        .assertResult(2);
    }

    @Test
    public void mapTwice() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                e.doNext(t * 2);
                e.doNext(t * 2);
            }
        }))
        .test()
        .assertFailure(IllegalStateException.class, 2);
    }

    @Test
    public void mapHidden() {
        Flowable.range(1, 5).hide()
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                e.doNext(t * 2);
            }
        }))
        .test()
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void filterHidden() {
        Flowable.range(1, 5).hide()
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                if (t % 2 == 0) {
                    e.doNext(t * 2);
                }
            }
        }))
        .test()
        .assertResult(4, 8);
    }

    @Test
    public void consumerThrows() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                throw new IOException();
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void consumerSignalsError() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                e.doError(new IOException());
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void consumerSignalsErrorCancel() {
        BehaviorProcessor<Integer> pp = BehaviorProcessor.createDefault(1);

        pp
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                e.doError(new IOException());
            }
        }))
        .test()
        .assertFailure(IOException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void consumerThrowsCancel() {
        BehaviorProcessor<Integer> pp = BehaviorProcessor.createDefault(1);

        pp
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                throw new IOException();
            }
        }))
        .test()
        .assertFailure(IOException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void consumerCompletes() {
        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                e.doComplete();
            }
        }))
        .test()
        .assertResult();
    }

    @Test
    public void consumerCompleteCancel() {
        BehaviorProcessor<Integer> pp = BehaviorProcessor.createDefault(1);

        pp
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                e.doComplete();
            }
        }))
        .test()
        .assertResult();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void mapFused() {
        TestSubscriberEx<Integer> ts = TestHelper.fusedSubscriber(QueueFuseable.ANY);

        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                e.doNext(t * 2);
            }
        }))
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.SYNC)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void mapAsyncFused() {
        TestSubscriberEx<Integer> ts = TestHelper.fusedSubscriber(QueueFuseable.ANY);

        UnicastProcessor<Integer> up = UnicastProcessor.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                e.doNext(t * 2);
            }
        }))
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(2, 4, 6, 8, 10);
    }

    @Test
    public void filterFused() {
        TestSubscriberEx<Integer> ts = TestHelper.fusedSubscriber(QueueFuseable.ANY);

        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                if (t % 2 == 0) {
                    e.doNext(t * 2);
                }
            }
        }))
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.SYNC)
        .assertResult(4, 8);
    }

    @Test
    public void consumerThrowsFused() {
        TestSubscriberEx<Integer> ts = TestHelper.fusedSubscriber(QueueFuseable.ANY);

        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                throw new IOException();
            }
        }))
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.SYNC)
        .assertFailure(IOException.class);
    }

    @Test
    public void consumerSignalsErrorFused() {
        TestSubscriberEx<Integer> ts = TestHelper.fusedSubscriber(QueueFuseable.ANY);

        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                e.doError(new IOException());
            }
        }))
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.SYNC)
        .assertFailure(IOException.class);
    }

    @Test
    public void consumerCompleteFused() {
        TestSubscriberEx<Integer> ts = TestHelper.fusedSubscriber(QueueFuseable.ANY);

        Flowable.range(1, 5)
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                e.doComplete();
            }
        }))
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.SYNC)
        .assertResult();
    }

    @Test
    public void error() {
        Flowable.<Integer>error(new IOException())
        .compose(FlowableTransformers.mapFilter(new BiConsumer<Integer, BasicEmitter<Integer>>() {
            @Override
            public void accept(Integer t, BasicEmitter<Integer> e) throws Exception {
                e.doComplete();
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }
}
