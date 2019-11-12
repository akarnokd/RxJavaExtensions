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
 * Proof of concept {@link io.reactivex.rxjava3.plugins.RxJavaPlugins RxJavaPlugins}
 * hook implementation for handling multiple hooks to
 * the {@code onSchedule} callback via
 * {@link hu.akarnokd.rxjava3.debug.multihook.OnScheduleMultiHandlerManager OnScheduleMultiHandlerManager}.
 */
package hu.akarnokd.rxjava3.debug.multihook;