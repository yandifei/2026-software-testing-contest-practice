package net.mooctest;

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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Extra mutation-killing probes for the surviving mutants of the card-approval project.
 *
 * <p>Every method is self-contained (no @BeforeAll / @AfterAll, no shared mutable state, no files,
 * no wall-clock time) so that it can be merged into the main suite as a nested class.</p>
 */
class CardProbeTest {

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
