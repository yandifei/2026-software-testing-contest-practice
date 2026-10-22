package net.mooctest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Literal fixtures cover every implemented prefix range, its endpoints and neighbours.
 * Deviations from source documentation are characterized in KnownDefectsTest.
 */
class CardFormatTest {
    @Test
    void visa() {
        assertProfile(VisaValidator::new, 13, 19,
            new String[] {"4"},
            new String[] {"0", "3", "5", "9"});
    }

    @Test
    void americanExpress() {
        assertProfile(AmericanExpressValidator::new, 15, 15,
            new String[] {"34", "35", "36", "37"},
            new String[] {"33", "38", "99"});
    }

    @Test
    void masterCard() {
        assertProfile(MasterCardValidator::new, 16, 16,
            new String[] {"51", "52", "53", "54", "55", "222100",
                "222101", "250000", "272098", "272099"},
            new String[] {"50", "56", "222099", "272100", "99"});
    }

    @Test
    void discover() {
        assertProfile(DiscoverValidator::new, 16, 16,
            new String[] {"6011", "622126", "622127", "622500", "622924", "622925",
                "644", "645", "648", "649", "65"},
            new String[] {"6010", "6012", "622125", "622926", "643", "66",
                "99"});
    }

    @Test
    void visaElectron() {
        assertProfile(VisaElectronValidator::new, 16, 16,
            new String[] {"4026", "4405", "4508", "4844", "4913", "4917",
                "04026", "04405", "04508", "04844", "04913", "04917"},
            new String[] {"4025", "4027", "4404", "4406", "4507", "4509",
                "4843", "4845", "4912", "4914", "4916", "4918",
                "417500", "99"});
    }

    @Test
    void maestro() {
        assertProfile(MaestroValidator::new, 12, 19,
            new String[] {"500000", "500001", "505000", "509998", "509999", "560000",
                "560001", "575000", "589998", "589999", "600000", "600001",
                "650000", "699998", "699999"},
            new String[] {"499999", "510000", "559999", "590000", "599999", "700000",
                "99"});
    }

    @Test
    void maestroUk() {
        assertProfile(MaestroUKValidator::new, 12, 19,
            new String[] {"6759", "676770", "676774"},
            new String[] {"6758", "6760", "676769", "676771", "676772", "676773",
                "676775", "99"});
    }

    @Test
    void jcb() {
        assertProfile(JCBValidator::new, 16, 16,
            new String[] {"3528", "3529", "3550", "3588", "3589"},
            new String[] {"3527", "3590", "99"});
    }

    @Test
    void chinaTUnion() {
        assertProfile(ChinaTUnionValidator::new, 19, 19,
            new String[] {"31"},
            new String[] {"30", "32", "99"});
    }

    @Test
    void chinaUnionPay() {
        assertProfile(ChinaUnionPayValidator::new, 16, 19,
            new String[] {"62"},
            new String[] {"61", "63", "99"});
    }

    @Test
    void dinersClubInternational() {
        assertProfile(DinersClubInternationalValidator::new, 16, 19,
            new String[] {"300", "301", "304", "305", "3095", "38",
                "39"},
            new String[] {"299", "306", "3094", "3096", "37", "40",
                "99"});
    }

    @Test
    void dinersClub() {
        assertProfile(DinersClubValidator::new, 16, 16,
            new String[] {"54", "55"},
            new String[] {"53", "56", "99"});
    }

    @Test
    void ruPay() {
        assertProfile(RuPayValidator::new, 16, 16,
            new String[] {"60", "6521", "6522"},
            new String[] {"59", "61", "6520", "6523", "99"});
    }

    @Test
    void interPayment() {
        assertProfile(InterPaymentValidator::new, 16, 19,
            new String[] {"636"},
            new String[] {"635", "637", "639", "99"});
    }

    @Test
    void instaPayment() {
        assertProfile(InstaPaymenttValidator::new, 16, 16,
            new String[] {"637", "638", "639"},
            new String[] {"636", "640", "99"});
    }

    @Test
    void dankort() {
        assertProfile(DankortValidator::new, 16, 16,
            new String[] {"4571", "4572", "4800", "5018", "5019"},
            new String[] {"4570", "5020", "99"});
    }

    @Test
    void mir() {
        assertProfile(MIRValidator::new, 16, 16,
            new String[] {"2200", "2201", "2202", "2203", "2204"},
            new String[] {"2199", "2205", "99"});
    }

    @Test
    void npsPridnestrovie() {
        assertProfile(NPS_PridnestrovieValidator::new, 16, 16,
            new String[] {"6054740", "6054741", "6054742", "6054743", "6054744"},
            new String[] {"6054739", "6054745", "99"});
    }

    @Test
    void troy() {
        assertProfile(TroyValidator::new, 16, 16,
            new String[] {"979200", "979201", "979250", "979288", "979289"},
            new String[] {"979199", "979290", "99"});
    }

    @Test
    void uatp() {
        assertProfile(UATPValidator::new, 15, 15,
            new String[] {"000001"},
            new String[] {"000000", "000002", "1", "99"});
    }

    @Test
    void verve() {
        assertProfile(VerveValidator::new, 16, 19,
            new String[] {"506099", "506100", "506150", "506197", "506198"},
            new String[] {"506098", "506199", "650001", "650002", "650015", "650027",
                "650028", "99"});
    }

    @Test
    void lankaPay() {
        assertProfile(LankaPayValidator::new, 16, 16,
            new String[] {"357111"},
            new String[] {"357110", "357112", "99"});
    }

    private static void assertProfile(Function<String, CreditCardValidator> factory,
            int minimumLength, int maximumLength, String[] acceptedPrefixes, String[] rejectedPrefixes) {
        for (int length = 0; length <= 21; length++) {
            boolean expected = length >= minimumLength && length <= maximumLength;
            assertEquals(expected, factory.apply("0".repeat(length)).checkLength(), "length=" + length);
            if (length >= 7) {
                CreditCardValidator validator = factory.apply(pad(acceptedPrefixes[0], length));
                assertEquals(expected, validator.validate(), "combined validation, length=" + length);
            }
        }
        for (String prefix : acceptedPrefixes) {
            CreditCardValidator validator = factory.apply(pad(prefix, minimumLength));
            assertTrue(validator.checkIINRanges(), "accepted prefix=" + prefix);
            assertTrue(validator.validate(), "accepted card prefix=" + prefix);
        }
        for (String prefix : rejectedPrefixes) {
            CreditCardValidator validator = factory.apply(pad(prefix, minimumLength));
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

    private static String pad(String prefix, int length) {
        return prefix + "0".repeat(length - prefix.length());
    }
}
