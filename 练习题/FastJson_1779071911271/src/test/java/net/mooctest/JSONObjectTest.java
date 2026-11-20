package net.mooctest;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.parser.ParserConfig;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JSONObjectTest {
    @Test
    void constructorsSelectMapKindAndRejectInvalidArguments() {
        assertInstanceOf(HashMap.class, new JSONObject().getInnerMap());
        assertInstanceOf(HashMap.class, new JSONObject(3).getInnerMap());
        assertInstanceOf(HashMap.class, new JSONObject(false).getInnerMap());
        assertInstanceOf(LinkedHashMap.class, new JSONObject(true).getInnerMap());
        assertInstanceOf(LinkedHashMap.class, new JSONObject(3, true).getInnerMap());
        assertThrows(IllegalArgumentException.class, () -> new JSONObject((Map<String, Object>) null));
        assertThrows(IllegalArgumentException.class, () -> new JSONObject(-1, true));
        assertThrows(IllegalArgumentException.class, () -> new JSONObject(-1, false));
    }

    @Test
    void suppliedMapIsLiveAndMapOperationsReturnPreviousValues() {
        Map<String, Object> backing = new LinkedHashMap<>();
        JSONObject object = new JSONObject(backing);
        assertSame(backing, object.getInnerMap());
        assertTrue(object.isEmpty());
        assertEquals(0, object.size());
        assertNull(object.put("first", 11));
        assertEquals(11, object.put("first", 12));
        object.putAll(Map.of("second", 22));
        assertEquals(2, object.size());
        assertFalse(object.isEmpty());
        assertTrue(object.containsValue(12));
        assertFalse(object.containsValue(11));
        assertEquals(List.of("first", "second"), new ArrayList<>(object.keySet()));
        assertEquals(List.of(12, 22), new ArrayList<>(object.values()));
        object.entrySet().iterator().next().setValue(13);
        assertEquals(13, backing.get("first"));
        object.keySet().remove("second");
        assertFalse(object.containsKey("second"));
        assertEquals(13, object.remove("first"));
        assertNull(object.remove("missing"));
        object.put("clear", 1);
        object.clear();
        assertTrue(backing.isEmpty());
    }

    @Test
    void fluentOperationsMutateAndPreserveIdentity() {
        JSONObject object = new JSONObject();
        assertSame(object, object.fluentPut("a", 3));
        assertEquals(3, object.get("a"));
        assertSame(object, object.fluentPutAll(Map.of("b", 4, "c", 5)));
        assertEquals(Map.of("a", 3, "b", 4, "c", 5), object);
        assertSame(object, object.fluentRemove("a"));
        assertFalse(object.containsKey("a"));
        assertSame(object, object.fluentClear());
        assertTrue(object.isEmpty());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void numericKeysUseTextFallbackOnlyWhenNecessary() {
        Map backing = new HashMap();
        backing.put("7", "text");
        backing.put(7, "number");
        backing.put("8", "fallback");
        backing.put(8, null);
        backing.put(null, "null-key");
        JSONObject object = new JSONObject(backing);
        assertEquals("number", object.get(7));
        assertTrue(object.containsKey(7));
        assertEquals("fallback", object.get(8));
        assertTrue(object.containsKey(8));
        assertEquals("text", object.get(7L));
        assertTrue(object.containsKey(7L));
        assertNull(object.get(9));
        assertFalse(object.containsKey(9));
        assertEquals("text", object.get("7"));
        assertTrue(object.containsKey("7"));
        assertNull(object.get("absent"));
        assertFalse(object.containsKey("absent"));
        assertEquals("null-key", object.get(null));
        assertTrue(object.containsKey(null));
        backing.remove(null);
        assertNull(object.get(null));
        assertFalse(object.containsKey(null));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cloneCopiesMapButRetainsNestedReferences(boolean ordered) {
        JSONObject original = new JSONObject(ordered);
        List<String> nested = new ArrayList<>(List.of("one"));
        original.put("z", nested);
        original.put("a", 7);
        JSONObject copy = (JSONObject) original.clone();
        assertNotSame(original, copy);
        assertNotSame(original.getInnerMap(), copy.getInnerMap());
        assertEquals(original, copy);
        assertEquals(ordered, copy.getInnerMap() instanceof LinkedHashMap);
        assertSame(nested, copy.get("z"));
        copy.put("a", 8);
        assertEquals(7, original.get("a"));
        if (ordered) {
            assertEquals(List.of("z", "a"), new ArrayList<>(copy.keySet()));
        }
    }

    @Test
    void equalityAndHashCodeFollowMapContract() {
        JSONObject object = new JSONObject(Map.of("answer", 42));
        assertEquals(object, object);
        assertEquals(Map.of("answer", 42), object);
        assertEquals(new JSONObject(Map.of("answer", 42)), object);
        assertNotEquals(new JSONObject(Map.of("answer", 43)), object);
        assertNotEquals(null, object);
        assertNotEquals("answer", object);
        assertEquals("answer".hashCode() ^ Integer.valueOf(42).hashCode(), object.hashCode());
    }

    @Test
    void nestedObjectsReuseWrapParseOrConvertTheirValues() {
        JSONObject child = new JSONObject(Map.of("age", 9));
        Map<String, Object> backing = new HashMap<>(Map.of("age", 10));
        JSONObject object = new JSONObject();
        object.put("existing", child);
        object.put("map", backing);
        object.put("text", "{\"age\":11}");
        object.put("bean", new Person("Ada", 12));
        assertSame(child, object.getJSONObject("existing"));
        assertSame(backing, object.getJSONObject("map").getInnerMap());
        assertEquals(11, object.getJSONObject("text").getIntValue("age"));
        assertEquals("Ada", object.getJSONObject("bean").getString("name"));
        assertEquals(12, object.getJSONObject("bean").getIntValue("age"));
        assertNull(object.getJSONObject("missing"));
        object.put("scalar", 3);
        assertThrows(ClassCastException.class, () -> object.getJSONObject("scalar"));
    }

    @Test
    void nestedArraysReuseListsAndConvertJavaArrays() {
        JSONArray array = new JSONArray();
        array.add(3);
        List<Object> list = new ArrayList<>(List.of(4, 5));
        JSONObject object = new JSONObject();
        object.put("existing", array);
        object.put("list", list);
        object.put("javaArray", new int[] {6, 7});
        object.put("text", "[8,9]");
        object.put("nullText", "null");
        assertSame(array, object.getJSONArray("existing"));
        object.getJSONArray("list").add(10);
        assertEquals(List.of(4, 5, 10), list);
        assertEquals(List.of(6, 7), object.getJSONArray("javaArray"));
        assertNull(object.getJSONArray("missing"));
        assertNull(object.getJSONArray("nullText"));
        // The delegated parser returns Alibaba's JSONArray, not this project's wrapper.
        assertThrows(ClassCastException.class, () -> object.getJSONArray("text"));
    }

    @Test
    void typedObjectAccessSupportsClassTypeAndTypeReference() {
        JSONObject object = new JSONObject();
        object.put("person", Map.of("name", "Lin", "age", "19"));
        object.put("numbers", List.of("17", "18"));
        Person person = object.getObject("person", Person.class);
        assertEquals("Lin", person.name);
        assertEquals(19, person.age);
        assertEquals(19, object.<Person>getObject("person", (Type) Person.class).age);
        com.alibaba.fastjson.TypeReference<List<Integer>> ref = new com.alibaba.fastjson.TypeReference<>() { };
        assertEquals(List.of(17, 18), object.getObject("numbers", ref));
        assertEquals(List.of(17, 18), object.getObject("numbers", ref.getType()));
        assertSame(object.get("numbers"), object.getObject("numbers", (com.alibaba.fastjson.TypeReference<?>) null));
        assertNull(object.getObject("missing", Person.class));
        assertNull(object.getObject("missing", ref));
    }

    @Test
    void absentAndExplicitNullValuesHaveBoxedNullAndPrimitiveDefaults() {
        JSONObject object = new JSONObject();
        object.put("nil", null);
        for (String key : List.of("missing", "nil")) {
            assertNull(object.getBoolean(key));
            assertNull(object.getBytes(key));
            assertNull(object.getByte(key));
            assertNull(object.getShort(key));
            assertNull(object.getInteger(key));
            assertNull(object.getLong(key));
            assertNull(object.getFloat(key));
            assertNull(object.getDouble(key));
            assertNull(object.getBigDecimal(key));
            assertNull(object.getBigInteger(key));
            assertNull(object.getString(key));
            assertNull(object.getDate(key));
            assertNull(object.getSqlDate(key));
            assertNull(object.getTimestamp(key));
            assertFalse(object.getBooleanValue(key));
            assertEquals((byte) 0, object.getByteValue(key));
            assertEquals((short) 0, object.getShortValue(key));
            assertEquals(0, object.getIntValue(key));
            assertEquals(0L, object.getLongValue(key));
            assertEquals(0F, object.getFloatValue(key));
            assertEquals(0D, object.getDoubleValue(key));
        }
    }

    @Test
    void scalarAccessorsCoerceValuesWithoutLosingNonzeroResults() {
        JSONObject object = new JSONObject();
        object.put("integer", "23");
        object.put("decimal", "12.5");
        object.put("true", "true");
        object.put("false", false);
        object.put("bytes", "AQID/w==");
        object.put("epoch", 123456789L);
        object.put("huge", "123456789012345678901234567890");
        assertEquals(Boolean.TRUE, object.getBoolean("true"));
        assertTrue(object.getBooleanValue("true"));
        assertEquals(Boolean.FALSE, object.getBoolean("false"));
        assertFalse(object.getBooleanValue("false"));
        assertArrayEquals(new byte[] {1, 2, 3, -1}, object.getBytes("bytes"));
        assertEquals(Byte.valueOf((byte) 23), object.getByte("integer"));
        assertEquals((byte) 23, object.getByteValue("integer"));
        assertEquals(Short.valueOf((short) 23), object.getShort("integer"));
        assertEquals((short) 23, object.getShortValue("integer"));
        assertEquals(Integer.valueOf(23), object.getInteger("integer"));
        assertEquals(23, object.getIntValue("integer"));
        assertEquals(Long.valueOf(23), object.getLong("integer"));
        assertEquals(23L, object.getLongValue("integer"));
        assertEquals(Float.valueOf(12.5F), object.getFloat("decimal"));
        assertEquals(12.5F, object.getFloatValue("decimal"));
        assertEquals(Double.valueOf(12.5D), object.getDouble("decimal"));
        assertEquals(12.5D, object.getDoubleValue("decimal"));
        assertEquals(new BigDecimal("12.5"), object.getBigDecimal("decimal"));
        assertEquals(new BigInteger("123456789012345678901234567890"), object.getBigInteger("huge"));
        assertEquals("123456789", object.getString("epoch"));
        assertEquals(new Date(123456789L), object.getDate("epoch"));
        assertEquals(new java.sql.Date(123456789L), object.getSqlDate("epoch"));
        assertEquals(new Timestamp(123456789L), object.getTimestamp("epoch"));
    }

    @Test
    void proxiesStoreNamedPropertiesCastGettersAndImplementObjectMethods() {
        JSONObject object = new JSONObject();
        Properties properties = (Properties) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {Properties.class}, object);
        properties.setName("Ada");
        properties.setAge("31");
        properties.setActive(true);
        properties.store("renamed");
        properties.setEmptyAnnotation("empty annotation");
        properties.setX("letter");
        properties.setURL("https://example.invalid/");
        object.put("numbers", List.of("2", "3"));
        assertEquals("Ada", properties.getName());
        assertEquals(31, properties.getAge());
        assertTrue(properties.isActive());
        assertEquals("renamed", properties.fetch());
        assertEquals("empty annotation", properties.getEmptyAnnotation());
        assertEquals("letter", properties.getX());
        assertEquals("https://example.invalid/", object.get("uRL"));
        assertEquals(List.of(2, 3), properties.getNumbers());
        assertTrue(properties.equals(object));
        assertFalse(properties.equals(Map.of("name", "different")));
        assertEquals(object.hashCode(), properties.hashCode());
        assertEquals(object.toString(), properties.toString());
        assertEquals("renamed", object.get("alternate"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"returningSetter", "rename", "set"})
    void proxyRejectsMalformedSetters(String methodName) throws Exception {
        JSONObject object = new JSONObject();
        Method method = InvalidProperties.class.getMethod(methodName, String.class);
        JSONException failure = assertThrows(JSONException.class,
                () -> object.invoke(null, method, new Object[] {"value"}));
        assertEquals("illegal setter", failure.getMessage());
        assertTrue(object.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"voidGetter", "get", "is", "unknown"})
    void proxyRejectsMalformedGetters(String methodName) throws Exception {
        JSONObject object = new JSONObject();
        Method method = InvalidProperties.class.getMethod(methodName);
        JSONException failure = assertThrows(JSONException.class,
                () -> object.invoke(null, method, null));
        assertEquals("illegal getter", failure.getMessage());
    }

    @Test
    void proxyRejectsMethodsWithMultipleArguments() throws Exception {
        Method method = InvalidProperties.class.getMethod("combine", String.class, String.class);
        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
                () -> new JSONObject().invoke(null, method, new Object[] {"a", "b"}));
        assertEquals(method.toGenericString(), failure.getMessage());
    }

    @Test
    void toJavaObjectPreservesSupportedViewsAndConvertsBeans() {
        JSONObject object = new JSONObject(Map.of("name", "Grace", "age", "37"));
        assertSame(object, object.toJavaObject(Map.class));
        assertSame(object, object.toJavaObject(JSONObject.class));
        assertSame(object, object.toJavaObject(JSON.class));
        assertSame(object, object.toJavaObject(Object.class));
        Person person = object.toJavaObject(Person.class);
        assertEquals("Grace", person.name);
        assertEquals(37, person.age);
        ParserConfig config = new ParserConfig();
        assertSame(object, object.toJavaObject(Map.class, config, 0));
        assertSame(object, object.toJavaObject(Object.class, config, 0));
        Person configured = object.toJavaObject(Person.class, config, 0);
        assertEquals("Grace", configured.name);
        assertEquals(37, configured.age);
    }

    @Test
    void explicitTypeMetadataDisablesObjectIdentityShortcut() {
        JSONObject object = new JSONObject();
        object.put(JSON.DEFAULT_TYPE_KEY, HashMap.class.getName());
        object.put("score", 8);
        // Explicit metadata must reach the bean converter, which rejects HashMap as a bean.
        assertThrows(JSONException.class, () -> object.toJavaObject(Object.class));
        assertThrows(JSONException.class,
                () -> object.toJavaObject(Object.class, new ParserConfig(), 0));
    }

    public static class Person {
        public String name;
        public int age;

        public Person() { }

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    public interface Properties {
        void setName(String value);
        String getName();
        void setAge(String value);
        int getAge();
        void setActive(boolean value);
        boolean isActive();
        @JSONField(name = "alternate") void store(String value);
        @JSONField(name = "alternate") String fetch();
        @JSONField void setEmptyAnnotation(String value);
        @JSONField String getEmptyAnnotation();
        void setX(String value);
        String getX();
        void setURL(String value);
        List<Integer> getNumbers();
    }

    public interface InvalidProperties {
        int returningSetter(String value);
        void rename(String value);
        void set(String value);
        void voidGetter();
        String get();
        boolean is();
        String unknown();
        String combine(String a, String b);
    }
}
