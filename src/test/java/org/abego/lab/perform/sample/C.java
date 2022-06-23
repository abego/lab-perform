package org.abego.lab.perform.sample;

public class C extends A {
    public String onlyInC() {
        return "only in C";
    }

    public String inCAndE() {
        return "C: in C (& E)";
    }

    public int plus(int a, int b) {
        return a + b;
    }

    @Override
    public String toString() {
        return "a C";
    }
}
