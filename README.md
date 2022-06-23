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

Different approaches are compared by running a standard test
multiple times, with changing condition (Times as reported 
by the JUnit runner):

- 588 ms - `perform` without memoization. 
- 86 ms - `perform` with memoization.
- 74 ms - `perform` with memoization and saving the memoization data to a file ("serialization").
- 49 ms - `perform` with memoization data loaded from file (i.e. no internal "expensiveGetMethod" calls)
