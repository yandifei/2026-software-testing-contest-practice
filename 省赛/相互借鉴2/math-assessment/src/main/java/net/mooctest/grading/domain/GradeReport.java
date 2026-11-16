package net.mooctest.grading.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record GradeReport(
        BigDecimal rawPoints,
        BigDecimal finalPoints,
        BigDecimal maximumPoints,
        int percentage,
        String letterGrade,
        boolean passed,
        boolean manualReview,
        Map<String, Integer> sectionPercentages,
        List<String> failures,
        List<String> warnings
) {
    public GradeReport {
        sectionPercentages = sectionPercentages == null ? Map.of() : Map.copyOf(sectionPercentages);
        failures = failures == null ? List.of() : List.copyOf(failures);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
