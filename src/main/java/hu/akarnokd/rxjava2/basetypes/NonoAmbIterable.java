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

import org.reactivestreams.*;

import hu.akarnokd.rxjava2.util.CompositeSubscription;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.subscriptions.BasicIntQueueSubscription;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Terminate as soon as one of the Mono sources terminates.
 */
final class NonoAmbIterable extends Nono {

    final Iterable<? extends Nono> sources;

    NonoAmbIterable(Iterable<? extends Nono> sources) {
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

    static final class AmbSubscriber extends BasicIntQueueSubscription<Void> implements Subscriber<Void> {

        private static final long serialVersionUID = 3576466667528056758L;

        final Subscriber<? super Void> actual;
        
        final CompositeSubscription set;
        
        AmbSubscriber(Subscriber<? super Void> actual) {
            this.actual = actual;
            this.set = new CompositeSubscription();
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            set.add(s);
        }
        
        @Override
        public void onNext(Void t) {
            // not called
        }
        
        @Override
        public void onError(Throwable t) {
            if (compareAndSet(0, 1)) {
                set.cancel();
                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }
        
        @Override
        public void onComplete() {
            if (compareAndSet(0, 1)) {
                set.cancel();
                actual.onComplete();
            }
        }
        
        @Override
        public void request(long n) {
            // no-op
        }
        
        @Override
        public void cancel() {
            if (compareAndSet(0, 1)) {
                set.cancel();
            }
        }
        
        @Override
        public void clear() {
            // no-op
        }
        
        @Override
        public boolean isEmpty() {
            return true;
        }
        
        @Override
        public Void poll() throws Exception {
            return null;
        }
        
        @Override
        public int requestFusion(int mode) {
            return mode & ASYNC;
        }
    }
}
