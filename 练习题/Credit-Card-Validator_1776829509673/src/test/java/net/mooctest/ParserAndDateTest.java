package net.mooctest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ParserAndDateTest {
    @Test
    void parsesDigitsInOrderWithoutDroppingLeadingZeroes() {
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
            CreditCardParser.parseNumber("0123456789"));
        assertEquals(List.of(9, 8, 7, 6, 5, 4, 3, 2, 1, 0),
            CreditCardParser.parseNumber("9876543210"));
        assertEquals(List.of(), CreditCardParser.parseNumber(""));
        List<Integer> first = CreditCardParser.parseNumber("123");
        List<Integer> second = CreditCardParser.parseNumber("123");
        first.set(0, 9);
        assertEquals(List.of(1, 2, 3), second);
        assertThrows(NullPointerException.class, () -> CreditCardParser.parseNumber(null));
    }

    @Test
    void extractsRequestedPrefixWithoutChangingTheInput() {
        List<Integer> digits = List.of(6, 2, 2, 1, 2, 6, 9);
        assertEquals(6, CreditCardParser.parseIIN(digits, 1));
        assertEquals(62, CreditCardParser.parseIIN(digits, 2));
        assertEquals(622, CreditCardParser.parseIIN(digits, 3));
        assertEquals(6221, CreditCardParser.parseIIN(digits, 4));
        assertEquals(62212, CreditCardParser.parseIIN(digits, 5));
        assertEquals(622126, CreditCardParser.parseIIN(digits, 6));
        assertEquals(6221269, CreditCardParser.parseIIN(digits, 7));
        assertEquals(List.of(6, 2, 2, 1, 2, 6, 9), digits);
        assertEquals(1, CreditCardParser.parseIIN(List.of(0, 0, 0, 0, 0, 1), 6));
        assertEquals(0, CreditCardParser.parseIIN(List.of(0), 1));
        assertEquals(Integer.MAX_VALUE,
            CreditCardParser.parseIIN(List.of(2, 1, 4, 7, 4, 8, 3, 6, 4, 7), 10));
    }

    @Test
    void rejectsInvalidPrefixLengthsAndOverflow() {
        assertThrows(NumberFormatException.class, () -> CreditCardParser.parseIIN(List.of(1), 0));
        assertThrows(NegativeArraySizeException.class, () -> CreditCardParser.parseIIN(List.of(1), -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CreditCardParser.parseIIN(List.of(1), 2));
        assertThrows(NullPointerException.class, () -> CreditCardParser.parseIIN(null, 1));
        assertThrows(NumberFormatException.class,
            () -> CreditCardParser.parseIIN(List.of(2, 1, 4, 7, 4, 8, 3, 6, 4, 8), 10));
    }

    @ParameterizedTest
    @CsvSource({"12/29,1229", "1229,1229", "/1229,1229", "1229/,1229",
        "12//29,12/29", "'',''", "/,''"})
    void removesOnlyTheFirstSlashInPlace(String input, String expected) {
        StringBuilder date = new StringBuilder(input);
        assertSame(date, DateParser.parseDate(date));
        assertEquals(expected, date.toString());
    }

    @Test
    void convertsDateToIndependentMutableBuffer() {
        String original = "01/29";
        StringBuilder first = DateChecker.convertDate(original);
        StringBuilder second = DateChecker.convertDate(original);
        assertEquals(original, first.toString());
        assertNotSame(first, second);
        first.deleteCharAt(2);
        assertEquals("0129", first.toString());
        assertEquals("01/29", second.toString());
        assertEquals("", DateChecker.convertDate("").toString());
        assertThrows(NullPointerException.class, () -> DateChecker.convertDate(null));
        assertThrows(NullPointerException.class, () -> DateParser.parseDate(null));
    }

    @Test
    void parsesExactHalfOpenDateSegments() {
        StringBuilder date = new StringBuilder("0129");
        assertEquals(1, DateParser.parseDate(date, 0, 2));
        assertEquals(29, DateParser.parseDate(date, 2, 4));
        assertEquals(12, DateParser.parseDate(date, 1, 3));
        assertEquals(129, DateParser.parseDate(date, 0, 4));
        assertEquals("0129", date.toString());
        assertEquals(-1, DateParser.parseDate(new StringBuilder("-1"), 0, 2));
        assertThrows(NumberFormatException.class, () -> DateParser.parseDate(date, 1, 1));
        assertThrows(NumberFormatException.class,
            () -> DateParser.parseDate(new StringBuilder("ab"), 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> DateParser.parseDate(date, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> DateParser.parseDate(date, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> DateParser.parseDate(date, 3, 2));
        assertThrows(NullPointerException.class, () -> DateParser.parseDate(null, 0, 2));
    }

    @ParameterizedTest
    @CsvSource({"1,0,true", "0,0,false", "-1,0,false", "0,-1,true",
        "12,11,true", "12,12,false", "11,12,false",
        "2147483647,-2147483648,true", "-2147483648,2147483647,false",
        "2147483647,2147483647,false", "-2147483648,-2147483648,false"})
    void comparesDatesStrictlyWithoutOverflow(int date, int current, boolean expected) {
        assertEquals(expected, DateChecker.compareDates(date, current));
    }

    @Test
    void formatsTwoDigitYearAndMonthWithFreshFormatters() {
        Calendar calendar = new GregorianCalendar();
        calendar.clear();
        calendar.set(2007, Calendar.FEBRUARY, 15, 12, 0, 0);
        DateFormat year = DateFormatter.yearFormat();
        DateFormat month = DateFormatter.monthFormat();
        assertEquals("yy", ((SimpleDateFormat) year).toPattern());
        assertEquals("MM", ((SimpleDateFormat) month).toPattern());
        assertEquals("07", year.format(calendar.getTime()));
        assertEquals("02", month.format(calendar.getTime()));
        calendar.set(2099, Calendar.DECEMBER, 15, 12, 0, 0);
        assertEquals("99", year.format(calendar.getTime()));
        assertEquals("12", month.format(calendar.getTime()));
        assertNotSame(year, DateFormatter.yearFormat());
        assertNotSame(month, DateFormatter.monthFormat());
    }

    @Test
    void readsTheCurrentCalendarYearAndMonth() {
        Calendar before = Calendar.getInstance();
        int year = DateChecker.getCurrentYear();
        int month = DateChecker.getCurrentMonth();
        Calendar after = Calendar.getInstance();
        assertTrue(year == before.get(Calendar.YEAR) % 100 || year == after.get(Calendar.YEAR) % 100);
        assertTrue(month == before.get(Calendar.MONTH) + 1 || month == after.get(Calendar.MONTH) + 1);
        assertFalse(month < 1 || month > 12);
    }

    @Test
    void utilityConstructorsRemainAccessibleInTheirPackage() {
        assertNotNull(new CreditCardParser());
        assertNotNull(new DateChecker());
        assertNotNull(new DateFormatter());
        assertNotNull(new DateParser());
        assertNotNull(new TypeChecker());
    }
}
