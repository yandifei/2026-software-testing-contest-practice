package net.mooctest;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ExtraProcessor;
import com.alibaba.fastjson.parser.deserializer.ExtraTypeProvider;
import com.alibaba.fastjson.parser.deserializer.FieldTypeResolver;
import com.alibaba.fastjson.parser.deserializer.ParseProcess;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parsing contracts: input boundaries, feature propagation, references and encodings. */
class JSONParsingTest {
    public static class Bean {
        public int id;
        public String name;
    }

    @Test
    void stringOverloadsHonorFeaturesAndResolveReferences() {
        String text = "{1:'one'}";
        assertEquals("one", ((Map<?, ?>) JSON.parse(text)).get(1));
        assertEquals("one", ((Map<?, ?>) JSON.parse(text, new ParserConfig())).get(1));
        assertEquals("one", ((Map<?, ?>) JSON.parse(text, Feature.NonStringKeyAsString)).get("1"));
        assertEquals("one", ((Map<?, ?>) JSON.parse(text, new ParserConfig(),
                Feature.NonStringKeyAsString)).get("1"));
        assertNull(JSON.parse((String) null));
        assertNull(JSON.parse(""));
        Map<?, ?> result = (Map<?, ?>) JSON.parse("{a:{id:7},b:{$ref:'$.a'}}");
        assertSame(result.get("a"), result.get("b"));
        assertThrows(JSONException.class, () -> JSON.parse("1 2"));
    }

    @Test
    void objectParsingWrapsMapsAndReportsNonObjectInput() {
        assertEquals(7, JSON.parseObject("{id:7}").getIntValue("id"));
        assertNull(JSON.parseObject((String) null));
        assertNull(JSON.parseObject("null"));
        JSONException exception = assertThrows(JSONException.class, () -> JSON.parseObject("17"));
        assertInstanceOf(ClassCastException.class, exception.getCause());
        assertThrows(JSONException.class, () -> JSON.parseObject("[1]"));
        // The feature overload currently casts Alibaba's JSONObject to the relocated class.
        assertThrows(ClassCastException.class, () -> JSON.parseObject("{}", Feature.OrderedField));
        assertNull(JSON.parseObject("null", Feature.OrderedField));
    }

    @Test
    void typedStringOverloadsPreserveValuesAndRequestedGenericType() {
        String text = "{id:7,name:'Ada'}";
        Type beanType = Bean.class;
        assertEquals(7, JSON.parseObject(text, Bean.class).id);
        assertEquals("Ada", JSON.parseObject(text, Bean.class, Feature.SupportArrayToBean).name);
        assertEquals(7, JSON.<Bean>parseObject(text, beanType).id);
        assertEquals(7, JSON.<Bean>parseObject(text, beanType, new ParserConfig()).id);
        assertEquals(7, JSON.<Bean>parseObject(text, beanType, JSON.DEFAULT_PARSER_FEATURE,
                Feature.AllowSingleQuotes).id);
        assertEquals(7, JSON.<Bean>parseObject(text, beanType, new ParserConfig(),
                JSON.DEFAULT_PARSER_FEATURE, Feature.AllowSingleQuotes).id);
        assertEquals(7, JSON.parseObject(text, Bean.class, (ParseProcess) null).id);
        assertEquals(7, JSON.<Bean>parseObject(text, beanType, (ParseProcess) null).id);
        assertEquals(7, JSON.<Bean>parseObject(text, beanType, new ParserConfig(), null,
                JSON.DEFAULT_PARSER_FEATURE, (Feature[]) null).id);
        List<Integer> values = JSON.parseObject("[1,3]", new TypeReference<List<Integer>>() { });
        assertEquals(List.of(1, 3), values);
        assertNull(JSON.parseObject((String) null, beanType, JSON.DEFAULT_PARSER_FEATURE));
        assertNull(JSON.parseObject((String) null, Bean.class));
        assertNull(JSON.parseObject("", Bean.class));
        assertThrows(JSONException.class, () -> JSON.parseObject("{} {}", beanType,
                JSON.DEFAULT_PARSER_FEATURE));
        assertThrows(JSONException.class, () -> JSON.parseObject("{} {}", Bean.class));
    }

    @Test
    void parseProcessesReceiveConvertedExtrasAndResolveFieldTypes() {
        ExtraCapture capture = new ExtraCapture();
        Bean bean = JSON.parseObject("{id:9,extra:'41'}", Bean.class, capture);
        assertEquals(9, bean.id);
        assertSame(bean, capture.object);
        assertEquals("extra", capture.key);
        assertEquals(41L, capture.value);
        assertEquals(Bean.class, capture.object.getClass());
        Bean basic = JSON.parseObject("{id:3}", Bean.class, new ParseProcess() { });
        assertEquals(3, basic.id);
        FieldTypeResolver resolver = (object, field) -> "child".equals(field) ? Bean.class : null;
        Map<?, ?> map = JSON.parseObject("{child:{id:12}}", Map.class, resolver);
        assertEquals(12, ((Bean) map.get("child")).id);
    }

    private static final class ExtraCapture implements ExtraTypeProvider, ExtraProcessor {
        private Object object;
        private String key;
        private Object value;

        @Override
        public Type getExtraType(Object target, String property) {
            return Long.class;
        }

        @Override
        public void processExtra(Object target, String property, Object extra) {
            object = target;
            key = property;
            value = extra;
        }
    }

    @Test
    void byteParsingUsesSlicesUtf8AndReusableDecoders() {
        byte[] utf8 = "{name:'中文',id:6}".getBytes(StandardCharsets.UTF_8);
        assertEquals("中文", ((Map<?, ?>) JSON.parse(utf8)).get("name"));
        assertNull(JSON.parse(new byte[]{(byte) 0xC3, 0x28}));
        byte[] sliced = "xx{1:'one'}yy".getBytes(StandardCharsets.UTF_8);
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        assertEquals("one", ((Map<?, ?>) JSON.parse(sliced, 2, sliced.length - 4,
                decoder, Feature.NonStringKeyAsString)).get("1"));
        assertEquals("one", ((Map<?, ?>) JSON.parse(sliced, 2, sliced.length - 4,
                decoder, JSON.DEFAULT_PARSER_FEATURE)).get(1));
        assertNull(JSON.parse(null, 0, 0, decoder));
        assertNull(JSON.parse(new byte[0], 0, 0, decoder));
        assertEquals("中文", JSON.<Bean>parseObject(utf8, Bean.class).name);
        assertEquals(6, JSON.<Bean>parseObject(utf8, 0, utf8.length, (java.nio.charset.Charset) null,
                Bean.class).id);
        assertEquals(6, JSON.<Bean>parseObject(utf8, StandardCharsets.UTF_8, Bean.class,
                new ParserConfig(), null, JSON.DEFAULT_PARSER_FEATURE).id);
        assertEquals(6, JSON.<Bean>parseObject(utf8, 0, utf8.length, decoder, Bean.class,
                Feature.AllowSingleQuotes).id);
        byte[] utf16 = "xx{id:19}yy".getBytes(StandardCharsets.UTF_16LE);
        assertEquals(19, JSON.<Bean>parseObject(utf16, 4, utf16.length - 8,
                StandardCharsets.UTF_16LE, Bean.class).id);
        assertNull(JSON.parseObject(utf8, 0, -1, StandardCharsets.ISO_8859_1, Bean.class));
        assertNull(JSON.parseObject(utf8, 0, 0, StandardCharsets.ISO_8859_1, Bean.class));
        assertThrows(IndexOutOfBoundsException.class, () -> JSON.parseObject(utf8, utf8.length + 1,
                0, StandardCharsets.ISO_8859_1, Bean.class));
        assertNull(JSON.parseObject(new byte[]{(byte) 0xFF}, Bean.class));
        assertNull(JSON.parseObject(new byte[0], Bean.class));
    }

    @Test
    void characterParsingRespectsLogicalLengthAndFeatures() {
        char[] input = "{id:8}ignored".toCharArray();
        assertEquals(8, JSON.<Bean>parseObject(input, 6, Bean.class, Feature.AllowUnQuotedFieldNames).id);
        assertNull(JSON.parseObject((char[]) null, 0, Bean.class));
        assertNull(JSON.parseObject(new char[0], 0, Bean.class));
        assertThrows(JSONException.class, () -> JSON.parseObject("{} {}".toCharArray(), 5, Bean.class));
    }

    @Test
    void streamOverloadsHandleMultipleReadsGrowthAndIoFailure() throws IOException {
        byte[] data = "{id:14}".getBytes(StandardCharsets.UTF_8);
        assertEquals(14, JSON.<Bean>parseObject(new ByteArrayInputStream(data), Bean.class).id);
        assertEquals(14, JSON.<Bean>parseObject(new ByteArrayInputStream(data),
                StandardCharsets.UTF_8, Bean.class).id);
        assertEquals(14, JSON.<Bean>parseObject(new ByteArrayInputStream(data),
                null, Bean.class, new ParserConfig()).id);
        String longValue = "x".repeat(100_000) + "终";
        byte[] large = ("\"" + longValue + "\"").getBytes(StandardCharsets.UTF_8);
        InputStream progressing = new ByteArrayInputStream(large) {
            @Override
            public synchronized int read(byte[] buffer, int offset, int length) {
                assertTrue(length > 0, "A parser must grow a full buffer before requesting more input");
                return super.read(buffer, offset, length);
            }
        };
        assertEquals(longValue, JSON.parseObject(progressing, String.class));
        assertNull(JSON.parseObject(new ByteArrayInputStream(new byte[0]), Bean.class));
        IOException failure = new IOException("read failed");
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw failure;
            }
        };
        assertSame(failure, assertThrows(IOException.class, () -> JSON.parseObject(broken, Bean.class)));
    }

    @Test
    void arrayOverloadsDistinguishNullEmptyTypedAndHeterogeneousArrays() {
        for (String empty : new String[]{null, "", "  ", "null"}) {
            assertNull(JSON.parseArray(empty));
            assertNull(JSON.parseArray(empty, Integer.class));
        }
        assertEquals(List.of(), JSON.parseArray("[]"));
        assertEquals(List.of(2, 4), JSON.parseArray("[2,4]"));
        assertEquals(List.of(2, 4), JSON.parseArray("['2',4]", Integer.class));
        assertNull(JSON.parseArray(null, new Type[]{Integer.class}));
        assertNull(JSON.parseArray("null", new Type[]{Integer.class}));
        assertEquals(List.of(7, "x"), JSON.parseArray("[7,'x']", new Type[]{Integer.class, String.class}));
        JSONArray refs = JSON.parseArray("[{id:1},{$ref:'$[0]'}]");
        assertSame(refs.get(0), refs.get(1));
        assertThrows(JSONException.class, () -> JSON.parseArray("[1] [2]"));
        assertThrows(JSONException.class, () -> JSON.parseArray("[1] [2]", Integer.class));
        assertThrows(JSONException.class, () -> JSON.parseArray("[1] [2]", new Type[]{Integer.class}));
    }

    @Test
    void explicitResolveHelperLinksForwardReferences() {
        try (DefaultJSONParser parser = new DefaultJSONParser("{first:{$ref:'$.last'},last:{id:5}}")) {
            Map<?, ?> map = (Map<?, ?>) parser.parse();
            assertNull(map.get("first"));
            JSON.handleResovleTask(parser, map);
            assertSame(map.get("last"), map.get("first"));
        }
    }
}
