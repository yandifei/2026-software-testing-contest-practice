package net.mooctest.attendance;

import java.time.LocalDateTime;

public record PunchRecord(
        LocalDateTime clockIn,
        LocalDateTime clockOut,
        int breakMinutes,
        boolean remotePunch,
        boolean locationVerified,
        boolean manualEntry,
        boolean correctionApproved) {

    public PunchRecord {
        if (breakMinutes < 0 || breakMinutes > 480) {
            throw new IllegalArgumentException("breakMinutes must be between 0 and 480");
        }
        if (clockIn != null && clockOut != null && clockOut.isBefore(clockIn)) {
            throw new IllegalArgumentException("clockOut must not be before clockIn");
        }
        if (correctionApproved && !manualEntry) {
            throw new IllegalArgumentException("only manual entries can be approved corrections");
        }
    }
}
