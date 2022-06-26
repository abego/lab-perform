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

#### Approach 1: Use ObjectStreams for Serialization

The first attempt to use serialization to speedup `getMethod` uses the standard
Java ObjectStreams (`ObjectOutputStream`, `ObjectInputStream`). When the data
was loaded from disk the memoization data was immediately restored completely,
especially the reference to the Java `Method` to return for each used (Class, 
Selector) combination was restored.

- 36 ms - `perform` with memoization (`perform_withMemoization_smallSampleMultipleTimes`).
- 68 ms - `perform` with memoization and saving the memoization data to a file (`perform_withMemoization_and_saveMethods_smallSampleMultipleTimes`).
- 72 ms - `perform` with memoization data loaded from file (`perform_withMemoization_andLoadedFromFile_smallSampleMultipleTimes`)

These numbers show no improvement when loading the cache from some serialization.

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
