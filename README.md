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
    compile "com.github.akarnokd:rxjava2-extensions:0.18.6"
}
```


Maven search:

[http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.akarnokd%22)

# Features

  - [Extra functional interfaces](#extra-functional-interfaces)
  - [Mathematical operations over numerical sequences](#mathematical-operations-over-numerical-sequences)
  - [String operations](#string-operations)
  - [Asynchronous jumpstarting a sequence](#asynchronous-jumpstarting-a-sequence)
  - [Computational expressions](#computational-expressions)
  - [Join patterns](#join-patterns)
  - [Debug support](#debug-support)
    - [Function tagging](#function-tagging)
    - [Protocol validation](#protocol-validation)
    - [Multi-hook handlers](#multi-hook-handlers)
  - Custom Processors and Subjects
    - [SoloProcessor, PerhapsProcessor and NonoProcessor](#soloprocessor-perhapsprocessor-and-nonoprocessor)
    - [MulticastProcessor](#multicastprocessor)
  - [FlowableProcessor utils](#flowableprocessor-utils)
    - [wrap](#wrap), [refCount](#refcount)
  - [Custom Schedulers](#custom-schedulers)
    - [SharedScheduler](#sharedscheduler)
    - [ParallelScheduler](#parallelscheduler)
    - [BlockingScheduler](#blockingscheduler)
  - [Custom operators and transformers](#custom-operators-and-transformers)
    - [valve()](#flowabletransflormersvalve), [orderedMerge()](#flowablesorderedmerge), [bufferWhile()](#flowabletransformersbufferwhile),
    - [bufferUntil()](#flowabletransformersbufferuntil), [bufferSplit()](#flowabletransformersbuffersplit), [spanout()](#flowabletransformersspanout),
    - [mapFilter()](#flowabletransformersmapfilter), [onBackpressureTimeout()](#flowabletransformersonbackpressuretimeout), [repeat()](#flowablesrepeat),
    - [repeatCallable()](#flowablesrepeatcallable), [every()](#flowabletransformersevery), [intervalBackpressure()](#flowablesintervalbackpressure),
    - [cacheLast()](#flowabletransformerscachelast), [timeoutLast()](#flowabletransformerstimeoutlast--timeoutlastabsolute), [timeoutLastAbsolute()](#flowabletransformerstimeoutlast--timeoutlastabsolute),
    - [debounceFirst()](#flowabletransformersdebouncefirst), [switchFlatMap()](#flowabletransformersswitchflatmap), [flatMapSync()](#flowabletransformersflatmapsync),
    - [flatMapAsync()](#flowabletransformersflatmapasync), [switchIfEmpty()](#flowabletransformersswitchifempty--switchifemptyarray),
    - [expand()](#flowabletransformersexpand), [mapAsync()](#flowabletransformersmapasync), [filterAsync()](#flowabletransformerfilterasync),
    - [refCount()](#flowabletransformersrefcount), [zipLatest()](#flowablesziplatest), [coalesce()](#flowabletransformerscoalesce),
    - [windowWhile()](#flowabletransformerswindowwhile), [windowUntil()](#flowabletransformerswindowuntil), [windowSplit()](#flowabletransformerswindowsplit),
    - [indexOf()](#flowabletransformersindexof), [requestObserveOn()](#flowabletransformersrequestobserveon), [requestSample()](#flowabletransformersrequestsample)
  - [Custom parallel operators and transformers](#custom-parallel-operators-and-transformers)
    - [sumX()](#paralleltransformerssumx)
    - [orderedMerge()](#paralleltransformersorderedmerge)
  - [Special Publisher implementations](#special-publisher-implementations)
  - Custom consumers
    - [FlowableConsumers](#flowableconsumers)
      - [subscribeAutoRelease()](#subscribeautorelease)
    - [ObservableConsumers](#observableconsumers)
    - [SingleConsumers](#singleconsumers)
    - [MaybeConsumers](#maybeconsumers)
    - [CompletableConsumers](#completableconsumers)

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

### Function tagging

Often, when a function throws or returns null, there is not enough information to
locate said function in the codebase. The `FunctionTagging` utility class offers
static wrappers for RxJava funtion types that when fail or return null, a custom
string tag is added or appended to the exception and allows locating that function
in your codebase. Since added logic has overhead, the tagging process has to be
enabled and can be disabled as necessary.

```java
FunctionTagging.enable();

Function<Integer, Integer> tagged = FunctionTagging.tagFunction(v -> null, "F1");

FunctionTagging.disable();

Function<Integer, Integer> notTagged = FunctionTagging.tagFunction(v -> null, "F2");

assertNull(notTagged.apply(1));

try {
   tagged.apply(1);
   fail("Should have thrown");
} catch (NullPointerException ex) {
   assertTrue(ex.getMessage().contains("F2"));
}
```

To avoid lambda ambiguity, the methods are named `tagX` where `X` is the functional type name such as `BiFunction`, `Function3` etc.

The wrappers check for `null` parameters and if the wrapped function returns a `null` and throw a `NullPointerException` containing the parameter
name (t1 .. t9) and the tag provided.

### Protocol validation

Custom operators and sources sometimes contain bugs that manifest themselves in odd sequence behavior or crashes 
from within the standard operators. Since the revealing stacktraces is often missing or incomplete, diagnosing 
such failures can be tiresome. Therefore, the `hu.akarnokd.rxjava2.debug.validator.RxJavaProtocolValidator` 
class offers assembly hooks for the standard reactive base types.

The validation hooks can be enabled via `RxJavaProtocolValidator.enable()` and disabled via `RxJavaProtocolValidator.disable()`.
The validator also supports chaining with existing hooks via `enableAndChain()` which returns a `SavedHooks` instance
to restore the original hooks specifically:

```java
SavedHooks hooks = RxJavaProtocolValidator.enableAndChain();

// assemble and run flows
// ...

hooks.restore();
```

By default, the violations, subclasses of `ProtocolNonConformanceException`, are reported to the `RxJavaHooks.onError`
handler but can be overridden via `RxJavaProtocolValidator.setOnViolationHandler`.

```java
RxJavaProtocolValidator.setOnViolationHandler(e -> e.printStackTrace());

RxJavaProtocolValidator.enable();

// ...
```

The following error violations are detected:

| Exception | Violation description |
|-----------|-----------------------|
| MultipleTerminationsException | When multiple calls to `onError` or `onComplete` happened. |
| MultipleOnSubscribeCallsException | When multiple calls to `onSubscribe` happened |
| NullOnErrorParameterException | When the `onError` was called with a `null` `Throwable`. |
| NullOnNextParameterException | When the `onNext` was called with a `null` value. |
| NullOnSubscribeParameterException | When the `onSubscribe` was called with a `null` `Disposable` or `Subscription`. |
| NullOnSuccessParameterException | When the `onSuccess` was called with a `null` value. |
| OnNextAfterTerminationException | Wen the `onNext` was called after `onError` or `onComplete`. |
| OnSubscribeNotCalledException | When any of the `onNext`, `onSuccess`, `onError` or `onComplete` is invoked without invoking `onSubscribe` first. |
| OnSuccessAfterTerminationException | Wen the `onSuccess` was called after `onError` or `onComplete`. |

### Multi-hook handlers

The standard `RxJavaPlugins` allows only one hook to be associated with each main intercept option.
If multiple hooks should be invoked, that option is not directly supported by `RxJavaPlugins` but
can be built upon the single hook scheme.

The `hu.akarnokd.rxjava2.debug.multihook` package offers hook managers that can work with multiple hooks
themselves.

The various multi-hook managers are built upon the generic `MultiHandlerManager<H>` class. The class offers the `register`
method to register a particular hook for which a `Disposable` is returned. This allows removing a particular
hook without the need to remember the hook instance or the manager class.

#### `OnScheduleMultiHookManager`

Offers multi-hook management for the `RxJavaPlugins.setScheduleHandler` and `onSchedule` hooks.

The `enable()` method will install the instance as the main hook, the `disable()` will restore the default no-hook.
The convenience `append()` will take any existing hook, register it with the manager and install the manager as the main hook.

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

### MulticastProcessor

Works similarly to `publish(Function)` and multicasts items to subscribers if all of them are ready to receive the items.
In addition, it supports a mode where the last subscriber cancelling will trigger a cancellation to the upstream.
If you need it to run without subscribing the `MulticastProcessor` to another `Publisher` use `start()` or `startUnbounded()`.
Use `offer()` to try and offer/emit items but don't fail if the internal buffer is full.

```java
MulticastProcessor<Integer> mp = Flowable.range(1, 10)
    .subscribeWith(MulticastProcessor.create());

mp.test().assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

// --------------------

MulticastProcessor<Integer> mp2 = MulticastProcessor.create(4);
mp2.start();

assertTrue(mp2.offer(1));
assertTrue(mp2.offer(2));
assertTrue(mp2.offer(3));
assertTrue(mp2.offer(4));

assertFalse(mp2.offer(5));

mp2.onComplete();

mp2.test().assertResult(1, 2, 3, 4);
```

## FlowableProcessor utils

An utility class that helps working with Reactive-Streams `Processor`, `FlowableProcessor`
`Subject` instances via static methods.

### wrap

Wraps an arbitrary `Processor` into a `FlowableProcessor`

### refCount

Wraps a `FlowableProcessor`/`Subject` and makes sure if all subscribers/observer cancel/dispose
their subscriptions, the upstream's `Subscription`/`Disposable` gets cancelled/disposed as well.

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

### BlockingScheduler

This type of scheduler runs its execution loop on the "current thread", more specifically, the thread which invoked its `execute()` method. The method blocks until the `shutdown()` is invoked. This type of scheduler allows returning to the "main" thread from other threads.

```java
public static void main(String[] args) {
    BlockingScheduler scheduler = new BlockingScheduler();

    scheduler.execute(() -> {
        Flowable.range(1,10)
        .subscribeOn(Schedulers.io())
        .observeOn(scheduler)
        .doAfterTerminate(() -> scheduler.shutdown())
        .subscribe(v -> System.out.println(v + " on " + Thread.currentThread()));
    });
    
    System.out.println("BlockingScheduler finished");
}
```

## Custom operators and transformers

The custom transformers (to be applied with `Flowable.compose` for example), can be found in `hu.akarnokd.rxjava2.operators.FlowableTransformers` class. The custom source-like operators can be found in `hu.akarnokd.rxjava2.operators.Flowables` class. The operators and transformers for the other base
reactive classes (will) follow the usual naming scheme.

### FlowableTransformers.valve()

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

### FlowableTransformers.debounceFirst()

Debounces the upstream by taking an item and dropping subsequent items until the specified amount of time elapses after the last item, after which the process repeats.

```java
Flowable.just(0, 50, 100, 150, 400, 500, 550, 1000)
.flatMap(v -> Flowable.timer(v, TimeUnit.MILLISECONDS).map(w -> v))
.compose(FlowableTransformers.debounceFirst(200, TimeUnit.MILLISECONDS))
.test()
.awaitDone(5, TimeUnit.SECONDS)
.assertResult(0, 400, 1000);
```

### FlowableTransformers.switchFlatMap()

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

### FlowableTransformers.flatMapSync()

A bounded-concurrency `flatMap` implementation optimized for mostly non-trivial, largely synchronous sources in mind and using different tracking method and configurable merging strategy: depth-first consumes each inner source as much as possible before switching to the next; breath-first consumes one element from each source in a round-robin fashion. Overloads allow specifying the concurrency level (32 default), inner-prefetch (`Flowable.bufferSize()` default) and the merge strategy (depth-first default).

```java
Flowable.range(1, 1000)
.compose(FlowableTransformers.flatMapSync(v -> Flowable.range(1, 1000)))
.test()
.assertValueCount(1_000_000)
.assertNoErrors()
.assertComplete();
```

### FlowableTransformers.flatMapAsync()

A bounded-concurrency `flatMap` implementation taking a scheduler which is used for collecting and emitting items from the active sources and freeing up the inner sources to keep producing. It also uses a different tracking method and configurable merging strategy: depth-first consumes each inner source as much as possible before switching to the next; breath-first consumes one element from each source in a round-robin fashion. Overloads allow specifying the concurrency level (32 default), inner-prefetch (`Flowable.bufferSize()` default) and the merge strategy (depth-first default).

```java
Flowable.range(1, 1000)
.compose(FlowableTransformers.flatMapAsync(v -> Flowable.range(1, 1000), Schedulers.single()))
.test()
.awaitDone(5, TimeUnit.SECONDS)
.assertValueCount(1_000_000)
.assertNoErrors()
.assertComplete();
```

### FlowableTransformers.switchIfEmpty() & switchIfEmptyArray()

Switches to the alternatives, one after the other if the main source or the previous alternative turns out to be empty.

```java
Flowable.empty()
.compose(FlowableTransformers.switchIfEmpty(Arrays.asList(Flowable.empty(), Flowable.range(1, 5))))
.test()
.assertResult(1, 2, 3, 4, 5);

Flowable.empty()
.compose(FlowableTransformers.switchIfEmptyArray(Flowable.empty(), Flowable.range(1, 5)))
.test()
.assertResult(1, 2, 3, 4, 5);
```

### FlowableTransformers.expand()

Streams values from the main source, maps each of them onto another Publisher and recursively streams those Publisher values until all Publishers terminate.
Two recursing mode is available: breath-first will stream the main source (level 1), then the Publishers generated by its items (level 2), then the Publishers generated by the level 2
and so on; depth-first will take an item from the main source, maps it to a Publisher then takes an item from this Publisher and maps it further.

```java
Flowable.just(10)
.compose(FlowableTransformers.expand(v -> v == 0 ? Flowable.empty() : Flowable.just(v - 1)))
.test()
.assertResult(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0);
```

Depth-first example:

```java
Flowable.just(new File("."))
.compose(FlowableTransofmers.expand(file -> {
    if (file.isDirectory()) {
        File[] files = file.listFiles();
        if (files != null) {
            return Flowable.fromArray(files);
        }
    }
    return Flowable.empty();
}, ExpandStrategy.DEPTH_FIRST))
.subscribe(System.out::println);

// prints something like
// ~/git/RxJava2Extensions
// ~/git/RxJava2Extensions/src
// ~/git/RxJava2Extensions/src/main
// ~/git/RxJava2Extensions/src/main/java
// ~/git/RxJava2Extensions/src/main/java/hu
// ~/git/RxJava2Extensions/src/main/java/hu/akarnokd
// ~/git/RxJava2Extensions/src/main/java/hu/akarnokd/rxjava2
// ~/git/RxJava2Extensions/src/main/java/hu/akarnokd/rxjava2/operators
// ~/git/RxJava2Extensions/src/main/java/hu/akarnokd/rxjava2/operators/FlowableExpand.java
// ...
// ~/git/RxJava2Extensions/src/test
// ~/git/RxJava2Extensions/src/test/java
```

Breadth-first example:

```java
Flowable.just(new File("."))
.compose(FlowableTransofmers.expand(file -> {
    if (file.isDirectory()) {
        File[] files = file.listFiles();
        if (files != null) {
            return Flowable.fromArray(files);
        }
    }
    return Flowable.empty();
}, ExpandStrategy.BREATH_FIRST))
.subscribe(System.out::println);

// prints something like
// ~/git/RxJava2Extensions
// ~/git/RxJava2Extensions/src
// ~/git/RxJava2Extensions/build
// ~/git/RxJava2Extensions/gradle
// ~/git/RxJava2Extensions/HEADER
// ~/git/RxJava2Extensions/README.md
// ...
// ~/git/RxJava2Extensions/src/main
// ~/git/RxJava2Extensions/src/test
// ~/git/RxJava2Extensions/src/jmh
// ~/git/RxJava2Extensions/src/main/java
// ~/git/RxJava2Extensions/src/main/java/hu
// ~/git/RxJava2Extensions/src/main/java/hu/akarnokd
// ~/git/RxJava2Extensions/src/main/java/hu/akarnokd/rxjava2
// ~/git/RxJava2Extensions/src/main/java/hu/akarnokd/rxjava2/operators
// ~/git/RxJava2Extensions/src/main/java/hu/akarnokd/rxjava2/math
// ~/git/RxJava2Extensions/src/main/java/hu/akarnokd/rxjava2/async
// ~/git/RxJava2Extensions/src/main/java/hu/akarnokd/rxjava2/debug
// ...
// ~/git/RxJava2Extensions/src/main/java/hu/akarnokd/rxjava2/operators/FlowableExpand.java
// ~/git/RxJava2Extensions/src/main/java/hu/akarnokd/rxjava2/operators/FlowableTransformers.java
```

### FlowableTransformers.mapAsync()

This is an "asynchronous" version of the regular `map()` operator where an upstream value is mapped to a `Publisher` which
is expected to emit a single value to be the result itself or through a combiner function become the result. Only
one such `Publisher` is executed at once and the source order is kept. If the `Publisher` is empty, no value is emitted
and the sequence continues with the next upstream value. If the `Publisher` has more than one element, only the first
element is considered and the inner sequence gets cancelled after that first element.

```java
Flowable.range(1, 5)
.compose(FlowableTransformers.mapAsync(v -> 
    Flowable.just(v + 1).delay(1, TimeUnit.SECONDS)))
.test()
.awaitDone(10, TimeUnit.SECONDS)
.assertResult(2, 3, 4, 5, 6);
```

Example when using a combiner function to combine the original and the generated values:

```java
Flowable.range(1, 5)
.compose(FlowableTransformers.mapAsync(v -> 
    Flowable.just(v + 1).delay(1, TimeUnit.SECONDS)), (v, w) -> v + "-" + w)
.test()
.awaitDone(10, TimeUnit.SECONDS)
.assertResult("1-2", "2-3", "3-4", "4-5", "5-6");
```

### FlowableTransformers.filterAsync()

This is an "asynchronous" version of the regular `filter()` operator where an upstream value is mapped to a `Publisher`
which is expected to emit a single `true` or `false` that indicates the original value should go through. An empty `Publisher`
is considered to be a `false` response. If the `Publisher` has more than one element, only the first
element is considered and the inner sequence gets cancelled after that first element.

```java
Flowable.range(1, 10)
.compose(FlowableTransformers.filterAsync(v -> Flowable.just(v).delay(1, TimeUnit.SECONDS).filter(v % 2 == 0))
.test()
.awaitDone(15, TimeUnit.SECONDS)
.assertResult(2, 4, 6, 8, 10);
```

### FlowableTransformers.refCount()

Offers the option to connect after a certain amount of subscribers have subscribed and/or specify a timeout
for disconnecting the upstream if all subscribers have unsubscribed. This allows keeping the connection alive if
there is a small window where new subscribers may subscribe after the previous set has unsubscribed. Note
that if the upstream to the transformer is not a `ConnectableFlowable`, the call to the transformer method
will throw an `IllegalArgumentException`.

This example will connect only after there is a second subscriber:

```java
Flowable<Integer> source = Flowable.range(1, 5)
    .publish()
    .compose(FlowableTransformers.refCount(2))
    ;

TestSubscriber<Integer> ts1 = source.test();

ts1.assertEmpty();

TestSubscriber<Integer> ts2 = source.test();

ts1.assertResult(1, 2, 3, 4, 5);
ts2.assertResult(1, 2, 3, 4, 5);
```

This example will disconnect only after a second:

```java
PublishProcessor<Integer> pp = PublishProcessor.create();

Flowable<Integer> source = pp
    .publish()
    .compose(FlowableTransformers.refCount(1, TimeUnit.SECONDS, Schedulers.single()));

assertFalse(pp.hasSubscribers());

TestSubscriber<Integer> ts = source.test();

assertTrue(pp.hasSubscribers());

ts.cancel();

assertTrue(pp.hasSubscribers());

Thread.sleep(1200);

assertFalse(pp.hasSubscribers());
```

### Flowables.zipLatest()

Zips the latest values from multiple sources and calls a combiner function for them.
If one of the sources is faster then the others, its unconsumed values will be overwritten by newer
values. 
Unlike `combineLatest`, source items are participating in the combination at most once; i.e., the 
operator emits only if all sources have produced an item.
The emission speed of this operator is determined by the slowest emitting source and the speed of the downstream consumer.
There are several overloads available: methods taking 2-4 sources and the respective combiner functions, a method taking a
varargs of sources and a method taking an `Iterable` of source `Publisher`s.
The operator supports combining and scheduling the emission of the result via a custom `Scheduler`, thus
allows avoiding the buffering effects of the `observeOn` operator.
The operator terminates if any of the sources runs out of items and terminates by itself. 
The operator works with asynchronous sources the best; synchronous sources may get consumed fully
in order they appear among the parameters and possibly never emit more than one combined result even
if the last source has more than one item.

```java
TestScheduler scheduler = new TestScheduler();

TestSubscriber<String> ts = Flowables.zipLatest(toString,
        Flowable.intervalRange(1, 6, 99, 100, TimeUnit.MILLISECONDS, scheduler),
        Flowable.intervalRange(4, 3, 200, 200, TimeUnit.MILLISECONDS, scheduler)
)
.test();

scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

ts.assertValue("[2, 4]");

scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

ts.assertValues("[2, 4]", "[4, 5]");

scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

ts.assertResult("[2, 4]", "[4, 5]", "[6, 6]");
```

### FlowableTransformers.coalesce()

Coalesces items from upstream into a container via a consumer and emits the container if
there is a downstream demand, otherwise it keeps coalescing into the same container. Note
that the operator keeps an internal unbounded buffer to collect up upstream values before
the coalescing happens and thus a computational heavy downstream hogging the emission thread
may lead to excessive memory usage. It is recommended to use `observeOn` in this case.

```java
Flowable.range(1, 5)
.compose(FlowableTransformers.coalesce(ArrayList::new, (a, b) -> a.add(b))
.test(1)
.assertValue(Arrays.asList(1))
.requestMore(1)
.assertResult(Arrays.asList(1), Arrays.asList(2, 3, 4, 5));
```

### FlowableTransformers.windowWhile

Emits elements into a Flowable window while the given predicate returns true. 
If the predicate returns false, a new Flowable window is emitted.

```java
Flowable.just("1", "2", "#", "3", "#", "4", "#")
.compose(FlowableTransformers.windowWhile(v -> !"#".equals(v)))
.flatMap(v -> v.toList())
.test()
.assertResult(
    Arrays.asList("1", "2"),
    Arrays.asList("#", "3"),
    Arrays.asList("#", "4"),
    Arrays.asList("#")
);
```

### FlowableTransformers.windowUntil

Emits elements into a Flowable window until the given predicate returns true 
at which point a new Flowable window is emitted.

```java
Flowable.just("1", "2", "#", "3", "#", "4", "#")
.compose(FlowableTransformers.windowUntil(v -> "#".equals(v)))
.flatMap(v -> v.toList())
.test()
.assertResult(
    Arrays.asList("1", "2", "#"),
    Arrays.asList("3", "#"),
    Arrays.asList("4", "#")
);
```

### FlowableTransformers.windowSplit

Emits elements into a Flowable window until the given predicate returns true at which
point a new Flowable window is emitted; the particular item will be dropped.

```java
Flowable.just("1", "2", "#", "3", "#", "4", "#")
.compose(FlowableTransformers.windowSplit(v -> "#".equals(v)))
.flatMap(v -> v.toList())
.test()
.assertResult(
    Arrays.asList("1", "2"),
    Arrays.asList("3"),
    Arrays.asList("4")
);
```

### FlowableTransformers.indexOf

Returns the first index of an element that matches a predicate or -1L if no elements match.
(Also available for `Observable`s as `ObservableTransformers.indexOf()`.)

```java
Flowable.range(1, 5)
.compose(FlowableTransformers.indexOf(v -> v == 5))
.test()
.assertResult(4);
```

### FlowableTransformers.requestObserveOn

Requests items one-by-one from the upstream on the specified `Scheduler` and emits the received items from the
given `Scheduler` as well in a fashion that allows tasks to be interleaved on the target `Scheduler` (aka "fair" use)
on a much more granular basis than `Flowable.observeOn`.

```java
Flowable.range(1, 5)
.compose(FlowableTransformers.requestObserveOn(Schedulers.single()))
.test()
.awaitDone(5, TimeUnit.SECONDS)
.assertResult(1, 2, 3, 4, 5)
;
```

### Flowabletransformers.requestSample

Periodically (and after an optional initial delay) issues a single `request(1)` to the upstream and forwards the
items to a downstream that must be ready to receive them.

```java
Flowables.repeatCallable(() -> 1)
.compose(FlowableTransformers.requestSample(1, TimeUnit.SECONDS, Schedulers.single()))
.take(5)
.test()
.awaitDone(7, TimeUnit.SECONDS)
.assertResult(1, 1, 1, 1, 1);
```

The sampling can be of a more complex pattern by using another `Publisher` as the indicator when to request:

```java
Flowables.repeatCallable(() -> 1)
.compose(FlowableTransformers.requestSample(
    Flowable.fromArray(100, 500, 1000, 2000, 5000)
    .concatMap(delay -> Flowable.timer(delay, TimeUnit.MILLISECONDS))
))
.take(5)
.test()
.awaitDone(10, TimeUnit.SECONDS)
.assertResult(1, 1, 1, 1, 1);
)
```

## Custom parallel operators and transformers

### ParallelTransformers.sumX()

Sums the numerical values on each rail as integer, long or double.

```java
Flowable.range(1, 5)
.parallel(1)
.compose(ParallelTransformers.<Integer>sumInteger())
.sequential()
.test()
.assertResult(15);

Flowable.range(1, 5)
.parallel(1)
.compose(ParallelTransformers.<Integer>sumLong())
.sequential()
.test()
.assertResult(15L);

Flowable.range(1, 5)
.parallel(1)
.compose(ParallelTransformers.<Integer>sumDouble())
.sequential()
.test()
.assertResult(15d);
```

### ParallelTransformers.orderedMerge()

Merges the source ParallelFlowable rails in an ordered fashion picking the smallest of the available value from
them (determined by their natural order or via a `Comparator`). The operator supports delaying error and setting
the internal prefetch amount.

```java
ParallelFlowable.fromArray(
    Flowable.just(1, 3, 5, 7),
    Flowable.just(0, 2, 4, 6, 8, 10)
)
.to(p -> ParallelTransformers.orderedMerge(p, (a, b) -> a.compareTo(b)))
.test()
.assertResult(0, 1, 2, 3, 4, 5, 6, 7, 8, 10);
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

## Custom consumers

The utility classes can be found in `hu.akarnokd.rxjava2.consumers` package.


### FlowableConsumers

#### `subscribeAutoRelease`

Wraps the given `onXXX` callbacks into a `Disposable` `Subscriber`,
adds it to the given `CompositeDisposable` and ensures, that if the upstream
completes or this particlular `Disposable` is disposed, the `Subscriber` is removed
from the given composite.

The Subscriber will be removed *after* the callback for the terminal event has been invoked.

```java
CompositeDisposable composite = new CompositeDisposable();

Disposable d = FlowableConsumers.subscribeAutoRelease(
    Flowable.just(1), composite,
    System.out::println, Throwable::printStackTrace, () -> System.out.println("Done")
);

assertEquals(0, composite.size());

// --------------------------

Disposable d2 = FlowableConsumers.subscribeAutoRelease(
    Flowable.never(), composite,
    System.out::println, Throwable::printStackTrace, () -> System.out.println("Done")
);

assertEquals(1, composite.size());

d2.dispose();

assertEquals(0, composite.size());
```

The Subscriber will be removed after the callback for the terminal event has been invoked.

### ObservableConsumers

- `subscribeAutoRelease`: Similar to `FlowableConsumers.subscribeAutoRelease()` but targeting `Observable`s and `Observer`s-like
consumers.

### SingleConsumers

- `subscribeAutoRelease`: Similar to `FlowableConsumers.subscribeAutoRelease()` but targeting `Single`s and `SingleObserver`s-like
consumers.

### MaybeConsumers

- `subscribeAutoRelease`: Similar to `FlowableConsumers.subscribeAutoRelease()` but targeting `Maybe`s and `MaybeObserver`s-like
consumers.

### CompletableConsumers

- `subscribeAutoRelease`: Similar to `FlowableConsumers.subscribeAutoRelease()` but targeting `Completable`s and `CompletableObserver`s-like
consumers.
