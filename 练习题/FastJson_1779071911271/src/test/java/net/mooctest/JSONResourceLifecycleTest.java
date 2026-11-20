package net.mooctest;

import com.alibaba.fastjson.parser.JSONLexerBase;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ensures public operations release their buffers for reuse instead of allocating on every call. */
class JSONResourceLifecycleTest {
    @ParameterizedTest(name = "{0} returns its character buffer to the pool")
    @MethodSource("writerOperations")
    void serializationReturnsItsBufferOnCompletion(String name, Operation operation) throws Exception {
        assertBufferPoolState(SerializeWriter.class, "bufLocal", operation, true);
    }

    static Stream<Arguments> writerOperations() {
        JSONObject object = new JSONObject().fluentPut("key", "value");
        return Stream.of(
                Arguments.of("string with features", (Operation) () -> JSON.toJSONString(Map.of("key", 4), 0)),
                Arguments.of("string with config", (Operation) () -> JSON.toJSONString(Map.of("key", 4))),
                Arguments.of("encoded bytes", (Operation) () -> JSON.toJSONBytes(Map.of("key", 4))),
                Arguments.of("instance string", (Operation) object::toJSONString),
                Arguments.of("instance features", (Operation) () -> object.toString(SerializerFeature.UseSingleQuotes)),
                Arguments.of("appendable", (Operation) () -> object.writeJSONString(new StringBuilder())),
                Arguments.of("output stream", (Operation) () ->
                        JSON.writeJSONString(new ByteArrayOutputStream(), object)));
    }

    @ParameterizedTest(name = "{0} returns its scanner buffer to the pool")
    @MethodSource("validatorOperations")
    void validationReturnsItsBufferOnCompletion(String name, Operation operation) throws Exception {
        assertBufferPoolState(JSONLexerBase.class, "SBUF_LOCAL", operation, true);
    }

    @ParameterizedTest(name = "{0} empty input does not allocate a scanner buffer")
    @MethodSource("emptyValidatorOperations")
    void emptyValidationAvoidsAllocatingScannerStorage(String name, Operation operation) throws Exception {
        assertBufferPoolState(JSONLexerBase.class, "SBUF_LOCAL", operation, false);
    }

    static Stream<Arguments> emptyValidatorOperations() {
        return Stream.of(
                Arguments.of("general", (Operation) () -> assertFalse(JSON.isValid(""))),
                Arguments.of("object", (Operation) () -> assertFalse(JSON.isValidObject(""))),
                Arguments.of("array", (Operation) () -> assertFalse(JSON.isValidArray(""))));
    }

    static Stream<Arguments> validatorOperations() {
        return Stream.of(
                Arguments.of("general", (Operation) () -> assertTrue(JSON.isValid("{\"id\":7}"))),
                Arguments.of("object", (Operation) () -> assertTrue(JSON.isValidObject("{\"id\":7}"))),
                Arguments.of("array", (Operation) () -> assertTrue(JSON.isValidArray("[1,2]"))));
    }

    private static void assertBufferPoolState(Class<?> owner, String fieldName, Operation operation,
            boolean bufferExpected) throws Exception {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<char[]> pool = (ThreadLocal<char[]>) field.get(null);
        char[] original = pool.get();
        try {
            pool.remove();
            operation.run();
            if (bufferExpected) {
                assertNotNull(pool.get(), "The completed operation must release its reusable character buffer");
            } else {
                assertNull(pool.get(), "Empty-input validation must not allocate scanner storage");
            }
        } finally {
            if (original == null) {
                pool.remove();
            } else {
                pool.set(original);
            }
        }
    }

    @FunctionalInterface
    interface Operation {
        void run() throws Exception;
    }
}
