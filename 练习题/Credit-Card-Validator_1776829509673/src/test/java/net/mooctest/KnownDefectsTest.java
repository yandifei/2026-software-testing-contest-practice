package net.mooctest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Characterization of defects and documentation discrepancies in the supplied code.
 * These assertions describe CURRENT behaviour, not desired business rules.
 * Update the expectations when production defects are authorized to be fixed.
 */
@Tag("known-defect")
class KnownDefectsTest {
    @Test
    void pastYearCanStillPassWhenMonthIsLarger() {
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
    void currentExpirationMonthIsRejectedAndInvalidMonthsAreAccepted() {
        String current = String.format(Locale.ROOT, "%02d/%02d",
            DateChecker.CURRENT_MONTH, DateChecker.CURRENT_YEAR);
        assertFalse(new Validator("4111111111111111", current, "123").checkExpirationDate());
        String future = String.format(Locale.ROOT, "00/%02d", DateChecker.CURRENT_YEAR + 1);
        assertTrue(new Validator("4111111111111111", future, "123").checkExpirationDate());
    }

    @Test
    void verveSecondDocumentedRangeIsNeverAccepted() {
        assertFalse(new VerveValidator("6500020000000000").validate());
        assertFalse(new VerveValidator("6500270000000000").validate());
        assertTrue(new VerveValidator("5060990000000000").validate());
    }

    @Test
    void uatpComparesSixDigitsAgainstOneDigitPrefix() {
        assertFalse(new UATPValidator("100000000000000").validate());
        assertTrue(new UATPValidator("000001000000000").validate());
    }

    @Test
    void visaElectronCannotRecognizeItsSixDigitPrefixAsDecimalInput() {
        assertFalse(new VisaElectronValidator("4175000000000000").validate());
        assertTrue(new VisaElectronValidator("0402600000000000").validate());
        // Character.getNumericValue('H') == 17, so five characters become six digits.
        assertTrue(new VisaElectronValidator("4H50000000000000").validate());
    }

    @Test
    void maestroUkRejectsInteriorOfItsDocumentedRange() {
        assertTrue(new MaestroUKValidator("6767700000000000").validate());
        assertFalse(new MaestroUKValidator("6767710000000000").validate());
        assertFalse(new MaestroUKValidator("6767720000000000").validate());
        assertFalse(new MaestroUKValidator("6767730000000000").validate());
        assertTrue(new MaestroUKValidator("6767740000000000").validate());
    }

    @Test
    void chinaTUnionAndInterPaymentDisagreeWithTheirSourceComments() {
        assertFalse(new ChinaTUnionValidator("3100000000000000").validate());
        assertTrue(new ChinaTUnionValidator("3100000000000000000").validate());
        assertTrue(new InterPaymentValidator("6360000000000000").validate());
        assertFalse(new InterPaymentValidator("6370000000000000").validate());
    }

    @Test
    void checksumDoesNotRequireARealCardOrDecimalOnlyInput() {
        assertTrue(new LuhnValidator().validate(""));
        assertTrue(new Validator("").validate());
        assertTrue(new Validator("0").validate());
        assertTrue(new Validator("0000000000000000").validate());
        assertTrue(new Validator("A").validate());
        assertEquals(List.of(10, 35, -1, 4), CreditCardParser.parseNumber("Az-４"));
    }

    @Test
    void cvvChecksOnlyLengthAndValidateDoesNotCheckOtherFields() {
        assertTrue(new Validator("4111111111111111", "12/99", "abc").checkCVV());
        assertTrue(new Validator("4111111111111111", "12/99", "    ").checkCVV());
        Validator card = new Validator("4111111111111111", "01/00", "");
        assertFalse(card.checkCVV());
        assertFalse(card.checkExpirationDate());
        assertTrue(card.validate());
    }

    @Test
    void dateParserIgnoresTrailingCharactersAndTypeCheckerSupportsOnlyFourBrands() {
        assertTrue(new Validator("4111111111111111", "12/99ignored", "123").checkExpirationDate());
        assertTrue(new JCBValidator("3528000000000000").validate());
        assertEquals(CreditCardType.OTHER, assertTimeoutPreemptively(Duration.ofSeconds(2),
            () -> TypeChecker.checkType("3528000000000000")));
    }
}
