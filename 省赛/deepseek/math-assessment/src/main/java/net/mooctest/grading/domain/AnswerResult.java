package net.mooctest.grading.domain;

import java.math.BigDecimal;

public record AnswerResult(
        String questionId,
        QuestionType type,
        String section,
        BigDecimal maximumPoints,
        boolean mandatory,
        boolean answered,
        boolean exactMatch,
        int correctOptionCount,
        int selectedCorrectOptions,
        int selectedIncorrectOptions,
        BigDecimal numericError,
        BigDecimal numericTolerance,
        int rubricPercent,
        int passedTests,
        int totalTests,
        boolean compilationSucceeded,
        boolean plagiarismFlag,
        boolean timedOut,
        boolean manualReviewRequired
) {
}
