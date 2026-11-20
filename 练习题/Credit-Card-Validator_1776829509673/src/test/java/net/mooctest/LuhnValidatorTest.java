package net.mooctest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LuhnValidatorTest {
    // Independent lookup oracle; no production helpers compute expected results.
    private static final int[] DOUBLED_DIGIT_SUM = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
    private final LuhnValidator validator = new LuhnValidator();

    @ParameterizedTest
    @CsvSource({"0,0", "1,1", "9,9", "10,1", "11,2", "18,9", "99,18",
        "100,1", "109,10", "12345,15", "1000000000,1", "2147483647,46"})
    void sumsDecimalDigits(int input, int expected) {
        assertEquals(expected, validator.sumOfDigits(input));
    }

    @ParameterizedTest
    @CsvSource({"0,true", "1,false", "9,false", "18,true", "59,true", "91,true",
        "10,false", "19,false", "1230,true", "1231,false", "79927398713,true",
        "79927398714,false", "4111111111111111,true", "4012888888881881,true",
        "5555555555554444,true", "378282246310005,true", "6011111111111117,true"})
    void validatesHandCheckedExamples(String number, boolean expected) {
        assertEquals(expected, validator.algorithmCheck(number));
        assertEquals(expected, validator.validate(number));
    }

    @Test
    void exhaustivelyChecksTwoAndThreeDigitParity() {
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
    void generatedChecksumsRejectEverySingleDigitSubstitution() {
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
    void rejectsNullAndNegativeDigitSumInputs() {
        assertThrows(NullPointerException.class, () -> validator.validate(null));
        assertThrows(NumberFormatException.class, () -> validator.sumOfDigits(-1));
        assertThrows(NumberFormatException.class, () -> validator.sumOfDigits(Integer.MIN_VALUE));
    }

    @ParameterizedTest
    @CsvSource({"-0,false", "-2,true", "0-,false", "2-,false", "' 0',false",
        "' 2',true", "'0 ',false", "?0,false", "?2,true", "½0,false", "½4,true"})
    void preservesCurrentBehaviourForNonDigitValuesOnBothLuhnParities(String number, boolean expected) {
        // Character.getNumericValue returns -1 for punctuation and -2 for a fraction.
        // These are characterization cases, not a claim that such inputs are valid cards.
        // A negative doubled value is handled directly; routing it through sumOfDigits
        // instead would throw and exposes a previously surviving conditional mutation.
        assertEquals(expected, validator.algorithmCheck(number));
        assertEquals(expected, validator.validate(number));
    }
}
