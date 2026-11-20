package net.mooctest.grading.expression;

import net.mooctest.grading.domain.AnswerResult;
import net.mooctest.grading.domain.QuestionType;

import java.math.BigDecimal;
import java.util.Objects;

public class ExpressionAnswerGradingService {

    private final ExpressionEvaluator evaluator;

    public ExpressionAnswerGradingService(ExpressionEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
    }

    public ExpressionAnswerEvaluation evaluate(ExpressionAnswerSubmission submission) {
        Objects.requireNonNull(submission, "submission must not be null");
        ExpressionQuestion question = submission.question();
        String submittedExpression = submission.submittedExpression();

        if (submittedExpression == null || submittedExpression.isBlank()) {
            return new ExpressionAnswerEvaluation(
                    answerResult(question, false, false, null),
                    ExpressionAnswerStatus.UNANSWERED,
                    null,
                    null,
                    null,
                    "ANSWER_NOT_PROVIDED"
            );
        }

        BigDecimal expectedValue;
        try {
            expectedValue = evaluator.evaluate(question.correctExpression());
        } catch (ExpressionEvaluationException exception) {
            throw new IllegalStateException(
                    "invalid correct expression for question " + question.questionId(),
                    exception
            );
        }

        BigDecimal actualValue;
        try {
            actualValue = evaluator.evaluate(submittedExpression);
        } catch (ExpressionEvaluationException exception) {
            return new ExpressionAnswerEvaluation(
                    answerResult(question, true, false, null),
                    ExpressionAnswerStatus.INVALID_EXPRESSION,
                    expectedValue,
                    null,
                    null,
                    "INVALID_STUDENT_EXPRESSION"
            );
        }

        BigDecimal absoluteError = actualValue.subtract(expectedValue).abs();
        boolean exact = absoluteError.compareTo(BigDecimal.ZERO) == 0;
        ExpressionAnswerStatus status;
        if (exact) {
            status = ExpressionAnswerStatus.EXACT;
        } else if (absoluteError.compareTo(question.tolerance()) <= 0) {
            status = ExpressionAnswerStatus.WITHIN_TOLERANCE;
        } else {
            status = ExpressionAnswerStatus.INCORRECT;
        }

        return new ExpressionAnswerEvaluation(
                answerResult(question, true, exact, absoluteError),
                status,
                expectedValue,
                actualValue,
                absoluteError,
                status.name()
        );
    }

    private AnswerResult answerResult(
            ExpressionQuestion question,
            boolean answered,
            boolean exactMatch,
            BigDecimal numericError
    ) {
        return new AnswerResult(
                question.questionId(),
                QuestionType.NUMERIC,
                question.section(),
                question.maximumPoints(),
                question.mandatory(),
                answered,
                exactMatch,
                0,
                0,
                0,
                numericError,
                question.tolerance(),
                0,
                0,
                0,
                false,
                false,
                false,
                false
        );
    }
}
