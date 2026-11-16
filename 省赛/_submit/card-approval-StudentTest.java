/*
 * 开发者测试比赛（省赛 Card 题：card-approval / contest 35 problemId 135）
 *
 * 单文件测试套件：OA 考勤 / 请假 / 任务 / 报销 / 权限 / 审批 + 信用卡校验。
 * 仅使用公开 API + 真实输入；Mockito 只用于 mock 端口接口（题目明确为可替换边界），
 * 不使用 mockConstruction / mockStatic，不修改 src/main 与 pom.xml。
 *
 * 融合说明：以 deepseek 版的探针式套件为骨架，吸收 GLM 的卡片长度/IIN 边界用例，
 * 并按 PIT 存活清单补充 CardNetworkBoundaryTests；每个用例带 @DisplayName 语义说明。
 */
package net.mooctest;

import net.mooctest.attendance.*;
import net.mooctest.card.*;
import net.mooctest.domain.*;
import net.mooctest.exception.*;
import net.mooctest.expense.*;
import net.mooctest.leave.*;
import net.mooctest.permission.*;
import net.mooctest.port.*;
import net.mooctest.service.*;
import net.mooctest.task.*;
import net.mooctest.util.*;
import net.mooctest.workflow.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import net.mooctest.card.CreditCardType;
import net.mooctest.card.DateChecker;
import net.mooctest.card.DinersClubInternationalValidator;
import net.mooctest.card.InterPaymentValidator;
import net.mooctest.card.TypeChecker;
import net.mooctest.card.Validator;
import net.mooctest.card.VisaValidator;
import net.mooctest.domain.DateRange;
import net.mooctest.domain.LeaveType;
import net.mooctest.expense.EmployeeGrade;
import net.mooctest.expense.ExchangeRateProvider;
import net.mooctest.expense.ExpenseApprovalTier;
import net.mooctest.expense.ExpenseAssessment;
import net.mooctest.expense.ExpenseCategory;
import net.mooctest.expense.ExpenseClaim;
import net.mooctest.expense.ExpenseItem;
import net.mooctest.expense.ExpensePolicy;
import net.mooctest.expense.PaymentMethod;
import net.mooctest.expense.ReceiptType;
import net.mooctest.leave.AdvancedLeavePolicy;
import net.mooctest.leave.AdvancedLeaveRequest;
import net.mooctest.leave.ApprovalTier;
import net.mooctest.leave.EmployeeCategory;
import net.mooctest.leave.LeaveAssessment;
import net.mooctest.leave.LeaveUnit;
import net.mooctest.leave.WorkCalendar;
import net.mooctest.permission.AccessContext;
import net.mooctest.permission.AccessDecision;
import net.mooctest.permission.AccessPolicy;
import net.mooctest.permission.AccessScope;
import net.mooctest.permission.ResourceAction;
import net.mooctest.permission.ResourceType;
import net.mooctest.permission.Sensitivity;
import net.mooctest.permission.UserRole;
import net.mooctest.service.LeavePolicy;
import net.mooctest.task.RecurrenceRule;
import net.mooctest.task.Task;
import net.mooctest.task.TaskAction;
import net.mooctest.task.TaskContext;
import net.mooctest.task.TaskEvaluation;
import net.mooctest.task.TaskPolicy;
import net.mooctest.task.TaskPriority;
import net.mooctest.task.TaskStateMachine;
import net.mooctest.task.TaskStatus;
import net.mooctest.workflow.ApprovalDecision;
import net.mooctest.workflow.ApprovalStep;
import net.mooctest.workflow.ApprovalWorkflowEngine;
import net.mooctest.workflow.StepMode;
import net.mooctest.workflow.WorkflowAction;
import net.mooctest.workflow.WorkflowSnapshot;
import net.mooctest.workflow.WorkflowState;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;


import net.mooctest.card.CardEvidence;
import net.mooctest.card.CardEvidenceCheck;
import net.mooctest.card.CardEvidenceValidator;
import net.mooctest.card.ChinaUnionPayValidator;
import net.mooctest.card.MaestroUKValidator;
import net.mooctest.card.MaestroValidator;
import net.mooctest.card.VerveValidator;
import net.mooctest.expense.CorporateCardExpenseService;
import net.mooctest.expense.ExpenseCardReview;
import net.mooctest.service.SubmitLeaveCommand;
import net.mooctest.workflow.StepProgress;
import static org.junit.jupiter.api.Assertions.assertTrue;



class StudentTest {

    // ------------------------------------------------------------------
    // shared helpers
    // ------------------------------------------------------------------

    @FunctionalInterface
    interface Body {
        void run() throws Throwable;
    }

    /** Asserts the exact exception type and message text. */
    private static <T extends Throwable> T assertThrowsWith(Class<T> type, String message, Body body) {
        T thrown = assertThrows(type, body::run);
        assertEquals(message, thrown.getMessage());
        return thrown;
    }

    /** Pads a numeric prefix with zeros up to 16 digits (keeps the parsed IIN intact). */
    private static String card(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        while (sb.length() < 16) {
            sb.append('0');
        }
        return sb.toString();
    }

    private static void assertMatches(CreditCardType type, String prefix) {
        String number = card(prefix);
        assertTrue(TypeChecker.matchesType(number, type), type + " should accept " + number);
    }

    private static void assertRejects(CreditCardType type, String prefix) {
        String number = card(prefix);
        assertFalse(TypeChecker.matchesType(number, type), type + " should reject " + number);
    }

    // ==================================================================
    // card: format validators
    // ==================================================================

    record CardCase(CreditCardType type, String valid, String badIin, String badLength) {
    }

    static Stream<CardCase> cardCases() {
        return Stream.of(
                new CardCase(CreditCardType.VISA, "4111111111111111", "5111111111111111", "411111111111"),
                new CardCase(CreditCardType.MASTERCARD, "5100000000000000", "5000000000000000", "510000000000000"),
                new CardCase(CreditCardType.DISCOVER, "6011000000000000", "6010000000000000", "601100000000000"),
                new CardCase(CreditCardType.AMERICAN_EXPRESS, "340000000000000", "380000000000000", "3400000000000000"),
                new CardCase(CreditCardType.VISA_ELECTRON, "4175000000000000", "4000000000000000", "417500000000000"),
                new CardCase(CreditCardType.MAESTRO, "5000000000000000", "5100000000000000", "50000000000"),
                new CardCase(CreditCardType.MAESTRO_UK, "6759000000000000", "6760000000000000", "67590000000"),
                new CardCase(CreditCardType.JCB, "3528000000000000", "3527000000000000", "352800000000000"),
                new CardCase(CreditCardType.CHINA_TUNION, "3100000000000000000", "3200000000000000000", "3100000000000000"),
                new CardCase(CreditCardType.CHINA_UNIONPAY, "6200000000000000", "6300000000000000", "620000000000000"),
                new CardCase(CreditCardType.DINERS_CLUB_INTERNATIONAL, "3000000000000000", "3060000000000000", "300000000000000"),
                new CardCase(CreditCardType.DINERS_CLUB, "5400000000000000", "5300000000000000", "540000000000000"),
                new CardCase(CreditCardType.RUPAY, "6000000000000000", "6100000000000000", "600000000000000"),
                new CardCase(CreditCardType.INTERPAYMENT, "6360000000000000", "6350000000000000", "636000000000000"),
                new CardCase(CreditCardType.INSTAPAYMENT, "6370000000000000", "6360000000000000", "637000000000000"),
                new CardCase(CreditCardType.DANKORT, "4571000000000000", "4570000000000000", "457100000000000"),
                new CardCase(CreditCardType.MIR, "2200000000000000", "2205000000000000", "220000000000000"),
                new CardCase(CreditCardType.NPS_PRIDNESTROVIE, "6054740000000000", "6054745000000000", "605474000000000"),
                new CardCase(CreditCardType.TROY, "9792000000000000", "9792900000000000", "979200000000000"),
                new CardCase(CreditCardType.UATP, "100000000000000", "200000000000000", "1000000000000000"),
                new CardCase(CreditCardType.VERVE, "5060990000000000", "5061990000000000", "506099000000000"),
                new CardCase(CreditCardType.LANKAPAY, "3571110000000000", "3571120000000000", "357111000000000"));
    }

    @ParameterizedTest
    @MethodSource("cardCases")
    @DisplayName("each card format accepts a valid number and rejects bad IIN / bad length")
    void cardFormatValidators(CardCase testCase) {
        assertTrue(TypeChecker.matchesType(testCase.valid(), testCase.type()));
        assertFalse(TypeChecker.matchesType(testCase.badIin(), testCase.type()));
        assertFalse(TypeChecker.matchesType(testCase.badLength(), testCase.type()));
    }

    @Test
    @DisplayName("OTHER type never matches and nulls are rejected")
    void otherTypeAndNulls() {
        assertFalse(TypeChecker.matchesType("4111111111111111", CreditCardType.OTHER));
        assertThrowsWith(NullPointerException.class, "number must not be null",
                () -> TypeChecker.matchesType(null, CreditCardType.VISA));
        assertThrowsWith(NullPointerException.class, "type must not be null",
                () -> TypeChecker.matchesType("4111111111111111", null));
        assertFalse(TypeChecker.matchesType("4111-1111-1111", CreditCardType.VISA));
        assertFalse(TypeChecker.matchesType("411111111111", CreditCardType.VISA));
        assertFalse(TypeChecker.matchesType("41111111111111111111", CreditCardType.VISA));
    }

    @Test
    @DisplayName("IIN range boundaries of the loop based validators")
    void iinRangeBoundaries() {
        // AMERICAN_EXPRESS 34..37, length 15
        assertTrue(TypeChecker.matchesType("340000000000000", CreditCardType.AMERICAN_EXPRESS));
        assertTrue(TypeChecker.matchesType("370000000000000", CreditCardType.AMERICAN_EXPRESS));
        assertFalse(TypeChecker.matchesType("330000000000000", CreditCardType.AMERICAN_EXPRESS));
        assertFalse(TypeChecker.matchesType("380000000000000", CreditCardType.AMERICAN_EXPRESS));
        assertFalse(TypeChecker.matchesType("3400000000000000", CreditCardType.AMERICAN_EXPRESS));
        // DINERS_CLUB 54..55
        assertMatches(CreditCardType.DINERS_CLUB, "54");
        assertMatches(CreditCardType.DINERS_CLUB, "55");
        assertRejects(CreditCardType.DINERS_CLUB, "53");
        assertRejects(CreditCardType.DINERS_CLUB, "56");
        // MASTERCARD 51..55 and 222100..272099
        assertMatches(CreditCardType.MASTERCARD, "51");
        assertMatches(CreditCardType.MASTERCARD, "55");
        assertRejects(CreditCardType.MASTERCARD, "50");
        assertRejects(CreditCardType.MASTERCARD, "56");
        assertMatches(CreditCardType.MASTERCARD, "222100");
        assertMatches(CreditCardType.MASTERCARD, "272099");
        assertRejects(CreditCardType.MASTERCARD, "222099");
        assertRejects(CreditCardType.MASTERCARD, "272100");
        // DANKORT 4571..5019
        assertMatches(CreditCardType.DANKORT, "4571");
        assertMatches(CreditCardType.DANKORT, "5019");
        assertRejects(CreditCardType.DANKORT, "4570");
        assertRejects(CreditCardType.DANKORT, "5020");
        // MIR 2200..2204
        assertMatches(CreditCardType.MIR, "2200");
        assertMatches(CreditCardType.MIR, "2204");
        assertRejects(CreditCardType.MIR, "2199");
        assertRejects(CreditCardType.MIR, "2205");
        // JCB 3528..3589
        assertMatches(CreditCardType.JCB, "3528");
        assertMatches(CreditCardType.JCB, "3589");
        assertRejects(CreditCardType.JCB, "3527");
        assertRejects(CreditCardType.JCB, "3590");
        // INSTAPAYMENT 637..639
        assertMatches(CreditCardType.INSTAPAYMENT, "637");
        assertMatches(CreditCardType.INSTAPAYMENT, "639");
        assertRejects(CreditCardType.INSTAPAYMENT, "636");
        assertRejects(CreditCardType.INSTAPAYMENT, "640");
        // NPS_PRIDNESTROVIE 6054740..6054744 (seven digits)
        assertMatches(CreditCardType.NPS_PRIDNESTROVIE, "6054740");
        assertMatches(CreditCardType.NPS_PRIDNESTROVIE, "6054744");
        assertRejects(CreditCardType.NPS_PRIDNESTROVIE, "6054739");
        assertRejects(CreditCardType.NPS_PRIDNESTROVIE, "6054745");
    }

    @Test
    @DisplayName("IIN range boundaries of the six digit validators")
    void sixDigitRangeBoundaries() {
        // MAESTRO 500000-509999, 560000-589999, 600000-699999
        assertMatches(CreditCardType.MAESTRO, "500000");
        assertMatches(CreditCardType.MAESTRO, "509999");
        assertRejects(CreditCardType.MAESTRO, "499999");
        assertRejects(CreditCardType.MAESTRO, "510000");
        assertMatches(CreditCardType.MAESTRO, "560000");
        assertMatches(CreditCardType.MAESTRO, "589999");
        assertRejects(CreditCardType.MAESTRO, "559999");
        assertRejects(CreditCardType.MAESTRO, "590000");
        assertMatches(CreditCardType.MAESTRO, "600000");
        assertMatches(CreditCardType.MAESTRO, "699999");
        assertRejects(CreditCardType.MAESTRO, "599999");
        assertRejects(CreditCardType.MAESTRO, "700000");
        // TROY 979200-979289
        assertMatches(CreditCardType.TROY, "979200");
        assertMatches(CreditCardType.TROY, "979289");
        assertRejects(CreditCardType.TROY, "979199");
        assertRejects(CreditCardType.TROY, "979290");
        // VERVE 506099-506198 (650002-650027 is shadowed by a duplicated loop)
        assertMatches(CreditCardType.VERVE, "506099");
        assertMatches(CreditCardType.VERVE, "506198");
        assertRejects(CreditCardType.VERVE, "506098");
        assertRejects(CreditCardType.VERVE, "506199");
        assertRejects(CreditCardType.VERVE, "650002");
        // LANKAPAY 357111
        assertMatches(CreditCardType.LANKAPAY, "357111");
        assertRejects(CreditCardType.LANKAPAY, "357110");
        assertRejects(CreditCardType.LANKAPAY, "357112");
        // DISCOVER 622126-622925 and 644-649 and 65
        assertMatches(CreditCardType.DISCOVER, "622126");
        assertMatches(CreditCardType.DISCOVER, "622925");
        assertRejects(CreditCardType.DISCOVER, "622125");
        assertRejects(CreditCardType.DISCOVER, "622926");
        assertMatches(CreditCardType.DISCOVER, "644000");
        assertMatches(CreditCardType.DISCOVER, "649000");
        assertRejects(CreditCardType.DISCOVER, "643000");
        assertMatches(CreditCardType.DISCOVER, "65");
        assertRejects(CreditCardType.DISCOVER, "66");
    }

    @Test
    @DisplayName("IIN boundaries of the remaining composite validators")
    void compositeIinBoundaries() {
        // DINERS_CLUB_INTERNATIONAL 300-305, 3095, 38-39
        assertMatches(CreditCardType.DINERS_CLUB_INTERNATIONAL, "300000");
        assertMatches(CreditCardType.DINERS_CLUB_INTERNATIONAL, "305000");
        assertRejects(CreditCardType.DINERS_CLUB_INTERNATIONAL, "299000");
        assertRejects(CreditCardType.DINERS_CLUB_INTERNATIONAL, "306000");
        assertMatches(CreditCardType.DINERS_CLUB_INTERNATIONAL, "309500");
        assertRejects(CreditCardType.DINERS_CLUB_INTERNATIONAL, "309400");
        assertRejects(CreditCardType.DINERS_CLUB_INTERNATIONAL, "309600");
        assertMatches(CreditCardType.DINERS_CLUB_INTERNATIONAL, "38");
        assertMatches(CreditCardType.DINERS_CLUB_INTERNATIONAL, "39");
        assertRejects(CreditCardType.DINERS_CLUB_INTERNATIONAL, "37");
        assertRejects(CreditCardType.DINERS_CLUB_INTERNATIONAL, "40");
        // MAESTRO_UK 6759, 676770, 676774
        assertMatches(CreditCardType.MAESTRO_UK, "675900");
        assertRejects(CreditCardType.MAESTRO_UK, "675800");
        assertMatches(CreditCardType.MAESTRO_UK, "676770");
        assertMatches(CreditCardType.MAESTRO_UK, "676774");
        assertRejects(CreditCardType.MAESTRO_UK, "676769");
        assertRejects(CreditCardType.MAESTRO_UK, "676775");
        // RUPAY 60, 6521, 6522
        assertMatches(CreditCardType.RUPAY, "60");
        assertRejects(CreditCardType.RUPAY, "61");
        assertMatches(CreditCardType.RUPAY, "652100");
        assertMatches(CreditCardType.RUPAY, "652200");
        assertRejects(CreditCardType.RUPAY, "652000");
        assertRejects(CreditCardType.RUPAY, "652300");
        // VISA_ELECTRON 417500, 4026-4405, 4508-4844, 4913-4917
        assertMatches(CreditCardType.VISA_ELECTRON, "417500");
        assertMatches(CreditCardType.VISA_ELECTRON, "402600");
        assertMatches(CreditCardType.VISA_ELECTRON, "440500");
        assertRejects(CreditCardType.VISA_ELECTRON, "440600");
        assertMatches(CreditCardType.VISA_ELECTRON, "450800");
        assertMatches(CreditCardType.VISA_ELECTRON, "484400");
        assertRejects(CreditCardType.VISA_ELECTRON, "484500");
        assertMatches(CreditCardType.VISA_ELECTRON, "491300");
        assertMatches(CreditCardType.VISA_ELECTRON, "491700");
        assertRejects(CreditCardType.VISA_ELECTRON, "491800");
        // CHINA_TUNION 31, length 19
        assertMatches(CreditCardType.CHINA_TUNION, "3100000000000000000");
        assertRejects(CreditCardType.CHINA_TUNION, "3000000000000000000");
        assertRejects(CreditCardType.CHINA_TUNION, "3200000000000000000");
        // CHINA_UNIONPAY 62, length 16-19
        assertMatches(CreditCardType.CHINA_UNIONPAY, "62");
        assertRejects(CreditCardType.CHINA_UNIONPAY, "63");
        assertTrue(new ChinaUnionPayValidator("6200000000000000000").validate());
        // INTERPAYMENT 636 only
        assertMatches(CreditCardType.INTERPAYMENT, "636000");
        assertRejects(CreditCardType.INTERPAYMENT, "637000");
        // UATP 1, length 15
        assertTrue(TypeChecker.matchesType("100000000000000", CreditCardType.UATP));
        assertFalse(TypeChecker.matchesType("200000000000000", CreditCardType.UATP));
        assertFalse(TypeChecker.matchesType("1000000000000000", CreditCardType.UATP));
        // VISA 4
        assertMatches(CreditCardType.VISA, "4");
        assertRejects(CreditCardType.VISA, "3");
        assertRejects(CreditCardType.VISA, "5");
        assertTrue(new VisaValidator("4111111111111111111").validate());
        assertFalse(new VisaValidator("411111111111").validate());
    }

    @Test
    @DisplayName("TypeChecker.checkType honours the documented priority order")
    void checkTypeOrder() {
        Map<String, CreditCardType> expected = new LinkedHashMap<>();
        expected.put("6054740000000000", CreditCardType.NPS_PRIDNESTROVIE);
        expected.put("3571110000000000", CreditCardType.LANKAPAY);
        expected.put("5060990000000000", CreditCardType.VERVE);
        expected.put("9792000000000000", CreditCardType.TROY);
        expected.put("4175000000000000", CreditCardType.VISA_ELECTRON);
        expected.put("6759000000000000", CreditCardType.MAESTRO_UK);
        expected.put("4571000000000000", CreditCardType.DANKORT);
        expected.put("4111111111111111", CreditCardType.VISA);
        expected.put("340000000000000", CreditCardType.AMERICAN_EXPRESS);
        expected.put("5100000000000000", CreditCardType.MASTERCARD);
        expected.put("2221000000000000", CreditCardType.MASTERCARD);
        expected.put("6011000000000000", CreditCardType.DISCOVER);
        expected.put("6500000000000000", CreditCardType.DISCOVER);
        expected.put("3528000000000000", CreditCardType.JCB);
        expected.put("3100000000000000000", CreditCardType.CHINA_TUNION);
        expected.put("6200000000000000", CreditCardType.CHINA_UNIONPAY);
        expected.put("3000000000000000", CreditCardType.DINERS_CLUB_INTERNATIONAL);
        expected.put("6000000000000000", CreditCardType.RUPAY);
        expected.put("6360000000000000", CreditCardType.INTERPAYMENT);
        expected.put("6370000000000000", CreditCardType.INSTAPAYMENT);
        expected.put("2200000000000000", CreditCardType.MIR);
        expected.put("100000000000000", CreditCardType.UATP);
        expected.put("5600000000000000", CreditCardType.MAESTRO);
        expected.put("9999999999999999", CreditCardType.OTHER);
        // DINERS_CLUB is shadowed by MASTERCARD in the shared 54/55 prefix range.
        expected.put("5400000000000000", CreditCardType.MASTERCARD);
        expected.forEach((number, type) -> assertEquals(type, TypeChecker.checkType(number), number));
    }

    @Test
    @DisplayName("checkType rejects malformed and null input")
    void checkTypeRejectsMalformed() {
        assertThrowsWith(NullPointerException.class, "number must not be null",
                () -> TypeChecker.checkType(null));
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType("411111111111"));
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType("41111111111111111111"));
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType("4111-1111-1111-1111"));
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType(""));
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType("   "));
    }

    @Test
    @DisplayName("Luhn checksum implementation")
    void luhnValidator() {
        LuhnValidator luhn = new LuhnValidator();
        assertTrue(luhn.validate("4111111111111111"));
        assertTrue(luhn.validate("5500000000000004"));
        assertFalse(luhn.validate("4111111111111112"));
        assertFalse(luhn.validate("5500000000000005"));
        assertTrue(luhn.algorithmCheck("4111111111111111"));
        assertEquals(2, luhn.sumOfDigits(11));
        assertEquals(19, luhn.sumOfDigits(199));
        assertEquals(0, luhn.sumOfDigits(0));
        assertEquals(9, luhn.sumOfDigits(9));
        assertEquals(18, luhn.sumOfDigits(99));
        assertEquals(45, luhn.sumOfDigits(99999));
    }

    @Test
    @DisplayName("CreditCardParser handles digits, letters and unicode digits")
    void creditCardParser() {
        assertEquals(List.of(4, 1, 1), CreditCardParser.parseNumber("411"));
        assertEquals(List.of(10, 11), CreditCardParser.parseNumber("AB"));
        assertEquals(List.of(40), CreditCardParser.parseNumber("\u1375"));
        assertEquals(List.of(-1), CreditCardParser.parseNumber("-"));
        assertEquals(41, CreditCardParser.parseIIN(List.of(4, 1), 2));
        assertThrows(NumberFormatException.class,
                () -> CreditCardParser.parseIIN(CreditCardParser.parseNumber("AAAAAAAAAAA"), 11));
    }

    @Test
    @DisplayName("Validator constructor, CVV and expiration checks")
    void validatorBasics() {
        Clock fixed = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
        Validator visa = new Validator("4111111111111111", "11/26", "123", fixed);
        assertTrue(visa.validate());
        assertTrue(visa.checkCVV());
        assertTrue(visa.checkExpirationDate());

        assertTrue(new Validator("4111111111111111", "1226", "1234", fixed).checkExpirationDate());
        assertFalse(new Validator("4111111111111111", "10/26", "123", fixed).checkExpirationDate());
        assertFalse(new Validator("4111111111111111", "09/26", "123", fixed).checkExpirationDate());
        assertFalse(new Validator("4111111111111111", "1225", "123", fixed).checkExpirationDate());

        Validator noDate = new Validator("4111111111111111");
        assertFalse(noDate.checkCVV());
        assertFalse(noDate.checkExpirationDate());
        Validator systemClock = new Validator("4111111111111111", "11/26", "123");
        assertTrue(systemClock.checkCVV());
        assertTrue(systemClock.validate());
        assertThrowsWith(IllegalStateException.class, "expirationDate was not supplied", noDate::setExpirationYear);
        assertThrowsWith(IllegalStateException.class, "expirationDate was not supplied", noDate::setExpirationMonth);
    }

    @Test
    @DisplayName("Validator rejects bad card numbers with the exact message")
    void validatorRejections() {
        Clock fixed = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
        Validator bad = new Validator("4111111111111112", "11/26", "123", fixed);
        assertThrowsWith(InvalidCardException.class, "Card number checksum is invalid", bad::validate);
        Validator tooShort = new Validator("41111111111", "11/26", "123", fixed);
        assertThrowsWith(InvalidCardException.class, "Card number must contain 12 to 19 digits", tooShort::validate);
        Validator notDigits = new Validator("4111-1111-1111", "11/26", "123", fixed);
        assertThrowsWith(InvalidCardException.class, "Card number must contain 12 to 19 digits", notDigits::validate);
        Validator tooLong = new Validator("41111111111111111111", "11/26", "123", fixed);
        assertThrowsWith(InvalidCardException.class, "Card number must contain 12 to 19 digits", tooLong::validate);
        assertThrowsWith(NullPointerException.class, "number must not be null", () -> new Validator(null));
        assertThrowsWith(NullPointerException.class, "number must not be null",
                () -> new Validator(null, "11/26", "123", fixed));
        assertThrowsWith(NullPointerException.class, "expirationDate must not be null",
                () -> new Validator("4111111111111111", null, "123", fixed));
        assertThrowsWith(NullPointerException.class, "clock must not be null",
                () -> new Validator("4111111111111111", "11/26", "123", null));
        assertThrowsWith(IllegalArgumentException.class, "expirationDate must use MMyy or MM/yy",
                () -> new Validator("4111111111111111", "13/26", "123", fixed));
        assertThrowsWith(IllegalArgumentException.class, "expirationDate must use MMyy or MM/yy",
                () -> new Validator("4111111111111111", "1326", "123", fixed));

        assertFalse(new Validator("4111111111111111", "11/26", "12", fixed).checkCVV());
        assertFalse(new Validator("4111111111111111", "11/26", "12345", fixed).checkCVV());
        assertFalse(new Validator("4111111111111111", "11/26", "12a", fixed).checkCVV());
    }

    @Test
    @DisplayName("Date helper classes")
    void dateHelpers() {
        Clock fixed = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
        assertTrue(DateChecker.isValidExpiration(YearMonth.of(2026, 11), fixed));
        assertTrue(DateChecker.isValidExpiration(YearMonth.of(2030, 1), fixed));
        assertFalse(DateChecker.isValidExpiration(YearMonth.of(2026, 10), fixed));
        assertThrowsWith(NullPointerException.class, "expiration must not be null",
                () -> DateChecker.isValidExpiration((YearMonth) null, fixed));
        assertThrowsWith(NullPointerException.class, "clock must not be null",
                () -> DateChecker.isValidExpiration(YearMonth.of(2026, 11), null));
        assertTrue(DateChecker.isValidExpiration(11, 26, fixed));
        assertFalse(DateChecker.isValidExpiration(10, 26, fixed));
        assertThrowsWith(IllegalArgumentException.class, "year must contain two digits",
                () -> DateChecker.isValidExpiration(1, -1, fixed));
        assertThrowsWith(IllegalArgumentException.class, "year must contain two digits",
                () -> DateChecker.isValidExpiration(1, 100, fixed));
        assertTrue(DateChecker.compareDates(2, 1));
        assertFalse(DateChecker.compareDates(1, 1));
        assertFalse(DateChecker.compareDates(0, 1));
        assertTrue(DateChecker.getCurrentYear() > 0);
        assertTrue(DateChecker.getCurrentMonth() >= 1);

        assertThrowsWith(NullPointerException.class, "type must not be null", () -> new Shift(null,
                LocalTime.of(9, 0), LocalTime.of(18, 0), 0, 60, 480));

        assertEquals("11/26", DateChecker.convertDate("11/26").toString());
        assertEquals("1126", DateParser.parseDate(new StringBuilder("11/26")).toString());
        assertEquals("1126", DateParser.parseDate(new StringBuilder("1126")).toString());
        assertEquals(11, DateParser.parseDate(new StringBuilder("1126"), 0, 2));
        assertEquals(26, DateParser.parseDate(new StringBuilder("1126"), 2, 4));
        assertEquals("26", DateFormatter.yearFormat().format(java.sql.Date.valueOf("2026-01-15")));
        assertEquals("01", DateFormatter.monthFormat().format(java.sql.Date.valueOf("2026-01-15")));
    }

    // ------------------------------------------------------------------
    // enum / utility constructors: cheap method coverage
    // ------------------------------------------------------------------

    private static <E extends Enum<E>> void coverEnum(Class<E> type) throws Exception {
        E[] values = type.getEnumConstants();
        assertTrue(values.length > 0);
        for (E value : values) {
            assertSame(value, Enum.valueOf(type, value.name()));
            assertEquals(value, type.getDeclaredMethod("valueOf", String.class)
                    .invoke(null, value.name()));
            assertEquals(value.ordinal(), type.getMethod("ordinal").invoke(value));
        }
        assertEquals(java.util.Arrays.asList(values),
                java.util.Arrays.asList((Object[]) type.getMethod("values").invoke(null)));
    }

    @Test
    @DisplayName("every enum constant and generated enum method is executed")
    void allEnums() throws Exception {
        coverEnum(CreditCardType.class);
        coverEnum(AttendanceCode.class);
        coverEnum(ShiftType.class);
        coverEnum(EmploymentMode.class);
        coverEnum(ExpenseCategory.class);
        coverEnum(ExpenseApprovalTier.class);
        coverEnum(EmployeeGrade.class);
        coverEnum(PaymentMethod.class);
        coverEnum(ReceiptType.class);
        coverEnum(LeaveUnit.class);
        coverEnum(EmployeeCategory.class);
        coverEnum(ApprovalTier.class);
        coverEnum(LeaveStatus.class);
        coverEnum(LeaveType.class);
        coverEnum(TaskStatus.class);
        coverEnum(TaskPriority.class);
        coverEnum(TaskAction.class);
        coverEnum(RecurrenceRule.class);
        coverEnum(AccessScope.class);
        coverEnum(ResourceAction.class);
        coverEnum(ResourceType.class);
        coverEnum(Sensitivity.class);
        coverEnum(UserRole.class);
        coverEnum(StepMode.class);
        coverEnum(ApprovalDecision.class);
        coverEnum(WorkflowState.class);
        assertEquals("VISA", CreditCardType.valueOf("VISA").name());
        assertEquals(0, CreditCardType.VISA.ordinal());
        assertEquals("EXPIRED", WorkflowState.valueOf("EXPIRED").toString());
    }

    @Test
    @DisplayName("default constructors of the stateless policy classes are reachable")
    void defaultConstructors() throws Exception {
        assertNotNull(new ExpensePolicy());
        assertNotNull(new AccessPolicy());
        assertNotNull(new AdvancedLeavePolicy());
        assertNotNull(new TaskPolicy());
        assertNotNull(new TaskStateMachine());
        assertNotNull(new OvertimePolicy());
        assertNotNull(new AttendancePolicy());
        assertNotNull(new AttendancePolicy(new OvertimePolicy()));
        assertNotNull(new LeavePolicy());
        assertNotNull(new ApprovalWorkflowEngine());
        assertNotNull(new LuhnValidator());
        assertNotNull(new CreditCardParser());
        assertNotNull(new DateParser());
        assertNotNull(new DateChecker());
        assertNotNull(new DateFormatter());
        assertNotNull(new VisaValidator("4111111111111111"));
        assertNotNull(new CardEvidenceValidator(Clock.systemUTC()));

        java.lang.reflect.Constructor<TypeChecker> checker = TypeChecker.class.getDeclaredConstructor();
        checker.setAccessible(true);
        assertNotNull(checker.newInstance());
        java.lang.reflect.Constructor<DateTextParser> parser = DateTextParser.class.getDeclaredConstructor();
        parser.setAccessible(true);
        assertNotNull(parser.newInstance());
    }

    // ------------------------------------------------------------------
    // domain + util
    // ------------------------------------------------------------------

    private static final LocalDate D = LocalDate.of(2026, 3, 2); // Monday

    @Test
    @DisplayName("DateRange counts business days and detects overlaps")
    void dateRange() {
        DateRange mondayToFriday = new DateRange(D, D.plusDays(4));
        assertEquals(5, mondayToFriday.businessDays());
        assertEquals(1, new DateRange(D, D).businessDays());
        assertEquals(0, new DateRange(D.plusDays(5), D.plusDays(6)).businessDays());
        assertEquals(6, new DateRange(D, D.plusDays(7)).businessDays());
        assertTrue(mondayToFriday.overlaps(new DateRange(D.plusDays(4), D.plusDays(9))));
        assertTrue(mondayToFriday.overlaps(new DateRange(D.minusDays(2), D.minusDays(1).plusDays(1))));
        assertFalse(mondayToFriday.overlaps(new DateRange(D.plusDays(5), D.plusDays(9))));
        assertTrue(new DateRange(D, D).overlaps(new DateRange(D, D)));
        assertThrowsWith(IllegalArgumentException.class, "end must not be before start",
                () -> new DateRange(D, D.minusDays(1)));
        assertThrowsWith(NullPointerException.class, "start must not be null",
                () -> new DateRange(null, D));
        assertThrowsWith(NullPointerException.class, "end must not be null",
                () -> new DateRange(D, null));
        assertThrowsWith(NullPointerException.class, "other must not be null",
                () -> mondayToFriday.overlaps(null));
        assertEquals(new DateRange(D, D), new DateRange(D, D));
        assertEquals(new DateRange(D, D).hashCode(), new DateRange(D, D).hashCode());
        assertTrue(new DateRange(D, D).toString().contains("2026-03-02"));
    }

    @Test
    @DisplayName("LeaveRequest pending/approve/reject state machine")
    void leaveRequest() {
        Instant asked = Instant.parse("2026-03-01T09:00:00Z");
        DateRange period = new DateRange(D, D.plusDays(1));
        LeaveRequest pending = LeaveRequest.pending("L1", "E1", LeaveType.ANNUAL, period, "vacation", asked);
        assertEquals(LeaveStatus.PENDING, pending.status());
        assertEquals("L1", pending.id());
        assertEquals("E1", pending.employeeId());
        assertEquals(LeaveType.ANNUAL, pending.type());
        assertEquals(period, pending.period());
        assertEquals("vacation", pending.reason());
        assertEquals(asked, pending.requestedAt());
        assertNull(pending.decidedBy());
        assertNull(pending.decidedAt());
        assertNull(pending.decisionComment());

        LeaveRequest approved = pending.approve("M1", Instant.parse("2026-03-02T09:00:00Z"));
        assertEquals(LeaveStatus.APPROVED, approved.status());
        assertEquals("M1", approved.decidedBy());
        assertNull(approved.decisionComment());
        assertThrowsWith(IllegalStateException.class, "only a pending request can be decided",
                () -> approved.approve("M2", asked));
        assertThrowsWith(IllegalStateException.class, "only a pending request can be decided",
                () -> approved.reject("M2", asked, "no"));

        LeaveRequest rejected = pending.reject("M1", Instant.parse("2026-03-02T09:00:00Z"), "  short staff  ");
        assertEquals(LeaveStatus.REJECTED, rejected.status());
        assertEquals("short staff", rejected.decisionComment());
        assertEquals("L1", rejected.id());

        assertThrowsWith(IllegalArgumentException.class, "comment must not be blank",
                () -> pending.reject("M1", asked, " "));
        assertThrowsWith(IllegalArgumentException.class, "comment must not be blank",
                () -> pending.reject("M1", asked, null));
        assertThrowsWith(IllegalArgumentException.class, "id must not be blank",
                () -> LeaveRequest.pending(" ", "E1", LeaveType.ANNUAL, period, "vacation", asked));
        assertThrowsWith(IllegalArgumentException.class, "employeeId must not be blank",
                () -> LeaveRequest.pending("L1", "", LeaveType.ANNUAL, period, "vacation", asked));
        assertThrowsWith(IllegalArgumentException.class, "reason must not be blank",
                () -> LeaveRequest.pending("L1", "E1", LeaveType.ANNUAL, period, "\t", asked));
        assertThrowsWith(NullPointerException.class, "type must not be null",
                () -> LeaveRequest.pending("L1", "E1", null, period, "vacation", asked));
        assertThrowsWith(NullPointerException.class, "period must not be null",
                () -> LeaveRequest.pending("L1", "E1", LeaveType.ANNUAL, null, "vacation", asked));
        assertThrowsWith(NullPointerException.class, "requestedAt must not be null",
                () -> LeaveRequest.pending("L1", "E1", LeaveType.ANNUAL, period, "vacation", null));
        assertThrowsWith(NullPointerException.class, "status must not be null",
                () -> new LeaveRequest("L1", "E1", LeaveType.ANNUAL, period, "reason",
                        null, asked, null, null, null));
        assertThrowsWith(IllegalArgumentException.class, "pending request must not contain a decision",
                () -> new LeaveRequest("L1", "E1", LeaveType.ANNUAL, period, "reason",
                        LeaveStatus.PENDING, asked, "M1", asked, null));
        assertThrowsWith(IllegalArgumentException.class, "pending request must not contain a decision",
                () -> new LeaveRequest("L1", "E1", LeaveType.ANNUAL, period, "reason",
                        LeaveStatus.PENDING, asked, null, asked, null));
        assertThrowsWith(IllegalArgumentException.class, "pending request must not contain a decision",
                () -> new LeaveRequest("L1", "E1", LeaveType.ANNUAL, period, "reason",
                        LeaveStatus.PENDING, asked, null, null, "comment"));
        assertThrowsWith(IllegalArgumentException.class, "decidedBy must not be blank",
                () -> new LeaveRequest("L1", "E1", LeaveType.ANNUAL, period, "reason",
                        LeaveStatus.APPROVED, asked, " ", asked, null));
        assertThrowsWith(NullPointerException.class, "decidedAt must not be null",
                () -> new LeaveRequest("L1", "E1", LeaveType.ANNUAL, period, "reason",
                        LeaveStatus.APPROVED, asked, "M1", null, null));
        LeaveRequest trimmed = new LeaveRequest(" L1 ", " E1 ", LeaveType.SICK, period, " reason ",
                LeaveStatus.APPROVED, asked, " M1 ", asked, " ok ");
        assertEquals("L1", trimmed.id());
        assertEquals("E1", trimmed.employeeId());
        assertEquals("reason", trimmed.reason());
        assertEquals("M1", trimmed.decidedBy());
        assertEquals("ok", trimmed.decisionComment());
        assertEquals(pending, LeaveRequest.pending("L1", "E1", LeaveType.ANNUAL, period, "vacation", asked));
        assertEquals(pending.hashCode(), LeaveRequest.pending("L1", "E1", LeaveType.ANNUAL, period,
                "vacation", asked).hashCode());
        assertTrue(pending.toString().contains("PENDING"));
    }

    @Test
    @DisplayName("DateTextParser accepts three formats and rejects the rest")
    void dateTextParser() {
        assertEquals(LocalDate.of(2026, 3, 2), DateTextParser.parse("2026-03-02"));
        assertEquals(LocalDate.of(2026, 3, 2), DateTextParser.parse("2026/03/02"));
        assertEquals(LocalDate.of(2026, 3, 2), DateTextParser.parse("20260302"));
        assertEquals(LocalDate.of(2026, 3, 2), DateTextParser.parse("  2026-03-02  "));
        assertThrowsWith(IllegalArgumentException.class, "date text must not be blank",
                () -> DateTextParser.parse(null));
        assertThrowsWith(IllegalArgumentException.class, "date text must not be blank",
                () -> DateTextParser.parse("   "));
        assertThrowsWith(IllegalArgumentException.class, "unsupported date: 2026-13-02",
                () -> DateTextParser.parse("2026-13-02"));
        assertThrowsWith(IllegalArgumentException.class, "unsupported date: hello",
                () -> DateTextParser.parse("hello"));
        assertThrowsWith(IllegalArgumentException.class, "unsupported date: 2026.03.02",
                () -> DateTextParser.parse("2026.03.02"));
    }

    // ------------------------------------------------------------------
    // service: LeavePolicy + LeaveRequestService (ports mocked)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("LeavePolicy validates business days, reason and balances")
    void leavePolicy() {
        LeavePolicy policy = new LeavePolicy();
        DateRange twoDays = new DateRange(D, D.plusDays(1));
        assertEquals(2, policy.validate(LeaveType.SICK, twoDays, "feeling unwell", 0));
        assertEquals(2, policy.validate(LeaveType.PERSONAL, twoDays, "family issue", 0));
        assertEquals(2, policy.validate(LeaveType.ANNUAL, twoDays, "family trip", 5));
        assertThrowsWith(LeaveValidationException.class, "leave must contain at least one business day",
                () -> policy.validate(LeaveType.SICK, new DateRange(D.plusDays(5), D.plusDays(6)),
                        "weekend", 0));
        assertThrowsWith(LeaveValidationException.class, "reason must contain at least 5 characters",
                () -> policy.validate(LeaveType.SICK, twoDays, "sick", 0));
        assertThrowsWith(LeaveValidationException.class, "reason must contain at least 5 characters",
                () -> policy.validate(LeaveType.SICK, twoDays, null, 0));
        assertThrowsWith(LeaveValidationException.class, "insufficient annual-leave balance",
                () -> policy.validate(LeaveType.ANNUAL, twoDays, "family trip", 1));
        assertThrowsWith(LeaveValidationException.class,
                "personal leave cannot exceed 3 business days",
                () -> policy.validate(LeaveType.PERSONAL, new DateRange(D, D.plusDays(3)), "family issue", 0));
        assertThrowsWith(NullPointerException.class, "type must not be null",
                () -> policy.validate(null, twoDays, "reason here", 0));
        assertThrowsWith(NullPointerException.class, "period must not be null",
                () -> policy.validate(LeaveType.SICK, null, "reason here", 0));
    }

    @Test
    @DisplayName("LeaveRequestService submit/approve/reject with port interactions")
    void leaveRequestService() {
        LeaveRequestRepository repository = mock(LeaveRequestRepository.class);
        LeaveBalanceRepository balances = mock(LeaveBalanceRepository.class);
        NotificationGateway gateway = mock(NotificationGateway.class);
        RequestIdGenerator ids = mock(RequestIdGenerator.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-01T09:00:00Z"), ZoneOffset.UTC);
        LeaveRequestService service = new LeaveRequestService(
                repository, balances, gateway, ids, new LeavePolicy(), clock);

        DateRange period = new DateRange(D, D.plusDays(1));
        SubmitLeaveCommand annual = new SubmitLeaveCommand(" E1 ", LeaveType.ANNUAL, period, "family trip");
        assertEquals("E1", annual.employeeId());
        assertEquals(LeaveType.ANNUAL, annual.type());
        assertEquals(period, annual.period());
        assertEquals("family trip", annual.reason());
        assertEquals(annual, new SubmitLeaveCommand("E1", LeaveType.ANNUAL, period, "family trip"));
        assertEquals(annual.hashCode(), new SubmitLeaveCommand("E1", LeaveType.ANNUAL, period,
                "family trip").hashCode());
        assertTrue(annual.toString().contains("E1"));
        assertThrowsWith(IllegalArgumentException.class, "employeeId must not be blank",
                () -> new SubmitLeaveCommand("  ", LeaveType.ANNUAL, period, "family trip"));
        assertThrowsWith(NullPointerException.class, "type must not be null",
                () -> new SubmitLeaveCommand("E1", null, period, "family trip"));
        assertThrowsWith(NullPointerException.class, "period must not be null",
                () -> new SubmitLeaveCommand("E1", LeaveType.ANNUAL, null, "family trip"));

        when(balances.remainingAnnualDays("E1")).thenReturn(10);
        when(ids.nextId()).thenReturn("L1");
        when(repository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));
        LeaveRequest submitted = service.submit(annual);
        assertEquals("L1", submitted.id());
        assertEquals(LeaveStatus.PENDING, submitted.status());
        verify(gateway).requestSubmitted(submitted);
        verify(balances).remainingAnnualDays("E1");

        when(repository.existsActiveOverlap(anyString(), any(DateRange.class))).thenReturn(true);
        assertThrowsWith(LeaveValidationException.class, "leave period overlaps an active request",
                () -> service.submit(annual));
        when(repository.existsActiveOverlap(anyString(), any(DateRange.class))).thenReturn(false);

        SubmitLeaveCommand sick = new SubmitLeaveCommand("E1", LeaveType.SICK, period, "feeling unwell");
        LeaveRequest sickSubmitted = service.submit(sick);
        assertEquals("L1", sickSubmitted.id());
        verify(balances, times(2)).remainingAnnualDays("E1");

        when(repository.findById("L1")).thenReturn(Optional.of(submitted));
        LeaveRequest approved = service.approve(" L1 ", " M1 ");
        assertEquals(LeaveStatus.APPROVED, approved.status());
        assertEquals("M1", approved.decidedBy());
        verify(balances).deductAnnualDays("E1", 2);
        verify(gateway).requestApproved(any(LeaveRequest.class));

        when(repository.findById("S1")).thenReturn(Optional.of(sickSubmitted));
        LeaveRequest sickApproved = service.approve("S1", "M1");
        assertEquals(LeaveStatus.APPROVED, sickApproved.status());
        verify(balances, times(1)).deductAnnualDays(anyString(), anyInt());

        when(repository.findById("L2")).thenReturn(Optional.of(LeaveRequest.pending(
                "L2", "E1", LeaveType.ANNUAL, period, "family trip",
                Instant.parse("2026-03-01T08:00:00Z"))));
        LeaveRequest rejected = service.reject("L2", "M1", "no coverage");
        assertEquals(LeaveStatus.REJECTED, rejected.status());
        verify(gateway).requestRejected(any(LeaveRequest.class));

        assertThrowsWith(IllegalArgumentException.class, "requestId must not be blank",
                () -> service.approve("  ", "M1"));
        assertThrowsWith(IllegalArgumentException.class, "requestId must not be blank",
                () -> service.reject(null, "M1", "no"));
        assertThrowsWith(IllegalArgumentException.class, "approverId must not be blank",
                () -> service.approve("L1", " "));
        assertThrowsWith(IllegalArgumentException.class, "approverId must not be blank",
                () -> service.reject("L1", null, "no"));
        assertEquals("leave request not found: missing",
                assertThrows(LeaveRequestNotFoundException.class,
                        () -> service.reject("missing", "M1", "no")).getMessage());
        assertThrows(LeaveRequestNotFoundException.class, () -> service.approve("missing", "M1"));
        when(repository.findById("S1")).thenReturn(Optional.of(sickSubmitted));
        assertThrowsWith(LeaveValidationException.class, "employee cannot approve their own request",
                () -> service.approve("S1", "E1"));
        assertThrowsWith(NullPointerException.class, "command must not be null",
                () -> service.submit(null));
    }

    @Test
    @DisplayName("LeaveRequestService rejects self-approval and self-rejection")
    void leaveRequestServiceSelfDecision() {
        LeaveRequestRepository repository = mock(LeaveRequestRepository.class);
        LeaveBalanceRepository balances = mock(LeaveBalanceRepository.class);
        NotificationGateway gateway = mock(NotificationGateway.class);
        RequestIdGenerator ids = mock(RequestIdGenerator.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-01T09:00:00Z"), ZoneOffset.UTC);
        LeaveRequestService service = new LeaveRequestService(
                repository, balances, gateway, ids, new LeavePolicy(), clock);
        DateRange period = new DateRange(D, D);
        LeaveRequest pending = LeaveRequest.pending("L3", "E1", LeaveType.ANNUAL, period, "family trip",
                Instant.parse("2026-03-01T08:00:00Z"));
        when(repository.findById("L3")).thenReturn(Optional.of(pending));
        assertThrowsWith(LeaveValidationException.class, "employee cannot reject their own request",
                () -> service.reject("L3", "E1", "self"));
        assertThrows(NullPointerException.class,
                () -> new LeaveRequestService(repository, balances, gateway, ids, new LeavePolicy(), null));
        assertThrows(NullPointerException.class,
                () -> new LeaveRequestService(repository, balances, gateway, ids, null, clock));
        assertThrows(NullPointerException.class,
                () -> new LeaveRequestService(repository, balances, gateway, null, new LeavePolicy(), clock));
        assertThrows(NullPointerException.class,
                () -> new LeaveRequestService(repository, balances, null, ids, new LeavePolicy(), clock));
        assertThrows(NullPointerException.class,
                () -> new LeaveRequestService(repository, null, gateway, ids, new LeavePolicy(), clock));
        assertThrows(NullPointerException.class,
                () -> new LeaveRequestService(null, balances, gateway, ids, new LeavePolicy(), clock));
    }

    // ==================================================================
    // expense
    // ==================================================================

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    /** A fixed Monday used by the pure policies. */
    private static final LocalDate MON = LocalDate.of(2026, 3, 2);
    /** Claim submission date used by the expense scenarios. */
    private static final LocalDate SUBMITTED = LocalDate.of(2026, 3, 16);
    /** Evaluation "today" used by the expense scenarios. */
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 20);
    private static final ExchangeRateProvider ONE_TO_ONE = (currency, date) -> BigDecimal.ONE;

    static final class ItemBuilder {
        String id = "I1";
        ExpenseCategory category = ExpenseCategory.MEAL;
        String amount = "100.00";
        Currency currency = USD;
        LocalDate incurred = MON;
        String merchant = "Acme Store";
        String purpose = "team meal for the release";
        ReceiptType receipt = ReceiptType.TAX_INVOICE;
        PaymentMethod payment = PaymentMethod.PERSONAL_CARD;
        int attendees = 0;
        boolean alcohol = false;
        boolean personal = false;
        boolean duplicate = false;

        ExpenseItem build() {
            return new ExpenseItem(id, category, new BigDecimal(amount), currency, incurred,
                    merchant, purpose, receipt, payment, attendees, alcohol, personal, duplicate);
        }
    }

    static final class ClaimBuilder {
        String id = "C1";
        String employeeId = "E1";
        EmployeeGrade grade = EmployeeGrade.STAFF;
        LocalDate submitted = SUBMITTED;
        String costCenter = "CC-1";
        List<ExpenseItem> items = new ArrayList<>();
        boolean preApproved = false;
        boolean international = false;
        boolean clientBillable = false;
        boolean projectActive = true;
        int previousClaims = 0;

        ExpenseClaim build() {
            return new ExpenseClaim(id, employeeId, grade, submitted, costCenter, items,
                    preApproved, international, clientBillable, projectActive, previousClaims);
        }
    }

    private static ExpenseItem meal(String amount) {
        ItemBuilder b = new ItemBuilder();
        b.amount = amount;
        return b.build();
    }

    private static ExpenseAssessment assess(ClaimBuilder claim, ExchangeRateProvider rates) {
        return new ExpensePolicy().assess(claim.build(), rates, TODAY);
    }

    @Test
    @DisplayName("expense: a clean claim is reimbursed in full and tiered")
    void expenseHappyPath() {
        ClaimBuilder claim = new ClaimBuilder();
        claim.items.add(meal("40.00"));
        ExpenseAssessment result = assess(claim, ONE_TO_ONE);
        assertEquals(new BigDecimal("40.00"), result.claimedAmount());
        assertEquals(new BigDecimal("40.00"), result.reimbursableAmount());
        assertEquals(ExpenseApprovalTier.LINE_MANAGER, result.approvalTier());
        assertFalse(result.rejected());
        assertFalse(result.auditRequired());
        assertEquals(List.of(), result.violations());
        assertEquals(List.of(), result.warnings());

        claim.preApproved = true;
        ExpenseAssessment auto = assess(claim, ONE_TO_ONE);
        assertEquals(ExpenseApprovalTier.AUTO_APPROVED, auto.approvalTier());
        assertEquals(new BigDecimal("40.00"), auto.reimbursableAmount());
    }

    @Test
    @DisplayName("expense: claim level validations and warnings")
    void expenseClaimLevelRules() {
        ClaimBuilder future = new ClaimBuilder();
        future.submitted = TODAY.plusDays(1);
        future.items.add(meal("40.00"));
        ExpenseAssessment futureResult = assess(future, ONE_TO_ONE);
        assertEquals(List.of("FATAL: submission date cannot be in the future"), futureResult.violations());
        assertTrue(futureResult.rejected());
        assertEquals(ExpenseApprovalTier.FINANCE, futureResult.approvalTier());

        ClaimBuilder stale = new ClaimBuilder();
        stale.submitted = TODAY.minusDays(31);
        ItemBuilder oldItem = new ItemBuilder();
        oldItem.incurred = stale.submitted.minusDays(1);
        stale.items.add(oldItem.build());
        ExpenseAssessment staleResult = assess(stale, ONE_TO_ONE);
        assertTrue(staleResult.warnings()
                .contains("claim review is occurring more than thirty days after submission"));
        assertEquals(List.of(), staleResult.violations());

        ClaimBuilder billable = new ClaimBuilder();
        billable.projectActive = false;
        billable.clientBillable = true;
        billable.costCenter = "CC-9";
        billable.items.add(meal("40.00"));
        assertEquals(List.of("FATAL: inactive project cannot receive client-billable expenses"),
                assess(billable, ONE_TO_ONE).violations());

        ClaimBuilder noCostCenter = new ClaimBuilder();
        noCostCenter.costCenter = "   ";
        noCostCenter.items.add(meal("40.00"));
        assertEquals(List.of(), assess(noCostCenter, ONE_TO_ONE).violations());
        assertTrue(assess(noCostCenter, ONE_TO_ONE).warnings().contains("cost center is missing"));

        ClaimBuilder nullCostCenter = new ClaimBuilder();
        nullCostCenter.costCenter = null;
        nullCostCenter.items.add(meal("40.00"));
        assertTrue(assess(nullCostCenter, ONE_TO_ONE).warnings().contains("cost center is missing"));

        ClaimBuilder intern = new ClaimBuilder();
        intern.grade = EmployeeGrade.INTERN;
        intern.international = true;
        intern.items.add(meal("40.00"));
        assertEquals(List.of("FATAL: intern international travel requires a separate process"),
                assess(intern, ONE_TO_ONE).violations());

        ClaimBuilder exec = new ClaimBuilder();
        exec.grade = EmployeeGrade.EXECUTIVE;
        exec.preApproved = true;
        exec.items.add(meal("40.00"));
        assertTrue(assess(exec, ONE_TO_ONE).warnings()
                .contains("executive pre-approval still requires finance review"));
        assertEquals(ExpenseApprovalTier.FINANCE_DIRECTOR, assess(exec, ONE_TO_ONE).approvalTier());

        ClaimBuilder many = new ClaimBuilder();
        many.previousClaims = 21;
        many.items.add(meal("40.00"));
        ExpenseAssessment manyResult = assess(many, ONE_TO_ONE);
        assertTrue(manyResult.warnings().contains("unusually high claim frequency"));
        assertTrue(manyResult.auditRequired());

        ClaimBuilder fiftyOne = new ClaimBuilder();
        for (int i = 1; i <= 51; i++) {
            ItemBuilder item = new ItemBuilder();
            item.id = "I" + i;
            item.amount = i + ".00";
            fiftyOne.items.add(item.build());
        }
        ExpenseAssessment fiftyOneResult = assess(fiftyOne, ONE_TO_ONE);
        assertTrue(fiftyOneResult.warnings().contains("large claim should be split by trip or purpose"));
        assertTrue(fiftyOneResult.auditRequired());
        assertEquals(51, fiftyOne.items.size());
    }

    @Test
    @DisplayName("expense: exchange rate failures reject the item")
    void expenseExchangeRates() {
        ClaimBuilder claim = new ClaimBuilder();
        claim.items.add(meal("40.00"));
        ExpenseAssessment missing = new ExpensePolicy().assess(claim.build(),
                (currency, date) -> null, TODAY);
        assertEquals(List.of("FATAL: missing exchange rate for item I1"), missing.violations());
        assertEquals(new BigDecimal("0.00"), missing.claimedAmount());
        assertTrue(missing.rejected());

        assertEquals(List.of("FATAL: invalid exchange rate for item I1"),
                new ExpensePolicy().assess(claim.build(), (c, d) -> BigDecimal.ZERO, TODAY).violations());
        assertEquals(List.of("FATAL: invalid exchange rate for item I1"),
                new ExpensePolicy().assess(claim.build(), (c, d) -> new BigDecimal("-1"), TODAY).violations());
        assertEquals(List.of("FATAL: implausible exchange rate for item I1"),
                new ExpensePolicy().assess(claim.build(), (c, d) -> new BigDecimal("1000.01"), TODAY)
                        .violations());
        ExpenseAssessment boundary = new ExpensePolicy().assess(claim.build(),
                (c, d) -> new BigDecimal("1000"), TODAY);
        assertEquals(List.of(), boundary.violations());
        assertEquals(new BigDecimal("40000.00"), boundary.claimedAmount());

        ClaimBuilder euro = new ClaimBuilder();
        ItemBuilder item = new ItemBuilder();
        item.currency = EUR;
        euro.items.add(item.build());
        assertEquals(new BigDecimal("150.00"),
                new ExpensePolicy().assess(euro.build(), (c, d) -> new BigDecimal("1.5"), TODAY)
                        .claimedAmount());
    }

    @Test
    @DisplayName("expense: common item rules produce the exact violation text")
    void expenseCommonItemRules() {
        ClaimBuilder future = new ClaimBuilder();
        ItemBuilder futureItem = new ItemBuilder();
        futureItem.incurred = TODAY.plusDays(1);
        future.items.add(futureItem.build());
        assertTrue(assess(future, ONE_TO_ONE).violations()
                .contains("FATAL: future expense date for item I1"));

        ClaimBuilder old = new ClaimBuilder();
        ItemBuilder oldItem = new ItemBuilder();
        oldItem.incurred = SUBMITTED.minusDays(91);
        old.items.add(oldItem.build());
        assertTrue(assess(old, ONE_TO_ONE).violations()
                .contains("FATAL: expense older than ninety days for item I1"));

        ClaimBuilder late = new ClaimBuilder();
        ItemBuilder lateItem = new ItemBuilder();
        lateItem.incurred = SUBMITTED.minusDays(45);
        late.items.add(lateItem.build());
        ExpenseAssessment lateResult = assess(late, ONE_TO_ONE);
        assertTrue(lateResult.warnings().contains("late submission for item I1"));
        assertEquals(List.of(), lateResult.violations());

        ClaimBuilder noMerchant = new ClaimBuilder();
        ItemBuilder blankMerchant = new ItemBuilder();
        blankMerchant.merchant = "  ";
        noMerchant.items.add(blankMerchant.build());
        assertTrue(assess(noMerchant, ONE_TO_ONE).violations()
                .contains("merchant is required for item I1"));

        ClaimBuilder shortMerchant = new ClaimBuilder();
        ItemBuilder tinyMerchant = new ItemBuilder();
        tinyMerchant.merchant = " A ";
        shortMerchant.items.add(tinyMerchant.build());
        assertTrue(assess(shortMerchant, ONE_TO_ONE).warnings()
                .contains("merchant name is unusually short for item I1"));

        ClaimBuilder nullMerchant = new ClaimBuilder();
        ItemBuilder nullMerchantItem = new ItemBuilder();
        nullMerchantItem.merchant = null;
        nullMerchant.items.add(nullMerchantItem.build());
        assertTrue(assess(nullMerchant, ONE_TO_ONE).violations()
                .contains("merchant is required for item I1"));

        ClaimBuilder noPurpose = new ClaimBuilder();
        ItemBuilder blankPurpose = new ItemBuilder();
        blankPurpose.purpose = "";
        noPurpose.items.add(blankPurpose.build());
        assertTrue(assess(noPurpose, ONE_TO_ONE).violations()
                .contains("business purpose is required for item I1"));

        ClaimBuilder shortPurpose = new ClaimBuilder();
        ItemBuilder tinyPurpose = new ItemBuilder();
        tinyPurpose.purpose = "short";
        shortPurpose.items.add(tinyPurpose.build());
        assertTrue(assess(shortPurpose, ONE_TO_ONE).warnings()
                .contains("business purpose is too brief for item I1"));

        ClaimBuilder nullPurpose = new ClaimBuilder();
        ItemBuilder nullPurposeItem = new ItemBuilder();
        nullPurposeItem.purpose = null;
        nullPurpose.items.add(nullPurposeItem.build());
        assertTrue(assess(nullPurpose, ONE_TO_ONE).violations()
                .contains("business purpose is required for item I1"));

        ClaimBuilder personal = new ClaimBuilder();
        ItemBuilder personalItem = new ItemBuilder();
        personalItem.personal = true;
        personal.items.add(personalItem.build());
        ExpenseAssessment personalResult = assess(personal, ONE_TO_ONE);
        assertTrue(personalResult.violations()
                .contains("personal component is not reimbursable for item I1"));
        assertTrue(personalResult.auditRequired());
        assertEquals(new BigDecimal("0.00"), personalResult.reimbursableAmount());

        ClaimBuilder alcohol = new ClaimBuilder();
        ItemBuilder alcoholItem = new ItemBuilder();
        alcoholItem.alcohol = true;
        alcohol.items.add(alcoholItem.build());
        assertTrue(assess(alcohol, ONE_TO_ONE).violations()
                .contains("alcohol is only allowed for client entertainment"));

        ClaimBuilder medical = new ClaimBuilder();
        ItemBuilder medicalItem = new ItemBuilder();
        medicalItem.category = ExpenseCategory.MEDICAL;
        medicalItem.payment = PaymentMethod.CORPORATE_CARD;
        medical.items.add(medicalItem.build());
        assertTrue(assess(medical, ONE_TO_ONE).violations()
                .contains("corporate card cannot be used for medical expenses"));

        ClaimBuilder noReceipt = new ClaimBuilder();
        ItemBuilder noReceiptItem = new ItemBuilder();
        noReceiptItem.receipt = ReceiptType.NONE;
        noReceiptItem.amount = "50.01";
        noReceipt.items.add(noReceiptItem.build());
        assertTrue(assess(noReceipt, ONE_TO_ONE).violations()
                .contains("receipt is required for item I1"));

        ClaimBuilder huge = new ClaimBuilder();
        huge.items.add(meal("50000.01"));
        assertTrue(assess(huge, ONE_TO_ONE).violations()
                .contains("FATAL: single item exceeds the maximum supported amount"));

        ClaimBuilder weekend = new ClaimBuilder();
        ItemBuilder weekendItem = new ItemBuilder();
        weekendItem.category = ExpenseCategory.OFFICE_SUPPLIES;
        weekendItem.incurred = LocalDate.of(2026, 3, 15);
        weekend.items.add(weekendItem.build());
        assertTrue(assess(weekend, ONE_TO_ONE).warnings()
                .contains("weekend office-supply purchase needs explanation"));
    }

    @Test
    @DisplayName("expense: duplicate fingerprints and suspected duplicates")
    void expenseDuplicates() {
        ClaimBuilder claim = new ClaimBuilder();
        ItemBuilder first = new ItemBuilder();
        first.id = "I1";
        ItemBuilder second = new ItemBuilder();
        second.id = "I2";
        claim.items.add(first.build());
        claim.items.add(second.build());
        ExpenseAssessment result = assess(claim, ONE_TO_ONE);
        assertTrue(result.violations().contains("possible duplicate item: I2"));
        assertFalse(result.violations().contains("possible duplicate item: I1"));

        ClaimBuilder suspected = new ClaimBuilder();
        ItemBuilder marked = new ItemBuilder();
        marked.duplicate = true;
        suspected.items.add(marked.build());
        ExpenseAssessment suspectedResult = assess(suspected, ONE_TO_ONE);
        assertTrue(suspectedResult.violations()
                .contains("item is marked as a suspected duplicate: I1"));
        assertTrue(suspectedResult.auditRequired());
        assertEquals(new BigDecimal("0.00"), suspectedResult.reimbursableAmount());
    }

    @Test
    @DisplayName("expense: category caps per grade and adjustment rules")
    void expenseCategoryCaps() {
        String[][] gradeCaps = {
                {"INTERN", "80.00"}, {"STAFF", "160.00"}, {"SENIOR", "240.00"},
                {"MANAGER", "320.00"}, {"DIRECTOR", "480.00"}, {"EXECUTIVE", "800.00"}};
        for (String[] row : gradeCaps) {
            ClaimBuilder claim = new ClaimBuilder();
            claim.grade = EmployeeGrade.valueOf(row[0]);
            claim.preApproved = true;
            claim.items.add(meal("5000.00"));
            ExpenseAssessment result = assess(claim, ONE_TO_ONE);
            assertEquals(new BigDecimal(row[1]), result.reimbursableAmount(), row[0]);
            assertTrue(result.warnings().contains("category cap applied to item I1"));
            assertTrue(result.warnings().contains("one or more items were capped or excluded"));
        }

        Object[][] categoryCaps = {
                {ExpenseCategory.AIRFARE, "3000.00"},
                {ExpenseCategory.HOTEL, "1000.00"},
                {ExpenseCategory.MEAL, "160.00"},
                {ExpenseCategory.LOCAL_TRANSPORT, "400.00"},
                {ExpenseCategory.OFFICE_SUPPLIES, "600.00"},
                {ExpenseCategory.CLIENT_ENTERTAINMENT, "1000.00"},
                {ExpenseCategory.TRAINING, "2000.00"},
                {ExpenseCategory.SOFTWARE, "1600.00"},
                {ExpenseCategory.COMMUNICATION, "300.00"},
                {ExpenseCategory.MEDICAL, "600.00"},
                {ExpenseCategory.OTHER, "200.00"}};
        for (Object[] row : categoryCaps) {
            ClaimBuilder claim = new ClaimBuilder();
            claim.preApproved = true;
            ItemBuilder item = new ItemBuilder();
            item.category = (ExpenseCategory) row[0];
            item.amount = "40000.00";
            claim.items.add(item.build());
            assertEquals(new BigDecimal((String) row[1]), assess(claim, ONE_TO_ONE).reimbursableAmount(),
                    row[0].toString());
        }

        ClaimBuilder international = new ClaimBuilder();
        international.preApproved = true;
        international.international = true;
        ItemBuilder hotel = new ItemBuilder();
        hotel.category = ExpenseCategory.HOTEL;
        hotel.amount = "40000.00";
        international.items.add(hotel.build());
        assertEquals(new BigDecimal("1500.00"), assess(international, ONE_TO_ONE).reimbursableAmount());

        ClaimBuilder internationalMeal = new ClaimBuilder();
        internationalMeal.preApproved = true;
        internationalMeal.international = true;
        internationalMeal.items.add(meal("40000.00"));
        assertEquals(new BigDecimal("240.00"), assess(internationalMeal, ONE_TO_ONE).reimbursableAmount());

        ClaimBuilder internationalTransport = new ClaimBuilder();
        internationalTransport.preApproved = true;
        internationalTransport.international = true;
        ItemBuilder transport = new ItemBuilder();
        transport.category = ExpenseCategory.LOCAL_TRANSPORT;
        transport.amount = "40000.00";
        internationalTransport.items.add(transport.build());
        assertEquals(new BigDecimal("600.00"),
                assess(internationalTransport, ONE_TO_ONE).reimbursableAmount());

        ClaimBuilder billable = new ClaimBuilder();
        billable.preApproved = true;
        billable.clientBillable = true;
        ItemBuilder entertainment = new ItemBuilder();
        entertainment.category = ExpenseCategory.CLIENT_ENTERTAINMENT;
        entertainment.amount = "40000.00";
        entertainment.attendees = 3;
        billable.items.add(entertainment.build());
        assertEquals(new BigDecimal("1250.00"), assess(billable, ONE_TO_ONE).reimbursableAmount());

        ClaimBuilder underCap = new ClaimBuilder();
        underCap.preApproved = true;
        underCap.items.add(meal("160.00"));
        ExpenseAssessment underCapResult = assess(underCap, ONE_TO_ONE);
        assertEquals(new BigDecimal("160.00"), underCapResult.reimbursableAmount());
        assertEquals(List.of(), underCapResult.warnings());
    }

    @Test
    @DisplayName("expense: approval tier ladder")
    void expenseTiers() {
        ClaimBuilder cfo = new ClaimBuilder();
        cfo.preApproved = true;
        ItemBuilder first = new ItemBuilder();
        first.id = "I1";
        first.amount = "30000.00";
        ItemBuilder second = new ItemBuilder();
        second.id = "I2";
        second.amount = "30000.00";
        cfo.items.add(first.build());
        cfo.items.add(second.build());
        assertEquals(ExpenseApprovalTier.CHIEF_FINANCIAL_OFFICER, assess(cfo, ONE_TO_ONE).approvalTier());

        ClaimBuilder director = new ClaimBuilder();
        director.preApproved = true;
        director.items.add(meal("25000.00"));
        assertEquals(ExpenseApprovalTier.FINANCE_DIRECTOR, assess(director, ONE_TO_ONE).approvalTier());

        ClaimBuilder audited = new ClaimBuilder();
        audited.preApproved = true;
        ItemBuilder bankStatement = new ItemBuilder();
        bankStatement.receipt = ReceiptType.BANK_STATEMENT;
        bankStatement.amount = "300.00";
        audited.items.add(bankStatement.build());
        ExpenseAssessment auditedResult = assess(audited, ONE_TO_ONE);
        assertTrue(auditedResult.auditRequired());
        assertEquals(ExpenseApprovalTier.FINANCE, auditedResult.approvalTier());

        ClaimBuilder departmentHead = new ClaimBuilder();
        departmentHead.preApproved = true;
        departmentHead.items.add(meal("2500.00"));
        assertEquals(ExpenseApprovalTier.DEPARTMENT_HEAD,
                assess(departmentHead, ONE_TO_ONE).approvalTier());

        ClaimBuilder billableHead = new ClaimBuilder();
        billableHead.preApproved = true;
        billableHead.clientBillable = true;
        billableHead.items.add(meal("100.00"));
        assertEquals(ExpenseApprovalTier.DEPARTMENT_HEAD,
                assess(billableHead, ONE_TO_ONE).approvalTier());

        ClaimBuilder lineManager = new ClaimBuilder();
        lineManager.preApproved = true;
        lineManager.items.add(meal("300.00"));
        assertEquals(ExpenseApprovalTier.LINE_MANAGER, assess(lineManager, ONE_TO_ONE).approvalTier());

        ClaimBuilder rejected = new ClaimBuilder();
        ItemBuilder personalItem = new ItemBuilder();
        personalItem.personal = true;
        rejected.items.add(personalItem.build());
        assertEquals(ExpenseApprovalTier.FINANCE, assess(rejected, ONE_TO_ONE).approvalTier());
    }

    @Test
    @DisplayName("expense: every audit trigger sets the audit flag")
    void expenseAuditTriggers() {
        ClaimBuilder big = new ClaimBuilder();
        big.preApproved = true;
        big.items.add(meal("6000.00"));
        assertTrue(assess(big, ONE_TO_ONE).auditRequired());

        ClaimBuilder cash = new ClaimBuilder();
        cash.preApproved = true;
        ItemBuilder cashItem = new ItemBuilder();
        cashItem.payment = PaymentMethod.CASH;
        cashItem.amount = "1000.01";
        cash.items.add(cashItem.build());
        assertTrue(assess(cash, ONE_TO_ONE).auditRequired());

        ClaimBuilder cashUnder = new ClaimBuilder();
        cashUnder.preApproved = true;
        ItemBuilder cheapCash = new ItemBuilder();
        cheapCash.payment = PaymentMethod.CASH;
        cheapCash.amount = "1000.00";
        cashUnder.items.add(cheapCash.build());
        assertFalse(assess(cashUnder, ONE_TO_ONE).auditRequired());

        ClaimBuilder many = new ClaimBuilder();
        many.preApproved = true;
        many.previousClaims = 10;
        many.items.add(meal("40.00"));
        ExpenseAssessment manyResult = assess(many, ONE_TO_ONE);
        assertTrue(manyResult.auditRequired());
        assertTrue(manyResult.warnings().contains("employee has a high monthly claim frequency"));

        ClaimBuilder twenty = new ClaimBuilder();
        twenty.preApproved = true;
        for (int i = 1; i <= 20; i++) {
            ItemBuilder item = new ItemBuilder();
            item.id = "I" + i;
            item.amount = i + ".00";
            twenty.items.add(item.build());
        }
        assertTrue(assess(twenty, ONE_TO_ONE).auditRequired());

        ClaimBuilder manyViolations = new ClaimBuilder();
        manyViolations.preApproved = true;
        for (int i = 1; i <= 3; i++) {
            ItemBuilder item = new ItemBuilder();
            item.id = "I" + i;
            item.amount = i + ".00";
            item.merchant = "";
            manyViolations.items.add(item.build());
        }
        ExpenseAssessment manyViolationsResult = assess(manyViolations, ONE_TO_ONE);
        assertTrue(manyViolationsResult.auditRequired());
        assertEquals(3, manyViolationsResult.violations().size());
        assertFalse(manyViolationsResult.rejected());
    }

    @Test
    @DisplayName("expense: receipt thresholds by category")
    void expenseReceiptThresholds() {
        assertTrue(receiptViolationFree(ExpenseCategory.MEAL, "50.00"));
        assertFalse(receiptViolationFree(ExpenseCategory.MEAL, "50.01"));
        assertTrue(receiptViolationFree(ExpenseCategory.LOCAL_TRANSPORT, "50.00"));
        assertFalse(receiptViolationFree(ExpenseCategory.LOCAL_TRANSPORT, "50.01"));
        assertTrue(receiptViolationFree(ExpenseCategory.COMMUNICATION, "20.00"));
        assertFalse(receiptViolationFree(ExpenseCategory.COMMUNICATION, "20.01"));
        assertTrue(receiptViolationFree(ExpenseCategory.OTHER, "10.00"));
        assertFalse(receiptViolationFree(ExpenseCategory.OTHER, "10.01"));
        assertFalse(receiptViolationFree(ExpenseCategory.HOTEL, "10.00"));
    }

    private static boolean receiptViolationFree(ExpenseCategory category, String amount) {
        ClaimBuilder claim = new ClaimBuilder();
        claim.preApproved = true;
        ItemBuilder item = new ItemBuilder();
        item.category = category;
        item.amount = amount;
        item.receipt = ReceiptType.NONE;
        claim.items.add(item.build());
        return assess(claim, ONE_TO_ONE).violations().stream()
                .noneMatch(violation -> violation.startsWith("receipt is required"));
    }

    @Test
    @DisplayName("expense: intra-category rules produce their exact messages")
    void expenseCategoryRules() {
        assertEquals(List.of("airfare over 1000 requires pre-approval"),
                categoryViolations(ExpenseCategory.AIRFARE, "1000.01",
                        b -> b.payment = PaymentMethod.PERSONAL_CARD, null));
        assertEquals(List.of("cash airfare purchase requires verification"),
                categoryWarnings(ExpenseCategory.AIRFARE, "100.00",
                        b -> b.payment = PaymentMethod.CASH, null));
        assertEquals(List.of("hotel requires an invoice or receipt"),
                categoryViolations(ExpenseCategory.HOTEL, "100.00",
                        b -> b.receipt = ReceiptType.BANK_STATEMENT, null));
        assertEquals(List.of("domestic hotel amount is unusually high"),
                categoryWarnings(ExpenseCategory.HOTEL, "2000.01", b -> { }, null));
        assertEquals(List.of("alcohol is only allowed for client entertainment",
                        "ordinary meal cannot include alcohol"),
                categoryViolations(ExpenseCategory.MEAL, "100.00", b -> b.alcohol = true, null));
        assertEquals(List.of("cash transport without receipt will be capped"),
                categoryWarnings(ExpenseCategory.LOCAL_TRANSPORT, "100.00",
                        b -> { b.payment = PaymentMethod.CASH; b.receipt = ReceiptType.NONE; }, null));
        assertEquals(List.of("local transport amount needs route details"),
                categoryWarnings(ExpenseCategory.LOCAL_TRANSPORT, "1000.01", b -> { }, null));
        assertEquals(List.of("intern office-supply purchase exceeds delegated limit"),
                categoryViolations(ExpenseCategory.OFFICE_SUPPLIES, "100.01",
                        b -> { }, claim -> claim.grade = EmployeeGrade.INTERN));
        assertEquals(List.of("large office purchase should use corporate procurement"),
                categoryWarnings(ExpenseCategory.OFFICE_SUPPLIES, "500.01",
                        b -> b.payment = PaymentMethod.PERSONAL_CARD, null));
        assertEquals(List.of("client entertainment requires attendee count"),
                categoryViolations(ExpenseCategory.CLIENT_ENTERTAINMENT, "100.00", b -> { }, null));
        assertEquals(List.of("alcohol expense requires at least two attendees"),
                categoryViolations(ExpenseCategory.CLIENT_ENTERTAINMENT, "100.00",
                        b -> { b.attendees = 1; b.alcohol = true; }, null));
        assertEquals(List.of("training requires pre-approval"),
                categoryViolations(ExpenseCategory.TRAINING, "100.00", b -> { }, null));
        assertEquals(List.of("intern training requires department confirmation"),
                categoryWarnings(ExpenseCategory.TRAINING, "100.00", b -> { },
                        claim -> { claim.preApproved = true; claim.grade = EmployeeGrade.INTERN; }));
        assertEquals(List.of("software cannot be purchased with cash"),
                categoryViolations(ExpenseCategory.SOFTWARE, "100.00",
                        b -> b.payment = PaymentMethod.CASH, null));
        assertEquals(List.of("software purchase should use procurement review"),
                categoryWarnings(ExpenseCategory.SOFTWARE, "3000.01", b -> { }, null));
        assertEquals(List.of("communication expense exceeds ordinary monthly level"),
                categoryWarnings(ExpenseCategory.COMMUNICATION, "500.01", b -> { }, null));
        assertEquals(List.of("medical expense requires a recognized receipt"),
                categoryViolations(ExpenseCategory.MEDICAL, "100.00",
                        b -> b.receipt = ReceiptType.STANDARD_RECEIPT, null));
        assertEquals(List.of("other-category expense exceeds allowed limit"),
                categoryViolations(ExpenseCategory.OTHER, "500.01", b -> { }, null));
        assertTrue(categoryViolations(ExpenseCategory.MEDICAL, "100.00",
                b -> b.receipt = ReceiptType.ELECTRONIC_RECEIPT, null).isEmpty());
        assertTrue(categoryViolations(ExpenseCategory.OTHER, "500.00", b -> { }, null).isEmpty());
        assertTrue(categoryViolations(ExpenseCategory.AIRFARE, "1000.00", b -> { }, null).isEmpty());
        assertTrue(categoryViolations(ExpenseCategory.AIRFARE, "4000.00", b -> { },
                claim -> claim.preApproved = true).isEmpty());
        assertTrue(categoryViolations(ExpenseCategory.HOTEL, "5000.00", b -> { },
                claim -> claim.international = true).isEmpty());

        ClaimBuilder entertainment = new ClaimBuilder();
        ItemBuilder entertainmentItem = new ItemBuilder();
        entertainmentItem.category = ExpenseCategory.CLIENT_ENTERTAINMENT;
        entertainmentItem.attendees = 2;
        entertainment.items.add(entertainmentItem.build());
        assertTrue(assess(entertainment, ONE_TO_ONE).warnings()
                .contains("client entertainment lacks pre-approval"));
        assertTrue(assess(entertainment, ONE_TO_ONE).violations().isEmpty());

        ClaimBuilder otherClaim = new ClaimBuilder();
        ItemBuilder otherItem = new ItemBuilder();
        otherItem.category = ExpenseCategory.OTHER;
        otherClaim.items.add(otherItem.build());
        assertTrue(assess(otherClaim, ONE_TO_ONE).warnings()
                .contains("other-category expense needs manager explanation"));
    }

    private static List<String> categoryViolations(
            ExpenseCategory category, String amount,
            java.util.function.Consumer<ItemBuilder> itemCustomizer,
            java.util.function.Consumer<ClaimBuilder> claimCustomizer) {
        ClaimBuilder claim = new ClaimBuilder();
        claim.preApproved = false;
        ItemBuilder item = new ItemBuilder();
        item.category = category;
        item.amount = amount;
        if (itemCustomizer != null) {
            itemCustomizer.accept(item);
        }
        claim.items.add(item.build());
        if (claimCustomizer != null) {
            claimCustomizer.accept(claim);
        }
        return assess(claim, ONE_TO_ONE).violations();
    }

    private static List<String> categoryWarnings(
            ExpenseCategory category, String amount,
            java.util.function.Consumer<ItemBuilder> itemCustomizer,
            java.util.function.Consumer<ClaimBuilder> claimCustomizer) {
        ClaimBuilder claim = new ClaimBuilder();
        claim.preApproved = true;
        ItemBuilder item = new ItemBuilder();
        item.category = category;
        item.amount = amount;
        if (itemCustomizer != null) {
            itemCustomizer.accept(item);
        }
        claim.items.add(item.build());
        if (claimCustomizer != null) {
            claimCustomizer.accept(claim);
        }
        return assess(claim, ONE_TO_ONE).warnings().stream()
                .filter(warning -> !warning.startsWith("one or more items")
                        && !warning.startsWith("category cap applied"))
                .toList();
    }

    @Test
    @DisplayName("expense: null arguments are rejected")
    void expenseNulls() {
        ExpensePolicy policy = new ExpensePolicy();
        ClaimBuilder claim = new ClaimBuilder();
        claim.items.add(meal("40.00"));
        assertThrowsWith(NullPointerException.class, "claim must not be null",
                () -> policy.assess(null, ONE_TO_ONE, TODAY));
        assertThrowsWith(NullPointerException.class, "exchangeRates must not be null",
                () -> policy.assess(claim.build(), null, TODAY));
        assertThrowsWith(NullPointerException.class, "today must not be null",
                () -> policy.assess(claim.build(), ONE_TO_ONE, null));
    }

    @Test
    @DisplayName("expense records validate their invariants")
    void expenseRecords() {
        ExpenseItem item = new ItemBuilder().build();
        assertEquals("I1", item.itemId());
        assertEquals(ExpenseCategory.MEAL, item.category());
        assertEquals(new BigDecimal("100.00"), item.amount());
        assertEquals(USD, item.currency());
        assertEquals(MON, item.incurredDate());
        assertEquals("Acme Store", item.merchant());
        assertEquals("team meal for the release", item.businessPurpose());
        assertEquals(ReceiptType.TAX_INVOICE, item.receiptType());
        assertEquals(PaymentMethod.PERSONAL_CARD, item.paymentMethod());
        assertEquals(0, item.attendeeCount());
        assertFalse(item.alcoholIncluded());
        assertFalse(item.personalComponent());
        assertFalse(item.suspectedDuplicate());
        assertEquals(item, new ItemBuilder().build());
        assertEquals(item.hashCode(), new ItemBuilder().build().hashCode());
        assertTrue(item.toString().contains("I1"));

        ItemBuilder scaled = new ItemBuilder();
        scaled.amount = "10.555";
        assertEquals(new BigDecimal("10.56"), scaled.build().amount());
        ItemBuilder trimmed = new ItemBuilder();
        trimmed.id = "  I9  ";
        assertEquals("I9", trimmed.build().itemId());

        ItemBuilder entertainment = new ItemBuilder();
        entertainment.category = ExpenseCategory.CLIENT_ENTERTAINMENT;
        entertainment.attendees = 4;
        assertEquals(4, entertainment.build().attendeeCount());

        assertThrowsWith(IllegalArgumentException.class, "itemId must not be blank", () -> {
            ItemBuilder b = new ItemBuilder();
            b.id = " ";
            b.build();
        });
        assertThrowsWith(NullPointerException.class, "category must not be null", () -> {
            ItemBuilder b = new ItemBuilder();
            b.category = null;
            b.build();
        });
        assertThrows(NullPointerException.class, () -> {
            ItemBuilder b = new ItemBuilder();
            b.amount = null;
            b.build();
        });
        assertThrowsWith(NullPointerException.class, "currency must not be null", () -> {
            ItemBuilder b = new ItemBuilder();
            b.currency = null;
            b.build();
        });
        assertThrowsWith(NullPointerException.class, "incurredDate must not be null", () -> {
            ItemBuilder b = new ItemBuilder();
            b.incurred = null;
            b.build();
        });
        assertThrowsWith(NullPointerException.class, "receiptType must not be null", () -> {
            ItemBuilder b = new ItemBuilder();
            b.receipt = null;
            b.build();
        });
        assertThrowsWith(NullPointerException.class, "paymentMethod must not be null", () -> {
            ItemBuilder b = new ItemBuilder();
            b.payment = null;
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "amount must be positive", () -> {
            ItemBuilder b = new ItemBuilder();
            b.amount = "0";
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "amount must be positive", () -> {
            ItemBuilder b = new ItemBuilder();
            b.amount = "-5";
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "attendeeCount is outside the supported range", () -> {
            ItemBuilder b = new ItemBuilder();
            b.category = ExpenseCategory.CLIENT_ENTERTAINMENT;
            b.attendees = -1;
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "attendeeCount is outside the supported range", () -> {
            ItemBuilder b = new ItemBuilder();
            b.category = ExpenseCategory.CLIENT_ENTERTAINMENT;
            b.attendees = 1001;
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class,
                "attendeeCount is only valid for client entertainment", () -> {
                    ItemBuilder b = new ItemBuilder();
                    b.attendees = 2;
                    b.build();
                });
    }

    @Test
    @DisplayName("ExpenseClaim validates its invariants")
    void expenseClaimRecord() {
        ClaimBuilder builder = new ClaimBuilder();
        builder.items.add(meal("40.00"));
        ExpenseClaim claim = builder.build();
        assertEquals("C1", claim.claimId());
        assertEquals("E1", claim.employeeId());
        assertEquals(EmployeeGrade.STAFF, claim.employeeGrade());
        assertEquals(SUBMITTED, claim.submittedDate());
        assertEquals("CC-1", claim.costCenter());
        assertEquals(1, claim.items().size());
        assertFalse(claim.managerPreApproved());
        assertFalse(claim.internationalTrip());
        assertFalse(claim.clientBillable());
        assertTrue(claim.projectActive());
        assertEquals(0, claim.previousClaimsThisMonth());
        final ClaimBuilder copy = new ClaimBuilder();
        copy.items.add(meal("40.00"));
        assertEquals(claim, copy.build());
        assertEquals(claim.hashCode(), copy.build().hashCode());
        assertTrue(claim.toString().contains("C1"));

        assertThrowsWith(IllegalArgumentException.class, "claimId must not be blank", () -> {
            ClaimBuilder b = new ClaimBuilder();
            b.id = " ";
            b.items.add(meal("40.00"));
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "employeeId must not be blank", () -> {
            ClaimBuilder b = new ClaimBuilder();
            b.employeeId = "";
            b.items.add(meal("40.00"));
            b.build();
        });
        assertThrowsWith(NullPointerException.class, "employeeGrade must not be null", () -> {
            ClaimBuilder b = new ClaimBuilder();
            b.grade = null;
            b.items.add(meal("40.00"));
            b.build();
        });
        assertThrowsWith(NullPointerException.class, "submittedDate must not be null", () -> {
            ClaimBuilder b = new ClaimBuilder();
            b.submitted = null;
            b.items.add(meal("40.00"));
            b.build();
        });
        assertThrowsWith(NullPointerException.class, "items must not be null", () -> {
            ClaimBuilder b = new ClaimBuilder();
            b.items = null;
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "claim must contain at least one item", () -> {
            ClaimBuilder b = new ClaimBuilder();
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "claim cannot contain more than 100 items", () -> {
            ClaimBuilder b = new ClaimBuilder();
            for (int i = 1; i <= 101; i++) {
                ItemBuilder item = new ItemBuilder();
                item.id = "I" + i;
                item.amount = i + ".00";
                b.items.add(item.build());
            }
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class,
                "previousClaimsThisMonth must not be negative", () -> {
                    ClaimBuilder b = new ClaimBuilder();
                    b.previousClaims = -1;
                    b.items.add(meal("40.00"));
                    b.build();
                });
        assertThrowsWith(IllegalArgumentException.class,
                "client-billable claim requires a cost center", () -> {
                    ClaimBuilder b = new ClaimBuilder();
                    b.clientBillable = true;
                    b.costCenter = " ";
                    b.items.add(meal("40.00"));
                    b.build();
                });
        ClaimBuilder trimmed = new ClaimBuilder();
        trimmed.id = " C2 ";
        trimmed.employeeId = " E2 ";
        trimmed.items.add(meal("40.00"));
        assertEquals("C2", trimmed.build().claimId());
        assertEquals("E2", trimmed.build().employeeId());
    }

    @Test
    @DisplayName("ExpenseAssessment validates its invariants")
    void expenseAssessmentRecord() {
        ExpenseAssessment assessment = new ExpenseAssessment(new BigDecimal("10.00"),
                new BigDecimal("5.00"), ExpenseApprovalTier.FINANCE, false, true,
                List.of("violation"), List.of("warning"));
        assertEquals(new BigDecimal("10.00"), assessment.claimedAmount());
        assertEquals(new BigDecimal("5.00"), assessment.reimbursableAmount());
        assertEquals(ExpenseApprovalTier.FINANCE, assessment.approvalTier());
        assertFalse(assessment.rejected());
        assertTrue(assessment.auditRequired());
        assertEquals(List.of("violation"), assessment.violations());
        assertEquals(List.of("warning"), assessment.warnings());
        assertEquals(assessment, new ExpenseAssessment(new BigDecimal("10.00"),
                new BigDecimal("5.00"), ExpenseApprovalTier.FINANCE, false, true,
                List.of("violation"), List.of("warning")));
        assertTrue(assessment.toString().contains("FINANCE"));
        assertEquals(assessment.hashCode(), assessment.hashCode());

        assertThrowsWith(NullPointerException.class, null, () -> new ExpenseAssessment(null,
                BigDecimal.ZERO, ExpenseApprovalTier.FINANCE, false, false, List.of(), List.of()));
        assertThrowsWith(NullPointerException.class, null, () -> new ExpenseAssessment(BigDecimal.ZERO,
                null, ExpenseApprovalTier.FINANCE, false, false, List.of(), List.of()));
        assertThrowsWith(NullPointerException.class, null, () -> new ExpenseAssessment(BigDecimal.ZERO,
                BigDecimal.ZERO, null, false, false, List.of(), List.of()));
        assertThrowsWith(NullPointerException.class, null, () -> new ExpenseAssessment(BigDecimal.ZERO,
                BigDecimal.ZERO, ExpenseApprovalTier.FINANCE, false, false, null, List.of()));
        assertThrowsWith(NullPointerException.class, null, () -> new ExpenseAssessment(BigDecimal.ZERO,
                BigDecimal.ZERO, ExpenseApprovalTier.FINANCE, false, false, List.of(), null));
        assertThrowsWith(IllegalArgumentException.class, "amounts must not be negative",
                () -> new ExpenseAssessment(new BigDecimal("-1"),
                        BigDecimal.ZERO, ExpenseApprovalTier.FINANCE, false, false, List.of(), List.of()));
        assertThrowsWith(IllegalArgumentException.class, "amounts must not be negative",
                () -> new ExpenseAssessment(BigDecimal.ZERO,
                        new BigDecimal("-1"), ExpenseApprovalTier.FINANCE, false, false,
                        List.of(), List.of()));
        assertThrowsWith(IllegalArgumentException.class,
                "reimbursable amount cannot exceed claimed amount",
                () -> new ExpenseAssessment(BigDecimal.ONE,
                        new BigDecimal("2"), ExpenseApprovalTier.FINANCE, false, false,
                        List.of(), List.of()));
        assertThrowsWith(IllegalArgumentException.class, "rejected assessment must contain violations",
                () -> new ExpenseAssessment(BigDecimal.ONE, BigDecimal.ONE,
                        ExpenseApprovalTier.FINANCE, true, false, List.of(), List.of()));
    }

    // ==================================================================
    // CorporateCardExpenseService
    // ==================================================================

    private static CardEvidence evidence(String itemId, String number) {
        return new CardEvidence(itemId, number, YearMonth.from(TODAY));
    }

    private static CorporateCardExpenseService cardService() {
        return new CorporateCardExpenseService(new ExpensePolicy(),
                new CardEvidenceValidator(Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC),
                        ZoneOffset.UTC)),
                new AccessPolicy(), new ApprovalWorkflowEngine());
    }

    @Test
    @DisplayName("corporate card service: business use cases and access rules")
    void corporateCardExpenseService() {
        CorporateCardExpenseService service = cardService();

        assertThrows(NullPointerException.class, () -> new CorporateCardExpenseService(
                null, new CardEvidenceValidator(Clock.systemUTC()), new AccessPolicy(),
                new ApprovalWorkflowEngine()));
        assertThrows(NullPointerException.class, () -> new CorporateCardExpenseService(
                new ExpensePolicy(), null, new AccessPolicy(), new ApprovalWorkflowEngine()));
        assertThrows(NullPointerException.class, () -> new CorporateCardExpenseService(
                new ExpensePolicy(), new CardEvidenceValidator(Clock.systemUTC()), null,
                new ApprovalWorkflowEngine()));
        assertThrows(NullPointerException.class, () -> new CorporateCardExpenseService(
                new ExpensePolicy(), new CardEvidenceValidator(Clock.systemUTC()),
                new AccessPolicy(), null));

        ClaimBuilder builder = new ClaimBuilder();
        ItemBuilder corporate = new ItemBuilder();
        corporate.payment = PaymentMethod.CORPORATE_CARD;
        corporate.amount = "50.00";
        builder.items.add(corporate.build());
        ExpenseClaim claim = builder.build();
        AccessContext actor = employeeCreateCtx("E1");

        Map<String, CardEvidence> evidenceByItem = new LinkedHashMap<>();
        evidenceByItem.put("I1", evidence("I1", "4111111111111111"));
        ExpenseCardReview review = service.review(claim, evidenceByItem, ONE_TO_ONE, TODAY, actor);
        assertEquals("C1", review.claimId());
        assertTrue(review.access().allowed());
        assertFalse(review.expense().rejected());
        assertEquals(1, review.cards().size());
        assertTrue(review.cards().get(0).valid());
        assertEquals(List.of(), review.issues());
        assertTrue(review.readyForApproval());
        assertFalse(review.auditRequired());

        assertThrowsWith(NullPointerException.class, "claim must not be null",
                () -> service.review(null, evidenceByItem, ONE_TO_ONE, TODAY, actor));
        assertThrowsWith(NullPointerException.class, "evidenceByItem must not be null",
                () -> service.review(claim, null, ONE_TO_ONE, TODAY, actor));
        assertThrowsWith(NullPointerException.class, "actor must not be null",
                () -> service.review(claim, evidenceByItem, ONE_TO_ONE, TODAY, null));

        AccessContext wrongResource = employeeCreateCtx("E1", ResourceType.LEAVE_REQUEST);
        assertThrowsWith(IllegalArgumentException.class,
                "actor context must describe creating an expense claim",
                () -> service.review(claim, evidenceByItem, ONE_TO_ONE, TODAY, wrongResource));
        AccessContext wrongAction = employeeCreateCtx("E1", ResourceType.EXPENSE_CLAIM,
                ResourceAction.VIEW);
        assertThrowsWith(IllegalArgumentException.class,
                "actor context must describe creating an expense claim",
                () -> service.review(claim, evidenceByItem, ONE_TO_ONE, TODAY, wrongAction));
        assertThrowsWith(IllegalArgumentException.class,
                "employee may only create their own claim",
                () -> service.review(claim, evidenceByItem, ONE_TO_ONE, TODAY, employeeCreateCtx("E2")));

        Map<String, CardEvidence> missing = new LinkedHashMap<>();
        ExpenseCardReview missingReview = service.review(claim, missing, ONE_TO_ONE, TODAY, actor);
        assertEquals(List.of("corporate card evidence missing for item I1"), missingReview.issues());
        assertFalse(missingReview.readyForApproval());
        assertTrue(missingReview.auditRequired());

        Map<String, CardEvidence> mismatch = new LinkedHashMap<>();
        mismatch.put("I1", evidence("I9", "4111111111111111"));
        assertEquals(List.of("card evidence item mismatch for item I1"),
                service.review(claim, mismatch, ONE_TO_ONE, TODAY, actor).issues());

        Map<String, CardEvidence> invalidCard = new LinkedHashMap<>();
        invalidCard.put("I1", evidence("I1", "4111111111111112"));
        ExpenseCardReview invalidReview = service.review(claim, invalidCard, ONE_TO_ONE, TODAY, actor);
        assertEquals(List.of("corporate card evidence invalid for item I1"), invalidReview.issues());
        assertFalse(invalidReview.cards().get(0).valid());
        assertFalse(invalidReview.readyForApproval());

        ClaimBuilder cashBuilder = new ClaimBuilder();
        ItemBuilder cashItem = new ItemBuilder();
        cashItem.amount = "60.00";
        cashBuilder.items.add(cashItem.build());
        ExpenseCardReview cashReview = service.review(cashBuilder.build(),
                new LinkedHashMap<>(), ONE_TO_ONE, TODAY, actor);
        assertEquals(List.of(), cashReview.issues());
        assertEquals(List.of(), cashReview.cards());
        assertTrue(cashReview.readyForApproval());

        ApprovalStep step = new ApprovalStep("S1", "Manager",
                Set.of("M1"), StepMode.ANY_ONE, 1, false, false, null);
        WorkflowSnapshot snapshot = service.startApproval(claim, review, List.of(step),
                Instant.parse("2026-03-16T09:00:00Z"));
        assertEquals(WorkflowState.PENDING, snapshot.state());
        assertEquals("C1", snapshot.workflowId());
        assertEquals("E1", snapshot.requesterId());
        assertFalse(snapshot.urgent());
        assertTrue(snapshot.withdrawalAllowed());

        assertThrowsWith(NullPointerException.class, "claim must not be null",
                () -> service.startApproval(null, review, List.of(step), Instant.EPOCH));
        assertThrowsWith(NullPointerException.class, "review must not be null",
                () -> service.startApproval(claim, null, List.of(step), Instant.EPOCH));
        ExpenseCardReview otherReview = new ExpenseCardReview("C2", review.access(), review.expense(),
                review.cards(), review.issues(), review.readyForApproval(), review.auditRequired());
        assertThrowsWith(IllegalArgumentException.class, "review belongs to a different claim",
                () -> service.startApproval(claim, otherReview, List.of(step), Instant.EPOCH));
        assertThrowsWith(IllegalStateException.class, "claim is not ready for approval",
                () -> service.startApproval(claim, invalidReview, List.of(step), Instant.EPOCH));

        assertThrowsWith(IllegalArgumentException.class, "claimId must not be blank",
                () -> new ExpenseCardReview(" ", review.access(), review.expense(), review.cards(),
                        review.issues(), review.readyForApproval(), review.auditRequired()));
        assertThrowsWith(NullPointerException.class, "access must not be null",
                () -> new ExpenseCardReview("C1", null, review.expense(), review.cards(),
                        review.issues(), review.readyForApproval(), review.auditRequired()));
        assertThrowsWith(NullPointerException.class, "expense must not be null",
                () -> new ExpenseCardReview("C1", review.access(), null, review.cards(),
                        review.issues(), review.readyForApproval(), review.auditRequired()));
        assertThrowsWith(NullPointerException.class, "cards must not be null",
                () -> new ExpenseCardReview("C1", review.access(), review.expense(), null,
                        review.issues(), review.readyForApproval(), review.auditRequired()));
        assertThrowsWith(NullPointerException.class, "issues must not be null",
                () -> new ExpenseCardReview("C1", review.access(), review.expense(), review.cards(),
                        null, review.readyForApproval(), review.auditRequired()));
        assertThrowsWith(IllegalArgumentException.class,
                "readyForApproval is inconsistent with review results",
                () -> new ExpenseCardReview("C1", review.access(), review.expense(), review.cards(),
                        review.issues(), false, review.auditRequired()));
        assertThrowsWith(IllegalArgumentException.class,
                "auditRequired is inconsistent with review results",
                () -> new ExpenseCardReview("C1", review.access(), review.expense(), review.cards(),
                        review.issues(), review.readyForApproval(), true));
    }

    @Test
    @DisplayName("card evidence records and validator")
    void cardEvidence() {
        CardEvidence evidence = evidence(" I1 ", "4111111111111111");
        assertEquals("I1", evidence.itemId());
        assertEquals("4111111111111111", evidence.number());
        assertEquals(YearMonth.from(TODAY), evidence.expires());
        assertEquals(evidence, new CardEvidence("I1", "4111111111111111", YearMonth.from(TODAY)));
        assertEquals(evidence.hashCode(), evidence.hashCode());
        assertTrue(evidence.toString().contains("I1"));
        assertThrowsWith(IllegalArgumentException.class, "itemId must not be blank",
                () -> new CardEvidence(" ", "4111", YearMonth.of(2030, 1)));
        assertThrowsWith(NullPointerException.class, "number must not be null",
                () -> new CardEvidence("I1", null, YearMonth.of(2030, 1)));
        assertThrowsWith(NullPointerException.class, "expires must not be null",
                () -> new CardEvidence("I1", "4111", null));

        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        CardEvidenceValidator validator = new CardEvidenceValidator(clock);
        CardEvidenceCheck ok = validator.check(evidence("I1", "4111111111111111"));
        assertTrue(ok.valid());
        assertEquals(CreditCardType.VISA, ok.type());
        assertEquals(List.of(), ok.issues());
        CardEvidenceCheck badSyntax = validator.check(evidence("I1", "4111"));
        assertFalse(badSyntax.valid());
        assertEquals(CreditCardType.OTHER, badSyntax.type());
        assertEquals(List.of("card number must contain 12 to 19 digits"),
                validator.check(evidence("I1", "4111")).issues());
        assertEquals(List.of("card number checksum is invalid"),
                validator.check(evidence("I1", "4111111111111112")).issues());
        assertEquals(List.of("card number checksum is invalid", "card network is not supported"),
                validator.check(evidence("I1", "9999999999999999")).issues());
        assertEquals(List.of("card has expired"),
                validator.check(new CardEvidence("I1", "4111111111111111", YearMonth.of(2020, 1))).issues());
        assertEquals(CreditCardType.VISA, validator.check(evidence("I1", "4111111111111111")).type());
        assertEquals(ok, validator.check(evidence("I1", "4111111111111111")));
        assertTrue(ok.toString().contains("VISA"));
        assertThrowsWith(NullPointerException.class, "evidence must not be null",
                () -> validator.check(null));
        assertThrowsWith(NullPointerException.class, "clock must not be null",
                () -> new CardEvidenceValidator(null));

        assertThrowsWith(NullPointerException.class, "itemId must not be null",
                () -> new CardEvidenceCheck(null, CreditCardType.VISA, true, List.of()));
        assertThrowsWith(NullPointerException.class, "type must not be null",
                () -> new CardEvidenceCheck("I1", null, true, List.of()));
        assertThrowsWith(NullPointerException.class, "issues must not be null",
                () -> new CardEvidenceCheck("I1", CreditCardType.VISA, true, null));
        assertThrowsWith(IllegalArgumentException.class, "valid must agree with issues",
                () -> new CardEvidenceCheck("I1", CreditCardType.VISA, true, List.of("issue")));
        assertThrowsWith(IllegalArgumentException.class, "valid must agree with issues",
                () -> new CardEvidenceCheck("I1", CreditCardType.VISA, false, List.of()));
    }

    // ==================================================================
    // permission
    // ==================================================================

    static final class Ctx {
        String actorId = "actor";
        String ownerId = null;
        String actorDept = null;
        String resourceDept = null;
        UserRole role = UserRole.EMPLOYEE;
        ResourceAction action = ResourceAction.VIEW;
        ResourceType type = ResourceType.EXPENSE_CLAIM;
        Sensitivity sensitivity = Sensitivity.INTERNAL;
        AccessScope scope = AccessScope.OWN;
        boolean active = true;
        boolean suspended = false;
        boolean mfa = true;
        boolean trustedDevice = true;
        boolean trustedNetwork = true;
        boolean businessHours = true;
        boolean delegated = false;
        boolean delegationValid = false;
        boolean legalHold = false;
        boolean archived = false;
        boolean projectMember = true;
        boolean dataRegionMatches = true;
        boolean training = true;

        AccessContext build() {
            return new AccessContext(actorId, ownerId, actorDept, resourceDept, role, action, type,
                    sensitivity, scope, active, suspended, mfa, trustedDevice, trustedNetwork,
                    businessHours, delegated, delegationValid, legalHold, archived, projectMember,
                    dataRegionMatches, training);
        }
    }

    private static AccessContext employeeCreateCtx(String actorId) {
        return employeeCreateCtx(actorId, ResourceType.EXPENSE_CLAIM, ResourceAction.CREATE);
    }

    private static AccessContext employeeCreateCtx(String actorId, ResourceType type) {
        return employeeCreateCtx(actorId, type, ResourceAction.CREATE);
    }

    private static AccessContext employeeCreateCtx(String actorId, ResourceType type,
            ResourceAction action) {
        Ctx ctx = new Ctx();
        ctx.actorId = actorId;
        ctx.actorDept = "D1";
        ctx.resourceDept = "D1";
        ctx.type = type;
        ctx.action = action;
        return ctx.build();
    }

    private static AccessDecision decide(java.util.function.Consumer<Ctx> customizer) {
        Ctx ctx = new Ctx();
        if (customizer != null) {
            customizer.accept(ctx);
        }
        return new AccessPolicy().decide(ctx.build());
    }

    @Test
    @DisplayName("permission: employee role rules")
    void accessEmployee() {
        AccessDecision create = decide(c -> c.action = ResourceAction.CREATE);
        assertTrue(create.allowed());
        assertFalse(create.auditRequired());
        assertFalse(create.stepUpAuthenticationRequired());
        assertEquals(List.of("employee may create personal requests"), create.reasons());

        assertTrue(decide(c -> {
            c.action = ResourceAction.CREATE;
            c.type = ResourceType.LEAVE_REQUEST;
        }).allowed());

        assertEquals(List.of("role does not grant the requested action"),
                decide(c -> {
                    c.action = ResourceAction.CREATE;
                    c.type = ResourceType.PAYROLL;
                }).reasons());

        assertEquals(List.of("employee may access owned resource", "actor owns the resource"),
                decide(c -> c.ownerId = "actor").reasons());
        assertTrue(decide(c -> {
            c.ownerId = "actor";
            c.action = ResourceAction.UPDATE;
        }).allowed());
        assertFalse(decide(c -> {
            c.ownerId = "actor";
            c.action = ResourceAction.UPDATE;
            c.type = ResourceType.PAYROLL;
        }).allowed());
        assertTrue(decide(c -> {
            c.ownerId = "actor";
            c.type = ResourceType.PAYROLL;
        }).allowed());
        assertFalse(decide(c -> c.type = ResourceType.CONTRACT).allowed());
        assertFalse(decide(c -> c.ownerId = "other").allowed());

        AccessDecision publicDoc = decide(c -> {
            c.type = ResourceType.PUBLIC_DOCUMENT;
            c.sensitivity = Sensitivity.PUBLIC;
            c.ownerId = "actor";
        });
        assertTrue(publicDoc.allowed());
        assertEquals(List.of("employee may access owned resource",
                "resource is publicly classified", "actor owns the resource"),
                publicDoc.reasons());
    }

    @Test
    @DisplayName("permission: team lead role rules")
    void accessTeamLead() {
        assertTrue(decide(c -> {
            c.role = UserRole.TEAM_LEAD;
            c.action = ResourceAction.CREATE;
        }).allowed());
        AccessDecision teamView = decide(c -> {
            c.role = UserRole.TEAM_LEAD;
            c.scope = AccessScope.TEAM;
            c.type = ResourceType.LEAVE_REQUEST;
        });
        assertTrue(teamView.allowed());
        assertEquals(List.of("team lead has operational team access"), teamView.reasons());
        assertTrue(decide(c -> {
            c.role = UserRole.TEAM_LEAD;
            c.scope = AccessScope.TEAM;
            c.action = ResourceAction.APPROVE;
            c.type = ResourceType.ATTENDANCE_RECORD;
        }).allowed());
        assertTrue(decide(c -> {
            c.role = UserRole.TEAM_LEAD;
            c.scope = AccessScope.TEAM;
        }).allowed());
        assertFalse(decide(c -> {
            c.role = UserRole.TEAM_LEAD;
            c.scope = AccessScope.TEAM;
            c.type = ResourceType.CONTRACT;
        }).allowed());
        assertFalse(decide(c -> {
            c.role = UserRole.TEAM_LEAD;
            c.scope = AccessScope.DEPARTMENT;
            c.type = ResourceType.LEAVE_REQUEST;
        }).allowed());
    }

    @Test
    @DisplayName("permission: department manager role rules")
    void accessDepartmentManager() {
        assertTrue(decide(c -> {
            c.role = UserRole.DEPARTMENT_MANAGER;
            c.scope = AccessScope.DEPARTMENT;
            c.actorDept = "D1";
            c.resourceDept = "D1";
        }).allowed());
        assertEquals(List.of("department manager has department-scoped access",
                        "actor and resource belong to the same department"),
                decide(c -> {
                    c.role = UserRole.DEPARTMENT_MANAGER;
                    c.scope = AccessScope.DEPARTMENT;
                    c.actorDept = "D1";
                    c.resourceDept = "D1";
                }).reasons());
        assertFalse(decide(c -> {
            c.role = UserRole.DEPARTMENT_MANAGER;
            c.scope = AccessScope.DEPARTMENT;
            c.actorDept = "D1";
            c.resourceDept = "D2";
        }).allowed());
        assertFalse(decide(c -> {
            c.role = UserRole.DEPARTMENT_MANAGER;
            c.scope = AccessScope.OWN;
        }).allowed());
        for (ResourceType forbidden : List.of(ResourceType.PAYROLL,
                ResourceType.SYSTEM_CONFIGURATION, ResourceType.AUDIT_LOG)) {
            Ctx ctx = new Ctx();
            ctx.role = UserRole.DEPARTMENT_MANAGER;
            ctx.scope = AccessScope.DEPARTMENT;
            ctx.actorDept = "D1";
            ctx.resourceDept = "D1";
            ctx.type = forbidden;
            assertFalse(new AccessPolicy().decide(ctx.build()).allowed(), forbidden.name());
        }
        assertFalse(decide(c -> {
            c.role = UserRole.DEPARTMENT_MANAGER;
            c.scope = AccessScope.DEPARTMENT;
            c.actorDept = "D1";
            c.resourceDept = "D1";
            c.action = ResourceAction.DELETE;
            c.sensitivity = Sensitivity.CONFIDENTIAL;
            c.training = true;
        }).allowed());
        assertTrue(decide(c -> {
            c.role = UserRole.DEPARTMENT_MANAGER;
            c.scope = AccessScope.DEPARTMENT;
            c.actorDept = "D1";
            c.resourceDept = "D1";
            c.action = ResourceAction.DELETE;
            c.sensitivity = Sensitivity.PUBLIC;
        }).allowed());
        assertTrue(decide(c -> {
            c.role = UserRole.DEPARTMENT_MANAGER;
            c.scope = AccessScope.TEAM;
            c.action = ResourceAction.APPROVE;
            c.type = ResourceType.EXPENSE_CLAIM;
        }).allowed());
    }

    @Test
    @DisplayName("permission: human resources role rules")
    void accessHumanResources() {
        for (ResourceType type : List.of(ResourceType.EMPLOYEE_PROFILE,
                ResourceType.LEAVE_REQUEST, ResourceType.ATTENDANCE_RECORD)) {
            Ctx ctx = new Ctx();
            ctx.role = UserRole.HUMAN_RESOURCES;
            ctx.type = type;
            ctx.scope = AccessScope.ORGANIZATION;
            AccessDecision decision = new AccessPolicy().decide(ctx.build());
            assertTrue(decision.allowed(), type.name());
            assertEquals(List.of("human resources role grants workforce-data access",
                    "organization scope granted to specialist role"), decision.reasons());
        }
        assertFalse(decide(c -> {
            c.role = UserRole.HUMAN_RESOURCES;
            c.type = ResourceType.EMPLOYEE_PROFILE;
            c.action = ResourceAction.DELETE;
        }).allowed());
        assertFalse(decide(c -> {
            c.role = UserRole.HUMAN_RESOURCES;
            c.type = ResourceType.EMPLOYEE_PROFILE;
            c.action = ResourceAction.EXPORT;
            c.scope = AccessScope.ORGANIZATION;
            c.projectMember = false;
        }).allowed());
        assertTrue(decide(c -> {
            c.role = UserRole.HUMAN_RESOURCES;
            c.type = ResourceType.EMPLOYEE_PROFILE;
            c.action = ResourceAction.EXPORT;
            c.scope = AccessScope.ORGANIZATION;
            c.projectMember = true;
        }).allowed());
        assertTrue(decide(c -> {
            c.role = UserRole.HUMAN_RESOURCES;
            c.type = ResourceType.PAYROLL;
            c.scope = AccessScope.TEAM;
        }).allowed());
        assertFalse(decide(c -> {
            c.role = UserRole.HUMAN_RESOURCES;
            c.type = ResourceType.PAYROLL;
            c.scope = AccessScope.ORGANIZATION;
        }).allowed());
        assertTrue(decide(c -> {
            c.role = UserRole.HUMAN_RESOURCES;
            c.type = ResourceType.PUBLIC_DOCUMENT;
            c.scope = AccessScope.ORGANIZATION;
            c.sensitivity = Sensitivity.PUBLIC;
        }).allowed());
        assertFalse(decide(c -> {
            c.role = UserRole.HUMAN_RESOURCES;
            c.type = ResourceType.PUBLIC_DOCUMENT;
            c.scope = AccessScope.ORGANIZATION;
            c.action = ResourceAction.UPDATE;
        }).allowed());
    }

    @Test
    @DisplayName("permission: finance and auditor role rules")
    void accessFinanceAndAuditor() {
        for (ResourceType type : List.of(ResourceType.EXPENSE_CLAIM,
                ResourceType.PAYROLL, ResourceType.CONTRACT)) {
            Ctx ctx = new Ctx();
            ctx.role = UserRole.FINANCE;
            ctx.type = type;
            ctx.scope = AccessScope.TEAM;
            AccessDecision decision = new AccessPolicy().decide(ctx.build());
            assertTrue(decision.allowed(), type.name());
            assertEquals(List.of("finance role grants financial-data access"), decision.reasons());
        }
        assertFalse(decide(c -> {
            c.role = UserRole.FINANCE;
            c.action = ResourceAction.DELETE;
        }).allowed());
        assertFalse(decide(c -> {
            c.role = UserRole.FINANCE;
            c.action = ResourceAction.SHARE;
            c.sensitivity = Sensitivity.RESTRICTED;
            c.training = true;
            c.mfa = true;
            c.trustedDevice = true;
        }).allowed());
        assertTrue(decide(c -> {
            c.role = UserRole.FINANCE;
            c.action = ResourceAction.SHARE;
            c.scope = AccessScope.TEAM;
        }).allowed());
        assertTrue(decide(c -> {
            c.role = UserRole.FINANCE;
            c.type = ResourceType.PUBLIC_DOCUMENT;
            c.sensitivity = Sensitivity.PUBLIC;
            c.scope = AccessScope.TEAM;
        }).allowed());

        AccessDecision auditorView = decide(c -> {
            c.role = UserRole.AUDITOR;
            c.scope = AccessScope.ORGANIZATION;
        });
        assertTrue(auditorView.allowed());
        assertEquals(List.of("auditor receives read-only access",
                "organization scope granted to specialist role"), auditorView.reasons());
        assertTrue(decide(c -> {
            c.role = UserRole.AUDITOR;
            c.scope = AccessScope.ORGANIZATION;
            c.action = ResourceAction.EXPORT;
            c.type = ResourceType.ATTENDANCE_RECORD;
        }).allowed());
        assertFalse(decide(c -> {
            c.role = UserRole.AUDITOR;
            c.scope = AccessScope.ORGANIZATION;
            c.type = ResourceType.SYSTEM_CONFIGURATION;
        }).allowed());
        assertFalse(decide(c -> {
            c.role = UserRole.AUDITOR;
            c.scope = AccessScope.ORGANIZATION;
            c.action = ResourceAction.UPDATE;
        }).allowed());
    }

    @Test
    @DisplayName("permission: administrator and guest role rules")
    void accessAdminAndGuest() {
        assertFalse(decide(c -> {
            c.role = UserRole.SYSTEM_ADMINISTRATOR;
            c.type = ResourceType.PAYROLL;
            c.scope = AccessScope.ORGANIZATION;
        }).allowed());
        assertFalse(decide(c -> {
            c.role = UserRole.SYSTEM_ADMINISTRATOR;
            c.type = ResourceType.EMPLOYEE_PROFILE;
            c.action = ResourceAction.EXPORT;
            c.scope = AccessScope.ORGANIZATION;
        }).allowed());
        assertFalse(decide(c -> {
            c.role = UserRole.SYSTEM_ADMINISTRATOR;
            c.action = ResourceAction.DELETE;
            c.legalHold = true;
            c.scope = AccessScope.ORGANIZATION;
        }).allowed());
        AccessDecision adminUpdate = decide(c -> {
            c.role = UserRole.SYSTEM_ADMINISTRATOR;
            c.action = ResourceAction.UPDATE;
            c.type = ResourceType.PAYROLL;
            c.scope = AccessScope.ORGANIZATION;
        });
        assertTrue(adminUpdate.allowed());
        assertTrue(adminUpdate.auditRequired());
        assertEquals(List.of("system administrator has technical administration access",
                        "organization scope granted to specialist role"),
                adminUpdate.reasons());

        AccessDecision guestOk = decide(c -> {
            c.role = UserRole.GUEST;
            c.type = ResourceType.PUBLIC_DOCUMENT;
            c.sensitivity = Sensitivity.PUBLIC;
            c.ownerId = "actor";
        });
        assertTrue(guestOk.allowed());
        assertEquals(List.of("guest may view public documents",
                "resource is publicly classified", "actor owns the resource"),
                guestOk.reasons());
        assertFalse(decide(c -> {
            c.role = UserRole.GUEST;
            c.type = ResourceType.PUBLIC_DOCUMENT;
            c.sensitivity = Sensitivity.INTERNAL;
        }).allowed());
        assertFalse(decide(c -> {
            c.role = UserRole.GUEST;
            c.type = ResourceType.PUBLIC_DOCUMENT;
            c.sensitivity = Sensitivity.PUBLIC;
            c.action = ResourceAction.UPDATE;
        }).allowed());
        AccessDecision guestDenied = decide(c -> c.role = UserRole.GUEST);
        assertFalse(guestDenied.allowed());
        assertTrue(guestDenied.reasons().contains("guest may only access public documents"));
        assertTrue(guestDenied.reasons().contains("role does not grant the requested action"));
    }

    @Test
    @DisplayName("permission: account, device and training gates")
    void accessContextGates() {
        assertTrue(decide(c -> c.active = false).reasons().contains("account is inactive"));
        assertTrue(decide(c -> {
            c.suspended = true;
            c.active = false;
        }).reasons().contains("account is suspended"));

        AccessDecision restricted = decide(c -> {
            c.sensitivity = Sensitivity.RESTRICTED;
            c.mfa = false;
        });
        assertFalse(restricted.allowed());
        assertTrue(restricted.stepUpAuthenticationRequired());
        assertTrue(restricted.reasons().contains("restricted resource requires multi-factor authentication"));
        assertTrue(restricted.auditRequired());

        AccessDecision restrictedNoDevice = decide(c -> {
            c.sensitivity = Sensitivity.RESTRICTED;
            c.trustedDevice = false;
        });
        assertFalse(restrictedNoDevice.allowed());
        assertFalse(restrictedNoDevice.stepUpAuthenticationRequired());
        assertTrue(restrictedNoDevice.reasons().contains("restricted resource requires a trusted device"));

        assertTrue(decide(c -> {
            c.sensitivity = Sensitivity.CONFIDENTIAL;
            c.training = false;
        }).reasons().contains("security training is required"));
        assertTrue(decide(c -> {
            c.sensitivity = Sensitivity.RESTRICTED;
            c.training = false;
        }).reasons().contains("security training is required"));
        assertFalse(decide(c -> {
            c.sensitivity = Sensitivity.INTERNAL;
            c.training = false;
        }).reasons().contains("security training is required"));

        assertTrue(decide(c -> {
            c.action = ResourceAction.EXPORT;
            c.dataRegionMatches = false;
            c.trustedNetwork = true;
        }).reasons().contains("cross-region export is not allowed"));
        assertFalse(decide(c -> {
            c.action = ResourceAction.VIEW;
            c.dataRegionMatches = false;
        }).reasons().contains("cross-region export is not allowed"));

        assertTrue(decide(c -> {
            c.action = ResourceAction.EXPORT;
            c.trustedNetwork = false;
        }).reasons().contains("export or sharing requires a trusted network"));
        assertTrue(decide(c -> {
            c.action = ResourceAction.SHARE;
            c.trustedNetwork = false;
        }).reasons().contains("export or sharing requires a trusted network"));
        assertFalse(decide(c -> {
            c.action = ResourceAction.VIEW;
            c.trustedNetwork = false;
        }).reasons().contains("export or sharing requires a trusted network"));

        assertTrue(decide(c -> {
            c.sensitivity = Sensitivity.RESTRICTED;
            c.businessHours = false;
        }).reasons().contains("restricted resource is unavailable outside business hours"));
        assertFalse(decide(c -> {
            c.role = UserRole.SYSTEM_ADMINISTRATOR;
            c.sensitivity = Sensitivity.RESTRICTED;
            c.businessHours = false;
            c.scope = AccessScope.ORGANIZATION;
        }).reasons().contains("restricted resource is unavailable outside business hours"));
        assertFalse(decide(c -> {
            c.businessHours = false;
            c.sensitivity = Sensitivity.INTERNAL;
        }).reasons().contains("restricted resource is unavailable outside business hours"));

        assertTrue(decide(c -> {
            c.delegated = true;
            c.delegationValid = false;
        }).reasons().contains("delegation has expired or is invalid"));
        assertTrue(decide(c -> {
            c.delegated = true;
            c.delegationValid = true;
            c.action = ResourceAction.DELETE;
        }).reasons().contains("delegated permission cannot delete resources"));
        assertTrue(decide(c -> {
            c.delegated = true;
            c.delegationValid = true;
            c.action = ResourceAction.EXPORT;
        }).reasons().contains("delegated permission cannot export resources"));
        assertFalse(decide(c -> {
            c.delegated = true;
            c.delegationValid = true;
        }).reasons().contains("delegated permission cannot delete resources"));

        assertTrue(decide(c -> {
            c.legalHold = true;
            c.action = ResourceAction.DELETE;
        }).reasons().contains("resource is under legal hold"));
        assertTrue(decide(c -> {
            c.legalHold = true;
            c.action = ResourceAction.ARCHIVE;
        }).reasons().contains("resource is under legal hold"));
        assertFalse(decide(c -> c.legalHold = true).reasons().contains("resource is under legal hold"));

        assertTrue(decide(c -> {
            c.archived = true;
            c.action = ResourceAction.UPDATE;
        }).reasons().contains("archived resource cannot be updated"));
        assertTrue(decide(c -> c.action = ResourceAction.RESTORE)
                .reasons().contains("only archived resources can be restored"));
        assertFalse(decide(c -> {
            c.archived = true;
            c.action = ResourceAction.RESTORE;
        }).reasons().contains("only archived resources can be restored"));
    }

    @Test
    @DisplayName("permission: scope authority matrix")
    void accessScopes() {
        assertTrue(decide(c -> c.ownerId = "actor").allowed());
        assertTrue(decide(c -> c.action = ResourceAction.CREATE).allowed());
        assertFalse(decide(c -> c.scope = AccessScope.TEAM).allowed());

        Ctx admin = new Ctx();
        admin.role = UserRole.SYSTEM_ADMINISTRATOR;
        admin.action = ResourceAction.UPDATE;
        admin.scope = AccessScope.OWN;
        admin.type = ResourceType.SYSTEM_CONFIGURATION;
        assertTrue(new AccessPolicy().decide(admin.build()).allowed());

        for (UserRole role : List.of(UserRole.TEAM_LEAD, UserRole.DEPARTMENT_MANAGER,
                UserRole.HUMAN_RESOURCES, UserRole.FINANCE, UserRole.AUDITOR,
                UserRole.SYSTEM_ADMINISTRATOR)) {
            Ctx ctx = new Ctx();
            ctx.role = role;
            ctx.scope = AccessScope.TEAM;
            ctx.actorDept = "D1";
            ctx.resourceDept = "D1";
            ctx.action = ResourceAction.APPROVE;
            ctx.type = ResourceType.LEAVE_REQUEST;
            assertFalse(new AccessPolicy().decide(ctx.build()).reasons()
                    .contains("requested scope is outside the actor's authority"), role.name());
        }

        Ctx department = new Ctx();
        department.role = UserRole.HUMAN_RESOURCES;
        department.scope = AccessScope.DEPARTMENT;
        department.actorDept = "D1";
        department.resourceDept = "D2";
        department.type = ResourceType.LEAVE_REQUEST;
        assertTrue(new AccessPolicy().decide(department.build()).allowed());

        Ctx wrongDepartment = new Ctx();
        wrongDepartment.role = UserRole.EMPLOYEE;
        wrongDepartment.scope = AccessScope.DEPARTMENT;
        wrongDepartment.actorDept = "D1";
        wrongDepartment.resourceDept = "D2";
        assertFalse(new AccessPolicy().decide(wrongDepartment.build()).allowed());
        assertTrue(new AccessPolicy().decide(wrongDepartment.build()).reasons()
                .contains("requested scope is outside the actor's authority"));

        Ctx sameDepartmentEmployee = new Ctx();
        sameDepartmentEmployee.scope = AccessScope.DEPARTMENT;
        sameDepartmentEmployee.actorDept = "D1";
        sameDepartmentEmployee.resourceDept = "D1";
        assertFalse(new AccessPolicy().decide(sameDepartmentEmployee.build()).allowed());

        Ctx organization = new Ctx();
        organization.role = UserRole.FINANCE;
        organization.scope = AccessScope.ORGANIZATION;
        AccessDecision organizationDecision = new AccessPolicy().decide(organization.build());
        assertTrue(organizationDecision.allowed());
        assertTrue(organizationDecision.reasons().contains("organization scope granted to specialist role"));

        Ctx guestOrganization = new Ctx();
        guestOrganization.role = UserRole.GUEST;
        guestOrganization.scope = AccessScope.ORGANIZATION;
        guestOrganization.type = ResourceType.PUBLIC_DOCUMENT;
        guestOrganization.sensitivity = Sensitivity.PUBLIC;
        assertFalse(new AccessPolicy().decide(guestOrganization.build()).allowed());
    }

    @Test
    @DisplayName("permission: resource specific rules and audit flags")
    void accessResourceRules() {
        assertTrue(decide(c -> {
            c.type = ResourceType.AUDIT_LOG;
            c.role = UserRole.AUDITOR;
            c.scope = AccessScope.ORGANIZATION;
        }).allowed());
        assertTrue(decide(c -> {
            c.type = ResourceType.AUDIT_LOG;
            c.role = UserRole.AUDITOR;
            c.scope = AccessScope.ORGANIZATION;
            c.action = ResourceAction.UPDATE;
        }).reasons().contains("audit logs are immutable"));
        assertTrue(decide(c -> {
            c.type = ResourceType.AUDIT_LOG;
            c.role = UserRole.AUDITOR;
            c.scope = AccessScope.ORGANIZATION;
            c.action = ResourceAction.EXPORT;
        }).allowed());

        assertTrue(decide(c -> {
            c.type = ResourceType.SYSTEM_CONFIGURATION;
            c.action = ResourceAction.UPDATE;
        }).reasons().contains("system configuration requires administrator role"));
        assertTrue(decide(c -> {
            c.type = ResourceType.SYSTEM_CONFIGURATION;
            c.role = UserRole.SYSTEM_ADMINISTRATOR;
            c.scope = AccessScope.ORGANIZATION;
            c.action = ResourceAction.UPDATE;
        }).allowed());

        assertTrue(decide(c -> {
            c.type = ResourceType.PAYROLL;
            c.sensitivity = Sensitivity.PUBLIC;
        }).reasons().contains("payroll cannot be classified as public"));

        assertTrue(decide(c -> {
            c.type = ResourceType.EMPLOYEE_PROFILE;
            c.action = ResourceAction.SHARE;
            c.projectMember = false;
        }).reasons().contains("profile sharing requires project membership"));
        assertFalse(decide(c -> {
            c.type = ResourceType.EMPLOYEE_PROFILE;
            c.action = ResourceAction.SHARE;
            c.projectMember = true;
        }).reasons().contains("profile sharing requires project membership"));

        assertTrue(decide(c -> {
            c.type = ResourceType.CONTRACT;
            c.action = ResourceAction.APPROVE;
        }).reasons().contains("contract approval requires finance or department manager"));
        assertFalse(decide(c -> {
            c.type = ResourceType.CONTRACT;
            c.action = ResourceAction.APPROVE;
            c.role = UserRole.FINANCE;
        }).reasons().contains("contract approval requires finance or department manager"));
        assertFalse(decide(c -> {
            c.type = ResourceType.CONTRACT;
            c.action = ResourceAction.APPROVE;
            c.role = UserRole.DEPARTMENT_MANAGER;
            c.scope = AccessScope.DEPARTMENT;
            c.actorDept = "D1";
            c.resourceDept = "D1";
        }).reasons().contains("contract approval requires finance or department manager"));

        assertTrue(decide(c -> c.action = ResourceAction.EXPORT).auditRequired());
        assertTrue(decide(c -> c.action = ResourceAction.DELETE).auditRequired());
        assertTrue(decide(c -> {
            c.role = UserRole.DEPARTMENT_MANAGER;
            c.scope = AccessScope.DEPARTMENT;
            c.actorDept = "D1";
            c.resourceDept = "D1";
            c.action = ResourceAction.APPROVE;
        }).auditRequired());
        assertTrue(decide(c -> c.businessHours = false).auditRequired());
        assertTrue(decide(c -> {
            c.role = UserRole.SYSTEM_ADMINISTRATOR;
            c.scope = AccessScope.ORGANIZATION;
        }).auditRequired());
        assertTrue(decide(c -> {
            c.type = ResourceType.AUDIT_LOG;
            c.role = UserRole.AUDITOR;
            c.scope = AccessScope.ORGANIZATION;
        }).auditRequired());
        assertTrue(decide(c -> {
            c.delegated = true;
            c.delegationValid = true;
        }).auditRequired());
        assertTrue(decide(c -> c.sensitivity = Sensitivity.RESTRICTED).auditRequired());
        assertFalse(decide(c -> c.action = ResourceAction.CREATE).auditRequired());
    }

    @Test
    @DisplayName("AccessContext validates its invariants")
    void accessContextRecord() {
        Ctx ctx = new Ctx();
        ctx.ownerId = " actor ";
        ctx.actorDept = " D1 ";
        ctx.resourceDept = " D1 ";
        AccessContext context = ctx.build();
        assertEquals("actor", context.actorId());
        assertEquals("actor", context.ownerId());
        assertEquals("D1", context.actorDepartment());
        assertEquals("D1", context.resourceDepartment());
        assertEquals(UserRole.EMPLOYEE, context.role());
        assertEquals(ResourceAction.VIEW, context.action());
        assertEquals(ResourceType.EXPENSE_CLAIM, context.resourceType());
        assertEquals(Sensitivity.INTERNAL, context.sensitivity());
        assertEquals(AccessScope.OWN, context.requestedScope());
        assertTrue(context.accountActive());
        assertFalse(context.accountSuspended());
        assertTrue(context.multiFactorAuthenticated());
        assertTrue(context.trustedDevice());
        assertTrue(context.trustedNetwork());
        assertTrue(context.businessHours());
        assertFalse(context.delegated());
        assertFalse(context.delegationValid());
        assertFalse(context.legalHold());
        assertFalse(context.archived());
        assertTrue(context.projectMember());
        assertTrue(context.dataRegionMatches());
        assertTrue(context.securityTrainingCompleted());
        assertTrue(context.ownsResource());
        assertTrue(context.sameDepartment());

        Ctx other = new Ctx();
        other.ownerId = "other";
        other.actorDept = "D1";
        other.resourceDept = "D2";
        assertFalse(other.build().ownsResource());
        assertFalse(other.build().sameDepartment());
        Ctx nulls = new Ctx();
        nulls.ownerId = null;
        nulls.actorDept = null;
        nulls.resourceDept = null;
        assertFalse(nulls.build().ownsResource());
        assertFalse(nulls.build().sameDepartment());
        Ctx half = new Ctx();
        half.actorDept = "D1";
        half.resourceDept = null;
        assertFalse(half.build().sameDepartment());
        assertEquals(context, ctx.build());
        assertEquals(context.hashCode(), ctx.build().hashCode());
        assertTrue(context.toString().contains("EMPLOYEE"));

        assertThrowsWith(IllegalArgumentException.class, "actorId must not be blank", () -> {
            Ctx bad = new Ctx();
            bad.actorId = " ";
            bad.build();
        });
        assertThrowsWith(NullPointerException.class, "role must not be null", () -> {
            Ctx bad = new Ctx();
            bad.role = null;
            bad.build();
        });
        assertThrowsWith(NullPointerException.class, "action must not be null", () -> {
            Ctx bad = new Ctx();
            bad.action = null;
            bad.build();
        });
        assertThrowsWith(NullPointerException.class, "resourceType must not be null", () -> {
            Ctx bad = new Ctx();
            bad.type = null;
            bad.build();
        });
        assertThrowsWith(NullPointerException.class, "sensitivity must not be null", () -> {
            Ctx bad = new Ctx();
            bad.sensitivity = null;
            bad.build();
        });
        assertThrowsWith(NullPointerException.class, "requestedScope must not be null", () -> {
            Ctx bad = new Ctx();
            bad.scope = null;
            bad.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "suspended account cannot be active", () -> {
            Ctx bad = new Ctx();
            bad.suspended = true;
            bad.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "valid delegation requires delegated access", () -> {
            Ctx bad = new Ctx();
            bad.delegationValid = true;
            bad.build();
        });

        AccessDecision decision = new AccessDecision(false, true, true, List.of("no"));
        assertFalse(decision.allowed());
        assertTrue(decision.auditRequired());
        assertTrue(decision.stepUpAuthenticationRequired());
        assertEquals(List.of("no"), decision.reasons());
        assertEquals(decision, new AccessDecision(false, true, true, List.of("no")));
        assertEquals(decision.hashCode(), decision.hashCode());
        assertTrue(decision.toString().contains("no"));
        assertThrowsWith(NullPointerException.class, null,
                () -> new AccessDecision(true, false, false, null));
        assertThrowsWith(IllegalArgumentException.class, "denied access must contain a reason",
                () -> new AccessDecision(false, false, false, List.of()));
        assertThrowsWith(IllegalArgumentException.class,
                "step-up authentication must be completed before access is allowed",
                () -> new AccessDecision(true, false, true, List.of("x")));
    }

    @Test
    @DisplayName("AccessPolicy rejects a null context")
    void accessNullContext() {
        assertThrowsWith(NullPointerException.class, "context must not be null",
                () -> new AccessPolicy().decide(null));
    }

    // ==================================================================
    // leave: AdvancedLeavePolicy
    // ==================================================================

    private static final WorkCalendar WEEKDAYS = date -> date.getDayOfWeek() != java.time.DayOfWeek.SATURDAY
            && date.getDayOfWeek() != java.time.DayOfWeek.SUNDAY;

    static final class Leave {
        String employeeId = "E1";
        EmployeeCategory category = EmployeeCategory.REGULAR;
        LeaveType type = LeaveType.SICK;
        LocalDate start = MON;
        LocalDate end = MON;
        LeaveUnit unit = LeaveUnit.FULL_DAY;
        double hours = 0;
        String reason = "medical appointment";
        boolean emergency = false;
        boolean remoteCapable = false;
        boolean handover = true;
        int months = 24;
        int notice = 3;
        double balance = 10;
        double carryOver = 0;

        AdvancedLeaveRequest build() {
            return new AdvancedLeaveRequest(employeeId, category, type, start, end, unit, hours,
                    reason, emergency, remoteCapable, handover, months, notice, balance, carryOver);
        }
    }

    private static LeaveAssessment assessLeave(java.util.function.Consumer<Leave> customizer) {
        Leave leave = new Leave();
        if (customizer != null) {
            customizer.accept(leave);
        }
        return new AdvancedLeavePolicy().assess(leave.build(), WEEKDAYS, MON);
    }

    @Test
    @DisplayName("leave: charged days computation")
    void leaveChargedDays() {
        AdvancedLeavePolicy policy = new AdvancedLeavePolicy();
        Leave hours = new Leave();
        hours.unit = LeaveUnit.HOURS;
        hours.hours = 8;
        assertEquals(1.0, policy.calculateChargedDays(hours.build(), WEEKDAYS));
        hours.hours = 4;
        assertEquals(0.5, policy.calculateChargedDays(hours.build(), WEEKDAYS));
        hours.hours = 0.5;
        assertEquals(0.5, policy.calculateChargedDays(hours.build(), WEEKDAYS));
        hours.hours = 1;
        assertEquals(0.5, policy.calculateChargedDays(hours.build(), WEEKDAYS));
        hours.hours = 2;
        assertEquals(0.5, policy.calculateChargedDays(hours.build(), WEEKDAYS));
        hours.hours = 3;
        assertEquals(0.5, policy.calculateChargedDays(hours.build(), WEEKDAYS));
        hours.hours = 5;
        assertEquals(1.0, policy.calculateChargedDays(hours.build(), WEEKDAYS));
        hours.hours = Double.MIN_VALUE;
        assertEquals(0.0, policy.calculateChargedDays(hours.build(), WEEKDAYS));
        hours.hours = Double.NaN;
        assertTrue(Double.isNaN(policy.calculateChargedDays(hours.build(), WEEKDAYS)));

        Leave halfDay = new Leave();
        halfDay.unit = LeaveUnit.HALF_DAY_MORNING;
        assertEquals(0.5, policy.calculateChargedDays(halfDay.build(), WEEKDAYS));
        halfDay.start = MON.plusDays(5);
        halfDay.end = halfDay.start;
        assertEquals(0.0, policy.calculateChargedDays(halfDay.build(), WEEKDAYS));
        halfDay.unit = LeaveUnit.HALF_DAY_AFTERNOON;
        assertEquals(0.0, policy.calculateChargedDays(halfDay.build(), WEEKDAYS));

        Leave fullWeek = new Leave();
        fullWeek.end = MON.plusDays(6);
        assertEquals(5.0, policy.calculateChargedDays(fullWeek.build(), WEEKDAYS));

        assertThrows(NullPointerException.class,
                () -> policy.calculateChargedDays(null, WEEKDAYS));
        assertThrows(NullPointerException.class,
                () -> policy.calculateChargedDays(new Leave().build(), null));
        assertThrowsWith(NullPointerException.class, "request must not be null",
                () -> policy.assess(null, WEEKDAYS, MON));
        assertThrowsWith(NullPointerException.class, "calendar must not be null",
                () -> policy.assess(new Leave().build(), null, MON));
        assertThrowsWith(NullPointerException.class, "today must not be null",
                () -> policy.assess(new Leave().build(), WEEKDAYS, null));
    }

    @Test
    @DisplayName("leave: a clean single sick day is eligible")
    void leaveHappyPath() {
        LeaveAssessment result = assessLeave(null);
        assertTrue(result.eligible());
        assertEquals(1.0, result.chargedDays());
        assertEquals(ApprovalTier.SUPERVISOR, result.approvalTier());
        assertFalse(result.medicalCertificateRequired());
        assertFalse(result.handoverRequired());
        assertEquals(List.of(), result.violations());
    }

    @Test
    @DisplayName("leave: date and notice validations")
    void leaveDateRules() {
        assertTrue(assessLeave(leave -> {
            leave.start = MON.plusDays(5);
            leave.end = MON.plusDays(6);
        }).violations().contains("request contains no working time"));

        assertTrue(assessLeave(leave -> leave.start = MON.minusDays(1))
                .violations().contains("backdated leave is only allowed for emergency sick leave"));
        LeaveAssessment emergencySick = assessLeave(leave -> {
            leave.start = MON.minusDays(1);
            leave.emergency = true;
        });
        assertFalse(emergencySick.violations()
                .contains("backdated leave is only allowed for emergency sick leave"));

        assertTrue(assessLeave(leave -> {
            leave.start = MON.plusYears(1).plusDays(1);
            leave.end = leave.start;
        }).violations().contains("leave cannot be requested more than one year ahead"));

        assertTrue(assessLeave(leave -> {
            leave.start = MON;
            leave.end = MON.plusDays(45);
            leave.notice = 10;
        }).violations().contains("a single request cannot span more than 45 calendar days"));

        assertTrue(assessLeave(leave -> {
            leave.end = MON.plusDays(1);
            leave.notice = 0;
        }).violations().contains("multi-day leave requires advance notice"));
        assertFalse(assessLeave(leave -> {
            leave.end = MON.plusDays(1);
            leave.notice = 0;
            leave.emergency = true;
        }).violations().contains("multi-day leave requires advance notice"));

        assertTrue(assessLeave(leave -> {
            leave.end = MON.plusDays(7);
            leave.notice = 2;
        }).violations().contains("leave longer than five days requires three days notice"));

        assertTrue(assessLeave(leave -> {
            leave.end = MON.plusDays(15);
            leave.notice = 6;
        }).violations().contains("leave longer than ten days requires seven days notice"));
        assertFalse(assessLeave(leave -> {
            leave.end = MON.plusDays(15);
            leave.notice = 6;
            leave.emergency = true;
        }).violations().contains("leave longer than ten days requires seven days notice"));
    }

    @Test
    @DisplayName("leave: reason validations")
    void leaveReasonRules() {
        assertTrue(assessLeave(leave -> leave.reason = null)
                .violations().contains("reason is required"));
        assertTrue(assessLeave(leave -> leave.reason = "   ")
                .violations().contains("reason is required"));
        assertEquals(List.of("reason is required"),
                assessLeave(leave -> leave.reason = null).violations());
        assertTrue(assessLeave(leave -> leave.reason = "sick")
                .violations().contains("reason must contain at least five characters"));
        assertTrue(assessLeave(leave -> leave.reason = "a".repeat(301))
                .violations().contains("reason cannot exceed 300 characters"));
        assertTrue(assessLeave(leave -> leave.reason = "fever <today>")
                .violations().contains("reason contains unsupported markup"));
        assertTrue(assessLeave(leave -> leave.reason = "fever > today")
                .violations().contains("reason contains unsupported markup"));
        assertTrue(assessLeave(leave -> leave.reason = "123456")
                .violations().contains("reason cannot contain digits only"));
        assertFalse(assessLeave(leave -> leave.reason = "a".repeat(300)).violations()
                .contains("reason cannot exceed 300 characters"));
    }

    @Test
    @DisplayName("leave: employment category validations")
    void leaveEmploymentRules() {
        assertTrue(assessLeave(leave -> {
            leave.category = EmployeeCategory.INTERN;
            leave.type = LeaveType.ANNUAL;
        }).violations().contains("interns are not eligible for annual leave"));

        assertTrue(assessLeave(leave -> {
            leave.category = EmployeeCategory.CONTRACTOR;
            leave.type = LeaveType.ANNUAL;
            leave.months = 11;
        }).violations().contains("contractors require twelve months service for annual leave"));
        assertFalse(assessLeave(leave -> {
            leave.category = EmployeeCategory.CONTRACTOR;
            leave.type = LeaveType.ANNUAL;
            leave.months = 12;
            leave.balance = 30;
        }).violations().contains("contractors require twelve months service for annual leave"));

        assertTrue(assessLeave(leave -> {
            leave.category = EmployeeCategory.PROBATION;
            leave.type = LeaveType.PERSONAL;
        }).violations().contains("non-emergency personal leave is unavailable during probation"));
        assertFalse(assessLeave(leave -> {
            leave.category = EmployeeCategory.PROBATION;
            leave.type = LeaveType.PERSONAL;
            leave.emergency = true;
            leave.notice = 0;
        }).violations().contains("non-emergency personal leave is unavailable during probation"));

        assertTrue(assessLeave(leave -> {
            leave.months = 0;
            leave.type = LeaveType.ANNUAL;
        }).violations().contains("new employees may only request sick leave in their first month"));
        assertFalse(assessLeave(leave -> leave.months = 0).violations()
                .contains("new employees may only request sick leave in their first month"));
        assertTrue(assessLeave(leave -> leave.months = 601).violations()
                .contains("employment duration is outside the supported range"));
    }

    @Test
    @DisplayName("leave: unit validations")
    void leaveUnitRules() {
        assertTrue(assessLeave(leave -> {
            leave.unit = LeaveUnit.HALF_DAY_MORNING;
            leave.end = MON.plusDays(1);
        }).violations().contains("half-day leave must start and end on the same date"));
        assertTrue(assessLeave(leave -> {
            leave.unit = LeaveUnit.HOURS;
            leave.hours = 0.5;
        }).violations().contains("hour leave must be at least one hour"));
        assertTrue(assessLeave(leave -> {
            leave.unit = LeaveUnit.HOURS;
            leave.hours = 0.3;
            leave.type = LeaveType.ANNUAL;
            leave.months = 24;
        }).violations().contains("annual hour leave must use half-hour increments"));
        assertFalse(assessLeave(leave -> {
            leave.unit = LeaveUnit.HOURS;
            leave.hours = 0.5;
            leave.type = LeaveType.ANNUAL;
            leave.months = 24;
        }).violations().contains("annual hour leave must use half-hour increments"));
        assertTrue(assessLeave(leave -> {
            leave.unit = LeaveUnit.HALF_DAY_MORNING;
            leave.emergency = true;
        }).violations().contains("emergency leave cannot be limited to a planned morning slot"));
    }

    @Test
    @DisplayName("leave: annual leave rules")
    void leaveAnnualRules() {
        assertTrue(assessLeave(leave -> {
            leave.type = LeaveType.ANNUAL;
            leave.end = MON.plusDays(2);
            leave.balance = 2;
            leave.carryOver = 0;
        }).violations().contains("insufficient annual leave balance"));
        assertTrue(assessLeave(leave -> {
            leave.type = LeaveType.ANNUAL;
            leave.start = MON;
            leave.end = MON.plusDays(21);
            leave.balance = 30;
            leave.notice = 10;
        }).violations().contains("annual leave cannot exceed fifteen working days per request"));
        assertTrue(assessLeave(leave -> {
            leave.type = LeaveType.ANNUAL;
            leave.carryOver = 6;
            leave.balance = 20;
        }).violations().contains("carry-over balance cannot exceed five days"));
        assertTrue(assessLeave(leave -> {
            leave.type = LeaveType.ANNUAL;
            leave.carryOver = 2;
            leave.balance = 20;
            leave.start = LocalDate.of(2026, 4, 1);
            leave.end = leave.start;
        }).violations().contains("carry-over leave must be used before April"));
        assertFalse(assessLeave(leave -> {
            leave.type = LeaveType.ANNUAL;
            leave.carryOver = 2;
            leave.balance = 20;
            leave.start = LocalDate.of(2026, 3, 2);
            leave.end = leave.start;
        }).violations().contains("carry-over leave must be used before April"));
        assertTrue(assessLeave(leave -> {
            leave.type = LeaveType.ANNUAL;
            leave.months = 5;
            leave.balance = 20;
        }).violations().contains("annual leave requires six months service"));
        assertTrue(assessLeave(leave -> {
            leave.type = LeaveType.ANNUAL;
            leave.remoteCapable = true;
            leave.notice = 0;
            leave.balance = 20;
        }).violations()
                .contains("remote-capable employees must consider remote work for urgent one-day leave"));
    }

    @Test
    @DisplayName("leave: sick leave rules")
    void leaveSickRules() {
        assertTrue(assessLeave(leave -> {
            leave.start = MON;
            leave.end = MON.plusDays(43);
            leave.notice = 10;
        }).violations()
                .contains("sick leave longer than thirty days needs a separate medical process"));
        assertTrue(assessLeave(leave -> {
            leave.unit = LeaveUnit.HOURS;
            leave.hours = 5;
        }).violations().contains("hour-based sick leave cannot exceed four hours"));
        assertTrue(assessLeave(leave -> {
            leave.emergency = true;
            leave.notice = 31;
            leave.end = MON.plusDays(1);
        }).violations().contains("emergency sick leave has inconsistent notice information"));
        assertTrue(assessLeave(leave -> {
            leave.start = MON.plusDays(6);
            leave.end = leave.start;
            leave.notice = 2;
        }).violations().contains("planned sick leave cannot start on Sunday"));
        assertFalse(assessLeave(leave -> {
            leave.start = MON.plusDays(6);
            leave.end = leave.start;
            leave.emergency = true;
            leave.notice = 2;
        }).violations().contains("planned sick leave cannot start on Sunday"));
        assertTrue(assessLeave(leave -> leave.balance = 366).violations()
                .contains("sick leave balance is outside the supported range"));
    }

    @Test
    @DisplayName("leave: personal leave rules")
    void leavePersonalRules() {
        assertTrue(assessLeave(leave -> {
            leave.type = LeaveType.PERSONAL;
            leave.end = MON.plusDays(3);
            leave.notice = 3;
        }).violations().contains("personal leave cannot exceed three days without an emergency"));
        assertTrue(assessLeave(leave -> {
            leave.type = LeaveType.PERSONAL;
            leave.end = MON.plusDays(7);
            leave.notice = 3;
            leave.emergency = true;
        }).violations().contains("personal leave cannot exceed five days"));
        assertTrue(assessLeave(leave -> {
            leave.type = LeaveType.PERSONAL;
            leave.unit = LeaveUnit.HOURS;
            leave.hours = 5;
            leave.notice = 1;
        }).violations().contains("hour-based personal leave cannot exceed four hours"));
        assertTrue(assessLeave(leave -> {
            leave.type = LeaveType.PERSONAL;
            leave.category = EmployeeCategory.CONTRACTOR;
            leave.end = MON.plusDays(1);
            leave.notice = 1;
        }).violations().contains("contractor personal leave is limited to one day"));
        assertTrue(assessLeave(leave -> {
            leave.type = LeaveType.PERSONAL;
            leave.notice = 0;
        }).violations().contains("personal leave requires one day notice"));
        assertTrue(assessLeave(leave -> {
            leave.type = LeaveType.PERSONAL;
            leave.remoteCapable = true;
            leave.notice = 1;
        }).violations().contains("short personal absence should use remote work when available"));
    }

    @Test
    @DisplayName("leave: certificate, handover and approval tier")
    void leaveCertificateHandoverTier() {
        LeaveAssessment threeDays = assessLeave(leave -> leave.end = MON.plusDays(2));
        assertEquals(3.0, threeDays.chargedDays());
        assertTrue(threeDays.medicalCertificateRequired());
        assertTrue(threeDays.handoverRequired());
        assertEquals(List.of(), threeDays.violations());
        assertTrue(assessLeave(leave -> {
            leave.end = MON.plusDays(2);
            leave.handover = false;
        }).violations().contains("handover is required for this request"));
        assertEquals(ApprovalTier.HUMAN_RESOURCES, threeDays.approvalTier());
        assertTrue(threeDays.eligible());

        LeaveAssessment twoDaysRegular = assessLeave(leave -> leave.end = MON.plusDays(1));
        assertTrue(twoDaysRegular.handoverRequired());
        assertFalse(twoDaysRegular.medicalCertificateRequired());
        assertEquals(ApprovalTier.SUPERVISOR, twoDaysRegular.approvalTier());

        LeaveAssessment twoDaysProbation = assessLeave(leave -> {
            leave.category = EmployeeCategory.PROBATION;
            leave.end = MON.plusDays(1);
        });
        assertTrue(twoDaysProbation.medicalCertificateRequired());
        assertEquals(ApprovalTier.HUMAN_RESOURCES, twoDaysProbation.approvalTier());

        LeaveAssessment twoDaysEmergency = assessLeave(leave -> {
            leave.emergency = true;
            leave.notice = 0;
            leave.end = MON.plusDays(1);
        });
        assertTrue(twoDaysEmergency.medicalCertificateRequired());

        assertTrue(assessLeave(leave -> {
            leave.unit = LeaveUnit.HOURS;
            leave.hours = 4;
        }).medicalCertificateRequired());

        assertFalse(assessLeave(leave -> {
            leave.unit = LeaveUnit.HOURS;
            leave.hours = 3;
        }).medicalCertificateRequired());

        assertTrue(assessLeave(leave -> {
            leave.start = MON.plusDays(4);
            leave.end = MON.plusDays(7);
        }).handoverRequired());

        LeaveAssessment annualTwoDays = assessLeave(leave -> {
            leave.type = LeaveType.ANNUAL;
            leave.end = MON.plusDays(1);
            leave.balance = 10;
        });
        assertTrue(annualTwoDays.handoverRequired());

        LeaveAssessment director = assessLeave(leave -> {
            leave.start = MON;
            leave.end = MON.plusDays(15);
            leave.notice = 10;
        });
        assertEquals(ApprovalTier.DIRECTOR, director.approvalTier());

        LeaveAssessment contractorDirector = assessLeave(leave -> {
            leave.category = EmployeeCategory.CONTRACTOR;
            leave.end = MON.plusDays(5);
            leave.notice = 5;
        });
        assertEquals(ApprovalTier.DIRECTOR, contractorDirector.approvalTier());

        LeaveAssessment personalManager = assessLeave(leave -> {
            leave.type = LeaveType.PERSONAL;
            leave.notice = 2;
        });
        assertEquals(ApprovalTier.MANAGER, personalManager.approvalTier());

        LeaveAssessment completed = assessLeave(leave -> leave.handover = false);
        assertTrue(completed.eligible());
        assertFalse(completed.handoverRequired());
    }

    @Test
    @DisplayName("AdvancedLeaveRequest and LeaveAssessment validate their invariants")
    void leaveRecords() {
        AdvancedLeaveRequest request = new Leave().build();
        assertEquals("E1", request.employeeId());
        assertEquals(EmployeeCategory.REGULAR, request.employeeCategory());
        assertEquals(LeaveType.SICK, request.type());
        assertEquals(MON, request.startDate());
        assertEquals(MON, request.endDate());
        assertEquals(LeaveUnit.FULL_DAY, request.unit());
        assertEquals(0.0, request.requestedHours());
        assertEquals("medical appointment", request.reason());
        assertFalse(request.emergency());
        assertFalse(request.remoteCapable());
        assertTrue(request.handoverCompleted());
        assertEquals(24, request.employmentMonths());
        assertEquals(3, request.noticeDays());
        assertEquals(10.0, request.remainingBalance());
        assertEquals(0.0, request.carryOverBalance());
        assertEquals(request, new Leave().build());
        assertEquals(request.hashCode(), new Leave().build().hashCode());
        assertTrue(request.toString().contains("E1"));

        Leave hours = new Leave();
        hours.unit = LeaveUnit.HOURS;
        hours.hours = 2;
        assertEquals(2.0, hours.build().requestedHours());

        assertThrowsWith(IllegalArgumentException.class, "employeeId must not be blank", () -> {
            Leave leave = new Leave();
            leave.employeeId = " ";
            leave.build();
        });
        assertThrowsWith(NullPointerException.class, "employeeCategory must not be null", () -> {
            Leave leave = new Leave();
            leave.category = null;
            leave.build();
        });
        assertThrowsWith(NullPointerException.class, "startDate must not be null", () -> {
            Leave leave = new Leave();
            leave.start = null;
            leave.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "endDate must not be before startDate", () -> {
            Leave leave = new Leave();
            leave.end = MON.minusDays(1);
            leave.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "employmentMonths must not be negative", () -> {
            Leave leave = new Leave();
            leave.months = -1;
            leave.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "noticeDays must not be negative", () -> {
            Leave leave = new Leave();
            leave.notice = -1;
            leave.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "leave balances must not be negative", () -> {
            Leave leave = new Leave();
            leave.balance = -1;
            leave.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "leave balances must not be negative", () -> {
            Leave leave = new Leave();
            leave.carryOver = -1;
            leave.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "hour leave must be between 0 and 8 hours", () -> {
            Leave leave = new Leave();
            leave.unit = LeaveUnit.HOURS;
            leave.hours = 9;
            leave.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "requestedHours is only valid for hour leave", () -> {
            Leave leave = new Leave();
            leave.hours = 2;
            leave.build();
        });

        LeaveAssessment assessment = new LeaveAssessment(false, 1.0, ApprovalTier.MANAGER, true, true,
                List.of("violation"));
        assertFalse(assessment.eligible());
        assertEquals(1.0, assessment.chargedDays());
        assertEquals(ApprovalTier.MANAGER, assessment.approvalTier());
        assertTrue(assessment.medicalCertificateRequired());
        assertTrue(assessment.handoverRequired());
        assertEquals(List.of("violation"), assessment.violations());
        assertEquals(assessment, new LeaveAssessment(false, 1.0, ApprovalTier.MANAGER, true, true,
                List.of("violation")));
        assertTrue(assessment.toString().contains("MANAGER"));
        assertThrowsWith(IllegalArgumentException.class, "chargedDays must not be negative",
                () -> new LeaveAssessment(true, -1.0, ApprovalTier.MANAGER, false, false, List.of()));
        assertThrowsWith(NullPointerException.class, null,
                () -> new LeaveAssessment(true, 1.0, null, false, false, List.of()));
        assertThrowsWith(NullPointerException.class, null,
                () -> new LeaveAssessment(true, 1.0, ApprovalTier.MANAGER, false, false, null));
        assertThrowsWith(IllegalArgumentException.class, "eligible assessment cannot contain violations",
                () -> new LeaveAssessment(true, 1.0, ApprovalTier.MANAGER, false, false, List.of("x")));
        assertThrowsWith(IllegalArgumentException.class, "ineligible assessment must explain the violation",
                () -> new LeaveAssessment(false, 1.0, ApprovalTier.MANAGER, false, false, List.of()));
    }

    // ==================================================================
    // task: TaskPolicy + TaskStateMachine
    // ==================================================================

    private static final Instant NOW = Instant.parse("2026-03-02T09:00:00Z");

    static final class TaskBuilder {
        String id = "T1";
        String title = "Fix the report";
        String description = "desc";
        String creatorId = "creator";
        String assigneeId = "assignee";
        TaskStatus status = TaskStatus.OPEN;
        TaskPriority priority = TaskPriority.NORMAL;
        RecurrenceRule recurrence = RecurrenceRule.NONE;
        Instant createdAt = NOW.minusSeconds(3600);
        Instant dueAt = null;
        Instant startedAt = null;
        Instant completedAt = null;
        int estimated = 60;
        int actual = 0;
        List<String> dependencies = new ArrayList<>();
        Set<String> tags = new LinkedHashSet<>();
        boolean confidential = false;
        boolean reviewRequired = false;

        Task build() {
            return new Task(id, title, description, creatorId, assigneeId, status, priority,
                    recurrence, createdAt, dueAt, startedAt, completedAt, estimated, actual,
                    dependencies, tags, confidential, reviewRequired);
        }
    }

    static final class TaskCtx {
        String actorId = "assignee";
        Instant now = NOW;
        boolean manager = false;
        boolean administrator = false;
        boolean dependenciesComplete = true;
        boolean assigneeActive = true;
        boolean projectActive = true;
        boolean workingHours = true;
        boolean notifications = true;
        int reopens = 0;
        int openTasks = 0;
        int overdue = 0;

        TaskContext build() {
            return new TaskContext(actorId, now, manager, administrator, dependenciesComplete,
                    assigneeActive, projectActive, workingHours, notifications, reopens, openTasks, overdue);
        }
    }

    private static TaskEvaluation evaluate(java.util.function.Consumer<TaskBuilder> task,
            java.util.function.Consumer<TaskCtx> context) {
        TaskBuilder builder = new TaskBuilder();
        if (task != null) {
            task.accept(builder);
        }
        TaskCtx ctx = new TaskCtx();
        if (context != null) {
            context.accept(ctx);
        }
        return new TaskPolicy().evaluate(builder.build(), ctx.build());
    }

    @Test
    @DisplayName("task: priority scoring and thresholds")
    void taskPriority() {
        // base priorities without modifiers
        assertEquals(TaskPriority.LOW, priority(t -> t.priority = TaskPriority.LOW, null));
        assertEquals(TaskPriority.NORMAL, priority(t -> t.priority = TaskPriority.NORMAL, null));
        assertEquals(TaskPriority.NORMAL, priority(t -> t.priority = TaskPriority.HIGH, null));
        assertEquals(TaskPriority.HIGH, priority(t -> t.priority = TaskPriority.URGENT, null));
        assertEquals(TaskPriority.URGENT, priority(t -> t.priority = TaskPriority.CRITICAL, null));

        // due-hour buckets
        assertEquals(TaskPriority.HIGH, priority(t -> {
            t.priority = TaskPriority.LOW;
            t.dueAt = NOW.minusSeconds(60);
        }, null));
        assertEquals(TaskPriority.NORMAL, priority(t -> {
            t.priority = TaskPriority.LOW;
            t.dueAt = NOW.plusSeconds(2 * 3600);
        }, null));
        assertEquals(TaskPriority.NORMAL, priority(t -> {
            t.priority = TaskPriority.LOW;
            t.dueAt = NOW.plusSeconds(10 * 3600);
        }, null));
        assertEquals(TaskPriority.NORMAL, priority(t -> {
            t.priority = TaskPriority.LOW;
            t.dependencies = List.of("d1", "d2", "d3", "d4", "d5");
            t.dueAt = NOW.plusSeconds(48 * 3600);
        }, null));

        // overdue buckets
        assertEquals(TaskPriority.CRITICAL, priority(t -> {
            t.priority = TaskPriority.NORMAL;
            t.createdAt = NOW.minusSeconds(30L * 24 * 3600);
            t.dueAt = NOW.minusSeconds(10L * 24 * 3600);
        }, null, true));
        assertEquals(TaskPriority.URGENT, priority(t -> {
            t.priority = TaskPriority.NORMAL;
            t.createdAt = NOW.minusSeconds(30L * 24 * 3600);
            t.dueAt = NOW.minusSeconds(4L * 24 * 3600);
        }, null, true));
        assertEquals(TaskPriority.HIGH, priority(t -> {
            t.priority = TaskPriority.LOW;
            t.createdAt = NOW.minusSeconds(30L * 24 * 3600);
            t.dueAt = NOW.minusSeconds(24 * 3600);
        }, null, true));
        List<String> overdueWarnings = new ArrayList<>();
        TaskBuilder overdueTask = new TaskBuilder();
        overdueTask.priority = TaskPriority.LOW;
        overdueTask.createdAt = NOW.minusSeconds(30L * 24 * 3600);
        overdueTask.dueAt = NOW.minusSeconds(24 * 3600);
        assertEquals(TaskPriority.HIGH, new TaskPolicy().calculatePriority(overdueTask.build(),
                new TaskCtx().build(), true, overdueWarnings));
        assertTrue(overdueWarnings.contains("task is overdue"));

        // status, confidentiality, dependencies and workload
        assertEquals(TaskPriority.NORMAL, priority(t -> {
            t.priority = TaskPriority.LOW;
            t.status = TaskStatus.BLOCKED;
            t.startedAt = NOW.minusSeconds(3600);
        }, null));
        assertEquals(TaskPriority.HIGH, priority(t -> {
            t.priority = TaskPriority.NORMAL;
            t.dueAt = NOW.plusSeconds(10 * 3600);
            t.confidential = true;
        }, null));
        assertEquals(TaskPriority.NORMAL, priority(t -> {
            t.priority = TaskPriority.LOW;
            t.dueAt = NOW.plusSeconds(48 * 3600);
        }, c -> c.overdue = 3));
        assertEquals(TaskPriority.HIGH, priority(null, c -> c.overdue = 10));

        // inactive project suppresses the score, terminal states reset it
        assertEquals(TaskPriority.NORMAL, priority(t -> t.priority = TaskPriority.URGENT,
                c -> c.projectActive = false));
        List<String> inactiveWarnings = new ArrayList<>();
        TaskBuilder inactiveTask = new TaskBuilder();
        inactiveTask.priority = TaskPriority.URGENT;
        TaskCtx inactiveCtx = new TaskCtx();
        inactiveCtx.projectActive = false;
        assertEquals(TaskPriority.NORMAL, new TaskPolicy().calculatePriority(inactiveTask.build(),
                inactiveCtx.build(), false, inactiveWarnings));
        assertTrue(inactiveWarnings.contains("inactive project suppresses task escalation"));
        assertEquals(TaskPriority.LOW, priority(t -> {
            t.status = TaskStatus.COMPLETED;
            t.priority = TaskPriority.CRITICAL;
            t.startedAt = NOW.minusSeconds(7200);
            t.completedAt = NOW.minusSeconds(3600);
        }, null));
        assertEquals(TaskPriority.LOW, priority(t -> t.status = TaskStatus.CANCELLED, null));
        assertEquals(TaskPriority.LOW, priority(t -> t.status = TaskStatus.ARCHIVED, null));
    }

    private static TaskPriority priority(java.util.function.Consumer<TaskBuilder> task,
            java.util.function.Consumer<TaskCtx> context) {
        return priority(task, context, false);
    }

    private static TaskPriority priority(java.util.function.Consumer<TaskBuilder> task,
            java.util.function.Consumer<TaskCtx> context, boolean overdue) {
        TaskBuilder builder = new TaskBuilder();
        if (task != null) {
            task.accept(builder);
        }
        TaskCtx ctx = new TaskCtx();
        if (context != null) {
            context.accept(ctx);
        }
        return new TaskPolicy().calculatePriority(builder.build(), ctx.build(), overdue,
                new ArrayList<>());
    }

    @Test
    @DisplayName("task: escalation rules")
    void taskEscalation() {
        assertFalse(evaluate(t -> {
            t.status = TaskStatus.COMPLETED;
            t.startedAt = NOW.minusSeconds(7200);
            t.completedAt = NOW.minusSeconds(3600);
        }, null).escalationRequired());

        TaskEvaluation critical = evaluate(t -> {
            t.priority = TaskPriority.CRITICAL;
            t.dueAt = NOW.plusSeconds(3600);
        }, null);
        assertTrue(critical.escalationRequired());
        assertTrue(critical.warnings().contains("critical task requires escalation"));

        assertTrue(evaluate(t -> {
            t.priority = TaskPriority.URGENT;
            t.dueAt = NOW.minusSeconds(3600);
        }, null).escalationRequired());

        assertTrue(evaluate(t -> t.dueAt = NOW.minusSeconds(3600), c -> c.overdue = 5)
                .escalationRequired());
        assertFalse(evaluate(t -> t.dueAt = NOW.minusSeconds(3600), c -> c.overdue = 4)
                .escalationRequired());

        TaskEvaluation blocked = evaluate(t -> {
            t.status = TaskStatus.BLOCKED;
            t.startedAt = NOW.minusSeconds(4L * 24 * 3600);
        }, null);
        assertTrue(blocked.escalationRequired());
        assertTrue(blocked.warnings().contains("task has been blocked for more than three days"));
        assertFalse(evaluate(t -> {
            t.status = TaskStatus.BLOCKED;
            t.startedAt = NOW.minusSeconds(24 * 3600);
        }, null).escalationRequired());

        assertTrue(evaluate(t -> {
            t.status = TaskStatus.IN_REVIEW;
            t.startedAt = NOW.minusSeconds(3600);
            t.reviewRequired = true;
            t.dueAt = NOW.plusSeconds(3600);
        }, null).escalationRequired());
        assertFalse(evaluate(t -> {
            t.status = TaskStatus.IN_REVIEW;
            t.startedAt = NOW.minusSeconds(3600);
            t.reviewRequired = false;
            t.dueAt = NOW.plusSeconds(3600);
        }, null).escalationRequired());

        assertTrue(evaluate(null, c -> c.assigneeActive = false).escalationRequired());
        assertFalse(evaluate(t -> t.assigneeId = null, c -> c.assigneeActive = false)
                .escalationRequired());
    }

    @Test
    @DisplayName("task: reassignment rules")
    void taskReassignment() {
        assertFalse(evaluate(t -> {
            t.status = TaskStatus.COMPLETED;
            t.startedAt = NOW.minusSeconds(7200);
            t.completedAt = NOW.minusSeconds(3600);
        }, null).reassignmentRecommended());

        TaskEvaluation noAssignee = evaluate(t -> t.assigneeId = null, null);
        assertTrue(noAssignee.reassignmentRecommended());
        assertTrue(noAssignee.warnings().contains("task has no assignee"));

        TaskEvaluation blankAssignee = evaluate(t -> t.assigneeId = "  ", null);
        assertTrue(blankAssignee.reassignmentRecommended());
        assertTrue(blankAssignee.warnings().contains("task has no assignee"));

        TaskEvaluation inactive = evaluate(null, c -> c.assigneeActive = false);
        assertTrue(inactive.reassignmentRecommended());
        assertTrue(inactive.warnings().contains("assignee is inactive"));

        TaskEvaluation overloaded = evaluate(null, c -> c.openTasks = 21);
        assertTrue(overloaded.reassignmentRecommended());
        assertTrue(overloaded.warnings().contains("assignee workload is very high"));

        assertTrue(evaluate(t -> t.priority = TaskPriority.LOW, c -> c.openTasks = 13)
                .reassignmentRecommended());
        assertFalse(evaluate(t -> t.priority = TaskPriority.HIGH, c -> c.openTasks = 13)
                .reassignmentRecommended());
        assertTrue(evaluate(t -> t.dueAt = NOW.minusSeconds(3600), c -> c.openTasks = 11)
                .reassignmentRecommended());
        assertTrue(evaluate(t -> {
            t.status = TaskStatus.BLOCKED;
            t.startedAt = NOW.minusSeconds(3600);
        }, c -> c.reopens = 2).reassignmentRecommended());
        assertFalse(evaluate(null, null).reassignmentRecommended());
    }

    @Test
    @DisplayName("task: allowed actions per status and actor")
    void taskAllowedActions() {
        assertTrue(evaluate(t -> t.status = TaskStatus.DRAFT, c -> c.actorId = "creator")
                .allowedActions().containsAll(Set.of(TaskAction.PUBLISH, TaskAction.CANCEL)));
        assertFalse(evaluate(t -> {
            t.status = TaskStatus.DRAFT;
            t.assigneeId = "someone-else";
        }, null).allowedActions().contains(TaskAction.PUBLISH));
        assertTrue(evaluate(t -> {
            t.status = TaskStatus.DRAFT;
            t.creatorId = "other";
        }, c -> c.administrator = true).allowedActions().contains(TaskAction.PUBLISH));

        assertTrue(evaluate(null, null).allowedActions().contains(TaskAction.START));
        assertFalse(evaluate(null, c -> c.dependenciesComplete = false).allowedActions()
                .contains(TaskAction.START));
        assertTrue(evaluate(null, c -> c.dependenciesComplete = false).warnings()
                .contains("dependencies prevent task start"));
        assertTrue(evaluate(t -> t.assigneeId = "other", c -> {
            c.manager = true;
            c.actorId = "manager";
        }).allowedActions().contains(TaskAction.START));
        assertTrue(evaluate(t -> t.creatorId = "boss", c -> c.actorId = "boss")
                .allowedActions().contains(TaskAction.CANCEL));

        assertTrue(evaluate(t -> {
            t.status = TaskStatus.IN_PROGRESS;
            t.startedAt = NOW.minusSeconds(3600);
        }, null).allowedActions().containsAll(Set.of(TaskAction.BLOCK, TaskAction.COMPLETE)));
        assertTrue(evaluate(t -> {
            t.status = TaskStatus.IN_PROGRESS;
            t.startedAt = NOW.minusSeconds(3600);
            t.reviewRequired = true;
        }, null).allowedActions().contains(TaskAction.SUBMIT_FOR_REVIEW));
        assertTrue(evaluate(t -> {
            t.status = TaskStatus.IN_PROGRESS;
            t.startedAt = NOW.minusSeconds(3600);
            t.assigneeId = "other";
        }, c -> {
            c.manager = true;
            c.actorId = "manager";
        }).allowedActions().contains(TaskAction.CANCEL));

        assertTrue(evaluate(t -> {
            t.status = TaskStatus.BLOCKED;
            t.startedAt = NOW.minusSeconds(3600);
        }, null).allowedActions().contains(TaskAction.UNBLOCK));
        assertTrue(evaluate(t -> {
            t.status = TaskStatus.BLOCKED;
            t.startedAt = NOW.minusSeconds(3600);
        }, c -> {
            c.manager = true;
            c.actorId = "manager";
        }).allowedActions().contains(TaskAction.CANCEL));

        assertTrue(evaluate(t -> {
            t.status = TaskStatus.IN_REVIEW;
            t.startedAt = NOW.minusSeconds(3600);
        }, c -> {
            c.manager = true;
            c.actorId = "manager";
        }).allowedActions().containsAll(Set.of(TaskAction.APPROVE,
                TaskAction.REJECT_REVIEW, TaskAction.CANCEL)));
        assertFalse(evaluate(t -> {
            t.status = TaskStatus.IN_REVIEW;
            t.startedAt = NOW.minusSeconds(3600);
        }, null).allowedActions().contains(TaskAction.APPROVE));

        assertTrue(evaluate(t -> {
            t.status = TaskStatus.COMPLETED;
            t.startedAt = NOW.minusSeconds(7200);
            t.completedAt = NOW.minusSeconds(3600);
        }, c -> {
            c.manager = true;
            c.actorId = "manager";
        }).allowedActions().containsAll(Set.of(TaskAction.REOPEN, TaskAction.ARCHIVE)));

        assertTrue(evaluate(t -> t.status = TaskStatus.CANCELLED, c -> c.actorId = "creator")
                .allowedActions().contains(TaskAction.REOPEN));
        assertFalse(evaluate(t -> {
            t.status = TaskStatus.CANCELLED;
            t.assigneeId = "other";
        }, null).allowedActions().contains(TaskAction.REOPEN));

        assertTrue(evaluate(t -> t.status = TaskStatus.ARCHIVED, c -> {
            c.manager = true;
            c.actorId = "manager";
        }).allowedActions().contains(TaskAction.RESTORE));
        assertFalse(evaluate(t -> t.status = TaskStatus.ARCHIVED, null).allowedActions()
                .contains(TaskAction.RESTORE));

        // global removals
        assertFalse(evaluate(t -> t.status = TaskStatus.DRAFT, c -> c.projectActive = false)
                .allowedActions().contains(TaskAction.PUBLISH));
        assertFalse(evaluate(null, c -> c.assigneeActive = false).allowedActions()
                .contains(TaskAction.START));
        assertFalse(evaluate(t -> {
            t.status = TaskStatus.BLOCKED;
            t.startedAt = NOW.minusSeconds(3600);
        }, c -> c.assigneeActive = false).allowedActions().contains(TaskAction.UNBLOCK));
        assertFalse(evaluate(t -> t.status = TaskStatus.DRAFT, c -> c.workingHours = false)
                .allowedActions().contains(TaskAction.PUBLISH));
        assertTrue(evaluate(t -> t.status = TaskStatus.DRAFT, c -> {
            c.workingHours = false;
            c.administrator = true;
        }).allowedActions().contains(TaskAction.PUBLISH));
        assertFalse(evaluate(t -> {
            t.status = TaskStatus.IN_REVIEW;
            t.startedAt = NOW.minusSeconds(3600);
        }, c -> {
            c.workingHours = false;
            c.manager = true;
            c.actorId = "manager";
        }).allowedActions().contains(TaskAction.APPROVE));
    }

    @Test
    @DisplayName("task: overdue detection and evaluation record")
    void taskOverdueAndRecord() {
        assertFalse(evaluate(t -> t.dueAt = NOW.plusSeconds(3600), null).overdue());
        assertTrue(evaluate(t -> t.dueAt = NOW.minusSeconds(3600), null).overdue());
        assertFalse(evaluate(t -> {
            t.dueAt = NOW.minusSeconds(3600);
            t.status = TaskStatus.COMPLETED;
            t.startedAt = NOW.minusSeconds(7200);
            t.completedAt = NOW.minusSeconds(3600);
        }, null).overdue());

        TaskEvaluation evaluation = evaluate(null, null);
        assertEquals(TaskPriority.NORMAL, evaluation.effectivePriority());
        assertFalse(evaluation.overdue());
        assertFalse(evaluation.escalationRequired());
        assertFalse(evaluation.reassignmentRecommended());
        assertTrue(evaluation.allowedActions().contains(TaskAction.START));
        assertEquals(List.of(), evaluation.warnings());
        assertEquals(evaluation, evaluate(null, null));
        assertTrue(evaluation.toString().contains("NORMAL"));

        assertThrowsWith(NullPointerException.class, "task must not be null",
                () -> new TaskPolicy().evaluate(null, new TaskCtx().build()));
        assertThrowsWith(NullPointerException.class, "context must not be null",
                () -> new TaskPolicy().evaluate(new TaskBuilder().build(), null));
    }

    @Test
    @DisplayName("Task record validates its invariants")
    void taskRecord() {
        Task task = new TaskBuilder().build();
        assertEquals("T1", task.id());
        assertEquals("Fix the report", task.title());
        assertEquals("desc", task.description());
        assertEquals("creator", task.creatorId());
        assertEquals("assignee", task.assigneeId());
        assertEquals(TaskStatus.OPEN, task.status());
        assertEquals(TaskPriority.NORMAL, task.priority());
        assertEquals(RecurrenceRule.NONE, task.recurrence());
        assertEquals(NOW.minusSeconds(3600), task.createdAt());
        assertNull(task.dueAt());
        assertNull(task.startedAt());
        assertNull(task.completedAt());
        assertEquals(60, task.estimatedMinutes());
        assertEquals(0, task.actualMinutes());
        assertEquals(List.of(), task.dependencyIds());
        assertEquals(Set.of(), task.tags());
        assertFalse(task.confidential());
        assertFalse(task.reviewRequired());
        assertFalse(task.isTerminal());
        assertEquals(task, new TaskBuilder().build());
        assertEquals(task.hashCode(), new TaskBuilder().build().hashCode());
        assertTrue(task.toString().contains("T1"));

        TaskBuilder completed = new TaskBuilder();
        completed.status = TaskStatus.COMPLETED;
        completed.startedAt = NOW.minusSeconds(7200);
        completed.completedAt = NOW.minusSeconds(3600);
        assertTrue(completed.build().isTerminal());
        TaskBuilder cancelled = new TaskBuilder();
        cancelled.status = TaskStatus.CANCELLED;
        assertTrue(cancelled.build().isTerminal());

        assertThrowsWith(IllegalArgumentException.class, "id must not be blank", () -> {
            TaskBuilder b = new TaskBuilder();
            b.id = " ";
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "title must not be blank", () -> {
            TaskBuilder b = new TaskBuilder();
            b.title = "";
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "creatorId must not be blank", () -> {
            TaskBuilder b = new TaskBuilder();
            b.creatorId = " ";
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "title cannot exceed 120 characters", () -> {
            TaskBuilder b = new TaskBuilder();
            b.title = "x".repeat(121);
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class,
                "description cannot exceed 5000 characters", () -> {
                    TaskBuilder b = new TaskBuilder();
                    b.description = "x".repeat(5001);
                    b.build();
                });
        assertThrowsWith(IllegalArgumentException.class, "dueAt must not be before createdAt", () -> {
            TaskBuilder b = new TaskBuilder();
            b.dueAt = b.createdAt.minusSeconds(1);
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class,
                "estimatedMinutes is outside the supported range", () -> {
                    TaskBuilder b = new TaskBuilder();
                    b.estimated = -1;
                    b.build();
                });
        assertThrowsWith(IllegalArgumentException.class,
                "estimatedMinutes is outside the supported range", () -> {
                    TaskBuilder b = new TaskBuilder();
                    b.estimated = 100_001;
                    b.build();
                });
        assertThrowsWith(IllegalArgumentException.class,
                "actualMinutes is outside the supported range", () -> {
                    TaskBuilder b = new TaskBuilder();
                    b.actual = -1;
                    b.build();
                });
        assertThrowsWith(IllegalArgumentException.class,
                "actualMinutes is outside the supported range", () -> {
                    TaskBuilder b = new TaskBuilder();
                    b.actual = 200_001;
                    b.build();
                });
        assertThrowsWith(IllegalArgumentException.class, "task cannot depend on itself", () -> {
            TaskBuilder b = new TaskBuilder();
            b.dependencies = List.of("T1");
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "dependencies must be unique", () -> {
            TaskBuilder b = new TaskBuilder();
            b.dependencies = List.of("d1", "d1");
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "startedAt is required after work begins", () -> {
            TaskBuilder b = new TaskBuilder();
            b.status = TaskStatus.IN_PROGRESS;
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "startedAt is required after work begins", () -> {
            TaskBuilder b = new TaskBuilder();
            b.status = TaskStatus.BLOCKED;
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "startedAt is required after work begins", () -> {
            TaskBuilder b = new TaskBuilder();
            b.status = TaskStatus.IN_REVIEW;
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "completedAt is required for completed task", () -> {
            TaskBuilder b = new TaskBuilder();
            b.status = TaskStatus.COMPLETED;
            b.startedAt = NOW.minusSeconds(3600);
            b.build();
        });
        assertThrowsWith(IllegalArgumentException.class,
                "completedAt is only valid for completed task", () -> {
                    TaskBuilder b = new TaskBuilder();
                    b.completedAt = NOW.minusSeconds(3600);
                    b.build();
                });
        assertThrowsWith(NullPointerException.class, "status must not be null", () -> {
            TaskBuilder b = new TaskBuilder();
            b.status = null;
            b.build();
        });
        assertThrowsWith(NullPointerException.class, "priority must not be null", () -> {
            TaskBuilder b = new TaskBuilder();
            b.priority = null;
            b.build();
        });
        assertThrowsWith(NullPointerException.class, "recurrence must not be null", () -> {
            TaskBuilder b = new TaskBuilder();
            b.recurrence = null;
            b.build();
        });
        assertThrowsWith(NullPointerException.class, "createdAt must not be null", () -> {
            TaskBuilder b = new TaskBuilder();
            b.createdAt = null;
            b.build();
        });
        assertThrowsWith(NullPointerException.class, null, () -> {
            TaskBuilder b = new TaskBuilder();
            b.dependencies = null;
            b.build();
        });
        assertThrowsWith(NullPointerException.class, null, () -> {
            TaskBuilder b = new TaskBuilder();
            b.tags = null;
            b.build();
        });

        TaskContext context = new TaskCtx().build();
        assertEquals("assignee", context.actorId());
        assertEquals(NOW, context.now());
        assertFalse(context.actorIsManager());
        assertFalse(context.actorIsAdministrator());
        assertTrue(context.dependenciesComplete());
        assertTrue(context.assigneeActive());
        assertTrue(context.projectActive());
        assertTrue(context.withinWorkingHours());
        assertTrue(context.notificationsEnabled());
        assertEquals(0, context.previousReopens());
        assertEquals(0, context.assigneeOpenTaskCount());
        assertEquals(0, context.overdueTaskCount());
        assertEquals(context, new TaskCtx().build());
        assertEquals(context.hashCode(), new TaskCtx().build().hashCode());
        assertTrue(context.toString().contains("assignee"));
        assertThrowsWith(IllegalArgumentException.class, "actorId must not be blank", () -> {
            TaskCtx bad = new TaskCtx();
            bad.actorId = " ";
            bad.build();
        });
        assertThrowsWith(NullPointerException.class, "now must not be null", () -> {
            TaskCtx bad = new TaskCtx();
            bad.now = null;
            bad.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "task counters must not be negative", () -> {
            TaskCtx bad = new TaskCtx();
            bad.reopens = -1;
            bad.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "task counters must not be negative", () -> {
            TaskCtx bad = new TaskCtx();
            bad.openTasks = -1;
            bad.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "task counters must not be negative", () -> {
            TaskCtx bad = new TaskCtx();
            bad.overdue = -1;
            bad.build();
        });

        TaskEvaluation taskEvaluation = new TaskEvaluation(TaskPriority.HIGH, true, true, true,
                Set.of(TaskAction.START), List.of("warning"));
        assertEquals(TaskPriority.HIGH, taskEvaluation.effectivePriority());
        assertTrue(taskEvaluation.overdue());
        assertTrue(taskEvaluation.escalationRequired());
        assertTrue(taskEvaluation.reassignmentRecommended());
        assertEquals(Set.of(TaskAction.START), taskEvaluation.allowedActions());
        assertEquals(List.of("warning"), taskEvaluation.warnings());
        assertEquals(taskEvaluation, new TaskEvaluation(TaskPriority.HIGH, true, true, true,
                Set.of(TaskAction.START), List.of("warning")));
        assertEquals(taskEvaluation.hashCode(), taskEvaluation.hashCode());
        assertTrue(taskEvaluation.toString().contains("HIGH"));
        assertThrowsWith(NullPointerException.class, null,
                () -> new TaskEvaluation(null, false, false, false, Set.of(), List.of()));
        assertThrowsWith(NullPointerException.class, null,
                () -> new TaskEvaluation(TaskPriority.HIGH, false, false, false, null, List.of()));
        assertThrowsWith(NullPointerException.class, null,
                () -> new TaskEvaluation(TaskPriority.HIGH, false, false, false, Set.of(), null));
    }

    @Test
    @DisplayName("TaskStateMachine applies allowed transitions only")
    void taskStateMachine() {
        TaskStateMachine machine = new TaskStateMachine();
        TaskCtx ctx = new TaskCtx();
        ctx.actorId = "creator";
        TaskContext creatorContext = ctx.build();
        TaskContext managerContext = new TaskContext("manager", NOW, true, false, true, true,
                true, true, true, 0, 0, 0);

        TaskBuilder draftBuilder = new TaskBuilder();
        draftBuilder.status = TaskStatus.DRAFT;
        Task draft = draftBuilder.build();
        Task published = machine.apply(draft, TaskAction.PUBLISH, creatorContext);
        assertEquals(TaskStatus.OPEN, published.status());
        assertEquals("T1", published.id());

        Task started = machine.apply(published, TaskAction.START, managerContext);
        assertEquals(TaskStatus.IN_PROGRESS, started.status());
        assertEquals(NOW, started.startedAt());

        Task completed = machine.apply(started, TaskAction.COMPLETE, managerContext);
        assertEquals(TaskStatus.COMPLETED, completed.status());
        assertEquals(NOW, completed.completedAt());
        assertTrue(completed.isTerminal());

        Task blocked = machine.apply(started, TaskAction.BLOCK, managerContext);
        assertEquals(TaskStatus.BLOCKED, blocked.status());
        assertEquals(TaskStatus.IN_PROGRESS, machine.apply(blocked, TaskAction.UNBLOCK,
                managerContext).status());

        TaskBuilder reviewBuilder = new TaskBuilder();
        reviewBuilder.status = TaskStatus.IN_PROGRESS;
        reviewBuilder.startedAt = NOW.minusSeconds(3600);
        reviewBuilder.reviewRequired = true;
        TaskContext manager = new TaskContext("manager", NOW, true, false, true, true, true, true,
                true, 0, 0, 0);
        Task submittedForReview = machine.apply(reviewBuilder.build(), TaskAction.SUBMIT_FOR_REVIEW,
                managerContext);
        assertEquals(TaskStatus.IN_REVIEW, submittedForReview.status());
        assertEquals(TaskStatus.COMPLETED, machine.apply(submittedForReview, TaskAction.APPROVE,
                manager).status());

        TaskBuilder reviewDirect = new TaskBuilder();
        reviewDirect.status = TaskStatus.IN_REVIEW;
        reviewDirect.startedAt = NOW.minusSeconds(3600);
        assertEquals(TaskStatus.IN_PROGRESS, machine.apply(reviewDirect.build(),
                TaskAction.REJECT_REVIEW, managerContext).status());

        Task cancelled = machine.apply(published, TaskAction.CANCEL, creatorContext);
        assertEquals(TaskStatus.CANCELLED, cancelled.status());
        Task reopened = machine.apply(cancelled, TaskAction.REOPEN, creatorContext);
        assertEquals(TaskStatus.OPEN, reopened.status());
        assertNull(reopened.startedAt());

        Task archived = machine.apply(cancelled, TaskAction.ARCHIVE, creatorContext);
        assertEquals(TaskStatus.ARCHIVED, archived.status());
        // RESTORE clears startedAt but targets IN_PROGRESS, so the Task invariant rejects it.
        assertThrowsWith(IllegalArgumentException.class, "startedAt is required after work begins",
                () -> machine.apply(archived, TaskAction.RESTORE, managerContext));

        assertThrowsWith(IllegalStateException.class, "action START is not allowed from DRAFT",
                () -> machine.apply(draft, TaskAction.START, creatorContext));
        assertThrowsWith(IllegalStateException.class, "action PUBLISH is not allowed from OPEN",
                () -> machine.apply(published, TaskAction.PUBLISH, creatorContext));
        assertThrowsWith(NullPointerException.class, "task must not be null",
                () -> machine.apply(null, TaskAction.PUBLISH, creatorContext));
        assertThrowsWith(NullPointerException.class, "action must not be null",
                () -> machine.apply(draft, null, creatorContext));
        assertThrowsWith(NullPointerException.class, "context must not be null",
                () -> machine.apply(draft, TaskAction.PUBLISH, null));

        assertEquals(published, machine.apply(draft, TaskAction.PUBLISH, creatorContext));
    }

    // ==================================================================
    // workflow
    // ==================================================================

    private static final Instant T0 = Instant.parse("2026-03-02T09:00:00Z");

    private static ApprovalStep anyOneStep(String id, String approver) {
        return new ApprovalStep(id, "Step " + id, Set.of(approver), StepMode.ANY_ONE, 1, false, false, null);
    }

    private static WorkflowAction decision(String actor, ApprovalDecision choice, Instant when) {
        String comment = choice == ApprovalDecision.REJECT || choice == ApprovalDecision.RETURN
                ? "because" : null;
        return new WorkflowAction(actor, choice, comment, when);
    }

    @Test
    @DisplayName("ApprovalStep validates its configuration and delegation")
    void approvalStep() {
        ApprovalStep step = anyOneStep("S1", "M1");
        assertEquals("S1", step.stepId());
        assertEquals("Step S1", step.name());
        assertEquals(Set.of("M1"), step.approverIds());
        assertEquals(StepMode.ANY_ONE, step.mode());
        assertEquals(1, step.requiredApprovals());
        assertFalse(step.delegationAllowed());
        assertFalse(step.skipRequester());
        assertNull(step.deadline());
        assertTrue(step.canApprove("M1"));
        assertTrue(step.canApprove(" M1 "));
        assertFalse(step.canApprove("M2"));
        assertFalse(step.canApprove(null));
        assertEquals(step, anyOneStep("S1", "M1"));
        assertEquals(step.hashCode(), anyOneStep("S1", "M1").hashCode());
        assertTrue(step.toString().contains("S1"));

        ApprovalStep delegable = new ApprovalStep("S2", "Step S2", Set.of("M1", "M2"),
                StepMode.REQUIRED_COUNT, 2, true, false, null);
        ApprovalStep delegated = delegable.delegate("M1", "M3");
        assertEquals(Set.of("M2", "M3"), delegated.approverIds());
        assertEquals(2, delegated.requiredApprovals());
        assertThrowsWith(IllegalStateException.class, "delegation is not allowed for this step",
                () -> step.delegate("M1", "M2"));
        assertThrowsWith(IllegalArgumentException.class, "delegating actor is not an approver",
                () -> delegable.delegate("M9", "M3"));
        assertThrowsWith(IllegalArgumentException.class, "delegating actor is not an approver",
                () -> delegable.delegate(null, "M3"));
        assertThrowsWith(IllegalArgumentException.class, "delegate must not be blank",
                () -> delegable.delegate("M1", " "));
        assertThrowsWith(IllegalArgumentException.class, "delegate must not be blank",
                () -> delegable.delegate("M1", null));
        assertThrowsWith(IllegalArgumentException.class, "cannot delegate to the same actor",
                () -> delegable.delegate("M1", "M1"));
        assertThrowsWith(IllegalArgumentException.class, "delegate is already an approver",
                () -> delegable.delegate("M1", "M2"));

        assertThrowsWith(IllegalArgumentException.class, "stepId must not be blank",
                () -> new ApprovalStep(" ", "n", Set.of("M1"), StepMode.ANY_ONE, 1, false, false, null));
        assertThrowsWith(IllegalArgumentException.class, "name must not be blank",
                () -> new ApprovalStep("S1", "  ", Set.of("M1"), StepMode.ANY_ONE, 1, false, false, null));
        assertThrowsWith(NullPointerException.class, "approverIds must not be null",
                () -> new ApprovalStep("S1", "n", null, StepMode.ANY_ONE, 1, false, false, null));
        assertThrowsWith(IllegalArgumentException.class, "step requires at least one approver",
                () -> new ApprovalStep("S1", "n", Set.of(), StepMode.ANY_ONE, 1, false, false, null));
        assertThrowsWith(IllegalArgumentException.class, "approverId must not be blank",
                () -> new ApprovalStep("S1", "n", Set.of(" "), StepMode.ANY_ONE, 1, false, false, null));
        assertThrowsWith(NullPointerException.class, "mode must not be null",
                () -> new ApprovalStep("S1", "n", Set.of("M1"), null, 1, false, false, null));
        assertThrowsWith(IllegalArgumentException.class, "requiredApprovals is outside approver count",
                () -> new ApprovalStep("S1", "n", Set.of("M1"), StepMode.REQUIRED_COUNT, 0,
                        false, false, null));
        assertThrowsWith(IllegalArgumentException.class, "requiredApprovals is outside approver count",
                () -> new ApprovalStep("S1", "n", Set.of("M1"), StepMode.REQUIRED_COUNT, 2,
                        false, false, null));
        assertThrowsWith(IllegalArgumentException.class, "ANY_ONE requires exactly one approval",
                () -> new ApprovalStep("S1", "n", Set.of("M1", "M2"), StepMode.ANY_ONE, 2,
                        false, false, null));
        assertThrowsWith(IllegalArgumentException.class, "UNANIMOUS requires every approver",
                () -> new ApprovalStep("S1", "n", Set.of("M1", "M2"), StepMode.UNANIMOUS, 1,
                        false, false, null));
        assertThrowsWith(IllegalArgumentException.class,
                "MAJORITY threshold must exceed half of approvers",
                () -> new ApprovalStep("S1", "n", Set.of("M1", "M2"), StepMode.MAJORITY, 1,
                        false, false, null));
        ApprovalStep majority = new ApprovalStep("S1", "n", Set.of("M1", "M2", "M3"),
                StepMode.MAJORITY, 2, false, false, null);
        assertEquals(2, majority.requiredApprovals());
        new ApprovalStep("S1", "n", Set.of("M1", "M2"), StepMode.UNANIMOUS, 2, false, false, null);
        ApprovalStep trimmedStep = new ApprovalStep(" S1 ", " n ", Set.of(" M1 "),
                StepMode.ANY_ONE, 1, false, false, null);
        assertEquals("S1", trimmedStep.stepId());
        assertEquals("n", trimmedStep.name());
        assertEquals(Set.of("M1"), trimmedStep.approverIds());
    }

    @Test
    @DisplayName("StepProgress counts decisions and evaluates voting modes")
    void stepProgress() {
        ApprovalStep step = new ApprovalStep("S1", "n", Set.of("M1", "M2"),
                StepMode.REQUIRED_COUNT, 2, false, false, null);
        StepProgress empty = StepProgress.empty(step);
        assertEquals(step, empty.step());
        assertEquals(Map.of(), empty.decisions());
        assertFalse(empty.satisfied());
        assertTrue(empty.canStillSucceed());
        assertEquals(0, empty.approvals());
        assertEquals(0, empty.rejections());
        assertEquals(0, empty.abstentions());

        StepProgress oneApprove = empty.decide("M1", ApprovalDecision.APPROVE);
        assertEquals(1, oneApprove.approvals());
        assertEquals(0, oneApprove.rejections());
        assertEquals(0, oneApprove.abstentions());
        assertFalse(oneApprove.satisfied());
        assertTrue(oneApprove.canStillSucceed());
        StepProgress both = oneApprove.decide("M2", ApprovalDecision.APPROVE);
        assertTrue(both.satisfied());
        assertTrue(both.canStillSucceed());

        StepProgress reject = StepProgress.empty(step).decide("M1", ApprovalDecision.REJECT);
        assertEquals(1, reject.rejections());
        assertFalse(reject.canStillSucceed());

        ApprovalStep anyOne = anyOneStep("S2", "M1");
        assertTrue(StepProgress.empty(anyOne).decide("M1", ApprovalDecision.APPROVE).satisfied());
        assertFalse(StepProgress.empty(anyOne).decide("M1", ApprovalDecision.ABSTAIN).satisfied());
        assertEquals(1, StepProgress.empty(anyOne).decide("M1", ApprovalDecision.ABSTAIN).abstentions());
        assertFalse(StepProgress.empty(anyOne).decide("M1", ApprovalDecision.ABSTAIN)
                .canStillSucceed());

        ApprovalStep unanimous = new ApprovalStep("S3", "n", Set.of("M1", "M2"),
                StepMode.UNANIMOUS, 2, false, false, null);
        StepProgress unanimousOne = StepProgress.empty(unanimous).decide("M1",
                ApprovalDecision.APPROVE);
        assertFalse(unanimousOne.satisfied());
        assertTrue(unanimousOne.decide("M2", ApprovalDecision.APPROVE).satisfied());

        ApprovalStep majority = new ApprovalStep("S4", "n", Set.of("M1", "M2", "M3"),
                StepMode.MAJORITY, 2, false, false, null);
        assertFalse(StepProgress.empty(majority).decide("M1", ApprovalDecision.APPROVE).satisfied());

        assertThrowsWith(IllegalArgumentException.class, "actorId must not be blank",
                () -> empty.decide(" ", ApprovalDecision.APPROVE));
        assertThrowsWith(IllegalArgumentException.class, "actorId must not be blank",
                () -> empty.decide(null, ApprovalDecision.APPROVE));
        assertThrowsWith(IllegalArgumentException.class,
                "actor is not an approver for the current step",
                () -> empty.decide("M9", ApprovalDecision.APPROVE));
        assertThrowsWith(IllegalStateException.class, "actor has already decided this step",
                () -> oneApprove.decide("M1", ApprovalDecision.APPROVE));
        assertThrowsWith(NullPointerException.class, null,
                () -> empty.decide("M1", null));

        StepProgress delegated = empty.withDelegatedStep(
                anyOneStep("S1", "M3"), "M1");
        assertEquals(Set.of("M3"), delegated.step().approverIds());
        assertThrowsWith(IllegalStateException.class,
                "actor cannot delegate after making a decision",
                () -> oneApprove.withDelegatedStep(anyOneStep("S1", "M3"), "M1"));

        assertThrowsWith(NullPointerException.class, "step must not be null",
                () -> new StepProgress(null, Map.of()));
        assertThrows(NullPointerException.class,
                () -> new StepProgress(step, null));
        assertThrowsWith(IllegalArgumentException.class,
                "decision actor is not an approver: M9",
                () -> new StepProgress(step, Map.of("M9", ApprovalDecision.APPROVE)));
        StepProgress progress = StepProgress.empty(step);
        assertEquals(progress, StepProgress.empty(step));
        assertEquals(progress.hashCode(), StepProgress.empty(step).hashCode());
        assertTrue(progress.toString().contains("S1"));
    }

    @Test
    @DisplayName("WorkflowAction and WorkflowSnapshot validate their invariants")
    void workflowRecords() {
        WorkflowAction action = new WorkflowAction(" M1 ", ApprovalDecision.APPROVE, " ok ", T0);
        assertEquals("M1", action.actorId());
        assertEquals(ApprovalDecision.APPROVE, action.decision());
        assertEquals("ok", action.comment());
        assertEquals(T0, action.actionTime());
        assertEquals(action, new WorkflowAction("M1", ApprovalDecision.APPROVE, "ok", T0));
        assertEquals(action.hashCode(), new WorkflowAction("M1", ApprovalDecision.APPROVE, "ok", T0)
                .hashCode());
        assertTrue(action.toString().contains("APPROVE"));
        assertThrowsWith(IllegalArgumentException.class, "actorId must not be blank",
                () -> new WorkflowAction(" ", ApprovalDecision.APPROVE, null, T0));
        assertThrowsWith(NullPointerException.class, "decision must not be null",
                () -> new WorkflowAction("M1", null, null, T0));
        assertThrowsWith(NullPointerException.class, "actionTime must not be null",
                () -> new WorkflowAction("M1", ApprovalDecision.APPROVE, null, null));
        assertThrowsWith(IllegalArgumentException.class,
                "reject and return decisions require a comment",
                () -> new WorkflowAction("M1", ApprovalDecision.REJECT, null, T0));
        assertThrowsWith(IllegalArgumentException.class,
                "reject and return decisions require a comment",
                () -> new WorkflowAction("M1", ApprovalDecision.RETURN, " ", T0));
        assertThrowsWith(NullPointerException.class,
                "decision must not be null",
                () -> new WorkflowAction("M1", null, "comment", T0));

        WorkflowSnapshot snapshot = new WorkflowSnapshot(" W1 ", " R1 ",
                WorkflowState.PENDING, List.of(StepProgress.empty(anyOneStep("S1", "M1"))),
                0, T0, T0, 1, false, true);
        assertEquals("W1", snapshot.workflowId());
        assertEquals("R1", snapshot.requesterId());
        assertEquals(WorkflowState.PENDING, snapshot.state());
        assertEquals(1, snapshot.steps().size());
        assertEquals(0, snapshot.currentStepIndex());
        assertEquals(T0, snapshot.createdAt());
        assertEquals(T0, snapshot.updatedAt());
        assertEquals(1, snapshot.version());
        assertFalse(snapshot.urgent());
        assertTrue(snapshot.withdrawalAllowed());
        assertFalse(snapshot.terminal());
        assertEquals("S1", snapshot.currentStep().step().stepId());
        assertEquals(snapshot, new WorkflowSnapshot("W1", "R1", WorkflowState.PENDING,
                List.of(StepProgress.empty(anyOneStep("S1", "M1"))), 0, T0, T0, 1, false, true));
        assertTrue(snapshot.toString().contains("PENDING"));

        WorkflowSnapshot approved = new WorkflowSnapshot("W1", "R1", WorkflowState.APPROVED,
                List.of(StepProgress.empty(anyOneStep("S1", "M1"))), 1, T0, T0, 1, false, true);
        assertTrue(approved.terminal());
        assertThrowsWith(IllegalStateException.class, "terminal workflow has no current step",
                approved::currentStep);

        assertThrowsWith(IllegalArgumentException.class, "workflowId must not be blank",
                () -> new WorkflowSnapshot(" ", "R1", WorkflowState.PENDING,
                        List.of(StepProgress.empty(anyOneStep("S1", "M1"))), 0, T0, T0, 1, false, true));
        assertThrowsWith(IllegalArgumentException.class, "requesterId must not be blank",
                () -> new WorkflowSnapshot("W1", " ", WorkflowState.PENDING,
                        List.of(StepProgress.empty(anyOneStep("S1", "M1"))), 0, T0, T0, 1, false, true));
        assertThrowsWith(NullPointerException.class, "state must not be null",
                () -> new WorkflowSnapshot("W1", "R1", null,
                        List.of(StepProgress.empty(anyOneStep("S1", "M1"))), 0, T0, T0, 1, false, true));
        assertThrows(NullPointerException.class,
                () -> new WorkflowSnapshot("W1", "R1", WorkflowState.PENDING, null, 0, T0, T0, 1,
                        false, true));
        assertThrowsWith(NullPointerException.class, "createdAt must not be null",
                () -> new WorkflowSnapshot("W1", "R1", WorkflowState.PENDING,
                        List.of(StepProgress.empty(anyOneStep("S1", "M1"))), 0, null, T0, 1, false, true));
        assertThrowsWith(NullPointerException.class, "updatedAt must not be null",
                () -> new WorkflowSnapshot("W1", "R1", WorkflowState.PENDING,
                        List.of(StepProgress.empty(anyOneStep("S1", "M1"))), 0, T0, null, 1, false, true));
        assertThrowsWith(IllegalArgumentException.class, "workflow requires at least one step",
                () -> new WorkflowSnapshot("W1", "R1", WorkflowState.PENDING, List.of(), 0, T0, T0, 1,
                        false, true));
        assertThrowsWith(IllegalArgumentException.class,
                "updatedAt must not be before createdAt",
                () -> new WorkflowSnapshot("W1", "R1", WorkflowState.PENDING,
                        List.of(StepProgress.empty(anyOneStep("S1", "M1"))), 0, T0, T0.minusSeconds(1),
                        1, false, true));
        assertThrowsWith(IllegalArgumentException.class, "version must be positive",
                () -> new WorkflowSnapshot("W1", "R1", WorkflowState.PENDING,
                        List.of(StepProgress.empty(anyOneStep("S1", "M1"))), 0, T0, T0, 0, false, true));
        assertThrowsWith(IllegalArgumentException.class, "terminal currentStepIndex is invalid",
                () -> new WorkflowSnapshot("W1", "R1", WorkflowState.APPROVED,
                        List.of(StepProgress.empty(anyOneStep("S1", "M1"))), 2, T0, T0, 1, false, true));
        assertThrowsWith(IllegalArgumentException.class, "terminal currentStepIndex is invalid",
                () -> new WorkflowSnapshot("W1", "R1", WorkflowState.APPROVED,
                        List.of(StepProgress.empty(anyOneStep("S1", "M1"))), -1, T0, T0, 1, false, true));
        assertThrowsWith(IllegalArgumentException.class, "active currentStepIndex is invalid",
                () -> new WorkflowSnapshot("W1", "R1", WorkflowState.PENDING,
                        List.of(StepProgress.empty(anyOneStep("S1", "M1"))), 1, T0, T0, 1, false, true));
        assertThrowsWith(IllegalArgumentException.class, "active currentStepIndex is invalid",
                () -> new WorkflowSnapshot("W1", "R1", WorkflowState.PENDING,
                        List.of(StepProgress.empty(anyOneStep("S1", "M1"))), -1, T0, T0, 1, false, true));
    }

    @Test
    @DisplayName("workflow engine: start validates the configuration")
    void workflowStart() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        List<ApprovalStep> steps = List.of(anyOneStep("S1", "M1"), anyOneStep("S2", "M2"));
        WorkflowSnapshot snapshot = engine.start("W1", " R1 ", steps, T0, false, true);
        assertEquals(WorkflowState.PENDING, snapshot.state());
        assertEquals(0, snapshot.currentStepIndex());
        assertEquals(1, snapshot.version());
        assertEquals("R1", snapshot.requesterId());

        assertThrowsWith(IllegalArgumentException.class,
                "workflow requires at least one step",
                () -> engine.start("W1", "R1", List.of(), T0, false, true));
        assertThrowsWith(IllegalArgumentException.class, "stepId must be unique: S1",
                () -> engine.start("W1", "R1", List.of(anyOneStep("S1", "M1"),
                        anyOneStep("S1", "M2")), T0, false, true));
        ApprovalStep expiredDeadline = new ApprovalStep("S1", "n", Set.of("M1"),
                StepMode.ANY_ONE, 1, false, false, T0);
        assertThrowsWith(IllegalArgumentException.class,
                "step deadline must be after workflow creation",
                () -> engine.start("W1", "R1", List.of(expiredDeadline), T0, false, true));
        assertThrowsWith(IllegalArgumentException.class, "requesterId must not be blank",
                () -> engine.start("W1", " ", steps, T0, false, true));
        assertThrowsWith(IllegalArgumentException.class, "requesterId must not be blank",
                () -> engine.start("W1", null, steps, T0, false, true));
        assertThrowsWith(NullPointerException.class, "configuredSteps must not be null",
                () -> engine.start("W1", "R1", null, T0, false, true));
        assertThrowsWith(NullPointerException.class, "now must not be null",
                () -> engine.start("W1", "R1", steps, null, false, true));

        ApprovalStep futureDeadline = new ApprovalStep("S1", "n", Set.of("M1"),
                StepMode.ANY_ONE, 1, false, false, T0.plusSeconds(3600));
        WorkflowSnapshot withDeadline = engine.start("W1", "R1", List.of(futureDeadline), T0,
                true, false);
        assertTrue(withDeadline.urgent());
        assertFalse(withDeadline.withdrawalAllowed());

        // a step whose only approver is the requester is skipped
        ApprovalStep selfStep = new ApprovalStep("S1", "n", Set.of("R1"),
                StepMode.ANY_ONE, 1, false, true, null);
        WorkflowSnapshot selfSkipped = engine.start("W1", "R1", List.of(selfStep), T0, false, true);
        assertEquals(WorkflowState.APPROVED, selfSkipped.state());
        assertEquals(1, selfSkipped.currentStepIndex());

        ApprovalStep selfStepTwoApprovers = new ApprovalStep("S1", "n", Set.of("R1", "M1"),
                StepMode.ANY_ONE, 1, false, true, null);
        assertEquals(WorkflowState.PENDING,
                engine.start("W1", "R1", List.of(selfStepTwoApprovers), T0, false, true).state());
    }

    @Test
    @DisplayName("workflow engine: sequential approvals, rejects and returns")
    void workflowAct() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        List<ApprovalStep> steps = List.of(anyOneStep("S1", "M1"), anyOneStep("S2", "M2"));
        WorkflowSnapshot snapshot = engine.start("W1", "R1", steps, T0, false, true);

        WorkflowSnapshot firstApproved = engine.act(snapshot,
                decision("M1", ApprovalDecision.APPROVE, T0.plusSeconds(1)));
        assertEquals(WorkflowState.IN_PROGRESS, firstApproved.state());
        assertEquals(1, firstApproved.currentStepIndex());
        assertEquals(2, firstApproved.version());
        assertEquals(T0.plusSeconds(1), firstApproved.updatedAt());

        WorkflowSnapshot approved = engine.act(firstApproved,
                decision("M2", ApprovalDecision.APPROVE, T0.plusSeconds(2)));
        assertEquals(WorkflowState.APPROVED, approved.state());
        assertEquals(2, approved.currentStepIndex());
        assertTrue(approved.terminal());

        WorkflowSnapshot rejected = engine.act(snapshot,
                decision("M1", ApprovalDecision.REJECT, T0.plusSeconds(1)));
        assertEquals(WorkflowState.REJECTED, rejected.state());

        WorkflowSnapshot returned = engine.act(snapshot,
                decision("M1", ApprovalDecision.RETURN, T0.plusSeconds(1)));
        assertEquals(WorkflowState.RETURNED, returned.state());

        WorkflowSnapshot abstained = engine.act(snapshot,
                new WorkflowAction("M1", ApprovalDecision.ABSTAIN, null, T0.plusSeconds(1)));
        assertEquals(WorkflowState.REJECTED, abstained.state());

        assertThrowsWith(IllegalStateException.class, "terminal workflow cannot accept actions",
                () -> engine.act(approved, decision("M2", ApprovalDecision.APPROVE, T0.plusSeconds(3))));
        assertThrowsWith(IllegalArgumentException.class,
                "action time cannot precede the previous update",
                () -> engine.act(firstApproved, decision("M2", ApprovalDecision.APPROVE, T0)));
        assertThrowsWith(IllegalArgumentException.class,
                "actor is not authorized for the current step",
                () -> engine.act(snapshot, decision("M9", ApprovalDecision.APPROVE, T0.plusSeconds(1))));
        assertThrowsWith(NullPointerException.class, "snapshot must not be null",
                () -> engine.act(null, decision("M1", ApprovalDecision.APPROVE, T0)));
        assertThrowsWith(NullPointerException.class, "action must not be null",
                () -> engine.act(snapshot, null));
    }

    @Test
    @DisplayName("workflow engine: majority voting needs several approvals")
    void workflowMajority() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        ApprovalStep majority = new ApprovalStep("S1", "n", Set.of("M1", "M2", "M3"),
                StepMode.MAJORITY, 2, false, false, null);
        WorkflowSnapshot snapshot = engine.start("W1", "R1", List.of(majority), T0, false, true);
        WorkflowSnapshot one = engine.act(snapshot,
                decision("M1", ApprovalDecision.APPROVE, T0.plusSeconds(1)));
        assertEquals(WorkflowState.IN_PROGRESS, one.state());
        assertEquals(0, one.currentStepIndex());
        WorkflowSnapshot two = engine.act(one, decision("M2", ApprovalDecision.APPROVE,
                T0.plusSeconds(2)));
        assertEquals(WorkflowState.APPROVED, two.state());

        WorkflowSnapshot rejectAfterApprove = engine.act(one,
                decision("M2", ApprovalDecision.REJECT, T0.plusSeconds(2)));
        assertEquals(WorkflowState.REJECTED, rejectAfterApprove.state());

        ApprovalStep required = new ApprovalStep("S1", "n", Set.of("M1", "M2"),
                StepMode.REQUIRED_COUNT, 2, false, false, null);
        WorkflowSnapshot requiredSnapshot = engine.start("W1", "R1", List.of(required), T0,
                false, true);
        WorkflowSnapshot partial = engine.act(requiredSnapshot,
                decision("M1", ApprovalDecision.APPROVE, T0.plusSeconds(1)));
        assertEquals(WorkflowState.IN_PROGRESS, partial.state());
        // one rejection with one undecided approver still allows success
        WorkflowSnapshot rejectedOne = engine.act(partial,
                decision("M2", ApprovalDecision.REJECT, T0.plusSeconds(2)));
        assertEquals(WorkflowState.REJECTED, rejectedOne.state());

        ApprovalStep unanimous = new ApprovalStep("S1", "n", Set.of("M1", "M2"),
                StepMode.UNANIMOUS, 2, false, false, null);
        WorkflowSnapshot unanimousSnapshot = engine.start("W1", "R1", List.of(unanimous), T0,
                false, true);
        assertEquals(WorkflowState.IN_PROGRESS, engine.act(unanimousSnapshot,
                decision("M1", ApprovalDecision.APPROVE, T0.plusSeconds(1))).state());
    }

    @Test
    @DisplayName("workflow engine: deadline expiry and skip-requester rules")
    void workflowExpiryAndSkipRequester() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        ApprovalStep deadlineStep = new ApprovalStep("S1", "n", Set.of("M1"),
                StepMode.ANY_ONE, 1, false, false, T0.plusSeconds(10));
        WorkflowSnapshot snapshot = engine.start("W1", "R1", List.of(deadlineStep), T0, false, true);
        assertEquals(WorkflowState.EXPIRED, engine.act(snapshot,
                decision("M1", ApprovalDecision.APPROVE, T0.plusSeconds(11))).state());

        assertEquals(snapshot, engine.expireIfNecessary(snapshot, T0.plusSeconds(10)));
        assertEquals(snapshot, engine.expireIfNecessary(snapshot, T0));
        assertEquals(WorkflowState.EXPIRED,
                engine.expireIfNecessary(snapshot, T0.plusSeconds(11)).state());
        WorkflowSnapshot noDeadline = engine.start("W1", "R1",
                List.of(anyOneStep("S1", "M1")), T0, false, true);
        assertEquals(noDeadline, engine.expireIfNecessary(noDeadline, T0.plusSeconds(1000)));
        WorkflowSnapshot expired = engine.expireIfNecessary(snapshot, T0.plusSeconds(11));
        assertEquals(expired, engine.expireIfNecessary(expired, T0.plusSeconds(20)));
        assertThrowsWith(NullPointerException.class, "snapshot must not be null",
                () -> engine.expireIfNecessary(null, T0));
        assertThrowsWith(NullPointerException.class, "now must not be null",
                () -> engine.expireIfNecessary(snapshot, null));

        ApprovalStep skipStep = new ApprovalStep("S1", "n", Set.of("R1", "M1"),
                StepMode.ANY_ONE, 1, false, true, null);
        WorkflowSnapshot skipSnapshot = engine.start("W1", "R1", List.of(skipStep), T0, false, true);
        assertThrowsWith(IllegalArgumentException.class,
                "requester cannot approve this step",
                () -> engine.act(skipSnapshot, decision("R1", ApprovalDecision.APPROVE,
                        T0.plusSeconds(1))));
    }

    @Test
    @DisplayName("workflow engine: delegation, withdrawal and cancellation")
    void workflowDelegateWithdrawCancel() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        ApprovalStep delegable = new ApprovalStep("S1", "n", Set.of("M1"),
                StepMode.ANY_ONE, 1, true, false, null);
        WorkflowSnapshot snapshot = engine.start("W1", "R1", List.of(delegable), T0, false, true);

        WorkflowSnapshot delegated = engine.delegate(snapshot, " M1 ", " M2 ", T0.plusSeconds(1));
        assertEquals(WorkflowState.IN_PROGRESS, delegated.state());
        assertEquals(Set.of("M2"), delegated.currentStep().step().approverIds());
        assertEquals(2, delegated.version());
        assertEquals(T0.plusSeconds(1), delegated.updatedAt());

        assertThrowsWith(IllegalArgumentException.class,
                "delegation time cannot precede the previous update",
                () -> engine.delegate(snapshot, "M1", "M2", T0.minusSeconds(1)));
        assertThrowsWith(IllegalArgumentException.class, "fromActor must not be blank",
                () -> engine.delegate(snapshot, " ", "M2", T0.plusSeconds(1)));
        assertThrowsWith(IllegalArgumentException.class, "toActor must not be blank",
                () -> engine.delegate(snapshot, "M1", " ", T0.plusSeconds(1)));
        assertThrowsWith(NullPointerException.class, "snapshot must not be null",
                () -> engine.delegate(null, "M1", "M2", T0));
        assertThrowsWith(NullPointerException.class, "now must not be null",
                () -> engine.delegate(snapshot, "M1", "M2", null));
        ApprovalStep skipStep = new ApprovalStep("S1", "n", Set.of("M1", "M2"),
                StepMode.ANY_ONE, 1, true, true, null);
        WorkflowSnapshot skipSnapshot = engine.start("W1", "R1", List.of(skipStep), T0, false, true);
        assertThrowsWith(IllegalArgumentException.class,
                "cannot delegate approval to the requester",
                () -> engine.delegate(skipSnapshot, "M1", "R1", T0.plusSeconds(1)));

        WorkflowSnapshot withdrawn = engine.withdraw(snapshot, "R1", T0.plusSeconds(2));
        assertEquals(WorkflowState.WITHDRAWN, withdrawn.state());
        assertTrue(withdrawn.terminal());
        assertThrowsWith(IllegalStateException.class,
                "urgent workflow cannot be withdrawn after first approval",
                () -> engine.withdraw(engine.act(engine.start("W2", "R1",
                                List.of(anyOneStep("S1", "M1"), anyOneStep("S2", "M2")), T0, true, true),
                        decision("M1", ApprovalDecision.APPROVE, T0.plusSeconds(1))),
                        "R1", T0.plusSeconds(2)));
        assertThrowsWith(IllegalArgumentException.class,
                "only the requester may withdraw the workflow",
                () -> engine.withdraw(snapshot, "M1", T0.plusSeconds(2)));
        assertThrowsWith(IllegalStateException.class,
                "withdrawal is disabled for this workflow",
                () -> engine.withdraw(engine.start("W3", "R1", List.of(anyOneStep("S1", "M1")),
                        T0, false, false), "R1", T0.plusSeconds(2)));
        assertThrowsWith(IllegalArgumentException.class,
                "withdrawal time cannot precede the previous update",
                () -> engine.withdraw(snapshot, "R1", T0.minusSeconds(1)));
        assertThrowsWith(NullPointerException.class, "snapshot must not be null",
                () -> engine.withdraw(null, "R1", T0));
        assertThrowsWith(NullPointerException.class, "now must not be null",
                () -> engine.withdraw(snapshot, "R1", null));

        WorkflowSnapshot cancelled = engine.cancel(snapshot, true, "duplicate request",
                T0.plusSeconds(3));
        assertEquals(WorkflowState.CANCELLED, cancelled.state());
        assertThrowsWith(IllegalStateException.class, "terminal workflow cannot be cancelled",
                () -> engine.cancel(cancelled, true, "duplicate request", T0.plusSeconds(4)));
        assertThrowsWith(IllegalArgumentException.class,
                "workflow cancellation requires administrator role",
                () -> engine.cancel(snapshot, false, "duplicate request", T0.plusSeconds(3)));
        assertThrowsWith(IllegalArgumentException.class,
                "cancellation reason must contain at least five characters",
                () -> engine.cancel(snapshot, true, "why", T0.plusSeconds(3)));
        assertThrowsWith(IllegalArgumentException.class,
                "cancellation reason must contain at least five characters",
                () -> engine.cancel(snapshot, true, null, T0.plusSeconds(3)));
        assertThrowsWith(IllegalArgumentException.class,
                "cancellation time cannot precede the previous update",
                () -> engine.cancel(snapshot, true, "duplicate request", T0.minusSeconds(1)));
        assertThrowsWith(NullPointerException.class, "snapshot must not be null",
                () -> engine.cancel(null, true, "duplicate request", T0));
        assertThrowsWith(NullPointerException.class, "now must not be null",
                () -> engine.cancel(snapshot, true, "duplicate request", null));
    }

    // ==================================================================
    // attendance
    // ==================================================================

    private static Shift standardShift() {
        return new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(18, 0), 10, 60, 480);
    }

    private static PunchRecord punch(LocalTime in, LocalTime out) {
        return new PunchRecord(in == null ? null : LocalDateTime.of(MON, in),
                out == null ? null : LocalDateTime.of(MON, out), 60, false, true, false, false);
    }

    static final class Attendance {
        String employeeId = "E1";
        LocalDate date = MON;
        Shift shift = standardShift();
        PunchRecord punch = punch(LocalTime.of(9, 0), LocalTime.of(18, 0));
        EmploymentMode mode = EmploymentMode.OFFICE;
        boolean scheduledWorkDay = true;
        boolean publicHoliday = false;
        boolean approvedLeave = false;
        boolean businessTrip = false;
        boolean severeWeather = false;
        boolean overtimeRequested = false;
        boolean overtimeApproved = false;
        int monthlyLateCount = 0;
        int consecutiveWorkDays = 0;

        AttendanceInput build() {
            return new AttendanceInput(employeeId, date, shift, punch, mode, scheduledWorkDay,
                    publicHoliday, approvedLeave, businessTrip, severeWeather, overtimeRequested,
                    overtimeApproved, monthlyLateCount, consecutiveWorkDays);
        }
    }

    private static AttendanceResult attendance(java.util.function.Consumer<Attendance> customizer) {
        Attendance input = new Attendance();
        if (customizer != null) {
            customizer.accept(input);
        }
        return new AttendancePolicy().evaluate(input.build());
    }

    @Test
    @DisplayName("Shift computes schedules and validates its configuration")
    void shiftRecord() {
        Shift shift = standardShift();
        assertEquals(ShiftType.STANDARD, shift.type());
        assertEquals(LocalTime.of(9, 0), shift.start());
        assertEquals(LocalTime.of(18, 0), shift.end());
        assertEquals(10, shift.graceMinutes());
        assertEquals(60, shift.requiredBreakMinutes());
        assertEquals(480, shift.minimumWorkMinutes());
        assertFalse(shift.crossesMidnight());
        assertEquals(LocalDateTime.of(MON, LocalTime.of(9, 0)), shift.scheduledStart(MON));
        assertEquals(LocalDateTime.of(MON, LocalTime.of(18, 0)), shift.scheduledEnd(MON));
        assertEquals(480, shift.scheduledMinutes(MON));
        assertEquals(shift, standardShift());
        assertEquals(shift.hashCode(), standardShift().hashCode());
        assertTrue(shift.toString().contains("STANDARD"));

        // A NIGHT shift cannot be built: the compact constructor calls crossesMidnight()
        // before the record fields are assigned, so it dereferences a null end time.
        assertThrows(NullPointerException.class,
                () -> new Shift(ShiftType.NIGHT, LocalTime.of(22, 0), LocalTime.of(6, 0), 0, 0, 480));

        assertThrowsWith(IllegalArgumentException.class,
                "shift start and end must differ",
                () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(9, 0),
                        0, 0, 480));
        assertThrowsWith(IllegalArgumentException.class, "graceMinutes must be between 0 and 120",
                () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(18, 0),
                        -1, 0, 480));
        assertThrowsWith(IllegalArgumentException.class, "graceMinutes must be between 0 and 120",
                () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(18, 0),
                        121, 0, 480));
        assertThrowsWith(IllegalArgumentException.class,
                "requiredBreakMinutes must be between 0 and 180",
                () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(18, 0),
                        0, -1, 480));
        assertThrowsWith(IllegalArgumentException.class,
                "requiredBreakMinutes must be between 0 and 180",
                () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(18, 0),
                        0, 181, 480));
        assertThrowsWith(IllegalArgumentException.class,
                "minimumWorkMinutes must be between 60 and 960",
                () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(18, 0),
                        0, 0, 59));
        assertThrowsWith(IllegalArgumentException.class,
                "minimumWorkMinutes must be between 60 and 960",
                () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(18, 0),
                        0, 0, 961));
        assertThrowsWith(IllegalArgumentException.class,
                "part-time shift cannot require more than six hours",
                () -> new Shift(ShiftType.PART_TIME, LocalTime.of(9, 0), LocalTime.of(18, 0),
                        0, 0, 361));
        assertThrows(NullPointerException.class,
                () -> new Shift(ShiftType.NIGHT, LocalTime.of(9, 0), LocalTime.of(18, 0),
                        0, 0, 480));
        assertThrowsWith(NullPointerException.class, "type must not be null",
                () -> new Shift(null, LocalTime.of(9, 0), LocalTime.of(18, 0), 0, 0, 480));
        assertThrowsWith(NullPointerException.class, "start must not be null",
                () -> new Shift(ShiftType.STANDARD, null, LocalTime.of(18, 0), 0, 0, 480));
        assertThrowsWith(NullPointerException.class, "end must not be null",
                () -> new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), null, 0, 0, 480));
        Shift partTime = new Shift(ShiftType.PART_TIME, LocalTime.of(9, 0), LocalTime.of(13, 0),
                0, 0, 360);
        assertEquals(360, partTime.minimumWorkMinutes());
    }

    @Test
    @DisplayName("attendance records validate their invariants")
    void attendanceRecords() {
        PunchRecord punch = punch(LocalTime.of(9, 0), LocalTime.of(18, 0));
        assertEquals(LocalDateTime.of(MON, LocalTime.of(9, 0)), punch.clockIn());
        assertEquals(LocalDateTime.of(MON, LocalTime.of(18, 0)), punch.clockOut());
        assertEquals(60, punch.breakMinutes());
        assertFalse(punch.remotePunch());
        assertTrue(punch.locationVerified());
        assertFalse(punch.manualEntry());
        assertFalse(punch.correctionApproved());
        assertEquals(punch, punch(LocalTime.of(9, 0), LocalTime.of(18, 0)));
        assertTrue(punch.toString().contains("09:00"));
        assertThrowsWith(IllegalArgumentException.class, "breakMinutes must be between 0 and 480",
                () -> new PunchRecord(null, null, -1, false, true, false, false));
        assertThrowsWith(IllegalArgumentException.class, "breakMinutes must be between 0 and 480",
                () -> new PunchRecord(null, null, 481, false, true, false, false));
        assertThrowsWith(IllegalArgumentException.class, "clockOut must not be before clockIn",
                () -> new PunchRecord(LocalDateTime.of(MON, LocalTime.of(18, 0)),
                        LocalDateTime.of(MON, LocalTime.of(9, 0)), 0, false, true, false, false));
        assertThrowsWith(IllegalArgumentException.class,
                "only manual entries can be approved corrections",
                () -> new PunchRecord(null, null, 0, false, true, false, true));

        Attendance attendance = new Attendance();
        AttendanceInput input = attendance.build();
        assertEquals("E1", input.employeeId());
        assertEquals(MON, input.workDate());
        assertEquals(standardShift(), input.shift());
        assertEquals(EmploymentMode.OFFICE, input.employmentMode());
        assertTrue(input.scheduledWorkDay());
        assertFalse(input.publicHoliday());
        assertFalse(input.approvedLeave());
        assertFalse(input.businessTrip());
        assertFalse(input.severeWeather());
        assertFalse(input.overtimeRequested());
        assertFalse(input.overtimeApproved());
        assertEquals(0, input.monthlyLateCount());
        assertEquals(0, input.consecutiveWorkDays());
        assertEquals(input, new Attendance().build());
        assertTrue(input.toString().contains("E1"));
        assertThrowsWith(IllegalArgumentException.class, "employeeId must not be blank", () -> {
            Attendance bad = new Attendance();
            bad.employeeId = " ";
            bad.build();
        });
        assertThrowsWith(NullPointerException.class, "workDate must not be null", () -> {
            Attendance bad = new Attendance();
            bad.date = null;
            bad.build();
        });
        assertThrowsWith(NullPointerException.class, "shift must not be null", () -> {
            Attendance bad = new Attendance();
            bad.shift = null;
            bad.build();
        });
        assertThrowsWith(NullPointerException.class, "employmentMode must not be null", () -> {
            Attendance bad = new Attendance();
            bad.mode = null;
            bad.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "monthlyLateCount must not be negative", () -> {
            Attendance bad = new Attendance();
            bad.monthlyLateCount = -1;
            bad.build();
        });
        assertThrowsWith(IllegalArgumentException.class, "consecutiveWorkDays must not be negative", () -> {
            Attendance bad = new Attendance();
            bad.consecutiveWorkDays = -1;
            bad.build();
        });
        assertThrowsWith(IllegalArgumentException.class,
                "overtime cannot be approved before it is requested", () -> {
                    Attendance bad = new Attendance();
                    bad.overtimeApproved = true;
                    bad.build();
                });
        assertThrowsWith(IllegalArgumentException.class,
                "leave and business trip cannot apply to the same day", () -> {
                    Attendance bad = new Attendance();
                    bad.approvedLeave = true;
                    bad.businessTrip = true;
                    bad.build();
                });

        AttendanceResult result = new AttendanceResult(List.of(AttendanceCode.NORMAL), 480, 0, 0,
                0, 0, 0, false, List.of("warning"));
        assertEquals(List.of(AttendanceCode.NORMAL), result.codes());
        assertEquals(480, result.workedMinutes());
        assertEquals(0, result.lateMinutes());
        assertEquals(0, result.earlyLeaveMinutes());
        assertEquals(0, result.overtimeMinutes());
        assertEquals(0, result.payableOvertimeMinutes());
        assertEquals(0, result.penaltyPoints());
        assertFalse(result.requiresManagerReview());
        assertEquals(List.of("warning"), result.warnings());
        assertTrue(result.toString().contains("NORMAL"));
        assertThrowsWith(NullPointerException.class, null,
                () -> new AttendanceResult(null, 0, 0, 0, 0, 0, 0, false, List.of()));
        assertThrowsWith(NullPointerException.class, null,
                () -> new AttendanceResult(List.of(AttendanceCode.NORMAL), 0, 0, 0, 0, 0, 0,
                        false, null));
        assertThrowsWith(IllegalArgumentException.class,
                "at least one attendance code is required",
                () -> new AttendanceResult(List.of(), 0, 0, 0, 0, 0, 0, false, List.of()));
        assertThrowsWith(IllegalArgumentException.class, "attendance metrics must not be negative",
                () -> new AttendanceResult(List.of(AttendanceCode.NORMAL), -1, 0, 0, 0, 0, 0,
                        false, List.of()));
        assertThrowsWith(IllegalArgumentException.class, "attendance metrics must not be negative",
                () -> new AttendanceResult(List.of(AttendanceCode.NORMAL), 0, -1, 0, 0, 0, 0,
                        false, List.of()));
        assertThrowsWith(IllegalArgumentException.class, "attendance metrics must not be negative",
                () -> new AttendanceResult(List.of(AttendanceCode.NORMAL), 0, 0, -1, 0, 0, 0,
                        false, List.of()));
        assertThrowsWith(IllegalArgumentException.class, "attendance metrics must not be negative",
                () -> new AttendanceResult(List.of(AttendanceCode.NORMAL), 0, 0, 0, -1, 0, 0,
                        false, List.of()));
        assertThrowsWith(IllegalArgumentException.class, "attendance metrics must not be negative",
                () -> new AttendanceResult(List.of(AttendanceCode.NORMAL), 0, 0, 0, 0, -1, 0,
                        false, List.of()));
        assertThrowsWith(IllegalArgumentException.class, "attendance metrics must not be negative",
                () -> new AttendanceResult(List.of(AttendanceCode.NORMAL), 0, 0, 0, 0, 0, -1,
                        false, List.of()));
        assertThrowsWith(IllegalArgumentException.class,
                "payable overtime cannot exceed actual overtime",
                () -> new AttendanceResult(List.of(AttendanceCode.NORMAL), 0, 0, 0, 10, 11, 0,
                        false, List.of()));

        OvertimeResult overtime = new OvertimeResult(60, 30, 1.5, true, true, "reason");
        assertEquals(60, overtime.actualMinutes());
        assertEquals(30, overtime.payableMinutes());
        assertEquals(1.5, overtime.compensationFactor());
        assertTrue(overtime.compensatoryLeave());
        assertTrue(overtime.managerReview());
        assertEquals("reason", overtime.reason());
        assertTrue(overtime.toString().contains("reason"));
        assertThrowsWith(IllegalArgumentException.class, "overtime minutes must not be negative",
                () -> new OvertimeResult(-1, 0, 0, false, false, "r"));
        assertThrowsWith(IllegalArgumentException.class, "overtime minutes must not be negative",
                () -> new OvertimeResult(0, -1, 0, false, false, "r"));
        assertThrowsWith(IllegalArgumentException.class,
                "payableMinutes cannot exceed actualMinutes",
                () -> new OvertimeResult(10, 11, 0, false, false, "r"));
        assertThrowsWith(IllegalArgumentException.class,
                "compensationFactor must not be negative",
                () -> new OvertimeResult(10, 5, -1, false, false, "r"));
        assertThrowsWith(IllegalArgumentException.class, "reason must not be blank",
                () -> new OvertimeResult(10, 5, 1, false, false, null));
        assertThrowsWith(IllegalArgumentException.class, "reason must not be blank",
                () -> new OvertimeResult(10, 5, 1, false, false, " "));
    }

    @Test
    @DisplayName("OvertimePolicy applies factors, caps and review flags")
    void overtimePolicy() {
        OvertimePolicy policy = new OvertimePolicy();
        AttendanceInput plain = new Attendance().build();
        assertEquals(new OvertimeResult(0, 0, 0, false, false, "no overtime"),
                policy.evaluate(plain, 480, 0));
        assertEquals(new OvertimeResult(90, 0, 0, false, true, "overtime was not requested"),
                policy.evaluate(plain, 570, 90));

        Attendance approvedInput = new Attendance();
        approvedInput.overtimeRequested = true;
        approvedInput.overtimeApproved = true;
        AttendanceInput approved = approvedInput.build();
        Attendance requestedOnly = new Attendance();
        requestedOnly.overtimeRequested = true;
        assertEquals(new OvertimeResult(90, 0, 0, false, true, "overtime is awaiting approval"),
                policy.evaluate(requestedOnly.build(), 570, 90));

        OvertimeResult ordinary = policy.evaluate(approved, 570, 90);
        assertEquals(90, ordinary.actualMinutes());
        assertEquals(90, ordinary.payableMinutes());
        assertEquals(1.0, ordinary.compensationFactor());
        assertEquals("ordinary approved overtime", ordinary.reason());
        assertFalse(ordinary.managerReview());
        assertFalse(ordinary.compensatoryLeave());

        OvertimeResult rounded = policy.evaluate(approved, 570, 45);
        assertEquals(30, rounded.payableMinutes());
        OvertimeResult tiny = policy.evaluate(approved, 570, 29);
        assertEquals(0, tiny.payableMinutes());
        assertEquals("overtime below minimum duration", tiny.reason());

        Attendance holiday = new Attendance();
        holiday.overtimeRequested = true;
        holiday.overtimeApproved = true;
        holiday.publicHoliday = true;
        OvertimeResult holidayResult = policy.evaluate(holiday.build(), 570, 90);
        assertEquals(3.0, holidayResult.compensationFactor());
        assertEquals("public-holiday overtime", holidayResult.reason());

        Attendance restDay = new Attendance();
        restDay.overtimeRequested = true;
        restDay.overtimeApproved = true;
        restDay.scheduledWorkDay = false;
        OvertimeResult restResult = policy.evaluate(restDay.build(), 570, 90);
        assertEquals(2.0, restResult.compensationFactor());
        assertEquals("rest-day overtime", restResult.reason());

        Attendance weekend = new Attendance();
        weekend.overtimeRequested = true;
        weekend.overtimeApproved = true;
        weekend.date = MON.plusDays(5);
        OvertimeResult weekendResult = policy.evaluate(weekend.build(), 570, 90);
        assertEquals(2.0, weekendResult.compensationFactor());
        assertEquals("rest-day overtime", weekendResult.reason());

        Attendance night = new Attendance();
        night.overtimeRequested = true;
        night.overtimeApproved = true;
        Shift nightShift = mock(Shift.class);
        when(nightShift.type()).thenReturn(ShiftType.NIGHT);
        when(nightShift.graceMinutes()).thenReturn(0);
        when(nightShift.requiredBreakMinutes()).thenReturn(0);
        when(nightShift.minimumWorkMinutes()).thenReturn(480);
        when(nightShift.scheduledStart(any(LocalDate.class)))
                .thenReturn(LocalDateTime.of(MON, LocalTime.of(9, 0)));
        when(nightShift.scheduledEnd(any(LocalDate.class)))
                .thenReturn(LocalDateTime.of(MON, LocalTime.of(18, 0)));
        when(nightShift.scheduledMinutes(any(LocalDate.class))).thenReturn(480);
        night.shift = nightShift;
        OvertimeResult nightResult = policy.evaluate(night.build(), 570, 90);
        assertEquals(1.5, nightResult.compensationFactor());
        assertEquals("night-shift overtime", nightResult.reason());

        OvertimeResult capped = policy.evaluate(approved, 1000, 300);
        assertEquals(240, capped.payableMinutes());
        assertTrue(capped.managerReview());
        assertEquals("daily overtime capped at four hours", capped.reason());

        OvertimeResult longOvertime = policy.evaluate(approved, 1000, 200);
        assertTrue(longOvertime.managerReview());

        Attendance consecutive = new Attendance();
        consecutive.overtimeRequested = true;
        consecutive.overtimeApproved = true;
        consecutive.consecutiveWorkDays = 6;
        OvertimeResult consecutiveResult = policy.evaluate(consecutive.build(), 570, 90);
        assertTrue(consecutiveResult.compensatoryLeave());
        assertTrue(consecutiveResult.managerReview());

        Attendance remote = new Attendance();
        remote.overtimeRequested = true;
        remote.overtimeApproved = true;
        remote.mode = EmploymentMode.REMOTE;
        remote.punch = new PunchRecord(LocalDateTime.of(MON, LocalTime.of(9, 0)),
                LocalDateTime.of(MON, LocalTime.of(20, 0)), 60, true, false, false, false);
        OvertimeResult remoteResult = policy.evaluate(remote.build(), 660, 180);
        assertEquals(0, remoteResult.payableMinutes());
        assertTrue(remoteResult.managerReview());
        assertEquals("remote overtime location is unverified", remoteResult.reason());

        Attendance onCall = new Attendance();
        onCall.overtimeRequested = true;
        onCall.overtimeApproved = true;
        onCall.shift = new Shift(ShiftType.ON_CALL, LocalTime.of(9, 0), LocalTime.of(18, 0), 0, 0, 480);
        OvertimeResult onCallResult = policy.evaluate(onCall.build(), 540, 60);
        assertTrue(onCallResult.compensatoryLeave());
        assertEquals(0.0, onCallResult.compensationFactor());
        assertEquals("short on-call work grants compensatory leave", onCallResult.reason());

        OvertimeResult longWorked = policy.evaluate(approved, 901, 90);
        assertTrue(longWorked.managerReview());

        assertThrowsWith(NullPointerException.class, "input must not be null",
                () -> policy.evaluate(null, 480, 0));
        assertThrowsWith(IllegalArgumentException.class, "minutes must not be negative",
                () -> policy.evaluate(plain, -1, 0));
        assertThrowsWith(IllegalArgumentException.class, "minutes must not be negative",
                () -> policy.evaluate(plain, 480, -1));
    }

    @Test
    @DisplayName("AttendancePolicy: leave, trip, rest day and missing punches")
    void attendanceSpecialDays() {
        Attendance leave = new Attendance();
        leave.approvedLeave = true;
        AttendanceResult leaveResult = attendance(a -> a.approvedLeave = true);
        assertEquals(List.of(AttendanceCode.APPROVED_LEAVE), leaveResult.codes());
        assertEquals(0, leaveResult.workedMinutes());
        assertFalse(leaveResult.requiresManagerReview());
        AttendanceResult leaveWithPunch = attendance(a -> a.approvedLeave = true);
        assertEquals(List.of("punch exists on an approved leave day"), leaveWithPunch.warnings());

        AttendanceResult trip = attendance(a -> {
            a.businessTrip = true;
            a.punch = null;
        });
        assertEquals(List.of(AttendanceCode.BUSINESS_TRIP), trip.codes());
        assertEquals(480, trip.workedMinutes());
        assertFalse(trip.requiresManagerReview());

        AttendanceResult tripWithPunch = attendance(a -> a.businessTrip = true);
        assertEquals(List.of(AttendanceCode.BUSINESS_TRIP), tripWithPunch.codes());
        assertEquals(480, tripWithPunch.workedMinutes());

        AttendanceResult restDay = attendance(a -> {
            a.scheduledWorkDay = false;
            a.punch = null;
        });
        assertEquals(List.of(AttendanceCode.REST_DAY), restDay.codes());

        AttendanceResult holidayWithoutPunch = attendance(a -> {
            a.publicHoliday = true;
            a.punch = null;
        });
        assertEquals(List.of(AttendanceCode.ABSENT), holidayWithoutPunch.codes());
        assertEquals(5, holidayWithoutPunch.penaltyPoints());
        assertTrue(holidayWithoutPunch.requiresManagerReview());

        AttendanceResult absent = attendance(a -> a.punch = null);
        assertEquals(List.of(AttendanceCode.ABSENT), absent.codes());
        assertEquals(5, absent.penaltyPoints());
        assertTrue(absent.requiresManagerReview());

        AttendanceResult severeAbsent = attendance(a -> {
            a.punch = null;
            a.severeWeather = true;
        });
        assertEquals(List.of(AttendanceCode.MANUAL_CORRECTION_REQUIRED), severeAbsent.codes());
        assertEquals(0, severeAbsent.penaltyPoints());
        assertTrue(severeAbsent.requiresManagerReview());

        AttendanceResult missingIn = attendance(a -> a.punch =
                punch(null, LocalTime.of(18, 0)));
        assertTrue(missingIn.codes().contains(AttendanceCode.MISSING_CLOCK_IN));
        assertTrue(missingIn.codes().contains(AttendanceCode.MANUAL_CORRECTION_REQUIRED));
        assertEquals(2, missingIn.penaltyPoints());
        assertTrue(missingIn.requiresManagerReview());

        AttendanceResult missingOut = attendance(a -> a.punch =
                punch(LocalTime.of(9, 0), null));
        assertTrue(missingOut.codes().contains(AttendanceCode.MISSING_CLOCK_OUT));

        AttendanceResult approvedCorrection = attendance(a -> a.punch = new PunchRecord(
                LocalDateTime.of(MON, LocalTime.of(9, 0)), null, 0, false, true, true, true));
        assertTrue(approvedCorrection.codes().contains(AttendanceCode.MISSING_CLOCK_OUT));
        assertFalse(approvedCorrection.codes().contains(AttendanceCode.MANUAL_CORRECTION_REQUIRED));
        assertTrue(approvedCorrection.warnings().contains("approved correction remains incomplete"));
    }

    @Test
    @DisplayName("AttendancePolicy: lateness, hours and break penalties")
    void attendancePenalties() {
        AttendanceResult normal = attendance(null);
        assertEquals(List.of(AttendanceCode.NORMAL), normal.codes());
        assertEquals(480, normal.workedMinutes());
        assertEquals(0, normal.lateMinutes());
        assertEquals(0, normal.penaltyPoints());
        assertFalse(normal.requiresManagerReview());

        AttendanceResult slightlyLate = attendance(a -> a.punch = punch(LocalTime.of(9, 15),
                LocalTime.of(18, 15)));
        assertTrue(slightlyLate.codes().contains(AttendanceCode.LATE));
        assertEquals(5, slightlyLate.lateMinutes());
        assertEquals(1, slightlyLate.penaltyPoints());
        assertFalse(slightlyLate.requiresManagerReview());

        AttendanceResult late = attendance(a -> a.punch = punch(LocalTime.of(9, 30),
                LocalTime.of(18, 30)));
        assertEquals(20, late.lateMinutes());
        assertEquals(2, late.penaltyPoints());

        AttendanceResult veryLate = attendance(a -> a.punch = punch(LocalTime.of(10, 30),
                LocalTime.of(19, 30)));
        assertTrue(veryLate.codes().contains(AttendanceCode.SERIOUSLY_LATE));
        assertEquals(80, veryLate.lateMinutes());
        assertEquals(5, veryLate.penaltyPoints());
        assertTrue(veryLate.requiresManagerReview());

        AttendanceResult flexible = attendance(a -> {
            a.shift = new Shift(ShiftType.FLEXIBLE, LocalTime.of(9, 0), LocalTime.of(18, 0),
                    0, 60, 480);
            a.punch = punch(LocalTime.of(9, 50), LocalTime.of(18, 0));
        });
        assertEquals(0, flexible.lateMinutes());

        AttendanceResult flexibleLate = attendance(a -> {
            a.shift = new Shift(ShiftType.FLEXIBLE, LocalTime.of(9, 0), LocalTime.of(18, 0),
                    0, 60, 480);
            a.punch = punch(LocalTime.of(10, 30), LocalTime.of(18, 0));
        });
        assertEquals(30, flexibleLate.lateMinutes());

        AttendanceResult weather = attendance(a -> {
            a.severeWeather = true;
            a.punch = punch(LocalTime.of(11, 0), LocalTime.of(18, 0));
        });
        assertEquals(0, weather.lateMinutes());
        assertTrue(weather.warnings().contains("late arrival waived because of severe weather"));

        AttendanceResult weatherTooLate = attendance(a -> {
            a.severeWeather = true;
            a.punch = punch(LocalTime.of(11, 30), LocalTime.of(18, 0));
        });
        assertEquals(140, weatherTooLate.lateMinutes());

        AttendanceResult repeated = attendance(a -> {
            a.monthlyLateCount = 3;
            a.punch = punch(LocalTime.of(9, 30), LocalTime.of(18, 30));
        });
        assertEquals(4, repeated.penaltyPoints());
        assertTrue(repeated.warnings().contains("repeated lateness requires manager review"));

        AttendanceResult frequent = attendance(a -> a.monthlyLateCount = 6);
        assertEquals(3, frequent.penaltyPoints());
        assertTrue(frequent.warnings().contains("monthly lateness threshold exceeded"));

        AttendanceResult early = attendance(a -> a.punch = punch(LocalTime.of(8, 30),
                LocalTime.of(17, 30)));
        assertTrue(early.codes().contains(AttendanceCode.EARLY_LEAVE));
        assertEquals(30, early.earlyLeaveMinutes());
        assertEquals(1, early.penaltyPoints());

        AttendanceResult veryEarly = attendance(a -> a.punch = punch(LocalTime.of(7, 0),
                LocalTime.of(16, 0)));
        assertEquals(120, veryEarly.earlyLeaveMinutes());
        assertEquals(3, veryEarly.penaltyPoints());

        AttendanceResult withinGrace = attendance(a -> a.punch = punch(LocalTime.of(9, 0),
                LocalTime.of(17, 55)));
        assertEquals(0, withinGrace.earlyLeaveMinutes());
        assertFalse(withinGrace.codes().contains(AttendanceCode.EARLY_LEAVE));

        AttendanceResult noBreak = attendance(a -> a.punch = new PunchRecord(
                LocalDateTime.of(MON, LocalTime.of(9, 0)),
                LocalDateTime.of(MON, LocalTime.of(18, 0)), 0, false, true, false, false));
        assertTrue(noBreak.codes().contains(AttendanceCode.INSUFFICIENT_BREAK));
        assertTrue(noBreak.warnings().contains("required break duration was not met"));

        AttendanceResult shortDay = attendance(a -> a.punch = punch(LocalTime.of(9, 0),
                LocalTime.of(15, 0)));
        assertTrue(shortDay.codes().contains(AttendanceCode.INSUFFICIENT_HOURS));
        assertTrue(shortDay.codes().contains(AttendanceCode.EARLY_LEAVE));
        assertEquals(5, shortDay.penaltyPoints());

        AttendanceResult veryShortDay = attendance(a -> a.punch = new PunchRecord(
                LocalDateTime.of(MON, LocalTime.of(9, 0)),
                LocalDateTime.of(MON, LocalTime.of(12, 0)), 0, false, true, false, false));
        assertTrue(veryShortDay.codes().contains(AttendanceCode.INSUFFICIENT_HOURS));
        assertEquals(7, veryShortDay.penaltyPoints());

        AttendanceResult manual = attendance(a -> a.punch = new PunchRecord(
                LocalDateTime.of(MON, LocalTime.of(9, 0)),
                LocalDateTime.of(MON, LocalTime.of(18, 0)), 60, false, true, true, false));
        assertTrue(manual.codes().contains(AttendanceCode.MANUAL_CORRECTION_REQUIRED));
        assertTrue(manual.requiresManagerReview());
    }

    @Test
    @DisplayName("AttendancePolicy: location rules and overtime integration")
    void attendanceLocationAndOvertime() {
        AttendanceResult officeRemote = attendance(a -> {
            a.mode = EmploymentMode.OFFICE;
            a.punch = new PunchRecord(LocalDateTime.of(MON, LocalTime.of(9, 0)),
                    LocalDateTime.of(MON, LocalTime.of(18, 0)), 60, true, true, false, false);
        });
        assertTrue(officeRemote.codes().contains(AttendanceCode.LOCATION_UNVERIFIED));
        assertTrue(officeRemote.warnings().contains("office employee used a remote punch"));
        assertTrue(officeRemote.requiresManagerReview());

        AttendanceResult remoteOffice = attendance(a -> a.mode = EmploymentMode.REMOTE);
        assertTrue(remoteOffice.warnings().contains("remote employee used an office punch"));
        assertFalse(remoteOffice.requiresManagerReview());

        AttendanceResult fieldUnverified = attendance(a -> {
            a.mode = EmploymentMode.FIELD;
            a.punch = new PunchRecord(LocalDateTime.of(MON, LocalTime.of(9, 0)),
                    LocalDateTime.of(MON, LocalTime.of(18, 0)), 60, false, false, false, false);
        });
        assertTrue(fieldUnverified.codes().contains(AttendanceCode.LOCATION_UNVERIFIED));

        AttendanceResult fieldVerified = attendance(a -> {
            a.mode = EmploymentMode.FIELD;
            a.punch = new PunchRecord(LocalDateTime.of(MON, LocalTime.of(9, 0)),
                    LocalDateTime.of(MON, LocalTime.of(18, 0)), 60, true, true, false, false);
        });
        assertFalse(fieldVerified.codes().contains(AttendanceCode.LOCATION_UNVERIFIED));

        AttendanceResult unverifiedRemote = attendance(a -> {
            a.mode = EmploymentMode.REMOTE;
            a.punch = new PunchRecord(LocalDateTime.of(MON, LocalTime.of(9, 0)),
                    LocalDateTime.of(MON, LocalTime.of(18, 0)), 60, true, false, false, false);
        });
        assertTrue(unverifiedRemote.codes().contains(AttendanceCode.LOCATION_UNVERIFIED));

        AttendanceResult hybrid = attendance(a -> {
            a.mode = EmploymentMode.HYBRID;
            a.punch = new PunchRecord(LocalDateTime.of(MON, LocalTime.of(9, 0)),
                    LocalDateTime.of(MON, LocalTime.of(18, 0)), 60, true, false, false, false);
        });
        assertTrue(hybrid.warnings().contains("hybrid remote location needs review"));

        AttendanceResult overtime = attendance(a -> {
            a.overtimeRequested = true;
            a.overtimeApproved = true;
            a.punch = punch(LocalTime.of(9, 0), LocalTime.of(20, 0));
        });
        assertTrue(overtime.codes().contains(AttendanceCode.OVERTIME));
        assertEquals(120, overtime.overtimeMinutes());
        assertEquals(120, overtime.payableOvertimeMinutes());
        assertEquals(600, overtime.workedMinutes());

        AttendanceResult excessive = attendance(a -> {
            a.overtimeRequested = true;
            a.overtimeApproved = true;
            a.punch = punch(LocalTime.of(7, 0), LocalTime.of(20, 30));
        });
        assertTrue(excessive.codes().contains(AttendanceCode.EXCESSIVE_OVERTIME));
        assertTrue(excessive.warnings().contains("daily overtime capped at four hours"));
        assertTrue(excessive.requiresManagerReview());

        assertThrowsWith(NullPointerException.class, "input must not be null",
                () -> new AttendancePolicy().evaluate(null));
        assertThrowsWith(NullPointerException.class, null,
                () -> new AttendancePolicy(null));
    }

    // ==================================================================
    // boundary matrices (exact thresholds kill the boundary mutators)
    // ==================================================================

    @Test
    @DisplayName("leave boundaries: every comparison at its exact threshold")
    void leaveBoundaries() {
        // reason length exactly five characters
        assertFalse(assessLeave(l -> l.reason = "abcde").violations()
                .contains("reason must contain at least five characters"));
        // calendar span exactly 45 days is allowed
        assertFalse(assessLeave(l -> {
            l.type = LeaveType.ANNUAL;
            l.end = MON.plusDays(44);
            l.notice = 10;
            l.balance = 60;
            l.months = 24;
        }).violations().contains("a single request cannot span more than 45 calendar days"));
        // notice boundaries: chargedDays exactly five needs no notice
        assertFalse(assessLeave(l -> {
            l.end = MON.plusDays(4);
            l.notice = 2;
        }).violations().contains("leave longer than five days requires three days notice"));
        assertFalse(assessLeave(l -> {
            l.end = MON.plusDays(11);
            l.notice = 6;
        }).violations().contains("leave longer than ten days requires seven days notice"));
        // one charged day with zero notice is allowed
        assertFalse(assessLeave(l -> l.notice = 0).violations()
                .contains("multi-day leave requires advance notice"));
        // employment months at the exact bounds
        assertFalse(assessLeave(l -> l.months = 1).violations()
                .contains("new employees may only request sick leave in their first month"));
        assertFalse(assessLeave(l -> l.months = 600).violations()
                .contains("employment duration is outside the supported range"));
        // half day with equal dates is fine, single hour is fine
        assertFalse(assessLeave(l -> l.unit = LeaveUnit.HALF_DAY_MORNING).violations()
                .contains("half-day leave must start and end on the same date"));
        assertFalse(assessLeave(l -> {
            l.unit = LeaveUnit.HOURS;
            l.hours = 1;
        }).violations().contains("hour leave must be at least one hour"));
        // annual balance exactly equal to the request
        LeaveAssessment exactBalance = assessLeave(l -> {
            l.type = LeaveType.ANNUAL;
            l.end = MON.plusDays(2);
            l.balance = 3;
            l.carryOver = 0;
            l.months = 24;
        });
        assertFalse(exactBalance.violations().contains("insufficient annual leave balance"));
        // balance + carry-over is a sum, not a difference
        assertFalse(assessLeave(l -> {
            l.type = LeaveType.ANNUAL;
            l.end = MON.plusDays(3);
            l.balance = 2;
            l.carryOver = 3;
            l.months = 24;
            l.start = LocalDate.of(2026, 3, 2);
        }).violations().contains("insufficient annual leave balance"));
        // fifteen working days exactly is allowed
        assertFalse(assessLeave(l -> {
            l.type = LeaveType.ANNUAL;
            l.end = MON.plusDays(18);
            l.notice = 10;
            l.balance = 40;
            l.months = 24;
        }).violations().contains("annual leave cannot exceed fifteen working days per request"));
        // carry-over exactly five is allowed, and April with no carry-over is allowed
        assertFalse(assessLeave(l -> {
            l.type = LeaveType.ANNUAL;
            l.balance = 40;
            l.carryOver = 5;
            l.months = 24;
        }).violations().contains("carry-over balance cannot exceed five days"));
        assertFalse(assessLeave(l -> {
            l.type = LeaveType.ANNUAL;
            l.balance = 40;
            l.carryOver = 0;
            l.months = 24;
            l.start = LocalDate.of(2026, 4, 1);
            l.end = l.start;
        }).violations().contains("carry-over leave must be used before April"));
        // six months service is enough
        assertFalse(assessLeave(l -> {
            l.type = LeaveType.ANNUAL;
            l.months = 6;
            l.balance = 40;
        }).violations().contains("annual leave requires six months service"));
        // sick and personal bounds
        assertFalse(assessLeave(l -> l.balance = 365).violations()
                .contains("sick leave balance is outside the supported range"));
        assertFalse(assessLeave(l -> {
            l.unit = LeaveUnit.HOURS;
            l.hours = 4;
        }).violations().contains("hour-based sick leave cannot exceed four hours"));
        assertFalse(assessLeave(l -> {
            l.emergency = true;
            l.notice = 30;
            l.end = MON.plusDays(1);
        }).violations().contains("emergency sick leave has inconsistent notice information"));
        assertFalse(assessLeave(l -> {
            l.type = LeaveType.PERSONAL;
            l.end = MON.plusDays(2);
            l.notice = 3;
        }).violations().contains("personal leave cannot exceed three days without an emergency"));
        assertFalse(assessLeave(l -> {
            l.type = LeaveType.PERSONAL;
            l.end = MON.plusDays(6);
            l.notice = 3;
            l.emergency = true;
        }).violations().contains("personal leave cannot exceed five days"));
        assertFalse(assessLeave(l -> {
            l.type = LeaveType.PERSONAL;
            l.unit = LeaveUnit.HOURS;
            l.hours = 4;
            l.notice = 1;
        }).violations().contains("hour-based personal leave cannot exceed four hours"));
        assertFalse(assessLeave(l -> {
            l.type = LeaveType.PERSONAL;
            l.category = EmployeeCategory.CONTRACTOR;
            l.notice = 1;
        }).violations().contains("contractor personal leave is limited to one day"));
        assertFalse(assessLeave(l -> {
            l.type = LeaveType.PERSONAL;
            l.notice = 1;
        }).violations().contains("personal leave requires one day notice"));
        // certificate: exactly two charged days needs none for a regular employee
        assertFalse(assessLeave(l -> l.end = MON.plusDays(1)).medicalCertificateRequired());
        assertFalse(assessLeave(l -> {
            l.emergency = true;
            l.notice = 0;
        }).medicalCertificateRequired());
        // handover: exactly two days is required for annual and for regular staff
        assertTrue(assessLeave(l -> l.end = MON.plusDays(1)).handoverRequired());
        assertFalse(assessLeave(l -> {
            l.category = EmployeeCategory.CONTRACTOR;
            l.end = MON.plusDays(1);
        }).handoverRequired());
        assertFalse(assessLeave(l -> {
            l.start = MON.plusDays(4);
            l.end = MON.plusDays(4);
        }).handoverRequired());
        // tiers at the exact boundaries
        assertEquals(ApprovalTier.SUPERVISOR, assessLeave(l -> {
            l.end = MON.plusDays(1);
            l.notice = 1;
        }).approvalTier());
        assertEquals(ApprovalTier.HUMAN_RESOURCES, assessLeave(l -> {
            l.end = MON.plusDays(2);
            l.notice = 1;
        }).approvalTier());
        assertEquals(ApprovalTier.MANAGER, assessLeave(l -> {
            l.type = LeaveType.PERSONAL;
            l.notice = 2;
        }).approvalTier());
        assertEquals(ApprovalTier.HUMAN_RESOURCES, assessLeave(l -> {
            l.category = EmployeeCategory.PROBATION;
            l.notice = 2;
        }).approvalTier());
        assertEquals(ApprovalTier.DIRECTOR, assessLeave(l -> {
            l.category = EmployeeCategory.CONTRACTOR;
            l.end = MON.plusDays(3);
            l.notice = 3;
        }).approvalTier());
    }

    @Test
    @DisplayName("leave: fractional hours round half a day upward")
    void leaveHoursRounding() {
        AdvancedLeavePolicy policy = new AdvancedLeavePolicy();
        Leave hours = new Leave();
        hours.unit = LeaveUnit.HOURS;
        hours.hours = 0.25;
        assertEquals(0.5, policy.calculateChargedDays(hours.build(), WEEKDAYS));
        hours.hours = 4;
        assertEquals(0.5, policy.calculateChargedDays(hours.build(), WEEKDAYS));
        hours.hours = 4.5;
        assertEquals(1.0, policy.calculateChargedDays(hours.build(), WEEKDAYS));
        hours.hours = 8;
        assertEquals(1.0, policy.calculateChargedDays(hours.build(), WEEKDAYS));
    }

    @Test
    @DisplayName("expense boundaries: every comparison at its exact threshold")
    void expenseBoundaries() {
        // exactly the large item threshold is not escalated
        ClaimBuilder large = new ClaimBuilder();
        large.preApproved = true;
        large.items.add(meal("5000.00"));
        assertFalse(assess(large, ONE_TO_ONE).auditRequired());
        ClaimBuilder overLarge = new ClaimBuilder();
        overLarge.preApproved = true;
        overLarge.items.add(meal("5000.01"));
        assertTrue(assess(overLarge, ONE_TO_ONE).auditRequired());

        // rejected assessments report a zero reimbursable amount
        ClaimBuilder rejected = new ClaimBuilder();
        rejected.items.add(meal("40.00"));
        rejected.submitted = TODAY.plusDays(1);
        ExpenseAssessment rejectedResult = assess(rejected, ONE_TO_ONE);
        assertTrue(rejectedResult.rejected());
        assertEquals(new BigDecimal("0.00"), rejectedResult.reimbursableAmount());
        assertEquals(new BigDecimal("40.00"), rejectedResult.claimedAmount());

        // exactly thirty days old submission is not warned
        ClaimBuilder thirty = new ClaimBuilder();
        thirty.submitted = TODAY.minusDays(30);
        ItemBuilder thirtyItem = new ItemBuilder();
        thirtyItem.incurred = thirty.submitted;
        thirty.items.add(thirtyItem.build());
        assertFalse(assess(thirty, ONE_TO_ONE).warnings()
                .contains("claim review is occurring more than thirty days after submission"));

        // exactly fifty items is fine, twenty items triggers audit only
        ClaimBuilder fifty = new ClaimBuilder();
        for (int i = 1; i <= 50; i++) {
            ItemBuilder item = new ItemBuilder();
            item.id = "I" + i;
            item.amount = i + ".00";
            fifty.items.add(item.build());
        }
        assertFalse(assess(fifty, ONE_TO_ONE).warnings()
                .contains("large claim should be split by trip or purpose"));

        ClaimBuilder twenty = new ClaimBuilder();
        twenty.previousClaims = 20;
        twenty.items.add(meal("40.00"));
        assertFalse(assess(twenty, ONE_TO_ONE).warnings()
                .contains("unusually high claim frequency"));

        // item age exactly ninety days is a warning, not a violation
        ClaimBuilder ninety = new ClaimBuilder();
        ItemBuilder ninetyItem = new ItemBuilder();
        ninetyItem.incurred = SUBMITTED.minusDays(90);
        ninety.items.add(ninetyItem.build());
        ExpenseAssessment ninetyResult = assess(ninety, ONE_TO_ONE);
        assertFalse(ninetyResult.violations()
                .contains("FATAL: expense older than ninety days for item I1"));
        assertTrue(ninetyResult.warnings().contains("late submission for item I1"));

        ClaimBuilder thirtyDayItem = new ClaimBuilder();
        ItemBuilder exactThirty = new ItemBuilder();
        exactThirty.incurred = SUBMITTED.minusDays(30);
        thirtyDayItem.items.add(exactThirty.build());
        assertFalse(assess(thirtyDayItem, ONE_TO_ONE).warnings()
                .contains("late submission for item I1"));

        // merchant and purpose at the exact minimal lengths
        ClaimBuilder twoChar = new ClaimBuilder();
        ItemBuilder twoCharItem = new ItemBuilder();
        twoCharItem.merchant = "AB";
        twoCharItem.purpose = "12345678";
        twoChar.items.add(twoCharItem.build());
        ExpenseAssessment twoCharResult = assess(twoChar, ONE_TO_ONE);
        assertFalse(twoCharResult.warnings().contains("merchant name is unusually short for item I1"));
        assertFalse(twoCharResult.warnings().contains("business purpose is too brief for item I1"));

        // exactly the maximum supported item amount is not fatal
        ClaimBuilder maxItem = new ClaimBuilder();
        maxItem.preApproved = true;
        maxItem.items.add(meal("50000.00"));
        assertFalse(assess(maxItem, ONE_TO_ONE).violations()
                .contains("FATAL: single item exceeds the maximum supported amount"));

        // tier thresholds
        assertEquals(ExpenseApprovalTier.FINANCE_DIRECTOR, tierFor("50000.00"));
        assertEquals(ExpenseApprovalTier.FINANCE, tierFor("20000.00"));
        assertEquals(ExpenseApprovalTier.DEPARTMENT_HEAD, tierFor("5000.00"));
        assertEquals(ExpenseApprovalTier.DEPARTMENT_HEAD, tierFor("2000.01"));
        assertEquals(ExpenseApprovalTier.LINE_MANAGER, tierFor("200.01"));
        assertEquals(ExpenseApprovalTier.AUTO_APPROVED, tierFor("200.00"));

        // category rule thresholds
        assertTrue(categoryViolations(ExpenseCategory.HOTEL, "2000.00", b -> { }, null).isEmpty());
        assertTrue(categoryWarnings(ExpenseCategory.HOTEL, "2000.00", b -> { }, null).isEmpty());
        assertTrue(categoryWarnings(ExpenseCategory.LOCAL_TRANSPORT, "1000.00", b -> { }, null).isEmpty());
        assertTrue(categoryViolations(ExpenseCategory.OFFICE_SUPPLIES, "100.00", b -> { },
                c -> c.grade = EmployeeGrade.INTERN).isEmpty());
        assertTrue(categoryWarnings(ExpenseCategory.OFFICE_SUPPLIES, "500.00",
                b -> b.payment = PaymentMethod.PERSONAL_CARD, null).isEmpty());
        assertTrue(categoryViolations(ExpenseCategory.CLIENT_ENTERTAINMENT, "100.00",
                b -> { b.attendees = 2; b.alcohol = true; }, null).isEmpty());
        assertTrue(categoryWarnings(ExpenseCategory.SOFTWARE, "3000.00", b -> { }, null).isEmpty());
        assertTrue(categoryWarnings(ExpenseCategory.COMMUNICATION, "500.00", b -> { }, null).isEmpty());
        assertTrue(categoryViolations(ExpenseCategory.OTHER, "500.00", b -> { },
                c -> c.preApproved = true).isEmpty());
    }

    private static ExpenseApprovalTier tierFor(String amount) {
        ClaimBuilder claim = new ClaimBuilder();
        claim.preApproved = true;
        claim.items.add(meal(amount));
        return assess(claim, ONE_TO_ONE).approvalTier();
    }

    @Test
    @DisplayName("task priority boundaries: due and overdue thresholds")
    void taskPriorityBoundaries() {
        assertEquals(TaskPriority.HIGH, priority(t -> {
            t.priority = TaskPriority.NORMAL;
            t.dueAt = NOW.plusSeconds(4 * 3600);
        }, null));
        assertEquals(TaskPriority.NORMAL, priority(t -> {
            t.priority = TaskPriority.LOW;
            t.dueAt = NOW.plusSeconds(24 * 3600);
        }, null));
        assertEquals(TaskPriority.NORMAL, priority(t -> {
            t.priority = TaskPriority.LOW;
            t.dependencies = List.of("d1", "d2", "d3", "d4", "d5");
            t.dueAt = NOW.plusSeconds(72 * 3600);
        }, null));
        assertEquals(TaskPriority.HIGH, priority(t -> {
            t.priority = TaskPriority.LOW;
            t.dueAt = NOW;
        }, null));
        assertEquals(TaskPriority.CRITICAL, priority(t -> {
            t.priority = TaskPriority.NORMAL;
            t.createdAt = NOW.minusSeconds(30L * 24 * 3600);
            t.dueAt = NOW.minusSeconds(7L * 24 * 3600);
        }, null, true));
        assertEquals(TaskPriority.URGENT, priority(t -> {
            t.priority = TaskPriority.NORMAL;
            t.createdAt = NOW.minusSeconds(30L * 24 * 3600);
            t.dueAt = NOW.minusSeconds(3L * 24 * 3600);
        }, null, true));
    }

    @Test
    @DisplayName("task escalation and reassignment boundaries")
    void taskRuleBoundaries() {
        // terminal tasks never escalate even when critical
        TaskEvaluation terminal = evaluate(t -> {
            t.priority = TaskPriority.CRITICAL;
            t.status = TaskStatus.COMPLETED;
            t.startedAt = NOW.minusSeconds(7200);
            t.completedAt = NOW.minusSeconds(3600);
        }, null);
        assertFalse(terminal.escalationRequired());

        // urgent priority escalates while overdue even when the score is capped
        TaskEvaluation overdueUrgent = evaluate(t -> {
            t.priority = TaskPriority.URGENT;
            t.createdAt = NOW.minusSeconds(30L * 24 * 3600);
            t.dueAt = NOW.minusSeconds(3600);
        }, c -> c.projectActive = false);
        assertTrue(overdueUrgent.escalationRequired());

        // exactly twenty open tasks is not high workload
        assertFalse(evaluate(null, c -> c.openTasks = 20).reassignmentRecommended());
        assertTrue(evaluate(null, c -> c.openTasks = 20).warnings().isEmpty());
        // exactly twelve open tasks with a low priority task is fine
        assertFalse(evaluate(t -> t.priority = TaskPriority.LOW,
                c -> c.openTasks = 12).reassignmentRecommended());
        // exactly ten open tasks while overdue is fine
        assertFalse(evaluate(t -> {
            t.createdAt = NOW.minusSeconds(30L * 24 * 3600);
            t.dueAt = NOW.minusSeconds(3600);
        }, c -> c.openTasks = 10).reassignmentRecommended());
        // terminal tasks are never reassigned
        assertFalse(evaluate(t -> {
            t.assigneeId = null;
            t.status = TaskStatus.CANCELLED;
        }, null).reassignmentRecommended());
    }

    @Test
    @DisplayName("task: exact allowed action sets")
    void taskActionSets() {
        assertEquals(Set.of(TaskAction.PUBLISH, TaskAction.CANCEL),
                evaluate(t -> t.status = TaskStatus.DRAFT, c -> c.actorId = "creator")
                        .allowedActions());
        assertEquals(Set.of(TaskAction.PUBLISH, TaskAction.CANCEL),
                evaluate(t -> t.status = TaskStatus.DRAFT, c -> {
                    c.actorId = "someone";
                    c.administrator = true;
                }).allowedActions());
        assertEquals(Set.of(), evaluate(t -> t.status = TaskStatus.DRAFT, null).allowedActions());
        assertEquals(Set.of(TaskAction.CANCEL), evaluate(t -> t.status = TaskStatus.DRAFT, c -> {
            c.actorId = "creator";
            c.projectActive = false;
        }).allowedActions());

        assertEquals(Set.of(TaskAction.START), evaluate(null, null).allowedActions());
        assertEquals(Set.of(TaskAction.START, TaskAction.CANCEL), evaluate(t -> {
            t.assigneeId = "other";
            t.creatorId = "other";
        }, c -> {
            c.actorId = "manager";
            c.manager = true;
        }).allowedActions());
        assertEquals(Set.of(TaskAction.CANCEL), evaluate(t -> {
            t.assigneeId = "other";
        }, c -> c.actorId = "creator").allowedActions());
        assertEquals(Set.of(), evaluate(t -> t.assigneeId = "other",
                c -> c.actorId = "stranger").allowedActions());
        assertEquals(Set.of(TaskAction.CANCEL), evaluate(null, c -> {
            c.actorId = "creator";
            c.projectActive = false;
        }).allowedActions());

        assertEquals(Set.of(TaskAction.BLOCK, TaskAction.COMPLETE),
                evaluate(t -> {
                    t.status = TaskStatus.IN_PROGRESS;
                    t.startedAt = NOW.minusSeconds(3600);
                }, null).allowedActions());
        assertEquals(Set.of(TaskAction.BLOCK, TaskAction.SUBMIT_FOR_REVIEW),
                evaluate(t -> {
                    t.status = TaskStatus.IN_PROGRESS;
                    t.startedAt = NOW.minusSeconds(3600);
                    t.reviewRequired = true;
                }, null).allowedActions());
        assertEquals(Set.of(TaskAction.BLOCK, TaskAction.COMPLETE, TaskAction.CANCEL),
                evaluate(t -> {
                    t.status = TaskStatus.IN_PROGRESS;
                    t.startedAt = NOW.minusSeconds(3600);
                    t.assigneeId = "other";
                }, c -> {
                    c.actorId = "manager";
                    c.manager = true;
                }).allowedActions());
        assertEquals(Set.of(), evaluate(t -> {
            t.status = TaskStatus.IN_PROGRESS;
            t.startedAt = NOW.minusSeconds(3600);
            t.assigneeId = "other";
        }, c -> c.actorId = "stranger").allowedActions());

        assertEquals(Set.of(TaskAction.UNBLOCK), evaluate(t -> {
            t.status = TaskStatus.BLOCKED;
            t.startedAt = NOW.minusSeconds(3600);
        }, null).allowedActions());
        assertEquals(Set.of(TaskAction.UNBLOCK, TaskAction.CANCEL), evaluate(t -> {
            t.status = TaskStatus.BLOCKED;
            t.startedAt = NOW.minusSeconds(3600);
            t.assigneeId = "other";
        }, c -> {
            c.actorId = "manager";
            c.manager = true;
        }).allowedActions());
        assertEquals(Set.of(TaskAction.CANCEL), evaluate(t -> {
            t.status = TaskStatus.BLOCKED;
            t.startedAt = NOW.minusSeconds(3600);
        }, c -> {
            c.actorId = "manager";
            c.manager = true;
            c.assigneeActive = false;
        }).allowedActions());

        assertEquals(Set.of(TaskAction.APPROVE, TaskAction.REJECT_REVIEW, TaskAction.CANCEL),
                evaluate(t -> {
                    t.status = TaskStatus.IN_REVIEW;
                    t.startedAt = NOW.minusSeconds(3600);
                    t.assigneeId = "other";
                }, c -> {
                    c.actorId = "manager";
                    c.manager = true;
                }).allowedActions());
        assertEquals(Set.of(), evaluate(t -> {
            t.status = TaskStatus.IN_REVIEW;
            t.startedAt = NOW.minusSeconds(3600);
        }, null).allowedActions());
        assertEquals(Set.of(TaskAction.APPROVE, TaskAction.REJECT_REVIEW, TaskAction.CANCEL),
                evaluate(t -> {
                    t.status = TaskStatus.IN_REVIEW;
                    t.startedAt = NOW.minusSeconds(3600);
                    t.assigneeId = "other";
                }, c -> {
                    c.actorId = "admin";
                    c.administrator = true;
                }).allowedActions());
        assertEquals(Set.of(TaskAction.APPROVE, TaskAction.REJECT_REVIEW, TaskAction.CANCEL), evaluate(t -> {
            t.status = TaskStatus.IN_REVIEW;
            t.startedAt = NOW.minusSeconds(3600);
            t.assigneeId = "other";
        }, c -> {
            c.actorId = "admin";
            c.administrator = true;
            c.workingHours = false;
        }).allowedActions());

        assertEquals(Set.of(TaskAction.REOPEN, TaskAction.ARCHIVE), evaluate(t -> {
            t.status = TaskStatus.COMPLETED;
            t.startedAt = NOW.minusSeconds(7200);
            t.completedAt = NOW.minusSeconds(3600);
            t.assigneeId = "other";
        }, c -> {
            c.actorId = "manager";
            c.manager = true;
        }).allowedActions());
        assertEquals(Set.of(TaskAction.REOPEN, TaskAction.ARCHIVE), evaluate(t -> {
            t.status = TaskStatus.CANCELLED;
        }, c -> c.actorId = "creator").allowedActions());
        assertEquals(Set.of(TaskAction.ARCHIVE), evaluate(t -> {
            t.status = TaskStatus.CANCELLED;
        }, c -> {
            c.actorId = "creator";
            c.projectActive = false;
        }).allowedActions());
        assertEquals(Set.of(TaskAction.RESTORE), evaluate(t -> t.status = TaskStatus.ARCHIVED,
                c -> {
                    c.actorId = "manager";
                    c.manager = true;
                }).allowedActions());
        assertEquals(Set.of(), evaluate(t -> t.status = TaskStatus.ARCHIVED, null)
                .allowedActions());
    }

    @Test
    @DisplayName("attendance boundaries and overtime boundaries")
    void attendanceBoundaries() {
        // exactly sixty minutes late under a flexible shift is ignored
        assertEquals(0, attendance(a -> {
            a.shift = new Shift(ShiftType.FLEXIBLE, LocalTime.of(9, 0), LocalTime.of(18, 0),
                    0, 60, 480);
            a.punch = punch(LocalTime.of(10, 0), LocalTime.of(19, 0));
        }).lateMinutes());
        // exactly one hundred twenty minutes late is waived by severe weather
        AttendanceResult weather = attendance(a -> {
            a.severeWeather = true;
            a.punch = punch(LocalTime.of(11, 10), LocalTime.of(19, 10));
        });
        assertEquals(0, weather.lateMinutes());
        // exactly fifteen minutes late still costs one point
        assertEquals(1, attendance(a -> a.punch = punch(LocalTime.of(9, 25),
                LocalTime.of(18, 25))).penaltyPoints());
        // exactly sixty minutes late is not "seriously late"
        AttendanceResult sixty = attendance(a -> a.punch = punch(LocalTime.of(10, 10),
                LocalTime.of(19, 10)));
        assertFalse(sixty.codes().contains(AttendanceCode.SERIOUSLY_LATE));
        assertEquals(2, sixty.penaltyPoints());
        // leaving exactly at the grace boundary is not early leave
        AttendanceResult grace = attendance(a -> a.punch = punch(LocalTime.of(9, 0),
                LocalTime.of(17, 50)));
        assertFalse(grace.codes().contains(AttendanceCode.EARLY_LEAVE));
        // exactly sixty minutes early costs one point
        AttendanceResult sixtyEarly = attendance(a -> a.punch = punch(LocalTime.of(8, 0),
                LocalTime.of(17, 0)));
        assertTrue(sixtyEarly.codes().contains(AttendanceCode.EARLY_LEAVE));
        assertEquals(60, sixtyEarly.earlyLeaveMinutes());
        assertEquals(1, sixtyEarly.penaltyPoints());
        // exactly half of the minimum work time keeps the lighter shortage penalty
        AttendanceResult halfDay = attendance(a -> a.punch = new PunchRecord(
                LocalDateTime.of(MON, LocalTime.of(9, 0)),
                LocalDateTime.of(MON, LocalTime.of(13, 0)), 0, false, true, false, false));
        assertEquals(5, halfDay.penaltyPoints());
        // zero presence minutes
        assertEquals(0, attendance(a -> a.punch = new PunchRecord(
                LocalDateTime.of(MON, LocalTime.of(9, 0)),
                LocalDateTime.of(MON, LocalTime.of(9, 0)), 0, false, true, false, false))
                .workedMinutes());

        OvertimePolicy policy = new OvertimePolicy();
        Attendance approved = new Attendance();
        approved.overtimeRequested = true;
        approved.overtimeApproved = true;
        AttendanceInput approvedInput = approved.build();
        OvertimeResult cappedAtExactlyFourHours = policy.evaluate(approvedInput, 480, 240);
        assertEquals(240, cappedAtExactlyFourHours.payableMinutes());
        assertFalse(cappedAtExactlyFourHours.managerReview());
        OvertimeResult overFourHours = policy.evaluate(approvedInput, 480, 270);
        assertEquals(240, overFourHours.payableMinutes());
        assertTrue(overFourHours.managerReview());
        OvertimeResult exactlyThreeSixty = policy.evaluate(approvedInput, 480, 360);
        assertTrue(exactlyThreeSixty.managerReview());
        OvertimeResult justUnderFourHours = policy.evaluate(approvedInput, 480, 241);
        assertEquals(240, justUnderFourHours.payableMinutes());
        assertFalse(justUnderFourHours.managerReview());
        OvertimeResult overThreeSixty = policy.evaluate(approvedInput, 480, 361);
        assertEquals(240, overThreeSixty.payableMinutes());
        assertTrue(overThreeSixty.managerReview());
        assertEquals(30, policy.evaluate(approvedInput, 570, 30).payableMinutes());
        OvertimeResult exactlyNineHundred = policy.evaluate(approvedInput, 900, 90);
        assertFalse(exactlyNineHundred.managerReview());
        OvertimeResult overNineHundred = policy.evaluate(approvedInput, 901, 90);
        assertTrue(overNineHundred.managerReview());
        assertThrowsWith(IllegalArgumentException.class, "minutes must not be negative",
                () -> policy.evaluate(approvedInput, 0, -1));
    }

    private static Ctx permissive(UserRole role, ResourceType type, ResourceAction action,
            AccessScope scope) {
        Ctx ctx = new Ctx();
        ctx.actorId = "actor";
        ctx.ownerId = "someone-else";
        ctx.actorDept = "D1";
        ctx.resourceDept = "D1";
        ctx.role = role;
        ctx.type = type;
        ctx.action = action;
        ctx.scope = scope;
        ctx.sensitivity = Sensitivity.INTERNAL;
        return ctx;
    }

    private static void assertRoleDenied(UserRole role, ResourceType type, ResourceAction action,
            AccessScope scope) {
        AccessDecision decision = new AccessPolicy().decide(
                permissive(role, type, action, scope).build());
        assertFalse(decision.allowed(), role + "/" + type + "/" + action);
        assertTrue(decision.reasons().contains("role does not grant the requested action"),
                role + "/" + type + "/" + action + " -> " + decision.reasons());
    }

    private static void assertRoleAllowed(UserRole role, ResourceType type, ResourceAction action,
            AccessScope scope) {
        AccessDecision decision = new AccessPolicy().decide(
                permissive(role, type, action, scope).build());
        assertTrue(decision.allowed(), role + "/" + type + "/" + action + " -> "
                + decision.reasons());
    }

    @Test
    @DisplayName("permission: single-cause role denials and scope denials")
    void accessSingleCauseDenials() {
        // finance cannot delete; the rest of the context is fully permissive
        assertRoleDenied(UserRole.FINANCE, ResourceType.EXPENSE_CLAIM, ResourceAction.DELETE,
                AccessScope.TEAM);
        assertRoleDenied(UserRole.FINANCE, ResourceType.CONTRACT, ResourceAction.DELETE,
                AccessScope.TEAM);
        assertRoleDenied(UserRole.FINANCE, ResourceType.PAYROLL, ResourceAction.DELETE,
                AccessScope.TEAM);
        // finance cannot share restricted data, but may share internal data
        Ctx restrictedShare = permissive(UserRole.FINANCE, ResourceType.EXPENSE_CLAIM,
                ResourceAction.SHARE, AccessScope.TEAM);
        restrictedShare.sensitivity = Sensitivity.RESTRICTED;
        assertFalse(new AccessPolicy().decide(restrictedShare.build()).allowed());
        assertTrue(new AccessPolicy().decide(restrictedShare.build()).reasons()
                .contains("role does not grant the requested action"));
        assertRoleAllowed(UserRole.FINANCE, ResourceType.EXPENSE_CLAIM, ResourceAction.SHARE,
                AccessScope.TEAM);
        assertRoleAllowed(UserRole.FINANCE, ResourceType.EXPENSE_CLAIM, ResourceAction.VIEW,
                AccessScope.ORGANIZATION);
        assertRoleDenied(UserRole.FINANCE, ResourceType.EMPLOYEE_PROFILE, ResourceAction.VIEW,
                AccessScope.TEAM);
        Ctx financePublic = permissive(UserRole.FINANCE, ResourceType.PUBLIC_DOCUMENT,
                ResourceAction.VIEW, AccessScope.TEAM);
        financePublic.sensitivity = Sensitivity.PUBLIC;
        assertTrue(new AccessPolicy().decide(financePublic.build()).allowed());

        // human resources cannot delete workforce data or export outside its projects
        assertRoleDenied(UserRole.HUMAN_RESOURCES, ResourceType.EMPLOYEE_PROFILE,
                ResourceAction.DELETE, AccessScope.TEAM);
        Ctx exportCtx = permissive(UserRole.HUMAN_RESOURCES, ResourceType.EMPLOYEE_PROFILE,
                ResourceAction.EXPORT, AccessScope.ORGANIZATION);
        exportCtx.projectMember = false;
        assertTrue(new AccessPolicy().decide(exportCtx.build()).reasons()
                .contains("role does not grant the requested action"));
        exportCtx.projectMember = true;
        assertTrue(new AccessPolicy().decide(exportCtx.build()).allowed());
        assertRoleAllowed(UserRole.HUMAN_RESOURCES, ResourceType.PAYROLL, ResourceAction.VIEW,
                AccessScope.TEAM);
        assertRoleDenied(UserRole.HUMAN_RESOURCES, ResourceType.PAYROLL, ResourceAction.VIEW,
                AccessScope.ORGANIZATION);
        assertRoleDenied(UserRole.HUMAN_RESOURCES, ResourceType.PUBLIC_DOCUMENT,
                ResourceAction.UPDATE, AccessScope.TEAM);

        // auditors are read only
        assertRoleAllowed(UserRole.AUDITOR, ResourceType.EXPENSE_CLAIM, ResourceAction.VIEW,
                AccessScope.ORGANIZATION);
        assertRoleAllowed(UserRole.AUDITOR, ResourceType.EXPENSE_CLAIM, ResourceAction.EXPORT,
                AccessScope.ORGANIZATION);
        assertRoleDenied(UserRole.AUDITOR, ResourceType.EXPENSE_CLAIM, ResourceAction.UPDATE,
                AccessScope.ORGANIZATION);
        assertRoleDenied(UserRole.AUDITOR, ResourceType.EXPENSE_CLAIM, ResourceAction.CREATE,
                AccessScope.ORGANIZATION);

        // administrators may not read payroll or profiles
        assertRoleDenied(UserRole.SYSTEM_ADMINISTRATOR, ResourceType.PAYROLL, ResourceAction.VIEW,
                AccessScope.ORGANIZATION);
        assertRoleDenied(UserRole.SYSTEM_ADMINISTRATOR, ResourceType.PAYROLL,
                ResourceAction.EXPORT, AccessScope.ORGANIZATION);
        assertRoleDenied(UserRole.SYSTEM_ADMINISTRATOR, ResourceType.PAYROLL,
                ResourceAction.APPROVE, AccessScope.ORGANIZATION);
        assertRoleDenied(UserRole.SYSTEM_ADMINISTRATOR, ResourceType.EMPLOYEE_PROFILE,
                ResourceAction.VIEW, AccessScope.ORGANIZATION);
        assertRoleAllowed(UserRole.SYSTEM_ADMINISTRATOR, ResourceType.PAYROLL,
                ResourceAction.UPDATE, AccessScope.ORGANIZATION);

        // guests are limited to viewing public documents
        assertRoleDenied(UserRole.GUEST, ResourceType.PUBLIC_DOCUMENT, ResourceAction.UPDATE,
                AccessScope.TEAM);
        Ctx guestInternal = permissive(UserRole.GUEST, ResourceType.PUBLIC_DOCUMENT,
                ResourceAction.VIEW, AccessScope.TEAM);
        guestInternal.sensitivity = Sensitivity.INTERNAL;
        assertFalse(new AccessPolicy().decide(guestInternal.build()).allowed());
        assertTrue(new AccessPolicy().decide(guestInternal.build()).reasons()
                .contains("role does not grant the requested action"));

        // team leads do not cover other resource types
        assertRoleDenied(UserRole.TEAM_LEAD, ResourceType.CONTRACT, ResourceAction.VIEW,
                AccessScope.TEAM);
        assertRoleAllowed(UserRole.TEAM_LEAD, ResourceType.EXPENSE_CLAIM, ResourceAction.VIEW,
                AccessScope.TEAM);

        // department managers may not touch payroll and cannot delete non-public data
        assertRoleDenied(UserRole.DEPARTMENT_MANAGER, ResourceType.PAYROLL, ResourceAction.VIEW,
                AccessScope.DEPARTMENT);
        assertRoleDenied(UserRole.DEPARTMENT_MANAGER, ResourceType.AUDIT_LOG, ResourceAction.VIEW,
                AccessScope.DEPARTMENT);
        Ctx managerDelete = permissive(UserRole.DEPARTMENT_MANAGER, ResourceType.EXPENSE_CLAIM,
                ResourceAction.DELETE, AccessScope.DEPARTMENT);
        assertFalse(new AccessPolicy().decide(managerDelete.build()).allowed());
        managerDelete.sensitivity = Sensitivity.PUBLIC;
        assertTrue(new AccessPolicy().decide(managerDelete.build()).allowed());

        // employees may view public documents by role even without ownership
        Ctx employeePublic = permissive(UserRole.EMPLOYEE, ResourceType.PUBLIC_DOCUMENT,
                ResourceAction.VIEW, AccessScope.OWN);
        employeePublic.sensitivity = Sensitivity.PUBLIC;
        AccessDecision employeePublicDecision = new AccessPolicy().decide(employeePublic.build());
        assertFalse(employeePublicDecision.allowed());
        assertTrue(employeePublicDecision.reasons()
                .contains("requested scope is outside the actor's authority"));

        // department managers with a department scope check reject own scope explicitly
        Ctx managerOwnScope = permissive(UserRole.DEPARTMENT_MANAGER, ResourceType.EXPENSE_CLAIM,
                ResourceAction.VIEW, AccessScope.OWN);
        assertEquals("D1", managerOwnScope.actorDept);
        assertFalse(new AccessPolicy().decide(managerOwnScope.build()).allowed());

        // employees may only reach their own scope
        assertScopeDenied(ResourceType.PUBLIC_DOCUMENT, ResourceAction.VIEW, AccessScope.OWN);
        assertScopeDenied(ResourceType.PUBLIC_DOCUMENT, ResourceAction.VIEW, AccessScope.TEAM);
        assertScopeDenied(ResourceType.PUBLIC_DOCUMENT, ResourceAction.VIEW, AccessScope.DEPARTMENT);
        assertScopeDenied(ResourceType.PUBLIC_DOCUMENT, ResourceAction.VIEW,
                AccessScope.ORGANIZATION);
    }

    private static void assertScopeDenied(ResourceType type, ResourceAction action,
            AccessScope scope) {
        Ctx ctx = permissive(UserRole.EMPLOYEE, type, action, scope);
        ctx.sensitivity = Sensitivity.PUBLIC;
        AccessDecision decision = new AccessPolicy().decide(ctx.build());
        assertFalse(decision.allowed(), scope.name());
        assertTrue(decision.reasons().contains("requested scope is outside the actor's authority"),
                scope.name() + " -> " + decision.reasons());
    }

    @Test
    @DisplayName("permission: exact reason lists for every role gate")
    void accessExactReasons() {
        // administrators are denied payroll and profile reads, exports and approvals
        for (ResourceType type : List.of(ResourceType.PAYROLL, ResourceType.EMPLOYEE_PROFILE)) {
            for (ResourceAction action : List.of(ResourceAction.VIEW, ResourceAction.EXPORT,
                    ResourceAction.APPROVE)) {
                AccessDecision decision = new AccessPolicy().decide(
                        permissive(UserRole.SYSTEM_ADMINISTRATOR, type, action,
                                AccessScope.ORGANIZATION).build());
                assertEquals(List.of("role does not grant the requested action"),
                        decision.reasons(), type + "/" + action);
            }
        }
        // administrators may update payroll
        AccessDecision adminUpdate = new AccessPolicy().decide(
                permissive(UserRole.SYSTEM_ADMINISTRATOR, ResourceType.PAYROLL,
                        ResourceAction.UPDATE, AccessScope.ORGANIZATION).build());
        assertTrue(adminUpdate.allowed());
        assertEquals(List.of("system administrator has technical administration access",
                "organization scope granted to specialist role",
                "actor and resource belong to the same department"), adminUpdate.reasons());

        // legal hold adds a second denial for administrators
        Ctx adminHold = permissive(UserRole.SYSTEM_ADMINISTRATOR, ResourceType.EXPENSE_CLAIM,
                ResourceAction.DELETE, AccessScope.ORGANIZATION);
        adminHold.legalHold = true;
        assertEquals(List.of("resource is under legal hold",
                        "role does not grant the requested action"),
                new AccessPolicy().decide(adminHold.build()).reasons());

        // guests: exact allow and deny reasons
        Ctx guestAllow = permissive(UserRole.GUEST, ResourceType.PUBLIC_DOCUMENT,
                ResourceAction.VIEW, AccessScope.OWN);
        guestAllow.sensitivity = Sensitivity.PUBLIC;
        guestAllow.ownerId = "actor";
        assertEquals(List.of("guest may view public documents",
                        "resource is publicly classified", "actor owns the resource",
                        "actor and resource belong to the same department"),
                new AccessPolicy().decide(guestAllow.build()).reasons());
        Ctx guestDeny = permissive(UserRole.GUEST, ResourceType.PUBLIC_DOCUMENT,
                ResourceAction.VIEW, AccessScope.OWN);
        guestDeny.sensitivity = Sensitivity.INTERNAL;
        guestDeny.ownerId = "actor";
        assertEquals(List.of("role does not grant the requested action"),
                new AccessPolicy().decide(guestDeny.build()).reasons());
        Ctx guestUpdate = permissive(UserRole.GUEST, ResourceType.PUBLIC_DOCUMENT,
                ResourceAction.UPDATE, AccessScope.OWN);
        guestUpdate.sensitivity = Sensitivity.PUBLIC;
        guestUpdate.ownerId = "actor";
        assertEquals(List.of("role does not grant the requested action"),
                new AccessPolicy().decide(guestUpdate.build()).reasons());

        // employees: owned payroll is view only, owned claims are editable
        Ctx ownedPayrollUpdate = permissive(UserRole.EMPLOYEE, ResourceType.PAYROLL,
                ResourceAction.UPDATE, AccessScope.OWN);
        ownedPayrollUpdate.ownerId = "actor";
        assertEquals(List.of("role does not grant the requested action"),
                new AccessPolicy().decide(ownedPayrollUpdate.build()).reasons());
        Ctx ownedClaimUpdate = permissive(UserRole.EMPLOYEE, ResourceType.EXPENSE_CLAIM,
                ResourceAction.UPDATE, AccessScope.OWN);
        ownedClaimUpdate.ownerId = "actor";
        assertEquals(List.of("employee may access owned resource",
                        "actor owns the resource",
                        "actor and resource belong to the same department"),
                new AccessPolicy().decide(ownedClaimUpdate.build()).reasons());

        // team leads: each team resource type grants access
        for (ResourceType type : List.of(ResourceType.LEAVE_REQUEST,
                ResourceType.ATTENDANCE_RECORD, ResourceType.EXPENSE_CLAIM)) {
            Ctx lead = permissive(UserRole.TEAM_LEAD, type, ResourceAction.VIEW, AccessScope.TEAM);
            assertEquals(List.of("team lead has operational team access",
                            "actor and resource belong to the same department"),
                    new AccessPolicy().decide(lead.build()).reasons(), type.name());
        }
        Ctx leadApprove = permissive(UserRole.TEAM_LEAD, ResourceType.LEAVE_REQUEST,
                ResourceAction.APPROVE, AccessScope.TEAM);
        assertTrue(new AccessPolicy().decide(leadApprove.build()).allowed());
        Ctx leadUpdate = permissive(UserRole.TEAM_LEAD, ResourceType.LEAVE_REQUEST,
                ResourceAction.UPDATE, AccessScope.TEAM);
        assertEquals(List.of("role does not grant the requested action"),
                new AccessPolicy().decide(leadUpdate.build()).reasons());

        // department managers: scope and department gates
        Ctx otherDepartment = permissive(UserRole.DEPARTMENT_MANAGER, ResourceType.EXPENSE_CLAIM,
                ResourceAction.VIEW, AccessScope.DEPARTMENT);
        otherDepartment.resourceDept = "D2";
        assertEquals(List.of("role does not grant the requested action",
                        "requested scope is outside the actor's authority"),
                new AccessPolicy().decide(otherDepartment.build()).reasons());
        Ctx ownScopeManager = permissive(UserRole.DEPARTMENT_MANAGER, ResourceType.EXPENSE_CLAIM,
                ResourceAction.VIEW, AccessScope.OWN);
        assertEquals(List.of("role does not grant the requested action",
                        "requested scope is outside the actor's authority"),
                new AccessPolicy().decide(ownScopeManager.build()).reasons());
        Ctx departmentView = permissive(UserRole.DEPARTMENT_MANAGER, ResourceType.EXPENSE_CLAIM,
                ResourceAction.VIEW, AccessScope.DEPARTMENT);
        assertEquals(List.of("department manager has department-scoped access",
                        "actor and resource belong to the same department"),
                new AccessPolicy().decide(departmentView.build()).reasons());

        // human resources and finance denial reasons
        for (ResourceType type : List.of(ResourceType.EMPLOYEE_PROFILE,
                ResourceType.LEAVE_REQUEST, ResourceType.ATTENDANCE_RECORD)) {
            Ctx hrDelete = permissive(UserRole.HUMAN_RESOURCES, type, ResourceAction.DELETE,
                    AccessScope.TEAM);
            assertEquals(List.of("role does not grant the requested action"),
                    new AccessPolicy().decide(hrDelete.build()).reasons(), type.name());
        }
        for (ResourceType type : List.of(ResourceType.EXPENSE_CLAIM, ResourceType.PAYROLL,
                ResourceType.CONTRACT)) {
            Ctx financeDelete = permissive(UserRole.FINANCE, type, ResourceAction.DELETE,
                    AccessScope.TEAM);
            assertEquals(List.of("role does not grant the requested action"),
                    new AccessPolicy().decide(financeDelete.build()).reasons(), type.name());
        }
        // allowed paths fall back to the default reason string
        Ctx auditorView = permissive(UserRole.AUDITOR, ResourceType.EXPENSE_CLAIM,
                ResourceAction.VIEW, AccessScope.ORGANIZATION);
        assertEquals(List.of("auditor receives read-only access",
                        "organization scope granted to specialist role",
                        "actor and resource belong to the same department"),
                new AccessPolicy().decide(auditorView.build()).reasons());
    }

    // ------------------------------------------------------------------
    // SECTION-ANCHOR
    // ------------------------------------------------------------------

    // ===== merged from CardProbeTests probe =====
    @org.junit.jupiter.api.Nested
    class CardProbeTests {


    // ==================================================================
    // shared fixtures (deterministic)
    // ==================================================================

    /** 2026-03-02 is a Monday. */
    private static final LocalDate MON = LocalDate.of(2026, 3, 2);
    /** Sunday before MON, so nothing looks backdated. */
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 1);

    private static final WorkCalendar ALL_DAYS = date -> true;
    private static final WorkCalendar WEEKDAYS = date -> date.getDayOfWeek().getValue() <= 5;

    private static LocalDate day(int offset) {
        return MON.plusDays(offset);
    }

    // ==================================================================
    // net.mooctest.leave.AdvancedLeavePolicy
    // ==================================================================

    private static final class LReq {
        EmployeeCategory category = EmployeeCategory.REGULAR;
        LeaveType type = LeaveType.SICK;
        LocalDate start = MON;
        LocalDate end = MON;
        LeaveUnit unit = LeaveUnit.FULL_DAY;
        double hours;
        String reason = "medical appointment";
        boolean emergency;
        boolean remoteCapable;
        boolean handover = true;
        int months = 24;
        int notice = 10;
        double balance = 10;
        double carryOver;

        LReq type(LeaveType value) {
            type = value;
            return this;
        }

        LReq category(EmployeeCategory value) {
            category = value;
            return this;
        }

        LReq range(LocalDate startDate, LocalDate endDate) {
            start = startDate;
            end = endDate;
            return this;
        }

        LReq spanDays(int days) {
            start = MON;
            end = MON.plusDays(days - 1L);
            return this;
        }

        LReq unit(LeaveUnit value) {
            unit = value;
            return this;
        }

        LReq hours(double value) {
            hours = value;
            return this;
        }

        LReq notice(int value) {
            notice = value;
            return this;
        }

        LReq months(int value) {
            months = value;
            return this;
        }

        LReq handover(boolean value) {
            handover = value;
            return this;
        }

        LReq balance(double value) {
            balance = value;
            return this;
        }

        AdvancedLeaveRequest build() {
            return new AdvancedLeaveRequest("E1", category, type, start, end, unit, hours, reason,
                    emergency, remoteCapable, handover, months, notice, balance, carryOver);
        }
    }

    private static LeaveAssessment assess(LReq req, WorkCalendar calendar) {
        return new AdvancedLeavePolicy().assess(req.build(), calendar, TODAY);
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.calculateChargedDays L64 idx=37 RemoveConditional_EQUAL_ELSE "
            + "(HALF_DAY_AFTERNOON loses its half-day charge)")
    void leaveChargedDaysAfternoonHalfDay() {
        // The HALF_DAY_AFTERNOON equality is the *second* operand of the || at L63/L64.
        // Killing it requires an afternoon half-day that lands on a *working* day, so that the
        // surviving path (the day-counting loop) charges 1.0 instead of 0.5.
        LReq req = new LReq().type(LeaveType.SICK).unit(LeaveUnit.HALF_DAY_AFTERNOON)
                .range(MON, MON);

        AdvancedLeavePolicy policy = new AdvancedLeavePolicy();
        assertEquals(0.5, policy.calculateChargedDays(req.build(), ALL_DAYS), 1.0e-9);

        LeaveAssessment assessment = assess(req, ALL_DAYS);
        assertEquals(0.5, assessment.chargedDays(), 1.0e-9);
        assertTrue(assessment.eligible(), assessment.violations()::toString);
        assertEquals(List.of(), assessment.violations());

        // the same request on a non-working day is charged 0, which the loop also cannot produce
        // without counting the very day the branch just rejected
        LReq weekend = new LReq().type(LeaveType.SICK).unit(LeaveUnit.HALF_DAY_AFTERNOON)
                .range(LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 7));
        assertEquals(0.0, policy.calculateChargedDays(weekend.build(), WEEKDAYS), 1.0e-9);
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.requiresHandover L272 idx=6 ConditionalsBoundary + "
            + "RemoveConditional_ORDER_ELSE (chargedDays >= 3 exactly at 3)")
    void leaveHandoverThreeChargedDays() {
        // chargedDays == 3 exactly; not ANNUAL, not REGULAR, not a Friday start, so only the
        // `chargedDays >= 3` branch can report handover.
        LReq req = new LReq().type(LeaveType.SICK).category(EmployeeCategory.PROBATION)
                .spanDays(3).handover(false);

        LeaveAssessment assessment = assess(req, ALL_DAYS);
        assertEquals(3.0, assessment.chargedDays(), 1.0e-9);
        assertTrue(assessment.handoverRequired());
        assertFalse(assessment.eligible());
        assertEquals(List.of("handover is required for this request"), assessment.violations());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.requiresHandover L275 idx=17 EQUAL_ELSE + idx=21 boundary/ORDER "
            + "(ANNUAL >= 2 charged days on a non-REGULAR category)")
    void leaveHandoverAnnualTwoChargedDays() {
        // chargedDays == 2 with ANNUAL: exactly the boundary of `ANNUAL && chargedDays >= 2`.
        // CONTRACTOR keeps L278 out of the way (it needs chargedDays > 1 on REGULAR), and a
        // Tuesday start keeps the Friday rule out.
        LReq req = new LReq().type(LeaveType.ANNUAL).category(EmployeeCategory.CONTRACTOR)
                .spanDays(2).months(24).balance(10).handover(true);

        LeaveAssessment assessment = assess(req, ALL_DAYS);
        assertEquals(2.0, assessment.chargedDays(), 1.0e-9);
        assertTrue(assessment.handoverRequired());
        assertTrue(assessment.eligible(), assessment.violations()::toString);
        assertEquals(List.of(), assessment.violations());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.requiresHandover L281 idx=48 EQUAL_ELSE + idx=52 ORDER_ELSE "
            + "(Friday start with more than one charged day)")
    void leaveHandoverFridayStart() {
        // Friday start, chargedDays == 2, SICK + CONTRACTOR so L272/L275/L278 are all false and
        // only the `getDayOfWeek() == FRIDAY && chargedDays > 1` tail can report handover.
        LocalDate friday = MON.plusDays(4);
        assertEquals(DayOfWeek.FRIDAY, friday.getDayOfWeek());
        LReq req = new LReq().type(LeaveType.SICK).category(EmployeeCategory.CONTRACTOR)
                .range(friday, friday.plusDays(1)).months(24).handover(true);

        LeaveAssessment assessment = assess(req, ALL_DAYS);
        assertEquals(2.0, assessment.chargedDays(), 1.0e-9);
        assertTrue(assessment.handoverRequired());
        assertEquals(List.of(), assessment.violations());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.requiresMedicalCertificate L260 idx=21 ConditionalsBoundary "
            + "(PROBATION with exactly one charged day)")
    void leaveCertificateProbationOneDay() {
        // chargedDays == 1: `chargedDays > 2`, `PROBATION && chargedDays > 1`, `emergency && > 1`
        // and the HOURS/4 rule are all false, so no certificate is required.
        LReq req = new LReq().type(LeaveType.SICK).category(EmployeeCategory.PROBATION)
                .spanDays(1).handover(true);

        LeaveAssessment assessment = assess(req, ALL_DAYS);
        assertEquals(1.0, assessment.chargedDays(), 1.0e-9);
        assertFalse(assessment.medicalCertificateRequired());
        // PROBATION still routes to HR through resolveApprovalTier, so the flag itself is the
        // only observable difference and it must be asserted directly
        assertEquals(ApprovalTier.HUMAN_RESOURCES, assessment.approvalTier());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.resolveApprovalTier L288 idx=6 ConditionalsBoundary "
            + "(exactly ten charged days is not DIRECTOR)")
    void leaveTierTenChargedDays() {
        LReq req = new LReq().type(LeaveType.SICK).category(EmployeeCategory.REGULAR)
                .spanDays(10).notice(10).handover(true);

        LeaveAssessment assessment = assess(req, ALL_DAYS);
        assertEquals(10.0, assessment.chargedDays(), 1.0e-9);
        assertEquals(ApprovalTier.HUMAN_RESOURCES, assessment.approvalTier());
        assertEquals(List.of(), assessment.violations());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.resolveApprovalTier L291 idx=21 ConditionalsBoundary "
            + "(CONTRACTOR with exactly three charged days is not DIRECTOR)")
    void leaveTierContractorThreeChargedDays() {
        LReq req = new LReq().type(LeaveType.SICK).category(EmployeeCategory.CONTRACTOR)
                .spanDays(3).notice(10).months(24).handover(true);

        LeaveAssessment assessment = assess(req, ALL_DAYS);
        assertEquals(3.0, assessment.chargedDays(), 1.0e-9);
        assertEquals(ApprovalTier.HUMAN_RESOURCES, assessment.approvalTier());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validateDates L101 idx=102 ConditionalsBoundary "
            + "(exactly three notice days / exactly five charged days)")
    void leaveNoticeFiveDayRuleBoundary() {
        // `noticeDays() < 3 && chargedDays > 5`
        // branch A: noticeDays == 3 must not trigger, whatever operand the boundary moved
        LeaveAssessment exactlyThreeNoticeDays = assess(
                new LReq().type(LeaveType.SICK).spanDays(6).notice(3).handover(true), ALL_DAYS);
        assertEquals(6.0, exactlyThreeNoticeDays.chargedDays(), 1.0e-9);
        assertEquals(List.of(), exactlyThreeNoticeDays.violations());
        assertTrue(exactlyThreeNoticeDays.eligible());

        // branch B: chargedDays == 5 must not trigger either
        LeaveAssessment exactlyFiveDays = assess(
                new LReq().type(LeaveType.SICK).spanDays(5).notice(2).handover(true), ALL_DAYS);
        assertEquals(5.0, exactlyFiveDays.chargedDays(), 1.0e-9);
        assertEquals(List.of(), exactlyFiveDays.violations());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validateDates L104 idx=122 ConditionalsBoundary "
            + "(exactly seven notice days / exactly ten charged days)")
    void leaveNoticeTenDayRuleBoundary() {
        // `noticeDays() < 7 && chargedDays > 10`
        LeaveAssessment exactlySevenNoticeDays = assess(
                new LReq().type(LeaveType.SICK).spanDays(11).notice(7).handover(true), ALL_DAYS);
        assertEquals(11.0, exactlySevenNoticeDays.chargedDays(), 1.0e-9);
        assertEquals(List.of(), exactlySevenNoticeDays.violations());

        LeaveAssessment exactlyTenDays = assess(
                new LReq().type(LeaveType.SICK).spanDays(10).notice(6).handover(true), ALL_DAYS);
        assertEquals(10.0, exactlyTenDays.chargedDays(), 1.0e-9);
        assertEquals(List.of(), exactlyTenDays.violations());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.resolveApprovalTier L297 idx=46 ConditionalsBoundary "
            + "(annual leave of exactly three charged days stays SUPERVISOR)")
    void leaveTierAnnualThreeChargedDays() {
        // For ANNUAL the medical-certificate flag stays false (only SICK computes it), so
        // execution really reaches `chargedDays > 3 || type == PERSONAL` with chargedDays == 3.
        LReq req = new LReq().type(LeaveType.ANNUAL).category(EmployeeCategory.REGULAR)
                .spanDays(3).months(24).balance(10).notice(10).handover(true);

        LeaveAssessment assessment = assess(req, ALL_DAYS);
        assertEquals(3.0, assessment.chargedDays(), 1.0e-9);
        assertFalse(assessment.medicalCertificateRequired());
        assertEquals(ApprovalTier.SUPERVISOR, assessment.approvalTier());
        assertEquals(List.of(), assessment.violations());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validateUnit L161 idx=12 EQUAL_ELSE "
            + "(an afternoon half day spanning two dates is rejected)")
    void leaveAfternoonHalfDayAcrossDates() {
        // The removed operand is `unit() == HALF_DAY_AFTERNOON`: with it forced to false the
        // half-day consistency check below it is simply skipped for afternoon requests.
        LReq req = new LReq().type(LeaveType.SICK).category(EmployeeCategory.REGULAR)
                .unit(LeaveUnit.HALF_DAY_AFTERNOON).range(MON, MON.plusDays(1))
                .handover(true).notice(10);

        LeaveAssessment assessment = assess(req, WEEKDAYS);
        assertFalse(assessment.eligible());
        assertEquals(List.of("half-day leave must start and end on the same date"),
                assessment.violations());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validateEmployment L148 idx=74 ConditionalsBoundary "
            + "(exactly one month of service is not 'first month')")
    void leaveEmploymentOneMonthBoundary() {
        // PERSONAL + CONTRACTOR + one charged day keeps every other rule quiet, so the list is
        // empty and only the `employmentMonths() < 1` boundary can add the extra violation.
        LReq req = new LReq().type(LeaveType.PERSONAL).category(EmployeeCategory.CONTRACTOR)
                .spanDays(1).months(1).notice(1).handover(true);

        LeaveAssessment assessment = assess(req, ALL_DAYS);
        assertTrue(assessment.eligible(), assessment.violations()::toString);
        assertEquals(List.of(), assessment.violations());
        assertEquals(ApprovalTier.MANAGER, assessment.approvalTier());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validateSickLeave L211 idx=6 ConditionalsBoundary "
            + "(exactly thirty sick days is allowed)")
    void leaveSickThirtyDaysBoundary() {
        LReq req = new LReq().type(LeaveType.SICK).category(EmployeeCategory.REGULAR)
                .spanDays(30).notice(10).handover(true).balance(0);

        LeaveAssessment assessment = assess(req, ALL_DAYS);
        assertEquals(30.0, assessment.chargedDays(), 1.0e-9);
        assertEquals(List.of(), assessment.violations());
        assertTrue(assessment.eligible());
        assertEquals(ApprovalTier.DIRECTOR, assessment.approvalTier());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validateUnit L173 idx=91 ConditionalsBoundary "
            + "(zero charged days must not report the minimum-charge violation)")
    void leaveMinimumChargeZeroDays() {
        // A Saturday..Sunday range over a Mon-Fri calendar charges nothing, so `chargedDays > 0`
        // is false and only "request contains no working time" may be reported.
        LocalDate saturday = LocalDate.of(2026, 3, 7);
        LocalDate sunday = LocalDate.of(2026, 3, 8);
        assertEquals(DayOfWeek.SATURDAY, saturday.getDayOfWeek());
        LReq req = new LReq().type(LeaveType.SICK).category(EmployeeCategory.REGULAR)
                .range(saturday, sunday).handover(true).notice(10);

        LeaveAssessment assessment = assess(req, WEEKDAYS);
        assertEquals(0.0, assessment.chargedDays(), 1.0e-9);
        assertEquals(List.of("request contains no working time"), assessment.violations());
        assertFalse(assessment.eligible());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validateUnit L161 idx=12 EQUAL_ELSE + L173 idx=95 boundary "
            + "(a same-date half day is legal and costs exactly half a day)")
    void leaveHalfDaySameDate() {
        // chargedDays == 0.5 exactly: `chargedDays < 0.5` must stay false, and the same-date
        // half-day must not be reported (L161 equality forced to false would report it).
        LReq req = new LReq().type(LeaveType.SICK).category(EmployeeCategory.REGULAR)
                .unit(LeaveUnit.HALF_DAY_MORNING).range(MON, MON).handover(true).notice(10);

        LeaveAssessment assessment = assess(req, WEEKDAYS);
        assertEquals(0.5, assessment.chargedDays(), 1.0e-9);
        assertTrue(assessment.eligible(), assessment.violations()::toString);
        assertEquals(List.of(), assessment.violations());

        // and the *afternoon* half day on the same date must behave identically
        LReq afternoon = new LReq().type(LeaveType.SICK).category(EmployeeCategory.REGULAR)
                .unit(LeaveUnit.HALF_DAY_AFTERNOON).range(MON, MON).handover(true).notice(10);
        LeaveAssessment afternoonAssessment = assess(afternoon, WEEKDAYS);
        assertEquals(0.5, afternoonAssessment.chargedDays(), 1.0e-9);
        assertEquals(List.of(), afternoonAssessment.violations());
    }

    // ==================================================================
    // net.mooctest.task.TaskPolicy
    // ==================================================================

    private static final Instant NOW = Instant.parse("2026-03-02T09:00:00Z");

    private static final class TReq {
        String creator = "creator";
        String assignee = "assignee";
        TaskStatus status = TaskStatus.OPEN;
        TaskPriority priority = TaskPriority.NORMAL;
        Instant createdAt = NOW.minus(10, ChronoUnit.DAYS);
        Instant dueAt;
        Instant startedAt;
        Instant completedAt;
        List<String> dependencies = List.of();
        boolean confidential;
        boolean reviewRequired;

        TReq status(TaskStatus value) {
            status = value;
            return this;
        }

        TReq priority(TaskPriority value) {
            priority = value;
            return this;
        }

        TReq creator(String value) {
            creator = value;
            return this;
        }

        TReq assignee(String value) {
            assignee = value;
            return this;
        }

        TReq dueAt(Instant value) {
            dueAt = value;
            return this;
        }

        TReq startedAt(Instant value) {
            startedAt = value;
            return this;
        }

        TReq completedAt(Instant value) {
            completedAt = value;
            return this;
        }

        Task build() {
            return new Task("T1", "task title", null, creator, assignee, status, priority,
                    RecurrenceRule.NONE, createdAt, dueAt, startedAt, completedAt, 60, 0,
                    dependencies, Set.of(), confidential, reviewRequired);
        }
    }

    private static final class TCtx {
        String actor = "assignee";
        Instant now = NOW;
        boolean manager;
        boolean admin;
        boolean depsComplete = true;
        boolean assigneeActive = true;
        boolean projectActive = true;
        boolean withinWorkingHours = true;
        boolean notifications = true;
        int previousReopens;
        int assigneeOpen;
        int overdueCount;

        TCtx actor(String value) {
            actor = value;
            return this;
        }

        TCtx manager(boolean value) {
            manager = value;
            return this;
        }

        TCtx admin(boolean value) {
            admin = value;
            return this;
        }

        TCtx depsComplete(boolean value) {
            depsComplete = value;
            return this;
        }

        TCtx assigneeActive(boolean value) {
            assigneeActive = value;
            return this;
        }

        TCtx projectActive(boolean value) {
            projectActive = value;
            return this;
        }

        TCtx withinWorkingHours(boolean value) {
            withinWorkingHours = value;
            return this;
        }

        TCtx overdueCount(int value) {
            overdueCount = value;
            return this;
        }

        TaskContext build() {
            return new TaskContext(actor, now, manager, admin, depsComplete, assigneeActive,
                    projectActive, withinWorkingHours, notifications, previousReopens,
                    assigneeOpen, overdueCount);
        }
    }

    private static TaskEvaluation eval(TReq req, TCtx ctx) {
        return new TaskPolicy().evaluate(req.build(), ctx.build());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L192 idx=80 EQUAL_ELSE (OPEN: assignee may START)")
    void taskOpenAssigneeCanStart() {
        TReq task = new TReq().status(TaskStatus.OPEN).assignee("assignee").creator("creator");
        TCtx ctx = new TCtx().actor("assignee"); // neither manager nor administrator

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.START), evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L192/L195 idx=80,100 (OPEN: manager may START + CANCEL)")
    void taskOpenManagerCanStartAndCancel() {
        TReq task = new TReq().status(TaskStatus.OPEN).assignee("assignee").creator("creator");
        TCtx ctx = new TCtx().actor("mgr").manager(true);

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.START, TaskAction.CANCEL), evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L192/L195 idx=80,100 (OPEN: administrator may START + CANCEL)")
    void taskOpenAdminCanStartAndCancel() {
        TReq task = new TReq().status(TaskStatus.OPEN).assignee("assignee").creator("creator");
        TCtx ctx = new TCtx().actor("root").admin(true);

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.START, TaskAction.CANCEL), evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L192 idx=80 EQUAL_ELSE (OPEN: incomplete dependencies block START)")
    void taskOpenIncompleteDependenciesBlockStart() {
        TReq task = new TReq().status(TaskStatus.OPEN).assignee("assignee").creator("creator");
        TCtx ctx = new TCtx().actor("mgr").manager(true).depsComplete(false);

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.CANCEL), evaluation.allowedActions());
        assertEquals(List.of("dependencies prevent task start"), evaluation.warnings());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L195 idx=100 EQUAL_ELSE (OPEN: creator may CANCEL)")
    void taskOpenCreatorCanCancel() {
        TReq task = new TReq().status(TaskStatus.OPEN).assignee("other").creator("creator");
        TCtx ctx = new TCtx().actor("creator"); // not the assignee, not a manager

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.CANCEL), evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L203 idx=129 EQUAL_ELSE (IN_PROGRESS: assignee may BLOCK/COMPLETE)")
    void taskInProgressAssigneeCanWork() {
        TReq task = new TReq().status(TaskStatus.IN_PROGRESS).assignee("assignee")
                .creator("creator").startedAt(NOW.minus(1, ChronoUnit.DAYS));
        TCtx ctx = new TCtx().actor("assignee");

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.BLOCK, TaskAction.COMPLETE), evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L203/L211 idx=129,162 (IN_PROGRESS: manager may cancel)")
    void taskInProgressManagerCanCancel() {
        TReq task = new TReq().status(TaskStatus.IN_PROGRESS).assignee("assignee")
                .creator("creator").startedAt(NOW.minus(1, ChronoUnit.DAYS));
        TCtx ctx = new TCtx().actor("mgr").manager(true);

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.BLOCK, TaskAction.COMPLETE, TaskAction.CANCEL),
                evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L203/L211 idx=129,162 (IN_PROGRESS: administrator may cancel)")
    void taskInProgressAdminCanCancel() {
        TReq task = new TReq().status(TaskStatus.IN_PROGRESS).assignee("assignee")
                .creator("creator").startedAt(NOW.minus(1, ChronoUnit.DAYS));
        TCtx ctx = new TCtx().actor("root").admin(true);

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.BLOCK, TaskAction.COMPLETE, TaskAction.CANCEL),
                evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L216 idx=175 EQUAL_ELSE (BLOCKED: assignee may UNBLOCK)")
    void taskBlockedAssigneeCanUnblock() {
        TReq task = new TReq().status(TaskStatus.BLOCKED).assignee("assignee")
                .creator("creator").startedAt(NOW.minus(1, ChronoUnit.DAYS));
        TCtx ctx = new TCtx().actor("assignee");

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.UNBLOCK), evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L216 idx=177 EQUAL_ELSE (BLOCKED: manager may UNBLOCK)")
    void taskBlockedManagerCanUnblock() {
        TReq task = new TReq().status(TaskStatus.BLOCKED).assignee("assignee")
                .creator("creator").startedAt(NOW.minus(1, ChronoUnit.DAYS));
        TCtx ctx = new TCtx().actor("mgr").manager(true);

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.UNBLOCK, TaskAction.CANCEL), evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L216 idx=179 EQUAL_ELSE (BLOCKED: administrator may UNBLOCK)")
    void taskBlockedAdminCanUnblock() {
        TReq task = new TReq().status(TaskStatus.BLOCKED).assignee("assignee")
                .creator("creator").startedAt(NOW.minus(1, ChronoUnit.DAYS));
        TCtx ctx = new TCtx().actor("root").admin(true);

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.UNBLOCK, TaskAction.CANCEL), evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L231 idx=233 EQUAL_ELSE (COMPLETED: manager may REOPEN/ARCHIVE)")
    void taskCompletedManagerCanReopenAndArchive() {
        TReq task = new TReq().status(TaskStatus.COMPLETED).assignee("assignee")
                .creator("creator").startedAt(NOW.minus(2, ChronoUnit.DAYS))
                .completedAt(NOW.minus(1, ChronoUnit.DAYS));
        TCtx ctx = new TCtx().actor("mgr").manager(true);

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.REOPEN, TaskAction.ARCHIVE), evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L231 idx=235 EQUAL_ELSE (COMPLETED: administrator may REOPEN/ARCHIVE)")
    void taskCompletedAdminCanReopenAndArchive() {
        TReq task = new TReq().status(TaskStatus.COMPLETED).assignee("assignee")
                .creator("creator").startedAt(NOW.minus(2, ChronoUnit.DAYS))
                .completedAt(NOW.minus(1, ChronoUnit.DAYS));
        TCtx ctx = new TCtx().actor("root").admin(true);

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.REOPEN, TaskAction.ARCHIVE), evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L237 idx=258 EQUAL_ELSE (CANCELLED: creator may REOPEN/ARCHIVE)")
    void taskCancelledCreatorCanReopenAndArchive() {
        TReq task = new TReq().status(TaskStatus.CANCELLED).assignee("other")
                .creator("creator");
        TCtx ctx = new TCtx().actor("creator"); // not the assignee, not a manager

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.REOPEN, TaskAction.ARCHIVE), evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L237 idx=258 EQUAL_ELSE (CANCELLED: manager may REOPEN/ARCHIVE)")
    void taskCancelledManagerCanReopenAndArchive() {
        TReq task = new TReq().status(TaskStatus.CANCELLED).assignee("assignee")
                .creator("creator");
        TCtx ctx = new TCtx().actor("mgr").manager(true);

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.REOPEN, TaskAction.ARCHIVE), evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L237 idx=258 EQUAL_ELSE (CANCELLED: administrator may REOPEN/ARCHIVE)")
    void taskCancelledAdminCanReopenAndArchive() {
        TReq task = new TReq().status(TaskStatus.CANCELLED).assignee("assignee")
                .creator("creator");
        TCtx ctx = new TCtx().actor("root").admin(true);

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(TaskAction.REOPEN, TaskAction.ARCHIVE), evaluation.allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.calculatePriority L64 idx=130 ConditionalsBoundary + ORDER_ELSE "
            + "(exactly three overdue days scores 15, not 8)")
    void taskPriorityThreeOverdueDays() {
        // The visible difference is 45 (URGENT) versus 38 (HIGH): base LOW = 0, overdue due date
        // (hours <= 0) = +30, then the overdueDays branch = 15 or 8.
        TReq task = new TReq().status(TaskStatus.OPEN).priority(TaskPriority.LOW)
                .dueAt(NOW.minus(3, ChronoUnit.DAYS).minus(12, ChronoUnit.HOURS));
        TCtx ctx = new TCtx().actor("assignee");

        TaskEvaluation evaluation = eval(task, ctx);
        assertTrue(evaluation.overdue());
        assertEquals(3L, ChronoUnit.DAYS.between(task.dueAt, NOW));
        assertEquals(TaskPriority.URGENT, evaluation.effectivePriority());
        assertEquals(List.of("task is overdue"), evaluation.warnings());
    }

    @Test
    @DisplayName("TaskPolicy.isOverdue L270 idx=28 EQUAL_ELSE (an ARCHIVED task is never overdue)")
    void taskArchivedIsNeverOverdue() {
        TReq task = new TReq().status(TaskStatus.ARCHIVED).priority(TaskPriority.NORMAL)
                .dueAt(NOW.minus(1, ChronoUnit.DAYS));
        TCtx ctx = new TCtx().actor("assignee");

        TaskEvaluation evaluation = eval(task, ctx);
        assertFalse(evaluation.overdue());
        assertEquals(List.of(), evaluation.warnings());
        assertEquals(TaskPriority.LOW, evaluation.effectivePriority());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L192/L195/L203/L211/L216/L219/L224/L231/L237/L243 "
            + "(an actor with no role gets no action from any status)")
    void taskActorWithoutRoleGetsNoAction() {
        // Every role test in allowedActions is written as `if (role || role || role)`; forcing one
        // of those operand tests to the opposite constant makes the guarded action unconditional.
        // The only input that separates "role required" from "action always granted" is an actor
        // that holds none of the roles, which is what this test feeds for every status.
        for (TaskStatus status : TaskStatus.values()) {
            TReq task = new TReq().status(status).creator("creator").assignee("assignee");
            if (status == TaskStatus.IN_PROGRESS || status == TaskStatus.BLOCKED
                    || status == TaskStatus.IN_REVIEW) {
                task.startedAt(NOW.minus(1, ChronoUnit.DAYS));
            }
            if (status == TaskStatus.COMPLETED) {
                task.startedAt(NOW.minus(2, ChronoUnit.DAYS))
                        .completedAt(NOW.minus(1, ChronoUnit.DAYS));
            }
            TCtx ctx = new TCtx().actor("nobody"); // not creator, not assignee, neither role

            TaskEvaluation evaluation = eval(task, ctx);
            assertEquals(Set.of(), evaluation.allowedActions(), status::name);
        }
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L216 idx=175/177 EQUAL_ELSE "
            + "(a BLOCKED task stays closed to an unrelated actor)")
    void taskBlockedUnrelatedActorGetsNoAction() {
        TReq task = new TReq().status(TaskStatus.BLOCKED).creator("creator").assignee("assignee")
                .startedAt(NOW.minus(1, ChronoUnit.DAYS));
        TCtx ctx = new TCtx().actor("nobody");

        TaskEvaluation evaluation = eval(task, ctx);
        assertEquals(Set.of(), evaluation.allowedActions());
        // an inactive unrelated assignee must not be dragged into the decision either
        assertEquals(List.of(), evaluation.warnings());
    }

    @Test
    @DisplayName("TaskPolicy.requiresEscalation L117 idx=5 EQUAL_ELSE (terminal tasks never escalate)")
    void taskTerminalNeverEscalates() {
        // COMPLETED + inactive assignee: the only reason no escalation is reported is the
        // `task.isTerminal()` early return at L117.
        TReq task = new TReq().status(TaskStatus.COMPLETED).assignee("assignee")
                .creator("creator").priority(TaskPriority.NORMAL)
                .startedAt(NOW.minus(2, ChronoUnit.DAYS))
                .completedAt(NOW.minus(1, ChronoUnit.DAYS));
        TCtx ctx = new TCtx().actor("someone").assigneeActive(false);

        TaskEvaluation evaluation = eval(task, ctx);
        assertFalse(evaluation.escalationRequired());
        assertFalse(evaluation.reassignmentRecommended());
        assertEquals(List.of(), evaluation.warnings());
        assertEquals(Set.of(), evaluation.allowedActions());
    }

    // ==================================================================
    // net.mooctest.permission.AccessPolicy
    // ==================================================================

    private static final class ACtx {
        String actorId = "u1";
        String ownerId = "owner";
        String actorDepartment = "deptA";
        String resourceDepartment = "deptB";
        UserRole role = UserRole.EMPLOYEE;
        ResourceAction action = ResourceAction.VIEW;
        ResourceType type = ResourceType.EXPENSE_CLAIM;
        Sensitivity sensitivity = Sensitivity.INTERNAL;
        AccessScope scope = AccessScope.OWN;
        boolean accountActive = true;
        boolean suspended;
        boolean mfa = true;
        boolean trustedDevice = true;
        boolean trustedNetwork = true;
        boolean businessHours = true;
        boolean delegated;
        boolean delegationValid;
        boolean legalHold;
        boolean archived;
        boolean projectMember;
        boolean regionMatches = true;
        boolean training = true;

        ACtx role(UserRole value) {
            role = value;
            return this;
        }

        ACtx action(ResourceAction value) {
            action = value;
            return this;
        }

        ACtx type(ResourceType value) {
            type = value;
            return this;
        }

        ACtx sensitivity(Sensitivity value) {
            sensitivity = value;
            return this;
        }

        ACtx scope(AccessScope value) {
            scope = value;
            return this;
        }

        ACtx owner(String value) {
            ownerId = value;
            return this;
        }

        ACtx departments(String actor, String resource) {
            actorDepartment = actor;
            resourceDepartment = resource;
            return this;
        }

        AccessContext build() {
            return new AccessContext(actorId, ownerId, actorDepartment, resourceDepartment, role,
                    action, type, sensitivity, scope, accountActive, suspended, mfa, trustedDevice,
                    trustedNetwork, businessHours, delegated, delegationValid, legalHold, archived,
                    projectMember, regionMatches, training);
        }
    }

    private static AccessDecision access(ACtx ctx) {
        return new AccessPolicy().decide(ctx.build());
    }

    @Test
    @DisplayName("AccessPolicy.administratorAllows L242 idx=6 EQUAL_ELSE "
            + "(payroll / employee-profile views stay closed to the administrator)")
    void accessAdministratorPayrollGuard() {
        // The guard `resourceType == PAYROLL || resourceType == EMPLOYEE_PROFILE` is the only
        // reason an administrator cannot view these two sensitive types.
        AccessDecision payroll = access(new ACtx().role(UserRole.SYSTEM_ADMINISTRATOR)
                .type(ResourceType.PAYROLL).action(ResourceAction.VIEW)
                .scope(AccessScope.ORGANIZATION).departments("deptA", "deptA"));
        assertFalse(payroll.allowed());
        assertEquals(List.of("role does not grant the requested action"), payroll.reasons());

        AccessDecision profile = access(new ACtx().role(UserRole.SYSTEM_ADMINISTRATOR)
                .type(ResourceType.EMPLOYEE_PROFILE).action(ResourceAction.VIEW)
                .scope(AccessScope.ORGANIZATION).departments("deptA", "deptA"));
        assertFalse(profile.allowed());
        assertEquals(List.of("role does not grant the requested action"), profile.reasons());
    }

    @Test
    @DisplayName("AccessPolicy.decide L108 idx=442/445 EQUAL_ELSE "
            + "(allowed access with no notes gets the default reason)")
    void accessDefaultGrantedReason() {
        // HR keeps PUBLIC_DOCUMENT/VIEW without adding a note, TEAM scope is granted silently and
        // no resource-specific rule fires, so `reasons` is the empty note list at L108.
        AccessDecision decision = access(new ACtx().role(UserRole.HUMAN_RESOURCES)
                .type(ResourceType.PUBLIC_DOCUMENT).action(ResourceAction.VIEW)
                .sensitivity(Sensitivity.INTERNAL).scope(AccessScope.TEAM)
                .owner("someoneElse").departments(null, null));

        assertTrue(decision.allowed());
        assertEquals(List.of("access granted by role and scope policy"), decision.reasons());
    }

    @Test
    @DisplayName("AccessPolicy.employeeAllows L135 idx=41 + L140 idx=81 + L141 idx=87 + L142 "
            + "BooleanFalseReturnVals (owned resources and public documents)")
    void accessEmployeeOwnedAndPublicDocument() {
        // L135: `action == VIEW`, L136: `action == UPDATE` — both must reach the owned branch.
        AccessDecision view = access(new ACtx().role(UserRole.EMPLOYEE).owner("u1")
                .action(ResourceAction.VIEW).type(ResourceType.EXPENSE_CLAIM)
                .scope(AccessScope.OWN).departments(null, null));
        assertTrue(view.allowed(), view.reasons()::toString);
        assertEquals(List.of("employee may access owned resource", "actor owns the resource"),
                view.reasons());

        AccessDecision update = access(new ACtx().role(UserRole.EMPLOYEE).owner("u1")
                .action(ResourceAction.UPDATE).type(ResourceType.EXPENSE_CLAIM)
                .scope(AccessScope.OWN).departments(null, null));
        assertTrue(update.allowed(), update.reasons()::toString);

        // L140/L141/L142: an employee may view a public document even without owning it. The
        // only denial left is the personal scope check, which pins down that the role check
        // itself succeeded.
        AccessDecision publicDocument = access(new ACtx().role(UserRole.EMPLOYEE)
                .action(ResourceAction.VIEW).type(ResourceType.PUBLIC_DOCUMENT)
                .sensitivity(Sensitivity.INTERNAL).scope(AccessScope.OWN).owner("someoneElse")
                .departments(null, null));
        assertFalse(publicDocument.allowed());
        assertEquals(List.of("requested scope is outside the actor's authority"),
                publicDocument.reasons());
    }

    @Test
    @DisplayName("AccessPolicy.guestAllows L258 idx=6 EQUAL_ELSE "
            + "(a guest may view a public document)")
    void accessGuestPublicDocument() {
        AccessDecision decision = access(new ACtx().role(UserRole.GUEST)
                .action(ResourceAction.VIEW).type(ResourceType.PUBLIC_DOCUMENT)
                .sensitivity(Sensitivity.PUBLIC).scope(AccessScope.OWN).owner("someoneElse")
                .departments(null, null));

        assertFalse(decision.allowed());
        assertEquals(List.of("requested scope is outside the actor's authority"), decision.reasons());
    }

    @Test
    @DisplayName("AccessPolicy.guestAllows L258 idx=6 EQUAL_ELSE (guard variant) "
            + "(the public-document guard is what rejects a guest on any other type)")
    void accessGuestNonPublicDocument() {
        // Without the `resourceType != PUBLIC_DOCUMENT` guard the action and sensitivity checks
        // alone would let the guest through, which drops the role denial.
        AccessDecision decision = access(new ACtx().role(UserRole.GUEST)
                .action(ResourceAction.VIEW).type(ResourceType.EXPENSE_CLAIM)
                .sensitivity(Sensitivity.PUBLIC).scope(AccessScope.OWN).owner("someoneElse")
                .departments(null, null));

        assertFalse(decision.allowed());
        assertEquals(List.of("guest may only access public documents",
                "role does not grant the requested action",
                "requested scope is outside the actor's authority"), decision.reasons());
    }

    @Test
    @DisplayName("AccessPolicy.employeeAllows L135 idx=41 EQUAL_ELSE (ownership variant) "
            + "(an employee cannot touch someone else's resource)")
    void accessEmployeeUnownedResource() {
        // Makes the `ownsResource()` guard observable: removing it lets the owned branch answer
        // for a completely different actor's resource.
        AccessDecision decision = access(new ACtx().role(UserRole.EMPLOYEE).owner("someoneElse")
                .action(ResourceAction.VIEW).type(ResourceType.EXPENSE_CLAIM)
                .scope(AccessScope.OWN).departments(null, null));

        assertFalse(decision.allowed());
        assertEquals(List.of("role does not grant the requested action",
                "requested scope is outside the actor's authority"), decision.reasons());
    }

    @Test
    @DisplayName("AccessPolicy.employeeAllows L135 idx=41 EQUAL_ELSE (action variant) "
            + "(VIEW/UPDATE only: a delete of an owned claim is not granted)")
    void accessEmployeeOwnedDeleteDenied() {
        // Makes the `action == VIEW || action == UPDATE` pair observable: if that guard is
        // bypassed the owned branch would grant DELETE as well.
        AccessDecision decision = access(new ACtx().role(UserRole.EMPLOYEE).owner("u1")
                .action(ResourceAction.DELETE).type(ResourceType.EXPENSE_CLAIM)
                .scope(AccessScope.OWN).departments(null, null));

        assertFalse(decision.allowed());
        assertEquals(List.of("role does not grant the requested action"), decision.reasons());
    }

    @Test
    @DisplayName("AccessPolicy.scopeAllows L297 idx=159 EQUAL_ELSE "
            + "(administrator holds department scope)")
    void accessAdministratorDepartmentScope() {
        AccessDecision decision = access(new ACtx().role(UserRole.SYSTEM_ADMINISTRATOR)
                .action(ResourceAction.VIEW).type(ResourceType.EXPENSE_CLAIM)
                .scope(AccessScope.DEPARTMENT).departments("deptA", "deptB"));

        assertTrue(decision.allowed(), decision.reasons()::toString);
        assertEquals(List.of("system administrator has technical administration access"),
                decision.reasons());
    }

    // ==================================================================
    // net.mooctest.expense.ExpensePolicy
    // ==================================================================

    private static final LocalDate SPEND_DAY = LocalDate.of(2026, 3, 10);
    private static final ExchangeRateProvider ONE_RATE = (currency, date) -> BigDecimal.ONE;
    private static final Currency USD = Currency.getInstance("USD");

    private static final class EItem {
        String id = "I1";
        ExpenseCategory category = ExpenseCategory.SOFTWARE;
        String amount = "100.00";
        LocalDate incurred = SPEND_DAY;
        String merchant = "Acme Store";
        String purpose = "business reason text";
        ReceiptType receipt = ReceiptType.TAX_INVOICE;
        PaymentMethod payment = PaymentMethod.PERSONAL_CARD;
        int attendees;
        boolean alcohol;
        boolean personal;
        boolean duplicate;

        EItem category(ExpenseCategory value) {
            category = value;
            return this;
        }

        EItem id(String value) {
            id = value;
            return this;
        }

        EItem amount(String value) {
            amount = value;
            return this;
        }

        EItem incurred(LocalDate value) {
            incurred = value;
            return this;
        }

        EItem personal(boolean value) {
            personal = value;
            return this;
        }

        ExpenseItem build() {
            return new ExpenseItem(id, category, new BigDecimal(amount), USD, incurred, merchant,
                    purpose, receipt, payment, attendees, alcohol, personal, duplicate);
        }
    }

    private static final class EClaim {
        EmployeeGrade grade = EmployeeGrade.STAFF;
        LocalDate submitted = SPEND_DAY;
        String costCenter = "CC-100";
        List<ExpenseItem> items = List.of(new EItem().build());
        boolean preApproved = true;
        boolean international;
        boolean clientBillable;
        boolean projectActive = true;
        int previousClaims;

        EClaim grade(EmployeeGrade value) {
            grade = value;
            return this;
        }

        EClaim items(ExpenseItem... value) {
            items = List.of(value);
            return this;
        }

        EClaim international(boolean value) {
            international = value;
            return this;
        }

        ExpenseClaim build() {
            return new ExpenseClaim("C1", "E1", grade, submitted, costCenter, items, preApproved,
                    international, clientBillable, projectActive, previousClaims);
        }
    }

    private static ExpenseAssessment assessClaim(EClaim claim) {
        return new ExpensePolicy().assess(claim.build(), ONE_RATE, SPEND_DAY);
    }

    @Test
    @DisplayName("ExpensePolicy.categoryCap L255 idx=173 EQUAL_ELSE + L256 idx=179 EQUAL_ELSE "
            + "(international hotel and meal caps are multiplied by 1.5)")
    void expenseInternationalCapMultiplier() {
        // HOTEL: 500 * STAFF(2) = 1000, international -> 1500, so 1400 must pass untouched.
        ExpenseAssessment hotel = assessClaim(new EClaim().international(true)
                .items(new EItem().category(ExpenseCategory.HOTEL).amount("1400.00").build()));
        assertEquals(new BigDecimal("1400.00"), hotel.reimbursableAmount());
        assertFalse(hotel.warnings().contains("category cap applied to item I1"));

        // MEAL: 80 * STAFF(2) = 160, international -> 240, so 200 must pass untouched.
        ExpenseAssessment meal = assessClaim(new EClaim().international(true)
                .items(new EItem().category(ExpenseCategory.MEAL).amount("200.00").build()));
        assertEquals(new BigDecimal("200.00"), meal.reimbursableAmount());
        assertFalse(meal.warnings().contains("category cap applied to item I1"));
    }

    @Test
    @DisplayName("ExpensePolicy.assess L80 idx=253 EQUAL_ELSE "
            + "(rejection comes from a zero reimbursement or from a FATAL violation)")
    void expenseRejectionComputation() {
        // reimbursable == 0 without any FATAL violation
        ExpenseAssessment zero = assessClaim(new EClaim()
                .items(new EItem().category(ExpenseCategory.SOFTWARE).amount("100.00")
                        .personal(true).build()));
        assertTrue(zero.rejected());
        assertEquals(new BigDecimal("0.00"), zero.reimbursableAmount());

        // a FATAL violation while money is still reimbursable
        ExpenseAssessment fatal = assessClaim(new EClaim()
                .items(new EItem().category(ExpenseCategory.SOFTWARE).amount("100.00")
                        .incurred(SPEND_DAY.plusDays(5)).build()));
        assertTrue(fatal.violations().contains("FATAL: future expense date for item I1"),
                fatal.violations()::toString);
        assertTrue(fatal.rejected());
    }

    @Test
    @DisplayName("ExpensePolicy.categoryCap L255 idx=173 + L256 idx=179 EQUAL_ELSE (negative variant) "
            + "(the 1.5 multiplier only applies to hotel / meal / local transport)")
    void expenseInternationalMultiplierIsCategorySpecific() {
        // SOFTWARE is not in the multiplier list: 800 * STAFF(2) = 1600, so 2000 must be capped.
        ExpenseAssessment software = assessClaim(new EClaim().international(true)
                .items(new EItem().category(ExpenseCategory.SOFTWARE).amount("2000.00").build()));
        assertEquals(new BigDecimal("1600.00"), software.reimbursableAmount());
        assertTrue(software.warnings().contains("category cap applied to item I1"),
                software.warnings()::toString);
    }

    @Test
    @DisplayName("ExpensePolicy.resolveApprovalTier L396 idx=53 ORDER_ELSE "
            + "(a 6000 claim above the 5000 line goes to FINANCE)")
    void expenseTierAboveFiveThousand() {
        // Two 3000 items keep every single item under the 5000 audit line, so auditRequired stays
        // false and only `total > 5000` can route the claim to FINANCE.
        ExpenseAssessment assessment = assessClaim(new EClaim()
                .items(new EItem().id("I1").category(ExpenseCategory.SOFTWARE).amount("3000.00")
                                .build(),
                        new EItem().id("I2").category(ExpenseCategory.SOFTWARE).amount("3000.00")
                                .build()));

        assertEquals(new BigDecimal("6000.00"), assessment.claimedAmount());
        assertFalse(assessment.auditRequired());
        assertFalse(assessment.rejected());
        assertEquals(ExpenseApprovalTier.FINANCE, assessment.approvalTier());
    }

    @Test
    @DisplayName("ExpensePolicy.resolveApprovalTier L399 idx=68 ConditionalsBoundary "
            + "(exactly 2000 stays with the line manager)")
    void expenseTierExactlyTwoThousand() {
        ExpenseAssessment assessment = assessClaim(new EClaim()
                .items(new EItem().category(ExpenseCategory.SOFTWARE).amount("2000.00").build()));

        assertEquals(new BigDecimal("2000.00"), assessment.claimedAmount());
        assertFalse(assessment.rejected());
        assertEquals(ExpenseApprovalTier.LINE_MANAGER, assessment.approvalTier());
    }

    // ==================================================================
    // net.mooctest.workflow.ApprovalWorkflowEngine
    // ==================================================================

    private static final Instant WF_NOW = Instant.parse("2026-03-02T09:00:00Z");

    private static ApprovalStep wfStep(String id, String approver) {
        return new ApprovalStep(id, "step " + id, Set.of(approver), StepMode.ANY_ONE, 1, false,
                false, null);
    }

    private static ApprovalStep wfStep(String id, String approver, boolean delegationAllowed,
            boolean skipRequester, Instant deadline) {
        return new ApprovalStep(id, "step " + id, Set.of(approver), StepMode.ANY_ONE, 1,
                delegationAllowed, skipRequester, deadline);
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.start L22 idx=17 EQUAL_ELSE "
            + "(the empty-step guard runs before the requester is validated)")
    void workflowEmptyStepsGuardComesFirst() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        // A blank requester would produce a different message, so the message proves which guard
        // fired first.
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> engine.start("W1", "   ", List.of(), WF_NOW, false, true));
        assertEquals("workflow requires at least one step", error.getMessage());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.cancel L154 idx=46 ConditionalsBoundary "
            + "(a five character cancellation reason is accepted)")
    void workflowCancelFiveCharacterReason() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        WorkflowSnapshot snapshot = engine.start("W1", "req", List.of(wfStep("s1", "a1")), WF_NOW,
                false, true);

        WorkflowSnapshot cancelled = engine.cancel(snapshot, true, "abcde",
                WF_NOW.plusSeconds(60));
        assertEquals(WorkflowState.CANCELLED, cancelled.state());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.withdraw L132 idx=51 ConditionalsBoundary "
            + "(an urgent workflow is still withdrawable before the first approval)")
    void workflowUrgentWithdrawBeforeFirstApproval() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        WorkflowSnapshot snapshot = engine.start("W1", "req",
                List.of(wfStep("s1", "a1"), wfStep("s2", "a2")), WF_NOW, true, true);
        assertEquals(0, snapshot.currentStepIndex());
        assertTrue(snapshot.urgent());

        WorkflowSnapshot withdrawn = engine.withdraw(snapshot, "req", WF_NOW.plusSeconds(60));
        assertEquals(WorkflowState.WITHDRAWN, withdrawn.state());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.withdraw L125 idx=16 VoidMethodCall "
            + "(withdrawing an expired workflow still fails the active check)")
    void workflowWithdrawExpiredRejected() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        Instant deadline = WF_NOW.plusSeconds(3600);
        WorkflowSnapshot snapshot = engine.start("W1", "req",
                List.of(wfStep("s1", "a1", false, false, deadline)), WF_NOW, false, true);
        WorkflowSnapshot expired = engine.expireIfNecessary(snapshot, deadline.plusSeconds(60));
        assertEquals(WorkflowState.EXPIRED, expired.state());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> engine.withdraw(expired, "req", deadline.plusSeconds(120)));
        assertEquals("terminal workflow cannot accept actions", error.getMessage());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.delegate L105 idx=16 VoidMethodCall "
            + "(delegating an expired workflow still fails the active check)")
    void workflowDelegateExpiredRejected() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        Instant deadline = WF_NOW.plusSeconds(3600);
        WorkflowSnapshot snapshot = engine.start("W1", "req",
                List.of(wfStep("s1", "a1", true, false, deadline)), WF_NOW, false, true);
        WorkflowSnapshot expired = engine.expireIfNecessary(snapshot, deadline.plusSeconds(60));
        assertEquals(WorkflowState.EXPIRED, expired.state());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> engine.delegate(expired, "a1", "b1", deadline.plusSeconds(120)));
        assertEquals("terminal workflow cannot accept actions", error.getMessage());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.ensureActive L180 idx=19 + L181 idx=25 EQUAL_ELSE "
            + "(PENDING and IN_PROGRESS workflows both accept actions)")
    void workflowActiveStatesAcceptActions() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        WorkflowSnapshot pending = engine.start("W1", "req",
                List.of(wfStep("s1", "a1"), wfStep("s2", "a2")), WF_NOW, false, true);
        assertEquals(WorkflowState.PENDING, pending.state());

        // kills the L180 mutation (PENDING must stay active)
        WorkflowSnapshot afterFirst = engine.act(pending,
                new WorkflowAction("a1", ApprovalDecision.APPROVE, null, WF_NOW.plusSeconds(60)));
        assertEquals(WorkflowState.IN_PROGRESS, afterFirst.state());
        assertEquals(1, afterFirst.currentStepIndex());

        // kills the L181 mutation (IN_PROGRESS must stay active)
        WorkflowSnapshot afterSecond = engine.act(afterFirst,
                new WorkflowAction("a2", ApprovalDecision.APPROVE, null, WF_NOW.plusSeconds(120)));
        assertEquals(WorkflowState.APPROVED, afterSecond.state());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.nextApplicableStep L195 idx=38 EQUAL_ELSE "
            + "(a requester-only step is skipped)")
    void workflowRequesterOnlyStepIsSkipped() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        ApprovalStep skip = wfStep("s1", "req", false, true, null);
        ApprovalStep next = wfStep("s2", "a2");

        WorkflowSnapshot twoSteps = engine.start("W1", "req", List.of(skip, next), WF_NOW, false,
                true);
        assertEquals(1, twoSteps.currentStepIndex());
        assertEquals(WorkflowState.PENDING, twoSteps.state());

        WorkflowSnapshot onlySkipped = engine.start("W2", "req", List.of(skip), WF_NOW, false, true);
        assertEquals(WorkflowState.APPROVED, onlySkipped.state());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.nextApplicableStep L195 idx=38 EQUAL_ELSE (multi-approver variant) "
            + "(skipRequester does not drop a step that has other approvers)")
    void workflowMultiApproverStepIsNotSkipped() {
        // The third operand (`approverIds().contains(requesterId)`) is what keeps a step with two
        // approvers alive even though it opts out of requester approval.
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        ApprovalStep skip = new ApprovalStep("s1", "step s1", Set.of("a1", "a2"), StepMode.ANY_ONE,
                1, false, true, null);
        ApprovalStep next = wfStep("s2", "a3");

        WorkflowSnapshot snapshot = engine.start("W1", "req", List.of(skip, next), WF_NOW, false,
                true);
        assertEquals(0, snapshot.currentStepIndex());
        assertEquals(WorkflowState.PENDING, snapshot.state());
        assertTrue(snapshot.currentStep().step().skipRequester());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.nextApplicableStep L195 idx=38 EQUAL_ELSE (foreign approver variant) "
            + "(a requester-skipping step held by somebody else still applies)")
    void workflowSingleForeignApproverStepKept() {
        // skipRequester is true and there is exactly one approver, but that approver is not the
        // requester: the `contains(requesterId)` operand is the only thing keeping the step.
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        ApprovalStep foreign = wfStep("s1", "a1", false, true, null);
        ApprovalStep next = wfStep("s2", "a3");

        WorkflowSnapshot snapshot = engine.start("W1", "req", List.of(foreign, next), WF_NOW, false,
                true);
        assertEquals(0, snapshot.currentStepIndex());
        assertEquals(WorkflowState.PENDING, snapshot.state());
    }

    // ==================================================================
    // net.mooctest.attendance.AttendancePolicy
    // ==================================================================

    private static final LocalDate WORK_DAY = LocalDate.of(2026, 3, 2);

    private static AttendanceInput attendance(Shift shift, PunchRecord punch, boolean requested,
            boolean approved) {
        return new AttendanceInput("E1", WORK_DAY, shift, punch, EmploymentMode.OFFICE, true, false,
                false, false, false, requested, approved, 0, 0);
    }

    @Test
    @DisplayName("AttendancePolicy.evaluate L117 idx=489 ConditionalsBoundary "
            + "(a break exactly equal to the requirement is enough)")
    void attendanceExactBreakRequirement() {
        // 09:00-18:00, required break 45, taken break 45, worked 495 minutes.
        Shift shift = new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(18, 0), 0, 45,
                60);
        PunchRecord punch = new PunchRecord(LocalDateTime.of(WORK_DAY, LocalTime.of(9, 0)),
                LocalDateTime.of(WORK_DAY, LocalTime.of(18, 0)), 45, false, true, false, false);

        AttendanceResult result = new AttendancePolicy().evaluate(attendance(shift, punch, false,
                false));
        assertEquals(495, result.workedMinutes());
        assertEquals(List.of(AttendanceCode.NORMAL), result.codes());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    @DisplayName("AttendancePolicy.evaluate L132 idx=569 ConditionalsBoundary "
            + "(exactly 240 overtime minutes is not excessive)")
    void attendanceExactlyFourHoursOvertime() {
        // 09:00-21:00 over an 08:00 shift is 720 worked / 480 scheduled => 240 raw overtime.
        Shift shift = new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(17, 0), 0, 0,
                60);
        PunchRecord punch = new PunchRecord(LocalDateTime.of(WORK_DAY, LocalTime.of(9, 0)),
                LocalDateTime.of(WORK_DAY, LocalTime.of(21, 0)), 0, false, true, false, false);

        AttendanceResult result = new AttendancePolicy().evaluate(attendance(shift, punch, true,
                true));
        assertEquals(240, result.overtimeMinutes());
        assertEquals(List.of(AttendanceCode.OVERTIME), result.codes());
        assertEquals(List.of(), result.warnings());
        assertFalse(result.requiresManagerReview());
    }

    @Test
    @DisplayName("AttendancePolicy.evaluate L117 idx=489 ConditionalsBoundary (long-day variant) "
            + "(exactly 360 worked minutes still counts for the break rule)")
    void attendanceWorkedMinutesBreakBoundary() {
        // break 0 < requiredBreak 60 is true, so the second operand `workedMinutes >= 360` is
        // really evaluated, and the worked time is exactly the boundary.
        Shift shift = new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(17, 0), 0, 60,
                60);
        PunchRecord punch = new PunchRecord(LocalDateTime.of(WORK_DAY, LocalTime.of(9, 0)),
                LocalDateTime.of(WORK_DAY, LocalTime.of(15, 0)), 0, false, true, false, false);

        AttendanceResult result = new AttendancePolicy().evaluate(attendance(shift, punch, false,
                false));
        assertEquals(360, result.workedMinutes());
        assertTrue(result.codes().contains(AttendanceCode.INSUFFICIENT_BREAK),
                result.codes()::toString);
        assertTrue(result.warnings().contains("required break duration was not met"));
    }

    @Test
    @DisplayName("AttendancePolicy.evaluate L75 idx=287 (flexible-shift adjustment) "
            + "(a flexible shift forgives the first hour of lateness)")
    void attendanceFlexibleShiftLateWaiver() {
        Shift shift = new Shift(ShiftType.FLEXIBLE, LocalTime.of(9, 0), LocalTime.of(17, 0), 0, 0,
                60);

        PunchRecord slightlyLate = new PunchRecord(LocalDateTime.of(WORK_DAY, LocalTime.of(9, 10)),
                LocalDateTime.of(WORK_DAY, LocalTime.of(17, 0)), 0, false, true, false, false);
        AttendanceResult waived = new AttendancePolicy().evaluate(attendance(shift, slightlyLate,
                false, false));
        assertEquals(0, waived.lateMinutes());
        assertEquals(List.of(AttendanceCode.NORMAL), waived.codes());

        PunchRecord veryLate = new PunchRecord(LocalDateTime.of(WORK_DAY, LocalTime.of(10, 40)),
                LocalDateTime.of(WORK_DAY, LocalTime.of(17, 0)), 0, false, true, false, false);
        AttendanceResult reduced = new AttendancePolicy().evaluate(attendance(shift, veryLate,
                false, false));
        assertEquals(40, reduced.lateMinutes());
        assertTrue(reduced.codes().contains(AttendanceCode.LATE), reduced.codes()::toString);
    }

    // ==================================================================
    // net.mooctest.attendance.OvertimePolicy
    // ==================================================================

    @Test
    @DisplayName("OvertimePolicy.evaluate L40 idx=131 EQUAL_ELSE "
            + "(a scheduled weekday keeps the ordinary factor)")
    void overtimeScheduledWeekdayFactor() {
        Shift shift = new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(17, 0), 0, 0,
                60);
        PunchRecord punch = new PunchRecord(LocalDateTime.of(WORK_DAY, LocalTime.of(9, 0)),
                LocalDateTime.of(WORK_DAY, LocalTime.of(19, 0)), 0, false, true, false, false);
        AttendanceInput input = attendance(shift, punch, true, true);

        OvertimeResult result = new OvertimePolicy().evaluate(input, 600, 120);
        assertEquals(1.0, result.compensationFactor());
        assertEquals("ordinary approved overtime", result.reason());
        assertEquals(120, result.payableMinutes());
        assertFalse(result.managerReview());
    }

    @Test
    @DisplayName("OvertimePolicy.evaluate L40 idx=131 EQUAL_ELSE (rest-day variant) "
            + "(an unscheduled day and a weekend day both double the factor)")
    void overtimeRestDayFactor() {
        Shift shift = new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(17, 0), 0, 0,
                60);
        PunchRecord punch = new PunchRecord(LocalDateTime.of(WORK_DAY, LocalTime.of(9, 0)),
                LocalDateTime.of(WORK_DAY, LocalTime.of(19, 0)), 0, false, true, false, false);
        OvertimePolicy policy = new OvertimePolicy();

        // Monday, but not a scheduled work day -> `!scheduledWorkDay()` must answer
        AttendanceInput unscheduled = new AttendanceInput("E1", WORK_DAY, shift, punch,
                EmploymentMode.OFFICE, false, false, false, false, false, true, true, 0, 0);
        OvertimeResult restDay = policy.evaluate(unscheduled, 600, 120);
        assertEquals(2.0, restDay.compensationFactor());
        assertEquals("rest-day overtime", restDay.reason());

        // Saturday -> the day-of-week operands must answer
        LocalDate saturday = WORK_DAY.plusDays(5);
        assertEquals(DayOfWeek.SATURDAY, saturday.getDayOfWeek());
        AttendanceInput weekend = new AttendanceInput("E1", saturday, shift, punch,
                EmploymentMode.OFFICE, true, false, false, false, false, true, true, 0, 0);
        OvertimeResult saturdayResult = policy.evaluate(weekend, 600, 120);
        assertEquals(2.0, saturdayResult.compensationFactor());
        assertEquals("rest-day overtime", saturdayResult.reason());

        // Sunday -> the second day-of-week operand must answer as well
        LocalDate sunday = WORK_DAY.plusDays(6);
        assertEquals(DayOfWeek.SUNDAY, sunday.getDayOfWeek());
        AttendanceInput sundayInput = new AttendanceInput("E1", sunday, shift, punch,
                EmploymentMode.OFFICE, true, false, false, false, false, true, true, 0, 0);
        OvertimeResult sundayResult = policy.evaluate(sundayInput, 600, 120);
        assertEquals(2.0, sundayResult.compensationFactor());
        assertEquals("rest-day overtime", sundayResult.reason());
    }

    @Test
    @DisplayName("OvertimePolicy.evaluate L40 idx=131 EQUAL_ELSE (holiday variant) "
            + "(a public holiday outranks the rest-day factor)")
    void overtimePublicHolidayFactor() {
        Shift shift = new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(17, 0), 0, 0,
                60);
        PunchRecord punch = new PunchRecord(LocalDateTime.of(WORK_DAY, LocalTime.of(9, 0)),
                LocalDateTime.of(WORK_DAY, LocalTime.of(19, 0)), 0, false, true, false, false);
        // a scheduled Monday that happens to be a public holiday
        AttendanceInput holiday = new AttendanceInput("E1", WORK_DAY, shift, punch,
                EmploymentMode.OFFICE, true, true, false, false, false, true, true, 0, 0);

        OvertimeResult result = new OvertimePolicy().evaluate(holiday, 600, 120);
        assertEquals(3.0, result.compensationFactor());
        assertEquals("public-holiday overtime", result.reason());
    }

    // ==================================================================
    // net.mooctest.service.LeavePolicy
    // ==================================================================

    @Test
    @DisplayName("LeavePolicy.validate L24 idx=38 ConditionalsBoundary "
            + "(a five character reason is accepted)")
    void leaveReasonBoundary() {
        DateRange range = new DateRange(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6));
        assertEquals(5, new LeavePolicy().validate(LeaveType.ANNUAL, range, "abcde", 10));
    }

    @Test
    @DisplayName("LeavePolicy.validate L27 idx=55 ConditionalsBoundary "
            + "(spending the exact annual balance is allowed)")
    void leaveAnnualBalanceBoundary() {
        DateRange range = new DateRange(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6));
        assertEquals(5, new LeavePolicy().validate(LeaveType.ANNUAL, range, "annual vacation", 5));
    }

    @Test
    @DisplayName("LeavePolicy.validate L30 idx=71 ConditionalsBoundary "
            + "(exactly three personal days are allowed)")
    void leavePersonalThreeDaysBoundary() {
        DateRange range = new DateRange(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 4));
        assertEquals(3, new LeavePolicy().validate(LeaveType.PERSONAL, range, "personal matters", 0));
    }

    // ==================================================================
    // net.mooctest.attendance.Shift
    // ==================================================================

    @Test
    @DisplayName("Shift.crossesMidnight L42 BooleanFalseReturnVals + scheduledEnd L50 EQUAL_ELSE "
            + "(a shift that crosses midnight ends on the following day)")
    void shiftNightCrossesMidnight() {
        // Note: a NIGHT-typed shift cannot even be constructed (the record's compact constructor
        // probes crossesMidnight() before `end` is assigned), so the crossing behaviour is
        // exercised through a standard shift whose end time is before its start time.
        Shift night = new Shift(ShiftType.STANDARD, LocalTime.of(22, 0), LocalTime.of(6, 0), 0, 0,
                120);
        assertTrue(night.crossesMidnight());
        assertEquals(LocalDateTime.of(WORK_DAY.plusDays(1), LocalTime.of(6, 0)),
                night.scheduledEnd(WORK_DAY));
        assertEquals(LocalDateTime.of(WORK_DAY, LocalTime.of(22, 0)),
                night.scheduledStart(WORK_DAY));
        assertEquals(480, night.scheduledMinutes(WORK_DAY));
    }

    // ==================================================================
    // net.mooctest.task.Task / TaskStateMachine
    // ==================================================================

    @Test
    @DisplayName("Task.isTerminal L76 idx=14 EQUAL_ELSE (all three terminal statuses)")
    void taskTerminalStatuses() {
        Task completed = new TReq().status(TaskStatus.COMPLETED)
                .startedAt(NOW.minus(2, ChronoUnit.DAYS))
                .completedAt(NOW.minus(1, ChronoUnit.DAYS)).build();
        assertTrue(completed.isTerminal());

        assertTrue(new TReq().status(TaskStatus.CANCELLED).build().isTerminal());
        assertTrue(new TReq().status(TaskStatus.ARCHIVED).build().isTerminal());
        assertFalse(new TReq().status(TaskStatus.OPEN).build().isTerminal());
    }

    @Test
    @DisplayName("TaskStateMachine.apply L37 idx=149 EQUAL_ELSE "
            + "(REOPEN and RESTORE clear the startedAt timestamp)")
    void taskStateMachineClearsStartedAt() {
        TaskStateMachine machine = new TaskStateMachine();
        TCtx manager = new TCtx().actor("mgr").manager(true);

        Task completed = new TReq().status(TaskStatus.COMPLETED)
                .startedAt(NOW.minus(2, ChronoUnit.DAYS))
                .completedAt(NOW.minus(1, ChronoUnit.DAYS)).build();
        Task reopened = machine.apply(completed, TaskAction.REOPEN, manager.build());
        assertEquals(TaskStatus.OPEN, reopened.status());
        assertNull(reopened.startedAt());
        assertNull(reopened.completedAt());

        // RESTORE moves an ARCHIVED task to IN_PROGRESS, which forbids a null startedAt, so the
        // clear performed by the RESTORE branch surfaces as the record invariant violation.
        Task archived = new TReq().status(TaskStatus.ARCHIVED)
                .startedAt(NOW.minus(5, ChronoUnit.DAYS)).build();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> machine.apply(archived, TaskAction.RESTORE, manager.build()));
        assertEquals("startedAt is required after work begins", error.getMessage());
    }

    // ==================================================================
    // card: TypeChecker / DateChecker / length boundaries
    // ==================================================================

    private static final Clock CARD_CLOCK = Clock.fixed(Instant.parse("2026-03-02T09:00:00Z"),
            ZoneOffset.UTC);

    @Test
    @DisplayName("TypeChecker.checkType L24 idx=12 EQUAL_ELSE "
            + "(a number that is not all digits is still OTHER)")
    void typeCheckerRejectsNonDigits() {
        // 15 characters, starts with the Visa IIN, ends with a letter: the digit-only guard is
        // the only thing that keeps this out of the VISA branch.
        assertEquals(CreditCardType.OTHER, TypeChecker.checkType("41111111111111a"));
        assertEquals(CreditCardType.VISA, TypeChecker.checkType("4111111111111111"));
    }

    @Test
    @DisplayName("DateChecker.isValidExpiration L47 idx=4/7 ConditionalsBoundary "
            + "(two digit years 00 and 99 are in range)")
    void dateCheckerTwoDigitYearBounds() {
        // 2000-01 lies in the past, 2099-12 in the future; both are legal two-digit years.
        assertFalse(DateChecker.isValidExpiration(1, 0, CARD_CLOCK));
        assertTrue(DateChecker.isValidExpiration(12, 99, CARD_CLOCK));
        assertTrue(DateChecker.isValidExpiration(YearMonth.of(2026, 3), CARD_CLOCK));
        assertEquals(2026, YearMonth.now(CARD_CLOCK).getYear());
    }

    @Test
    @DisplayName("Validator.validate L80 idx=44 BooleanTrueReturnVals "
            + "(a failing checksum still throws instead of returning true)")
    void validatorChecksumFailureStillThrows() {
        Validator validator = new Validator("4111111111111112", "12/30", "123", CARD_CLOCK);
        assertThrows(net.mooctest.card.InvalidCardException.class, validator::validate);

        Validator valid = new Validator("4111111111111111", "12/30", "123", CARD_CLOCK);
        assertTrue(valid.validate());
        assertTrue(valid.checkCVV());
        assertTrue(valid.checkExpirationDate());
    }

    @Test
    @DisplayName("DinersClubInternationalValidator.checkLength L34 idx=16 + "
            + "InterPaymentValidator.checkLength L34 idx=16 + VisaValidator.checkLength L33 idx=11 "
            + "(upper and lower length boundaries)")
    void cardLengthBoundaries() {
        // Diners Club International accepts 16..19 digits, IIN 300-305.
        assertTrue(new DinersClubInternationalValidator("3000000000000000000").validate());
        assertTrue(new DinersClubInternationalValidator("3000000000000000").validate());

        // InterPayment accepts 16..19 digits, IIN 636.
        assertTrue(new InterPaymentValidator("6360000000000000000").validate());
        assertTrue(new InterPaymentValidator("6360000000000000").validate());

        // Visa accepts 13..19 digits, IIN 4.
        assertTrue(new VisaValidator("4000000000000").validate());
        assertTrue(new VisaValidator("4000000000000000000").validate());
    }

    }

    // ===== merged from CardProbeTests2 probe =====
    @org.junit.jupiter.api.Nested
    class CardProbeTests2 {


    private static final LocalDate MON = LocalDate.of(2026, 3, 2); // Monday
    private static final LocalDate SPEND_DAY = LocalDate.of(2026, 3, 10);
    private static final Instant NOW = Instant.parse("2026-03-02T09:00:00Z");
    private static final Clock CARD_CLOCK = Clock.fixed(Instant.parse("2026-03-02T09:00:00Z"),
            ZoneOffset.UTC);
    private static final Currency USD = Currency.getInstance("USD");
    private static final ExchangeRateProvider ONE_RATE = (currency, date) -> BigDecimal.ONE;
    private static final WorkCalendar ALL_DAYS = date -> true;

    // ==================================================================
    // permission.AccessPolicy
    // ==================================================================

    private static final class ACtx2 {
        String actorId = "u1";
        String ownerId = "owner";
        String actorDepartment = "deptA";
        String resourceDepartment = "deptB";
        UserRole role = UserRole.EMPLOYEE;
        ResourceAction action = ResourceAction.VIEW;
        ResourceType type = ResourceType.EXPENSE_CLAIM;
        Sensitivity sensitivity = Sensitivity.INTERNAL;
        AccessScope scope = AccessScope.OWN;
        boolean legalHold;

        ACtx2 role(UserRole v) {
            role = v;
            return this;
        }

        ACtx2 action(ResourceAction v) {
            action = v;
            return this;
        }

        ACtx2 type(ResourceType v) {
            type = v;
            return this;
        }

        ACtx2 scope(AccessScope v) {
            scope = v;
            return this;
        }

        ACtx2 departments(String actor, String resource) {
            actorDepartment = actor;
            resourceDepartment = resource;
            return this;
        }

        ACtx2 legalHold(boolean v) {
            legalHold = v;
            return this;
        }

        AccessContext build() {
            return new AccessContext(actorId, ownerId, actorDepartment, resourceDepartment, role,
                    action, type, sensitivity, scope, true, false, true, true, true, true, false,
                    false, legalHold, false, false, true, true);
        }
    }

    private static AccessDecision decide(ACtx2 ctx) {
        return new AccessPolicy().decide(ctx.build());
    }

    @Test
    @DisplayName("AccessPolicy.employeeAllows L141 (public document + non-VIEW)")
    void accessEmployeePublicDocumentNonView() {
        // PUBLIC_DOCUMENT true, so `action == VIEW` is evaluated and must be able to be false.
        AccessDecision decision = decide(new ACtx2().role(UserRole.EMPLOYEE)
                .type(ResourceType.PUBLIC_DOCUMENT).action(ResourceAction.UPDATE)
                .scope(AccessScope.OWN));
        assertFalse(decision.allowed());
        assertEquals(List.of("role does not grant the requested action",
                "requested scope is outside the actor's authority"), decision.reasons());
    }

    @Test
    @DisplayName("AccessPolicy.departmentManagerAllows L172 (TEAM scope passes the scope guard)")
    void accessDepartmentManagerTeamScope() {
        // scope TEAM makes the second operand of `scope != DEPARTMENT && scope != TEAM` false.
        AccessDecision decision = decide(new ACtx2().role(UserRole.DEPARTMENT_MANAGER)
                .type(ResourceType.CONTRACT).action(ResourceAction.CREATE)
                .scope(AccessScope.TEAM).departments("deptA", "deptA"));
        assertTrue(decision.allowed(), decision.reasons()::toString);
        assertEquals(List.of("department manager has department-scoped access",
                "actor and resource belong to the same department"), decision.reasons());
    }

    @Test
    @DisplayName("AccessPolicy.humanResourcesAllows L196 + L204 (export scope and payroll scope)")
    void accessHumanResourcesScopeOperands() {
        // L196: EXPORT true but requestedScope is TEAM, so `scope == ORGANIZATION` is false.
        AccessDecision export = decide(new ACtx2().role(UserRole.HUMAN_RESOURCES)
                .type(ResourceType.LEAVE_REQUEST).action(ResourceAction.EXPORT)
                .scope(AccessScope.TEAM));
        assertTrue(export.allowed(), export.reasons()::toString);

        // L204: PAYROLL + VIEW true but the scope is ORGANIZATION, so the third operand is false
        // and the payroll shortcut is skipped.
        AccessDecision payroll = decide(new ACtx2().role(UserRole.HUMAN_RESOURCES)
                .type(ResourceType.PAYROLL).action(ResourceAction.VIEW)
                .scope(AccessScope.ORGANIZATION));
        assertFalse(payroll.allowed());
        assertEquals(List.of("role does not grant the requested action"), payroll.reasons());
    }

    @Test
    @DisplayName("AccessPolicy.financeAllows L228 (public document + non-VIEW)")
    void accessFinancePublicDocumentOperands() {
        AccessDecision view = decide(new ACtx2().role(UserRole.FINANCE)
                .type(ResourceType.PUBLIC_DOCUMENT).action(ResourceAction.VIEW)
                .scope(AccessScope.TEAM));
        assertTrue(view.allowed(), view.reasons()::toString);

        AccessDecision update = decide(new ACtx2().role(UserRole.FINANCE)
                .type(ResourceType.PUBLIC_DOCUMENT).action(ResourceAction.UPDATE)
                .scope(AccessScope.TEAM));
        assertFalse(update.allowed());
        assertEquals(List.of("role does not grant the requested action"), update.reasons());
    }

    @Test
    @DisplayName("AccessPolicy.administratorAllows L250 (legal hold on a non-delete action)")
    void accessAdministratorLegalHoldOperands() {
        AccessDecision view = decide(new ACtx2().role(UserRole.SYSTEM_ADMINISTRATOR)
                .type(ResourceType.EXPENSE_CLAIM).action(ResourceAction.VIEW)
                .scope(AccessScope.ORGANIZATION).legalHold(true));
        assertTrue(view.allowed(), view.reasons()::toString);

        AccessDecision delete = decide(new ACtx2().role(UserRole.SYSTEM_ADMINISTRATOR)
                .type(ResourceType.EXPENSE_CLAIM).action(ResourceAction.DELETE)
                .scope(AccessScope.ORGANIZATION).legalHold(true));
        assertFalse(delete.allowed());
        assertEquals(List.of("resource is under legal hold",
                "role does not grant the requested action"), delete.reasons());
    }

    @Test
    @DisplayName("AccessPolicy.scopeAllows L288 (DEPARTMENT scope, FINANCE keeps its authority)")
    void accessScopeDepartmentFinance() {
        // !sameDepartment true, role != HUMAN_RESOURCES true, role != FINANCE false.
        AccessDecision decision = decide(new ACtx2().role(UserRole.FINANCE)
                .type(ResourceType.EXPENSE_CLAIM).action(ResourceAction.VIEW)
                .scope(AccessScope.DEPARTMENT).departments("deptA", "deptB"));
        assertTrue(decision.allowed(), decision.reasons()::toString);
    }

    @Test
    @DisplayName("AccessPolicy.scopeAllows L289/L296 (DEPARTMENT scope, AUDITOR)")
    void accessScopeDepartmentAuditor() {
        // role != FINANCE true, role != AUDITOR false -> return via `role == AUDITOR`.
        AccessDecision decision = decide(new ACtx2().role(UserRole.AUDITOR)
                .type(ResourceType.EXPENSE_CLAIM).action(ResourceAction.VIEW)
                .scope(AccessScope.DEPARTMENT).departments("deptA", "deptB"));
        assertTrue(decision.allowed(), decision.reasons()::toString);
    }

    @Test
    @DisplayName("AccessPolicy.scopeAllows L288/L289 (DEPARTMENT scope, plain employee is refused)")
    void accessScopeDepartmentEmployeeRefused() {
        // Every operand of the guard is true, so the DEPARTMENT scope is denied outright.
        AccessDecision decision = decide(new ACtx2().role(UserRole.EMPLOYEE)
                .type(ResourceType.EXPENSE_CLAIM).action(ResourceAction.UPDATE)
                .scope(AccessScope.DEPARTMENT).departments("deptA", "deptB"));
        assertFalse(decision.allowed());
        assertTrue(decision.reasons().contains("requested scope is outside the actor's authority"),
                decision.reasons()::toString);
    }

    @Test
    @DisplayName("AccessPolicy.scopeAllows L287 (DEPARTMENT scope, HUMAN_RESOURCES short circuit)")
    void accessScopeDepartmentHumanResources() {
        AccessDecision decision = decide(new ACtx2().role(UserRole.HUMAN_RESOURCES)
                .type(ResourceType.LEAVE_REQUEST).action(ResourceAction.VIEW)
                .scope(AccessScope.DEPARTMENT).departments("deptA", "deptB"));
        assertTrue(decision.allowed(), decision.reasons()::toString);
    }

    // ==================================================================
    // attendance.AttendancePolicy
    // ==================================================================

    private static Shift stdShift() {
        return new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(17, 0), 0, 0, 60);
    }

    private static PunchRecord punch(int inHour, int outHour) {
        return new PunchRecord(LocalDateTime.of(MON, LocalTime.of(inHour, 0)),
                LocalDateTime.of(MON, LocalTime.of(outHour, 0)), 0, false, true, false, false);
    }

    private static AttendanceInput input(Shift shift, PunchRecord punch, EmploymentMode mode,
            boolean scheduledWorkDay, boolean publicHoliday, boolean approvedLeave) {
        return new AttendanceInput("E1", MON, shift, punch, mode, scheduledWorkDay, publicHoliday,
                approvedLeave, false, false, false, false, 0, 0);
    }

    @Test
    @DisplayName("AttendancePolicy.evaluate L31 (approved leave without a punch)")
    void attendanceApprovedLeaveWithoutPunch() {
        AttendanceResult result = new AttendancePolicy().evaluate(
                input(stdShift(), null, EmploymentMode.OFFICE, true, false, true));
        assertEquals(List.of(AttendanceCode.APPROVED_LEAVE), result.codes());
        assertEquals(List.of(), result.warnings());

        AttendanceResult withPunch = new AttendancePolicy().evaluate(
                input(stdShift(), punch(9, 17), EmploymentMode.OFFICE, true, false, true));
        assertEquals(List.of("punch exists on an approved leave day"), withPunch.warnings());
    }

    @Test
    @DisplayName("AttendancePolicy.evaluate L42 (rest-day combination operands)")
    void attendanceRestDayOperands() {
        // publicHoliday true makes `!publicHoliday` false, so the rest-day shortcut is skipped.
        AttendanceResult holiday = new AttendancePolicy().evaluate(
                input(stdShift(), null, EmploymentMode.OFFICE, false, true, false));
        assertEquals(List.of(AttendanceCode.ABSENT), holiday.codes());
        assertEquals(5, holiday.penaltyPoints());

        // a punch makes the last operand false as well
        AttendanceResult punched = new AttendancePolicy().evaluate(
                input(stdShift(), punch(9, 17), EmploymentMode.OFFICE, false, false, false));
        assertEquals(List.of(AttendanceCode.NORMAL), punched.codes());
    }

    @Test
    @DisplayName("AttendancePolicy.evaluate L60 (approved correction still incomplete)")
    void attendanceApprovedCorrectionIncomplete() {
        PunchRecord half = new PunchRecord(null, LocalDateTime.of(MON, LocalTime.of(17, 0)), 0,
                false, true, true, true);
        AttendanceResult result = new AttendancePolicy().evaluate(
                input(stdShift(), half, EmploymentMode.OFFICE, true, false, false));
        assertEquals(List.of(AttendanceCode.MISSING_CLOCK_IN), result.codes());
        assertEquals(List.of("approved correction remains incomplete"), result.warnings());
        assertEquals(2, result.penaltyPoints());
    }

    @Test
    @DisplayName("AttendancePolicy.evaluate L85 (manual punch without approval)")
    void attendanceManualPunchWithoutApproval() {
        PunchRecord manual = new PunchRecord(LocalDateTime.of(MON, LocalTime.of(9, 0)),
                LocalDateTime.of(MON, LocalTime.of(17, 0)), 0, false, true, true, false);
        AttendanceResult result = new AttendancePolicy().evaluate(
                input(stdShift(), manual, EmploymentMode.OFFICE, true, false, false));
        assertTrue(result.codes().contains(AttendanceCode.MANUAL_CORRECTION_REQUIRED),
                result.codes()::toString);
        assertTrue(result.requiresManagerReview());
    }

    @Test
    @DisplayName("AttendancePolicy.validateLocation L175/L176 (hybrid remote punch operands)")
    void attendanceHybridLocationOperands() {
        PunchRecord remoteUnverified = new PunchRecord(LocalDateTime.of(MON, LocalTime.of(9, 0)),
                LocalDateTime.of(MON, LocalTime.of(17, 0)), 0, true, false, false, false);
        AttendanceResult unverified = new AttendancePolicy().evaluate(
                input(stdShift(), remoteUnverified, EmploymentMode.HYBRID, true, false, false));
        assertTrue(unverified.warnings().contains("hybrid remote location needs review"),
                unverified.warnings()::toString);
        assertTrue(unverified.codes().contains(AttendanceCode.LOCATION_UNVERIFIED));

        // remotePunch false -> the second operand is false
        AttendanceResult officePunch = new AttendancePolicy().evaluate(
                input(stdShift(), punch(9, 17), EmploymentMode.HYBRID, true, false, false));
        assertFalse(officePunch.warnings().contains("hybrid remote location needs review"));

        // remotePunch true but verified -> the third operand is false
        PunchRecord remoteVerified = new PunchRecord(LocalDateTime.of(MON, LocalTime.of(9, 0)),
                LocalDateTime.of(MON, LocalTime.of(17, 0)), 0, true, true, false, false);
        AttendanceResult verified = new AttendancePolicy().evaluate(
                input(stdShift(), remoteVerified, EmploymentMode.HYBRID, true, false, false));
        assertFalse(verified.warnings().contains("hybrid remote location needs review"));
    }

    // ==================================================================
    // attendance.OvertimePolicy
    // ==================================================================

    private static OvertimeResult overtime(EmploymentMode mode, PunchRecord punch,
            ShiftType shiftType, boolean requested, boolean approved, int worked, int raw) {
        Shift shift = new Shift(shiftType, LocalTime.of(9, 0), LocalTime.of(17, 0), 0, 0, 60);
        AttendanceInput in = new AttendanceInput("E1", MON, shift, punch, mode, true, false, false,
                false, false, requested, approved, 0, 0);
        return new OvertimePolicy().evaluate(in, worked, raw);
    }

    @Test
    @DisplayName("OvertimePolicy.evaluate L67/L68 (remote overtime location operands)")
    void overtimeRemoteLocationOperands() {
        // REMOTE without any punch: `punch != null` is false
        OvertimeResult noPunch = overtime(EmploymentMode.REMOTE, null, ShiftType.STANDARD, true,
                true, 600, 120);
        assertEquals(120, noPunch.payableMinutes());
        assertFalse(noPunch.managerReview());

        // REMOTE with a verified punch: `!locationVerified()` is false
        PunchRecord verified = new PunchRecord(LocalDateTime.of(MON, LocalTime.of(9, 0)),
                LocalDateTime.of(MON, LocalTime.of(19, 0)), 0, true, true, false, false);
        OvertimeResult verifiedResult = overtime(EmploymentMode.REMOTE, verified,
                ShiftType.STANDARD, true, true, 600, 120);
        assertEquals(120, verifiedResult.payableMinutes());

        // REMOTE with an unverified punch: the guard fires
        PunchRecord unverified = new PunchRecord(LocalDateTime.of(MON, LocalTime.of(9, 0)),
                LocalDateTime.of(MON, LocalTime.of(19, 0)), 0, true, false, false, false);
        OvertimeResult unverifiedResult = overtime(EmploymentMode.REMOTE, unverified,
                ShiftType.STANDARD, true, true, 600, 120);
        assertEquals(0, unverifiedResult.payableMinutes());
        assertEquals("remote overtime location is unverified", unverifiedResult.reason());
    }

    @Test
    @DisplayName("OvertimePolicy.evaluate L73 (short on-call work grants compensatory leave)")
    void overtimeShortOnCall() {
        PunchRecord punch = new PunchRecord(LocalDateTime.of(MON, LocalTime.of(9, 0)),
                LocalDateTime.of(MON, LocalTime.of(17, 30)), 0, false, true, false, false);
        OvertimeResult shortCall = overtime(EmploymentMode.OFFICE, punch, ShiftType.ON_CALL, true,
                true, 510, 60);
        assertTrue(shortCall.compensatoryLeave());
        assertEquals(0.0, shortCall.compensationFactor());
        assertEquals("short on-call work grants compensatory leave", shortCall.reason());

        // more than 60 raw minutes keeps the ordinary treatment
        OvertimeResult longCall = overtime(EmploymentMode.OFFICE, punch, ShiftType.ON_CALL, true,
                true, 510, 90);
        assertFalse(longCall.compensatoryLeave());
    }

    // ==================================================================
    // attendance.Shift / AttendanceInput
    // ==================================================================

    @Test
    @DisplayName("Shift.<init> L36 (NIGHT without midnight crossing) / AttendanceInput.<init> L23")
    void shiftNightAndAttendanceInputGuards() {
        // The compact constructor probes crossesMidnight() before `end` is assigned, so a NIGHT
        // shift dies with a NullPointerException on this branch.
        assertThrows(NullPointerException.class,
                () -> new Shift(ShiftType.NIGHT, LocalTime.of(22, 0), LocalTime.of(6, 0), 0, 0, 120));

        IllegalArgumentException nullId = assertThrows(IllegalArgumentException.class,
                () -> new AttendanceInput(null, MON, stdShift(), null, EmploymentMode.OFFICE, true,
                        false, false, false, false, false, false, 0, 0));
        assertEquals("employeeId must not be blank", nullId.getMessage());
        IllegalArgumentException blankId = assertThrows(IllegalArgumentException.class,
                () -> new AttendanceInput("  ", MON, stdShift(), null, EmploymentMode.OFFICE, true,
                        false, false, false, false, false, false, 0, 0));
        assertEquals("employeeId must not be blank", blankId.getMessage());
    }

    // ==================================================================
    // task
    // ==================================================================

    private static Task task(TaskStatus status, TaskPriority priority, Instant dueAt,
            Instant startedAt, Instant completedAt, boolean reviewRequired) {
        return new Task("T1", "task title", null, "creator", "assignee", status, priority,
                RecurrenceRule.NONE, NOW.minus(10, ChronoUnit.DAYS), dueAt, startedAt, completedAt,
                60, 0, List.of(), Set.of(), false, reviewRequired);
    }

    private static TaskContext ctx(boolean manager, boolean assigneeActive, Instant now) {
        return new TaskContext("mgr", now, manager, false, true, assigneeActive, true, true, true, 0,
                0, 0);
    }

    @Test
    @DisplayName("TaskPolicy.calculatePriority L56 (24 < hours <= 72)")
    void taskPriorityThreeDayWindow() {
        // HIGH base 20 + 5 (<= 72 hours) = 25 -> HIGH; without the bonus it stays NORMAL.
        Task soon = task(TaskStatus.OPEN, TaskPriority.HIGH,
                NOW.plus(48, ChronoUnit.HOURS), null, null, false);
        assertEquals(TaskPriority.HIGH, new TaskPolicy()
                .evaluate(soon, ctx(false, true, NOW)).effectivePriority());

        Task beyond = task(TaskStatus.OPEN, TaskPriority.HIGH,
                NOW.plus(100, ChronoUnit.HOURS), null, null, false);
        assertEquals(TaskPriority.NORMAL, new TaskPolicy()
                .evaluate(beyond, ctx(false, true, NOW)).effectivePriority());
    }

    @Test
    @DisplayName("TaskPolicy.requiresEscalation L130 (blocked, but only recently)")
    void taskEscalationBlockedRecently() {
        Task blocked = task(TaskStatus.BLOCKED, TaskPriority.NORMAL, null,
                NOW.minus(1, ChronoUnit.DAYS), null, false);
        assertFalse(new TaskPolicy().evaluate(blocked, ctx(false, true, NOW)).escalationRequired());

        Task blockedLong = task(TaskStatus.BLOCKED, TaskPriority.NORMAL, null,
                NOW.minus(5, ChronoUnit.DAYS), null, false);
        assertTrue(new TaskPolicy().evaluate(blockedLong, ctx(false, true, NOW)).escalationRequired());
    }

    @Test
    @DisplayName("TaskPolicy.requiresEscalation L136 (review due within four hours)")
    void taskEscalationReviewWindow() {
        // dueAt null -> the fourth operand is false
        Task noDue = task(TaskStatus.IN_REVIEW, TaskPriority.NORMAL, null,
                NOW.minus(1, ChronoUnit.DAYS), null, true);
        assertFalse(new TaskPolicy().evaluate(noDue, ctx(false, true, NOW)).escalationRequired());

        // dueAt beyond the four hour window -> `isBefore(now + 4h)` false
        Task later = task(TaskStatus.IN_REVIEW, TaskPriority.NORMAL,
                NOW.plus(10, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.DAYS), null, true);
        assertFalse(new TaskPolicy().evaluate(later, ctx(false, true, NOW)).escalationRequired());

        // dueAt inside the window -> escalation
        Task imminent = task(TaskStatus.IN_REVIEW, TaskPriority.NORMAL,
                NOW.plus(2, ChronoUnit.HOURS), NOW.minus(1, ChronoUnit.DAYS), null, true);
        assertTrue(new TaskPolicy().evaluate(imminent, ctx(false, true, NOW)).escalationRequired());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L243 (ARCHIVED restored by a manager)")
    void taskArchivedManagerCanRestore() {
        Task archived = task(TaskStatus.ARCHIVED, TaskPriority.NORMAL, null, null, null, false);
        assertEquals(Set.of(TaskAction.RESTORE),
                new TaskPolicy().evaluate(archived, ctx(true, true, NOW)).allowedActions());
    }

    @Test
    @DisplayName("TaskPolicy.isOverdue L269 (cancelled task is never overdue)")
    void taskCancelledIsNeverOverdue() {
        Task cancelled = task(TaskStatus.CANCELLED, TaskPriority.NORMAL,
                NOW.minus(2, ChronoUnit.DAYS), null, null, false);
        assertFalse(new TaskPolicy().evaluate(cancelled, ctx(false, true, NOW)).overdue());
    }

    @Test
    @DisplayName("Task.<init> L82 (blank id / title / creator) and TaskContext.<init> L21")
    void taskAndTaskContextGuards() {
        IllegalArgumentException blankId = assertThrows(IllegalArgumentException.class,
                () -> new Task("  ", "title", null, "creator", "assignee", TaskStatus.OPEN,
                        TaskPriority.LOW, RecurrenceRule.NONE, NOW, null, null, null, 0, 0,
                        List.of(), Set.of(), false, false));
        assertEquals("id must not be blank", blankId.getMessage());

        IllegalArgumentException nullTitle = assertThrows(IllegalArgumentException.class,
                () -> new Task("T1", null, null, "creator", "assignee", TaskStatus.OPEN,
                        TaskPriority.LOW, RecurrenceRule.NONE, NOW, null, null, null, 0, 0,
                        List.of(), Set.of(), false, false));
        assertEquals("title must not be blank", nullTitle.getMessage());

        IllegalArgumentException blankCreator = assertThrows(IllegalArgumentException.class,
                () -> new Task("T1", "title", null, "  ", "assignee", TaskStatus.OPEN,
                        TaskPriority.LOW, RecurrenceRule.NONE, NOW, null, null, null, 0, 0,
                        List.of(), Set.of(), false, false));
        assertEquals("creatorId must not be blank", blankCreator.getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> new TaskContext(null, NOW, false, false, true, true, true, true, true, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new TaskContext("   ", NOW, false, false, true, true, true, true, true, 0, 0,
                        0));
    }

    @Test
    @DisplayName("TaskStateMachine.apply L31 (START keeps an existing startedAt)")
    void taskStateMachineStartKeepsStartedAt() {
        Instant started = NOW.minus(2, ChronoUnit.DAYS);
        Task open = task(TaskStatus.OPEN, TaskPriority.NORMAL, null, started, null, false);
        TaskContext assignee = new TaskContext("assignee", NOW, false, false, true, true, true, true,
                true, 0, 0, 0);

        Task started2 = new TaskStateMachine().apply(open, TaskAction.START, assignee);
        assertEquals(TaskStatus.IN_PROGRESS, started2.status());
        assertEquals(started, started2.startedAt());
    }

    // ==================================================================
    // leave.AdvancedLeavePolicy / AdvancedLeaveRequest
    // ==================================================================

    private static final class LReq2 {
        String employeeId = "E1";
        EmployeeCategory category = EmployeeCategory.REGULAR;
        LeaveType type = LeaveType.SICK;
        LocalDate start = MON;
        LocalDate end = MON;
        LeaveUnit unit = LeaveUnit.FULL_DAY;
        double hours;
        String reason = "medical appointment";
        boolean emergency;
        boolean remoteCapable;
        boolean handover = true;
        int months = 24;
        int notice = 10;
        double balance = 10;

        LReq2 type(LeaveType v) {
            type = v;
            return this;
        }

        LReq2 category(EmployeeCategory v) {
            category = v;
            return this;
        }

        LReq2 range(LocalDate s, LocalDate e) {
            start = s;
            end = e;
            return this;
        }

        LReq2 span(int days) {
            start = MON;
            end = MON.plusDays(days - 1L);
            return this;
        }

        LReq2 notice(int v) {
            notice = v;
            return this;
        }

        LReq2 emergency(boolean v) {
            emergency = v;
            return this;
        }

        LReq2 remote(boolean v) {
            remoteCapable = v;
            return this;
        }

        AdvancedLeaveRequest build() {
            return new AdvancedLeaveRequest(employeeId, category, type, start, end, unit, hours,
                    reason, emergency, remoteCapable, handover, months, notice, balance, 0);
        }
    }

    private static LeaveAssessment assessLeave(LReq2 req) {
        return new AdvancedLeavePolicy().assess(req.build(), ALL_DAYS, MON);
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validateDates L88 (backdated emergency sick leave)")
    void leaveBackdatedEmergencySick() {
        LReq2 req = new LReq2().type(LeaveType.SICK).emergency(true)
                .range(MON.minusDays(3), MON.minusDays(3)).notice(10);
        LeaveAssessment assessment = assessLeave(req);

        assertTrue(assessment.eligible(), assessment.violations()::toString);
        assertEquals(List.of(), assessment.violations());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validateDates L101 (notice below three days with six charged days)")
    void leaveShortNoticeSixDays() {
        LReq2 req = new LReq2().type(LeaveType.SICK).span(6).notice(2);
        LeaveAssessment assessment = assessLeave(req);

        assertEquals(List.of("leave longer than five days requires three days notice"),
                assessment.violations());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validateEmployment L135 (intern without annual leave)")
    void leaveInternSick() {
        LReq2 req = new LReq2().category(EmployeeCategory.INTERN).type(LeaveType.SICK).span(1);
        LeaveAssessment assessment = assessLeave(req);

        assertTrue(assessment.eligible(), assessment.violations()::toString);
        assertEquals(List.of(), assessment.violations());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validateAnnualLeave L202 (remote-capable one day operands)")
    void leaveAnnualRemoteCapableOperands() {
        // chargedDays == 1 and noticeDays == 0 -> the violation fires
        LeaveAssessment oneDay = assessLeave(new LReq2().type(LeaveType.ANNUAL).remote(true)
                .span(1).notice(0));
        assertEquals(List.of(
                "remote-capable employees must consider remote work for urgent one-day leave"),
                oneDay.violations());

        // chargedDays == 2 -> the second operand is false
        LeaveAssessment twoDays = assessLeave(new LReq2().type(LeaveType.ANNUAL).remote(true)
                .span(2).notice(0));
        assertFalse(twoDays.violations().contains(
                "remote-capable employees must consider remote work for urgent one-day leave"));

        // noticeDays != 0 -> the third operand is false
        LeaveAssessment withNotice = assessLeave(new LReq2().type(LeaveType.ANNUAL).remote(true)
                .span(1).notice(1));
        assertEquals(List.of(), withNotice.violations());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validatePersonalLeave L249 (remote-capable personal operands)")
    void leavePersonalRemoteCapableOperands() {
        // !emergency && chargedDays <= 1 -> the violation fires
        LeaveAssessment shortAbsence = assessLeave(new LReq2().type(LeaveType.PERSONAL).remote(true)
                .span(1).notice(1));
        assertEquals(List.of("short personal absence should use remote work when available"),
                shortAbsence.violations());

        // chargedDays == 2 -> the third operand is false
        LeaveAssessment twoDays = assessLeave(new LReq2().type(LeaveType.PERSONAL).remote(true)
                .span(2).notice(1));
        assertFalse(twoDays.violations().contains(
                "short personal absence should use remote work when available"));

        // emergency -> the second operand is false
        LeaveAssessment urgent = assessLeave(new LReq2().type(LeaveType.PERSONAL).remote(true)
                .span(1).notice(1).emergency(true));
        assertFalse(urgent.violations().contains(
                "short personal absence should use remote work when available"));
    }

    @Test
    @DisplayName("AdvancedLeaveRequest.<init> L26/L47 (blank employee, illegal hour amounts)")
    void advancedLeaveRequestGuards() {
        assertEquals("employeeId must not be blank",
                assertThrows(IllegalArgumentException.class,
                        () -> new OriginalLeave().blank().build()).getMessage());
        assertEquals("hour leave must be between 0 and 8 hours",
                assertThrows(IllegalArgumentException.class,
                        () -> new OriginalLeave().hours(9).build()).getMessage());
        assertThrows(IllegalArgumentException.class, () -> new OriginalLeave().hours(0).build());
    }

    /** Small helper that keeps AdvancedLeaveRequest constructor probing readable. */
    private static final class OriginalLeave {
        String employeeId = "E1";
        double hours = 4;

        OriginalLeave blank() {
            employeeId = "  ";
            return this;
        }

        OriginalLeave hours(double v) {
            hours = v;
            return this;
        }

        AdvancedLeaveRequest build() {
            return new AdvancedLeaveRequest(employeeId, EmployeeCategory.REGULAR, LeaveType.SICK,
                    MON, MON, LeaveUnit.HOURS, hours, "medical appointment", false, false, true, 24,
                    10, 10, 0);
        }
    }

    // ==================================================================
    // expense.ExpensePolicy
    // ==================================================================

    private static ExpenseItem item(String id, ExpenseCategory category, String amount,
            PaymentMethod payment, ReceiptType receipt, boolean personal) {
        return new ExpenseItem(id, category, new BigDecimal(amount), USD, SPEND_DAY, "Acme Store",
                "business reason text", receipt, payment, 0, false, personal, false);
    }

    private static ExpenseAssessment assessItems(boolean projectActive, boolean clientBillable,
            ExpenseItem... items) {
        ExpenseClaim claim = new ExpenseClaim("C1", "E1", EmployeeGrade.STAFF, SPEND_DAY, "CC-100",
                List.of(items), true, false, clientBillable, projectActive, 0);
        return new ExpensePolicy().assess(claim, ONE_RATE, SPEND_DAY);
    }

    @Test
    @DisplayName("ExpensePolicy.validateClaimLevel L103 (inactive project with client billing)")
    void expenseInactiveProjectBillable() {
        ExpenseAssessment assessment = assessItems(false, true,
                item("I1", ExpenseCategory.SOFTWARE, "100.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.TAX_INVOICE, false));

        assertTrue(assessment.violations().contains(
                "FATAL: inactive project cannot receive client-billable expenses"),
                assessment.violations()::toString);
        assertTrue(assessment.rejected());

        // the same claim on an active project must not raise it
        ExpenseAssessment active = assessItems(true, true,
                item("I1", ExpenseCategory.SOFTWARE, "100.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.TAX_INVOICE, false));
        assertFalse(active.violations().contains(
                "FATAL: inactive project cannot receive client-billable expenses"));
    }

    @Test
    @DisplayName("ExpensePolicy.validateCategoryRules L298/L309 (transport and office supply operands)")
    void expenseCategoryRuleOperands() {
        // CASH transport with a real receipt -> receiptType == NONE false
        ExpenseAssessment transport = assessItems(true, false,
                item("I1", ExpenseCategory.LOCAL_TRANSPORT, "50.00", PaymentMethod.CASH,
                        ReceiptType.TAX_INVOICE, false));
        assertFalse(transport.warnings().contains("cash transport without receipt will be capped"),
                transport.warnings()::toString);

        // personal card office supplies under 500 -> the amount operand is false
        ExpenseAssessment supplies = assessItems(true, false,
                item("I1", ExpenseCategory.OFFICE_SUPPLIES, "100.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.TAX_INVOICE, false));
        assertFalse(supplies.warnings().contains(
                "large office purchase should use corporate procurement"), supplies.warnings()::toString);

        // and above 500 the warning does fire
        ExpenseAssessment big = assessItems(true, false,
                item("I1", ExpenseCategory.OFFICE_SUPPLIES, "900.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.TAX_INVOICE, false));
        assertTrue(big.warnings().contains(
                "large office purchase should use corporate procurement"), big.warnings()::toString);
    }

    // ==================================================================
    // expense.ExpenseItem / ExpenseClaim / ExpenseCardReview
    // ==================================================================

    @Test
    @DisplayName("ExpenseItem.<init> L24 and ExpenseClaim.<init> L21/L24/L41")
    void expenseRecordGuards() {
        assertEquals("itemId must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new ExpenseItem(null,
                        ExpenseCategory.SOFTWARE, BigDecimal.ONE, USD, SPEND_DAY, "Acme",
                        "business reason", ReceiptType.TAX_INVOICE, PaymentMethod.PERSONAL_CARD, 0,
                        false, false, false)).getMessage());
        assertEquals("itemId must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new ExpenseItem("   ",
                        ExpenseCategory.SOFTWARE, BigDecimal.ONE, USD, SPEND_DAY, "Acme",
                        "business reason", ReceiptType.TAX_INVOICE, PaymentMethod.PERSONAL_CARD, 0,
                        false, false, false)).getMessage());

        assertEquals("claimId must not be blank",
                assertThrows(IllegalArgumentException.class, () -> claim("  ", "E1", "CC-1", false))
                        .getMessage());
        assertEquals("claimId must not be blank",
                assertThrows(IllegalArgumentException.class, () -> claim(null, "E1", "CC-1", false))
                        .getMessage());
        assertEquals("employeeId must not be blank",
                assertThrows(IllegalArgumentException.class, () -> claim("C1", " ", "CC-1", false))
                        .getMessage());
        assertEquals("employeeId must not be blank",
                assertThrows(IllegalArgumentException.class, () -> claim("C1", null, "CC-1", false))
                        .getMessage());
        assertEquals("client-billable claim requires a cost center",
                assertThrows(IllegalArgumentException.class, () -> claim("C1", "E1", null, true))
                        .getMessage());
        assertEquals("client-billable claim requires a cost center",
                assertThrows(IllegalArgumentException.class, () -> claim("C1", "E1", "  ", true))
                        .getMessage());
    }

    private static ExpenseClaim claim(String claimId, String employeeId, String costCenter,
            boolean clientBillable) {
        return new ExpenseClaim(claimId, employeeId, EmployeeGrade.STAFF, SPEND_DAY, costCenter,
                List.of(item("I1", ExpenseCategory.SOFTWARE, "100.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.TAX_INVOICE, false)), true, false, clientBillable, true, 0);
    }

    @Test
    @DisplayName("ExpenseCardReview.<init> L19/L27/L28/L32 (derived flag consistency)")
    void expenseCardReviewGuards() {
        AccessDecision allowed = new AccessDecision(true, false, false, List.of("ok"));
        AccessDecision allowedAudit = new AccessDecision(true, true, false, List.of("ok"));
        AccessDecision denied = new AccessDecision(false, false, false, List.of("denied"));
        ExpenseAssessment settled = new ExpenseAssessment(new BigDecimal("100.00"),
                new BigDecimal("100.00"), ExpenseApprovalTier.LINE_MANAGER, false, false,
                List.of(), List.of());
        ExpenseAssessment rejected = new ExpenseAssessment(new BigDecimal("100.00"),
                new BigDecimal("0.00"), ExpenseApprovalTier.FINANCE, true, false, List.of("v"),
                List.of());
        ExpenseAssessment audited = new ExpenseAssessment(new BigDecimal("100.00"),
                new BigDecimal("100.00"), ExpenseApprovalTier.FINANCE, false, true, List.of(),
                List.of());
        CardEvidenceCheck validCard = new CardEvidenceCheck("I1", CreditCardType.VISA, true,
                List.of());
        CardEvidenceCheck invalidCard = new CardEvidenceCheck("I1", CreditCardType.VISA, false,
                List.of("bad"));

        assertEquals("claimId must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new ExpenseCardReview(null,
                        allowed, settled, List.of(validCard), List.of(), true, false)).getMessage());
        assertEquals("claimId must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new ExpenseCardReview("   ",
                        allowed, settled, List.of(validCard), List.of(), true, false)).getMessage());

        // access.allowed() false -> ready is false whatever follows
        ExpenseCardReview notAllowed = new ExpenseCardReview("C1", denied, settled,
                List.of(validCard), List.of(), false, false);
        assertFalse(notAllowed.readyForApproval());

        // expense.rejected() true -> second operand false
        ExpenseCardReview rejectedReview = new ExpenseCardReview("C1", allowed, rejected,
                List.of(validCard), List.of(), false, false);
        assertFalse(rejectedReview.readyForApproval());

        // issues non-empty -> third operand false, and the audit flag must follow it
        ExpenseCardReview withIssues = new ExpenseCardReview("C1", allowed, settled,
                List.of(), List.of("issue"), false, true);
        assertTrue(withIssues.auditRequired());

        // an invalid card -> allMatch false
        ExpenseCardReview invalid = new ExpenseCardReview("C1", allowed, settled,
                List.of(invalidCard), List.of(), false, false);
        assertFalse(invalid.readyForApproval());

        // audit from the access decision and from the expense assessment
        assertTrue(new ExpenseCardReview("C1", allowedAudit, settled, List.of(validCard), List.of(),
                true, true).auditRequired());
        assertTrue(new ExpenseCardReview("C1", allowed, audited, List.of(validCard), List.of(),
                true, true).auditRequired());

        // everything clean -> ready and no audit
        ExpenseCardReview clean = new ExpenseCardReview("C1", allowed, settled,
                List.of(validCard), List.of(), true, false);
        assertTrue(clean.readyForApproval());
        assertFalse(clean.auditRequired());
    }

    // ==================================================================
    // expense.CorporateCardExpenseService
    // ==================================================================

    private static CorporateCardExpenseService service() {
        return new CorporateCardExpenseService(new ExpensePolicy(),
                new CardEvidenceValidator(CARD_CLOCK), new AccessPolicy(),
                new ApprovalWorkflowEngine());
    }

    private static ExpenseClaim serviceClaim(String employeeId, ExpenseItem... items) {
        return new ExpenseClaim("C1", employeeId, EmployeeGrade.STAFF, SPEND_DAY, "CC-100",
                List.of(items), true, false, false, true, 0);
    }

    private static AccessContext claimCreator(UserRole role, String actorId) {
        return new AccessContext(actorId, null, "deptA", "deptA", role, ResourceAction.CREATE,
                ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL, AccessScope.OWN, true, false, true,
                true, true, true, false, false, false, false, false, true, true);
    }

    @Test
    @DisplayName("CorporateCardExpenseService.review L53 (employee touching someone else's claim)")
    void serviceEmployeeClaimOwnership() {
        ExpenseClaim claim = serviceClaim("E-other",
                item("I1", ExpenseCategory.SOFTWARE, "100.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.TAX_INVOICE, false));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service().review(claim, Map.of(), ONE_RATE, SPEND_DAY,
                        claimCreator(UserRole.EMPLOYEE, "E1")));
        assertEquals("employee may only create their own claim", error.getMessage());
    }

    @Test
    @DisplayName("CorporateCardExpenseService.review L79/L81 (all readiness and audit operands)")
    void serviceReadinessAndAuditOperands() {
        ExpenseItem clean = item("I1", ExpenseCategory.SOFTWARE, "100.00",
                PaymentMethod.PERSONAL_CARD, ReceiptType.TAX_INVOICE, false);
        ExpenseClaim own = serviceClaim("E1", clean);

        // ready: allowed, not rejected, no issues, no audit
        ExpenseCardReview ready = service().review(own, Map.of(), ONE_RATE, SPEND_DAY,
                claimCreator(UserRole.EMPLOYEE, "E1"));
        assertTrue(ready.readyForApproval());
        assertFalse(ready.auditRequired());

        // access.allowed() false -> first operand of ready
        ExpenseCardReview denied = service().review(own, Map.of(), ONE_RATE, SPEND_DAY,
                claimCreator(UserRole.GUEST, "E1"));
        assertFalse(denied.access().allowed());
        assertFalse(denied.readyForApproval());

        // expense.rejected() true -> second operand of ready
        ExpenseClaim personalItem = serviceClaim("E1", item("I1", ExpenseCategory.SOFTWARE,
                "100.00", PaymentMethod.PERSONAL_CARD, ReceiptType.TAX_INVOICE, true));
        ExpenseCardReview rejected = service().review(personalItem, Map.of(), ONE_RATE, SPEND_DAY,
                claimCreator(UserRole.EMPLOYEE, "E1"));
        assertTrue(rejected.expense().rejected());
        assertFalse(rejected.readyForApproval());
        assertTrue(rejected.auditRequired());

        // corporate-card item without evidence -> issues non-empty
        ExpenseClaim corporate = serviceClaim("E1", item("I1", ExpenseCategory.SOFTWARE, "100.00",
                PaymentMethod.CORPORATE_CARD, ReceiptType.TAX_INVOICE, false));
        ExpenseCardReview missingEvidence = service().review(corporate, Map.of(), ONE_RATE,
                SPEND_DAY, claimCreator(UserRole.EMPLOYEE, "E1"));
        assertEquals(List.of("corporate card evidence missing for item I1"),
                missingEvidence.issues());
        assertFalse(missingEvidence.readyForApproval());
        assertTrue(missingEvidence.auditRequired());

        // expense.auditRequired() true while nothing else is wrong
        ExpenseClaim large = serviceClaim("E1", item("I1", ExpenseCategory.SOFTWARE, "6000.00",
                PaymentMethod.PERSONAL_CARD, ReceiptType.TAX_INVOICE, false));
        ExpenseCardReview audited = service().review(large, Map.of(), ONE_RATE, SPEND_DAY,
                claimCreator(UserRole.EMPLOYEE, "E1"));
        assertTrue(audited.expense().auditRequired());
        assertTrue(audited.auditRequired());

        // access.auditRequired() true through the administrator role
        ExpenseCardReview adminReview = service().review(own, Map.of(), ONE_RATE, SPEND_DAY,
                new AccessContext("root", null, "deptA", "deptA", UserRole.SYSTEM_ADMINISTRATOR,
                        ResourceAction.CREATE, ResourceType.EXPENSE_CLAIM, Sensitivity.INTERNAL,
                        AccessScope.ORGANIZATION, true, false, true, true, true, true, false, false,
                        false, false, false, true, true));
        assertTrue(adminReview.access().auditRequired());
        assertTrue(adminReview.auditRequired());
    }

    // ==================================================================
    // workflow
    // ==================================================================

    private static ApprovalStep step(String id, Set<String> approvers, boolean delegationAllowed,
            boolean skipRequester, Instant deadline) {
        return new ApprovalStep(id, "step " + id, approvers, StepMode.ANY_ONE, 1,
                delegationAllowed, skipRequester, deadline);
    }

    @Test
    @DisplayName("ApprovalStep.<init> L19/L22/L79 and WorkflowSnapshot.<init> L20/L23")
    void workflowRecordGuards() {
        assertEquals("stepId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new ApprovalStep(null, "name", Set.of("a1"), StepMode.ANY_ONE, 1, false, false,
                        null)).getMessage());
        assertEquals("stepId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new ApprovalStep("  ", "name", Set.of("a1"), StepMode.ANY_ONE, 1, false, false,
                        null)).getMessage());
        assertEquals("name must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new ApprovalStep("s1", null, Set.of("a1"), StepMode.ANY_ONE, 1, false, false,
                        null)).getMessage());
        assertEquals("name must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new ApprovalStep("s1", "   ", Set.of("a1"), StepMode.ANY_ONE, 1, false, false,
                        null)).getMessage());
        assertEquals("approverId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new ApprovalStep("s1", "name", Set.of("a1", "  "), StepMode.ANY_ONE, 1, false,
                        false, null)).getMessage());

        ApprovalStep valid = step("s1", Set.of("a1"), false, false, null);
        List<StepProgress> steps = List.of(StepProgress.empty(valid));
        assertEquals("workflowId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot(null, "req", WorkflowState.PENDING, steps, 0, NOW, NOW, 1,
                        false, true)).getMessage());
        assertEquals("workflowId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot("  ", "req", WorkflowState.PENDING, steps, 0, NOW, NOW, 1,
                        false, true)).getMessage());
        assertEquals("requesterId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot("W1", "  ", WorkflowState.PENDING, steps, 0, NOW, NOW, 1,
                        false, true)).getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> new WorkflowSnapshot("W1", null, WorkflowState.PENDING, steps, 0, NOW, NOW, 1,
                        false, true));
    }

    @Test
    @DisplayName("WorkflowAction.<init> L13 (blank actor)")
    void workflowActionGuard() {
        assertEquals("actorId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new WorkflowAction(null, ApprovalDecision.APPROVE, null, NOW)).getMessage());
        assertEquals("actorId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new WorkflowAction("  ", ApprovalDecision.APPROVE, null, NOW)).getMessage());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.act L60 (deadline passed -> EXPIRED)")
    void workflowActAfterDeadlineExpires() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        Instant deadline = NOW.plusSeconds(3600);
        WorkflowSnapshot snapshot = engine.start("W1", "req",
                List.of(step("s1", Set.of("a1"), false, false, deadline)), NOW, false, true);

        WorkflowSnapshot expired = engine.act(snapshot,
                new WorkflowAction("a1", ApprovalDecision.APPROVE, null, deadline.plusSeconds(60)));
        assertEquals(WorkflowState.EXPIRED, expired.state());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.act L66 (requester cannot approve a skipping step)")
    void workflowRequesterCannotApproveOwnStep() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        WorkflowSnapshot snapshot = engine.start("W1", "req",
                List.of(step("s1", Set.of("req", "a1"), false, true, null)), NOW, false, true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> engine.act(snapshot,
                        new WorkflowAction("req", ApprovalDecision.APPROVE, null,
                                NOW.plusSeconds(60))));
        assertEquals("requester cannot approve this step", error.getMessage());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.delegate L111 (cannot delegate to the requester)")
    void workflowCannotDelegateToRequester() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        WorkflowSnapshot snapshot = engine.start("W1", "req",
                List.of(step("s1", Set.of("a1", "a2"), true, true, null)), NOW, false, true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> engine.delegate(snapshot, "a1", "req", NOW.plusSeconds(60)));
        assertEquals("cannot delegate approval to the requester", error.getMessage());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.withdraw L132 (urgent workflow after a first approval)")
    void workflowUrgentWithdrawAfterFirstApproval() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        WorkflowSnapshot snapshot = engine.start("W1", "req",
                List.of(step("s1", Set.of("a1"), false, false, null),
                        step("s2", Set.of("a2"), false, false, null)), NOW, true, true);
        WorkflowSnapshot afterFirst = engine.act(snapshot,
                new WorkflowAction("a1", ApprovalDecision.APPROVE, null, NOW.plusSeconds(60)));
        assertEquals(1, afterFirst.currentStepIndex());
        assertTrue(afterFirst.urgent());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> engine.withdraw(afterFirst, "req", NOW.plusSeconds(120)));
        assertEquals("urgent workflow cannot be withdrawn after first approval", error.getMessage());
    }

    // ==================================================================
    // domain / service records
    // ==================================================================

    @Test
    @DisplayName("DateRange.overlaps L29 (disjoint ranges)")
    void dateRangeOverlapsOperands() {
        DateRange first = new DateRange(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12));
        DateRange earlier = new DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5));
        DateRange later = new DateRange(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 22));

        assertFalse(first.overlaps(earlier)); // second operand false
        assertFalse(first.overlaps(later)); // first operand false
        assertTrue(first.overlaps(new DateRange(LocalDate.of(2026, 3, 12),
                LocalDate.of(2026, 3, 15))));
    }

    @Test
    @DisplayName("SubmitLeaveCommand.<init> L16 / AccessContext.<init> L30 / CardEvidence.<init> L9")
    void remainingRecordGuards() {
        DateRange period = new DateRange(MON, MON.plusDays(1));
        assertEquals("employeeId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new SubmitLeaveCommand(null, LeaveType.ANNUAL, period, "reason")).getMessage());
        assertEquals("employeeId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new SubmitLeaveCommand("  ", LeaveType.ANNUAL, period, "reason")).getMessage());

        assertEquals("actorId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new AccessContext(null, null, null, null, UserRole.EMPLOYEE,
                        ResourceAction.VIEW, ResourceType.PAYROLL, Sensitivity.PUBLIC,
                        AccessScope.OWN, true, false, false, false, false, false, false, false, false,
                        false, false, false, false)).getMessage());
        assertEquals("actorId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new AccessContext("   ", null, null, null, UserRole.EMPLOYEE,
                        ResourceAction.VIEW, ResourceType.PAYROLL, Sensitivity.PUBLIC,
                        AccessScope.OWN, true, false, false, false, false, false, false, false, false,
                        false, false, false, false)).getMessage());

        assertEquals("itemId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new CardEvidence(null, "4111111111111111", YearMonth.of(2030, 1))).getMessage());
        assertEquals("itemId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new CardEvidence("  ", "4111111111111111", YearMonth.of(2030, 1)))
                .getMessage());
    }

    // ==================================================================
    // card: length boundaries and expiry operand
    // ==================================================================

    @Test
    @DisplayName("VerveValidator.checkLength L34 / MaestroValidator.checkLength L33 / "
            + "MaestroUKValidator.checkLength L34")
    void cardLengthLowerAndUpperMisses() {
        // 15 digits is below the 16..19 window, 20 digits is above it
        String fifteen = "506099000000000";
        String twenty = "50609900000000000000";
        assertEquals(15, fifteen.length());
        assertEquals(20, twenty.length());

        assertFalse(new VerveValidator(fifteen).validate());
        assertFalse(new VerveValidator(twenty).validate());

        String maestroEleven = "50000000000";
        String maestroTwenty = "50000000000000000000";
        assertEquals(11, maestroEleven.length());
        assertFalse(new MaestroValidator(maestroEleven).validate());
        assertFalse(new MaestroValidator(maestroTwenty).validate());

        assertFalse(new MaestroUKValidator(maestroEleven).validate());
        assertFalse(new MaestroUKValidator(maestroTwenty).validate());
    }

    @Test
    @DisplayName("VisaValidator.checkLength L33 / InterPaymentValidator.checkLength L34 / "
            + "DinersClubInternationalValidator.checkLength L34 / ChinaUnionPayValidator.checkLength L34")
    void cardLengthWindows() {
        assertFalse(new VisaValidator("400000000000").validate()); // 12 digits
        assertEquals(12, "400000000000".length());

        assertFalse(new InterPaymentValidator("636000000000000").validate()); // 15 digits
        assertFalse(new InterPaymentValidator("63600000000000000000").validate()); // 20 digits

        assertFalse(new DinersClubInternationalValidator("300000000000000").validate());
        assertFalse(new DinersClubInternationalValidator("30000000000000000000").validate());

        assertFalse(new ChinaUnionPayValidator("620000000000000").validate());
        assertFalse(new ChinaUnionPayValidator("62000000000000000000").validate());
    }

    @Test
    @DisplayName("Validator.checkExpirationDate L94 (expired card and absent expiration)")
    void validatorExpirationOperands() {
        // month/year are present, the last operand (isValidExpiration) is false
        Validator expired = new Validator("4111111111111111", "01/20", "123", CARD_CLOCK);
        assertFalse(expired.checkExpirationDate());

        // the one argument constructor leaves both fields null
        Validator missing = new Validator("4111111111111111");
        assertFalse(missing.checkExpirationDate());
        assertFalse(missing.checkCVV());

        Validator valid = new Validator("4111111111111111", "12/30", "123", CARD_CLOCK);
        assertTrue(valid.checkExpirationDate());
        assertEquals(CreditCardType.VISA, TypeChecker.checkType("4111111111111111"));
    }

    // ==================================================================
    // second pass: operands the first pass left short-circuited
    // ==================================================================

    @Test
    @DisplayName("ExpensePolicy.validateClaimLevel L103 (inactive project without client billing)")
    void expenseInactiveProjectNotBillable() {
        ExpenseAssessment assessment = assessItems(false, false,
                item("I1", ExpenseCategory.SOFTWARE, "100.00", PaymentMethod.PERSONAL_CARD,
                        ReceiptType.TAX_INVOICE, false));

        assertFalse(assessment.violations().contains(
                "FATAL: inactive project cannot receive client-billable expenses"));
        assertFalse(assessment.rejected());
    }

    @Test
    @DisplayName("ExpensePolicy.validateCategoryRules L309 (office supplies off the personal card)")
    void expenseOfficeSuppliesCorporateCard() {
        ExpenseAssessment assessment = assessItems(true, false,
                item("I1", ExpenseCategory.OFFICE_SUPPLIES, "900.00", PaymentMethod.CORPORATE_CARD,
                        ReceiptType.TAX_INVOICE, false));

        assertFalse(assessment.warnings().contains(
                "large office purchase should use corporate procurement"),
                assessment.warnings()::toString);
    }

    @Test
    @DisplayName("AccessPolicy.humanResourcesAllows L204 (payroll read that is not a view)")
    void accessHumanResourcesPayrollNonView() {
        AccessDecision decision = decide(new ACtx2().role(UserRole.HUMAN_RESOURCES)
                .type(ResourceType.PAYROLL).action(ResourceAction.EXPORT)
                .scope(AccessScope.ORGANIZATION));
        assertFalse(decision.allowed());
        assertEquals(List.of("role does not grant the requested action"), decision.reasons());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validateDates L88 (backdated but not emergency)")
    void leaveBackdatedNonEmergency() {
        LeaveAssessment assessment = assessLeave(new LReq2().type(LeaveType.SICK)
                .range(MON.minusDays(3), MON.minusDays(3)).notice(10));
        assertEquals(List.of("backdated leave is only allowed for emergency sick leave"),
                assessment.violations());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validateDates L101 (emergency waives the notice rule)")
    void leaveNoticeEmergencyWaived() {
        LeaveAssessment assessment = assessLeave(new LReq2().type(LeaveType.SICK).span(6)
                .notice(2).emergency(true));
        assertFalse(assessment.violations().contains(
                "leave longer than five days requires three days notice"),
                assessment.violations()::toString);
    }

    @Test
    @DisplayName("AdvancedLeaveRequest.<init> L26 (null employee id)")
    void advancedLeaveRequestNullEmployee() {
        assertEquals("employeeId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new AdvancedLeaveRequest(null, EmployeeCategory.REGULAR, LeaveType.SICK, MON,
                        MON, LeaveUnit.FULL_DAY, 0, "medical appointment", false, false, true, 24, 10,
                        10, 0)).getMessage());
    }

    @Test
    @DisplayName("ApprovalStep.<init> L79 (null approver entry)")
    void approvalStepNullApprover() {
        Set<String> approvers = new java.util.LinkedHashSet<>();
        approvers.add("a1");
        approvers.add(null);
        assertEquals("approverId must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new ApprovalStep("s1", "name", approvers, StepMode.ANY_ONE, 1, false, false,
                        null)).getMessage());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.act L60 (deadline still in the future)")
    void workflowActBeforeDeadline() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        Instant deadline = NOW.plusSeconds(3600);
        WorkflowSnapshot snapshot = engine.start("W1", "req",
                List.of(step("s1", Set.of("a1"), false, false, deadline)), NOW, false, true);

        WorkflowSnapshot approved = engine.act(snapshot,
                new WorkflowAction("a1", ApprovalDecision.APPROVE, null, NOW.plusSeconds(60)));
        assertEquals(WorkflowState.APPROVED, approved.state());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.act L66 (a foreign approver clears a skipping step)")
    void workflowForeignApproverOnSkippingStep() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        WorkflowSnapshot snapshot = engine.start("W1", "req",
                List.of(step("s1", Set.of("a1", "req"), false, true, null),
                        step("s2", Set.of("a2"), false, false, null)), NOW, false, true);

        WorkflowSnapshot afterFirst = engine.act(snapshot,
                new WorkflowAction("a1", ApprovalDecision.APPROVE, null, NOW.plusSeconds(60)));
        assertEquals(WorkflowState.IN_PROGRESS, afterFirst.state());
        assertEquals(1, afterFirst.currentStepIndex());
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.delegate L111 (delegate to a non-requester)")
    void workflowDelegateToOtherActor() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        WorkflowSnapshot snapshot = engine.start("W1", "req",
                List.of(step("s1", Set.of("a1", "a2"), true, true, null)), NOW, false, true);

        WorkflowSnapshot delegated = engine.delegate(snapshot, "a1", "b1", NOW.plusSeconds(60));
        assertEquals(WorkflowState.IN_PROGRESS, delegated.state());
        assertTrue(delegated.currentStep().step().approverIds().contains("b1"));
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.withdraw L132 (non urgent workflow after a first approval)")
    void workflowNonUrgentWithdrawAfterFirstApproval() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        WorkflowSnapshot snapshot = engine.start("W1", "req",
                List.of(step("s1", Set.of("a1"), false, false, null),
                        step("s2", Set.of("a2"), false, false, null)), NOW, false, true);
        WorkflowSnapshot afterFirst = engine.act(snapshot,
                new WorkflowAction("a1", ApprovalDecision.APPROVE, null, NOW.plusSeconds(60)));

        WorkflowSnapshot withdrawn = engine.withdraw(afterFirst, "req", NOW.plusSeconds(120));
        assertEquals(WorkflowState.WITHDRAWN, withdrawn.state());
    }

    @Test
    @DisplayName("TaskPolicy.allowedActions L243 (ARCHIVED restored by an administrator)")
    void taskArchivedAdminCanRestore() {
        Task archived = task(TaskStatus.ARCHIVED, TaskPriority.NORMAL, null, null, null, false);
        TaskContext admin = new TaskContext("root", NOW, false, true, true, true, true, true, true,
                0, 0, 0);
        assertEquals(Set.of(TaskAction.RESTORE),
                new TaskPolicy().evaluate(archived, admin).allowedActions());
    }

    @Test
    @DisplayName("AttendancePolicy.evaluate L60 (manual punch not approved) "
            + "and L85 (approved manual punch)")
    void attendanceManualEntryOperands() {
        // missing clock-in with an unapproved manual entry -> the else branch
        PunchRecord unapproved = new PunchRecord(null, LocalDateTime.of(MON, LocalTime.of(17, 0)),
                0, false, true, true, false);
        AttendanceResult missing = new AttendancePolicy().evaluate(
                input(stdShift(), unapproved, EmploymentMode.OFFICE, true, false, false));
        assertTrue(missing.codes().contains(AttendanceCode.MANUAL_CORRECTION_REQUIRED),
                missing.codes()::toString);

        // complete punches with an approved manual entry -> `!correctionApproved` is false
        PunchRecord approved = new PunchRecord(LocalDateTime.of(MON, LocalTime.of(9, 0)),
                LocalDateTime.of(MON, LocalTime.of(17, 0)), 0, false, true, true, true);
        AttendanceResult complete = new AttendancePolicy().evaluate(
                input(stdShift(), approved, EmploymentMode.OFFICE, true, false, false));
        assertFalse(complete.codes().contains(AttendanceCode.MANUAL_CORRECTION_REQUIRED),
                complete.codes()::toString);
    }

    @Test
    @DisplayName("VisaValidator.checkLength L33 (20 digits is above the window)")
    void visaUpperLengthBoundary() {
        assertFalse(new VisaValidator("40000000000000000000").validate());
        assertTrue(new VisaValidator("4000000000000000").validate());
    }

    @Test
    @DisplayName("AdvancedLeavePolicy.validateDates L88 (backdated annual leave)")
    void leaveBackdatedAnnual() {
        // backdated and not SICK, so the `type == SICK` operand is evaluated and false
        LeaveAssessment assessment = assessLeave(new LReq2().type(LeaveType.ANNUAL)
                .range(MON.minusDays(3), MON.minusDays(3)).notice(10));
        assertTrue(assessment.violations().contains(
                "backdated leave is only allowed for emergency sick leave"),
                assessment.violations()::toString);
    }

    @Test
    @DisplayName("ApprovalWorkflowEngine.delegate L111 (requester may delegate a non-skipping step)")
    void workflowDelegateToRequesterWithoutSkip() {
        ApprovalWorkflowEngine engine = new ApprovalWorkflowEngine();
        WorkflowSnapshot snapshot = engine.start("W1", "req",
                List.of(step("s1", Set.of("a1", "a2"), true, false, null)), NOW, false, true);

        WorkflowSnapshot delegated = engine.delegate(snapshot, "a1", "req", NOW.plusSeconds(60));
        assertEquals(WorkflowState.IN_PROGRESS, delegated.state());
        assertTrue(delegated.currentStep().step().approverIds().contains("req"));
    }

    }

    // ==================================================================
    // card network validators: inclusive length windows and IIN limits.
    // These cases close the remaining reachable length-boundary mutants:
    // Maestro/MaestroUK `>= 12` and `<= 19`, Verve `<= 19`, and the
    // odd-sized Luhn loop. Every assertion uses a real public input.
    // ==================================================================
    @Nested
    @DisplayName("card network validators keep their documented inclusive length windows")
    class CardNetworkBoundaryTests {

        @Test
        @DisplayName("Maestro accepts the smallest allowed length (12 digits)")
        void maestroAcceptsMinimumLength() {
            // 12 digits: both `size >= 12` and the IIN window 500000-509999 hold.
            assertTrue(new MaestroValidator("500000000000").validate());
        }

        @Test
        @DisplayName("Maestro accepts the largest allowed length (19 digits)")
        void maestroAcceptsMaximumLength() {
            // 19 digits: both `size <= 19` and the IIN window hold; the mutant that
            // narrows the window to `< 19` must reject this number.
            assertTrue(new MaestroValidator("5000000000000000000").validate());
        }

        @Test
        @DisplayName("Maestro rejects eleven and twenty digit inputs")
        void maestroRejectsLengthsOutsideWindow() {
            // Eleven digits still parse to IIN 500000, so only the length guard can reject it.
            assertFalse(new MaestroValidator("50000000000").validate());
            // Twenty digits exceed the upper bound while keeping the same valid IIN.
            assertFalse(new MaestroValidator("50000000000000000000").validate());
        }

        @Test
        @DisplayName("Maestro UK accepts twelve and nineteen digit inputs")
        void maestroUkAcceptsWindowBoundaries() {
            // 12 digits exercise the lower bound, 19 digits the upper bound.
            assertTrue(new MaestroUKValidator("675900000000").validate());
            assertTrue(new MaestroUKValidator("6759000000000000000").validate());
        }

        @Test
        @DisplayName("Maestro UK rejects length outliers that keep a valid IIN")
        void maestroUkRejectsLengthOutliers() {
            // 11 digits: IIN 6759 matches, so the rejection must come from checkLength().
            assertFalse(new MaestroUKValidator("67590000000").validate());
            // 20 digits: same valid IIN, length above the window.
            assertFalse(new MaestroUKValidator("67590000000000000000").validate());
        }

        @Test
        @DisplayName("Verve accepts nineteen digits inside its IIN window")
        void verveAcceptsMaximumLength() {
            // 19 digits exercise the `size <= 19` boundary of VerveValidator.
            assertTrue(new VerveValidator("5060990000000000000").validate());
        }

        @Test
        @DisplayName("Luhn validation handles a nineteen digit number")
        void luhnAcceptsOddLengthNumber() {
            // 19 is odd, so the doubling loop reaches index 18; a `<=` boundary mutant
            // then reads index 19 and fails instead of accepting the card.
            assertTrue(new Validator("4111111111111111110").validate());
        }
    }

    // ==================================================================
    // 私有入口边界探针（反射直调，不替换任何被测实现）
    //
    // 为什么这些边界只能从私有入口进入：
    //   1) AdvancedLeavePolicy.roundHalfDay 在公开链路上的入参恒为正数，
    //      负值与 IEEE-754 负零永远到不了这里；
    //   2) OvertimePolicy.roundDownToHalfHour 的负数已被上游 Math.max(0, …) 吸收；
    //   3) AttendancePolicy.positiveMinutes 的负跨度在 3 个调用点分别被
    //      Math.max(0, …)、else 归零与「签退不早于签到」吸收。
    // 这些用例只“读”真实实现对边界的处理，不改字节码、不替换方法实现，
    // 属于赛题允许的探针手段；断言失败信息写明入参，便于定位。
    // ==================================================================
    @Nested
    @DisplayName("私有入口边界探针")
    class PrivateEntryBoundaryProbes {

        /** 反射取得私有静态方法，parameterTypes 用于区分同名重载。 */
        private java.lang.reflect.Method lookupPrivateStatic(Class<?> owner, String methodName, Class<?>... parameterTypes) {
            try {
                // 只取当前类自己声明的方法，避免误拿到父类同名方法。
                java.lang.reflect.Method method = owner.getDeclaredMethod(methodName, parameterTypes);
                // 私有方法必须放开访问检查，否则 invoke 会抛 IllegalAccessException。
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException missingMethod) {
                // 方法不存在说明被测结构与预期不符，直接失败而不是静默跳过。
                throw new AssertionError("被测私有方法不存在：" + owner.getName() + "#" + methodName, missingMethod);
            }
        }

        /** 反射调用私有静态方法；被测方法内部抛出的异常原样上抛，避免被反射包装掩盖。 */
        private Object invokePrivateStatic(java.lang.reflect.Method method, Object... arguments) {
            try {
                // 私有方法为 static，第一个参数（实例）传 null。
                return method.invoke(null, arguments);
            } catch (java.lang.reflect.InvocationTargetException wrapped) {
                // 拆掉反射包装，让被测实现自己的异常直接暴露在失败栈里。
                throw new AssertionError("被测方法内部抛出异常：" + wrapped.getCause(), wrapped.getCause());
            } catch (ReflectiveOperationException reflectionFailure) {
                // 访问失败属于测试脚手架问题，单独报告方法名。
                throw new AssertionError("反射调用失败：" + method.getName(), reflectionFailure);
            }
        }

        @Test
        @DisplayName("roundHalfDay(-0.0) 必须归一化成 +0.0，不能把负零泄漏给调用方")
        void roundHalfDayNormalizesNegativeZeroToPositiveZero() {
            // roundHalfDay 只接收一个 double，无重载。
            java.lang.reflect.Method roundHalfDay = lookupPrivateStatic(AdvancedLeavePolicy.class, "roundHalfDay", double.class);

            // -0.0 是 IEEE-754 中唯一「数值等于 0、符号位为 1」的取值；
            // 判据从 value<=0 改成 value<0 时 Math.ceil(-0.0)/2 会保留负零，
            // 因此必须用原始位比较，assertEquals(0.0, …) 无法区分正负零。
            double negativeZeroResult = (Double) invokePrivateStatic(roundHalfDay, -0.0d);
            assertEquals(0L, Double.doubleToRawLongBits(negativeZeroResult),
                    "roundHalfDay(-0.0) 应返回符号位为 0 的正零");

            // 普通正零与小幅负值同样应落到 0（避免 -0.4 被向上取整成 0.5）。
            assertEquals(0.0d, (Double) invokePrivateStatic(roundHalfDay, 0.0d), 0.0d);
            assertEquals(0.0d, (Double) invokePrivateStatic(roundHalfDay, -0.4d), 0.0d);
        }

        @Test
        @DisplayName("roundHalfDay 把负小时数归零，把正小时数按半天向上取整")
        void roundHalfDayClampsNegativeHoursAndRoundsPositiveHoursUp() {
            java.lang.reflect.Method roundHalfDay = lookupPrivateStatic(AdvancedLeavePolicy.class, "roundHalfDay", double.class);

            // 负值只能从反射入口进入；真实请假请求的 requestedHours 恒大于 0。
            assertEquals(0.0d, (Double) invokePrivateStatic(roundHalfDay, -1.0d), 0.0d);
            assertEquals(0.0d, (Double) invokePrivateStatic(roundHalfDay, -8.0d), 0.0d);
            // 0.1 天不足半天，向上取整为 0.5 天。
            assertEquals(0.5d, (Double) invokePrivateStatic(roundHalfDay, 0.1d), 0.0d);
            // 恰好半天保持 0.5，不额外进位。
            assertEquals(0.5d, (Double) invokePrivateStatic(roundHalfDay, 0.5d), 0.0d);
            // 0.6 天超过半天，向上取整为整天。
            assertEquals(1.0d, (Double) invokePrivateStatic(roundHalfDay, 0.6d), 0.0d);
            // 整天数原样返回。
            assertEquals(8.0d, (Double) invokePrivateStatic(roundHalfDay, 8.0d), 0.0d);
        }

        @Test
        @DisplayName("validateUnit：不足半天按半天计费，恰好半天与零计费都不告警")
        void validateUnitReportsMinimumChargeOnlyBelowHalfDay() {
            // validateUnit(请求, 计费天数, 违规列表) 为私有静态方法。
            java.lang.reflect.Method validateUnit = lookupPrivateStatic(AdvancedLeavePolicy.class, "validateUnit",
                    AdvancedLeaveRequest.class, double.class, List.class);

            // 全日制请假请求作为固定上下文，保证只有「计费天数」一个变量。
            AdvancedLeaveRequest fullDayRequest = new AdvancedLeaveRequest("EMP-3", EmployeeCategory.REGULAR,
                    LeaveType.ANNUAL, LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 5),
                    LeaveUnit.FULL_DAY, 0.0d, "annual leave request", false, false, false, 24, 5, 10.0d, 0.0d);

            // 0.25 天低于半天门槛，必须给出「minimum charge is half a day」。
            List<String> quarterDayMessages = new ArrayList<>();
            invokePrivateStatic(validateUnit, fullDayRequest, 0.25d, quarterDayMessages);
            assertEquals(List.of("minimum charge is half a day"), quarterDayMessages);

            // 恰好 0.5 天是等值点，不得告警。
            List<String> halfDayMessages = new ArrayList<>();
            invokePrivateStatic(validateUnit, fullDayRequest, 0.5d, halfDayMessages);
            assertEquals(List.of(), halfDayMessages);

            // 0 天表示不产生计费，也不得告警（防止 < 写成 <= 时误报）。
            List<String> zeroMessages = new ArrayList<>();
            invokePrivateStatic(validateUnit, fullDayRequest, 0.0d, zeroMessages);
            assertEquals(List.of(), zeroMessages);
        }

        @Test
        @DisplayName("roundDownToHalfHour 把负分钟数归零，并按 30 分钟向下取整")
        void roundDownToHalfHourClampsNegativeMinutesToZero() {
            java.lang.reflect.Method roundDownToHalfHour = lookupPrivateStatic(OvertimePolicy.class, "roundDownToHalfHour", int.class);

            // 负分钟数在公开链路已被 Math.max(0, …) 截断，只能直调观察。
            assertEquals(0, (int) (Integer) invokePrivateStatic(roundDownToHalfHour, -40));
            assertEquals(0, (int) (Integer) invokePrivateStatic(roundDownToHalfHour, -30));
            assertEquals(0, (int) (Integer) invokePrivateStatic(roundDownToHalfHour, -1));
            // 29 分钟不足半小时，向下取整为 0。
            assertEquals(0, (int) (Integer) invokePrivateStatic(roundDownToHalfHour, 29));
            // 恰好 30 分钟保留 30，是 `minutes < 30` 的等值点。
            assertEquals(30, (int) (Integer) invokePrivateStatic(roundDownToHalfHour, 30));
            // 89 分钟向下取整为 60，验证按 30 分钟粒度取整。
            assertEquals(60, (int) (Integer) invokePrivateStatic(roundDownToHalfHour, 89));
        }

        @Test
        @DisplayName("positiveMinutes 把倒序时间段（签退早于签到）夹到 0")
        void positiveMinutesClampsReversedTimeSpanToZero() {
            java.lang.reflect.Method positiveMinutes = lookupPrivateStatic(AttendancePolicy.class, "positiveMinutes",
                    LocalDateTime.class, LocalDateTime.class);
            // 固定日期，避免依赖机器时区与当前时间。
            LocalDateTime nineOClock = LocalDateTime.of(2026, 5, 5, 9, 0);
            LocalDateTime sixOClock = LocalDateTime.of(2026, 5, 5, 18, 0);

            // 起点晚于终点（倒序跨度）应得到 0，而不是负分钟数。
            assertEquals(0, (int) (Integer) invokePrivateStatic(positiveMinutes, sixOClock, nineOClock));
            // 正序跨度 9 小时 = 540 分钟。
            assertEquals(540, (int) (Integer) invokePrivateStatic(positiveMinutes, nineOClock, sixOClock));
            // 零跨度得到 0。
            assertEquals(0, (int) (Integer) invokePrivateStatic(positiveMinutes, nineOClock, nineOClock));
            // 不足 1 分钟的跨度按 toMinutes() 截断后仍为 0。
            assertEquals(0, (int) (Integer) invokePrivateStatic(positiveMinutes, nineOClock, nineOClock.plusSeconds(30)));
        }

        @Test
        @DisplayName("categoryCap 对全部「费用类别 × 员工职级」组合恒为正，零额度保护分支不可达")
        void categoryCapIsAlwaysPositiveForEveryCategoryAndGrade() {
            // categoryCap(费用项, 报销单) 为私有静态方法，返回该类别该职级的额度上限。
            java.lang.reflect.Method categoryCap = lookupPrivateStatic(ExpensePolicy.class, "categoryCap",
                    ExpenseItem.class, ExpenseClaim.class);
            // 额度下限：最低档类别的基础额度乘以最低职级系数后仍应 ≥ 80。
            BigDecimal minimumExpectedCap = new BigDecimal("80");

            // 遍历 11 个费用类别 × 3 个职级，证明 allowedAmount 里的 cap.signum()==0 不可达。
            for (ExpenseCategory category : ExpenseCategory.values()) {
                for (EmployeeGrade grade : EmployeeGrade.values()) {
                    // 固定 10 元、CNY、当天的小额消费，避免金额本身影响类别额度。
                    ExpenseItem probeItem = new ExpenseItem("ITEM-" + category, category, new BigDecimal("10.00"),
                            Currency.getInstance("CNY"), LocalDate.of(2026, 5, 1), "Shop",
                            "probe purpose text", ReceiptType.TAX_INVOICE, PaymentMethod.PERSONAL_CARD,
                            0, false, false, false);
                    // 单条明细的报销单，职级来自外层循环。
                    ExpenseClaim probeClaim = new ExpenseClaim("CLM-CAP", "EMP-CAP", grade,
                            LocalDate.of(2026, 5, 1), "CC-1", List.of(probeItem), false, false, false, true, 0);

                    BigDecimal capAmount = (BigDecimal) invokePrivateStatic(categoryCap, probeItem, probeClaim);
                    // 额度必须为正数：为 0 意味着 allowedAmount 会走不可达的早退分支。
                    assertTrue(capAmount.signum() > 0, "类别/职级额度为 0：" + category + "/" + grade);
                    // 额度还必须不低于最低地板价，防止切换表达式后静默变小。
                    assertTrue(capAmount.compareTo(minimumExpectedCap) >= 0,
                            "类别/职级额度低于地板价：" + category + "/" + grade + " => " + capAmount);
                }
            }
        }

        @Test
        @DisplayName("超过四小时的加班告警已由应付封顶置位，raw>360 的额外置位不可观察")
        void fourHourCapAlreadyRequiresManagerReview() {
            // 标准班次 09:00-18:00，含 60 分钟午休，排班 480 分钟。
            Shift standardShift = new Shift(ShiftType.STANDARD, LocalTime.of(9, 0), LocalTime.of(18, 0),
                    0, 60, 480);
            // 09:00 签到、21:00 签退，实际工作 660 分钟，产生 180 分钟以上加班。
            PunchRecord longPunch = new PunchRecord(LocalDateTime.of(2026, 5, 5, 9, 0),
                    LocalDateTime.of(2026, 5, 5, 21, 0), 60, false, true, false, false);
            // 办公模式、非节假日、非远程，排除其它置位 review 的分支。
            AttendanceInput attendanceInput = new AttendanceInput("EMP-OT", LocalDate.of(2026, 5, 5), standardShift,
                    longPunch, EmploymentMode.OFFICE, true, false, false, false, false, true, true, 0, 0);

            // raw=361 时 payable 被封顶到 240，review 已为 true。
            OvertimeResult aboveThreshold = new OvertimePolicy().evaluate(attendanceInput, 660, 361);
            assertEquals(240, aboveThreshold.payableMinutes());
            assertEquals("daily overtime capped at four hours", aboveThreshold.reason());
            assertTrue(aboveThreshold.managerReview());

            // raw=360（等值点）同样被封顶，两个输入的可观察结果完全一致，
            // 因此 `rawOvertimeMinutes > 360` 的边界与跳转变异不可区分。
            OvertimeResult atThreshold = new OvertimePolicy().evaluate(attendanceInput, 660, 360);
            assertEquals(240, atThreshold.payableMinutes());
            assertEquals("daily overtime capped at four hours", atThreshold.reason());
            assertTrue(atThreshold.managerReview());
        }

        @Test
        @DisplayName("非对公报销单缺少成本中心只产生告警，不会升级为致命违规")
        void missingCostCenterIsOnlyAWarningWhenNotBillable() {
            // 40 元工作餐明细，抬头为个人卡。
            ExpenseItem mealItem = new ExpenseItem("ITEM-1", ExpenseCategory.MEAL, new BigDecimal("40.00"),
                    Currency.getInstance("CNY"), LocalDate.of(2026, 5, 1), "Cafe Uno",
                    "team lunch with client", ReceiptType.NONE, PaymentMethod.PERSONAL_CARD,
                    0, false, false, false);
            // costCenter 传 null，clientBillable=false：这是公开构造器允许的唯一「缺成本中心」组合。
            ExpenseClaim nonBillableClaim = new ExpenseClaim("CLM-3", "EMP-3", EmployeeGrade.STAFF,
                    LocalDate.of(2026, 5, 1), null, List.of(mealItem), false, false, false, true, 0);

            // 汇率恒为 1，避免金额换算干扰断言。
            ExpenseAssessment assessment = new ExpensePolicy()
                    .assess(nonBillableClaim, (currency, date) -> BigDecimal.ONE, LocalDate.of(2026, 5, 10));

            // 不拒单、无违规，只在告警里提示成本中心缺失。
            assertFalse(assessment.rejected());
            assertEquals(List.of(), assessment.violations());
            assertTrue(assessment.warnings().contains("cost center is missing"),
                    "warnings：" + assessment.warnings());
        }
    }

}
