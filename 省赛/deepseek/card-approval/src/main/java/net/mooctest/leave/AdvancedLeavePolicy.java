package net.mooctest.leave;

import net.mooctest.domain.LeaveType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Rule-rich leave policy used for boundary and decision-table testing. */
public final class AdvancedLeavePolicy {

    public LeaveAssessment assess(
            AdvancedLeaveRequest request,
            WorkCalendar calendar,
            LocalDate today) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(calendar, "calendar must not be null");
        Objects.requireNonNull(today, "today must not be null");

        List<String> violations = new ArrayList<>();
        double chargedDays = calculateChargedDays(request, calendar);

        validateDates(request, today, chargedDays, violations);
        validateReason(request, violations);
        validateEmployment(request, violations);
        validateUnit(request, chargedDays, violations);

        boolean certificateRequired = false;
        switch (request.type()) {
            case ANNUAL -> validateAnnualLeave(request, chargedDays, violations);
            case SICK -> {
                certificateRequired = requiresMedicalCertificate(request, chargedDays);
                validateSickLeave(request, chargedDays, violations);
            }
            case PERSONAL -> validatePersonalLeave(request, chargedDays, violations);
        }

        boolean handoverRequired = requiresHandover(request, chargedDays);
        if (handoverRequired && !request.handoverCompleted()) {
            violations.add("handover is required for this request");
        }

        ApprovalTier tier = resolveApprovalTier(request, chargedDays, certificateRequired);
        return new LeaveAssessment(
                violations.isEmpty(),
                chargedDays,
                tier,
                certificateRequired,
                handoverRequired,
                violations);
    }

    public double calculateChargedDays(AdvancedLeaveRequest request, WorkCalendar calendar) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(calendar);

        if (request.unit() == LeaveUnit.HOURS) {
            return roundHalfDay(request.requestedHours() / 8.0);
        }
        if (request.unit() == LeaveUnit.HALF_DAY_MORNING
                || request.unit() == LeaveUnit.HALF_DAY_AFTERNOON) {
            return calendar.isWorkingDay(request.startDate()) ? 0.5 : 0;
        }

        double days = 0;
        LocalDate date = request.startDate();
        while (!date.isAfter(request.endDate())) {
            if (calendar.isWorkingDay(date)) {
                days++;
            }
            date = date.plusDays(1);
        }
        return days;
    }

    private static void validateDates(
            AdvancedLeaveRequest request,
            LocalDate today,
            double chargedDays,
            List<String> violations) {
        if (chargedDays == 0) {
            violations.add("request contains no working time");
        }
        if (request.startDate().isBefore(today)
                && !(request.type() == LeaveType.SICK && request.emergency())) {
            violations.add("backdated leave is only allowed for emergency sick leave");
        }
        if (request.startDate().isAfter(today.plusYears(1))) {
            violations.add("leave cannot be requested more than one year ahead");
        }
        long calendarDays = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        if (calendarDays > 45) {
            violations.add("a single request cannot span more than 45 calendar days");
        }
        if (request.noticeDays() == 0 && chargedDays > 1 && !request.emergency()) {
            violations.add("multi-day leave requires advance notice");
        }
        if (request.noticeDays() < 3 && chargedDays > 5 && !request.emergency()) {
            violations.add("leave longer than five days requires three days notice");
        }
        if (request.noticeDays() < 7 && chargedDays > 10 && !request.emergency()) {
            violations.add("leave longer than ten days requires seven days notice");
        }
    }

    private static void validateReason(
            AdvancedLeaveRequest request,
            List<String> violations) {
        if (request.reason() == null || request.reason().isBlank()) {
            violations.add("reason is required");
            return;
        }
        String reason = request.reason().trim();
        if (reason.length() < 5) {
            violations.add("reason must contain at least five characters");
        }
        if (reason.length() > 300) {
            violations.add("reason cannot exceed 300 characters");
        }
        if (reason.contains("<") || reason.contains(">")) {
            violations.add("reason contains unsupported markup");
        }
        if (reason.chars().allMatch(Character::isDigit)) {
            violations.add("reason cannot contain digits only");
        }
    }

    private static void validateEmployment(
            AdvancedLeaveRequest request,
            List<String> violations) {
        if (request.employeeCategory() == EmployeeCategory.INTERN
                && request.type() == LeaveType.ANNUAL) {
            violations.add("interns are not eligible for annual leave");
        }
        if (request.employeeCategory() == EmployeeCategory.CONTRACTOR
                && request.type() == LeaveType.ANNUAL
                && request.employmentMonths() < 12) {
            violations.add("contractors require twelve months service for annual leave");
        }
        if (request.employeeCategory() == EmployeeCategory.PROBATION
                && request.type() == LeaveType.PERSONAL
                && !request.emergency()) {
            violations.add("non-emergency personal leave is unavailable during probation");
        }
        if (request.employmentMonths() < 1 && request.type() != LeaveType.SICK) {
            violations.add("new employees may only request sick leave in their first month");
        }
        if (request.employmentMonths() > 600) {
            violations.add("employment duration is outside the supported range");
        }
    }

    private static void validateUnit(
            AdvancedLeaveRequest request,
            double chargedDays,
            List<String> violations) {
        boolean halfDay = request.unit() == LeaveUnit.HALF_DAY_MORNING
                || request.unit() == LeaveUnit.HALF_DAY_AFTERNOON;
        if (halfDay && !request.startDate().equals(request.endDate())) {
            violations.add("half-day leave must start and end on the same date");
        }
        if (request.unit() == LeaveUnit.HOURS && request.requestedHours() < 1) {
            violations.add("hour leave must be at least one hour");
        }
        if (request.unit() == LeaveUnit.HOURS
                && request.type() == LeaveType.ANNUAL
                && request.requestedHours() % 0.5 != 0) {
            violations.add("annual hour leave must use half-hour increments");
        }
        if (chargedDays > 0 && chargedDays < 0.5) {
            violations.add("minimum charge is half a day");
        }
        if (request.unit() == LeaveUnit.HALF_DAY_MORNING && request.emergency()) {
            violations.add("emergency leave cannot be limited to a planned morning slot");
        }
    }

    private static void validateAnnualLeave(
            AdvancedLeaveRequest request,
            double chargedDays,
            List<String> violations) {
        double available = request.remainingBalance() + request.carryOverBalance();
        if (chargedDays > available) {
            violations.add("insufficient annual leave balance");
        }
        if (chargedDays > 15) {
            violations.add("annual leave cannot exceed fifteen working days per request");
        }
        if (request.carryOverBalance() > 5) {
            violations.add("carry-over balance cannot exceed five days");
        }
        if (request.carryOverBalance() > 0
                && request.startDate().getMonthValue() > 3) {
            violations.add("carry-over leave must be used before April");
        }
        if (request.employmentMonths() < 6) {
            violations.add("annual leave requires six months service");
        }
        if (request.remoteCapable() && chargedDays == 1 && request.noticeDays() == 0) {
            violations.add("remote-capable employees must consider remote work for urgent one-day leave");
        }
    }

    private static void validateSickLeave(
            AdvancedLeaveRequest request,
            double chargedDays,
            List<String> violations) {
        if (chargedDays > 30) {
            violations.add("sick leave longer than thirty days needs a separate medical process");
        }
        if (request.unit() == LeaveUnit.HOURS && request.requestedHours() > 4) {
            violations.add("hour-based sick leave cannot exceed four hours");
        }
        if (request.emergency() && request.noticeDays() > 30) {
            violations.add("emergency sick leave has inconsistent notice information");
        }
        if (!request.emergency()
                && request.startDate().getDayOfWeek() == DayOfWeek.SUNDAY) {
            violations.add("planned sick leave cannot start on Sunday");
        }
        if (request.remainingBalance() > 365) {
            violations.add("sick leave balance is outside the supported range");
        }
    }

    private static void validatePersonalLeave(
            AdvancedLeaveRequest request,
            double chargedDays,
            List<String> violations) {
        if (chargedDays > 3 && !request.emergency()) {
            violations.add("personal leave cannot exceed three days without an emergency");
        }
        if (chargedDays > 5) {
            violations.add("personal leave cannot exceed five days");
        }
        if (request.unit() == LeaveUnit.HOURS && request.requestedHours() > 4) {
            violations.add("hour-based personal leave cannot exceed four hours");
        }
        if (request.employeeCategory() == EmployeeCategory.CONTRACTOR
                && chargedDays > 1) {
            violations.add("contractor personal leave is limited to one day");
        }
        if (request.noticeDays() < 1 && !request.emergency()) {
            violations.add("personal leave requires one day notice");
        }
        if (request.remoteCapable() && !request.emergency() && chargedDays <= 1) {
            violations.add("short personal absence should use remote work when available");
        }
    }

    private static boolean requiresMedicalCertificate(
            AdvancedLeaveRequest request,
            double chargedDays) {
        if (chargedDays > 2) {
            return true;
        }
        if (request.employeeCategory() == EmployeeCategory.PROBATION && chargedDays > 1) {
            return true;
        }
        if (request.emergency() && chargedDays > 1) {
            return true;
        }
        return request.unit() == LeaveUnit.HOURS && request.requestedHours() == 4;
    }

    private static boolean requiresHandover(
            AdvancedLeaveRequest request,
            double chargedDays) {
        if (chargedDays >= 3) {
            return true;
        }
        if (request.type() == LeaveType.ANNUAL && chargedDays >= 2) {
            return true;
        }
        if (request.employeeCategory() == EmployeeCategory.REGULAR && chargedDays > 1) {
            return true;
        }
        return request.startDate().getDayOfWeek() == DayOfWeek.FRIDAY && chargedDays > 1;
    }

    private static ApprovalTier resolveApprovalTier(
            AdvancedLeaveRequest request,
            double chargedDays,
            boolean certificateRequired) {
        if (chargedDays > 10) {
            return ApprovalTier.DIRECTOR;
        }
        if (request.employeeCategory() == EmployeeCategory.CONTRACTOR && chargedDays > 3) {
            return ApprovalTier.DIRECTOR;
        }
        if (certificateRequired || request.employeeCategory() == EmployeeCategory.PROBATION) {
            return ApprovalTier.HUMAN_RESOURCES;
        }
        if (chargedDays > 3 || request.type() == LeaveType.PERSONAL) {
            return ApprovalTier.MANAGER;
        }
        return ApprovalTier.SUPERVISOR;
    }

    private static double roundHalfDay(double value) {
        if (value <= 0) {
            return 0;
        }
        return Math.ceil(value * 2) / 2.0;
    }
}
