package net.mooctest.grading.expression;

import java.math.BigDecimal;
import java.util.Objects;

public record ExpressionQuestion(
        String questionId,
        String section,
        String correctExpression,
        BigDecimal maximumPoints,
        BigDecimal tolerance,
        boolean mandatory
) {
    public ExpressionQuestion {
        questionId = requireText(questionId, "questionId");
        section = requireText(section, "section");
        correctExpression = requireText(correctExpression, "correctExpression");
        maximumPoints = Objects.requireNonNull(maximumPoints, "maximumPoints must not be null");
        tolerance = Objects.requireNonNull(tolerance, "tolerance must not be null");
        if (maximumPoints.signum() <= 0) {
            throw new IllegalArgumentException("maximumPoints must be positive");
        }
        if (tolerance.signum() < 0) {
            throw new IllegalArgumentException("tolerance must not be negative");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
