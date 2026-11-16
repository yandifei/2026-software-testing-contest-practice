package net.mooctest.workflow;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record ApprovalStep(
        String stepId,
        String name,
        Set<String> approverIds,
        StepMode mode,
        int requiredApprovals,
        boolean delegationAllowed,
        boolean skipRequester,
        Instant deadline) {

    public ApprovalStep {
        if (stepId == null || stepId.isBlank()) {
            throw new IllegalArgumentException("stepId must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        stepId = stepId.trim();
        name = name.trim();
        approverIds = normalizeApprovers(approverIds);
        mode = Objects.requireNonNull(mode, "mode must not be null");
        if (approverIds.isEmpty()) {
            throw new IllegalArgumentException("step requires at least one approver");
        }
        if (requiredApprovals < 1 || requiredApprovals > approverIds.size()) {
            throw new IllegalArgumentException("requiredApprovals is outside approver count");
        }
        if (mode == StepMode.ANY_ONE && requiredApprovals != 1) {
            throw new IllegalArgumentException("ANY_ONE requires exactly one approval");
        }
        if (mode == StepMode.UNANIMOUS && requiredApprovals != approverIds.size()) {
            throw new IllegalArgumentException("UNANIMOUS requires every approver");
        }
        if (mode == StepMode.MAJORITY && requiredApprovals <= approverIds.size() / 2) {
            throw new IllegalArgumentException("MAJORITY threshold must exceed half of approvers");
        }
    }

    public boolean canApprove(String actorId) {
        return actorId != null && approverIds.contains(actorId.trim());
    }

    public ApprovalStep delegate(String fromActor, String toActor) {
        if (!delegationAllowed) {
            throw new IllegalStateException("delegation is not allowed for this step");
        }
        if (fromActor == null || !approverIds.contains(fromActor.trim())) {
            throw new IllegalArgumentException("delegating actor is not an approver");
        }
        if (toActor == null || toActor.isBlank()) {
            throw new IllegalArgumentException("delegate must not be blank");
        }
        String target = toActor.trim();
        if (target.equals(fromActor.trim())) {
            throw new IllegalArgumentException("cannot delegate to the same actor");
        }
        if (approverIds.contains(target)) {
            throw new IllegalArgumentException("delegate is already an approver");
        }
        Set<String> replaced = new LinkedHashSet<>(approverIds);
        replaced.remove(fromActor.trim());
        replaced.add(target);
        return new ApprovalStep(
                stepId, name, replaced, mode, requiredApprovals,
                delegationAllowed, skipRequester, deadline);
    }

    private static Set<String> normalizeApprovers(Set<String> approvers) {
        Objects.requireNonNull(approvers, "approverIds must not be null");
        Set<String> normalized = new LinkedHashSet<>();
        for (String approver : approvers) {
            if (approver == null || approver.isBlank()) {
                throw new IllegalArgumentException("approverId must not be blank");
            }
            normalized.add(approver.trim());
        }
        return Set.copyOf(normalized);
    }
}
