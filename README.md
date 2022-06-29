perform
=======

A playground to evaluate how a "perform" mechanism, as known
from Smalltalk, could efficiently be implemented in Java.

## getMethod

One focus is on the internal `getMethod` code, responsible
for providing the Java method corresponding to a given
class and "message"(/"selector"). 

### Motivation

The `getMethod` code has multiple responsibilities:

- convert "Smalltalk" selectors into Java-conform method names
  (think of unary selector like `+` or `<<`, or keyword messages like 
  `add:`or `insert:at:`)
- find the Java method implementing the requested selector, taking care of the 
  class hierarchy of the receiver, and possibly methods added through 
  "class extensions".
- provide enough information for the `perform` implementation to support
  the `doesNotUnderstand` feature.

Typically, this wide range of requirements makes the `getMethod` the most 
cpu-intensive part of the `perform` operation (beside the actual code being
invoked by `perform`). Thus, having as little time spend in `getMethod` is a 
major goal when optimizing the `perform` operation.

### Memoization

A first idea to speed up `getMethod` is using 
[memoization](https://en.wikipedia.org/wiki/Memoization).

When using memoization the "original" code of the `getMethod` implementation is
only executed once per each (Class, Selector) combination. Once the Method 
corresponding to this combination is calculated initially all subsequent 
requests for that (Class, Selector) combination will reuse the already 
calculated Method, retrieving it from some internal cache and not recalculating
it again.

This approach requires some additional memory (for the cache), but reduces
the accumulated `getMethod` time when methods are called repeatedly.

#### Results using Memoization

JUnit tests are included to verify the assumption memoization improves the
overall `perform` performance. Therefore, a sample test code using `perform`
calls is executed multiple times, both in the "without memoization" and the
"with memoization" scenario.

_(General remark regarding performance measurements: To get the durations
the test cases are repeated 3 times, started individually from the IDE, and
the lowest number is used. Results in "real world" may vary.)_


__Results: Run Small Sample multiple times__
- 68 ms - `perform` without memoization (`perform_withoutMemoization_smallSampleMultipleTimes`).
- 36 ms - `perform` with memoization (`perform_withMemoization_smallSampleMultipleTimes`).

_(approx. 80 micro seconds per perform, speedup factor nearly 1.9. An "extra"
delay of 40 micro seconds was added to our original `getMethod` 
implementation to measure these numbers.)_

The numbers show the speed improvement when using memoization. However, the 
absolute numbers and the ratio heavily depends on the specific implementation
of the "original" getMethod, and the number of "repeated" method calls. E.g.
if the original `getMethod` is slower the benefits of memoization becomes even 
more convincing. So measuring the numbers in the real systems is essential.

### Serialize Memoization Data

When using memoization the original, time-intensive `getMethod` routine must
be executed once for every (Class, Selector) combination in an application run.

If we save the memoization data ("the cache") to disk, e.g. at application end
and reload it the next time we run the application we can avoid even this one 
call per (Class, Selector) combination.

To make serialization of memoization data an improvement it is important that
loading the memoization data and initially providing the Method through the 
data loaded from the disk takes less time than the memoization approach without
serialization. Again, this heavily depends on the original `getMethod` routine,
the message "reuse" frequency, but also the way, the serialization and 
de-serialization is implemented.

#### Direct Loading of Serialized Memoization Data

We use the standard Java ObjectStreams (`ObjectOutputStream`, 
`ObjectInputStream`) for serialization of `getMethod` memoization data. After
loading the complete memoization data is restored, ready to use by the 
application.

Using this approach on our initial test sample shows no significant improvement 
when comparing the memoization performance of the "simple" and "loading" 
approach:

__Results: Run Small Sample multiple times__
- 36 ms - `perform` with memoization (`perform_withMemoization_smallSampleMultipleTimes`).
- 73 ms - `perform` with memoization and saving the memoization data to a file (`perform_withMemoization_and_saveMethods_smallSampleMultipleTimes`).
- 68 ms - `perform` with memoization data loaded from file (`perform_withMemoization_andLoadedFromFile_smallSampleMultipleTimes`)

However, this changes when we switch to a larger test sample (5000 classes, 
with 100 methods each).

As before, "simple" memoization brings improvements for this larger test sample
as soon as methods are called more than once: 

__Results: Run Big Sample _twice___
- 17.4 s - `perform` without memoization (`perform_withoutMemoization_bigSampleTwice`).
- 11.0 s - `perform` with memoization (`perform_withMemoization_bigSampleTwice`).

In addition, with this larger test sample the serialization even
makes the initial `perform` call faster than without memoization, by a 
factor of approx. 2.5 in our example: 

__Test: Run Big Sample _once___
- 10.8 s - `perform` without memoization (`perform_withMemoization_bigSample`).
- 4.3 s - `perform` with memoization data loaded from file (`perform_withMemoization_afterLoadMethods_bigSample`)

#### Lazy Loading of Serialized Memoization Data

One disadvantage of the memoization serialization approach compared to the 
"just memoize" approach is we introduce an extra delay at the start of the 
application when we load the cache from disk. As the numbers from above show we
can actually be faster than in the "just memoize" approach, however we spend 
this time in the startup phase of the application, making the application seem 
to be "slower". In the "just memoize" approach the time for the various 
"initial" getMethod calls is distributed over the whole application execution.

One way to speed up loading the serialized memoization data is to postpone 
time intensive parts of this load process "to later". 

When profiling the load phase it became obvious that a lot of time is spend by 
resolving/restoring the actual objects from the serialized data. If we can
postpone this expensive resolution to later loading should become faster. We 
just need to make sure we save enough information to support this 
"lazy loading". 

__General remark regarding lazy loading:__ Lazy loading may increase the overall 
application execution time as some cpu-time is moved to some later point in 
time, requiring some extra work to ensure it is executed later. This is 
especially true when all methods of the method map are used at runtime. On the 
other side lazy loading may reduce the overall application execution time when 
not all methods stored in the method map are actually used at runtime. In the 
non-lazy case also these "unused" methods are installed, hence waste some time.

__Implementation Note__: When implementing lazy loading it turned out that one
must take special care to choose a file format allowing for fast 
de-serialization into an intermediate state that can later be "resolved" into 
the `getMethod` memoization data actually needed by the application. 
Especially one should avoid resolving `Class` objects at load time.
For this reason `Class` objects are serialized just by their name. When a 
`Class` is actually needed (for the type of the receiver or the parameter types)
it can be constructed based on the type name.

Using lazy loading on the larger test sample reduces the initial load time
significantly, by a factor of 12:

__Results: Load Big Sample Method Map (5000 classes, each with 100 methods)__
- 3450 ms - loadMethods (`loadMethods_bigSample`).
- 271 ms - loadMethodsLazy (`loadMethodsLazy_bigSample`).

As already mentioned above "lazy loading" may mean, one may "just" move some 
cpu-time to later. With our larger test sample we can therefore see roughly
equals times when calling perform on every method, no matter if we use 
`loadMethods` or `loadMethodsLazy`:

__Results: perform-call every method in Big Sample after loading method map from disk__
- 4.4 s - loadMethods - `perform_withMemoization_afterLoadMethods_bigSample`
- 4.3 s - loadMethodsLazy - `perform_withMemoization_afterLoadMethodsLazy_bigSample`

But if one calls less different methods the difference between the total time 
using `loadMethods` vs. `loadMethodsLazy` can be quite large, e.g. a factor 12
as mentioned above.

## Overall Application Flow

Using memoization and serialization one can now use the following application flow:
- on application start the memoization data is loaded
- from now on all perform/getMethod calls will use the data from the memoization and never call the original 
  (expensive) getMethod code.

Preparing the serialization (i.e. writing the file with the method map) can be done in various ways:
- write the data after an application run (will only contain the methods actually used in that run).
- write the data after building the memoization data synthetically by adding all possible combiniations of classes and
  methods.

## Existing Caches

With the getMethod memoization is place it may be possible to remove some other "caches" that are used in the 
original getMethod workflow.
