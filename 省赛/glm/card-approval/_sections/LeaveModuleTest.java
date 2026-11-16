/*
 * Leave subsystem: full-branch tests for leave/, domain/, exception/, service/ and port/ types.
 * All expected values are hand-computed against fixed LocalDate values; today is fixed at 2026-03-10 (Tuesday).
 */
package net.mooctest;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeaveModuleTest {

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
    }

}
