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

package hu.akarnokd.rxjava2.operators;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import hu.akarnokd.rxjava2.test.TestHelper;
import io.reactivex.*;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.PublishSubject;

public class ObservableFlatMapLatestTest {

    @Test
    public void simple() {
        Observable.range(1, 5)
        .compose(ObservableTransformers.flatMapLatest(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v)
                    throws Exception {
                return Observable.just(v);
            }
        }))
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void outerError() {
        Observable.<Integer>error(new IOException())
        .compose(ObservableTransformers.flatMapLatest(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v)
                    throws Exception {
                return Observable.just(v);
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void innerError() {
        Observable.just(1)
        .compose(ObservableTransformers.flatMapLatest(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v)
                    throws Exception {
                return Observable.<Integer>error(new IOException());
            }
        }))
        .test()
        .assertFailure(IOException.class);
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Observable.range(1, 5)
        .compose(ObservableTransformers.flatMapLatest(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v)
                    throws Exception {
                return Observable.just(v);
            }
        })));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o)
                    throws Exception {
                return o.compose(ObservableTransformers.flatMapLatest(new Function<Object, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(Object v)
                            throws Exception {
                        return Observable.just(v);
                    }
                }));
            }
        });
    }

    @Test
    public void ignoreWhileActive() {
        PublishSubject<Integer> ps1 = PublishSubject.create();
        @SuppressWarnings("unchecked")
        final PublishSubject<Integer>[] ps2 = new PublishSubject[] {
                PublishSubject.<Integer>create(),
                PublishSubject.<Integer>create(),
                PublishSubject.<Integer>create()
        };

        TestObserver<Integer> to = ps1.compose(
                ObservableTransformers.flatMapLatest(new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer v)
                            throws Exception {
                        return ps2[v];
                    }
                }))
                .test();

        ps1.onNext(0);

        assertTrue(ps2[0].hasObservers());
        assertFalse(ps2[1].hasObservers());
        assertFalse(ps2[2].hasObservers());

        ps1.onNext(1);

        assertTrue(ps2[0].hasObservers());
        assertFalse(ps2[1].hasObservers());
        assertFalse(ps2[2].hasObservers());

        ps2[0].onComplete();

        ps1.onNext(2);

        assertFalse(ps2[0].hasObservers());
        assertTrue(ps2[1].hasObservers());
        assertFalse(ps2[2].hasObservers());

        ps1.onComplete();

        ps2[1].onComplete();

        assertFalse(ps2[0].hasObservers());
        assertFalse(ps2[1].hasObservers());
        assertTrue(ps2[2].hasObservers());

        ps2[2].onComplete();

        to.assertResult();
    }

    @Test
    public void mapperCrash() {
        final Boolean[] disposed = { null };

        Observable.range(1, 5)
        .doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                disposed[0] = true;
            }
        })
        .compose(ObservableTransformers.flatMapLatest(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v)
                    throws Exception {
                throw new IOException();
            }
        }))
        .test()
        .assertFailure(IOException.class);

        assertTrue(disposed[0]);
    }

    @Test
    public void innerErrorLast() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = Observable.just(1)
        .compose(ObservableTransformers.flatMapLatest(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v)
                    throws Exception {
                return ps;
            }
        }))
        .test();

        to.assertEmpty();

        ps.onError(new IOException());

        to.assertFailure(IOException.class);
    }

    @Test
    public void lateErrorOuter() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(
                        Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onError(new IOException("first"));
                    observer.onError(new IOException("second"));
                }
            }
            .compose(ObservableTransformers.flatMapLatest(new Function<Integer, ObservableSource<Integer>>() {
                @Override
                public ObservableSource<Integer> apply(Integer v)
                        throws Exception {
                    return Observable.just(v);
                }
            }))
            .test()
            .assertFailureAndMessage(IOException.class, "first");

            TestHelper.assertUndeliverable(errors, 0, IOException.class, "second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void lateErrorInner() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final AtomicReference<Observer<? super Integer>> ref = new AtomicReference<Observer<? super Integer>>();

            TestObserver<Integer> to = Observable.just(1)
            .compose(ObservableTransformers.flatMapLatest(new Function<Integer, ObservableSource<Integer>>() {
                @Override
                public ObservableSource<Integer> apply(Integer v)
                        throws Exception {
                    return new Observable<Integer>() {
                        @Override
                        protected void subscribeActual(
                                Observer<? super Integer> observer) {
                            observer.onSubscribe(Disposables.empty());
                            ref.set(observer);
                        }
                    };
                }
            }))
            .test();

            ref.get().onError(new IOException("first"));
            ref.get().onError(new IOException("second"));

            to.assertFailureAndMessage(IOException.class, "first");

            TestHelper.assertUndeliverable(errors, 0, IOException.class, "second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void disposeWhileInDrain() {
        final TestObserver<Integer> to = new TestObserver<Integer>();

        Observable.just(1)
        .compose(ObservableTransformers.flatMapLatest(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v)
                    throws Exception {
                to.dispose();
                return Observable.never();
            }
        }))
        .subscribe(to);

        to.assertEmpty();
    }
}
