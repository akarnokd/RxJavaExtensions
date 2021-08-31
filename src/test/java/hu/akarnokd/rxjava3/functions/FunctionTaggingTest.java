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

package hu.akarnokd.rxjava3.functions;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import hu.akarnokd.rxjava3.functions.FunctionTagging.FunctionTaggingException;
import hu.akarnokd.rxjava3.test.TestHelper;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public class FunctionTaggingTest {

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(FunctionTagging.class);
    }

    @Test
    public void justThrow() {
        try {
            throw FunctionTagging.<Exception>justThrow(new Throwable());
        } catch (Throwable ex) {
        }
    }

    @Test
    public void addLastWhenCausePresent() {
        IOException ex = new IOException(new NullPointerException());

        new FunctionTaggingException("Tag").appendLast(ex);

        assertTrue(ex.getCause().getCause().toString(), ex.getCause().getCause().toString().contains("Tag"));
    }

    @Test
    public void addLastWhenNullCause() {
        List<Throwable> list = TestHelper.trackPluginErrors();
        try {
            IOException ex = new IOException((Throwable)null);

            new FunctionTaggingException("Tag").appendLast(ex);

            list = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(list, 0, IOException.class);
            TestHelper.assertError(list, 1, FunctionTaggingException.class, "Tag");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void addLastWhenLoopCause() {
        List<Throwable> list = TestHelper.trackPluginErrors();
        try {
            IOException ex = new IOException();
            IllegalArgumentException ex2 = new IllegalArgumentException();
            ex.initCause(ex2);
            ex2.initCause(ex);

            new FunctionTaggingException("Tag").appendLast(ex);

            list = TestHelper.compositeList(list.get(0));
            TestHelper.assertError(list, 0, IOException.class);
            TestHelper.assertError(list, 1, FunctionTaggingException.class, "Tag");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void f1Tag() throws Throwable {
        boolean enabled = FunctionTagging.isEnabled();
        try {
            FunctionTagging.disable();

            Function<Integer, Integer> f = FunctionTagging.tagFunction(Functions.<Integer>identity(), "Custom tag");

            assertEquals((Integer)1, f.apply(1));
            assertNull(f.apply(null));

            FunctionTagging.enable();

            f = FunctionTagging.tagFunction(Functions.<Integer>identity(), "Custom tag");

            assertEquals((Integer)1, f.apply(1));

            try {
                f.apply(null);
                throw new AssertionError("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagFunction(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer a) throws Throwable {
                    return null;
                }
            }, "Custom tag");

            try {
                f.apply(1);
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagFunction(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer a) throws Throwable {
                    throw new IOException();
                }
            }, "Custom tag");

            try {
                f.apply(1);
                fail("Should have thrown!");
            } catch (IOException ex) {
                assertTrue(ex.getCause().getMessage(), ex.getCause().getMessage().contains("Custom tag"));
            }

        } finally {
            if (!enabled) {
                FunctionTagging.disable();
            }
        }
    }

    @Test
    public void f2Tag() throws Throwable {
        boolean enabled = FunctionTagging.isEnabled();
        try {
            FunctionTagging.disable();

            BiFunction<Integer, Integer, Integer> f = FunctionTagging.tagBiFunction(new BiFunction<Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b) throws Throwable {
                    return a;
                }
            }, "Custom tag");

            assertEquals((Integer)1, f.apply(1, 2));
            assertNull(f.apply(null, 2));

            f = FunctionTagging.tagBiFunction(new BiFunction<Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b) throws Throwable {
                    return b;
                }
            }, "Custom tag");

            assertEquals((Integer)2, f.apply(1, 2));
            assertNull(f.apply(1, null));

            // -----------------------------------------------------------------------------

            FunctionTagging.enable();

            f = FunctionTagging.tagBiFunction(new BiFunction<Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b) throws Throwable {
                    return a;
                }
            }, "Custom tag");

            assertEquals((Integer)1, f.apply(1, 2));

            try {
                f.apply(null, 2);
                throw new AssertionError("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagBiFunction(new BiFunction<Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b) throws Throwable {
                    return b;
                }
            }, "Custom tag");

            assertEquals((Integer)2, f.apply(1, 2));

            try {
                f.apply(1, null);
                throw new AssertionError("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            // oooooooooooooooooooooooooooooooo

            f = FunctionTagging.tagBiFunction(new BiFunction<Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b) throws Throwable {
                    return null;
                }
            }, "Custom tag");

            try {
                f.apply(1, 2);
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagBiFunction(new BiFunction<Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b) throws Throwable {
                    throw new IOException();
                }
            }, "Custom tag");

            try {
                f.apply(1, 2);
                fail("Should have thrown!");
            } catch (IOException ex) {
                assertTrue(ex.getCause().getMessage(), ex.getCause().getMessage().contains("Custom tag"));
            }

        } finally {
            if (!enabled) {
                FunctionTagging.disable();
            }
        }
    }

    @Test
    public void f3Tag() throws Throwable {
        boolean enabled = FunctionTagging.isEnabled();
        try {
            FunctionTagging.disable();

            Function3<Integer, Integer, Integer, Integer> f = FunctionTagging.tagFunction3(new Function3<Integer, Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b, Integer c) throws Throwable {
                    return a;
                }
            }, "Custom tag");

            assertEquals((Integer)1, f.apply(1, 2, 3));
            assertNull(f.apply(null, 2, 3));

            f = FunctionTagging.tagFunction3(new Function3<Integer, Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b, Integer c) throws Throwable {
                    return b;
                }
            }, "Custom tag");

            assertEquals((Integer)2, f.apply(1, 2, 3));
            assertNull(f.apply(1, null, 3));

            f = FunctionTagging.tagFunction3(new Function3<Integer, Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b, Integer c) throws Throwable {
                    return c;
                }
            }, "Custom tag");

            assertEquals((Integer)3, f.apply(1, 2, 3));
            assertNull(f.apply(1, 2, null));

            // -----------------------------------------------------------------------------

            FunctionTagging.enable();

            f = FunctionTagging.tagFunction3(new Function3<Integer, Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b, Integer c) throws Throwable {
                    return a;
                }
            }, "Custom tag");

            assertEquals((Integer)1, f.apply(1, 2, 3));

            try {
                f.apply(null, 2, 3);
                throw new AssertionError("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagFunction3(new Function3<Integer, Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b, Integer c) throws Throwable {
                    return b;
                }
            }, "Custom tag");

            assertEquals((Integer)2, f.apply(1, 2, 3));

            try {
                f.apply(1, null, 3);
                throw new AssertionError("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagFunction3(new Function3<Integer, Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b, Integer c) throws Throwable {
                    return c;
                }
            }, "Custom tag");

            assertEquals((Integer)3, f.apply(1, 2, 3));

            try {
                f.apply(1, 2, null);
                throw new AssertionError("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            // oooooooooooooooooooooooooooooooo

            f = FunctionTagging.tagFunction3(new Function3<Integer, Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b, Integer c) throws Throwable {
                    return null;
                }
            }, "Custom tag");

            try {
                f.apply(1, 2, 3);
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagFunction3(new Function3<Integer, Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b, Integer c) throws Throwable {
                    throw new IOException();
                }
            }, "Custom tag");

            try {
                f.apply(1, 2, 3);
                fail("Should have thrown!");
            } catch (IOException ex) {
                assertTrue(ex.getCause().getMessage(), ex.getCause().getMessage().contains("Custom tag"));
            }

        } finally {
            if (!enabled) {
                FunctionTagging.disable();
            }
        }
    }

    @Test
    public void f4Tag() throws Throwable {
        boolean enabled = FunctionTagging.isEnabled();
        try {
            FunctionTagging.disable();

            Function4<Integer, Integer, Integer, Integer, Integer> f = FunctionTagging.tagFunction4(
            new Function4<Integer, Integer, Integer, Integer, Integer>() {
                @Override
                public Integer apply(Integer a, Integer b, Integer c, Integer d) throws Throwable {
                    return a;
                }
            }, "Custom tag");

            assertEquals((Integer)1, f.apply(1, 2, 3, 4));
            assertNull(f.apply(null, 2, 3, 4));

            f = FunctionTagging.tagFunction4(
                    new Function4<Integer, Integer, Integer, Integer, Integer>() {
                        @Override
                        public Integer apply(Integer a, Integer b, Integer c, Integer d) throws Throwable {
                            return b;
                        }
                    }, "Custom tag");

            assertEquals((Integer)2, f.apply(1, 2, 3, 4));
            assertNull(f.apply(1, null, 3, 4));

            f = FunctionTagging.tagFunction4(
                    new Function4<Integer, Integer, Integer, Integer, Integer>() {
                        @Override
                        public Integer apply(Integer a, Integer b, Integer c, Integer d) throws Throwable {
                            return c;
                        }
                    }, "Custom tag");

            assertEquals((Integer)3, f.apply(1, 2, 3, 4));
            assertNull(f.apply(1, 2, null, 4));

            f = FunctionTagging.tagFunction4(
                    new Function4<Integer, Integer, Integer, Integer, Integer>() {
                        @Override
                        public Integer apply(Integer a, Integer b, Integer c, Integer d) throws Throwable {
                            return d;
                        }
                    }, "Custom tag");

            assertEquals((Integer)4, f.apply(1, 2, 3, 4));
            assertNull(f.apply(1, 2, 3, null));

            // -----------------------------------------------------------------------------

            FunctionTagging.enable();

            f = FunctionTagging.tagFunction4(
                    new Function4<Integer, Integer, Integer, Integer, Integer>() {
                        @Override
                        public Integer apply(Integer a, Integer b, Integer c, Integer d) throws Throwable {
                            return a;
                        }
                    }, "Custom tag");

            assertEquals((Integer)1, f.apply(1, 2, 3, 4));

            try {
                f.apply(null, 2, 3, 4);
                throw new AssertionError("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagFunction4(
                    new Function4<Integer, Integer, Integer, Integer, Integer>() {
                        @Override
                        public Integer apply(Integer a, Integer b, Integer c, Integer d) throws Throwable {
                            return b;
                        }
                    }, "Custom tag");

            assertEquals((Integer)2, f.apply(1, 2, 3, 4));

            try {
                f.apply(1, null, 3, 4);
                throw new AssertionError("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagFunction4(
                    new Function4<Integer, Integer, Integer, Integer, Integer>() {
                        @Override
                        public Integer apply(Integer a, Integer b, Integer c, Integer d) throws Throwable {
                            return c;
                        }
                    }, "Custom tag");

            assertEquals((Integer)3, f.apply(1, 2, 3, 4));

            try {
                f.apply(1, 2, null, 4);
                throw new AssertionError("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagFunction4(
                    new Function4<Integer, Integer, Integer, Integer, Integer>() {
                        @Override
                        public Integer apply(Integer a, Integer b, Integer c, Integer d) throws Throwable {
                            return d;
                        }
                    }, "Custom tag");

            assertEquals((Integer)4, f.apply(1, 2, 3, 4));

            try {
                f.apply(1, 2, 3, null);
                throw new AssertionError("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            // oooooooooooooooooooooooooooooooo

            f = FunctionTagging.tagFunction4(
                    new Function4<Integer, Integer, Integer, Integer, Integer>() {
                        @Override
                        public Integer apply(Integer a, Integer b, Integer c, Integer d) throws Throwable {
                            return null;
                        }
                    }, "Custom tag");

            try {
                f.apply(1, 2, 3, 4);
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagFunction4(
                    new Function4<Integer, Integer, Integer, Integer, Integer>() {
                        @Override
                        public Integer apply(Integer a, Integer b, Integer c, Integer d) throws Throwable {
                    throw new IOException();
                }
            }, "Custom tag");

            try {
                f.apply(1, 2, 3, 4);
                fail("Should have thrown!");
            } catch (IOException ex) {
                assertTrue(ex.getCause().getMessage(), ex.getCause().getMessage().contains("Custom tag"));
            }

        } finally {
            if (!enabled) {
                FunctionTagging.disable();
            }
        }
    }

    @Test
    public void f5Tag() throws Throwable {
        boolean enabled = FunctionTagging.isEnabled();
        try {
            FunctionTagging.disable();

            for (int i = 1; i <= 5; i++) {
                Function5<Integer, Integer, Integer, Integer, Integer, Integer> f = FunctionTagging.tagFunction5(
                        new FunctionTaggingComposite(i, false, false), "Custom tag");

                Integer t1 = 1;
                Integer t2 = 2;
                Integer t3 = 3;
                Integer t4 = 4;
                Integer t5 = 5;

                assertEquals((Integer)i, f.apply(t1, t2, t3, t4, t5));

                if (i == 1) { t1 = null; }
                if (i == 2) { t2 = null; }
                if (i == 3) { t3 = null; }
                if (i == 4) { t4 = null; }
                if (i == 5) { t5 = null; }

                assertNull(f.apply(t1, t2, t3, t4, t5));
            }

            // -----------------------------------------------------------------------------

            FunctionTagging.enable();

            for (int i = 1; i <= 5; i++) {
                Function5<Integer, Integer, Integer, Integer, Integer, Integer> f = FunctionTagging.tagFunction5(
                        new FunctionTaggingComposite(i, false, false), "Custom tag");

                Integer t1 = 1;
                Integer t2 = 2;
                Integer t3 = 3;
                Integer t4 = 4;
                Integer t5 = 5;

                assertEquals((Integer)i, f.apply(t1, t2, t3, t4, t5));

                if (i == 1) { t1 = null; }
                if (i == 2) { t2 = null; }
                if (i == 3) { t3 = null; }
                if (i == 4) { t4 = null; }
                if (i == 5) { t5 = null; }

                try {
                    f.apply(t1, t2, t3, t4, t5);
                    throw new AssertionError("Should have thrown!");
                } catch (NullPointerException ex) {
                    assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
                    assertTrue(ex.getMessage(), ex.getMessage().contains("t" + i + " is null"));
                }
            }

            // oooooooooooooooooooooooooooooooo

            Function5<Integer, Integer, Integer, Integer, Integer, Integer> f = FunctionTagging.tagFunction5(
                    new FunctionTaggingComposite(0, true, false), "Custom tag");

            try {
                f.apply(1, 2, 3, 4, 5);
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagFunction5(
                    new FunctionTaggingComposite(0, false, true), "Custom tag");

            try {
                f.apply(1, 2, 3, 4, 5);
                fail("Should have thrown!");
            } catch (IOException ex) {
                assertTrue(ex.getCause().getMessage(), ex.getCause().getMessage().contains("Custom tag"));
            }

        } finally {
            if (!enabled) {
                FunctionTagging.disable();
            }
        }
    }

    @Test
    public void f6Tag() throws Throwable {
        boolean enabled = FunctionTagging.isEnabled();
        try {
            Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer> f;
            int k = 6;

            FunctionTagging.disable();

            for (int i = 1; i <= k; i++) {
                f = FunctionTagging.tagFunction6(
                        new FunctionTaggingComposite(i, false, false), "Custom tag");

                Integer t1 = 1;
                Integer t2 = 2;
                Integer t3 = 3;
                Integer t4 = 4;
                Integer t5 = 5;
                Integer t6 = 6;

                assertEquals((Integer)i, f.apply(t1, t2, t3, t4, t5, t6));

                if (i == 1) { t1 = null; }
                if (i == 2) { t2 = null; }
                if (i == 3) { t3 = null; }
                if (i == 4) { t4 = null; }
                if (i == 5) { t5 = null; }
                if (i == 6) { t6 = null; }

                assertNull(f.apply(t1, t2, t3, t4, t5, t6));
            }

            // -----------------------------------------------------------------------------

            FunctionTagging.enable();

            for (int i = 1; i <= k; i++) {
                f = FunctionTagging.tagFunction6(
                        new FunctionTaggingComposite(i, false, false), "Custom tag");

                Integer t1 = 1;
                Integer t2 = 2;
                Integer t3 = 3;
                Integer t4 = 4;
                Integer t5 = 5;
                Integer t6 = 6;

                assertEquals((Integer)i, f.apply(t1, t2, t3, t4, t5, t6));

                if (i == 1) { t1 = null; }
                if (i == 2) { t2 = null; }
                if (i == 3) { t3 = null; }
                if (i == 4) { t4 = null; }
                if (i == 5) { t5 = null; }
                if (i == 6) { t6 = null; }

                try {
                    f.apply(t1, t2, t3, t4, t5, t6);
                    throw new AssertionError("Should have thrown!");
                } catch (NullPointerException ex) {
                    assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
                    assertTrue(ex.getMessage(), ex.getMessage().contains("t" + i + " is null"));
                }
            }

            // oooooooooooooooooooooooooooooooo

            f = FunctionTagging.tagFunction6(
                    new FunctionTaggingComposite(0, true, false), "Custom tag");

            Integer t1 = 1;
            Integer t2 = 2;
            Integer t3 = 3;
            Integer t4 = 4;
            Integer t5 = 5;
            Integer t6 = 6;

            try {
                f.apply(t1, t2, t3, t4, t5, t6);
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagFunction6(
                    new FunctionTaggingComposite(0, false, true), "Custom tag");

            try {
                f.apply(t1, t2, t3, t4, t5, t6);
                fail("Should have thrown!");
            } catch (IOException ex) {
                assertTrue(ex.getCause().getMessage(), ex.getCause().getMessage().contains("Custom tag"));
            }

        } finally {
            if (!enabled) {
                FunctionTagging.disable();
            }
        }
    }

    @Test
    public void f7Tag() throws Throwable {
        boolean enabled = FunctionTagging.isEnabled();
        try {
            Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> f;
            int k = 7;

            FunctionTagging.disable();

            for (int i = 1; i <= k; i++) {
                f = FunctionTagging.tagFunction7(
                        new FunctionTaggingComposite(i, false, false), "Custom tag");

                Integer t1 = 1;
                Integer t2 = 2;
                Integer t3 = 3;
                Integer t4 = 4;
                Integer t5 = 5;
                Integer t6 = 6;
                Integer t7 = 7;

                assertEquals((Integer)i, f.apply(t1, t2, t3, t4, t5, t6, t7));

                if (i == 1) { t1 = null; }
                if (i == 2) { t2 = null; }
                if (i == 3) { t3 = null; }
                if (i == 4) { t4 = null; }
                if (i == 5) { t5 = null; }
                if (i == 6) { t6 = null; }
                if (i == 7) { t7 = null; }

                assertNull(f.apply(t1, t2, t3, t4, t5, t6, t7));
            }

            // -----------------------------------------------------------------------------

            FunctionTagging.enable();

            for (int i = 1; i <= k; i++) {
                f = FunctionTagging.tagFunction7(
                        new FunctionTaggingComposite(i, false, false), "Custom tag");

                Integer t1 = 1;
                Integer t2 = 2;
                Integer t3 = 3;
                Integer t4 = 4;
                Integer t5 = 5;
                Integer t6 = 6;
                Integer t7 = 7;

                assertEquals((Integer)i, f.apply(t1, t2, t3, t4, t5, t6, t7));

                if (i == 1) { t1 = null; }
                if (i == 2) { t2 = null; }
                if (i == 3) { t3 = null; }
                if (i == 4) { t4 = null; }
                if (i == 5) { t5 = null; }
                if (i == 6) { t6 = null; }
                if (i == 7) { t7 = null; }

                try {
                    f.apply(t1, t2, t3, t4, t5, t6, t7);
                    throw new AssertionError("Should have thrown!");
                } catch (NullPointerException ex) {
                    assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
                    assertTrue(ex.getMessage(), ex.getMessage().contains("t" + i + " is null"));
                }
            }

            // oooooooooooooooooooooooooooooooo

            f = FunctionTagging.tagFunction7(
                    new FunctionTaggingComposite(0, true, false), "Custom tag");

            Integer t1 = 1;
            Integer t2 = 2;
            Integer t3 = 3;
            Integer t4 = 4;
            Integer t5 = 5;
            Integer t6 = 6;
            Integer t7 = 7;

            try {
                f.apply(t1, t2, t3, t4, t5, t6, t7);
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagFunction7(
                    new FunctionTaggingComposite(0, false, true), "Custom tag");

            try {
                f.apply(t1, t2, t3, t4, t5, t6, t7);
                fail("Should have thrown!");
            } catch (IOException ex) {
                assertTrue(ex.getCause().getMessage(), ex.getCause().getMessage().contains("Custom tag"));
            }

        } finally {
            if (!enabled) {
                FunctionTagging.disable();
            }
        }
    }

    @Test
    public void f8Tag() throws Throwable {
        boolean enabled = FunctionTagging.isEnabled();
        try {
            Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> f;
            int k = 8;

            FunctionTagging.disable();

            for (int i = 1; i <= k; i++) {
                f = FunctionTagging.tagFunction8(
                        new FunctionTaggingComposite(i, false, false), "Custom tag");

                Integer t1 = 1;
                Integer t2 = 2;
                Integer t3 = 3;
                Integer t4 = 4;
                Integer t5 = 5;
                Integer t6 = 6;
                Integer t7 = 7;
                Integer t8 = 8;

                assertEquals((Integer)i, f.apply(t1, t2, t3, t4, t5, t6, t7, t8));

                if (i == 1) { t1 = null; }
                if (i == 2) { t2 = null; }
                if (i == 3) { t3 = null; }
                if (i == 4) { t4 = null; }
                if (i == 5) { t5 = null; }
                if (i == 6) { t6 = null; }
                if (i == 7) { t7 = null; }
                if (i == 8) { t8 = null; }

                assertNull(f.apply(t1, t2, t3, t4, t5, t6, t7, t8));
            }

            // -----------------------------------------------------------------------------

            FunctionTagging.enable();

            for (int i = 1; i <= k; i++) {
                f = FunctionTagging.tagFunction8(
                        new FunctionTaggingComposite(i, false, false), "Custom tag");

                Integer t1 = 1;
                Integer t2 = 2;
                Integer t3 = 3;
                Integer t4 = 4;
                Integer t5 = 5;
                Integer t6 = 6;
                Integer t7 = 7;
                Integer t8 = 8;

                assertEquals((Integer)i, f.apply(t1, t2, t3, t4, t5, t6, t7, t8));

                if (i == 1) { t1 = null; }
                if (i == 2) { t2 = null; }
                if (i == 3) { t3 = null; }
                if (i == 4) { t4 = null; }
                if (i == 5) { t5 = null; }
                if (i == 6) { t6 = null; }
                if (i == 7) { t7 = null; }
                if (i == 8) { t8 = null; }

                try {
                    f.apply(t1, t2, t3, t4, t5, t6, t7, t8);
                    throw new AssertionError("Should have thrown!");
                } catch (NullPointerException ex) {
                    assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
                    assertTrue(ex.getMessage(), ex.getMessage().contains("t" + i + " is null"));
                }
            }

            // oooooooooooooooooooooooooooooooo

            f = FunctionTagging.tagFunction8(
                    new FunctionTaggingComposite(0, true, false), "Custom tag");

            Integer t1 = 1;
            Integer t2 = 2;
            Integer t3 = 3;
            Integer t4 = 4;
            Integer t5 = 5;
            Integer t6 = 6;
            Integer t7 = 7;
            Integer t8 = 8;

            try {
                f.apply(t1, t2, t3, t4, t5, t6, t7, t8);
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagFunction8(
                    new FunctionTaggingComposite(0, false, true), "Custom tag");

            try {
                f.apply(t1, t2, t3, t4, t5, t6, t7, t8);
                fail("Should have thrown!");
            } catch (IOException ex) {
                assertTrue(ex.getCause().getMessage(), ex.getCause().getMessage().contains("Custom tag"));
            }

        } finally {
            if (!enabled) {
                FunctionTagging.disable();
            }
        }
    }

    @Test
    public void f9Tag() throws Throwable {
        boolean enabled = FunctionTagging.isEnabled();
        try {
            Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> f;
            int k = 9;

            FunctionTagging.disable();

            for (int i = 1; i <= k; i++) {
                f = FunctionTagging.tagFunction9(
                        new FunctionTaggingComposite(i, false, false), "Custom tag");

                Integer t1 = 1;
                Integer t2 = 2;
                Integer t3 = 3;
                Integer t4 = 4;
                Integer t5 = 5;
                Integer t6 = 6;
                Integer t7 = 7;
                Integer t8 = 8;
                Integer t9 = 9;

                assertEquals((Integer)i, f.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9));

                if (i == 1) { t1 = null; }
                if (i == 2) { t2 = null; }
                if (i == 3) { t3 = null; }
                if (i == 4) { t4 = null; }
                if (i == 5) { t5 = null; }
                if (i == 6) { t6 = null; }
                if (i == 7) { t7 = null; }
                if (i == 8) { t8 = null; }
                if (i == 9) { t9 = null; }

                assertNull(f.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9));
            }

            // -----------------------------------------------------------------------------

            FunctionTagging.enable();

            for (int i = 1; i <= k; i++) {
                f = FunctionTagging.tagFunction9(
                        new FunctionTaggingComposite(i, false, false), "Custom tag");

                Integer t1 = 1;
                Integer t2 = 2;
                Integer t3 = 3;
                Integer t4 = 4;
                Integer t5 = 5;
                Integer t6 = 6;
                Integer t7 = 7;
                Integer t8 = 8;
                Integer t9 = 9;

                assertEquals((Integer)i, f.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9));

                if (i == 1) { t1 = null; }
                if (i == 2) { t2 = null; }
                if (i == 3) { t3 = null; }
                if (i == 4) { t4 = null; }
                if (i == 5) { t5 = null; }
                if (i == 6) { t6 = null; }
                if (i == 7) { t7 = null; }
                if (i == 8) { t8 = null; }
                if (i == 9) { t9 = null; }

                try {
                    f.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
                    throw new AssertionError("Should have thrown!");
                } catch (NullPointerException ex) {
                    assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
                    assertTrue(ex.getMessage(), ex.getMessage().contains("t" + i + " is null"));
                }
            }

            // oooooooooooooooooooooooooooooooo

            f = FunctionTagging.tagFunction9(
                    new FunctionTaggingComposite(0, true, false), "Custom tag");

            Integer t1 = 1;
            Integer t2 = 2;
            Integer t3 = 3;
            Integer t4 = 4;
            Integer t5 = 5;
            Integer t6 = 6;
            Integer t7 = 7;
            Integer t8 = 8;
            Integer t9 = 9;

            try {
                f.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Custom tag"));
            }

            f = FunctionTagging.tagFunction9(
                    new FunctionTaggingComposite(0, false, true), "Custom tag");

            try {
                f.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
                fail("Should have thrown!");
            } catch (IOException ex) {
                assertTrue(ex.getCause().getMessage(), ex.getCause().getMessage().contains("Custom tag"));
            }

        } finally {
            if (!enabled) {
                FunctionTagging.disable();
            }
        }
    }

    static final class FunctionTaggingComposite implements
    Function5<Integer, Integer, Integer, Integer, Integer, Integer>,
    Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer>,
    Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>,
    Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>,
    Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> {
        final int index;
        final boolean returnNull;
        final boolean crash;

        FunctionTaggingComposite(int index, boolean returnNull,
                boolean crash) {
            this.index = index;
            this.returnNull = returnNull;
            this.crash = crash;
        }

        @Override
        public Integer apply(Integer t1, Integer t2, Integer t3, Integer t4,
                Integer t5, Integer t6, Integer t7, Integer t8, Integer t9)
                throws Throwable {
            if (crash) {
                throw new IOException();
            }
            if (returnNull) {
                return null;
            }
            switch (index) {
            case 1: return t1;
            case 2: return t2;
            case 3: return t3;
            case 4: return t4;
            case 5: return t5;
            case 6: return t6;
            case 7: return t7;
            case 8: return t8;
            case 9: return t9;
            default:
            }
            return null;
        }

        @Override
        public Integer apply(Integer t1, Integer t2, Integer t3, Integer t4,
                Integer t5, Integer t6, Integer t7, Integer t8)
                throws Throwable {
            if (crash) {
                throw new IOException();
            }
            if (returnNull) {
                return null;
            }
            switch (index) {
            case 1: return t1;
            case 2: return t2;
            case 3: return t3;
            case 4: return t4;
            case 5: return t5;
            case 6: return t6;
            case 7: return t7;
            case 8: return t8;
            default:
            }
            return null;
        }

        @Override
        public Integer apply(Integer t1, Integer t2, Integer t3, Integer t4,
                Integer t5, Integer t6, Integer t7) throws Throwable {
            if (crash) {
                throw new IOException();
            }
            if (returnNull) {
                return null;
            }
            switch (index) {
            case 1: return t1;
            case 2: return t2;
            case 3: return t3;
            case 4: return t4;
            case 5: return t5;
            case 6: return t6;
            case 7: return t7;
            default:
            }
            return null;
        }

        @Override
        public Integer apply(Integer t1, Integer t2, Integer t3, Integer t4,
                Integer t5, Integer t6) throws Throwable {
            if (crash) {
                throw new IOException();
            }
            if (returnNull) {
                return null;
            }
            switch (index) {
            case 1: return t1;
            case 2: return t2;
            case 3: return t3;
            case 4: return t4;
            case 5: return t5;
            case 6: return t6;
            default:
            }
            return null;
        }

        @Override
        public Integer apply(Integer t1, Integer t2, Integer t3, Integer t4,
                Integer t5) throws Throwable {
            if (crash) {
                throw new IOException();
            }
            if (returnNull) {
                return null;
            }
            switch (index) {
            case 1: return t1;
            case 2: return t2;
            case 3: return t3;
            case 4: return t4;
            case 5: return t5;
            default:
            }
            return null;
        }
    }
}
