package org.abego.lab.perform.core;

import java.util.Map;
import java.util.function.Function;

/**
 * A 2-step map, mapping
 * ({@link Class} | typeName: {@link String} ->
 * (selector: {@link String} ->
 * {@link java.lang.reflect.Method}|{@link NoSuchMethodException}|MethodSerializerSpecificType))
 */

interface MethodMap {

    int size();

    Map<String, Object> get(Object classOrTypename);

    Map<String, Object> put(Object classOrTypename, Map<String, Object> selectorToMethodMap);

    Map<String, Object> computeIfAbsent(
            Object classOrTypename,
            Function<? super Object, ? extends Map<String, Object>> mappingFunction);

    Iterable<Map.Entry<Object, Map<String, Object>>> entries();
}
