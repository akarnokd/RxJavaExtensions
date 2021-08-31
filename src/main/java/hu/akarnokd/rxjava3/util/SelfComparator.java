/*
 * Copyright 2016-present David Karnok
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

package hu.akarnokd.rxjava3.util;

import java.util.Comparator;

/**
 * Comparator that compares Comparables.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public enum SelfComparator implements Comparator<Comparable> {
    INSTANCE;

    public static <T extends Comparable<? super T>> Comparator<T> instance() {
        return (Comparator)INSTANCE;
    }

    @Override
    public int compare(Comparable o1, Comparable o2) {
        return o1.compareTo(o2);
    }
}
