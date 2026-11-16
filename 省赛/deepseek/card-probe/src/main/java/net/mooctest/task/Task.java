package net.mooctest.task;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record Task(
        String id,
        String title,
        String description,
        String creatorId,
        String assigneeId,
        TaskStatus status,
        TaskPriority priority,
        RecurrenceRule recurrence,
        Instant createdAt,
        Instant dueAt,
        Instant startedAt,
        Instant completedAt,
        int estimatedMinutes,
        int actualMinutes,
        List<String> dependencyIds,
        Set<String> tags,
        boolean confidential,
        boolean reviewRequired) {

    public Task {
        id = requireText(id, "id");
        title = requireText(title, "title");
        creatorId = requireText(creatorId, "creatorId");
        if (assigneeId != null) {
            assigneeId = assigneeId.trim();
        }
        status = Objects.requireNonNull(status, "status must not be null");
        priority = Objects.requireNonNull(priority, "priority must not be null");
        recurrence = Objects.requireNonNull(recurrence, "recurrence must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        dependencyIds = List.copyOf(Objects.requireNonNull(dependencyIds));
        tags = Set.copyOf(Objects.requireNonNull(tags));
        if (title.length() > 120) {
            throw new IllegalArgumentException("title cannot exceed 120 characters");
        }
        if (description != null && description.length() > 5000) {
            throw new IllegalArgumentException("description cannot exceed 5000 characters");
        }
        if (dueAt != null && dueAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("dueAt must not be before createdAt");
        }
        if (estimatedMinutes < 0 || estimatedMinutes > 100_000) {
            throw new IllegalArgumentException("estimatedMinutes is outside the supported range");
        }
        if (actualMinutes < 0 || actualMinutes > 200_000) {
            throw new IllegalArgumentException("actualMinutes is outside the supported range");
        }
        if (dependencyIds.contains(id)) {
            throw new IllegalArgumentException("task cannot depend on itself");
        }
        if (dependencyIds.size() != dependencyIds.stream().distinct().count()) {
            throw new IllegalArgumentException("dependencies must be unique");
        }
        if ((status == TaskStatus.IN_PROGRESS || status == TaskStatus.BLOCKED
                || status == TaskStatus.IN_REVIEW || status == TaskStatus.COMPLETED)
                && startedAt == null) {
            throw new IllegalArgumentException("startedAt is required after work begins");
        }
        if (status == TaskStatus.COMPLETED && completedAt == null) {
            throw new IllegalArgumentException("completedAt is required for completed task");
        }
        if (status != TaskStatus.COMPLETED && completedAt != null) {
            throw new IllegalArgumentException("completedAt is only valid for completed task");
        }
    }

    public boolean isTerminal() {
        return status == TaskStatus.COMPLETED
                || status == TaskStatus.CANCELLED
                || status == TaskStatus.ARCHIVED;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
