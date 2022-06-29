package org.abego.lab.perform.core;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * A {@link MethodSerializer} saves and loads a {@link MethodMap} and provides
 * a way to "resolve" an actual {@link Method} object from entries in the
 * method map.
 */
interface MethodSerializer {
    void saveMethods(String filePath, MethodMap methodMap) throws IOException;

    MethodMap loadMethods(String filePath, boolean loadMethodsLazy) throws IOException, ClassNotFoundException, NoSuchMethodException;

    Method resolveMethod(
            Class<?> type,
            String selector,
            Object value,
            MethodMap methodMap) throws NoSuchMethodException;
}
