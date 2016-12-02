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

import java.util.concurrent.Callable;

import org.reactivestreams.Subscriber;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.internal.subscriptions.EmptySubscription;

/**
 * Execute an action and complete the sequence or relay it error.
 */
final class NonoFromAction extends Nono implements Callable<Void> {

    final Action action;
    
    NonoFromAction(Action action) {
        this.action = action;
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super Void> s) {
        s.onSubscribe(EmptySubscription.INSTANCE);
        try {
            action.run();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            s.onError(ex);
            return;
        }
        s.onComplete();
    }

    @Override
    public Void call() throws Exception {
        action.run();
        return null;
    }
}
