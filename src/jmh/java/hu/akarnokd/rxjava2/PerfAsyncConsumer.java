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

package hu.akarnokd.rxjava2;

import java.util.concurrent.*;

import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;

public final class PerfAsyncConsumer
extends CountDownLatch
implements Subscriber<Object>, Observer<Object>,
SingleObserver<Object>, MaybeObserver<Object>, CompletableObserver {

    final Blackhole bh;

    PerfAsyncConsumer(Blackhole bh) {
        super(1);
        this.bh = bh;
    }

    @Override
    public void onSuccess(Object t) {
        bh.consume(t);
    }

    @Override
    public void onSubscribe(Disposable d) {
        bh.consume(d);
    }

    @Override
    public void onSubscribe(Subscription s) {
        s.request(Long.MAX_VALUE);
        bh.consume(s);
    }

    @Override
    public void onNext(Object t) {
        bh.consume(t);
    }

    @Override
    public void onError(Throwable t) {
        bh.consume(t);
        countDown();
    }

    @Override
    public void onComplete() {
        bh.consume(true);
        countDown();
    }

    public void await(long count) {
        if (count <= 1000) {
            while (getCount() != 0) { }
        } else {
            try {
                if (!await(10, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Timed out!");
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException("Interrupted", ex);
            }
        }
    }
}
