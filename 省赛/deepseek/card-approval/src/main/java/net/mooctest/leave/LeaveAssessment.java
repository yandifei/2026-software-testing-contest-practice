package net.mooctest.leave;

import java.util.List;
import java.util.Objects;

public record LeaveAssessment(
        boolean eligible,
        double chargedDays,
        ApprovalTier approvalTier,
        boolean medicalCertificateRequired,
        boolean handoverRequired,
        List<String> violations) {

    public LeaveAssessment {
        if (chargedDays < 0) {
            throw new IllegalArgumentException("chargedDays must not be negative");
        }
        approvalTier = Objects.requireNonNull(approvalTier);
        violations = List.copyOf(Objects.requireNonNull(violations));
        if (eligible && !violations.isEmpty()) {
            throw new IllegalArgumentException("eligible assessment cannot contain violations");
        }
        if (!eligible && violations.isEmpty()) {
            throw new IllegalArgumentException("ineligible assessment must explain the violation");
        }
    }
}
