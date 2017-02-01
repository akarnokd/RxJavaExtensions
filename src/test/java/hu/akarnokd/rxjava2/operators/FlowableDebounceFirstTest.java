package hu.akarnokd.rxjava2.operators;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class FlowableDebounceFirstTest {

    Function<Integer, Publisher<Integer>> delayByItself() {
        return new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(final Integer v) throws Exception {
                return Flowable.timer(v, TimeUnit.MILLISECONDS)
                        .map(new Function<Long, Integer>() {
                    @Override
                    public Integer apply(Long w) throws Exception {
                        return v;
                    }
                });
            }
        };
    }

    @Test
    public void normal() {
        Flowable.just(0, 50, 100, 150, 400, 500, 550, 1000)
        .flatMap(delayByItself())
        .compose(FlowableTransformers.debounceFirst(200, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0, 400, 1000);
    }

    @Test
    public void normalScheduler() {
        Flowable.just(0, 50, 100, 150, 400, 500, 550, 1000)
        .flatMap(delayByItself())
        .compose(FlowableTransformers.debounceFirst(200, TimeUnit.MILLISECONDS, Schedulers.single()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0, 400, 1000);
    }

    @Test
    public void empty() {
        Flowable.<Integer>empty()
        .flatMap(delayByItself())
        .compose(FlowableTransformers.debounceFirst(200, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void error() {
        Flowable.<Integer>error(new IOException())
        .flatMap(delayByItself())
        .compose(FlowableTransformers.debounceFirst(200, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void take() {
        Flowable.just(0, 50, 100, 150, 400, 500, 550, 1000)
        .flatMap(delayByItself())
        .compose(FlowableTransformers.debounceFirst(200, TimeUnit.MILLISECONDS))
        .take(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0, 400);
    }

    @Test
    public void backpressure() {
        Flowable.just(0, 50, 100, 150, 400, 500, 550, 1000)
        .flatMap(delayByItself())
        .compose(FlowableTransformers.debounceFirst(200, TimeUnit.MILLISECONDS))
        .rebatchRequests(1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0, 400, 1000);
    }
}
