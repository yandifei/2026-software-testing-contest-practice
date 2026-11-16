/*
 * Coverage suite for the attendance module (net.mooctest.attendance) and the
 * card date/validation core (net.mooctest.card: DateChecker, DateFormatter,
 * DateParser, InvalidCardException, Validator).
 *
 * Validator is a concrete class (not an interface), so no anonymous
 * implementation is required: every method is exercised directly on real
 * instances, with a fixed Clock injected through the 4-argument constructor.
 */
package net.mooctest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
import net.mooctest.card.DateChecker;
import net.mooctest.card.DateFormatter;
import net.mooctest.card.DateParser;
import net.mooctest.card.InvalidCardException;
import net.mooctest.card.Validator;

class AttendanceCardDateModuleTest {

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
    }
}
