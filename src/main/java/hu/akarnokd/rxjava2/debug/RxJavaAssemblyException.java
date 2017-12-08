/*
 * Copyright 2016-2017 David Karnok
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

package hu.akarnokd.rxjava2.debug;

import java.util.*;

/**
 * Holds onto the assembly stacktrace.
 */
public final class RxJavaAssemblyException extends RuntimeException {

    private static final long serialVersionUID = -6757520270386306081L;

    final String stacktrace;

    public RxJavaAssemblyException() {
        this.stacktrace = buildStackTrace();
    }

    public static String buildStackTrace() {
        StringBuilder b = new StringBuilder();

        StackTraceElement[] es = Thread.currentThread().getStackTrace();

        b.append("RxJavaAssemblyException: assembled\r\n");

        for (StackTraceElement e : es) {
            if (filter(e)) {
                b.append("at ").append(e).append("\r\n");
            }
        }

        return b.toString();
    }

    /**
     * Filters out irrelevant stacktrace entries.
     * @param e the stacktrace element
     * @return true if the element may pass
     */
    private static boolean filter(StackTraceElement e) {
        // ignore bridge methods
        if (e.getLineNumber() == 1) {
            return false;
        }

        String cn = e.getClassName();

        if (cn.contains("java.lang.Thread")) {
            return false;
        }

        // ignore JUnit elements
        if (cn.contains("junit.runner")
                || cn.contains("org.junit.internal")
                || cn.contains("junit4.runner")) {
            return false;
        }

        // ignore reflective accessors
        if (cn.contains("java.lang.reflect")
                || cn.contains("sun.reflect")) {
            return false;
        }

        // ignore RxJavaAssemblyException itself
        if (cn.contains(".RxJavaAssemblyException")) {
            return false;
        }

        // the shims injecting the error
        if (cn.contains("OnAssembly")
                || cn.contains("RxJavaAssemblyTracking")
                || cn.contains("RxJavaPlugins")) {
            return false;
        }

        return true;
    }

    /**
     * Returns the captured and filtered stacktrace.
     * @return the captured and filtered stacktrace
     */
    public String stacktrace() {
        return stacktrace;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // don't store own stacktrace
        // empty stacktrace prevents crashes on some JVMs when `getStackTrace()` is invoked
        setStackTrace(new StackTraceElement[0]);
        return this;
    }

    /**
     * Try appending this RxJavaAssemblyException as the very last cause of
     * the given throwable.
     * @param ex the Throwable to append to
     * @return ex
     */
    public Throwable appendLast(Throwable ex) {
        Throwable r = ex;
        Set<Throwable> memory = new HashSet<Throwable>();
        while (ex.getCause() != null) {
            if (memory.add(ex)) {
                ex = ex.getCause();
            } else {
                // didn't work
                return r;
            }
        }

        try {
            ex.initCause(this);
        } catch (Throwable exc) {
            // didn't work, oh well
        }
        return r;
    }

    /**
     * Tries to locate the RxJavaAssemblyException in the chain of causes of the
     * given Throwable.
     * @param ex the Throwable to start scanning
     * @return the RxJavaAssemblyException found or null
     */
    public static RxJavaAssemblyException find(Throwable ex) {
        Set<Throwable> memory = new HashSet<Throwable>();
        while (ex != null) {
            if (ex instanceof RxJavaAssemblyException) {
                return (RxJavaAssemblyException)ex;
            }

            if (memory.add(ex)) {
                ex = ex.getCause();
            } else {
                return null;
            }
        }
        return null;
    }
}
