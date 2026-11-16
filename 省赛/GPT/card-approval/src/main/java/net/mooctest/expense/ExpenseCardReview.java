package net.mooctest.expense;

import net.mooctest.card.CardEvidenceCheck;
import net.mooctest.permission.AccessDecision;

import java.util.List;
import java.util.Objects;

/** Combined OA permission, expense policy and corporate-card evidence decision. */
public record ExpenseCardReview(
        String claimId,
        AccessDecision access,
        ExpenseAssessment expense,
        List<CardEvidenceCheck> cards,
        List<String> issues,
        boolean readyForApproval,
        boolean auditRequired) {
    public ExpenseCardReview {
        if (claimId == null || claimId.isBlank()) {
            throw new IllegalArgumentException("claimId must not be blank");
        }
        claimId = claimId.trim();
        access = Objects.requireNonNull(access, "access must not be null");
        expense = Objects.requireNonNull(expense, "expense must not be null");
        cards = List.copyOf(Objects.requireNonNull(cards, "cards must not be null"));
        issues = List.copyOf(Objects.requireNonNull(issues, "issues must not be null"));
        boolean expectedReady = access.allowed() && !expense.rejected() && issues.isEmpty()
                && cards.stream().allMatch(CardEvidenceCheck::valid);
        if (readyForApproval != expectedReady) {
            throw new IllegalArgumentException("readyForApproval is inconsistent with review results");
        }
        boolean expectedAudit = access.auditRequired() || expense.auditRequired() || !issues.isEmpty();
        if (auditRequired != expectedAudit) {
            throw new IllegalArgumentException("auditRequired is inconsistent with review results");
        }
    }
}
