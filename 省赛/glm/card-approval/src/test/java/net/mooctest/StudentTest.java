/*
 * 2026 软件测试大赛-开发者测试-预选赛
 * 题目：corporate-card-expense-approval
 * 单一测试文件 StudentTest.java：覆盖 card 校验、报销策略、权限策略与审批工作流。
 *
 * Provably dead branch sides ( JaCoCo sides that no test can reach, documented with proof):
 * 1. card/VerveValidator.java L62, true side of `if (IIN == i)` in the SECOND loop of
 *    checkIINRanges: the second loop iterates exactly the same IINRange[0]..IINRange[1]
 *    bounds (506099..506198) as the first loop and only executes when the first loop
 *    completed without a match, so `IIN == i` can never hold there. (The 650002..650027
 *    band was clearly intended, but the source reuses the first bounds - hence
 *    6500-prefixed Verve numbers are rejected, as asserted by existing tests.)
 * 2. expense/ExpensePolicy.java L107, true side of `if (claim.clientBillable())` inside
 *    the missing-cost-center block: ExpenseClaim's compact constructor already throws
 *    "client-billable claim requires a cost center" whenever clientBillable && (costCenter
 *    == null || costCenter.isBlank()) - exactly the condition guarding L107 - so no
 *    ExpenseClaim instance can reach L107 with clientBillable == true.
 * 3. expense/ExpensePolicy.java L221, true side of `if (cap.signum() == 0)`: categoryCap()
 *    multiplies a positive per-category base (100..1500) by a grade multiplier in 1..10
 *    and only ever scales it up (x1.5 international, x1.25 client-billable), so cap is
 *    strictly positive and can never be zero.
 * 4. expense/ExpensePolicy.java L272, the switch's default target: the statement switches
 *    over the 11-constant ExpenseCategory enum and all 11 case sides are covered; the
 *    selector item.category() is requireNonNull in the ExpenseItem compact constructor,
 *    so the out-of-range tableswitch default is unreachable.
 * 5. expense/ExpensePolicy.java L293, true side of `if (item.attendeeCount() > 0)` in the
 *    MEAL case: the ExpenseItem compact constructor throws "attendeeCount is only valid
 *    for client entertainment" for any category other than CLIENT_ENTERTAINMENT with
 *    attendeeCount > 0, so a MEAL item can never carry a positive attendee count here.
 * 6. card/Validator.java L94, the false side of `expirationYear != null` inside
 *    checkExpirationDate: the 3/4-arg constructor assigns expirationMonth and
 *    expirationYear back to back before the object escapes, and the 1-arg constructor
 *    leaves the StringBuilder null so setExpirationMonth()/setExpirationYear() throw
 *    IllegalStateException from requireExpirationDate() before assigning. No externally
 *    visible state can have month != null while year == null.
 * 7. workflow/ApprovalWorkflowEngine.java L181, true side of `state != IN_PROGRESS` in
 *    ensureActive: every terminal state is rejected by the terminal() guard two lines
 *    above, and the only non-terminal states are PENDING (short-circuits the first
 *    condition at L180) and IN_PROGRESS (evaluates false at L181). The "workflow is not
 *    active" IllegalStateException at L182 is therefore unreachable.
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
import net.mooctest.card.DateChecker;
import net.mooctest.card.DateParser;
import net.mooctest.card.DinersClubInternationalValidator;
import net.mooctest.card.DinersClubValidator;
import net.mooctest.card.DiscoverValidator;
import net.mooctest.card.InstaPaymenttValidator;
import net.mooctest.card.InterPaymentValidator;
import net.mooctest.card.InvalidCardException;
import net.mooctest.card.JCBValidator;
import net.mooctest.card.LankaPayValidator;
import net.mooctest.card.LuhnValidator;
import net.mooctest.card.MaestroUKValidator;
import net.mooctest.card.MaestroValidator;
import net.mooctest.card.MasterCardValidator;
import net.mooctest.card.MIRValidator;
import net.mooctest.card.NPS_PridnestrovieValidator;
import net.mooctest.card.RuPayValidator;
import net.mooctest.card.TroyValidator;
import net.mooctest.card.TypeChecker;
import net.mooctest.card.UATPValidator;
import net.mooctest.card.Validator;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.Nested;
import net.mooctest.attendance.AttendanceCode;
import net.mooctest.attendance.AttendanceInput;
import net.mooctest.attendance.AttendancePolicy;
import net.mooctest.attendance.AttendanceResult;
import net.mooctest.attendance.EmploymentMode;
import net.mooctest.attendance.OvertimePolicy;
import net.mooctest.attendance.OvertimeResult;
import net.mooctest.attendance.PunchRecord;
import net.mooctest.attendance.Shift;
import net.mooctest.attendance.ShiftType;
import net.mooctest.card.DateFormatter;
import net.mooctest.domain.DateRange;
import net.mooctest.domain.LeaveRequest;
import net.mooctest.domain.LeaveStatus;
import net.mooctest.domain.LeaveType;
import net.mooctest.exception.LeaveRequestNotFoundException;
import net.mooctest.exception.LeaveValidationException;
import net.mooctest.leave.AdvancedLeavePolicy;
import net.mooctest.leave.AdvancedLeaveRequest;
import net.mooctest.leave.ApprovalTier;
import net.mooctest.leave.EmployeeCategory;
import net.mooctest.leave.LeaveAssessment;
import net.mooctest.leave.LeaveUnit;
import net.mooctest.leave.WorkCalendar;
import net.mooctest.port.LeaveBalanceRepository;
import net.mooctest.port.LeaveRequestRepository;
import net.mooctest.port.NotificationGateway;
import net.mooctest.port.RequestIdGenerator;
import net.mooctest.service.LeavePolicy;
import net.mooctest.service.LeaveRequestService;
import net.mooctest.service.SubmitLeaveCommand;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import net.mooctest.task.RecurrenceRule;
import net.mooctest.task.Task;
import net.mooctest.task.TaskAction;
import net.mooctest.task.TaskContext;
import net.mooctest.task.TaskEvaluation;
import net.mooctest.task.TaskPolicy;
import net.mooctest.task.TaskPriority;
import net.mooctest.task.TaskStateMachine;
import net.mooctest.task.TaskStatus;
import net.mooctest.util.DateTextParser;
import java.util.EnumSet;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

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
        // checkIINRanges now parses six digits: the documented 417500 prefix matches, a
        // two-position leading-zero shift reproduces the four-digit prefixes, the old
        // one-position shift ("04026") no longer matches, and inputs shorter than six
        // digits throw IndexOutOfBounds from the prefix parse.
        assertCardFormat(VisaElectronValidator::new, 16, 16, 6,
                new String[]{"4026", "4405", "4508", "4844", "4913", "4917",
                        "417500", "004026", "004405", "004913"},
                new String[]{"4025", "4027", "4404", "4406", "4507", "4509",
                        "4843", "4845", "4912", "4914", "4916", "4918",
                        "04026", "041750", "417501", "99"});
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
        // checkIINRanges now parses a single digit, so the documented "1" prefix matches,
        // the old zero-padded "000001" trick no longer does, and zero padding of any
        // length >= 1 stays inside the prefix parse bounds.
        assertCardFormat(UATPValidator::new, 15, 15, 1,
                new String[]{"1"}, new String[]{"0", "2", "9", "000000", "000001", "000002", "99"});
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
        // UATP now compares the one-digit prefix 1 against its 15-digit length.
        assertTrue(new UATPValidator("100000000000000").validate());
        assertFalse(new UATPValidator("000001000000000").validate());
        // Visa Electron now recognizes its documented six-digit prefix 417500; the old
        // one-position zero shift ("04026") no longer matches. A two-position shift
        // ("004026") still lands on 4026, while the "4H5" trick breaks: 'H' parses as 17
        // and the six-element parse reads "4"+"17"+"5"+"0"+"0"+"0" = 4175000.
        assertTrue(new VisaElectronValidator("4175000000000000").validate());
        assertFalse(new VisaElectronValidator("0402600000000000").validate());
        assertTrue(new VisaElectronValidator("0040260000000000").validate());
        assertFalse(new VisaElectronValidator("4H50000000000000").validate());
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

    @Test
    void luhnValidator_sumsDigitsAndDoublesEverySecondDigitFromTheRight() {
        LuhnValidator luhn = new LuhnValidator();
        assertTrue(luhn.validate("4111111111111111"));
        assertFalse(luhn.validate("4111111111111112"));
        // sumOfDigits collapses two-digit doubles: 5*2=10 -> 1, 9*2=18 -> 9; singles pass through.
        assertEquals(1, luhn.sumOfDigits(10));
        assertEquals(9, luhn.sumOfDigits(18));
        assertEquals(9, luhn.sumOfDigits(9));
        assertEquals(0, luhn.sumOfDigits(0));
        // No digits produce an empty array, sum 0, and the modulo check accepts it.
        assertTrue(luhn.algorithmCheck(""));
        assertTrue(luhn.algorithmCheck("00"));
    }

    // ==================== TypeChecker ====================

    @ParameterizedTest
    @CsvSource({
            "4026000000000000,VISA_ELECTRON", "4405000000000000,VISA_ELECTRON",
            "4508000000000000,VISA_ELECTRON", "4844000000000000,VISA_ELECTRON",
            "4913000000000000,VISA_ELECTRON", "4917000000000000,VISA_ELECTRON",
            "4175000000000000,VISA_ELECTRON", "0040260000000000,VISA_ELECTRON",
            "0402600000000000,OTHER", "4H50000000000000,OTHER",
            "4111111111111111,VISA", "4111111111111,VISA", "4999999999999999999,VISA",
            "4000000000000000,VISA", "4570000000000000,VISA",
            "4571000000000000,DANKORT", "5019000000000000,DANKORT", "5000000000000000,DANKORT",
            "341111111111111,AMERICAN_EXPRESS", "371111111111111,AMERICAN_EXPRESS",
            "5100000000000000,MASTERCARD", "5500000000000000,MASTERCARD",
            "2221000000000000,MASTERCARD", "2720990000000000,MASTERCARD",
            "5400000000000000,MASTERCARD",
            "6011000000000000,DISCOVER", "6221260000000000,DISCOVER",
            "6229250000000000,DISCOVER", "6440000000000000,DISCOVER",
            "6490000000000000,DISCOVER", "6500000000000000,DISCOVER",
            "6759000000000000,MAESTRO_UK", "6767700000000000,MAESTRO_UK",
            "6767740000000000,MAESTRO_UK",
            "5600000000000000,MAESTRO", "6999990000000000,MAESTRO", "500000000000,MAESTRO",
            "3528000000000000,JCB", "3589000000000000,JCB",
            "3100000000000000000,CHINA_TUNION",
            "3000000000000000,DINERS_CLUB_INTERNATIONAL",
            "3050000000000000,DINERS_CLUB_INTERNATIONAL",
            "3095000000000000,DINERS_CLUB_INTERNATIONAL",
            "3800000000000000,DINERS_CLUB_INTERNATIONAL",
            "3900000000000000,DINERS_CLUB_INTERNATIONAL",
            "6054740000000000,NPS_PRIDNESTROVIE", "3571110000000000,LANKAPAY",
            "5060990000000000,VERVE",
            "2200000000000000,MIR", "2204000000000000,MIR",
            "9792000000000000,TROY", "9792890000000000,TROY",
            "6000000000000000,RUPAY", "6200000000000000,CHINA_UNIONPAY",
            "6221250000000000,CHINA_UNIONPAY",
            "6360000000000000,INTERPAYMENT", "6370000000000000,INSTAPAYMENT",
            "100000000000000,UATP",
            "411111111111,OTHER", "41111111111111111111,OTHER",
            "3411111111111111,OTHER", "510000000000000,OTHER",
            "2220990000000000,OTHER",
            "2721000000000000,OTHER", "2199000000000000,OTHER",
            "2205000000000000,OTHER", "3100000000000000,OTHER",
            "000001000000000,OTHER", "9111111111111111,OTHER"})
    void typeChecker_classifiesByPrefixAndLength(String number, CreditCardType expected) {
        assertEquals(expected, TypeChecker.checkType(number));
    }

    @Test
    void typeChecker_earlierNetworksShadowLaterOnes() {
        // CHECK_ORDER puts specific prefixes first: NPS, LankaPay, Verve, Troy, Visa
        // Electron, Maestro UK, Dankort, ..., RuPay, InterPayment, InstaPayment, MIR,
        // UATP and Maestro last. Dankort's wide 4571-5019 first-four window shadows
        // Maestro's 500000-5019xx and Visa's 4571-4800xx; RuPay's 60 prefix shadows
        // Maestro inside 600000-609999; Diners Club is absent from the order entirely.
        assertEquals(CreditCardType.RUPAY, TypeChecker.checkType("6000000000000000"));
        assertEquals(CreditCardType.DISCOVER, TypeChecker.checkType("6521000000000000"));
        assertEquals(CreditCardType.INTERPAYMENT, TypeChecker.checkType("6360000000000000"));
        assertEquals(CreditCardType.INSTAPAYMENT, TypeChecker.checkType("6370000000000000"));
        assertEquals(CreditCardType.VERVE, TypeChecker.checkType("5060990000000000"));
        assertEquals(CreditCardType.DISCOVER, TypeChecker.checkType("6500020000000000"));
        assertEquals(CreditCardType.NPS_PRIDNESTROVIE, TypeChecker.checkType("6054740000000000"));
        assertEquals(CreditCardType.CHINA_UNIONPAY, TypeChecker.checkType("6200000000000000"));
        assertEquals(CreditCardType.CHINA_UNIONPAY, TypeChecker.checkType("6221250000000000"));
        assertEquals(CreditCardType.DANKORT, TypeChecker.checkType("5019000000000000"));
        assertEquals(CreditCardType.MAESTRO, TypeChecker.checkType("601100000000000"));
        assertEquals(CreditCardType.LANKAPAY, TypeChecker.checkType("3571110000000000"));
        assertEquals(CreditCardType.DANKORT, TypeChecker.checkType("4571000000000000"));
        assertEquals(CreditCardType.MASTERCARD, TypeChecker.checkType("5400000000000000"));
    }

    @Test
    void typeChecker_matchesTypeChecksEachNetworkExplicitly() {
        // matchesType bypasses the CHECK_ORDER shadowing: every network arm answers for
        // its own sample, and a successful match still says nothing about the checksum.
        List<Map.Entry<CreditCardType, String>> samples = List.of(
                Map.entry(CreditCardType.VISA, "4111111111111111"),
                Map.entry(CreditCardType.MASTERCARD, "5100000000000000"),
                Map.entry(CreditCardType.MASTERCARD, "2221000000000000"),
                Map.entry(CreditCardType.DISCOVER, "6011000000000000"),
                Map.entry(CreditCardType.AMERICAN_EXPRESS, "341111111111111"),
                Map.entry(CreditCardType.VISA_ELECTRON, "4026000000000000"),
                Map.entry(CreditCardType.VISA_ELECTRON, "4175000000000000"),
                Map.entry(CreditCardType.MAESTRO, "5600000000000000"),
                Map.entry(CreditCardType.MAESTRO_UK, "6759000000000000"),
                Map.entry(CreditCardType.JCB, "3528000000000000"),
                Map.entry(CreditCardType.CHINA_TUNION, "3100000000000000000"),
                Map.entry(CreditCardType.CHINA_UNIONPAY, "6200000000000000"),
                Map.entry(CreditCardType.DINERS_CLUB_INTERNATIONAL, "3000000000000000"),
                Map.entry(CreditCardType.DINERS_CLUB, "5400000000000000"),
                Map.entry(CreditCardType.RUPAY, "6000000000000000"),
                Map.entry(CreditCardType.INTERPAYMENT, "6360000000000000"),
                Map.entry(CreditCardType.INSTAPAYMENT, "6370000000000000"),
                Map.entry(CreditCardType.DANKORT, "4571000000000000"),
                Map.entry(CreditCardType.MIR, "2200000000000000"),
                Map.entry(CreditCardType.NPS_PRIDNESTROVIE, "6054740000000000"),
                Map.entry(CreditCardType.TROY, "9792000000000000"),
                Map.entry(CreditCardType.UATP, "100000000000000"),
                Map.entry(CreditCardType.VERVE, "5060990000000000"),
                Map.entry(CreditCardType.LANKAPAY, "3571110000000000"));
        for (Map.Entry<CreditCardType, String> sample : samples) {
            assertTrue(TypeChecker.matchesType(sample.getValue(), sample.getKey()),
                    sample.getKey() + " sample " + sample.getValue());
        }
        // Shadowed networks still answer true when asked explicitly.
        assertTrue(TypeChecker.matchesType("5000000000000000", CreditCardType.MAESTRO));
        assertTrue(TypeChecker.matchesType("5400000000000000", CreditCardType.DINERS_CLUB));
        assertFalse(TypeChecker.matchesType("5400000000000000", CreditCardType.DINERS_CLUB_INTERNATIONAL));
        assertFalse(TypeChecker.matchesType("4111111111111111", CreditCardType.MASTERCARD));
        // OTHER never matches, and the syntax gate returns false without touching validators.
        assertFalse(TypeChecker.matchesType("4111111111111111", CreditCardType.OTHER));
        assertFalse(TypeChecker.matchesType("4111", CreditCardType.VISA));
        assertFalse(TypeChecker.matchesType("", CreditCardType.VISA));
        assertThrows(NullPointerException.class, () -> TypeChecker.matchesType(null, CreditCardType.VISA));
        assertThrows(NullPointerException.class, () -> TypeChecker.matchesType("4111111111111111", null));
    }

    @Test
    void typeChecker_rejectsNullAndTooShortInputs() {
        assertThrows(NullPointerException.class, () -> TypeChecker.checkType(null));
        // The 12-to-19 digit syntax gate returns OTHER before any validator runs, so
        // short, long and non-decimal inputs no longer throw from the prefix parse.
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType(""));
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType("1"));
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType("12345"));
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType("000000"));
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType("12345678901"));
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType("12345678901234567890"));
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType("A4111111111111"));
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType("000001000000000"));
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
    @ValueSource(strings = {"", "1", "12345678901", "12345678901234567890", "A4111111111111111"})
    void cardEvidenceValidator_rejectsSyntaxViolations(String number) {
        CardEvidenceCheck check = cardValidator().check(evidence(number, YearMonth.of(2027, 1)));
        assertFalse(check.valid());
        assertEquals(CreditCardType.OTHER, check.type());
        assertEquals(List.of("card number must contain 12 to 19 digits"), check.issues());
    }

    @Test
    void cardEvidenceValidator_twelveDigitNumberReachesChecksumAndNetworkChecks() {
        // "123456789012" used to be a syntax violation; the widened 12-19 window sends it
        // through the Luhn and network checks instead (checksum invalid, network unsupported).
        CardEvidenceCheck check = cardValidator().check(evidence("123456789012", YearMonth.of(2027, 1)));
        assertFalse(check.valid());
        assertEquals(CreditCardType.OTHER, check.type());
        assertEquals(List.of("card number checksum is invalid", "card network is not supported"),
                check.issues());
    }

    @Test
    void cardEvidenceValidator_acceptsTwelveDigitLowerBound() {
        // A 12-digit Maestro number with a valid checksum is now fully accepted;
        // one digit fewer is still a syntax violation.
        String twelve = luhnFill("50", 12);
        assertTrue(cardValidator().check(evidence(twelve, YearMonth.of(2027, 1))).valid(), twelve);
        CardEvidenceCheck eleven = cardValidator().check(evidence(twelve.substring(0, 11), YearMonth.of(2027, 1)));
        assertFalse(eleven.valid());
        assertEquals(List.of("card number must contain 12 to 19 digits"), eleven.issues());
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

    // ==================== Validator / DateChecker / DateParser ====================

    @Test
    void validator_constructorRejectsNullAndMalformedExpirationDates() {
        assertThrows(NullPointerException.class, () -> new Validator(null, "0627", "123", FIXED_CLOCK));
        assertThrows(NullPointerException.class, () -> new Validator("4111111111111111", null, "123", FIXED_CLOCK));
        assertThrows(NullPointerException.class, () -> new Validator("4111111111111111", "0627", "123", null));
        IllegalArgumentException malformed = assertThrows(IllegalArgumentException.class,
                () -> new Validator("4111111111111111", "13/27", "123", FIXED_CLOCK));
        assertEquals("expirationDate must use MMyy or MM/yy", malformed.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> new Validator("4111111111111111", "0027", "123", FIXED_CLOCK));
        // Both accepted formats (MM/yy and MMyy) construct successfully against a fixed clock.
        assertTrue(new Validator("4111111111111111", "06/27", "123", FIXED_CLOCK).checkExpirationDate());
        assertTrue(new Validator("4111111111111111", "0627", "123", FIXED_CLOCK).checkExpirationDate());
    }

    @Test
    void dateParser_stripsOnlyTheFirstSlashWhenPresent() {
        assertEquals("0627", DateParser.parseDate(new StringBuilder("06/27")).toString());
        assertEquals("12/3", DateParser.parseDate(new StringBuilder("1/2/3")).toString());
        assertEquals("0627", DateParser.parseDate(new StringBuilder("0627")).toString());
        assertEquals("", DateParser.parseDate(new StringBuilder("")).toString());
    }

    @Test
    void validator_validateEnforcesDigitWindowAndLuhnChecksum() {
        InvalidCardException shortNumber = assertThrows(InvalidCardException.class,
                () -> new Validator("12345", "0627", "123", FIXED_CLOCK).validate());
        assertEquals("Card number must contain 12 to 19 digits", shortNumber.getMessage());
        InvalidCardException nonDigit = assertThrows(InvalidCardException.class,
                () -> new Validator("4111111111111ABC", "0627", "123", FIXED_CLOCK).validate());
        assertEquals("Card number must contain 12 to 19 digits", nonDigit.getMessage());
        InvalidCardException badChecksum = assertThrows(InvalidCardException.class,
                () -> new Validator("4111111111111112", "0627", "123", FIXED_CLOCK).validate());
        assertEquals("Card number checksum is invalid", badChecksum.getMessage());
        assertTrue(new Validator(luhnFill("4", 12), "0627", "123", FIXED_CLOCK).validate());
        assertTrue(new Validator(luhnFill("4", 19), "0627", "123", FIXED_CLOCK).validate());
    }

    @Test
    void validator_checkCvvAcceptsOnlyThreeOrFourDigits() {
        assertFalse(new Validator("4111111111111111").checkCVV());
        assertFalse(new Validator("4111111111111111", "0627", null, FIXED_CLOCK).checkCVV());
        assertTrue(new Validator("4111111111111111", "0627", "123", FIXED_CLOCK).checkCVV());
        assertTrue(new Validator("4111111111111111", "0627", "0123", FIXED_CLOCK).checkCVV());
        assertFalse(new Validator("4111111111111111", "0627", "12", FIXED_CLOCK).checkCVV());
        assertFalse(new Validator("4111111111111111", "0627", "12345", FIXED_CLOCK).checkCVV());
        assertFalse(new Validator("4111111111111111", "0627", "12a", FIXED_CLOCK).checkCVV());
    }

    @Test
    void validator_checkExpirationDateIsMonthInclusiveAgainstTheFixedClock() {
        // Fixed clock 2026-06-15: a card stays valid through the last day of its expiry month.
        assertTrue(new Validator("4111111111111111", "0626", "123", FIXED_CLOCK).checkExpirationDate());
        assertTrue(new Validator("4111111111111111", "0726", "123", FIXED_CLOCK).checkExpirationDate());
        assertFalse(new Validator("4111111111111111", "0526", "123", FIXED_CLOCK).checkExpirationDate());
        assertFalse(new Validator("4111111111111111", "1225", "123", FIXED_CLOCK).checkExpirationDate());
        Validator bare = new Validator("4111111111111111");
        assertFalse(bare.checkExpirationDate());
        IllegalStateException missing = assertThrows(IllegalStateException.class, bare::setExpirationMonth);
        assertEquals("expirationDate was not supplied", missing.getMessage());
        assertThrows(IllegalStateException.class, bare::setExpirationYear);
    }

    @Test
    void dateChecker_compareDatesUsesStrictlyLaterAsValid() {
        assertTrue(DateChecker.compareDates(202706, 202611));
        assertFalse(DateChecker.compareDates(202611, 202611));
        assertFalse(DateChecker.compareDates(202605, 202611));
    }

    @Test
    void dateChecker_twoDigitYearBoundsAreZeroToNinetyNine() {
        IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
                () -> DateChecker.isValidExpiration(1, -1, FIXED_CLOCK));
        assertEquals("year must contain two digits", negative.getMessage());
        IllegalArgumentException threeDigits = assertThrows(IllegalArgumentException.class,
                () -> DateChecker.isValidExpiration(1, 100, FIXED_CLOCK));
        assertEquals("year must contain two digits", threeDigits.getMessage());
        assertFalse(DateChecker.isValidExpiration(1, 0, FIXED_CLOCK));
        assertTrue(DateChecker.isValidExpiration(12, 99, FIXED_CLOCK));
        // 2026-06 clock: June 2026 (year 26) is still valid, May 2026 is not.
        assertTrue(DateChecker.isValidExpiration(6, 26, FIXED_CLOCK));
        assertFalse(DateChecker.isValidExpiration(5, 26, FIXED_CLOCK));
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
    void expenseRecords_rejectNullIdentifiersWithTheBlanknessMessage() {
        IllegalArgumentException nullEmployee = assertThrows(IllegalArgumentException.class,
                () -> new ExpenseClaim("c", null, EmployeeGrade.STAFF, LocalDate.of(2026, 6, 15), "CC",
                        List.of(validItem()), true, false, false, true, 0));
        assertEquals("employeeId must not be blank", nullEmployee.getMessage());
        IllegalArgumentException nullItem = assertThrows(IllegalArgumentException.class,
                () -> new ExpenseItem(null, ExpenseCategory.MEAL, new BigDecimal("10"),
                        Currency.getInstance("CNY"), LocalDate.of(2026, 6, 10), "Merchant X",
                        "Business purpose here", ReceiptType.STANDARD_RECEIPT, PaymentMethod.PERSONAL_CARD,
                        0, false, false, false));
        assertEquals("itemId must not be blank", nullItem.getMessage());
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

    // ==================== ExpensePolicy branch-gap tests ====================

    @Test
    void expensePolicy_inactiveProjectIsOnlyFatalForClientBillableClaims() {
        ExpenseAssessment billable = assess(claim("E1", EmployeeGrade.STAFF, TODAY,
                List.of(validItem()), true, false, true, false, 0));
        assertTrue(billable.violations()
                .contains("FATAL: inactive project cannot receive client-billable expenses"));
        assertTrue(billable.rejected());
        ExpenseAssessment internal = assess(claim("E1", EmployeeGrade.STAFF, TODAY,
                List.of(validItem()), true, false, false, false, 0));
        assertTrue(internal.violations().isEmpty());
        assertTrue(internal.warnings().isEmpty());
        assertFalse(internal.rejected());
    }

    @Test
    void expensePolicy_internationalTripRaisesLocalTransportCapByHalf() {
        // LOCAL_TRANSPORT base 200 x STAFF(2) x 1.5 international multiplier = 600.00.
        ExpenseAssessment assessment = assess(claim("E1", EmployeeGrade.STAFF, TODAY,
                List.of(customItem("i1", ExpenseCategory.LOCAL_TRANSPORT, "1000", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, true, false, true, 0));
        assertEquals(new BigDecimal("1000.00"), assessment.claimedAmount());
        assertEquals(new BigDecimal("600.00"), assessment.reimbursableAmount());
        assertTrue(assessment.warnings().contains("category cap applied to item i1"));
        assertTrue(assessment.violations().isEmpty());
        // The international surcharge only applies to hotel, meal and transport caps;
        // an international airfare keeps its plain 1500 x grade cap.
        ExpenseAssessment airfare = assess(claim("E1", EmployeeGrade.STAFF, TODAY,
                List.of(customItem("i1", ExpenseCategory.AIRFARE, "800", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)),
                true, true, false, true, 0));
        assertEquals(new BigDecimal("800.00"), airfare.reimbursableAmount());
        assertTrue(airfare.warnings().isEmpty());
    }

    @Test
    void expensePolicy_largePersonalCardOfficePurchasesWarnButSmallOnesDoNot() {
        ExpenseAssessment large = assess(List.of(
                customItem("o1", ExpenseCategory.OFFICE_SUPPLIES, "700", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(large.warnings().contains("large office purchase should use corporate procurement"));
        assertTrue(large.warnings().contains("category cap applied to item o1"));
        assertEquals(new BigDecimal("600.00"), large.reimbursableAmount());
        ExpenseAssessment small = assess(List.of(
                customItem("o2", ExpenseCategory.OFFICE_SUPPLIES, "100", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(small.warnings().isEmpty());
        assertEquals(new BigDecimal("100.00"), small.reimbursableAmount());
        // Non-personal-card office purchases never reach the procurement warning.
        ExpenseAssessment corporate = assess(List.of(
                customItem("o3", ExpenseCategory.OFFICE_SUPPLIES, "100", PaymentMethod.CORPORATE_CARD,
                        ReceiptType.STANDARD_RECEIPT, TODAY.minusDays(5), 0, false, false, false)));
        assertTrue(corporate.warnings().isEmpty());
        assertEquals(new BigDecimal("100.00"), corporate.reimbursableAmount());
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
    void expenseService_nonEmployeeActorsMayReviewClaimsTheyDoNotOwn() {
        AccessContext teamLead = new AccessContext("L1", null, "DEP", "DEP", UserRole.TEAM_LEAD,
                ResourceAction.CREATE, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true);
        ExpenseCardReview review = service().review(defaultClaim(List.of(validItem())), Map.of(),
                oneRate(), TODAY, teamLead);
        assertTrue(review.access().allowed());
        assertTrue(review.readyForApproval());
        assertFalse(review.auditRequired());
    }

    @Test
    void expenseService_offHoursActorsAreAuditedWithoutBeingDenied() {
        AccessContext offHours = new AccessContext("E1", null, "DEP", "DEP", UserRole.EMPLOYEE,
                ResourceAction.CREATE, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, false, false, false, false, false, true, true, true);
        ExpenseCardReview review = service().review(defaultClaim(List.of(validItem())), Map.of(),
                oneRate(), TODAY, offHours);
        assertTrue(review.access().allowed());
        assertTrue(review.access().auditRequired());
        assertTrue(review.readyForApproval());
        assertTrue(review.auditRequired());
    }

    @Test
    void expenseService_expenseAuditAloneFlagsTheWholeReviewForAudit() {
        // A bank-statement receipt makes the expense assessment itself audit-required even
        // though the access decision is clean and no corporate-card issues exist.
        ExpenseItem statementItem = customItem("i1", ExpenseCategory.MEAL, "10", PaymentMethod.PERSONAL_CARD,
                ReceiptType.BANK_STATEMENT, TODAY.minusDays(5), 0, false, false, false);
        ExpenseCardReview review = service().review(defaultClaim(List.of(statementItem)), Map.of(),
                oneRate(), TODAY, createActor("E1"));
        assertTrue(review.expense().auditRequired());
        assertTrue(review.auditRequired());
        assertTrue(review.issues().isEmpty());
        assertTrue(review.readyForApproval());
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

    // ==================== AccessPolicy branch-gap tests ====================

    @Test
    void accessPolicy_crossRegionCheckOnlyAppliesToExports() {
        AccessDecision offRegionView = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.TEAM,
                true, false, true, true, true, true, false, false, false, false, true, false, true));
        assertTrue(offRegionView.allowed());
        assertFalse(offRegionView.reasons().contains("cross-region export is not allowed"));
    }

    @Test
    void accessPolicy_legalHoldBlocksOnlyDeleteAndArchiveActions() {
        AccessDecision holdUpdate = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.UPDATE, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION, true, false, true, true, true, true, false, false, true, false,
                true, true, true));
        assertTrue(holdUpdate.allowed());
        assertTrue(holdUpdate.auditRequired());
        assertFalse(holdUpdate.reasons().contains("resource is under legal hold"));
    }

    @Test
    void accessPolicy_allowedDecisionWithoutNotesFallsBackToTheDefaultReason() {
        // HR reaches PUBLIC_DOCUMENT VIEW through the note-free fallback, TEAM scope grants
        // no note, INTERNAL sensitivity avoids the public-classification note, and with no
        // owner and null departments the notes list stays empty.
        AccessDecision decision = decide(actx("A", null, null, null, UserRole.HUMAN_RESOURCES,
                ResourceAction.VIEW, ResourceType.PUBLIC_DOCUMENT, Sensitivity.INTERNAL,
                AccessScope.TEAM, true, false, true, true, true, true, false, false, false, false,
                true, true, true));
        assertTrue(decision.allowed());
        assertFalse(decision.auditRequired());
        assertEquals(List.of("access granted by role and scope policy"), decision.reasons());
    }

    @Test
    void accessPolicy_hrAttendanceRecordGrantsWorkforceAccessAndPublicDocumentFallbackIsViewOnly() {
        AccessDecision attendance = decide(actx("A", null, UserRole.HUMAN_RESOURCES, ResourceAction.VIEW,
                ResourceType.ATTENDANCE_RECORD, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertTrue(attendance.allowed());
        assertTrue(attendance.reasons().contains("human resources role grants workforce-data access"));
        AccessDecision publicUpdate = decide(actx("A", null, UserRole.HUMAN_RESOURCES,
                ResourceAction.UPDATE, ResourceType.PUBLIC_DOCUMENT, Sensitivity.INTERNAL,
                AccessScope.TEAM));
        assertFalse(publicUpdate.allowed());
        assertEquals(List.of("role does not grant the requested action"), publicUpdate.reasons());
    }

    @Test
    void accessPolicy_financePublicDocumentFallbackIsViewOnly() {
        AccessDecision publicUpdate = decide(actx("A", null, UserRole.FINANCE, ResourceAction.UPDATE,
                ResourceType.PUBLIC_DOCUMENT, Sensitivity.INTERNAL, AccessScope.TEAM));
        assertFalse(publicUpdate.allowed());
        assertEquals(List.of("role does not grant the requested action"), publicUpdate.reasons());
    }

    @Test
    void accessPolicy_administratorProfileSharingFallsThroughForNonReadActions() {
        AccessDecision sharedProfile = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.SHARE, ResourceType.EMPLOYEE_PROFILE, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION));
        assertTrue(sharedProfile.allowed());
        assertTrue(sharedProfile.reasons()
                .contains("system administrator has technical administration access"));
    }

    @Test
    void accessPolicy_departmentScopeGrantsAdministratorsButNotOtherRoles() {
        AccessDecision admin = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR, ResourceAction.VIEW,
                ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertTrue(admin.allowed());
        AccessDecision employee = decide(actx("A", null, UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertFalse(employee.allowed());
        assertTrue(employee.reasons().contains("requested scope is outside the actor's authority"));
    }

    @Test
    void accessContext_sameDepartmentHandlesNullAndUnequalDepartments() {
        AccessContext nullActor = actx("A", null, null, "DEP", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true);
        assertFalse(nullActor.sameDepartment());
        AccessContext nullResource = actx("A", null, "DEP", null, UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true);
        assertFalse(nullResource.sameDepartment());
        AccessContext equal = actx("A", null, "DEP", "DEP", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true);
        assertTrue(equal.sameDepartment());
        AccessContext different = actx("A", null, "DEP", "OTHER", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, true);
        assertFalse(different.sameDepartment());
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
        // Prefixes force digits to double past nine (5, 6, 7, 9) on the checksum path;
        // expected networks follow the new CHECK_ORDER: Dankort's 4571-5019 first-four
        // window shadows Maestro at 16 digits, and RuPay's 60 prefix shadows 600000-609999.
        List<String[]> fixtures = List.of(
                new String[]{"4", "16", "VISA"},
                new String[]{"5", "16", "DANKORT"},
                new String[]{"560", "16", "MAESTRO"},
                new String[]{"6", "16", "RUPAY"},
                new String[]{"605", "16", "RUPAY"},
                new String[]{"607", "16", "RUPAY"},
                new String[]{"609", "16", "RUPAY"},
                new String[]{"615", "16", "MAESTRO"},
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

    // ==================== exact denial reasons (single-failure isolation) ====================

    @Test
    void accessPolicy_isolatedDenialsProduceExactReasons() {
        AccessDecision training = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.CONFIDENTIAL, AccessScope.OWN,
                true, false, true, true, true, true, false, false, false, false, true, true, false));
        assertEquals(List.of("security training is required"), training.reasons());
        AccessDecision mfa = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.RESTRICTED, AccessScope.OWN,
                true, false, false, true, true, true, false, false, false, false, true, true, true));
        assertEquals(List.of("restricted resource requires multi-factor authentication"), mfa.reasons());
        assertTrue(mfa.stepUpAuthenticationRequired());
        AccessDecision device = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.RESTRICTED, AccessScope.OWN,
                true, false, true, false, true, true, false, false, false, false, true, true, true));
        assertEquals(List.of("restricted resource requires a trusted device"), device.reasons());
        AccessDecision network = decide(actx("A", "A", UserRole.FINANCE, ResourceAction.SHARE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, false, true, false, false, false, false, true, true, true));
        assertEquals(List.of("export or sharing requires a trusted network"), network.reasons());
        AccessDecision region = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.EXPORT,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.TEAM,
                true, false, true, true, true, true, false, false, false, false, true, false, true));
        assertEquals(List.of("cross-region export is not allowed"), region.reasons());
        AccessDecision hours = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.RESTRICTED, AccessScope.OWN,
                true, false, true, true, true, false, false, false, false, false, true, true, true));
        assertEquals(List.of("restricted resource is unavailable outside business hours"), hours.reasons());
        AccessDecision delegation = decide(actx("A", "A", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, true, false, false, false, true, true, true));
        assertEquals(List.of("delegation has expired or is invalid"), delegation.reasons());
        AccessDecision holdArchive = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.ARCHIVE, ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.ORGANIZATION,
                true, false, true, true, true, true, false, false, true, false, true, true, true));
        assertEquals(List.of("resource is under legal hold"), holdArchive.reasons());
        AccessDecision holdDelete = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.DELETE, ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.ORGANIZATION,
                true, false, true, true, true, true, false, false, true, false, true, true, true));
        assertEquals(List.of("resource is under legal hold", "role does not grant the requested action"),
                holdDelete.reasons());
        AccessDecision archivedUpdate = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.UPDATE, ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.ORGANIZATION,
                true, false, true, true, true, true, false, false, false, true, true, true, true));
        assertEquals(List.of("archived resource cannot be updated"), archivedUpdate.reasons());
        AccessDecision restoreLive = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.RESTORE, ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.ORGANIZATION,
                true, false, true, true, true, true, false, false, false, false, true, true, true));
        assertEquals(List.of("only archived resources can be restored"), restoreLive.reasons());
        AccessDecision delegatedExport = decide(actx("A", "A", UserRole.AUDITOR, ResourceAction.EXPORT,
                ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, true, true, false, false, true, true, true));
        assertEquals(List.of("delegated permission cannot export resources"), delegatedExport.reasons());
        AccessDecision delegatedDelete = decide(actx("A", "A", UserRole.FINANCE, ResourceAction.DELETE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN,
                true, false, true, true, true, true, true, true, false, false, true, true, true));
        assertEquals(List.of("delegated permission cannot delete resources",
                "role does not grant the requested action"), delegatedDelete.reasons());
        AccessDecision config = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.VIEW,
                ResourceType.SYSTEM_CONFIGURATION, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertEquals(List.of("role does not grant the requested action",
                "system configuration requires administrator role"), config.reasons());
        AccessDecision publicPayroll = decide(actx("A", null, UserRole.FINANCE, ResourceAction.VIEW,
                ResourceType.PAYROLL, Sensitivity.PUBLIC, AccessScope.DEPARTMENT));
        assertEquals(List.of("payroll cannot be classified as public"), publicPayroll.reasons());
        AccessDecision shareProfile = decide(actx("A", null, UserRole.HUMAN_RESOURCES, ResourceAction.SHARE,
                ResourceType.EMPLOYEE_PROFILE, Sensitivity.INTERNAL, AccessScope.DEPARTMENT,
                true, false, true, true, true, true, false, false, false, false, false, true, true));
        assertEquals(List.of("profile sharing requires project membership"), shareProfile.reasons());
        AccessDecision contractApprove = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.APPROVE,
                ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertEquals(List.of("role does not grant the requested action",
                "contract approval requires finance or department manager"), contractApprove.reasons());
        AccessDecision guestApprove = decide(actx("A", "A", UserRole.GUEST, ResourceAction.APPROVE,
                ResourceType.PUBLIC_DOCUMENT, Sensitivity.PUBLIC, AccessScope.OWN));
        assertEquals(List.of("role does not grant the requested action"), guestApprove.reasons());
        AccessDecision guestInternal = decide(actx("A", "A", UserRole.GUEST, ResourceAction.VIEW,
                ResourceType.PUBLIC_DOCUMENT, Sensitivity.INTERNAL, AccessScope.OWN));
        assertEquals(List.of("role does not grant the requested action"), guestInternal.reasons());
        AccessDecision guestExpense = decide(actx("A", "A", UserRole.GUEST, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN));
        assertEquals(List.of("guest may only access public documents",
                "role does not grant the requested action"), guestExpense.reasons());
        AccessDecision adminPayroll = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.VIEW, ResourceType.PAYROLL, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertEquals(List.of("role does not grant the requested action"), adminPayroll.reasons());
        AccessDecision adminPayrollExport = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.EXPORT, ResourceType.PAYROLL, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertEquals(List.of("role does not grant the requested action"), adminPayrollExport.reasons());
        AccessDecision adminProfileApprove = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.APPROVE, ResourceType.EMPLOYEE_PROFILE, Sensitivity.INTERNAL,
                AccessScope.ORGANIZATION));
        assertEquals(List.of("role does not grant the requested action"), adminProfileApprove.reasons());
        AccessDecision deptPayroll = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER,
                ResourceAction.VIEW, ResourceType.PAYROLL, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertEquals(List.of("role does not grant the requested action"), deptPayroll.reasons());
        AccessDecision deptConfig = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER,
                ResourceAction.VIEW, ResourceType.SYSTEM_CONFIGURATION, Sensitivity.INTERNAL,
                AccessScope.DEPARTMENT));
        assertEquals(List.of("role does not grant the requested action",
                "system configuration requires administrator role"), deptConfig.reasons());
        AccessDecision deptAudit = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER,
                ResourceAction.VIEW, ResourceType.AUDIT_LOG, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertEquals(List.of("role does not grant the requested action"), deptAudit.reasons());
        AccessDecision deptDelete = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER,
                ResourceAction.DELETE, ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertEquals(List.of("role does not grant the requested action"), deptDelete.reasons());
        AccessDecision deptWrongScope = decide(actx("A", null, UserRole.DEPARTMENT_MANAGER,
                ResourceAction.VIEW, ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertEquals(List.of("role does not grant the requested action",
                "requested scope is outside the actor's authority"), deptWrongScope.reasons());
        AccessDecision deptOther = decide(actx("A", null, "D1", "D2", UserRole.DEPARTMENT_MANAGER,
                ResourceAction.VIEW, ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.DEPARTMENT,
                true, false, true, true, true, true, false, false, false, false, true, true, true));
        assertEquals(List.of("role does not grant the requested action",
                "requested scope is outside the actor's authority"), deptOther.reasons());
        AccessDecision employeeNoScope = decide(actx("A", "B", UserRole.EMPLOYEE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN));
        assertEquals(List.of("role does not grant the requested action",
                "requested scope is outside the actor's authority"), employeeNoScope.reasons());
        AccessDecision teamUpdate = decide(actx("A", null, UserRole.TEAM_LEAD, ResourceAction.UPDATE,
                ResourceType.LEAVE_REQUEST, Sensitivity.INTERNAL, AccessScope.TEAM));
        assertEquals(List.of("role does not grant the requested action"), teamUpdate.reasons());
        AccessDecision auditorConfig = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.VIEW,
                ResourceType.SYSTEM_CONFIGURATION, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertEquals(List.of("role does not grant the requested action",
                "system configuration requires administrator role"), auditorConfig.reasons());
    }

    @Test
    void accessPolicy_teamAndDepartmentScopeMembersAreExact() {
        AccessDecision hrTeam = decide(actx("A", null, UserRole.HUMAN_RESOURCES, ResourceAction.VIEW,
                ResourceType.LEAVE_REQUEST, Sensitivity.INTERNAL, AccessScope.TEAM));
        assertTrue(hrTeam.allowed());
        AccessDecision financeTeam = decide(actx("A", null, UserRole.FINANCE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.TEAM));
        assertTrue(financeTeam.allowed());
        AccessDecision auditorTeam = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.TEAM));
        assertTrue(auditorTeam.allowed());
        AccessDecision adminTeam = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR, ResourceAction.VIEW,
                ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.TEAM));
        assertTrue(adminTeam.allowed());
        AccessDecision hrDeptOther = decide(actx("A", null, "D1", "D2", UserRole.HUMAN_RESOURCES,
                ResourceAction.VIEW, ResourceType.PAYROLL, Sensitivity.CONFIDENTIAL, AccessScope.DEPARTMENT,
                true, false, true, true, true, true, false, false, false, false, true, true, true));
        assertTrue(hrDeptOther.allowed());
        AccessDecision financeDeptOther = decide(actx("A", null, "D1", "D2", UserRole.FINANCE,
                ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.CONFIDENTIAL,
                AccessScope.DEPARTMENT, true, false, true, true, true, true, false, false, false, false,
                true, true, true));
        assertTrue(financeDeptOther.allowed());
        AccessDecision auditorDeptOther = decide(actx("A", null, "D1", "D2", UserRole.AUDITOR,
                ResourceAction.VIEW, ResourceType.EXPENSE_CLAIM, Sensitivity.CONFIDENTIAL,
                AccessScope.DEPARTMENT, true, false, true, true, true, true, false, false, false, false,
                true, true, true));
        assertTrue(auditorDeptOther.allowed());
        AccessDecision adminDeptOther = decide(actx("A", null, "D1", "D2", UserRole.SYSTEM_ADMINISTRATOR,
                ResourceAction.VIEW, ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.DEPARTMENT,
                true, false, true, true, true, true, false, false, false, false, true, true, true));
        assertTrue(adminDeptOther.allowed());
        AccessDecision auditorDept = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertTrue(auditorDept.allowed());
        AccessDecision adminDept = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR, ResourceAction.VIEW,
                ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.DEPARTMENT));
        assertTrue(adminDept.allowed());
        AccessDecision financeOrg = decide(actx("A", null, UserRole.FINANCE, ResourceAction.VIEW,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertTrue(financeOrg.allowed());
        AccessDecision adminOrg = decide(actx("A", null, UserRole.SYSTEM_ADMINISTRATOR, ResourceAction.VIEW,
                ResourceType.CONTRACT, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertTrue(adminOrg.allowed());
        AccessDecision employeeOwnCreate = decide(actx("A", null, UserRole.EMPLOYEE, ResourceAction.CREATE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN));
        assertTrue(employeeOwnCreate.allowed());
    }

    @Test
    void expensePolicy_minimumTextLengthsSkipWarnings() {
        ExpenseAssessment twoCharMerchant = assess(List.of(
                new ExpenseItem("i1", ExpenseCategory.MEAL, new BigDecimal("10"), Currency.getInstance("CNY"),
                        TODAY.minusDays(5), "AB", "Business purpose here", ReceiptType.STANDARD_RECEIPT,
                        PaymentMethod.PERSONAL_CARD, 0, false, false, false)));
        assertFalse(twoCharMerchant.warnings().contains("merchant name is unusually short for item i1"));
        ExpenseAssessment eightCharPurpose = assess(List.of(
                new ExpenseItem("i1", ExpenseCategory.MEAL, new BigDecimal("10"), Currency.getInstance("CNY"),
                        TODAY.minusDays(5), "Merchant X", "12345678", ReceiptType.STANDARD_RECEIPT,
                        PaymentMethod.PERSONAL_CARD, 0, false, false, false)));
        assertFalse(eightCharPurpose.warnings().contains("business purpose is too brief for item i1"));
        assertEquals(List.of(), eightCharPurpose.warnings());
    }

    @Test
    void workflowEngine_cancelReasonAndTimestampBoundaries() {
        ApprovalStep step = step("s", Set.of("A"), StepMode.ANY_ONE, 1, false, false, null);
        WorkflowSnapshot pending = ENGINE.start("W1", "R1", List.of(step), T0, false, true);
        IllegalArgumentException fourChars = assertThrows(IllegalArgumentException.class,
                () -> ENGINE.cancel(pending, true, "abcd", T1));
        assertEquals("cancellation reason must contain at least five characters", fourChars.getMessage());
        WorkflowSnapshot fiveChars = ENGINE.cancel(pending, true, "abcde", T1);
        assertEquals(WorkflowState.CANCELLED, fiveChars.state());
        WorkflowSnapshot sameInstant = ENGINE.act(pending, new WorkflowAction("A",
                ApprovalDecision.APPROVE, null, T0));
        assertEquals(WorkflowState.APPROVED, sameInstant.state());
        WorkflowSnapshot pendingWithdraw = ENGINE.start("W2", "R1", List.of(step), T0, false, true);
        assertEquals(WorkflowState.WITHDRAWN, ENGINE.withdraw(pendingWithdraw, "R1", T0).state());
    }

    // ==================== exact failure messages and audit flags ====================

    @Test
    void accessPolicy_auditFlagIsSetForAuditLogResources() {
        AccessDecision viewAuditLog = decide(actx("A", null, UserRole.AUDITOR, ResourceAction.VIEW,
                ResourceType.AUDIT_LOG, Sensitivity.INTERNAL, AccessScope.ORGANIZATION));
        assertTrue(viewAuditLog.allowed());
        assertTrue(viewAuditLog.auditRequired());
    }

    @Test
    void workflowEngine_actAndCancelMessagesIdentifyEachCheck() {
        WorkflowSnapshot pending = ENGINE.start("W1", "R1", List.of(step("s", Set.of("A"),
                StepMode.ANY_ONE, 1, false, false, null)), T0, false, true);
        IllegalArgumentException before = assertThrows(IllegalArgumentException.class,
                () -> ENGINE.act(pending, new WorkflowAction("A", ApprovalDecision.APPROVE, null,
                        T0.minusSeconds(1))));
        assertEquals("action time cannot precede the previous update", before.getMessage());
        IllegalArgumentException unauthorized = assertThrows(IllegalArgumentException.class,
                () -> ENGINE.act(pending, new WorkflowAction("X", ApprovalDecision.APPROVE, null, T1)));
        assertEquals("actor is not authorized for the current step", unauthorized.getMessage());
        IllegalArgumentException blankRequester = assertThrows(IllegalArgumentException.class,
                () -> ENGINE.start("W", "  ", List.of(step("s", Set.of("A"), StepMode.ANY_ONE, 1, false,
                        false, null)), T0, false, true));
        assertEquals("requesterId must not be blank", blankRequester.getMessage());
        WorkflowSnapshot approved = ENGINE.start("W1", "R1", List.of(step("s", Set.of("R1"),
                StepMode.ANY_ONE, 1, false, true, null)), T0, false, true);
        IllegalStateException terminalCancel = assertThrows(IllegalStateException.class,
                () -> ENGINE.cancel(approved, true, "reason", T1));
        assertEquals("terminal workflow cannot be cancelled", terminalCancel.getMessage());
        IllegalArgumentException blankReason = assertThrows(IllegalArgumentException.class,
                () -> ENGINE.cancel(pending, true, "   ", T1));
        assertEquals("cancellation reason must contain at least five characters", blankReason.getMessage());
    }

    @Test
    void workflowEngine_withdrawAndDelegateMessagesIdentifyEachCheck() {
        ApprovalStep step = step("s", Set.of("A", "B"), StepMode.REQUIRED_COUNT, 2, true, false, null);
        WorkflowSnapshot pending = ENGINE.start("W1", "R1", List.of(step), T0, false, true);
        IllegalArgumentException notRequester = assertThrows(IllegalArgumentException.class,
                () -> ENGINE.withdraw(pending, "X", T1));
        assertEquals("only the requester may withdraw the workflow", notRequester.getMessage());
        IllegalArgumentException blankWithdraw = assertThrows(IllegalArgumentException.class,
                () -> ENGINE.withdraw(pending, "  ", T1));
        assertEquals("actorId must not be blank", blankWithdraw.getMessage());
        IllegalArgumentException delegateBefore = assertThrows(IllegalArgumentException.class,
                () -> ENGINE.delegate(pending, "A", "C", T0.minusSeconds(1)));
        assertEquals("delegation time cannot precede the previous update", delegateBefore.getMessage());
        IllegalArgumentException delegateNotApprover = assertThrows(IllegalArgumentException.class,
                () -> ENGINE.delegate(pending, "X", "C", T1));
        assertEquals("delegating actor is not an approver", delegateNotApprover.getMessage());
        IllegalArgumentException delegateBlank = assertThrows(IllegalArgumentException.class,
                () -> ENGINE.delegate(pending, "A", "  ", T1));
        assertEquals("toActor must not be blank", delegateBlank.getMessage());
        WorkflowSnapshot requesterApprover = ENGINE.start("W1", "R1", List.of(step("s", Set.of("R1", "A"),
                StepMode.ANY_ONE, 1, true, true, null)), T0, false, true);
        IllegalArgumentException toRequester = assertThrows(IllegalArgumentException.class,
                () -> ENGINE.delegate(requesterApprover, "R1", "R1", T1));
        assertEquals("cannot delegate approval to the requester", toRequester.getMessage());
    }

    @Test
    void workflowEngine_requesterCannotActOnSkipRequesterStepsButOtherApproversMay() {
        WorkflowSnapshot started = ENGINE.start("W1", "R1", List.of(
                step("s", Set.of("R1", "A"), StepMode.REQUIRED_COUNT, 1, false, true, null)), T0, false, true);
        assertEquals(WorkflowState.PENDING, started.state());
        IllegalArgumentException self = assertThrows(IllegalArgumentException.class,
                () -> ENGINE.act(started, new WorkflowAction("R1", ApprovalDecision.APPROVE, "ok", T1)));
        assertEquals("requester cannot approve this step", self.getMessage());
        WorkflowSnapshot approved = ENGINE.act(started,
                new WorkflowAction("A", ApprovalDecision.APPROVE, "ok", T1));
        assertEquals(WorkflowState.APPROVED, approved.state());
    }

    @Test
    void workflowEngine_delegatingToTheRequesterIsBlockedOnlyWhenTheStepSkipsTheRequester() {
        WorkflowSnapshot plain = ENGINE.start("W1", "R1", List.of(
                step("s", Set.of("A"), StepMode.ANY_ONE, 1, true, false, null)), T0, false, true);
        WorkflowSnapshot delegated = ENGINE.delegate(plain, "A", "R1", T1);
        assertEquals(WorkflowState.IN_PROGRESS, delegated.state());
        assertEquals(Set.of("R1"), delegated.currentStep().step().approverIds());
        WorkflowSnapshot skipping = ENGINE.start("W2", "R1", List.of(
                step("s", Set.of("R1", "A"), StepMode.REQUIRED_COUNT, 1, true, true, null)), T0, false, true);
        IllegalArgumentException blocked = assertThrows(IllegalArgumentException.class,
                () -> ENGINE.delegate(skipping, "A", "R1", T1));
        assertEquals("cannot delegate approval to the requester", blocked.getMessage());
    }

    @Test
    void stepProgress_andApprovalStepMessagesIdentifyEachCheck() {
        ApprovalStep step = step("s", Set.of("A", "B"), StepMode.REQUIRED_COUNT, 2, true, false, null);
        StepProgress empty = StepProgress.empty(step);
        IllegalArgumentException blankActor = assertThrows(IllegalArgumentException.class,
                () -> empty.decide("  ", ApprovalDecision.APPROVE));
        assertEquals("actorId must not be blank", blankActor.getMessage());
        IllegalArgumentException unknownActor = assertThrows(IllegalArgumentException.class,
                () -> empty.decide("X", ApprovalDecision.APPROVE));
        assertEquals("actor is not an approver for the current step", unknownActor.getMessage());
        IllegalArgumentException blankTo = assertThrows(IllegalArgumentException.class,
                () -> step.delegate("A", "  "));
        assertEquals("delegate must not be blank", blankTo.getMessage());
        IllegalArgumentException sameActor = assertThrows(IllegalArgumentException.class,
                () -> step.delegate("A", "A"));
        assertEquals("cannot delegate to the same actor", sameActor.getMessage());
        IllegalArgumentException alreadyApprover = assertThrows(IllegalArgumentException.class,
                () -> step.delegate("A", "B"));
        assertEquals("delegate is already an approver", alreadyApprover.getMessage());
        IllegalArgumentException notApprover = assertThrows(IllegalArgumentException.class,
                () -> step.delegate("X", "C"));
        assertEquals("delegating actor is not an approver", notApprover.getMessage());
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

    // ------------------------------------------------------------------
    // Attendance + card date subsystem (provincial additions)
    // ------------------------------------------------------------------
    @Nested
    class AttendanceCardDateModuleTests {

    /** 2026-03-02 is a Monday; 2026-03-01 Sunday; 2026-03-07 Saturday. */
    private static final LocalDate MONDAY = LocalDate.of(2026, 3, 2);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 3, 7);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 3, 1);

    /** Fixed clock: "today" is 2026-01-15 (UTC), i.e. YearMonth 2026-01. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);

    private static final String LUHN_OK_12 = "411111111117";
    private static final String LUHN_OK_16 = "4111111111111111";
    private static final String LUHN_OK_19 = "4111111111111111110";
    private static final String LUHN_BAD_16 = "4111111111111110";

    // ------------------------------------------------------------------
    // Factories (hard-coded values only, no production calls)
    // ------------------------------------------------------------------

    private static Shift standardShift() {
        return new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(17, 0), 10, 60, 240);
    }

    private static Shift flexibleShift() {
        return new Shift(ShiftType.FLEXIBLE, LocalTime.of(9, 0), LocalTime.of(17, 0), 10, 60, 240);
    }

    /**
     * Overnight 22:00-06:00 shift typed STANDARD. (A NIGHT-typed shift cannot
     * be constructed at all: see ShiftTests for the compact-constructor quirk.)
     */
    private static Shift overnightShift() {
        return new Shift(ShiftType.STANDARD, LocalTime.of(22, 0), LocalTime.of(6, 0), 10, 45, 60);
    }

    private static Shift onCallShift() {
        return new Shift(ShiftType.ON_CALL, LocalTime.of(9, 0), LocalTime.of(17, 0), 10, 60, 240);
    }

    /** Wide grace so a late-morning arrival still counts as on time. */
    private static Shift longGraceShift() {
        return new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(17, 0), 60, 60, 240);
    }

    /** 09:00-18:00, scheduled 480 minutes after the 60-minute break. */
    private static Shift longDayShift() {
        return new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(18, 0), 10, 60, 480);
    }

    /** 09:00-20:00 so a mid-afternoon clock-out is still under 360 worked minutes. */
    private static Shift lateDayShift() {
        return new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(20, 0), 10, 60, 240);
    }

    private static PunchRecord punch(LocalDateTime in, LocalDateTime out, int breakMinutes,
            boolean remote, boolean verified, boolean manual, boolean approved) {
        return new PunchRecord(in, out, breakMinutes, remote, verified, manual, approved);
    }

    /** Clean punch: 08:55 in (inside grace), 16:55 out (inside grace), full break. */
    private static PunchRecord cleanPunch() {
        return punch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 55), 60, false, true, false, false);
    }

    private static PunchRecord cleanPunch(LocalDateTime in, LocalDateTime out) {
        return punch(in, out, 60, false, true, false, false);
    }

    private static AttendanceInput input(Shift shift, PunchRecord punch, boolean scheduledWorkDay,
            boolean publicHoliday, boolean approvedLeave, boolean businessTrip, boolean severeWeather,
            boolean overtimeRequested, boolean overtimeApproved, int monthlyLateCount,
            int consecutiveWorkDays) {
        return new AttendanceInput("E-001", MONDAY, shift, punch, EmploymentMode.OFFICE,
                scheduledWorkDay, publicHoliday, approvedLeave, businessTrip, severeWeather,
                overtimeRequested, overtimeApproved, monthlyLateCount, consecutiveWorkDays);
    }

    /** Default weekday, scheduled, OFFICE employee, no flags, no lateness history. */
    private static AttendanceInput dayInput(Shift shift, PunchRecord punch) {
        return input(shift, punch, true, false, false, false, false, false, false, 0, 0);
    }

    private static AttendanceInput modeInput(EmploymentMode mode, PunchRecord punch) {
        return new AttendanceInput("E-001", MONDAY, standardShift(), punch, mode,
                true, false, false, false, false, false, false, 0, 0);
    }

    private static AttendanceInput otInput(Shift shift, LocalDate date, EmploymentMode mode,
            PunchRecord punch, boolean requested, boolean approved, boolean holiday,
            boolean scheduledDay, int consecutiveDays) {
        return new AttendanceInput("E-001", date, shift, punch, mode, scheduledDay, holiday,
                false, false, false, requested, approved, 0, consecutiveDays);
    }

    /** Approved-overtime input for direct OvertimePolicy.evaluate calls. */
    private static AttendanceInput approvedOtInput(Shift shift) {
        return otInput(shift, MONDAY, EmploymentMode.OFFICE, null, true, true, false, true, 0);
    }

    /** Asserts all nine AttendanceResult components, lists as exact ordered values. */
    private static void assertResult(AttendanceResult r, List<AttendanceCode> codes, int worked,
            int late, int early, int overtime, int payable, int penalty, boolean review,
            List<String> warnings) {
        assertEquals(codes, r.codes());
        assertEquals(worked, r.workedMinutes());
        assertEquals(late, r.lateMinutes());
        assertEquals(early, r.earlyLeaveMinutes());
        assertEquals(overtime, r.overtimeMinutes());
        assertEquals(payable, r.payableOvertimeMinutes());
        assertEquals(penalty, r.penaltyPoints());
        assertEquals(review, r.requiresManagerReview());
        assertEquals(warnings, r.warnings());
    }

    // ------------------------------------------------------------------
    // Enumerations: every constant, in declaration order
    // ------------------------------------------------------------------

    @Nested
    class EnumTests {

        @Test
        void attendanceCodeHasAllSixteenValuesInOrder() {
            List<String> names = Arrays.stream(AttendanceCode.values()).map(Enum::name).toList();
            assertEquals(List.of("NORMAL", "REST_DAY", "APPROVED_LEAVE", "BUSINESS_TRIP",
                    "ABSENT", "MISSING_CLOCK_IN", "MISSING_CLOCK_OUT", "LATE", "SERIOUSLY_LATE",
                    "EARLY_LEAVE", "INSUFFICIENT_BREAK", "INSUFFICIENT_HOURS",
                    "LOCATION_UNVERIFIED", "MANUAL_CORRECTION_REQUIRED", "OVERTIME",
                    "EXCESSIVE_OVERTIME"), names);
            assertEquals(AttendanceCode.NORMAL, AttendanceCode.valueOf("NORMAL"));
            assertEquals(AttendanceCode.EXCESSIVE_OVERTIME,
                    AttendanceCode.valueOf("EXCESSIVE_OVERTIME"));
        }

        @Test
        void employmentModeHasAllFourValuesInOrder() {
            List<String> names = Arrays.stream(EmploymentMode.values()).map(Enum::name).toList();
            assertEquals(List.of("OFFICE", "REMOTE", "HYBRID", "FIELD"), names);
            assertEquals(EmploymentMode.HYBRID, EmploymentMode.valueOf("HYBRID"));
        }

        @Test
        void shiftTypeHasAllFiveValuesInOrder() {
            List<String> names = Arrays.stream(ShiftType.values()).map(Enum::name).toList();
            assertEquals(List.of("STANDARD", "FLEXIBLE", "NIGHT", "PART_TIME", "ON_CALL"), names);
            assertEquals(ShiftType.PART_TIME, ShiftType.valueOf("PART_TIME"));
        }
    }

    // ------------------------------------------------------------------
    // PunchRecord: compact-constructor validation and accessors
    // ------------------------------------------------------------------

    @Nested
    class PunchRecordTests {

        @Test
        void recordExposesAllSevenComponents() {
            LocalDateTime in = MONDAY.atTime(9, 0);
            LocalDateTime out = MONDAY.atTime(17, 0);
            PunchRecord r = punch(in, out, 30, true, false, true, true);
            assertEquals(in, r.clockIn());
            assertEquals(out, r.clockOut());
            assertEquals(30, r.breakMinutes());
            assertTrue(r.remotePunch());
            assertFalse(r.locationVerified());
            assertTrue(r.manualEntry());
            assertTrue(r.correctionApproved());
            assertEquals(r, new PunchRecord(in, out, 30, true, false, true, true));
            assertNotEquals(r, new PunchRecord(in, out, 31, true, false, true, true));
        }

        @Test
        void rejectsNegativeBreakMinutes() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> punch(null, null, -1, false, true, false, false));
            assertEquals("breakMinutes must be between 0 and 480", ex.getMessage());
        }

        @Test
        void rejectsBreakMinutesAbove480() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> punch(null, null, 481, false, true, false, false));
            assertEquals("breakMinutes must be between 0 and 480", ex.getMessage());
        }

        @Test
        void acceptsBreakBoundariesZeroAnd480() {
            assertEquals(0, punch(null, null, 0, false, true, false, false).breakMinutes());
            assertEquals(480, punch(null, null, 480, false, true, false, false).breakMinutes());
        }

        @Test
        void rejectsClockOutBeforeClockIn() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> punch(MONDAY.atTime(10, 0), MONDAY.atTime(9, 59), 0,
                            false, true, false, false));
            assertEquals("clockOut must not be before clockIn", ex.getMessage());
        }

        @Test
        void acceptsClockOutEqualToClockIn() {
            LocalDateTime same = MONDAY.atTime(10, 0);
            assertEquals(same, punch(same, same, 0, false, true, false, false).clockOut());
        }

        @Test
        void acceptsNullClockInAndNullClockOut() {
            PunchRecord r = punch(null, null, 0, false, true, false, false);
            assertNull(r.clockIn());
            assertNull(r.clockOut());
        }

        @Test
        void rejectsApprovedCorrectionWithoutManualEntry() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> punch(null, null, 0, false, true, false, true));
            assertEquals("only manual entries can be approved corrections", ex.getMessage());
        }

        @Test
        void acceptsApprovedCorrectionWithManualEntry() {
            assertTrue(punch(null, null, 0, false, true, true, true).correctionApproved());
        }
    }

    // ------------------------------------------------------------------
    // Shift: compact-constructor validation and schedule math
    // ------------------------------------------------------------------

    @Nested
    class ShiftTests {

        @Test
        void recordExposesAllSixComponents() {
            Shift s = new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(17, 0), 15, 45, 120);
            assertEquals(ShiftType.STANDARD, s.type());
            assertEquals(LocalTime.of(9, 0), s.start());
            assertEquals(LocalTime.of(17, 0), s.end());
            assertEquals(15, s.graceMinutes());
            assertEquals(45, s.requiredBreakMinutes());
            assertEquals(120, s.minimumWorkMinutes());
            assertFalse(s.crossesMidnight());
            assertEquals(s, new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                    LocalTime.of(17, 0), 15, 45, 120));
        }

        @Test
        void standardOvernightShiftCrossesMidnight() {
            assertTrue(overnightShift().crossesMidnight());
        }

        @Test
        void dayShiftDoesNotCrossMidnight() {
            assertFalse(standardShift().crossesMidnight());
        }

        @Test
        void rejectsNullType() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new Shift(null, LocalTime.of(9, 0), LocalTime.of(17, 0), 10, 60, 240));
            assertEquals("type must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullStart() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new Shift(ShiftType.STANDARD, null, LocalTime.of(17, 0), 10, 60, 240));
            assertEquals("start must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullEnd() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), null, 10, 60, 240));
            assertEquals("end must not be null", ex.getMessage());
        }

        @Test
        void rejectsIdenticalStartAndEnd() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                            LocalTime.of(9, 0), 10, 60, 240));
            assertEquals("shift start and end must differ", ex.getMessage());
        }

        @Test
        void rejectsNegativeGraceMinutes() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                            LocalTime.of(17, 0), -1, 60, 240));
            assertEquals("graceMinutes must be between 0 and 120", ex.getMessage());
        }

        @Test
        void rejectsGraceMinutesAbove120() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                            LocalTime.of(17, 0), 121, 60, 240));
            assertEquals("graceMinutes must be between 0 and 120", ex.getMessage());
        }

        @Test
        void acceptsGraceBoundariesZeroAnd120() {
            assertEquals(0, new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                    LocalTime.of(17, 0), 0, 60, 240).graceMinutes());
            assertEquals(120, new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                    LocalTime.of(17, 0), 120, 60, 240).graceMinutes());
        }

        @Test
        void rejectsNegativeRequiredBreak() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                            LocalTime.of(17, 0), 10, -1, 240));
            assertEquals("requiredBreakMinutes must be between 0 and 180", ex.getMessage());
        }

        @Test
        void rejectsRequiredBreakAbove180() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                            LocalTime.of(17, 0), 10, 181, 240));
            assertEquals("requiredBreakMinutes must be between 0 and 180", ex.getMessage());
        }

        @Test
        void acceptsRequiredBreakBoundariesZeroAnd180() {
            assertEquals(0, new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                    LocalTime.of(17, 0), 10, 0, 240).requiredBreakMinutes());
            assertEquals(180, new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                    LocalTime.of(17, 0), 10, 180, 240).requiredBreakMinutes());
        }

        @Test
        void rejectsMinimumWorkBelow60() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                            LocalTime.of(17, 0), 10, 60, 59));
            assertEquals("minimumWorkMinutes must be between 60 and 960", ex.getMessage());
        }

        @Test
        void rejectsMinimumWorkAbove960() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                            LocalTime.of(17, 0), 10, 60, 961));
            assertEquals("minimumWorkMinutes must be between 60 and 960", ex.getMessage());
        }

        @Test
        void acceptsMinimumWorkBoundaries60And960() {
            assertEquals(60, new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                    LocalTime.of(17, 0), 10, 60, 60).minimumWorkMinutes());
            assertEquals(960, new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                    LocalTime.of(17, 0), 10, 60, 960).minimumWorkMinutes());
        }

        @Test
        void rejectsPartTimeRequiringMoreThanSixHours() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Shift(ShiftType.PART_TIME, LocalTime.of(9, 0),
                            LocalTime.of(17, 0), 10, 60, 361));
            assertEquals("part-time shift cannot require more than six hours", ex.getMessage());
        }

        @Test
        void acceptsPartTimeAtExactlySixHours() {
            assertEquals(360, new Shift(ShiftType.PART_TIME, LocalTime.of(9, 0),
                    LocalTime.of(17, 0), 10, 60, 360).minimumWorkMinutes());
        }

        @Test
        void acceptsNonPartTimeShiftAboveSixHours() {
            assertEquals(500, new Shift(ShiftType.STANDARD, LocalTime.of(9, 0),
                    LocalTime.of(17, 0), 10, 60, 500).minimumWorkMinutes());
        }

        /**
         * Production quirk: in a record compact constructor the fields are
         * assigned only AFTER the body, so the NIGHT check invokes
         * crossesMidnight() while this.end is still null. Constructing any
         * NIGHT-typed shift therefore throws NullPointerException (JVM text,
         * not asserted), and the IllegalArgumentException
         * "night shift must cross midnight" is unreachable dead code.
         */
        @Test
        void crossingNightShiftConstructionThrowsBecauseEndFieldIsStillNull() {
            assertThrows(NullPointerException.class,
                    () -> new Shift(ShiftType.NIGHT, LocalTime.of(22, 0),
                            LocalTime.of(6, 0), 10, 45, 60));
        }

        @Test
        void nonCrossingNightShiftHitsTheSameNullEndBeforeTheExplicitCheck() {
            assertThrows(NullPointerException.class,
                    () -> new Shift(ShiftType.NIGHT, LocalTime.of(9, 0),
                            LocalTime.of(17, 0), 10, 60, 240));
        }

        @Test
        void scheduledStartAndEndStayOnSameDayForDayShift() {
            assertEquals(LocalDateTime.of(2026, 3, 2, 9, 0), standardShift().scheduledStart(MONDAY));
            assertEquals(LocalDateTime.of(2026, 3, 2, 17, 0), standardShift().scheduledEnd(MONDAY));
        }

        @Test
        void scheduledEndRollsToNextDayForCrossMidnightShift() {
            assertEquals(LocalDateTime.of(2026, 3, 2, 22, 0), overnightShift().scheduledStart(MONDAY));
            assertEquals(LocalDateTime.of(2026, 3, 3, 6, 0), overnightShift().scheduledEnd(MONDAY));
        }

        @Test
        void scheduledMinutesSubtractBreakOnBothShiftShapes() {
            assertEquals(420, standardShift().scheduledMinutes(MONDAY));
            assertEquals(435, overnightShift().scheduledMinutes(MONDAY));
        }
    }

    // ------------------------------------------------------------------
    // AttendanceInput: compact-constructor validation and accessors
    // ------------------------------------------------------------------

    @Nested
    class AttendanceInputTests {

        private AttendanceInput fullInput() {
            return new AttendanceInput(" E-7 ", MONDAY, standardShift(), cleanPunch(),
                    EmploymentMode.HYBRID, true, true, false, false, true, true, true, 2, 4);
        }

        @Test
        void recordExposesAllFourteenComponents() {
            AttendanceInput in = fullInput();
            assertEquals("E-7", in.employeeId());
            assertEquals(MONDAY, in.workDate());
            assertEquals(standardShift(), in.shift());
            assertEquals(cleanPunch(), in.punch());
            assertEquals(EmploymentMode.HYBRID, in.employmentMode());
            assertTrue(in.scheduledWorkDay());
            assertTrue(in.publicHoliday());
            assertFalse(in.approvedLeave());
            assertFalse(in.businessTrip());
            assertTrue(in.severeWeather());
            assertTrue(in.overtimeRequested());
            assertTrue(in.overtimeApproved());
            assertEquals(2, in.monthlyLateCount());
            assertEquals(4, in.consecutiveWorkDays());
        }

        @Test
        void trimsSurroundingWhitespaceFromEmployeeId() {
            assertEquals("E-7", fullInput().employeeId());
        }

        @Test
        void rejectsNullEmployeeId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new AttendanceInput(null, MONDAY, standardShift(), null,
                            EmploymentMode.OFFICE, true, false, false, false, false,
                            false, false, 0, 0));
            assertEquals("employeeId must not be blank", ex.getMessage());
        }

        @Test
        void rejectsBlankEmployeeId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new AttendanceInput("   ", MONDAY, standardShift(), null,
                            EmploymentMode.OFFICE, true, false, false, false, false,
                            false, false, 0, 0));
            assertEquals("employeeId must not be blank", ex.getMessage());
        }

        @Test
        void rejectsNullWorkDate() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new AttendanceInput("E-1", null, standardShift(), null,
                            EmploymentMode.OFFICE, true, false, false, false, false,
                            false, false, 0, 0));
            assertEquals("workDate must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullShift() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new AttendanceInput("E-1", MONDAY, null, null,
                            EmploymentMode.OFFICE, true, false, false, false, false,
                            false, false, 0, 0));
            assertEquals("shift must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullEmploymentMode() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new AttendanceInput("E-1", MONDAY, standardShift(), null,
                            null, true, false, false, false, false, false, false, 0, 0));
            assertEquals("employmentMode must not be null", ex.getMessage());
        }

        @Test
        void rejectsNegativeMonthlyLateCount() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new AttendanceInput("E-1", MONDAY, standardShift(), null,
                            EmploymentMode.OFFICE, true, false, false, false, false,
                            false, false, -1, 0));
            assertEquals("monthlyLateCount must not be negative", ex.getMessage());
        }

        @Test
        void rejectsNegativeConsecutiveWorkDays() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new AttendanceInput("E-1", MONDAY, standardShift(), null,
                            EmploymentMode.OFFICE, true, false, false, false, false,
                            false, false, 0, -1));
            assertEquals("consecutiveWorkDays must not be negative", ex.getMessage());
        }

        @Test
        void rejectsApprovedOvertimeWithoutRequest() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new AttendanceInput("E-1", MONDAY, standardShift(), null,
                            EmploymentMode.OFFICE, true, false, false, false, false,
                            false, true, 0, 0));
            assertEquals("overtime cannot be approved before it is requested", ex.getMessage());
        }

        @Test
        void acceptsOvertimeNeitherRequestedNorApproved() {
            AttendanceInput in = new AttendanceInput("E-1", MONDAY, standardShift(), null,
                    EmploymentMode.OFFICE, true, false, false, false, false,
                    false, false, 0, 0);
            assertFalse(in.overtimeRequested());
            assertFalse(in.overtimeApproved());
        }

        @Test
        void rejectsLeaveAndBusinessTripOnSameDay() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new AttendanceInput("E-1", MONDAY, standardShift(), null,
                            EmploymentMode.OFFICE, true, false, true, true, false,
                            false, false, 0, 0));
            assertEquals("leave and business trip cannot apply to the same day", ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // AttendanceResult: compact-constructor validation
    // ------------------------------------------------------------------

    @Nested
    class AttendanceResultTests {

        private AttendanceResult result(int worked, int late, int early, int overtime,
                int payable, int penalty) {
            return new AttendanceResult(List.of(AttendanceCode.NORMAL), worked, late, early,
                    overtime, payable, penalty, false, List.of());
        }

        @Test
        void recordExposesAllNineComponentsAndCopiesListsDefensively() {
            AttendanceResult r = new AttendanceResult(
                    List.of(AttendanceCode.LATE, AttendanceCode.EARLY_LEAVE),
                    420, 5, 10, 60, 30, 2, true, List.of("w1", "w2"));
            assertEquals(List.of(AttendanceCode.LATE, AttendanceCode.EARLY_LEAVE), r.codes());
            assertEquals(420, r.workedMinutes());
            assertEquals(5, r.lateMinutes());
            assertEquals(10, r.earlyLeaveMinutes());
            assertEquals(60, r.overtimeMinutes());
            assertEquals(30, r.payableOvertimeMinutes());
            assertEquals(2, r.penaltyPoints());
            assertTrue(r.requiresManagerReview());
            assertEquals(List.of("w1", "w2"), r.warnings());
            assertThrows(UnsupportedOperationException.class, () -> r.codes().add(AttendanceCode.ABSENT));
            assertThrows(UnsupportedOperationException.class, () -> r.warnings().add("x"));
        }

        @Test
        void rejectsNullCodes() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new AttendanceResult(null, 0, 0, 0, 0, 0, 0, false, List.of()));
            assertNull(ex.getMessage());
        }

        @Test
        void rejectsNullWarnings() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new AttendanceResult(List.of(AttendanceCode.NORMAL), 0, 0, 0, 0, 0, 0,
                            false, null));
            assertNull(ex.getMessage());
        }

        @Test
        void rejectsEmptyCodes() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new AttendanceResult(List.of(), 0, 0, 0, 0, 0, 0, false, List.of()));
            assertEquals("at least one attendance code is required", ex.getMessage());
        }

        @Test
        void rejectsNegativeWorkedMinutes() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> result(-1, 0, 0, 0, 0, 0));
            assertEquals("attendance metrics must not be negative", ex.getMessage());
        }

        @Test
        void rejectsNegativeLateMinutes() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> result(0, -1, 0, 0, 0, 0));
            assertEquals("attendance metrics must not be negative", ex.getMessage());
        }

        @Test
        void rejectsNegativeEarlyLeaveMinutes() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> result(0, 0, -1, 0, 0, 0));
            assertEquals("attendance metrics must not be negative", ex.getMessage());
        }

        @Test
        void rejectsNegativeOvertimeMinutes() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> result(0, 0, 0, -1, 0, 0));
            assertEquals("attendance metrics must not be negative", ex.getMessage());
        }

        @Test
        void rejectsNegativePayableOvertimeMinutes() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> result(0, 0, 0, 0, -1, 0));
            assertEquals("attendance metrics must not be negative", ex.getMessage());
        }

        @Test
        void rejectsNegativePenaltyPoints() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> result(0, 0, 0, 0, 0, -1));
            assertEquals("attendance metrics must not be negative", ex.getMessage());
        }

        @Test
        void acceptsAllZeroMetricsWithSingleCode() {
            AttendanceResult r = result(0, 0, 0, 0, 0, 0);
            assertEquals(List.of(AttendanceCode.NORMAL), r.codes());
            assertEquals(0, r.penaltyPoints());
        }

        @Test
        void rejectsPayableOvertimeAboveActual() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> result(0, 0, 0, 0, 1, 0));
            assertEquals("payable overtime cannot exceed actual overtime", ex.getMessage());
        }

        @Test
        void acceptsPayableOvertimeEqualToActual() {
            assertEquals(30, result(0, 0, 0, 30, 30, 0).payableOvertimeMinutes());
        }
    }

    // ------------------------------------------------------------------
    // OvertimePolicy: direct evaluate() tests (all expected values by hand)
    // ------------------------------------------------------------------

    @Nested
    class OvertimePolicyTests {

        private final OvertimePolicy policy = new OvertimePolicy();

        private OvertimeResult eval(AttendanceInput in, int worked, int raw) {
            return policy.evaluate(in, worked, raw);
        }

        private void assertOt(OvertimeResult r, int actual, int payable, double factor,
                boolean compensatory, boolean review, String reason) {
            assertEquals(actual, r.actualMinutes());
            assertEquals(payable, r.payableMinutes());
            assertEquals(factor, r.compensationFactor());
            assertEquals(compensatory, r.compensatoryLeave());
            assertEquals(review, r.managerReview());
            assertEquals(reason, r.reason());
            assertEquals(new OvertimeResult(actual, payable, factor, compensatory, review, reason), r);
        }

        @Test
        void rejectsNullInput() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> eval(null, 0, 0));
            assertEquals("input must not be null", ex.getMessage());
        }

        @Test
        void rejectsNegativeWorkedMinutes() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> eval(approvedOtInput(standardShift()), -1, 0));
            assertEquals("minutes must not be negative", ex.getMessage());
        }

        @Test
        void rejectsNegativeRawOvertimeMinutes() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> eval(approvedOtInput(standardShift()), 0, -1));
            assertEquals("minutes must not be negative", ex.getMessage());
        }

        @Test
        void returnsNoOvertimeWhenRawOvertimeIsZero() {
            assertOt(eval(approvedOtInput(standardShift()), 420, 0),
                    0, 0, 0.0, false, false, "no overtime");
        }

        @Test
        void flagsUnrequestedOvertimeForManagerReview() {
            AttendanceInput in = otInput(standardShift(), MONDAY, EmploymentMode.OFFICE, null,
                    false, false, false, true, 0);
            assertOt(eval(in, 465, 45), 45, 0, 0.0, false, true, "overtime was not requested");
        }

        @Test
        void holdsRequestedButUnapprovedOvertimeForReview() {
            AttendanceInput in = otInput(standardShift(), MONDAY, EmploymentMode.OFFICE, null,
                    true, false, false, true, 0);
            assertOt(eval(in, 465, 45), 45, 0, 0.0, false, true, "overtime is awaiting approval");
        }

        @Test
        void paysOrdinaryApprovedWeekdayOvertime() {
            assertOt(eval(approvedOtInput(standardShift()), 465, 45),
                    45, 30, 1.0, false, false, "ordinary approved overtime");
        }

        @Test
        void zeroesOvertimeBelowThirtyMinutes() {
            assertOt(eval(approvedOtInput(standardShift()), 454, 29),
                    29, 0, 1.0, false, false, "overtime below minimum duration");
        }

        @Test
        void paysExactlyThirtyMinutesOfOvertime() {
            assertOt(eval(approvedOtInput(standardShift()), 455, 30),
                    30, 30, 1.0, false, false, "ordinary approved overtime");
        }

        @Test
        void paysOvertimeAtExactlyFourHours() {
            assertOt(eval(approvedOtInput(standardShift()), 665, 240),
                    240, 240, 1.0, false, false, "ordinary approved overtime");
        }

        @Test
        void capsOvertimeAboveFourHoursAndFlagsReview() {
            assertOt(eval(approvedOtInput(standardShift()), 695, 270),
                    270, 240, 1.0, false, true, "daily overtime capped at four hours");
        }

        @Test
        void capsOvertimeBeyondSixHoursAndKeepsReviewFlag() {
            assertOt(eval(approvedOtInput(standardShift()), 786, 361),
                    361, 240, 1.0, false, true, "daily overtime capped at four hours");
        }

        @Test
        void keepsPayableUncappedWhenRoundedValueIsExactly240() {
            assertOt(eval(approvedOtInput(standardShift()), 661, 241),
                    241, 240, 1.0, false, false, "ordinary approved overtime");
        }

        @Test
        void grantsCompensatoryLeaveAfterSixConsecutiveWorkDays() {
            AttendanceInput in = otInput(standardShift(), MONDAY, EmploymentMode.OFFICE, null,
                    true, true, false, true, 6);
            assertOt(eval(in, 480, 60), 60, 60, 1.0, true, true, "ordinary approved overtime");
        }

        @Test
        void grantsNoCompensatoryLeaveAtFiveConsecutiveWorkDays() {
            AttendanceInput in = otInput(standardShift(), MONDAY, EmploymentMode.OFFICE, null,
                    true, true, false, true, 5);
            assertOt(eval(in, 480, 60), 60, 60, 1.0, false, false, "ordinary approved overtime");
        }

        @Test
        void triplesPublicHolidayOvertime() {
            AttendanceInput in = otInput(standardShift(), MONDAY, EmploymentMode.OFFICE, null,
                    true, true, true, true, 0);
            assertOt(eval(in, 465, 45), 45, 30, 3.0, false, false, "public-holiday overtime");
        }

        @Test
        void doublesRestDayOvertimeOnUnscheduledWeekday() {
            AttendanceInput in = otInput(standardShift(), MONDAY, EmploymentMode.OFFICE, null,
                    true, true, false, false, 0);
            assertOt(eval(in, 465, 45), 45, 30, 2.0, false, false, "rest-day overtime");
        }

        @Test
        void doublesSaturdayOvertimeEvenWhenScheduled() {
            AttendanceInput in = otInput(standardShift(), SATURDAY, EmploymentMode.OFFICE, null,
                    true, true, false, true, 0);
            assertOt(eval(in, 465, 45), 45, 30, 2.0, false, false, "rest-day overtime");
        }

        @Test
        void doublesSundayOvertimeEvenWhenScheduled() {
            AttendanceInput in = otInput(standardShift(), SUNDAY, EmploymentMode.OFFICE, null,
                    true, true, false, true, 0);
            assertOt(eval(in, 465, 45), 45, 30, 2.0, false, false, "rest-day overtime");
        }

        // NOTE: the NIGHT factor (1.5, "night-shift overtime") is unreachable
        // through the public API because a NIGHT-typed Shift can never be
        // constructed - see ShiftTests for the compact-constructor quirk.

        @Test
        void zeroesUnverifiedRemoteOvertimeAndFlagsReview() {
            PunchRecord unverified = punch(MONDAY.atTime(9, 0), MONDAY.atTime(17, 0),
                    60, true, false, false, false);
            AttendanceInput in = otInput(standardShift(), MONDAY, EmploymentMode.REMOTE,
                    unverified, true, true, false, true, 0);
            assertOt(eval(in, 465, 45), 45, 0, 1.0, false, true,
                    "remote overtime location is unverified");
        }

        @Test
        void paysVerifiedRemoteOvertimeInFull() {
            PunchRecord verified = punch(MONDAY.atTime(9, 0), MONDAY.atTime(17, 0),
                    60, true, true, false, false);
            AttendanceInput in = otInput(standardShift(), MONDAY, EmploymentMode.REMOTE,
                    verified, true, true, false, true, 0);
            assertOt(eval(in, 465, 45), 45, 30, 1.0, false, false, "ordinary approved overtime");
        }

        @Test
        void skipsRemoteLocationCheckWhenPunchIsMissing() {
            AttendanceInput in = otInput(standardShift(), MONDAY, EmploymentMode.REMOTE, null,
                    true, true, false, true, 0);
            assertOt(eval(in, 465, 45), 45, 30, 1.0, false, false, "ordinary approved overtime");
        }

        @Test
        void skipsRemoteLocationCheckForOfficeEmployee() {
            PunchRecord unverified = punch(MONDAY.atTime(9, 0), MONDAY.atTime(17, 0),
                    60, false, false, false, false);
            AttendanceInput in = otInput(standardShift(), MONDAY, EmploymentMode.OFFICE,
                    unverified, true, true, false, true, 0);
            assertOt(eval(in, 465, 45), 45, 30, 1.0, false, false, "ordinary approved overtime");
        }

        @Test
        void grantsCompensatoryLeaveForShortOnCallWork() {
            assertOt(eval(approvedOtInput(onCallShift()), 480, 60),
                    60, 60, 0.0, true, false, "short on-call work grants compensatory leave");
        }

        @Test
        void paysOnCallOvertimeBeyondSixtyMinutesNormally() {
            assertOt(eval(approvedOtInput(onCallShift()), 481, 61),
                    61, 60, 1.0, false, false, "ordinary approved overtime");
        }

        @Test
        void flagsReviewWhenWorkedTimeExceedsFifteenHours() {
            assertOt(eval(approvedOtInput(standardShift()), 901, 45),
                    45, 30, 1.0, false, true, "ordinary approved overtime");
        }

        @Test
        void keepsReviewOffAtExactlyFifteenHoursWorked() {
            assertOt(eval(approvedOtInput(standardShift()), 900, 45),
                    45, 30, 1.0, false, false, "ordinary approved overtime");
        }
    }

    // ------------------------------------------------------------------
    // OvertimeResult: compact-constructor validation
    // ------------------------------------------------------------------

    @Nested
    class OvertimeResultTests {

        @Test
        void rejectsNegativeActualMinutes() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new OvertimeResult(-1, 0, 1.0, false, false, "x"));
            assertEquals("overtime minutes must not be negative", ex.getMessage());
        }

        @Test
        void rejectsNegativePayableMinutes() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new OvertimeResult(0, -1, 1.0, false, false, "x"));
            assertEquals("overtime minutes must not be negative", ex.getMessage());
        }

        @Test
        void rejectsPayableMinutesAboveActual() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new OvertimeResult(0, 1, 1.0, false, false, "x"));
            assertEquals("payableMinutes cannot exceed actualMinutes", ex.getMessage());
        }

        @Test
        void rejectsNegativeCompensationFactor() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new OvertimeResult(0, 0, -0.5, false, false, "x"));
            assertEquals("compensationFactor must not be negative", ex.getMessage());
        }

        @Test
        void rejectsNullReason() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new OvertimeResult(0, 0, 1.0, false, false, null));
            assertEquals("reason must not be blank", ex.getMessage());
        }

        @Test
        void rejectsBlankReason() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new OvertimeResult(0, 0, 1.0, false, false, "   "));
            assertEquals("reason must not be blank", ex.getMessage());
        }

        @Test
        void acceptsZeroFactorEqualPayableAndValidReason() {
            OvertimeResult r = new OvertimeResult(30, 30, 0.0, true, false, "ok");
            assertEquals(30, r.actualMinutes());
            assertEquals(30, r.payableMinutes());
            assertEquals(0.0, r.compensationFactor());
            assertTrue(r.compensatoryLeave());
            assertFalse(r.managerReview());
            assertEquals("ok", r.reason());
        }
    }

    // ------------------------------------------------------------------
    // AttendancePolicy: full evaluate() flow, expected values hand-computed
    // ------------------------------------------------------------------

    @Nested
    class AttendancePolicyTests {

        private final AttendancePolicy policy = new AttendancePolicy();

        private AttendanceResult eval(AttendanceInput in) {
            return policy.evaluate(in);
        }

        @Test
        void rejectsNullInput() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> policy.evaluate(null));
            assertEquals("input must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullOvertimePolicyInConstructor() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new AttendancePolicy(null));
            assertNull(ex.getMessage());
        }

        @Test
        void acceptsExplicitlyProvidedOvertimePolicy() {
            AttendancePolicy explicit = new AttendancePolicy(new OvertimePolicy());
            assertResult(explicit.evaluate(dayInput(standardShift(), cleanPunch())),
                    List.of(AttendanceCode.NORMAL), 420, 0, 0, 0, 0, 0, false, List.of());
        }

        @Test
        void cleanStandardDayIsNormal() {
            assertResult(eval(dayInput(standardShift(), cleanPunch())),
                    List.of(AttendanceCode.NORMAL), 420, 0, 0, 0, 0, 0, false, List.of());
        }

        // ---------------- leave / trip / rest-day / absence ----------------

        @Test
        void approvedLeaveDayWithoutPunchIsQuiet() {
            AttendanceInput in = input(standardShift(), null, true, false, true, false,
                    false, false, false, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.APPROVED_LEAVE),
                    0, 0, 0, 0, 0, 0, false, List.of());
        }

        @Test
        void approvedLeaveDayWithPunchAddsWarning() {
            AttendanceInput in = input(standardShift(), cleanPunch(), true, false, true, false,
                    false, false, false, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.APPROVED_LEAVE),
                    0, 0, 0, 0, 0, 0, false, List.of("punch exists on an approved leave day"));
        }

        @Test
        void businessTripWithoutPunchCreditsMinimumWorkMinutes() {
            AttendanceInput in = input(standardShift(), null, true, false, false, true,
                    false, false, false, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.BUSINESS_TRIP),
                    240, 0, 0, 0, 0, 0, false, List.of());
        }

        @Test
        void businessTripWithPunchEvaluatesNormally() {
            AttendanceInput in = input(standardShift(), cleanPunch(), true, false, false, true,
                    false, false, false, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.BUSINESS_TRIP),
                    420, 0, 0, 0, 0, 0, false, List.of());
        }

        @Test
        void unscheduledDayWithoutPunchIsRestDay() {
            AttendanceInput in = input(standardShift(), null, false, false, false, false,
                    false, false, false, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.REST_DAY),
                    0, 0, 0, 0, 0, 0, false, List.of());
        }

        @Test
        void unscheduledDayWithPunchIsStillNormal() {
            AttendanceInput in = input(standardShift(), cleanPunch(), false, false, false, false,
                    false, false, false, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.NORMAL),
                    420, 0, 0, 0, 0, 0, false, List.of());
        }

        @Test
        void publicHolidayWithoutPunchIsAbsentWithFivePoints() {
            AttendanceInput in = input(standardShift(), null, false, true, false, false,
                    false, false, false, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.ABSENT),
                    0, 0, 0, 0, 0, 5, true, List.of());
        }

        @Test
        void publicHolidayInSevereWeatherWithoutPunchNeedsManualCorrection() {
            AttendanceInput in = input(standardShift(), null, false, true, false, false,
                    true, false, false, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.MANUAL_CORRECTION_REQUIRED),
                    0, 0, 0, 0, 0, 0, true, List.of());
        }

        @Test
        void scheduledDayWithoutPunchIsAbsentWithFivePoints() {
            AttendanceInput in = input(standardShift(), null, true, false, false, false,
                    false, false, false, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.ABSENT),
                    0, 0, 0, 0, 0, 5, true, List.of());
        }

        @Test
        void severeWeatherOnScheduledDayWithoutPunchNeedsManualCorrection() {
            AttendanceInput in = input(standardShift(), null, true, false, false, false,
                    true, false, false, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.MANUAL_CORRECTION_REQUIRED),
                    0, 0, 0, 0, 0, 0, true, List.of());
        }

        // ---------------- incomplete punches ----------------

        @Test
        void missingClockInOnlyNeedsManualCorrection() {
            PunchRecord p = punch(null, MONDAY.atTime(16, 55), 60, false, true, false, false);
            assertResult(eval(dayInput(standardShift(), p)),
                    List.of(AttendanceCode.MISSING_CLOCK_IN, AttendanceCode.MANUAL_CORRECTION_REQUIRED),
                    0, 0, 0, 0, 0, 2, true, List.of());
        }

        @Test
        void missingClockOutOnlyNeedsManualCorrection() {
            PunchRecord p = punch(MONDAY.atTime(8, 55), null, 60, false, true, false, false);
            assertResult(eval(dayInput(standardShift(), p)),
                    List.of(AttendanceCode.MISSING_CLOCK_OUT, AttendanceCode.MANUAL_CORRECTION_REQUIRED),
                    0, 0, 0, 0, 0, 2, true, List.of());
        }

        @Test
        void missingBothClocksNeedsManualCorrection() {
            PunchRecord p = punch(null, null, 0, false, true, false, false);
            assertResult(eval(dayInput(standardShift(), p)),
                    List.of(AttendanceCode.MISSING_CLOCK_IN, AttendanceCode.MISSING_CLOCK_OUT,
                            AttendanceCode.MANUAL_CORRECTION_REQUIRED),
                    0, 0, 0, 0, 0, 2, true, List.of());
        }

        @Test
        void approvedCorrectionWithMissingClocksWarnsWithoutCode() {
            PunchRecord p = punch(null, null, 0, false, true, true, true);
            assertResult(eval(dayInput(standardShift(), p)),
                    List.of(AttendanceCode.MISSING_CLOCK_IN, AttendanceCode.MISSING_CLOCK_OUT),
                    0, 0, 0, 0, 0, 2, true, List.of("approved correction remains incomplete"));
        }

        @Test
        void unapprovedManualCorrectionWithMissingClockInNeedsCorrection() {
            PunchRecord p = punch(null, MONDAY.atTime(16, 55), 60, false, true, true, false);
            assertResult(eval(dayInput(standardShift(), p)),
                    List.of(AttendanceCode.MISSING_CLOCK_IN, AttendanceCode.MANUAL_CORRECTION_REQUIRED),
                    0, 0, 0, 0, 0, 2, true, List.of());
        }

        @Test
        void unapprovedManualEntryOnCompletePunchNeedsCorrection() {
            PunchRecord p = punch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 55), 60,
                    false, true, true, false);
            assertResult(eval(dayInput(standardShift(), p)),
                    List.of(AttendanceCode.MANUAL_CORRECTION_REQUIRED),
                    420, 0, 0, 0, 0, 0, true, List.of());
        }

        @Test
        void approvedManualEntryOnCompletePunchIsNormal() {
            PunchRecord p = punch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 55), 60,
                    false, true, true, true);
            assertResult(eval(dayInput(standardShift(), p)),
                    List.of(AttendanceCode.NORMAL), 420, 0, 0, 0, 0, 0, false, List.of());
        }

        // ---------------- lateness (grace 10) ----------------

        @Test
        void lateWithinFifteenMinutesCostsOnePoint() {
            assertResult(eval(dayInput(standardShift(),
                    cleanPunch(MONDAY.atTime(9, 25), MONDAY.atTime(16, 55)))),
                    List.of(AttendanceCode.LATE), 390, 15, 0, 0, 0, 1, false, List.of());
        }

        @Test
        void lateBeyondFifteenMinutesCostsTwoPoints() {
            assertResult(eval(dayInput(standardShift(),
                    cleanPunch(MONDAY.atTime(9, 26), MONDAY.atTime(16, 55)))),
                    List.of(AttendanceCode.LATE), 389, 16, 0, 0, 0, 2, false, List.of());
        }

        @Test
        void arrivalInsideGraceIsNotLate() {
            assertResult(eval(dayInput(standardShift(),
                    cleanPunch(MONDAY.atTime(9, 10), MONDAY.atTime(16, 55)))),
                    List.of(AttendanceCode.NORMAL), 405, 0, 0, 0, 0, 0, false, List.of());
        }

        @Test
        void arrivalOneMinuteBeyondGraceIsLate() {
            assertResult(eval(dayInput(standardShift(),
                    cleanPunch(MONDAY.atTime(9, 11), MONDAY.atTime(16, 55)))),
                    List.of(AttendanceCode.LATE), 404, 1, 0, 0, 0, 1, false, List.of());
        }

        @Test
        void seriousLateAddsThreePointsAndFlagsReview() {
            assertResult(eval(dayInput(standardShift(),
                    cleanPunch(MONDAY.atTime(10, 15), MONDAY.atTime(16, 55)))),
                    List.of(AttendanceCode.LATE, AttendanceCode.SERIOUSLY_LATE),
                    340, 65, 0, 0, 0, 5, true, List.of());
        }

        @Test
        void repeatedLatenessAddsWarningAndTwoPoints() {
            AttendanceInput in = input(standardShift(),
                    cleanPunch(MONDAY.atTime(9, 11), MONDAY.atTime(16, 55)),
                    true, false, false, false, false, false, false, 3, 0);
            assertResult(eval(in), List.of(AttendanceCode.LATE),
                    404, 1, 0, 0, 0, 3, false, List.of("repeated lateness requires manager review"));
        }

        @Test
        void repeatedLatenessWarningNeedsThreeOrMore() {
            AttendanceInput in = input(standardShift(),
                    cleanPunch(MONDAY.atTime(9, 11), MONDAY.atTime(16, 55)),
                    true, false, false, false, false, false, false, 2, 0);
            assertResult(eval(in), List.of(AttendanceCode.LATE),
                    404, 1, 0, 0, 0, 1, false, List.of());
        }

        @Test
        void monthlyThresholdExceededAddsThreePointsEvenWithoutLateArrival() {
            AttendanceInput in = input(standardShift(), cleanPunch(),
                    true, false, false, false, false, false, false, 6, 0);
            assertResult(eval(in), List.of(AttendanceCode.NORMAL), 420, 0, 0, 0, 0, 3, false,
                    List.of("monthly lateness threshold exceeded"));
        }

        @Test
        void monthlyThresholdWarningNeedsSixOrMore() {
            AttendanceInput in = input(standardShift(), cleanPunch(),
                    true, false, false, false, false, false, false, 5, 0);
            assertResult(eval(in), List.of(AttendanceCode.NORMAL), 420, 0, 0, 0, 0, 0, false,
                    List.of());
        }

        // ---------------- flexible shift relief ----------------

        @Test
        void flexibleShiftAbsorbsLateUnderAnHour() {
            assertResult(eval(dayInput(flexibleShift(),
                    cleanPunch(MONDAY.atTime(10, 0), MONDAY.atTime(16, 55)))),
                    List.of(AttendanceCode.NORMAL), 355, 0, 0, 0, 0, 0, false, List.of());
        }

        @Test
        void flexibleShiftAbsorbsExactlySixtyLateMinutes() {
            assertResult(eval(dayInput(flexibleShift(),
                    cleanPunch(MONDAY.atTime(10, 10), MONDAY.atTime(16, 55)))),
                    List.of(AttendanceCode.NORMAL), 345, 0, 0, 0, 0, 0, false, List.of());
        }

        @Test
        void flexibleShiftChargesOneMinuteBeyondTheHourRelief() {
            assertResult(eval(dayInput(flexibleShift(),
                    cleanPunch(MONDAY.atTime(10, 11), MONDAY.atTime(16, 55)))),
                    List.of(AttendanceCode.LATE), 344, 1, 0, 0, 0, 1, false, List.of());
        }

        @Test
        void flexibleShiftCanStillBeSeriouslyLateAfterRelief() {
            assertResult(eval(dayInput(flexibleShift(),
                    cleanPunch(MONDAY.atTime(11, 30), MONDAY.atTime(16, 55)))),
                    List.of(AttendanceCode.LATE, AttendanceCode.SERIOUSLY_LATE),
                    265, 80, 0, 0, 0, 5, true, List.of());
        }

        // ---------------- severe-weather waiver ----------------

        @Test
        void severeWeatherWaivesLateWithinTwoHours() {
            AttendanceInput in = input(standardShift(),
                    cleanPunch(MONDAY.atTime(11, 0), MONDAY.atTime(16, 55)),
                    true, false, false, false, true, false, false, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.NORMAL), 295, 0, 0, 0, 0, 0, false,
                    List.of("late arrival waived because of severe weather"));
        }

        @Test
        void severeWeatherWaivesExactlyTwoHoursOfLate() {
            AttendanceInput in = input(standardShift(),
                    cleanPunch(MONDAY.atTime(11, 10), MONDAY.atTime(16, 55)),
                    true, false, false, false, true, false, false, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.NORMAL), 285, 0, 0, 0, 0, 0, false,
                    List.of("late arrival waived because of severe weather"));
        }

        @Test
        void severeWeatherKeepsLateBeyondTwoHours() {
            AttendanceInput in = input(standardShift(),
                    cleanPunch(MONDAY.atTime(11, 11), MONDAY.atTime(16, 55)),
                    true, false, false, false, true, false, false, 0, 0);
            assertResult(eval(in),
                    List.of(AttendanceCode.LATE, AttendanceCode.SERIOUSLY_LATE),
                    284, 121, 0, 0, 0, 5, true, List.of());
        }

        @Test
        void severeWeatherWaiverAlsoFiresWhenThereIsNoLateArrivalAtAll() {
            AttendanceInput in = input(standardShift(), cleanPunch(),
                    true, false, false, false, true, false, false, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.NORMAL), 420, 0, 0, 0, 0, 0, false,
                    List.of("late arrival waived because of severe weather"));
        }

        // ---------------- early leave (grace 10) ----------------

        @Test
        void earlyLeaveWithinAnHourCostsOnePoint() {
            assertResult(eval(dayInput(standardShift(),
                    cleanPunch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 30)))),
                    List.of(AttendanceCode.EARLY_LEAVE), 395, 0, 30, 0, 0, 1, false, List.of());
        }

        @Test
        void earlyLeaveAtExactlySixtyMinutesCostsOnePoint() {
            assertResult(eval(dayInput(standardShift(),
                    cleanPunch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 0)))),
                    List.of(AttendanceCode.EARLY_LEAVE), 365, 0, 60, 0, 0, 1, false, List.of());
        }

        @Test
        void earlyLeaveBeyondSixtyMinutesCostsThreePoints() {
            assertResult(eval(dayInput(standardShift(),
                    cleanPunch(MONDAY.atTime(8, 55), MONDAY.atTime(15, 59)))),
                    List.of(AttendanceCode.EARLY_LEAVE), 364, 0, 61, 0, 0, 3, false, List.of());
        }

        @Test
        void earlyLeaveInsideGraceIsIgnored() {
            assertResult(eval(dayInput(standardShift(),
                    cleanPunch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 50)))),
                    List.of(AttendanceCode.NORMAL), 415, 0, 0, 0, 0, 0, false, List.of());
        }

        @Test
        void earlyLeaveOneMinuteBeyondGraceCounts() {
            assertResult(eval(dayInput(standardShift(),
                    cleanPunch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 49)))),
                    List.of(AttendanceCode.EARLY_LEAVE), 414, 0, 11, 0, 0, 1, false, List.of());
        }

        // ---------------- insufficient break ----------------

        @Test
        void shortBreakOnLongDayFlagsInsufficientBreak() {
            PunchRecord p = punch(MONDAY.atTime(9, 51), MONDAY.atTime(16, 50), 59,
                    false, true, false, false);
            assertResult(eval(dayInput(longGraceShift(), p)),
                    List.of(AttendanceCode.INSUFFICIENT_BREAK), 360, 0, 0, 0, 0, 0, false,
                    List.of("required break duration was not met"));
        }

        @Test
        void adequateBreakOnSameDayIsNormal() {
            PunchRecord p = punch(MONDAY.atTime(9, 51), MONDAY.atTime(16, 50), 60,
                    false, true, false, false);
            assertResult(eval(dayInput(longGraceShift(), p)),
                    List.of(AttendanceCode.NORMAL), 359, 0, 0, 0, 0, 0, false, List.of());
        }

        @Test
        void shortBreakOnShortDayIsIgnored() {
            PunchRecord p = punch(MONDAY.atTime(9, 5), MONDAY.atTime(15, 54), 59,
                    false, true, false, false);
            assertResult(eval(dayInput(lateDayShift(), p)),
                    List.of(AttendanceCode.EARLY_LEAVE), 350, 0, 246, 0, 0, 3, false, List.of());
        }

        // ---------------- insufficient hours (09:00-18:00, minimum 480) ----------------

        @Test
        void halfOfMinimumHoursCostsFourPoints() {
            assertResult(eval(dayInput(longDayShift(),
                    cleanPunch(MONDAY.atTime(8, 55), MONDAY.atTime(13, 54)))),
                    List.of(AttendanceCode.EARLY_LEAVE, AttendanceCode.INSUFFICIENT_HOURS),
                    239, 0, 246, 0, 0, 7, true, List.of());
        }

        @Test
        void justAboveHalfMinimumHoursCostsTwoPoints() {
            assertResult(eval(dayInput(longDayShift(),
                    cleanPunch(MONDAY.atTime(8, 55), MONDAY.atTime(13, 55)))),
                    List.of(AttendanceCode.EARLY_LEAVE, AttendanceCode.INSUFFICIENT_HOURS),
                    240, 0, 245, 0, 0, 5, true, List.of());
        }

        @Test
        void justUnderMinimumHoursCostsTwoPoints() {
            assertResult(eval(dayInput(longDayShift(),
                    cleanPunch(MONDAY.atTime(8, 55), MONDAY.atTime(17, 54)))),
                    List.of(AttendanceCode.INSUFFICIENT_HOURS), 479, 0, 0, 0, 0, 2, false,
                    List.of());
        }

        @Test
        void exactlyMinimumHoursIsNormal() {
            assertResult(eval(dayInput(longDayShift(),
                    cleanPunch(MONDAY.atTime(8, 55), MONDAY.atTime(17, 55)))),
                    List.of(AttendanceCode.NORMAL), 480, 0, 0, 0, 0, 0, false, List.of());
        }

        // ---------------- location validation ----------------

        @Test
        void officeEmployeeWithRemotePunchIsUnverified() {
            PunchRecord p = punch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 55), 60,
                    true, false, false, false);
            assertResult(eval(modeInput(EmploymentMode.OFFICE, p)),
                    List.of(AttendanceCode.LOCATION_UNVERIFIED), 420, 0, 0, 0, 0, 0, true,
                    List.of("office employee used a remote punch"));
        }

        @Test
        void remoteEmployeeWithOfficePunchWarnsWithoutCode() {
            PunchRecord p = punch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 55), 60,
                    false, true, false, false);
            assertResult(eval(modeInput(EmploymentMode.REMOTE, p)),
                    List.of(AttendanceCode.NORMAL), 420, 0, 0, 0, 0, 0, false,
                    List.of("remote employee used an office punch"));
        }

        @Test
        void remoteEmployeeWithVerifiedRemotePunchIsNormal() {
            PunchRecord p = punch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 55), 60,
                    true, true, false, false);
            assertResult(eval(modeInput(EmploymentMode.REMOTE, p)),
                    List.of(AttendanceCode.NORMAL), 420, 0, 0, 0, 0, 0, false, List.of());
        }

        @Test
        void fieldEmployeeWithoutVerificationIsUnverified() {
            PunchRecord p = punch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 55), 60,
                    false, false, false, false);
            assertResult(eval(modeInput(EmploymentMode.FIELD, p)),
                    List.of(AttendanceCode.LOCATION_UNVERIFIED), 420, 0, 0, 0, 0, 0, true,
                    List.of());
        }

        @Test
        void fieldEmployeeWithVerificationIsNormal() {
            PunchRecord p = punch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 55), 60,
                    false, true, false, false);
            assertResult(eval(modeInput(EmploymentMode.FIELD, p)),
                    List.of(AttendanceCode.NORMAL), 420, 0, 0, 0, 0, 0, false, List.of());
        }

        @Test
        void hybridUnverifiedRemotePunchNeedsReview() {
            PunchRecord p = punch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 55), 60,
                    true, false, false, false);
            assertResult(eval(modeInput(EmploymentMode.HYBRID, p)),
                    List.of(AttendanceCode.LOCATION_UNVERIFIED), 420, 0, 0, 0, 0, 0, true,
                    List.of("hybrid remote location needs review"));
        }

        @Test
        void hybridVerifiedRemotePunchIsNormal() {
            PunchRecord p = punch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 55), 60,
                    true, true, false, false);
            assertResult(eval(modeInput(EmploymentMode.HYBRID, p)),
                    List.of(AttendanceCode.NORMAL), 420, 0, 0, 0, 0, 0, false, List.of());
        }

        @Test
        void hybridEmployeeWithOfficePunchIsNormal() {
            PunchRecord p = punch(MONDAY.atTime(8, 55), MONDAY.atTime(16, 55), 60,
                    false, true, false, false);
            assertResult(eval(modeInput(EmploymentMode.HYBRID, p)),
                    List.of(AttendanceCode.NORMAL), 420, 0, 0, 0, 0, 0, false, List.of());
        }

        // ---------------- overtime coding ----------------

        @Test
        void approvedOvertimeIsCodedEvenWhenBelowPayableMinimum() {
            AttendanceInput in = input(standardShift(),
                    cleanPunch(MONDAY.atTime(8, 55), MONDAY.atTime(17, 10)),
                    true, false, false, false, false, true, true, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.OVERTIME), 435, 0, 0, 15, 0, 0,
                    false, List.of());
        }

        @Test
        void unrequestedOvertimeRequiresManagerReview() {
            AttendanceInput in = input(standardShift(),
                    cleanPunch(MONDAY.atTime(8, 55), MONDAY.atTime(17, 10)),
                    true, false, false, false, false, false, false, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.OVERTIME), 435, 0, 0, 15, 0, 0,
                    true, List.of());
        }

        @Test
        void excessiveOvertimeAddsCodeAndCappedReasonWarning() {
            AttendanceInput in = input(standardShift(),
                    cleanPunch(MONDAY.atTime(8, 0), MONDAY.atTime(20, 30)),
                    true, false, false, false, false, true, true, 0, 0);
            assertResult(eval(in),
                    List.of(AttendanceCode.OVERTIME, AttendanceCode.EXCESSIVE_OVERTIME),
                    690, 0, 0, 270, 240, 0, true,
                    List.of("daily overtime capped at four hours"));
        }

        @Test
        void overtimeAtExactlyFourHoursIsNotExcessive() {
            AttendanceInput in = input(standardShift(),
                    cleanPunch(MONDAY.atTime(8, 0), MONDAY.atTime(20, 0)),
                    true, false, false, false, false, true, true, 0, 0);
            assertResult(eval(in), List.of(AttendanceCode.OVERTIME), 660, 0, 0, 240, 240, 0,
                    false, List.of());
        }

        /**
         * raw 241 rounds down to payable 240, which is NOT > 240, so the
         * four-hour cap does not fire: actual 241 is excessive, but payable is
         * uncapped, review stays off and the warning carries the ordinary
         * reason. The cap itself (raw >= 271) is covered by the 270-minute
         * tests below.
         */
        @Test
        void overtimeOf241ActualMinutesIsExcessiveWithUncappedPayable() {
            AttendanceInput in = input(standardShift(),
                    cleanPunch(MONDAY.atTime(8, 0), MONDAY.atTime(20, 1)),
                    true, false, false, false, false, true, true, 0, 0);
            assertResult(eval(in),
                    List.of(AttendanceCode.OVERTIME, AttendanceCode.EXCESSIVE_OVERTIME),
                    661, 0, 0, 241, 240, 0, false,
                    List.of("ordinary approved overtime"));
        }

        @Test
        void unrequestedExcessiveOvertimeCarriesRequestReasonWarning() {
            AttendanceInput in = input(standardShift(),
                    cleanPunch(MONDAY.atTime(8, 0), MONDAY.atTime(20, 30)),
                    true, false, false, false, false, false, false, 0, 0);
            assertResult(eval(in),
                    List.of(AttendanceCode.OVERTIME, AttendanceCode.EXCESSIVE_OVERTIME),
                    690, 0, 0, 270, 0, 0, true, List.of("overtime was not requested"));
        }

        @Test
        void equalClockInAndClockOutYieldZeroWorkedHours() {
            PunchRecord p = punch(MONDAY.atTime(9, 0), MONDAY.atTime(9, 0), 0,
                    false, true, false, false);
            assertResult(eval(dayInput(standardShift(), p)),
                    List.of(AttendanceCode.EARLY_LEAVE, AttendanceCode.INSUFFICIENT_HOURS),
                    0, 0, 480, 0, 0, 7, true, List.of());
        }
    }

    // ------------------------------------------------------------------
    // Card date core: DateChecker, DateFormatter, DateParser, InvalidCardException
    // ------------------------------------------------------------------

    @Nested
    class CardDateTests {

        @Test
        void compareDatesAcceptsStrictlyLaterDate() {
            assertTrue(DateChecker.compareDates(13, 12));
            assertTrue(DateChecker.compareDates(2027, 2026));
        }

        @Test
        void compareDatesRejectsEqualDate() {
            assertFalse(DateChecker.compareDates(12, 12));
        }

        @Test
        void compareDatesRejectsEarlierDate() {
            assertFalse(DateChecker.compareDates(11, 12));
        }

        @Test
        void expirationEqualToCurrentMonthIsValidThroughMonthEnd() {
            assertTrue(DateChecker.isValidExpiration(YearMonth.of(2026, 1), FIXED_CLOCK));
        }

        @Test
        void expirationAfterCurrentMonthIsValid() {
            assertTrue(DateChecker.isValidExpiration(YearMonth.of(2026, 2), FIXED_CLOCK));
        }

        @Test
        void expirationBeforeCurrentMonthIsExpired() {
            assertFalse(DateChecker.isValidExpiration(YearMonth.of(2025, 12), FIXED_CLOCK));
        }

        @Test
        void rejectsNullExpirationYearMonth() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> DateChecker.isValidExpiration(null, FIXED_CLOCK));
            assertEquals("expiration must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullClock() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> DateChecker.isValidExpiration(YearMonth.of(2026, 1), null));
            assertEquals("clock must not be null", ex.getMessage());
        }

        @Test
        void twoDigitYearTwentySixExpiresInJune2026() {
            assertTrue(DateChecker.isValidExpiration(6, 26, FIXED_CLOCK));
            assertFalse(DateChecker.isValidExpiration(12, 25, FIXED_CLOCK));
        }

        @Test
        void twoDigitYearBoundariesMapTo2000And2099() {
            assertFalse(DateChecker.isValidExpiration(1, 0, FIXED_CLOCK));
            assertTrue(DateChecker.isValidExpiration(12, 99, FIXED_CLOCK));
        }

        @Test
        void rejectsNegativeTwoDigitYear() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateChecker.isValidExpiration(1, -1, FIXED_CLOCK));
            assertEquals("year must contain two digits", ex.getMessage());
        }

        @Test
        void rejectsYearAboveNinetyNine() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateChecker.isValidExpiration(1, 100, FIXED_CLOCK));
            assertEquals("year must contain two digits", ex.getMessage());
        }

        @Test
        void convertDateWrapsTheGivenTextUnchanged() {
            assertEquals("12/26", DateChecker.convertDate("12/26").toString());
            assertEquals("0924", DateChecker.convertDate("0924").toString());
        }

        /**
         * getCurrentYear/getCurrentMonth (and the CURRENT_* constants) read the
         * live wall clock through Calendar + SimpleDateFormat("yy"/"MM") with
         * no injectable clock, so the independent oracle is java.time taken on
         * the same day; the "yy" pattern makes getCurrentYear() two-digit by
         * design (verified: returns 26 in 2026).
         */
        @Test
        void currentYearMatchesTwoDigitYearOfToday() {
            assertEquals(YearMonth.now().getYear() % 100, DateChecker.getCurrentYear());
            assertEquals(DateChecker.CURRENT_YEAR, DateChecker.getCurrentYear());
        }

        @Test
        void currentMonthMatchesCalendarMonthOfToday() {
            assertEquals(YearMonth.now().getMonthValue(), DateChecker.getCurrentMonth());
            assertEquals(DateChecker.CURRENT_MONTH, DateChecker.getCurrentMonth());
        }

        @Test
        void yearFormatProducesTwoDigitYear() {
            DateFormat format = DateFormatter.yearFormat();
            assertTrue(format instanceof SimpleDateFormat);
            assertEquals("yy", ((SimpleDateFormat) format).toPattern());
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date noon = Date.from(LocalDateTime.of(2026, 3, 5, 12, 0).toInstant(ZoneOffset.UTC));
            assertEquals("26", format.format(noon));
        }

        @Test
        void monthFormatProducesZeroPaddedMonth() {
            DateFormat format = DateFormatter.monthFormat();
            assertTrue(format instanceof SimpleDateFormat);
            assertEquals("MM", ((SimpleDateFormat) format).toPattern());
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date noon = Date.from(LocalDateTime.of(2026, 3, 5, 12, 0).toInstant(ZoneOffset.UTC));
            assertEquals("03", format.format(noon));
        }

        @Test
        void parseDateRemovesOnlyTheFirstSlash() {
            assertEquals("1226", DateParser.parseDate(new StringBuilder("12/26")).toString());
            assertEquals("12/3", DateParser.parseDate(new StringBuilder("1/2/3")).toString());
        }

        @Test
        void parseDateWithoutSlashIsReturnedUnchanged() {
            assertEquals("1226", DateParser.parseDate(new StringBuilder("1226")).toString());
        }

        @Test
        void parseDateWithLocationsExtractsMonthAndYear() {
            assertEquals(12, DateParser.parseDate(new StringBuilder("1226"), 0, 2));
            assertEquals(26, DateParser.parseDate(new StringBuilder("1226"), 2, 4));
            assertEquals(5, DateParser.parseDate(new StringBuilder("0526"), 0, 2));
        }

        @Test
        void invalidCardExceptionCarriesMessageAndExtendsIllegalArgumentException() {
            InvalidCardException ex = new InvalidCardException("boom");
            assertEquals("boom", ex.getMessage());
            assertTrue(ex instanceof IllegalArgumentException);
        }

        @Test
        void utilityImplicitConstructorsAreInstantiable() {
            assertNotNull(new DateChecker());
            assertNotNull(new DateFormatter());
            assertNotNull(new DateParser());
        }
    }

    // ------------------------------------------------------------------
    // Validator (concrete class - no interface, exercised directly)
    // ------------------------------------------------------------------

    @Nested
    class ValidatorTests {

        private Validator validator(String expiration, String cvv) {
            return new Validator(LUHN_OK_16, expiration, cvv, FIXED_CLOCK);
        }

        @Test
        void rejectsNullCardNumberInSingleArgumentConstructor() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new Validator(null));
            assertEquals("number must not be null", ex.getMessage());
        }

        @Test
        void singleArgumentValidatorHasNoExpirationDateOrCvv() throws InvalidCardException {
            Validator v = new Validator(LUHN_OK_16);
            assertFalse(v.checkCVV());
            assertFalse(v.checkExpirationDate());
            assertTrue(v.validate());
        }

        @Test
        void missingExpirationDateBlocksBothSetters() {
            Validator v = new Validator(LUHN_OK_16);
            IllegalStateException ex1 = assertThrows(IllegalStateException.class,
                    v::setExpirationMonth);
            assertEquals("expirationDate was not supplied", ex1.getMessage());
            IllegalStateException ex2 = assertThrows(IllegalStateException.class,
                    v::setExpirationYear);
            assertEquals("expirationDate was not supplied", ex2.getMessage());
        }

        @Test
        void validateAcceptsTwelveDigitLuhnNumber() throws InvalidCardException {
            assertTrue(new Validator(LUHN_OK_12).validate());
        }

        @Test
        void validateAcceptsNineteenDigitLuhnNumber() throws InvalidCardException {
            assertTrue(new Validator(LUHN_OK_19).validate());
        }

        @Test
        void validateRejectsElevenDigitNumber() {
            InvalidCardException ex = assertThrows(InvalidCardException.class,
                    () -> new Validator("41111111111").validate());
            assertEquals("Card number must contain 12 to 19 digits", ex.getMessage());
        }

        @Test
        void validateRejectsTwentyDigitNumber() {
            InvalidCardException ex = assertThrows(InvalidCardException.class,
                    () -> new Validator("41111111111111111111").validate());
            assertEquals("Card number must contain 12 to 19 digits", ex.getMessage());
        }

        @Test
        void validateRejectsNonDigitCharacters() {
            InvalidCardException ex = assertThrows(InvalidCardException.class,
                    () -> new Validator("4111-1111-1111-1111").validate());
            assertEquals("Card number must contain 12 to 19 digits", ex.getMessage());
        }

        @Test
        void validateRejectsFailedChecksum() {
            InvalidCardException ex = assertThrows(InvalidCardException.class,
                    () -> new Validator(LUHN_BAD_16).validate());
            assertEquals("Card number checksum is invalid", ex.getMessage());
        }

        @Test
        void threeArgumentConstructorValidatesNumberAndCvv() throws InvalidCardException {
            Validator v = new Validator(LUHN_OK_16, "1226", "123");
            assertTrue(v.validate());
            assertTrue(v.checkCVV());
        }

        @Test
        void compactExpirationDateIsValidAgainstFixedClock() {
            Validator v = validator("1226", "123");
            assertTrue(v.checkExpirationDate());
            v.setExpirationMonth();
            v.setExpirationYear();
            assertTrue(v.checkExpirationDate());
        }

        @Test
        void slashedExpirationDateIsValidAgainstFixedClock() {
            assertTrue(validator("07/30", "123").checkExpirationDate());
        }

        @Test
        void expiredCompactDateFailsCheck() {
            assertFalse(validator("1225", "123").checkExpirationDate());
        }

        @Test
        void expirationMonthEqualToCurrentMonthIsValid() {
            assertTrue(validator("0126", "123").checkExpirationDate());
        }

        @Test
        void rejectsMalformedExpirationDates() {
            for (String bad : List.of("1326", "0026", "126", "12266", "ab26")) {
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> validator(bad, "123"));
                assertEquals("expirationDate must use MMyy or MM/yy", ex.getMessage());
            }
        }

        @Test
        void rejectsNullExpirationDateString() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> validator(null, "123"));
            assertEquals("expirationDate must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullClockInFourArgumentConstructor() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new Validator(LUHN_OK_16, "1226", "123", null));
            assertEquals("clock must not be null", ex.getMessage());
        }

        @Test
        void checkCvvAcceptsOnlyThreeOrFourDigits() {
            assertFalse(validator("1226", null).checkCVV());
            assertFalse(validator("1226", "").checkCVV());
            assertFalse(validator("1226", "12").checkCVV());
            assertTrue(validator("1226", "123").checkCVV());
            assertTrue(validator("1226", "1234").checkCVV());
            assertFalse(validator("1226", "12345").checkCVV());
            assertFalse(validator("1226", "12a").checkCVV());
        }
    }}

    // ------------------------------------------------------------------
    // Leave subsystem (provincial additions)
    // ------------------------------------------------------------------
    @Nested
    class LeaveModuleTests {

    // Fixed reference dates. 2026-03-10 is a Tuesday.
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    private static final LocalDate MON = LocalDate.of(2026, 3, 16);      // Monday
    private static final LocalDate TUE = LocalDate.of(2026, 3, 17);
    private static final LocalDate WED = LocalDate.of(2026, 3, 18);
    private static final LocalDate THU = LocalDate.of(2026, 3, 19);
    private static final LocalDate FRI = LocalDate.of(2026, 3, 20);
    private static final LocalDate SAT = LocalDate.of(2026, 3, 21);
    private static final LocalDate SUN = LocalDate.of(2026, 3, 22);
    private static final LocalDate MON2 = LocalDate.of(2026, 3, 23);
    private static final LocalDate FRI13 = LocalDate.of(2026, 3, 13);    // Friday before TODAY
    private static final LocalDate SUN15 = LocalDate.of(2026, 3, 15);    // Sunday before TODAY
    private static final LocalDate MON9 = LocalDate.of(2026, 3, 9);      // Monday, backdated
    private static final LocalDate APR6 = LocalDate.of(2026, 4, 6);      // Monday in April
    private static final LocalDate APR27 = LocalDate.of(2026, 4, 27);    // Monday
    private static final LocalDate APR29 = LocalDate.of(2026, 4, 29);    // Wednesday
    private static final LocalDate APR30 = LocalDate.of(2026, 4, 30);    // Thursday
    private static final LocalDate NEXT_YEAR_LIMIT = LocalDate.of(2027, 3, 10);  // today + 1 year (Wednesday)
    private static final LocalDate NEXT_YEAR_OVER = LocalDate.of(2027, 3, 11);   // one day past the limit (Thursday)

    /** Monday-Friday calendar with no holidays: deterministic and hand-computable. */
    private static final WorkCalendar WORKDAYS = d -> d.getDayOfWeek().getValue() <= 5;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-10T09:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    private static final AdvancedLeavePolicy POLICY = new AdvancedLeavePolicy();

    /** Fluent builder so every test states exactly what varies; each test gets a fresh instance. */
    private static final class Req {
        private String employeeId = "E001";
        private EmployeeCategory category = EmployeeCategory.REGULAR;
        private LeaveType type = LeaveType.ANNUAL;
        private LocalDate start = MON;
        private LocalDate end = MON;
        private LeaveUnit unit = LeaveUnit.FULL_DAY;
        private double hours = 0;
        private String reason = "family visit";
        private boolean emergency = false;
        private boolean remote = false;
        private boolean handover = false;
        private int months = 24;
        private int notice = 3;
        private double remaining = 10;
        private double carry = 0;

        private Req() { }

        static Req of() { return new Req(); }

        Req employeeId(String id) { this.employeeId = id; return this; }
        Req cat(EmployeeCategory c) { this.category = c; return this; }
        Req type(LeaveType t) { this.type = t; return this; }
        Req span(LocalDate s, LocalDate e) { this.start = s; this.end = e; return this; }
        Req unit(LeaveUnit u) { this.unit = u; return this; }
        Req hours(double h) { this.hours = h; return this; }
        Req reason(String r) { this.reason = r; return this; }
        Req emergency(boolean b) { this.emergency = b; return this; }
        Req remote(boolean b) { this.remote = b; return this; }
        Req handover(boolean b) { this.handover = b; return this; }
        Req months(int m) { this.months = m; return this; }
        Req notice(int n) { this.notice = n; return this; }
        Req balances(double remaining, double carry) { this.remaining = remaining; this.carry = carry; return this; }

        AdvancedLeaveRequest build() {
            return new AdvancedLeaveRequest(employeeId, category, type, start, end, unit,
                    hours, reason, emergency, remote, handover, months, notice, remaining, carry);
        }
    }

    private static LeaveAssessment assess(Req req) {
        return POLICY.assess(req.build(), WORKDAYS, TODAY);
    }

    private static LeaveAssessment assessWithCalendar(Req req, WorkCalendar calendar) {
        return POLICY.assess(req.build(), calendar, TODAY);
    }

    @Nested
    class EnumValuesTests {

        @Test
        void leaveTypeExposesExactlyThreeValues() {
            assertEquals(3, LeaveType.values().length);
            assertEquals(LeaveType.ANNUAL, LeaveType.valueOf("ANNUAL"));
            assertEquals(LeaveType.SICK, LeaveType.valueOf("SICK"));
            assertEquals(LeaveType.PERSONAL, LeaveType.valueOf("PERSONAL"));
        }

        @Test
        void leaveStatusExposesExactlyThreeValues() {
            assertEquals(3, LeaveStatus.values().length);
            assertEquals(LeaveStatus.PENDING, LeaveStatus.valueOf("PENDING"));
            assertEquals(LeaveStatus.APPROVED, LeaveStatus.valueOf("APPROVED"));
            assertEquals(LeaveStatus.REJECTED, LeaveStatus.valueOf("REJECTED"));
        }

        @Test
        void leaveUnitExposesExactlyFourValues() {
            assertEquals(4, LeaveUnit.values().length);
            assertEquals(LeaveUnit.FULL_DAY, LeaveUnit.valueOf("FULL_DAY"));
            assertEquals(LeaveUnit.HALF_DAY_MORNING, LeaveUnit.valueOf("HALF_DAY_MORNING"));
            assertEquals(LeaveUnit.HALF_DAY_AFTERNOON, LeaveUnit.valueOf("HALF_DAY_AFTERNOON"));
            assertEquals(LeaveUnit.HOURS, LeaveUnit.valueOf("HOURS"));
        }

        @Test
        void employeeCategoryExposesExactlyFourValues() {
            assertEquals(4, EmployeeCategory.values().length);
            assertEquals(EmployeeCategory.REGULAR, EmployeeCategory.valueOf("REGULAR"));
            assertEquals(EmployeeCategory.PROBATION, EmployeeCategory.valueOf("PROBATION"));
            assertEquals(EmployeeCategory.INTERN, EmployeeCategory.valueOf("INTERN"));
            assertEquals(EmployeeCategory.CONTRACTOR, EmployeeCategory.valueOf("CONTRACTOR"));
        }

        @Test
        void approvalTierExposesExactlyFourValues() {
            assertEquals(4, ApprovalTier.values().length);
            assertEquals(ApprovalTier.SUPERVISOR, ApprovalTier.valueOf("SUPERVISOR"));
            assertEquals(ApprovalTier.MANAGER, ApprovalTier.valueOf("MANAGER"));
            assertEquals(ApprovalTier.HUMAN_RESOURCES, ApprovalTier.valueOf("HUMAN_RESOURCES"));
            assertEquals(ApprovalTier.DIRECTOR, ApprovalTier.valueOf("DIRECTOR"));
        }
    }

    @Nested
    class DateRangeTests {

        @Test
        void nullStartIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new DateRange(null, LocalDate.of(2026, 3, 16)));
            assertEquals("start must not be null", ex.getMessage());
        }

        @Test
        void nullEndIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new DateRange(LocalDate.of(2026, 3, 16), null));
            assertEquals("end must not be null", ex.getMessage());
        }

        @Test
        void invertedRangeIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new DateRange(LocalDate.of(2026, 3, 17), LocalDate.of(2026, 3, 16)));
            assertEquals("end must not be before start", ex.getMessage());
        }

        @Test
        void singleDayRangeIsAccepted() {
            DateRange range = new DateRange(MON, MON);
            assertEquals(MON, range.start());
            assertEquals(MON, range.end());
        }

        @Test
        void businessDaysCountsFullWeekAsFive() {
            // 2026-03-16 (Mon) .. 2026-03-20 (Fri) = 5 business days
            assertEquals(5, new DateRange(MON, FRI).businessDays());
        }

        @Test
        void businessDaysCountsSingleMondayAsOne() {
            assertEquals(1, new DateRange(MON, MON).businessDays());
        }

        @Test
        void businessDaysOfPureWeekendIsZero() {
            // 2026-03-21 (Sat) .. 2026-03-22 (Sun)
            assertEquals(0, new DateRange(SAT, SUN).businessDays());
        }

        @Test
        void businessDaysOfSingleSaturdayIsZero() {
            assertEquals(0, new DateRange(SAT, SAT).businessDays());
        }

        @Test
        void businessDaysOfSingleSundayIsZero() {
            assertEquals(0, new DateRange(SUN, SUN).businessDays());
        }

        @Test
        void businessDaysOfWeekLongSpanIsFive() {
            // Mon 2026-03-16 .. Sun 2026-03-22 spans 7 calendar days, 5 business days
            assertEquals(5, new DateRange(MON, SUN).businessDays());
        }

        @Test
        void businessDaysAcrossWeekendGapCountsOnlyWeekdays() {
            // Fri 2026-03-20 .. Mon 2026-03-23 = Fri + Mon = 2
            assertEquals(2, new DateRange(FRI, MON2).businessDays());
        }

        @Test
        void overlapsTrueForIdenticalRanges() {
            DateRange a = new DateRange(MON, WED);
            assertTrue(a.overlaps(new DateRange(MON, WED)));
        }

        @Test
        void overlapsTrueWhenRangesShareOneBoundaryDate() {
            // a ends Tue 17, b starts Tue 17: inclusive touch counts as overlap
            assertTrue(new DateRange(MON, TUE).overlaps(new DateRange(TUE, THU)));
        }

        @Test
        void overlapsFalseWhenRangesAreAdjacentButDisjoint() {
            // a ends Mon 16, b starts Tue 17
            assertFalse(new DateRange(MON, MON).overlaps(new DateRange(TUE, WED)));
        }

        @Test
        void overlapsTrueWhenOtherIsContained() {
            assertTrue(new DateRange(MON, FRI).overlaps(new DateRange(TUE, WED)));
        }

        @Test
        void overlapsTrueWhenOtherContainsThis() {
            assertTrue(new DateRange(TUE, WED).overlaps(new DateRange(MON, FRI)));
        }

        @Test
        void overlapsFalseWhenOtherLiesEntirelyBefore() {
            assertFalse(new DateRange(MON, WED).overlaps(new DateRange(THU, FRI)));
        }

        @Test
        void overlapsFalseWhenOtherLiesEntirelyAfter() {
            assertFalse(new DateRange(THU, FRI).overlaps(new DateRange(MON, WED)));
        }

        @Test
        void overlapsRejectsNullOther() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new DateRange(MON, WED).overlaps(null));
            assertEquals("other must not be null", ex.getMessage());
        }
    }

    @Nested
    class LeaveRequestRecordTests {

        private static final DateRange PERIOD = new DateRange(MON, MON);
        private static final Instant ASKED_AT = FIXED_INSTANT;

        @Test
        void nullIdIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveRequest(null, "E1", LeaveType.SICK, PERIOD, "family trip",
                            LeaveStatus.PENDING, ASKED_AT, null, null, null));
            assertEquals("id must not be blank", ex.getMessage());
        }

        @Test
        void blankIdIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveRequest("   ", "E1", LeaveType.SICK, PERIOD, "family trip",
                            LeaveStatus.PENDING, ASKED_AT, null, null, null));
            assertEquals("id must not be blank", ex.getMessage());
        }

        @Test
        void nullEmployeeIdIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveRequest("R1", null, LeaveType.SICK, PERIOD, "family trip",
                            LeaveStatus.PENDING, ASKED_AT, null, null, null));
            assertEquals("employeeId must not be blank", ex.getMessage());
        }

        @Test
        void blankEmployeeIdIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveRequest("R1", "  ", LeaveType.SICK, PERIOD, "family trip",
                            LeaveStatus.PENDING, ASKED_AT, null, null, null));
            assertEquals("employeeId must not be blank", ex.getMessage());
        }

        @Test
        void nullTypeIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new LeaveRequest("R1", "E1", null, PERIOD, "family trip",
                            LeaveStatus.PENDING, ASKED_AT, null, null, null));
            assertEquals("type must not be null", ex.getMessage());
        }

        @Test
        void nullPeriodIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new LeaveRequest("R1", "E1", LeaveType.SICK, null, "family trip",
                            LeaveStatus.PENDING, ASKED_AT, null, null, null));
            assertEquals("period must not be null", ex.getMessage());
        }

        @Test
        void nullReasonIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveRequest("R1", "E1", LeaveType.SICK, PERIOD, null,
                            LeaveStatus.PENDING, ASKED_AT, null, null, null));
            assertEquals("reason must not be blank", ex.getMessage());
        }

        @Test
        void blankReasonIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveRequest("R1", "E1", LeaveType.SICK, PERIOD, "  ",
                            LeaveStatus.PENDING, ASKED_AT, null, null, null));
            assertEquals("reason must not be blank", ex.getMessage());
        }

        @Test
        void nullStatusIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new LeaveRequest("R1", "E1", LeaveType.SICK, PERIOD, "family trip",
                            null, ASKED_AT, null, null, null));
            assertEquals("status must not be null", ex.getMessage());
        }

        @Test
        void nullRequestedAtIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new LeaveRequest("R1", "E1", LeaveType.SICK, PERIOD, "family trip",
                            LeaveStatus.PENDING, null, null, null, null));
            assertEquals("requestedAt must not be null", ex.getMessage());
        }

        @Test
        void pendingRequestMustNotCarryDecidedBy() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveRequest("R1", "E1", LeaveType.SICK, PERIOD, "family trip",
                            LeaveStatus.PENDING, ASKED_AT, "A1", null, null));
            assertEquals("pending request must not contain a decision", ex.getMessage());
        }

        @Test
        void pendingRequestMustNotCarryDecidedAt() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveRequest("R1", "E1", LeaveType.SICK, PERIOD, "family trip",
                            LeaveStatus.PENDING, ASKED_AT, null, ASKED_AT, null));
            assertEquals("pending request must not contain a decision", ex.getMessage());
        }

        @Test
        void pendingRequestMustNotCarryDecisionComment() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveRequest("R1", "E1", LeaveType.SICK, PERIOD, "family trip",
                            LeaveStatus.PENDING, ASKED_AT, null, null, "nope"));
            assertEquals("pending request must not contain a decision", ex.getMessage());
        }

        @Test
        void decidedRequestRequiresDecidedBy() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveRequest("R1", "E1", LeaveType.SICK, PERIOD, "family trip",
                            LeaveStatus.APPROVED, ASKED_AT, null, ASKED_AT, null));
            assertEquals("decidedBy must not be blank", ex.getMessage());
        }

        @Test
        void decidedRequestRejectsBlankDecidedBy() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveRequest("R1", "E1", LeaveType.SICK, PERIOD, "family trip",
                            LeaveStatus.REJECTED, ASKED_AT, " ", ASKED_AT, null));
            assertEquals("decidedBy must not be blank", ex.getMessage());
        }

        @Test
        void decidedRequestRequiresDecidedAt() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new LeaveRequest("R1", "E1", LeaveType.SICK, PERIOD, "family trip",
                            LeaveStatus.APPROVED, ASKED_AT, "A1", null, null));
            assertEquals("decidedAt must not be null", ex.getMessage());
        }

        @Test
        void textFieldsAreTrimmedOnConstruction() {
            LeaveRequest request = new LeaveRequest(" R1 ", " E1 ", LeaveType.SICK, PERIOD, " family trip ",
                    LeaveStatus.PENDING, ASKED_AT, null, null, null);
            assertEquals("R1", request.id());
            assertEquals("E1", request.employeeId());
            assertEquals("family trip", request.reason());
        }

        @Test
        void decidedRequestMayOmitCommentAndTrimsPresentComment() {
            LeaveRequest withoutComment = new LeaveRequest("R1", "E1", LeaveType.SICK, PERIOD, "family trip",
                    LeaveStatus.APPROVED, ASKED_AT, "A1", ASKED_AT, null);
            assertNull(withoutComment.decisionComment());

            LeaveRequest withComment = new LeaveRequest("R1", "E1", LeaveType.SICK, PERIOD, "family trip",
                    LeaveStatus.REJECTED, ASKED_AT, "A1", ASKED_AT, "  not enough balance  ");
            assertEquals("not enough balance", withComment.decisionComment());
        }

        @Test
        void pendingFactoryBuildsCleanPendingRequest() {
            LeaveRequest request = LeaveRequest.pending("R1", "E1", LeaveType.ANNUAL, PERIOD, "family trip", ASKED_AT);
            assertEquals("R1", request.id());
            assertEquals("E1", request.employeeId());
            assertEquals(LeaveType.ANNUAL, request.type());
            assertEquals(PERIOD, request.period());
            assertEquals("family trip", request.reason());
            assertEquals(LeaveStatus.PENDING, request.status());
            assertEquals(ASKED_AT, request.requestedAt());
            assertNull(request.decidedBy());
            assertNull(request.decidedAt());
            assertNull(request.decisionComment());
        }

        @Test
        void approveTransitionsPendingToApproved() {
            LeaveRequest pending = LeaveRequest.pending("R1", "E1", LeaveType.ANNUAL, PERIOD, "family trip", ASKED_AT);
            Instant decidedAt = FIXED_INSTANT.plusSeconds(60);
            LeaveRequest approved = pending.approve("A9", decidedAt);
            assertEquals(LeaveStatus.APPROVED, approved.status());
            assertEquals("A9", approved.decidedBy());
            assertEquals(decidedAt, approved.decidedAt());
            assertNull(approved.decisionComment());
        }

        @Test
        void rejectTransitionsPendingToRejectedWithComment() {
            LeaveRequest pending = LeaveRequest.pending("R1", "E1", LeaveType.ANNUAL, PERIOD, "family trip", ASKED_AT);
            Instant decidedAt = FIXED_INSTANT.plusSeconds(120);
            LeaveRequest rejected = pending.reject("A9", decidedAt, " project crunch ");
            assertEquals(LeaveStatus.REJECTED, rejected.status());
            assertEquals("A9", rejected.decidedBy());
            assertEquals(decidedAt, rejected.decidedAt());
            assertEquals("project crunch", rejected.decisionComment());
        }

        @Test
        void approveOnApprovedRequestIsRejected() {
            LeaveRequest approved = LeaveRequest.pending("R1", "E1", LeaveType.ANNUAL, PERIOD, "family trip", ASKED_AT)
                    .approve("A9", FIXED_INSTANT);
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> approved.approve("A2", FIXED_INSTANT));
            assertEquals("only a pending request can be decided", ex.getMessage());
        }

        @Test
        void rejectOnApprovedRequestIsRejected() {
            LeaveRequest approved = LeaveRequest.pending("R1", "E1", LeaveType.ANNUAL, PERIOD, "family trip", ASKED_AT)
                    .approve("A9", FIXED_INSTANT);
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> approved.reject("A2", FIXED_INSTANT, "no"));
            assertEquals("only a pending request can be decided", ex.getMessage());
        }

        @Test
        void approveOnRejectedRequestIsRejected() {
            LeaveRequest rejected = LeaveRequest.pending("R1", "E1", LeaveType.ANNUAL, PERIOD, "family trip", ASKED_AT)
                    .reject("A9", FIXED_INSTANT, "no");
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> rejected.approve("A2", FIXED_INSTANT));
            assertEquals("only a pending request can be decided", ex.getMessage());
        }

        @Test
        void rejectOnRejectedRequestIsRejected() {
            LeaveRequest rejected = LeaveRequest.pending("R1", "E1", LeaveType.ANNUAL, PERIOD, "family trip", ASKED_AT)
                    .reject("A9", FIXED_INSTANT, "no");
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> rejected.reject("A2", FIXED_INSTANT, "still no"));
            assertEquals("only a pending request can be decided", ex.getMessage());
        }

        @Test
        void rejectRequiresNonBlankComment() {
            LeaveRequest pending = LeaveRequest.pending("R1", "E1", LeaveType.ANNUAL, PERIOD, "family trip", ASKED_AT);
            IllegalArgumentException nullComment = assertThrows(IllegalArgumentException.class,
                    () -> pending.reject("A9", FIXED_INSTANT, null));
            assertEquals("comment must not be blank", nullComment.getMessage());

            LeaveRequest fresh = LeaveRequest.pending("R2", "E1", LeaveType.ANNUAL, PERIOD, "family trip", ASKED_AT);
            IllegalArgumentException blankComment = assertThrows(IllegalArgumentException.class,
                    () -> fresh.reject("A9", FIXED_INSTANT, "   "));
            assertEquals("comment must not be blank", blankComment.getMessage());
        }
    }

    @Nested
    class LeavePolicyTests {

        private final LeavePolicy policy = new LeavePolicy();
        private static final DateRange WEEK = new DateRange(MON, FRI);          // 5 business days
        private static final DateRange WEEKEND = new DateRange(SAT, SUN);       // 0 business days
        private static final DateRange THREE_DAYS = new DateRange(MON, WED);    // 3 business days
        private static final DateRange FOUR_DAYS = new DateRange(MON, THU);     // 4 business days

        @Test
        void nullTypeIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> policy.validate(null, WEEK, "family trip", 5));
            assertEquals("type must not be null", ex.getMessage());
        }

        @Test
        void nullPeriodIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> policy.validate(LeaveType.ANNUAL, null, "family trip", 5));
            assertEquals("period must not be null", ex.getMessage());
        }

        @Test
        void weekendOnlyRequestIsRejectedAsZeroBusinessDays() {
            LeaveValidationException ex = assertThrows(LeaveValidationException.class,
                    () -> policy.validate(LeaveType.ANNUAL, WEEKEND, "family trip", 5));
            assertEquals("leave must contain at least one business day", ex.getMessage());
        }

        @Test
        void singleBusinessDayPassesTheZeroDayCheck() {
            assertEquals(1, policy.validate(LeaveType.SICK, new DateRange(MON, MON), "family trip", 0));
        }

        @Test
        void nullReasonIsRejected() {
            LeaveValidationException ex = assertThrows(LeaveValidationException.class,
                    () -> policy.validate(LeaveType.ANNUAL, WEEK, null, 5));
            assertEquals("reason must contain at least 5 characters", ex.getMessage());
        }

        @Test
        void shortReasonIsRejectedAfterTrimming() {
            LeaveValidationException ex = assertThrows(LeaveValidationException.class,
                    () -> policy.validate(LeaveType.ANNUAL, WEEK, "  abc  ", 5));
            assertEquals("reason must contain at least 5 characters", ex.getMessage());
        }

        @Test
        void fourCharacterReasonIsRejectedButFiveCharactersPass() {
            LeaveValidationException ex = assertThrows(LeaveValidationException.class,
                    () -> policy.validate(LeaveType.SICK, WEEK, "abcd", 0));
            assertEquals("reason must contain at least 5 characters", ex.getMessage());
            assertEquals(5, policy.validate(LeaveType.SICK, WEEK, "abcde", 0));
        }

        @Test
        void annualLeaveBeyondBalanceIsRejected() {
            LeaveValidationException ex = assertThrows(LeaveValidationException.class,
                    () -> policy.validate(LeaveType.ANNUAL, WEEK, "family trip", 4));
            assertEquals("insufficient annual-leave balance", ex.getMessage());
        }

        @Test
        void annualLeaveExactlyAtBalanceIsAccepted() {
            assertEquals(5, policy.validate(LeaveType.ANNUAL, WEEK, "family trip", 5));
        }

        @Test
        void annualLeaveWithinBalanceIsAccepted() {
            assertEquals(5, policy.validate(LeaveType.ANNUAL, WEEK, "family trip", 6));
        }

        @Test
        void personalLeaveBeyondThreeBusinessDaysIsRejected() {
            LeaveValidationException ex = assertThrows(LeaveValidationException.class,
                    () -> policy.validate(LeaveType.PERSONAL, FOUR_DAYS, "family trip", 0));
            assertEquals("personal leave cannot exceed 3 business days", ex.getMessage());
        }

        @Test
        void personalLeaveOfExactlyThreeBusinessDaysIsAccepted() {
            assertEquals(3, policy.validate(LeaveType.PERSONAL, THREE_DAYS, "family trip", 0));
        }

        @Test
        void sickLeaveIsExemptFromBalanceAndPersonalCaps() {
            // SICK is not gated by the annual balance nor the 3-day personal cap
            assertEquals(4, policy.validate(LeaveType.SICK, FOUR_DAYS, "family trip", 0));
        }

        @Test
        void validateReturnsTheBusinessDayCount() {
            // Mon 2026-03-16 .. Sun 2026-03-22 contains 5 business days
            assertEquals(5, policy.validate(LeaveType.SICK, new DateRange(MON, SUN), "family trip", 0));
        }
    }

    @Nested
    class SubmitLeaveCommandTests {

        @Test
        void nullEmployeeIdIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new SubmitLeaveCommand(null, LeaveType.ANNUAL, new DateRange(MON, MON), "family trip"));
            assertEquals("employeeId must not be blank", ex.getMessage());
        }

        @Test
        void blankEmployeeIdIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new SubmitLeaveCommand("  ", LeaveType.ANNUAL, new DateRange(MON, MON), "family trip"));
            assertEquals("employeeId must not be blank", ex.getMessage());
        }

        @Test
        void nullTypeIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new SubmitLeaveCommand("E1", null, new DateRange(MON, MON), "family trip"));
            assertEquals("type must not be null", ex.getMessage());
        }

        @Test
        void nullPeriodIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> new SubmitLeaveCommand("E1", LeaveType.ANNUAL, null, "family trip"));
            assertEquals("period must not be null", ex.getMessage());
        }

        @Test
        void employeeIdIsTrimmed() {
            SubmitLeaveCommand command = new SubmitLeaveCommand("  E1  ", LeaveType.SICK,
                    new DateRange(MON, MON), "family trip");
            assertEquals("E1", command.employeeId());
            assertEquals(LeaveType.SICK, command.type());
            assertEquals(new DateRange(MON, MON), command.period());
            assertEquals("family trip", command.reason());
        }
    }

    @Nested
    class ExceptionTests {

        @Test
        void notFoundExceptionBuildsExactMessage() {
            LeaveRequestNotFoundException ex = new LeaveRequestNotFoundException("REQ-77");
            assertEquals("leave request not found: REQ-77", ex.getMessage());
            assertTrue(ex instanceof RuntimeException);
        }

        @Test
        void notFoundExceptionKeepsEmptyIdInMessage() {
            LeaveRequestNotFoundException ex = new LeaveRequestNotFoundException("");
            assertEquals("leave request not found: ", ex.getMessage());
        }

        @Test
        void validationExceptionCarriesMessageAndIsIllegalArgumentException() {
            LeaveValidationException ex = new LeaveValidationException("insufficient annual-leave balance");
            assertEquals("insufficient annual-leave balance", ex.getMessage());
            assertTrue(ex instanceof IllegalArgumentException);
        }
    }

    @Nested
    class LeaveRequestServiceTests {

        private LeaveRequestRepository requestRepository;
        private LeaveBalanceRepository balanceRepository;
        private NotificationGateway notificationGateway;
        private RequestIdGenerator idGenerator;
        private LeaveRequestService service;

        private void setUpService() {
            requestRepository = mock(LeaveRequestRepository.class);
            balanceRepository = mock(LeaveBalanceRepository.class);
            notificationGateway = mock(NotificationGateway.class);
            idGenerator = mock(RequestIdGenerator.class);
            service = new LeaveRequestService(requestRepository, balanceRepository,
                    notificationGateway, idGenerator, new LeavePolicy(), FIXED_CLOCK);
        }

        private static final DateRange MON_TUE = new DateRange(MON, TUE);   // 2 business days

        @Test
        void constructorRejectsNullRequestRepository() {
            assertThrows(NullPointerException.class, () -> new LeaveRequestService(null,
                    mock(LeaveBalanceRepository.class), mock(NotificationGateway.class),
                    mock(RequestIdGenerator.class), new LeavePolicy(), FIXED_CLOCK));
        }

        @Test
        void constructorRejectsNullBalanceRepository() {
            assertThrows(NullPointerException.class, () -> new LeaveRequestService(
                    mock(LeaveRequestRepository.class), null, mock(NotificationGateway.class),
                    mock(RequestIdGenerator.class), new LeavePolicy(), FIXED_CLOCK));
        }

        @Test
        void constructorRejectsNullNotificationGateway() {
            assertThrows(NullPointerException.class, () -> new LeaveRequestService(
                    mock(LeaveRequestRepository.class), mock(LeaveBalanceRepository.class), null,
                    mock(RequestIdGenerator.class), new LeavePolicy(), FIXED_CLOCK));
        }

        @Test
        void constructorRejectsNullIdGenerator() {
            assertThrows(NullPointerException.class, () -> new LeaveRequestService(
                    mock(LeaveRequestRepository.class), mock(LeaveBalanceRepository.class),
                    mock(NotificationGateway.class), null, new LeavePolicy(), FIXED_CLOCK));
        }

        @Test
        void constructorRejectsNullPolicy() {
            assertThrows(NullPointerException.class, () -> new LeaveRequestService(
                    mock(LeaveRequestRepository.class), mock(LeaveBalanceRepository.class),
                    mock(NotificationGateway.class), mock(RequestIdGenerator.class), null, FIXED_CLOCK));
        }

        @Test
        void constructorRejectsNullClock() {
            assertThrows(NullPointerException.class, () -> new LeaveRequestService(
                    mock(LeaveRequestRepository.class), mock(LeaveBalanceRepository.class),
                    mock(NotificationGateway.class), mock(RequestIdGenerator.class),
                    new LeavePolicy(), null));
        }

        @Test
        void submitRejectsNullCommand() {
            setUpService();
            NullPointerException ex = assertThrows(NullPointerException.class, () -> service.submit(null));
            assertEquals("command must not be null", ex.getMessage());
        }

        @Test
        void submitQueriesBalanceForAnnualLeaveAndSavesPendingRequest() {
            setUpService();
            when(balanceRepository.remainingAnnualDays("E1")).thenReturn(9);
            when(requestRepository.existsActiveOverlap("E1", MON_TUE)).thenReturn(false);
            when(idGenerator.nextId()).thenReturn("REQ-1");
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SubmitLeaveCommand command = new SubmitLeaveCommand(" E1 ", LeaveType.ANNUAL, MON_TUE, "family trip");
            LeaveRequest saved = service.submit(command);

            verify(balanceRepository).remainingAnnualDays("E1");
            org.mockito.ArgumentCaptor<LeaveRequest> captor =
                    org.mockito.ArgumentCaptor.forClass(LeaveRequest.class);
            verify(requestRepository).save(captor.capture());
            LeaveRequest persisted = captor.getValue();
            assertEquals("REQ-1", persisted.id());
            assertEquals("E1", persisted.employeeId());
            assertEquals(LeaveType.ANNUAL, persisted.type());
            assertEquals(MON_TUE, persisted.period());
            assertEquals("family trip", persisted.reason());
            assertEquals(LeaveStatus.PENDING, persisted.status());
            assertEquals(FIXED_INSTANT, persisted.requestedAt());
            assertNull(persisted.decidedBy());
            assertNull(persisted.decidedAt());
            assertNull(persisted.decisionComment());
            assertSame(persisted, saved);
            verify(notificationGateway).requestSubmitted(saved);
        }

        @Test
        void submitReturnsTheRepositoryResultAndNotifiesWithIt() {
            setUpService();
            when(balanceRepository.remainingAnnualDays("E1")).thenReturn(9);
            when(requestRepository.existsActiveOverlap("E1", MON_TUE)).thenReturn(false);
            when(idGenerator.nextId()).thenReturn("REQ-1");
            LeaveRequest persisted = LeaveRequest.pending("REQ-1", "E1", LeaveType.ANNUAL, MON_TUE,
                    "family trip", FIXED_INSTANT);
            when(requestRepository.save(any())).thenReturn(persisted);

            LeaveRequest returned = service.submit(
                    new SubmitLeaveCommand("E1", LeaveType.ANNUAL, MON_TUE, "family trip"));

            assertSame(persisted, returned);
            verify(notificationGateway).requestSubmitted(persisted);
        }

        @Test
        void submitSkipsBalanceLookupForSickLeave() {
            setUpService();
            when(requestRepository.existsActiveOverlap("E1", MON_TUE)).thenReturn(false);
            when(idGenerator.nextId()).thenReturn("REQ-2");
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LeaveRequest saved = service.submit(
                    new SubmitLeaveCommand("E1", LeaveType.SICK, MON_TUE, "family trip"));

            verify(balanceRepository, never()).remainingAnnualDays(anyString());
            assertEquals(LeaveType.SICK, saved.type());
            verify(notificationGateway).requestSubmitted(saved);
        }

        @Test
        void submitSkipsBalanceLookupForPersonalLeave() {
            setUpService();
            when(requestRepository.existsActiveOverlap("E1", MON_TUE)).thenReturn(false);
            when(idGenerator.nextId()).thenReturn("REQ-3");
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.submit(new SubmitLeaveCommand("E1", LeaveType.PERSONAL, MON_TUE, "family trip"));

            verify(balanceRepository, never()).remainingAnnualDays(anyString());
            verify(notificationGateway).requestSubmitted(any());
        }

        @Test
        void submitRejectsOverlapAndPersistsNothing() {
            setUpService();
            when(balanceRepository.remainingAnnualDays("E1")).thenReturn(9);
            when(requestRepository.existsActiveOverlap("E1", MON_TUE)).thenReturn(true);

            LeaveValidationException ex = assertThrows(LeaveValidationException.class,
                    () -> service.submit(new SubmitLeaveCommand("E1", LeaveType.ANNUAL, MON_TUE, "family trip")));
            assertEquals("leave period overlaps an active request", ex.getMessage());

            verify(requestRepository, never()).save(any());
            verify(notificationGateway, never()).requestSubmitted(any());
        }

        @Test
        void submitPropagatesPolicyBalanceRejectionWithoutOverlapCheck() {
            setUpService();
            when(balanceRepository.remainingAnnualDays("E1")).thenReturn(1);

            // MON..FRI = 5 business days > 1 remaining
            LeaveValidationException ex = assertThrows(LeaveValidationException.class,
                    () -> service.submit(new SubmitLeaveCommand("E1", LeaveType.ANNUAL,
                            new DateRange(MON, FRI), "family trip")));
            assertEquals("insufficient annual-leave balance", ex.getMessage());

            verify(requestRepository, never()).existsActiveOverlap(anyString(), any());
            verify(requestRepository, never()).save(any());
            verify(notificationGateway, never()).requestSubmitted(any());
        }

        @Test
        void approveRejectsBlankRequestId() {
            setUpService();
            IllegalArgumentException nullId = assertThrows(IllegalArgumentException.class,
                    () -> service.approve(null, "A1"));
            assertEquals("requestId must not be blank", nullId.getMessage());

            IllegalArgumentException blankId = assertThrows(IllegalArgumentException.class,
                    () -> service.approve("   ", "A1"));
            assertEquals("requestId must not be blank", blankId.getMessage());
        }

        @Test
        void approveRejectsBlankApproverId() {
            setUpService();
            when(requestRepository.findById("R1")).thenReturn(Optional.of(pending("R1", "E1")));

            IllegalArgumentException nullActor = assertThrows(IllegalArgumentException.class,
                    () -> service.approve("R1", null));
            assertEquals("approverId must not be blank", nullActor.getMessage());

            IllegalArgumentException blankActor = assertThrows(IllegalArgumentException.class,
                    () -> service.approve("R1", "  "));
            assertEquals("approverId must not be blank", blankActor.getMessage());
        }

        @Test
        void approveThrowsNotFoundForUnknownRequestId() {
            setUpService();
            when(requestRepository.findById("NOPE")).thenReturn(Optional.empty());

            LeaveRequestNotFoundException ex = assertThrows(LeaveRequestNotFoundException.class,
                    () -> service.approve("NOPE", "A1"));
            assertEquals("leave request not found: NOPE", ex.getMessage());
            verify(notificationGateway, never()).requestApproved(any());
        }

        @Test
        void approveTrimsRequestIdAndApproverId() {
            setUpService();
            when(requestRepository.findById("R1")).thenReturn(Optional.of(pending("R1", "E1")));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LeaveRequest approved = service.approve(" R1 ", " A9 ");

            assertEquals(LeaveStatus.APPROVED, approved.status());
            assertEquals("A9", approved.decidedBy());
            verify(requestRepository).findById("R1");
            verify(notificationGateway).requestApproved(approved);
        }

        @Test
        void approveBlocksSelfApproval() {
            setUpService();
            when(requestRepository.findById("R1")).thenReturn(Optional.of(pending("R1", "E1")));

            LeaveValidationException ex = assertThrows(LeaveValidationException.class,
                    () -> service.approve("R1", "E1"));
            assertEquals("employee cannot approve their own request", ex.getMessage());

            verify(requestRepository, never()).save(any());
            verify(notificationGateway, never()).requestApproved(any());
        }

        @Test
        void approveOnAnnualLeaveDeductsBusinessDays() {
            setUpService();
            // MON..WED = 3 business days
            when(requestRepository.findById("R1"))
                    .thenReturn(Optional.of(pending("R1", "E1", LeaveType.ANNUAL, new DateRange(MON, WED))));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LeaveRequest approved = service.approve("R1", "A9");

            verify(balanceRepository).deductAnnualDays("E1", 3);
            assertEquals(LeaveStatus.APPROVED, approved.status());
            assertEquals(FIXED_INSTANT, approved.decidedAt());
            verify(notificationGateway).requestApproved(approved);
        }

        @Test
        void approveOnNonAnnualLeaveDoesNotTouchBalance() {
            setUpService();
            when(requestRepository.findById("R1"))
                    .thenReturn(Optional.of(pending("R1", "E1", LeaveType.PERSONAL, MON_TUE)));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.approve("R1", "A9");
            verify(balanceRepository, never()).deductAnnualDays(anyString(), anyInt());

            setUpService();
            when(requestRepository.findById("R2"))
                    .thenReturn(Optional.of(pending("R2", "E1", LeaveType.SICK, MON_TUE)));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.approve("R2", "A9");
            verify(balanceRepository, never()).deductAnnualDays(anyString(), anyInt());
        }

        @Test
        void rejectRejectsBlankRequestId() {
            setUpService();
            LeaveRequestNotFoundException notFound = assertThrows(LeaveRequestNotFoundException.class,
                    () -> service.reject("GONE", "A1", "no"));
            assertEquals("leave request not found: GONE", notFound.getMessage());

            IllegalArgumentException blank = assertThrows(IllegalArgumentException.class,
                    () -> service.reject("  ", "A1", "no"));
            assertEquals("requestId must not be blank", blank.getMessage());
        }

        @Test
        void rejectBlocksSelfRejection() {
            setUpService();
            when(requestRepository.findById("R1")).thenReturn(Optional.of(pending("R1", "E1")));

            LeaveValidationException ex = assertThrows(LeaveValidationException.class,
                    () -> service.reject("R1", "E1", "no"));
            assertEquals("employee cannot reject their own request", ex.getMessage());

            verify(requestRepository, never()).save(any());
            verify(notificationGateway, never()).requestRejected(any());
        }

        @Test
        void rejectTransitionsAndNotifies() {
            setUpService();
            when(requestRepository.findById("R1")).thenReturn(Optional.of(pending("R1", "E1")));
            when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LeaveRequest rejected = service.reject("R1", "A9", "team is short-handed");

            assertEquals(LeaveStatus.REJECTED, rejected.status());
            assertEquals("A9", rejected.decidedBy());
            assertEquals("team is short-handed", rejected.decisionComment());
            assertEquals(FIXED_INSTANT, rejected.decidedAt());
            verify(notificationGateway).requestRejected(rejected);
            verify(balanceRepository, never()).deductAnnualDays(anyString(), anyInt());
        }

        @Test
        void rejectPropagatesBlankCommentRule() {
            setUpService();
            when(requestRepository.findById("R1")).thenReturn(Optional.of(pending("R1", "E1")));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.reject("R1", "A9", "   "));
            assertEquals("comment must not be blank", ex.getMessage());
            verify(requestRepository, never()).save(any());
            verify(notificationGateway, never()).requestRejected(any());
        }

        private static LeaveRequest pending(String id, String employeeId) {
            return pending(id, employeeId, LeaveType.SICK, MON_TUE);
        }

        private static LeaveRequest pending(String id, String employeeId, LeaveType type, DateRange period) {
            return LeaveRequest.pending(id, employeeId, type, period, "family trip", FIXED_INSTANT);
        }
    }

    @Nested
    class WorkCalendarTests {

        @Test
        void weekendCalendarRejectsSaturdayAndSunday() {
            WorkCalendar weekdaysOnly = d -> d.getDayOfWeek() != DayOfWeek.SATURDAY
                    && d.getDayOfWeek() != DayOfWeek.SUNDAY;
            assertTrue(weekdaysOnly.isWorkingDay(MON));
            assertTrue(weekdaysOnly.isWorkingDay(FRI));
            assertFalse(weekdaysOnly.isWorkingDay(SAT));
            assertFalse(weekdaysOnly.isWorkingDay(SUN));
        }

        @Test
        void sevenDayCalendarAcceptsWeekends() {
            WorkCalendar allDays = d -> true;
            assertTrue(allDays.isWorkingDay(SAT));
            assertTrue(allDays.isWorkingDay(SUN));
        }

        @Test
        void holidayCalendarExcludesDeclaredHolidays() {
            // Treat Wednesday 2026-03-18 as a public holiday
            WorkCalendar withHoliday = d -> d.getDayOfWeek().getValue() <= 5 && !d.equals(WED);
            assertTrue(withHoliday.isWorkingDay(TUE));
            assertFalse(withHoliday.isWorkingDay(WED));
            assertTrue(withHoliday.isWorkingDay(THU));
        }
    }

    @Nested
    class AdvancedRequestRecordTests {

        @Test
        void nullEmployeeIdIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Req.of().employeeId(null).build());
            assertEquals("employeeId must not be blank", ex.getMessage());
        }

        @Test
        void blankEmployeeIdIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Req.of().employeeId("   ").build());
            assertEquals("employeeId must not be blank", ex.getMessage());
        }

        @Test
        void employeeIdIsTrimmed() {
            AdvancedLeaveRequest request = Req.of().build();
            assertEquals("E001", request.employeeId());
        }

        @Test
        void nullCategoryIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> Req.of().cat(null).build());
            assertEquals("employeeCategory must not be null", ex.getMessage());
        }

        @Test
        void nullTypeIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> Req.of().type(null).build());
            assertEquals("type must not be null", ex.getMessage());
        }

        @Test
        void nullStartDateIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> Req.of().span(null, MON).build());
            assertEquals("startDate must not be null", ex.getMessage());
        }

        @Test
        void nullEndDateIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> Req.of().span(MON, null).build());
            assertEquals("endDate must not be null", ex.getMessage());
        }

        @Test
        void nullUnitIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> Req.of().unit(null).build());
            assertEquals("unit must not be null", ex.getMessage());
        }

        @Test
        void invertedRangeIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Req.of().span(TUE, MON).build());
            assertEquals("endDate must not be before startDate", ex.getMessage());
        }

        @Test
        void sameStartAndEndIsAccepted() {
            AdvancedLeaveRequest request = Req.of().span(MON, MON).build();
            assertEquals(MON, request.startDate());
            assertEquals(MON, request.endDate());
        }

        @Test
        void negativeEmploymentMonthsIsRejectedButZeroIsAllowed() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Req.of().type(LeaveType.SICK).months(-1).build());
            assertEquals("employmentMonths must not be negative", ex.getMessage());
            assertEquals(0, Req.of().type(LeaveType.SICK).months(0).build().employmentMonths());
        }

        @Test
        void negativeNoticeDaysIsRejectedButZeroIsAllowed() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Req.of().notice(-1).build());
            assertEquals("noticeDays must not be negative", ex.getMessage());
            assertEquals(0, Req.of().notice(0).build().noticeDays());
        }

        @Test
        void negativeRemainingBalanceIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Req.of().balances(-1, 0).build());
            assertEquals("leave balances must not be negative", ex.getMessage());
        }

        @Test
        void negativeCarryOverBalanceIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Req.of().balances(0, -0.5).build());
            assertEquals("leave balances must not be negative", ex.getMessage());
        }

        @Test
        void zeroBalancesAreAccepted() {
            AdvancedLeaveRequest request = Req.of().balances(0, 0).build();
            assertEquals(0, request.remainingBalance(), 1e-9);
            assertEquals(0, request.carryOverBalance(), 1e-9);
        }

        @Test
        void hourLeaveWithZeroHoursIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Req.of().unit(LeaveUnit.HOURS).hours(0).build());
            assertEquals("hour leave must be between 0 and 8 hours", ex.getMessage());
        }

        @Test
        void hourLeaveWithNegativeHoursIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Req.of().unit(LeaveUnit.HOURS).hours(-2).build());
            assertEquals("hour leave must be between 0 and 8 hours", ex.getMessage());
        }

        @Test
        void hourLeaveAboveEightHoursIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Req.of().unit(LeaveUnit.HOURS).hours(8.5).build());
            assertEquals("hour leave must be between 0 and 8 hours", ex.getMessage());
        }

        @Test
        void hourLeaveOfExactlyEightHoursIsAccepted() {
            AdvancedLeaveRequest request = Req.of().unit(LeaveUnit.HOURS).hours(8).build();
            assertEquals(8, request.requestedHours(), 1e-9);
        }

        @Test
        void nonHourLeaveWithNonZeroHoursIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Req.of().unit(LeaveUnit.FULL_DAY).hours(2).build());
            assertEquals("requestedHours is only valid for hour leave", ex.getMessage());
        }

        @Test
        void halfDayLeaveRequiresZeroHours() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Req.of().unit(LeaveUnit.HALF_DAY_MORNING).hours(0.5).build());
            assertEquals("requestedHours is only valid for hour leave", ex.getMessage());
        }
    }

    @Nested
    class LeaveAssessmentRecordTests {

        @Test
        void negativeChargedDaysIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveAssessment(true, -0.5, ApprovalTier.SUPERVISOR, false, false, List.of()));
            assertEquals("chargedDays must not be negative", ex.getMessage());
        }

        @Test
        void zeroChargedDaysIsAccepted() {
            LeaveAssessment assessment = new LeaveAssessment(false, 0, ApprovalTier.SUPERVISOR,
                    false, false, List.of("request contains no working time"));
            assertEquals(0, assessment.chargedDays(), 1e-9);
        }

        @Test
        void nullApprovalTierIsRejected() {
            assertThrows(NullPointerException.class,
                    () -> new LeaveAssessment(true, 1, null, false, false, List.of()));
        }

        @Test
        void nullViolationListIsRejected() {
            assertThrows(NullPointerException.class,
                    () -> new LeaveAssessment(true, 1, ApprovalTier.SUPERVISOR, false, false, null));
        }

        @Test
        void eligibleAssessmentCannotCarryViolations() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveAssessment(true, 1, ApprovalTier.SUPERVISOR, false, false,
                            List.of("reason is required")));
            assertEquals("eligible assessment cannot contain violations", ex.getMessage());
        }

        @Test
        void ineligibleAssessmentMustExplainItself() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveAssessment(false, 1, ApprovalTier.SUPERVISOR, false, false, List.of()));
            assertEquals("ineligible assessment must explain the violation", ex.getMessage());
        }

        @Test
        void violationListIsDefensivelyCopied() {
            List<String> mutable = new ArrayList<>();
            mutable.add("reason is required");
            LeaveAssessment assessment = new LeaveAssessment(false, 1, ApprovalTier.MANAGER,
                    false, false, mutable);
            mutable.clear();
            assertEquals(List.of("reason is required"), assessment.violations());
            assertThrows(UnsupportedOperationException.class, () -> assessment.violations().add("x"));
        }

        @Test
        void accessorsExposeEveryField() {
            LeaveAssessment assessment = new LeaveAssessment(true, 2.5, ApprovalTier.HUMAN_RESOURCES,
                    true, true, List.of());
            assertTrue(assessment.eligible());
            assertEquals(2.5, assessment.chargedDays(), 1e-9);
            assertEquals(ApprovalTier.HUMAN_RESOURCES, assessment.approvalTier());
            assertTrue(assessment.medicalCertificateRequired());
            assertTrue(assessment.handoverRequired());
            assertTrue(assessment.violations().isEmpty());
        }
    }

    @Nested
    class ChargedDaysTests {

        @Test
        void nullRequestIsRejectedInChargedDayCalculation() {
            assertThrows(NullPointerException.class, () -> POLICY.calculateChargedDays(null, WORKDAYS));
        }

        @Test
        void nullCalendarIsRejectedInChargedDayCalculation() {
            assertThrows(NullPointerException.class,
                    () -> POLICY.calculateChargedDays(Req.of().build(), null));
        }

        @Test
        void hourLeaveIsRoundedUpToHalfDays() {
            // 8h -> exactly 1.0; 7.5h -> ceil(1.875)/2 = 1.0
            assertEquals(1.0, POLICY.calculateChargedDays(
                    Req.of().unit(LeaveUnit.HOURS).hours(8).build(), WORKDAYS), 1e-9);
            assertEquals(1.0, POLICY.calculateChargedDays(
                    Req.of().unit(LeaveUnit.HOURS).hours(7.5).build(), WORKDAYS), 1e-9);
        }

        @Test
        void smallHourAmountsRoundUpToHalfADay() {
            // 4h -> 0.5; 2h -> ceil(0.5)/2 = 0.5; 1h -> ceil(0.25)/2 = 0.5; 0.5h -> 0.5
            assertEquals(0.5, POLICY.calculateChargedDays(
                    Req.of().unit(LeaveUnit.HOURS).hours(4).build(), WORKDAYS), 1e-9);
            assertEquals(0.5, POLICY.calculateChargedDays(
                    Req.of().unit(LeaveUnit.HOURS).hours(2).build(), WORKDAYS), 1e-9);
            assertEquals(0.5, POLICY.calculateChargedDays(
                    Req.of().unit(LeaveUnit.HOURS).hours(1).build(), WORKDAYS), 1e-9);
            assertEquals(0.5, POLICY.calculateChargedDays(
                    Req.of().unit(LeaveUnit.HOURS).hours(0.5).build(), WORKDAYS), 1e-9);
        }

        @Test
        void morningHalfDayOnWorkingDayChargesHalf() {
            assertEquals(0.5, POLICY.calculateChargedDays(
                    Req.of().unit(LeaveUnit.HALF_DAY_MORNING).span(MON, MON).build(), WORKDAYS), 1e-9);
        }

        @Test
        void morningHalfDayOnNonWorkingDayChargesNothing() {
            assertEquals(0, POLICY.calculateChargedDays(
                    Req.of().unit(LeaveUnit.HALF_DAY_MORNING).span(SAT, SAT).build(), WORKDAYS), 1e-9);
        }

        @Test
        void afternoonHalfDayOnWorkingDayChargesHalf() {
            assertEquals(0.5, POLICY.calculateChargedDays(
                    Req.of().unit(LeaveUnit.HALF_DAY_AFTERNOON).span(FRI, FRI).build(), WORKDAYS), 1e-9);
        }

        @Test
        void afternoonHalfDayOnNonWorkingDayChargesNothing() {
            assertEquals(0, POLICY.calculateChargedDays(
                    Req.of().unit(LeaveUnit.HALF_DAY_AFTERNOON).span(SUN, SUN).build(), WORKDAYS), 1e-9);
        }

        @Test
        void fullDayRangeCountsWorkingDaysOnly() {
            // Mon 16 .. Fri 20 = 5 working days
            assertEquals(5, POLICY.calculateChargedDays(Req.of().span(MON, FRI).build(), WORKDAYS), 1e-9);
            // Mon 16 only = 1
            assertEquals(1, POLICY.calculateChargedDays(Req.of().span(MON, MON).build(), WORKDAYS), 1e-9);
        }

        @Test
        void fullDayRangeOverPureWeekendChargesZero() {
            assertEquals(0, POLICY.calculateChargedDays(Req.of().span(SAT, SUN).build(), WORKDAYS), 1e-9);
        }

        @Test
        void fullDayRangeAcrossWeekendSkipsNonWorkingDays() {
            // Fri 20 .. Mon 23: Fri + Mon = 2; Mon 16 .. Sun 22 = 5
            assertEquals(2, POLICY.calculateChargedDays(Req.of().span(FRI, MON2).build(), WORKDAYS), 1e-9);
            assertEquals(5, POLICY.calculateChargedDays(Req.of().span(MON, SUN).build(), WORKDAYS), 1e-9);
        }

        @Test
        void holidayInsideRangeIsNotCharged() {
            // Mon 16 .. Wed 18 with Wednesday as holiday -> Mon + Tue = 2
            WorkCalendar withHoliday = d -> d.getDayOfWeek().getValue() <= 5 && !d.equals(WED);
            assertEquals(2, POLICY.calculateChargedDays(Req.of().span(MON, WED).build(), withHoliday), 1e-9);
        }

        @Test
        void singleDayRangeOnHolidayChargesZero() {
            WorkCalendar withHoliday = d -> d.getDayOfWeek().getValue() <= 5 && !d.equals(WED);
            assertEquals(0, POLICY.calculateChargedDays(Req.of().span(WED, WED).build(), withHoliday), 1e-9);
        }
    }

    @Nested
    class AssessArgumentTests {

        @Test
        void nullRequestIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> POLICY.assess(null, WORKDAYS, TODAY));
            assertEquals("request must not be null", ex.getMessage());
        }

        @Test
        void nullCalendarIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> POLICY.assess(Req.of().build(), null, TODAY));
            assertEquals("calendar must not be null", ex.getMessage());
        }

        @Test
        void nullTodayIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> POLICY.assess(Req.of().build(), WORKDAYS, null));
            assertEquals("today must not be null", ex.getMessage());
        }
    }

    @Nested
    class AssessEligibleBaselineTests {

        @Test
        void cleanOneDayAnnualRequestIsEligibleWithSupervisorTier() {
            LeaveAssessment assessment = assess(Req.of());
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(1, assessment.chargedDays(), 1e-9);
            assertEquals(ApprovalTier.SUPERVISOR, assessment.approvalTier());
            assertFalse(assessment.medicalCertificateRequired());
            assertFalse(assessment.handoverRequired());
        }

        @Test
        void twoDayAnnualRequestNeedsCompletedHandover() {
            LeaveAssessment assessment = assess(Req.of().span(MON, TUE).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(2, assessment.chargedDays(), 1e-9);
            assertTrue(assessment.handoverRequired());
            assertEquals(ApprovalTier.SUPERVISOR, assessment.approvalTier());
        }

        @Test
        void threeDayAnnualRequestNeedsCompletedHandoverAndStaysEligible() {
            LeaveAssessment assessment = assess(Req.of().span(MON, WED).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(3, assessment.chargedDays(), 1e-9);
            assertTrue(assessment.handoverRequired());
        }

        @Test
        void sameDayStartAndTodayIsNotBackdated() {
            // start == today: neither "before today" nor "after today+1y"
            LeaveAssessment assessment = assess(Req.of().span(TODAY, TODAY));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(1, assessment.chargedDays(), 1e-9);
        }

        @Test
        void startExactlyOneYearAheadIsAllowed() {
            // 2027-03-10 is a Wednesday (working day), exactly today + 1 year
            LeaveAssessment assessment = assess(Req.of().span(NEXT_YEAR_LIMIT, NEXT_YEAR_LIMIT));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(1, assessment.chargedDays(), 1e-9);
        }

        @Test
        void emergencySickLeaveMayBeBackdated() {
            // Monday 2026-03-09 is before today; SICK + emergency is the exempt combination
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .span(MON9, MON9).emergency(true).notice(3));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(1, assessment.chargedDays(), 1e-9);
            assertFalse(assessment.medicalCertificateRequired());
            assertEquals(ApprovalTier.SUPERVISOR, assessment.approvalTier());
        }

        @Test
        void zeroNoticeIsFineForSingleDayLeave() {
            // notice == 0 only matters when chargedDays > 1
            LeaveAssessment assessment = assess(Req.of().notice(0));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }
    }

    @Nested
    class AssessDateRuleTests {

        @Test
        void zeroWorkingTimeIsReported() {
            // Sat 21 .. Sun 22 with FULL_DAY unit -> no working time
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK).span(SAT, SUN));
            assertFalse(assessment.eligible());
            assertEquals(List.of("request contains no working time"), assessment.violations());
            assertEquals(0, assessment.chargedDays(), 1e-9);
            assertEquals(ApprovalTier.SUPERVISOR, assessment.approvalTier());
        }

        @Test
        void backdatedAnnualLeaveIsReported() {
            LeaveAssessment assessment = assess(Req.of().span(MON9, MON9));
            assertFalse(assessment.eligible());
            assertEquals(List.of("backdated leave is only allowed for emergency sick leave"),
                    assessment.violations());
        }

        @Test
        void backdatedNonEmergencySickLeaveIsReported() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK).span(MON9, MON9));
            assertFalse(assessment.eligible());
            assertEquals(List.of("backdated leave is only allowed for emergency sick leave"),
                    assessment.violations());
        }

        @Test
        void requestMoreThanOneYearAheadIsReported() {
            LeaveAssessment assessment = assess(Req.of().span(NEXT_YEAR_OVER, NEXT_YEAR_OVER));
            assertFalse(assessment.eligible());
            assertEquals(List.of("leave cannot be requested more than one year ahead"),
                    assessment.violations());
            assertEquals(1, assessment.chargedDays(), 1e-9);
        }

        @Test
        void spanOfExactly45CalendarDaysDoesNotTriggerSpanRule() {
            // Mon 2026-03-16 .. Wed 2026-04-29 = 45 calendar days, 33 working days.
            // Only the 15-day annual cap fires; the 45-day span rule stays silent.
            LeaveAssessment assessment = assess(Req.of().span(MON, APR29)
                    .notice(7).balances(100, 0).handover(true));
            assertFalse(assessment.eligible());
            assertEquals(List.of("annual leave cannot exceed fifteen working days per request"),
                    assessment.violations());
        }

        @Test
        void spanOf46CalendarDaysTriggersSpanRule() {
            // Mon 2026-03-16 .. Thu 2026-04-30 = 46 calendar days, 34 working days.
            // SICK: 34 > 30 also fires the 30-day rule; certificate becomes required.
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .span(MON, APR30).notice(7).balances(0, 0).handover(true));
            assertFalse(assessment.eligible());
            assertEquals(List.of(
                    "a single request cannot span more than 45 calendar days",
                    "sick leave longer than thirty days needs a separate medical process"),
                    assessment.violations());
            assertEquals(34, assessment.chargedDays(), 1e-9);
            assertTrue(assessment.medicalCertificateRequired());
            assertTrue(assessment.handoverRequired());
            // 34 > 10: DIRECTOR outranks the certificate-driven HR gate
            assertEquals(ApprovalTier.DIRECTOR, assessment.approvalTier());
        }

        @Test
        void multiDayLeaveWithoutNoticeIsReported() {
            // Two working days, notice 0 -> exactly the notice violation
            LeaveAssessment assessment = assess(Req.of().span(MON, TUE).notice(0).handover(true));
            assertFalse(assessment.eligible());
            assertEquals(List.of("multi-day leave requires advance notice"), assessment.violations());
            assertEquals(ApprovalTier.SUPERVISOR, assessment.approvalTier());
        }

        @Test
        void emergencyBypassesTheMultiDayNoticeRule() {
            // SICK emergency, two working days, notice 0 -> no notice violation
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .span(MON, TUE).notice(0).emergency(true).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void noticeOfOneStillFailsTheMultiDayRuleButThreeDaysPasses() {
            // notice 1 with two working days: the == 0 gate no longer fires
            LeaveAssessment assessment = assess(Req.of().span(MON, TUE).notice(1).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void leaveOverFiveDaysWithTwoDaysNoticeIsReported() {
            // Six working days (Mon 16 .. Mon 23), notice 2
            LeaveAssessment assessment = assess(Req.of().span(MON, MON2).notice(2).handover(true));
            assertFalse(assessment.eligible());
            assertEquals(List.of("leave longer than five days requires three days notice"),
                    assessment.violations());
        }

        @Test
        void sixWorkingDaysWithExactlyThreeDaysNoticeIsEligible() {
            LeaveAssessment assessment = assess(Req.of().span(MON, MON2).notice(3).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void leaveOverTenDaysWithSixDaysNoticeIsReported() {
            // Eleven working days (Mon 16 .. Mon 30), notice 6
            LeaveAssessment assessment = assess(Req.of().span(MON, LocalDate.of(2026, 3, 30))
                    .notice(6).balances(20, 0).handover(true));
            assertFalse(assessment.eligible());
            assertEquals(List.of("leave longer than ten days requires seven days notice"),
                    assessment.violations());
            assertEquals(11, assessment.chargedDays(), 1e-9);
            assertEquals(ApprovalTier.DIRECTOR, assessment.approvalTier());
        }

        @Test
        void tenWorkingDaysWithSixDaysNoticeIsEligible() {
            // Ten working days: the >10 gate stays closed at exactly 10, and notice 6 >= 3;
            // 10 > 3 still routes to MANAGER
            LeaveAssessment assessment = assess(Req.of().span(MON, LocalDate.of(2026, 3, 27))
                    .notice(6).balances(20, 0).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(10, assessment.chargedDays(), 1e-9);
            assertEquals(ApprovalTier.MANAGER, assessment.approvalTier());
        }

        @Test
        void elevenWorkingDaysWithSevenDaysNoticeIsEligible() {
            LeaveAssessment assessment = assess(Req.of().span(MON, LocalDate.of(2026, 3, 30))
                    .notice(7).balances(20, 0).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(11, assessment.chargedDays(), 1e-9);
            assertEquals(ApprovalTier.DIRECTOR, assessment.approvalTier());
        }

        @Test
        void emergencyBypassesTheFiveDayNoticeRule() {
            // SICK emergency, six working days, notice 2: the third operand of the 5-day gate is false
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .span(MON, MON2).notice(2).emergency(true).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(6, assessment.chargedDays(), 1e-9);
            assertTrue(assessment.medicalCertificateRequired());
            assertEquals(ApprovalTier.HUMAN_RESOURCES, assessment.approvalTier());
        }

        @Test
        void emergencyBypassesTheTenDayNoticeRule() {
            // ANNUAL emergency, eleven working days, notice 6: the third operand of the 10-day gate is false
            LeaveAssessment assessment = assess(Req.of().span(MON, LocalDate.of(2026, 3, 30))
                    .notice(6).balances(20, 0).emergency(true).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(11, assessment.chargedDays(), 1e-9);
            assertEquals(ApprovalTier.DIRECTOR, assessment.approvalTier());
        }
    }

    @Nested
    class AssessReasonRuleTests {

        @Test
        void nullReasonIsReported() {
            LeaveAssessment assessment = assess(Req.of().reason(null));
            assertFalse(assessment.eligible());
            assertEquals(List.of("reason is required"), assessment.violations());
        }

        @Test
        void blankReasonIsReported() {
            LeaveAssessment assessment = assess(Req.of().reason("   "));
            assertFalse(assessment.eligible());
            assertEquals(List.of("reason is required"), assessment.violations());
        }

        @Test
        void shortReasonIsReported() {
            LeaveAssessment assessment = assess(Req.of().reason("  abc  "));
            assertFalse(assessment.eligible());
            assertEquals(List.of("reason must contain at least five characters"), assessment.violations());
        }

        @Test
        void fourCharacterReasonIsReportedButFiveCharactersPass() {
            LeaveAssessment bad = assess(Req.of().reason("abcd"));
            assertFalse(bad.eligible());
            assertEquals(List.of("reason must contain at least five characters"), bad.violations());

            LeaveAssessment good = assess(Req.of().reason("abcde"));
            assertTrue(good.eligible());
            assertTrue(good.violations().isEmpty());
        }

        @Test
        void reasonOf301CharactersIsReported() {
            LeaveAssessment assessment = assess(Req.of().reason("a".repeat(301)));
            assertFalse(assessment.eligible());
            assertEquals(List.of("reason cannot exceed 300 characters"), assessment.violations());
        }

        @Test
        void reasonOfExactly300CharactersIsAccepted() {
            LeaveAssessment assessment = assess(Req.of().reason("a".repeat(300)));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void reasonWithLessThanSignIsReported() {
            LeaveAssessment assessment = assess(Req.of().reason("family <3"));
            assertFalse(assessment.eligible());
            assertEquals(List.of("reason contains unsupported markup"), assessment.violations());
        }

        @Test
        void reasonWithGreaterThanSignIsReported() {
            LeaveAssessment assessment = assess(Req.of().reason("urgent >rest"));
            assertFalse(assessment.eligible());
            assertEquals(List.of("reason contains unsupported markup"), assessment.violations());
        }

        @Test
        void digitOnlyReasonIsReported() {
            LeaveAssessment assessment = assess(Req.of().reason("12345"));
            assertFalse(assessment.eligible());
            assertEquals(List.of("reason cannot contain digits only"), assessment.violations());
        }

        @Test
        void mixedDigitReasonIsAccepted() {
            LeaveAssessment assessment = assess(Req.of().reason("1234a"));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }
    }

    @Nested
    class AssessEmploymentRuleTests {

        @Test
        void internCannotTakeAnnualLeave() {
            LeaveAssessment assessment = assess(Req.of().cat(EmployeeCategory.INTERN));
            assertFalse(assessment.eligible());
            assertEquals(List.of("interns are not eligible for annual leave"), assessment.violations());
            assertEquals(ApprovalTier.SUPERVISOR, assessment.approvalTier());
            assertFalse(assessment.handoverRequired());
        }

        @Test
        void internCanTakeSickLeave() {
            LeaveAssessment assessment = assess(Req.of().cat(EmployeeCategory.INTERN).type(LeaveType.SICK));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void contractorNeedsTwelveMonthsForAnnualLeave() {
            LeaveAssessment assessment = assess(Req.of().cat(EmployeeCategory.CONTRACTOR).months(11));
            assertFalse(assessment.eligible());
            assertEquals(List.of("contractors require twelve months service for annual leave"),
                    assessment.violations());
        }

        @Test
        void contractorWithExactlyTwelveMonthsPassesTheServiceGate() {
            LeaveAssessment assessment = assess(Req.of().cat(EmployeeCategory.CONTRACTOR).months(12));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void twelveMonthGateAppliesOnlyToAnnualLeave() {
            // CONTRACTOR + months 11 + SICK: no service-gate violation
            LeaveAssessment assessment = assess(Req.of().cat(EmployeeCategory.CONTRACTOR)
                    .type(LeaveType.SICK).months(11));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void probationCannotTakeNonEmergencyPersonalLeave() {
            LeaveAssessment assessment = assess(Req.of().cat(EmployeeCategory.PROBATION)
                    .type(LeaveType.PERSONAL));
            assertFalse(assessment.eligible());
            assertEquals(List.of("non-emergency personal leave is unavailable during probation"),
                    assessment.violations());
            assertEquals(ApprovalTier.HUMAN_RESOURCES, assessment.approvalTier());
        }

        @Test
        void probationEmergencyPersonalLeaveIsAllowed() {
            LeaveAssessment assessment = assess(Req.of().cat(EmployeeCategory.PROBATION)
                    .type(LeaveType.PERSONAL).emergency(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(ApprovalTier.HUMAN_RESOURCES, assessment.approvalTier());
        }

        @Test
        void employeeInFirstMonthMayOnlyTakeSickLeave() {
            // months 0 + PERSONAL: exactly the first-month violation
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL).months(0));
            assertFalse(assessment.eligible());
            assertEquals(List.of("new employees may only request sick leave in their first month"),
                    assessment.violations());
            assertEquals(ApprovalTier.MANAGER, assessment.approvalTier());
        }

        @Test
        void employeeWithOneMonthServicePassesTheFirstMonthGate() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL).months(1));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void firstMonthRuleDoesNotApplyToSickLeave() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK).months(0));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void employmentMonthsAbove600IsReported() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK).months(601));
            assertFalse(assessment.eligible());
            assertEquals(List.of("employment duration is outside the supported range"),
                    assessment.violations());
        }

        @Test
        void employmentMonthsOfExactly600IsAccepted() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK).months(600));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }
    }

    @Nested
    class AssessUnitRuleTests {

        @Test
        void morningHalfDayAcrossTwoDatesIsReported() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .unit(LeaveUnit.HALF_DAY_MORNING).span(MON, TUE));
            assertFalse(assessment.eligible());
            assertEquals(List.of("half-day leave must start and end on the same date"),
                    assessment.violations());
            assertEquals(0.5, assessment.chargedDays(), 1e-9);
            assertEquals(ApprovalTier.SUPERVISOR, assessment.approvalTier());
        }

        @Test
        void afternoonHalfDayAcrossTwoDatesIsReported() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .unit(LeaveUnit.HALF_DAY_AFTERNOON).span(MON, TUE));
            assertFalse(assessment.eligible());
            assertEquals(List.of("half-day leave must start and end on the same date"),
                    assessment.violations());
        }

        @Test
        void halfDayOnSingleDateIsAccepted() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .unit(LeaveUnit.HALF_DAY_AFTERNOON).span(MON, MON));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(0.5, assessment.chargedDays(), 1e-9);
        }

        @Test
        void hourLeaveBelowOneHourIsReported() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .unit(LeaveUnit.HOURS).hours(0.5));
            assertFalse(assessment.eligible());
            assertEquals(List.of("hour leave must be at least one hour"), assessment.violations());
            assertEquals(0.5, assessment.chargedDays(), 1e-9);
        }

        @Test
        void hourLeaveOfExactlyOneHourPassesTheMinimum() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .unit(LeaveUnit.HOURS).hours(1));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(0.5, assessment.chargedDays(), 1e-9);
        }

        @Test
        void annualHourLeaveMustUseHalfHourIncrements() {
            // 1.25h % 0.5 == 0.25 -> violation; charged rounds to 0.5
            LeaveAssessment assessment = assess(Req.of().unit(LeaveUnit.HOURS).hours(1.25));
            assertFalse(assessment.eligible());
            assertEquals(List.of("annual hour leave must use half-hour increments"),
                    assessment.violations());
            assertEquals(0.5, assessment.chargedDays(), 1e-9);
        }

        @Test
        void annualHourLeaveOnExactHalfHoursIsAccepted() {
            LeaveAssessment assessment = assess(Req.of().unit(LeaveUnit.HOURS).hours(1.5));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(0.5, assessment.chargedDays(), 1e-9);
        }

        @Test
        void halfHourIncrementGateAppliesOnlyToAnnualLeave() {
            // SICK + HOURS 1.25: no increment violation
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .unit(LeaveUnit.HOURS).hours(1.25));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void emergencyLeaveCannotUsePlannedMorningSlot() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .unit(LeaveUnit.HALF_DAY_MORNING).span(MON, MON).emergency(true));
            assertFalse(assessment.eligible());
            assertEquals(List.of("emergency leave cannot be limited to a planned morning slot"),
                    assessment.violations());
            assertEquals(0.5, assessment.chargedDays(), 1e-9);
        }

        @Test
        void emergencyLeaveMayUseAfternoonSlot() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .unit(LeaveUnit.HALF_DAY_AFTERNOON).span(MON, MON).emergency(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(0.5, assessment.chargedDays(), 1e-9);
        }
    }

    @Nested
    class AssessAnnualRuleTests {

        @Test
        void insufficientAnnualBalanceIsReported() {
            // 6 working days (Mon 16 .. Mon 23) against 5 remaining + 0 carry-over
            LeaveAssessment assessment = assess(Req.of().span(MON, MON2).notice(3).balances(5, 0).handover(true));
            assertFalse(assessment.eligible());
            assertEquals(List.of("insufficient annual leave balance"), assessment.violations());
        }

        @Test
        void annualBalanceMayBeMetExactlyWithCarryOver() {
            // available = 5 + 1 = 6 equals the 6 working days requested
            LeaveAssessment assessment = assess(Req.of().span(MON, MON2).notice(3).balances(5, 1).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(6, assessment.chargedDays(), 1e-9);
        }

        @Test
        void annualLeaveOverFifteenWorkingDaysIsReported() {
            // 16 working days (Mon 2026-03-16 .. Mon 2026-04-06)
            LeaveAssessment assessment = assess(Req.of().span(MON, APR6)
                    .notice(7).balances(50, 0).handover(true));
            assertFalse(assessment.eligible());
            assertEquals(List.of("annual leave cannot exceed fifteen working days per request"),
                    assessment.violations());
            assertEquals(16, assessment.chargedDays(), 1e-9);
            assertEquals(ApprovalTier.DIRECTOR, assessment.approvalTier());
        }

        @Test
        void annualLeaveOfExactlyFifteenWorkingDaysIsAccepted() {
            // 15 working days (Mon 2026-03-16 .. Fri 2026-04-03)
            LeaveAssessment assessment = assess(Req.of().span(MON, LocalDate.of(2026, 4, 3))
                    .notice(7).balances(50, 0).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(15, assessment.chargedDays(), 1e-9);
            assertEquals(ApprovalTier.DIRECTOR, assessment.approvalTier());
        }

        @Test
        void carryOverAboveFiveDaysIsReported() {
            LeaveAssessment assessment = assess(Req.of().balances(10, 5.5));
            assertFalse(assessment.eligible());
            assertEquals(List.of("carry-over balance cannot exceed five days"), assessment.violations());
        }

        @Test
        void carryOverOfExactlyFiveDaysIsAccepted() {
            LeaveAssessment assessment = assess(Req.of().balances(10, 5));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void carryOverMustBeUsedBeforeApril() {
            // carry-over 1 with a start in April (2026-04-06)
            LeaveAssessment assessment = assess(Req.of().span(APR6, APR6).balances(10, 1));
            assertFalse(assessment.eligible());
            assertEquals(List.of("carry-over leave must be used before April"), assessment.violations());
        }

        @Test
        void carryOverInMarchIsAccepted() {
            LeaveAssessment assessment = assess(Req.of().span(MON, MON).balances(10, 1));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void zeroCarryOverInAprilIsAccepted() {
            LeaveAssessment assessment = assess(Req.of().span(APR6, APR6).balances(10, 0));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void annualLeaveBeforeSixMonthsOfServiceIsReported() {
            LeaveAssessment assessment = assess(Req.of().months(5));
            assertFalse(assessment.eligible());
            assertEquals(List.of("annual leave requires six months service"), assessment.violations());
        }

        @Test
        void annualLeaveWithExactlySixMonthsOfServiceIsAccepted() {
            LeaveAssessment assessment = assess(Req.of().months(6));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void remoteCapableEmployeeWithZeroNoticeOneDayLeaveIsReported() {
            LeaveAssessment assessment = assess(Req.of().remote(true).notice(0));
            assertFalse(assessment.eligible());
            assertEquals(List.of(
                    "remote-capable employees must consider remote work for urgent one-day leave"),
                    assessment.violations());
        }

        @Test
        void remoteCapableEmployeeWithOneDayNoticeIsAccepted() {
            LeaveAssessment assessment = assess(Req.of().remote(true).notice(1));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void remoteGateNeedsExactlyOneChargedDayForAnnualLeave() {
            // remote-capable ANNUAL with two working days: the chargedDays == 1 operand is false
            LeaveAssessment assessment = assess(Req.of().remote(true).span(MON, TUE).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertTrue(assessment.handoverRequired());
            assertEquals(ApprovalTier.SUPERVISOR, assessment.approvalTier());
        }
    }

    @Nested
    class AssessSickRuleTests {

        @Test
        void sickLeaveOverThirtyWorkingDaysIsReported() {
            // 31 working days (Mon 2026-03-16 .. Mon 2026-04-27 = 43 calendar days)
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .span(MON, APR27).notice(7).balances(0, 0).handover(true));
            assertFalse(assessment.eligible());
            assertEquals(List.of("sick leave longer than thirty days needs a separate medical process"),
                    assessment.violations());
            assertEquals(31, assessment.chargedDays(), 1e-9);
            assertTrue(assessment.medicalCertificateRequired());
            assertTrue(assessment.handoverRequired());
            // 31 > 10: the DIRECTOR gate outranks the certificate-driven HR gate
            assertEquals(ApprovalTier.DIRECTOR, assessment.approvalTier());
        }

        @Test
        void sickLeaveOfExactlyThirtyWorkingDaysIsAccepted() {
            // 30 working days (Mon 2026-03-16 .. Fri 2026-04-24)
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .span(MON, LocalDate.of(2026, 4, 24)).notice(7).balances(0, 0).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(30, assessment.chargedDays(), 1e-9);
            assertTrue(assessment.medicalCertificateRequired());
            // 30 > 10: DIRECTOR outranks the certificate-driven HR gate
            assertEquals(ApprovalTier.DIRECTOR, assessment.approvalTier());
        }

        @Test
        void hourSickLeaveOverFourHoursIsReported() {
            // 4.5h rounds to 1.0 charged days
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .unit(LeaveUnit.HOURS).hours(4.5));
            assertFalse(assessment.eligible());
            assertEquals(List.of("hour-based sick leave cannot exceed four hours"),
                    assessment.violations());
            assertEquals(1.0, assessment.chargedDays(), 1e-9);
            assertFalse(assessment.medicalCertificateRequired());
            assertEquals(ApprovalTier.SUPERVISOR, assessment.approvalTier());
        }

        @Test
        void hourSickLeaveOfExactlyFourHoursIsAccepted() {
            // 4h = half a working day (ceil(0.5 * 2) / 2 = 0.5); 4h triggers the certificate rule
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .unit(LeaveUnit.HOURS).hours(4));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(0.5, assessment.chargedDays(), 1e-9);
            assertTrue(assessment.medicalCertificateRequired());
            assertEquals(ApprovalTier.HUMAN_RESOURCES, assessment.approvalTier());
        }

        @Test
        void emergencySickLeaveWithNoticeOver30IsReported() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .emergency(true).notice(31));
            assertFalse(assessment.eligible());
            assertEquals(List.of("emergency sick leave has inconsistent notice information"),
                    assessment.violations());
        }

        @Test
        void emergencySickLeaveWithExactly30DaysNoticeIsAccepted() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .emergency(true).notice(30));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void plannedSickLeaveStartingOnSundayIsReported() {
            // Sun 2026-03-15 .. Mon 2026-03-16: one working day, non-emergency
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK).span(SUN15, MON).notice(3));
            assertFalse(assessment.eligible());
            assertEquals(List.of("planned sick leave cannot start on Sunday"), assessment.violations());
            assertEquals(1, assessment.chargedDays(), 1e-9);
        }

        @Test
        void emergencySickLeaveMayStartOnSunday() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .span(SUN15, MON).notice(3).emergency(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void sickBalanceAbove365IsReported() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK).balances(366, 0));
            assertFalse(assessment.eligible());
            assertEquals(List.of("sick leave balance is outside the supported range"),
                    assessment.violations());
        }

        @Test
        void sickBalanceOfExactly365IsAccepted() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK).balances(365, 0));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }
    }

    @Nested
    class AssessPersonalRuleTests {

        @Test
        void personalLeaveOverThreeDaysWithoutEmergencyIsReported() {
            // 4 working days (Mon 16 .. Thu 19)
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL).span(MON, THU).handover(true));
            assertFalse(assessment.eligible());
            assertEquals(List.of("personal leave cannot exceed three days without an emergency"),
                    assessment.violations());
            assertEquals(ApprovalTier.MANAGER, assessment.approvalTier());
        }

        @Test
        void personalLeaveOfExactlyThreeDaysWithoutEmergencyIsAccepted() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL).span(MON, WED).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertTrue(assessment.handoverRequired());
            assertEquals(ApprovalTier.MANAGER, assessment.approvalTier());
        }

        @Test
        void emergencyPersonalLeaveMayExceedThreeDays() {
            // 4 working days with emergency flag: the 3-day gate is bypassed
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL)
                    .span(MON, THU).emergency(true).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void personalLeaveOverFiveDaysIsReportedEvenInEmergencies() {
            // 6 working days (Mon 16 .. Mon 23), emergency keeps the 3-day gate quiet
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL)
                    .span(MON, MON2).emergency(true).handover(true));
            assertFalse(assessment.eligible());
            assertEquals(List.of("personal leave cannot exceed five days"), assessment.violations());
            assertEquals(ApprovalTier.MANAGER, assessment.approvalTier());
        }

        @Test
        void personalLeaveOfExactlyFiveDaysInEmergenciesIsAccepted() {
            // 5 working days (Mon 16 .. Fri 20)
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL)
                    .span(MON, FRI).emergency(true).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(5, assessment.chargedDays(), 1e-9);
        }

        @Test
        void hourPersonalLeaveOverFourHoursIsReported() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL)
                    .unit(LeaveUnit.HOURS).hours(4.5));
            assertFalse(assessment.eligible());
            assertEquals(List.of("hour-based personal leave cannot exceed four hours"),
                    assessment.violations());
            assertEquals(1.0, assessment.chargedDays(), 1e-9);
            assertFalse(assessment.handoverRequired());
            assertEquals(ApprovalTier.MANAGER, assessment.approvalTier());
        }

        @Test
        void hourPersonalLeaveOfExactlyFourHoursIsAccepted() {
            // 4h = half a working day
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL)
                    .unit(LeaveUnit.HOURS).hours(4));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(0.5, assessment.chargedDays(), 1e-9);
        }

        @Test
        void contractorPersonalLeaveOverOneDayIsReported() {
            // 2 working days, contractor
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL)
                    .cat(EmployeeCategory.CONTRACTOR).span(MON, TUE));
            assertFalse(assessment.eligible());
            assertEquals(List.of("contractor personal leave is limited to one day"),
                    assessment.violations());
            assertFalse(assessment.handoverRequired());
            assertEquals(ApprovalTier.MANAGER, assessment.approvalTier());
        }

        @Test
        void contractorPersonalLeaveOfExactlyOneDayIsAccepted() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL)
                    .cat(EmployeeCategory.CONTRACTOR).span(MON, MON));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void personalLeaveWithoutOneDayNoticeIsReported() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL).notice(0));
            assertFalse(assessment.eligible());
            assertEquals(List.of("personal leave requires one day notice"), assessment.violations());
        }

        @Test
        void personalLeaveWithExactlyOneDayNoticeIsAccepted() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL).notice(1));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void emergencyPersonalLeaveNeedsNoNotice() {
            // emergency true bypasses the one-day notice gate (second operand false)
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL).notice(0).emergency(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
            assertEquals(ApprovalTier.MANAGER, assessment.approvalTier());
        }

        @Test
        void remoteCapableEmployeeWithShortPersonalAbsenceIsReported() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL).remote(true));
            assertFalse(assessment.eligible());
            assertEquals(List.of("short personal absence should use remote work when available"),
                    assessment.violations());
            assertEquals(ApprovalTier.MANAGER, assessment.approvalTier());
        }

        @Test
        void remoteCapableEmployeeWithLongerPersonalAbsenceIsAccepted() {
            // 2 working days: charged > 1 so the remote nudge does not fire
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL)
                    .remote(true).span(MON, TUE).handover(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }

        @Test
        void remoteNudgeIsSkippedForEmergencies() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.PERSONAL)
                    .remote(true).emergency(true));
            assertTrue(assessment.eligible());
            assertTrue(assessment.violations().isEmpty());
        }
    }

    @Nested
    class MedicalCertificateTests {

        @Test
        void certificateNotRequiredForTwoDaySickLeave() {
            // REGULAR, non-emergency, FULL_DAY, 2 working days: no certificate rule fires
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK).span(MON, TUE).handover(true));
            assertFalse(assessment.medicalCertificateRequired());
            assertTrue(assessment.eligible());
            assertEquals(ApprovalTier.SUPERVISOR, assessment.approvalTier());
        }

        @Test
        void certificateRequiredAboveTwoChargedDays() {
            // 3 working days crosses the first gate
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK).span(MON, WED).handover(true));
            assertTrue(assessment.medicalCertificateRequired());
            assertTrue(assessment.eligible());
            assertEquals(ApprovalTier.HUMAN_RESOURCES, assessment.approvalTier());
        }

        @Test
        void probationRequiresCertificateAboveOneChargedDay() {
            // PROBATION + 2 working days: the probation gate fires while the >2 gate stays closed
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .cat(EmployeeCategory.PROBATION).span(MON, TUE));
            assertTrue(assessment.medicalCertificateRequired());
            assertTrue(assessment.eligible());
            assertEquals(ApprovalTier.HUMAN_RESOURCES, assessment.approvalTier());
        }

        @Test
        void probationSingleDayNeedsNoCertificate() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .cat(EmployeeCategory.PROBATION).span(MON, MON));
            assertFalse(assessment.medicalCertificateRequired());
            assertTrue(assessment.eligible());
        }

        @Test
        void emergencyRequiresCertificateAboveOneChargedDay() {
            // emergency + 2 working days: the emergency gate fires while the >2 gate stays closed
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .span(MON, TUE).emergency(true).handover(true));
            assertTrue(assessment.medicalCertificateRequired());
            assertTrue(assessment.eligible());
        }

        @Test
        void emergencySingleDayNeedsNoCertificate() {
            // covered by backdated emergency test, but asserted on a non-backdated date too
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .span(MON, MON).emergency(true));
            assertFalse(assessment.medicalCertificateRequired());
            assertTrue(assessment.eligible());
        }

        @Test
        void exactlyFourHoursRequiresCertificate() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .unit(LeaveUnit.HOURS).hours(4));
            assertTrue(assessment.medicalCertificateRequired());
        }

        @Test
        void threeAndAHalfHoursNeedsNoCertificate() {
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK)
                    .unit(LeaveUnit.HOURS).hours(3.5));
            assertFalse(assessment.medicalCertificateRequired());
            assertTrue(assessment.eligible());
            assertEquals(0.5, assessment.chargedDays(), 1e-9);
        }
    }

    @Nested
    class HandoverRuleTests {

        @Test
        void missingHandoverIsReportedWhenRequired() {
            // ANNUAL with 2 working days requires handover; handoverCompleted=false
            LeaveAssessment assessment = assess(Req.of().span(MON, TUE).handover(false));
            assertFalse(assessment.eligible());
            assertEquals(List.of("handover is required for this request"), assessment.violations());
            assertTrue(assessment.handoverRequired());
            assertEquals(ApprovalTier.SUPERVISOR, assessment.approvalTier());
        }

        @Test
        void handoverRequiredForAnyTypeAtThreeChargedDays() {
            // SICK REGULAR 3 days: the >= 3 gate alone fires
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK).span(MON, WED).handover(true));
            assertTrue(assessment.handoverRequired());
        }

        @Test
        void handoverRequiredForAnnualAtTwoChargedDays() {
            // INTERN + ANNUAL + 2 days, handover not completed: >= 3 closed, the ANNUAL >= 2 gate
            // fires, and both the intern rule and the missing-handover rule land in the list.
            LeaveAssessment assessment = assess(Req.of().cat(EmployeeCategory.INTERN).span(MON, TUE));
            assertTrue(assessment.handoverRequired());
            assertEquals(List.of(
                    "interns are not eligible for annual leave",
                    "handover is required for this request"),
                    assessment.violations());
        }

        @Test
        void annualSingleDayNeedsNoHandover() {
            LeaveAssessment assessment = assess(Req.of().span(MON, MON));
            assertFalse(assessment.handoverRequired());
        }

        @Test
        void handoverRequiredForRegularEmployeeAboveOneChargedDay() {
            // INTERN + SICK + 2 days: >= 3 closed, not ANNUAL, but REGULAR gate fires... INTERN is not
            // REGULAR, so this must stay false; the REGULAR side is covered by the 2-day annual test.
            LeaveAssessment assessment = assess(Req.of().cat(EmployeeCategory.INTERN)
                    .type(LeaveType.SICK).span(MON, TUE));
            assertFalse(assessment.handoverRequired());
            assertTrue(assessment.eligible());
        }

        @Test
        void handoverRequiredForRegularNonAnnualAboveOneChargedDay() {
            // REGULAR + SICK + 2 days: >= 3 closed, not ANNUAL, REGULAR > 1 fires
            LeaveAssessment assessment = assess(Req.of().type(LeaveType.SICK).span(MON, TUE).handover(true));
            assertTrue(assessment.handoverRequired());
            assertTrue(assessment.eligible());
        }

        @Test
        void handoverRequiredForFridayStartAboveOneChargedDay() {
            // INTERN + SICK starting Friday 2026-03-13 .. Monday 2026-03-16 = 2 working days:
            // >= 3 closed, not ANNUAL, not REGULAR, but Friday gate fires
            LeaveAssessment assessment = assess(Req.of().cat(EmployeeCategory.INTERN)
                    .type(LeaveType.SICK).span(FRI13, MON).handover(true));
            assertTrue(assessment.handoverRequired());
            assertTrue(assessment.eligible());
            assertEquals(2, assessment.chargedDays(), 1e-9);
        }

        @Test
        void fridayStartWithSingleChargedDayNeedsNoHandover() {
            LeaveAssessment assessment = assess(Req.of().cat(EmployeeCategory.INTERN)
                    .type(LeaveType.SICK).span(FRI13, FRI13));
            assertFalse(assessment.handoverRequired());
            assertTrue(assessment.eligible());
        }
    }

    @Nested
    class ApprovalTierMatrixTests {

        @Test
        void oneDayAnnualRegularIsSupervisor() {
            assertEquals(ApprovalTier.SUPERVISOR, assess(Req.of()).approvalTier());
        }

        @Test
        void threeDayAnnualRegularIsSupervisor() {
            // 3 working days: not > 10, not contractor > 3, not > 3, not PERSONAL
            assertEquals(ApprovalTier.SUPERVISOR,
                    assess(Req.of().span(MON, WED).handover(true)).approvalTier());
        }

        @Test
        void fourDayAnnualRegularIsManager() {
            // 4 working days: the > 3 gate fires before the PERSONAL gate
            assertEquals(ApprovalTier.MANAGER,
                    assess(Req.of().span(MON, THU).handover(true)).approvalTier());
        }

        @Test
        void singleDayPersonalIsManager() {
            // The PERSONAL type gate fires at any size
            assertEquals(ApprovalTier.MANAGER,
                    assess(Req.of().type(LeaveType.PERSONAL)).approvalTier());
        }

        @Test
        void tenChargedDaysAnnualIsManagerButElevenGoesDirector() {
            // 10 > 3 lands on the MANAGER gate; only > 10 reaches DIRECTOR
            assertEquals(ApprovalTier.MANAGER,
                    assess(Req.of().span(MON, LocalDate.of(2026, 3, 27))
                            .notice(6).balances(20, 0).handover(true)).approvalTier());
            assertEquals(ApprovalTier.DIRECTOR,
                    assess(Req.of().span(MON, LocalDate.of(2026, 3, 30))
                            .notice(7).balances(20, 0).handover(true)).approvalTier());
        }

        @Test
        void contractorOverThreeChargedDaysGoesStraightToDirector() {
            // CONTRACTOR + 4 working days: contractor gate fires even though the amount is small
            assertEquals(ApprovalTier.DIRECTOR,
                    assess(Req.of().cat(EmployeeCategory.CONTRACTOR)
                            .span(MON, THU).handover(true)).approvalTier());
        }

        @Test
        void contractorAtExactlyThreeChargedDaysStaysSupervisor() {
            assertEquals(ApprovalTier.SUPERVISOR,
                    assess(Req.of().cat(EmployeeCategory.CONTRACTOR)
                            .span(MON, WED).handover(true)).approvalTier());
        }

        @Test
        void certificateRequiredRoutesToHumanResources() {
            // SICK 2 days + probation: certificate gate (not the DIRECTOR or contractor gates)
            assertEquals(ApprovalTier.HUMAN_RESOURCES,
                    assess(Req.of().type(LeaveType.SICK).cat(EmployeeCategory.PROBATION)
                            .span(MON, TUE)).approvalTier());
        }

        @Test
        void probationRoutesToHumanResourcesEvenWithoutCertificate() {
            // PROBATION + SICK + 1 day: no certificate, but the category gate fires
            assertEquals(ApprovalTier.HUMAN_RESOURCES,
                    assess(Req.of().type(LeaveType.SICK).cat(EmployeeCategory.PROBATION)
                            .span(MON, MON)).approvalTier());
        }

        @Test
        void directorGateBeatsProbationAndCertificate() {
            // PROBATION + 11 working days: > 10 wins over the HR gates
            assertEquals(ApprovalTier.DIRECTOR,
                    assess(Req.of().type(LeaveType.SICK).cat(EmployeeCategory.PROBATION)
                            .span(MON, LocalDate.of(2026, 3, 30))
                            .notice(7).balances(20, 0).handover(true)).approvalTier());
        }

        @Test
        void contractorCertificateBeatsManagerGate() {
            // CONTRACTOR + SICK + 3 days: certificate (3 > 2) fires before any MANAGER gate,
            // while the contractor DIRECTOR gate (charged > 3) stays closed
            assertEquals(ApprovalTier.HUMAN_RESOURCES,
                    assess(Req.of().type(LeaveType.SICK).cat(EmployeeCategory.CONTRACTOR)
                            .span(MON, WED).handover(true)).approvalTier());
        }
    }}

    // ------------------------------------------------------------------
    // ------------------------------------------------------------------
    // Task subsystem (provincial additions)
    // ------------------------------------------------------------------
    @Nested
    class TaskModuleTests {

    // Fixed reference instants. "Now" is 2026-06-15T02:00:00Z; the sample task was created 2026-01-05T09:00:00Z.
    private static final Instant CREATED_AT = Instant.parse("2026-01-05T09:00:00Z");
    private static final Instant NOW = Instant.parse("2026-06-15T02:00:00Z");
    private static final Instant NOW_PLUS_1H = Instant.parse("2026-06-15T03:00:00Z");
    private static final Instant NOW_PLUS_2H = Instant.parse("2026-06-15T04:00:00Z");
    private static final Instant NOW_PLUS_4H = Instant.parse("2026-06-15T06:00:00Z");
    private static final Instant NOW_PLUS_5H = Instant.parse("2026-06-15T07:00:00Z");
    private static final Instant NOW_PLUS_24H = Instant.parse("2026-06-16T02:00:00Z");
    private static final Instant NOW_PLUS_25H = Instant.parse("2026-06-16T03:00:00Z");
    private static final Instant NOW_PLUS_48H = Instant.parse("2026-06-17T02:00:00Z");
    private static final Instant NOW_PLUS_72H = Instant.parse("2026-06-18T02:00:00Z");
    private static final Instant NOW_PLUS_73H = Instant.parse("2026-06-18T03:00:00Z");
    private static final Instant NOW_MINUS_1H = Instant.parse("2026-06-15T01:00:00Z");
    private static final Instant DAY_BEFORE = Instant.parse("2026-06-14T02:00:00Z");
    private static final Instant TWO_DAYS_BEFORE = Instant.parse("2026-06-13T02:00:00Z");
    private static final Instant THREE_DAYS_BEFORE = Instant.parse("2026-06-12T02:00:00Z");
    private static final Instant FOUR_DAYS_BEFORE = Instant.parse("2026-06-11T02:00:00Z");
    private static final Instant SIX_DAYS_BEFORE = Instant.parse("2026-06-09T02:00:00Z");
    private static final Instant SEVEN_DAYS_BEFORE = Instant.parse("2026-06-08T02:00:00Z");

    // Both collaborators are stateless, so one instance per suite is safe; every test builds fresh tasks/contexts.
    private static final TaskPolicy POLICY = new TaskPolicy();
    private static final TaskStateMachine MACHINE = new TaskStateMachine();

    private static TaskBuilder task() {
        return new TaskBuilder();
    }

    private static CtxBuilder ctx() {
        return new CtxBuilder();
    }

    /** Default task in the given status, with the timestamps that status demands. */
    private static TaskBuilder taskIn(TaskStatus status) {
        TaskBuilder builder = task().status(status);
        if (status == TaskStatus.IN_PROGRESS || status == TaskStatus.BLOCKED || status == TaskStatus.IN_REVIEW) {
            builder.startedAt(DAY_BEFORE);
        } else if (status == TaskStatus.COMPLETED) {
            builder.startedAt(DAY_BEFORE).completedAt(DAY_BEFORE);
        }
        return builder;
    }

    /** Fluent builder for the 18-component Task record; defaults describe a valid DRAFT task. */
    private static final class TaskBuilder {
        private String id = "T-100";
        private String title = "Prepare quarterly report";
        private String description;
        private String creatorId = "creator-1";
        private String assigneeId = "assignee-1";
        private TaskStatus status = TaskStatus.DRAFT;
        private TaskPriority priority = TaskPriority.NORMAL;
        private RecurrenceRule recurrence = RecurrenceRule.NONE;
        private Instant createdAt = CREATED_AT;
        private Instant dueAt;
        private Instant startedAt;
        private Instant completedAt;
        private int estimatedMinutes = 90;
        private int actualMinutes;
        private List<String> dependencyIds = List.of();
        private Set<String> tags = Set.of("ops");
        private boolean confidential;
        private boolean reviewRequired;

        TaskBuilder id(String value) { this.id = value; return this; }

        TaskBuilder title(String value) { this.title = value; return this; }

        TaskBuilder description(String value) { this.description = value; return this; }

        TaskBuilder creatorId(String value) { this.creatorId = value; return this; }

        TaskBuilder assigneeId(String value) { this.assigneeId = value; return this; }

        TaskBuilder status(TaskStatus value) { this.status = value; return this; }

        TaskBuilder priority(TaskPriority value) { this.priority = value; return this; }

        TaskBuilder recurrence(RecurrenceRule value) { this.recurrence = value; return this; }

        TaskBuilder createdAt(Instant value) { this.createdAt = value; return this; }

        TaskBuilder dueAt(Instant value) { this.dueAt = value; return this; }

        TaskBuilder startedAt(Instant value) { this.startedAt = value; return this; }

        TaskBuilder completedAt(Instant value) { this.completedAt = value; return this; }

        TaskBuilder estimatedMinutes(int value) { this.estimatedMinutes = value; return this; }

        TaskBuilder actualMinutes(int value) { this.actualMinutes = value; return this; }

        TaskBuilder dependencyIds(List<String> value) { this.dependencyIds = value; return this; }

        TaskBuilder tags(Set<String> value) { this.tags = value; return this; }

        TaskBuilder confidential(boolean value) { this.confidential = value; return this; }

        TaskBuilder reviewRequired(boolean value) { this.reviewRequired = value; return this; }

        Task build() {
            return new Task(id, title, description, creatorId, assigneeId, status, priority, recurrence,
                    createdAt, dueAt, startedAt, completedAt, estimatedMinutes, actualMinutes,
                    dependencyIds, tags, confidential, reviewRequired);
        }
    }

    /** Fluent builder for the 12-component TaskContext record; defaults: creator-1 at NOW, everything active. */
    private static final class CtxBuilder {
        private String actorId = "creator-1";
        private Instant now = NOW;
        private boolean manager;
        private boolean administrator;
        private boolean dependenciesComplete = true;
        private boolean assigneeActive = true;
        private boolean projectActive = true;
        private boolean withinWorkingHours = true;
        private boolean notificationsEnabled = true;
        private int previousReopens;
        private int assigneeOpenTaskCount;
        private int overdueTaskCount;

        CtxBuilder actorId(String value) { this.actorId = value; return this; }

        CtxBuilder now(Instant value) { this.now = value; return this; }

        CtxBuilder manager(boolean value) { this.manager = value; return this; }

        CtxBuilder administrator(boolean value) { this.administrator = value; return this; }

        CtxBuilder dependenciesComplete(boolean value) { this.dependenciesComplete = value; return this; }

        CtxBuilder assigneeActive(boolean value) { this.assigneeActive = value; return this; }

        CtxBuilder projectActive(boolean value) { this.projectActive = value; return this; }

        CtxBuilder withinWorkingHours(boolean value) { this.withinWorkingHours = value; return this; }

        CtxBuilder notificationsEnabled(boolean value) { this.notificationsEnabled = value; return this; }

        CtxBuilder previousReopens(int value) { this.previousReopens = value; return this; }

        CtxBuilder assigneeOpenTaskCount(int value) { this.assigneeOpenTaskCount = value; return this; }

        CtxBuilder overdueTaskCount(int value) { this.overdueTaskCount = value; return this; }

        TaskContext build() {
            return new TaskContext(actorId, now, manager, administrator, dependenciesComplete, assigneeActive,
                    projectActive, withinWorkingHours, notificationsEnabled, previousReopens,
                    assigneeOpenTaskCount, overdueTaskCount);
        }
    }

    @Nested
    class TaskValidationTests {

        @Test
        void rejectsNullId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().id(null).build());
            assertEquals("id must not be blank", ex.getMessage());
        }

        @Test
        void rejectsBlankId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().id("   ").build());
            assertEquals("id must not be blank", ex.getMessage());
        }

        @Test
        void rejectsNullTitle() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().title(null).build());
            assertEquals("title must not be blank", ex.getMessage());
        }

        @Test
        void rejectsBlankTitle() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().title("\t ").build());
            assertEquals("title must not be blank", ex.getMessage());
        }

        @Test
        void rejectsNullCreatorId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().creatorId(null).build());
            assertEquals("creatorId must not be blank", ex.getMessage());
        }

        @Test
        void rejectsBlankCreatorId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().creatorId("  ").build());
            assertEquals("creatorId must not be blank", ex.getMessage());
        }

        @Test
        void rejectsNullStatus() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> task().status(null).build());
            assertEquals("status must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullPriority() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> task().priority(null).build());
            assertEquals("priority must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullRecurrence() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> task().recurrence(null).build());
            assertEquals("recurrence must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullCreatedAt() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> task().createdAt(null).build());
            assertEquals("createdAt must not be null", ex.getMessage());
        }

        @Test
        void rejectsNullDependencyIds() {
            // Objects.requireNonNull without a message.
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> task().dependencyIds(null).build());
            assertNull(ex.getMessage());
        }

        @Test
        void rejectsNullTags() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> task().tags(null).build());
            assertNull(ex.getMessage());
        }

        @Test
        void acceptsTitleOf120CharactersAndRejects121() {
            String longest = "T".repeat(120);
            assertEquals(longest, task().title(longest).build().title());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().title("T".repeat(121)).build());
            assertEquals("title cannot exceed 120 characters", ex.getMessage());
        }

        @Test
        void acceptsDescriptionUpTo5000CharactersAndRejects5001() {
            assertEquals(5000, task().description("D".repeat(5000)).build().description().length());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().description("D".repeat(5001)).build());
            assertEquals("description cannot exceed 5000 characters", ex.getMessage());
        }

        @Test
        void acceptsNullDescription() {
            assertNull(task().description(null).build().description());
        }

        @Test
        void acceptsDueAtEqualToCreatedAtAndRejectsEarlierDueAt() {
            assertEquals(CREATED_AT, task().dueAt(CREATED_AT).build().dueAt());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().dueAt(CREATED_AT.minusSeconds(1)).build());
            assertEquals("dueAt must not be before createdAt", ex.getMessage());
        }

        @Test
        void estimatedMinutesAcceptsZeroAnd100000RejectsMinusOneAnd100001() {
            assertEquals(0, task().estimatedMinutes(0).build().estimatedMinutes());
            assertEquals(100_000, task().estimatedMinutes(100_000).build().estimatedMinutes());
            IllegalArgumentException low = assertThrows(IllegalArgumentException.class,
                    () -> task().estimatedMinutes(-1).build());
            assertEquals("estimatedMinutes is outside the supported range", low.getMessage());
            IllegalArgumentException high = assertThrows(IllegalArgumentException.class,
                    () -> task().estimatedMinutes(100_001).build());
            assertEquals("estimatedMinutes is outside the supported range", high.getMessage());
        }

        @Test
        void actualMinutesAcceptsZeroAnd200000RejectsMinusOneAnd200001() {
            assertEquals(0, task().actualMinutes(0).build().actualMinutes());
            assertEquals(200_000, task().actualMinutes(200_000).build().actualMinutes());
            IllegalArgumentException low = assertThrows(IllegalArgumentException.class,
                    () -> task().actualMinutes(-1).build());
            assertEquals("actualMinutes is outside the supported range", low.getMessage());
            IllegalArgumentException high = assertThrows(IllegalArgumentException.class,
                    () -> task().actualMinutes(200_001).build());
            assertEquals("actualMinutes is outside the supported range", high.getMessage());
        }

        @Test
        void rejectsTaskDependingOnItself() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().id("T-100").dependencyIds(List.of("T-100")).build());
            assertEquals("task cannot depend on itself", ex.getMessage());
        }

        @Test
        void rejectsDuplicateDependencies() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> task().dependencyIds(Arrays.asList("d1", "d1")).build());
            assertEquals("dependencies must be unique", ex.getMessage());
        }

        @Test
        void requiresStartedAtOnceWorkBegins() {
            for (TaskStatus active : List.of(TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED, TaskStatus.IN_REVIEW)) {
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> taskIn(active).startedAt(null).build());
                assertEquals("startedAt is required after work begins", ex.getMessage());
            }
            IllegalArgumentException completed = assertThrows(IllegalArgumentException.class,
                    () -> task().status(TaskStatus.COMPLETED).completedAt(NOW).build());
            assertEquals("startedAt is required after work begins", completed.getMessage());
        }

        @Test
        void requiresCompletedAtForCompletedTask() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> taskIn(TaskStatus.COMPLETED).completedAt(null).build());
            assertEquals("completedAt is required for completed task", ex.getMessage());
        }

        @Test
        void rejectsCompletedAtOutsideCompletedStatus() {
            IllegalArgumentException draft = assertThrows(IllegalArgumentException.class,
                    () -> task().status(TaskStatus.DRAFT).completedAt(NOW).build());
            assertEquals("completedAt is only valid for completed task", draft.getMessage());
            IllegalArgumentException cancelled = assertThrows(IllegalArgumentException.class,
                    () -> task().status(TaskStatus.CANCELLED).completedAt(NOW).build());
            assertEquals("completedAt is only valid for completed task", cancelled.getMessage());
        }

        @Test
        void trimsIdTitleCreatorAndAssignee() {
            Task trimmed = task().id("  T-7  ").title("  Fix login bug  ").creatorId("  u9  ")
                    .assigneeId("  a5  ").build();
            assertEquals("T-7", trimmed.id());
            assertEquals("Fix login bug", trimmed.title());
            assertEquals("u9", trimmed.creatorId());
            assertEquals("a5", trimmed.assigneeId());
            assertEquals("", task().assigneeId("   ").build().assigneeId());
            assertNull(task().assigneeId(null).build().assigneeId());
        }

        @Test
        void isTerminalMatchesOnlyEndStates() {
            assertTrue(taskIn(TaskStatus.COMPLETED).build().isTerminal());
            assertTrue(task().status(TaskStatus.CANCELLED).build().isTerminal());
            assertTrue(task().status(TaskStatus.ARCHIVED).build().isTerminal());
            assertFalse(task().status(TaskStatus.DRAFT).build().isTerminal());
            assertFalse(task().status(TaskStatus.OPEN).build().isTerminal());
            assertFalse(taskIn(TaskStatus.IN_PROGRESS).build().isTerminal());
            assertFalse(taskIn(TaskStatus.BLOCKED).build().isTerminal());
            assertFalse(taskIn(TaskStatus.IN_REVIEW).build().isTerminal());
        }

        @Test
        void exposesEveryComponent() {
            Task t = task().id("T-1").title("Ship release").description("Notes").creatorId("c1").assigneeId("a1")
                    .status(TaskStatus.OPEN).priority(TaskPriority.HIGH).recurrence(RecurrenceRule.WEEKLY)
                    .createdAt(CREATED_AT).dueAt(NOW_PLUS_24H).startedAt(DAY_BEFORE).completedAt(null)
                    .estimatedMinutes(30).actualMinutes(45)
                    .dependencyIds(List.of("d1", "d2")).tags(Set.of("x", "y"))
                    .confidential(true).reviewRequired(true).build();
            assertEquals("T-1", t.id());
            assertEquals("Ship release", t.title());
            assertEquals("Notes", t.description());
            assertEquals("c1", t.creatorId());
            assertEquals("a1", t.assigneeId());
            assertEquals(TaskStatus.OPEN, t.status());
            assertEquals(TaskPriority.HIGH, t.priority());
            assertEquals(RecurrenceRule.WEEKLY, t.recurrence());
            assertEquals(CREATED_AT, t.createdAt());
            assertEquals(NOW_PLUS_24H, t.dueAt());
            assertEquals(DAY_BEFORE, t.startedAt());
            assertNull(t.completedAt());
            assertEquals(30, t.estimatedMinutes());
            assertEquals(45, t.actualMinutes());
            assertEquals(List.of("d1", "d2"), t.dependencyIds());
            assertEquals(Set.of("x", "y"), t.tags());
            assertTrue(t.confidential());
            assertTrue(t.reviewRequired());
        }

        @Test
        void defensivelyCopiesDependencyAndTagCollections() {
            List<String> deps = new ArrayList<>(List.of("d1"));
            Set<String> tags = new HashSet<>(Set.of("t1"));
            Task t = task().dependencyIds(deps).tags(tags).build();
            deps.add("d2");
            tags.add("t2");
            assertEquals(List.of("d1"), t.dependencyIds());
            assertEquals(Set.of("t1"), t.tags());
        }
    }

    @Nested
    class TaskContextTests {

        @Test
        void rejectsNullActorId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ctx().actorId(null).build());
            assertEquals("actorId must not be blank", ex.getMessage());
        }

        @Test
        void rejectsBlankActorId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ctx().actorId("   ").build());
            assertEquals("actorId must not be blank", ex.getMessage());
        }

        @Test
        void trimsActorId() {
            assertEquals("a7", ctx().actorId("  a7  ").build().actorId());
        }

        @Test
        void rejectsNullNow() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> ctx().now(null).build());
            assertEquals("now must not be null", ex.getMessage());
        }

        @Test
        void rejectsNegativePreviousReopens() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ctx().previousReopens(-1).build());
            assertEquals("task counters must not be negative", ex.getMessage());
        }

        @Test
        void rejectsNegativeAssigneeOpenTaskCount() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ctx().assigneeOpenTaskCount(-1).build());
            assertEquals("task counters must not be negative", ex.getMessage());
        }

        @Test
        void rejectsNegativeOverdueTaskCount() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ctx().overdueTaskCount(-1).build());
            assertEquals("task counters must not be negative", ex.getMessage());
        }

        @Test
        void exposesEveryField() {
            TaskContext c = ctx().actorId("mgr-2").now(NOW_PLUS_1H).manager(true).administrator(true)
                    .dependenciesComplete(false).assigneeActive(false).projectActive(false)
                    .withinWorkingHours(false).notificationsEnabled(false)
                    .previousReopens(3).assigneeOpenTaskCount(4).overdueTaskCount(5).build();
            assertEquals("mgr-2", c.actorId());
            assertEquals(NOW_PLUS_1H, c.now());
            assertTrue(c.actorIsManager());
            assertTrue(c.actorIsAdministrator());
            assertFalse(c.dependenciesComplete());
            assertFalse(c.assigneeActive());
            assertFalse(c.projectActive());
            assertFalse(c.withinWorkingHours());
            assertFalse(c.notificationsEnabled());
            assertEquals(3, c.previousReopens());
            assertEquals(4, c.assigneeOpenTaskCount());
            assertEquals(5, c.overdueTaskCount());
        }
    }

    @Nested
    class TaskEvaluationTests {

        @Test
        void rejectsNullEffectivePriority() {
            assertThrows(NullPointerException.class,
                    () -> new TaskEvaluation(null, false, false, false, Set.of(), List.of()));
        }

        @Test
        void rejectsNullAllowedActions() {
            assertThrows(NullPointerException.class,
                    () -> new TaskEvaluation(TaskPriority.LOW, false, false, false, null, List.of()));
        }

        @Test
        void rejectsNullWarnings() {
            assertThrows(NullPointerException.class,
                    () -> new TaskEvaluation(TaskPriority.LOW, false, false, false, Set.of(), null));
        }

        @Test
        void copiesCollectionsAndExposesEveryField() {
            EnumSet<TaskAction> actions = EnumSet.of(TaskAction.PUBLISH, TaskAction.CANCEL);
            List<String> warnings = new ArrayList<>(List.of("w1"));
            TaskEvaluation evaluation = new TaskEvaluation(
                    TaskPriority.HIGH, true, false, true, actions, warnings);
            actions.remove(TaskAction.PUBLISH);
            warnings.add("w2");
            assertEquals(TaskPriority.HIGH, evaluation.effectivePriority());
            assertTrue(evaluation.overdue());
            assertFalse(evaluation.escalationRequired());
            assertTrue(evaluation.reassignmentRecommended());
            assertEquals(Set.of(TaskAction.PUBLISH, TaskAction.CANCEL), evaluation.allowedActions());
            assertEquals(List.of("w1"), evaluation.warnings());
        }
    }

    @Nested
    class EnumCoverageTests {

        @Test
        void taskActionExposesExactlyTwelveValuesInOrder() {
            assertArrayEquals(new TaskAction[] {TaskAction.PUBLISH, TaskAction.START, TaskAction.BLOCK,
                    TaskAction.UNBLOCK, TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE,
                    TaskAction.REJECT_REVIEW, TaskAction.COMPLETE, TaskAction.REOPEN,
                    TaskAction.CANCEL, TaskAction.ARCHIVE, TaskAction.RESTORE}, TaskAction.values());
            for (TaskAction action : TaskAction.values()) {
                assertSame(action, TaskAction.valueOf(action.name()));
            }
        }

        @Test
        void taskStatusExposesExactlyEightValuesInOrder() {
            assertArrayEquals(new TaskStatus[] {TaskStatus.DRAFT, TaskStatus.OPEN, TaskStatus.IN_PROGRESS,
                    TaskStatus.BLOCKED, TaskStatus.IN_REVIEW, TaskStatus.COMPLETED,
                    TaskStatus.CANCELLED, TaskStatus.ARCHIVED}, TaskStatus.values());
            for (TaskStatus status : TaskStatus.values()) {
                assertSame(status, TaskStatus.valueOf(status.name()));
            }
        }

        @Test
        void taskPriorityExposesExactlyFiveValuesInOrder() {
            assertArrayEquals(new TaskPriority[] {TaskPriority.LOW, TaskPriority.NORMAL, TaskPriority.HIGH,
                    TaskPriority.URGENT, TaskPriority.CRITICAL}, TaskPriority.values());
            for (TaskPriority priority : TaskPriority.values()) {
                assertSame(priority, TaskPriority.valueOf(priority.name()));
            }
        }

        @Test
        void recurrenceRuleExposesExactlySixValuesInOrder() {
            assertArrayEquals(new RecurrenceRule[] {RecurrenceRule.NONE, RecurrenceRule.DAILY,
                    RecurrenceRule.WEEKLY, RecurrenceRule.MONTHLY, RecurrenceRule.QUARTERLY,
                    RecurrenceRule.YEARLY}, RecurrenceRule.values());
            for (RecurrenceRule rule : RecurrenceRule.values()) {
                assertSame(rule, RecurrenceRule.valueOf(rule.name()));
            }
        }
    }

    @Nested
    class TaskPolicyPriorityTests {

        private List<String> warnings() {
            return new ArrayList<>();
        }

        @Test
        void baseScoresMapToStartingEffectivePriorities() {
            // Hand-computed: LOW=0, NORMAL=10, HIGH=20, URGENT=30, CRITICAL=40 against thresholds 10/25/40/60.
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.HIGH).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.URGENT).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.URGENT, POLICY.calculatePriority(
                    task().priority(TaskPriority.CRITICAL).build(), ctx().build(), false, warnings()));
        }

        @Test
        void noWarningsWhenNothingSpecialApplies() {
            List<String> warnings = warnings();
            POLICY.calculatePriority(task().build(), ctx().build(), false, warnings);
            assertTrue(warnings.isEmpty());
        }

        @Test
        void dueAtWithinZeroHoursAdds30() {
            // toHours() truncates: NOW -> 0h (+30) gives LOW+30=30 HIGH; NOW+1h (+20) gives 20 NORMAL.
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_PLUS_1H).build(), ctx().build(), false, warnings()));
        }

        @Test
        void negativeHoursAlsoTakeTheUrgentWindow() {
            // dueAt one hour before now -> Duration.toHours() = -1, which is <= 0, so +30.
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_MINUS_1H).build(), ctx().build(), false, warnings()));
        }

        @Test
        void dueAtWithinFourHoursAdds20() {
            // 4h -> +20 => NORMAL+20=30 HIGH; 5h falls into the <=24h window (+12) => 22 NORMAL.
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_4H).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_5H).build(), ctx().build(), false, warnings()));
        }

        @Test
        void dueAtWithin24HoursAdds12() {
            // 24h -> +12 => LOW+12=12 NORMAL; 25h -> +5 => 5 LOW.
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_PLUS_24H).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_PLUS_25H).build(), ctx().build(), false, warnings()));
        }

        @Test
        void dueAtWithin72HoursAdds5() {
            // HIGH base 20: 72h -> +5 => 25 HIGH; 73h -> +0 => 20 NORMAL.
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.HIGH).dueAt(NOW_PLUS_72H).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.HIGH).dueAt(NOW_PLUS_73H).build(), ctx().build(), false, warnings()));
        }

        @Test
        void overdueSevenDaysAdds25AndSixDaysAdds15() {
            // A past dueAt also takes the hours<=0 window (+30). NORMAL base:
            // -7d -> 10+30+25 = 65 CRITICAL; -6d -> 10+30+15 = 55 URGENT.
            assertEquals(TaskPriority.CRITICAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(SEVEN_DAYS_BEFORE).build(),
                    ctx().build(), true, warnings()));
            assertEquals(TaskPriority.URGENT, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(SIX_DAYS_BEFORE).build(),
                    ctx().build(), true, warnings()));
        }

        @Test
        void overdueThreeDaysAdds15AndTwoDaysAdds8() {
            // LOW base: -3d -> 0+30+15 = 45 URGENT; -2d -> 0+30+8 = 38 HIGH.
            assertEquals(TaskPriority.URGENT, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(THREE_DAYS_BEFORE).build(),
                    ctx().build(), true, warnings()));
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(TWO_DAYS_BEFORE).build(),
                    ctx().build(), true, warnings()));
        }

        @Test
        void overdueLessThanThreeDaysAdds8AndWarnsOnce() {
            // -1h -> 0 overdue days -> +8 => LOW 0+30+8 = 38 HIGH, plus exactly one "task is overdue" warning.
            List<String> warnings = warnings();
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_MINUS_1H).build(),
                    ctx().build(), true, warnings));
            assertEquals(List.of("task is overdue"), warnings);
        }

        @Test
        void blockedStatusAdds10() {
            // BLOCKED LOW -> 0+10=10 NORMAL; DRAFT LOW -> 0 LOW.
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    taskIn(TaskStatus.BLOCKED).priority(TaskPriority.LOW).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).build(), ctx().build(), false, warnings()));
        }

        @Test
        void confidentialAdds3() {
            // NORMAL(10) + 24h window(+12) = 22 NORMAL; +confidential(3) = 25 HIGH.
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_24H).confidential(true).build(),
                    ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_24H).build(),
                    ctx().build(), false, warnings()));
        }

        @Test
        void fiveDependenciesAdd5() {
            // NORMAL(10) + 24h window(+12) + 5 deps(+5) = 27 HIGH; 4 deps => 22 NORMAL.
            List<String> five = List.of("d1", "d2", "d3", "d4", "d5");
            List<String> four = List.of("d1", "d2", "d3", "d4");
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_24H).dependencyIds(five).build(),
                    ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_24H).dependencyIds(four).build(),
                    ctx().build(), false, warnings()));
        }

        @Test
        void threeAssigneeOverdueTasksAdd5() {
            // LOW(0) + 72h window(+5) + count 3(+5) = 10 NORMAL; count 2 => 5 LOW.
            CtxBuilder base = ctx().overdueTaskCount(3);
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_PLUS_72H).build(), base.build(), false, warnings()));
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_PLUS_72H).build(),
                    base.overdueTaskCount(2).build(), false, warnings()));
        }

        @Test
        void tenAssigneeOverdueTasksAdd15Total() {
            // NORMAL(10): count 9 -> +5 => 15 NORMAL; count 10 -> +5+10 => 25 HIGH.
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).build(),
                    ctx().overdueTaskCount(9).build(), false, warnings()));
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).build(),
                    ctx().overdueTaskCount(10).build(), false, warnings()));
        }

        @Test
        void inactiveProjectCapsScoreAtTenAndWarns() {
            // HIGH(20) capped to 10 => NORMAL; LOW(0) stays 0 => LOW. One warning either way.
            List<String> warnings = warnings();
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.HIGH).build(), ctx().projectActive(false).build(),
                    false, warnings));
            assertEquals(List.of("inactive project suppresses task escalation"), warnings);
            List<String> lowWarnings = warnings();
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).build(), ctx().projectActive(false).build(),
                    false, lowWarnings));
            assertEquals(List.of("inactive project suppresses task escalation"), lowWarnings);
        }

        @Test
        void terminalStatusesForceLowPriority() {
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    taskIn(TaskStatus.COMPLETED).priority(TaskPriority.CRITICAL).build(),
                    ctx().build(), false, warnings()));
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().status(TaskStatus.CANCELLED).priority(TaskPriority.CRITICAL).build(),
                    ctx().build(), false, warnings()));
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().status(TaskStatus.ARCHIVED).priority(TaskPriority.CRITICAL).build(),
                    ctx().build(), false, warnings()));
        }

        @Test
        void criticalThresholdIsReachedAtExactly60() {
            // CRITICAL(40) + 24h window(+12) + 5 deps(+5) + confidential(+3) = 60 CRITICAL.
            assertEquals(TaskPriority.CRITICAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.CRITICAL).dueAt(NOW_PLUS_24H).confidential(true)
                            .dependencyIds(List.of("d1", "d2", "d3", "d4", "d5")).build(),
                    ctx().build(), false, warnings()));
            // Same score minus the confidential 3 = 57 URGENT.
            assertEquals(TaskPriority.URGENT, POLICY.calculatePriority(
                    task().priority(TaskPriority.CRITICAL).dueAt(NOW_PLUS_24H)
                            .dependencyIds(List.of("d1", "d2", "d3", "d4", "d5")).build(),
                    ctx().build(), false, warnings()));
        }

        @Test
        void urgentThresholdIsReachedAtExactly40() {
            // NORMAL(10) + due now(+30) = 40 URGENT; one hour out = 30 HIGH.
            assertEquals(TaskPriority.URGENT, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_1H).build(), ctx().build(), false, warnings()));
        }

        @Test
        void highThresholdIsReachedAtExactly25() {
            // NORMAL(10) + 24h window(+12) + confidential(+3) = 25 HIGH; without confidential 22 NORMAL.
            assertEquals(TaskPriority.HIGH, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_24H).confidential(true).build(),
                    ctx().build(), false, warnings()));
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).dueAt(NOW_PLUS_24H).build(),
                    ctx().build(), false, warnings()));
        }

        @Test
        void normalThresholdIsReachedAtExactly10() {
            // NORMAL base alone = 10 NORMAL; LOW + 72h window(+5) = 5 LOW.
            assertEquals(TaskPriority.NORMAL, POLICY.calculatePriority(
                    task().priority(TaskPriority.NORMAL).build(), ctx().build(), false, warnings()));
            assertEquals(TaskPriority.LOW, POLICY.calculatePriority(
                    task().priority(TaskPriority.LOW).dueAt(NOW_PLUS_72H).build(), ctx().build(), false, warnings()));
        }
    }

    @Nested
    class TaskPolicyEscalationTests {

        @Test
        void terminalTasksNeverEscalateEvenForInactiveAssignees() {
            for (TaskStatus terminal : List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.ARCHIVED)) {
                TaskEvaluation evaluation = POLICY.evaluate(
                        taskIn(terminal).priority(TaskPriority.CRITICAL).build(), ctx().assigneeActive(false).build());
                assertFalse(evaluation.escalationRequired(), terminal.name());
                assertFalse(evaluation.reassignmentRecommended(), terminal.name());
            }
        }

        @Test
        void criticalEffectivePriorityEscalatesWithWarning() {
            // CRITICAL(40) + due now(+30) = 70 CRITICAL -> escalation warning, no other warnings.
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().priority(TaskPriority.CRITICAL).dueAt(NOW).build(), ctx().build());
            assertTrue(evaluation.escalationRequired());
            assertFalse(evaluation.reassignmentRecommended());
            assertEquals(List.of("critical task requires escalation"), evaluation.warnings());
        }

        @Test
        void overdueUrgentTaskEscalatesAsCriticalWithBothWarnings() {
            // URGENT(30) + past dueAt hours<=0 window(+30) + overdue 0 days(+8) = 68 CRITICAL.
            // The CRITICAL rule fires before the overdue+URGENT rule can, and both warnings accumulate.
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().priority(TaskPriority.URGENT).dueAt(NOW_MINUS_1H).build(), ctx().build());
            assertEquals(TaskPriority.CRITICAL, evaluation.effectivePriority());
            assertTrue(evaluation.escalationRequired());
            assertFalse(evaluation.reassignmentRecommended());
            assertEquals(List.of("task is overdue", "critical task requires escalation"), evaluation.warnings());
        }

        @Test
        void overdueWithFiveAssigneeOverdueTasksEscalates() {
            // LOW(0) + past dueAt window(+30) + overdue 0 days(+8) = 38 URGENT; count 5 trips the
            // overdue-count escalation rule (no warning), count 4 leaves every escalation rule false.
            TaskEvaluation five = POLICY.evaluate(
                    task().priority(TaskPriority.LOW).dueAt(NOW_MINUS_1H).build(),
                    ctx().overdueTaskCount(5).build());
            assertTrue(five.escalationRequired());
            assertEquals(TaskPriority.URGENT, five.effectivePriority());
            assertEquals(List.of("task is overdue"), five.warnings());
            TaskEvaluation four = POLICY.evaluate(
                    task().priority(TaskPriority.LOW).dueAt(NOW_MINUS_1H).build(),
                    ctx().overdueTaskCount(4).build());
            assertFalse(four.escalationRequired());
            assertEquals(List.of("task is overdue"), four.warnings());
        }

        @Test
        void blockedMoreThanThreeDaysEscalatesWithWarning() {
            // startedAt four days ago is strictly before now-3d -> escalation warning, no other warnings.
            TaskEvaluation evaluation = POLICY.evaluate(
                    taskIn(TaskStatus.BLOCKED).priority(TaskPriority.LOW).startedAt(FOUR_DAYS_BEFORE).build(),
                    ctx().build());
            assertTrue(evaluation.escalationRequired());
            assertFalse(evaluation.reassignmentRecommended());
            assertEquals(List.of("task has been blocked for more than three days"), evaluation.warnings());
        }

        @Test
        void blockedExactlyThreeDaysDoesNotEscalate() {
            // isBefore(now-3d) is false at exactly three days, so no escalation and no warnings.
            TaskEvaluation evaluation = POLICY.evaluate(
                    taskIn(TaskStatus.BLOCKED).priority(TaskPriority.LOW).startedAt(THREE_DAYS_BEFORE).build(),
                    ctx().build());
            assertFalse(evaluation.escalationRequired());
            assertEquals(List.of(), evaluation.warnings());
        }

        @Test
        void reviewDueWithinFourHoursEscalatesSilently() {
            // IN_REVIEW + reviewRequired + dueAt two hours out (before now+4h) -> escalation, no warnings.
            TaskEvaluation evaluation = POLICY.evaluate(
                    taskIn(TaskStatus.IN_REVIEW).reviewRequired(true).dueAt(NOW_PLUS_2H).build(), ctx().build());
            assertTrue(evaluation.escalationRequired());
            assertEquals(List.of(), evaluation.warnings());
        }

        @Test
        void reviewDueExactlyAtFourHourBoundaryDoesNotEscalate() {
            // isBefore(now+4h) is false when dueAt equals the boundary.
            TaskEvaluation evaluation = POLICY.evaluate(
                    taskIn(TaskStatus.IN_REVIEW).reviewRequired(true).dueAt(NOW_PLUS_4H).build(), ctx().build());
            assertFalse(evaluation.escalationRequired());
        }

        @Test
        void reviewWithoutDeadlineOrWithoutFlagDoesNotEscalate() {
            TaskEvaluation noFlag = POLICY.evaluate(
                    taskIn(TaskStatus.IN_REVIEW).reviewRequired(false).dueAt(NOW_PLUS_2H).build(), ctx().build());
            assertFalse(noFlag.escalationRequired());
            TaskEvaluation noDeadline = POLICY.evaluate(
                    taskIn(TaskStatus.IN_REVIEW).reviewRequired(true).build(), ctx().build());
            assertFalse(noDeadline.escalationRequired());
        }

        @Test
        void inactiveAssigneeEscalatesAndReassignmentWarns() {
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).build(), ctx().assigneeActive(false).build());
            assertTrue(evaluation.escalationRequired());
            assertTrue(evaluation.reassignmentRecommended());
            assertEquals(List.of("assignee is inactive"), evaluation.warnings());
        }

        @Test
        void inactiveAssigneeIgnoredWhenTaskHasNoAssignee() {
            // Last escalation rule needs a non-null assignee; reassignment still flags the missing assignee.
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).assigneeId(null).build(),
                    ctx().assigneeActive(false).build());
            assertFalse(evaluation.escalationRequired());
            assertTrue(evaluation.reassignmentRecommended());
            assertEquals(List.of("task has no assignee"), evaluation.warnings());
        }
    }

    @Nested
    class TaskPolicyReassignmentTests {

        @Test
        void terminalTasksNeverRecommendReassignment() {
            // Terminal short-circuit runs before the missing-assignee rule, so no warning either.
            TaskEvaluation evaluation = POLICY.evaluate(
                    taskIn(TaskStatus.COMPLETED).assigneeId(null).build(), ctx().build());
            assertFalse(evaluation.reassignmentRecommended());
            assertEquals(List.of(), evaluation.warnings());
        }

        @Test
        void unassignedTaskRecommendsReassignment() {
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).assigneeId(null).build(), ctx().build());
            assertTrue(evaluation.reassignmentRecommended());
            assertFalse(evaluation.escalationRequired());
            assertEquals(List.of("task has no assignee"), evaluation.warnings());
        }

        @Test
        void blankAssigneeCountsAsUnassigned() {
            // Task trims "   " to "", which isBlank() catches.
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).assigneeId("   ").build(), ctx().build());
            assertTrue(evaluation.reassignmentRecommended());
            assertEquals(List.of("task has no assignee"), evaluation.warnings());
        }

        @Test
        void inactiveAssigneeRecommendsReassignmentWithWarning() {
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).build(), ctx().assigneeActive(false).build());
            assertTrue(evaluation.reassignmentRecommended());
            assertEquals(List.of("assignee is inactive"), evaluation.warnings());
        }

        @Test
        void workloadAboveTwentyRecommendsReassignment() {
            // Boundary: 21 > 20 trips the rule with a warning; 20 does not.
            TaskEvaluation over = POLICY.evaluate(task().build(), ctx().assigneeOpenTaskCount(21).build());
            assertTrue(over.reassignmentRecommended());
            assertEquals(List.of("assignee workload is very high"), over.warnings());
            TaskEvaluation at = POLICY.evaluate(task().build(), ctx().assigneeOpenTaskCount(20).build());
            assertFalse(at.reassignmentRecommended());
            assertEquals(List.of(), at.warnings());
        }

        @Test
        void highWorkloadWithLowPriorityRecommendsReassignmentSilently() {
            // 13 > 12 and LOW priority -> true without warning; NORMAL priority or count 12 -> false.
            TaskEvaluation low = POLICY.evaluate(
                    task().priority(TaskPriority.LOW).build(), ctx().assigneeOpenTaskCount(13).build());
            assertTrue(low.reassignmentRecommended());
            assertEquals(List.of(), low.warnings());
            TaskEvaluation normal = POLICY.evaluate(
                    task().priority(TaskPriority.NORMAL).build(), ctx().assigneeOpenTaskCount(13).build());
            assertFalse(normal.reassignmentRecommended());
            TaskEvaluation boundary = POLICY.evaluate(
                    task().priority(TaskPriority.LOW).build(), ctx().assigneeOpenTaskCount(12).build());
            assertFalse(boundary.reassignmentRecommended());
        }

        @Test
        void overdueWithMoreThanTenOpenTasksRecommendsReassignment() {
            // assigneeOpenTaskCount feeds reassignment only (overdueTaskCount feeds priority/escalation):
            // LOW(0) + past dueAt window(+30) + overdue 0 days(+8) = 38 HIGH; count 11 > 10 -> reassignment.
            TaskEvaluation over = POLICY.evaluate(
                    task().priority(TaskPriority.LOW).dueAt(NOW_MINUS_1H).build(),
                    ctx().assigneeOpenTaskCount(11).build());
            assertTrue(over.reassignmentRecommended());
            assertFalse(over.escalationRequired());
            assertEquals(TaskPriority.HIGH, over.effectivePriority());
            assertEquals(List.of("task is overdue"), over.warnings());
            TaskEvaluation at = POLICY.evaluate(
                    task().priority(TaskPriority.LOW).dueAt(NOW_MINUS_1H).build(),
                    ctx().assigneeOpenTaskCount(10).build());
            assertFalse(at.reassignmentRecommended());
            assertFalse(at.escalationRequired());
            assertEquals(List.of("task is overdue"), at.warnings());
        }

        @Test
        void blockedAfterTwoReopensRecommendsReassignment() {
            // Boundary: previousReopens 2 trips the rule, 1 does not.
            TaskEvaluation two = POLICY.evaluate(
                    taskIn(TaskStatus.BLOCKED).priority(TaskPriority.LOW).build(), ctx().previousReopens(2).build());
            assertTrue(two.reassignmentRecommended());
            assertEquals(List.of(), two.warnings());
            TaskEvaluation one = POLICY.evaluate(
                    taskIn(TaskStatus.BLOCKED).priority(TaskPriority.LOW).build(), ctx().previousReopens(1).build());
            assertFalse(one.reassignmentRecommended());
        }

        @Test
        void balancedTaskNeedsNoReassignment() {
            TaskEvaluation evaluation = POLICY.evaluate(task().build(), ctx().build());
            assertFalse(evaluation.reassignmentRecommended());
            assertFalse(evaluation.escalationRequired());
            assertEquals(List.of(), evaluation.warnings());
        }
    }

    @Nested
    class TaskPolicyActionTests {

        @Test
        void draftCreatorAndAdministratorMayPublishOrCancel() {
            assertEquals(Set.of(TaskAction.PUBLISH, TaskAction.CANCEL),
                    POLICY.evaluate(task().build(), ctx().build()).allowedActions());
            assertEquals(Set.of(TaskAction.PUBLISH, TaskAction.CANCEL),
                    POLICY.evaluate(task().build(), ctx().actorId("admin-1").administrator(true).build())
                            .allowedActions());
        }

        @Test
        void draftManagerAndAssigneeGetNoActions() {
            assertEquals(Set.of(), POLICY.evaluate(task().build(), ctx().actorId("mgr-1").manager(true).build())
                    .allowedActions());
            assertEquals(Set.of(), POLICY.evaluate(task().build(), ctx().actorId("assignee-1").build())
                    .allowedActions());
        }

        @Test
        void inactiveProjectKeepsDraftCancelButDropsPublish() {
            assertEquals(Set.of(TaskAction.CANCEL),
                    POLICY.evaluate(task().build(), ctx().projectActive(false).build()).allowedActions());
        }

        @Test
        void outsideWorkingHoursDropsDraftPublishExceptForAdministrator() {
            assertEquals(Set.of(TaskAction.CANCEL),
                    POLICY.evaluate(task().build(), ctx().withinWorkingHours(false).build()).allowedActions());
            assertEquals(Set.of(TaskAction.PUBLISH, TaskAction.CANCEL),
                    POLICY.evaluate(task().build(),
                            ctx().actorId("admin-1").administrator(true).withinWorkingHours(false).build())
                            .allowedActions());
        }

        @Test
        void openAssigneeMayStartWhenDependenciesComplete() {
            assertEquals(Set.of(TaskAction.START),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(), ctx().actorId("assignee-1").build())
                            .allowedActions());
        }

        @Test
        void openAssigneeWithIncompleteDependenciesGetsNothingAndWarning() {
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).build(),
                    ctx().actorId("assignee-1").dependenciesComplete(false).build());
            assertEquals(Set.of(), evaluation.allowedActions());
            assertEquals(List.of("dependencies prevent task start"), evaluation.warnings());
        }

        @Test
        void openManagerAndAdministratorMayStartAndCancel() {
            assertEquals(Set.of(TaskAction.START, TaskAction.CANCEL),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(), ctx().actorId("mgr-1").manager(true).build())
                            .allowedActions());
            assertEquals(Set.of(TaskAction.START, TaskAction.CANCEL),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(),
                            ctx().actorId("admin-1").administrator(true).build()).allowedActions());
        }

        @Test
        void openCreatorMayCancelButNotStart() {
            assertEquals(Set.of(TaskAction.CANCEL),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(), ctx().build()).allowedActions());
        }

        @Test
        void inactiveProjectDropsStartFromOpenTasks() {
            assertEquals(Set.of(TaskAction.CANCEL),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(), ctx().actorId("mgr-1").manager(true)
                            .projectActive(false).build()).allowedActions());
        }

        @Test
        void inactiveAssigneeDropsStartFromOpenTasks() {
            assertEquals(Set.of(),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(),
                            ctx().actorId("assignee-1").assigneeActive(false).build()).allowedActions());
        }

        @Test
        void outsideWorkingHoursKeepsOpenStart() {
            assertEquals(Set.of(TaskAction.START),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(),
                            ctx().actorId("assignee-1").withinWorkingHours(false).build()).allowedActions());
        }

        @Test
        void allThreeFiltersCombinedLeaveOnlyCancelForManager() {
            assertEquals(Set.of(TaskAction.CANCEL),
                    POLICY.evaluate(task().status(TaskStatus.OPEN).build(),
                            ctx().actorId("mgr-1").manager(true).projectActive(false)
                                    .assigneeActive(false).withinWorkingHours(false).build()).allowedActions());
        }

        @Test
        void workInProgressAssigneeBlocksAndCompletesWithoutReview() {
            assertEquals(Set.of(TaskAction.BLOCK, TaskAction.COMPLETE),
                    POLICY.evaluate(taskIn(TaskStatus.IN_PROGRESS).reviewRequired(false).build(),
                            ctx().actorId("assignee-1").build()).allowedActions());
        }

        @Test
        void workInProgressAssigneeBlocksAndSubmitsWhenReviewRequired() {
            assertEquals(Set.of(TaskAction.BLOCK, TaskAction.SUBMIT_FOR_REVIEW),
                    POLICY.evaluate(taskIn(TaskStatus.IN_PROGRESS).reviewRequired(true).build(),
                            ctx().actorId("assignee-1").build()).allowedActions());
        }

        @Test
        void workInProgressManagerAndAdministratorAlsoCancel() {
            assertEquals(Set.of(TaskAction.BLOCK, TaskAction.COMPLETE, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.IN_PROGRESS).build(),
                            ctx().actorId("mgr-1").manager(true).build()).allowedActions());
            assertEquals(Set.of(TaskAction.BLOCK, TaskAction.COMPLETE, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.IN_PROGRESS).build(),
                            ctx().actorId("admin-1").administrator(true).build()).allowedActions());
        }

        @Test
        void unrelatedActorGetsNothingFromWorkInProgress() {
            assertEquals(Set.of(),
                    POLICY.evaluate(taskIn(TaskStatus.IN_PROGRESS).build(), ctx().actorId("nobody").build())
                            .allowedActions());
        }

        @Test
        void outsideWorkingHoursKeepsBlockAndComplete() {
            assertEquals(Set.of(TaskAction.BLOCK, TaskAction.COMPLETE),
                    POLICY.evaluate(taskIn(TaskStatus.IN_PROGRESS).build(),
                            ctx().actorId("assignee-1").withinWorkingHours(false).build()).allowedActions());
        }

        @Test
        void blockedAssigneeMayUnblock() {
            assertEquals(Set.of(TaskAction.UNBLOCK),
                    POLICY.evaluate(taskIn(TaskStatus.BLOCKED).build(), ctx().actorId("assignee-1").build())
                            .allowedActions());
        }

        @Test
        void blockedManagerAndAdministratorMayUnblockAndCancel() {
            assertEquals(Set.of(TaskAction.UNBLOCK, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.BLOCKED).build(), ctx().actorId("mgr-1").manager(true).build())
                            .allowedActions());
            assertEquals(Set.of(TaskAction.UNBLOCK, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.BLOCKED).build(),
                            ctx().actorId("admin-1").administrator(true).build()).allowedActions());
        }

        @Test
        void inactiveAssigneeDropsUnblock() {
            assertEquals(Set.of(),
                    POLICY.evaluate(taskIn(TaskStatus.BLOCKED).build(),
                            ctx().actorId("assignee-1").assigneeActive(false).build()).allowedActions());
            assertEquals(Set.of(TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.BLOCKED).build(),
                            ctx().actorId("mgr-1").manager(true).assigneeActive(false).build()).allowedActions());
        }

        @Test
        void reviewManagerAndAdministratorApproveRejectCancel() {
            assertEquals(Set.of(TaskAction.APPROVE, TaskAction.REJECT_REVIEW, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.IN_REVIEW).build(), ctx().actorId("mgr-1").manager(true).build())
                            .allowedActions());
            assertEquals(Set.of(TaskAction.APPROVE, TaskAction.REJECT_REVIEW, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.IN_REVIEW).build(),
                            ctx().actorId("admin-1").administrator(true).build()).allowedActions());
        }

        @Test
        void reviewAssigneeGetsNoActions() {
            assertEquals(Set.of(),
                    POLICY.evaluate(taskIn(TaskStatus.IN_REVIEW).build(), ctx().actorId("assignee-1").build())
                            .allowedActions());
        }

        @Test
        void outsideWorkingHoursDropsApproveExceptForAdministrator() {
            assertEquals(Set.of(TaskAction.REJECT_REVIEW, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.IN_REVIEW).build(),
                            ctx().actorId("mgr-1").manager(true).withinWorkingHours(false).build())
                            .allowedActions());
            assertEquals(Set.of(TaskAction.APPROVE, TaskAction.REJECT_REVIEW, TaskAction.CANCEL),
                    POLICY.evaluate(taskIn(TaskStatus.IN_REVIEW).build(),
                            ctx().actorId("admin-1").administrator(true).withinWorkingHours(false).build())
                            .allowedActions());
        }

        @Test
        void completedManagerAndAdministratorMayReopenOrArchive() {
            assertEquals(Set.of(TaskAction.REOPEN, TaskAction.ARCHIVE),
                    POLICY.evaluate(taskIn(TaskStatus.COMPLETED).build(), ctx().actorId("mgr-1").manager(true).build())
                            .allowedActions());
            assertEquals(Set.of(TaskAction.REOPEN, TaskAction.ARCHIVE),
                    POLICY.evaluate(taskIn(TaskStatus.COMPLETED).build(),
                            ctx().actorId("admin-1").administrator(true).build()).allowedActions());
        }

        @Test
        void completedCreatorAloneGetsNoActions() {
            assertEquals(Set.of(),
                    POLICY.evaluate(taskIn(TaskStatus.COMPLETED).build(), ctx().build()).allowedActions());
        }

        @Test
        void inactiveProjectDropsReopenFromCompletedTasks() {
            assertEquals(Set.of(TaskAction.ARCHIVE),
                    POLICY.evaluate(taskIn(TaskStatus.COMPLETED).build(),
                            ctx().actorId("mgr-1").manager(true).projectActive(false).build()).allowedActions());
        }

        @Test
        void cancelledCreatorManagerAdministratorMayReopenOrArchive() {
            Set<TaskAction> expected = Set.of(TaskAction.REOPEN, TaskAction.ARCHIVE);
            assertEquals(expected, POLICY.evaluate(task().status(TaskStatus.CANCELLED).build(), ctx().build())
                    .allowedActions());
            assertEquals(expected, POLICY.evaluate(task().status(TaskStatus.CANCELLED).build(),
                    ctx().actorId("mgr-1").manager(true).build()).allowedActions());
            assertEquals(expected, POLICY.evaluate(task().status(TaskStatus.CANCELLED).build(),
                    ctx().actorId("admin-1").administrator(true).build()).allowedActions());
        }

        @Test
        void cancelledAssigneeGetsNoActions() {
            assertEquals(Set.of(),
                    POLICY.evaluate(task().status(TaskStatus.CANCELLED).build(), ctx().actorId("assignee-1").build())
                            .allowedActions());
        }

        @Test
        void inactiveProjectDropsReopenFromCancelledTasks() {
            assertEquals(Set.of(TaskAction.ARCHIVE),
                    POLICY.evaluate(task().status(TaskStatus.CANCELLED).build(),
                            ctx().projectActive(false).build()).allowedActions());
        }

        @Test
        void archivedAdministratorAndManagerMayRestore() {
            assertEquals(Set.of(TaskAction.RESTORE),
                    POLICY.evaluate(task().status(TaskStatus.ARCHIVED).build(),
                            ctx().actorId("admin-1").administrator(true).build()).allowedActions());
            assertEquals(Set.of(TaskAction.RESTORE),
                    POLICY.evaluate(task().status(TaskStatus.ARCHIVED).build(),
                            ctx().actorId("mgr-1").manager(true).build()).allowedActions());
        }

        @Test
        void archivedCreatorAndAssigneeGetNoActions() {
            assertEquals(Set.of(),
                    POLICY.evaluate(task().status(TaskStatus.ARCHIVED).build(), ctx().build()).allowedActions());
            assertEquals(Set.of(),
                    POLICY.evaluate(task().status(TaskStatus.ARCHIVED).build(), ctx().actorId("assignee-1").build())
                            .allowedActions());
        }

        @Test
        void outsideWorkingHoursKeepsRestore() {
            assertEquals(Set.of(TaskAction.RESTORE),
                    POLICY.evaluate(task().status(TaskStatus.ARCHIVED).build(),
                            ctx().actorId("admin-1").administrator(true).withinWorkingHours(false).build())
                            .allowedActions());
        }
    }

    @Nested
    class TaskPolicyOverdueTests {

        @Test
        void nullDueAtIsNeverOverdue() {
            assertFalse(POLICY.evaluate(task().build(), ctx().build()).overdue());
        }

        @Test
        void dueAtEqualToNowIsNotOverdue() {
            assertFalse(POLICY.evaluate(task().dueAt(NOW).build(), ctx().build()).overdue());
        }

        @Test
        void pastDueAtOnOpenWorkIsOverdue() {
            assertTrue(POLICY.evaluate(task().dueAt(NOW_MINUS_1H).build(), ctx().build()).overdue());
        }

        @Test
        void futureDueAtIsNotOverdue() {
            assertFalse(POLICY.evaluate(task().dueAt(NOW_PLUS_1H).build(), ctx().build()).overdue());
        }

        @Test
        void terminalStatusesSuppressOverdue() {
            for (TaskStatus terminal : List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.ARCHIVED)) {
                assertFalse(POLICY.evaluate(
                        taskIn(terminal).dueAt(SEVEN_DAYS_BEFORE).build(), ctx().build()).overdue(),
                        terminal.name());
            }
        }
    }

    @Nested
    class TaskPolicyEvaluationTests {

        @Test
        void fullEvaluationForBlockedStartAttempt() {
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).build(),
                    ctx().actorId("assignee-1").dependenciesComplete(false).build());
            assertEquals(TaskPriority.NORMAL, evaluation.effectivePriority());
            assertFalse(evaluation.overdue());
            assertFalse(evaluation.escalationRequired());
            assertFalse(evaluation.reassignmentRecommended());
            assertEquals(Set.of(), evaluation.allowedActions());
            assertEquals(List.of("dependencies prevent task start"), evaluation.warnings());
        }

        @Test
        void fullEvaluationForOverdueUrgentDraft() {
            // URGENT(30) + past dueAt window(+30) + overdue 0 days(+8) = 68 CRITICAL; creator may publish/cancel.
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().priority(TaskPriority.URGENT).dueAt(NOW_MINUS_1H).build(), ctx().build());
            assertEquals(TaskPriority.CRITICAL, evaluation.effectivePriority());
            assertTrue(evaluation.overdue());
            assertTrue(evaluation.escalationRequired());
            assertFalse(evaluation.reassignmentRecommended());
            assertEquals(Set.of(TaskAction.PUBLISH, TaskAction.CANCEL), evaluation.allowedActions());
            assertEquals(List.of("task is overdue", "critical task requires escalation"), evaluation.warnings());
        }

        @Test
        void fullEvaluationForUnassignedOpenTask() {
            TaskEvaluation evaluation = POLICY.evaluate(
                    task().status(TaskStatus.OPEN).assigneeId(null).build(), ctx().build());
            assertEquals(TaskPriority.NORMAL, evaluation.effectivePriority());
            assertFalse(evaluation.overdue());
            assertFalse(evaluation.escalationRequired());
            assertTrue(evaluation.reassignmentRecommended());
            assertEquals(Set.of(TaskAction.CANCEL), evaluation.allowedActions());
            assertEquals(List.of("task has no assignee"), evaluation.warnings());
        }

        @Test
        void evaluateRejectsNullTask() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> POLICY.evaluate(null, ctx().build()));
            assertEquals("task must not be null", ex.getMessage());
        }

        @Test
        void evaluateRejectsNullContext() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> POLICY.evaluate(task().build(), null));
            assertEquals("context must not be null", ex.getMessage());
        }
    }

    /** Every field the state machine is supposed to carry over unchanged. */
    private static void assertCarriedOver(Task original, Task result) {
        assertEquals(original.id(), result.id());
        assertEquals(original.title(), result.title());
        assertEquals(original.description(), result.description());
        assertEquals(original.creatorId(), result.creatorId());
        assertEquals(original.assigneeId(), result.assigneeId());
        assertEquals(original.priority(), result.priority());
        assertEquals(original.recurrence(), result.recurrence());
        assertEquals(original.createdAt(), result.createdAt());
        assertEquals(original.dueAt(), result.dueAt());
        assertEquals(original.estimatedMinutes(), result.estimatedMinutes());
        assertEquals(original.actualMinutes(), result.actualMinutes());
        assertEquals(original.dependencyIds(), result.dependencyIds());
        assertEquals(original.tags(), result.tags());
        assertEquals(original.confidential(), result.confidential());
        assertEquals(original.reviewRequired(), result.reviewRequired());
    }

    @Nested
    class TaskStateMachineTests {

        @Test
        void applyRejectsNullTaskActionAndContext() {
            Task task = task().build();
            assertEquals("task must not be null",
                    assertThrows(NullPointerException.class, () -> MACHINE.apply(null, TaskAction.PUBLISH, ctx().build()))
                            .getMessage());
            assertEquals("action must not be null",
                    assertThrows(NullPointerException.class, () -> MACHINE.apply(task, null, ctx().build()))
                            .getMessage());
            assertEquals("context must not be null",
                    assertThrows(NullPointerException.class, () -> MACHINE.apply(task, TaskAction.PUBLISH, null))
                            .getMessage());
        }

        @Test
        void publishMovesDraftToOpen() {
            Task original = task().build();
            Task result = MACHINE.apply(original, TaskAction.PUBLISH, ctx().build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.OPEN, result.status());
            assertNull(result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void creatorCancelsDraft() {
            Task original = task().build();
            Task result = MACHINE.apply(original, TaskAction.CANCEL, ctx().build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.CANCELLED, result.status());
            assertNull(result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void startBeginsWorkAndStampsStartedAtFromContext() {
            Task original = task().status(TaskStatus.OPEN).build();
            Task result = MACHINE.apply(original, TaskAction.START, ctx().actorId("assignee-1").build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.IN_PROGRESS, result.status());
            assertEquals(NOW, result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void startKeepsAnExistingStartedAt() {
            Task original = task().status(TaskStatus.OPEN).startedAt(DAY_BEFORE).build();
            Task result = MACHINE.apply(original, TaskAction.START, ctx().actorId("assignee-1").build());
            assertEquals(TaskStatus.IN_PROGRESS, result.status());
            assertEquals(DAY_BEFORE, result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void managerCancelsOpenTask() {
            Task original = task().status(TaskStatus.OPEN).build();
            Task result = MACHINE.apply(original, TaskAction.CANCEL, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.CANCELLED, result.status());
            assertNull(result.completedAt());
        }

        @Test
        void assigneeBlocksWorkInProgress() {
            Task original = taskIn(TaskStatus.IN_PROGRESS).build();
            Task result = MACHINE.apply(original, TaskAction.BLOCK, ctx().actorId("assignee-1").build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.BLOCKED, result.status());
            assertEquals(DAY_BEFORE, result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void assigneeCompletesWorkWithoutReview() {
            Task original = taskIn(TaskStatus.IN_PROGRESS).reviewRequired(false).build();
            Task result = MACHINE.apply(original, TaskAction.COMPLETE, ctx().actorId("assignee-1").build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.COMPLETED, result.status());
            assertEquals(NOW, result.completedAt());
            assertEquals(DAY_BEFORE, result.startedAt());
        }

        @Test
        void assigneeSubmitsReviewedWork() {
            Task original = taskIn(TaskStatus.IN_PROGRESS).reviewRequired(true).build();
            Task result = MACHINE.apply(original, TaskAction.SUBMIT_FOR_REVIEW, ctx().actorId("assignee-1").build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.IN_REVIEW, result.status());
            assertNull(result.completedAt());
            assertEquals(DAY_BEFORE, result.startedAt());
        }

        @Test
        void managerCancelsWorkInProgress() {
            Task original = taskIn(TaskStatus.IN_PROGRESS).build();
            Task result = MACHINE.apply(original, TaskAction.CANCEL, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.CANCELLED, result.status());
            assertNull(result.completedAt());
        }

        @Test
        void assigneeUnblocksBlockedTask() {
            Task original = taskIn(TaskStatus.BLOCKED).build();
            Task result = MACHINE.apply(original, TaskAction.UNBLOCK, ctx().actorId("assignee-1").build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.IN_PROGRESS, result.status());
            assertEquals(DAY_BEFORE, result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void managerCancelsBlockedTask() {
            Task original = taskIn(TaskStatus.BLOCKED).build();
            Task result = MACHINE.apply(original, TaskAction.CANCEL, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.CANCELLED, result.status());
        }

        @Test
        void managerApprovesReview() {
            Task original = taskIn(TaskStatus.IN_REVIEW).build();
            Task result = MACHINE.apply(original, TaskAction.APPROVE, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.COMPLETED, result.status());
            assertEquals(NOW, result.completedAt());
            assertEquals(DAY_BEFORE, result.startedAt());
        }

        @Test
        void managerRejectsReviewBackToWork() {
            Task original = taskIn(TaskStatus.IN_REVIEW).build();
            Task result = MACHINE.apply(original, TaskAction.REJECT_REVIEW, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.IN_PROGRESS, result.status());
            assertNull(result.completedAt());
            assertEquals(DAY_BEFORE, result.startedAt());
        }

        @Test
        void managerCancelsTaskInReview() {
            Task original = taskIn(TaskStatus.IN_REVIEW).build();
            Task result = MACHINE.apply(original, TaskAction.CANCEL, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.CANCELLED, result.status());
            assertNull(result.completedAt());
        }

        @Test
        void managerReopensCompletedTaskClearingTimestamps() {
            Task original = taskIn(TaskStatus.COMPLETED).build();
            Task result = MACHINE.apply(original, TaskAction.REOPEN, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.OPEN, result.status());
            assertNull(result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void managerArchivesCompletedTaskDroppingCompletedAt() {
            Task original = taskIn(TaskStatus.COMPLETED).build();
            Task result = MACHINE.apply(original, TaskAction.ARCHIVE, ctx().actorId("mgr-1").manager(true).build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.ARCHIVED, result.status());
            assertNull(result.completedAt());
            assertEquals(DAY_BEFORE, result.startedAt());
        }

        @Test
        void creatorReopensCancelledTaskClearingStartedAt() {
            Task original = task().status(TaskStatus.CANCELLED).startedAt(DAY_BEFORE).build();
            Task result = MACHINE.apply(original, TaskAction.REOPEN, ctx().build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.OPEN, result.status());
            assertNull(result.startedAt());
            assertNull(result.completedAt());
        }

        @Test
        void creatorArchivesCancelledTask() {
            Task original = task().status(TaskStatus.CANCELLED).build();
            Task result = MACHINE.apply(original, TaskAction.ARCHIVE, ctx().build());
            assertCarriedOver(original, result);
            assertEquals(TaskStatus.ARCHIVED, result.status());
            assertNull(result.completedAt());
        }

        @Test
        void restoreFromArchivedAlwaysFailsBecauseInProgressRequiresStartedAt() {
            // RESTORE is policy-legal from ARCHIVED, but the machine nulls startedAt while targeting
            // IN_PROGRESS, so Task's constructor always rejects the result. Verified production behavior.
            Task withHistory = task().status(TaskStatus.ARCHIVED).startedAt(DAY_BEFORE).build();
            IllegalArgumentException withHistoryEx = assertThrows(IllegalArgumentException.class,
                    () -> MACHINE.apply(withHistory, TaskAction.RESTORE, ctx().actorId("admin-1").administrator(true).build()));
            assertEquals("startedAt is required after work begins", withHistoryEx.getMessage());
            Task fresh = task().status(TaskStatus.ARCHIVED).build();
            IllegalArgumentException freshEx = assertThrows(IllegalArgumentException.class,
                    () -> MACHINE.apply(fresh, TaskAction.RESTORE, ctx().actorId("admin-1").administrator(true).build()));
            assertEquals("startedAt is required after work begins", freshEx.getMessage());
        }

        @Test
        void draftRejectsEveryActionExceptPublishAndCancel() {
            Task original = task().build();
            TaskContext context = ctx().build();
            assertEquals(TaskStatus.OPEN, MACHINE.apply(original, TaskAction.PUBLISH, context).status());
            assertEquals(TaskStatus.CANCELLED, MACHINE.apply(original, TaskAction.CANCEL, context).status());
            for (TaskAction action : List.of(TaskAction.START, TaskAction.BLOCK, TaskAction.UNBLOCK,
                    TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE, TaskAction.REJECT_REVIEW,
                    TaskAction.COMPLETE, TaskAction.REOPEN, TaskAction.ARCHIVE, TaskAction.RESTORE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.DRAFT, ex.getMessage());
            }
        }

        @Test
        void openRejectsEveryActionExceptStartForAssignee() {
            Task original = task().status(TaskStatus.OPEN).build();
            TaskContext context = ctx().actorId("assignee-1").build();
            assertEquals(TaskStatus.IN_PROGRESS, MACHINE.apply(original, TaskAction.START, context).status());
            for (TaskAction action : List.of(TaskAction.PUBLISH, TaskAction.BLOCK, TaskAction.UNBLOCK,
                    TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE, TaskAction.REJECT_REVIEW,
                    TaskAction.COMPLETE, TaskAction.CANCEL, TaskAction.REOPEN, TaskAction.ARCHIVE,
                    TaskAction.RESTORE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.OPEN, ex.getMessage());
            }
        }

        @Test
        void workInProgressRejectsEveryActionExceptBlockAndCompleteForAssignee() {
            Task original = taskIn(TaskStatus.IN_PROGRESS).reviewRequired(false).build();
            TaskContext context = ctx().actorId("assignee-1").build();
            assertEquals(TaskStatus.BLOCKED, MACHINE.apply(original, TaskAction.BLOCK, context).status());
            assertEquals(TaskStatus.COMPLETED, MACHINE.apply(original, TaskAction.COMPLETE, context).status());
            for (TaskAction action : List.of(TaskAction.PUBLISH, TaskAction.START, TaskAction.UNBLOCK,
                    TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE, TaskAction.REJECT_REVIEW,
                    TaskAction.CANCEL, TaskAction.REOPEN, TaskAction.ARCHIVE, TaskAction.RESTORE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.IN_PROGRESS, ex.getMessage());
            }
        }

        @Test
        void blockedRejectsEveryActionExceptUnblockForAssignee() {
            Task original = taskIn(TaskStatus.BLOCKED).build();
            TaskContext context = ctx().actorId("assignee-1").build();
            assertEquals(TaskStatus.IN_PROGRESS, MACHINE.apply(original, TaskAction.UNBLOCK, context).status());
            for (TaskAction action : List.of(TaskAction.PUBLISH, TaskAction.START, TaskAction.BLOCK,
                    TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE, TaskAction.REJECT_REVIEW,
                    TaskAction.COMPLETE, TaskAction.CANCEL, TaskAction.REOPEN, TaskAction.ARCHIVE,
                    TaskAction.RESTORE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.BLOCKED, ex.getMessage());
            }
        }

        @Test
        void inReviewRejectsEveryActionExceptApproveRejectAndCancelForManager() {
            Task original = taskIn(TaskStatus.IN_REVIEW).build();
            TaskContext context = ctx().actorId("mgr-1").manager(true).build();
            assertEquals(TaskStatus.COMPLETED, MACHINE.apply(original, TaskAction.APPROVE, context).status());
            assertEquals(TaskStatus.IN_PROGRESS, MACHINE.apply(original, TaskAction.REJECT_REVIEW, context).status());
            assertEquals(TaskStatus.CANCELLED, MACHINE.apply(original, TaskAction.CANCEL, context).status());
            for (TaskAction action : List.of(TaskAction.PUBLISH, TaskAction.START, TaskAction.BLOCK,
                    TaskAction.UNBLOCK, TaskAction.SUBMIT_FOR_REVIEW, TaskAction.COMPLETE,
                    TaskAction.REOPEN, TaskAction.ARCHIVE, TaskAction.RESTORE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.IN_REVIEW, ex.getMessage());
            }
        }

        @Test
        void completedRejectsEveryActionExceptReopenAndArchiveForManager() {
            Task original = taskIn(TaskStatus.COMPLETED).build();
            TaskContext context = ctx().actorId("mgr-1").manager(true).build();
            assertEquals(TaskStatus.OPEN, MACHINE.apply(original, TaskAction.REOPEN, context).status());
            assertEquals(TaskStatus.ARCHIVED, MACHINE.apply(original, TaskAction.ARCHIVE, context).status());
            for (TaskAction action : List.of(TaskAction.PUBLISH, TaskAction.START, TaskAction.BLOCK,
                    TaskAction.UNBLOCK, TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE,
                    TaskAction.REJECT_REVIEW, TaskAction.COMPLETE, TaskAction.CANCEL, TaskAction.RESTORE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.COMPLETED, ex.getMessage());
            }
        }

        @Test
        void cancelledRejectsEveryActionExceptReopenAndArchiveForCreator() {
            Task original = task().status(TaskStatus.CANCELLED).build();
            TaskContext context = ctx().build();
            assertEquals(TaskStatus.OPEN, MACHINE.apply(original, TaskAction.REOPEN, context).status());
            assertEquals(TaskStatus.ARCHIVED, MACHINE.apply(original, TaskAction.ARCHIVE, context).status());
            for (TaskAction action : List.of(TaskAction.PUBLISH, TaskAction.START, TaskAction.BLOCK,
                    TaskAction.UNBLOCK, TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE,
                    TaskAction.REJECT_REVIEW, TaskAction.COMPLETE, TaskAction.CANCEL, TaskAction.RESTORE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.CANCELLED, ex.getMessage());
            }
        }

        @Test
        void archivedRejectsEveryActionIncludingRestoreForAdministrator() {
            // RESTORE is in the policy's allowed set but always fails in the Task constructor (tested above);
            // every other action must be rejected by the policy gate with IllegalStateException.
            Task original = task().status(TaskStatus.ARCHIVED).build();
            TaskContext context = ctx().actorId("admin-1").administrator(true).build();
            for (TaskAction action : List.of(TaskAction.PUBLISH, TaskAction.START, TaskAction.BLOCK,
                    TaskAction.UNBLOCK, TaskAction.SUBMIT_FOR_REVIEW, TaskAction.APPROVE,
                    TaskAction.REJECT_REVIEW, TaskAction.COMPLETE, TaskAction.REOPEN, TaskAction.CANCEL,
                    TaskAction.ARCHIVE)) {
                IllegalStateException ex = assertThrows(IllegalStateException.class,
                        () -> MACHINE.apply(original, action, context), action.name());
                assertEquals("action " + action + " is not allowed from " + TaskStatus.ARCHIVED, ex.getMessage());
            }
        }
    }

    @Nested
    class DateTextParserTests {

        @Test
        void rejectsNullText() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DateTextParser.parse(null));
            assertEquals("date text must not be blank", ex.getMessage());
        }

        @Test
        void rejectsEmptyText() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DateTextParser.parse(""));
            assertEquals("date text must not be blank", ex.getMessage());
        }

        @Test
        void rejectsWhitespaceOnlyText() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("   "));
            assertEquals("date text must not be blank", ex.getMessage());
        }

        @Test
        void parsesDashSeparatedFormat() {
            assertEquals(LocalDate.of(2026, 6, 15), DateTextParser.parse("2026-06-15"));
        }

        @Test
        void parsesSlashSeparatedFormat() {
            assertEquals(LocalDate.of(2026, 6, 15), DateTextParser.parse("2026/06/15"));
        }

        @Test
        void parsesCompactFormat() {
            assertEquals(LocalDate.of(2026, 6, 15), DateTextParser.parse("20260615"));
        }

        @Test
        void trimsSurroundingWhitespaceBeforeParsing() {
            assertEquals(LocalDate.of(2026, 6, 15), DateTextParser.parse("  2026-06-15\t"));
        }

        @Test
        void parsesLeapDayInLeapYear() {
            assertEquals(LocalDate.of(2024, 2, 29), DateTextParser.parse("2024-02-29"));
            assertEquals(LocalDate.of(2024, 2, 29), DateTextParser.parse("2024/02/29"));
        }

        @Test
        void rejectsImpossibleDayWithStrictResolving() {
            // LENIENT would roll 2026-02-30 into March; STRICT must refuse.
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("2026-02-30"));
            assertEquals("unsupported date: 2026-02-30", ex.getMessage());
        }

        @Test
        void rejectsLeapDayInNonLeapYear() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("2025-02-29"));
            assertEquals("unsupported date: 2025-02-29", ex.getMessage());
        }

        @Test
        void rejectsMonthThirteen() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("2026-13-01"));
            assertEquals("unsupported date: 2026-13-01", ex.getMessage());
        }

        @Test
        void rejectsDayFirstOrdering() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("15-06-2026"));
            assertEquals("unsupported date: 15-06-2026", ex.getMessage());
        }

        @Test
        void rejectsSingleDigitMonthForTwoDigitPattern() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("2026-6-15"));
            assertEquals("unsupported date: 2026-6-15", ex.getMessage());
        }

        @Test
        void rejectsDateTimeText() {
            // Time components are not part of any accepted exam date format.
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("2026-06-15T10:00"));
            assertEquals("unsupported date: 2026-06-15T10:00", ex.getMessage());
        }

        @Test
        void reportsTheTrimmedValueInUnsupportedDateErrors() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DateTextParser.parse("   2026-99-99  "));
            assertEquals("unsupported date: 2026-99-99", ex.getMessage());
        }
    }}

}
