package net.mooctest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

public class IdGeneratorTest {
    @ParameterizedTest
    @NullAndEmptySource
    void rejectsMissingIds(String value) {
        DomainException exception = assertThrows(DomainException.class, () -> IdGenerator.fromString(value));
        assertEquals("id string invalid", exception.getMessage());
        assertNull(exception.getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "-1", " 1", "1 ", "18446744073709551616", "1.0"})
    void wrapsMalformedOrOverflowingIds(String value) {
        DomainException exception = assertThrows(DomainException.class, () -> IdGenerator.fromString(value));
        assertEquals("id parse failed", exception.getMessage());
        assertInstanceOf(NumberFormatException.class, exception.getCause());
    }

    @Test
    void parsesUnsignedRangeAndRejectsZero() {
        assertEquals(1, IdGenerator.fromString("1"));
        assertEquals(12345, IdGenerator.fromString("12345"));
        assertEquals(Long.MAX_VALUE, IdGenerator.fromString("9223372036854775807"));
        assertEquals(Long.MIN_VALUE, IdGenerator.fromString("9223372036854775808"));
        assertEquals(-1, IdGenerator.fromString("18446744073709551615"));
        DomainException exception = assertThrows(DomainException.class, () -> IdGenerator.fromString("0"));
        assertEquals("id zero invalid", exception.getMessage());
        assertNull(exception.getCause());
        String generated = IdGenerator.nextIdStr();
        assertTrue(IdGenerator.fromString(generated) > 0);
        assertEquals(generated, Long.toUnsignedString(IdGenerator.fromString(generated)));
    }

    @Test
    void counterAdvancesAndBothSignBranchesProducePositiveIds() throws Exception {
        // Force otherwise clock-dependent branches; restore shared state even on assertion failure.
        Field field = IdGenerator.class.getDeclaredField("COUNTER");
        field.setAccessible(true);
        AtomicLong counter = (AtomicLong) field.get(null);
        long saved = counter.get();
        try {
            for (long seed : new long[] {0, Long.MIN_VALUE}) {
                counter.set(seed);
                long before = System.currentTimeMillis();
                long actual = IdGenerator.nextId();
                long after = System.currentTimeMillis();
                assertEquals(seed + 1, counter.get());
                assertTrue(actual > 0);
                boolean matches = false;
                for (long millis = before; millis <= after; millis++) {
                    long expected = (millis << 20) ^ (seed + 1);
                    if (expected < 0) expected = -expected;
                    if (actual == expected) matches = true;
                }
                assertTrue(matches, "ID must encode timestamp and incremented counter");
            }
        } finally {
            counter.set(saved);
        }
    }

    @Test
    void domainExceptionPreservesMessageAndCause() {
        Throwable cause = new IllegalArgumentException("root");
        DomainException exception = new DomainException("context", cause);
        assertEquals("context", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("plain", new DomainException("plain").getMessage());
    }
}
