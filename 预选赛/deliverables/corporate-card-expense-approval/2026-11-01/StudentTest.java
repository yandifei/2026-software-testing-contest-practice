/*
 * 2026 软件测试大赛-开发者测试-预选赛
 * 题目：corporate-card-expense-approval
 * 单一测试文件 StudentTest.java：覆盖 card 校验、报销策略、权限策略与审批工作流。
 */
package net.mooctest;

import net.mooctest.card.AmericanExpressValidator;
import net.mooctest.card.CardEvidence;
import net.mooctest.card.CardEvidenceCheck;
import net.mooctest.card.CardEvidenceValidator;
import net.mooctest.card.ChinaTUnionValidator;
import net.mooctest.card.ChinaUnionPayValidator;
import net.mooctest.card.CreditCardParser;
import net.mooctest.card.CreditCardType;
import net.mooctest.card.CreditCardValidator;
import net.mooctest.card.DankortValidator;
import net.mooctest.card.DinersClubInternationalValidator;
import net.mooctest.card.DinersClubValidator;
import net.mooctest.card.DiscoverValidator;
import net.mooctest.card.InstaPaymenttValidator;
import net.mooctest.card.InterPaymentValidator;
import net.mooctest.card.JCBValidator;
import net.mooctest.card.LankaPayValidator;
import net.mooctest.card.MaestroUKValidator;
import net.mooctest.card.MaestroValidator;
import net.mooctest.card.MasterCardValidator;
import net.mooctest.card.MIRValidator;
import net.mooctest.card.NPS_PridnestrovieValidator;
import net.mooctest.card.RuPayValidator;
import net.mooctest.card.TroyValidator;
import net.mooctest.card.TypeChecker;
import net.mooctest.card.UATPValidator;
import net.mooctest.card.VerveValidator;
import net.mooctest.card.VisaElectronValidator;
import net.mooctest.card.VisaValidator;
import net.mooctest.expense.CorporateCardExpenseService;
import net.mooctest.expense.EmployeeGrade;
import net.mooctest.expense.ExchangeRateProvider;
import net.mooctest.expense.ExpenseApprovalTier;
import net.mooctest.expense.ExpenseAssessment;
import net.mooctest.expense.ExpenseCardReview;
import net.mooctest.expense.ExpenseCategory;
import net.mooctest.expense.ExpenseClaim;
import net.mooctest.expense.ExpenseItem;
import net.mooctest.expense.ExpensePolicy;
import net.mooctest.expense.PaymentMethod;
import net.mooctest.expense.ReceiptType;
import net.mooctest.permission.AccessContext;
import net.mooctest.permission.AccessDecision;
import net.mooctest.permission.AccessPolicy;
import net.mooctest.permission.AccessScope;
import net.mooctest.permission.ResourceAction;
import net.mooctest.permission.ResourceType;
import net.mooctest.permission.Sensitivity;
import net.mooctest.permission.UserRole;
import net.mooctest.workflow.ApprovalDecision;
import net.mooctest.workflow.ApprovalStep;
import net.mooctest.workflow.ApprovalWorkflowEngine;
import net.mooctest.workflow.StepMode;
import net.mooctest.workflow.StepProgress;
import net.mooctest.workflow.WorkflowAction;
import net.mooctest.workflow.WorkflowSnapshot;
import net.mooctest.workflow.WorkflowState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentTest {

    // ==================== CreditCardParser ====================

    @Test
    void parser_parsesDigitsInOrderPreservingLeadingZeroes() {
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), CreditCardParser.parseNumber("0123456789"));
        assertEquals(List.of(), CreditCardParser.parseNumber(""));
        List<Integer> first = CreditCardParser.parseNumber("123");
        List<Integer> second = CreditCardParser.parseNumber("123");
        first.set(0, 9);
        assertEquals(List.of(1, 2, 3), second);
        assertThrows(NullPointerException.class, () -> CreditCardParser.parseNumber(null));
    }

    @Test
    void parser_usesCharacterNumericValueForNonDigitInputs() {
        // Character.getNumericValue('A')=10, 'z'=35, '-'=-1, full-width '4'=4.
        assertEquals(List.of(10, 35, -1, 4), CreditCardParser.parseNumber("Az-\uFF14"));
    }

    @Test
    void parser_extractsRequestedPrefixWithoutChangingTheInput() {
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
        assertEquals(Integer.MAX_VALUE, CreditCardParser.parseIIN(List.of(2, 1, 4, 7, 4, 8, 3, 6, 4, 7), 10));
    }

    @Test
    void parser_rejectsInvalidPrefixLengthsAndOverflow() {
        assertThrows(NumberFormatException.class, () -> CreditCardParser.parseIIN(List.of(1), 0));
        assertThrows(NegativeArraySizeException.class, () -> CreditCardParser.parseIIN(List.of(1), -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CreditCardParser.parseIIN(List.of(1), 2));
        assertThrows(NullPointerException.class, () -> CreditCardParser.parseIIN(null, 1));
        assertThrows(NumberFormatException.class,
                () -> CreditCardParser.parseIIN(List.of(2, 1, 4, 7, 4, 8, 3, 6, 4, 8), 10));
    }

    // ==================== Card format validators ====================

    @Test
    void cardFormat_visa() {
        assertCardFormat(VisaValidator::new, 13, 19, 1,
                new String[]{"4"}, new String[]{"0", "3", "5", "9"});
    }

    @Test
    void cardFormat_americanExpress() {
        assertCardFormat(AmericanExpressValidator::new, 15, 15, 2,
                new String[]{"34", "35", "36", "37"}, new String[]{"33", "38", "99"});
    }

    @Test
    void cardFormat_masterCard() {
        assertCardFormat(MasterCardValidator::new, 16, 16, 6,
                new String[]{"51", "52", "53", "54", "55", "222100", "222101", "250000", "272098", "272099"},
                new String[]{"50", "56", "222099", "272100", "99"});
    }

    @Test
    void cardFormat_discover() {
        assertCardFormat(DiscoverValidator::new, 16, 16, 6,
                new String[]{"6011", "622126", "622127", "622500", "622924", "622925",
                        "644", "645", "648", "649", "65"},
                new String[]{"6010", "6012", "622125", "622926", "643", "66", "99"});
    }

    @Test
    void cardFormat_visaElectron() {
        assertCardFormat(VisaElectronValidator::new, 16, 16, 5,
                new String[]{"4026", "4405", "4508", "4844", "4913", "4917",
                        "04026", "04405", "04508", "04844", "04913", "04917"},
                new String[]{"4025", "4027", "4404", "4406", "4507", "4509",
                        "4843", "4845", "4912", "4914", "4916", "4918", "417500", "99"});
    }

    @Test
    void cardFormat_maestro() {
        assertCardFormat(MaestroValidator::new, 12, 19, 6,
                new String[]{"500000", "500001", "505000", "509998", "509999", "560000",
                        "560001", "575000", "589998", "589999", "600000", "600001",
                        "650000", "699998", "699999"},
                new String[]{"499999", "510000", "559999", "590000", "599999", "700000", "99"});
    }

    @Test
    void cardFormat_maestroUk() {
        assertCardFormat(MaestroUKValidator::new, 12, 19, 6,
                new String[]{"6759", "676770", "676774"},
                new String[]{"6758", "6760", "676769", "676771", "676772", "676773", "676775", "99"});
    }

    @Test
    void cardFormat_jcb() {
        assertCardFormat(JCBValidator::new, 16, 16, 4,
                new String[]{"3528", "3529", "3550", "3588", "3589"},
                new String[]{"3527", "3590", "99"});
    }

    @Test
    void cardFormat_chinaTUnion() {
        assertCardFormat(ChinaTUnionValidator::new, 19, 19, 2,
                new String[]{"31"}, new String[]{"30", "32", "99"});
    }

    @Test
    void cardFormat_chinaUnionPay() {
        assertCardFormat(ChinaUnionPayValidator::new, 16, 19, 2,
                new String[]{"62"}, new String[]{"61", "63", "99"});
    }

    @Test
    void cardFormat_dinersClubInternational() {
        assertCardFormat(DinersClubInternationalValidator::new, 16, 19, 4,
                new String[]{"300", "301", "304", "305", "3095", "38", "39"},
                new String[]{"299", "306", "3094", "3096", "37", "40", "99"});
    }

    @Test
    void cardFormat_dinersClub() {
        assertCardFormat(DinersClubValidator::new, 16, 16, 2,
                new String[]{"54", "55"}, new String[]{"53", "56", "99"});
    }

    @Test
    void cardFormat_ruPay() {
        assertCardFormat(RuPayValidator::new, 16, 16, 4,
                new String[]{"60", "6521", "6522"}, new String[]{"59", "61", "6520", "6523", "99"});
    }

    @Test
    void cardFormat_interPayment() {
        assertCardFormat(InterPaymentValidator::new, 16, 19, 3,
                new String[]{"636"}, new String[]{"635", "637", "639", "99"});
    }

    @Test
    void cardFormat_instaPayment() {
        assertCardFormat(InstaPaymenttValidator::new, 16, 16, 3,
                new String[]{"637", "638", "639"}, new String[]{"636", "640", "99"});
    }

    @Test
    void cardFormat_dankort() {
        assertCardFormat(DankortValidator::new, 16, 16, 4,
                new String[]{"4571", "4572", "4800", "5018", "5019"},
                new String[]{"4570", "5020", "99"});
    }

    @Test
    void cardFormat_mir() {
        assertCardFormat(MIRValidator::new, 16, 16, 4,
                new String[]{"2200", "2201", "2202", "2203", "2204"},
                new String[]{"2199", "2205", "99"});
    }

    @Test
    void cardFormat_npsPridnestrovie() {
        assertCardFormat(NPS_PridnestrovieValidator::new, 16, 16, 7,
                new String[]{"6054740", "6054741", "6054742", "6054743", "6054744"},
                new String[]{"6054739", "6054745", "99"});
    }

    @Test
    void cardFormat_troy() {
        assertCardFormat(TroyValidator::new, 16, 16, 6,
                new String[]{"979200", "979201", "979250", "979288", "979289"},
                new String[]{"979199", "979290", "99"});
    }

    @Test
    void cardFormat_uatp() {
        assertCardFormat(UATPValidator::new, 15, 15, 6,
                new String[]{"000001"}, new String[]{"000000", "000002", "1", "99"});
    }

    @Test
    void cardFormat_verve() {
        assertCardFormat(VerveValidator::new, 16, 19, 6,
                new String[]{"506099", "506100", "506150", "506197", "506198"},
                new String[]{"506098", "506199", "650001", "650002", "650015", "650027", "650028", "99"});
    }

    @Test
    void cardFormat_lankaPay() {
        assertCardFormat(LankaPayValidator::new, 16, 16, 6,
                new String[]{"357111"}, new String[]{"357110", "357112", "99"});
    }

    @Test
    void cardFormat_knownDefectsAreCharacterized() {
        // Verve's second documented range is never accepted (loop reuses the first range).
        assertFalse(new VerveValidator("6500020000000000").validate());
        assertFalse(new VerveValidator("6500270000000000").validate());
        assertTrue(new VerveValidator("5060990000000000").validate());
        // UATP compares a six-digit parse against the one-digit prefix.
        assertFalse(new UATPValidator("100000000000000").validate());
        assertTrue(new UATPValidator("000001000000000").validate());
        // Visa Electron cannot recognize its documented six-digit prefix as decimal input.
        assertFalse(new VisaElectronValidator("4175000000000000").validate());
        assertTrue(new VisaElectronValidator("0402600000000000").validate());
        assertTrue(new VisaElectronValidator("4H50000000000000").validate());
        // Maestro UK accepts only the endpoints of its documented second range.
        assertTrue(new MaestroUKValidator("6767700000000000").validate());
        assertFalse(new MaestroUKValidator("6767710000000000").validate());
        assertFalse(new MaestroUKValidator("6767720000000000").validate());
        assertFalse(new MaestroUKValidator("6767730000000000").validate());
        assertTrue(new MaestroUKValidator("6767740000000000").validate());
        // China T-Union requires its documented 19-digit length; InterPayment uses 636 only.
        assertFalse(new ChinaTUnionValidator("3100000000000000").validate());
        assertTrue(new ChinaTUnionValidator("3100000000000000000").validate());
        assertTrue(new InterPaymentValidator("6360000000000000").validate());
        assertFalse(new InterPaymentValidator("6370000000000000").validate());
    }

    // ==================== TypeChecker ====================

    @ParameterizedTest
    @CsvSource({
            "4026000000000000,VISA_ELECTRON", "4405000000000000,VISA_ELECTRON",
            "4508000000000000,VISA_ELECTRON", "4844000000000000,VISA_ELECTRON",
            "4913000000000000,VISA_ELECTRON", "4917000000000000,VISA_ELECTRON",
            "0402600000000000,VISA_ELECTRON", "4H50000000000000,VISA_ELECTRON",
            "4111111111111111,VISA", "4111111111111,VISA", "4999999999999999999,VISA",
            "4000000000000000,VISA",
            "341111111111111,AMERICAN_EXPRESS", "371111111111111,AMERICAN_EXPRESS",
            "5100000000000000,MASTERCARD", "5500000000000000,MASTERCARD",
            "2221000000000000,MASTERCARD", "2720990000000000,MASTERCARD",
            "6011000000000000,DISCOVER", "6221260000000000,DISCOVER",
            "6229250000000000,DISCOVER", "6440000000000000,DISCOVER",
            "6490000000000000,DISCOVER", "6500000000000000,DISCOVER",
            "6759000000000000,MAESTRO_UK", "6767700000000000,MAESTRO_UK",
            "6767740000000000,MAESTRO_UK",
            "5000000000000000,MAESTRO", "5600000000000000,MAESTRO",
            "6000000000000000,MAESTRO", "6999990000000000,MAESTRO",
            "3528000000000000,JCB", "3589000000000000,JCB",
            "3100000000000000000,CHINA_TUNION",
            "3000000000000000,DINERS_CLUB_INTERNATIONAL",
            "3050000000000000,DINERS_CLUB_INTERNATIONAL",
            "3095000000000000,DINERS_CLUB_INTERNATIONAL",
            "3800000000000000,DINERS_CLUB_INTERNATIONAL",
            "3900000000000000,DINERS_CLUB_INTERNATIONAL",
            "2200000000000000,MIR", "2204000000000000,MIR",
            "9792000000000000,TROY", "9792890000000000,TROY",
            "000001000000000,UATP",
            "411111111111,OTHER", "41111111111111111111,OTHER",
            "3411111111111111,OTHER", "510000000000000,OTHER",
            "2220990000000000,OTHER",
            "2721000000000000,OTHER", "2199000000000000,OTHER",
            "2205000000000000,OTHER", "3100000000000000,OTHER",
            "100000000000000,OTHER", "9111111111111111,OTHER"})
    void typeChecker_classifiesByPrefixAndLength(String number, CreditCardType expected) {
        assertEquals(expected, TypeChecker.checkType(number));
    }

    @Test
    void typeChecker_earlierNetworksShadowLaterOnes() {
        // RuPay, InterPayment, InstaPayment, Verve, NPS, LankaPay, Dankort,
        // Diners Club and China Union Pay formats are all shadowed by earlier validators.
        assertEquals(CreditCardType.MAESTRO, TypeChecker.checkType("6000000000000000"));
        assertEquals(CreditCardType.DISCOVER, TypeChecker.checkType("6521000000000000"));
        assertEquals(CreditCardType.MAESTRO, TypeChecker.checkType("6360000000000000"));
        assertEquals(CreditCardType.MAESTRO, TypeChecker.checkType("6370000000000000"));
        assertEquals(CreditCardType.MAESTRO, TypeChecker.checkType("5060990000000000"));
        assertEquals(CreditCardType.DISCOVER, TypeChecker.checkType("6500020000000000"));
        assertEquals(CreditCardType.MAESTRO, TypeChecker.checkType("6054740000000000"));
        assertEquals(CreditCardType.MAESTRO, TypeChecker.checkType("6200000000000000"));
        assertEquals(CreditCardType.MAESTRO, TypeChecker.checkType("6221250000000000"));
        assertEquals(CreditCardType.MAESTRO, TypeChecker.checkType("5019000000000000"));
        assertEquals(CreditCardType.MAESTRO, TypeChecker.checkType("601100000000000"));
        assertEquals(CreditCardType.JCB, TypeChecker.checkType("3571110000000000"));
        assertEquals(CreditCardType.VISA, TypeChecker.checkType("4571000000000000"));
        assertEquals(CreditCardType.MASTERCARD, TypeChecker.checkType("5400000000000000"));
    }

    @Test
    void typeChecker_rejectsNullAndTooShortInputs() {
        assertThrows(NullPointerException.class, () -> TypeChecker.checkType(null));
        assertThrows(IndexOutOfBoundsException.class, () -> TypeChecker.checkType(""));
        assertThrows(IndexOutOfBoundsException.class, () -> TypeChecker.checkType("12345"));
        assertThrows(IndexOutOfBoundsException.class, () -> TypeChecker.checkType("1"));
        assertThrows(IndexOutOfBoundsException.class, () -> TypeChecker.checkType("000000"));
    }

    // ==================== CardEvidence / CardEvidenceCheck ====================

    @Test
    void cardEvidence_validatesAndTrimsItemId() {
        CardEvidence evidence = new CardEvidence("  item-1  ", "4111111111111111", YearMonth.of(2027, 1));
        assertEquals("item-1", evidence.itemId());
        assertThrows(IllegalArgumentException.class, () -> new CardEvidence(null, "4", YearMonth.of(2027, 1)));
        assertThrows(IllegalArgumentException.class, () -> new CardEvidence("  ", "4", YearMonth.of(2027, 1)));
        assertThrows(NullPointerException.class, () -> new CardEvidence("i", null, YearMonth.of(2027, 1)));
        assertThrows(NullPointerException.class, () -> new CardEvidence("i", "4", null));
    }

    @Test
    void cardEvidenceCheck_enforcesValidityAgreementWithIssues() {
        CardEvidenceCheck valid = new CardEvidenceCheck("i", CreditCardType.VISA, true, List.of());
        assertTrue(valid.valid());
        CardEvidenceCheck invalid = new CardEvidenceCheck("i", CreditCardType.OTHER, false, List.of("x"));
        assertFalse(invalid.valid());
        assertThrows(IllegalArgumentException.class,
                () -> new CardEvidenceCheck("i", CreditCardType.VISA, true, List.of("x")));
        assertThrows(IllegalArgumentException.class,
                () -> new CardEvidenceCheck("i", CreditCardType.VISA, false, List.of()));
        assertThrows(NullPointerException.class,
                () -> new CardEvidenceCheck(null, CreditCardType.VISA, true, List.of()));
        assertThrows(NullPointerException.class,
                () -> new CardEvidenceCheck("i", null, true, List.of()));
        assertThrows(NullPointerException.class,
                () -> new CardEvidenceCheck("i", CreditCardType.VISA, true, null));
        List<String> issues = new java.util.ArrayList<>(List.of("x"));
        CardEvidenceCheck copy = new CardEvidenceCheck("i", CreditCardType.VISA, false, issues);
        issues.add("y");
        assertEquals(List.of("x"), copy.issues());
        assertThrows(UnsupportedOperationException.class, () -> copy.issues().add("z"));
    }

    // ==================== CardEvidenceValidator ====================

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-15T08:00:00Z"), ZoneOffset.UTC);

    private static CardEvidenceValidator cardValidator() {
        return new CardEvidenceValidator(FIXED_CLOCK);
    }

    private static CardEvidence evidence(String number, YearMonth expires) {
        return new CardEvidence("item-1", number, expires);
    }

    @Test
    void cardEvidenceValidator_rejectsNullInputs() {
        CardEvidenceValidator validator = cardValidator();
        assertThrows(NullPointerException.class, () -> new CardEvidenceValidator(null));
        assertThrows(NullPointerException.class, () -> validator.check(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1", "123456789012", "12345678901234567890", "A4111111111111111"})
    void cardEvidenceValidator_rejectsSyntaxViolations(String number) {
        CardEvidenceCheck check = cardValidator().check(evidence(number, YearMonth.of(2027, 1)));
        assertFalse(check.valid());
        assertEquals(CreditCardType.OTHER, check.type());
        assertEquals(List.of("card number must contain 13 to 19 digits"), check.issues());
    }

    @Test
    void cardEvidenceValidator_rejectsInvalidChecksum() {
        CardEvidenceCheck check = cardValidator().check(evidence("4111111111111112", YearMonth.of(2027, 1)));
        assertFalse(check.valid());
        assertEquals(CreditCardType.VISA, check.type());
        assertEquals(List.of("card number checksum is invalid"), check.issues());
    }

    @Test
    void cardEvidenceValidator_rejectsUnsupportedNetworkForValidChecksum() {
        String number = luhnFill("91", 16);
        CardEvidenceCheck check = cardValidator().check(evidence(number, YearMonth.of(2027, 1)));
        assertFalse(check.valid());
        assertEquals(CreditCardType.OTHER, check.type());
        assertEquals(List.of("card network is not supported"), check.issues());
    }

    @Test
    void cardEvidenceValidator_acceptsValidVisaCard() {
        String number = luhnFill("4", 16);
        CardEvidenceCheck check = cardValidator().check(evidence(number, YearMonth.of(2027, 1)));
        assertTrue(check.valid());
        assertEquals(CreditCardType.VISA, check.type());
        assertEquals(List.of(), check.issues());
    }

    @Test
    void cardEvidenceValidator_checksExpiryRelativeToClock() {
        CardEvidenceValidator validator = cardValidator();
        String number = luhnFill("4", 16);
        assertTrue(validator.check(evidence(number, YearMonth.of(2026, 6))).valid());
        assertTrue(validator.check(evidence(number, YearMonth.of(2026, 7))).valid());
        CardEvidenceCheck expired = validator.check(evidence(number, YearMonth.of(2026, 5)));
        assertFalse(expired.valid());
        assertEquals(List.of("card has expired"), expired.issues());
    }

    @Test
    void cardEvidenceValidator_reportsBothChecksumAndExpiryIssues() {
        CardEvidenceCheck check = cardValidator().check(evidence("4111111111111112", YearMonth.of(2026, 5)));
        assertFalse(check.valid());
        assertEquals(List.of("card number checksum is invalid", "card has expired"), check.issues());
    }

    // __EXPENSE_RECORDS__

    // ==================== ExpenseClaim / ExpenseItem ====================

    @Test
    void expenseClaim_validatesMandatoryFieldsAndBounds() {
        List<ExpenseItem> oneItem = List.of(validItem());
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseClaim(null, "e", EmployeeGrade.STAFF, LocalDate.of(2026, 6, 15), "CC", oneItem,
                        true, false, false, true, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseClaim(" ", "e", EmployeeGrade.STAFF, LocalDate.of(2026, 6, 15), "CC", oneItem,
                        true, false, false, true, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseClaim("c", " ", EmployeeGrade.STAFF, LocalDate.of(2026, 6, 15), "CC", oneItem,
                        true, false, false, true, 0));
        assertThrows(NullPointerException.class,
                () -> new ExpenseClaim("c", "e", null, LocalDate.of(2026, 6, 15), "CC", oneItem,
                        true, false, false, true, 0));
        assertThrows(NullPointerException.class,
                () -> new ExpenseClaim("c", "e", EmployeeGrade.STAFF, null, "CC", oneItem,
                        true, false, false, true, 0));
        assertThrows(NullPointerException.class,
                () -> new ExpenseClaim("c", "e", EmployeeGrade.STAFF, LocalDate.of(2026, 6, 15), "CC", null,
                        true, false, false, true, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseClaim("c", "e", EmployeeGrade.STAFF, LocalDate.of(2026, 6, 15), "CC", List.of(),
                        true, false, false, true, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseClaim("c", "e", EmployeeGrade.STAFF, LocalDate.of(2026, 6, 15), "CC",
                        java.util.Collections.nCopies(101, validItem()), true, false, false, true, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseClaim("c", "e", EmployeeGrade.STAFF, LocalDate.of(2026, 6, 15), "CC", oneItem,
                        true, false, false, true, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseClaim("c", "e", EmployeeGrade.STAFF, LocalDate.of(2026, 6, 15), null, oneItem,
                        true, false, true, true, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseClaim("c", "e", EmployeeGrade.STAFF, LocalDate.of(2026, 6, 15), " ", oneItem,
                        true, false, true, true, 0));
        ExpenseClaim hundred = new ExpenseClaim("  c  ", " e ", EmployeeGrade.STAFF, LocalDate.of(2026, 6, 15),
                "CC", java.util.Collections.nCopies(100, validItem()), true, false, false, true, 0);
        assertEquals("c", hundred.claimId());
        assertEquals("e", hundred.employeeId());
    }

    @Test
    void expenseClaim_copiesItemsDefensively() {
        List<ExpenseItem> mutable = new java.util.ArrayList<>(List.of(validItem()));
        ExpenseClaim claim = new ExpenseClaim("c", "e", EmployeeGrade.STAFF, LocalDate.of(2026, 6, 15), "CC",
                mutable, true, false, false, true, 0);
        mutable.add(validItem());
        assertEquals(1, claim.items().size());
        assertThrows(UnsupportedOperationException.class, () -> claim.items().add(validItem()));
    }

    @Test
    void expenseItem_validatesFieldsAndRoundsAmount() {
        assertThrows(IllegalArgumentException.class, () -> expenseItem("", "10"));
        assertThrows(IllegalArgumentException.class, () -> expenseItem("  ", "10"));
        assertThrows(NullPointerException.class,
                () -> new ExpenseItem("i", null, new BigDecimal("10"), Currency.getInstance("CNY"),
                        LocalDate.of(2026, 6, 1), "M", "Business purpose here", ReceiptType.STANDARD_RECEIPT,
                        PaymentMethod.PERSONAL_CARD, 0, false, false, false));
        assertThrows(NullPointerException.class,
                () -> new ExpenseItem("i", ExpenseCategory.MEAL, null, Currency.getInstance("CNY"),
                        LocalDate.of(2026, 6, 1), "M", "Business purpose here", ReceiptType.STANDARD_RECEIPT,
                        PaymentMethod.PERSONAL_CARD, 0, false, false, false));
        assertThrows(NullPointerException.class,
                () -> new ExpenseItem("i", ExpenseCategory.MEAL, new BigDecimal("10"), null,
                        LocalDate.of(2026, 6, 1), "M", "Business purpose here", ReceiptType.STANDARD_RECEIPT,
                        PaymentMethod.PERSONAL_CARD, 0, false, false, false));
        assertThrows(NullPointerException.class,
                () -> new ExpenseItem("i", ExpenseCategory.MEAL, new BigDecimal("10"), Currency.getInstance("CNY"),
                        null, "M", "Business purpose here", ReceiptType.STANDARD_RECEIPT,
                        PaymentMethod.PERSONAL_CARD, 0, false, false, false));
        assertThrows(NullPointerException.class,
                () -> new ExpenseItem("i", ExpenseCategory.MEAL, new BigDecimal("10"), Currency.getInstance("CNY"),
                        LocalDate.of(2026, 6, 1), "M", "Business purpose here", null,
                        PaymentMethod.PERSONAL_CARD, 0, false, false, false));
        assertThrows(NullPointerException.class,
                () -> new ExpenseItem("i", ExpenseCategory.MEAL, new BigDecimal("10"), Currency.getInstance("CNY"),
                        LocalDate.of(2026, 6, 1), "M", "Business purpose here", ReceiptType.STANDARD_RECEIPT,
                        null, 0, false, false, false));
        assertThrows(IllegalArgumentException.class, () -> expenseItemAmount("i", "0"));
        assertThrows(IllegalArgumentException.class, () -> expenseItemAmount("i", "-1"));
        ExpenseItem rounded = expenseItemAmount("i", "1.005");
        assertEquals(new BigDecimal("1.01"), rounded.amount());
        assertEquals(new BigDecimal("1.00"), expenseItemAmount("i", "1.004").amount());
        assertThrows(IllegalArgumentException.class, () -> expenseItemAttendees("i", ExpenseCategory.MEAL, -1));
        assertThrows(IllegalArgumentException.class, () -> expenseItemAttendees("i", ExpenseCategory.MEAL, 1001));
        assertThrows(IllegalArgumentException.class,
                () -> expenseItemAttendees("i", ExpenseCategory.MEAL, 1));
        assertEquals(1000, expenseItemAttendees("i", ExpenseCategory.CLIENT_ENTERTAINMENT, 1000).attendeeCount());
        assertEquals("i", expenseItem("  i  ", "10").itemId());
        assertEquals("i", expenseItem(" i ", "10").itemId());
    }

    @Test
    void expenseAssessment_validatesAmountRelationships() {
        assertThrows(NullPointerException.class,
                () -> new ExpenseAssessment(null, BigDecimal.ZERO, ExpenseApprovalTier.FINANCE, false, false,
                        List.of(), List.of()));
        assertThrows(NullPointerException.class,
                () -> new ExpenseAssessment(BigDecimal.ZERO, null, ExpenseApprovalTier.FINANCE, false, false,
                        List.of(), List.of()));
        assertThrows(NullPointerException.class,
                () -> new ExpenseAssessment(BigDecimal.ZERO, BigDecimal.ZERO, null, false, false,
                        List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseAssessment(new BigDecimal("-1"), BigDecimal.ZERO, ExpenseApprovalTier.FINANCE,
                        false, false, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseAssessment(BigDecimal.ZERO, new BigDecimal("-1"), ExpenseApprovalTier.FINANCE,
                        false, false, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseAssessment(new BigDecimal("1"), new BigDecimal("2"), ExpenseApprovalTier.FINANCE,
                        false, false, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseAssessment(new BigDecimal("1"), new BigDecimal("1"), ExpenseApprovalTier.FINANCE,
                        true, false, List.of(), List.of()));
        ExpenseAssessment valid = new ExpenseAssessment(new BigDecimal("1"), BigDecimal.ZERO,
                ExpenseApprovalTier.FINANCE, true, false, List.of("v"), List.of());
        assertTrue(valid.rejected());
        assertEquals(List.of("v"), valid.violations());
    }

    @Test
    void expenseCardReview_validatesConsistencyOfDerivedFlags() {
        AccessDecision allowed = new AccessDecision(true, false, false, List.of());
        AccessDecision denied = new AccessDecision(false, false, false, List.of("blocked"));
        ExpenseAssessment ok = new ExpenseAssessment(new BigDecimal("10"), new BigDecimal("10"),
                ExpenseApprovalTier.LINE_MANAGER, false, false, List.of(), List.of());
        ExpenseCardReview consistentReady = new ExpenseCardReview("c", allowed, ok, List.of(), List.of(), true, false);
        assertTrue(consistentReady.readyForApproval());
        assertFalse(consistentReady.auditRequired());
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseCardReview("c", allowed, ok, List.of(), List.of(), false, false));
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseCardReview("c", allowed, ok, List.of(), List.of(), true, true));
        ExpenseCardReview deniedReview = new ExpenseCardReview("c", denied, ok, List.of(), List.of(), false, false);
        assertFalse(deniedReview.readyForApproval());
        ExpenseCardReview issueReview = new ExpenseCardReview("c", allowed, ok, List.of(), List.of("i"), false, true);
        assertTrue(issueReview.auditRequired());
        assertThrows(IllegalArgumentException.class, () -> new ExpenseCardReview(null, allowed, ok, List.of(),
                List.of(), true, false));
        assertThrows(IllegalArgumentException.class, () -> new ExpenseCardReview("  ", allowed, ok, List.of(),
                List.of(), true, false));
        assertEquals("c", new ExpenseCardReview(" c ", allowed, ok, List.of(), List.of(), true, false).claimId());
        ExpenseCardReview invalidCard = new ExpenseCardReview("c", allowed, ok,
                List.of(new CardEvidenceCheck("i", CreditCardType.VISA, false, List.of("bad"))),
                List.of(), false, false);
        assertFalse(invalidCard.readyForApproval());
        assertFalse(invalidCard.auditRequired());
        assertThrows(IllegalArgumentException.class,
                () -> new ExpenseCardReview("c", allowed, ok,
                        List.of(new CardEvidenceCheck("i", CreditCardType.VISA, false, List.of("bad"))),
                        List.of(), true, true));
    }

    // __EXPENSE_POLICY__

    // ==================== ExpensePolicy ====================

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    private static ExpenseItem validItem() {
        return expenseItem("i1", "10");
    }

    private static ExpenseItem expenseItem(String itemId, String amount) {
        return new ExpenseItem(itemId, ExpenseCategory.MEAL, new BigDecimal(amount), Currency.getInstance("CNY"),
                LocalDate.of(2026, 6, 10), "Merchant X", "Business purpose here",
                ReceiptType.STANDARD_RECEIPT, PaymentMethod.PERSONAL_CARD, 0, false, false, false);
    }

    private static ExpenseItem expenseItemAmount(String itemId, String amount) {
        return expenseItem(itemId, amount);
    }

    private static ExpenseItem expenseItemAttendees(String itemId, ExpenseCategory category, int attendees) {
        return new ExpenseItem(itemId, category, new BigDecimal("10"), Currency.getInstance("CNY"),
                LocalDate.of(2026, 6, 10), "Merchant X", "Business purpose here",
                ReceiptType.STANDARD_RECEIPT, PaymentMethod.PERSONAL_CARD, attendees, false, false, false);
    }

    private static ExpenseItem customItem(String id, ExpenseCategory category, String amount, PaymentMethod method,
            ReceiptType receipt, LocalDate incurred, int attendees, boolean alcohol, boolean personal, boolean dup) {
        return new ExpenseItem(id, category, new BigDecimal(amount), Currency.getInstance("CNY"), incurred,
                "Merchant X", "Business purpose here", receipt, method, attendees, alcohol, personal, dup);
    }

    private static ExpenseClaim claim(String employee, EmployeeGrade grade, LocalDate submitted,
            List<ExpenseItem> items, boolean preApproved, boolean international, boolean billable,
            boolean active, int previous) {
        return new ExpenseClaim("C1", employee, grade, submitted, "CC1", items,
                preApproved, international, billable, active, previous);
    }

    private static ExpenseClaim defaultClaim(List<ExpenseItem> items) {
        return claim("E1", EmployeeGrade.STAFF, TODAY, items, true, false, false, true, 0);
    }

    private static ExpenseAssessment assess(List<ExpenseItem> items, ExchangeRateProvider rates) {
        return new ExpensePolicy().assess(defaultClaim(items), rates, TODAY);
    }

    private static ExpenseAssessment assess(List<ExpenseItem> items) {
        return new ExpensePolicy().assess(defaultClaim(items), oneRate(), TODAY);
    }

    private static ExpenseAssessment assess(ExpenseClaim claim) {
        return new ExpensePolicy().assess(claim, oneRate(), TODAY);
    }

    private static ExchangeRateProvider oneRate() {
        return (currency, date) -> new BigDecimal("1");
    }

    @Test
    void expensePolicy_rejectsNullArguments() {
        ExpensePolicy policy = new ExpensePolicy();
        assertThrows(NullPointerException.class, () -> policy.assess(null, oneRate(), TODAY));
        assertThrows(NullPointerException.class, () -> policy.assess(defaultClaim(List.of(validItem())), null, TODAY));
        assertThrows(NullPointerException.class, () -> policy.assess(defaultClaim(List.of(validItem())), oneRate(), null));
    }

    @Test
    void expensePolicy_handlesMissingAndInvalidExchangeRates() {
        ExpenseAssessment missing = assess(List.of(validItem()), (currency, date) -> null);
        assertTrue(missing.rejected());
        assertEquals(new BigDecimal("0.00"), missing.reimbursableAmount());
        assertEquals(List.of("FATAL: missing exchange rate for item i1"), missing.violations());
        assertEquals(ExpenseApprovalTier.FINANCE, missing.approvalTier());
        ExpenseAssessment zero = assess(List.of(validItem()), (currency, date) -> BigDecimal.ZERO);
        assertTrue(zero.rejected());
        assertEquals(List.of("FATAL: invalid exchange rate for item i1"), zero.violations());
        ExpenseAssessment negative = assess(List.of(validItem()), (currency, date) -> new BigDecimal("-0.5"));
        assertTrue(negative.rejected());
        assertEquals(List.of("FATAL: invalid exchange rate for item i1"), negative.violations());
        ExpenseAssessment implausible = assess(List.of(validItem()), (currency, date) -> new BigDecimal("1000.01"));
        assertTrue(implausible.rejected());
        assertEquals(List.of("FATAL: implausible exchange rate for item i1"), implausible.violations());
        ExpenseAssessment maxOk = assess(List.of(validItem()), (currency, date) -> new BigDecimal("1000"));
        assertFalse(maxOk.rejected());
    }

    @Test
    void expensePolicy_convertsAndRoundsWithHalfUp() {
        ExpenseAssessment result = assess(List.of(validItem()), (currency, date) -> new BigDecimal("1.234"));
        assertEquals(new BigDecimal("12.34"), result.claimedAmount());
        assertEquals(new BigDecimal("12.34"), result.reimbursableAmount());
    }

    @Test
    void expensePolicy_simpleMealAutoApprovesWhenCheap() {
        ExpenseAssessment result = assess(List.of(validItem()));
        assertFalse(result.rejected());
        assertEquals(new BigDecimal("10.00"), result.claimedAmount());
        assertEquals(new BigDecimal("10.00"), result.reimbursableAmount());
        assertEquals(ExpenseApprovalTier.AUTO_APPROVED, result.approvalTier());
        assertFalse(result.auditRequired());
        assertEquals(List.of(), result.violations());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void expensePolicy_flagsFutureSubmissionAndLateReviews() {
        ExpenseAssessment future = assess(claim("E1", EmployeeGrade.STAFF, TODAY.plusDays(1), List.of(validItem()),
                true, false, false, true, 0));
        assertTrue(future.rejected());
        assertEquals(List.of("FATAL: submission date cannot be in the future"), future.violations());
        ExpenseAssessment late = assess(claim("E1", EmployeeGrade.STAFF, TODAY.minusDays(31), List.of(validItem()),
                true, false, false, true, 0));
        assertEquals(List.of("claim review is occurring more than thirty days after submission"), late.warnings());
        ExpenseAssessment withinThirty = assess(claim("E1", EmployeeGrade.STAFF, TODAY.minusDays(30),
                List.of(validItem()), true, false, false, true, 0));
        assertEquals(List.of(), withinThirty.warnings());
    }

    @Test
    void expensePolicy_flagsInactiveProjectForClientBillableClaims() {
        ExpenseAssessment result = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(validItem()),
                true, false, true, false, 0));
        assertTrue(result.rejected());
        assertEquals(List.of("FATAL: inactive project cannot receive client-billable expenses"),
                result.violations());
    }

    @Test
    void expensePolicy_warnsAboutMissingCostCenterForNonBillableClaims() {
        ExpenseClaim noCostCenter = new ExpenseClaim("C1", "E1", EmployeeGrade.STAFF, TODAY, "",
                List.of(validItem()), true, false, false, true, 0);
        ExpenseAssessment result = new ExpensePolicy().assess(noCostCenter, oneRate(), TODAY);
        assertEquals(List.of("cost center is missing"), result.warnings());
    }

    @Test
    void expensePolicy_flagsInternInternationalTravel() {
        ExpenseAssessment result = assess(claim("E1", EmployeeGrade.INTERN, TODAY, List.of(validItem()),
                true, true, false, true, 0));
        assertTrue(result.rejected());
        assertEquals(List.of("FATAL: intern international travel requires a separate process"),
                result.violations());
    }

    @Test
    void expensePolicy_warnsForLargeClaimsAndFrequentSubmissions() {
        List<ExpenseItem> bigList = new java.util.ArrayList<>();
        for (int i = 0; i < 51; i++) {
            bigList.add(validItem());
        }
        ExpenseAssessment big = assess(bigList, oneRate());
        assertEquals(List.of("large claim should be split by trip or purpose"), big.warnings());
        ExpenseAssessment frequent = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(validItem()),
                true, false, false, true, 21));
        assertEquals(List.of("unusually high claim frequency", "employee has a high monthly claim frequency"),
                frequent.warnings());
        ExpenseAssessment executive = assess(claim("E1", EmployeeGrade.EXECUTIVE, TODAY, List.of(validItem()),
                true, false, false, true, 0));
        assertEquals(List.of("executive pre-approval still requires finance review"), executive.warnings());
    }

    @Test
    void expensePolicy_flagsFutureAndAgedExpenseDates() {
        LocalDate future = TODAY.plusDays(1);
        ExpenseAssessment futureResult = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, future, 0, false, false, false)));
        assertTrue(futureResult.rejected());
        assertEquals(List.of("FATAL: future expense date for item i1"), futureResult.violations());
        LocalDate veryOld = TODAY.minusDays(91);
        ExpenseAssessment oldResult = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, veryOld, 0, false, false, false)));
        assertTrue(oldResult.rejected());
        assertEquals(List.of("FATAL: expense older than ninety days for item i1"), oldResult.violations());
        LocalDate slightlyOld = TODAY.minusDays(31);
        ExpenseAssessment warnResult = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, slightlyOld, 0, false, false, false)));
        assertEquals(List.of("late submission for item i1"), warnResult.warnings());
        LocalDate boundary = TODAY.minusDays(90);
        ExpenseAssessment boundaryResult = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, boundary, 0, false, false, false)));
        assertTrue(boundaryResult.warnings().contains("late submission for item i1"));
        assertFalse(boundaryResult.rejected());
    }

    @Test
    void expensePolicy_validatesMerchantAndPurposeFields() {
        ExpenseAssessment noMerchant = assess(List.of(
                new ExpenseItem("i1", ExpenseCategory.MEAL, new BigDecimal("10"), Currency.getInstance("CNY"),
                        TODAY.minusDays(5), null, "Business purpose here", ReceiptType.STANDARD_RECEIPT,
                        PaymentMethod.PERSONAL_CARD, 0, false, false, false)));
        assertEquals(List.of("merchant is required for item i1"), noMerchant.violations());
        ExpenseAssessment blankMerchant = assess(List.of(
                new ExpenseItem("i1", ExpenseCategory.MEAL, new BigDecimal("10"), Currency.getInstance("CNY"),
                        TODAY.minusDays(5), "   ", "Business purpose here", ReceiptType.STANDARD_RECEIPT,
                        PaymentMethod.PERSONAL_CARD, 0, false, false, false)));
        assertEquals(List.of("merchant is required for item i1"), blankMerchant.violations());
        ExpenseAssessment shortMerchant = assess(List.of(
                new ExpenseItem("i1", ExpenseCategory.MEAL, new BigDecimal("10"), Currency.getInstance("CNY"),
                        TODAY.minusDays(5), "A", "Business purpose here", ReceiptType.STANDARD_RECEIPT,
                        PaymentMethod.PERSONAL_CARD, 0, false, false, false)));
        assertEquals(List.of("merchant name is unusually short for item i1"), shortMerchant.warnings());
        ExpenseAssessment noPurpose = assess(List.of(
                new ExpenseItem("i1", ExpenseCategory.MEAL, new BigDecimal("10"), Currency.getInstance("CNY"),
                        TODAY.minusDays(5), "Merchant X", null, ReceiptType.STANDARD_RECEIPT,
                        PaymentMethod.PERSONAL_CARD, 0, false, false, false)));
        assertEquals(List.of("business purpose is required for item i1"), noPurpose.violations());
        ExpenseAssessment shortPurpose = assess(List.of(
                new ExpenseItem("i1", ExpenseCategory.MEAL, new BigDecimal("10"), Currency.getInstance("CNY"),
                        TODAY.minusDays(5), "Merchant X", "short", ReceiptType.STANDARD_RECEIPT,
                        PaymentMethod.PERSONAL_CARD, 0, false, false, false)));
        assertEquals(List.of("business purpose is too brief for item i1"), shortPurpose.warnings());
    }

    @Test
    void expensePolicy_personalComponentIsNotReimbursable() {
        ExpenseAssessment result = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, true, false)));
        assertTrue(result.rejected());
        assertEquals(new BigDecimal("0.00"), result.reimbursableAmount());
        assertTrue(result.violations().contains("personal component is not reimbursable for item i1"));
        assertTrue(result.auditRequired());
    }

    @Test
    void expensePolicy_alcoholOutsideEntertainmentIsAViolation() {
        ExpenseAssessment result = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, true, false, false)));
        assertTrue(result.violations().contains("alcohol is only allowed for client entertainment"));
        assertTrue(result.violations().contains("ordinary meal cannot include alcohol"));
    }

    @Test
    void expensePolicy_corporateCardCannotPayMedicalExpenses() {
        ExpenseAssessment result = assess(List.of(
                customItem("i1", ExpenseCategory.MEDICAL, "10", PaymentMethod.CORPORATE_CARD,
                        ReceiptType.TAX_INVOICE, TODAY.minusDays(5), 0, false, false, false)));
        assertEquals(List.of("corporate card cannot be used for medical expenses"), result.violations());
    }

    @Test
    void expensePolicy_receiptRequirementsFollowCategoryAndAmount() {
        ExpenseAssessment exemptSmallMeal = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "50.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.NONE, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(exemptSmallMeal.rejected());
        assertFalse(exemptSmallMeal.violations().contains("receipt is required for item i1"));
        ExpenseAssessment mealNeedsReceipt = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "50.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.NONE, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(mealNeedsReceipt.violations().contains("receipt is required for item i1"));
        ExpenseAssessment transportExempt = assess(List.of(
                customItem("i1", ExpenseCategory.LOCAL_TRANSPORT, "50.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.NONE, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(transportExempt.violations().contains("receipt is required for item i1"));
        ExpenseAssessment communicationExempt = assess(List.of(
                customItem("i1", ExpenseCategory.COMMUNICATION, "20.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.NONE, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(communicationExempt.violations().contains("receipt is required for item i1"));
        ExpenseAssessment communicationNeeds = assess(List.of(
                customItem("i1", ExpenseCategory.COMMUNICATION, "20.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.NONE, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(communicationNeeds.violations().contains("receipt is required for item i1"));
        ExpenseAssessment otherExempt = assess(List.of(
                customItem("i1", ExpenseCategory.OTHER, "10.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.NONE, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(otherExempt.violations().contains("receipt is required for item i1"));
        ExpenseAssessment otherNeeds = assess(List.of(
                customItem("i1", ExpenseCategory.OTHER, "10.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.NONE, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(otherNeeds.violations().contains("receipt is required for item i1"));
        ExpenseAssessment airfareNeeds = assess(List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "10.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.NONE, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(airfareNeeds.violations().contains("receipt is required for item i1"));
    }

    @Test
    void expensePolicy_singleItemOverMaximumIsFatal() {
        ExpenseAssessment result = assess(List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "50000.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(result.rejected());
        assertEquals(List.of("FATAL: single item exceeds the maximum supported amount"), result.violations());
        ExpenseAssessment boundary = assess(List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "50000.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(boundary.rejected());
    }

    @Test
    void expensePolicy_weekendOfficeSuppliesNeedExplanation() {
        LocalDate saturday = LocalDate.of(2026, 6, 13);
        ExpenseAssessment weekend = assess(List.of(
                customItem("i1", ExpenseCategory.OFFICE_SUPPLIES, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, saturday, 0, false, false, false)));
        assertEquals(List.of("weekend office-supply purchase needs explanation"), weekend.warnings());
        LocalDate friday = LocalDate.of(2026, 6, 12);
        ExpenseAssessment weekday = assess(List.of(
                customItem("i1", ExpenseCategory.OFFICE_SUPPLIES, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, friday, 0, false, false, false)));
        assertEquals(List.of(), weekday.warnings());
    }

    @Test
    void expensePolicy_detectsDuplicateFingerprintsAndSuspectedDuplicates() {
        ExpenseItem first = customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.PERSONAL_CARD,
                ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false);
        ExpenseItem second = customItem("i2", ExpenseCategory.MEAL, "10", PaymentMethod.PERSONAL_CARD,
                ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false);
        ExpenseAssessment duplicate = assess(List.of(first, second));
        assertEquals(List.of("possible duplicate item: i2"), duplicate.violations());
        ExpenseAssessment marked = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, true)));
        assertEquals(List.of("item is marked as a suspected duplicate: i1"), marked.violations());
        assertTrue(marked.auditRequired());
        assertEquals(new BigDecimal("0.00"), marked.reimbursableAmount());
        assertTrue(marked.rejected());
    }

    @Test
    void expensePolicy_appliesCategoryCapsAndNotesThem() {
        ExpenseAssessment result = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "200", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertEquals(new BigDecimal("200.00"), result.claimedAmount());
        assertEquals(new BigDecimal("160.00"), result.reimbursableAmount());
        assertEquals(List.of("category cap applied to item i1",
                "one or more items were capped or excluded"), result.warnings());
        ExpenseAssessment exactCap = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "160", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertEquals(new BigDecimal("160.00"), exactCap.reimbursableAmount());
        assertEquals(List.of(), exactCap.warnings());
    }

    @Test
    void expensePolicy_internationalTripRaisesHotelMealAndTransportCaps() {
        ExpenseClaim international = claim("E1", EmployeeGrade.STAFF, TODAY, List.of(
                customItem("i1", ExpenseCategory.MEAL, "240", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, true, false, true, 0);
        ExpenseAssessment result = new ExpensePolicy().assess(international, oneRate(), TODAY);
        assertEquals(new BigDecimal("240.00"), result.reimbursableAmount());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void expensePolicy_clientBillableRaisesEntertainmentCap() {
        ExpenseClaim billable = claim("E1", EmployeeGrade.STAFF, TODAY, List.of(
                customItem("i1", ExpenseCategory.CLIENT_ENTERTAINMENT, "1250", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 2, false, false, false)),
                true, false, true, true, 0);
        ExpenseAssessment result = new ExpensePolicy().assess(billable, oneRate(), TODAY);
        assertEquals(new BigDecimal("1250.00"), result.reimbursableAmount());
    }

    @Test
    void expensePolicy_airfareNeedsPreApprovalAboveThreshold() {
        ExpenseAssessment withoutApproval = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "1000.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                false, false, false, true, 0));
        assertEquals(List.of("airfare over 1000 requires pre-approval"), withoutApproval.violations());
        ExpenseAssessment cashAirfare = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "500", PaymentMethod.CASH,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, false, false, true, 0));
        assertEquals(List.of("cash airfare purchase requires verification"), cashAirfare.warnings());
        ExpenseAssessment withApproval = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "1000.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, false, false, true, 0));
        assertEquals(List.of(), withApproval.violations());
    }

    @Test
    void expensePolicy_hotelRulesCoverStatementReceiptsAndDomesticAmounts() {
        ExpenseAssessment statement = assess(List.of(
                customItem("i1", ExpenseCategory.HOTEL, "100", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.BANK_STATEMENT, TODAY.minusDays(5), 0, false, false, false)));
        assertEquals(List.of("hotel requires an invoice or receipt"), statement.violations());
        assertTrue(statement.auditRequired());
        ExpenseAssessment domesticHigh = assess(List.of(
                customItem("i1", ExpenseCategory.HOTEL, "2000.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(domesticHigh.warnings().contains("domestic hotel amount is unusually high"));
    }

    @Test
    void expensePolicy_mealRulesCoverAlcohol() {
        ExpenseAssessment alcohol = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, true, false, false)));
        assertTrue(alcohol.violations().contains("ordinary meal cannot include alcohol"));
        assertTrue(alcohol.violations().contains("alcohol is only allowed for client entertainment"));
    }

    @Test
    void expensePolicy_localTransportRulesCoverCashWithoutReceiptAndLargeAmounts() {
        ExpenseAssessment cash = assess(List.of(
                customItem("i1", ExpenseCategory.LOCAL_TRANSPORT, "100", PaymentMethod.CASH,
                        ReceiptType.NONE, TODAY.minusDays(5), 0, false, false, false)));
        assertEquals(List.of("cash transport without receipt will be capped"), cash.warnings());
        ExpenseAssessment large = assess(List.of(
                customItem("i1", ExpenseCategory.LOCAL_TRANSPORT, "1000.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(large.warnings().contains("local transport amount needs route details"));
    }

    @Test
    void expensePolicy_officeSupplyRulesCoverInternLimitAndLargePersonalPurchases() {
        ExpenseAssessment internOver = assess(claim("E1", EmployeeGrade.INTERN, TODAY, List.of(
                customItem("i1", ExpenseCategory.OFFICE_SUPPLIES, "100.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, false, false, true, 0));
        assertEquals(List.of("intern office-supply purchase exceeds delegated limit"), internOver.violations());
        ExpenseAssessment largePersonal = assess(List.of(
                customItem("i1", ExpenseCategory.OFFICE_SUPPLIES, "500.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(largePersonal.warnings().contains("large office purchase should use corporate procurement"));
    }

    @Test
    void expensePolicy_entertainmentRulesCoverAttendeesAlcoholAndApproval() {
        ExpenseAssessment noAttendees = assess(List.of(
                customItem("i1", ExpenseCategory.CLIENT_ENTERTAINMENT, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertEquals(List.of("client entertainment requires attendee count"), noAttendees.violations());
        ExpenseAssessment noApproval = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(
                customItem("i1", ExpenseCategory.CLIENT_ENTERTAINMENT, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 2, false, false, false)),
                false, false, false, true, 0));
        assertEquals(List.of("client entertainment lacks pre-approval"), noApproval.warnings());
        ExpenseAssessment soloAlcohol = assess(List.of(
                customItem("i1", ExpenseCategory.CLIENT_ENTERTAINMENT, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 1, true, false, false)));
        assertEquals(List.of("alcohol expense requires at least two attendees"), soloAlcohol.violations());
        ExpenseAssessment ok = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(
                customItem("i1", ExpenseCategory.CLIENT_ENTERTAINMENT, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 2, true, false, false)),
                true, false, false, true, 0));
        assertEquals(List.of(), ok.violations());
    }

    @Test
    void expensePolicy_trainingRulesCoverApprovalAndInterns() {
        ExpenseAssessment noApproval = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(
                customItem("i1", ExpenseCategory.TRAINING, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                false, false, false, true, 0));
        assertEquals(List.of("training requires pre-approval"), noApproval.violations());
        ExpenseAssessment intern = assess(claim("E1", EmployeeGrade.INTERN, TODAY, List.of(
                customItem("i1", ExpenseCategory.TRAINING, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, false, false, true, 0));
        assertEquals(List.of("intern training requires department confirmation"), intern.warnings());
    }

    @Test
    void expensePolicy_softwareRulesCoverCashAndLargePurchases() {
        ExpenseAssessment cash = assess(List.of(
                customItem("i1", ExpenseCategory.SOFTWARE, "10", PaymentMethod.CASH,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertEquals(List.of("software cannot be purchased with cash"), cash.violations());
        ExpenseAssessment large = assess(List.of(
                customItem("i1", ExpenseCategory.SOFTWARE, "3000.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(large.warnings().contains("software purchase should use procurement review"));
    }

    @Test
    void expensePolicy_communicationRulesCoverLargeAmounts() {
        ExpenseAssessment large = assess(List.of(
                customItem("i1", ExpenseCategory.COMMUNICATION, "500.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(large.warnings().contains("communication expense exceeds ordinary monthly level"));
    }

    @Test
    void expensePolicy_medicalRulesCoverReceiptTypes() {
        ExpenseAssessment standard = assess(List.of(
                customItem("i1", ExpenseCategory.MEDICAL, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertEquals(List.of("medical expense requires a recognized receipt"), standard.violations());
        ExpenseAssessment taxInvoice = assess(List.of(
                customItem("i1", ExpenseCategory.MEDICAL, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.TAX_INVOICE, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(taxInvoice.violations().contains("medical expense requires a recognized receipt"));
        ExpenseAssessment electronic = assess(List.of(
                customItem("i1", ExpenseCategory.MEDICAL, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.ELECTRONIC_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(electronic.violations().contains("medical expense requires a recognized receipt"));
    }

    @Test
    void expensePolicy_otherCategoryRulesCoverApprovalAndLimit() {
        ExpenseAssessment noApproval = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(
                customItem("i1", ExpenseCategory.OTHER, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                false, false, false, true, 0));
        assertEquals(List.of("other-category expense needs manager explanation"), noApproval.warnings());
        ExpenseAssessment overLimit = assess(List.of(
                customItem("i1", ExpenseCategory.OTHER, "500.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertEquals(List.of("other-category expense exceeds allowed limit"), overLimit.violations());
    }

    @Test
    void expensePolicy_auditTriggersCoverAmountsMethodsAndCounts() {
        ExpenseAssessment largeItem = assess(List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "5000.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(largeItem.auditRequired());
        ExpenseAssessment largeCash = assess(List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "1000.01", PaymentMethod.CASH,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(largeCash.auditRequired());
        ExpenseAssessment statement = assess(List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.BANK_STATEMENT, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(statement.auditRequired());
        ExpenseAssessment frequent = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(validItem()),
                true, false, false, true, 10));
        assertTrue(frequent.auditRequired());
        assertTrue(frequent.warnings().contains("employee has a high monthly claim frequency"));
        List<ExpenseItem> twentyItems = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            twentyItems.add(validItem());
        }
        ExpenseAssessment twenty = assess(twentyItems, oneRate());
        assertTrue(twenty.auditRequired());
        ExpenseAssessment threeViolations = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.NONE, TODAY.minusDays(5), 0, true, false, false),
                customItem("i2", ExpenseCategory.SOFTWARE, "10", PaymentMethod.CASH,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false),
                customItem("i3", ExpenseCategory.TRAINING, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(threeViolations.auditRequired());
    }

    @Test
    void expensePolicy_resolvesApprovalTiersFromClaimedAmounts() {
        assertEquals(ExpenseApprovalTier.AUTO_APPROVED,
                assess(List.of(customItem("i1", ExpenseCategory.OTHER, "200.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false))).approvalTier());
        assertEquals(ExpenseApprovalTier.LINE_MANAGER,
                assess(List.of(customItem("i1", ExpenseCategory.AIRFARE, "200.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false))).approvalTier());
        assertEquals(ExpenseApprovalTier.LINE_MANAGER,
                assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(validItem()),
                        false, false, false, true, 0)).approvalTier());
        assertEquals(ExpenseApprovalTier.DEPARTMENT_HEAD,
                assess(List.of(customItem("i1", ExpenseCategory.AIRFARE, "2000.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false))).approvalTier());
        assertEquals(ExpenseApprovalTier.DEPARTMENT_HEAD,
                assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(validItem()),
                        true, false, true, true, 0)).approvalTier());
        assertEquals(ExpenseApprovalTier.FINANCE,
                assess(List.of(customItem("i1", ExpenseCategory.AIRFARE, "5000.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false))).approvalTier());
        assertEquals(ExpenseApprovalTier.FINANCE_DIRECTOR,
                assess(List.of(customItem("i1", ExpenseCategory.AIRFARE, "20000.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false))).approvalTier());
        assertEquals(ExpenseApprovalTier.FINANCE_DIRECTOR,
                assess(claim("E1", EmployeeGrade.EXECUTIVE, TODAY, List.of(validItem()),
                        false, false, false, true, 0)).approvalTier());
        assertEquals(ExpenseApprovalTier.CHIEF_FINANCIAL_OFFICER,
                assess(List.of(
                        customItem("i1", ExpenseCategory.AIRFARE, "25000.01", PaymentMethod.PERSONAL_CARD,
                                ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false),
                        customItem("i2", ExpenseCategory.AIRFARE, "25000.01", PaymentMethod.PERSONAL_CARD,
                                ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)))
                        .approvalTier());
        ExpenseAssessment exactlyFiftyThousand = assess(List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "25000.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false),
                customItem("i2", ExpenseCategory.AIRFARE, "25000.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertEquals(ExpenseApprovalTier.FINANCE_DIRECTOR, exactlyFiftyThousand.approvalTier());
    }

    // __SERVICE__

    // ==================== CorporateCardExpenseService ====================

    private static CorporateCardExpenseService service() {
        return new CorporateCardExpenseService(new ExpensePolicy(), cardValidator(),
                new AccessPolicy(), new ApprovalWorkflowEngine());
    }

    private static AccessContext createActor(String actorId) {
        return new AccessContext(actorId, null, "DEP", "DEP", UserRole.EMPLOYEE, ResourceAction.CREATE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true);
    }

    @Test
    void expenseService_rejectsNullConstructorArguments() {
        assertThrows(NullPointerException.class,
                () -> new CorporateCardExpenseService(null, cardValidator(), new AccessPolicy(),
                        new ApprovalWorkflowEngine()));
        assertThrows(NullPointerException.class,
                () -> new CorporateCardExpenseService(new ExpensePolicy(), null, new AccessPolicy(),
                        new ApprovalWorkflowEngine()));
        assertThrows(NullPointerException.class,
                () -> new CorporateCardExpenseService(new ExpensePolicy(), cardValidator(), null,
                        new ApprovalWorkflowEngine()));
        assertThrows(NullPointerException.class,
                () -> new CorporateCardExpenseService(new ExpensePolicy(), cardValidator(), new AccessPolicy(),
                        null));
    }

    @Test
    void expenseService_reviewRejectsInvalidActorContexts() {
        CorporateCardExpenseService svc = service();
        ExpenseClaim claim = defaultClaim(List.of(validItem()));
        assertThrows(NullPointerException.class,
                () -> svc.review(null, Map.of(), oneRate(), TODAY, createActor("E1")));
        assertThrows(NullPointerException.class,
                () -> svc.review(claim, null, oneRate(), TODAY, createActor("E1")));
        assertThrows(NullPointerException.class,
                () -> svc.review(claim, Map.of(), oneRate(), TODAY, null));
        AccessContext wrongResource = new AccessContext("E1", null, "DEP", "DEP", UserRole.EMPLOYEE,
                ResourceAction.CREATE, ResourceType.LEAVE_REQUEST, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true);
        assertThrows(IllegalArgumentException.class,
                () -> svc.review(claim, Map.of(), oneRate(), TODAY, wrongResource));
        AccessContext wrongAction = new AccessContext("E1", null, "DEP", "DEP", UserRole.EMPLOYEE,
                ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true);
        assertThrows(IllegalArgumentException.class,
                () -> svc.review(claim, Map.of(), oneRate(), TODAY, wrongAction));
        assertThrows(IllegalArgumentException.class,
                () -> svc.review(claim, Map.of(), oneRate(), TODAY, createActor("someone-else")));
    }

    @Test
    void expenseService_reviewSkipsNonCorporateCardItems() {
        ExpenseCardReview review = service().review(defaultClaim(List.of(validItem())), Map.of(),
                oneRate(), TODAY, createActor("E1"));
        assertTrue(review.readyForApproval());
        assertFalse(review.auditRequired());
        assertEquals(List.of(), review.cards());
        assertEquals(List.of(), review.issues());
    }

    @Test
    void expenseService_reviewFlagsMissingAndMismatchedEvidence() {
        ExpenseItem cardItem = customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.CORPORATE_CARD,
                ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false);
        ExpenseCardReview missing = service().review(defaultClaim(List.of(cardItem)), Map.of(),
                oneRate(), TODAY, createActor("E1"));
        assertFalse(missing.readyForApproval());
        assertTrue(missing.auditRequired());
        assertEquals(List.of("corporate card evidence missing for item i1"), missing.issues());
        ExpenseCardReview mismatch = service().review(defaultClaim(List.of(cardItem)),
                Map.of("i1", new CardEvidence("other", luhnFill("4", 16), YearMonth.of(2027, 1))),
                oneRate(), TODAY, createActor("E1"));
        assertEquals(List.of("card evidence item mismatch for item i1"), mismatch.issues());
    }

    @Test
    void expenseService_reviewValidatesCardEvidence() {
        ExpenseItem cardItem = customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.CORPORATE_CARD,
                ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false);
        ExpenseCardReview valid = service().review(defaultClaim(List.of(cardItem)),
                Map.of("i1", new CardEvidence("i1", luhnFill("4", 16), YearMonth.of(2027, 1))),
                oneRate(), TODAY, createActor("E1"));
        assertTrue(valid.readyForApproval());
        assertEquals(1, valid.cards().size());
        assertTrue(valid.cards().get(0).valid());
        ExpenseCardReview invalid = service().review(defaultClaim(List.of(cardItem)),
                Map.of("i1", new CardEvidence("i1", "4111111111111112", YearMonth.of(2026, 5))),
                oneRate(), TODAY, createActor("E1"));
        assertFalse(invalid.readyForApproval());
        assertTrue(invalid.auditRequired());
        assertEquals(List.of("corporate card evidence invalid for item i1"), invalid.issues());
    }

    @Test
    void expenseService_reviewRespectsDeniedAccess() {
        AccessContext inactive = new AccessContext("E1", null, "DEP", "DEP", UserRole.EMPLOYEE,
                ResourceAction.CREATE, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                false, false, true, true, true, true, false, false, false, false, true, true, true);
        ExpenseCardReview review = service().review(defaultClaim(List.of(validItem())), Map.of(),
                oneRate(), TODAY, inactive);
        assertFalse(review.readyForApproval());
        assertFalse(review.access().allowed());
        assertTrue(review.access().reasons().contains("account is inactive"));
    }

    @Test
    void expenseService_startApprovalValidatesAndStartsWorkflow() {
        CorporateCardExpenseService svc = service();
        ExpenseItem cardItem = customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.CORPORATE_CARD,
                ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false);
        ExpenseClaim claim = defaultClaim(List.of(cardItem));
        ExpenseCardReview review = svc.review(claim,
                Map.of("i1", new CardEvidence("i1", luhnFill("4", 16), YearMonth.of(2027, 1))),
                oneRate(), TODAY, createActor("E1"));
        assertTrue(review.readyForApproval());
        ApprovalStep step = new ApprovalStep("s1", "Manager", Set.of("M1"), StepMode.ANY_ONE, 1,
                false, false, null);
        Instant now = Instant.parse("2026-06-15T08:00:00Z");
        WorkflowSnapshot snapshot = svc.startApproval(claim, review, List.of(step), now);
        assertEquals("C1", snapshot.workflowId());
        assertEquals("E1", snapshot.requesterId());
        assertFalse(snapshot.urgent());
        assertTrue(snapshot.withdrawalAllowed());
        assertEquals(WorkflowState.PENDING, snapshot.state());
        assertThrows(NullPointerException.class, () -> svc.startApproval(null, review, List.of(step), now));
        assertThrows(NullPointerException.class, () -> svc.startApproval(claim, null, List.of(step), now));
        ExpenseClaim otherClaim = new ExpenseClaim("C2", "E1", EmployeeGrade.STAFF, TODAY, "CC1",
                List.of(cardItem), true, false, false, true, 0);
        assertThrows(IllegalArgumentException.class,
                () -> svc.startApproval(otherClaim, review, List.of(step), now));
        ExpenseCardReview notReady = svc.review(defaultClaim(List.of(cardItem)), Map.of(),
                oneRate(), TODAY, createActor("E1"));
        assertThrows(IllegalStateException.class, () -> svc.startApproval(claim, notReady, List.of(step), now));
    }

    // __PERMISSION__

    // ==================== AccessContext / AccessDecision ====================

    @Test
    void accessContext_validatesFieldsAndDerivedProperties() {
        assertThrows(IllegalArgumentException.class, () -> new AccessContext(null, null, "D", "D",
                UserRole.EMPLOYEE, ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                AccessScope.OWN, true, false, true, true, true, true, false, false, false, false,
                true, true, true));
        assertThrows(IllegalArgumentException.class, () -> new AccessContext(" ", null, "D", "D",
                UserRole.EMPLOYEE, ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                AccessScope.OWN, true, false, true, true, true, true, false, false, false, false,
                true, true, true));
        assertThrows(NullPointerException.class, () -> new AccessContext("A", null, "D", "D",
                null, ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                AccessScope.OWN, true, false, true, true, true, true, false, false, false, false,
                true, true, true));
        assertThrows(NullPointerException.class, () -> new AccessContext("A", null, "D", "D",
                UserRole.EMPLOYEE, null, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                AccessScope.OWN, true, false, true, true, true, true, false, false, false, false,
                true, true, true));
        assertThrows(NullPointerException.class, () -> new AccessContext("A", null, "D", "D",
                UserRole.EMPLOYEE, ResourceAction.VIEW, null, Sensitivity.INTERNAL,
                AccessScope.OWN, true, false, true, true, true, true, false, false, false, false,
                true, true, true));
        assertThrows(NullPointerException.class, () -> new AccessContext("A", null, "D", "D",
                UserRole.EMPLOYEE, ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, null,
                AccessScope.OWN, true, false, true, true, true, true, false, false, false, false,
                true, true, true));
        assertThrows(NullPointerException.class, () -> new AccessContext("A", null, "D", "D",
                UserRole.EMPLOYEE, ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                null, true, false, true, true, true, true, false, false, false, false,
                true, true, true));
        assertThrows(IllegalArgumentException.class, () -> new AccessContext("A", null, "D", "D",
                UserRole.EMPLOYEE, ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                AccessScope.OWN, true, true, true, true, true, true, false, false, false, false,
                true, true, true));
        assertThrows(IllegalArgumentException.class, () -> new AccessContext("A", null, "D", "D",
                UserRole.EMPLOYEE, ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                AccessScope.OWN, true, false, true, true, true, true, false, true, false, false,
                true, true, true));
        AccessContext trimmed = new AccessContext(" A ", " B ", " D ", " E ", UserRole.EMPLOYEE,
                ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true);
        assertEquals("A", trimmed.actorId());
        assertEquals("B", trimmed.ownerId());
        assertEquals("D", trimmed.actorDepartment());
        assertEquals("E", trimmed.resourceDepartment());
        assertFalse(trimmed.ownsResource());
        assertFalse(trimmed.sameDepartment());
        AccessContext noOwner = new AccessContext("A", null, "D", "D", UserRole.EMPLOYEE,
                ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true);
        assertFalse(noOwner.ownsResource());
        assertTrue(noOwner.sameDepartment());
        AccessContext differentOwner = new AccessContext("A", "C", "D", "D", UserRole.EMPLOYEE,
                ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true);
        assertFalse(differentOwner.ownsResource());
        assertTrue(differentOwner.sameDepartment());
        AccessContext differentDept = new AccessContext("A", null, "D1", "D2", UserRole.EMPLOYEE,
                ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true);
        assertFalse(differentDept.sameDepartment());
    }

    @Test
    void accessDecision_validatesDenialReasonsAndStepUpConsistency() {
        assertThrows(NullPointerException.class,
                () -> new AccessDecision(false, false, false, null));
        assertThrows(IllegalArgumentException.class,
                () -> new AccessDecision(false, false, false, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new AccessDecision(true, false, true, List.of()));
        AccessDecision allowed = new AccessDecision(true, false, false, List.of());
        assertTrue(allowed.allowed());
        AccessDecision deniedWithStepUp = new AccessDecision(false, false, true, List.of("mfa"));
        assertTrue(deniedWithStepUp.stepUpAuthenticationRequired());
        List<String> reasons = new java.util.ArrayList<>(List.of("r"));
        AccessDecision copied = new AccessDecision(false, false, false, reasons);
        reasons.add("r2");
        assertEquals(List.of("r"), copied.reasons());
    }

    // ==================== AccessPolicy ====================

    private static AccessContext actx(String actorId, String ownerId, String actorDepartment,
            String resourceDepartment, UserRole role, ResourceAction action, ResourceType resourceType,
            Sensitivity sensitivity, AccessScope scope, boolean accountActive, boolean accountSuspended,
            boolean mfa, boolean trustedDevice, boolean trustedNetwork, boolean businessHours, boolean delegated,
            boolean delegationValid, boolean legalHold, boolean archived, boolean projectMember,
            boolean dataRegionMatches, boolean training) {
        return new AccessContext(actorId, ownerId, actorDepartment, resourceDepartment, role, action, resourceType,
                sensitivity, scope, accountActive, accountSuspended, mfa, trustedDevice, trustedNetwork,
                businessHours, delegated, delegationValid, legalHold, archived, projectMember, dataRegionMatches,
                training);
    }

    private static AccessContext actx(String actorId, String ownerId, UserRole role, ResourceAction action,
            ResourceType resourceType, Sensitivity sensitivity, AccessScope scope,
            boolean accountActive, boolean accountSuspended, boolean mfa, boolean trustedDevice,
            boolean trustedNetwork, boolean businessHours, boolean delegated, boolean delegationValid,
            boolean legalHold, boolean archived, boolean projectMember, boolean dataRegionMatches,
            boolean training) {
        return new AccessContext(actorId, ownerId, "DEP", "DEP", role, action, resourceType, sensitivity,
                scope, accountActive, accountSuspended, mfa, trustedDevice, trustedNetwork, businessHours,
                delegated, delegationValid, legalHold, archived, projectMember, dataRegionMatches, training);
    }

    private static AccessContext actx(String actorId, String ownerId, UserRole role, ResourceAction action,
            ResourceType resourceType, Sensitivity sensitivity, AccessScope scope) {
        return actx(actorId, ownerId, role, action, resourceType, sensitivity, scope, true, false, true, true,
                true, true, false, false, false, false, true, true, true);
    }

    private static AccessContext actx(UserRole role, ResourceAction action, ResourceType resourceType,
            AccessScope scope, Sensitivity sensitivity) {
        return actx("A", null, role, action, resourceType, sensitivity, scope);
    }

    private static AccessDecision decide(AccessContext context) {
        return new AccessPolicy().decide(context);
    }

    @Test
    void accessPolicy_rejectsNullContext() {
        assertThrows(NullPointerException.class, () -> new AccessPolicy().decide(null));
    }

    @Test
    void accessPolicy_deniesInactiveAndSuspendedAccounts() {
        AccessDecision inactive = decide(actx("A", null, UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.PUBLIC_DOCUMENT, Sensitivity.PUBLIC, AccessScope.OWN,
                false, false, true, true, true, true, false, false, false, false, true, true, true));
        assertFalse(inactive.allowed());
        assertEquals(List.of("account is inactive", "requested scope is outside the actor's authority"),
                inactive.reasons());
        AccessDecision suspended = decide(actx("A", null, UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.PUBLIC_DOCUMENT, Sensitivity.PUBLIC, AccessScope.OWN,
                false, true, true, true, true, true, false, false, false, false, true, true, true));
        assertFalse(suspended.allowed());
        assertEquals(List.of("account is inactive", "account is suspended",
                "requested scope is outside the actor's authority"), suspended.reasons());
    }

    @Test
    void accessPolicy_guestMayOnlyViewPublicDocuments() {
        AccessDecision guestExpense = decide(actx(UserRole.GUEST, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, AccessScope.OWN, Sensitivity.PUBLIC));
        assertFalse(guestExpense.allowed());
        assertTrue(guestExpense.reasons().contains("guest may only access public documents"));
        assertTrue(guestExpense.reasons().contains("role does not grant the requested action"));
        assertTrue(guestExpense.reasons().contains("requested scope is outside the actor's authority"));
        AccessDecision guestConfidential = decide(actx(UserRole.GUEST, ResourceAction.VIEW,
                ResourceType.PUBLIC_DOCUMENT, AccessScope.OWN, Sensitivity.CONFIDENTIAL));
        assertFalse(guestConfidential.allowed());
        AccessDecision guestWrongAction = decide(actx(UserRole.GUEST, ResourceAction.APPROVE,
                ResourceType.PUBLIC_DOCUMENT, AccessScope.OWN, Sensitivity.PUBLIC));
        assertFalse(guestWrongAction.allowed());
        AccessDecision guestOk = decide(actx("A", "A", UserRole.GUEST, ResourceAction.VIEW,
                ResourceType.PUBLIC_DOCUMENT, Sensitivity.PUBLIC, AccessScope.OWN));
        assertTrue(guestOk.allowed());
        assertTrue(guestOk.reasons().contains("guest may view public documents"));
        assertTrue(guestOk.reasons().contains("resource is publicly classified"));
    }

    @Test
    void accessPolicy_restrictedResourcesRequireMfaDeviceAndTraining() {
        AccessDecision noMfa = decide(actx("A", null, UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.RESTRICTED, AccessScope.OWN,
                true, false, false, true, true, true, false, false, false, false, true, true, true));
        assertFalse(noMfa.allowed());
        assertTrue(noMfa.stepUpAuthenticationRequired());
        assertTrue(noMfa.reasons().contains("restricted resource requires multi-factor authentication"));
        AccessDecision untrustedDevice = decide(actx("A", null, UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.RESTRICTED, AccessScope.OWN,
                true, false, true, false, true, true, false, false, false, false, true, true, true));
        assertTrue(untrustedDevice.reasons().contains("restricted resource requires a trusted device"));
        AccessDecision noTraining = decide(actx("A", null, UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.CONFIDENTIAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, false));
        assertTrue(noTraining.reasons().contains("security training is required"));
        AccessDecision restrictedOk = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.RESTRICTED, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true));
        assertTrue(restrictedOk.allowed());
        assertTrue(restrictedOk.auditRequired());
    }

    @Test
    void accessPolicy_exportAndSharingRequireTrustedNetworkAndRegion() {
        AccessDecision crossRegion = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.EXPORT,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.TEAM,
                true, false, true, true, true, true, false, false, false, false, true, false, true));
        assertFalse(crossRegion.allowed());
        assertTrue(crossRegion.reasons().contains("cross-region export is not allowed"));
        AccessDecision untrustedNetwork = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.EXPORT,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.TEAM,
                true, false, true, true, false, true, false, false, false, false, true, true, true));
        assertTrue(untrustedNetwork.reasons().contains("export or sharing requires a trusted network"));
        AccessDecision shareOffNetwork = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.SHARE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.TEAM,
                true, false, true, true, false, true, false, false, false, false, true, true, true));
        assertFalse(shareOffNetwork.allowed());
    }

    @Test
    void accessPolicy_restrictedOutsideBusinessHoursIsDeniedExceptForAdmins() {
        AccessDecision offHours = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.RESTRICTED, AccessScope.OWN,
                true, false, true, true, true, false, false, false, false, false, true, true, true));
        assertFalse(offHours.allowed());
        assertTrue(offHours.reasons().contains("restricted resource is unavailable outside business hours"));
        assertTrue(offHours.auditRequired());
        AccessDecision adminOffHours = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.RESTRICTED,
                AccessScope.OWN, true, false, true, true, true, false, false, false, false, false,
                true, true, true));
        assertTrue(adminOffHours.allowed());
    }

    @Test
    void accessPolicy_delegationRules() {
        AccessDecision invalid = decide(actx("A", null, UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, true, false, false, false, true, true, true));
        assertFalse(invalid.allowed());
        assertTrue(invalid.reasons().contains("delegation has expired or is invalid"));
        AccessDecision delegatedDelete = decide(actx("A", null, UserRole.FINANCE, ResourceAction.DELETE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, true, true, false, false, true, true, true));
        assertTrue(delegatedDelete.reasons().contains("delegated permission cannot delete resources"));
        AccessDecision delegatedExport = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.EXPORT,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.TEAM,
                true, false, true, true, true, true, true, true, false, false, true, true, true));
        assertTrue(delegatedExport.reasons().contains("delegated permission cannot export resources"));
        AccessDecision delegatedOk = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, true, true, false, false, true, true, true));
        assertTrue(delegatedOk.allowed());
        assertTrue(delegatedOk.auditRequired());
    }

    @Test
    void accessPolicy_legalHoldAndArchiveRules() {
        AccessDecision legalHoldDelete = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.DELETE, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION, true, false, true, true, true, true, false, false, true, false,
                true, true, true));
        assertFalse(legalHoldDelete.allowed());
        assertTrue(legalHoldDelete.reasons().contains("resource is under legal hold"));
        AccessDecision legalHoldArchive = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.ARCHIVE, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION, true, false, true, true, true, true, false, false, true, false,
                true, true, true));
        assertFalse(legalHoldArchive.allowed());
        AccessDecision archivedUpdate = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.UPDATE, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION, true, false, true, true, true, true, false, false, false, true,
                true, true, true));
        assertTrue(archivedUpdate.reasons().contains("archived resource cannot be updated"));
        AccessDecision restoreActive = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.RESTORE, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION, true, false, true, true, true, true, false, false, false, false,
                true, true, true));
        assertTrue(restoreActive.reasons().contains("only archived resources can be restored"));
        AccessDecision restoreArchived = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.RESTORE, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION, true, false, true, true, true, true, false, false, false, true,
                true, true, true));
        assertTrue(restoreArchived.allowed());
    }

    @Test
    void accessPolicy_employeeRoleMatrix() {
        AccessDecision createExpense = decide(actx("A", null, UserRole.EMPLOYEE, ResourceAction.CREATE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN));
        assertTrue(createExpense.allowed());
        assertTrue(createExpense.reasons().contains("employee may create personal requests"));
        AccessDecision createLeave = decide(actx("A", null, UserRole.EMPLOYEE, ResourceAction.CREATE,
                ResourceType.LEAVE_REQUEST, Sensitivity.INTERNAL, AccessScope.OWN));
        assertTrue(createLeave.allowed());
        AccessDecision createOther = decide(actx("A", null, UserRole.EMPLOYEE, ResourceAction.CREATE,
                ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.OWN));
        assertFalse(createOther.allowed());
        assertTrue(createOther.reasons().contains("role does not grant the requested action"));
        AccessDecision ownView = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN));
        assertTrue(ownView.allowed());
        assertTrue(ownView.reasons().contains("employee may access owned resource"));
        AccessDecision ownUpdate = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.UPDATE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN));
        assertTrue(ownUpdate.allowed());
        AccessDecision payrollUpdate = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.UPDATE,
                ResourceType.PAYROLL, Sensitivity.INTERNAL, AccessScope.OWN));
        assertFalse(payrollUpdate.allowed());
        AccessDecision payrollView = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.PAYROLL, Sensitivity.INTERNAL, AccessScope.OWN));
        assertTrue(payrollView.allowed());
        AccessDecision notOwner = decide(actx("A", "B", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN));
        assertFalse(notOwner.allowed());
        assertTrue(notOwner.reasons().contains("requested scope is outside the actor's authority"));
        AccessDecision publicDoc = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.PUBLIC_DOCUMENT, Sensitivity.PUBLIC, AccessScope.OWN));
        assertTrue(publicDoc.allowed());
    }

    @Test
    void accessPolicy_teamLeadRoleMatrix() {
        AccessDecision teamApprove = decide(actx("A", null, UserRole.TEAM_LEAD, ResourceAction.APPROVE,
                ResourceType.LEAVE_REQUEST, Sensitivity.INTERNAL, AccessScope.TEAM));
        assertTrue(teamApprove.allowed());
        assertTrue(teamApprove.reasons().contains("team lead has operational team access"));
        assertTrue(teamApprove.auditRequired());
        AccessDecision teamViewExpense = decide(actx("A", null, UserRole.TEAM_LEAD, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.TEAM));
        assertTrue(teamViewExpense.allowed());
        AccessDecision teamAttendance = decide(actx("A", null, UserRole.TEAM_LEAD, ResourceAction.VIEW,
                ResourceType.ATTENDANCE_RECORD, Sensitivity.INTERNAL, AccessScope.TEAM));
        assertTrue(teamAttendance.allowed());
        AccessDecision teamContract = decide(actx("A", null, UserRole.TEAM_LEAD, ResourceAction.VIEW,
                ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.TEAM));
        assertFalse(teamContract.allowed());
        AccessDecision teamOwnScope = decide(actx("A", null, UserRole.TEAM_LEAD, ResourceAction.APPROVE,
                ResourceType.LEAVE_REQUEST, Sensitivity.INTERNAL, AccessScope.OWN));
        assertFalse(teamOwnScope.allowed());
        AccessDecision teamCreate = decide(actx("A", null, UserRole.TEAM_LEAD, ResourceAction.CREATE,
                ResourceType.LEAVE_REQUEST, Sensitivity.INTERNAL, AccessScope.OWN));
        assertTrue(teamCreate.allowed());
    }

    @Test
    void accessPolicy_departmentManagerRoleMatrix() {
        AccessDecision sameDept = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER, ResourceAction.VIEW,
                ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertTrue(sameDept.allowed());
        assertTrue(sameDept.reasons().contains("department manager has department-scoped access"));
        AccessDecision otherDept = decide(actx("A", null, "D1", "D2", UserRole.DEPARTMENT_MANAGER,
                ResourceAction.VIEW, ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.DEPARTMENT,
                true, false, true, true, true, true, false, false, false, false, true, true, true));
        assertFalse(otherDept.allowed());
        AccessDecision wrongScope = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER, ResourceAction.VIEW,
                ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertFalse(wrongScope.allowed());
        AccessDecision teamScope = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER, ResourceAction.VIEW,
                ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.TEAM));
        assertTrue(teamScope.allowed());
        AccessDecision payroll = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER, ResourceAction.VIEW,
                ResourceType.PAYROLL, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertFalse(payroll.allowed());
        AccessDecision systemConfig = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER,
                ResourceAction.VIEW, ResourceType.SYSTEM_CONFIGURATION, Sensitivity.INTERNAL,
                AccessScope.DEPARTMENT));
        assertFalse(systemConfig.allowed());
        AccessDecision auditLog = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER, ResourceAction.VIEW,
                ResourceType.AUDIT_LOG, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertFalse(auditLog.allowed());
        AccessDecision deletePublic = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER,
                ResourceAction.DELETE, ResourceType.PUBLIC_DOCUMENT, Sensitivity.PUBLIC,
                AccessScope.DEPARTMENT));
        assertTrue(deletePublic.allowed());
        AccessDecision deleteInternal = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER,
                ResourceAction.DELETE, ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertFalse(deleteInternal.allowed());
    }

    @Test
    void accessPolicy_humanResourcesRoleMatrix() {
        AccessDecision profileView = decide(actx("A", null, UserRole.HUMAN_RESOURCES, ResourceAction.VIEW,
                ResourceType.EMPLOYEE_PROFILE, Sensitivity.CONFIDENTIAL, AccessScope.DEPARTMENT));
        assertTrue(profileView.allowed());
        assertTrue(profileView.reasons().contains("human resources role grants workforce-data access"));
        AccessDecision profileDelete = decide(actx("A", null, UserRole.HUMAN_RESOURCES,
                ResourceAction.DELETE, ResourceType.EMPLOYEE_PROFILE, Sensitivity.CONFIDENTIAL,
                AccessScope.DEPARTMENT));
        assertFalse(profileDelete.allowed());
        AccessDecision profileExport = decide(actx("A", null, UserRole.HUMAN_RESOURCES,
                ResourceAction.EXPORT, ResourceType.EMPLOYEE_PROFILE, Sensitivity.CONFIDENTIAL,
                AccessScope.ORGANIZATION, true, false, true, true, true, true, false, false, false, false,
                false, true, true));
        assertFalse(profileExport.allowed());
        AccessDecision profileExportMember = decide(actx("A", null, UserRole.HUMAN_RESOURCES,
                ResourceAction.EXPORT, ResourceType.EMPLOYEE_PROFILE, Sensitivity.CONFIDENTIAL,
                AccessScope.ORGANIZATION, true, false, true, true, true, true, false, false, false, false,
                true, true, true));
        assertTrue(profileExportMember.allowed());
        AccessDecision payrollView = decide(actx("A", null, UserRole.HUMAN_RESOURCES, ResourceAction.VIEW,
                ResourceType.PAYROLL, Sensitivity.CONFIDENTIAL, AccessScope.DEPARTMENT));
        assertTrue(payrollView.allowed());
        assertTrue(payrollView.reasons().contains("human resources may view scoped payroll data"));
        AccessDecision payrollOrg = decide(actx("A", null, UserRole.HUMAN_RESOURCES, ResourceAction.VIEW,
                ResourceType.PAYROLL, Sensitivity.CONFIDENTIAL, AccessScope.ORGANIZATION));
        assertFalse(payrollOrg.allowed());
        AccessDecision publicDoc = decide(actx("A", "A", UserRole.HUMAN_RESOURCES, ResourceAction.VIEW,
                ResourceType.PUBLIC_DOCUMENT, Sensitivity.PUBLIC, AccessScope.OWN));
        assertTrue(publicDoc.allowed());
    }

    @Test
    void accessPolicy_financeRoleMatrix() {
        AccessDecision expenseView = decide(actx("A", null, UserRole.FINANCE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.CONFIDENTIAL, AccessScope.DEPARTMENT));
        assertTrue(expenseView.allowed());
        assertTrue(expenseView.reasons().contains("finance role grants financial-data access"));
        AccessDecision expenseDelete = decide(actx("A", null, UserRole.FINANCE, ResourceAction.DELETE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.CONFIDENTIAL, AccessScope.DEPARTMENT));
        assertFalse(expenseDelete.allowed());
        AccessDecision restrictedShare = decide(actx("A", null, UserRole.FINANCE, ResourceAction.SHARE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.RESTRICTED, AccessScope.DEPARTMENT));
        assertFalse(restrictedShare.allowed());
        AccessDecision contractView = decide(actx("A", null, UserRole.FINANCE, ResourceAction.VIEW,
                ResourceType.CONTRACT, Sensitivity.CONFIDENTIAL, AccessScope.DEPARTMENT));
        assertTrue(contractView.allowed());
        AccessDecision otherResource = decide(actx("A", null, UserRole.FINANCE, ResourceAction.VIEW,
                ResourceType.ATTENDANCE_RECORD, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertFalse(otherResource.allowed());
        AccessDecision publicDoc = decide(actx("A", "A", UserRole.FINANCE, ResourceAction.VIEW,
                ResourceType.PUBLIC_DOCUMENT, Sensitivity.PUBLIC, AccessScope.OWN));
        assertTrue(publicDoc.allowed());
    }

    @Test
    void accessPolicy_auditorRoleMatrix() {
        AccessDecision viewAny = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.VIEW,
                ResourceType.PAYROLL, Sensitivity.CONFIDENTIAL, AccessScope.ORGANIZATION));
        assertTrue(viewAny.allowed());
        assertTrue(viewAny.reasons().contains("auditor receives read-only access"));
        AccessDecision export = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.EXPORT,
                ResourceType.EXPENSE_CLAIM, Sensitivity.CONFIDENTIAL, AccessScope.ORGANIZATION));
        assertTrue(export.allowed());
        AccessDecision exportConfig = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.EXPORT,
                ResourceType.SYSTEM_CONFIGURATION, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertFalse(exportConfig.allowed());
        AccessDecision update = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.UPDATE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertFalse(update.allowed());
    }

    @Test
    void accessPolicy_administratorRoleMatrix() {
        AccessDecision configUpdate = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.UPDATE, ResourceType.SYSTEM_CONFIGURATION, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION));
        assertTrue(configUpdate.allowed());
        assertTrue(configUpdate.auditRequired());
        AccessDecision payrollView = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.VIEW, ResourceType.PAYROLL, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertFalse(payrollView.allowed());
        AccessDecision profileExport = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.EXPORT, ResourceType.EMPLOYEE_PROFILE, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION));
        assertFalse(profileExport.allowed());
        AccessDecision profileApprove = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.APPROVE, ResourceType.EMPLOYEE_PROFILE, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION));
        assertFalse(profileApprove.allowed());
        AccessDecision legalHoldDelete = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.DELETE, ResourceType.CONTRACT, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION, true, false, true, true, true, true, false, false, true, false,
                true, true, true));
        assertFalse(legalHoldDelete.allowed());
        AccessDecision ordinaryDelete = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.DELETE, ResourceType.CONTRACT, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION));
        assertTrue(ordinaryDelete.allowed());
        assertTrue(ordinaryDelete.auditRequired());
    }

    @Test
    void accessPolicy_scopeRules() {
        AccessDecision ownWithoutOwnership = decide(actx("A", "B", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN));
        assertFalse(ownWithoutOwnership.allowed());
        AccessDecision ownCreate = decide(actx("A", null, UserRole.EMPLOYEE, ResourceAction.CREATE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN));
        assertTrue(ownCreate.allowed());
        AccessDecision ownAdmin = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.VIEW, ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.OWN));
        assertTrue(ownAdmin.allowed());
        AccessDecision teamScopeEmployee = decide(actx("A", null, UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.TEAM));
        assertFalse(teamScopeEmployee.allowed());
        AccessDecision departmentScopeFinance = decide(actx("A", null, UserRole.FINANCE,
                ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                AccessScope.DEPARTMENT));
        assertTrue(departmentScopeFinance.allowed());
        AccessDecision departmentOtherDeptManager = decide(actx("A", null, "D1", "D2",
                UserRole.DEPARTMENT_MANAGER, ResourceAction.VIEW, ResourceType.CONTRACT,
                Sensitivity.INTERNAL, AccessScope.DEPARTMENT,
                true, false, true, true, true, true, false, false, false, false, true, true, true));
        assertFalse(departmentOtherDeptManager.allowed());
        AccessDecision organizationHr = decide(actx("A", null, UserRole.HUMAN_RESOURCES,
                ResourceAction.VIEW, ResourceType.LEAVE_REQUEST, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION));
        assertTrue(organizationHr.allowed());
        assertTrue(organizationHr.reasons().contains("organization scope granted to specialist role"));
        AccessDecision organizationEmployee = decide(actx("A", null, UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertFalse(organizationEmployee.allowed());
    }

    @Test
    void accessPolicy_resourceSpecificRules() {
        AccessDecision auditImmutable = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.DELETE, ResourceType.AUDIT_LOG, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION));
        assertFalse(auditImmutable.allowed());
        assertTrue(auditImmutable.reasons().contains("audit logs are immutable"));
        assertTrue(auditImmutable.auditRequired());
        AccessDecision auditExport = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.EXPORT,
                ResourceType.AUDIT_LOG, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertTrue(auditExport.allowed());
        assertTrue(auditExport.auditRequired());
        AccessDecision configByNonAdmin = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.VIEW,
                ResourceType.SYSTEM_CONFIGURATION, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertTrue(configByNonAdmin.reasons().contains("system configuration requires administrator role"));
        AccessDecision publicPayroll = decide(actx("A", null, UserRole.FINANCE, ResourceAction.VIEW,
                ResourceType.PAYROLL, Sensitivity.PUBLIC, AccessScope.DEPARTMENT));
        assertTrue(publicPayroll.reasons().contains("payroll cannot be classified as public"));
        AccessDecision shareProfile = decide(actx("A", null, UserRole.FINANCE, ResourceAction.SHARE,
                ResourceType.EMPLOYEE_PROFILE, Sensitivity.INTERNAL, AccessScope.DEPARTMENT,
                true, false, true, true, true, true, false, false, false, false, false, true, true));
        assertTrue(shareProfile.reasons().contains("profile sharing requires project membership"));
        AccessDecision contractApproveFinance = decide(actx("A", null, UserRole.FINANCE,
                ResourceAction.APPROVE, ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertTrue(contractApproveFinance.allowed());
        AccessDecision contractApproveOther = decide(actx("A", null, UserRole.AUDITOR,
                ResourceAction.APPROVE, ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertFalse(contractApproveOther.allowed());
        assertTrue(contractApproveOther.reasons()
                .contains("contract approval requires finance or department manager"));
    }

    @Test
    void accessPolicy_grantedFallbackReasonIsProduced() {
        AccessDecision decision = decide(actx(UserRole.SYSTEM_ADMINISTRATOR, ResourceAction.UPDATE,
                ResourceType.CONTRACT, AccessScope.ORGANIZATION, Sensitivity.INTERNAL));
        assertTrue(decision.allowed());
        assertTrue(decision.reasons().contains("system administrator has technical administration access"));
        assertTrue(decision.reasons().contains("organization scope granted to specialist role"));
    }

    // __WORKFLOW_RECORDS__

    // ==================== ApprovalStep ====================

    private static ApprovalStep step(String stepId, Set<String> approvers, StepMode mode,
            int requiredApprovals, boolean delegationAllowed, boolean skipRequester, Instant deadline) {
        return new ApprovalStep(stepId, "Name", approvers, mode, requiredApprovals, delegationAllowed,
                skipRequester, deadline);
    }

    @Test
    void approvalStep_validatesConstruction() {
        assertThrows(IllegalArgumentException.class, () -> step(null, Set.of("A"), StepMode.ANY_ONE, 1,
                false, false, null));
        assertThrows(IllegalArgumentException.class, () -> step(" ", Set.of("A"), StepMode.ANY_ONE, 1,
                false, false, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ApprovalStep("s", null, Set.of("A"), StepMode.ANY_ONE, 1, false, false, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ApprovalStep("s", " ", Set.of("A"), StepMode.ANY_ONE, 1, false, false, null));
        assertThrows(NullPointerException.class, () -> step("s", null, StepMode.ANY_ONE, 1, false, false, null));
        assertThrows(IllegalArgumentException.class, () -> step("s", Set.of(), StepMode.ANY_ONE, 1,
                false, false, null));
        Set<String> withNullElement = new LinkedHashSet<>();
        withNullElement.add("A");
        withNullElement.add(null);
        assertThrows(IllegalArgumentException.class, () -> step("s", withNullElement, StepMode.ANY_ONE, 1,
                false, false, null));
        assertThrows(IllegalArgumentException.class, () -> step("s", Set.of("A", " "), StepMode.ANY_ONE, 1,
                false, false, null));
        assertThrows(NullPointerException.class, () -> step("s", Set.of("A"), null, 1, false, false, null));
        assertThrows(IllegalArgumentException.class, () -> step("s", Set.of("A", "B"), StepMode.REQUIRED_COUNT,
                0, false, false, null));
        assertThrows(IllegalArgumentException.class, () -> step("s", Set.of("A", "B"), StepMode.REQUIRED_COUNT,
                3, false, false, null));
        assertThrows(IllegalArgumentException.class, () -> step("s", Set.of("A", "B"), StepMode.ANY_ONE, 2,
                false, false, null));
        assertThrows(IllegalArgumentException.class, () -> step("s", Set.of("A", "B"), StepMode.UNANIMOUS, 1,
                false, false, null));
        assertThrows(IllegalArgumentException.class, () -> step("s", Set.of("A", "B", "C"), StepMode.MAJORITY, 1,
                false, false, null));
        ApprovalStep trimmed = new ApprovalStep(" s ", " n ", new LinkedHashSet<>(List.of(" A ", " B ")),
                StepMode.REQUIRED_COUNT, 1, false, false, null);
        assertEquals("s", trimmed.stepId());
        assertEquals("n", trimmed.name());
        assertEquals(Set.of("A", "B"), trimmed.approverIds());
        ApprovalStep unanimous = step("s", Set.of("A", "B"), StepMode.UNANIMOUS, 2, false, false, null);
        assertEquals(2, unanimous.requiredApprovals());
        ApprovalStep majority = step("s", Set.of("A", "B", "C"), StepMode.MAJORITY, 2, false, false, null);
        assertEquals(2, majority.requiredApprovals());
    }

    @Test
    void approvalStep_canApproveTrimsAndChecksMembership() {
        ApprovalStep step = step("s", Set.of("A", "B"), StepMode.ANY_ONE, 1, false, false, null);
        assertTrue(step.canApprove("A"));
        assertTrue(step.canApprove(" A "));
        assertFalse(step.canApprove("C"));
        assertFalse(step.canApprove(null));
    }

    @Test
    void approvalStep_delegatesWithValidation() {
        ApprovalStep noDelegation = step("s", Set.of("A", "B"), StepMode.ANY_ONE, 1, false, false, null);
        assertThrows(IllegalStateException.class, () -> noDelegation.delegate("A", "C"));
        ApprovalStep step = step("s", Set.of("A", "B"), StepMode.REQUIRED_COUNT, 2, true, true,
                Instant.parse("2026-07-01T00:00:00Z"));
        assertThrows(IllegalArgumentException.class, () -> step.delegate("X", "C"));
        assertThrows(IllegalArgumentException.class, () -> step.delegate(null, "C"));
        assertThrows(IllegalArgumentException.class, () -> step.delegate("A", null));
        assertThrows(IllegalArgumentException.class, () -> step.delegate("A", " "));
        assertThrows(IllegalArgumentException.class, () -> step.delegate("A", "A"));
        assertThrows(IllegalArgumentException.class, () -> step.delegate("A", "B"));
        ApprovalStep delegated = step.delegate("A", "C");
        assertEquals(Set.of("C", "B"), delegated.approverIds());
        assertEquals("s", delegated.stepId());
        assertEquals("Name", delegated.name());
        assertEquals(StepMode.REQUIRED_COUNT, delegated.mode());
        assertEquals(2, delegated.requiredApprovals());
        assertTrue(delegated.delegationAllowed());
        assertTrue(delegated.skipRequester());
        assertEquals(Instant.parse("2026-07-01T00:00:00Z"), delegated.deadline());
        assertEquals(Set.of("A", "B"), step.approverIds());
    }

    // ==================== WorkflowAction ====================

    @Test
    void workflowAction_validatesActorDecisionAndComment() {
        Instant now = Instant.parse("2026-06-15T08:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> new WorkflowAction(null, ApprovalDecision.APPROVE,
                null, now));
        assertThrows(IllegalArgumentException.class, () -> new WorkflowAction(" ", ApprovalDecision.APPROVE,
                null, now));
        assertThrows(NullPointerException.class, () -> new WorkflowAction("A", null, null, now));
        assertThrows(NullPointerException.class, () -> new WorkflowAction("A", ApprovalDecision.APPROVE, null,
                null));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowAction("A", ApprovalDecision.REJECT, null, now));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowAction("A", ApprovalDecision.REJECT, "   ", now));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowAction("A", ApprovalDecision.RETURN, null, now));
        WorkflowAction ok = new WorkflowAction(" A ", ApprovalDecision.REJECT, "  no good  ", now);
        assertEquals("A", ok.actorId());
        assertEquals("no good", ok.comment());
        WorkflowAction approveNoComment = new WorkflowAction("A", ApprovalDecision.APPROVE, null, now);
        assertNull(approveNoComment.comment());
    }

    // ==================== StepProgress ====================

    @Test
    void stepProgress_validatesDecisionActors() {
        ApprovalStep step = step("s", Set.of("A", "B"), StepMode.REQUIRED_COUNT, 1, false, false, null);
        assertThrows(IllegalArgumentException.class,
                () -> new StepProgress(step, Map.of("X", ApprovalDecision.APPROVE)));
        StepProgress empty = StepProgress.empty(step);
        assertEquals(Map.of(), empty.decisions());
        assertEquals(step, empty.step());
        assertThrows(IllegalArgumentException.class, () -> empty.decide(null, ApprovalDecision.APPROVE));
        assertThrows(IllegalArgumentException.class, () -> empty.decide("  ", ApprovalDecision.APPROVE));
        assertThrows(IllegalArgumentException.class, () -> empty.decide("X", ApprovalDecision.APPROVE));
        assertThrows(NullPointerException.class, () -> empty.decide("A", null));
        StepProgress decided = empty.decide(" A ", ApprovalDecision.APPROVE);
        assertTrue(decided.decisions().containsKey("A"));
        assertEquals(Map.of(), empty.decisions());
        assertThrows(IllegalStateException.class, () -> decided.decide("A", ApprovalDecision.APPROVE));
        assertThrows(UnsupportedOperationException.class,
                () -> decided.decisions().put("B", ApprovalDecision.APPROVE));
    }

    @Test
    void stepProgress_delegationPreservesUndecidedStateOnly() {
        ApprovalStep step = step("s", Set.of("A", "B"), StepMode.REQUIRED_COUNT, 2, true, false, null);
        StepProgress empty = StepProgress.empty(step);
        ApprovalStep delegatedStep = step.delegate("A", "C");
        StepProgress delegated = empty.withDelegatedStep(delegatedStep, "A");
        assertEquals(delegatedStep, delegated.step());
        StepProgress decided = empty.decide("A", ApprovalDecision.APPROVE);
        assertThrows(IllegalStateException.class,
                () -> decided.withDelegatedStep(delegatedStep, "A"));
    }

    @Test
    void stepProgress_countsDecisionsByKind() {
        ApprovalStep step = step("s", Set.of("A", "B", "C"), StepMode.REQUIRED_COUNT, 2, false, false, null);
        StepProgress progress = StepProgress.empty(step)
                .decide("A", ApprovalDecision.APPROVE)
                .decide("B", ApprovalDecision.REJECT)
                .decide("C", ApprovalDecision.ABSTAIN);
        assertEquals(1, progress.approvals());
        assertEquals(1, progress.rejections());
        assertEquals(1, progress.abstentions());
    }

    @Test
    void stepProgress_satisfiedAndCanStillSucceedFollowModes() {
        ApprovalStep anyOne = step("s", Set.of("A", "B"), StepMode.ANY_ONE, 1, false, false, null);
        assertFalse(StepProgress.empty(anyOne).satisfied());
        assertTrue(StepProgress.empty(anyOne).decide("A", ApprovalDecision.APPROVE).satisfied());
        assertFalse(StepProgress.empty(anyOne).decide("A", ApprovalDecision.ABSTAIN).satisfied());
        ApprovalStep required = step("s", Set.of("A", "B", "C"), StepMode.REQUIRED_COUNT, 2, false, false, null);
        assertFalse(StepProgress.empty(required)
                .decide("A", ApprovalDecision.APPROVE).satisfied());
        assertTrue(StepProgress.empty(required)
                .decide("A", ApprovalDecision.APPROVE).decide("B", ApprovalDecision.APPROVE).satisfied());
        ApprovalStep unanimous = step("s", Set.of("A", "B"), StepMode.UNANIMOUS, 2, false, false, null);
        assertFalse(StepProgress.empty(unanimous).decide("A", ApprovalDecision.APPROVE).satisfied());
        assertTrue(StepProgress.empty(unanimous)
                .decide("A", ApprovalDecision.APPROVE).decide("B", ApprovalDecision.APPROVE).satisfied());
        ApprovalStep majority = step("s", Set.of("A", "B", "C"), StepMode.MAJORITY, 2, false, false, null);
        assertTrue(StepProgress.empty(majority)
                .decide("A", ApprovalDecision.APPROVE).decide("B", ApprovalDecision.APPROVE).satisfied());
        StepProgress cannot = StepProgress.empty(required).decide("A", ApprovalDecision.REJECT)
                .decide("B", ApprovalDecision.REJECT);
        assertFalse(cannot.canStillSucceed());
        StepProgress can = StepProgress.empty(required).decide("A", ApprovalDecision.REJECT);
        assertTrue(can.canStillSucceed());
    }

    // ==================== WorkflowSnapshot ====================

    private static WorkflowSnapshot snapshot(WorkflowState state, List<StepProgress> steps, int index,
            Instant createdAt, Instant updatedAt, long version, boolean urgent, boolean withdrawalAllowed) {
        return new WorkflowSnapshot("W1", "R1", state, steps, index, createdAt, updatedAt, version, urgent,
                withdrawalAllowed);
    }

    @Test
    void workflowSnapshot_validatesConstruction() {
        Instant t0 = Instant.parse("2026-06-15T08:00:00Z");
        Instant t1 = Instant.parse("2026-06-15T09:00:00Z");
        List<StepProgress> steps = List.of(StepProgress.empty(step("s", Set.of("A"), StepMode.ANY_ONE, 1,
                false, false, null)));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot(null, "R", WorkflowState.PENDING, steps, 0, t0, t0, 1, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot("W", null, WorkflowState.PENDING, steps, 0, t0, t0, 1, false, true));
        assertThrows(NullPointerException.class,
                () -> new WorkflowSnapshot("W", "R", null, steps, 0, t0, t0, 1, false, true));
        assertThrows(NullPointerException.class,
                () -> new WorkflowSnapshot("W", "R", WorkflowState.PENDING, null, 0, t0, t0, 1, false, true));
        assertThrows(NullPointerException.class,
                () -> new WorkflowSnapshot("W", "R", WorkflowState.PENDING, steps, 0, null, t0, 1, false, true));
        assertThrows(NullPointerException.class,
                () -> new WorkflowSnapshot("W", "R", WorkflowState.PENDING, steps, 0, t0, null, 1, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot("W", "R", WorkflowState.PENDING, List.of(), 0, t0, t0, 1, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot("W", "R", WorkflowState.PENDING, steps, 0, t1, t0, 1, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot("W", "R", WorkflowState.PENDING, steps, 0, t0, t0, 0, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot("W", "R", WorkflowState.PENDING, steps, -1, t0, t0, 1, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot("W", "R", WorkflowState.PENDING, steps, 1, t0, t0, 1, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot("W", "R", WorkflowState.APPROVED, steps, -1, t0, t0, 1, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot("W", "R", WorkflowState.APPROVED, steps, 2, t0, t0, 1, false, true));
        WorkflowSnapshot terminal = snapshot(WorkflowState.APPROVED, steps, 1, t0, t0, 1, false, true);
        assertTrue(terminal.terminal());
        assertThrows(IllegalStateException.class, terminal::currentStep);
        WorkflowSnapshot pending = snapshot(WorkflowState.PENDING, steps, 0, t0, t0, 1, false, true);
        assertFalse(pending.terminal());
        assertEquals(steps.get(0), pending.currentStep());
        for (WorkflowState state : WorkflowState.values()) {
            int index = state == WorkflowState.PENDING || state == WorkflowState.IN_PROGRESS ? 0 : 1;
            boolean active = state == WorkflowState.PENDING || state == WorkflowState.IN_PROGRESS;
            assertEquals(!active, snapshot(state, steps, index, t0, t0, 1, false, true).terminal(),
                    "state=" + state);
        }
    }

    // __WORKFLOW_ENGINE__

    // ==================== ApprovalWorkflowEngine ====================

    private static final ApprovalWorkflowEngine ENGINE = new ApprovalWorkflowEngine();
    private static final Instant T0 = Instant.parse("2026-06-15T08:00:00Z");
    private static final Instant T1 = Instant.parse("2026-06-15T09:00:00Z");
    private static final Instant T2 = Instant.parse("2026-06-15T10:00:00Z");

    @Test
    void workflowEngine_startValidatesInputs() {
        assertThrows(NullPointerException.class, () -> ENGINE.start("W", "R", null, T0, false, true));
        assertThrows(NullPointerException.class, () -> ENGINE.start("W", "R", List.of(), null, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> ENGINE.start("W", "R", List.of(), T0, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> ENGINE.start("W", null, List.of(step("s", Set.of("A"), StepMode.ANY_ONE, 1, false, false,
                        null)), T0, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> ENGINE.start("W", " ", List.of(step("s", Set.of("A"), StepMode.ANY_ONE, 1, false, false,
                        null)), T0, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> ENGINE.start("W", "R", List.of(
                        step("s", Set.of("A"), StepMode.ANY_ONE, 1, false, false, null),
                        step("s", Set.of("B"), StepMode.ANY_ONE, 1, false, false, null)), T0, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> ENGINE.start("W", "R", List.of(step("s", Set.of("A"), StepMode.ANY_ONE, 1, false, false,
                        T0)), T0, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> ENGINE.start("W", "R", List.of(step("s", Set.of("A"), StepMode.ANY_ONE, 1, false, false,
                        T0.minusSeconds(1))), T0, false, true));
    }

    @Test
    void workflowEngine_startSkipsRequesterOnlyStepsOrStaysPending() {
        ApprovalStep requesterOnly = step("s", Set.of("R1"), StepMode.ANY_ONE, 1, false, true, null);
        WorkflowSnapshot approved = ENGINE.start("W1", "R1", List.of(requesterOnly), T0, true, true);
        assertEquals(WorkflowState.APPROVED, approved.state());
        assertEquals(1, approved.currentStepIndex());
        assertEquals(1, approved.version());
        assertEquals(T0, approved.createdAt());
        assertEquals(T0, approved.updatedAt());
        assertTrue(approved.urgent());
        assertTrue(approved.withdrawalAllowed());
        assertTrue(approved.terminal());
        ApprovalStep multiApprover = step("s", Set.of("R1", "A"), StepMode.ANY_ONE, 1, false, true, null);
        WorkflowSnapshot pending = ENGINE.start("W1", "R1", List.of(multiApprover), T0, false, false);
        assertEquals(WorkflowState.PENDING, pending.state());
        assertEquals(0, pending.currentStepIndex());
        assertEquals("W1", pending.workflowId());
        assertEquals("R1", pending.requesterId());
        WorkflowSnapshot secondPending = ENGINE.start("W1", "R1", List.of(
                step("s1", Set.of("A"), StepMode.ANY_ONE, 1, false, false, null),
                requesterOnly), T0, false, false);
        assertEquals(WorkflowState.PENDING, secondPending.state());
        assertEquals(0, secondPending.currentStepIndex());
    }

    @Test
    void workflowEngine_actValidatesInputsAndTerminalState() {
        WorkflowSnapshot pending = ENGINE.start("W1", "R1", List.of(step("s", Set.of("A"), StepMode.ANY_ONE, 1,
                false, false, null)), T0, false, true);
        assertThrows(NullPointerException.class, () -> ENGINE.act(null, new WorkflowAction("A",
                ApprovalDecision.APPROVE, null, T1)));
        assertThrows(NullPointerException.class, () -> ENGINE.act(pending, null));
        WorkflowSnapshot approved = ENGINE.start("W1", "R1", List.of(step("s", Set.of("R1"),
                StepMode.ANY_ONE, 1, false, true, null)), T0, false, true);
        assertThrows(IllegalStateException.class, () -> ENGINE.act(approved, new WorkflowAction("A",
                ApprovalDecision.APPROVE, null, T1)));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.act(pending, new WorkflowAction("A",
                ApprovalDecision.APPROVE, null, T0.minusSeconds(1))));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.act(pending, new WorkflowAction("X",
                ApprovalDecision.APPROVE, null, T1)));
    }

    @Test
    void workflowEngine_actEnforcesDeadlinesAndRequesterSkip() {
        WorkflowSnapshot withDeadline = ENGINE.start("W1", "R1", List.of(step("s", Set.of("A"),
                StepMode.ANY_ONE, 1, false, false, T1)), T0, false, true);
        WorkflowSnapshot expired = ENGINE.act(withDeadline, new WorkflowAction("A",
                ApprovalDecision.APPROVE, null, T1.plusSeconds(1)));
        assertEquals(WorkflowState.EXPIRED, expired.state());
        assertTrue(expired.terminal());
        WorkflowSnapshot beforeDeadline = ENGINE.act(withDeadline, new WorkflowAction("A",
                ApprovalDecision.APPROVE, null, T1));
        assertEquals(WorkflowState.APPROVED, beforeDeadline.state());
        WorkflowSnapshot requesterSkipped = ENGINE.start("W1", "R1", List.of(step("s", Set.of("R1", "A"),
                StepMode.ANY_ONE, 1, false, true, null)), T0, false, true);
        assertThrows(IllegalArgumentException.class, () -> ENGINE.act(requesterSkipped,
                new WorkflowAction("R1", ApprovalDecision.APPROVE, null, T1)));
    }

    @Test
    void workflowEngine_actRejectsAndReturns() {
        WorkflowSnapshot pending = ENGINE.start("W1", "R1", List.of(step("s", Set.of("A"), StepMode.ANY_ONE, 1,
                false, false, null)), T0, false, true);
        WorkflowSnapshot rejected = ENGINE.act(pending, new WorkflowAction("A", ApprovalDecision.REJECT,
                "bad receipt", T1));
        assertEquals(WorkflowState.REJECTED, rejected.state());
        assertEquals(2, rejected.version());
        WorkflowSnapshot returned = ENGINE.act(pending, new WorkflowAction("A", ApprovalDecision.RETURN,
                "fix it", T1));
        assertEquals(WorkflowState.RETURNED, returned.state());
        assertEquals(WorkflowState.PENDING, pending.state());
    }

    @Test
    void workflowEngine_actCountsVotesUntilSatisfied() {
        ApprovalStep required = step("s", Set.of("A", "B"), StepMode.REQUIRED_COUNT, 2, false, false, null);
        WorkflowSnapshot pending = ENGINE.start("W1", "R1", List.of(required), T0, false, true);
        WorkflowSnapshot inProgress = ENGINE.act(pending, new WorkflowAction("A",
                ApprovalDecision.APPROVE, null, T1));
        assertEquals(WorkflowState.IN_PROGRESS, inProgress.state());
        assertEquals(0, inProgress.currentStepIndex());
        assertEquals(1, inProgress.currentStep().approvals());
        assertEquals(2, inProgress.version());
        assertThrows(IllegalStateException.class, () -> ENGINE.act(inProgress, new WorkflowAction("A",
                ApprovalDecision.APPROVE, null, T1.plusSeconds(1))));
        WorkflowSnapshot finished = ENGINE.act(inProgress, new WorkflowAction("B",
                ApprovalDecision.APPROVE, null, T1.plusSeconds(1)));
        assertEquals(WorkflowState.APPROVED, finished.state());
        assertEquals(1, finished.currentStepIndex());
        assertTrue(finished.terminal());
    }

    @Test
    void workflowEngine_actAbstainMayStillFailUnanimousSteps() {
        ApprovalStep unanimous = step("s", Set.of("A", "B"), StepMode.UNANIMOUS, 2, false, false, null);
        WorkflowSnapshot pending = ENGINE.start("W1", "R1", List.of(unanimous), T0, false, true);
        WorkflowSnapshot abstained = ENGINE.act(pending, new WorkflowAction("A", ApprovalDecision.ABSTAIN,
                null, T1));
        assertEquals(WorkflowState.REJECTED, abstained.state());
        ApprovalStep majority = step("s", Set.of("A", "B", "C"), StepMode.MAJORITY, 2, false, false, null);
        WorkflowSnapshot majorityPending = ENGINE.start("W1", "R1", List.of(majority), T0, false, true);
        WorkflowSnapshot stillRunning = ENGINE.act(majorityPending, new WorkflowAction("A",
                ApprovalDecision.ABSTAIN, null, T1));
        assertEquals(WorkflowState.IN_PROGRESS, stillRunning.state());
        WorkflowSnapshot majorityFail = ENGINE.act(stillRunning, new WorkflowAction("B",
                ApprovalDecision.REJECT, "no", T1.plusSeconds(1)));
        assertEquals(WorkflowState.REJECTED, majorityFail.state());
        WorkflowSnapshot majorityThird = ENGINE.act(stillRunning, new WorkflowAction("B",
                ApprovalDecision.ABSTAIN, null, T1.plusSeconds(1)));
        assertEquals(WorkflowState.REJECTED, majorityThird.state());
    }

    @Test
    void workflowEngine_actAdvancesThroughSequentialSteps() {
        ApprovalStep first = step("s1", Set.of("A"), StepMode.ANY_ONE, 1, false, false, null);
        ApprovalStep second = step("s2", Set.of("B"), StepMode.ANY_ONE, 1, false, false, null);
        WorkflowSnapshot started = ENGINE.start("W1", "R1", List.of(first, second), T0, false, true);
        WorkflowSnapshot afterFirst = ENGINE.act(started, new WorkflowAction("A",
                ApprovalDecision.APPROVE, null, T1));
        assertEquals(WorkflowState.PENDING, started.state());
        assertEquals(WorkflowState.IN_PROGRESS, afterFirst.state());
        assertEquals(1, afterFirst.currentStepIndex());
        assertEquals("s2", afterFirst.currentStep().step().stepId());
        WorkflowSnapshot afterSecond = ENGINE.act(afterFirst, new WorkflowAction("B",
                ApprovalDecision.APPROVE, null, T1.plusSeconds(1)));
        assertEquals(WorkflowState.APPROVED, afterSecond.state());
        assertEquals(2, afterSecond.currentStepIndex());
    }

    @Test
    void workflowEngine_actSkipsRequesterOnlyLaterSteps() {
        ApprovalStep first = step("s1", Set.of("A"), StepMode.ANY_ONE, 1, false, false, null);
        ApprovalStep requesterOnly = step("s2", Set.of("R1"), StepMode.ANY_ONE, 1, false, true, null);
        WorkflowSnapshot started = ENGINE.start("W1", "R1", List.of(first, requesterOnly), T0, false, true);
        WorkflowSnapshot afterFirst = ENGINE.act(started, new WorkflowAction("A",
                ApprovalDecision.APPROVE, null, T1));
        assertEquals(WorkflowState.APPROVED, afterFirst.state());
        assertEquals(2, afterFirst.currentStepIndex());
    }

    @Test
    void workflowEngine_delegateValidatesAndUpdatesStep() {
        ApprovalStep step = step("s", Set.of("A", "B"), StepMode.REQUIRED_COUNT, 2, true, false, null);
        WorkflowSnapshot pending = ENGINE.start("W1", "R1", List.of(step), T0, false, true);
        assertThrows(NullPointerException.class, () -> ENGINE.delegate(null, "A", "C", T1));
        assertThrows(NullPointerException.class, () -> ENGINE.delegate(pending, "A", "C", null));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.delegate(pending, " ", "C", T1));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.delegate(pending, "A", " ", T1));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.delegate(pending, "A", "C",
                T0.minusSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.delegate(pending, "X", "C", T1));
        WorkflowSnapshot requesterApprover = ENGINE.start("W1", "R1", List.of(step("s", Set.of("R1", "A"),
                StepMode.ANY_ONE, 1, true, true, null)), T0, false, true);
        assertThrows(IllegalArgumentException.class,
                () -> ENGINE.delegate(requesterApprover, "R1", "R1", T1));
        WorkflowSnapshot delegated = ENGINE.delegate(pending, "A", "C", T1);
        assertEquals(WorkflowState.IN_PROGRESS, delegated.state());
        assertEquals(Set.of("C", "B"), delegated.currentStep().step().approverIds());
        assertEquals(2, delegated.version());
        assertThrows(IllegalArgumentException.class,
                () -> ENGINE.delegate(delegated, "A", "D", T1.plusSeconds(1)));
        WorkflowSnapshot decided = ENGINE.act(pending, new WorkflowAction("A", ApprovalDecision.APPROVE, null, T1));
        assertThrows(IllegalStateException.class,
                () -> ENGINE.delegate(decided, "A", "D", T1.plusSeconds(1)));
    }

    @Test
    void workflowEngine_withdrawValidatesAndTransitions() {
        ApprovalStep step = step("s", Set.of("A"), StepMode.ANY_ONE, 1, false, false, null);
        WorkflowSnapshot pending = ENGINE.start("W1", "R1", List.of(step), T0, false, true);
        WorkflowSnapshot noWithdrawal = ENGINE.start("W1", "R1", List.of(step), T0, false, false);
        assertThrows(IllegalStateException.class, () -> ENGINE.withdraw(noWithdrawal, "R1", T1));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.withdraw(pending, "X", T1));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.withdraw(pending, null, T1));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.withdraw(pending, "R1", T0.minusSeconds(1)));
        assertThrows(NullPointerException.class, () -> ENGINE.withdraw(null, "R1", T1));
        WorkflowSnapshot withdrawn = ENGINE.withdraw(pending, "R1", T1);
        assertEquals(WorkflowState.WITHDRAWN, withdrawn.state());
        assertTrue(withdrawn.terminal());
        WorkflowSnapshot urgent = ENGINE.start("W1", "R1", List.of(
                step("s1", Set.of("A"), StepMode.ANY_ONE, 1, false, false, null),
                step("s2", Set.of("B"), StepMode.ANY_ONE, 1, false, false, null)), T0, true, true);
        WorkflowSnapshot urgentAfterFirst = ENGINE.act(urgent, new WorkflowAction("A",
                ApprovalDecision.APPROVE, null, T1));
        assertThrows(IllegalStateException.class,
                () -> ENGINE.withdraw(urgentAfterFirst, "R1", T1.plusSeconds(1)));
        WorkflowSnapshot urgentWithdrawn = ENGINE.withdraw(urgent, "R1", T1);
        assertEquals(WorkflowState.WITHDRAWN, urgentWithdrawn.state());
    }

    @Test
    void workflowEngine_cancelValidatesAndTransitions() {
        ApprovalStep step = step("s", Set.of("A"), StepMode.ANY_ONE, 1, false, false, null);
        WorkflowSnapshot pending = ENGINE.start("W1", "R1", List.of(step), T0, false, true);
        WorkflowSnapshot approved = ENGINE.start("W1", "R1", List.of(step("s", Set.of("R1"),
                StepMode.ANY_ONE, 1, false, true, null)), T0, false, true);
        assertThrows(IllegalStateException.class, () -> ENGINE.cancel(approved, true, "reason", T1));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.cancel(pending, false, "reason", T1));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.cancel(pending, true, null, T1));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.cancel(pending, true, "abc", T1));
        assertThrows(IllegalArgumentException.class, () -> ENGINE.cancel(pending, true, "reason",
                T0.minusSeconds(1)));
        assertThrows(NullPointerException.class, () -> ENGINE.cancel(null, true, "reason", T1));
        WorkflowSnapshot cancelled = ENGINE.cancel(pending, true, "reason", T1);
        assertEquals(WorkflowState.CANCELLED, cancelled.state());
        assertTrue(cancelled.terminal());
    }

    @Test
    void workflowEngine_expireIfNecessary() {
        WorkflowSnapshot noDeadline = ENGINE.start("W1", "R1", List.of(step("s", Set.of("A"),
                StepMode.ANY_ONE, 1, false, false, null)), T0, false, true);
        assertSame(noDeadline, ENGINE.expireIfNecessary(noDeadline, T2));
        WorkflowSnapshot withDeadline = ENGINE.start("W1", "R1", List.of(step("s", Set.of("A"),
                StepMode.ANY_ONE, 1, false, false, T1)), T0, false, true);
        assertSame(withDeadline, ENGINE.expireIfNecessary(withDeadline, T1));
        WorkflowSnapshot expired = ENGINE.expireIfNecessary(withDeadline, T1.plusSeconds(1));
        assertEquals(WorkflowState.EXPIRED, expired.state());
        assertTrue(expired.terminal());
        assertSame(expired, ENGINE.expireIfNecessary(expired, T2));
        assertThrows(NullPointerException.class, () -> ENGINE.expireIfNecessary(null, T2));
        assertThrows(NullPointerException.class, () -> ENGINE.expireIfNecessary(withDeadline, null));
    }

    // ==================== supplementary branch coverage ====================

    @Test
    void expensePolicy_gradeMultipliersRaiseCategoryCaps() {
        ExpenseAssessment senior = assess(claim("E1", EmployeeGrade.SENIOR, TODAY, List.of(
                customItem("i1", ExpenseCategory.MEAL, "240", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, false, false, true, 0));
        assertEquals(new BigDecimal("240.00"), senior.reimbursableAmount());
        ExpenseAssessment manager = assess(claim("E1", EmployeeGrade.MANAGER, TODAY, List.of(
                customItem("i1", ExpenseCategory.MEAL, "320", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, false, false, true, 0));
        assertEquals(new BigDecimal("320.00"), manager.reimbursableAmount());
        ExpenseAssessment managerCapped = assess(claim("E1", EmployeeGrade.MANAGER, TODAY, List.of(
                customItem("i1", ExpenseCategory.MEAL, "320.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, false, false, true, 0));
        assertEquals(new BigDecimal("320.00"), managerCapped.reimbursableAmount());
        ExpenseAssessment director = assess(claim("E1", EmployeeGrade.DIRECTOR, TODAY, List.of(
                customItem("i1", ExpenseCategory.MEAL, "480", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, false, false, true, 0));
        assertEquals(new BigDecimal("480.00"), director.reimbursableAmount());
    }

    @Test
    void expensePolicy_internationalTripRaisesHotelAndTransportCapsToo() {
        ExpenseAssessment hotel = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(
                customItem("i1", ExpenseCategory.HOTEL, "1500", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, true, false, true, 0));
        assertEquals(new BigDecimal("1500.00"), hotel.reimbursableAmount());
        ExpenseAssessment transport = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(
                customItem("i1", ExpenseCategory.LOCAL_TRANSPORT, "600", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, true, false, true, 0));
        assertEquals(new BigDecimal("600.00"), transport.reimbursableAmount());
        ExpenseAssessment internationalHighHotel = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(
                customItem("i1", ExpenseCategory.HOTEL, "2000.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, true, false, true, 0));
        assertFalse(internationalHighHotel.warnings().contains("domestic hotel amount is unusually high"));
    }

    @Test
    void expensePolicy_transportCashWithReceiptAndSmallInternSuppliesAreClean() {
        ExpenseAssessment cashWithReceipt = assess(List.of(
                customItem("i1", ExpenseCategory.LOCAL_TRANSPORT, "100", PaymentMethod.CASH,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(cashWithReceipt.warnings().contains("cash transport without receipt will be capped"));
        ExpenseAssessment internOk = assess(claim("E1", EmployeeGrade.INTERN, TODAY, List.of(
                customItem("i1", ExpenseCategory.OFFICE_SUPPLIES, "100", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, false, false, true, 0));
        assertEquals(List.of(), internOk.violations());
    }

    @Test
    void expensePolicy_smallAirfareWithoutApprovalIsAccepted() {
        ExpenseAssessment small = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "1000.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                false, false, false, true, 0));
        assertEquals(List.of(), small.violations());
    }

    @Test
    void expensePolicy_weekendRulesCoverSundayAndNonOfficeDays() {
        LocalDate sunday = LocalDate.of(2026, 6, 14);
        ExpenseAssessment sundaySupplies = assess(List.of(
                customItem("i1", ExpenseCategory.OFFICE_SUPPLIES, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, sunday, 0, false, false, false)));
        assertEquals(List.of("weekend office-supply purchase needs explanation"), sundaySupplies.warnings());
        LocalDate saturday = LocalDate.of(2026, 6, 13);
        ExpenseAssessment saturdayMeal = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, saturday, 0, false, false, false)));
        assertEquals(List.of(), saturdayMeal.warnings());
    }

    @Test
    void expensePolicy_blankPurposeAndNullCostCenter() {
        ExpenseAssessment blankPurpose = assess(List.of(
                new ExpenseItem("i1", ExpenseCategory.MEAL, new BigDecimal("10"), Currency.getInstance("CNY"),
                        TODAY.minusDays(5), "Merchant X", "", ReceiptType.STANDARD_RECEIPT,
                        PaymentMethod.PERSONAL_CARD, 0, false, false, false)));
        assertEquals(List.of("business purpose is required for item i1"), blankPurpose.violations());
        ExpenseClaim nullCost = new ExpenseClaim("C1", "E1", EmployeeGrade.STAFF, TODAY, null,
                List.of(validItem()), true, false, false, true, 0);
        assertEquals(List.of("cost center is missing"),
                new ExpensePolicy().assess(nullCost, oneRate(), TODAY).warnings());
    }

    @Test
    void accessContext_handlesNullDepartments() {
        AccessContext nullDepts = new AccessContext("A", null, null, null, UserRole.EMPLOYEE,
                ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true);
        assertFalse(nullDepts.sameDepartment());
        assertFalse(nullDepts.ownsResource());
    }

    @Test
    void accessPolicy_additionalMatrixBranches() {
        AccessDecision hrExportDept = decide(actx("A", null, UserRole.HUMAN_RESOURCES, ResourceAction.EXPORT,
                ResourceType.EMPLOYEE_PROFILE, Sensitivity.CONFIDENTIAL, AccessScope.DEPARTMENT));
        assertTrue(hrExportDept.allowed());
        AccessDecision hrUpdatePayroll = decide(actx("A", null, UserRole.HUMAN_RESOURCES,
                ResourceAction.UPDATE, ResourceType.PAYROLL, Sensitivity.CONFIDENTIAL, AccessScope.DEPARTMENT));
        assertFalse(hrUpdatePayroll.allowed());
        AccessDecision financeShare = decide(actx("A", null, UserRole.FINANCE, ResourceAction.SHARE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertTrue(financeShare.allowed());
        AccessDecision deptApproveContract = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER,
                ResourceAction.APPROVE, ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertTrue(deptApproveContract.allowed());
        AccessDecision internalPublicDoc = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.PUBLIC_DOCUMENT, Sensitivity.INTERNAL, AccessScope.OWN));
        assertTrue(internalPublicDoc.allowed());
        AccessDecision ownDelete = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.DELETE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN));
        assertFalse(ownDelete.allowed());
        AccessDecision teamUpdate = decide(actx("A", null, UserRole.TEAM_LEAD, ResourceAction.UPDATE,
                ResourceType.LEAVE_REQUEST, Sensitivity.INTERNAL, AccessScope.TEAM));
        assertFalse(teamUpdate.allowed());
        AccessDecision managerTeamLeave = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER,
                ResourceAction.VIEW, ResourceType.LEAVE_REQUEST, Sensitivity.INTERNAL, AccessScope.TEAM));
        assertTrue(managerTeamLeave.allowed());
        AccessDecision untrustedView = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, false, true, false, false, false, false, true, true, true));
        assertTrue(untrustedView.allowed());
        AccessDecision offHoursInternal = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, false, false, false, false, false, true, true, true));
        assertTrue(offHoursInternal.allowed());
        assertTrue(offHoursInternal.auditRequired());
    }

    @Test
    void expenseCardReview_considersRejectedExpenseAndAuditFlags() {
        AccessDecision allowed = new AccessDecision(true, false, false, List.of());
        AccessDecision allowedAudit = new AccessDecision(true, true, false, List.of());
        ExpenseAssessment ok = new ExpenseAssessment(new BigDecimal("10"), new BigDecimal("10"),
                ExpenseApprovalTier.LINE_MANAGER, false, false, List.of(), List.of());
        ExpenseAssessment rejectedExpense = new ExpenseAssessment(new BigDecimal("10"), BigDecimal.ZERO,
                ExpenseApprovalTier.FINANCE, true, false, List.of("v"), List.of());
        ExpenseAssessment auditedExpense = new ExpenseAssessment(new BigDecimal("10"), new BigDecimal("10"),
                ExpenseApprovalTier.FINANCE, false, true, List.of(), List.of());
        ExpenseCardReview rejected = new ExpenseCardReview("c", allowed, rejectedExpense, List.of(), List.of(),
                false, false);
        assertFalse(rejected.readyForApproval());
        ExpenseCardReview accessAudit = new ExpenseCardReview("c", allowedAudit, ok, List.of(), List.of(), true,
                true);
        assertTrue(accessAudit.auditRequired());
        ExpenseCardReview expenseAudit = new ExpenseCardReview("c", allowed, auditedExpense, List.of(), List.of(),
                true, true);
        assertTrue(expenseAudit.auditRequired());
    }

    @Test
    void expenseService_reviewReflectsRejectedExpenseAndAuditedAccess() {
        CorporateCardExpenseService svc = service();
        ExpenseClaim futureClaim = claim("E1", EmployeeGrade.STAFF, TODAY.plusDays(1), List.of(validItem()),
                true, false, false, true, 0);
        ExpenseCardReview rejectedReview = svc.review(futureClaim, Map.of(), oneRate(), TODAY, createActor("E1"));
        assertFalse(rejectedReview.readyForApproval());
        assertTrue(rejectedReview.expense().rejected());
        AccessContext restrictedCreator = new AccessContext("E1", null, "DEP", "DEP", UserRole.EMPLOYEE,
                ResourceAction.CREATE, ResourceType.EXPENSE_CLAIM, Sensitivity.RESTRICTED, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true);
        ExpenseCardReview audited = svc.review(defaultClaim(List.of(validItem())), Map.of(), oneRate(), TODAY,
                restrictedCreator);
        assertTrue(audited.readyForApproval());
        assertTrue(audited.auditRequired());
    }

    @Test
    void workflowEngine_skipRequesterStepWithNonRequesterApproverStaysPending() {
        WorkflowSnapshot pending = ENGINE.start("W1", "R1", List.of(step("s", Set.of("B"), StepMode.ANY_ONE, 1,
                false, true, null)), T0, false, true);
        assertEquals(WorkflowState.PENDING, pending.state());
        assertEquals(0, pending.currentStepIndex());
    }

    @Test
    void workflowEngine_withdrawAfterFirstApprovalWhenNotUrgent() {
        WorkflowSnapshot nonUrgent = ENGINE.start("W1", "R1", List.of(
                step("s1", Set.of("A"), StepMode.ANY_ONE, 1, false, false, null),
                step("s2", Set.of("B"), StepMode.ANY_ONE, 1, false, false, null)), T0, false, true);
        WorkflowSnapshot afterStep = ENGINE.act(nonUrgent, new WorkflowAction("A", ApprovalDecision.APPROVE,
                null, T1));
        WorkflowSnapshot withdrawn = ENGINE.withdraw(afterStep, "R1", T1.plusSeconds(1));
        assertEquals(WorkflowState.WITHDRAWN, withdrawn.state());
    }

    @Test
    void workflowEngine_delegatesOnSkipRequesterStepToNonRequester() {
        WorkflowSnapshot pending = ENGINE.start("W1", "R1", List.of(step("s", Set.of("A", "B"),
                StepMode.REQUIRED_COUNT, 2, true, true, null)), T0, false, true);
        WorkflowSnapshot delegated = ENGINE.delegate(pending, "A", "C", T1);
        assertEquals(WorkflowState.IN_PROGRESS, delegated.state());
        assertEquals(Set.of("C", "B"), delegated.currentStep().step().approverIds());
    }

    @Test
    void workflowSnapshot_rejectsBlankIdentifiers() {
        Instant t0 = Instant.parse("2026-06-15T08:00:00Z");
        List<StepProgress> steps = List.of(StepProgress.empty(step("s", Set.of("A"), StepMode.ANY_ONE, 1,
                false, false, null)));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot(" ", "R", WorkflowState.PENDING, steps, 0, t0, t0, 1, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot("W", " ", WorkflowState.PENDING, steps, 0, t0, t0, 1, false, true));
    }

    // ==================== threshold boundary and mutation-edge cases ====================

    @Test
    void expensePolicy_exactThresholdAmountsDoNotTriggerAuditOrTierBumps() {
        ExpenseAssessment exactlyFiveThousand = assess(List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "5000.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(exactlyFiveThousand.auditRequired());
        assertEquals(ExpenseApprovalTier.DEPARTMENT_HEAD, exactlyFiveThousand.approvalTier());
        ExpenseAssessment exactCashThousand = assess(List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "1000.00", PaymentMethod.CASH,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(exactCashThousand.auditRequired());
        assertEquals(ExpenseApprovalTier.LINE_MANAGER, exactCashThousand.approvalTier());
        ExpenseAssessment exactlyTwentyThousand = assess(List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "20000.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertEquals(ExpenseApprovalTier.FINANCE, exactlyTwentyThousand.approvalTier());
        ExpenseAssessment exactlyTwoThousand = assess(List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "2000.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertEquals(ExpenseApprovalTier.LINE_MANAGER, exactlyTwoThousand.approvalTier());
        ExpenseAssessment splitOverFiveThousand = assess(List.of(
                customItem("i1", ExpenseCategory.AIRFARE, "2500.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false),
                customItem("i2", ExpenseCategory.AIRFARE, "2500.01", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(splitOverFiveThousand.auditRequired());
        assertEquals(ExpenseApprovalTier.FINANCE, splitOverFiveThousand.approvalTier());
    }

    @Test
    void expensePolicy_itemCountAndViolationCountBoundaries() {
        List<ExpenseItem> twentyDistinct = new java.util.ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            twentyDistinct.add(customItem("i" + i, ExpenseCategory.MEAL, String.valueOf(i),
                    PaymentMethod.PERSONAL_CARD, ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5),
                    0, false, false, false));
        }
        assertTrue(assess(twentyDistinct, oneRate()).auditRequired());
        List<ExpenseItem> nineteenDistinct = twentyDistinct.subList(0, 19);
        assertFalse(assess(nineteenDistinct, oneRate()).auditRequired());
        ExpenseAssessment exactlyThreeViolations = assess(List.of(
                customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, true, false, false),
                customItem("i2", ExpenseCategory.SOFTWARE, "11", PaymentMethod.CASH,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertEquals(3, exactlyThreeViolations.violations().size());
        assertTrue(exactlyThreeViolations.auditRequired());
        List<ExpenseItem> fiftyItems = new java.util.ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            fiftyItems.add(customItem("i" + i, ExpenseCategory.MEAL, String.valueOf(i),
                    PaymentMethod.PERSONAL_CARD, ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5),
                    0, false, false, false));
        }
        ExpenseAssessment fifty = assess(fiftyItems, oneRate());
        assertFalse(fifty.warnings().contains("large claim should be split by trip or purpose"));
        ExpenseAssessment twentyClaims = assess(claim("E1", EmployeeGrade.STAFF, TODAY, List.of(validItem()),
                true, false, false, true, 20));
        assertFalse(twentyClaims.warnings().contains("unusually high claim frequency"));
    }

    @Test
    void expensePolicy_rejectedClaimsReturnZeroReimbursable() {
        ExpenseAssessment future = assess(claim("E1", EmployeeGrade.STAFF, TODAY.plusDays(1),
                List.of(validItem()), true, false, false, true, 0));
        assertTrue(future.rejected());
        assertEquals(new BigDecimal("0.00"), future.reimbursableAmount());
        assertEquals(new BigDecimal("10.00"), future.claimedAmount());
    }

    @Test
    void expensePolicy_exactCategoryThresholdsProduceNoWarnings() {
        ExpenseAssessment hotel = assess(List.of(
                customItem("i1", ExpenseCategory.HOTEL, "2000.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(hotel.warnings().contains("domestic hotel amount is unusually high"));
        ExpenseAssessment transport = assess(List.of(
                customItem("i1", ExpenseCategory.LOCAL_TRANSPORT, "1000.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(transport.warnings().contains("local transport amount needs route details"));
        ExpenseAssessment office = assess(List.of(
                customItem("i1", ExpenseCategory.OFFICE_SUPPLIES, "500.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(office.warnings().contains("large office purchase should use corporate procurement"));
        ExpenseAssessment software = assess(List.of(
                customItem("i1", ExpenseCategory.SOFTWARE, "3000.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(software.warnings().contains("software purchase should use procurement review"));
        ExpenseAssessment communication = assess(List.of(
                customItem("i1", ExpenseCategory.COMMUNICATION, "500.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(communication.warnings().contains("communication expense exceeds ordinary monthly level"));
        ExpenseAssessment other = assess(List.of(
                customItem("i1", ExpenseCategory.OTHER, "500.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertFalse(other.violations().contains("other-category expense exceeds allowed limit"));
    }

    @Test
    void cardEvidenceValidator_exercisesLuhnDoublingBoundaries() {
        CardEvidenceValidator validator = cardValidator();
        List<String[]> fixtures = List.of(
                new String[]{"4", "16", "VISA"},
                new String[]{"5", "16", "MAESTRO"},
                new String[]{"6", "16", "MAESTRO"},
                new String[]{"605", "16", "MAESTRO"},
                new String[]{"607", "16", "MAESTRO"},
                new String[]{"609", "16", "MAESTRO"},
                new String[]{"37", "15", "AMERICAN_EXPRESS"},
                new String[]{"5", "13", "MAESTRO"},
                new String[]{"5", "19", "MAESTRO"});
        for (String[] fixture : fixtures) {
            String number = luhnFill(fixture[0], Integer.parseInt(fixture[1]));
            CardEvidenceCheck check = validator.check(new CardEvidence("x", number, YearMonth.of(2027, 1)));
            assertTrue(check.valid(), "fixture " + fixture[0] + "/" + fixture[1]);
            assertEquals(CreditCardType.valueOf(fixture[2]), check.type());
        }
        String validFive = luhnFill("5", 16);
        String corrupted = validFive.substring(0, 15) + (validFive.charAt(15) == '0' ? '1' : '0');
        assertFalse(validator.check(new CardEvidence("x", corrupted, YearMonth.of(2027, 1))).valid());
    }

    @Test
    void workflowEngine_terminalActionsAreRejectedWithSpecificMessages() {
        WorkflowSnapshot approved = ENGINE.start("W1", "R1", List.of(step("s", Set.of("R1"),
                StepMode.ANY_ONE, 1, false, true, null)), T0, false, true);
        IllegalStateException actError = assertThrows(IllegalStateException.class, () -> ENGINE.act(approved,
                new WorkflowAction("A", ApprovalDecision.APPROVE, null, T1)));
        assertEquals("terminal workflow cannot accept actions", actError.getMessage());
        IllegalStateException delegateError = assertThrows(IllegalStateException.class, () -> ENGINE.delegate(
                approved, "A", "B", T1));
        assertEquals("terminal workflow cannot accept actions", delegateError.getMessage());
        IllegalStateException withdrawError = assertThrows(IllegalStateException.class, () -> ENGINE.withdraw(
                approved, "R1", T1));
        assertEquals("terminal workflow cannot accept actions", withdrawError.getMessage());
    }

    // ==================== helpers ====================

    private static String pad(String prefix, int length) {
        return prefix + "0".repeat(Math.max(0, length - prefix.length()));
    }

    /**
     * Exercises a card validator across lengths 0..21, accepted/rejected IIN prefixes and
     * null/empty inputs. Only the public validate() contract is asserted.
     */
    private static void assertCardFormat(Function<String, CreditCardValidator> factory,
            int minLen, int maxLen, int iinDigits,
            String[] acceptedPrefixes, String[] rejectedPrefixes) {
        assertThrows(NullPointerException.class, () -> factory.apply(null));
        assertThrows(IndexOutOfBoundsException.class, () -> factory.apply("").validate());
        for (int length = 1; length <= 21; length++) {
            String zeros = "0".repeat(length);
            if (length < iinDigits) {
                int len = length;
                assertThrows(IndexOutOfBoundsException.class, () -> factory.apply(zeros).validate(),
                        "zero padding too short at length " + len);
            } else {
                assertFalse(factory.apply(zeros).validate(), "zero padding at length " + length);
            }
        }
        for (String prefix : acceptedPrefixes) {
            assertTrue(factory.apply(pad(prefix, minLen)).validate(), "accepted prefix " + prefix + " at minLen");
            if (maxLen != minLen) {
                assertTrue(factory.apply(pad(prefix, maxLen)).validate(),
                        "accepted prefix " + prefix + " at maxLen");
            }
            if (minLen - 1 >= 1) {
                assertFalse(factory.apply(pad(prefix, minLen - 1)).validate(),
                        "accepted prefix " + prefix + " at minLen-1");
            }
            if (maxLen + 1 <= 21) {
                assertFalse(factory.apply(pad(prefix, maxLen + 1)).validate(),
                        "accepted prefix " + prefix + " at maxLen+1");
            }
        }
        for (String prefix : rejectedPrefixes) {
            assertFalse(factory.apply(pad(prefix, minLen)).validate(), "rejected prefix " + prefix);
        }
    }

    /** Independent Luhn check-digit completion; never calls production code. */
    private static String luhnFill(String prefix, int length) {
        int[] digits = new int[length];
        for (int i = 0; i < prefix.length(); i++) {
            digits[i] = prefix.charAt(i) - '0';
        }
        int sum = 0;
        for (int i = length - 2; i >= 0; i--) {
            boolean doubleIt = (length - 1 - i) % 2 == 1;
            sum += doubleIt ? doubled(digits[i]) : digits[i];
        }
        digits[length - 1] = (10 - sum % 10) % 10;
        StringBuilder builder = new StringBuilder(length);
        for (int digit : digits) {
            builder.append((char) ('0' + digit));
        }
        return builder.toString();
    }

    private static int doubled(int digit) {
        int result = digit * 2;
        return result < 10 ? result : result - 9;
    }
}
