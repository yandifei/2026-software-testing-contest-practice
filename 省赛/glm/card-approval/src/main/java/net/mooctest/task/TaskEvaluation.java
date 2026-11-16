package net.mooctest.task;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record TaskEvaluation(
        TaskPriority effectivePriority,
        boolean overdue,
        boolean escalationRequired,
        boolean reassignmentRecommended,
        Set<TaskAction> allowedActions,
        List<String> warnings) {

    public TaskEvaluation {
        effectivePriority = Objects.requireNonNull(effectivePriority);
        allowedActions = Set.copyOf(Objects.requireNonNull(allowedActions));
        warnings = List.copyOf(Objects.requireNonNull(warnings));
    }
}
