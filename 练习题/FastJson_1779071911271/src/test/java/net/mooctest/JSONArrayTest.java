package net.mooctest;

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

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.ParserConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests the local JSONArray implementation, including its backing-list contract. */
class JSONArrayTest {

    private static JSONArray array(Object... values) {
        return new JSONArray(new ArrayList<>(Arrays.asList(values)));
    }

    @Test
    void constructorsValidateArgumentsAndUseTheProvidedList() {
        JSONArray empty = new JSONArray();
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
        assertEquals(Collections.emptyList(), empty);
        JSONArray capacity = new JSONArray(2);
        assertTrue(capacity.isEmpty());
        assertTrue(capacity.add("first"));
        assertEquals(List.of("first"), capacity);
        assertThrows(IllegalArgumentException.class, () -> new JSONArray(-1));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new JSONArray((List<Object>) null));
        assertEquals("list is null.", error.getMessage());

        List<Object> backing = new ArrayList<>();
        JSONArray wrapped = new JSONArray(backing);
        backing.add("external");
        assertEquals("external", wrapped.get(0));
        wrapped.add("internal");
        assertEquals(List.of("external", "internal"), backing);
        assertFalse(wrapped.isEmpty());
        assertEquals(2, wrapped.size());
    }

    @Test
    void relatedArrayAndComponentTypeAreStoredByIdentity() {
        JSONArray values = new JSONArray();
        assertNull(values.getRelatedArray());
        assertNull(values.getComponentType());
        String[] related = {"a", "b"};
        Type type = new TypeReference<List<String>>() { }.getType();
        values.setRelatedArray(related);
        values.setComponentType(type);
        assertSame(related, values.getRelatedArray());
        assertSame(type, values.getComponentType());
        values.setRelatedArray(null);
        values.setComponentType(null);
        assertNull(values.getRelatedArray());
        assertNull(values.getComponentType());
    }

    @Test
    void membershipAndArrayConversionsPreserveOrderingAndNulls() {
        JSONArray values = array("alpha", null, "beta", "alpha");
        assertTrue(values.contains("alpha"));
        assertTrue(values.contains(null));
        assertFalse(values.contains("missing"));
        assertTrue(values.containsAll(Arrays.asList("beta", null)));
        assertTrue(values.containsAll(Collections.emptyList()));
        assertFalse(values.containsAll(List.of("beta", "missing")));
        assertEquals(0, values.indexOf("alpha"));
        assertEquals(3, values.lastIndexOf("alpha"));
        assertEquals(1, values.indexOf(null));
        assertEquals(-1, values.indexOf("missing"));
        assertEquals(-1, values.lastIndexOf("missing"));
        Object[] snapshot = values.toArray();
        assertArrayEquals(new Object[]{"alpha", null, "beta", "alpha"}, snapshot);
        snapshot[0] = "detached";
        assertEquals("alpha", values.get(0));
        assertArrayEquals(new String[]{"alpha", null, "beta", "alpha"},
                values.toArray(new String[0]));
        String[] oversized = {"old", "old", "old", "old", "old", "sentinel"};
        assertSame(oversized, values.toArray(oversized));
        assertArrayEquals(new String[]{"alpha", null, "beta", "alpha", null, "sentinel"},
                oversized);
    }

    @Test
    void ordinaryListMutatorsReturnChangesAndModifyTheList() {
        JSONArray values = new JSONArray();
        assertTrue(values.add("a"));
        values.add(0, "b");
        assertTrue(values.addAll(List.of("c", "d")));
        assertFalse(values.addAll(Collections.emptyList()));
        assertTrue(values.addAll(1, List.of("x", "y")));
        assertFalse(values.addAll(0, Collections.emptyList()));
        assertEquals(List.of("b", "x", "y", "a", "c", "d"), values);
        assertEquals("y", values.remove(2));
        assertTrue(values.remove("x"));
        assertFalse(values.remove("missing"));
        assertEquals(List.of("b", "a", "c", "d"), values);
        assertTrue(values.removeAll(List.of("a", "c")));
        assertFalse(values.removeAll(List.of("missing")));
        assertEquals(List.of("b", "d"), values);
        assertTrue(values.retainAll(List.of("d")));
        assertFalse(values.retainAll(List.of("d")));
        assertEquals(List.of("d"), values);
        values.clear();
        assertTrue(values.isEmpty());
        assertEquals(0, values.size());
    }

    @Test
    void fluentMutatorsReturnTheReceiverAndApplyEveryOperation() {
        JSONArray values = new JSONArray();
        assertSame(values, values.fluentAdd("b"));
        assertEquals(List.of("b"), values);
        assertSame(values, values.fluentAdd(0, "a"));
        assertEquals(List.of("a", "b"), values);
        assertSame(values, values.fluentAddAll(List.of("d", "e")));
        assertEquals(List.of("a", "b", "d", "e"), values);
        assertSame(values, values.fluentAddAll(2, List.of("c")));
        assertEquals(List.of("a", "b", "c", "d", "e"), values);
        assertSame(values, values.fluentSet(1, "B"));
        assertEquals(List.of("a", "B", "c", "d", "e"), values);
        assertSame(values, values.fluentRemove("B"));
        assertEquals(List.of("a", "c", "d", "e"), values);
        assertSame(values, values.fluentRemove(0));
        assertEquals(List.of("c", "d", "e"), values);
        assertSame(values, values.fluentRemoveAll(List.of("c")));
        assertEquals(List.of("d", "e"), values);
        assertSame(values, values.fluentRetainAll(List.of("e")));
        assertEquals(List.of("e"), values);
        assertSame(values, values.fluentClear());
        assertTrue(values.isEmpty());
    }

    @Test
    void setReplacesAppendsAndPadsOnlyTheRequiredPositions() {
        JSONArray values = array("old", "tail");
        assertEquals("old", values.set(0, "new"));
        assertEquals(List.of("new", "tail"), values);
        assertNull(values.set(-1, "minus-one"));
        assertEquals(List.of("new", "tail", "minus-one"), values);
        assertNull(values.set(values.size(), "at-size"));
        assertEquals(List.of("new", "tail", "minus-one", "at-size"), values);
        assertNull(values.set(6, "padded"));
        assertEquals(Arrays.asList("new", "tail", "minus-one", "at-size", null, null,
                "padded"), values);
        assertNull(values.set(4, "hole"));
        assertEquals("hole", values.get(4));
        assertThrows(IndexOutOfBoundsException.class, () -> values.set(-2, "bad"));
        assertThrows(IndexOutOfBoundsException.class, () -> values.get(7));
        JSONArray initiallyEmpty = new JSONArray();
        assertNull(initiallyEmpty.set(0, "first"));
        assertEquals(List.of("first"), initiallyEmpty);
    }

    @Test
    void iteratorsAndSubListsAreLiveViewsOfTheBackingList() {
        JSONArray values = array("a", "b", "c", "d");
        Iterator<Object> iterator = values.iterator();
        assertEquals("a", iterator.next());
        iterator.remove();
        assertEquals(List.of("b", "c", "d"), values);
        ListIterator<Object> first = values.listIterator();
        assertEquals("b", first.next());
        first.set("B");
        assertEquals(List.of("B", "c", "d"), values);
        ListIterator<Object> middle = values.listIterator(2);
        assertEquals("c", middle.previous());
        middle.add("inserted");
        assertEquals(List.of("B", "inserted", "c", "d"), values);
        List<Object> view = values.subList(1, 3);
        assertEquals(List.of("inserted", "c"), view);
        view.clear();
        assertEquals(List.of("B", "d"), values);
    }

    @Test
    void cloneCopiesTheListButSharesElementsAndDropsTransientMetadata() {
        List<String> child = new ArrayList<>(List.of("nested"));
        JSONArray values = array(child, "tail");
        values.setRelatedArray(new Object[2]);
        values.setComponentType(String.class);
        JSONArray clone = assertInstanceOf(JSONArray.class, values.clone());
        assertNotSame(values, clone);
        assertEquals(values, clone);
        assertSame(child, clone.get(0));
        assertNull(clone.getRelatedArray());
        assertNull(clone.getComponentType());
        clone.remove(1);
        assertEquals(2, values.size());
        child.add("shared");
        assertEquals(List.of("nested", "shared"), clone.get(0));
    }

    @Test
    void equalityAndHashCodeFollowListSemantics() {
        JSONArray values = array("x", 7, null);
        List<Object> equalList = Arrays.asList("x", 7, null);
        assertEquals(values, values);
        assertEquals(values, equalList);
        assertEquals(equalList, values);
        assertEquals(values, array("x", 7, null));
        assertEquals(equalList.hashCode(), values.hashCode());
        assertNotEquals(values, array("x", 8, null));
        assertNotEquals(values, List.of("x", 7));
        assertNotEquals(values, "unrelated");
        assertFalse(values.equals(null));
    }

    @Test
    void getJSONObjectReturnsExistingObjectsWrapsMapsAndConvertsBeans() {
        JSONObject existing = new JSONObject().fluentPut("name", "existing");
        Map<String, Object> backing = new LinkedHashMap<>();
        backing.put("name", "mapped");
        ArrayBean bean = new ArrayBean();
        bean.setName("converted");
        bean.setCount(11);
        JSONArray values = array(existing, backing, bean, null, 17);
        assertSame(existing, values.getJSONObject(0));
        JSONObject wrapped = values.getJSONObject(1);
        assertEquals("mapped", wrapped.getString("name"));
        wrapped.put("extra", 9);
        assertEquals(9, backing.get("extra"));
        JSONObject converted = values.getJSONObject(2);
        assertEquals("converted", converted.getString("name"));
        assertEquals(11, converted.getIntValue("count"));
        assertNull(values.getJSONObject(3));
        assertThrows(ClassCastException.class, () -> values.getJSONObject(4));
    }

    @Test
    void getJSONArrayReturnsExistingArraysWrapsListsAndConvertsCollections() {
        JSONArray existing = array("existing");
        List<Object> backing = new ArrayList<>(List.of("mapped"));
        LinkedHashSet<String> set = new LinkedHashSet<>(List.of("a", "b"));
        JSONArray values = array(existing, backing, new int[]{2, 4}, set, null, "bad");
        assertSame(existing, values.getJSONArray(0));
        JSONArray wrapped = values.getJSONArray(1);
        wrapped.add("shared");
        assertEquals(List.of("mapped", "shared"), backing);
        assertEquals(List.of(2, 4), values.getJSONArray(2));
        assertEquals(List.of("a", "b"), values.getJSONArray(3));
        assertNull(values.getJSONArray(4));
        assertThrows(ClassCastException.class, () -> values.getJSONArray(5));
    }

    @Test
    void getObjectConvertsClassAndParameterizedTypes() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", "Ada");
        properties.put("count", "21");
        JSONArray values = array(properties, Arrays.asList("12", "34"), null, "45");
        ArrayBean bean = values.getObject(0, ArrayBean.class);
        assertEquals("Ada", bean.getName());
        assertEquals(21, bean.getCount());
        ArrayBean typedBean = values.getObject(0, (Type) ArrayBean.class);
        assertEquals("Ada", typedBean.getName());
        assertEquals(21, typedBean.getCount());
        Type integers = new TypeReference<List<Integer>>() { }.getType();
        List<Integer> converted = values.getObject(1, integers);
        assertEquals(List.of(12, 34), converted);
        assertInstanceOf(Integer.class, converted.get(0));
        assertNull(values.getObject(2, ArrayBean.class));
        assertNull(values.getObject(2, integers));
        assertEquals(45, values.getObject(3, Integer.class));
        assertEquals(45, values.<Integer>getObject(3, (Type) Integer.class));
    }

    @Test
    void booleanGettersDistinguishNullTrueAndFalse() {
        JSONArray values = array(null, true, false, "true", "false", 1, 0);
        assertNull(values.getBoolean(0));
        assertFalse(values.getBooleanValue(0));
        for (int index : new int[]{1, 3, 5}) {
            assertEquals(Boolean.TRUE, values.getBoolean(index));
            assertTrue(values.getBooleanValue(index));
        }
        for (int index : new int[]{2, 4, 6}) {
            assertEquals(Boolean.FALSE, values.getBoolean(index));
            assertFalse(values.getBooleanValue(index));
        }
        assertThrows(JSONException.class, () -> array("not-a-boolean").getBoolean(0));
    }

    @Test
    void numericGettersConvertNonzeroValuesAndPreservePrecision() {
        JSONArray values = array("-12", "32000", "1234567", "5000000000", "1.25", "-2.5",
                "12345678901234567890.123400", "123456789012345678901234567890");
        assertEquals(Byte.valueOf((byte) -12), values.getByte(0));
        assertEquals((byte) -12, values.getByteValue(0));
        assertEquals(Short.valueOf((short) 32000), values.getShort(1));
        assertEquals((short) 32000, values.getShortValue(1));
        assertEquals(Integer.valueOf(1234567), values.getInteger(2));
        assertEquals(1234567, values.getIntValue(2));
        assertEquals(Long.valueOf(5000000000L), values.getLong(3));
        assertEquals(5000000000L, values.getLongValue(3));
        assertEquals(Float.valueOf(1.25F), values.getFloat(4));
        assertEquals(1.25F, values.getFloatValue(4));
        assertEquals(Double.valueOf(-2.5D), values.getDouble(5));
        assertEquals(-2.5D, values.getDoubleValue(5));
        assertEquals(new BigDecimal("12345678901234567890.123400"), values.getBigDecimal(6));
        assertEquals(new BigInteger("123456789012345678901234567890"), values.getBigInteger(7));
        assertThrows(NumberFormatException.class, () -> array("bad").getIntValue(0));
    }

    @Test
    void nullAndEmptyNumericValuesUseBoxedNullAndPrimitiveZero() {
        for (Object value : new Object[]{null, "", "null"}) {
            JSONArray values = array(value);
            assertNull(values.getByte(0));
            assertNull(values.getShort(0));
            assertNull(values.getInteger(0));
            assertNull(values.getLong(0));
            assertNull(values.getFloat(0));
            assertNull(values.getDouble(0));
            if ("null".equals(value)) {
                assertThrows(NumberFormatException.class, () -> values.getBigDecimal(0));
            } else {
                assertNull(values.getBigDecimal(0));
            }
            assertNull(values.getBigInteger(0));
            assertEquals((byte) 0, values.getByteValue(0));
            assertEquals((short) 0, values.getShortValue(0));
            assertEquals(0, values.getIntValue(0));
            assertEquals(0L, values.getLongValue(0));
            assertEquals(0F, values.getFloatValue(0));
            assertEquals(0D, values.getDoubleValue(0));
        }
    }

    @Test
    void stringAndDateGettersPreserveValuesAndNulls() {
        long instant = 1609459200123L;
        Date date = new Date(instant);
        java.sql.Date sqlDate = new java.sql.Date(instant);
        java.sql.Timestamp timestamp = new java.sql.Timestamp(instant);
        JSONArray values = array(null, "text", 42, instant, date, sqlDate, timestamp);
        assertNull(values.getString(0));
        assertEquals("text", values.getString(1));
        assertEquals("42", values.getString(2));
        assertNull(values.getDate(0));
        assertNull(values.getSqlDate(0));
        assertNull(values.getTimestamp(0));
        assertEquals(date, values.getDate(3));
        assertEquals(sqlDate, values.getSqlDate(3));
        assertEquals(timestamp, values.getTimestamp(3));
        assertSame(date, values.getDate(4));
        assertSame(sqlDate, values.getSqlDate(5));
        assertSame(timestamp, values.getTimestamp(6));
    }

    @Test
    void toJavaListConvertsEveryElementAndReturnsAnIndependentList() {
        JSONArray values = array("7", 9L, null);
        List<Integer> converted = values.toJavaList(Integer.class);
        assertEquals(Arrays.asList(7, 9, null), converted);
        converted.set(0, 88);
        assertEquals("7", values.get(0));
        assertEquals(Collections.emptyList(), new JSONArray().toJavaList(ArrayBean.class));
        JSONArray beans = array(Map.of("name", "Grace", "count", 3));
        List<ArrayBean> typed = beans.toJavaList(ArrayBean.class);
        assertEquals(1, typed.size());
        assertEquals("Grace", typed.get(0).getName());
        assertEquals(3, typed.get(0).getCount());
    }

    @Test
    void serializationFallbackRoundTripsNullsValuesAndTransientMetadata() throws Exception {
        Field[] savedFields = JSONObject.SecureObjectInputStream.fields;
        boolean savedError = JSONObject.SecureObjectInputStream.fields_error;
        try {
            JSONObject.SecureObjectInputStream.fields = null;
            JSONObject.SecureObjectInputStream.fields_error = true;
            JSONArray original = array(null, "text", 17, new ArrayList<>(List.of("nested")));
            original.setRelatedArray(new Object[]{"transient"});
            original.setComponentType(String.class);
            JSONArray restored = roundTrip(original);
            assertNotSame(original, restored);
            assertEquals(original, restored);
            assertNull(restored.getRelatedArray());
            assertNull(restored.getComponentType());
            restored.add("new");
            assertEquals(4, original.size());
            assertEquals(5, restored.size());
        } finally {
            JSONObject.SecureObjectInputStream.fields = savedFields;
            JSONObject.SecureObjectInputStream.fields_error = savedError;
        }
    }

    @Test
    void secureDeserializationInitializesFieldsAndRestoresContents() throws Exception {
        Field[] savedFields = JSONObject.SecureObjectInputStream.fields;
        boolean savedError = JSONObject.SecureObjectInputStream.fields_error;
        try {
            JSONObject.SecureObjectInputStream.fields = null;
            JSONObject.SecureObjectInputStream.fields_error = false;
            JSONArray original = array("secure", null, 31);
            JSONArray restored = roundTrip(original);
            assertEquals(original, restored);
            assertNotSame(original, restored);
            assertEquals(4, JSONObject.SecureObjectInputStream.fields.length);
            assertFalse(JSONObject.SecureObjectInputStream.fields_error);
        } finally {
            JSONObject.SecureObjectInputStream.fields = savedFields;
            JSONObject.SecureObjectInputStream.fields_error = savedError;
        }
    }

    @Test
    void inactiveSecureStreamFallsBackToOriginalStream() throws Exception {
        Field[] savedFields = JSONObject.SecureObjectInputStream.fields;
        boolean savedError = JSONObject.SecureObjectInputStream.fields_error;
        try {
            // A secure stream without copied context cannot defaultReadObject; the original can.
            JSONObject.SecureObjectInputStream.fields = new Field[0];
            JSONObject.SecureObjectInputStream.fields_error = false;
            JSONArray original = array("kept", null, 23);
            assertEquals(original, roundTrip(original));
            JSONObject.SecureObjectInputStream.fields_error = true;
            assertEquals(original, roundTrip(original));
        } finally {
            JSONObject.SecureObjectInputStream.fields = savedFields;
            JSONObject.SecureObjectInputStream.fields_error = savedError;
        }
    }

    @Test
    void fallbackDeserializationEnforcesParserSafeModeForNonNullItems() throws Exception {
        Field[] savedFields = JSONObject.SecureObjectInputStream.fields;
        boolean savedError = JSONObject.SecureObjectInputStream.fields_error;
        ParserConfig savedConfig = ParserConfig.global;
        try {
            JSONObject.SecureObjectInputStream.fields = null;
            JSONObject.SecureObjectInputStream.fields_error = true;
            ParserConfig isolated = new ParserConfig();
            isolated.setSafeMode(true);
            ParserConfig.global = isolated;
            assertEquals(array((Object) null), roundTrip(array((Object) null)));
            JSONException error = assertThrows(JSONException.class,
                    () -> roundTrip(array(null, "safe string")));
            assertTrue(error.getMessage().contains("safeMode"));
        } finally {
            ParserConfig.global = savedConfig;
            JSONObject.SecureObjectInputStream.fields = savedFields;
            JSONObject.SecureObjectInputStream.fields_error = savedError;
        }
    }

    private static JSONArray roundTrip(JSONArray original) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(original);
        }
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            return (JSONArray) input.readObject();
        }
    }

    public static class ArrayBean {
        private String name;
        private int count;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
