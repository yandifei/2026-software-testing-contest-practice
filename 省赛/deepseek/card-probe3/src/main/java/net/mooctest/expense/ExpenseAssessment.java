package net.mooctest.expense;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record ExpenseAssessment(
        BigDecimal claimedAmount,
        BigDecimal reimbursableAmount,
        ExpenseApprovalTier approvalTier,
        boolean rejected,
        boolean auditRequired,
        List<String> violations,
        List<String> warnings) {

    public ExpenseAssessment {
        claimedAmount = Objects.requireNonNull(claimedAmount);
        reimbursableAmount = Objects.requireNonNull(reimbursableAmount);
        approvalTier = Objects.requireNonNull(approvalTier);
        violations = List.copyOf(Objects.requireNonNull(violations));
        warnings = List.copyOf(Objects.requireNonNull(warnings));
        if (claimedAmount.signum() < 0 || reimbursableAmount.signum() < 0) {
            throw new IllegalArgumentException("amounts must not be negative");
        }
        if (reimbursableAmount.compareTo(claimedAmount) > 0) {
            throw new IllegalArgumentException("reimbursable amount cannot exceed claimed amount");
        }
        if (rejected && violations.isEmpty()) {
            throw new IllegalArgumentException("rejected assessment must contain violations");
        }
    }
}
