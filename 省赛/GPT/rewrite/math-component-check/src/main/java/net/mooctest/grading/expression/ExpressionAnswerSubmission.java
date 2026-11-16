package net.mooctest.grading.expression;

import java.util.Objects;

public record ExpressionAnswerSubmission(
        ExpressionQuestion question,
        String submittedExpression
) {
    public ExpressionAnswerSubmission {
        Objects.requireNonNull(question, "question must not be null");
    }
}
