package net.mooctest.grading.expression;

import net.mooctest.grading.domain.AnswerResult;
import net.mooctest.grading.domain.ExamSubmission;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ExpressionExamSubmission(
        String examId,
        String studentId,
        List<ExpressionAnswerSubmission> answers,
        Instant startedAt,
        Instant submittedAt,
        Instant deadline,
        int allowedDurationSeconds,
        int attemptNumber,
        boolean identityVerified,
        boolean termsAccepted,
        boolean suspendedAccount,
        boolean proctorDisconnected,
        boolean ipAddressChanged,
        int fullscreenExitCount,
        int copyPasteEventCount,
        BigDecimal bonusPoints,
        Integer instructorOverridePercent,
        Map<String, Integer> sectionPassPercentages
) {
    public ExpressionExamSubmission {
        answers = answers == null ? List.of() : List.copyOf(answers);
        sectionPassPercentages = sectionPassPercentages == null
                ? Map.of()
                : Map.copyOf(sectionPassPercentages);
    }

    public ExamSubmission toExamSubmission(List<AnswerResult> evaluatedAnswers) {
        return new ExamSubmission(
                examId,
                studentId,
                evaluatedAnswers,
                startedAt,
                submittedAt,
                deadline,
                allowedDurationSeconds,
                attemptNumber,
                identityVerified,
                termsAccepted,
                suspendedAccount,
                proctorDisconnected,
                ipAddressChanged,
                fullscreenExitCount,
                copyPasteEventCount,
                bonusPoints,
                instructorOverridePercent,
                sectionPassPercentages
        );
    }
}
