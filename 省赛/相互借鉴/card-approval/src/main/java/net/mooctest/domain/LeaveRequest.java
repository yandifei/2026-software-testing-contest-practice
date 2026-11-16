package net.mooctest.domain;

import java.time.Instant;
import java.util.Objects;

/** Immutable leave request with explicit state transitions. */
public record LeaveRequest(
        String id,
        String employeeId,
        LeaveType type,
        DateRange period,
        String reason,
        LeaveStatus status,
        Instant requestedAt,
        String decidedBy,
        Instant decidedAt,
        String decisionComment) {

    public LeaveRequest {
        id = requireText(id, "id");
        employeeId = requireText(employeeId, "employeeId");
        type = Objects.requireNonNull(type, "type must not be null");
        period = Objects.requireNonNull(period, "period must not be null");
        reason = requireText(reason, "reason");
        status = Objects.requireNonNull(status, "status must not be null");
        requestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");

        if (status == LeaveStatus.PENDING) {
            if (decidedBy != null || decidedAt != null || decisionComment != null) {
                throw new IllegalArgumentException("pending request must not contain a decision");
            }
        } else {
            decidedBy = requireText(decidedBy, "decidedBy");
            decidedAt = Objects.requireNonNull(decidedAt, "decidedAt must not be null");
            if (decisionComment != null) {
                decisionComment = decisionComment.trim();
            }
        }
    }

    public static LeaveRequest pending(
            String id,
            String employeeId,
            LeaveType type,
            DateRange period,
            String reason,
            Instant requestedAt) {
        return new LeaveRequest(
                id, employeeId, type, period, reason, LeaveStatus.PENDING,
                requestedAt, null, null, null);
    }

    public LeaveRequest approve(String approverId, Instant decisionTime) {
        ensurePending();
        return new LeaveRequest(
                id, employeeId, type, period, reason, LeaveStatus.APPROVED,
                requestedAt, approverId, decisionTime, null);
    }

    public LeaveRequest reject(String approverId, Instant decisionTime, String comment) {
        ensurePending();
        String rejectionComment = requireText(comment, "comment");
        return new LeaveRequest(
                id, employeeId, type, period, reason, LeaveStatus.REJECTED,
                requestedAt, approverId, decisionTime, rejectionComment);
    }

    private void ensurePending() {
        if (status != LeaveStatus.PENDING) {
            throw new IllegalStateException("only a pending request can be decided");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
