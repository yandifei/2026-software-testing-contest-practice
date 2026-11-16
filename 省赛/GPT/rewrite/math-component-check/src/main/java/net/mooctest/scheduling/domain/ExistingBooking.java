package net.mooctest.scheduling.domain;

import java.time.Instant;

public record ExistingBooking(
        String bookingId,
        String instructorId,
        String roomId,
        String cohortId,
        Instant start,
        Instant end,
        boolean cancelled,
        boolean tentative,
        CourseKind kind
) {
}
