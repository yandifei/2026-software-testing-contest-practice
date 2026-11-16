package net.mooctest.grading.service;

import net.mooctest.grading.domain.AnswerResult;
import net.mooctest.grading.domain.ExamSubmission;
import net.mooctest.grading.domain.GradeReport;
import net.mooctest.grading.domain.GradingPolicy;
import net.mooctest.grading.domain.QuestionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ExamGradingService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    public GradeReport grade(ExamSubmission submission, GradingPolicy policy) {
        validate(submission, policy);
        List<String> failures = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, BigDecimal> sectionEarned = new HashMap<>();
        Map<String, BigDecimal> sectionMaximum = new HashMap<>();
        BigDecimal rawPoints = ZERO;
        BigDecimal maximumPoints = ZERO;
        boolean manualReview = false;

        for (AnswerResult answer : submission.answers()) {
            BigDecimal awarded = scoreAnswer(answer, policy, warnings);
            rawPoints = rawPoints.add(awarded);
            maximumPoints = maximumPoints.add(answer.maximumPoints());
            sectionEarned.merge(answer.section(), awarded, BigDecimal::add);
            sectionMaximum.merge(answer.section(), answer.maximumPoints(), BigDecimal::add);

            if (answer.mandatory() && !answer.answered()) {
                failures.add("MANDATORY_QUESTION_UNANSWERED:" + answer.questionId());
            }
            if (answer.mandatory() && awarded.signum() <= 0) {
                warnings.add("MANDATORY_QUESTION_NO_CREDIT:" + answer.questionId());
            }
            if (answer.plagiarismFlag()) {
                manualReview = true;
                failures.add("ANSWER_INTEGRITY_VIOLATION:" + answer.questionId());
            }
            if (answer.manualReviewRequired()) {
                manualReview = true;
                warnings.add("MANUAL_GRADING_REQUIRED:" + answer.questionId());
            }
            if (answer.timedOut()) {
                warnings.add("QUESTION_TIMED_OUT:" + answer.questionId());
            }
        }

        rawPoints = money(rawPoints.max(BigDecimal.ZERO));
        maximumPoints = money(maximumPoints);
        BigDecimal adjusted = applySubmissionRules(
                rawPoints,
                maximumPoints,
                submission,
                policy,
                failures,
                warnings
        );
        Map<String, Integer> sectionPercentages = calculateSections(
                sectionEarned,
                sectionMaximum,
                submission,
                policy,
                failures
        );

        int percentage = maximumPoints.signum() == 0
                ? 0
                : adjusted.multiply(BigDecimal.valueOf(100))
                .divide(maximumPoints, 0, RoundingMode.HALF_UP)
                .intValue();
        if (policy.capScoreAtOneHundred()) {
            percentage = Math.min(100, percentage);
            adjusted = adjusted.min(maximumPoints);
        }
        percentage = Math.max(0, percentage);
        adjusted = money(adjusted.max(BigDecimal.ZERO));

        evaluateSubmissionIntegrity(submission, policy, failures, warnings);
        if (submission.proctorDisconnected()
                || submission.fullscreenExitCount() >= 3
                || submission.copyPasteEventCount() >= 5) {
            manualReview = true;
        }
        if (submission.instructorOverridePercent() != null) {
            percentage = submission.instructorOverridePercent();
            adjusted = money(maximumPoints.multiply(BigDecimal.valueOf(percentage).movePointLeft(2)));
            warnings.add("INSTRUCTOR_OVERRIDE_APPLIED");
        }

        boolean passed = percentage >= policy.passPercentage()
                && sectionRequirementsMet(sectionPercentages, submission, policy)
                && mandatoryRequirementsMet(submission, policy)
                && (!policy.failOnIntegrityViolation() || !hasIntegrityFailure(failures));
        if (!passed && percentage < policy.passPercentage()) {
            failures.add("OVERALL_SCORE_BELOW_PASS_MARK");
        }
        if (manualReview) {
            warnings.add("RESULT_PENDING_MANUAL_REVIEW");
        }

        return new GradeReport(
                rawPoints,
                adjusted,
                maximumPoints,
                percentage,
                letterGrade(percentage, passed),
                passed,
                manualReview,
                sectionPercentages,
                failures,
                warnings
        );
    }

    private BigDecimal scoreAnswer(
            AnswerResult answer,
            GradingPolicy policy,
            List<String> warnings
    ) {
        BigDecimal maximum = answer.maximumPoints();
        if (!answer.answered()) {
            if (policy.unansweredPenaltyRate().signum() > 0) {
                warnings.add("UNANSWERED_PENALTY:" + answer.questionId());
                return maximum.multiply(policy.unansweredPenaltyRate()).negate();
            }
            return ZERO;
        }

        BigDecimal score = switch (answer.type()) {
            case SINGLE_CHOICE, TRUE_FALSE -> scoreBinary(answer, maximum, policy);
            case MULTIPLE_CHOICE -> scoreMultipleChoice(answer, maximum, policy, warnings);
            case FILL_BLANK -> scoreFillBlank(answer, maximum, policy);
            case NUMERIC -> scoreNumeric(answer, maximum, policy, warnings);
            case ESSAY -> scoreEssay(answer, maximum, warnings);
            case PROGRAMMING -> scoreProgramming(answer, maximum, policy, warnings);
        };
        if (answer.timedOut() && score.signum() > 0) {
            score = score.multiply(new BigDecimal("0.80"));
        }
        if (score.compareTo(maximum) > 0) {
            score = maximum;
        }
        if (!policy.negativeMarkingEnabled() && score.signum() < 0) {
            score = ZERO;
        }
        return money(score);
    }

    private BigDecimal scoreBinary(
            AnswerResult answer,
            BigDecimal maximum,
            GradingPolicy policy
    ) {
        if (answer.exactMatch()) {
            return maximum;
        }
        if (policy.negativeMarkingEnabled()) {
            return maximum.multiply(policy.wrongAnswerPenaltyRate()).negate();
        }
        return ZERO;
    }

    private BigDecimal scoreMultipleChoice(
            AnswerResult answer,
            BigDecimal maximum,
            GradingPolicy policy,
            List<String> warnings
    ) {
        if (answer.exactMatch()) {
            return maximum;
        }
        if (answer.correctOptionCount() == 0) {
            warnings.add("MULTIPLE_CHOICE_HAS_NO_CORRECT_OPTIONS:" + answer.questionId());
            return ZERO;
        }
        if (!policy.partialCreditEnabled()) {
            return policy.negativeMarkingEnabled() && answer.selectedIncorrectOptions() > 0
                    ? maximum.multiply(policy.wrongAnswerPenaltyRate()).negate()
                    : ZERO;
        }
        BigDecimal earnedRate = BigDecimal.valueOf(answer.selectedCorrectOptions())
                .divide(BigDecimal.valueOf(answer.correctOptionCount()), 6, RoundingMode.HALF_UP);
        BigDecimal incorrectRate = BigDecimal.valueOf(answer.selectedIncorrectOptions())
                .divide(BigDecimal.valueOf(Math.max(1, answer.correctOptionCount())), 6, RoundingMode.HALF_UP);
        BigDecimal score = maximum.multiply(earnedRate.min(BigDecimal.ONE));
        if (answer.selectedIncorrectOptions() > 0) {
            BigDecimal deductionRate = policy.negativeMarkingEnabled()
                    ? incorrectRate.multiply(policy.wrongAnswerPenaltyRate())
                    : incorrectRate.multiply(new BigDecimal("0.25"));
            score = score.subtract(maximum.multiply(deductionRate));
        }
        if (answer.selectedCorrectOptions() == 0 && answer.selectedIncorrectOptions() > 0) {
            warnings.add("ONLY_INCORRECT_OPTIONS_SELECTED:" + answer.questionId());
        }
        return score;
    }

    private BigDecimal scoreFillBlank(
            AnswerResult answer,
            BigDecimal maximum,
            GradingPolicy policy
    ) {
        if (answer.exactMatch()) {
            return maximum;
        }
        if (policy.partialCreditEnabled() && answer.rubricPercent() > 0) {
            return maximum.multiply(BigDecimal.valueOf(answer.rubricPercent()).movePointLeft(2));
        }
        if (policy.negativeMarkingEnabled()) {
            return maximum.multiply(policy.wrongAnswerPenaltyRate()).negate();
        }
        return ZERO;
    }

    private BigDecimal scoreNumeric(
            AnswerResult answer,
            BigDecimal maximum,
            GradingPolicy policy,
            List<String> warnings
    ) {
        if (answer.exactMatch()) {
            return maximum;
        }
        if (answer.numericError() == null || answer.numericTolerance() == null) {
            warnings.add("NUMERIC_TOLERANCE_DATA_MISSING:" + answer.questionId());
            return ZERO;
        }
        if (answer.numericError().abs().compareTo(answer.numericTolerance()) <= 0) {
            return maximum;
        }
        if (policy.partialCreditEnabled()
                && answer.numericTolerance().signum() > 0
                && answer.numericError().abs().compareTo(answer.numericTolerance().multiply(BigDecimal.TEN)) <= 0) {
            BigDecimal ratio = answer.numericError().abs()
                    .divide(answer.numericTolerance(), 6, RoundingMode.HALF_UP);
            BigDecimal credit = BigDecimal.ONE.subtract(ratio.subtract(BigDecimal.ONE)
                    .divide(BigDecimal.valueOf(9), 6, RoundingMode.HALF_UP));
            return maximum.multiply(credit.max(BigDecimal.ZERO));
        }
        return policy.negativeMarkingEnabled()
                ? maximum.multiply(policy.wrongAnswerPenaltyRate()).negate()
                : ZERO;
    }

    private BigDecimal scoreEssay(
            AnswerResult answer,
            BigDecimal maximum,
            List<String> warnings
    ) {
        if (answer.manualReviewRequired()) {
            warnings.add("ESSAY_SCORE_IS_PROVISIONAL:" + answer.questionId());
        }
        if (answer.rubricPercent() == 0 && !answer.exactMatch()) {
            return ZERO;
        }
        int percent = answer.exactMatch() ? 100 : answer.rubricPercent();
        return maximum.multiply(BigDecimal.valueOf(percent).movePointLeft(2));
    }

    private BigDecimal scoreProgramming(
            AnswerResult answer,
            BigDecimal maximum,
            GradingPolicy policy,
            List<String> warnings
    ) {
        if (!answer.compilationSucceeded()) {
            warnings.add("PROGRAM_DID_NOT_COMPILE:" + answer.questionId());
            return policy.programmingCompilationRequired()
                    ? ZERO
                    : maximum.multiply(new BigDecimal("0.10"));
        }
        if (answer.totalTests() == 0) {
            warnings.add("PROGRAM_HAS_NO_AUTOMATED_TESTS:" + answer.questionId());
            return answer.manualReviewRequired() ? ZERO : maximum.multiply(new BigDecimal("0.20"));
        }
        if (answer.passedTests() == answer.totalTests()) {
            return maximum;
        }
        if (!policy.partialCreditEnabled()) {
            return ZERO;
        }
        BigDecimal passRate = BigDecimal.valueOf(answer.passedTests())
                .divide(BigDecimal.valueOf(answer.totalTests()), 6, RoundingMode.HALF_UP);
        BigDecimal score = maximum.multiply(passRate);
        if (answer.timedOut()) {
            score = score.multiply(new BigDecimal("0.75"));
        }
        if (answer.plagiarismFlag()) {
            score = ZERO;
        }
        return score;
    }

    private BigDecimal applySubmissionRules(
            BigDecimal rawPoints,
            BigDecimal maximumPoints,
            ExamSubmission submission,
            GradingPolicy policy,
            List<String> failures,
            List<String> warnings
    ) {
        BigDecimal adjusted = rawPoints;
        if (submission.bonusPoints().signum() > 0) {
            adjusted = adjusted.add(submission.bonusPoints());
            warnings.add("BONUS_POINTS_APPLIED");
        }
        if (submission.deadline() != null && submission.submittedAt().isAfter(submission.deadline())) {
            long lateMinutes = Duration.between(submission.deadline(), submission.submittedAt()).toMinutes();
            long chargedHours = Math.max(1, (lateMinutes + 59) / 60);
            long penaltyPercent = Math.min(
                    policy.maximumLatePenaltyPercent(),
                    chargedHours * policy.latePenaltyPercentPerHour()
            );
            adjusted = adjusted.subtract(maximumPoints
                    .multiply(BigDecimal.valueOf(penaltyPercent).movePointLeft(2)));
            warnings.add("LATE_SUBMISSION_PENALTY:" + penaltyPercent);
            if (penaltyPercent >= 100) {
                failures.add("SUBMISSION_TOO_LATE");
            }
        }
        long duration = Duration.between(submission.startedAt(), submission.submittedAt()).toSeconds();
        if (submission.allowedDurationSeconds() > 0 && duration > submission.allowedDurationSeconds()) {
            long overtime = duration - submission.allowedDurationSeconds();
            if (overtime <= 60) {
                warnings.add("EXAM_GRACE_PERIOD_USED");
            } else if (overtime <= 300) {
                adjusted = adjusted.multiply(new BigDecimal("0.95"));
                warnings.add("EXAM_DURATION_MINOR_OVERRUN");
            } else {
                adjusted = adjusted.multiply(new BigDecimal("0.80"));
                failures.add("EXAM_DURATION_EXCEEDED");
            }
        }
        if (submission.attemptNumber() > policy.maximumAttempts()) {
            failures.add("MAXIMUM_ATTEMPTS_EXCEEDED");
            adjusted = ZERO;
        } else if (submission.attemptNumber() == policy.maximumAttempts()) {
            warnings.add("FINAL_ALLOWED_ATTEMPT");
        }
        return money(adjusted.max(BigDecimal.ZERO));
    }

    private Map<String, Integer> calculateSections(
            Map<String, BigDecimal> sectionEarned,
            Map<String, BigDecimal> sectionMaximum,
            ExamSubmission submission,
            GradingPolicy policy,
            List<String> failures
    ) {
        Map<String, Integer> percentages = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : sectionMaximum.entrySet()) {
            BigDecimal maximum = entry.getValue();
            BigDecimal earned = sectionEarned.getOrDefault(entry.getKey(), ZERO).max(BigDecimal.ZERO);
            int percent = maximum.signum() == 0
                    ? 0
                    : earned.multiply(BigDecimal.valueOf(100))
                    .divide(maximum, 0, RoundingMode.HALF_UP)
                    .intValue();
            percent = Math.max(0, Math.min(100, percent));
            percentages.put(entry.getKey(), percent);
            int required = submission.sectionPassPercentages()
                    .getOrDefault(entry.getKey(), policy.minimumSectionPercentage());
            if (percent < required) {
                failures.add("SECTION_BELOW_PASS_MARK:" + entry.getKey());
            }
        }
        for (String requiredSection : submission.sectionPassPercentages().keySet()) {
            if (!percentages.containsKey(requiredSection)) {
                percentages.put(requiredSection, 0);
                failures.add("REQUIRED_SECTION_MISSING:" + requiredSection);
            }
        }
        return percentages;
    }

    private void evaluateSubmissionIntegrity(
            ExamSubmission submission,
            GradingPolicy policy,
            List<String> failures,
            List<String> warnings
    ) {
        if (!submission.termsAccepted()) {
            failures.add("EXAM_TERMS_NOT_ACCEPTED");
        }
        if (submission.suspendedAccount()) {
            failures.add("ACCOUNT_SUSPENDED");
        }
        if (policy.identityVerificationRequired() && !submission.identityVerified()) {
            failures.add("IDENTITY_NOT_VERIFIED");
        } else if (!submission.identityVerified()) {
            warnings.add("IDENTITY_NOT_VERIFIED");
        }
        if (submission.proctorDisconnected()) {
            warnings.add("PROCTOR_CONNECTION_LOST");
        }
        if (submission.ipAddressChanged()) {
            warnings.add("IP_ADDRESS_CHANGED");
        }
        if (submission.fullscreenExitCount() == 1) {
            warnings.add("FULLSCREEN_EXIT_RECORDED");
        } else if (submission.fullscreenExitCount() == 2) {
            warnings.add("MULTIPLE_FULLSCREEN_EXITS");
        } else if (submission.fullscreenExitCount() >= 3) {
            failures.add("EXCESSIVE_FULLSCREEN_EXITS");
        }
        if (submission.copyPasteEventCount() > 0 && submission.copyPasteEventCount() < 5) {
            warnings.add("COPY_PASTE_EVENTS_RECORDED");
        } else if (submission.copyPasteEventCount() >= 5) {
            failures.add("EXCESSIVE_COPY_PASTE_EVENTS");
        }
    }

    private boolean sectionRequirementsMet(
            Map<String, Integer> sections,
            ExamSubmission submission,
            GradingPolicy policy
    ) {
        return sections.entrySet().stream().allMatch(entry -> entry.getValue()
                >= submission.sectionPassPercentages()
                .getOrDefault(entry.getKey(), policy.minimumSectionPercentage()));
    }

    private boolean mandatoryRequirementsMet(ExamSubmission submission, GradingPolicy policy) {
        if (!policy.allMandatoryQuestionsRequired()) {
            return true;
        }
        return submission.answers().stream()
                .filter(AnswerResult::mandatory)
                .allMatch(AnswerResult::answered);
    }

    private boolean hasIntegrityFailure(List<String> failures) {
        return failures.stream().anyMatch(failure -> failure.contains("INTEGRITY")
                || failure.contains("IDENTITY")
                || failure.contains("FULLSCREEN")
                || failure.contains("COPY_PASTE")
                || failure.equals("ACCOUNT_SUSPENDED"));
    }

    private String letterGrade(int percentage, boolean passed) {
        if (!passed && percentage < 60) {
            return "F";
        }
        if (percentage >= 95) {
            return "A+";
        }
        if (percentage >= 90) {
            return "A";
        }
        if (percentage >= 85) {
            return "A-";
        }
        if (percentage >= 80) {
            return "B+";
        }
        if (percentage >= 75) {
            return "B";
        }
        if (percentage >= 70) {
            return "C+";
        }
        if (percentage >= 65) {
            return "C";
        }
        if (percentage >= 60) {
            return "D";
        }
        return "F";
    }

    private void validate(ExamSubmission submission, GradingPolicy policy) {
        Objects.requireNonNull(submission, "submission must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        if (submission.examId() == null || submission.examId().isBlank()) {
            throw new IllegalArgumentException("examId must not be blank");
        }
        if (submission.studentId() == null || submission.studentId().isBlank()) {
            throw new IllegalArgumentException("studentId must not be blank");
        }
        if (submission.startedAt() == null || submission.submittedAt() == null) {
            throw new IllegalArgumentException("exam timestamps must not be null");
        }
        if (submission.submittedAt().isBefore(submission.startedAt())) {
            throw new IllegalArgumentException("submittedAt must not be before startedAt");
        }
        if (submission.allowedDurationSeconds() < 0) {
            throw new IllegalArgumentException("allowedDurationSeconds must not be negative");
        }
        if (submission.attemptNumber() <= 0) {
            throw new IllegalArgumentException("attemptNumber must be positive");
        }
        if (submission.fullscreenExitCount() < 0 || submission.copyPasteEventCount() < 0) {
            throw new IllegalArgumentException("integrity event counts must not be negative");
        }
        if (submission.bonusPoints() == null || submission.bonusPoints().signum() < 0) {
            throw new IllegalArgumentException("bonusPoints must not be negative");
        }
        if (submission.instructorOverridePercent() != null
                && (submission.instructorOverridePercent() < 0
                || submission.instructorOverridePercent() > 100)) {
            throw new IllegalArgumentException("override percentage must be between 0 and 100");
        }
        validatePolicy(policy);
        Set<String> ids = new HashSet<>();
        for (AnswerResult answer : submission.answers()) {
            validateAnswer(answer);
            if (!ids.add(answer.questionId())) {
                throw new IllegalArgumentException("questionId must be unique");
            }
        }
        for (Map.Entry<String, Integer> entry : submission.sectionPassPercentages().entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("section name must not be blank");
            }
            if (entry.getValue() < 0 || entry.getValue() > 100) {
                throw new IllegalArgumentException("section percentage must be between 0 and 100");
            }
        }
    }

    private void validatePolicy(GradingPolicy policy) {
        if (policy.passPercentage() < 0 || policy.passPercentage() > 100) {
            throw new IllegalArgumentException("passPercentage must be between 0 and 100");
        }
        if (policy.wrongAnswerPenaltyRate() == null
                || policy.wrongAnswerPenaltyRate().signum() < 0
                || policy.wrongAnswerPenaltyRate().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("wrongAnswerPenaltyRate must be between 0 and 1");
        }
        if (policy.unansweredPenaltyRate() == null
                || policy.unansweredPenaltyRate().signum() < 0
                || policy.unansweredPenaltyRate().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("unansweredPenaltyRate must be between 0 and 1");
        }
        if (policy.latePenaltyPercentPerHour() < 0
                || policy.maximumLatePenaltyPercent() < 0
                || policy.maximumLatePenaltyPercent() > 100) {
            throw new IllegalArgumentException("late penalties must be between 0 and 100");
        }
        if (policy.minimumSectionPercentage() < 0 || policy.minimumSectionPercentage() > 100) {
            throw new IllegalArgumentException("minimumSectionPercentage must be between 0 and 100");
        }
        if (policy.maximumAttempts() <= 0) {
            throw new IllegalArgumentException("maximumAttempts must be positive");
        }
    }

    private void validateAnswer(AnswerResult answer) {
        Objects.requireNonNull(answer, "answer must not be null");
        if (answer.questionId() == null || answer.questionId().isBlank()) {
            throw new IllegalArgumentException("questionId must not be blank");
        }
        if (answer.type() == null) {
            throw new IllegalArgumentException("question type must not be null");
        }
        if (answer.section() == null || answer.section().isBlank()) {
            throw new IllegalArgumentException("section must not be blank");
        }
        if (answer.maximumPoints() == null || answer.maximumPoints().signum() <= 0) {
            throw new IllegalArgumentException("maximumPoints must be positive");
        }
        if (answer.correctOptionCount() < 0
                || answer.selectedCorrectOptions() < 0
                || answer.selectedIncorrectOptions() < 0) {
            throw new IllegalArgumentException("option counts must not be negative");
        }
        if (answer.selectedCorrectOptions() > answer.correctOptionCount()) {
            throw new IllegalArgumentException("selected correct options exceed available options");
        }
        if (answer.numericTolerance() != null && answer.numericTolerance().signum() < 0) {
            throw new IllegalArgumentException("numericTolerance must not be negative");
        }
        if (answer.rubricPercent() < 0 || answer.rubricPercent() > 100) {
            throw new IllegalArgumentException("rubricPercent must be between 0 and 100");
        }
        if (answer.passedTests() < 0 || answer.totalTests() < 0
                || answer.passedTests() > answer.totalTests()) {
            throw new IllegalArgumentException("program test counts are invalid");
        }
        if (answer.type() == QuestionType.PROGRAMMING && answer.answered()
                && answer.compilationSucceeded() && answer.totalTests() == 0
                && !answer.manualReviewRequired()) {
            throw new IllegalArgumentException("compiled program requires tests or manual review");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
