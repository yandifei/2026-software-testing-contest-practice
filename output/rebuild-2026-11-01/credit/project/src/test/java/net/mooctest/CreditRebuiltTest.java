package net.mooctest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Fresh reconstruction from the supplied API, without production changes.
 * Format fixtures specify the supplied implementation's accepted language.
 * Characterization cases name behavior that is not a real banking guarantee.
 */
public class CreditRebuiltTest {
    private static Arguments profile(String name, Function<String, CreditCardValidator> factory,
            int min, int max, String accepted, String rejected) {
        return Arguments.of(name, factory, min, max, accepted.split(" "), rejected.split(" "));
    }

    static Stream<Arguments> formats() {
        return Stream.of(
            profile("Visa", VisaValidator::new, 13, 19, "4", "3 5 0"),
            profile("American Express", AmericanExpressValidator::new, 15, 15,
                    "34 35 36 37", "33 38 99"),
            profile("Mastercard", MasterCardValidator::new, 16, 16,
                    "51 53 55 222100 222101 250000 272098 272099", "50 56 222099 272100 99"),
            profile("Discover", DiscoverValidator::new, 16, 16,
                    "6011 622126 622127 622500 622924 622925 644 647 649 65",
                    "6010 6012 622125 622926 643 66 99"),
            profile("Visa Electron", VisaElectronValidator::new, 16, 16,
                    "4026 4405 4508 4844 4913 4917 04026 04405 04508 04844 04913 04917 4H500",
                    "4025 4027 4404 4406 4507 4509 4843 4845 4912 4914 4916 4918 417500 99"),
            profile("Maestro", MaestroValidator::new, 12, 19,
                    "500000 500001 505000 509998 509999 560000 560001 575000 589998 589999 600000 600001 650000 699998 699999",
                    "499999 510000 559999 590000 599999 700000 99"),
            profile("Maestro UK", MaestroUKValidator::new, 12, 19,
                    "6759 676770 676774", "6758 6760 676769 676771 676772 676773 676775 99"),
            profile("JCB", JCBValidator::new, 16, 16,
                    "3528 3529 3558 3588 3589", "3527 3590 99"),
            profile("China TUnion", ChinaTUnionValidator::new, 19, 19, "31", "30 32 99"),
            profile("China UnionPay", ChinaUnionPayValidator::new, 16, 19, "62", "61 63 99"),
            profile("Diners International", DinersClubInternationalValidator::new, 16, 19,
                    "300 301 304 305 3095 38 39", "299 306 3094 3096 37 40 99"),
            profile("Diners", DinersClubValidator::new, 16, 16, "54 55", "53 56 99"),
            profile("RuPay", RuPayValidator::new, 16, 16, "60 6521 6522", "59 61 6520 6523 99"),
            profile("InterPayment", InterPaymentValidator::new, 16, 19, "636", "635 637 639 99"),
            profile("InstaPayment", InstaPaymenttValidator::new, 16, 16, "637 638 639", "636 640 99"),
            profile("Dankort", DankortValidator::new, 16, 16,
                    "4571 4572 4800 5018 5019", "4570 5020 99"),
            profile("MIR", MIRValidator::new, 16, 16, "2200 2201 2202 2203 2204", "2199 2205 99"),
            profile("NPS", NPS_PridnestrovieValidator::new, 16, 16,
                    "6054740 6054741 6054742 6054743 6054744", "6054739 6054745 99"),
            profile("Troy", TroyValidator::new, 16, 16,
                    "979200 979201 979250 979288 979289", "979199 979290 99"),
            profile("UATP", UATPValidator::new, 15, 15, "000001", "000000 000002 100000 99"),
            profile("Verve", VerveValidator::new, 16, 19,
                    "506099 506100 506150 506197 506198", "506098 506199 650002 650015 650027 99"),
            profile("LankaPay", LankaPayValidator::new, 16, 16, "357111", "357110 357112 99")
        );
    }

    @ParameterizedTest(name="{0}: length, IIN and combined contract")
    @MethodSource("formats")
    void suppliedCardProfilesExerciseEveryBoundary(String name,
            Function<String, CreditCardValidator> factory, int min, int max,
            String[] accepted, String[] rejected) {
        for (int n = 0; n <= 21; n++) {
            boolean lengthAllowed = min <= n && n <= max;
            assertEquals(lengthAllowed, factory.apply("0".repeat(n)).checkLength(), name + " length " + n);
            if (n >= 7) {
                assertEquals(lengthAllowed, factory.apply(padded(accepted[0], n)).validate(), name + " combined " + n);
            }
        }
        for (String prefix : accepted) {
            CreditCardValidator v = factory.apply(padded(prefix, min));
            assertTrue(v.checkIINRanges(), name + " accepts " + prefix);
            assertTrue(v.validate(), name + " combines " + prefix);
        }
        for (String prefix : rejected) {
            CreditCardValidator v = factory.apply(padded(prefix, min));
            assertFalse(v.checkIINRanges(), name + " rejects " + prefix);
            assertFalse(v.validate(), name + " rejects combined " + prefix);
        }
        assertThrows(NullPointerException.class, () -> factory.apply(null));
        CreditCardValidator empty = factory.apply("");
        assertThrows(IndexOutOfBoundsException.class, empty::checkIINRanges);
        assertThrows(IndexOutOfBoundsException.class, empty::validate,
                "supplied base class eagerly checks IIN even for a bad length");
    }

    private static String padded(String prefix, int length) {
        return prefix + "0".repeat(length - prefix.length());
    }

    @ParameterizedTest
    @CsvSource({"0,0", "7,7", "10,1", "18,9", "123,6", "90909,27", "1000000000,1", "2147483647,46"})
    void decimalDigitSumHasLiteralExpectedValues(int number, int sum) {
        assertEquals(sum, new LuhnValidator().sumOfDigits(number));
    }

    @Test
    void digitSumRejectsNegativeDecimalRepresentation() {
        LuhnValidator luhn = new LuhnValidator();
        assertThrows(NumberFormatException.class, () -> luhn.sumOfDigits(-1));
        assertThrows(NumberFormatException.class, () -> luhn.sumOfDigits(Integer.MIN_VALUE));
        assertThrows(NullPointerException.class, () -> luhn.validate(null));
    }

    @ParameterizedTest
    @CsvSource({"0,true", "9,false", "18,true", "91,true", "19,false", "1230,true", "1231,false",
            "79927398713,true", "79927398714,false", "4012888888881881,true",
            "378282246310005,true", "5555555555554444,true", "6011111111111117,true"})
    void luhnChecksHandCalculatedSamples(String digits, boolean expected) {
        LuhnValidator luhn = new LuhnValidator();
        assertEquals(expected, luhn.algorithmCheck(digits));
        assertEquals(expected, luhn.validate(digits));
    }

    @Test
    void luhnMatchesIndependentLookupForAllShortNumbers() {
        // Two-state weighted-digit oracle: literal weights, without parsing/reversing production lists.
        int[] doubleWeights = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
        LuhnValidator luhn = new LuhnValidator();
        for (int a = 0; a < 10; a++) {
            for (int b = 0; b < 10; b++) {
                assertEquals((doubleWeights[a] + b) % 10 == 0, luhn.validate("" + a + b));
                for (int c = 0; c < 10; c++) {
                    assertEquals((a + doubleWeights[b] + c) % 10 == 0, luhn.validate("" + a + b + c));
                }
            }
        }
    }

    @Test
    void eachSingleDigitReplacementBreaksFixedValidCards() {
        LuhnValidator luhn = new LuhnValidator();
        for (String valid : List.of("18", "1230", "79927398713", "4012888888881881", "378282246310005")) {
            assertTrue(luhn.validate(valid));
            for (int position = 0; position < valid.length(); position++) {
                for (char replacement = '0'; replacement <= '9'; replacement++) {
                    if (replacement == valid.charAt(position)) continue;
                    char[] changed = valid.toCharArray();
                    changed[position] = replacement;
                    assertFalse(luhn.validate(new String(changed)), "one substituted decimal digit");
                }
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"-0,false", "-2,true", "0-,false", "2-,false", "½0,false", "½4,true", "A,true", "' 2',true"})
    void characterizesUnicodeAndNegativeNumericContributions(String number, boolean expected) {
        // Parsing accepts Character.getNumericValue results; this is not a banking acceptance rule.
        LuhnValidator luhn = new LuhnValidator();
        assertEquals(expected, luhn.algorithmCheck(number));
        assertEquals(expected, luhn.validate(number));
    }

    @Test
    void parserPreservesOrderLeadingZeroAndIndependentResults() {
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), CreditCardParser.parseNumber("0123456789"));
        assertEquals(List.of(10, 35, -1, -2, 4), CreditCardParser.parseNumber("Az-½４"));
        assertEquals(List.of(), CreditCardParser.parseNumber(""));
        List<Integer> left = CreditCardParser.parseNumber("012");
        List<Integer> right = CreditCardParser.parseNumber("012");
        left.clear();
        assertEquals(List.of(0, 1, 2), right);
        assertThrows(NullPointerException.class, () -> CreditCardParser.parseNumber(null));
        List<Integer> digits = List.of(0, 7, 2, 8, 3, 6, 1);
        int[] prefixes = {0, 7, 72, 728, 7283, 72836, 728361};
        for (int n = 1; n <= digits.size(); n++) {
            assertEquals(prefixes[n - 1], CreditCardParser.parseIIN(digits, n));
        }
        assertEquals(List.of(0, 7, 2, 8, 3, 6, 1), digits);
        assertEquals(Integer.MAX_VALUE, CreditCardParser.parseIIN(List.of(2,1,4,7,4,8,3,6,4,7), 10));
        assertThrows(NumberFormatException.class, () -> CreditCardParser.parseIIN(List.of(2,1,4,7,4,8,3,6,4,8), 10));
        assertThrows(NumberFormatException.class, () -> CreditCardParser.parseIIN(List.of(1), 0));
        assertThrows(NegativeArraySizeException.class, () -> CreditCardParser.parseIIN(List.of(1), -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CreditCardParser.parseIIN(List.of(1), 2));
        assertThrows(NullPointerException.class, () -> CreditCardParser.parseIIN(null, 1));
    }

    @ParameterizedTest
    @CsvSource({"'11/28','1128'", "'1128','1128'", "'/1128','1128'", "'1128/','1128'",
            "'11//28','11/28'", "'/',''", "'',''"})
    void dateSlashRemovalMutatesExactlyTheFirstOccurrence(String input, String expected) {
        StringBuilder buffer = new StringBuilder(input);
        assertSame(buffer, DateParser.parseDate(buffer));
        assertEquals(expected, buffer.toString());
    }

    @Test
    void dateBuffersAreIndependentAndSlicesAreHalfOpen() {
        StringBuilder a = DateChecker.convertDate("0728");
        StringBuilder b = DateChecker.convertDate("0728");
        assertNotSame(a, b);
        assertEquals(7, DateParser.parseDate(a, 0, 2));
        assertEquals(28, DateParser.parseDate(a, 2, 4));
        assertEquals(72, DateParser.parseDate(a, 1, 3));
        a.setCharAt(0, '9');
        assertEquals("0728", b.toString());
        assertEquals("", DateChecker.convertDate("").toString());
        assertThrows(NullPointerException.class, () -> DateChecker.convertDate(null));
        assertThrows(NullPointerException.class, () -> DateParser.parseDate(null));
        assertThrows(NullPointerException.class, () -> DateParser.parseDate(null, 0, 1));
        assertThrows(NumberFormatException.class, () -> DateParser.parseDate(b, 2, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> DateParser.parseDate(b, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> DateParser.parseDate(b, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> DateParser.parseDate(b, 0, 5));
        assertThrows(NumberFormatException.class, () -> DateParser.parseDate(new StringBuilder("ab"), 0, 2));
    }

    @ParameterizedTest
    @CsvSource({"0,0,false", "1,0,true", "-1,0,false", "0,-1,true", "12,11,true", "12,12,false",
            "11,12,false", "2147483647,-2147483648,true", "-2147483648,2147483647,false"})
    void dateComparisonIsStrictAndDoesNotSubtractOverflowingValues(int candidate, int current, boolean expected) {
        assertEquals(expected, DateChecker.compareDates(candidate, current));
    }

    @Test
    void dateFormattersAreFreshAndProducePaddedMonthAndYear() {
        Calendar when = new GregorianCalendar(2003, Calendar.APRIL, 17, 12, 0, 0);
        DateFormat year = DateFormatter.yearFormat();
        DateFormat month = DateFormatter.monthFormat();
        assertEquals("yy", ((SimpleDateFormat) year).toPattern());
        assertEquals("MM", ((SimpleDateFormat) month).toPattern());
        assertEquals("03", year.format(when.getTime()));
        assertEquals("04", month.format(when.getTime()));
        when.set(2098, Calendar.NOVEMBER, 17);
        assertEquals("98", year.format(when.getTime()));
        assertEquals("11", month.format(when.getTime()));
        assertNotSame(year, DateFormatter.yearFormat());
        assertNotSame(month, DateFormatter.monthFormat());
        Calendar before = Calendar.getInstance();
        int actualYear = DateChecker.getCurrentYear();
        int actualMonth = DateChecker.getCurrentMonth();
        Calendar after = Calendar.getInstance();
        assertTrue(actualYear == before.get(Calendar.YEAR) % 100 || actualYear == after.get(Calendar.YEAR) % 100);
        assertTrue(actualMonth == before.get(Calendar.MONTH) + 1 || actualMonth == after.get(Calendar.MONTH) + 1);
    }

    @ParameterizedTest
    @CsvSource({"4,13,VISA", "4,19,VISA", "4,12,OTHER", "4,20,OTHER",
            "34,15,AMERICAN_EXPRESS", "37,15,AMERICAN_EXPRESS", "34,16,OTHER",
            "51,16,MASTERCARD", "55,16,MASTERCARD", "222100,16,MASTERCARD", "272099,16,MASTERCARD",
            "50,16,OTHER", "222099,16,OTHER", "272100,16,OTHER",
            "6011,16,DISCOVER", "622126,16,DISCOVER", "622925,16,DISCOVER", "644,16,DISCOVER", "649,16,DISCOVER", "65,16,DISCOVER",
            "6011,15,OTHER", "3528,16,OTHER", "9,16,OTHER"})
    void typeSelectionRespectsOrderedRecognizersAndTerminates(String prefix, int length, CreditCardType expected) {
        assertEquals(expected, assertTimeoutPreemptively(Duration.ofSeconds(1),
                () -> TypeChecker.checkType(padded(prefix, length))));
    }

    @Test
    void typeSelectionPropagatesParsingErrors() {
        assertThrows(NullPointerException.class, () -> TypeChecker.checkType(null));
        assertThrows(IndexOutOfBoundsException.class, () -> TypeChecker.checkType(""));
        assertThrows(IndexOutOfBoundsException.class, () -> TypeChecker.checkType("12345"));
    }

    @Test
    void completeValidatorReturnsTrueOrDomainFailureFromChecksum() {
        for (String valid : List.of("4012888888881881", "5555555555554444", "378282246310005")) {
            assertTrue(new Validator(valid).validate());
            assertTrue(new Validator(valid, "01/00", "").validate(), "validate ignores CVV and expiration");
        }
        for (String invalid : List.of("4012888888881882", "1", "1231")) {
            InvalidCardException error = assertThrows(InvalidCardException.class, () -> new Validator(invalid).validate());
            assertEquals("This card isn't invalid", error.getMessage());
            assertNull(error.getCause());
        }
        assertTrue(new Validator("").validate(), "characterizes empty checksum acceptance");
        assertThrows(NullPointerException.class, () -> new Validator(null).validate());
        assertEquals("literal", new InvalidCardException("literal").getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints={0,1,2,3,4,5,6,10})
    void cvvAcceptsExactlyThreeOrFourCharacters(int length) {
        assertEquals(length == 3 || length == 4, new Validator("0", "01/99", "x".repeat(length)).checkCVV());
    }

    @Test
    void expirationAndOptionalFieldsExposeTheirCurrentContract() {
        int y = DateChecker.CURRENT_YEAR;
        int m = DateChecker.CURRENT_MONTH;
        assertFalse(cardWithDate(m, y).checkExpirationDate());
        assertFalse(cardWithDate(m - 1, y).checkExpirationDate());
        assertTrue(cardWithDate(m + 1, y).checkExpirationDate());
        assertFalse(cardWithDate(m, y - 1).checkExpirationDate());
        assertTrue(cardWithDate(m + 1, y - 1).checkExpirationDate(), "characterizes past-year month fallthrough");
        if (y < 99) assertTrue(cardWithDate(0, y + 1).checkExpirationDate());
        Validator unseparated = new Validator("0", String.format(Locale.ROOT, "%02d%02d", m, y), "123");
        unseparated.setExpirationMonth();
        unseparated.setExpirationYear();
        assertFalse(unseparated.checkExpirationDate());
        assertTrue(unseparated.checkCVV());
        Validator numberOnly = new Validator("0");
        assertThrows(NullPointerException.class, numberOnly::checkCVV);
        assertThrows(NullPointerException.class, numberOnly::checkExpirationDate);
        assertThrows(NullPointerException.class, numberOnly::setExpirationMonth);
        assertThrows(NullPointerException.class, numberOnly::setExpirationYear);
        assertThrows(NullPointerException.class, () -> new Validator("0", "01/99", null).checkCVV());
        assertThrows(NullPointerException.class, () -> new Validator("0", null, "123"));
    }

    private static Validator cardWithDate(int month, int year) {
        return new Validator("0", String.format(Locale.ROOT, "%02d/%02d", month, year), "1234");
    }

    @ParameterizedTest
    @ValueSource(strings={"", "1", "12", "12/", "1/2"})
    void shortExpirationStringsFailAtSegmentExtraction(String date) {
        assertThrows(IndexOutOfBoundsException.class, () -> new Validator("0", date, "123"));
    }

    @ParameterizedTest
    @ValueSource(strings={"ab/99", "12/ab", "12//99", "xx/yy"})
    void nondigitDateSegmentsFailWithNumberFormat(String date) {
        assertThrows(NumberFormatException.class, () -> new Validator("0", date, "123"));
    }

    @Test
    void utilityObjectsAndEnumConstantsRemainUsable() {
        assertNotNull(new CreditCardParser());
        assertNotNull(new DateParser());
        assertNotNull(new DateChecker());
        assertNotNull(new DateFormatter());
        assertNotNull(new TypeChecker());
        assertEquals(23, CreditCardType.values().length);
        assertSame(CreditCardType.OTHER, CreditCardType.valueOf("OTHER"));
    }
}
