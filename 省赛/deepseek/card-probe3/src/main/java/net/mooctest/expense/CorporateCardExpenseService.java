package net.mooctest.expense;

import net.mooctest.card.CardEvidence;
import net.mooctest.card.CardEvidenceCheck;
import net.mooctest.card.CardEvidenceValidator;
import net.mooctest.permission.AccessContext;
import net.mooctest.permission.AccessDecision;
import net.mooctest.permission.AccessPolicy;
import net.mooctest.permission.ResourceAction;
import net.mooctest.permission.ResourceType;
import net.mooctest.permission.UserRole;
import net.mooctest.workflow.ApprovalStep;
import net.mooctest.workflow.ApprovalWorkflowEngine;
import net.mooctest.workflow.WorkflowSnapshot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Reviews a claim before starting its existing OA approval workflow. */
public final class CorporateCardExpenseService {
    private final ExpensePolicy expensePolicy;
    private final CardEvidenceValidator cardValidator;
    private final AccessPolicy accessPolicy;
    private final ApprovalWorkflowEngine workflowEngine;

    public CorporateCardExpenseService(
            ExpensePolicy expensePolicy,
            CardEvidenceValidator cardValidator,
            AccessPolicy accessPolicy,
            ApprovalWorkflowEngine workflowEngine) {
        this.expensePolicy = Objects.requireNonNull(expensePolicy);
        this.cardValidator = Objects.requireNonNull(cardValidator);
        this.accessPolicy = Objects.requireNonNull(accessPolicy);
        this.workflowEngine = Objects.requireNonNull(workflowEngine);
    }

    public ExpenseCardReview review(
            ExpenseClaim claim,
            Map<String, CardEvidence> evidenceByItem,
            ExchangeRateProvider exchangeRates,
            LocalDate today,
            AccessContext actor) {
        Objects.requireNonNull(claim, "claim must not be null");
        Objects.requireNonNull(evidenceByItem, "evidenceByItem must not be null");
        Objects.requireNonNull(actor, "actor must not be null");
        if (actor.resourceType() != ResourceType.EXPENSE_CLAIM || actor.action() != ResourceAction.CREATE) {
            throw new IllegalArgumentException("actor context must describe creating an expense claim");
        }
        if (actor.role() == UserRole.EMPLOYEE && !claim.employeeId().equals(actor.actorId())) {
            throw new IllegalArgumentException("employee may only create their own claim");
        }
        AccessDecision access = accessPolicy.decide(actor);
        ExpenseAssessment expense = expensePolicy.assess(claim, exchangeRates, today);
        List<CardEvidenceCheck> cards = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        for (ExpenseItem item : claim.items()) {
            if (item.paymentMethod() != PaymentMethod.CORPORATE_CARD) {
                continue;
            }
            CardEvidence evidence = evidenceByItem.get(item.itemId());
            if (evidence == null) {
                issues.add("corporate card evidence missing for item " + item.itemId());
                continue;
            }
            if (!item.itemId().equals(evidence.itemId())) {
                issues.add("card evidence item mismatch for item " + item.itemId());
                continue;
            }
            CardEvidenceCheck check = cardValidator.check(evidence);
            cards.add(check);
            if (!check.valid()) {
                issues.add("corporate card evidence invalid for item " + item.itemId());
            }
        }
        boolean ready = access.allowed() && !expense.rejected() && issues.isEmpty();
        return new ExpenseCardReview(claim.claimId(), access, expense, cards, issues,
                ready, expense.auditRequired() || access.auditRequired() || !issues.isEmpty());
    }

    public WorkflowSnapshot startApproval(
            ExpenseClaim claim, ExpenseCardReview review, List<ApprovalStep> steps, Instant now) {
        Objects.requireNonNull(claim, "claim must not be null");
        Objects.requireNonNull(review, "review must not be null");
        if (!claim.claimId().equals(review.claimId())) {
            throw new IllegalArgumentException("review belongs to a different claim");
        }
        if (!review.readyForApproval()) {
            throw new IllegalStateException("claim is not ready for approval");
        }
        return workflowEngine.start(claim.claimId(), claim.employeeId(), steps, now,
                review.auditRequired(), true);
    }
}
