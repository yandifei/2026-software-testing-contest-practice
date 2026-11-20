package net.mooctest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Public entry points: checksum, classification, CVV and expiration state. */
public class CreditCardValidatorTest {
    private static final String VALID_VISA = "4111111111111111";

    @ParameterizedTest
    @ValueSource(strings = {"4111111111111111", "5555555555554444",
        "378282246310005", "6011111111111117", "79927398713"})
    void acceptsValidChecksums(String number) {
        assertTrue(new Validator(number).validate());
        assertTrue(new Validator(number, "12/99", "123").validate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"4111111111111112", "5555555555554445",
        "378282246310006", "6011111111111118", "1"})
    void rejectsInvalidChecksumsWithDomainException(String number) {
        InvalidCardException exception = assertThrows(InvalidCardException.class,
            () -> new Validator(number).validate());
        assertEquals("This card isn't invalid", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
        "4111111111111,VISA", "4111111111111111,VISA", "4999999999999999999,VISA",
        "341111111111111,AMERICAN_EXPRESS", "371111111111111,AMERICAN_EXPRESS",
        "5100000000000000,MASTERCARD", "5500000000000000,MASTERCARD",
        "2221000000000000,MASTERCARD", "2720990000000000,MASTERCARD",
        "6011000000000000,DISCOVER", "6221260000000000,DISCOVER",
        "6229250000000000,DISCOVER", "6440000000000000,DISCOVER",
        "6490000000000000,DISCOVER", "6500000000000000,DISCOVER",
        "9111111111111111,OTHER", "411111111111,OTHER",
        "41111111111111111111,OTHER", "3411111111111111,OTHER",
        "510000000000000,OTHER", "601100000000000,OTHER",
        "2220990000000000,OTHER", "2721000000000000,OTHER",
        "3528000000000000,OTHER", "000000,OTHER"
    })
    void classifiesByPrefixAndLength(String number, CreditCardType expected) {
        // The fallback must terminate too, rather than repeatedly scanning unknown types.
        assertEquals(expected, assertTimeoutPreemptively(Duration.ofSeconds(2),
            () -> TypeChecker.checkType(number)));
    }

    @ParameterizedTest
    @CsvSource({"0,false", "1,false", "2,false", "3,true", "4,true",
        "5,false", "6,false", "20,false"})
    void checksBothCvvLengthsAndAdjacentBoundaries(int length, boolean expected) {
        assertEquals(expected, new Validator(VALID_VISA, "12/99", "7".repeat(length)).checkCVV());
    }

    @Test
    void evaluatesExpirationRelativeToTheLoadedClock() {
        int year = DateChecker.CURRENT_YEAR;
        int month = DateChecker.CURRENT_MONTH;
        assertTrue(withDate(1, year + 1).checkExpirationDate());
        assertFalse(withDate(month, year).checkExpirationDate());
        assertFalse(withDate(month - 1, year).checkExpirationDate());
        assertFalse(withDate(month, year - 1).checkExpirationDate());
        if (month < 12) {
            assertTrue(withDate(month + 1, year).checkExpirationDate());
        }
        Validator unseparated = new Validator(VALID_VISA,
            String.format(Locale.ROOT, "01%02d", year + 1), "1234");
        unseparated.setExpirationMonth();
        unseparated.setExpirationYear();
        assertTrue(unseparated.checkExpirationDate());
        assertTrue(unseparated.checkCVV());
    }

    @Test
    void optionalFieldsAreNotInitializedByNumberOnlyConstructor() {
        Validator validator = new Validator(VALID_VISA);
        assertThrows(NullPointerException.class, validator::checkCVV);
        assertThrows(NullPointerException.class, validator::checkExpirationDate);
        assertThrows(NullPointerException.class, validator::setExpirationMonth);
        assertThrows(NullPointerException.class, validator::setExpirationYear);
        assertThrows(NullPointerException.class, () -> new Validator(null).validate());
        assertThrows(NullPointerException.class,
            () -> new Validator(VALID_VISA, "12/99", null).checkCVV());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1", "12", "12/", "1/2"})
    void rejectsDatesTooShortToContainMonthAndYear(String date) {
        assertThrows(IndexOutOfBoundsException.class, () -> new Validator(VALID_VISA, date, "123"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab/99", "12/ab", "12//99", "xx/yy"})
    void rejectsNonNumericDateSegments(String date) {
        assertThrows(NumberFormatException.class, () -> new Validator(VALID_VISA, date, "123"));
    }

    @Test
    void rejectsNullDatesAndUnparseableTypeInputs() {
        assertThrows(NullPointerException.class, () -> new Validator(VALID_VISA, null, "123"));
        assertThrows(NullPointerException.class, () -> TypeChecker.checkType(null));
        assertThrows(IndexOutOfBoundsException.class, () -> TypeChecker.checkType(""));
        assertThrows(IndexOutOfBoundsException.class, () -> TypeChecker.checkType("12345"));
    }

    private static Validator withDate(int month, int year) {
        return new Validator(VALID_VISA, String.format(Locale.ROOT, "%02d/%02d", month, year), "123");
    }
}
