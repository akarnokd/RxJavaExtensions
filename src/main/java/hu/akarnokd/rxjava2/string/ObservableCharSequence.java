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

package hu.akarnokd.rxjava2.string;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.disposables.*;

public final class ObservableCharSequence extends Observable<Integer> {

    final CharSequence string;
    
    public ObservableCharSequence(CharSequence string) {
        this.string = string;
    }

    @Override
    protected void subscribeActual(Observer<? super Integer> observer) {
        Disposable d = new BooleanDisposable();
        
        observer.onSubscribe(d);
        
        CharSequence s = string;
        int len = s.length();
        
        for (int i = 0; i < len; i++) {
            if (d.isDisposed()) {
                return;
            }
            observer.onNext((int)s.charAt(i));
        }
        if (d.isDisposed()) {
            return;
        }
        observer.onComplete();
    }
    
    static final class BooleanDisposable extends AtomicBoolean implements Disposable {
        /** */
        private static final long serialVersionUID = -4762798297183704664L;

        @Override
        public void dispose() {
            lazySet(true);
        }

        @Override
        public boolean isDisposed() {
            return get();
        }
    }
}
