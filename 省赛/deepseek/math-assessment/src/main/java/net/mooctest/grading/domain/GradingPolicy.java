package net.mooctest.grading.domain;

import java.math.BigDecimal;

public record GradingPolicy(
        int passPercentage,
        boolean partialCreditEnabled,
        boolean negativeMarkingEnabled,
        BigDecimal wrongAnswerPenaltyRate,
        BigDecimal unansweredPenaltyRate,
        int latePenaltyPercentPerHour,
        int maximumLatePenaltyPercent,
        int minimumSectionPercentage,
        boolean allMandatoryQuestionsRequired,
        boolean identityVerificationRequired,
        int maximumAttempts,
        boolean failOnIntegrityViolation,
        boolean capScoreAtOneHundred,
        boolean programmingCompilationRequired
) {
}
