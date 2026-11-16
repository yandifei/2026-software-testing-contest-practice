package net.mooctest.leave;

import net.mooctest.domain.LeaveType;

import java.time.LocalDate;
import java.util.Objects;

public record AdvancedLeaveRequest(
        String employeeId,
        EmployeeCategory employeeCategory,
        LeaveType type,
        LocalDate startDate,
        LocalDate endDate,
        LeaveUnit unit,
        double requestedHours,
        String reason,
        boolean emergency,
        boolean remoteCapable,
        boolean handoverCompleted,
        int employmentMonths,
        int noticeDays,
        double remainingBalance,
        double carryOverBalance) {

    public AdvancedLeaveRequest {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("employeeId must not be blank");
        }
        employeeId = employeeId.trim();
        employeeCategory = Objects.requireNonNull(employeeCategory, "employeeCategory must not be null");
        type = Objects.requireNonNull(type, "type must not be null");
        startDate = Objects.requireNonNull(startDate, "startDate must not be null");
        endDate = Objects.requireNonNull(endDate, "endDate must not be null");
        unit = Objects.requireNonNull(unit, "unit must not be null");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        if (employmentMonths < 0) {
            throw new IllegalArgumentException("employmentMonths must not be negative");
        }
        if (noticeDays < 0) {
            throw new IllegalArgumentException("noticeDays must not be negative");
        }
        if (remainingBalance < 0 || carryOverBalance < 0) {
            throw new IllegalArgumentException("leave balances must not be negative");
        }
        if (unit == LeaveUnit.HOURS && (requestedHours <= 0 || requestedHours > 8)) {
            throw new IllegalArgumentException("hour leave must be between 0 and 8 hours");
        }
        if (unit != LeaveUnit.HOURS && requestedHours != 0) {
            throw new IllegalArgumentException("requestedHours is only valid for hour leave");
        }
    }
}
