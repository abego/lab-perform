package org.abego.lab.perform.core;

import org.abego.lab.perform.sample.A;
import org.abego.lab.perform.sample.B;
import org.abego.lab.perform.sample.C;
import org.abego.lab.perform.sample.D;
import org.abego.lab.perform.sample.E;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class SmokeTest {
    private static final String METHOD_MAP_FILE_PATH = "methods.ser";
    public static final int TEST_REPEAT_COUNT = 20;

    @Test
    void runPerformTestCodeMultipleTimes_noMemoization() {
        Performer.setMemoizationEnabled(false);

        runPerformTestCodeMultipleTimes();
    }

    @Test
    void runPerformTestCodeMultipleTimes_withMemoization() {
        Performer.setMemoizationEnabled(true);

        runPerformTestCodeMultipleTimes();
    }


    @Test
    void runPerformTestCodeMultipleTimes_withMemoization_andSave() throws IOException {
        Performer.setMemoizationEnabled(true);

        runPerformTestCodeMultipleTimes();

        Performer.saveMethods(METHOD_MAP_FILE_PATH);
    }

    @Test
    void runPerformTestCodeMultipleTimes_withMemoization_loaded() throws
            IOException, ClassNotFoundException, NoSuchMethodException {

        Performer.loadMethods(METHOD_MAP_FILE_PATH);

        runPerformTestCodeMultipleTimes();
    }

    @Test
    void forceInvocationTargetException() {
        A a = new A();

        PerformException e = assertThrows(PerformException.class,
                () -> Performer.perform(a, "throwIllegalAccessException"));
        assertEquals("java.lang.reflect.InvocationTargetException", e.getMessage());
    }

    private void runPerformTestCodeMultipleTimes() {
        for (int i = 0; i < TEST_REPEAT_COUNT; i++) {
            runPerformTestCode();
        }
    }

    private void runPerformTestCode() {
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
