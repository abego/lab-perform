package org.abego.lab.perform.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

final class MethodSerializerUsingObjectStreams implements MethodSerializer {
    private final class MethodLocator {
        public final String methodName;
        public final Class<?>[] parameterTypes;

        public MethodLocator(String methodName, Class<?>[] parameterTypes) {
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
        }

        public Method getMethodForClass(Class<?> type) throws NoSuchMethodException {
            return type.getMethod(methodName, parameterTypes);
        }
    }

    @Override
    public void saveMethods(String filePath, Map<Class<?>, Map<String, Object>> methodMap) throws IOException {
        try (OutputStream outputStream = newOutputStream(filePathForSerialization(filePath));
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {

            writeMethodMap(objectOutputStream, methodMap);

            objectOutputStream.flush();
        }
    }

    @Override
    public Map<Class<?>, Map<String, Object>> loadMethods(String filePath, boolean loadMethodsLazy) throws IOException, ClassNotFoundException, NoSuchMethodException {
        try (InputStream inputStream = newInputStream(filePathForSerialization(filePath));
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {

            return readMethodMap(objectInputStream, loadMethodsLazy);
        }
    }

    @Override
    public Method resolveMethod(
            Class<?> type,
            String selector,
            Object value,
            Map<Class<?>, Map<String, Object>> methodMap) throws NoSuchMethodException {

        if (value instanceof MethodLocator) {
            // replace the MethodLocator stored for type and selector with
            // the Method it refers to and return the Method. Next time
            // a lookup for this (type,selector) combination in
            // `classToSelectorToMethodMap` will directly return the
            // Method object.
            Method method = ((MethodLocator) value).getMethodForClass(type);
            methodMap.get(type).put(selector, method);
            return method;
        }
        throw new IllegalStateException(
                String.format("Unexpected value memoized for %s and selector '%s': %s",
                        type, selector, value));
    }

    private static void writeMethodMap(
            ObjectOutputStream objectOutputStream,
            Map<Class<?>, Map<String, Object>> methodMap) throws IOException {

        // We cannot just use
        //    objectOutputStream.writeObject(classToSelectorToMethodMap)
        // to save the classToSelectorToMethodMap as a Method instance is not
        // serializable. Therefore, we need to do "manually".

        // write the number of classes
        int classCount = methodMap.size();
        objectOutputStream.writeInt(classCount);

        // for each "classTo..." entry ...
        for (Map.Entry<Class<?>, Map<String, Object>> entry : methodMap.entrySet()) {
            // ...  write the class
            objectOutputStream.writeObject(entry.getKey());

            Map<String, Object> selectorToMethodMap = entry.getValue();
            int selectorCount = selectorToMethodMap.size();
            // ...  write the number of selectors/methods for this class
            objectOutputStream.writeInt(selectorCount);
            // for each selector -> method entry ...
            for (Map.Entry<String, Object> selectorToMethod : entry.getValue()
                    .entrySet()) {
                // write the selector
                objectOutputStream.writeObject(selectorToMethod.getKey());
                // write the method or other value associated
                Object value = selectorToMethod.getValue();
                if (value instanceof Method) {
                    // for a method write its locator info
                    // (name and parameter types)
                    Method method = (Method) value;
                    writeMethodLocatorInfo(
                            objectOutputStream,
                            method.getName(), method.getParameterTypes());
                } else if (value instanceof MethodLocator) {
                    // for a method write its locator info
                    // (name and parameter types)
                    MethodLocator methodLocator = (MethodLocator) value;
                    writeMethodLocatorInfo(
                            objectOutputStream,
                            methodLocator.methodName, methodLocator.parameterTypes);
                } else {
                    // other objects (e.g. Exceptions) write as they are
                    objectOutputStream.writeObject(value);
                }
            }
        }
    }

    private static void writeMethodLocatorInfo(ObjectOutputStream objectOutputStream, String methodName, Class<?>[] parameterTypes) throws IOException {
        objectOutputStream.writeObject(methodName);
        objectOutputStream.writeObject(parameterTypes);
    }

    private Map<Class<?>, Map<String, Object>> readMethodMap(
            ObjectInputStream in, boolean loadMethodsLazy)
            throws IOException, ClassNotFoundException, NoSuchMethodException {

        Map<Class<?>, Map<String, Object>> result = new IdentityHashMap<>();

        // read the number of classes
        int classCount = in.readInt();
        // for each class ...
        for (int i = 0; i < classCount; i++) {
            // ... read the class (and create its "selector -> Method" map)
            Class<?> type = (Class<?>) in.readObject();
            Map<String, Object> selectorToMethodMap = new HashMap<>();
            result.put(type, selectorToMethodMap);

            // ...  read the number of selectors/methods for this class
            int selectorCount = in.readInt();
            // for each selector -> method entry ...
            for (int j = 0; j < selectorCount; j++) {
                // ... read the selector
                String selector = (String) in.readObject();
                Object methodNameOrException = in.readObject();
                if (methodNameOrException instanceof String) {
                    // this is the Method case

                    String methodName = (String) methodNameOrException;
                    Class<?>[] parameterTypes = (Class<?>[]) in.readObject();
                    // either store a MethodLocator in the map (to be converted
                    // into a "real" Method when the Method is needed the first
                    // time) or the "real" Method
                    // TODO: check if using "getMethods" is more efficient
                    Object someKindOfMethod = loadMethodsLazy
                            ? new MethodLocator(methodName, parameterTypes)
                            : type.getMethod(methodName, parameterTypes);
                    selectorToMethodMap.put(selector, someKindOfMethod);
                } else {
                    selectorToMethodMap.put(selector, methodNameOrException);
                }
            }
        }
        return result;
    }

    private Path filePathForSerialization(String filePath) {
        return Paths.get(filePath + ".ser");
    }

    private static InputStream newInputStream(Path filePath)
            throws IOException {
        return new BufferedInputStream(Files.newInputStream(filePath));
    }

    private static OutputStream newOutputStream(Path filePath)
            throws IOException {
        return new BufferedOutputStream(Files.newOutputStream(filePath));
    }
}
