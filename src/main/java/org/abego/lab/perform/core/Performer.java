package org.abego.lab.perform.core;

import java.io.IOException;
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
            if (value instanceof NoSuchMethodException) {
                throw (NoSuchMethodException) value;
            }
            // The value for the given type and selector is not a Method
            // and not NoSuchMethodException, i.e. it is some data defined by
            // the MethodSerializer required to resolve the Method lazily.
            //
            // Delegate this job to the MethodSerializer.
            return methodSerializer.resolveMethod(
                    type, selector, value, classToSelectorToMethodMap);

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

    private static final MethodSerializer methodSerializer = new MethodSerializerUsingObjectStreams();

    public static void saveMethods(String filePath) throws IOException {
        if (!isMemoizationEnabled()) {
            throw new IllegalStateException("Must enable memoization to save methods.");
        }

        methodSerializer.saveMethods(filePath, classToSelectorToMethodMap);
    }

    public static void loadMethods(String filePath) throws IOException, ClassNotFoundException, NoSuchMethodException {
        loadMethods(filePath, false);
    }

    public static void loadMethodsLazy(String filePath) throws IOException, ClassNotFoundException, NoSuchMethodException {
        loadMethods(filePath, true);
    }

    private static void loadMethods(String filePath, boolean loadMethodsLazy) throws IOException, ClassNotFoundException, NoSuchMethodException {
        classToSelectorToMethodMap = methodSerializer.loadMethods(filePath, loadMethodsLazy);
    }

    //endregion

}
