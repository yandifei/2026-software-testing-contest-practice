package net.mooctest.scheduling.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record ScheduleRequest(
        String courseId,
        String cohortId,
        String instructorId,
        DeliveryMode deliveryMode,
        CourseKind courseKind,
        Instant start,
        Instant end,
        String zoneId,
        String roomId,
        String meetingUrl,
        int expectedParticipants,
        int registeredParticipants,
        int roomCapacity,
        Set<String> requiredEquipment,
        Set<String> availableEquipment,
        boolean accessibleRoomRequired,
        boolean roomAccessible,
        boolean weekendAllowed,
        boolean afterHoursAllowed,
        boolean lunchTimeAllowed,
        boolean recurring,
        int occurrenceCount,
        int minimumBufferMinutes,
        int maximumInstructorSessionsPerDay,
        List<TimeWindow> instructorAvailability,
        List<TimeWindow> maintenanceWindows,
        List<ExistingBooking> existingBookings,
        Set<LocalDate> holidays,
        boolean administratorOverride,
        boolean recordingRequired,
        boolean recordingSupported
) {
    public ScheduleRequest {
        requiredEquipment = requiredEquipment == null ? Set.of() : Set.copyOf(requiredEquipment);
        availableEquipment = availableEquipment == null ? Set.of() : Set.copyOf(availableEquipment);
        instructorAvailability = instructorAvailability == null
                ? List.of()
                : List.copyOf(instructorAvailability);
        maintenanceWindows = maintenanceWindows == null ? List.of() : List.copyOf(maintenanceWindows);
        existingBookings = existingBookings == null ? List.of() : List.copyOf(existingBookings);
        holidays = holidays == null ? Set.of() : Set.copyOf(holidays);
    }
}
