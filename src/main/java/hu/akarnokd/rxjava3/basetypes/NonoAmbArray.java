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

package hu.akarnokd.rxjava3.basetypes;

import org.reactivestreams.Subscriber;

import hu.akarnokd.rxjava3.basetypes.NonoAmbIterable.AmbSubscriber;
import io.reactivex.rxjava3.exceptions.Exceptions;

/**
 * Terminate as soon as one of the Mono sources terminates.
 */
final class NonoAmbArray extends Nono {

    final Nono[] sources;

    NonoAmbArray(Nono[] sources) {
        this.sources = sources;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        AmbSubscriber parent = new AmbSubscriber(s);
        s.onSubscribe(parent);

        try {
            for (Nono np : sources) {
                if (parent.get() != 0) {
                    break;
                }
                np.subscribe(parent);
            }
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            parent.onError(ex);
        }
    }
}
