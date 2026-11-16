package net.mooctest.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

public final class OvertimePolicy {

    public OvertimeResult evaluate(
            AttendanceInput input,
            int workedMinutes,
            int rawOvertimeMinutes) {
        Objects.requireNonNull(input, "input must not be null");
        if (workedMinutes < 0 || rawOvertimeMinutes < 0) {
            throw new IllegalArgumentException("minutes must not be negative");
        }
        if (rawOvertimeMinutes == 0) {
            return new OvertimeResult(0, 0, 0, false, false, "no overtime");
        }
        if (!input.overtimeRequested()) {
            return new OvertimeResult(rawOvertimeMinutes, 0, 0, false, true,
                    "overtime was not requested");
        }
        if (!input.overtimeApproved()) {
            return new OvertimeResult(rawOvertimeMinutes, 0, 0, false, true,
                    "overtime is awaiting approval");
        }

        int payable = roundDownToHalfHour(rawOvertimeMinutes);
        boolean review = false;
        boolean compensatoryLeave = false;
        double factor = 1.0;
        String reason = "ordinary approved overtime";

        LocalDate date = input.workDate();
        DayOfWeek day = date.getDayOfWeek();
        if (input.publicHoliday()) {
            factor = 3.0;
            reason = "public-holiday overtime";
        } else if (!input.scheduledWorkDay()
                || day == DayOfWeek.SATURDAY
                || day == DayOfWeek.SUNDAY) {
            factor = 2.0;
            reason = "rest-day overtime";
        } else if (input.shift().type() == ShiftType.NIGHT) {
            factor = 1.5;
            reason = "night-shift overtime";
        }

        if (payable < 30) {
            payable = 0;
            reason = "overtime below minimum duration";
        }
        if (payable > 240) {
            payable = 240;
            review = true;
            reason = "daily overtime capped at four hours";
        }
        if (rawOvertimeMinutes > 360) {
            review = true;
        }
        if (input.consecutiveWorkDays() >= 6) {
            compensatoryLeave = true;
            review = true;
        }
        if (input.employmentMode() == EmploymentMode.REMOTE
                && input.punch() != null
                && !input.punch().locationVerified()) {
            payable = 0;
            review = true;
            reason = "remote overtime location is unverified";
        }
        if (input.shift().type() == ShiftType.ON_CALL && rawOvertimeMinutes <= 60) {
            compensatoryLeave = true;
            factor = 0;
            reason = "short on-call work grants compensatory leave";
        }
        if (workedMinutes > 900) {
            review = true;
        }

        return new OvertimeResult(
                rawOvertimeMinutes,
                payable,
                factor,
                compensatoryLeave,
                review,
                reason);
    }

    private static int roundDownToHalfHour(int minutes) {
        if (minutes < 30) {
            return 0;
        }
        return minutes - minutes % 30;
    }
}
