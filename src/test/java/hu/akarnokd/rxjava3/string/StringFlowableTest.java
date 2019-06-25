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

package hu.akarnokd.rxjava3.string;

import java.io.IOException;

import org.junit.Test;

import hu.akarnokd.rxjava3.test.BaseTest;
import io.reactivex.Flowable;

public class StringFlowableTest extends BaseTest {

    @Test
    public void characters() {
        assertResult(StringFlowable.characters("abcdef"), 97, 98, 99, 100, 101, 102);
    }

    @Test
    public void split1() {
        Flowable.just("ab", ":cd", "e:fgh")
        .compose(StringFlowable.split(":"))
        .test()
        .assertResult("ab", "cde", "fgh");
    }

    @Test
    public void split1Request1() {
        Flowable.just("ab", ":cd", "e:fgh")
        .compose(StringFlowable.split(":"))
        .rebatchRequests(1)
        .test()
        .assertResult("ab", "cde", "fgh");
    }

    @Test
    public void split2() {
        Flowable.just("abcdefgh")
        .compose(StringFlowable.split(":"))
        .test()
        .assertResult("abcdefgh");
    }

    @Test
    public void split2Request1() {
        Flowable.just("abcdefgh")
        .compose(StringFlowable.split(":"))
        .rebatchRequests(1)
        .test()
        .assertResult("abcdefgh");
    }

    @Test
    public void split1Buffer1() {
        Flowable.just("ab", ":cd", "e:fgh")
        .compose(StringFlowable.split(":", 1))
        .test()
        .assertResult("ab", "cde", "fgh");
    }

    @Test
    public void split2Buffer1() {
        Flowable.just("abcdefgh")
        .compose(StringFlowable.split(":", 1))
        .test()
        .assertResult("abcdefgh");
    }

    @Test
    public void splitEmpty() {
        Flowable.<String>empty()
        .compose(StringFlowable.split(":"))
        .test()
        .assertResult();
    }

    @Test
    public void splitError() {
        Flowable.just("abcdefgh").concatWith(Flowable.<String>error(new IOException()))
        .compose(StringFlowable.split(":"))
        .test()
        .assertFailure(IOException.class, "abcdefgh");
    }

    @Test
    public void splitErrorBuffer1() {
        Flowable.just("abcdefgh").concatWith(Flowable.<String>error(new IOException()))
        .compose(StringFlowable.split(":", 1))
        .test()
        .assertFailure(IOException.class, "abcdefgh");
    }

    @Test
    public void splitExample1() {
        Flowable.just("boo:and:foo")
        .compose(StringFlowable.split(":"))
        .test()
        .assertResult("boo", "and", "foo");
    }

    @Test
    public void splitExample1Buffer1() {
        Flowable.just("boo:and:foo")
        .compose(StringFlowable.split(":", 1))
        .test()
        .assertResult("boo", "and", "foo");
    }

    @Test
    public void splitExample1Buffer1Request1() {
        Flowable.just("boo:and:foo")
        .compose(StringFlowable.split(":", 1))
        .rebatchRequests(1)
        .test()
        .assertResult("boo", "and", "foo");
    }

    @Test
    public void splitExample2() {
        Flowable.just("boo:and:foo")
        .compose(StringFlowable.split("o"))
        .test()
        .assertResult("b", "", ":and:f");
    }

    @Test
    public void splitExample2Buffer1() {
        Flowable.just("boo:and:foo")
        .compose(StringFlowable.split("o", 1))
        .test()
        .assertResult("b", "", ":and:f");
    }

    @Test
    public void splitExample2Buffer1Request1() {
        Flowable.just("boo:and:foo")
        .compose(StringFlowable.split("o", 1))
        .rebatchRequests(1)
        .test()
        .assertResult("b", "", ":and:f");
    }

    @Test
    public void split3() {
        Flowable.just("abqw", "ercdqw", "eref")
        .compose(StringFlowable.split("qwer"))
        .test()
        .assertResult("ab", "cd", "ef");
    }

    @Test
    public void split3Buffer1() {
        Flowable.just("abqw", "ercdqw", "eref")
        .compose(StringFlowable.split("qwer", 1))
        .test()
        .assertResult("ab", "cd", "ef");
    }

    @Test
    public void split3Buffer1Request1() {
        Flowable.just("abqw", "ercdqw", "eref")
        .compose(StringFlowable.split("qwer", 1))
        .rebatchRequests(1)
        .test()
        .assertResult("ab", "cd", "ef");
    }

    @Test
    public void split4() {
        Flowable.just("ab", ":", "", "", "c:d", "", "e:")
        .compose(StringFlowable.split(":", 1))
        .test()
        .assertResult("ab", "c", "de");
    }

    @Test
    public void split5() {
        Flowable.just("ab", ":cd:", "ef")
        .compose(StringFlowable.split(":"))
        .test()
        .assertResult("ab", "cd", "ef");
    }

    @Test
    public void split5Buffer1() {
        Flowable.just("ab", ":cd:", "ef")
        .compose(StringFlowable.split(":", 1))
        .test()
        .assertResult("ab", "cd", "ef");
    }

    @Test
    public void split5Buffer1Request1() {
        Flowable.just("ab", ":cd:", "ef")
        .compose(StringFlowable.split(":", 1))
        .rebatchRequests(1)
        .test()
        .assertResult("ab", "cd", "ef");
    }
}
