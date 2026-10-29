package net.mooctest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Single-file contest regression suite. Tests use real production components.
 * Format fixtures cover endpoints, neighbours and valid interior prefixes.
 * KnownDefects scenarios characterize the supplied implementation, not desired banking rules.
 * Luhn expectations use literal examples and an independent digit lookup table.
 */
public class CreditCardValidatorTest {
    // CardFormatTest
    @Test
    void cardFormat_visa() {
        cardFormat_assertProfile(VisaValidator::new, 13, 19,
            new String[] {"4"},
            new String[] {"0", "3", "5", "9"});
    }

    @Test
    void cardFormat_americanExpress() {
        cardFormat_assertProfile(AmericanExpressValidator::new, 15, 15,
            new String[] {"34", "35", "36", "37"},
            new String[] {"33", "38", "99"});
    }

    @Test
    void cardFormat_masterCard() {
        cardFormat_assertProfile(MasterCardValidator::new, 16, 16,
            new String[] {"51", "52", "53", "54", "55", "222100",
                "222101", "250000", "272098", "272099"},
            new String[] {"50", "56", "222099", "272100", "99"});
    }

    @Test
    void cardFormat_discover() {
        cardFormat_assertProfile(DiscoverValidator::new, 16, 16,
            new String[] {"6011", "622126", "622127", "622500", "622924", "622925",
                "644", "645", "648", "649", "65"},
            new String[] {"6010", "6012", "622125", "622926", "643", "66",
                "99"});
    }

    @Test
    void cardFormat_visaElectron() {
        cardFormat_assertProfile(VisaElectronValidator::new, 16, 16,
            new String[] {"4026", "4405", "4508", "4844", "4913", "4917",
                "04026", "04405", "04508", "04844", "04913", "04917"},
            new String[] {"4025", "4027", "4404", "4406", "4507", "4509",
                "4843", "4845", "4912", "4914", "4916", "4918",
                "417500", "99"});
    }

    @Test
    void cardFormat_maestro() {
        cardFormat_assertProfile(MaestroValidator::new, 12, 19,
            new String[] {"500000", "500001", "505000", "509998", "509999", "560000",
                "560001", "575000", "589998", "589999", "600000", "600001",
                "650000", "699998", "699999"},
            new String[] {"499999", "510000", "559999", "590000", "599999", "700000",
                "99"});
    }

    @Test
    void cardFormat_maestroUk() {
        cardFormat_assertProfile(MaestroUKValidator::new, 12, 19,
            new String[] {"6759", "676770", "676774"},
            new String[] {"6758", "6760", "676769", "676771", "676772", "676773",
                "676775", "99"});
    }

    @Test
    void cardFormat_jcb() {
        cardFormat_assertProfile(JCBValidator::new, 16, 16,
            new String[] {"3528", "3529", "3550", "3588", "3589"},
            new String[] {"3527", "3590", "99"});
    }

    @Test
    void cardFormat_chinaTUnion() {
        cardFormat_assertProfile(ChinaTUnionValidator::new, 19, 19,
            new String[] {"31"},
            new String[] {"30", "32", "99"});
    }

    @Test
    void cardFormat_chinaUnionPay() {
        cardFormat_assertProfile(ChinaUnionPayValidator::new, 16, 19,
            new String[] {"62"},
            new String[] {"61", "63", "99"});
    }

    @Test
    void cardFormat_dinersClubInternational() {
        cardFormat_assertProfile(DinersClubInternationalValidator::new, 16, 19,
            new String[] {"300", "301", "304", "305", "3095", "38",
                "39"},
            new String[] {"299", "306", "3094", "3096", "37", "40",
                "99"});
    }

    @Test
    void cardFormat_dinersClub() {
        cardFormat_assertProfile(DinersClubValidator::new, 16, 16,
            new String[] {"54", "55"},
            new String[] {"53", "56", "99"});
    }

    @Test
    void cardFormat_ruPay() {
        cardFormat_assertProfile(RuPayValidator::new, 16, 16,
            new String[] {"60", "6521", "6522"},
            new String[] {"59", "61", "6520", "6523", "99"});
    }

    @Test
    void cardFormat_interPayment() {
        cardFormat_assertProfile(InterPaymentValidator::new, 16, 19,
            new String[] {"636"},
            new String[] {"635", "637", "639", "99"});
    }

    @Test
    void cardFormat_instaPayment() {
        cardFormat_assertProfile(InstaPaymenttValidator::new, 16, 16,
            new String[] {"637", "638", "639"},
            new String[] {"636", "640", "99"});
    }

    @Test
    void cardFormat_dankort() {
        cardFormat_assertProfile(DankortValidator::new, 16, 16,
            new String[] {"4571", "4572", "4800", "5018", "5019"},
            new String[] {"4570", "5020", "99"});
    }

    @Test
    void cardFormat_mir() {
        cardFormat_assertProfile(MIRValidator::new, 16, 16,
            new String[] {"2200", "2201", "2202", "2203", "2204"},
            new String[] {"2199", "2205", "99"});
    }

    @Test
    void cardFormat_npsPridnestrovie() {
        cardFormat_assertProfile(NPS_PridnestrovieValidator::new, 16, 16,
            new String[] {"6054740", "6054741", "6054742", "6054743", "6054744"},
            new String[] {"6054739", "6054745", "99"});
    }

    @Test
    void cardFormat_troy() {
        cardFormat_assertProfile(TroyValidator::new, 16, 16,
            new String[] {"979200", "979201", "979250", "979288", "979289"},
            new String[] {"979199", "979290", "99"});
    }

    @Test
    void cardFormat_uatp() {
        cardFormat_assertProfile(UATPValidator::new, 15, 15,
            new String[] {"000001"},
            new String[] {"000000", "000002", "1", "99"});
    }

    @Test
    void cardFormat_verve() {
        cardFormat_assertProfile(VerveValidator::new, 16, 19,
            new String[] {"506099", "506100", "506150", "506197", "506198"},
            new String[] {"506098", "506199", "650001", "650002", "650015", "650027",
                "650028", "99"});
    }

    @Test
    void cardFormat_lankaPay() {
        cardFormat_assertProfile(LankaPayValidator::new, 16, 16,
            new String[] {"357111"},
            new String[] {"357110", "357112", "99"});
    }

    private static void cardFormat_assertProfile(Function<String, CreditCardValidator> factory,
            int minimumLength, int maximumLength, String[] acceptedPrefixes, String[] rejectedPrefixes) {
        for (int length = 0; length <= 21; length++) {
            boolean expected = length >= minimumLength && length <= maximumLength;
            assertEquals(expected, factory.apply("0".repeat(length)).checkLength(), "length=" + length);
            if (length >= 7) {
                CreditCardValidator validator = factory.apply(cardFormat_pad(acceptedPrefixes[0], length));
                assertEquals(expected, validator.validate(), "combined validation, length=" + length);
            }
        }
        for (String prefix : acceptedPrefixes) {
            CreditCardValidator validator = factory.apply(cardFormat_pad(prefix, minimumLength));
            assertTrue(validator.checkIINRanges(), "accepted prefix=" + prefix);
            assertTrue(validator.validate(), "accepted card prefix=" + prefix);
        }
        for (String prefix : rejectedPrefixes) {
            CreditCardValidator validator = factory.apply(cardFormat_pad(prefix, minimumLength));
            assertFalse(validator.checkIINRanges(), "rejected prefix=" + prefix);
            assertFalse(validator.validate(), "rejected card prefix=" + prefix);
        }
        assertThrows(NullPointerException.class, () -> factory.apply(null));
        CreditCardValidator empty = factory.apply("");
        assertFalse(empty.checkLength());
        assertThrows(IndexOutOfBoundsException.class, empty::checkIINRanges);
        // Current validate() evaluates the prefix even when length is invalid.
        assertThrows(IndexOutOfBoundsException.class, empty::validate);
    }

    private static String cardFormat_pad(String prefix, int length) {
        return prefix + "0".repeat(length - prefix.length());
    }

    // CreditCardValidatorTest
    private static final String VALID_VISA = "4111111111111111";

    @ParameterizedTest
    @ValueSource(strings = {"4111111111111111", "5555555555554444",
        "378282246310005", "6011111111111117", "79927398713"})
    void creditCardValidator_acceptsValidChecksums(String number) {
        assertTrue(new Validator(number).validate());
        assertTrue(new Validator(number, "12/99", "123").validate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"4111111111111112", "5555555555554445",
        "378282246310006", "6011111111111118", "1"})
    void creditCardValidator_rejectsInvalidChecksumsWithDomainException(String number) {
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
    void creditCardValidator_classifiesByPrefixAndLength(String number, CreditCardType expected) {
        // The fallback must terminate too, rather than repeatedly scanning unknown types.
        assertEquals(expected, assertTimeoutPreemptively(Duration.ofSeconds(2),
            () -> TypeChecker.checkType(number)));
    }

    @ParameterizedTest
    @CsvSource({"0,false", "1,false", "2,false", "3,true", "4,true",
        "5,false", "6,false", "20,false"})
    void creditCardValidator_checksBothCvvLengthsAndAdjacentBoundaries(int length, boolean expected) {
        assertEquals(expected, new Validator(VALID_VISA, "12/99", "7".repeat(length)).checkCVV());
    }

    @Test
    void creditCardValidator_evaluatesExpirationRelativeToTheLoadedClock() {
        int year = DateChecker.CURRENT_YEAR;
        int month = DateChecker.CURRENT_MONTH;
        assertTrue(creditCardValidator_withDate(1, year + 1).checkExpirationDate());
        assertFalse(creditCardValidator_withDate(month, year).checkExpirationDate());
        assertFalse(creditCardValidator_withDate(month - 1, year).checkExpirationDate());
        assertFalse(creditCardValidator_withDate(month, year - 1).checkExpirationDate());
        if (month < 12) {
            assertTrue(creditCardValidator_withDate(month + 1, year).checkExpirationDate());
        }
        Validator unseparated = new Validator(VALID_VISA,
            String.format(Locale.ROOT, "01%02d", year + 1), "1234");
        unseparated.setExpirationMonth();
        unseparated.setExpirationYear();
        assertTrue(unseparated.checkExpirationDate());
        assertTrue(unseparated.checkCVV());
    }

    @Test
    void creditCardValidator_optionalFieldsAreNotInitializedByNumberOnlyConstructor() {
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
    void creditCardValidator_rejectsDatesTooShortToContainMonthAndYear(String date) {
        assertThrows(IndexOutOfBoundsException.class, () -> new Validator(VALID_VISA, date, "123"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab/99", "12/ab", "12//99", "xx/yy"})
    void creditCardValidator_rejectsNonNumericDateSegments(String date) {
        assertThrows(NumberFormatException.class, () -> new Validator(VALID_VISA, date, "123"));
    }

    @Test
    void creditCardValidator_rejectsNullDatesAndUnparseableTypeInputs() {
        assertThrows(NullPointerException.class, () -> new Validator(VALID_VISA, null, "123"));
        assertThrows(NullPointerException.class, () -> TypeChecker.checkType(null));
        assertThrows(IndexOutOfBoundsException.class, () -> TypeChecker.checkType(""));
        assertThrows(IndexOutOfBoundsException.class, () -> TypeChecker.checkType("12345"));
    }

    private static Validator creditCardValidator_withDate(int month, int year) {
        return new Validator(VALID_VISA, String.format(Locale.ROOT, "%02d/%02d", month, year), "123");
    }

    // KnownDefectsTest
    @Test
    void knownDefects_pastYearCanStillPassWhenMonthIsLarger() {
        // Month 13 keeps the reproducer deterministic even in December; also exposes missing bounds.
        String date = String.format(Locale.ROOT, "13/%02d", DateChecker.CURRENT_YEAR - 1);
        assertTrue(new Validator("4111111111111111", date, "123").checkExpirationDate());
        if (DateChecker.CURRENT_MONTH < 12) {
            String validMonth = String.format(Locale.ROOT, "%02d/%02d",
                DateChecker.CURRENT_MONTH + 1, DateChecker.CURRENT_YEAR - 1);
            assertTrue(new Validator("4111111111111111", validMonth, "123").checkExpirationDate());
        }
    }

    @Test
    void knownDefects_currentExpirationMonthIsRejectedAndInvalidMonthsAreAccepted() {
        String current = String.format(Locale.ROOT, "%02d/%02d",
            DateChecker.CURRENT_MONTH, DateChecker.CURRENT_YEAR);
        assertFalse(new Validator("4111111111111111", current, "123").checkExpirationDate());
        String future = String.format(Locale.ROOT, "00/%02d", DateChecker.CURRENT_YEAR + 1);
        assertTrue(new Validator("4111111111111111", future, "123").checkExpirationDate());
    }

    @Test
    void knownDefects_verveSecondDocumentedRangeIsNeverAccepted() {
        assertFalse(new VerveValidator("6500020000000000").validate());
        assertFalse(new VerveValidator("6500270000000000").validate());
        assertTrue(new VerveValidator("5060990000000000").validate());
    }

    @Test
    void knownDefects_uatpComparesSixDigitsAgainstOneDigitPrefix() {
        assertFalse(new UATPValidator("100000000000000").validate());
        assertTrue(new UATPValidator("000001000000000").validate());
    }

    @Test
    void knownDefects_visaElectronCannotRecognizeItsSixDigitPrefixAsDecimalInput() {
        assertFalse(new VisaElectronValidator("4175000000000000").validate());
        assertTrue(new VisaElectronValidator("0402600000000000").validate());
        // Character.getNumericValue('H') == 17, so five characters become six digits.
        assertTrue(new VisaElectronValidator("4H50000000000000").validate());
    }

    @Test
    void knownDefects_maestroUkRejectsInteriorOfItsDocumentedRange() {
        assertTrue(new MaestroUKValidator("6767700000000000").validate());
        assertFalse(new MaestroUKValidator("6767710000000000").validate());
        assertFalse(new MaestroUKValidator("6767720000000000").validate());
        assertFalse(new MaestroUKValidator("6767730000000000").validate());
        assertTrue(new MaestroUKValidator("6767740000000000").validate());
    }

    @Test
    void knownDefects_chinaTUnionAndInterPaymentDisagreeWithTheirSourceComments() {
        assertFalse(new ChinaTUnionValidator("3100000000000000").validate());
        assertTrue(new ChinaTUnionValidator("3100000000000000000").validate());
        assertTrue(new InterPaymentValidator("6360000000000000").validate());
        assertFalse(new InterPaymentValidator("6370000000000000").validate());
    }

    @Test
    void knownDefects_checksumDoesNotRequireARealCardOrDecimalOnlyInput() {
        assertTrue(new LuhnValidator().validate(""));
        assertTrue(new Validator("").validate());
        assertTrue(new Validator("0").validate());
        assertTrue(new Validator("0000000000000000").validate());
        assertTrue(new Validator("A").validate());
        assertEquals(List.of(10, 35, -1, 4), CreditCardParser.parseNumber("Az-４"));
    }

    @Test
    void knownDefects_cvvChecksOnlyLengthAndValidateDoesNotCheckOtherFields() {
        assertTrue(new Validator("4111111111111111", "12/99", "abc").checkCVV());
        assertTrue(new Validator("4111111111111111", "12/99", "    ").checkCVV());
        Validator card = new Validator("4111111111111111", "01/00", "");
        assertFalse(card.checkCVV());
        assertFalse(card.checkExpirationDate());
        assertTrue(card.validate());
    }

    @Test
    void knownDefects_dateParserIgnoresTrailingCharactersAndTypeCheckerSupportsOnlyFourBrands() {
        assertTrue(new Validator("4111111111111111", "12/99ignored", "123").checkExpirationDate());
        assertTrue(new JCBValidator("3528000000000000").validate());
        assertEquals(CreditCardType.OTHER, assertTimeoutPreemptively(Duration.ofSeconds(2),
            () -> TypeChecker.checkType("3528000000000000")));
    }

    // LuhnValidatorTest
    // Independent lookup oracle; no production helpers compute expected results.
    private static final int[] DOUBLED_DIGIT_SUM = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
    private final LuhnValidator validator = new LuhnValidator();

    @ParameterizedTest
    @CsvSource({"0,0", "1,1", "9,9", "10,1", "11,2", "18,9", "99,18",
        "100,1", "109,10", "12345,15", "1000000000,1", "2147483647,46"})
    void luhnValidator_sumsDecimalDigits(int input, int expected) {
        assertEquals(expected, validator.sumOfDigits(input));
    }

    @ParameterizedTest
    @CsvSource({"0,true", "1,false", "9,false", "18,true", "59,true", "91,true",
        "10,false", "19,false", "1230,true", "1231,false", "79927398713,true",
        "79927398714,false", "4111111111111111,true", "4012888888881881,true",
        "5555555555554444,true", "378282246310005,true", "6011111111111117,true"})
    void luhnValidator_validatesHandCheckedExamples(String number, boolean expected) {
        assertEquals(expected, validator.algorithmCheck(number));
        assertEquals(expected, validator.validate(number));
    }

    @Test
    void luhnValidator_exhaustivelyChecksTwoAndThreeDigitParity() {
        for (int first = 0; first < 10; first++) {
            for (int second = 0; second < 10; second++) {
                String pair = "" + first + second;
                assertEquals((DOUBLED_DIGIT_SUM[first] + second) % 10 == 0,
                    validator.algorithmCheck(pair), pair);
                for (int third = 0; third < 10; third++) {
                    String triple = pair + third;
                    assertEquals((first + DOUBLED_DIGIT_SUM[second] + third) % 10 == 0,
                        validator.algorithmCheck(triple), triple);
                }
            }
        }
    }

    @Test
    void luhnValidator_generatedChecksumsRejectEverySingleDigitSubstitution() {
        Random random = new Random(0x5EEDL);
        for (int length = 2; length <= 25; length++) {
            StringBuilder payload = new StringBuilder();
            int checksum = 0;
            for (int position = 0; position < length - 1; position++) {
                int digit = random.nextInt(10);
                payload.append(digit);
                checksum += (length - 1 - position) % 2 == 1 ? DOUBLED_DIGIT_SUM[digit] : digit;
            }
            String valid = payload.append((10 - checksum % 10) % 10).toString();
            assertTrue(validator.validate(valid), valid);
            for (int position = 0; position < length; position++) {
                for (int delta = 1; delta <= 9; delta++) {
                    char[] changed = valid.toCharArray();
                    changed[position] = (char) ('0' + (changed[position] - '0' + delta) % 10);
                    assertFalse(validator.validate(new String(changed)), new String(changed));
                }
            }
        }
    }

    @Test
    void luhnValidator_rejectsNullAndNegativeDigitSumInputs() {
        assertThrows(NullPointerException.class, () -> validator.validate(null));
        assertThrows(NumberFormatException.class, () -> validator.sumOfDigits(-1));
        assertThrows(NumberFormatException.class, () -> validator.sumOfDigits(Integer.MIN_VALUE));
    }

    @ParameterizedTest
    @CsvSource({"-0,false", "-2,true", "0-,false", "2-,false", "' 0',false",
        "' 2',true", "'0 ',false", "?0,false", "?2,true", "½0,false", "½4,true"})
    void luhnValidator_preservesCurrentBehaviourForNonDigitValuesOnBothLuhnParities(String number, boolean expected) {
        // Character.getNumericValue returns -1 for punctuation and -2 for a fraction.
        // These are characterization cases, not a claim that such inputs are valid cards.
        // A negative doubled value is handled directly; routing it through sumOfDigits
        // instead would throw and exposes a previously surviving conditional mutation.
        assertEquals(expected, validator.algorithmCheck(number));
        assertEquals(expected, validator.validate(number));
    }

    // ParserAndDateTest
    @Test
    void parserAndDate_parsesDigitsInOrderWithoutDroppingLeadingZeroes() {
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
    void parserAndDate_extractsRequestedPrefixWithoutChangingTheInput() {
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
    void parserAndDate_rejectsInvalidPrefixLengthsAndOverflow() {
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
    void parserAndDate_removesOnlyTheFirstSlashInPlace(String input, String expected) {
        StringBuilder date = new StringBuilder(input);
        assertSame(date, DateParser.parseDate(date));
        assertEquals(expected, date.toString());
    }

    @Test
    void parserAndDate_convertsDateToIndependentMutableBuffer() {
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
    void parserAndDate_parsesExactHalfOpenDateSegments() {
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
    void parserAndDate_comparesDatesStrictlyWithoutOverflow(int date, int current, boolean expected) {
        assertEquals(expected, DateChecker.compareDates(date, current));
    }

    @Test
    void parserAndDate_formatsTwoDigitYearAndMonthWithFreshFormatters() {
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
    void parserAndDate_readsTheCurrentCalendarYearAndMonth() {
        Calendar before = Calendar.getInstance();
        int year = DateChecker.getCurrentYear();
        int month = DateChecker.getCurrentMonth();
        Calendar after = Calendar.getInstance();
        assertTrue(year == before.get(Calendar.YEAR) % 100 || year == after.get(Calendar.YEAR) % 100);
        assertTrue(month == before.get(Calendar.MONTH) + 1 || month == after.get(Calendar.MONTH) + 1);
        assertFalse(month < 1 || month > 12);
    }

    @Test
    void parserAndDate_utilityConstructorsRemainAccessibleInTheirPackage() {
        assertNotNull(new CreditCardParser());
        assertNotNull(new DateChecker());
        assertNotNull(new DateFormatter());
        assertNotNull(new DateParser());
        assertNotNull(new TypeChecker());
    }
}
