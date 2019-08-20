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

package hu.akarnokd.rxjava3;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Publisher;

import hu.akarnokd.rxjava3.operators.FlowableTransformers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Example benchmark. Run from command line as
 * <br>
 * gradle jmh -Pjmh='FlatMapExPerf'
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class FlatMapExPerf {

    @Param({"1", "1000", "100000"})
    public int count;

    @Param({"0", "1", "10", "100", "1000"})
    public int range;

    Flowable<Integer> standardSync;

    Flowable<Integer> standardAsync;

    Flowable<Integer> observeOnSync;

    Flowable<Integer> observeOnAsync;

    Flowable<Integer> syncSync;

    Flowable<Integer> syncAsync;

    Flowable<Integer> asyncSync;

    Flowable<Integer> asyncAsync;

    Flowable<Integer> syncSyncB;

    Flowable<Integer> syncAsyncB;

    Flowable<Integer> asyncSyncB;

    Flowable<Integer> asyncAsyncB;

    @Setup
    public void setup() {
        Integer[] array = new Integer[count];
        Arrays.fill(array, 777);
        Integer[] inner = new Integer[range];
        Arrays.fill(inner, 888);

        Flowable<Integer> source = Flowable.fromArray(array);
        final Flowable<Integer> finner = Flowable.fromArray(inner);

        Function<Integer, Publisher<Integer>> f1 = new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return finner;
            }
        };

        Function<Integer, Publisher<Integer>> f2 = new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return finner.observeOn(Schedulers.computation());
            }
        };

        standardSync = source.flatMap(f1, 32);

        standardAsync = source.flatMap(f2, 32);

        observeOnSync = source.flatMap(f1, 32).observeOn(Schedulers.single());

        observeOnAsync = source.flatMap(f2, 32).observeOn(Schedulers.single());

        syncSync = source.compose(FlowableTransformers.flatMapSync(f1));

        syncAsync = source.compose(FlowableTransformers.flatMapSync(f2));

        asyncSync = source.compose(FlowableTransformers.flatMapAsync(f1, Schedulers.single()));

        asyncAsync = source.compose(FlowableTransformers.flatMapAsync(f2, Schedulers.single()));

        syncSyncB = source.compose(FlowableTransformers.flatMapSync(f1, false));

        syncAsyncB = source.compose(FlowableTransformers.flatMapSync(f2, false));

        asyncSyncB = source.compose(FlowableTransformers.flatMapAsync(f1, Schedulers.single(), false));

        asyncAsyncB = source.compose(FlowableTransformers.flatMapAsync(f2, Schedulers.single(), false));
}

    @Benchmark
    public void standardSync(Blackhole bh) {
        PerfConsumer c = new PerfConsumer(bh);
        standardSync.subscribe(c);
    }

    @Benchmark
    public void standardAsync(Blackhole bh) {
        PerfAsyncConsumer c = new PerfAsyncConsumer(bh);
        standardAsync.subscribe(c);
        c.await(count * range);
    }

    @Benchmark
    public void observeOnSync(Blackhole bh) {
        PerfAsyncConsumer c = new PerfAsyncConsumer(bh);
        observeOnSync.subscribe(c);
        c.await(count * range);
    }

    @Benchmark
    public void observeOnAsync(Blackhole bh) {
        PerfAsyncConsumer c = new PerfAsyncConsumer(bh);
        observeOnAsync.subscribe(c);
        c.await(count * range);
    }

    @Benchmark
    public void syncSync(Blackhole bh) {
        PerfConsumer c = new PerfConsumer(bh);
        syncSync.subscribe(c);
    }

    @Benchmark
    public void syncAsync(Blackhole bh) {
        PerfAsyncConsumer c = new PerfAsyncConsumer(bh);
        syncAsync.subscribe(c);
        c.await(count * range);
    }

    @Benchmark
    public void asyncSync(Blackhole bh) {
        PerfAsyncConsumer c = new PerfAsyncConsumer(bh);
        asyncSync.subscribe(c);
        c.await(count * range);
    }

    @Benchmark
    public void asyncAsync(Blackhole bh) {
        PerfAsyncConsumer c = new PerfAsyncConsumer(bh);
        asyncAsync.subscribe(c);
        c.await(count * range);
    }

    @Benchmark
    public void syncSyncB(Blackhole bh) {
        PerfConsumer c = new PerfConsumer(bh);
        syncSyncB.subscribe(c);
    }

    @Benchmark
    public void syncAsyncB(Blackhole bh) {
        PerfAsyncConsumer c = new PerfAsyncConsumer(bh);
        syncAsync.subscribe(c);
        c.await(count * range);
    }

    @Benchmark
    public void asyncSyncB(Blackhole bh) {
        PerfAsyncConsumer c = new PerfAsyncConsumer(bh);
        asyncSyncB.subscribe(c);
        c.await(count * range);
    }

    @Benchmark
    public void asyncAsyncB(Blackhole bh) {
        PerfAsyncConsumer c = new PerfAsyncConsumer(bh);
        asyncAsyncB.subscribe(c);
        c.await(count * range);
    }

}