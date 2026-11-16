package net.mooctest.service;

import net.mooctest.domain.DateRange;
import net.mooctest.domain.LeaveType;
import net.mooctest.exception.LeaveValidationException;

import java.util.Objects;

/** Pure business rules, intentionally independent from frameworks and databases. */
public final class LeavePolicy {

    public int validate(
            LeaveType type,
            DateRange period,
            String reason,
            int remainingAnnualDays) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(period, "period must not be null");

        int requestedDays = period.businessDays();
        if (requestedDays == 0) {
            throw new LeaveValidationException("leave must contain at least one business day");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new LeaveValidationException("reason must contain at least 5 characters");
        }
        if (type == LeaveType.ANNUAL && requestedDays > remainingAnnualDays) {
            throw new LeaveValidationException("insufficient annual-leave balance");
        }
        if (type == LeaveType.PERSONAL && requestedDays > 3) {
            throw new LeaveValidationException("personal leave cannot exceed 3 business days");
        }
        return requestedDays;
    }
}
