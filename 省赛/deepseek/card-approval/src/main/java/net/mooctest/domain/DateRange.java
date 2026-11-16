package net.mooctest.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

/** An inclusive date range used by leave requests. */
public record DateRange(LocalDate start, LocalDate end) {

    public DateRange {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end must not be before start");
        }
    }

    /** Counts Monday through Friday in this inclusive range. */
    public int businessDays() {
        long days = start.datesUntil(end.plusDays(1))
                .filter(DateRange::isBusinessDay)
                .count();
        return Math.toIntExact(days);
    }

    /** Returns true when two inclusive ranges share at least one date. */
    public boolean overlaps(DateRange other) {
        Objects.requireNonNull(other, "other must not be null");
        return !end.isBefore(other.start) && !other.end.isBefore(start);
    }

    private static boolean isBusinessDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}
