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

package hu.akarnokd.rxjava3.functions;

import io.reactivex.rxjava3.functions.*;

/**
 * Utility methods to work with functional interfaces of this package.
 */
public final class FunctionsEx {

    /** Utility class. */
    private FunctionsEx() {
        throw new IllegalStateException("No instances!");
    }

    /** Implements many consumer interfaces to no-op. */
    @SuppressWarnings("rawtypes")
    enum EmptyConsumer implements
    Consumer, BiConsumer, Consumer3, Consumer4, Consumer5, Consumer6, Consumer7, Consumer8, Consumer9, Action {
        INSTANCE;

        @Override
        public void accept(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7, Object t8,
                Object t9) throws Exception {
        }

        @Override
        public void accept(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7, Object t8)
                throws Exception {
        }

        @Override
        public void accept(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6, Object t7)
                throws Exception {
        }

        @Override
        public void accept(Object t1, Object t2, Object t3, Object t4, Object t5, Object t6) throws Exception {
        }

        @Override
        public void accept(Object t1, Object t2, Object t3, Object t4, Object t5) throws Exception {
        }

        @Override
        public void accept(Object t1, Object t2, Object t3, Object t4) throws Exception {
        }

        @Override
        public void accept(Object t1, Object t2, Object t3) throws Exception {
        }

        @Override
        public void accept(Object t1, Object t2) throws Exception {
        }

        @Override
        public void accept(Object t) throws Exception {
        }

        @Override
        public void run() throws Throwable {

        }
    }

    /**
     * Returns a Consumer1-9 shared instance that ignores the argument(s) of its {@code accept()} methods.
     * @param <T> the combined types of Consumer 1-9.
     * @return the empty Consumer1-9 shared instance
     */
    @SuppressWarnings("unchecked")
    public static <T extends
    Consumer<Object>
    & BiConsumer<Object, Object>
    & Consumer3<Object, Object, Object>
    & Consumer4<Object, Object, Object, Object>
    & Consumer5<Object, Object, Object, Object, Object>
    & Consumer6<Object, Object, Object, Object, Object, Object>
    & Consumer7<Object, Object, Object, Object, Object, Object, Object>
    & Consumer8<Object, Object, Object, Object, Object, Object, Object, Object>
    & Consumer9<Object, Object, Object, Object, Object, Object, Object, Object, Object>
    & Action>
    T emptyConsumer() {
        return (T)EmptyConsumer.INSTANCE;
    }
}
