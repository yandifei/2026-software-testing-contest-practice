package net.mooctest.grading.expression;

import net.mooctest.grading.domain.GradeReport;

import java.util.List;
import java.util.Objects;

public record ExpressionExamGrade(
        GradeReport report,
        List<ExpressionAnswerEvaluation> answerEvaluations
) {
    public ExpressionExamGrade {
        Objects.requireNonNull(report, "report must not be null");
        answerEvaluations = answerEvaluations == null
                ? List.of()
                : List.copyOf(answerEvaluations);
    }
}
