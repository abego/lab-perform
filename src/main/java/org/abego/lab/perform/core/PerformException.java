package org.abego.lab.perform.core;

public class PerformException extends RuntimeException {
    public PerformException(Throwable e) {
        super(e);
    }

    public PerformException(String message) {
        super(message);
    }
}
