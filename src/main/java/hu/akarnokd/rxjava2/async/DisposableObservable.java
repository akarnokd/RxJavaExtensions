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

package hu.akarnokd.rxjava2.async;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;

/**
 * A {@link Observable} that also offers a means to dispose it, cancelling/disposing
 * some shared underlying computation or resource via {@link Disposable#dispose()}.
 *
 * @param <T> the value type of the sequence
 */
public abstract class DisposableObservable<T> extends Observable<T> implements Disposable {

}
