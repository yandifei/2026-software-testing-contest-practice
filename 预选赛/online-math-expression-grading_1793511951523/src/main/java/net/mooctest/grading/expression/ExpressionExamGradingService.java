package net.mooctest.grading.expression;

import net.mooctest.grading.domain.AnswerResult;
import net.mooctest.grading.domain.ExamSubmission;
import net.mooctest.grading.domain.GradeReport;
import net.mooctest.grading.domain.GradingPolicy;
import net.mooctest.grading.service.ExamGradingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExpressionExamGradingService {

    private final ExpressionAnswerGradingService answerGradingService;
    private final ExamGradingService examGradingService;

    public ExpressionExamGradingService(
            ExpressionAnswerGradingService answerGradingService,
            ExamGradingService examGradingService
    ) {
        this.answerGradingService = Objects.requireNonNull(
                answerGradingService,
                "answerGradingService must not be null"
        );
        this.examGradingService = Objects.requireNonNull(
                examGradingService,
                "examGradingService must not be null"
        );
    }

    public ExpressionExamGrade grade(ExpressionExamSubmission submission, GradingPolicy policy) {
        Objects.requireNonNull(submission, "submission must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        List<ExpressionAnswerEvaluation> evaluations = new ArrayList<>();
        List<AnswerResult> answers = new ArrayList<>();
        for (ExpressionAnswerSubmission answer : submission.answers()) {
            ExpressionAnswerEvaluation evaluation = answerGradingService.evaluate(answer);
            evaluations.add(evaluation);
            answers.add(evaluation.answerResult());
        }

        ExamSubmission examSubmission = submission.toExamSubmission(answers);
        GradeReport report = examGradingService.grade(examSubmission, policy);
        return new ExpressionExamGrade(report, evaluations);
    }
}
