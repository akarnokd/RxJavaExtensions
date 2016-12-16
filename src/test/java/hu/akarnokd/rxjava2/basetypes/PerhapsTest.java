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

package hu.akarnokd.rxjava2.basetypes;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.*;

import org.junit.Test;

import io.reactivex.internal.functions.Functions;

public class PerhapsTest {

    @Test
    public void fromFuture() {
        FutureTask<Integer> ft = new FutureTask<Integer>(Functions.EMPTY_RUNNABLE, 1);
        ft.run();

        Perhaps.fromFuture(ft)
        .test()
        .assertResult(1);
    }

    @Test
    public void fromFutureNull() {
        FutureTask<Integer> ft = new FutureTask<Integer>(Functions.EMPTY_RUNNABLE, null);
        ft.run();

        Perhaps.fromFuture(ft)
        .test()
        .assertResult();
    }

    @Test
    public void fromFutureTimeout() {
        FutureTask<Integer> ft = new FutureTask<Integer>(Functions.EMPTY_RUNNABLE, 1);

        Perhaps.fromFuture(ft, 1, TimeUnit.MILLISECONDS)
        .test()
        .assertFailure(TimeoutException.class);
    }

    @Test
    public void fromFutureCrash() {
        FutureTask<Integer> ft = new FutureTask<Integer>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                throw new IOException();
            }
        });
        ft.run();

        Perhaps.fromFuture(ft)
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void toFuture() throws Exception {
        assertEquals(1, Perhaps.just(1).toFuture().get().intValue());
    }

    @Test
    public void toFutureEmpty() throws Exception {
        assertNull(Perhaps.empty().toFuture().get());
    }

    @Test(expected = ExecutionException.class)
    public void toFutureError() throws Exception {
        Perhaps.error(new IOException()).toFuture().get();
    }

}
