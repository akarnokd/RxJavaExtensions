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

import java.util.concurrent.*;

import org.reactivestreams.*;

import hu.akarnokd.rxjava2.functions.*;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscribers.LambdaSubscriber;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.*;
import io.reactivex.schedulers.Schedulers;

/**
 * Utility methods to convert functions and actions into asynchronous operations through the Publisher/Subscriber
 * pattern.
 */
public final class AsyncFlowable {

    /** Utility class. */
    private AsyncFlowable() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Invokes the specified function asynchronously and returns an Flowable that emits the result.
     * <p>
     * Note: The function is called immediately and once, not whenever an observer subscribes to the resulting
     * Flowable. Multiple subscriptions to this Flowable observe the same return value.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/start.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned Flowable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code start} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T> the result value type
     * @param func function to run asynchronously
     * @return an Flowable that emits the function's result value, or notifies observers of an exception
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-start">RxJava Wiki: start()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229036.aspx">MSDN: Observable.Start</a>
     */
    public static <T> Flowable<T> start(Callable<? extends T> func) {
        return start(func, Schedulers.computation());
    }

    /**
     * Invokes the specified function asynchronously on the specified Scheduler and returns an Flowable that
     * emits the result.
     * <p>
     * Note: The function is called immediately and once, not whenever an observer subscribes to the resulting
     * Flowable. Multiple subscriptions to this Flowable observe the same return value.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/start.s.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The returned Flowable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code start} works on.</dd>
     * </dl>
     * <p>
     * @param <T> the result value type
     * @param func function to run asynchronously
     * @param scheduler Scheduler to run the function on
     * @return an Flowable that emits the function's result value, or notifies observers of an exception
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-start">RxJava Wiki: start()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211721.aspx">MSDN: Observable.Start</a>
     */
    public static <T> Flowable<T> start(Callable<? extends T> func, Scheduler scheduler) {
        return Flowable.fromCallable(func).subscribeOn(scheduler).subscribeWith(AsyncProcessor.<T>create());
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through a Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param action the action to convert
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229868.aspx">MSDN: Observable.ToAsync</a>
     */
    public static Supplier<Flowable<Object>> toAsync(Action action) {
        return toAsync(action, Schedulers.computation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through a Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result value type
     * @param func the function to convert
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229182.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <R> Supplier<Flowable<R>> toAsync(Callable<? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through a Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> first parameter type of the action
     * @param action the action to convert
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229657.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1> PlainFunction<T1, Flowable<Object>> toAsync(Consumer<? super T1> action) {
        return toAsync(action, Schedulers.computation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through a Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> first parameter type of the action
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229755.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, R> PlainFunction<T1, Flowable<R>> toAsync(Function<? super T1, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through a Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param action the action to convert
     * @return a function that returns a Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211875.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2> PlainBiFunction<T1, T2, Flowable<Object>> toAsync(BiConsumer<? super T1, ? super T2> action) {
        return toAsync(action, Schedulers.computation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through a Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns a Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229851.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, R> PlainBiFunction<T1, T2, Flowable<R>> toAsync(BiFunction<? super T1, ? super T2, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param action the action to convert
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229336.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3> PlainFunction3<T1, T2, T3, Flowable<Object>> toAsync(Consumer3<? super T1, ? super T2, ? super T3> action) {
        return toAsync(action, Schedulers.computation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229450.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, R> PlainFunction3<T1, T2, T3, Flowable<R>> toAsync(Function3<? super T1, ? super T2, ? super T3, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param action the action to convert
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229769.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4> PlainFunction4<T1, T2, T3, T4, Flowable<Object>> toAsync(Consumer4<? super T1, ? super T2, ? super T3, ? super T4> action) {
        return toAsync(action, Schedulers.computation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229911.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, R> PlainFunction4<T1, T2, T3, T4, Flowable<R>> toAsync(Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param action the action to convert
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229577.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5> PlainFunction5<T1, T2, T3, T4, T5, Flowable<Object>> toAsync(Consumer5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5> action) {
        return toAsync(action, Schedulers.computation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229571.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, R> PlainFunction5<T1, T2, T3, T4, T5, Flowable<R>> toAsync(Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param action the action to convert
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211773.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6> PlainFunction6<T1, T2, T3, T4, T5, T6, Flowable<Object>> toAsync(Consumer6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6> action) {
        return toAsync(action, Schedulers.computation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229716.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, R> PlainFunction6<T1, T2, T3, T4, T5, T6, Flowable<R>> toAsync(Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param action the action to convert
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211812.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7> PlainFunction7<T1, T2, T3, T4, T5, T6, T7, Flowable<Object>> toAsync(Consumer7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7> action) {
        return toAsync(action, Schedulers.computation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229773.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, R> PlainFunction7<T1, T2, T3, T4, T5, T6, T7, Flowable<R>> toAsync(Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param action the action to convert
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh228993.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> PlainFunction8<T1, T2, T3, T4, T5, T6, T7, T8, Flowable<Object>> toAsync(Consumer8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8> action) {
        return toAsync(action, Schedulers.computation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229910.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> PlainFunction8<T1, T2, T3, T4, T5, T6, T7, T8, Flowable<R>> toAsync(Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param <T9> the ninth parameter type
     * @param action the action to convert
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211702.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> PlainFunction9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Flowable<Object>> toAsync(Consumer9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9> action) {
        return toAsync(action, Schedulers.computation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsync} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param <T9> the ninth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh212074.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> PlainFunction9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Flowable<R>> toAsync(Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> func) {
        return toAsync(func, Schedulers.computation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsyncArray} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param action the action to convert
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     */
    public static PlainFunction<Object[], Flowable<Object>> toAsyncArray(Consumer<? super Object[]> action) {
        return toAsyncArray(action, Schedulers.computation());
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Callable honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code toAsyncArray} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the result type
     * @param func the function to convert
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     */
    public static <R> PlainFunction<Object[], Flowable<R>> toAsyncArray(Function<? super Object[], ? extends R> func) {
        return toAsyncArray(func, Schedulers.computation());
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through a Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Supplier honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Flowable that executes the {@code action} and emits an Object
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229868.aspx">MSDN: Observable.ToAsync</a>
     */
    public static Supplier<Flowable<Object>> toAsync(final Action action, final Scheduler scheduler) {
        return new Supplier<Flowable<Object>>() {
            @Override
            public Flowable<Object> call() {
                return Flowable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        action.run();
                        return AnyValue.INSTANCE;
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through a Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Supplier honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <R> the result value type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229182.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <R> Supplier<Flowable<R>> toAsync(final Callable<? extends R> func, final Scheduler scheduler) {
        return new Supplier<Flowable<R>>() {
            @Override
            public Flowable<R> call() {
                return Flowable.fromCallable(func).subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through a Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> first parameter type of the action
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229657.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1> PlainFunction<T1, Flowable<Object>> toAsync(final Consumer<? super T1> action, final Scheduler scheduler) {
        return new PlainFunction<T1, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(final T1 t1) {
                return Flowable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        action.accept(t1);
                        return AnyValue.INSTANCE;
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through a Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> first parameter type of the action
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229755.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, R> PlainFunction<T1, Flowable<R>> toAsync(final Function<? super T1, ? extends R> func, final Scheduler scheduler) {
        return new PlainFunction<T1, Flowable<R>>() {
            @Override
            public Flowable<R> apply(final T1 t1) {
                return Flowable.fromCallable(new Callable<R>() {
                    @Override
                    public R call() throws Exception {
                        return func.apply(t1);
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through a Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns a Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211875.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2> PlainBiFunction<T1, T2, Flowable<Object>> toAsync(final BiConsumer<? super T1, ? super T2> action, final Scheduler scheduler) {
        return new PlainBiFunction<T1, T2, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(final T1 t1, final T2 t2) {
                return Flowable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        action.accept(t1, t2);
                        return AnyValue.INSTANCE;
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through a Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns a Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229851.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, R> PlainBiFunction<T1, T2, Flowable<R>> toAsync(final BiFunction<? super T1, ? super T2, ? extends R> func, final Scheduler scheduler) {
        return new PlainBiFunction<T1, T2, Flowable<R>>() {
            @Override
            public Flowable<R> apply(final T1 t1, final T2 t2) {
                return Flowable.fromCallable(new Callable<R>() {
                    @Override
                    public R call() throws Exception {
                        return func.apply(t1, t2);
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229336.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3> PlainFunction3<T1, T2, T3, Flowable<Object>> toAsync(final Consumer3<? super T1, ? super T2, ? super T3> action, final Scheduler scheduler) {
        return new PlainFunction3<T1, T2, T3, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(final T1 t1, final T2 t2, final T3 t3) {
                return Flowable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        action.accept(t1, t2, t3);
                        return AnyValue.INSTANCE;
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229450.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, R> PlainFunction3<T1, T2, T3, Flowable<R>> toAsync(final Function3<? super T1, ? super T2, ? super T3, ? extends R> func, final Scheduler scheduler) {
        return new PlainFunction3<T1, T2, T3, Flowable<R>>() {
            @Override
            public Flowable<R> apply(final T1 t1, final T2 t2, final T3 t3) {
                return Flowable.fromCallable(new Callable<R>() {
                    @Override
                    public R call() throws Exception {
                        return func.apply(t1, t2, t3);
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229769.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4> PlainFunction4<T1, T2, T3, T4, Flowable<Object>> toAsync(final Consumer4<? super T1, ? super T2, ? super T3, ? super T4> action, final Scheduler scheduler) {
        return new PlainFunction4<T1, T2, T3, T4, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(final T1 t1, final T2 t2, final T3 t3, final T4 t4) {
                return Flowable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        action.accept(t1, t2, t3, t4);
                        return AnyValue.INSTANCE;
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229911.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, R> PlainFunction4<T1, T2, T3, T4, Flowable<R>> toAsync(final Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> func, final Scheduler scheduler) {
        return new PlainFunction4<T1, T2, T3, T4, Flowable<R>>() {
            @Override
            public Flowable<R> apply(final T1 t1, final T2 t2, final T3 t3, final T4 t4) {
                return Flowable.fromCallable(new Callable<R>() {
                    @Override
                    public R call() throws Exception {
                        return func.apply(t1, t2, t3, t4);
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229577.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5> PlainFunction5<T1, T2, T3, T4, T5, Flowable<Object>> toAsync(final Consumer5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5> action, final Scheduler scheduler) {
        return new PlainFunction5<T1, T2, T3, T4, T5, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5) {
                return Flowable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        action.accept(t1, t2, t3, t4, t5);
                        return AnyValue.INSTANCE;
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229571.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, R> PlainFunction5<T1, T2, T3, T4, T5, Flowable<R>> toAsync(final Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> func, final Scheduler scheduler) {
        return new PlainFunction5<T1, T2, T3, T4, T5, Flowable<R>>() {
            @Override
            public Flowable<R> apply(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5) {
                return Flowable.fromCallable(new Callable<R>() {
                    @Override
                    public R call() throws Exception {
                        return func.apply(t1, t2, t3, t4, t5);
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211773.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6> PlainFunction6<T1, T2, T3, T4, T5, T6, Flowable<Object>> toAsync(final Consumer6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6> action, final Scheduler scheduler) {
        return new PlainFunction6<T1, T2, T3, T4, T5, T6, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6) {
                return Flowable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        action.accept(t1, t2, t3, t4, t5, t6);
                        return AnyValue.INSTANCE;
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229716.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, R> PlainFunction6<T1, T2, T3, T4, T5, T6, Flowable<R>> toAsync(final Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> func, final Scheduler scheduler) {
        return new PlainFunction6<T1, T2, T3, T4, T5, T6, Flowable<R>>() {
            @Override
            public Flowable<R> apply(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6) {
                return Flowable.fromCallable(new Callable<R>() {
                    @Override
                    public R call() throws Exception {
                        return func.apply(t1, t2, t3, t4, t5, t6);
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211812.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7> PlainFunction7<T1, T2, T3, T4, T5, T6, T7, Flowable<Object>> toAsync(
            final Consumer7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7> action, final Scheduler scheduler) {
        return new PlainFunction7<T1, T2, T3, T4, T5, T6, T7, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7) {
                return Flowable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        action.accept(t1, t2, t3, t4, t5, t6, t7);
                        return AnyValue.INSTANCE;
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229773.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, R> PlainFunction7<T1, T2, T3, T4, T5, T6, T7, Flowable<R>> toAsync(
            final Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> func, final Scheduler scheduler) {
        return new PlainFunction7<T1, T2, T3, T4, T5, T6, T7, Flowable<R>>() {
            @Override
            public Flowable<R> apply(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7) {
                return Flowable.fromCallable(new Callable<R>() {
                    @Override
                    public R call() throws Exception {
                        return func.apply(t1, t2, t3, t4, t5, t6, t7);
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh228993.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> PlainFunction8<T1, T2, T3, T4, T5, T6, T7, T8, Flowable<Object>> toAsync(
            final Consumer8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8> action, final Scheduler scheduler) {
        return new PlainFunction8<T1, T2, T3, T4, T5, T6, T7, T8, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7, final T8 t8) {
                return Flowable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        action.accept(t1, t2, t3, t4, t5, t6, t7, t8);
                        return AnyValue.INSTANCE;
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229910.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> PlainFunction8<T1, T2, T3, T4, T5, T6, T7, T8, Flowable<R>> toAsync(
            final Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> func, final Scheduler scheduler) {
        return new PlainFunction8<T1, T2, T3, T4, T5, T6, T7, T8, Flowable<R>>() {
            @Override
            public Flowable<R> apply(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7, final T8 t8) {
                return Flowable.fromCallable(new Callable<R>() {
                    @Override
                    public R call() throws Exception {
                        return func.apply(t1, t2, t3, t4, t5, t6, t7, t8);
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param <T9> the ninth parameter type
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211702.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> PlainFunction9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Flowable<Object>> toAsync(
            final Consumer9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9> action, final Scheduler scheduler) {
        return new PlainFunction9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7, final T8 t8, final T9 t9) {
                return Flowable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        action.accept(t1, t2, t3, t4, t5, t6, t7, t8, t9);
                        return AnyValue.INSTANCE;
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <T1> the first parameter type
     * @param <T2> the second parameter type
     * @param <T3> the third parameter type
     * @param <T4> the fourth parameter type
     * @param <T5> the fifth parameter type
     * @param <T6> the sixth parameter type
     * @param <T7> the seventh parameter type
     * @param <T8> the eighth parameter type
     * @param <T9> the ninth parameter type
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh212074.aspx">MSDN: Observable.ToAsync</a>
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> PlainFunction9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Flowable<R>> toAsync(
            final Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> func, final Scheduler scheduler) {
        return new PlainFunction9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Flowable<R>>() {
            @Override
            public Flowable<R> apply(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5, final T6 t6, final T7 t7, final T8 t8, final T9 t9) {
                return Flowable.fromCallable(new Callable<R>() {
                    @Override
                    public R call() throws Exception {
                        return func.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous action call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.an.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param action the action to convert
     * @param scheduler the Scheduler used to execute the {@code action}
     * @return a function that returns an Flowable that executes the {@code action} and emits {@code null}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     */
    public static PlainFunction<Object[], Flowable<Object>> toAsyncArray(final Consumer<? super Object[]> action, final Scheduler scheduler) {
        return new PlainFunction<Object[], Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(final Object[] t) {
                return Flowable.fromCallable(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        action.accept(t);
                        return AnyValue.INSTANCE;
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Convert a synchronous function call into an asynchronous function call through an Flowable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/toAsync.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned by the Function honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code toAsync} works on.</dd>
     * </dl>
     * @param <R> the result type
     * @param func the function to convert
     * @param scheduler the Scheduler used to call the {@code func}
     * @return a function that returns an Flowable that executes the {@code func} and emits its returned value
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-toasync-or-asyncaction-or-asyncfunc">RxJava Wiki: toAsync()</a>
     */
    public static <R> PlainFunction<Object[], Flowable<R>> toAsyncArray(final Function<? super Object[], ? extends R> func, final Scheduler scheduler) {
        return new PlainFunction<Object[], Flowable<R>>() {
            @Override
            public Flowable<R> apply(final Object[] t) {
                return Flowable.fromCallable(new Callable<R>() {
                    @Override
                    public R call() throws Exception {
                        return func.apply(t);
                    }
                })
                .subscribeOn(scheduler);
            }
        };
    }

    /**
     * Invokes the asynchronous function immediately, surfacing the result through an Observable.
     * <p>
     * <em>Important note</em> subscribing to the resulting Observable blocks until the future completes.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/startFuture.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code startFuture} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the result type
     * @param functionAsync the asynchronous function to run
     * @return an Observable that surfaces the result of the future
     * @see #startFuture(Callable, Scheduler)
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-startfuture">RxJava Wiki: startFuture()</a>
     */
    public static <T> Flowable<T> startFuture(final Callable<? extends Future<? extends T>> functionAsync) {
        return RxJavaPlugins.onAssembly(new FlowableFromCallableNull<T>(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return functionAsync.call().get();
            }
        }));
    }

    /**
     * Invokes the asynchronous function immediately, surfacing the result through an Observable and waits on
     * the specified Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/startFuture.s.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code startFuture} works on.</dd>
     * </dl>
     * @param <T> the result type
     * @param functionAsync the asynchronous function to run
     * @param scheduler the Scheduler where the completion of the Future is awaited
     * @return an Observable that surfaces the result of the future
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-startfuture">RxJava Wiki: startFuture()</a>
     */
    public static <T> Flowable<T> startFuture(Callable<? extends Future<? extends T>> functionAsync,
        Scheduler scheduler) {
        return startFuture(functionAsync).subscribeOn(scheduler);
    }

    /**
     * Returns an Observable that starts the specified asynchronous factory function whenever a new subscriber
     * subscribes.
     * <p>
     * <em>Important note</em> subscribing to the resulting Observable blocks until the future completes.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/deferFuture.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code deferFuture} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the result type
     * @param publisherFactoryAsync the asynchronous function to start for each observer
     * @return the Observable emitting items produced by the asynchronous observer produced by the factory
     * @see #deferFuture(Callable, Scheduler)
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-deferfuture">RxJava Wiki: deferFuture()</a>
     */
    public static <T> Flowable<T> deferFuture(Callable<? extends Future<? extends Publisher<? extends T>>> publisherFactoryAsync) {
        return deferFuture(publisherFactoryAsync, Schedulers.computation());
    }

    /**
     * Returns an Observable that starts the specified asynchronous factory function whenever a new subscriber
     * subscribes.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/deferFuture.s.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The Flowable returned honors downstream backpressure.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code deferFuture} works on.</dd>
     * </dl>
     * @param <T> the result type
     * @param publisherFactoryAsync the asynchronous function to start for each observer
     * @param scheduler the Scheduler where the completion of the Future is awaited
     * @return the Observable emitting items produced by the asynchronous observer produced by the factory
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-deferfuture">RxJava Wiki: deferFuture()</a>
     */
    public static <T> Flowable<T> deferFuture(
            final Callable<? extends Future<? extends Publisher<? extends T>>> publisherFactoryAsync,
            Scheduler scheduler) {
        return Flowable.defer(new Callable<Publisher<? extends T>>() {
            @Override
            public Publisher<? extends T> call() throws Exception {
                return publisherFactoryAsync.call().get();
            }
        }).subscribeOn(scheduler);
    }

    /**
     * Subscribes to the given source and calls the callback for each emitted item, and surfaces the completion
     * or error through a Future.
     * <p>
     * <em>Important note:</em> The returned task blocks indefinitely unless the {@code run()} method is called
     * or the task is scheduled on an Executor.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/forEachFuture.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The input Publisher is consumed in an unbounded manner.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code forEachFuture} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the source value type
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @return the Future representing the entire for-each operation
     * @see #forEachFuture(Publisher, Consumer, Scheduler)
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-foreachfuture">RxJava Wiki: forEachFuture()</a>
     */
    public static <T> Future<Object> forEachFuture(
            Publisher<? extends T> source,
            Consumer<? super T> onNext) {
        return forEachFuture(source, onNext, Functions.emptyConsumer(), Functions.EMPTY_ACTION, Schedulers.computation());
    }

    /**
     * Subscribes to the given source and calls the callback for each emitted item, and surfaces the completion
     * or error through a Future.
     * <p>
     * <em>Important note:</em> The returned task blocks indefinitely unless the {@code run()} method is called
     * or the task is scheduled on an Executor.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/forEachFuture.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The input Publisher is consumed in an unbounded manner.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code forEachFuture} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the source value type
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @param onError the action to call when an exception is emitted
     * @return the Future representing the entire for-each operation
     * @see #forEachFuture(Publisher, Consumer, Consumer, Scheduler)
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-foreachfuture">RxJava Wiki: forEachFuture()</a>
     */
    public static <T> Future<Object> forEachFuture(
            Publisher<? extends T> source,
            Consumer<? super T> onNext,
            final Consumer<? super Throwable> onError) {
        return forEachFuture(source, onNext, onError, Functions.EMPTY_ACTION, Schedulers.computation());
    }

    /**
     * Subscribes to the given source and calls the callback for each emitted item, and surfaces the completion
     * or error through a Future.
     * <p>
     * <em>Important note:</em> The returned task blocks indefinitely unless the {@code run()} method is called
     * or the task is scheduled on an Executor.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/forEachFuture.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The input Publisher is consumed in an unbounded manner.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code forEachFuture} by default operates on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * @param <T> the source value type
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @param onError the action to call when an exception is emitted
     * @param onComplete the action to call when the source completes
     * @return the Future representing the entire for-each operation
     * @see #forEachFuture(Publisher, Consumer, Consumer, Action, Scheduler)
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-foreachfuture">RxJava Wiki: forEachFuture()</a>
     */
    public static <T> Future<Object> forEachFuture(
            Publisher<? extends T> source,
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Action onComplete) {
        return forEachFuture(source, onNext, onError, onComplete, Schedulers.computation());
    }

    /**
     * Subscribes to the given source and calls the callback for each emitted item, and surfaces the completion
     * or error through a Future, scheduled on the given scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/forEachFuture.s.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The input Publisher is consumed in an unbounded manner.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code forEachFuture} works on.</dd>
     * </dl>
     * @param <T> the source value type
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @param scheduler the Scheduler where the task will await the termination of the for-each
     * @return the Future representing the entire for-each operation
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-foreachfuture">RxJava Wiki: forEachFuture()</a>
     */
    public static <T> Future<Object> forEachFuture(
            Publisher<? extends T> source,
            Consumer<? super T> onNext,
            Scheduler scheduler) {
        return forEachFuture(source, onNext, Functions.emptyConsumer(), Functions.EMPTY_ACTION, scheduler);
    }

    /**
     * Subscribes to the given source and calls the callback for each emitted item, and surfaces the completion
     * or error through a Future, scheduled on the given Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/forEachFuture.s.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The input Publisher is consumed in an unbounded manner.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code forEachFuture} works on.</dd>
     * </dl>
     * @param <T> the source value type
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @param onError the action to call when an exception is emitted
     * @param scheduler the Scheduler where the task will await the termination of the for-each
     * @return the Future representing the entire for-each operation
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-foreachfuture">RxJava Wiki: forEachFuture()</a>
     */
    public static <T> Future<Object> forEachFuture(
            Publisher<? extends T> source,
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Scheduler scheduler) {
        return forEachFuture(source, onNext, onError, Functions.EMPTY_ACTION, scheduler);
    }

    /**
     * Subscribes to the given source and calls the callback for each emitted item, and surfaces the completion
     * or error through a Future, scheduled on the given Scheduler.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/forEachFuture.s.png" alt="">
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The input Publisher is consumed in an unbounded manner.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code forEachFuture} works on.</dd>
     * </dl>
     * @param <T> the source value type
     * @param source the source Observable
     * @param onNext the action to call with each emitted element
     * @param onError the action to call when an exception is emitted
     * @param onComplete the action to call when the source completes
     * @param scheduler the Scheduler where the task will await the termination of the for-each
     * @return the Future representing the entire for-each operation
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-foreachfuture">RxJava Wiki: forEachFuture()</a>
     */
    public static <T> Future<Object> forEachFuture(
            Publisher<? extends T> source,
            Consumer<? super T> onNext,
            final Consumer<? super Throwable> onError,
            final Action onComplete,
            Scheduler scheduler) {

        SequentialDisposable d = new SequentialDisposable();
        final FutureCompletable<Object> f = new FutureCompletable<Object>(d);

        LambdaSubscriber<T> ls = new LambdaSubscriber<T>(onNext,
        new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                try {
                    onError.accept(e);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    f.completeExceptionally(new CompositeException(e, ex));
                    return;
                }
                f.completeExceptionally(e);
            }
        }, new Action() {
            @Override
            public void run() throws Exception {
                try {
                    onComplete.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    f.completeExceptionally(ex);
                    return;
                }
                f.complete(null);
            }
        }, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) throws Exception {
                s.request(Long.MAX_VALUE);
            }
        });
        d.lazySet(ls);

        Flowable.fromPublisher(source).subscribeOn(scheduler).subscribe(ls);

        return f;
    }

    /**
     * Runs the provided action on the given scheduler and allows propagation of multiple events to the
     * observers of the returned DisposableFlowable. The action is immediately executed and unobserved values
     * will be lost.
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The resulting Flowable signals MissingBackpressureException if the downstream can't keep up.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code forEachFuture} works on.</dd>
     * </dl>
     * @param <T> the output value type
     * @param scheduler the Scheduler where the action is executed
     * @param action the action to execute, receives a Subscriber where the events can be pumped and a
     *               Disposable which lets it check for cancellation condition
     * @return an DisposableFlowable that provides a Disposable interface to cancel the action
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-runasync">RxJava Wiki: runAsync()</a>
     */
    public static <T> DisposableFlowable<T> runAsync(Scheduler scheduler,
            final BiConsumer<? super Subscriber<? super T>, ? super Disposable> action) {
        return runAsync(scheduler, PublishProcessor.<T>create(), action);
    }

    /**
     * Runs the provided action on the given scheduler and allows propagation of multiple events to the
     * observers of the returned DisposableFlowable. The action is immediately executed and unobserved values
     * might be lost, depending on the Subject type used.
     *
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The provided Processor's backpressure behavior determines the returned Flowable's backpressure behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>You specify the {@link Scheduler} {@code forEachFuture} works on.</dd>
     * </dl>
     * @param <T> the output value of the action
     * @param <U> the output type of the observable sequence
     * @param scheduler the Scheduler where the action is executed
     * @param processor the subject to use to distribute values emitted by the action
     * @param action the action to execute, receives a Subscriber where the events can be pumped and a
     *               Disposable which lets it check for cancellation condition
     * @return an DisposableFlowable that provides a Disposable interface to cancel the action
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Async-Operators#wiki-runasync">RxJava Wiki: runAsync()</a>
     */
    public static <T, U> DisposableFlowable<U> runAsync(Scheduler scheduler,
            final Processor<T, U> processor,
            final BiConsumer<? super Subscriber<? super T>, ? super Disposable> action) {

        final SequentialDisposable d = new SequentialDisposable();

        d.replace(scheduler.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                try {
                    action.accept(processor, d);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    processor.onError(ex);
                }
            }
        }));

        return new DisposableFlowable<U>() {
            @Override
            protected void subscribeActual(Subscriber<? super U> s) {
                processor.subscribe(s);
            }

            @Override
            public boolean isDisposed() {
                return d.isDisposed();
            }

            @Override
            public void dispose() {
                d.dispose();
            }
        };
    }

}
