perform
=======

A playground to evaluate how a "perform" mechanism, as known
from Smalltalk, could efficiently be implemented in Java.

## getMethod

One focus is on the internal `getMethod` code, responsible
for providing the Java method corresponding to a given
class and "message"(/"selector"). A first idea is to use
[memoization](https://en.wikipedia.org/wiki/Memoization) to
speedup `getMethod`.

### First Performance results

Different approaches are compared by running a standard test
multiple times, with changing condition (Times as reported 
by the JUnit runner):

- 588 ms - `perform` without memoization. 
- 86 ms - `perform` with memoization.
- 74 ms - `perform` with memoization and saving the memoization data to a file ("serialization").
- 49 ms - `perform` with memoization data loaded from file (i.e. no internal "expensiveGetMethod" calls)

(Note: to simulate the "slow" original getMethod the implementation of "originalGetMethod" add a synthetic 
delay of 1 ms to each call. For more realistic results one needs to use the "real" original getMethod code.)

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
