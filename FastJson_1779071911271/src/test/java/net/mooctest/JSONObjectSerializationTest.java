package net.mooctest;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.util.TypeUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

class JSONObjectSerializationTest {
    // Intentionally inaccessible to the production stream: exercises its access-denied fallback.
    private static final long INACCESSIBLE = 19L;
    private Field[] originalFields;
    private boolean originalError;

    @BeforeEach
    void preserveReflectionCache() {
        originalFields = JSONObject.SecureObjectInputStream.fields;
        originalError = JSONObject.SecureObjectInputStream.fields_error;
    }

    @AfterEach
    void restoreReflectionCache() {
        JSONObject.SecureObjectInputStream.fields = originalFields;
        JSONObject.SecureObjectInputStream.fields_error = originalError;
    }

    @Test
    void secureSerializationRestoresOrderedMapAndNestedValues() throws Exception {
        initializeSecureFields();
        JSONObject object = new JSONObject(true);
        object.put("z", new java.util.ArrayList<>(List.of("first", "second")));
        object.put("a", new int[] {4, 9});
        JSONObject restored = roundTrip(object);
        assertNotSame(object, restored);
        assertInstanceOf(LinkedHashMap.class, restored.getInnerMap());
        assertEquals(List.of("z", "a"), List.copyOf(restored.keySet()));
        assertEquals(List.of("first", "second"), restored.get("z"));
        assertArrayEquals(new int[] {4, 9}, (int[]) restored.get("a"));
        assertFalse(JSONObject.SecureObjectInputStream.fields_error);
    }

    @Test
    void reflectionCacheIsInitializedOnceAndReused() {
        initializeSecureFields();
        Field[] first = JSONObject.SecureObjectInputStream.fields;
        assertEquals(4, first.length);
        assertEquals(List.of("bin", "passHandle", "handles", "curContext"),
                java.util.Arrays.stream(first).map(Field::getName).toList());
        JSONObject.SecureObjectInputStream.ensureFields();
        assertSame(first, JSONObject.SecureObjectInputStream.fields);
        assertFalse(JSONObject.SecureObjectInputStream.fields_error);
    }

    @Test
    void fallbackSerializationPreservesNullKeysAndNullValues() throws Exception {
        JSONObject.SecureObjectInputStream.fields = null;
        JSONObject.SecureObjectInputStream.fields_error = true;
        Map<String, Object> backing = new HashMap<>();
        backing.put(null, 12);
        backing.put("nullValue", null);
        backing.put("text", "value");
        JSONObject restored = roundTrip(new JSONObject(backing));
        assertEquals(backing, restored.getInnerMap());
        assertEquals(12, restored.get(null));
        assertTrue(restored.containsKey("nullValue"));
        assertNull(restored.get("nullValue"));
        assertTrue(JSONObject.SecureObjectInputStream.fields_error);
        assertNull(JSONObject.SecureObjectInputStream.fields);
    }

    @Test
    void errorFlagDisablesAnOtherwisePresentReflectionCache() throws Exception {
        JSONObject.SecureObjectInputStream.fields = new Field[0];
        JSONObject.SecureObjectInputStream.fields_error = true;
        assertEquals(Map.of("value", 71), roundTrip(new JSONObject(Map.of("value", 71))));
        assertTrue(JSONObject.SecureObjectInputStream.fields_error);
    }

    @Test
    void inactiveSecureStreamFallsBackToTheOriginalObjectInputStream() throws Exception {
        // An unavailable copied callback context raises NotActiveException in defaultReadObject.
        JSONObject.SecureObjectInputStream.fields = new Field[0];
        JSONObject.SecureObjectInputStream.fields_error = false;
        JSONObject restored = roundTrip(new JSONObject(Map.of("value", 81)));
        assertEquals(Map.of("value", 81), restored);
    }

    @Test
    void inaccessibleCopiedFieldMarksReflectionAsFailed() throws Exception {
        JSONObject.SecureObjectInputStream.fields = new Field[] {
            JSONObjectSerializationTest.class.getDeclaredField("INACCESSIBLE")
        };
        JSONObject.SecureObjectInputStream.fields_error = false;
        try (ObjectInputStream input = streamFor("content");
             JSONObject.SecureObjectInputStream ignored = new JSONObject.SecureObjectInputStream(input)) {
            assertTrue(JSONObject.SecureObjectInputStream.fields_error);
        }
    }

    @Test
    void deniedFieldLookupIsRememberedAndAllowsFallbackDeserialization() throws Exception {
        JSONObject.SecureObjectInputStream.fields = null;
        JSONObject.SecureObjectInputStream.fields_error = false;
        // Model the JVM denying private-field discovery; normal data parsing remains real.
        try (MockedStatic<TypeUtils> reflection = mockStatic(TypeUtils.class, CALLS_REAL_METHODS)) {
            reflection.when(() -> TypeUtils.getField(eq(ObjectInputStream.class), anyString(), any(Field[].class)))
                    .thenThrow(new SecurityException("reflection denied by fixture"));
            JSONObject.SecureObjectInputStream.ensureFields();
        }
        assertTrue(JSONObject.SecureObjectInputStream.fields_error);
        assertNull(JSONObject.SecureObjectInputStream.fields);
        assertEquals(Map.of("value", 91), roundTrip(new JSONObject(Map.of("value", 91))));
    }

    @Test
    void resolvesOrdinaryClassesAndPrimitiveAndReferenceArrayDescriptors() throws Exception {
        initializeSecureFields();
        try (ObjectInputStream input = streamFor("content");
             JSONObject.SecureObjectInputStream secure = new JSONObject.SecureObjectInputStream(input)) {
            for (Class<?> type : List.of(String.class, String[].class, String[][].class,
                    int[].class, long[].class, Map[].class)) {
                assertSame(type, secure.resolveClass(ObjectStreamClass.lookupAny(type)));
            }
            // Multi-dimensional primitive arrays are normalized to "I" and rejected by autoType.
            assertThrows(JSONException.class,
                    () -> secure.resolveClass(ObjectStreamClass.lookupAny(int[][].class)));
        }
    }

    @Test
    void deniesUnsafeClassesIncludingTheirArrayDescriptors() throws Exception {
        initializeSecureFields();
        try (ObjectInputStream input = streamFor("content");
             JSONObject.SecureObjectInputStream secure = new JSONObject.SecureObjectInputStream(input)) {
            assertThrows(JSONException.class,
                    () -> secure.resolveClass(ObjectStreamClass.lookupAny(ClassLoader.class)));
            assertThrows(JSONException.class,
                    () -> secure.resolveClass(ObjectStreamClass.lookupAny(ClassLoader[].class)));
        }
    }

    @Test
    void malformedReferenceDescriptorWithoutSemicolonIsNotSilentlyStripped() throws Exception {
        initializeSecureFields();
        // Fresh descriptor, not lookupAny's shared cache: represents a corrupted stream class name.
        Constructor<ObjectStreamClass> constructor = ObjectStreamClass.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ObjectStreamClass malformed = constructor.newInstance();
        Field name = ObjectStreamClass.class.getDeclaredField("name");
        name.setAccessible(true);
        name.set(malformed, "Ljava.lang.String");
        try (ObjectInputStream input = streamFor("content");
             JSONObject.SecureObjectInputStream secure = new JSONObject.SecureObjectInputStream(input)) {
            ClassNotFoundException failure = assertThrows(ClassNotFoundException.class,
                    () -> secure.resolveClass(malformed));
            assertEquals("Ljava.lang.String", failure.getMessage());
        }
    }

    @Test
    void incompleteArrayDescriptorRetainsItsNameInTheDiagnostic() throws Exception {
        initializeSecureFields();
        Constructor<ObjectStreamClass> constructor = ObjectStreamClass.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ObjectStreamClass malformed = constructor.newInstance();
        Field name = ObjectStreamClass.class.getDeclaredField("name");
        name.setAccessible(true);
        name.set(malformed, "[L;");
        try (ObjectInputStream input = streamFor("content");
             JSONObject.SecureObjectInputStream secure = new JSONObject.SecureObjectInputStream(input)) {
            JSONException failure = assertThrows(JSONException.class, () -> secure.resolveClass(malformed));
            assertTrue(failure.getMessage().contains("L;"), "The invalid descriptor must be identifiable");
        }
    }

    @Test
    void resolvesProxyInterfacesAndRejectsUnsafeInterfaceNames() throws Exception {
        initializeSecureFields();
        try (ObjectInputStream input = streamFor("content");
             JSONObject.SecureObjectInputStream secure = new JSONObject.SecureObjectInputStream(input)) {
            Class<?> emptyProxy = secure.resolveProxyClass(new String[0]);
            assertTrue(Proxy.isProxyClass(emptyProxy));
            assertEquals(0, emptyProxy.getInterfaces().length);
            Class<?> serializableProxy = secure.resolveProxyClass(new String[] {Serializable.class.getName()});
            assertTrue(Proxy.isProxyClass(serializableProxy));
            assertArrayEquals(new Class<?>[] {Serializable.class}, serializableProxy.getInterfaces());
            assertThrows(JSONException.class,
                    () -> secure.resolveProxyClass(new String[] {ClassLoader.class.getName()}));
            assertThrows(JSONException.class,
                    () -> secure.resolveProxyClass(new String[] {"no.such.Interface"}));
        }
    }

    private static void initializeSecureFields() {
        JSONObject.SecureObjectInputStream.fields = null;
        JSONObject.SecureObjectInputStream.fields_error = false;
        JSONObject.SecureObjectInputStream.ensureFields();
        assertNotNull(JSONObject.SecureObjectInputStream.fields,
                "Java 21 requires --add-opens java.base/java.io=ALL-UNNAMED for the secure stream path");
        assertFalse(JSONObject.SecureObjectInputStream.fields_error);
    }

    private static JSONObject roundTrip(JSONObject value) throws Exception {
        try (ObjectInputStream stream = streamFor(value)) {
            return (JSONObject) stream.readObject();
        }
    }

    private static ObjectInputStream streamFor(Object value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        return new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    }
}
