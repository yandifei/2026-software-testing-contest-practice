package net.mooctest.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

/** Thread-safe parser for the date formats accepted by the exam domain. */
public final class DateTextParser {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            strict("uuuu-MM-dd"),
            strict("uuuu/MM/dd"),
            strict("uuuuMMdd"));

    private DateTextParser() {
    }

    public static LocalDate parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("date text must not be blank");
        }

        String value = text.trim();
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }
        throw new IllegalArgumentException("unsupported date: " + value);
    }

    private static DateTimeFormatter strict(String pattern) {
        return DateTimeFormatter.ofPattern(pattern)
                .withResolverStyle(ResolverStyle.STRICT);
    }
}
