package org.abego.lab.perform.core;

import org.abego.lab.perform.bigsample.BigSample;
import org.abego.lab.perform.bigsample.C0;
import org.abego.lab.perform.sample.A;
import org.abego.lab.perform.sample.B;
import org.abego.lab.perform.sample.C;
import org.abego.lab.perform.sample.D;
import org.abego.lab.perform.sample.E;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PerformerTest {
    private static final String SMALL_TEST_SAMPLE_METHOD_MAP_FILE_PATH = "methods-smallsample";
    private static final String BIG_TEST_SAMPLE_METHOD_MAP_FILE_PATH = "methods-bigsample";
    private static final String METHOD_MAP_FILE_NAME = "methodMap";
    private static final String METHOD_MAP_DUMP_FILE_NAME = "methodMap-dump.csv";
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
        Performer.setMemoizationEnabled(false);
        Performer.setExtraDelayInOriginalGetMethodInMicros(EXTRA_ORIGINAL_GET_METHOD_DELAY_MICROS);
    }

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
    void perform_withMemoization_and_saveMethods_bigSample(
            @TempDir File tempDir) throws IOException {
        setDelayForBigSample();
        Performer.setMemoizationEnabled(true);
        long startTime = System.nanoTime();

        runBigSample();

        Performer.saveMethods(pathOfMethodMapFileInDir(tempDir));

        printDuration(startTime, System.nanoTime(), "perform_withMemoization_and_saveMethods_bigSample");
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
    void perform_withMemoization_afterLoadMethods_bigSample() throws
            IOException, ClassNotFoundException, NoSuchMethodException {
        setDelayForBigSample();
        long startTime = System.nanoTime();

        Performer.loadMethods(BIG_TEST_SAMPLE_METHOD_MAP_FILE_PATH);

        runBigSample();

        printDuration(startTime, System.nanoTime(), "perform_withMemoization_afterLoadMethods_bigSample");
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
        IllegalStateException e = assertThrows(IllegalStateException.class,
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
    void dumpMethods_Writer() throws IOException {
        Performer.setMemoizationEnabled(true);
        runSmallTestSample();
        StringWriter sw = new StringWriter();

        Performer.dumpMethods(sw);

        assertIsSmallTestSampleMethodDump(sw.toString());
    }

    @Test
    void dumpMethods_File(@TempDir File tempDir) throws IOException {
        Performer.setMemoizationEnabled(true);
        runSmallTestSample();

        String path = new File(tempDir, METHOD_MAP_DUMP_FILE_NAME).getAbsolutePath();
        Performer.dumpMethods(path);

        assertIsSmallTestSampleMethodDump(readFileText(path));
    }

    private static String readFileText(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(fileName);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)
        ) {
            String str;
            while ((str = reader.readLine()) != null) {
                sb.append(str);
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private void assertIsSmallTestSampleMethodDump(String actual) {
        assertEquals("" +
                        "org.abego.lab.perform.sample.A\tfoo\n" +
                        "org.abego.lab.perform.sample.A\tinCAndE\n" +
                        "org.abego.lab.perform.sample.A\tonlyInA\n" +
                        "org.abego.lab.perform.sample.A\tonlyInC\n" +
                        "org.abego.lab.perform.sample.A\ttoString\n" +
                        "org.abego.lab.perform.sample.B\tinCAndE\n" +
                        "org.abego.lab.perform.sample.B\tonlyInA\n" +
                        "org.abego.lab.perform.sample.B\tonlyInC\n" +
                        "org.abego.lab.perform.sample.B\ttoString\n" +
                        "org.abego.lab.perform.sample.C\t+\n" +
                        "org.abego.lab.perform.sample.C\tinCAndE\n" +
                        "org.abego.lab.perform.sample.C\tonlyInA\n" +
                        "org.abego.lab.perform.sample.C\tonlyInC\n" +
                        "org.abego.lab.perform.sample.C\ttoString\n" +
                        "org.abego.lab.perform.sample.D\tinCAndE\n" +
                        "org.abego.lab.perform.sample.D\tonlyInA\n" +
                        "org.abego.lab.perform.sample.D\tonlyInC\n" +
                        "org.abego.lab.perform.sample.D\ttoString\n" +
                        "org.abego.lab.perform.sample.E\tinCAndE\n" +
                        "org.abego.lab.perform.sample.E\tonlyInA\n" +
                        "org.abego.lab.perform.sample.E\tonlyInC\n" +
                        "org.abego.lab.perform.sample.E\ttoString\n",
                actual);
    }
}
