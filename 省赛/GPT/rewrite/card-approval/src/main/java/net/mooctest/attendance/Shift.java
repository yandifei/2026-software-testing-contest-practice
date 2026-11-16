package net.mooctest.attendance;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public record Shift(
        ShiftType type,
        LocalTime start,
        LocalTime end,
        int graceMinutes,
        int requiredBreakMinutes,
        int minimumWorkMinutes) {

    public Shift {
        type = Objects.requireNonNull(type, "type must not be null");
        start = Objects.requireNonNull(start, "start must not be null");
        end = Objects.requireNonNull(end, "end must not be null");
        if (start.equals(end)) {
            throw new IllegalArgumentException("shift start and end must differ");
        }
        if (graceMinutes < 0 || graceMinutes > 120) {
            throw new IllegalArgumentException("graceMinutes must be between 0 and 120");
        }
        if (requiredBreakMinutes < 0 || requiredBreakMinutes > 180) {
            throw new IllegalArgumentException("requiredBreakMinutes must be between 0 and 180");
        }
        if (minimumWorkMinutes < 60 || minimumWorkMinutes > 960) {
            throw new IllegalArgumentException("minimumWorkMinutes must be between 60 and 960");
        }
        if (type == ShiftType.PART_TIME && minimumWorkMinutes > 360) {
            throw new IllegalArgumentException("part-time shift cannot require more than six hours");
        }
        if (type == ShiftType.NIGHT && !crossesMidnight()) {
            throw new IllegalArgumentException("night shift must cross midnight");
        }
    }

    public boolean crossesMidnight() {
        return end.isBefore(start);
    }

    public LocalDateTime scheduledStart(LocalDate date) {
        return LocalDateTime.of(date, start);
    }

    public LocalDateTime scheduledEnd(LocalDate date) {
        LocalDate endDate = crossesMidnight() ? date.plusDays(1) : date;
        return LocalDateTime.of(endDate, end);
    }

    public int scheduledMinutes(LocalDate date) {
        long total = Duration.between(scheduledStart(date), scheduledEnd(date)).toMinutes();
        return Math.toIntExact(total - requiredBreakMinutes);
    }
}
