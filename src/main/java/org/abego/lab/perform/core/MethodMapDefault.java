package org.abego.lab.perform.core;

import java.util.HashMap;
import java.util.Map;

final class MethodMapDefault
        extends HashMap<Object, Map<String, Object>>
        implements MethodMap {

    private MethodMapDefault(){}

    public static MethodMapDefault createMethodMapDefault() {
        return new MethodMapDefault();
    }

    @Override
    public Iterable<Entry<Object, Map<String, Object>>> entries() {
        return entrySet();
    }
}
