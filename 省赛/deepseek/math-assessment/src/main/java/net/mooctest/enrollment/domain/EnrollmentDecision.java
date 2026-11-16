package net.mooctest.enrollment.domain;

import java.util.List;

public record EnrollmentDecision(
        EnrollmentOutcome outcome,
        boolean paymentRequired,
        int availableSeats,
        List<String> reasons,
        List<String> warnings
) {

    public EnrollmentDecision {
        reasons = List.copyOf(reasons);
        warnings = List.copyOf(warnings);
    }

    public boolean accepted() {
        return outcome == EnrollmentOutcome.ENROLLED || outcome == EnrollmentOutcome.WAITLISTED;
    }
}
