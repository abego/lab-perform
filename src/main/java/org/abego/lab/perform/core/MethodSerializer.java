package org.abego.lab.perform.core;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Map;

interface MethodSerializer {
    void saveMethods(String filePath, Map<Class<?>, Map<String, Object>> methodMap) throws IOException;

    Map<Class<?>, Map<String, Object>> loadMethods(String filePath, boolean loadMethodsLazy) throws IOException, ClassNotFoundException, NoSuchMethodException;

    Method resolveMethod(
            Class<?> type,
            String selector,
            Object value,
            Map<Class<?>, Map<String, Object>> methodMap) throws NoSuchMethodException;

    default void dumpMethods(
            Writer writer, Map<Class<?>, Map<String, Object>> methodMap) throws IOException {
       throw new PerformException("dumpMethods not supported");
    }
}
