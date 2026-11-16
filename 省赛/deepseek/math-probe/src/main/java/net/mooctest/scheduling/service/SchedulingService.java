package net.mooctest.scheduling.service;

import net.mooctest.scheduling.domain.CourseKind;
import net.mooctest.scheduling.domain.DeliveryMode;
import net.mooctest.scheduling.domain.ExistingBooking;
import net.mooctest.scheduling.domain.ScheduleRequest;
import net.mooctest.scheduling.domain.SchedulingDecision;
import net.mooctest.scheduling.domain.TimeWindow;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class SchedulingService {

    private final Clock clock;

    public SchedulingService(Clock clock) {
        this.clock = clock;
    }

    public SchedulingDecision evaluate(ScheduleRequest request) {
        validate(request);
        List<String> conflicts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        ZoneId zone = parseZone(request.zoneId());
        ZonedDateTime localStart = request.start().atZone(zone);
        ZonedDateTime localEnd = request.end().atZone(zone);
        int durationMinutes = Math.toIntExact(Duration.between(request.start(), request.end()).toMinutes());

        evaluateTiming(request, localStart, localEnd, durationMinutes, conflicts, warnings);
        evaluateDeliveryResources(request, conflicts, warnings);
        evaluateCapacity(request, conflicts, warnings);
        evaluateAvailability(request, conflicts, warnings);
        evaluateExistingBookings(request, localStart.toLocalDate(), conflicts, warnings);
        evaluateRecurrence(request, localStart, conflicts, warnings);

        if (request.administratorOverride() && !conflicts.isEmpty()) {
            List<String> nonOverridable = conflicts.stream()
                    .filter(this::isNonOverridable)
                    .toList();
            if (nonOverridable.isEmpty()) {
                warnings.add("ADMINISTRATOR_OVERRIDE_APPLIED");
                conflicts.clear();
            } else {
                warnings.add("ADMINISTRATOR_OVERRIDE_PARTIALLY_REJECTED");
            }
        }

        boolean manualReview = requiresManualReview(request, warnings);
        boolean approved = conflicts.isEmpty() && !manualReview;
        String status = approved ? "APPROVED" : manualReview && conflicts.isEmpty()
                ? "MANUAL_REVIEW"
                : "REJECTED";
        return new SchedulingDecision(
                approved,
                manualReview,
                status,
                durationMinutes,
                conflicts,
                warnings
        );
    }

    private void evaluateTiming(
            ScheduleRequest request,
            ZonedDateTime localStart,
            ZonedDateTime localEnd,
            int durationMinutes,
            List<String> conflicts,
            List<String> warnings
    ) {
        Instant now = Instant.now(clock);
        if (request.start().isBefore(now)) {
            conflicts.add("START_TIME_IN_PAST");
        } else {
            long leadHours = Duration.between(now, request.start()).toHours();
            if (leadHours < 2) {
                conflicts.add("INSUFFICIENT_BOOKING_NOTICE");
            } else if (leadHours < 24) {
                warnings.add("SHORT_BOOKING_NOTICE");
            }
        }
        if (!localStart.toLocalDate().equals(localEnd.toLocalDate())) {
            conflicts.add("SESSION_CROSSES_LOCAL_DATE");
        }
        DayOfWeek day = localStart.getDayOfWeek();
        if ((day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
                && !request.weekendAllowed()) {
            conflicts.add("WEEKEND_NOT_ALLOWED");
        }
        if (request.holidays().contains(localStart.toLocalDate())) {
            conflicts.add("HOLIDAY_NOT_AVAILABLE");
        }
        LocalTime startTime = localStart.toLocalTime();
        LocalTime endTime = localEnd.toLocalTime();
        boolean outsideHours = startTime.isBefore(LocalTime.of(8, 0))
                || endTime.isAfter(LocalTime.of(21, 0));
        if (outsideHours && !request.afterHoursAllowed()) {
            conflicts.add("OUTSIDE_OPERATING_HOURS");
        } else if (outsideHours) {
            warnings.add("AFTER_HOURS_SESSION");
        }
        boolean overlapsLunch = startTime.isBefore(LocalTime.of(13, 30))
                && endTime.isAfter(LocalTime.of(12, 0));
        if (overlapsLunch && !request.lunchTimeAllowed()) {
            conflicts.add("LUNCH_BREAK_CONFLICT");
        } else if (overlapsLunch) {
            warnings.add("LUNCH_TIME_SESSION");
        }

        switch (request.courseKind()) {
            case LECTURE -> {
                if (durationMinutes < 30) {
                    conflicts.add("LECTURE_TOO_SHORT");
                }
                if (durationMinutes > 180) {
                    warnings.add("LECTURE_LONGER_THAN_RECOMMENDED");
                }
            }
            case LAB -> {
                if (durationMinutes < 60) {
                    conflicts.add("LAB_TOO_SHORT");
                }
                if (durationMinutes > 240) {
                    conflicts.add("LAB_TOO_LONG");
                }
            }
            case EXAM -> {
                if (durationMinutes < 20) {
                    conflicts.add("EXAM_TOO_SHORT");
                }
                if (durationMinutes > 300) {
                    conflicts.add("EXAM_TOO_LONG");
                }
                if (Duration.between(now, request.start()).toHours() < 48) {
                    warnings.add("EXAM_SCHEDULED_WITH_SHORT_NOTICE");
                }
            }
            case WORKSHOP -> {
                if (durationMinutes < 45) {
                    conflicts.add("WORKSHOP_TOO_SHORT");
                }
                if (durationMinutes > 360) {
                    warnings.add("WORKSHOP_REQUIRES_BREAK_PLAN");
                }
            }
            case WEBINAR -> {
                if (durationMinutes < 15) {
                    conflicts.add("WEBINAR_TOO_SHORT");
                }
                if (durationMinutes > 120) {
                    warnings.add("WEBINAR_LONGER_THAN_RECOMMENDED");
                }
            }
            default -> throw new IllegalStateException("Unsupported course kind");
        }
    }

    private void evaluateDeliveryResources(
            ScheduleRequest request,
            List<String> conflicts,
            List<String> warnings
    ) {
        switch (request.deliveryMode()) {
            case ONLINE -> {
                if (request.meetingUrl() == null || request.meetingUrl().isBlank()) {
                    conflicts.add("MEETING_URL_REQUIRED");
                } else if (!request.meetingUrl().startsWith("https://")) {
                    conflicts.add("SECURE_MEETING_URL_REQUIRED");
                }
                if (request.roomId() != null && !request.roomId().isBlank()) {
                    warnings.add("ROOM_IGNORED_FOR_ONLINE_SESSION");
                }
            }
            case CLASSROOM -> {
                if (request.roomId() == null || request.roomId().isBlank()) {
                    conflicts.add("ROOM_REQUIRED");
                }
                if (request.meetingUrl() != null && !request.meetingUrl().isBlank()) {
                    warnings.add("MEETING_URL_OPTIONAL_FOR_CLASSROOM");
                }
            }
            case HYBRID -> {
                if (request.roomId() == null || request.roomId().isBlank()) {
                    conflicts.add("HYBRID_ROOM_REQUIRED");
                }
                if (request.meetingUrl() == null || request.meetingUrl().isBlank()) {
                    conflicts.add("HYBRID_MEETING_URL_REQUIRED");
                } else if (!request.meetingUrl().startsWith("https://")) {
                    conflicts.add("SECURE_MEETING_URL_REQUIRED");
                }
            }
            default -> throw new IllegalStateException("Unsupported delivery mode");
        }
        if (request.recordingRequired() && !request.recordingSupported()) {
            conflicts.add("RECORDING_NOT_SUPPORTED");
        }
        if (!request.recordingRequired() && request.recordingSupported()) {
            warnings.add("RECORDING_AVAILABLE");
        }
        Set<String> missingEquipment = new HashSet<>(request.requiredEquipment());
        missingEquipment.removeAll(request.availableEquipment());
        for (String equipment : missingEquipment) {
            conflicts.add("MISSING_EQUIPMENT:" + equipment);
        }
        if (request.courseKind() == CourseKind.LAB
                && !request.availableEquipment().contains("LAB_SAFETY_KIT")) {
            conflicts.add("LAB_SAFETY_KIT_REQUIRED");
        }
        if (request.courseKind() == CourseKind.EXAM
                && request.deliveryMode() != DeliveryMode.CLASSROOM
                && !request.availableEquipment().contains("PROCTORING")) {
            warnings.add("REMOTE_PROCTORING_NOT_CONFIGURED");
        }
    }

    private void evaluateCapacity(
            ScheduleRequest request,
            List<String> conflicts,
            List<String> warnings
    ) {
        if (request.registeredParticipants() > request.expectedParticipants()) {
            warnings.add("REGISTRATION_EXCEEDS_FORECAST");
        }
        if (request.deliveryMode() != DeliveryMode.ONLINE) {
            if (request.roomCapacity() == 0) {
                conflicts.add("ROOM_CAPACITY_UNKNOWN");
            } else if (request.registeredParticipants() > request.roomCapacity()) {
                conflicts.add("ROOM_TOO_SMALL_FOR_REGISTRATION");
            } else if (request.expectedParticipants() > request.roomCapacity()) {
                warnings.add("ROOM_BELOW_EXPECTED_CAPACITY");
            } else if (request.roomCapacity() > Math.max(10, request.expectedParticipants() * 3)) {
                warnings.add("ROOM_SIGNIFICANTLY_OVERSIZED");
            }
        }
        if (request.accessibleRoomRequired() && !request.roomAccessible()
                && request.deliveryMode() != DeliveryMode.ONLINE) {
            conflicts.add("ACCESSIBLE_ROOM_REQUIRED");
        }
        if (request.deliveryMode() == DeliveryMode.ONLINE && request.expectedParticipants() > 1000) {
            conflicts.add("ONLINE_PLATFORM_CAPACITY_EXCEEDED");
        } else if (request.deliveryMode() == DeliveryMode.ONLINE
                && request.expectedParticipants() > 500) {
            warnings.add("LARGE_ONLINE_EVENT");
        }
        if (request.courseKind() == CourseKind.WORKSHOP && request.expectedParticipants() > 50) {
            warnings.add("WORKSHOP_GROUP_TOO_LARGE");
        }
        if (request.courseKind() == CourseKind.LAB && request.expectedParticipants() > 30) {
            conflicts.add("LAB_CAPACITY_EXCEEDED");
        }
    }

    private void evaluateAvailability(
            ScheduleRequest request,
            List<String> conflicts,
            List<String> warnings
    ) {
        boolean containedInAvailability = request.instructorAvailability().stream()
                .anyMatch(window -> !request.start().isBefore(window.start())
                        && !request.end().isAfter(window.end()));
        if (request.instructorAvailability().isEmpty()) {
            warnings.add("INSTRUCTOR_AVAILABILITY_NOT_PROVIDED");
        } else if (!containedInAvailability) {
            conflicts.add("INSTRUCTOR_UNAVAILABLE");
        }
        for (TimeWindow maintenance : request.maintenanceWindows()) {
            if (overlaps(request.start(), request.end(), maintenance.start(), maintenance.end())) {
                conflicts.add("MAINTENANCE_WINDOW_CONFLICT");
            }
        }
    }

    private void evaluateExistingBookings(
            ScheduleRequest request,
            LocalDate localDate,
            List<String> conflicts,
            List<String> warnings
    ) {
        int instructorSessions = 0;
        int instructorMinutes = 0;
        ZoneId zone = ZoneId.of(request.zoneId());
        for (ExistingBooking booking : request.existingBookings()) {
            if (booking.cancelled()) {
                continue;
            }
            if (booking.start().atZone(zone).toLocalDate().equals(localDate)
                    && Objects.equals(booking.instructorId(), request.instructorId())) {
                instructorSessions++;
                instructorMinutes += Duration.between(booking.start(), booking.end()).toMinutes();
            }
            Instant bufferedStart = request.start().minusSeconds(request.minimumBufferMinutes() * 60L);
            Instant bufferedEnd = request.end().plusSeconds(request.minimumBufferMinutes() * 60L);
            if (!overlaps(bufferedStart, bufferedEnd, booking.start(), booking.end())) {
                continue;
            }
            if (Objects.equals(booking.instructorId(), request.instructorId())) {
                conflicts.add("INSTRUCTOR_BOOKING_CONFLICT:" + booking.bookingId());
            }
            if (request.deliveryMode() != DeliveryMode.ONLINE
                    && Objects.equals(booking.roomId(), request.roomId())) {
                conflicts.add("ROOM_BOOKING_CONFLICT:" + booking.bookingId());
            }
            if (Objects.equals(booking.cohortId(), request.cohortId())) {
                conflicts.add("COHORT_BOOKING_CONFLICT:" + booking.bookingId());
            }
            if (booking.tentative()) {
                warnings.add("CONFLICTING_BOOKING_IS_TENTATIVE:" + booking.bookingId());
            }
        }
        if (instructorSessions >= request.maximumInstructorSessionsPerDay()) {
            conflicts.add("INSTRUCTOR_DAILY_SESSION_LIMIT");
        } else if (instructorSessions + 1 == request.maximumInstructorSessionsPerDay()) {
            warnings.add("INSTRUCTOR_REACHES_DAILY_SESSION_LIMIT");
        }
        int newDuration = Math.toIntExact(Duration.between(request.start(), request.end()).toMinutes());
        if (instructorMinutes + newDuration > 8 * 60) {
            conflicts.add("INSTRUCTOR_DAILY_HOURS_EXCEEDED");
        } else if (instructorMinutes + newDuration > 6 * 60) {
            warnings.add("INSTRUCTOR_LONG_WORKDAY");
        }
    }

    private void evaluateRecurrence(
            ScheduleRequest request,
            ZonedDateTime localStart,
            List<String> conflicts,
            List<String> warnings
    ) {
        if (!request.recurring() && request.occurrenceCount() > 1) {
            conflicts.add("OCCURRENCE_COUNT_REQUIRES_RECURRENCE");
        }
        if (request.recurring() && request.occurrenceCount() < 2) {
            conflicts.add("RECURRING_SESSION_REQUIRES_MULTIPLE_OCCURRENCES");
        }
        if (request.occurrenceCount() > 52) {
            conflicts.add("TOO_MANY_RECURRENCES");
        } else if (request.occurrenceCount() > 20) {
            warnings.add("LONG_RECURRENCE_SERIES");
        }
        if (request.recurring() && (localStart.getDayOfWeek() == DayOfWeek.SATURDAY
                || localStart.getDayOfWeek() == DayOfWeek.SUNDAY)) {
            warnings.add("RECURRING_WEEKEND_SESSION");
        }
        if (request.recurring() && request.courseKind() == CourseKind.EXAM) {
            warnings.add("RECURRING_EXAM_REQUIRES_REVIEW");
        }
    }

    private boolean requiresManualReview(ScheduleRequest request, List<String> warnings) {
        boolean largeHybrid = request.deliveryMode() == DeliveryMode.HYBRID
                && request.expectedParticipants() > 200;
        boolean extendedWorkshop = request.courseKind() == CourseKind.WORKSHOP
                && Duration.between(request.start(), request.end()).toHours() >= 6;
        boolean highRiskRecurrence = request.recurring() && request.occurrenceCount() > 20;
        boolean remoteExamWithoutProctoring = request.courseKind() == CourseKind.EXAM
                && request.deliveryMode() != DeliveryMode.CLASSROOM
                && !request.availableEquipment().contains("PROCTORING");
        if (largeHybrid) {
            warnings.add("LARGE_HYBRID_EVENT_REQUIRES_REVIEW");
        }
        if (extendedWorkshop) {
            warnings.add("EXTENDED_WORKSHOP_REQUIRES_REVIEW");
        }
        return largeHybrid || extendedWorkshop || highRiskRecurrence || remoteExamWithoutProctoring;
    }

    private boolean isNonOverridable(String conflict) {
        return conflict.equals("START_TIME_IN_PAST")
                || conflict.equals("SESSION_CROSSES_LOCAL_DATE")
                || conflict.equals("ROOM_TOO_SMALL_FOR_REGISTRATION")
                || conflict.equals("ACCESSIBLE_ROOM_REQUIRED")
                || conflict.startsWith("INSTRUCTOR_BOOKING_CONFLICT")
                || conflict.startsWith("COHORT_BOOKING_CONFLICT");
    }

    private boolean overlaps(Instant firstStart, Instant firstEnd, Instant secondStart, Instant secondEnd) {
        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }

    private ZoneId parseZone(String zoneId) {
        try {
            return ZoneId.of(zoneId);
        } catch (ZoneRulesException exception) {
            throw new IllegalArgumentException("zoneId is invalid", exception);
        }
    }

    private void validate(ScheduleRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (request.courseId() == null || request.courseId().isBlank()) {
            throw new IllegalArgumentException("courseId must not be blank");
        }
        if (request.cohortId() == null || request.cohortId().isBlank()) {
            throw new IllegalArgumentException("cohortId must not be blank");
        }
        if (request.instructorId() == null || request.instructorId().isBlank()) {
            throw new IllegalArgumentException("instructorId must not be blank");
        }
        if (request.deliveryMode() == null || request.courseKind() == null) {
            throw new IllegalArgumentException("delivery mode and course kind are required");
        }
        if (request.start() == null || request.end() == null) {
            throw new IllegalArgumentException("start and end are required");
        }
        if (!request.end().isAfter(request.start())) {
            throw new IllegalArgumentException("end must be after start");
        }
        if (Duration.between(request.start(), request.end()).toDays() >= 1) {
            throw new IllegalArgumentException("session duration must be less than one day");
        }
        if (request.zoneId() == null || request.zoneId().isBlank()) {
            throw new IllegalArgumentException("zoneId must not be blank");
        }
        parseZone(request.zoneId());
        if (request.expectedParticipants() < 0
                || request.registeredParticipants() < 0
                || request.roomCapacity() < 0) {
            throw new IllegalArgumentException("participant and capacity values must not be negative");
        }
        if (request.occurrenceCount() <= 0) {
            throw new IllegalArgumentException("occurrenceCount must be positive");
        }
        if (request.minimumBufferMinutes() < 0 || request.minimumBufferMinutes() > 240) {
            throw new IllegalArgumentException("minimumBufferMinutes must be between 0 and 240");
        }
        if (request.maximumInstructorSessionsPerDay() <= 0) {
            throw new IllegalArgumentException("maximumInstructorSessionsPerDay must be positive");
        }
        validateWindows(request.instructorAvailability(), "instructor availability");
        validateWindows(request.maintenanceWindows(), "maintenance window");
        Set<String> bookingIds = new HashSet<>();
        for (ExistingBooking booking : request.existingBookings()) {
            Objects.requireNonNull(booking, "booking must not be null");
            if (booking.bookingId() == null || booking.bookingId().isBlank()) {
                throw new IllegalArgumentException("bookingId must not be blank");
            }
            if (!bookingIds.add(booking.bookingId())) {
                throw new IllegalArgumentException("bookingId must be unique");
            }
            if (booking.start() == null || booking.end() == null
                    || !booking.end().isAfter(booking.start())) {
                throw new IllegalArgumentException("booking window is invalid");
            }
        }
        if (request.requiredEquipment().stream().anyMatch(item -> item == null || item.isBlank())) {
            throw new IllegalArgumentException("required equipment must not contain blanks");
        }
        if (request.availableEquipment().stream().anyMatch(item -> item == null || item.isBlank())) {
            throw new IllegalArgumentException("available equipment must not contain blanks");
        }
    }

    private void validateWindows(List<TimeWindow> windows, String name) {
        for (TimeWindow window : windows) {
            if (window == null || window.start() == null || window.end() == null
                    || !window.end().isAfter(window.start())) {
                throw new IllegalArgumentException(name + " is invalid");
            }
        }
    }
}
