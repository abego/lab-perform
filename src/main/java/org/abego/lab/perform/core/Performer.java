package org.abego.lab.perform.core;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class Performer {
    private Performer() {
    }

    //region perform: the basic functionality of this class
    private static long extraDelayInOriginalGetMethodInMicros = 1000;

    public static long getExtraDelayInOriginalGetMethodInMicros() {
        return extraDelayInOriginalGetMethodInMicros;
    }

    public static void setExtraDelayInOriginalGetMethodInMicros(long delayInMicros) {
        if (delayInMicros < 0) {
            throw new IllegalArgumentException("delayInMicros must not be negative");
        }
        extraDelayInOriginalGetMethodInMicros = delayInMicros;
    }

    public static Object perform(Object receiver, String selector, Object... arguments) {
        try {
            return getMethod(receiver.getClass(), selector)
                    .invoke(receiver, arguments);

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new PerformException(e);
        } catch (NoSuchMethodException e) {
            // this would be a good place to implement "doesNotUnderstand"
            // behaviour, as known from Smalltalk.
            // For now, we just fail
            throw new UnsupportedOperationException(
                    String.format("%s does not understand '%s'", receiver.getClass()
                            .getName(), selector),
                    e);
        }
    }

    private static Method originalGetMethod(Class<?> type, String selector) throws NoSuchMethodException {
        // To demonstrate the effect of memoization better make the original
        // getMethod implementation slower with a little delay. This also
        // compensates the fact a little that "original" getMethod is more
        // expensive than our implementation.
        long endTime = System.nanoTime() + 1000*getExtraDelayInOriginalGetMethodInMicros();
        //noinspection StatementWithEmptyBody
        while (System.nanoTime() < endTime) {
            // busy waiting
        }

        // Just to demonstrate a dispatch with a non-ID selector name we
        // translate the "+" selector to the "plus" method name
        String methodName = selector.equals("+") ? "plus" : selector;

        for (Method method : type.getMethods()) {
            // for now just return the first method matching the methodName
            // (ignore overloads)
            if (method.getName().equals(methodName)) {
                return method;
            }
        }

        // no method found.
        throw new NoSuchMethodException(selector);
    }

    //endregion

    //region Memoization

    private static final class MethodLocator {
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

    /**
     * A 2-step map, mapping (Class -> (selector: String -> Method|MethodLocator|NoSuchMethodException))
     */
    private static Map<Class<?>, Map<String, Object>> classToSelectorToMethodMap;

    public static boolean isMemoizationEnabled() {
        return classToSelectorToMethodMap != null;
    }

    public static void setMemoizationEnabled(boolean value) {
        if (value == isMemoizationEnabled()) {
            // nothing to change
            return;
        }

        if (value) {
            newEmptyMethodMap();
        } else {
            removeMethodMap();
        }
    }

    private static Method getMethod(Class<?> type, String selector)
            throws NoSuchMethodException {

        if (isMemoizationEnabled()) {
            Object value = classToSelectorToMethodMap.computeIfAbsent(type,
                            c -> new HashMap<>())
                    .computeIfAbsent(selector, s -> {
                        try {
                            // only when no method was found in the cache the
                            // "expensive" original getMethod is called.
                            return originalGetMethod(type, selector);
                        } catch (NoSuchMethodException e) {
                            return e;
                        }
                    });
            if (value instanceof Method) {
                return (Method) value;
            }
            if (value instanceof MethodLocator) {
                // replace the MethodLocator stored for type and selector with
                // the Method it refers to and return the Method. Next time
                // a lookup for this (type,selector) combination in
                // `classToSelectorToMethodMap` will directly return the
                // Method object.
                Method method = ((MethodLocator) value).getMethodForClass(type);
                classToSelectorToMethodMap.get(type).put(selector, method);
                return method;
            }
            if (value instanceof NoSuchMethodException) {
                throw (NoSuchMethodException) value;
            }
            throw new IllegalStateException(
                    String.format("Unexpected value memoized for %s and selector '%s': %s",
                            type, selector, value));

        } else {
            return originalGetMethod(type, selector);
        }
    }

    private static void newEmptyMethodMap() {
        classToSelectorToMethodMap = new IdentityHashMap<>();
    }

    private static void removeMethodMap() {
        classToSelectorToMethodMap = null;
    }

    //endregion

    //region Serialization of Memoization data

    public static void saveMethods(String filePath) throws IOException {
        if (!isMemoizationEnabled()) {
            throw new IllegalStateException("Must enable memoization to save methods.");
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {

            writeMethodMap(objectOutputStream, classToSelectorToMethodMap);

            objectOutputStream.flush();
        }
    }

    public static void loadMethods(String filePath) throws IOException, ClassNotFoundException, NoSuchMethodException {
        loadMethods(filePath, false);
    }

    public static void loadMethodsLazy(String filePath) throws IOException, ClassNotFoundException, NoSuchMethodException {
        loadMethods(filePath, true);
    }

    public static void loadMethods(String filePath, boolean loadMethodsLazy) throws IOException, ClassNotFoundException, NoSuchMethodException {
        try (FileInputStream fileInputStream = new FileInputStream(filePath);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {

            classToSelectorToMethodMap = readMethodMap(objectInputStream, loadMethodsLazy);
        }
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

    private static Map<Class<?>, Map<String, Object>> readMethodMap(
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

    //endregion

}
