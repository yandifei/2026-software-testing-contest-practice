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
import net.mooctest.card.CardEvidence;
import net.mooctest.card.CardEvidenceCheck;
import net.mooctest.card.CardEvidenceValidator;
import net.mooctest.card.ChinaUnionPayValidator;
import net.mooctest.card.CreditCardType;
import net.mooctest.card.DinersClubInternationalValidator;
import net.mooctest.card.InterPaymentValidator;
import net.mooctest.card.MaestroUKValidator;
import net.mooctest.card.MaestroValidator;
import net.mooctest.card.TypeChecker;
import net.mooctest.card.Validator;
import net.mooctest.card.VerveValidator;
import net.mooctest.card.VisaValidator;
import net.mooctest.domain.DateRange;
import net.mooctest.domain.LeaveType;
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
import net.mooctest.service.SubmitLeaveCommand;
import net.mooctest.task.RecurrenceRule;
import net.mooctest.task.Task;
import net.mooctest.task.TaskAction;
import net.mooctest.task.TaskContext;
import net.mooctest.task.TaskPolicy;
import net.mooctest.task.TaskPriority;
import net.mooctest.task.TaskStateMachine;
import net.mooctest.task.TaskStatus;
import net.mooctest.workflow.ApprovalDecision;
import net.mooctest.workflow.ApprovalStep;
import net.mooctest.workflow.ApprovalWorkflowEngine;
import net.mooctest.workflow.StepMode;
import net.mooctest.workflow.StepProgress;
import net.mooctest.workflow.WorkflowAction;
import net.mooctest.workflow.WorkflowSnapshot;
import net.mooctest.workflow.WorkflowState;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Branch-coverage probes for the branches left uncovered by StudentTest and CardProbeTest.
 *
 * <p>Each method names the {@code Class.method Lline} it drives in its {@link DisplayName}.
 * Everything is self-contained: no @BeforeAll / @AfterAll, no files, no network, no wall clock.</p>
 */
class CardProbeTest2 {

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

/*
 * Surefire default includes only match classes whose name ends with "Test", so CardProbeTest2
 * alone is not picked up by a plain "mvn test". This alias (same package, inherited test
 * methods) makes the probes run in the default build. It declares no tests of its own and can be
 * dropped as soon as CardProbeTest2 is merged into the main suite as a nested class.
 */
class CardProbeTest2Test extends CardProbeTest2 {
}

