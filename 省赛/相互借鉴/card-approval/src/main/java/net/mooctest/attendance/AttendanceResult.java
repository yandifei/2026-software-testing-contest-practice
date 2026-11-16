package net.mooctest.attendance;

import java.util.List;
import java.util.Objects;

public record AttendanceResult(
        List<AttendanceCode> codes,
        int workedMinutes,
        int lateMinutes,
        int earlyLeaveMinutes,
        int overtimeMinutes,
        int payableOvertimeMinutes,
        int penaltyPoints,
        boolean requiresManagerReview,
        List<String> warnings) {

    public AttendanceResult {
        codes = List.copyOf(Objects.requireNonNull(codes));
        warnings = List.copyOf(Objects.requireNonNull(warnings));
        if (codes.isEmpty()) {
            throw new IllegalArgumentException("at least one attendance code is required");
        }
        if (workedMinutes < 0 || lateMinutes < 0 || earlyLeaveMinutes < 0
                || overtimeMinutes < 0 || payableOvertimeMinutes < 0 || penaltyPoints < 0) {
            throw new IllegalArgumentException("attendance metrics must not be negative");
        }
        if (payableOvertimeMinutes > overtimeMinutes) {
            throw new IllegalArgumentException("payable overtime cannot exceed actual overtime");
        }
    }
}
