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

/**
 * Utility methods for join patterns (more generic way than {@code combineLatest}) via
 * {@link hu.akarnokd.rxjava2.joins.JoinObservable JoinObservable} wrapper and the {@code and/then/when}
 * vocabulary;
 * a port of the
 * <a href="https://github.com/ReactiveX/RxJavaJoins">RxJavaJoins</a>
 * library.
 */
package hu.akarnokd.rxjava2.joins;