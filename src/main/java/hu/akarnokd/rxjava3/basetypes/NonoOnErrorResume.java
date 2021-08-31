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

package hu.akarnokd.rxjava3.basetypes;

import org.reactivestreams.*;

import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;

/**
 * If the main source fails, resume with another Nono returned from
 * a function receiving the error.
 */
final class NonoOnErrorResume extends Nono {

    final Nono source;

    final Function<? super Throwable, ? extends Nono> errorHandler;

    NonoOnErrorResume(Nono source, Function<? super Throwable, ? extends Nono> errorHandler) {
        this.source = source;
        this.errorHandler = errorHandler;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        source.subscribe(new OnErrorResumeSubscriber(s, errorHandler));
    }

    static final class OnErrorResumeSubscriber extends BasicRefQueueSubscription<Void, Subscription>
    implements Subscriber<Void> {

        private static final long serialVersionUID = 5344018235737739066L;

        final Subscriber<? super Void> downstream;

        final Function<? super Throwable, ? extends Nono> errorHandler;

        boolean once;

        OnErrorResumeSubscriber(Subscriber<? super Void> downstream,
                Function<? super Throwable, ? extends Nono> errorHandler) {
            this.downstream = downstream;
            this.errorHandler = errorHandler;
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(this);
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.replace(this, s);
            if (!once) {
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(Void t) {
            // never called
        }

        @Override
        public void onError(Throwable t) {
            if (!once) {
                once = true;
                Nono np;

                try {
                    np = errorHandler.apply(t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    downstream.onError(new CompositeException(t, ex));
                    return;
                }

                np.subscribe(this);
            } else {
                downstream.onError(t);
            }
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
