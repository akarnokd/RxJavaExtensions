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

package hu.akarnokd.rxjava3.debug.multihook;

import io.reactivex.functions.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Handles the OnSchedule hooks.
 * <p>
 * Use {@link #enable()} to install the single global RxJavaPlugins handler.
 * @since 0.18.0
 */
public class OnScheduleMultiHandlerManager extends MultiHandlerManager<Function<Runnable, Runnable>>
implements Function<Runnable, Runnable>,
BiConsumer<Runnable[], Function<Runnable, Runnable>> {

    /**
     * Enables this manager by replacing any existing OnSchedule hook in RxJavaPlugins.
     */
    public void enable() {
        RxJavaPlugins.setScheduleHandler(this);
    }

    /**
     * Disables this manager by restoring a {@code null} OnSchedule hook.
     */
    public void disable() {
        RxJavaPlugins.setScheduleHandler(null);
    }

    /**
     * Adds the current non-null OnSchedule hook to this handler and replaces it
     * in RxJavaPlugins with this OnScheduleMultiHandlerManager.
     */
    public void append() {
        @SuppressWarnings("unchecked")
        Function<Runnable, Runnable> existing = (Function<Runnable, Runnable>)RxJavaPlugins.getScheduleHandler();
        if (existing != this) {
            if (existing != null) {
                register(existing);
            }
            RxJavaPlugins.setScheduleHandler(this);
        }
    }

    @Override
    public Runnable apply(Runnable t) throws Exception {
        Runnable[] ref = { t };
        forEach(ref, this);
        return ref[0];
    }

    @Override
    public void accept(Runnable[] t1,
            Function<Runnable, Runnable> t2) throws Throwable {
        t1[0] = t2.apply(t1[0]);
    }
}
