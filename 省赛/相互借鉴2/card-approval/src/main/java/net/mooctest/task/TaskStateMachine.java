package net.mooctest.task;

import java.time.Instant;
import java.util.Objects;

public final class TaskStateMachine {

    public Task apply(Task task, TaskAction action, TaskContext context) {
        Objects.requireNonNull(task, "task must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(context, "context must not be null");

        TaskEvaluation evaluation = new TaskPolicy().evaluate(task, context);
        if (!evaluation.allowedActions().contains(action)) {
            throw new IllegalStateException("action " + action + " is not allowed from " + task.status());
        }

        TaskStatus newStatus = switch (action) {
            case PUBLISH -> TaskStatus.OPEN;
            case START, UNBLOCK, REJECT_REVIEW, RESTORE -> TaskStatus.IN_PROGRESS;
            case BLOCK -> TaskStatus.BLOCKED;
            case SUBMIT_FOR_REVIEW -> TaskStatus.IN_REVIEW;
            case APPROVE, COMPLETE -> TaskStatus.COMPLETED;
            case REOPEN -> TaskStatus.OPEN;
            case CANCEL -> TaskStatus.CANCELLED;
            case ARCHIVE -> TaskStatus.ARCHIVED;
        };

        Instant startedAt = task.startedAt();
        Instant completedAt = null;
        if (action == TaskAction.START && startedAt == null) {
            startedAt = context.now();
        }
        if (action == TaskAction.APPROVE || action == TaskAction.COMPLETE) {
            completedAt = context.now();
        }
        if (action == TaskAction.REOPEN || action == TaskAction.RESTORE) {
            startedAt = null;
        }

        return new Task(
                task.id(), task.title(), task.description(), task.creatorId(), task.assigneeId(),
                newStatus, task.priority(), task.recurrence(), task.createdAt(), task.dueAt(),
                startedAt, completedAt, task.estimatedMinutes(), task.actualMinutes(),
                task.dependencyIds(), task.tags(), task.confidential(), task.reviewRequired());
    }
}
