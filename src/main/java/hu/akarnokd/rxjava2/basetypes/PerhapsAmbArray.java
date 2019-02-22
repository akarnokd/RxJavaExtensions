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

package hu.akarnokd.rxjava2.basetypes;

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.*;

import hu.akarnokd.rxjava2.util.CompositeSubscription;
import io.reactivex.internal.subscriptions.DeferredScalarSubscription;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Signal the events of the first source that signals.
 *
 * @param <T> the value type
 */
final class PerhapsAmbArray<T> extends Perhaps<T> {

    final Perhaps<? extends T>[] sources;

    PerhapsAmbArray(Perhaps<? extends T>[] sources) {
        this.sources = sources;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        AmbSubscriber<T> parent = new AmbSubscriber<T>(s);
        s.onSubscribe(parent);

        for (Perhaps<? extends T> source : sources) {
            if (source == null) {
                parent.onError(new NullPointerException("One of the sources is null"));
                break;
            }
            source.subscribe(parent);
        }
    }

    static final class AmbSubscriber<T> extends DeferredScalarSubscription<T>
    implements Subscriber<T> {

        private static final long serialVersionUID = -5477345444871880990L;

        final CompositeSubscription set;

        final AtomicBoolean once;

        AmbSubscriber(Subscriber<? super T> downstream) {
            super(downstream);
            this.set = new CompositeSubscription();
            this.once = new AtomicBoolean();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (set.add(s)) {
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (once.compareAndSet(false, true)) {
                set.cancel();

                complete(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (once.compareAndSet(false, true)) {
                set.cancel();

                downstream.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (once.compareAndSet(false, true)) {
                set.cancel();

                downstream.onComplete();
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            set.cancel();
        }
    }
}
