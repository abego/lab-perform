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

    private static Method expensiveGetMethod(Class<?> type, String selector) throws NoSuchMethodException {
        // To demonstrate the effect of memoization better make the original
        // getMethod implementation slower with a little delay. This also
        // compensates the fact a little that "original" getMethod is more
        // expensive than our implementation.r
        delayForMilliseconds(1);

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

    /**
     * A 2-step map, mapping (Class -> (selector String -> Method|NoSuchMethodException))
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
                            return expensiveGetMethod(type, selector);
                        } catch (NoSuchMethodException e) {
                            return e;
                        }
                    });
            if (value instanceof Method) {
                return (Method) value;
            }
            if (value instanceof NoSuchMethodException) {
                throw (NoSuchMethodException) value;
            }
            throw new IllegalStateException(
                    String.format("Unexpected value memoized for %s and selector '%s': %s",
                            type, selector, value));

        } else {
            return expensiveGetMethod(type, selector);
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
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {

            writeMethodMap(objectOutputStream, classToSelectorToMethodMap);

            objectOutputStream.flush();
        }
    }

    public static void loadMethods(String filePath) throws IOException, ClassNotFoundException, NoSuchMethodException {
        try (FileInputStream fileInputStream = new FileInputStream(filePath);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {

            classToSelectorToMethodMap = readMethodMap(objectInputStream);
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
                    // for a method ...
                    Method method = (Method) value;
                    // ... write its name
                    objectOutputStream.writeObject(method.getName());
                    // ... write its parameter types
                    objectOutputStream.writeObject(method.getParameterTypes());
                } else {
                    // other objects (e.g. Exceptions) write as they are
                    objectOutputStream.writeObject(value);
                }
            }
        }
    }

    private static Map<Class<?>, Map<String, Object>> readMethodMap(
            ObjectInputStream in) throws IOException, ClassNotFoundException, NoSuchMethodException {
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
                    Method method = type.getMethod(methodName, parameterTypes);
                    selectorToMethodMap.put(selector, method);
                } else {
                    selectorToMethodMap.put(selector, methodNameOrException);
                }
            }
        }
        return result;
    }

    //endregion

    //region Helper code
    private static void delayForMilliseconds(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    //endregion

}
