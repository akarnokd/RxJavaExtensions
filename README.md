# RxJava2Extensions

<a href='https://travis-ci.org/akarnokd/RxJava2Extensions/builds'><img src='https://travis-ci.org/akarnokd/RxJava2Extensions.svg?branch=master'></a>
[![codecov.io](http://codecov.io/github/akarnokd/RxJava2Extensions/coverage.svg?branch=master)](http://codecov.io/github/akarnokd/RxJava2Extensions?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.akarnokd/rxjava2-extensions/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.akarnokd/rxjava2-extensions)

Extra sources, operators and components and ports of many 1.x companion libraries for RxJava 2.x.

# Features

## Extra functional interfaces

Support the join-patterns and async-util with functional interfaces of consumers with 3-9 type arguments
and have functional interfaces of functions without the `throws Exception`.

  - `Supplier<T>` - `Callable<T>` without `throws Exception`
  - `Consumer3` - 3 argument `Consumer`
  - `Consumer4` - 4 argument `Consumer`
  - `Consumer5` - 5 argument `Consumer`
  - `Consumer6` - 6 argument `Consumer`
  - `Consumer7` - 7 argument `Consumer`
  - `Consumer8` - 8 argument `Consumer`
  - `Consumer9` - 9 argument `Consumer`
  - `PlainFunction` - `Function` without `throws Exception`
  - `PlainBiFunction` - `BiFunction` without `throws Exception`
  - `PlainFunction3` - `Function3` without `throws Exception`
  - `PlainFunction4` - `Function4` without `throws Exception`
  - `PlainFunction5` - `Function5` without `throws Exception`
  - `PlainFunction6` - `Function6` without `throws Exception`
  - `PlainFunction7` - `Function7` without `throws Exception`
  - `PlainFunction8` - `Function8` without `throws Exception`
  - `PlainFunction9` - `Function9` without `throws Exception`
  

Utility functions supporting these can be found in `FunctionsEx` class.


## Mathematical operations over numerical sequences

Although most of the operations can be performed with `reduce`, these operators have lower overhead
as they cut out the reboxing of primitive intermediate values.

The following operations are available in `MathFlowable` for `Flowable` sequences and `MathObservable` in `Observable`
sequences:

  - `averageDouble()`
  - `averageFloat()`
  - `max()`
  - `min()`
  - `sumDouble()`
  - `sumFloat()`
  - `sumInt()`
  - `sumLong()`
  
Example

```java
MathFlowable.averageDouble(Flowable.range(1, 10))
.test()
.assertResult(5.5);

Flowable.just(5, 1, 3, 2, 4)
.to(MathFlowable::min)
.test()
.assertResult(1);
```
  

## Parallel operations

RxJava is sequential by nature and you can achieve parallelism by having parallel sequences. Forking out
and joining a single sequence can become complicated and incurs quite an overhead. By using the `ParallelFlowable`
API, the forking and joining of parallel computations started in and ending in a Flowable is more efficient than
the classical methods (groupBy/flatMap, window/flatMap, etc).

The main entry point is `ParallelFlowable.from(Publisher, int)` where given a `Flowable` and the parallelism level, the source
sequence is dispatched into N parallel 'rails' which are fed in a round-robin fashion from the source `Flowable`.

The `from()` method only defines how many rails there will be but it won't actually run these rails on different threads. As with other RxJava, asynchrony is optional and introduced via an operator `runOn` which takes the usual `Scheduler` of your chosing. Note that the parallelism level of the `Scheduler` and the `ParallelFlowable` need not to match (but it is recommended).

Not all sequential operations make sense in the parallel world. These are the currently supported operations:

  - `map`, 
  - `filter`
  - `flatMap`, 
  - `concatMap`
  - `reduce`
  - `collect`
  - `sort`
  - `toSortedList`
  - `compose`
  - `doOnCancel`, `doOnError`, `doOnComplete`, `doOnNext`, `doOnSubscribe`, `doAfterTerminate`, `doOnRequest`

Example:

```java
ParallelFlowable.from(Flowable.range(1, 1000))
.runOn(Schedulers.computation())
.map(v -> v * v)
.filter(v -> (v & 1) != 0)
.sequential()
.subscribe(System.out::println)
```

## String operations

Currently, only the `StringFlowable` and `StringObservable` support streaming the characters of a `CharSequence`:

```java
StringFlowable.characters("Hello world")
.map(v -> Characters.toLower((char)v))
.subscribe(System.out::print, Throwable::printStackTrace, System.out::println);
```

## Asynchronous jumpstarting a sequence

Wrap functions and consumers into Flowables and Observables or into another layer of Functions.
Most of these can now be achieved via `fromCallable` and some function composition in plain RxJava (1.x and 2.x alike).

### start 

Run a function or action once on a background thread and cache its result.

```java
AtomicInteger counter = new AtomicInteger();

Flowable<Integer> source = AsyncFlowable.start(() -> counter.incrementAndGet());

source.test()
    .awaitDone(5, TimeUnit.SECONDS)
    .assertResult(1);

source.test()
    .awaitDone(5, TimeUnit.SECONDS)
    .assertResult(1);
```

### toAsync

Call a function (with parameters) to call a function inside a `Flowable`/`Observable` with the same
parameter and have the result emitted by that `Flowable`/`Observable` from a background thread.

```java

Function<Integer, Flowable<String>> func = AsyncFlowable.toAsync(
    param -> "[" + param + "]"
);

func.apply(1)
    .test()
    .awaitDone(5, TimeUnit.SECONDS)
    .assertResult("[1]")
;
```
### startFuture

Run a Callable that returns a Future to call blocking get() on to get the solo value or exception.

```java
ExecutorService exec = Executors.newSingleThreadedScheduler();

AsyncFlowable.startFuture(() -> exec.submit(() -> 1))
    .test()
    .awaitDone(5, TimeUnit.SECONDS)
    .assertResult(1);
    
exec.shutdown();
```

### deferFuture

Run a Callable that returns a Future to call blocking get() on to get a `Publisher` to stream back.

```java
ExecutorService exec = Executors.newSingleThreadedScheduler();

AsyncFlowable.startFuture(() -> exec.submit(() -> Flowable.range(1, 5)))
    .test()
    .awaitDone(5, TimeUnit.SECONDS)
    .assertResult(1, 2, 3, 4, 5);
    
exec.shutdown();
```

### forEachFuture

Consume a `Publisher` and have `Future` that completes when the consumption ends with `onComplete` or `onError`.

```java
Future<Object> f = AsyncFlowable.forEachFuture(Flowable.range(1, 100), System.out::println);

f.get();
```

### runAsync

Allows emitting multiple values through a Processor mediator from a background thread and allows disposing
the sequence externally.

```java
AsyncFlowable.runAsync(Schedulers.single(),
        UnicastProcessor.<Object>create(),
        new BiConsumer<Subscriber<Object>, Disposable>() {
            @Override
            public void accept(Subscriber<? super Object> s, Disposable d) throws Exception {
                s.onNext(1);
                s.onNext(2);
                s.onNext(3);
                Thread.sleep(200);
                s.onNext(4);
                s.onNext(5);
                s.onComplete();
            }
        }
).test()
.awaitDone(5, TimeUnit.SECONDS)
.assertResult(1, 2, 3, 4, 5);
```

## Computational expressions

The operators on `StatementFlowable` and `StatementObservable` allow taking different branches at subscription time:

### ifThen

Conditionally chose a source to subscribe to. This is similar to the imperative `if` statement but with reactive flows:

```java
if ((System.currentTimeMillis() & 1) != 0) {
    System.out.println("An odd millisecond");
} else {
    System.out.println("An even millisecond");
}
```

```java
Flowable<String> source = StatementFlowable.ifThen(
    () -> (System.currentTimeMillis() & 1) != 0,
    Flowable.just("An odd millisecond"),
    Flowable.just("An even millisecond")
);

source
.delay(1, TimeUnit.MILLISECONDS)
.repeat(1000)
.subscribe(System.out::println);
```

### switchCase

Calculate a key and pick a source from a Map. This is similar to the imperative `switch` statement:

```java
switch ((int)(System.currentTimeMillis() & 7)) {
case 1: System.out.println("one"); break;
case 2: System.out.println("two"); break;
case 3: System.out.println("three"); break;
default: System.out.println("Something else");
}
```

```java
Map<Integer, Flowable<String>> map = new HashMap<>();

map.put(1, Flowable.just("one"));
map.put(2, Flowable.just("two"));
map.put(3, Flowable.just("three"));

Flowable<String> source = StatementFlowable.switchCase(
    () -> (int)(System.currentTimeMillis() & 7),
    map,
    Flowable.just("Something else")
);

source
.delay(1, TimeUnit.MILLISECONDS)
.repeat(1000)
.subscribe(System.out::println);
```

### doWhile

Resubscribe if a condition is true after the last subscription completed normally. This is similar to the imperative `do-while` loop (executing the loop body at least once):

```java
long start = System.currentTimeMillis();
do {
    Thread.sleep(1);
    System.out.println("Working...");
while (start + 100 > System.currentTimeMillis());
```

```java
long start = System.currentTimeMillis();

Flowable<String> source = StatementFlowable.doWhile(
    Flowable.just("Working...").delay(1, TimeUnit.MILLISECONDS),
    () -> start + 100 > System.currentTimeMillis()
);

source.subscribe(System.out::println);
```


### whileDo

Subscribe and resubscribe if a condition is true. This is similar to the imperative `while` loop (where the loop body may not execute if the condition is false
to begin with):

```java

while ((System.currentTimeMillis() & 1) != 0) {
    System.out.println("What an odd millisecond!");
}
```

```java
Flowable<String> source = StatementFlowable.whileDo(
    Flowable.just("What an odd millisecond!"),
    () -> (System.currentTimeMillis() & 1) != 0
);

source.subscribe(System.out::println);
```

## Join patterns

(Conversion done)

TBD: examples

## Debug support

By default, RxJava 2's RxJavaPlugins only offers the ability to hook into the assembly process (i.e., when you apply an operator on a sequence or create one) unlike 1.x where there is an `RxJavaHooks.enableAssemblyTracking()` method. Since the standard format is of discussion there, 2.x doesn't have such feature built in but only
in this extension library.

### Usage

You enable tracking via:

```java
RxJavaAssemblyTracking.enable();
```

and disable via:

```java
RxJavaAssemblyTracking.disable();
```

Note that this doesn't save or preserve the old hooks (named `Assembly`) you may have set as of now.

### Output

In debug mode, you can walk through the reference graph of Disposables and Subscriptions to find an `FlowableOnAssemblyX` named nodes (similar in the other base types) where there is an `assembled` field of type `RxJavaAssemblyException`. This has also a field named `stacktrace` that contains a pretty printed stacktrace string pointing to the assembly location:

```
RxJavaAssemblyException: assembled
at io.reactivex.Completable.error(Completable.java:280)
at hu.akarnokd.rxjava2.debug.RxJava2AssemblyTrackingTest.createCompletable(RxJava2AssemblyTrackingTest.java:78)
at hu.akarnokd.rxjava2.debug.RxJava2AssemblyTrackingTest.completable(RxJava2AssemblyTrackingTest.java:185)
```

This is a filtered list of stacktrace elements (skipping threading, unit test and self-related entries). Most modern IDEs should allow you to navigate to the locations when printed on (or pasted into) its console.

To avoid interference, the `RxJavaAssemblyException` is attached as the last cause to potential chain of the original exception that travels through each operator to the end consumer.

You can programmatically find this via:

```java
RxJavaAssemblyException assembled = RxJavaAssemblyException.find(someThrowable);

if (assembled != null) {
    System.err.println(assembled.stacktrace());
}
```

## SingleSubject, MaybeSubject and CompletableSubject

These are the hot variants of the respective base reactive types.

```java
MaybeSubject<Integer> ms = MaybeSubject.create();

TestObserver<Integer> to = ms.test();

ms.onSuccess(1);

to.assertResult(1);
```

Similarly:

```java
CompletableSubject cs = CompletableSubject.create();

TestObserver<Void> to2 = cs.test();

ms.onComplete();

to2.assertResult();
```

# Releases

**gradle**

```
dependencies {
    compile "com.github.akarnokd:rxjava2-extensions:0.5.1"
}
```


Maven search:

[http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.akarnokd%22)
