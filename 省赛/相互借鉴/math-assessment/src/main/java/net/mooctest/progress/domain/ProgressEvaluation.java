package net.mooctest.progress.domain;

import java.util.List;

public record ProgressEvaluation(
        int overallPercent,
        int completedLessons,
        int totalLessons,
        boolean courseCompleted,
        boolean certificateEligible,
        String nextLessonId,
        List<String> blockers,
        List<String> warnings
) {
    public ProgressEvaluation {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
