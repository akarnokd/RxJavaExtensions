/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

module hu.akarnokd.rxjava4 {
    exports hu.akarnokd.rxjava4.async;
    exports hu.akarnokd.rxjava4.basetypes;
    exports hu.akarnokd.rxjava4.consumers;
    exports hu.akarnokd.rxjava4.debug;
    exports hu.akarnokd.rxjava4.debug.multihook;
    exports hu.akarnokd.rxjava4.debug.validator;
    exports hu.akarnokd.rxjava4.expr;
    exports hu.akarnokd.rxjava4.functions;
    exports hu.akarnokd.rxjava4.joins;
    exports hu.akarnokd.rxjava4.math;
    exports hu.akarnokd.rxjava4.operators;
    exports hu.akarnokd.rxjava4.parallel;
    exports hu.akarnokd.rxjava4.processors;
    exports hu.akarnokd.rxjava4.schedulers;
    exports hu.akarnokd.rxjava4.string;
    exports hu.akarnokd.rxjava4.subjects;
    exports hu.akarnokd.rxjava4.util;

    requires java.management;
    requires transitive io.reactivex.rxjava4;

 // === Add these for JUnit 6 / Jupiter in tests (Eclipse + modular) ===
    requires static org.junit.jupiter.api;
    requires static org.junit.platform.commons;
}