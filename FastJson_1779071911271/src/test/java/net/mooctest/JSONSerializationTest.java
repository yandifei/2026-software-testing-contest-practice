package net.mooctest;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.serializer.NameFilter;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ValueFilter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Exact wire representations detect dropped writes, filters, features and encodings. */
class JSONSerializationTest {
    private static final NameFilter RENAME = (object, name, value) -> "key_" + name;
    private static final ValueFilter DOUBLE = (object, name, value) -> ((Integer) value) * 2;

    @Test
    void stringOverloadsWriteTheObjectAndApplyFeaturesAndFilters() {
        Map<String, Object> value = Map.of("n", 3);
        SerializeConfig config = new SerializeConfig();
        assertEquals("{\"n\":3}", JSON.toJSONString(value));
        assertEquals("{'n':3}", JSON.toJSONString(value, SerializerFeature.UseSingleQuotes));
        assertEquals("{n:3}", JSON.toJSONString(value, 0));
        assertEquals("{\"key_n\":3}", JSON.toJSONString(value, RENAME));
        assertEquals("{\"key_n\":6}", JSON.toJSONString(value, new SerializeFilter[]{RENAME, DOUBLE}));
        assertEquals("{\"n\":3}", JSON.toJSONString(value, config));
        assertEquals("{\"key_n\":3}", JSON.toJSONString(value, config, RENAME));
        assertEquals("{\"key_n\":6}", JSON.toJSONString(value, config,
                new SerializeFilter[]{RENAME, DOUBLE}));
        assertEquals("{\"n\":3}", JSON.toJSONString(value, config, null, "",
                JSON.DEFAULT_GENERATE_FEATURE));
        assertEquals("{n:3}", JSON.toJSONStringZ(value, config));
        assertEquals("null", JSON.toJSONString(null));
        assertEquals("{\"n\":3}", JSON.toJSONString(value, false));
        assertEquals("{\n\t\"n\":3\n}", JSON.toJSONString(value, true));
    }

    @Test
    void dateFormattingIsAppliedAcrossStringByteAndStreamWriters() throws IOException {
        Date date = new Date(1_234);
        // Literal format is independent of machine timezone and locale.
        assertEquals("\"date\"", JSON.toJSONStringWithDateFormat(date, "'date'"));
        assertEquals("1234", JSON.toJSONStringWithDateFormat(date, ""));
        SerializeConfig config = new SerializeConfig();
        assertEquals("\"date\"", utf8(JSON.toJSONBytes(date, config, null, "'date'",
                JSON.DEFAULT_GENERATE_FEATURE)));
        assertEquals("1234", utf8(JSON.toJSONBytes(date, config, null, "",
                JSON.DEFAULT_GENERATE_FEATURE)));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertEquals(6, JSON.writeJSONString(output, StandardCharsets.UTF_8, date, config, null,
                "'date'", JSON.DEFAULT_GENERATE_FEATURE));
        assertEquals("\"date\"", output.toString(StandardCharsets.UTF_8));
        output.reset();
        assertEquals(4, JSON.writeJSONString(output, StandardCharsets.UTF_8, date, config, null,
                "", JSON.DEFAULT_GENERATE_FEATURE));
        assertEquals("1234", output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void byteOverloadsPreserveFiltersFeaturesAndCharset() {
        Map<String, Object> value = Map.of("n", 3);
        SerializeConfig config = new SerializeConfig();
        assertEquals("{\"n\":3}", utf8(JSON.toJSONBytes(value)));
        assertEquals("{n:3}", utf8(JSON.toJSONBytes(value, 0)));
        assertEquals("{'n':3}", utf8(JSON.toJSONBytes(value, SerializerFeature.UseSingleQuotes)));
        assertEquals("{\"key_n\":3}", utf8(JSON.toJSONBytes(value, RENAME)));
        assertEquals("{\"n\":3}", utf8(JSON.toJSONBytes(value, config)));
        assertEquals("{n:3}", utf8(JSON.toJSONBytes(value, config, 0)));
        assertEquals("{\"key_n\":6}", utf8(JSON.toJSONBytes(value,
                new SerializeFilter[]{RENAME, DOUBLE})));
        assertEquals("{\"key_n\":3}", utf8(JSON.toJSONBytes(value, config, RENAME)));
        assertEquals("{\"key_n\":6}", utf8(JSON.toJSONBytes(value, config,
                new SerializeFilter[]{RENAME, DOUBLE}, JSON.DEFAULT_GENERATE_FEATURE)));
        byte[] utf16 = JSON.toJSONBytes(StandardCharsets.UTF_16LE, "中文", config, null,
                null, JSON.DEFAULT_GENERATE_FEATURE);
        assertArrayEquals("\"中文\"".getBytes(StandardCharsets.UTF_16LE), utf16);
    }

    @Test
    void writerOverloadsFlushSerializedCharactersAndHonorFeatureMask() {
        StringWriter writer = new StringWriter();
        JSON.writeJSONString(writer, Map.of("n", 3));
        assertEquals("{\"n\":3}", writer.toString());
        writer = new StringWriter();
        JSON.writeJSONStringTo(Map.of("n", 3), writer, SerializerFeature.UseSingleQuotes);
        assertEquals("{'n':3}", writer.toString());
        writer = new StringWriter();
        JSON.writeJSONString(writer, Map.of("n", 3), 0);
        assertEquals("{n:3}", writer.toString());
    }

    @Test
    void outputStreamOverloadsReturnEncodedByteCountAndApplyFilters() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertEquals(8, JSON.writeJSONString(output, "中文"));
        assertArrayEquals("\"中文\"".getBytes(StandardCharsets.UTF_8), output.toByteArray());
        output.reset();
        assertEquals(5, JSON.writeJSONString(output, Map.of("n", 3), 0));
        assertEquals("{n:3}", utf8(output.toByteArray()));
        output.reset();
        assertEquals(8, JSON.writeJSONString(output, StandardCharsets.UTF_16LE, "中文"));
        assertArrayEquals("\"中文\"".getBytes(StandardCharsets.UTF_16LE), output.toByteArray());
        output.reset();
        assertEquals(11, JSON.writeJSONString(output, StandardCharsets.UTF_8, Map.of("n", 3),
                new SerializeConfig(), new SerializeFilter[]{RENAME, DOUBLE}, null,
                JSON.DEFAULT_GENERATE_FEATURE));
        assertEquals("{\"key_n\":6}", utf8(output.toByteArray()));
    }

    @Test
    void instanceSerializationWritesOrderedEntriesAndWrapsAppendFailure() {
        JSONObject object = new JSONObject(new LinkedHashMap<>());
        object.put("b", 2);
        object.put("a", 1);
        assertEquals("{\"b\":2,\"a\":1}", object.toJSONString());
        assertEquals("{\"b\":2,\"a\":1}", object.toString());
        assertEquals("{'b':2,'a':1}", object.toString(SerializerFeature.UseSingleQuotes));
        StringBuilder builder = new StringBuilder("prefix:");
        object.writeJSONString(builder);
        assertEquals("prefix:{\"b\":2,\"a\":1}", builder.toString());
        IOException cause = new IOException("append failed");
        Appendable broken = new Appendable() {
            public Appendable append(CharSequence value) throws IOException { throw cause; }
            public Appendable append(CharSequence value, int start, int end) throws IOException { throw cause; }
            public Appendable append(char value) throws IOException { throw cause; }
        };
        JSONException exception = assertThrows(JSONException.class, () -> object.writeJSONString(broken));
        assertSame(cause, exception.getCause());
        assertEquals("append failed", exception.getMessage());
    }

    @Test
    void serializationPropagatesGetterAndOutputFailures() {
        assertThrows(IllegalStateException.class, () -> JSON.toJSONString(new BrokenBean()));
        assertThrows(IllegalStateException.class, () -> JSON.toJSONString(new BrokenBean(),
                JSON.DEFAULT_GENERATE_FEATURE));
        assertThrows(IllegalStateException.class, () -> JSON.toJSONBytes(new BrokenBean()));
        assertThrows(IllegalStateException.class, () -> JSON.writeJSONString(new StringWriter(), new BrokenBean()));
        IOException cause = new IOException("write failed");
        OutputStream broken = new OutputStream() {
            @Override
            public void write(int value) throws IOException { throw cause; }
        };
        assertSame(cause, assertThrows(IOException.class, () -> JSON.writeJSONString(broken, "value")));
    }

    public static final class BrokenBean {
        public String getValue() {
            throw new IllegalStateException("getter failed");
        }
    }

    private static String utf8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
