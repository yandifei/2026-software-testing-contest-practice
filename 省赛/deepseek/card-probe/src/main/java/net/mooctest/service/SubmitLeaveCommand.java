package net.mooctest.service;

import net.mooctest.domain.DateRange;
import net.mooctest.domain.LeaveType;

import java.util.Objects;

/** Input for submitting a leave request. */
public record SubmitLeaveCommand(
        String employeeId,
        LeaveType type,
        DateRange period,
        String reason) {

    public SubmitLeaveCommand {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("employeeId must not be blank");
        }
        employeeId = employeeId.trim();
        type = Objects.requireNonNull(type, "type must not be null");
        period = Objects.requireNonNull(period, "period must not be null");
    }
}
