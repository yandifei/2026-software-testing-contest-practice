package net.mooctest.workflow;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record StepProgress(
        ApprovalStep step,
        Map<String, ApprovalDecision> decisions) {

    public StepProgress {
        step = Objects.requireNonNull(step, "step must not be null");
        decisions = Map.copyOf(Objects.requireNonNull(decisions));
        for (String actor : decisions.keySet()) {
            if (!step.approverIds().contains(actor)) {
                throw new IllegalArgumentException("decision actor is not an approver: " + actor);
            }
        }
    }

    public static StepProgress empty(ApprovalStep step) {
        return new StepProgress(step, Map.of());
    }

    public StepProgress decide(String actorId, ApprovalDecision decision) {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actorId must not be blank");
        }
        String actor = actorId.trim();
        if (!step.canApprove(actor)) {
            throw new IllegalArgumentException("actor is not an approver for the current step");
        }
        if (decisions.containsKey(actor)) {
            throw new IllegalStateException("actor has already decided this step");
        }
        Map<String, ApprovalDecision> updated = new LinkedHashMap<>(decisions);
        updated.put(actor, Objects.requireNonNull(decision));
        return new StepProgress(step, updated);
    }

    public StepProgress withDelegatedStep(ApprovalStep delegated, String previousActor) {
        if (decisions.containsKey(previousActor)) {
            throw new IllegalStateException("actor cannot delegate after making a decision");
        }
        return new StepProgress(delegated, decisions);
    }

    public int approvals() {
        return count(ApprovalDecision.APPROVE);
    }

    public int rejections() {
        return count(ApprovalDecision.REJECT);
    }

    public int abstentions() {
        return count(ApprovalDecision.ABSTAIN);
    }

    public boolean satisfied() {
        int approved = approvals();
        return switch (step.mode()) {
            case ANY_ONE -> approved >= 1;
            case MAJORITY, REQUIRED_COUNT -> approved >= step.requiredApprovals();
            case UNANIMOUS -> approved == step.approverIds().size();
        };
    }

    public boolean canStillSucceed() {
        int undecided = step.approverIds().size() - decisions.size();
        return approvals() + undecided >= step.requiredApprovals();
    }

    private int count(ApprovalDecision target) {
        int result = 0;
        for (ApprovalDecision decision : decisions.values()) {
            if (decision == target) {
                result++;
            }
        }
        return result;
    }
}
