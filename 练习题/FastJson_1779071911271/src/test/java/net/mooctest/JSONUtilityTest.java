package net.mooctest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.SymbolTable;
import com.alibaba.fastjson.serializer.JSONSerializable;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.JavaBeanSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class JSONUtilityTest {
    @Test
    void configurationTogglesMapSortingWithoutChangingUnrelatedBits() throws Exception {
        int saved = JSON.DEFAULT_GENERATE_FEATURE;
        int parser = JSON.DEFAULT_PARSER_FEATURE;
        try {
            int other = SerializerFeature.WriteMapNullValue.getMask();
            int mask = SerializerFeature.MapSortField.getMask();
            JSON.DEFAULT_GENERATE_FEATURE = other;
            configure("fastjson.serializerFeatures.MapSortField", "true");
            assertEquals(other | mask, JSON.DEFAULT_GENERATE_FEATURE);
            configure("fastjson.serializerFeatures.MapSortField", "false");
            assertEquals(other, JSON.DEFAULT_GENERATE_FEATURE);
            configure("fastjson.serializerFeatures.MapSortField", "TRUE");
            assertEquals(other, JSON.DEFAULT_GENERATE_FEATURE);
            JSON.DEFAULT_GENERATE_FEATURE = other | mask;
            configure("fastjson.serializerFeatures.MapSortField", "unrecognized");
            assertEquals(other | mask, JSON.DEFAULT_GENERATE_FEATURE);
        } finally {
            JSON.DEFAULT_GENERATE_FEATURE = saved;
            JSON.DEFAULT_PARSER_FEATURE = parser;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"parser.features.NonStringKeyAsString", "parser.features.ErrorOnEnumNotMatch",
            "fastjson.parser.features.ErrorOnEnumNotMatch"})
    void configurationAddsParserFlagWithoutClearingExistingFlags(String property) throws Exception {
        int saved = JSON.DEFAULT_PARSER_FEATURE;
        try {
            int original = Feature.AllowSingleQuotes.getMask();
            int expected = property.contains("NonString") ? Feature.NonStringKeyAsString.getMask()
                    : Feature.ErrorOnEnumNotMatch.getMask();
            JSON.DEFAULT_PARSER_FEATURE = original;
            configure(property, "true");
            assertEquals(original | expected, JSON.DEFAULT_PARSER_FEATURE);
            configure(property, "false");
            assertEquals(original | expected, JSON.DEFAULT_PARSER_FEATURE);
        } finally {
            JSON.DEFAULT_PARSER_FEATURE = saved;
        }
    }

    @Test
    void configurationDisablesBothAsmEnginesOnlyForLiteralFalse() throws Exception {
        ParserConfig parser = ParserConfig.getGlobalInstance();
        SerializeConfig serializer = SerializeConfig.getGlobalInstance();
        boolean parserBefore = parser.isAsmEnable();
        boolean serializerBefore = serializer.isAsmEnable();
        try {
            parser.setAsmEnable(true);
            serializer.setAsmEnable(true);
            configure("fastjson.asmEnable", "true");
            assertTrue(parser.isAsmEnable());
            assertTrue(serializer.isAsmEnable());
            configure("fastjson.asmEnable", "false");
            assertFalse(parser.isAsmEnable());
            assertFalse(serializer.isAsmEnable());
            configure("fastjson.asmEnable", "TRUE");
            assertFalse(parser.isAsmEnable());
            assertFalse(serializer.isAsmEnable());
        } finally {
            parser.setAsmEnable(parserBefore);
            serializer.setAsmEnable(serializerBefore);
        }
    }

    @Test
    void settingDefaultTypeKeyUpdatesTheKeyAndParserSymbolTable() throws Exception {
        String originalKey = JSON.DEFAULT_TYPE_KEY;
        Field field = SymbolTable.class.getDeclaredField("symbols");
        field.setAccessible(true);
        String[] symbols = (String[]) field.get(ParserConfig.global.symbolTable);
        String[] savedSymbols = symbols.clone();
        try {
            String key = new String("@utilityType");
            JSON.setDefaultTypeKey(key);
            assertSame(key, JSON.DEFAULT_TYPE_KEY);
            assertTrue(Arrays.asList(symbols).contains(key));
        } finally {
            JSON.DEFAULT_TYPE_KEY = originalKey;
            System.arraycopy(savedSymbols, 0, symbols, 0, symbols.length);
        }
    }

    @Test
    void nullAndExistingJsonNodesAreReturnedWithoutConversion() {
        assertNull(JSON.toJSON(null));
        JSONObject object = new JSONObject();
        object.put("value", 1);
        JSONArray array = new JSONArray();
        array.add(object);
        assertSame(object, JSON.toJSON(object));
        assertSame(array, JSON.toJSON(array));
        assertSame(object, JSON.toJSON(object, new ParserConfig()));
    }

    @Test
    void linkedMapPreservesOrderConvertsKeysAndRecursesThroughCollections() {
        Map<Object, Object> source = new LinkedHashMap<>();
        source.put("z", List.of(Map.of("nested", 7), Shade.BLUE));
        source.put(1, null);
        JSONObject result = assertInstanceOf(JSONObject.class, JSON.toJSON(source));

        assertEquals(List.of("z", "1"), new ArrayList<>(result.keySet()));
        assertTrue(result.containsKey("1"));
        assertNull(result.get("1"));
        JSONArray nested = assertInstanceOf(JSONArray.class, result.get("z"));
        assertEquals(7, assertInstanceOf(JSONObject.class, nested.get(0)).get("nested"));
        assertEquals("BLUE", nested.get(1));
        assertNotSame(source, result);
    }

    @Test
    void treeMapProducesSortedObjectAndHashMapSupportsNullKeys() {
        Map<String, Object> sorted = new TreeMap<>();
        sorted.put("p", 2);
        sorted.put("a", 1);
        JSONObject result = assertInstanceOf(JSONObject.class, JSON.toJSON(sorted));
        assertEquals(List.of("a", "p"), new ArrayList<>(result.keySet()));

        Map<String, Object> unordered = new HashMap<>();
        unordered.put(null, "null key");
        unordered.put("empty", Map.of());
        JSONObject converted = assertInstanceOf(JSONObject.class, JSON.toJSON(unordered));
        assertEquals("null key", converted.get(null));
        assertTrue(assertInstanceOf(JSONObject.class, converted.get("empty")).isEmpty());
    }

    @Test
    void emptyAndPopulatedCollectionsAndArraysBecomeIndependentJsonArrays() {
        assertTrue(assertInstanceOf(JSONArray.class, JSON.toJSON(List.of())).isEmpty());
        assertTrue(assertInstanceOf(JSONArray.class, JSON.toJSON(new int[0])).isEmpty());
        JSONArray integers = assertInstanceOf(JSONArray.class, JSON.toJSON(new int[] {3, 1, 4}));
        assertEquals(List.of(3, 1, 4), integers);
        JSONArray nested = assertInstanceOf(JSONArray.class,
                JSON.toJSON(new Object[] {new int[] {2}, null, Map.of("x", "y")}));
        assertEquals(List.of(2), nested.get(0));
        assertNull(nested.get(1));
        assertEquals("y", assertInstanceOf(JSONObject.class, nested.get(2)).get("x"));
    }

    @Test
    void primitiveValuesKeepIdentityAndEnumsUseNameInsteadOfToString() {
        Object[] primitives = {"value", Integer.valueOf(1000), new BigDecimal("12.30"),
                Boolean.TRUE, Character.valueOf('x'), new Date(123L)};
        for (Object primitive : primitives) {
            assertSame(primitive, JSON.toJSON(primitive));
        }
        assertEquals("BLUE", JSON.toJSON(Shade.BLUE));
    }

    @Test
    void beansConvertTheirFieldsRecursivelyAndEmptyBeansRemainEmptyObjects() {
        Bean bean = new Bean();
        bean.name = "Ada";
        bean.values = List.of(2, 5);
        JSONObject converted = assertInstanceOf(JSONObject.class, JSON.toJSON(bean));
        assertEquals("Ada", converted.get("name"));
        assertEquals(List.of(2, 5), assertInstanceOf(JSONArray.class, converted.get("values")));
        assertTrue(assertInstanceOf(JSONObject.class, JSON.toJSON(new EmptyBean())).isEmpty());
    }

    @Test
    void beanGetterFailuresAreWrappedAndRetainTheOriginalCause() {
        JSONException failure = assertThrows(JSONException.class, () -> JSON.toJSON(new FailingBean()));
        assertEquals("toJSON error", failure.getMessage());
        Throwable root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertInstanceOf(IllegalStateException.class, root);
        assertEquals("getter failure", root.getMessage());
    }

    @Test
    void jsonSerializableTakesPriorityOverTheConfiguredBeanSerializer() {
        SerializeConfig config = new SerializeConfig();
        config.put(SerializedValue.class, new JavaBeanSerializer(SerializedValue.class));
        assertEquals("wire value", JSON.toJSON(new SerializedValue(), config));
    }

    @Test
    void characterizationNonBeanSerializerFallbackIgnoresTheSuppliedSerializer() {
        // Existing behavior: the fallback serializes using the global config, not this local serializer.
        SerializeConfig config = new SerializeConfig();
        config.put(CustomValue.class, (serializer, object, fieldName, fieldType, features) ->
                serializer.write("custom value"));
        Object result = JSON.toJSON(new CustomValue(), config);
        com.alibaba.fastjson.JSONObject object = assertInstanceOf(com.alibaba.fastjson.JSONObject.class, result);
        assertEquals(7, object.getIntValue("number"));
    }

    @Test
    void staticAndInstanceTypeConversionsProduceTypedValues() {
        JSONObject object = new JSONObject();
        object.put("name", "Lin");
        object.put("values", List.of(8, 9));
        Bean result = JSON.toJavaObject(object, Bean.class);
        assertEquals("Lin", result.name);
        assertEquals(List.of(8, 9), result.values);
        Bean typed = object.toJavaObject((Type) Bean.class);
        assertEquals("Lin", typed.name);
        Bean referenced = object.toJavaObject(new TypeReference<Bean>() { });
        assertEquals(List.of(8, 9), referenced.values);
        assertThrows(JSONException.class, () -> object.toJavaObject((TypeReference<?>) null));
        assertNull(JSON.toJavaObject(null, Bean.class));
    }

    @Test
    void baseJsonClassConversionsPreserveCompatibleArrayInterfaces() {
        JSONArray array = new JSONArray();
        array.add("4");
        assertSame(array, array.toJavaObject(JSONArray.class));
        assertSame(array, array.toJavaObject(JSON.class));
        assertSame(array, array.toJavaObject(Collection.class));
        assertSame(array, array.toJavaObject(List.class));
        assertSame(array, array.toJavaObject(Object.class));
        List<Integer> values = array.toJavaObject(new TypeReference<List<Integer>>() { });
        assertEquals(List.of(4), values);
        assertInstanceOf(Integer.class, values.get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bytes", "Chars"})
    void bufferAllocationCachesAtThresholdReusesAndDoesNotRetainOversizedArrays(String suffix) throws Exception {
        Field field = JSON.class.getDeclaredField(suffix.equals("Bytes") ? "bytesLocal" : "charsLocal");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> local = (ThreadLocal<Object>) field.get(null);
        Object original = local.get();
        Method allocator = JSON.class.getDeclaredMethod("allocate" + suffix, int.class);
        allocator.setAccessible(true);
        try {
            local.remove();
            Object atThreshold = allocator.invoke(null, 65536);
            assertEquals(65536, Array.getLength(atThreshold));
            assertSame(atThreshold, local.get());
            assertSame(atThreshold, allocator.invoke(null, 65536));
            assertSame(atThreshold, allocator.invoke(null, 1));
            Object larger = allocator.invoke(null, 65537);
            assertEquals(65537, Array.getLength(larger));
            assertNotSame(atThreshold, larger);
            assertSame(atThreshold, local.get());
            local.remove();
            Object uncached = allocator.invoke(null, 65537);
            assertEquals(65537, Array.getLength(uncached));
            assertNull(local.get());
            Object small = allocator.invoke(null, 0);
            assertEquals(65536, Array.getLength(small));
            assertSame(small, local.get());
        } finally {
            if (original == null) {
                local.remove();
            } else {
                local.set(original);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("validationCases")
    void validatorsDistinguishCompleteValuesObjectsAndArrays(String text, boolean valid,
            boolean object, boolean array) {
        assertEquals(valid, JSON.isValid(text), "general validator: " + text);
        assertEquals(object, JSON.isValidObject(text), "object validator: " + text);
        assertEquals(array, JSON.isValidArray(text), "array validator: " + text);
    }

    static Stream<Arguments> validationCases() {
        return Stream.of(
                Arguments.of(null, false, false, false),
                Arguments.of("", false, false, false),
                Arguments.of("   ", false, false, false),
                Arguments.of("{}", true, true, false),
                Arguments.of(" {\"x\":[1,true,null]} ", true, true, false),
                Arguments.of("[]", true, false, true),
                Arguments.of(" [1,{\"x\":2}] ", true, false, true),
                Arguments.of("123", true, false, false),
                Arguments.of("-1.25", true, false, false),
                Arguments.of("\"text\"", true, false, false),
                Arguments.of("null", true, false, false),
                Arguments.of("true", true, false, false),
                Arguments.of("false", true, false, false),
                Arguments.of("{", false, false, false),
                Arguments.of("{" + (char) 0x1A + "}", false, false, false),
                Arguments.of("[", false, false, false),
                Arguments.of("}", false, false, false),
                Arguments.of("{} {}", false, false, false),
                Arguments.of("[] []", false, false, false),
                Arguments.of("1 2", false, false, false),
                Arguments.of("\"unterminated", false, false, false),
                Arguments.of("{\"unterminated", false, false, false),
                Arguments.of("[\"unterminated", false, false, false));
    }

    @Test
    void mixinsSupportReplacementRemovalClearAndNullArgumentsWithoutLeakingState() throws Exception {
        Field field = JSON.class.getDeclaredField("mixInsMapper");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Type, Type> map = (ConcurrentMap<Type, Type>) field.get(null);
        Map<Type, Type> saved = new HashMap<>(map);
        try {
            JSON.clearMixInAnnotations();
            assertNull(JSON.getMixInAnnotations(Bean.class));
            assertNull(JSON.getMixInAnnotations(null));
            JSON.addMixInAnnotations(null, Bean.class);
            JSON.addMixInAnnotations(Bean.class, null);
            assertTrue(map.isEmpty());
            JSON.addMixInAnnotations(Bean.class, CustomValue.class);
            assertSame(CustomValue.class, JSON.getMixInAnnotations(Bean.class));
            JSON.addMixInAnnotations(Bean.class, EmptyBean.class);
            assertSame(EmptyBean.class, JSON.getMixInAnnotations(Bean.class));
            JSON.removeMixInAnnotations(null);
            assertSame(EmptyBean.class, JSON.getMixInAnnotations(Bean.class));
            JSON.removeMixInAnnotations(Bean.class);
            assertNull(JSON.getMixInAnnotations(Bean.class));
            JSON.addMixInAnnotations(Bean.class, EmptyBean.class);
            JSON.addMixInAnnotations(CustomValue.class, SerializedValue.class);
            JSON.clearMixInAnnotations();
            assertNull(JSON.getMixInAnnotations(Bean.class));
            assertNull(JSON.getMixInAnnotations(CustomValue.class));
        } finally {
            map.clear();
            map.putAll(saved);
        }
    }

    @Test
    void resolveTaskConnectsAForwardReferenceToTheResolvedObject() {
        String text = "{\"first\":{\"$ref\":\"$.second\"},\"second\":{\"v\":9}}";
        try (DefaultJSONParser parser = new DefaultJSONParser(text)) {
            com.alibaba.fastjson.JSONObject object = (com.alibaba.fastjson.JSONObject) parser.parse();
            assertNull(object.get("first"));
            JSON.handleResovleTask(parser, object);
            assertSame(object.get("second"), object.get("first"));
            assertEquals(9, object.getJSONObject("first").getIntValue("v"));
        }
    }

    private static void configure(String name, String value) throws Exception {
        Properties properties = new Properties();
        properties.setProperty(name, value);
        Method method = JSON.class.getDeclaredMethod("config", Properties.class);
        method.setAccessible(true);
        try {
            method.invoke(null, properties);
        } catch (InvocationTargetException exception) {
            throw new AssertionError("configuration failed", exception.getCause());
        }
    }

    enum Shade {
        BLUE;
        @Override public String toString() { return "a different display name"; }
    }

    public static class Bean {
        public String name;
        public List<Integer> values;
    }

    public static class EmptyBean { }

    public static class FailingBean {
        public int getValue() { throw new IllegalStateException("getter failure"); }
    }

    public static class CustomValue {
        public int getNumber() { return 7; }
    }

    public static class SerializedValue implements JSONSerializable {
        public int getNumber() { return 42; }
        @Override
        public void write(JSONSerializer serializer, Object name, Type type, int features) throws IOException {
            serializer.write("wire value");
        }
    }
}
