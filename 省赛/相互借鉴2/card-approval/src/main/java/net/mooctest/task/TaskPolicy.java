package net.mooctest.task;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Calculates task urgency and actor-specific allowed actions. */
public final class TaskPolicy {

    public TaskEvaluation evaluate(Task task, TaskContext context) {
        Objects.requireNonNull(task, "task must not be null");
        Objects.requireNonNull(context, "context must not be null");

        List<String> warnings = new ArrayList<>();
        boolean overdue = isOverdue(task, context.now());
        TaskPriority effectivePriority = calculatePriority(task, context, overdue, warnings);
        boolean escalation = requiresEscalation(task, context, overdue, effectivePriority, warnings);
        boolean reassignment = recommendsReassignment(task, context, overdue, warnings);
        Set<TaskAction> actions = allowedActions(task, context, warnings);

        return new TaskEvaluation(
                effectivePriority,
                overdue,
                escalation,
                reassignment,
                actions,
                warnings);
    }

    public TaskPriority calculatePriority(
            Task task,
            TaskContext context,
            boolean overdue,
            List<String> warnings) {
        int score = switch (task.priority()) {
            case LOW -> 0;
            case NORMAL -> 10;
            case HIGH -> 20;
            case URGENT -> 30;
            case CRITICAL -> 40;
        };

        if (task.dueAt() != null) {
            long hours = Duration.between(context.now(), task.dueAt()).toHours();
            if (hours <= 0) {
                score += 30;
            } else if (hours <= 4) {
                score += 20;
            } else if (hours <= 24) {
                score += 12;
            } else if (hours <= 72) {
                score += 5;
            }
        }
        if (overdue) {
            long overdueDays = ChronoUnit.DAYS.between(task.dueAt(), context.now());
            if (overdueDays >= 7) {
                score += 25;
            } else if (overdueDays >= 3) {
                score += 15;
            } else {
                score += 8;
            }
            warnings.add("task is overdue");
        }
        if (task.status() == TaskStatus.BLOCKED) {
            score += 10;
        }
        if (task.confidential()) {
            score += 3;
        }
        if (task.dependencyIds().size() >= 5) {
            score += 5;
        }
        if (context.overdueTaskCount() >= 3) {
            score += 5;
        }
        if (context.overdueTaskCount() >= 10) {
            score += 10;
        }
        if (!context.projectActive()) {
            score = Math.min(score, 10);
            warnings.add("inactive project suppresses task escalation");
        }
        if (task.status() == TaskStatus.COMPLETED
                || task.status() == TaskStatus.CANCELLED
                || task.status() == TaskStatus.ARCHIVED) {
            score = 0;
        }

        if (score >= 60) {
            return TaskPriority.CRITICAL;
        }
        if (score >= 40) {
            return TaskPriority.URGENT;
        }
        if (score >= 25) {
            return TaskPriority.HIGH;
        }
        if (score >= 10) {
            return TaskPriority.NORMAL;
        }
        return TaskPriority.LOW;
    }

    private static boolean requiresEscalation(
            Task task,
            TaskContext context,
            boolean overdue,
            TaskPriority priority,
            List<String> warnings) {
        if (task.isTerminal()) {
            return false;
        }
        if (priority == TaskPriority.CRITICAL) {
            warnings.add("critical task requires escalation");
            return true;
        }
        if (overdue && task.priority() == TaskPriority.URGENT) {
            return true;
        }
        if (overdue && context.overdueTaskCount() >= 5) {
            return true;
        }
        if (task.status() == TaskStatus.BLOCKED && task.startedAt() != null
                && task.startedAt().isBefore(context.now().minus(3, ChronoUnit.DAYS))) {
            warnings.add("task has been blocked for more than three days");
            return true;
        }
        if (task.reviewRequired() && task.status() == TaskStatus.IN_REVIEW
                && task.dueAt() != null && task.dueAt().isBefore(context.now().plus(4, ChronoUnit.HOURS))) {
            return true;
        }
        return !context.assigneeActive() && task.assigneeId() != null;
    }

    private static boolean recommendsReassignment(
            Task task,
            TaskContext context,
            boolean overdue,
            List<String> warnings) {
        if (task.isTerminal()) {
            return false;
        }
        if (task.assigneeId() == null || task.assigneeId().isBlank()) {
            warnings.add("task has no assignee");
            return true;
        }
        if (!context.assigneeActive()) {
            warnings.add("assignee is inactive");
            return true;
        }
        if (context.assigneeOpenTaskCount() > 20) {
            warnings.add("assignee workload is very high");
            return true;
        }
        if (context.assigneeOpenTaskCount() > 12 && task.priority() == TaskPriority.LOW) {
            return true;
        }
        if (overdue && context.assigneeOpenTaskCount() > 10) {
            return true;
        }
        if (task.status() == TaskStatus.BLOCKED && context.previousReopens() >= 2) {
            return true;
        }
        return false;
    }

    private static Set<TaskAction> allowedActions(
            Task task,
            TaskContext context,
            List<String> warnings) {
        EnumSet<TaskAction> actions = EnumSet.noneOf(TaskAction.class);
        boolean creator = task.creatorId().equals(context.actorId());
        boolean assignee = task.assigneeId() != null && task.assigneeId().equals(context.actorId());
        boolean manager = context.actorIsManager();
        boolean administrator = context.actorIsAdministrator();

        switch (task.status()) {
            case DRAFT -> {
                if (creator || administrator) {
                    actions.add(TaskAction.PUBLISH);
                    actions.add(TaskAction.CANCEL);
                }
            }
            case OPEN -> {
                if ((assignee || manager || administrator) && context.dependenciesComplete()) {
                    actions.add(TaskAction.START);
                }
                if (creator || manager || administrator) {
                    actions.add(TaskAction.CANCEL);
                }
                if (!context.dependenciesComplete()) {
                    warnings.add("dependencies prevent task start");
                }
            }
            case IN_PROGRESS -> {
                if (assignee || manager || administrator) {
                    actions.add(TaskAction.BLOCK);
                    if (task.reviewRequired()) {
                        actions.add(TaskAction.SUBMIT_FOR_REVIEW);
                    } else {
                        actions.add(TaskAction.COMPLETE);
                    }
                }
                if (manager || administrator) {
                    actions.add(TaskAction.CANCEL);
                }
            }
            case BLOCKED -> {
                if (assignee || manager || administrator) {
                    actions.add(TaskAction.UNBLOCK);
                }
                if (manager || administrator) {
                    actions.add(TaskAction.CANCEL);
                }
            }
            case IN_REVIEW -> {
                if (manager || administrator) {
                    actions.add(TaskAction.APPROVE);
                    actions.add(TaskAction.REJECT_REVIEW);
                    actions.add(TaskAction.CANCEL);
                }
            }
            case COMPLETED -> {
                if (manager || administrator) {
                    actions.add(TaskAction.REOPEN);
                    actions.add(TaskAction.ARCHIVE);
                }
            }
            case CANCELLED -> {
                if (creator || manager || administrator) {
                    actions.add(TaskAction.REOPEN);
                    actions.add(TaskAction.ARCHIVE);
                }
            }
            case ARCHIVED -> {
                if (administrator || manager) {
                    actions.add(TaskAction.RESTORE);
                }
            }
        }
        if (!context.projectActive()) {
            actions.remove(TaskAction.PUBLISH);
            actions.remove(TaskAction.START);
            actions.remove(TaskAction.REOPEN);
        }
        if (!context.assigneeActive()) {
            actions.remove(TaskAction.START);
            actions.remove(TaskAction.UNBLOCK);
        }
        if (!context.withinWorkingHours() && !administrator) {
            actions.remove(TaskAction.PUBLISH);
            actions.remove(TaskAction.APPROVE);
        }
        return actions;
    }

    private static boolean isOverdue(Task task, Instant now) {
        if (task.dueAt() == null) {
            return false;
        }
        if (task.status() == TaskStatus.COMPLETED
                || task.status() == TaskStatus.CANCELLED
                || task.status() == TaskStatus.ARCHIVED) {
            return false;
        }
        return task.dueAt().isBefore(now);
    }
}
