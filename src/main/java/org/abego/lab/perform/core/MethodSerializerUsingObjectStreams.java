package org.abego.lab.perform.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import java.util.Map;

import static org.abego.lab.perform.core.MethodMapDefault.createMethodMapDefault;

final class MethodSerializerUsingObjectStreams implements MethodSerializer {
    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0] ;
    private static final Map<String, Class<?>> TYPES_BY_NAME = new HashMap<>();

    static {
        TYPES_BY_NAME.put("byte", Byte.TYPE);
        TYPES_BY_NAME.put("short", Short.TYPE);
        TYPES_BY_NAME.put("int", Integer.TYPE);
        TYPES_BY_NAME.put("long", Long.TYPE);
        TYPES_BY_NAME.put("float", Float.TYPE);
        TYPES_BY_NAME.put("double", Double.TYPE);
        TYPES_BY_NAME.put("char", Character.TYPE);
        TYPES_BY_NAME.put("boolean", Boolean.TYPE);
    }

    private static final class MethodLocator {
        public final String methodName;
        public final String[] parameterTypesNames;

        public MethodLocator(String methodName, String[] parameterTypesNames) {
            this.methodName = methodName;
            this.parameterTypesNames = parameterTypesNames;
        }

        public Method getMethodForClass(Class<?> type) throws NoSuchMethodException {
            Class<?>[] parameterTypes = parameterTypesNames.length == 0
                    ? EMPTY_CLASS_ARRAY : asTypesArray(parameterTypesNames);
            return type.getMethod(methodName, parameterTypes);
        }
    }

    @Override
    public void saveMethods(String filePath, MethodMap methodMap) throws IOException {
        try (OutputStream outputStream = newOutputStream(filePathForSerialization(filePath));
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {

            writeMethodMap(objectOutputStream, methodMap);

            objectOutputStream.flush();
        }
    }

    @Override
    public MethodMap loadMethods(String filePath, boolean loadMethodsLazy) throws IOException, ClassNotFoundException, NoSuchMethodException {
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
            MethodMap methodMap) throws NoSuchMethodException {

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
        throw new PerformException(
                String.format("Unexpected value memoized for %s and selector '%s': %s",
                        type, selector, value));
    }

    private static void writeMethodMap(
            ObjectOutputStream objectOutputStream,
            MethodMap methodMap) throws IOException {

        // We cannot just use
        //    objectOutputStream.writeObject(classToSelectorToMethodMap)
        // to save the classToSelectorToMethodMap as a Method instance is not
        // serializable. Therefore, we need to do "manually".

        // write the number of classes
        int classCount = methodMap.size();
        objectOutputStream.writeInt(classCount);

        // for each "classTo..." entry ...
        for (Map.Entry<Object, Map<String, Object>> entry : methodMap.entries()) {
            // ...  write the class name
            Object classOrClassName = entry.getKey();
            objectOutputStream.writeObject(
                    classOrClassName instanceof Class<?>
                            ? ((Class<?>) classOrClassName).getName()
                            : classOrClassName);

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
                            method.getName(),
                            asTypeNamesArray(method.getParameterTypes()));
                } else if (value instanceof MethodLocator) {
                    // for a method write its locator info
                    // (name and parameter types)
                    MethodLocator methodLocator = (MethodLocator) value;
                    writeMethodLocatorInfo(
                            objectOutputStream,
                            methodLocator.methodName,
                            methodLocator.parameterTypesNames);
                } else {
                    // other objects (e.g. Exceptions) write as they are
                    objectOutputStream.writeObject(value);
                }
            }
        }
    }

    private static void writeMethodLocatorInfo(
            ObjectOutputStream objectOutputStream,
            String methodName,
            String[] parameterTypesNames) throws IOException {
        objectOutputStream.writeObject(methodName);
        writeStringArray(objectOutputStream, parameterTypesNames);
    }

    private MethodMap readMethodMap(
            ObjectInputStream in, boolean loadMethodsLazy)
            throws IOException, ClassNotFoundException, NoSuchMethodException {

        MethodMap result = createMethodMapDefault();

        // read the number of classes
        int classCount = in.readInt();
        // for each class ...
        for (int i = 0; i < classCount; i++) {
            // ... read the class (by name) (and create its "selector -> Method" map)
            String typeName = (String) in.readObject();
            Map<String, Object> selectorToMethodMap = new HashMap<>();
            result.put(typeName, selectorToMethodMap);

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
                    String[] parameterTypeNames = readStringArray(in);
                    // either store a MethodLocator in the map (to be converted
                    // into a "real" Method when the Method is needed the first
                    // time) or the "real" Method
                    // TODO: check if using "getMethods" is more efficient
                    Object someKindOfMethod = loadMethodsLazy
                            ? new MethodLocator(methodName, parameterTypeNames)
                            : getTypeNamed(typeName).getMethod(methodName, asTypesArray(parameterTypeNames));
                    selectorToMethodMap.put(selector, someKindOfMethod);
                } else {
                    selectorToMethodMap.put(selector, methodNameOrException);
                }
            }
        }
        return result;
    }

    private static Class<?> getTypeNamed(String typeName) {
        return TYPES_BY_NAME.computeIfAbsent(typeName, className -> {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new PerformException(String.format(
                        "Error when looking for type %s", className), e);
            }
        });
    }

    private static Class<?>[] asTypesArray(String[] typeNames) {
        int n = typeNames.length;
        Class<?>[] result = new Class<?>[n];
        for (int i = 0; i < n; i++) {
            result[i] = getTypeNamed(typeNames[i]);
        }
        return result;
    }

    private static String[] asTypeNamesArray(Class<?>[] types) {
        int n = types.length;
        String[] result = new String[n];
        for (int i = 0; i < n; i++) {
            result[i] = types[i].getName();
        }
        return result;
    }

    private String[] readStringArray(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int nParams = in.readInt();
        if (nParams == 0) {
            return EMPTY_STRING_ARRAY;
        }
        String[] parameterTypeNames = new String[nParams];
        for (int ip = 0; ip < nParams; ip++) {
            parameterTypeNames[ip] = (String) in.readObject();
        }
        return parameterTypeNames;
    }

    private static void writeStringArray(
            ObjectOutputStream objectOutputStream, String[] strings) throws IOException {
        objectOutputStream.writeInt(strings.length);
        for (String s : strings) {
            objectOutputStream.writeObject(s);
        }
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
