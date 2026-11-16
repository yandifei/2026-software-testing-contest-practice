package net.mooctest.attendance;

public record OvertimeResult(
        int actualMinutes,
        int payableMinutes,
        double compensationFactor,
        boolean compensatoryLeave,
        boolean managerReview,
        String reason) {

    public OvertimeResult {
        if (actualMinutes < 0 || payableMinutes < 0) {
            throw new IllegalArgumentException("overtime minutes must not be negative");
        }
        if (payableMinutes > actualMinutes) {
            throw new IllegalArgumentException("payableMinutes cannot exceed actualMinutes");
        }
        if (compensationFactor < 0) {
            throw new IllegalArgumentException("compensationFactor must not be negative");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }
}
