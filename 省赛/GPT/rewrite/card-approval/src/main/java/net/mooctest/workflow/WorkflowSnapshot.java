package net.mooctest.workflow;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record WorkflowSnapshot(
        String workflowId,
        String requesterId,
        WorkflowState state,
        List<StepProgress> steps,
        int currentStepIndex,
        Instant createdAt,
        Instant updatedAt,
        long version,
        boolean urgent,
        boolean withdrawalAllowed) {

    public WorkflowSnapshot {
        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalArgumentException("workflowId must not be blank");
        }
        if (requesterId == null || requesterId.isBlank()) {
            throw new IllegalArgumentException("requesterId must not be blank");
        }
        workflowId = workflowId.trim();
        requesterId = requesterId.trim();
        state = Objects.requireNonNull(state, "state must not be null");
        steps = List.copyOf(Objects.requireNonNull(steps));
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("workflow requires at least one step");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        if (isTerminal(state)) {
            if (currentStepIndex < 0 || currentStepIndex > steps.size()) {
                throw new IllegalArgumentException("terminal currentStepIndex is invalid");
            }
        } else if (currentStepIndex < 0 || currentStepIndex >= steps.size()) {
            throw new IllegalArgumentException("active currentStepIndex is invalid");
        }
    }

    public StepProgress currentStep() {
        if (currentStepIndex >= steps.size()) {
            throw new IllegalStateException("terminal workflow has no current step");
        }
        return steps.get(currentStepIndex);
    }

    public boolean terminal() {
        return isTerminal(state);
    }

    private static boolean isTerminal(WorkflowState state) {
        return state == WorkflowState.APPROVED
                || state == WorkflowState.REJECTED
                || state == WorkflowState.RETURNED
                || state == WorkflowState.WITHDRAWN
                || state == WorkflowState.CANCELLED
                || state == WorkflowState.EXPIRED;
    }
}
