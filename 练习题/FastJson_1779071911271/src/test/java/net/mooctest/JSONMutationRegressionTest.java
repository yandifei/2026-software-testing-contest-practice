package net.mooctest;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regressions chosen from surviving mutations, with observable value or identity assertions. */
class JSONMutationRegressionTest {
    @Test
    void emptyByteInputDoesNotRequireADecoder() {
        assertNull(JSON.parse(new byte[0], 0, 0, (java.nio.charset.CharsetDecoder) null));
    }

    @Test
    void untypedUtf8ParsingRejectsNullFeatureArraysEvenForEmptyInput() {
        assertThrows(NullPointerException.class, () -> JSON.parse(new byte[0], (Feature[]) null));
    }

    @Test
    void emptyCharacterInputIgnoresUnusedLengthAndFeatureArguments() {
        assertNull(JSON.parseObject(new char[0], 8, String.class, (Feature[]) null));
    }

    @Test
    void emptyTypedInputDoesNotRequireParserConfiguration() {
        assertNull(JSON.parseObject("", (Type) String.class, null, null, 0,
                new Feature[]{null}));
    }

    @Test
    void characterizationBaseJsonCollectionShortcutsDoNotValidateReceiverType() {
        // The public abstract base is extensible, but these shortcuts return a non-collection.
        // Record the existing defect explicitly; a correct implementation should reject these targets.
        JSON custom = new JSON() { };
        Object array = custom.toJavaObject(JSONArray.class);
        Object collection = custom.toJavaObject(java.util.Collection.class);
        Object list = custom.toJavaObject(List.class);
        assertSame(custom, array);
        assertSame(custom, collection);
        assertSame(custom, list);
        assertFalse(array instanceof JSONArray);
        assertFalse(collection instanceof java.util.Collection<?>);
        assertFalse(list instanceof List<?>);
    }

    @Test
    void decoderCapacityHandlesLargeGb18030InputWithMoreThanOneMaximumCharPerByte() {
        Charset charset = Charset.forName("GB18030");
        String value = "a".repeat(70_000) + "中文";
        byte[] data = ("\"" + value + "\"").getBytes(charset);
        assertEquals(value, JSON.parse(data, 0, data.length, charset.newDecoder()));
        assertEquals(value, JSON.parseObject(data, 0, data.length, charset.newDecoder(), String.class));
    }

    @Test
    void streamGrowthKeepsTemporaryStorageProportionalToPayload() throws Exception {
        String value = "x".repeat(66_000);
        byte[] data = ("\"" + value + "\"").getBytes(StandardCharsets.UTF_8);
        MeasuringInputStream input = new MeasuringInputStream(data);
        assertEquals(value, JSON.parseObject(input, String.class));
        assertTrue(input.largestBuffer < data.length * 2,
                "Reading a payload just over the initial buffer should not allocate over twice its size");
    }

    private static final class MeasuringInputStream extends ByteArrayInputStream {
        private int largestBuffer;
        MeasuringInputStream(byte[] value) { super(value); }
        @Override
        public synchronized int read(byte[] buffer, int offset, int length) {
            assertTrue(length > 0, "A full parser buffer must be expanded before the next read");
            largestBuffer = Math.max(largestBuffer, buffer.length);
            return super.read(buffer, offset, length);
        }
    }

    @Test
    void compatibleObjectTargetsPreserveIdentityEvenWithUnrelatedTypeMetadata() {
        JSONObject object = new JSONObject().fluentPut("@type", "no.such.Type");
        assertSame(object, object.toJavaObject(Map.class));
        assertSame(object, object.toJavaObject(JSONObject.class));
        assertSame(object, object.toJavaObject(JSON.class));
    }

    @Test
    void objectShortcutUsesTheRelocatedDefaultTypeKey() {
        String originalKey = JSON.DEFAULT_TYPE_KEY;
        try {
            JSON.DEFAULT_TYPE_KEY = "@kind";
            JSONObject object = new JSONObject().fluentPut("@type", "no.such.Type");
            assertSame(object, object.toJavaObject(Object.class));
            assertSame(object, object.toJavaObject(Object.class, new ParserConfig(), 0));
        } finally {
            JSON.DEFAULT_TYPE_KEY = originalKey;
        }
    }

    @Test
    void typedParsersResolveForwardReferencesBeforeReturning() {
        String text = "{first:{$ref:'$.last'},last:{id:6}}";
        assertResolved(JSON.parseObject(text, (Type) Map.class, JSON.DEFAULT_PARSER_FEATURE));
        assertResolved(JSON.parseObject(text, Map.class));
        assertResolved(JSON.parseObject(text.toCharArray(), text.length(), Map.class));
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        assertResolved(JSON.parse(bytes, 0, bytes.length, StandardCharsets.UTF_8.newDecoder()));
        String extra = "{} {}";
        byte[] trailing = extra.getBytes(StandardCharsets.UTF_8);
        assertThrows(JSONException.class, () -> JSON.parse(trailing, 0, trailing.length,
                StandardCharsets.UTF_8.newDecoder()));
    }

    private static void assertResolved(Map<?, ?> map) {
        assertNotNull(map.get("last"));
        assertSame(map.get("last"), map.get("first"));
    }

    private static void assertResolved(Object value) {
        assertResolved(assertInstanceOf(Map.class, value));
    }

    @Test
    void typedAndHeterogeneousArraysResolveNestedForwardReferences() {
        String text = "[{first:{$ref:'$[0].last'},last:{id:6}}]";
        List<Map> typed = JSON.parseArray(text, Map.class);
        assertResolved(typed.get(0));
        List<Object> heterogeneous = JSON.parseArray(text, new Type[]{Map.class});
        assertResolved(heterogeneous.get(0));
        assertThrows(JSONException.class, () -> JSON.parseArray("/* comment only */", Integer.class));
    }

    @Test
    void perCallFeaturesMatterWhenTheyAreAbsentFromDefaultMask() {
        Map<?, ?> map = JSON.parseObject("{1:2}", Map.class, new ParserConfig(), null,
                JSON.DEFAULT_PARSER_FEATURE, Feature.NonStringKeyAsString);
        assertEquals(2, map.get("1"));
        assertFalse(map.containsKey(1));
    }

    @Test
    void typeMetadataCanReturnTheRelocatedJsonObjectDirectly() {
        ParserConfig original = ParserConfig.global;
        try {
            ParserConfig isolated = new ParserConfig();
            isolated.addAccept(JSONObject.class.getName());
            ParserConfig.global = isolated;
            String text = "{\"@type\":\"net.mooctest.JSONObject\",\"answer\":42}";
            JSONObject direct = JSON.parseObject(text);
            assertEquals(42, direct.getIntValue("answer"));
            JSONObject featured = JSON.parseObject(text, Feature.SupportAutoType);
            assertEquals(42, featured.getIntValue("answer"));
        } finally {
            ParserConfig.global = original;
        }
    }

    @Test
    void relocatedArrayFromRegisteredDeserializerCanBeReadFromStringProperty() {
        ParserConfig original = ParserConfig.global;
        try {
            ParserConfig isolated = new ParserConfig();
            isolated.addAccept(ArrayEnvelope.class.getName());
            isolated.putDeserializer(ArrayEnvelope.class, new ObjectDeserializer() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T deserialze(DefaultJSONParser parser, Type type, Object name) {
                    Map<String, Object> content = new java.util.LinkedHashMap<>();
                    parser.parseObject(content);
                    return (T) new JSONArray(new ArrayList<>(List.of(content.get("value"))));
                }
                @Override
                public int getFastMatchToken() { return 12; }
            });
            ParserConfig.global = isolated;
            JSONObject object = new JSONObject();
            object.put("array", "{\"@type\":\"" + ArrayEnvelope.class.getName() + "\",\"value\":19}");
            assertEquals(List.of(19), object.getJSONArray("array"));
        } finally {
            ParserConfig.global = original;
        }
    }

    public static class ArrayEnvelope { }

    @Test
    void arrayClassConversionPreservesAnAlreadyCompatibleBeanIdentity() {
        JSONParsingTest.Bean bean = new JSONParsingTest.Bean();
        bean.id = 17;
        JSONArray array = new JSONArray(new ArrayList<>(List.of(bean)));
        assertSame(bean, array.getObject(0, (Type) JSONParsingTest.Bean.class));
    }

    @Test
    void baseClassConversionFallsThroughForAnIncompatibleTarget() {
        JSONArray array = new JSONArray(new ArrayList<>(List.of("four")));
        String result = array.toJavaObject(String.class);
        assertEquals("[\"four\"]", result);
    }

    @Test
    void addReturnsTheBackingListsActualModificationResult() {
        List<Object> unique = new ArrayList<>() {
            @Override
            public boolean add(Object value) {
                return !contains(value) && super.add(value);
            }
        };
        JSONArray array = new JSONArray(unique);
        assertTrue(array.add("one"));
        assertFalse(array.add("one"));
        assertEquals(List.of("one"), array);
    }

    @Test
    void proxyRejectsMethodsWhoseNamesFitButReturnTypesDoNot() throws Exception {
        JSONObject object = new JSONObject();
        assertThrows(JSONException.class, () -> object.invoke(null,
                InvalidAccessors.class.getMethod("setCount", int.class), new Object[]{5}));
        assertFalse(object.containsKey("count"));
        assertThrows(JSONException.class, () -> object.invoke(null,
                InvalidAccessors.class.getMethod("getCount"), null));
    }

    interface InvalidAccessors {
        int setCount(int count);
        void getCount();
    }

    @Test
    void secureAndFallbackArrayReadPathsHaveDifferentValidationResponsibilities() throws Exception {
        Field[] originalFields = JSONObject.SecureObjectInputStream.fields;
        boolean originalError = JSONObject.SecureObjectInputStream.fields_error;
        ParserConfig originalConfig = ParserConfig.global;
        try {
            JSONObject.SecureObjectInputStream.fields = null;
            JSONObject.SecureObjectInputStream.fields_error = false;
            JSONObject.SecureObjectInputStream.ensureFields();
            ParserConfig isolated = new ParserConfig();
            isolated.setSafeMode(true);
            ParserConfig.global = isolated;
            // Secure reading checks the list descriptor; fallback only checks individual elements.
            JSONArray value = new JSONArray();
            assertThrows(JSONException.class, () -> roundTrip(value));
            JSONObject.SecureObjectInputStream.fields_error = true;
            assertEquals(List.of(), roundTrip(value));
        } finally {
            ParserConfig.global = originalConfig;
            JSONObject.SecureObjectInputStream.fields = originalFields;
            JSONObject.SecureObjectInputStream.fields_error = originalError;
        }
    }

    @Test
    void objectReadInitializesItsOwnSecureFieldCache() throws Exception {
        Field[] originalFields = JSONObject.SecureObjectInputStream.fields;
        boolean originalError = JSONObject.SecureObjectInputStream.fields_error;
        try {
            JSONObject.SecureObjectInputStream.fields = null;
            JSONObject.SecureObjectInputStream.fields_error = false;
            assertEquals(Map.of("id", 29), roundTrip(new JSONObject(new java.util.HashMap<>(Map.of("id", 29)))));
            assertNotNull(JSONObject.SecureObjectInputStream.fields);
            assertEquals(4, JSONObject.SecureObjectInputStream.fields.length);
        } finally {
            JSONObject.SecureObjectInputStream.fields = originalFields;
            JSONObject.SecureObjectInputStream.fields_error = originalError;
        }
    }

    @Test
    void fallbackObjectDeserializationDoesNotValidateNullAsAClass() throws Exception {
        Field[] originalFields = JSONObject.SecureObjectInputStream.fields;
        boolean originalError = JSONObject.SecureObjectInputStream.fields_error;
        ParserConfig originalConfig = ParserConfig.global;
        try {
            JSONObject.SecureObjectInputStream.fields = null;
            JSONObject.SecureObjectInputStream.fields_error = true;
            ParserConfig.global = new ParserConfig() {
                @Override
                public Class<?> checkAutoType(Class type) {
                    return super.checkAutoType(java.util.Objects.requireNonNull(type,
                            "Only existing classes can be validated"));
                }
            };
            Map<String, Object> values = new java.util.HashMap<>();
            values.put(null, 17);
            values.put("empty", null);
            assertEquals(values, roundTrip(new JSONObject(values)));
        } finally {
            ParserConfig.global = originalConfig;
            JSONObject.SecureObjectInputStream.fields = originalFields;
            JSONObject.SecureObjectInputStream.fields_error = originalError;
        }
    }

    @Test
    void fallbackObjectDeserializationChecksBothNonNullKeysAndValues() throws Exception {
        Field[] originalFields = JSONObject.SecureObjectInputStream.fields;
        boolean originalError = JSONObject.SecureObjectInputStream.fields_error;
        ParserConfig originalConfig = ParserConfig.global;
        try {
            JSONObject.SecureObjectInputStream.fields = null;
            JSONObject.SecureObjectInputStream.fields_error = true;
            // Applications may install a stricter validation policy than the built-in class cache.
            ParserConfig.global = new ParserConfig() {
                @Override
                public Class<?> checkAutoType(Class type) {
                    if (type == String.class) {
                        throw new JSONException("String values are denied by this parser policy");
                    }
                    return super.checkAutoType(type);
                }
            };
            Map<String, Object> keyOnly = new java.util.HashMap<>();
            keyOnly.put("key", null);
            assertThrows(JSONException.class, () -> roundTrip(new JSONObject(keyOnly)));
            Map<String, Object> valueOnly = new java.util.HashMap<>();
            valueOnly.put(null, "value");
            assertThrows(JSONException.class, () -> roundTrip(new JSONObject(valueOnly)));
        } finally {
            ParserConfig.global = originalConfig;
            JSONObject.SecureObjectInputStream.fields = originalFields;
            JSONObject.SecureObjectInputStream.fields_error = originalError;
        }
    }

    private static Object roundTrip(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return input.readObject();
        }
    }
}
