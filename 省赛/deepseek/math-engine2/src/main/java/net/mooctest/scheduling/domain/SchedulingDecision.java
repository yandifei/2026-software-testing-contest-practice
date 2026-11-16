package net.mooctest.scheduling.domain;

import java.util.List;

public record SchedulingDecision(
        boolean approved,
        boolean manualReview,
        String status,
        int durationMinutes,
        List<String> conflicts,
        List<String> warnings
) {
    public SchedulingDecision {
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
