package org.abego.lab.perform.core;

import org.abego.lab.perform.sample.A;
import org.abego.lab.perform.sample.B;
import org.abego.lab.perform.sample.C;
import org.abego.lab.perform.sample.D;
import org.abego.lab.perform.sample.E;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PerformerTest {
    private static final String SMALL_TEST_SAMPLE_METHOD_MAP_FILE_PATH = "methods-smallsample";
    private static final String METHOD_MAP_FILE_NAME = "methodMap";
    private static final int TEST_REPEAT_COUNT = 20;
    private static final int LOAD_REPEAT_COUNT = 100;

    private static String pathOfMethodMapFileInDir(File directory) {
        return new File(directory, METHOD_MAP_FILE_NAME).getAbsolutePath();
    }

    @BeforeEach
    void setUp() {
        Performer.setMemoizationEnabled(false);
    }

    @Test
    void perform_withoutMemoization_smallSampleMultipleTimes() {
        Performer.setMemoizationEnabled(false);

        runSmallTestSampleMultipleTimes();
    }

    @Test
    void perform_withMemoization_smallSampleMultipleTimes() {
        Performer.setMemoizationEnabled(true);

        runSmallTestSampleMultipleTimes();
    }

    @Test
    void perform_withMemoization_and_saveMethods_smallSampleMultipleTimes(
            @TempDir File tempDir) throws IOException {

        Performer.setMemoizationEnabled(true);

        runSmallTestSampleMultipleTimes();

        Performer.saveMethods(pathOfMethodMapFileInDir(tempDir));
    }

    @Test
    void perform_withMemoization_andLoadedFromFile_smallSampleMultipleTimes() throws
            IOException, ClassNotFoundException, NoSuchMethodException {

        Performer.loadMethods(SMALL_TEST_SAMPLE_METHOD_MAP_FILE_PATH);

        runSmallTestSampleMultipleTimes();
    }

    @Test
    void loadMethods_multipleTime_smallSample() throws IOException, ClassNotFoundException, NoSuchMethodException {
        Performer.setMemoizationEnabled(true);

        for (int i = 0; i < LOAD_REPEAT_COUNT; i++) {
            Performer.loadMethods(SMALL_TEST_SAMPLE_METHOD_MAP_FILE_PATH);
        }

        assertEquals("an A", Performer.perform(new A(), "toString"));
    }

    @Test
    void loadMethodsLazy_multipleTime_smallSample() throws IOException, ClassNotFoundException, NoSuchMethodException {
        Performer.setMemoizationEnabled(true);

        for (int i = 0; i < LOAD_REPEAT_COUNT; i++) {
            Performer.loadMethodsLazy(SMALL_TEST_SAMPLE_METHOD_MAP_FILE_PATH);
        }

        assertEquals("an A", Performer.perform(new A(), "toString"));
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
}
