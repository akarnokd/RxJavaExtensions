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

package hu.akarnokd.rxjava3.functions;

import java.util.*;

import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Wraps the regular {@code io.reactivex.functions} function types and appends
 * the user-specified tag to the exception the wrapped function produces.
 * <p>
 * The tagging can be optionally enabled before assembling flows, thus the
 * overhead can be limited to when diagnosing problems around failing functions.
 * <p>
 * Each functional type has its own {code tagX} postfix to avoid lambda ambiguity.
 *
 * @since 0.17.4
 */
public final class FunctionTagging {

    /** Utility class. */
    private FunctionTagging() {
        throw new IllegalStateException("No instances!");
    }

    /** Indicates the wrapping is allowed. */
    static volatile boolean enabled;

    /**
     * Enable the function tagging wrappers in the {@code tagX} methods.
     */
    public static void enable() {
        enabled = true;
    }

    /**
     * Disable the function tagging wrappers in the {@code tagX} methods.
     * <p>
     * Note that disabling doesn't remove the wrappers from previously tagged
     * functions.
     */
    public static void disable() {
        enabled = false;
    }

    /**
     * Returns true if the function tagging wrappers are enabled.
     * @return true if the function tagging wrappers are enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }

    public static <T, R> Function<T, R> tagFunction(Function<T, R> func, String tag) {
        if (enabled) {
            ObjectHelper.requireNonNull(func, "func is null");
            ObjectHelper.requireNonNull(tag, "tag is null");
            return new TagFunction<T, R>(func, tag);
        }
        return func;
    }

    public static <T1, T2, R> BiFunction<T1, T2, R> tagBiFunction(BiFunction<T1, T2, R> func, String tag) {
        if (enabled) {
            ObjectHelper.requireNonNull(func, "func is null");
            ObjectHelper.requireNonNull(tag, "tag is null");
            return new TagBiFunction<T1, T2, R>(func, tag);
        }
        return func;
    }

    public static <T1, T2, T3, R> Function3<T1, T2, T3, R> tagFunction3(Function3<T1, T2, T3, R> func, String tag) {
        if (enabled) {
            ObjectHelper.requireNonNull(func, "func is null");
            ObjectHelper.requireNonNull(tag, "tag is null");
            return new TagFunction3<T1, T2, T3, R>(func, tag);
        }
        return func;
    }

    public static <T1, T2, T3, T4, R> Function4<T1, T2, T3, T4, R> tagFunction4(Function4<T1, T2, T3, T4, R> func, String tag) {
        if (enabled) {
            ObjectHelper.requireNonNull(func, "func is null");
            ObjectHelper.requireNonNull(tag, "tag is null");
            return new TagFunction4<T1, T2, T3, T4, R>(func, tag);
        }
        return func;
    }

    public static <T1, T2, T3, T4, T5, R> Function5<T1, T2, T3, T4, T5, R> tagFunction5(Function5<T1, T2, T3, T4, T5, R> func, String tag) {
        if (enabled) {
            ObjectHelper.requireNonNull(func, "func is null");
            ObjectHelper.requireNonNull(tag, "tag is null");
            return new TagFunction5<T1, T2, T3, T4, T5, R>(func, tag);
        }
        return func;
    }

    public static <T1, T2, T3, T4, T5, T6, R> Function6<T1, T2, T3, T4, T5, T6, R> tagFunction6(Function6<T1, T2, T3, T4, T5, T6, R> func, String tag) {
        if (enabled) {
            ObjectHelper.requireNonNull(func, "func is null");
            ObjectHelper.requireNonNull(tag, "tag is null");
            return new TagFunction6<T1, T2, T3, T4, T5, T6, R>(func, tag);
        }
        return func;
    }

    public static <T1, T2, T3, T4, T5, T6, T7, R> Function7<T1, T2, T3, T4, T5, T6, T7, R> tagFunction7(Function7<T1, T2, T3, T4, T5, T6, T7, R> func, String tag) {
        if (enabled) {
            ObjectHelper.requireNonNull(func, "func is null");
            ObjectHelper.requireNonNull(tag, "tag is null");
            return new TagFunction7<T1, T2, T3, T4, T5, T6, T7, R>(func, tag);
        }
        return func;
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> tagFunction8(Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> func, String tag) {
        if (enabled) {
            ObjectHelper.requireNonNull(func, "func is null");
            ObjectHelper.requireNonNull(tag, "tag is null");
            return new TagFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R>(func, tag);
        }
        return func;
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> tagFunction9(Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> func, String tag) {
        if (enabled) {
            ObjectHelper.requireNonNull(func, "func is null");
            ObjectHelper.requireNonNull(tag, "tag is null");
            return new TagFunction9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R>(func, tag);
        }
        return func;
    }

    /**
     * Return an Exception to be thrown by the caller or sneak out the original Throwable.
     * @param <E> the generic exception type
     * @param error the original throwable error
     * @return the error if it is an instance of Exception
     * @throws E the if the error is not an Exception
     */
    @SuppressWarnings("unchecked")
    static <E extends Throwable> Exception justThrow(Throwable error) throws E {
        if (error instanceof Exception) {
            return (Exception)error;
        }
        throw (E)error;
    }

    /**
     * The stackless tagging exception appended to the chain of causes of
     * the original exception thrown by the wrapped function.
     * <p>
     * This exception doesn't store the actual stacktrace as it only clutters
     * the original stacktrace when printed.
     */
    public static final class FunctionTaggingException extends RuntimeException {

        private static final long serialVersionUID = -8382312975142579020L;

        /**
         * Constructs a FunctionTaggingException with the given message.
         * @param message the message
         */
        public FunctionTaggingException(String message) {
            super(message);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }

        /**
         * Try appending this FunctionTaggingException as the very last cause of
         * the given throwable.
         * @param ex the Throwable to append to
         * @return ex the original Throwable
         */
        public Throwable appendLast(Throwable ex) {
            Throwable r = ex;
            Set<Throwable> memory = new HashSet<Throwable>();
            while (ex.getCause() != null) {
                if (memory.add(ex)) {
                    ex = ex.getCause();
                } else {
                    // didn't work
                    RxJavaPlugins.onError(new CompositeException(r, this));
                    return r;
                }
            }

            try {
                ex.initCause(this);
            } catch (Throwable exc) {
                // didn't work, oh well
                RxJavaPlugins.onError(new CompositeException(r, this));
            }
            return r;
        }
    }

    static final class TagFunction<T, R> implements Function<T, R> {

        final Function<T, R> actual;

        final String tag;

        TagFunction(Function<T, R> actual, String tag) {
            this.actual = actual;
            this.tag = tag;
        }

        @Override
        public R apply(T t) throws Exception {
            if (t == null) {
                throw new NullPointerException("t is null, tag = " + tag);
            }

            R v;

            try {
                v = actual.apply(t);
            } catch (Throwable ex) {
                throw FunctionTagging.<Exception>justThrow(new FunctionTaggingException(tag).appendLast(ex));
            }

            if (v == null) {
                throw new NullPointerException("The Function returned null, tag = " + tag);
            }

            return v;
        }
    }

    static final class TagBiFunction<T1, T2, R> implements BiFunction<T1, T2, R> {

        final BiFunction<T1, T2, R> actual;

        final String tag;

        TagBiFunction(BiFunction<T1, T2, R> actual, String tag) {
            this.actual = actual;
            this.tag = tag;
        }

        @Override
        public R apply(T1 t1, T2 t2) throws Exception {
            if (t1 == null) {
                throw new NullPointerException("t1 is null, tag = " + tag);
            }
            if (t2 == null) {
                throw new NullPointerException("t2 is null, tag = " + tag);
            }

            R v;

            try {
                v = actual.apply(t1, t2);
            } catch (Throwable ex) {
                throw FunctionTagging.<Exception>justThrow(new FunctionTaggingException(tag).appendLast(ex));
            }

            if (v == null) {
                throw new NullPointerException("The BiFunction returned null, tag = " + tag);
            }

            return v;
        }
    }

    static final class TagFunction3<T1, T2, T3, R> implements Function3<T1, T2, T3, R> {

        final Function3<T1, T2, T3, R> actual;

        final String tag;

        TagFunction3(Function3<T1, T2, T3, R> actual, String tag) {
            this.actual = actual;
            this.tag = tag;
        }

        @Override
        public R apply(T1 t1, T2 t2, T3 t3) throws Exception {
            if (t1 == null) {
                throw new NullPointerException("t1 is null, tag = " + tag);
            }
            if (t2 == null) {
                throw new NullPointerException("t2 is null, tag = " + tag);
            }
            if (t3 == null) {
                throw new NullPointerException("t3 is null, tag = " + tag);
            }

            R v;

            try {
                v = actual.apply(t1, t2, t3);
            } catch (Throwable ex) {
                throw FunctionTagging.<Exception>justThrow(new FunctionTaggingException(tag).appendLast(ex));
            }

            if (v == null) {
                throw new NullPointerException("The BiFunction returned null, tag = " + tag);
            }

            return v;
        }
    }

    static final class TagFunction4<T1, T2, T3, T4, R> implements Function4<T1, T2, T3, T4, R> {

        final Function4<T1, T2, T3, T4, R> actual;

        final String tag;

        TagFunction4(Function4<T1, T2, T3, T4, R> actual, String tag) {
            this.actual = actual;
            this.tag = tag;
        }

        @Override
        public R apply(T1 t1, T2 t2, T3 t3, T4 t4) throws Exception {
            if (t1 == null) {
                throw new NullPointerException("t1 is null, tag = " + tag);
            }
            if (t2 == null) {
                throw new NullPointerException("t2 is null, tag = " + tag);
            }
            if (t3 == null) {
                throw new NullPointerException("t3 is null, tag = " + tag);
            }
            if (t4 == null) {
                throw new NullPointerException("t4 is null, tag = " + tag);
            }

            R v;

            try {
                v = actual.apply(t1, t2, t3, t4);
            } catch (Throwable ex) {
                throw FunctionTagging.<Exception>justThrow(new FunctionTaggingException(tag).appendLast(ex));
            }

            if (v == null) {
                throw new NullPointerException("The BiFunction returned null, tag = " + tag);
            }

            return v;
        }
    }

    static final class TagFunction5<T1, T2, T3, T4, T5, R> implements Function5<T1, T2, T3, T4, T5, R> {

        final Function5<T1, T2, T3, T4, T5, R> actual;

        final String tag;

        TagFunction5(Function5<T1, T2, T3, T4, T5, R> actual, String tag) {
            this.actual = actual;
            this.tag = tag;
        }

        @Override
        public R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) throws Exception {
            if (t1 == null) {
                throw new NullPointerException("t1 is null, tag = " + tag);
            }
            if (t2 == null) {
                throw new NullPointerException("t2 is null, tag = " + tag);
            }
            if (t3 == null) {
                throw new NullPointerException("t3 is null, tag = " + tag);
            }
            if (t4 == null) {
                throw new NullPointerException("t4 is null, tag = " + tag);
            }
            if (t5 == null) {
                throw new NullPointerException("t5 is null, tag = " + tag);
            }

            R v;

            try {
                v = actual.apply(t1, t2, t3, t4, t5);
            } catch (Throwable ex) {
                throw FunctionTagging.<Exception>justThrow(new FunctionTaggingException(tag).appendLast(ex));
            }

            if (v == null) {
                throw new NullPointerException("The BiFunction returned null, tag = " + tag);
            }

            return v;
        }
    }

    static final class TagFunction6<T1, T2, T3, T4, T5, T6, R> implements Function6<T1, T2, T3, T4, T5, T6, R> {

        final Function6<T1, T2, T3, T4, T5, T6, R> actual;

        final String tag;

        TagFunction6(Function6<T1, T2, T3, T4, T5, T6, R> actual, String tag) {
            this.actual = actual;
            this.tag = tag;
        }

        @Override
        public R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) throws Exception {
            if (t1 == null) {
                throw new NullPointerException("t1 is null, tag = " + tag);
            }
            if (t2 == null) {
                throw new NullPointerException("t2 is null, tag = " + tag);
            }
            if (t3 == null) {
                throw new NullPointerException("t3 is null, tag = " + tag);
            }
            if (t4 == null) {
                throw new NullPointerException("t4 is null, tag = " + tag);
            }
            if (t5 == null) {
                throw new NullPointerException("t5 is null, tag = " + tag);
            }
            if (t6 == null) {
                throw new NullPointerException("t6 is null, tag = " + tag);
            }

            R v;

            try {
                v = actual.apply(t1, t2, t3, t4, t5, t6);
            } catch (Throwable ex) {
                throw FunctionTagging.<Exception>justThrow(new FunctionTaggingException(tag).appendLast(ex));
            }

            if (v == null) {
                throw new NullPointerException("The BiFunction returned null, tag = " + tag);
            }

            return v;
        }
    }

    static final class TagFunction7<T1, T2, T3, T4, T5, T6, T7, R> implements Function7<T1, T2, T3, T4, T5, T6, T7, R> {

        final Function7<T1, T2, T3, T4, T5, T6, T7, R> actual;

        final String tag;

        TagFunction7(Function7<T1, T2, T3, T4, T5, T6, T7, R> actual, String tag) {
            this.actual = actual;
            this.tag = tag;
        }

        @Override
        public R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) throws Exception {
            if (t1 == null) {
                throw new NullPointerException("t1 is null, tag = " + tag);
            }
            if (t2 == null) {
                throw new NullPointerException("t2 is null, tag = " + tag);
            }
            if (t3 == null) {
                throw new NullPointerException("t3 is null, tag = " + tag);
            }
            if (t4 == null) {
                throw new NullPointerException("t4 is null, tag = " + tag);
            }
            if (t5 == null) {
                throw new NullPointerException("t5 is null, tag = " + tag);
            }
            if (t6 == null) {
                throw new NullPointerException("t6 is null, tag = " + tag);
            }
            if (t7 == null) {
                throw new NullPointerException("t7 is null, tag = " + tag);
            }

            R v;

            try {
                v = actual.apply(t1, t2, t3, t4, t5, t6, t7);
            } catch (Throwable ex) {
                throw FunctionTagging.<Exception>justThrow(new FunctionTaggingException(tag).appendLast(ex));
            }

            if (v == null) {
                throw new NullPointerException("The BiFunction returned null, tag = " + tag);
            }

            return v;
        }
    }

    static final class TagFunction8<T1, T2, T3, T4, T5, T6, T7, T8, R> implements Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> {

        final Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> actual;

        final String tag;

        TagFunction8(Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> actual, String tag) {
            this.actual = actual;
            this.tag = tag;
        }

        @Override
        public R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) throws Exception {
            if (t1 == null) {
                throw new NullPointerException("t1 is null, tag = " + tag);
            }
            if (t2 == null) {
                throw new NullPointerException("t2 is null, tag = " + tag);
            }
            if (t3 == null) {
                throw new NullPointerException("t3 is null, tag = " + tag);
            }
            if (t4 == null) {
                throw new NullPointerException("t4 is null, tag = " + tag);
            }
            if (t5 == null) {
                throw new NullPointerException("t5 is null, tag = " + tag);
            }
            if (t6 == null) {
                throw new NullPointerException("t6 is null, tag = " + tag);
            }
            if (t7 == null) {
                throw new NullPointerException("t7 is null, tag = " + tag);
            }
            if (t8 == null) {
                throw new NullPointerException("t8 is null, tag = " + tag);
            }

            R v;

            try {
                v = actual.apply(t1, t2, t3, t4, t5, t6, t7, t8);
            } catch (Throwable ex) {
                throw FunctionTagging.<Exception>justThrow(new FunctionTaggingException(tag).appendLast(ex));
            }

            if (v == null) {
                throw new NullPointerException("The BiFunction returned null, tag = " + tag);
            }

            return v;
        }
    }

    static final class TagFunction9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> implements Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> {

        final Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> actual;

        final String tag;

        TagFunction9(Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> actual, String tag) {
            this.actual = actual;
            this.tag = tag;
        }

        @Override
        public R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) throws Exception {
            if (t1 == null) {
                throw new NullPointerException("t1 is null, tag = " + tag);
            }
            if (t2 == null) {
                throw new NullPointerException("t2 is null, tag = " + tag);
            }
            if (t3 == null) {
                throw new NullPointerException("t3 is null, tag = " + tag);
            }
            if (t4 == null) {
                throw new NullPointerException("t4 is null, tag = " + tag);
            }
            if (t5 == null) {
                throw new NullPointerException("t5 is null, tag = " + tag);
            }
            if (t6 == null) {
                throw new NullPointerException("t6 is null, tag = " + tag);
            }
            if (t7 == null) {
                throw new NullPointerException("t7 is null, tag = " + tag);
            }
            if (t8 == null) {
                throw new NullPointerException("t8 is null, tag = " + tag);
            }
            if (t9 == null) {
                throw new NullPointerException("t9 is null, tag = " + tag);
            }

            R v;

            try {
                v = actual.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
            } catch (Throwable ex) {
                throw FunctionTagging.<Exception>justThrow(new FunctionTaggingException(tag).appendLast(ex));
            }

            if (v == null) {
                throw new NullPointerException("The BiFunction returned null, tag = " + tag);
            }

            return v;
        }
    }
}
