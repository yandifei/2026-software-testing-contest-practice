package net.mooctest;

import net.mooctest.scheduling.domain.CourseKind;
import net.mooctest.scheduling.domain.DeliveryMode;
import net.mooctest.scheduling.domain.ExistingBooking;
import net.mooctest.scheduling.domain.ScheduleRequest;
import net.mooctest.scheduling.domain.SchedulingDecision;
import net.mooctest.scheduling.domain.TimeWindow;
import net.mooctest.scheduling.service.SchedulingService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SchedulingModuleTest for the scheduling module (SchedulingService + domain records).
 * Fixed clock: 2026-06-15T02:00:00Z (a Monday), UTC.
 *
 * <p>All expected conflict/warning codes and numbers are hard-coded literals;
 * every threshold is exercised on both sides and every enum case is covered.</p>
 */
class SchedulingModuleTest {

    private static final Instant NOW = Instant.parse("2026-06-15T02:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("UTC");

    private static SchedulingService service() {
        return new SchedulingService(Clock.fixed(NOW, ZONE));
    }

    private static TimeWindow window(String startIso, String endIso) {
        return new TimeWindow(Instant.parse(startIso), Instant.parse(endIso));
    }

    private static ExistingBooking booking(String id, String instructorId, String roomId, String cohortId,
                                           String startIso, String endIso, boolean cancelled, boolean tentative) {
        return new ExistingBooking(id, instructorId, roomId, cohortId,
                Instant.parse(startIso), Instant.parse(endIso), cancelled, tentative, CourseKind.LECTURE);
    }

    /**
     * Mutable builder producing a fresh {@link ScheduleRequest} per test (no shared state).
     * Defaults describe a clean request: LECTURE 60min, Thursday 2026-06-18 10:00-11:00 UTC,
     * classroom R-1, capacity 30 for 10 expected / 5 registered -> APPROVED, no conflicts, no warnings.
     */
    private static final class Req {
        String courseId = "C-1";
        String cohortId = "CO-1";
        String instructorId = "I-1";
        DeliveryMode deliveryMode = DeliveryMode.CLASSROOM;
        CourseKind courseKind = CourseKind.LECTURE;
        Instant start = Instant.parse("2026-06-18T10:00:00Z");
        Instant end = Instant.parse("2026-06-18T11:00:00Z");
        String zoneId = "UTC";
        String roomId = "R-1";
        String meetingUrl = null;
        int expectedParticipants = 10;
        int registeredParticipants = 5;
        int roomCapacity = 30;
        Set<String> requiredEquipment = Set.of();
        Set<String> availableEquipment = Set.of();
        boolean accessibleRoomRequired = false;
        boolean roomAccessible = false;
        boolean weekendAllowed = false;
        boolean afterHoursAllowed = false;
        boolean lunchTimeAllowed = false;
        boolean recurring = false;
        int occurrenceCount = 1;
        int minimumBufferMinutes = 0;
        int maximumInstructorSessionsPerDay = 3;
        List<TimeWindow> instructorAvailability = null; // null -> default window containing the session
        List<TimeWindow> maintenanceWindows = List.of();
        List<ExistingBooking> existingBookings = List.of();
        Set<LocalDate> holidays = Set.of();
        boolean administratorOverride = false;
        boolean recordingRequired = false;
        boolean recordingSupported = false;

        ScheduleRequest build() {
            List<TimeWindow> availability = instructorAvailability == null
                    ? List.of(window("2026-06-14T00:00:00Z", "2026-06-25T00:00:00Z")) // covers every session used here
                    : instructorAvailability;
            return new ScheduleRequest(courseId, cohortId, instructorId, deliveryMode, courseKind,
                    start, end, zoneId, roomId, meetingUrl, expectedParticipants, registeredParticipants,
                    roomCapacity, requiredEquipment, availableEquipment, accessibleRoomRequired,
                    roomAccessible, weekendAllowed, afterHoursAllowed, lunchTimeAllowed, recurring,
                    occurrenceCount, minimumBufferMinutes, maximumInstructorSessionsPerDay,
                    availability, maintenanceWindows, existingBookings, holidays,
                    administratorOverride, recordingRequired, recordingSupported);
        }
    }

    private static Req req() {
        return new Req();
    }

    // ------------------------------------------------------------------
    // Validation: every distinct rule with exact message, both boundary sides
    // ------------------------------------------------------------------
    @Nested
    class Validation {

        @Test
        void nullRequestIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> service().evaluate(null));
            assertEquals("request must not be null", ex.getMessage());
        }

        @Test
        void blankCourseIdIsRejected() {
            IllegalArgumentException nullId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.courseId = null; service().evaluate(r.build()); });
            assertEquals("courseId must not be blank", nullId.getMessage());
            IllegalArgumentException blankId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.courseId = "  "; service().evaluate(r.build()); });
            assertEquals("courseId must not be blank", blankId.getMessage());
        }

        @Test
        void blankCohortIdIsRejected() {
            IllegalArgumentException nullId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.cohortId = null; service().evaluate(r.build()); });
            assertEquals("cohortId must not be blank", nullId.getMessage());
            IllegalArgumentException blankId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.cohortId = " "; service().evaluate(r.build()); });
            assertEquals("cohortId must not be blank", blankId.getMessage());
        }

        @Test
        void blankInstructorIdIsRejected() {
            IllegalArgumentException nullId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.instructorId = null; service().evaluate(r.build()); });
            assertEquals("instructorId must not be blank", nullId.getMessage());
            IllegalArgumentException blankId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.instructorId = " "; service().evaluate(r.build()); });
            assertEquals("instructorId must not be blank", blankId.getMessage());
        }

        @Test
        void missingEnumIsRejected() {
            IllegalArgumentException noMode = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.deliveryMode = null; service().evaluate(r.build()); });
            assertEquals("delivery mode and course kind are required", noMode.getMessage());
            IllegalArgumentException noKind = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.courseKind = null; service().evaluate(r.build()); });
            assertEquals("delivery mode and course kind are required", noKind.getMessage());
        }

        @Test
        void missingInstantsAreRejected() {
            IllegalArgumentException noStart = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.start = null; service().evaluate(r.build()); });
            assertEquals("start and end are required", noStart.getMessage());
            IllegalArgumentException noEnd = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.end = null; service().evaluate(r.build()); });
            assertEquals("start and end are required", noEnd.getMessage());
        }

        @Test
        void endNotAfterStartIsRejected() {
            IllegalArgumentException equal = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.end = r.start; service().evaluate(r.build()); });
            assertEquals("end must be after start", equal.getMessage());
            IllegalArgumentException before = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.end = Instant.parse("2026-06-18T09:59:00Z"); service().evaluate(r.build()); });
            assertEquals("end must be after start", before.getMessage());
        }

        @Test
        void durationOfOneDayOrMoreIsRejected() {
            IllegalArgumentException fullDay = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.end = Instant.parse("2026-06-19T10:00:00Z"); service().evaluate(r.build()); });
            assertEquals("session duration must be less than one day", fullDay.getMessage());
            Req justUnderDay = req();
            justUnderDay.end = Instant.parse("2026-06-19T09:59:00Z");
            SchedulingDecision decision = service().evaluate(justUnderDay.build());
            assertEquals(1439, decision.durationMinutes());
        }

        @Test
        void blankZoneIdIsRejected() {
            IllegalArgumentException nullZone = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.zoneId = null; service().evaluate(r.build()); });
            assertEquals("zoneId must not be blank", nullZone.getMessage());
            IllegalArgumentException blankZone = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.zoneId = "  "; service().evaluate(r.build()); });
            assertEquals("zoneId must not be blank", blankZone.getMessage());
        }

        @Test
        void unknownZoneIdIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.zoneId = "Mars/Olympus"; service().evaluate(r.build()); });
            assertEquals("zoneId is invalid", ex.getMessage());
        }

        @Test
        void negativeCountsAreRejected() {
            IllegalArgumentException expected = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.expectedParticipants = -1; service().evaluate(r.build()); });
            assertEquals("participant and capacity values must not be negative", expected.getMessage());
            IllegalArgumentException registered = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.registeredParticipants = -1; service().evaluate(r.build()); });
            assertEquals("participant and capacity values must not be negative", registered.getMessage());
            IllegalArgumentException capacity = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.roomCapacity = -1; service().evaluate(r.build()); });
            assertEquals("participant and capacity values must not be negative", capacity.getMessage());
        }

        @Test
        void nonPositiveOccurrenceCountIsRejected() {
            IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.occurrenceCount = 0; service().evaluate(r.build()); });
            assertEquals("occurrenceCount must be positive", zero.getMessage());
            IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.occurrenceCount = -3; service().evaluate(r.build()); });
            assertEquals("occurrenceCount must be positive", negative.getMessage());
        }

        @Test
        void bufferMinutesOutsideZeroTo240AreRejected() {
            IllegalArgumentException below = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.minimumBufferMinutes = -1; service().evaluate(r.build()); });
            assertEquals("minimumBufferMinutes must be between 0 and 240", below.getMessage());
            IllegalArgumentException above = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.minimumBufferMinutes = 241; service().evaluate(r.build()); });
            assertEquals("minimumBufferMinutes must be between 0 and 240", above.getMessage());
            Req atZero = req();
            atZero.minimumBufferMinutes = 0;
            Req atMax = req();
            atMax.minimumBufferMinutes = 240;
            assertTrue(service().evaluate(atMax.build()).conflicts().isEmpty());
        }

        @Test
        void nonPositiveMaxSessionsPerDayIsRejected() {
            IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.maximumInstructorSessionsPerDay = 0; service().evaluate(r.build()); });
            assertEquals("maximumInstructorSessionsPerDay must be positive", zero.getMessage());
            IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.maximumInstructorSessionsPerDay = -2; service().evaluate(r.build()); });
            assertEquals("maximumInstructorSessionsPerDay must be positive", negative.getMessage());
        }

        @Test
        void invalidInstructorAvailabilityWindowIsRejected() {
            IllegalArgumentException emptyEnd = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.instructorAvailability = List.of(window("2026-06-18T09:00:00Z", "2026-06-18T09:00:00Z")); service().evaluate(r.build()); });
            assertEquals("instructor availability is invalid", emptyEnd.getMessage());
            IllegalArgumentException backwards = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.instructorAvailability = List.of(window("2026-06-18T10:00:00Z", "2026-06-18T09:00:00Z")); service().evaluate(r.build()); });
            assertEquals("instructor availability is invalid", backwards.getMessage());
            IllegalArgumentException nullStart = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.instructorAvailability = List.of(new TimeWindow(null, Instant.parse("2026-06-18T12:00:00Z"))); service().evaluate(r.build()); });
            assertEquals("instructor availability is invalid", nullStart.getMessage());
            IllegalArgumentException nullEnd = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.instructorAvailability = List.of(new TimeWindow(Instant.parse("2026-06-18T09:00:00Z"), null)); service().evaluate(r.build()); });
            assertEquals("instructor availability is invalid", nullEnd.getMessage());
        }

        @Test
        void invalidMaintenanceWindowIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.maintenanceWindows = List.of(window("2026-06-18T12:00:00Z", "2026-06-18T11:00:00Z")); service().evaluate(r.build()); });
            assertEquals("maintenance window is invalid", ex.getMessage());
        }

        @Test
        void blankBookingIdIsRejected() {
            IllegalArgumentException nullId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.existingBookings = List.of(booking(null, "I-9", "R-9", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false)); service().evaluate(r.build()); });
            assertEquals("bookingId must not be blank", nullId.getMessage());
            IllegalArgumentException blankId = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.existingBookings = List.of(booking(" ", "I-9", "R-9", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false)); service().evaluate(r.build()); });
            assertEquals("bookingId must not be blank", blankId.getMessage());
        }

        @Test
        void duplicateBookingIdIsRejected() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-9", "R-9", "CO-9", "2026-06-18T08:00:00Z", "2026-06-18T09:00:00Z", false, false),
                    booking("b1", "I-9", "R-9", "CO-9", "2026-06-18T12:00:00Z", "2026-06-18T13:00:00Z", false, false));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service().evaluate(r.build()));
            assertEquals("bookingId must be unique", ex.getMessage());
        }

        @Test
        void invalidBookingWindowIsRejected() {
            IllegalArgumentException nullStart = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.existingBookings = List.of(new ExistingBooking("b1", "I-9", "R-9", "CO-9", null, Instant.parse("2026-06-18T11:30:00Z"), false, false, CourseKind.LECTURE)); service().evaluate(r.build()); });
            assertEquals("booking window is invalid", nullStart.getMessage());
            IllegalArgumentException nullEnd = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.existingBookings = List.of(new ExistingBooking("b1", "I-9", "R-9", "CO-9", Instant.parse("2026-06-18T10:30:00Z"), null, false, false, CourseKind.LECTURE)); service().evaluate(r.build()); });
            assertEquals("booking window is invalid", nullEnd.getMessage());
            IllegalArgumentException equalEnd = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.existingBookings = List.of(booking("b1", "I-9", "R-9", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T10:30:00Z", false, false)); service().evaluate(r.build()); });
            assertEquals("booking window is invalid", equalEnd.getMessage());
            IllegalArgumentException backwards = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.existingBookings = List.of(booking("b1", "I-9", "R-9", "CO-9", "2026-06-18T11:30:00Z", "2026-06-18T10:30:00Z", false, false)); service().evaluate(r.build()); });
            assertEquals("booking window is invalid", backwards.getMessage());
        }

        @Test
        void blankEquipmentEntriesAreRejected() {
            IllegalArgumentException required = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.requiredEquipment = Set.of("PROJECTOR", " "); service().evaluate(r.build()); });
            assertEquals("required equipment must not contain blanks", required.getMessage());
            IllegalArgumentException available = assertThrows(IllegalArgumentException.class,
                    () -> { Req r = req(); r.availableEquipment = Set.of(" ", "LAB_SAFETY_KIT"); service().evaluate(r.build()); });
            assertEquals("available equipment must not contain blanks", available.getMessage());
        }

        @Test
        void validRequestPassesValidationAndIsApproved() {
            SchedulingDecision decision = service().evaluate(req().build());
            assertTrue(decision.approved());
            assertFalse(decision.manualReview());
            assertEquals("APPROVED", decision.status());
            assertEquals(60, decision.durationMinutes());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }
    }

    // ------------------------------------------------------------------
    // Timing: past start, notice thresholds, date crossing, weekend/holiday,
    // operating hours, lunch, per-courseKind duration boundaries
    // ------------------------------------------------------------------
    @Nested
    class Timing {

        @Test
        void startTimeBeforeNowConflicts() {
            Req r = req();
            r.start = Instant.parse("2026-06-15T01:00:00Z");
            r.end = Instant.parse("2026-06-15T01:30:00Z");
            r.afterHoursAllowed = true; // isolate START_TIME_IN_PAST from OUTSIDE_OPERATING_HOURS
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("START_TIME_IN_PAST"), decision.conflicts());
            assertEquals(List.of("AFTER_HOURS_SESSION"), decision.warnings());
            assertFalse(decision.approved());
            assertEquals("REJECTED", decision.status());
        }

        @Test
        void leadTimeJustUnderTwoHoursConflicts() {
            Req r = req();
            r.start = Instant.parse("2026-06-15T03:59:00Z"); // 1h59m lead
            r.end = Instant.parse("2026-06-15T04:59:00Z");
            r.afterHoursAllowed = true; // local 03:59 is before opening; isolate the notice rule
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSUFFICIENT_BOOKING_NOTICE"), decision.conflicts());
            assertEquals(List.of("AFTER_HOURS_SESSION"), decision.warnings());
        }

        @Test
        void leadTimeExactlyTwoHoursWarnsInstead() {
            Req r = req();
            r.start = Instant.parse("2026-06-15T04:00:00Z"); // exactly 2h lead
            r.end = Instant.parse("2026-06-15T05:00:00Z");
            r.afterHoursAllowed = true; // local 04:00 is before opening; isolate the notice rule
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("SHORT_BOOKING_NOTICE", "AFTER_HOURS_SESSION"), decision.warnings());
            assertTrue(decision.approved());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void leadTimeJustUnder24HoursWarns() {
            // 23h59m after now is 01:59Z = 09:59 local in Asia/Shanghai (inside operating hours)
            Req r = req();
            r.zoneId = "Asia/Shanghai";
            r.start = Instant.parse("2026-06-16T01:59:00Z"); // 23h59m lead
            r.end = Instant.parse("2026-06-16T02:59:00Z");
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("SHORT_BOOKING_NOTICE"), decision.warnings());
        }

        @Test
        void leadTimeExactly24HoursIsSilent() {
            // now + 24h is 02:00Z, which is 10:00 local in Asia/Shanghai (inside operating hours)
            Req r = req();
            r.zoneId = "Asia/Shanghai";
            r.start = Instant.parse("2026-06-16T02:00:00Z"); // exactly 24h lead
            r.end = Instant.parse("2026-06-16T03:00:00Z");
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void sessionCrossingLocalDateConflicts() {
            Req r = req();
            r.start = Instant.parse("2026-06-18T23:30:00Z");
            r.end = Instant.parse("2026-06-19T00:30:00Z");
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("SESSION_CROSSES_LOCAL_DATE"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void weekendDisallowedConflictsAndAllowedIsSilent() {
            Req blocked = req();
            blocked.start = Instant.parse("2026-06-20T10:00:00Z"); // Saturday
            blocked.end = Instant.parse("2026-06-20T11:00:00Z");
            SchedulingDecision denied = service().evaluate(blocked.build());
            assertEquals(List.of("WEEKEND_NOT_ALLOWED"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req allowed = req();
            allowed.start = Instant.parse("2026-06-20T10:00:00Z");
            allowed.end = Instant.parse("2026-06-20T11:00:00Z");
            allowed.weekendAllowed = true;
            SchedulingDecision cleared = service().evaluate(allowed.build());
            assertEquals(List.of(), cleared.conflicts());
            assertEquals(List.of(), cleared.warnings());
            assertEquals("APPROVED", cleared.status());
        }

        @Test
        void holidayDateConflictsAndOtherDatesDoNot() {
            Req onHoliday = req();
            onHoliday.holidays = Set.of(LocalDate.of(2026, 6, 18));
            SchedulingDecision denied = service().evaluate(onHoliday.build());
            assertEquals(List.of("HOLIDAY_NOT_AVAILABLE"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req otherDate = req();
            otherDate.holidays = Set.of(LocalDate.of(2026, 6, 19));
            SchedulingDecision cleared = service().evaluate(otherDate.build());
            assertEquals(List.of(), cleared.conflicts());
            assertEquals(List.of(), cleared.warnings());
        }

        @Test
        void beforeOpeningHourConflictsOrWarnsByPermission() {
            Req blocked = req();
            blocked.start = Instant.parse("2026-06-18T07:00:00Z");
            blocked.end = Instant.parse("2026-06-18T08:00:00Z");
            SchedulingDecision denied = service().evaluate(blocked.build());
            assertEquals(List.of("OUTSIDE_OPERATING_HOURS"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req allowed = req();
            allowed.start = Instant.parse("2026-06-18T07:00:00Z");
            allowed.end = Instant.parse("2026-06-18T08:00:00Z");
            allowed.afterHoursAllowed = true;
            SchedulingDecision warned = service().evaluate(allowed.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("AFTER_HOURS_SESSION"), warned.warnings());
            assertEquals("APPROVED", warned.status());
        }

        @Test
        void openingAndClosingBoundariesAreInsideHours() {
            Req opensAt8 = req();
            opensAt8.start = Instant.parse("2026-06-18T08:00:00Z");
            opensAt8.end = Instant.parse("2026-06-18T09:00:00Z");
            SchedulingDecision atOpen = service().evaluate(opensAt8.build());
            assertEquals(List.of(), atOpen.conflicts());
            assertEquals(List.of(), atOpen.warnings());

            Req endsAt21 = req();
            endsAt21.start = Instant.parse("2026-06-18T20:00:00Z");
            endsAt21.end = Instant.parse("2026-06-18T21:00:00Z");
            SchedulingDecision atClose = service().evaluate(endsAt21.build());
            assertEquals(List.of(), atClose.conflicts());
            assertEquals(List.of(), atClose.warnings());
        }

        @Test
        void endAfterClosingHourConflicts() {
            Req r = req();
            r.start = Instant.parse("2026-06-18T20:00:00Z");
            r.end = Instant.parse("2026-06-18T21:01:00Z");
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("OUTSIDE_OPERATING_HOURS"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void lunchOverlapConflictsOrWarnsByPermission() {
            Req blocked = req();
            blocked.start = Instant.parse("2026-06-18T12:00:00Z");
            blocked.end = Instant.parse("2026-06-18T12:30:00Z");
            SchedulingDecision denied = service().evaluate(blocked.build());
            assertEquals(List.of("LUNCH_BREAK_CONFLICT"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req allowed = req();
            allowed.start = Instant.parse("2026-06-18T12:00:00Z");
            allowed.end = Instant.parse("2026-06-18T12:30:00Z");
            allowed.lunchTimeAllowed = true;
            SchedulingDecision warned = service().evaluate(allowed.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("LUNCH_TIME_SESSION"), warned.warnings());
            assertEquals("APPROVED", warned.status());
        }

        @Test
        void sessionsTouchingLunchEdgesDoNotOverlapLunch() {
            Req before = req();
            before.start = Instant.parse("2026-06-18T11:00:00Z");
            before.end = Instant.parse("2026-06-18T12:00:00Z"); // ends exactly at 12:00
            SchedulingDecision endsAtNoon = service().evaluate(before.build());
            assertEquals(List.of(), endsAtNoon.conflicts());
            assertEquals(List.of(), endsAtNoon.warnings());

            Req after = req();
            after.start = Instant.parse("2026-06-18T13:30:00Z");
            after.end = Instant.parse("2026-06-18T14:30:00Z"); // starts exactly at 13:30
            SchedulingDecision startsAtEdge = service().evaluate(after.build());
            assertEquals(List.of(), startsAtEdge.conflicts());
            assertEquals(List.of(), startsAtEdge.warnings());
        }

        @Test
        void lectureDurationBoundaries() {
            Req short29 = req();
            short29.end = Instant.parse("2026-06-18T10:29:00Z");
            SchedulingDecision tooShort = service().evaluate(short29.build());
            assertEquals(List.of("LECTURE_TOO_SHORT"), tooShort.conflicts());
            assertEquals(List.of(), tooShort.warnings());

            Req at30 = req();
            at30.end = Instant.parse("2026-06-18T10:30:00Z");
            SchedulingDecision minLength = service().evaluate(at30.build());
            assertEquals(List.of(), minLength.conflicts());
            assertEquals(List.of(), minLength.warnings());

            Req at180 = req();
            at180.start = Instant.parse("2026-06-18T09:00:00Z");
            at180.end = Instant.parse("2026-06-18T12:00:00Z");
            SchedulingDecision maxLength = service().evaluate(at180.build());
            assertEquals(List.of(), maxLength.conflicts());
            assertEquals(List.of(), maxLength.warnings());

            Req at181 = req();
            at181.start = Instant.parse("2026-06-18T13:30:00Z");
            at181.end = Instant.parse("2026-06-18T16:31:00Z"); // starts at lunch edge, no lunch overlap
            SchedulingDecision over = service().evaluate(at181.build());
            assertEquals(List.of(), over.conflicts());
            assertEquals(List.of("LECTURE_LONGER_THAN_RECOMMENDED"), over.warnings());
        }

        @Test
        void labDurationBoundaries() {
            Req short59 = req();
            short59.courseKind = CourseKind.LAB;
            short59.end = Instant.parse("2026-06-18T10:59:00Z");
            short59.availableEquipment = Set.of("LAB_SAFETY_KIT");
            SchedulingDecision tooShort = service().evaluate(short59.build());
            assertEquals(List.of("LAB_TOO_SHORT"), tooShort.conflicts());
            assertEquals(List.of(), tooShort.warnings());

            Req at60 = req();
            at60.courseKind = CourseKind.LAB;
            at60.availableEquipment = Set.of("LAB_SAFETY_KIT");
            SchedulingDecision minLength = service().evaluate(at60.build());
            assertEquals(List.of(), minLength.conflicts());
            assertEquals(List.of(), minLength.warnings());

            Req at240 = req();
            at240.courseKind = CourseKind.LAB;
            at240.start = Instant.parse("2026-06-18T13:30:00Z");
            at240.end = Instant.parse("2026-06-18T17:30:00Z");
            at240.availableEquipment = Set.of("LAB_SAFETY_KIT");
            SchedulingDecision maxLength = service().evaluate(at240.build());
            assertEquals(List.of(), maxLength.conflicts());
            assertEquals(List.of(), maxLength.warnings());

            Req at241 = req();
            at241.courseKind = CourseKind.LAB;
            at241.start = Instant.parse("2026-06-18T13:30:00Z");
            at241.end = Instant.parse("2026-06-18T17:31:00Z");
            at241.availableEquipment = Set.of("LAB_SAFETY_KIT");
            SchedulingDecision over = service().evaluate(at241.build());
            assertEquals(List.of("LAB_TOO_LONG"), over.conflicts());
            assertEquals(List.of(), over.warnings());
        }

        @Test
        void examDurationAnd48hNoticeBoundaries() {
            Req short19 = req();
            short19.courseKind = CourseKind.EXAM;
            short19.end = Instant.parse("2026-06-18T10:19:00Z");
            SchedulingDecision tooShort = service().evaluate(short19.build());
            assertEquals(List.of("EXAM_TOO_SHORT"), tooShort.conflicts());
            assertEquals(List.of(), tooShort.warnings());

            Req at20 = req();
            at20.courseKind = CourseKind.EXAM;
            at20.end = Instant.parse("2026-06-18T10:20:00Z");
            SchedulingDecision minLength = service().evaluate(at20.build());
            assertEquals(List.of(), minLength.conflicts());
            assertEquals(List.of(), minLength.warnings());

            Req at300 = req();
            at300.courseKind = CourseKind.EXAM;
            at300.start = Instant.parse("2026-06-18T13:30:00Z");
            at300.end = Instant.parse("2026-06-18T18:30:00Z");
            SchedulingDecision maxLength = service().evaluate(at300.build());
            assertEquals(List.of(), maxLength.conflicts());
            assertEquals(List.of(), maxLength.warnings());

            Req at301 = req();
            at301.courseKind = CourseKind.EXAM;
            at301.start = Instant.parse("2026-06-18T13:30:00Z");
            at301.end = Instant.parse("2026-06-18T18:31:00Z");
            SchedulingDecision over = service().evaluate(at301.build());
            assertEquals(List.of("EXAM_TOO_LONG"), over.conflicts());
            assertEquals(List.of(), over.warnings());
        }

        @Test
        void examNoticeBoundaryAt48HoursUsesShanghaiZone() {
            // 48h after 02:00Z is 02:00Z on 2026-06-17 = 10:00 local in Asia/Shanghai (inside hours)
            Req exactly48 = req();
            exactly48.courseKind = CourseKind.EXAM;
            exactly48.zoneId = "Asia/Shanghai";
            exactly48.start = Instant.parse("2026-06-17T02:00:00Z");
            exactly48.end = Instant.parse("2026-06-17T03:00:00Z");
            SchedulingDecision atThreshold = service().evaluate(exactly48.build());
            assertEquals(List.of(), atThreshold.conflicts());
            assertEquals(List.of(), atThreshold.warnings());
            assertEquals("APPROVED", atThreshold.status());

            Req oneMinuteLess = req();
            oneMinuteLess.courseKind = CourseKind.EXAM;
            oneMinuteLess.zoneId = "Asia/Shanghai";
            oneMinuteLess.start = Instant.parse("2026-06-17T01:59:00Z"); // 47h59m lead
            oneMinuteLess.end = Instant.parse("2026-06-17T02:59:00Z");
            SchedulingDecision under = service().evaluate(oneMinuteLess.build());
            assertEquals(List.of(), under.conflicts());
            assertEquals(List.of("EXAM_SCHEDULED_WITH_SHORT_NOTICE"), under.warnings());
        }

        @Test
        void workshopDurationBoundaries() {
            Req short44 = req();
            short44.courseKind = CourseKind.WORKSHOP;
            short44.end = Instant.parse("2026-06-18T10:44:00Z");
            SchedulingDecision tooShort = service().evaluate(short44.build());
            assertEquals(List.of("WORKSHOP_TOO_SHORT"), tooShort.conflicts());
            assertEquals(List.of(), tooShort.warnings());

            Req at45 = req();
            at45.courseKind = CourseKind.WORKSHOP;
            at45.end = Instant.parse("2026-06-18T10:45:00Z");
            SchedulingDecision minLength = service().evaluate(at45.build());
            assertEquals(List.of(), minLength.conflicts());
            assertEquals(List.of(), minLength.warnings());
            assertEquals("APPROVED", minLength.status());

            Req at361 = req();
            at361.courseKind = CourseKind.WORKSHOP;
            at361.start = Instant.parse("2026-06-18T13:30:00Z");
            at361.end = Instant.parse("2026-06-18T19:31:00Z"); // 361 min, inside 08:00-21:00, after lunch edge
            SchedulingDecision over = service().evaluate(at361.build());
            assertEquals(List.of(), over.conflicts());
            // 361 min alone also crosses the 6h instructor-workday line
            assertEquals(List.of("WORKSHOP_REQUIRES_BREAK_PLAN", "INSTRUCTOR_LONG_WORKDAY",
                    "EXTENDED_WORKSHOP_REQUIRES_REVIEW"), over.warnings());
            assertTrue(over.manualReview());
            assertEquals("MANUAL_REVIEW", over.status());
        }

        @Test
        void webinarDurationBoundaries() {
            Req short14 = req();
            short14.courseKind = CourseKind.WEBINAR;
            short14.end = Instant.parse("2026-06-18T10:14:00Z");
            SchedulingDecision tooShort = service().evaluate(short14.build());
            assertEquals(List.of("WEBINAR_TOO_SHORT"), tooShort.conflicts());
            assertEquals(List.of(), tooShort.warnings());

            Req at15 = req();
            at15.courseKind = CourseKind.WEBINAR;
            at15.end = Instant.parse("2026-06-18T10:15:00Z");
            SchedulingDecision minLength = service().evaluate(at15.build());
            assertEquals(List.of(), minLength.conflicts());
            assertEquals(List.of(), minLength.warnings());

            Req at120 = req();
            at120.courseKind = CourseKind.WEBINAR;
            at120.start = Instant.parse("2026-06-18T09:00:00Z");
            at120.end = Instant.parse("2026-06-18T11:00:00Z");
            SchedulingDecision maxLength = service().evaluate(at120.build());
            assertEquals(List.of(), maxLength.conflicts());
            assertEquals(List.of(), maxLength.warnings());

            Req at121 = req();
            at121.courseKind = CourseKind.WEBINAR;
            at121.start = Instant.parse("2026-06-18T09:00:00Z");
            at121.end = Instant.parse("2026-06-18T11:01:00Z");
            SchedulingDecision over = service().evaluate(at121.build());
            assertEquals(List.of(), over.conflicts());
            assertEquals(List.of("WEBINAR_LONGER_THAN_RECOMMENDED"), over.warnings());
        }

        @Test
        void durationMinutesIsReportedExactly() {
            Req r = req();
            r.end = Instant.parse("2026-06-18T11:30:00Z");
            assertEquals(90, service().evaluate(r.build()).durationMinutes());
        }
    }

    // ------------------------------------------------------------------
    // DeliveryResources: ONLINE/CLASSROOM/HYBRID sub-branches, recording,
    // equipment matching, LAB safety kit, remote exam proctoring
    // ------------------------------------------------------------------
    @Nested
    class DeliveryResources {

        @Test
        void onlineWithoutMeetingUrlConflicts() {
            Req nullUrl = req();
            nullUrl.deliveryMode = DeliveryMode.ONLINE;
            nullUrl.roomId = null;
            SchedulingDecision decision = service().evaluate(nullUrl.build());
            assertEquals(List.of("MEETING_URL_REQUIRED"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("REJECTED", decision.status());

            Req blankUrl = req();
            blankUrl.deliveryMode = DeliveryMode.ONLINE;
            blankUrl.roomId = null;
            blankUrl.meetingUrl = "   ";
            SchedulingDecision blank = service().evaluate(blankUrl.build());
            assertEquals(List.of("MEETING_URL_REQUIRED"), blank.conflicts());
        }

        @Test
        void onlineWithInsecureMeetingUrlConflicts() {
            Req r = req();
            r.deliveryMode = DeliveryMode.ONLINE;
            r.roomId = null;
            r.meetingUrl = "http://meet.example.com/room";
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("SECURE_MEETING_URL_REQUIRED"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void onlineWithSecureUrlAndNoRoomIsClean() {
            Req r = req();
            r.deliveryMode = DeliveryMode.ONLINE;
            r.roomId = null;
            r.meetingUrl = "https://meet.example.com/room";
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void onlineWithRoomWarnsRoomIgnored() {
            Req r = req();
            r.deliveryMode = DeliveryMode.ONLINE;
            r.meetingUrl = "https://meet.example.com/room";
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("ROOM_IGNORED_FOR_ONLINE_SESSION"), decision.warnings());
        }

        @Test
        void classroomWithoutRoomConflicts() {
            Req nullRoom = req();
            nullRoom.roomId = null;
            SchedulingDecision decision = service().evaluate(nullRoom.build());
            assertEquals(List.of("ROOM_REQUIRED"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());

            Req blankRoom = req();
            blankRoom.roomId = "  ";
            SchedulingDecision blank = service().evaluate(blankRoom.build());
            assertEquals(List.of("ROOM_REQUIRED"), blank.conflicts());
        }

        @Test
        void classroomWithMeetingUrlWarnsOptional() {
            Req r = req();
            r.meetingUrl = "https://meet.example.com/room";
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("MEETING_URL_OPTIONAL_FOR_CLASSROOM"), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void hybridWithoutRoomAndUrlConflictsInOrder() {
            Req r = req();
            r.deliveryMode = DeliveryMode.HYBRID;
            r.roomId = null;
            r.meetingUrl = null;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("HYBRID_ROOM_REQUIRED", "HYBRID_MEETING_URL_REQUIRED"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void hybridWithInsecureUrlConflicts() {
            Req noRoom = req();
            noRoom.deliveryMode = DeliveryMode.HYBRID;
            noRoom.roomId = null;
            noRoom.meetingUrl = "https://meet.example.com/room";
            SchedulingDecision roomOnly = service().evaluate(noRoom.build());
            assertEquals(List.of("HYBRID_ROOM_REQUIRED"), roomOnly.conflicts());

            Req insecure = req();
            insecure.deliveryMode = DeliveryMode.HYBRID;
            insecure.meetingUrl = "http://meet.example.com/room";
            SchedulingDecision urlOnly = service().evaluate(insecure.build());
            assertEquals(List.of("SECURE_MEETING_URL_REQUIRED"), urlOnly.conflicts());
            assertEquals(List.of(), urlOnly.warnings());
        }

        @Test
        void hybridWithRoomAndSecureUrlIsClean() {
            Req r = req();
            r.deliveryMode = DeliveryMode.HYBRID;
            r.meetingUrl = "https://meet.example.com/room";
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void hybridBlankRoomAndBlankUrlFollowSameBranches() {
            Req r = req();
            r.deliveryMode = DeliveryMode.HYBRID;
            r.roomId = "  ";
            r.meetingUrl = "  ";
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("HYBRID_ROOM_REQUIRED", "HYBRID_MEETING_URL_REQUIRED"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void recordingCombinations() {
            Req unsupported = req();
            unsupported.recordingRequired = true;
            unsupported.recordingSupported = false;
            SchedulingDecision denied = service().evaluate(unsupported.build());
            assertEquals(List.of("RECORDING_NOT_SUPPORTED"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req supportedAndRequired = req();
            supportedAndRequired.recordingRequired = true;
            supportedAndRequired.recordingSupported = true;
            SchedulingDecision silentRequired = service().evaluate(supportedAndRequired.build());
            assertEquals(List.of(), silentRequired.conflicts());
            assertEquals(List.of(), silentRequired.warnings());

            Req availableUnused = req();
            availableUnused.recordingRequired = false;
            availableUnused.recordingSupported = true;
            SchedulingDecision warned = service().evaluate(availableUnused.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("RECORDING_AVAILABLE"), warned.warnings());
            assertEquals("APPROVED", warned.status());
        }

        @Test
        void missingEquipmentIsReportedExactForSingleItem() {
            Req r = req();
            r.requiredEquipment = Set.of("MIC");
            r.availableEquipment = Set.of("PROJECTOR");
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("MISSING_EQUIPMENT:MIC"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void missingEquipmentContentIsCheckedIgnoringOrder() {
            Req r = req();
            r.requiredEquipment = Set.of("PROJECTOR", "MIC", "CAMERA");
            r.availableEquipment = Set.of("PROJECTOR");
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(2, decision.conflicts().size());
            assertTrue(decision.conflicts().contains("MISSING_EQUIPMENT:MIC"));
            assertTrue(decision.conflicts().contains("MISSING_EQUIPMENT:CAMERA"));
        }

        @Test
        void fullySatisfiedEquipmentProducesNoConflicts() {
            Req r = req();
            r.requiredEquipment = Set.of("PROJECTOR", "MIC");
            r.availableEquipment = Set.of("PROJECTOR", "MIC", "SPEAKERS");
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void labRequiresSafetyKit() {
            Req without = req();
            without.courseKind = CourseKind.LAB;
            SchedulingDecision denied = service().evaluate(without.build());
            assertEquals(List.of("LAB_SAFETY_KIT_REQUIRED"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req with = req();
            with.courseKind = CourseKind.LAB;
            with.availableEquipment = Set.of("LAB_SAFETY_KIT");
            SchedulingDecision cleared = service().evaluate(with.build());
            assertEquals(List.of(), cleared.conflicts());
            assertEquals(List.of(), cleared.warnings());
        }

        @Test
        void remoteExamWithoutProctoringWarnsButClassroomExamDoesNot() {
            Req onlineNoProctoring = req();
            onlineNoProctoring.courseKind = CourseKind.EXAM;
            onlineNoProctoring.deliveryMode = DeliveryMode.ONLINE;
            onlineNoProctoring.roomId = null;
            onlineNoProctoring.meetingUrl = "https://meet.example.com/exam";
            SchedulingDecision warned = service().evaluate(onlineNoProctoring.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("REMOTE_PROCTORING_NOT_CONFIGURED"), warned.warnings());
            assertTrue(warned.manualReview());
            assertEquals("MANUAL_REVIEW", warned.status());

            Req onlineWithProctoring = req();
            onlineWithProctoring.courseKind = CourseKind.EXAM;
            onlineWithProctoring.deliveryMode = DeliveryMode.ONLINE;
            onlineWithProctoring.roomId = null;
            onlineWithProctoring.meetingUrl = "https://meet.example.com/exam";
            onlineWithProctoring.availableEquipment = Set.of("PROCTORING");
            SchedulingDecision configured = service().evaluate(onlineWithProctoring.build());
            assertEquals(List.of(), configured.conflicts());
            assertEquals(List.of(), configured.warnings());
            assertFalse(configured.manualReview());
            assertEquals("APPROVED", configured.status());

            Req classroomExam = req();
            classroomExam.courseKind = CourseKind.EXAM;
            SchedulingDecision inRoom = service().evaluate(classroomExam.build());
            assertEquals(List.of(), inRoom.warnings());
        }
    }

    // ------------------------------------------------------------------
    // Capacity: forecast vs registration, room capacity ladder, oversized
    // room, accessibility, online platform limits, kind-specific group caps
    // ------------------------------------------------------------------
    @Nested
    class Capacity {

        @Test
        void registrationAboveForecastWarns() {
            Req above = req();
            above.registeredParticipants = 11;
            SchedulingDecision warned = service().evaluate(above.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("REGISTRATION_EXCEEDS_FORECAST"), warned.warnings());
            assertEquals("APPROVED", warned.status());

            Req equal = req();
            equal.registeredParticipants = 10;
            SchedulingDecision silent = service().evaluate(equal.build());
            assertEquals(List.of(), silent.warnings());
        }

        @Test
        void zeroRoomCapacityConflictsUnlessOnline() {
            Req classroom = req();
            classroom.roomCapacity = 0;
            SchedulingDecision denied = service().evaluate(classroom.build());
            assertEquals(List.of("ROOM_CAPACITY_UNKNOWN"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req online = req();
            online.deliveryMode = DeliveryMode.ONLINE;
            online.roomId = null;
            online.meetingUrl = "https://meet.example.com/room";
            online.roomCapacity = 0;
            SchedulingDecision skipped = service().evaluate(online.build());
            assertEquals(List.of(), skipped.conflicts());
            assertEquals(List.of(), skipped.warnings());
        }

        @Test
        void registrationAboveRoomCapacityConflicts() {
            Req r = req();
            r.expectedParticipants = 40;
            r.registeredParticipants = 31;
            r.roomCapacity = 30;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("ROOM_TOO_SMALL_FOR_REGISTRATION"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void expectedAboveRoomCapacityWarns() {
            Req r = req();
            r.expectedParticipants = 40;
            r.registeredParticipants = 30;
            r.roomCapacity = 30;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("ROOM_BELOW_EXPECTED_CAPACITY"), decision.warnings());
        }

        @Test
        void exactlyFullRoomIsSilent() {
            Req r = req();
            r.expectedParticipants = 30;
            r.registeredParticipants = 30;
            r.roomCapacity = 30;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void oversizedRoomWarnsAboveThreeTimesExpected() {
            Req over = req();
            over.roomCapacity = 31; // max(10, 10*3) = 30, so 31 is oversized
            SchedulingDecision warned = service().evaluate(over.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("ROOM_SIGNIFICANTLY_OVERSIZED"), warned.warnings());

            Req atLimit = req();
            atLimit.roomCapacity = 30;
            SchedulingDecision silent = service().evaluate(atLimit.build());
            assertEquals(List.of(), silent.warnings());
        }

        @Test
        void smallSessionsUseFloorOfTenForOversizing() {
            Req over = req();
            over.expectedParticipants = 3;
            over.registeredParticipants = 1;
            over.roomCapacity = 11; // max(10, 3*3) = 10, so 11 is oversized
            SchedulingDecision warned = service().evaluate(over.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("ROOM_SIGNIFICANTLY_OVERSIZED"), warned.warnings());

            Req atLimit = req();
            atLimit.expectedParticipants = 3;
            atLimit.registeredParticipants = 1;
            atLimit.roomCapacity = 10;
            SchedulingDecision silent = service().evaluate(atLimit.build());
            assertEquals(List.of(), silent.warnings());
        }

        @Test
        void accessibleRoomRequiredConflictsUnlessOnlineOrAccessible() {
            Req classroom = req();
            classroom.accessibleRoomRequired = true;
            classroom.roomAccessible = false;
            SchedulingDecision denied = service().evaluate(classroom.build());
            assertEquals(List.of("ACCESSIBLE_ROOM_REQUIRED"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req accessible = req();
            accessible.accessibleRoomRequired = true;
            accessible.roomAccessible = true;
            SchedulingDecision cleared = service().evaluate(accessible.build());
            assertEquals(List.of(), cleared.conflicts());

            Req online = req();
            online.deliveryMode = DeliveryMode.ONLINE;
            online.roomId = null;
            online.meetingUrl = "https://meet.example.com/room";
            online.accessibleRoomRequired = true;
            online.roomAccessible = false;
            SchedulingDecision skipped = service().evaluate(online.build());
            assertEquals(List.of(), skipped.conflicts());
            assertEquals(List.of(), skipped.warnings());
        }

        @Test
        void onlinePlatformCapacityBoundaries() {
            Req exceeded = req();
            exceeded.deliveryMode = DeliveryMode.ONLINE;
            exceeded.roomId = null;
            exceeded.meetingUrl = "https://meet.example.com/room";
            exceeded.expectedParticipants = 1001;
            SchedulingDecision denied = service().evaluate(exceeded.build());
            assertEquals(List.of("ONLINE_PLATFORM_CAPACITY_EXCEEDED"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req atThousand = req();
            atThousand.deliveryMode = DeliveryMode.ONLINE;
            atThousand.roomId = null;
            atThousand.meetingUrl = "https://meet.example.com/room";
            atThousand.expectedParticipants = 1000;
            SchedulingDecision large = service().evaluate(atThousand.build());
            assertEquals(List.of(), large.conflicts());
            assertEquals(List.of("LARGE_ONLINE_EVENT"), large.warnings());

            Req at501 = req();
            at501.deliveryMode = DeliveryMode.ONLINE;
            at501.roomId = null;
            at501.meetingUrl = "https://meet.example.com/room";
            at501.expectedParticipants = 501;
            SchedulingDecision above500 = service().evaluate(at501.build());
            assertEquals(List.of(), above500.conflicts());
            assertEquals(List.of("LARGE_ONLINE_EVENT"), above500.warnings());

            Req at500 = req();
            at500.deliveryMode = DeliveryMode.ONLINE;
            at500.roomId = null;
            at500.meetingUrl = "https://meet.example.com/room";
            at500.expectedParticipants = 500;
            SchedulingDecision silent = service().evaluate(at500.build());
            assertEquals(List.of(), silent.conflicts());
            assertEquals(List.of(), silent.warnings());
        }

        @Test
        void workshopGroupAboveFiftyWarns() {
            Req over = req();
            over.courseKind = CourseKind.WORKSHOP;
            over.deliveryMode = DeliveryMode.ONLINE;
            over.roomId = null;
            over.meetingUrl = "https://meet.example.com/room";
            over.expectedParticipants = 51;
            SchedulingDecision warned = service().evaluate(over.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("WORKSHOP_GROUP_TOO_LARGE"), warned.warnings());

            Req at50 = req();
            at50.courseKind = CourseKind.WORKSHOP;
            at50.deliveryMode = DeliveryMode.ONLINE;
            at50.roomId = null;
            at50.meetingUrl = "https://meet.example.com/room";
            at50.expectedParticipants = 50;
            SchedulingDecision silent = service().evaluate(at50.build());
            assertEquals(List.of(), silent.warnings());
        }

        @Test
        void labAboveThirtyParticipantsConflicts() {
            Req over = req();
            over.courseKind = CourseKind.LAB;
            over.deliveryMode = DeliveryMode.ONLINE;
            over.roomId = null;
            over.meetingUrl = "https://meet.example.com/room";
            over.availableEquipment = Set.of("LAB_SAFETY_KIT");
            over.expectedParticipants = 31;
            SchedulingDecision denied = service().evaluate(over.build());
            assertEquals(List.of("LAB_CAPACITY_EXCEEDED"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req at30 = req();
            at30.courseKind = CourseKind.LAB;
            at30.deliveryMode = DeliveryMode.ONLINE;
            at30.roomId = null;
            at30.meetingUrl = "https://meet.example.com/room";
            at30.availableEquipment = Set.of("LAB_SAFETY_KIT");
            at30.expectedParticipants = 30;
            SchedulingDecision silent = service().evaluate(at30.build());
            assertEquals(List.of(), silent.conflicts());
            assertEquals(List.of(), silent.warnings());
        }
    }

    // ------------------------------------------------------------------
    // Availability: instructor availability containment, maintenance overlaps
    // ------------------------------------------------------------------
    @Nested
    class Availability {

        @Test
        void emptyAvailabilityWarnsButDoesNotConflict() {
            Req r = req();
            r.instructorAvailability = List.of();
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("INSTRUCTOR_AVAILABILITY_NOT_PROVIDED"), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void sessionOutsideAllWindowsConflicts() {
            Req r = req();
            r.instructorAvailability = List.of(window("2026-06-18T12:00:00Z", "2026-06-18T13:00:00Z"));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSTRUCTOR_UNAVAILABLE"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void containmentBoundaryExactWindowIsInsideOneMinuteShortIsOutside() {
            Req exact = req();
            exact.instructorAvailability = List.of(window("2026-06-18T10:00:00Z", "2026-06-18T11:00:00Z"));
            SchedulingDecision contained = service().evaluate(exact.build());
            assertEquals(List.of(), contained.conflicts());
            assertEquals(List.of(), contained.warnings());

            Req shortByMinute = req();
            shortByMinute.instructorAvailability = List.of(window("2026-06-18T10:00:00Z", "2026-06-18T10:59:00Z"));
            SchedulingDecision outside = service().evaluate(shortByMinute.build());
            assertEquals(List.of("INSTRUCTOR_UNAVAILABLE"), outside.conflicts());
        }

        @Test
        void overlappingMaintenanceWindowConflicts() {
            Req r = req();
            r.maintenanceWindows = List.of(window("2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z"));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("MAINTENANCE_WINDOW_CONFLICT"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void touchingMaintenanceWindowsDoNotOverlap() {
            Req endsAtSessionStart = req();
            endsAtSessionStart.maintenanceWindows = List.of(window("2026-06-18T08:00:00Z", "2026-06-18T10:00:00Z"));
            SchedulingDecision before = service().evaluate(endsAtSessionStart.build());
            assertEquals(List.of(), before.conflicts());
            assertEquals(List.of(), before.warnings());

            Req startsAtSessionEnd = req();
            startsAtSessionEnd.maintenanceWindows = List.of(window("2026-06-18T11:00:00Z", "2026-06-18T12:00:00Z"));
            SchedulingDecision after = service().evaluate(startsAtSessionEnd.build());
            assertEquals(List.of(), after.conflicts());
            assertEquals(List.of(), after.warnings());
        }
    }

    // ------------------------------------------------------------------
    // Bookings: cancelled skip, conflict codes, tentative, buffer window,
    // daily session limit, daily minutes
    // ------------------------------------------------------------------
    @Nested
    class Bookings {

        @Test
        void cancelledBookingIsSkipped() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-1", "CO-1", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", true, false));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void overlappingBookingMatchingAllThreeConflictsInOrder() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-1", "CO-1", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(
                    "INSTRUCTOR_BOOKING_CONFLICT:b1",
                    "ROOM_BOOKING_CONFLICT:b1",
                    "COHORT_BOOKING_CONFLICT:b1"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("REJECTED", decision.status());
        }

        @Test
        void onlineModeSkipsRoomBookingConflict() {
            Req r = req();
            r.deliveryMode = DeliveryMode.ONLINE;
            r.meetingUrl = "https://meet.example.com/room";
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-1", "CO-1", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSTRUCTOR_BOOKING_CONFLICT:b1", "COHORT_BOOKING_CONFLICT:b1"), decision.conflicts());
            assertEquals(List.of("ROOM_IGNORED_FOR_ONLINE_SESSION"), decision.warnings());
        }

        @Test
        void unrelatedOverlappingBookingIsClean() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-9", "R-9", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void differentDaySameInstructorBookingDoesNotCountTowardDailyTotals() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-19T10:00:00Z", "2026-06-19T11:00:00Z", false, false));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void tentativeBookingWarnsEvenWithoutConflict() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-9", "R-9", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, true));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("CONFLICTING_BOOKING_IS_TENTATIVE:b1"), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void tentativeBookingNextToRealConflictReportsBoth() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, true));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSTRUCTOR_BOOKING_CONFLICT:b1"), decision.conflicts());
            assertEquals(List.of("CONFLICTING_BOOKING_IS_TENTATIVE:b1"), decision.warnings());
        }

        @Test
        void bufferWindowOverlapConflicts() {
            Req r = req();
            r.minimumBufferMinutes = 30;
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T11:00:00Z", "2026-06-18T12:00:00Z", false, false));
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSTRUCTOR_BOOKING_CONFLICT:b1"), decision.conflicts());
        }

        @Test
        void bookingTouchingBufferedWindowOrWithoutBufferIsClean() {
            Req withinBufferEdge = req();
            withinBufferEdge.minimumBufferMinutes = 30;
            withinBufferEdge.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T08:30:00Z", "2026-06-18T09:30:00Z", false, false));
            SchedulingDecision before = service().evaluate(withinBufferEdge.build());
            assertEquals(List.of(), before.conflicts());
            assertEquals(List.of(), before.warnings());

            Req noBuffer = req();
            noBuffer.minimumBufferMinutes = 0;
            noBuffer.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T11:00:00Z", "2026-06-18T12:00:00Z", false, false));
            SchedulingDecision touching = service().evaluate(noBuffer.build());
            assertEquals(List.of(), touching.conflicts());
            assertEquals(List.of(), touching.warnings());
        }

        @Test
        void exactlyAtSessionLimitWarns() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T08:00:00Z", "2026-06-18T09:00:00Z", false, false),
                    booking("b2", "I-1", "R-9", "CO-9", "2026-06-18T12:00:00Z", "2026-06-18T13:00:00Z", false, false));
            // 2 prior sessions + this one == maximum 3
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("INSTRUCTOR_REACHES_DAILY_SESSION_LIMIT"), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void overSessionLimitConflicts() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T08:00:00Z", "2026-06-18T09:00:00Z", false, false),
                    booking("b2", "I-1", "R-9", "CO-9", "2026-06-18T12:00:00Z", "2026-06-18T13:00:00Z", false, false),
                    booking("b3", "I-1", "R-9", "CO-9", "2026-06-18T14:00:00Z", "2026-06-18T15:00:00Z", false, false));
            // 3 prior sessions >= maximum 3
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSTRUCTOR_DAILY_SESSION_LIMIT"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void dailyMinutesExactly360IsSilent() {
            Req r = req();
            r.maximumInstructorSessionsPerDay = 5;
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T08:00:00Z", "2026-06-18T10:00:00Z", false, false),
                    booking("b2", "I-1", "R-9", "CO-9", "2026-06-18T11:00:00Z", "2026-06-18T13:00:00Z", false, false));
            // 240 booking minutes + 60 new = 360: neither > 480 nor > 360
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void dailyMinutes361WarnsLongWorkday() {
            Req r = req();
            r.maximumInstructorSessionsPerDay = 5;
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T08:00:00Z", "2026-06-18T10:00:00Z", false, false),
                    booking("b2", "I-1", "R-9", "CO-9", "2026-06-18T11:00:00Z", "2026-06-18T14:01:00Z", false, false));
            // 120 + 181 = 301 booking minutes, + 60 new = 361 > 360
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("INSTRUCTOR_LONG_WORKDAY"), decision.warnings());
        }

        @Test
        void dailyMinutesExactly480WarnsButDoesNotConflict() {
            Req r = req();
            r.maximumInstructorSessionsPerDay = 5;
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T08:00:00Z", "2026-06-18T10:00:00Z", false, false),
                    booking("b2", "I-1", "R-9", "CO-9", "2026-06-18T11:00:00Z", "2026-06-18T14:00:00Z", false, false),
                    booking("b3", "I-1", "R-9", "CO-9", "2026-06-18T15:00:00Z", "2026-06-18T17:00:00Z", false, false));
            // 420 + 60 = 480: not > 480, but > 360
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("INSTRUCTOR_LONG_WORKDAY"), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void dailyMinutes481ConflictsWithoutWarning() {
            Req r = req();
            r.maximumInstructorSessionsPerDay = 5;
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T08:00:00Z", "2026-06-18T10:00:00Z", false, false),
                    booking("b2", "I-1", "R-9", "CO-9", "2026-06-18T11:00:00Z", "2026-06-18T14:01:00Z", false, false),
                    booking("b3", "I-1", "R-9", "CO-9", "2026-06-18T15:00:00Z", "2026-06-18T17:00:00Z", false, false));
            // 421 + 60 = 481 > 480
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSTRUCTOR_DAILY_HOURS_EXCEEDED"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }
    }

    // ------------------------------------------------------------------
    // Recurrence: recurring/occurrence-count consistency and limits
    // ------------------------------------------------------------------
    @Nested
    class Recurrence {

        @Test
        void nonRecurringWithMultipleOccurrencesConflicts() {
            Req r = req();
            r.occurrenceCount = 2;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("OCCURRENCE_COUNT_REQUIRES_RECURRENCE"), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
        }

        @Test
        void recurringWithSingleOccurrenceConflicts() {
            Req single = req();
            single.recurring = true;
            single.occurrenceCount = 1;
            SchedulingDecision denied = service().evaluate(single.build());
            assertEquals(List.of("RECURRING_SESSION_REQUIRES_MULTIPLE_OCCURRENCES"), denied.conflicts());
            assertEquals(List.of(), denied.warnings());

            Req pair = req();
            pair.recurring = true;
            pair.occurrenceCount = 2;
            SchedulingDecision cleared = service().evaluate(pair.build());
            assertEquals(List.of(), cleared.conflicts());
            assertEquals(List.of(), cleared.warnings());
            assertEquals("APPROVED", cleared.status());
        }

        @Test
        void recurrenceCountBoundaries() {
            Req at20 = req();
            at20.recurring = true;
            at20.occurrenceCount = 20;
            SchedulingDecision silent = service().evaluate(at20.build());
            assertEquals(List.of(), silent.conflicts());
            assertEquals(List.of(), silent.warnings());
            assertFalse(silent.manualReview());
            assertEquals("APPROVED", silent.status());

            Req at21 = req();
            at21.recurring = true;
            at21.occurrenceCount = 21;
            SchedulingDecision longSeries = service().evaluate(at21.build());
            assertEquals(List.of(), longSeries.conflicts());
            assertEquals(List.of("LONG_RECURRENCE_SERIES"), longSeries.warnings());
            assertTrue(longSeries.manualReview());
            assertEquals("MANUAL_REVIEW", longSeries.status());

            Req at52 = req();
            at52.recurring = true;
            at52.occurrenceCount = 52;
            SchedulingDecision max = service().evaluate(at52.build());
            assertEquals(List.of(), max.conflicts());
            assertEquals(List.of("LONG_RECURRENCE_SERIES"), max.warnings());

            Req at53 = req();
            at53.recurring = true;
            at53.occurrenceCount = 53;
            SchedulingDecision over = service().evaluate(at53.build());
            assertEquals(List.of("TOO_MANY_RECURRENCES"), over.conflicts());
            assertEquals(List.of(), over.warnings());
            assertEquals("REJECTED", over.status());
        }

        @Test
        void recurringWeekendSessionWarnsOnlyWhenRecurring() {
            Req weekend = req();
            weekend.recurring = true;
            weekend.occurrenceCount = 2;
            weekend.start = Instant.parse("2026-06-20T10:00:00Z"); // Saturday
            weekend.end = Instant.parse("2026-06-20T11:00:00Z");
            weekend.weekendAllowed = true;
            SchedulingDecision warned = service().evaluate(weekend.build());
            assertEquals(List.of(), warned.conflicts());
            assertEquals(List.of("RECURRING_WEEKEND_SESSION"), warned.warnings());

            Req notRecurring = req();
            notRecurring.start = Instant.parse("2026-06-20T10:00:00Z");
            notRecurring.end = Instant.parse("2026-06-20T11:00:00Z");
            notRecurring.weekendAllowed = true;
            SchedulingDecision silent = service().evaluate(notRecurring.build());
            assertEquals(List.of(), silent.warnings());
        }

        @Test
        void recurringExamRequiresReview() {
            Req r = req();
            r.recurring = true;
            r.occurrenceCount = 2;
            r.courseKind = CourseKind.EXAM;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("RECURRING_EXAM_REQUIRES_REVIEW"), decision.warnings());
        }
    }

    // ------------------------------------------------------------------
    // OverrideAndDecision: administrator override matrix, manual review
    // triggers, status outcomes, record normalization of null collections
    // ------------------------------------------------------------------
    @Nested
    class OverrideAndDecision {

        @Test
        void overrideClearsAllOverridableConflicts() {
            Req single = req();
            single.start = Instant.parse("2026-06-20T10:00:00Z"); // Saturday
            single.end = Instant.parse("2026-06-20T11:00:00Z");
            SchedulingDecision denied = service().evaluate(single.build());
            assertEquals(List.of("WEEKEND_NOT_ALLOWED"), denied.conflicts());
            assertEquals("REJECTED", denied.status());

            Req overrideOnly = req();
            overrideOnly.start = Instant.parse("2026-06-20T10:00:00Z");
            overrideOnly.end = Instant.parse("2026-06-20T11:00:00Z");
            overrideOnly.administratorOverride = true;
            SchedulingDecision cleared = service().evaluate(overrideOnly.build());
            assertEquals(List.of(), cleared.conflicts());
            assertEquals(List.of("ADMINISTRATOR_OVERRIDE_APPLIED"), cleared.warnings());
            assertTrue(cleared.approved());
            assertFalse(cleared.manualReview());
            assertEquals("APPROVED", cleared.status());

            Req multiple = req();
            multiple.start = Instant.parse("2026-06-20T10:00:00Z");
            multiple.end = Instant.parse("2026-06-20T10:29:00Z"); // also LECTURE_TOO_SHORT
            SchedulingDecision before = service().evaluate(multiple.build());
            assertEquals(List.of("WEEKEND_NOT_ALLOWED", "LECTURE_TOO_SHORT"), before.conflicts());

            Req overridden = multiple;
            overridden.administratorOverride = true;
            SchedulingDecision after = service().evaluate(overridden.build());
            assertEquals(List.of(), after.conflicts());
            assertEquals(List.of("ADMINISTRATOR_OVERRIDE_APPLIED"), after.warnings());
            assertEquals("APPROVED", after.status());
        }

        @Test
        void overrideClearsOverridableBookingRoomAndMaintenanceConflicts() {
            Req roomConflict = req();
            roomConflict.existingBookings = List.of(
                    booking("b1", "I-9", "R-1", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false));
            SchedulingDecision room = service().evaluate(roomConflict.build());
            assertEquals(List.of("ROOM_BOOKING_CONFLICT:b1"), room.conflicts());
            roomConflict.administratorOverride = true;
            SchedulingDecision roomCleared = service().evaluate(roomConflict.build());
            assertEquals(List.of(), roomCleared.conflicts());
            assertEquals(List.of("ADMINISTRATOR_OVERRIDE_APPLIED"), roomCleared.warnings());

            Req unavailable = req();
            unavailable.instructorAvailability = List.of(window("2026-06-18T12:00:00Z", "2026-06-18T13:00:00Z"));
            unavailable.administratorOverride = true;
            SchedulingDecision availabilityCleared = service().evaluate(unavailable.build());
            assertEquals(List.of(), availabilityCleared.conflicts());
            assertEquals(List.of("ADMINISTRATOR_OVERRIDE_APPLIED"), availabilityCleared.warnings());

            Req maintenance = req();
            maintenance.maintenanceWindows = List.of(window("2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z"));
            maintenance.administratorOverride = true;
            SchedulingDecision maintenanceCleared = service().evaluate(maintenance.build());
            assertEquals(List.of(), maintenanceCleared.conflicts());
            assertEquals(List.of("ADMINISTRATOR_OVERRIDE_APPLIED"), maintenanceCleared.warnings());
        }

        private void assertOverridePartiallyRejected(Req r, String expectedConflict) {
            r.administratorOverride = true;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("ADMINISTRATOR_OVERRIDE_PARTIALLY_REJECTED"), decision.warnings());
            assertTrue(decision.conflicts().contains(expectedConflict));
            assertFalse(decision.approved());
            assertEquals("REJECTED", decision.status());
        }

        @Test
        void overrideRejectedForStartTimeInPast() {
            Req r = req();
            r.start = Instant.parse("2026-06-15T01:00:00Z");
            r.end = Instant.parse("2026-06-15T01:30:00Z");
            r.afterHoursAllowed = true; // downgrades hours conflict, but its warning remains
            r.administratorOverride = true;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("START_TIME_IN_PAST"), decision.conflicts());
            assertEquals(List.of("AFTER_HOURS_SESSION", "ADMINISTRATOR_OVERRIDE_PARTIALLY_REJECTED"),
                    decision.warnings());
            assertFalse(decision.approved());
            assertEquals("REJECTED", decision.status());
        }

        @Test
        void overrideRejectedForCrossedLocalDate() {
            Req r = req();
            r.start = Instant.parse("2026-06-18T23:30:00Z");
            r.end = Instant.parse("2026-06-19T00:30:00Z");
            assertOverridePartiallyRejected(r, "SESSION_CROSSES_LOCAL_DATE");
        }

        @Test
        void overrideRejectedForRoomTooSmall() {
            Req r = req();
            r.expectedParticipants = 40;
            r.registeredParticipants = 31;
            r.roomCapacity = 30;
            assertOverridePartiallyRejected(r, "ROOM_TOO_SMALL_FOR_REGISTRATION");
        }

        @Test
        void overrideRejectedForAccessibleRoom() {
            Req r = req();
            r.accessibleRoomRequired = true;
            r.roomAccessible = false;
            assertOverridePartiallyRejected(r, "ACCESSIBLE_ROOM_REQUIRED");
        }

        @Test
        void overrideRejectedForInstructorBookingConflict() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-1", "R-9", "CO-9", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false));
            assertOverridePartiallyRejected(r, "INSTRUCTOR_BOOKING_CONFLICT:b1");
        }

        @Test
        void overrideRejectedForCohortBookingConflict() {
            Req r = req();
            r.existingBookings = List.of(
                    booking("b1", "I-9", "R-9", "CO-1", "2026-06-18T10:30:00Z", "2026-06-18T11:30:00Z", false, false));
            assertOverridePartiallyRejected(r, "COHORT_BOOKING_CONFLICT:b1");
        }

        @Test
        void overrideWithMixedConflictsKeepsAllAndRejects() {
            Req r = req();
            r.start = Instant.parse("2026-06-20T10:00:00Z"); // Saturday, overridable
            r.end = Instant.parse("2026-06-20T11:00:00Z");
            r.expectedParticipants = 40;
            r.registeredParticipants = 31; // non-overridable
            r.roomCapacity = 30;
            r.administratorOverride = true;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("WEEKEND_NOT_ALLOWED", "ROOM_TOO_SMALL_FOR_REGISTRATION"), decision.conflicts());
            assertEquals(List.of("ADMINISTRATOR_OVERRIDE_PARTIALLY_REJECTED"), decision.warnings());
            assertEquals("REJECTED", decision.status());
        }

        @Test
        void largeHybridEventBoundaryTriggersManualReview() {
            Req at200 = req();
            at200.deliveryMode = DeliveryMode.HYBRID;
            at200.meetingUrl = "https://meet.example.com/room";
            at200.expectedParticipants = 200;
            at200.roomCapacity = 300; // room fits 200 without triggering room warnings
            SchedulingDecision silent = service().evaluate(at200.build());
            assertEquals(List.of(), silent.conflicts());
            assertEquals(List.of(), silent.warnings());
            assertFalse(silent.manualReview());
            assertEquals("APPROVED", silent.status());

            Req at201 = req();
            at201.deliveryMode = DeliveryMode.HYBRID;
            at201.meetingUrl = "https://meet.example.com/room";
            at201.expectedParticipants = 201;
            at201.roomCapacity = 300;
            SchedulingDecision review = service().evaluate(at201.build());
            assertEquals(List.of(), review.conflicts());
            assertEquals(List.of("LARGE_HYBRID_EVENT_REQUIRES_REVIEW"), review.warnings());
            assertTrue(review.manualReview());
            assertFalse(review.approved());
            assertEquals("MANUAL_REVIEW", review.status());
        }

        @Test
        void extendedWorkshopBoundaryTriggersManualReview() {
            Req at359 = req();
            at359.courseKind = CourseKind.WORKSHOP;
            at359.start = Instant.parse("2026-06-18T13:30:00Z");
            at359.end = Instant.parse("2026-06-18T19:29:00Z");
            SchedulingDecision justUnder = service().evaluate(at359.build());
            assertEquals(List.of(), justUnder.conflicts());
            assertEquals(List.of(), justUnder.warnings());
            assertFalse(justUnder.manualReview());
            assertEquals("APPROVED", justUnder.status());

            Req at360 = req();
            at360.courseKind = CourseKind.WORKSHOP;
            at360.start = Instant.parse("2026-06-18T13:30:00Z");
            at360.end = Instant.parse("2026-06-18T19:30:00Z");
            SchedulingDecision atThreshold = service().evaluate(at360.build());
            assertEquals(List.of(), atThreshold.conflicts());
            assertEquals(List.of("EXTENDED_WORKSHOP_REQUIRES_REVIEW"), atThreshold.warnings());
            assertTrue(atThreshold.manualReview());
            assertEquals("MANUAL_REVIEW", atThreshold.status());
        }

        @Test
        void manualReviewPlusConflictYieldsRejectedStatus() {
            Req r = req();
            r.deliveryMode = DeliveryMode.HYBRID;
            r.zoneId = "Asia/Shanghai";
            r.start = Instant.parse("2026-06-15T03:00:00Z"); // local 11:00, 1h lead
            r.end = Instant.parse("2026-06-15T04:00:00Z");
            r.meetingUrl = "https://meet.example.com/room";
            r.expectedParticipants = 201;
            r.roomCapacity = 300;
            SchedulingDecision decision = service().evaluate(r.build());
            assertEquals(List.of("INSUFFICIENT_BOOKING_NOTICE"), decision.conflicts());
            assertEquals(List.of("LARGE_HYBRID_EVENT_REQUIRES_REVIEW"), decision.warnings());
            assertTrue(decision.manualReview());
            assertFalse(decision.approved());
            assertEquals("REJECTED", decision.status());
        }

        @Test
        void requestRecordNormalizesNullCollections() {
            ScheduleRequest raw = new ScheduleRequest("C-1", "CO-1", "I-1", DeliveryMode.CLASSROOM,
                    CourseKind.LECTURE,
                    Instant.parse("2026-06-18T10:00:00Z"), Instant.parse("2026-06-18T11:00:00Z"),
                    "UTC", "R-1", null, 10, 5, 30,
                    null, null, false, false, false, false, false, false, 1, 0, 3,
                    null, null, null, null, false, false, false);
            SchedulingDecision decision = service().evaluate(raw);
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of("INSTRUCTOR_AVAILABILITY_NOT_PROVIDED"), decision.warnings());
            assertEquals("APPROVED", decision.status());
        }

        @Test
        void decisionRecordNormalizesNullLists() {
            SchedulingDecision decision = new SchedulingDecision(true, false, "APPROVED", 60, null, null);
            assertEquals(List.of(), decision.conflicts());
            assertEquals(List.of(), decision.warnings());
            assertTrue(decision.approved());
            assertFalse(decision.manualReview());
            assertEquals(60, decision.durationMinutes());
        }
    }
}
