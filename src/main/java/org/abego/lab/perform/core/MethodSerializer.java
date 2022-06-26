package org.abego.lab.perform.core;

import java.io.IOException;
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
}
