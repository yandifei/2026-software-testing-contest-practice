package net.mooctest.grading.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ExamSubmission(
        String examId,
        String studentId,
        List<AnswerResult> answers,
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
    public ExamSubmission {
        answers = answers == null ? List.of() : List.copyOf(answers);
        sectionPassPercentages = sectionPassPercentages == null
                ? Map.of()
                : Map.copyOf(sectionPassPercentages);
    }
}
