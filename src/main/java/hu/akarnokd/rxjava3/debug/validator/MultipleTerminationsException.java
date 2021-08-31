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

package hu.akarnokd.rxjava3.debug.validator;

/**
 * Indicates there were multiple {@code onError} or {@code onComplete} calls.
 * @since 0.17.4
 */
public final class MultipleTerminationsException extends ProtocolNonConformanceException {

    private static final long serialVersionUID = -6096755460680899745L;

    public MultipleTerminationsException() {
        super();
    }

    public MultipleTerminationsException(Throwable cause) {
        super(cause);
    }

}