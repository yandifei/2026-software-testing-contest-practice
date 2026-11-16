package net.mooctest.workflow;

import java.time.Instant;
import java.util.Objects;

public record WorkflowAction(
        String actorId,
        ApprovalDecision decision,
        String comment,
        Instant actionTime) {

    public WorkflowAction {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actorId must not be blank");
        }
        actorId = actorId.trim();
        decision = Objects.requireNonNull(decision, "decision must not be null");
        actionTime = Objects.requireNonNull(actionTime, "actionTime must not be null");
        if ((decision == ApprovalDecision.REJECT || decision == ApprovalDecision.RETURN)
                && (comment == null || comment.isBlank())) {
            throw new IllegalArgumentException("reject and return decisions require a comment");
        }
        if (comment != null) {
            comment = comment.trim();
        }
    }
}
