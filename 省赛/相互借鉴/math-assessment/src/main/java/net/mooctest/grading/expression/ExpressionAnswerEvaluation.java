package net.mooctest.grading.expression;

import net.mooctest.grading.domain.AnswerResult;

import java.math.BigDecimal;
import java.util.Objects;

public record ExpressionAnswerEvaluation(
        AnswerResult answerResult,
        ExpressionAnswerStatus status,
        BigDecimal expectedValue,
        BigDecimal actualValue,
        BigDecimal absoluteError,
        String message
) {
    public ExpressionAnswerEvaluation {
        Objects.requireNonNull(answerResult, "answerResult must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
