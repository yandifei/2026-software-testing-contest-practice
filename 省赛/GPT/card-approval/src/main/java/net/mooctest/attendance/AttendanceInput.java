package net.mooctest.attendance;

import java.time.LocalDate;
import java.util.Objects;

public record AttendanceInput(
        String employeeId,
        LocalDate workDate,
        Shift shift,
        PunchRecord punch,
        EmploymentMode employmentMode,
        boolean scheduledWorkDay,
        boolean publicHoliday,
        boolean approvedLeave,
        boolean businessTrip,
        boolean severeWeather,
        boolean overtimeRequested,
        boolean overtimeApproved,
        int monthlyLateCount,
        int consecutiveWorkDays) {

    public AttendanceInput {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("employeeId must not be blank");
        }
        employeeId = employeeId.trim();
        workDate = Objects.requireNonNull(workDate, "workDate must not be null");
        shift = Objects.requireNonNull(shift, "shift must not be null");
        employmentMode = Objects.requireNonNull(employmentMode, "employmentMode must not be null");
        if (monthlyLateCount < 0) {
            throw new IllegalArgumentException("monthlyLateCount must not be negative");
        }
        if (consecutiveWorkDays < 0) {
            throw new IllegalArgumentException("consecutiveWorkDays must not be negative");
        }
        if (overtimeApproved && !overtimeRequested) {
            throw new IllegalArgumentException("overtime cannot be approved before it is requested");
        }
        if (approvedLeave && businessTrip) {
            throw new IllegalArgumentException("leave and business trip cannot apply to the same day");
        }
    }
}
