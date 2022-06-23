package org.abego.lab.perform.sample;

public class A {
    @Override
    public String toString() {
        return "an A";
    }

    public String onlyInA() { return "only in A";}

    public void throwIllegalAccessException() throws IllegalAccessException { throw new IllegalAccessException();}
}
