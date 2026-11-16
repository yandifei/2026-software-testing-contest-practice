package net.mooctest.attendance;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Calculates attendance codes without a database or system clock. */
public final class AttendancePolicy {

    private final OvertimePolicy overtimePolicy;

    public AttendancePolicy() {
        this(new OvertimePolicy());
    }

    public AttendancePolicy(OvertimePolicy overtimePolicy) {
        this.overtimePolicy = Objects.requireNonNull(overtimePolicy);
    }

    public AttendanceResult evaluate(AttendanceInput input) {
        Objects.requireNonNull(input, "input must not be null");
        Set<AttendanceCode> codes = new LinkedHashSet<>();
        List<String> warnings = new ArrayList<>();

        if (input.approvedLeave()) {
            codes.add(AttendanceCode.APPROVED_LEAVE);
            if (input.punch() != null) {
                warnings.add("punch exists on an approved leave day");
            }
            return result(codes, 0, 0, 0, 0, 0, false, warnings);
        }
        if (input.businessTrip()) {
            codes.add(AttendanceCode.BUSINESS_TRIP);
            if (input.punch() == null) {
                return result(codes, input.shift().minimumWorkMinutes(), 0, 0, 0, 0, false, warnings);
            }
        }
        if (!input.scheduledWorkDay() && !input.publicHoliday() && input.punch() == null) {
            codes.add(AttendanceCode.REST_DAY);
            return result(codes, 0, 0, 0, 0, 0, false, warnings);
        }
        if (input.punch() == null) {
            codes.add(input.severeWeather() ? AttendanceCode.MANUAL_CORRECTION_REQUIRED : AttendanceCode.ABSENT);
            int penalty = input.severeWeather() ? 0 : 5;
            return result(codes, 0, 0, 0, 0, penalty, true, warnings);
        }

        PunchRecord punch = input.punch();
        if (punch.clockIn() == null) {
            codes.add(AttendanceCode.MISSING_CLOCK_IN);
        }
        if (punch.clockOut() == null) {
            codes.add(AttendanceCode.MISSING_CLOCK_OUT);
        }
        if (punch.clockIn() == null || punch.clockOut() == null) {
            if (punch.manualEntry() && punch.correctionApproved()) {
                warnings.add("approved correction remains incomplete");
            } else {
                codes.add(AttendanceCode.MANUAL_CORRECTION_REQUIRED);
            }
            return result(codes, 0, 0, 0, 0, 2, true, warnings);
        }

        LocalDateTime scheduledStart = input.shift().scheduledStart(input.workDate());
        LocalDateTime scheduledEnd = input.shift().scheduledEnd(input.workDate());
        int lateMinutes = positiveMinutes(scheduledStart, punch.clockIn()) - input.shift().graceMinutes();
        int earlyMinutes = positiveMinutes(punch.clockOut(), scheduledEnd);
        lateMinutes = Math.max(0, lateMinutes);

        if (input.shift().type() == ShiftType.FLEXIBLE) {
            if (lateMinutes <= 60) {
                lateMinutes = 0;
            } else {
                lateMinutes -= 60;
            }
        }
        if (input.severeWeather() && lateMinutes <= 120) {
            lateMinutes = 0;
            warnings.add("late arrival waived because of severe weather");
        }
        if (punch.manualEntry() && !punch.correctionApproved()) {
            codes.add(AttendanceCode.MANUAL_CORRECTION_REQUIRED);
        }

        int totalPresence = positiveMinutes(punch.clockIn(), punch.clockOut());
        int workedMinutes = Math.max(0, totalPresence - punch.breakMinutes());
        int scheduledMinutes = input.shift().scheduledMinutes(input.workDate());
        int rawOvertime = Math.max(0, workedMinutes - scheduledMinutes);

        int penalty = 0;
        if (lateMinutes > 0) {
            codes.add(AttendanceCode.LATE);
            penalty += lateMinutes <= 15 ? 1 : 2;
        }
        if (lateMinutes > 60) {
            codes.add(AttendanceCode.SERIOUSLY_LATE);
            penalty += 3;
        }
        if (input.monthlyLateCount() >= 3 && lateMinutes > 0) {
            penalty += 2;
            warnings.add("repeated lateness requires manager review");
        }
        if (input.monthlyLateCount() >= 6) {
            penalty += 3;
            warnings.add("monthly lateness threshold exceeded");
        }
        if (earlyMinutes > input.shift().graceMinutes()) {
            codes.add(AttendanceCode.EARLY_LEAVE);
            penalty += earlyMinutes > 60 ? 3 : 1;
        } else {
            earlyMinutes = 0;
        }
        if (punch.breakMinutes() < input.shift().requiredBreakMinutes()
                && workedMinutes >= 360) {
            codes.add(AttendanceCode.INSUFFICIENT_BREAK);
            warnings.add("required break duration was not met");
        }
        if (workedMinutes < input.shift().minimumWorkMinutes()) {
            codes.add(AttendanceCode.INSUFFICIENT_HOURS);
            penalty += workedMinutes < input.shift().minimumWorkMinutes() / 2 ? 4 : 2;
        }

        validateLocation(input, punch, codes, warnings);
        OvertimeResult overtime = overtimePolicy.evaluate(input, workedMinutes, rawOvertime);
        if (overtime.actualMinutes() > 0) {
            codes.add(AttendanceCode.OVERTIME);
        }
        if (overtime.actualMinutes() > 240) {
            codes.add(AttendanceCode.EXCESSIVE_OVERTIME);
            warnings.add(overtime.reason());
        }
        if (codes.isEmpty()) {
            codes.add(AttendanceCode.NORMAL);
        }

        boolean review = penalty >= 5
                || overtime.managerReview()
                || codes.contains(AttendanceCode.MANUAL_CORRECTION_REQUIRED)
                || codes.contains(AttendanceCode.LOCATION_UNVERIFIED);
        return new AttendanceResult(
                List.copyOf(codes),
                workedMinutes,
                lateMinutes,
                earlyMinutes,
                overtime.actualMinutes(),
                overtime.payableMinutes(),
                penalty,
                review,
                warnings);
    }

    private static void validateLocation(
            AttendanceInput input,
            PunchRecord punch,
            Set<AttendanceCode> codes,
            List<String> warnings) {
        if (input.employmentMode() == EmploymentMode.OFFICE && punch.remotePunch()) {
            codes.add(AttendanceCode.LOCATION_UNVERIFIED);
            warnings.add("office employee used a remote punch");
        }
        if (input.employmentMode() == EmploymentMode.REMOTE && !punch.remotePunch()) {
            warnings.add("remote employee used an office punch");
        }
        if (input.employmentMode() == EmploymentMode.FIELD && !punch.locationVerified()) {
            codes.add(AttendanceCode.LOCATION_UNVERIFIED);
        }
        if (punch.remotePunch() && !punch.locationVerified()) {
            codes.add(AttendanceCode.LOCATION_UNVERIFIED);
        }
        if (input.employmentMode() == EmploymentMode.HYBRID
                && punch.remotePunch()
                && !punch.locationVerified()) {
            warnings.add("hybrid remote location needs review");
        }
    }

    private static AttendanceResult result(
            Set<AttendanceCode> codes,
            int workedMinutes,
            int lateMinutes,
            int earlyMinutes,
            int overtimeMinutes,
            int penalty,
            boolean review,
            List<String> warnings) {
        return new AttendanceResult(
                List.copyOf(codes), workedMinutes, lateMinutes, earlyMinutes,
                overtimeMinutes, 0, penalty, review, warnings);
    }

    private static int positiveMinutes(LocalDateTime from, LocalDateTime to) {
        long minutes = Duration.between(from, to).toMinutes();
        if (minutes <= 0) {
            return 0;
        }
        return Math.toIntExact(minutes);
    }
}
