/*
 * Copyright 2016-2017 David Karnok
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

import io.reactivex.*;

/**
 * Additional operators in the form of {@link ObservableTransformer},
 * use {@link Observable#compose(ObservableTransformer)}
 * to apply the operators to an existing sequence.
 * 
 * @since 0.18.2
 */
public final class ObservableTransformers {

    /**
     * Utility class.
     */
    private ObservableTransformers() {
        throw new IllegalStateException("No instances!");
    }
}
