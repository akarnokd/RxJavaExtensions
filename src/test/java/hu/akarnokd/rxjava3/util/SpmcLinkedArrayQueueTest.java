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

package hu.akarnokd.rxjava3.util;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import hu.akarnokd.rxjava3.test.TestHelper;

public class SpmcLinkedArrayQueueTest {

    @Test(timeout = 5000)
    public void simple() {
        SpmcLinkedArrayQueue<Integer> q = new SpmcLinkedArrayQueue<Integer>(32);

        for (int i = 0; i < 128; i++) {
            assertTrue(q.isEmpty());
            assertTrue(q.offer(i));
            assertFalse(q.isEmpty());
            assertEquals(i, q.poll().intValue());
            assertTrue("" + i, q.isEmpty());
            assertNull(q.poll());
        }

        for (int i = 0; i < 128; i++) {
            q.offer(i);
        }

        for (int i = 0; i < 128; i++) {
            assertEquals(i, q.poll().intValue());
        }

        assertTrue(q.isEmpty());

        for (int i = 0; i < 128; i++) {
            q.offer(i);
        }

        q.clear();

        assertTrue(q.isEmpty());
    }

    @Test(timeout = 5000)
    public void simple2() {
        SpmcLinkedArrayQueue<Integer> q = new SpmcLinkedArrayQueue<Integer>(32);

        for (int i = 0; i < 128; i++) {
            assertTrue(q.isEmpty());
            assertTrue(q.offer(i, i));
            assertFalse(q.isEmpty());
            assertEquals(i, q.poll().intValue());
            assertEquals(i, q.poll().intValue());
            assertTrue("" + i, q.isEmpty());
            assertNull(q.poll());
        }

        for (int i = 0; i < 128; i++) {
            q.offer(i, i);
        }

        for (int i = 0; i < 128; i++) {
            assertEquals(i, q.poll().intValue());
            assertEquals(i, q.poll().intValue());
        }

        assertTrue(q.isEmpty());

        for (int i = 0; i < 128; i++) {
            q.offer(i, i);
        }

        q.clear();

        assertTrue(q.isEmpty());
    }

    @Test
    public void consumerRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final SpmcLinkedArrayQueue<Integer> q = new SpmcLinkedArrayQueue<Integer>(32);

            for (int j = 0; j < 1000; j++) {
                q.offer(j);
            }

            final Set<Integer> set1 = new HashSet<Integer>();
            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    for (;;) {
                        Integer k = q.poll();
                        if (k == null) {
                            break;
                        }
                        set1.add(k);
                    }
                }
            };

            final Set<Integer> set2 = new HashSet<Integer>();
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    for (;;) {
                        Integer k = q.poll();
                        if (k == null) {
                            break;
                        }
                        set2.add(k);
                    }
                }
            };

            TestHelper.race(r1, r2);

            set1.addAll(set2);

            assertEquals(1000, set1.size());

            for (int k = 0; k < 1000; k++) {
                assertTrue(set1.remove(k));
            }

            assertTrue(set1.isEmpty());
        }
    }

    @Test
    public void consumerRaceLong() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS / 25; i++) {
            final SpmcLinkedArrayQueue<Integer> q = new SpmcLinkedArrayQueue<Integer>(32);

            int n = 50000;

            for (int j = 0; j < n; j++) {
                q.offer(j);
            }

            final Set<Integer> set1 = new HashSet<Integer>();
            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    for (;;) {
                        Integer k = q.poll();
                        if (k == null) {
                            break;
                        }
                        set1.add(k);
                    }
                }
            };

            final Set<Integer> set2 = new HashSet<Integer>();
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    for (;;) {
                        Integer k = q.poll();
                        if (k == null) {
                            break;
                        }
                        set2.add(k);
                    }
                }
            };

            TestHelper.race(r1, r2);

            set1.addAll(set2);

            assertEquals(n, set1.size());

            for (int k = 0; k < n; k++) {
                assertTrue(set1.remove(k));
            }

            assertTrue(set1.isEmpty());
        }
    }

    @Test
    public void consumerRace2() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final SpmcLinkedArrayQueue<Integer> q = new SpmcLinkedArrayQueue<Integer>(32);

            for (int j = 0; j < 1000; j++) {
                q.offer(j);
            }

            final Set<Integer> set1 = new HashSet<Integer>();
            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    for (;;) {
                        Integer k = q.poll();
                        if (k == null) {
                            break;
                        }
                        set1.add(k);
                    }
                }
            };

            final Set<Integer> set2 = new HashSet<Integer>();
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    while (!q.isEmpty()) {
                        Integer k = q.poll();
                        if (k == null) {
                            break;
                        }
                        set2.add(k);
                    }
                }
            };

            TestHelper.race(r1, r2);

            set1.addAll(set2);

            assertEquals(1000, set1.size());

            for (int k = 0; k < 1000; k++) {
                assertTrue(set1.remove(k));
            }

            assertTrue(set1.isEmpty());
        }
    }

    @Test
    public void produceConsumeRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
            final SpmcLinkedArrayQueue<Integer> q = new SpmcLinkedArrayQueue<Integer>(32);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 1000; j++) {
                        q.offer(j);
                    }
                }
            };

            final List<Integer> list = new ArrayList<Integer>();
            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    int c = 0;
                    while (!Thread.currentThread().isInterrupted() && c != 1000) {
                        Integer k = q.poll();
                        if (k != null) {
                            list.add(k);
                            c++;
                        }
                    }
                }
            };

            TestHelper.race(r1, r2);

            assertEquals(1000, list.size());

            for (int k = 0; k < 1000; k++) {
                assertEquals(k, list.get(k).intValue());
            }
        }
    }
}
