package org.abego.lab.perform.core;

import org.abego.lab.perform.bigsample.BigSample;
import org.abego.lab.perform.bigsample.C0;
import org.abego.lab.perform.sample.A;
import org.abego.lab.perform.sample.B;
import org.abego.lab.perform.sample.C;
import org.abego.lab.perform.sample.D;
import org.abego.lab.perform.sample.E;
import org.abego.lab.perform.typesample.UsingVariousTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PerformerTest {
    private static final String SMALL_TEST_SAMPLE_METHOD_MAP_FILE_PATH = "methods-smallsample";
    private static final String BIG_TEST_SAMPLE_METHOD_MAP_FILE_PATH = "methods-bigsample";
    private static final String VARIOUS_TYPES_METHOD_MAP_FILE_PATH = "methods-varioustypes";
    private static final String REMOVED_CLASS_METHOD_MAP_FILE_PATH = "methods-removedclass";
    private static final String METHOD_MAP_FILE_NAME = "methodMap";
    private static final long EXTRA_ORIGINAL_GET_METHOD_DELAY_MICROS = 40;

    private static final int TEST_REPEAT_COUNT = 20;
    private static final int LOAD_REPEAT_COUNT = 100;
    private static final int DELAY_MICROS_FOR_BIG_SAMPLE = 10;

    private static String pathOfMethodMapFileInDir(File directory) {
        return new File(directory, METHOD_MAP_FILE_NAME).getAbsolutePath();
    }

    private static void setDelayForBigSample() {
        Performer.setExtraDelayInOriginalGetMethodInMicros(DELAY_MICROS_FOR_BIG_SAMPLE);
    }

    @BeforeEach
    void setUp() {
        Performer.reset();
        Performer.setExtraDelayInOriginalGetMethodInMicros(EXTRA_ORIGINAL_GET_METHOD_DELAY_MICROS);
    }

    // region test sample file generation

    /**
     * Not a real test, but generates the method mop serialization file used
     * in other tests.
     * <p>
     * Re-run this code when the file format of the method map files changed.
     */
    @Test
    @Disabled
    void generateSampleSerializationFiles() throws IOException {
        saveMethodMapForRemovedClass();
        saveMethodMapForSmallSample();
        saveMethodMapForVariousTypesSample();
        saveMethodMapForBigSample();

        fail("Method map files are generated. Copy them to their final destination, if necessary.\n" +
                "Also comment out the 'RemovedClass' so it is not found in its test.");
    }

    private void saveMethodMapForVariousTypesSample() throws IOException {
        Performer.reset();
        Performer.setMemoizationEnabled(true);
        runVariousTypesCode();
        Performer.saveMethods(VARIOUS_TYPES_METHOD_MAP_FILE_PATH);
    }

    private void saveMethodMapForSmallSample() throws IOException {
        Performer.reset();
        Performer.setMemoizationEnabled(true);
        runSmallTestSample();
        Performer.saveMethods(SMALL_TEST_SAMPLE_METHOD_MAP_FILE_PATH);
    }

    private void saveMethodMapForBigSample() throws IOException {
        Performer.setMemoizationEnabled(true);
        runBigSample();
        Performer.saveMethods(BIG_TEST_SAMPLE_METHOD_MAP_FILE_PATH);
    }

    private void saveMethodMapForRemovedClass() throws IOException {
        Performer.reset();
        Performer.setMemoizationEnabled(true);
        fail("Uncomment the next line (and comment out this line) to generate " + REMOVED_CLASS_METHOD_MAP_FILE_PATH);
        // Performer.perform(new RemovedClass(), "foo");
        Performer.saveMethods(REMOVED_CLASS_METHOD_MAP_FILE_PATH);
    }

    // DONT REMOVE THIS BLOCK. It is temporarily needed to generate test data
    // (see saveMethodMapForRemovedClass())
    //
    //    private static class RemovedClass {
    //        public void foo() {}
    //    }

    // endregion

    @Test
    void perform_withoutMemoization_smallSampleMultipleTimes() {
        Performer.setMemoizationEnabled(false);
        long startTime = System.nanoTime();

        runSmallTestSampleMultipleTimes();

        printDuration(startTime, System.nanoTime(), "perform_withoutMemoization_smallSampleMultipleTimes");
    }

    private void printDuration(long startTime, long endTime, String testName) {
        System.out.println(testName + ": " + ((endTime - startTime + 500) / 1000) / 1000.0 + " ms");
    }

    @Test
    void perform_withoutMemoization_bigSample() {
        setDelayForBigSample();
        Performer.setMemoizationEnabled(false);
        long startTime = System.nanoTime();

        runBigSample();

        printDuration(startTime, System.nanoTime(), "perform_withoutMemoization_bigSample");
    }

    @Test
    void perform_withoutMemoization_bigSampleTwice() {
        setDelayForBigSample();
        Performer.setMemoizationEnabled(false);
        long startTime = System.nanoTime();

        runBigSample();
        runBigSample();

        printDuration(startTime, System.nanoTime(), "perform_withoutMemoization_bigSampleTwice");
    }

    @Test
    void perform_withMemoization_smallSampleMultipleTimes() {
        Performer.setMemoizationEnabled(true);
        long startTime = System.nanoTime();

        runSmallTestSampleMultipleTimes();

        printDuration(startTime, System.nanoTime(), "perform_withMemoization_smallSampleMultipleTimes");
    }

    @Test
    void perform_withMemoization_bigSample() {
        setDelayForBigSample();
        Performer.setMemoizationEnabled(true);
        long startTime = System.nanoTime();

        runBigSample();

        printDuration(startTime, System.nanoTime(), "perform_withMemoization_bigSample");
    }

    @Test
    void perform_withMemoization_bigSampleTwice() {
        setDelayForBigSample();
        Performer.setMemoizationEnabled(true);
        long startTime = System.nanoTime();

        runBigSample();
        runBigSample();

        printDuration(startTime, System.nanoTime(), "perform_withMemoization_bigSampleTwice");
    }

    @Test
    void perform_withMemoization_and_saveMethods_smallSampleMultipleTimes(
            @TempDir File tempDir) throws IOException {

        Performer.setMemoizationEnabled(true);
        long startTime = System.nanoTime();

        runSmallTestSampleMultipleTimes();

        Performer.saveMethods(pathOfMethodMapFileInDir(tempDir));

        printDuration(startTime, System.nanoTime(), "perform_withMemoization_and_saveMethods_smallSampleMultipleTimes");
    }

    @Test
    void perform_withMemoization_afterLoadMethods_smallSampleMultipleTimes() throws
            IOException, ClassNotFoundException, NoSuchMethodException {
        long startTime = System.nanoTime();

        Performer.loadMethods(SMALL_TEST_SAMPLE_METHOD_MAP_FILE_PATH);

        runSmallTestSampleMultipleTimes();

        printDuration(startTime, System.nanoTime(), "perform_withMemoization_andLoadedFromFile_smallSampleMultipleTimes");
    }

    @Test
    void perform_withMemoization_afterLoadMethodsLazy_smallSampleMultipleTimes() throws
            IOException, ClassNotFoundException, NoSuchMethodException {
        long startTime = System.nanoTime();

        Performer.loadMethodsLazy(SMALL_TEST_SAMPLE_METHOD_MAP_FILE_PATH);

        runSmallTestSampleMultipleTimes();

        printDuration(startTime, System.nanoTime(), "perform_withMemoization_andLoadedFromFile_smallSampleMultipleTimes");
    }

    @Test
    void perform_withMemoization_afterLoadMethods_bigSample() throws
            IOException, ClassNotFoundException, NoSuchMethodException {
        setDelayForBigSample();
        long startTime = System.nanoTime();

        Performer.loadMethods(BIG_TEST_SAMPLE_METHOD_MAP_FILE_PATH);

        runBigSample();

        printDuration(startTime, System.nanoTime(), "perform_withMemoization_afterLoadMethods_bigSample");
    }

    @Test
    void perform_withMemoization_afterLoadMethodsLazy_bigSample() throws
            IOException, ClassNotFoundException, NoSuchMethodException {
        setDelayForBigSample();
        long startTime = System.nanoTime();

        Performer.loadMethodsLazy(BIG_TEST_SAMPLE_METHOD_MAP_FILE_PATH);

        runBigSample();

        printDuration(startTime, System.nanoTime(), "perform_withMemoization_afterLoadMethodsLazy_bigSample");
    }

    @Test
    void loadMethods_multipleTime_smallSample() throws IOException, ClassNotFoundException, NoSuchMethodException {
        Performer.setMemoizationEnabled(true);
        long startTime = System.nanoTime();

        for (int i = 0; i < LOAD_REPEAT_COUNT; i++) {
            Performer.loadMethods(SMALL_TEST_SAMPLE_METHOD_MAP_FILE_PATH);
        }

        assertEquals("an A", Performer.perform(new A(), "toString"));

        printDuration(startTime, System.nanoTime(), "loadMethods_multipleTime_smallSample");
    }

    @Test
    void loadMethods_bigSample() throws IOException, ClassNotFoundException, NoSuchMethodException {
        setDelayForBigSample();
        Performer.setMemoizationEnabled(true);
        long startTime = System.nanoTime();

        Performer.loadMethods(BIG_TEST_SAMPLE_METHOD_MAP_FILE_PATH);

        assertEquals("C0#m0()", Performer.perform(new C0(), "m0"));

        printDuration(startTime, System.nanoTime(), "loadMethods_bigSample");
    }

    @Test
    void loadMethodsLazy_multipleTime_smallSample() throws IOException, ClassNotFoundException, NoSuchMethodException {
        Performer.setMemoizationEnabled(true);
        long startTime = System.nanoTime();

        for (int i = 0; i < LOAD_REPEAT_COUNT; i++) {
            Performer.loadMethodsLazy(SMALL_TEST_SAMPLE_METHOD_MAP_FILE_PATH);
        }

        assertEquals("an A", Performer.perform(new A(), "toString"));

        printDuration(startTime, System.nanoTime(), "loadMethodsLazy_multipleTime_smallSample");
    }

    @Test
    void loadMethodsLazy_bigSample() throws IOException, ClassNotFoundException, NoSuchMethodException {
        setDelayForBigSample();
        Performer.setMemoizationEnabled(true);
        long startTime = System.nanoTime();

        Performer.loadMethodsLazy(BIG_TEST_SAMPLE_METHOD_MAP_FILE_PATH);

        assertEquals("C0#m0()", Performer.perform(new C0(), "m0"));

        printDuration(startTime, System.nanoTime(), "loadMethodsLazy_bigSample");
    }

    @Test
    void perform_failingMethodThrowsInvocationTargetException() {
        A a = new A();

        PerformException e = assertThrows(PerformException.class,
                () -> Performer.perform(a, "throwIllegalAccessException"));
        assertEquals("java.lang.reflect.InvocationTargetException", e.getMessage());
    }

    @Test
    void saveMethods_requiresMemoizationEnabled(@TempDir File tempDir) {
        String filePath = pathOfMethodMapFileInDir(tempDir);
        PerformException e = assertThrows(PerformException.class,
                () -> Performer.saveMethods(filePath));

        assertEquals("Must enable memoization to save methods.", e.getMessage());
    }

    /**
     * Check if a methodMap containing a combination of
     * Method, NoSuchMethod AND MethodLocator objects is correctly stored
     * and loaded
     */
    @Test
    void savingMixOfMethodAndNoSuchMethodAndMethodLocatorObjectsWorks(@TempDir File tempDir)
            throws IOException, ClassNotFoundException, NoSuchMethodException {

        Performer.setMemoizationEnabled(true);

        // load the method map lazy. It will contain MethodLocator values.
        Performer.loadMethodsLazy(SMALL_TEST_SAMPLE_METHOD_MAP_FILE_PATH);

        // Run one method. This will replace the corresponding
        // MethodLocator object with its Method object
        A a = new A();
        Performer.perform(a, "toString");

        // save the method map and load it.
        String methodMapFilePath = pathOfMethodMapFileInDir(tempDir);
        Performer.saveMethods(methodMapFilePath);
        Performer.loadMethods(methodMapFilePath);

        // make sure the tests run with the newly loaded methodMap
        runSmallTestSample();
    }

    @Test
    void testVariousTypes() throws IOException, ClassNotFoundException, NoSuchMethodException {
        Performer.setMemoizationEnabled(true);
        Performer.loadMethodsLazy(VARIOUS_TYPES_METHOD_MAP_FILE_PATH);
        runVariousTypesCode();
    }

    private void runVariousTypesCode() {
        UsingVariousTypes testee = new UsingVariousTypes();

        byte aByte = 123;
        assertEquals("byte: 123", Performer.perform(testee, "byteToString", aByte));

        short aShort = 1234;
        assertEquals("short: 1234", Performer.perform(testee, "shortToString", aShort));

        int anInt = 12345;
        assertEquals("int: 12345", Performer.perform(testee, "intToString", anInt));

        long aLong = 123456;
        assertEquals("long: 123456", Performer.perform(testee, "longToString", aLong));

        float aFloat = 123456.7f;
        assertEquals("float: 123456.7", Performer.perform(testee, "floatToString", aFloat));

        double aDouble = 123456.78;
        assertEquals("double: 123456.78", Performer.perform(testee, "doubleToString", aDouble));

        char aChar = 'Q';
        assertEquals("char: Q", Performer.perform(testee, "charToString", aChar));

        boolean aBoolean = true;
        assertEquals("boolean: true", Performer.perform(testee, "booleanToString", aBoolean));

        long[] longArray = new long[]{1, 4, 7};
        assertEquals("1\n4\n7\n", Performer.perform(testee, "longArrayToString", (Object) longArray));

        long[][] longArrayArray = new long[][]{new long[]{1, 4, 7}, new long[]{1, 2, 6}, new long[]{4, 9, 8}};
        assertEquals("" +
                        "1 4 7 \n" +
                        "1 2 6 \n" +
                        "4 9 8 \n",
                Performer.perform(testee, "longArrayArrayToString", (Object) longArrayArray));

        UsingVariousTypes.Foo[] foos = new UsingVariousTypes.Foo[]{
                new UsingVariousTypes.Foo("a", 1),
                new UsingVariousTypes.Foo("b", 12),
                new UsingVariousTypes.Foo("c", 123),
        };
        assertEquals("" +
                "Foo{text='a', value=1}\n" +
                "Foo{text='b', value=12}\n" +
                "Foo{text='c', value=123}\n", Performer.perform(testee, "fooArrayToString", (Object) foos));
    }

    private void runBigSample() {
        BigSample.main(new String[0]);
    }

    private void runSmallTestSampleMultipleTimes() {
        for (int i = 0; i < TEST_REPEAT_COUNT; i++) {
            runSmallTestSample();
        }
    }

    private void runSmallTestSample() {
        A a = new A();
        B b = new B();
        C c = new C();
        D d = new D();
        E e = new E();

        // method defined in each class ("toString")
        assertEquals("an A", Performer.perform(a, "toString"));
        assertEquals("a B", Performer.perform(b, "toString"));
        assertEquals("a C", Performer.perform(c, "toString"));
        assertEquals("a D", Performer.perform(d, "toString"));
        assertEquals("an E", Performer.perform(e, "toString"));

        // method defined in base class "A", no overrides
        assertEquals("only in A", Performer.perform(a, "onlyInA"));
        assertEquals("only in A", Performer.perform(b, "onlyInA"));
        assertEquals("only in A", Performer.perform(c, "onlyInA"));
        assertEquals("only in A", Performer.perform(d, "onlyInA"));
        assertEquals("only in A", Performer.perform(e, "onlyInA"));

        // method defined in subclass "C", no overload
        assertThrows(UnsupportedOperationException.class, () -> Performer.perform(a, "onlyInC"));
        assertThrows(UnsupportedOperationException.class, () -> Performer.perform(b, "onlyInC"));
        assertEquals("only in C", Performer.perform(c, "onlyInC"));
        assertEquals("only in C", Performer.perform(d, "onlyInC"));
        assertEquals("only in C", Performer.perform(e, "onlyInC"));

        // multiple method (re-)definitions
        assertThrows(UnsupportedOperationException.class, () -> Performer.perform(a, "inCAndE"));
        assertThrows(UnsupportedOperationException.class, () -> Performer.perform(b, "inCAndE"));
        assertEquals("C: in C (& E)", Performer.perform(c, "inCAndE"));
        assertEquals("C: in C (& E)", Performer.perform(d, "inCAndE"));
        assertEquals("E: in C (& E)", Performer.perform(e, "inCAndE"));

        // method with a non-identifier method name ("+")
        assertEquals(3, Performer.perform(c, "+", 1, 2));

        // undefined method ("does not understand")
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> Performer.perform(a, "foo"));

        assertEquals("org.abego.lab.perform.sample.A does not understand 'foo'", ex.getMessage());
    }

    @Test
    void removedClassesAreReported() throws IOException, ClassNotFoundException, NoSuchMethodException {
        Performer.setMemoizationEnabled(false);

        PerformException e = assertThrows(PerformException.class, () ->
                Performer.loadMethods(REMOVED_CLASS_METHOD_MAP_FILE_PATH));

        assertEquals(
                "Error when looking for type org.abego.lab.perform.core.PerformerTest$RemovedClass",e.getMessage());
    }


}
