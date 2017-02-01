# RxJava2Extensions

<a href='https://travis-ci.org/akarnokd/RxJava2Extensions/builds'><img src='https://travis-ci.org/akarnokd/RxJava2Extensions.svg?branch=master'></a>
[![codecov.io](http://codecov.io/github/akarnokd/RxJava2Extensions/coverage.svg?branch=master)](http://codecov.io/github/akarnokd/RxJava2Extensions?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.akarnokd/rxjava2-extensions/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.akarnokd/rxjava2-extensions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.reactivex.rxjava2/rxjava/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.reactivex.rxjava2/rxjava)

RxJava 2.x implementation of extra sources, operators and components and ports of many 1.x companion libraries.

# Releases

**gradle**

```
dependencies {
    compile "com.github.akarnokd:rxjava2-extensions:0.15.0"
}
```


Maven search:

[http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.akarnokd%22)

# Features

  - [Extra functional interfaces](#extra-functional-interfaces)
  - [Mathematical operations over numerical sequences](#mathematical-operations-over-numerical-sequences)
  - [Parallel operations](#parallel-operations)
  - [String operations](#string-operations)
  - [Asynchronous jumpstarting a sequence](#asynchronous-jumpstarting-a-sequence)
  - [Computational expressions](#computational-expressions)
  - [Join patterns](#join-patterns)
  - [Debug support](#debug-support)
  - [SingleSubject, MaybeSubject and CompletableSubject](#singlesubject-maybesubject-and-completablesubject)
  - [SoloProcessor, PerhapsProcessor and NonoProcessor](#soloprocessor-perhapsprocessor-and-nonoprocessor)
  - [FlowableProcessor utils](#flowableprocessor-utils)
  - [Custom Schedulers](#custom-schedulers)
  - [Custom operators and transformers](#custom-operators-and-transformers)
  - [Special Publisher implementations](#special-publisher-implementations)

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

**Deprecated in 0.14.4, functions moved to `io.reactivex.parallel.ParallelFlowable` in RxJava 2.0.5; this will be removed when RxJava 2.1 becomes available.**

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

### characters
The `StringFlowable` and `StringObservable` support streaming the characters of a `CharSequence`:

```java
StringFlowable.characters("Hello world")
.map(v -> Characters.toLower((char)v))
.subscribe(System.out::print, Throwable::printStackTrace, System.out::println);
```

### split

Splits an incoming sequence of Strings based on a Regex pattern within and between subsequent elements if necessary.

```java
Flowable.just("abqw", "ercdqw", "eref")
.compose(StringFlowable.split("qwer"))
.test()
.assertResult("ab", "cd", "ef");

Flowable.just("ab", ":cde:" "fg")
.compose(StringFlowable.split(":"))
.test()
.assertResult("ab", "cde", "fg");
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

**Deprecated in 0.14.4, functions moved to `io.reactivex.subjects.*` in RxJava 2.0.5; they will be removed when RxJava 2.1 becomes available.**


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

cs.onComplete();

to2.assertResult();
```

and


```java
SingleSubject<Integer> ss = SingleSubject.create();

TestObserver<Integer> to3 = ss.test();

ss.onSuccess(1);

to3.assertResult(1);
```

## SoloProcessor, PerhapsProcessor and NonoProcessor

These are the backpressure-aware, Reactive-Streams Processor-based implementations of the `SingleSubject`, `MaybeSubject` and CompletableSubject respectively. Their usage is quite similar.


```java
PerhapsProcessor<Integer> ms = PerhapsProcessor.create();

TestSubscriber<Integer> to = ms.test();

ms.onNext(1);
ms.onComplete();

to.assertResult(1);
```

Similarly with NonoProcessor, although calling `onNext(null)` will throw a `NullPointerException` to the caller.

```java
NonoProcessor cs = NonoProcessor.create();

TestSubscriber<Void> to2 = cs.test();

cs.onComplete();

to2.assertResult();
```

Finally


```java
SoloProcessor<Integer> ss = SoloProcessor.create();

TestSubscriber<Integer> to3 = ss.test();

ss.onNext(1);
ss.onComplete();

to3.assertResult(1);
```

Note that calling `onComplete` after `onNext` is optional with `SoloProcessor` but calling `onComplete` without calling `onNext` terminates the `SoloProcessor` with a `NoSuchElementException`.


## FlowableProcessor utils

### FlowableProcessors

An utility class that helps working with Reactive-Streams `Processor` and `FlowableProcessor` instances.

- `wrap`: wraps an arbitrary `Processor` into a `FlowableProcessor`

## Custom Schedulers

### SharedScheduler

This `Scheduler` implementation takes a `Worker` directly or from another `Scheduler` and shares it across its own `Worker`s while
making sure disposing one of its own `SharedWorker` doesn't dispose any other `SharedWorker` or the underlying shared `Worker`.

This type of scheduler may help solve the problem when one has to return to the same thread/scheduler at different stages of the pipeline
but one doesn't want to or isn't able to use `SingleScheduler` or some other single-threaded thread-pool wrapped via `Schedulers.from()`.

```java
SharedScheduler shared = new SharedScheduler(Schedulers.io());

Flowable.just(1)
.subscribeOn(shared)
.map(v -> Thread.currentThread().getName())
.observeOn(Schedulers.computation())
.map(v -> v.toLowerCase())
.observeOn(shared)
.map(v -> v.equals(Thread.currentThread().getName().toLowerCase()))
.blockingForEach(System.out::println);
```

### ParallelScheduler

It is similar to `Schedulers.computation()` but you can control the number of threads, the thread name prefix, the thread priority and to track each task sumbitted to its worker.

Tracking a task means that if one calls `Worker.dispose()`, all outstanding tasks is cancelled. However, certain use cases can get away with just preventing the execution of the task body and just run through all outstanding tasks yielding lower overhead.

The `ParallelScheduler` supports `start` and `shutdown` to start and stop the backing thread-pools. The non-`ThreadFactory` constructors create a daemon-thread backed set of single-threaded thread-pools.

```java
Scheduler s = new ParallelScheduler(3);

try {
    Flowable.range(1, 10)
    .flatMap(v -> Flowable.just(1).subscribeOn(s).map(v -> v + 1))
    .test()
    .awaitDone(5, TimeUnit.SECONDS)
    .assertValueSet(Arrays.asList(2, 3, 4, 5, 6, 7, 8, 9, 10, 11))
    .assertComplete()
    .assertNoErrors();
} finally {
    s.shutdown();
}
```

## Custom operators and transformers

The custom transformers (to be applied with `Flowable.compose` for example), can be found in `hu.akarnokd.rxjava2.operators.FlowableTransformers` class. The custom source-like operators can be found in `hu.akarnokd.rxjava2.operators.Flowables` class. The operators and transformers for the other base
reactive classes (will) follow the usual naming scheme.

### FlowableTransflormers.valve()

Pauses and resumes a main flow if the secondary flow signals false and true respectively.

```java
PublishProcessor<Boolean> valveSource = PublishProcessor.create();

Flowable.intervalRange(1, 20, 1, 1, TimeUnit.SECONDS)
.compose(FlowableTransformers.<Long>valve(valveSource))
.subscribe(System.out::println, Throwable::printStackTrace);

Thread.sleep(3100);

valveSource.onNext(false);

Thread.sleep(5000);

valveSource.onNext(true);

Thread.sleep(3000);

valveSource.onNext(false);

Thread.sleep(6000);

valveSource.onNext(true);

Thread.sleep(3000);
```

### Flowables.orderedMerge()

Given a fixed number of input sources (which can be self-comparable or given a `Comparator`) merges them
into a single stream by repeatedly picking the smallest one from each source until all of them completes.

```java
Flowables.orderedMerge(Flowable.just(1, 3, 5), Flowable.just(2, 4, 6))
.test()
.assertResult(1, 2, 3, 4, 5, 6);
```

### FlowableTransformers.bufferWhile()

Buffers into a list/collection while the given predicate returns true for
the current item, otherwise starts a new list/collection containing the given item (i.e., the "separator" ends up in the next list/collection).

```java
Flowable.just("1", "2", "#", "3", "#", "4", "#")
.compose(FlowableTransformers.bufferWhile(v -> !"#".equals(v)))
.test()
.assertResult(
    Arrays.asList("1", "2"),
    Arrays.asList("#", "3"),
    Arrays.asList("#", "4"),
    Arrays.asList("#")
);
```

### FlowableTransformers.bufferUntil()

Buffers into a list/collection until the given predicate returns true for
the current item and starts an new empty list/collection  (i.e., the "separator" ends up in the same list/collection).

```java
Flowable.just("1", "2", "#", "3", "#", "4", "#")
.compose(FlowableTransformers.bufferUntil(v -> "#".equals(v)))
.test()
.assertResult(
    Arrays.asList("1", "2", "#"),
    Arrays.asList("3", "#"),
    Arrays.asList("4", "#")
);
```

### FlowableTransformers.bufferSplit()

Buffers into a list/collection while the predicate returns false. When it returns true,
a new buffer is started and the particular item won't be in any of the buffers.

```java
Flowable.just("1", "2", "#", "3", "#", "4", "#")
.compose(FlowableTransformers.bufferSplit(v -> "#".equals(v)))
.test()
.assertResult(
    Arrays.asList("1", "2"),
    Arrays.asList("3"),
    Arrays.asList("4")
);
```

### FlowableTransformers.spanout()

Inserts a time delay between emissions from the upstream. For example, if the upstream emits 1, 2, 3 in a quick succession, a spanout(1, TimeUnit.SECONDS) will emit 1 immediately, 2 after a second and 3 after a second after 2. You can specify the initial delay, a custom scheduler and if an upstream error should be delayed after the normal items or not.

```java
Flowable.range(1, 10)
.compose(FlowableTransformers.spanout(1, 1, TimeUnit.SECONDS))
.doOnNext(v -> System.out.println(System.currentTimeMillis() + ": " + v))
.test()
.awaitDone(20, TimeUnit.SECONDS)
.assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
```

### FlowableTransformers.mapFilter()

A callback `Consumer` is called with the current upstream value and a `BasicEmitter` on which doXXX methods can be called
to transform a value, signal an error or stop a sequence. If none of the `doXXX` methods is called, the current value is dropped and another is requested from upstream. The operator is a pass-through for downstream requests otherwise.

```java
Flowable.range(1, 10)
.compose(FlowableTransformers.mapFilter((v, e) -> {
    if (v % 2 == 0) {
        e.doNext(v * 2);
    }
    if (v == 5) {
        e.doComplete();
    }
}))
.test()
.assertResult(4, 8);
```

### FlowableTransformers.onBackpressureTimeout()

Consumes the upstream in an unbounded manner and buffers elements until the downstream requests but each buffered element has an associated timeout after which it becomes unavailable. Note that this may create discontinuities in the stream. In addition, an overload allows specifying the maximum buffer size and an eviction action which gets triggered when the buffer reaches its
capacity or elements time out.

```java
Flowable.intervalRange(1, 5, 100, 100, TimeUnit.MILLISECONDS)
        .compose(FlowableTransformers
            .onBackpressureTimeout(2, 100, TimeUnit.MILLISECONDS,
                 Schedulers.single(), System.out::println))
        .test(0)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
```

### Flowables.repeat()

Repeats a scalar value indefinitely (until the downstream actually cancels), honoring backpressure and supporting synchronous fusion and/or conditional fusion.

```java
Flowable.repeat("doesn't matter")
.map(v -> ThreadLocalRandom.current().nextDouble())
.take(100)
.all(v -> v < 1d)
.test()
.assertResult(true);
```

### Flowables.repeatCallable()

Repeatedly calls a callable, indefinitely (until the downstream actually cancels) or if the callable throws or returns null (when it signals `NullPointerException`), honoring backpressure and supporting synchronous fusion and/or conditional fusion.

```java
Flowable.repeatCallable(() -> ThreadLocalRandom.current().nextDouble())
.take(100)
.all(v -> v < 1d)
.test()
.assertResult(true);
```

### FlowableTransformers.every()

Relays every Nth item from upstream (skipping the in-between items).

```java
Flowable.range(1, 5)
.compose(FlowableTransformers.<Integer>every(2))
.test()
.assertResult(2, 4)
```

### Flowables.intervalBackpressure()

Emit an ever increasing series of long values, starting from 0L and "buffer"
emissions in case the downstream can't keep up. The "buffering" is virtual and isn't accompanied by increased memory usage if it happens for a longer
period of time.

```java
Flowables.intervalBackpressure(1, TimeUnit.MILLISECONDS)
.observeOn(Schedulers.single())
.take(1000)
.test()
.awaitDone(5, TimeUnit.SECONDS)
.assertValueCount(1000)
.assertNoErrors()
.assertComplete();
```

### FlowableTransformers.cacheLast()

Caches the very last value of the upstream source and relays/replays it to Subscribers. The difference from `replay(1)` is that this operator is guaranteed
to hold onto exactly one value whereas `replay(1)` may keep a reference to the one before too due to continuity reasons.

```java
Flowable<Integer> f = Flowable.range(1, 5)
.doOnSubscribe(s -> System.out.println("Subscribed!"))
.compose(FlowableTransformers.cacheLast());

// prints "Subscribed!"
f.test().assertResult(5);

// doesn't print anything else
f.test().assertResult(5);
f.test().assertResult(5);
```

### FlowableTransformers.timeoutLast() & timeoutLastAbsolute()

The operator consumes the upstream to get to the last value but completes if the
sequence doesn't complete within the specified timeout. A use case is when the upstream generates estimates, each better than the previous but we'd like to receive the last of it and not wait for a potentially infinite series.

There are two variants: relative timeout and absolute timeout.

With relative timeout, the operator restarts the timeout after each upstream item, cancels the upstream and emits that latest item if the timeout happens:

```java
Flowable.just(0, 50, 100, 400)
.flatMap(v -> Flowable.timer(v, TimeUnit.MILLISECONDS).map(w -> v))
.compose(FlowableTransformers.timeoutLast(200, TimeUnit.MILLISECONDS))
.test()
.awaitDone(5, TimeUnit.SECONDS)
.assertResult(100);
```

With absolute timeout, the upstream operator is expected to complete within the
specified amount of time and if it doesn't, the upstream gets cancelled and the latest item emitted.

```java
Flowable.just(0, 50, 100, 150, 400)
.flatMap(v -> Flowable.timer(v, TimeUnit.MILLISECONDS).map(w -> v))
.compose(FlowableTransformers.timeoutLastAbsolute(200, TimeUnit.MILLISECONDS))
.test()
.awaitDone(5, TimeUnit.SECONDS)
.assertResult(150);
```

### FlowableTransformer.debounceFirst()

Debounces the upstream by taking an item and dropping subsequent items until the specified amount of time elapses after the last item, after which the process repeats.

```java
Flowable.just(0, 50, 100, 150, 400, 500, 550, 1000)
.flatMap(v -> Flowable.timer(v, TimeUnit.MILLISECONDS).map(w -> v))
.compose(FlowableTransformers.debounceFirst(200, TimeUnit.MILLISECONDS))
.test()
.awaitDone(5, TimeUnit.SECONDS)
.assertResult(0, 400, 1000);
```

### FlowableTransformer.switchFlatMap

This is a combination of switchMap and a limited flatMap. It merges a maximum number of Publishers at once but if a new inner Publisher gets mapped in and the active count is at max, the oldest active Publisher is cancelled and the new inner Publisher gets flattened as well. Running with `maxActive == 1` is equivalent to the plain `switchMap`.

```java
Flowable.just(100, 300, 500)
.flatMap(v -> Flowable.timer(v, TimeUnit.MILLISECONDS).map(w -> v))
.compose(FlowableTransformers.switchFlatMap(v -> {
    if (v == 100) {
        return Flowable.intervalRange(1, 3, 75, 100, TimeUnit.MILLISECONDS)
           .map(w -> "A" + w);
    } else
    if (v == 300) {
        return Flowable.intervalRange(1, 3, 10, 100, TimeUnit.MILLISECONDS)
           .map(w -> "B" + w);
    }
    return Flowable.intervalRange(1, 3, 20, 100, TimeUnit.MILLISECONDS)
        .map(w -> "C" + w);
}, 2)
.test()
.awaitDone(5, TimeUnit.SECONDS)
.assertResult("A1", "A2", "B1", "A3", "B2", "C1", B3", "C2", "C3);
``` 

## Special Publisher implementations

### Nono - 0-error publisher

The `Publisher`-based sibling of the `Completable` type. The usage is practically the same as `Completable` with the exception that because `Nono` implements the Reactive-Streams `Publisher`, you can use it directly with operators of `Flowable` that accept `Publisher` in some form.

Examples:

```java
Nono.fromAction(() -> System.out.println("Hello world!"))
    .subscribe();

Nono.fromAction(() -> System.out.println("Hello world!"))
    .delay(1, TimeUnit.SECONDS)
    .blockingSubscribe();

Nono.complete()
    .test()
    .assertResult();

Nono.error(new IOException())
    .test()
    .assertFailure(IOException.class);

Flowable.range(1, 10)
    .to(Nono::fromPublisher)
    .test()
    .assertResult();
```

#### NonoProcessor

A hot, Reactive-Streams `Processor` implementation of `Nono`.

```java
NonoProcessor np = NonoProcessor.create();

TestSubscriber<Void> ts = np.test();

np.onComplete();

ts.assertResult();
```


### Solo - 1-error publisher

The `Publisher`-based sibling of the `Single` type. The usage is practically the same as `Single` with the exception that because `Solo` implements the Reactive-Streams `Publisher`, you can use it directly with operators of `Flowable` that accept `Publisher` in some form.

`Solo`'s emission protocol is a restriction over the general `Publisher` protocol: one either calls `onNext` followed by `onComplete` or just `onError`. Operators will and should never call `onNext` followed by `onError` or `onComplete` on its own. Note that some operators may react to `onNext` immediately not waiting for an `onComplete` but on their emission side, `onComplete` is always called after an `onNext`.

Examples:

```java
Solo.fromCallable(() -> {
    System.out.println("Hello world!");
    return 1;
}).subscribe();

Solo.fromCallable(() -> "Hello world!")
    .delay(1, TimeUnit.SECONDS)
    .blockingSubscribe(System.out::println);

Flowable.concat(Solo.just(1), Solo.just(2))
.test()
.assertResult(1, 2);
```

#### SoloProcessor

A hot, Reactive-Streams `Processor` implementation of `Solo`.

```java
SoloProcessor<Integer> sp = SoloProcessor.create();

TestSubscriber<Integer> ts = sp.test();

sp.onNext(1);
sp.onComplete();

ts.assertResult(1);
```

### Perhaps - 0-1-error publisher

The `Publisher`-based sibling of the `Maybe` type. The usage is practically the same as `Maybe` with the exception that because `Perhaps` implements the Reactive-Streams `Publisher`, you can use it directly with operators of `Flowable` that accept `Publisher` in some form.

`Perhaps`'s emission protocol is a restriction over the general `Publisher` protocol: one either calls `onNext` followed by `onComplete`, `onComplete` only or just `onError`. Operators will and should never call `onNext` followed by `onError` on its own. Note that some operators may react to `onNext` immediately not waiting for an `onComplete` but on their emission side, `onComplete` is always called after an `onNext`.

Examples:

```java
Perhaps.fromCallable(() -> {
    System.out.println("Hello world!");
    return 1;
}).subscribe();

Perhaps.fromCallable(() -> "Hello world!")
    .delay(1, TimeUnit.SECONDS)
    .blockingSubscribe(System.out::println);

Flowable.concat(Perhaps.just(1), Perhaps.just(2))
.test()
.assertResult(1, 2);

Perhaps.fromCallable(() -> {
    System.out.println("Hello world!");
    return null;  // null is considered to indicate an empty Perhaps
})
.test()
.assertResult();
```

#### PerhapsProcessor

A hot, Reactive-Streams `Processor` implementation of `Perhaps`.

```java
PerhapsProcessor<Integer> ph = PerhapsProcessor.create();

TestSubscriber<Integer> ts = ph.test();

ph.onNext(1);
ph.onComplete();

ts.assertResult(1);
```

