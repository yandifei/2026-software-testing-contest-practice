package net.mooctest.progress.service;

import net.mooctest.progress.domain.CourseProgressSnapshot;
import net.mooctest.progress.domain.LessonProgress;
import net.mooctest.progress.domain.LessonType;
import net.mooctest.progress.domain.ProgressEvaluation;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class LearningProgressService {

    private final Clock clock;

    public LearningProgressService(Clock clock) {
        this.clock = clock;
    }

    public ProgressEvaluation evaluate(CourseProgressSnapshot snapshot) {
        validate(snapshot);
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<LessonProgress> lessons = snapshot.lessons().stream()
                .sorted(Comparator.comparingInt(LessonProgress::order))
                .toList();

        evaluateAccountAndAccess(snapshot, blockers, warnings);
        int earnedWeight = 0;
        int totalWeight = 0;
        int completedLessons = 0;
        boolean previousComplete = true;
        String nextLessonId = null;
        boolean mandatorySatisfied = true;

        for (LessonProgress lesson : lessons) {
            LessonAssessment assessment = assessLesson(lesson, snapshot.requiredQuizScore(), warnings);
            totalWeight += assessment.weight();
            earnedWeight += assessment.earnedWeight();

            if (assessment.complete()) {
                completedLessons++;
            }
            if (lesson.mandatory() && !assessment.complete()) {
                mandatorySatisfied = false;
                blockers.add("MANDATORY_LESSON_INCOMPLETE:" + lesson.lessonId());
            }
            if (lesson.plagiarismFlag()) {
                blockers.add("PLAGIARISM_REVIEW:" + lesson.lessonId());
            }
            if (lesson.maximumAttempts() > 0 && lesson.attempts() >= lesson.maximumAttempts()
                    && !assessment.complete()) {
                blockers.add("ATTEMPT_LIMIT_REACHED:" + lesson.lessonId());
            }
            if (!lesson.unlocked() && lesson.completionPercent() > 0) {
                warnings.add("LOCKED_LESSON_HAS_PROGRESS:" + lesson.lessonId());
            }
            if (snapshot.sequentialLearning() && !previousComplete && lesson.unlocked()) {
                warnings.add("SEQUENCE_RULE_BYPASSED:" + lesson.lessonId());
            }
            if (nextLessonId == null && !assessment.complete() && lesson.unlocked()
                    && (!snapshot.sequentialLearning() || previousComplete)) {
                nextLessonId = lesson.lessonId();
            }
            if (snapshot.sequentialLearning() && lesson.mandatory()) {
                previousComplete = assessment.complete();
            } else if (snapshot.sequentialLearning() && !assessment.complete()) {
                previousComplete = previousComplete && lesson.completionPercent() >= 50;
            }
        }

        int overallPercent = totalWeight == 0
                ? 0
                : Math.min(100, Math.max(0, earnedWeight * 100 / totalWeight));
        if (lessons.isEmpty()) {
            blockers.add("COURSE_HAS_NO_LESSONS");
        }
        if (overallPercent < snapshot.requiredOverallPercent()) {
            blockers.add("OVERALL_PROGRESS_BELOW_REQUIRED");
        }
        if (snapshot.finalProjectRequired() && !hasApprovedProject(lessons)) {
            blockers.add("FINAL_PROJECT_NOT_APPROVED");
        }
        if (snapshot.instructorApprovalRequired() && !hasInstructorApproval(lessons)) {
            blockers.add("INSTRUCTOR_APPROVAL_REQUIRED");
        }

        boolean completed = !lessons.isEmpty()
                && overallPercent >= snapshot.requiredOverallPercent()
                && mandatorySatisfied
                && noAcademicBlocker(blockers);
        boolean certificateEligible = evaluateCertificate(snapshot, completed, blockers, warnings);

        if (completed && nextLessonId != null) {
            warnings.add("COMPLETION_STATE_CONFLICT");
        }
        if (!completed && nextLessonId == null && !lessons.isEmpty()) {
            warnings.add("NO_ACCESSIBLE_NEXT_LESSON");
        }

        return new ProgressEvaluation(
                overallPercent,
                completedLessons,
                lessons.size(),
                completed,
                certificateEligible,
                nextLessonId,
                blockers,
                warnings
        );
    }

    private LessonAssessment assessLesson(
            LessonProgress lesson,
            int requiredQuizScore,
            List<String> warnings
    ) {
        int weight = switch (lesson.type()) {
            case VIDEO -> 15;
            case ARTICLE -> 10;
            case QUIZ -> 20;
            case ASSIGNMENT -> 25;
            case LIVE_SESSION -> 20;
            case PROJECT -> 30;
        };

        boolean complete;
        int earned;
        switch (lesson.type()) {
            case VIDEO -> {
                complete = lesson.completionPercent() >= 90;
                earned = weight * Math.min(100, lesson.completionPercent()) / 100;
                if (lesson.completionPercent() >= 70 && lesson.completionPercent() < 90) {
                    warnings.add("VIDEO_NEAR_COMPLETION:" + lesson.lessonId());
                }
            }
            case ARTICLE -> {
                complete = lesson.completionPercent() >= 100;
                earned = complete ? weight : weight * lesson.completionPercent() / 100;
            }
            case QUIZ -> {
                complete = lesson.submitted() && lesson.score() >= requiredQuizScore;
                earned = lesson.submitted() ? weight * lesson.score() / 100 : 0;
                if (lesson.submitted() && lesson.score() < requiredQuizScore) {
                    warnings.add("QUIZ_SCORE_TOO_LOW:" + lesson.lessonId());
                }
            }
            case ASSIGNMENT -> {
                complete = lesson.submitted() && lesson.approved();
                earned = !lesson.submitted() ? 0 : lesson.approved() ? weight : weight / 2;
                if (lesson.submitted() && !lesson.approved()) {
                    warnings.add("ASSIGNMENT_AWAITING_REVISION:" + lesson.lessonId());
                }
            }
            case LIVE_SESSION -> {
                complete = lesson.requiredMinutes() == 0
                        ? lesson.attendedMinutes() > 0
                        : lesson.attendedMinutes() * 100 / lesson.requiredMinutes() >= 80;
                earned = lesson.requiredMinutes() == 0
                        ? (lesson.attendedMinutes() > 0 ? weight : 0)
                        : weight * Math.min(lesson.attendedMinutes(), lesson.requiredMinutes())
                        / lesson.requiredMinutes();
                if (!complete && lesson.attendedMinutes() > 0) {
                    warnings.add("LIVE_ATTENDANCE_TOO_SHORT:" + lesson.lessonId());
                }
            }
            case PROJECT -> {
                complete = lesson.submitted() && lesson.approved() && !lesson.plagiarismFlag();
                earned = complete ? weight : lesson.submitted() ? weight / 3 : 0;
                if (lesson.submitted() && lesson.plagiarismFlag()) {
                    warnings.add("PROJECT_HELD_FOR_INTEGRITY_REVIEW:" + lesson.lessonId());
                }
            }
            default -> throw new IllegalStateException("Unsupported lesson type: " + lesson.type());
        }
        earned = Math.max(0, Math.min(weight, earned));
        return new LessonAssessment(weight, earned, complete);
    }

    private void evaluateAccountAndAccess(
            CourseProgressSnapshot snapshot,
            List<String> blockers,
            List<String> warnings
    ) {
        Instant now = Instant.now(clock);
        if (!snapshot.coursePublished()) {
            blockers.add("COURSE_NOT_PUBLISHED");
        }
        if (snapshot.accountSuspended()) {
            blockers.add("ACCOUNT_SUSPENDED");
        }
        if (!snapshot.subscriptionActive()) {
            blockers.add("SUBSCRIPTION_INACTIVE");
        }
        if (!snapshot.codeOfConductAccepted()) {
            blockers.add("CODE_OF_CONDUCT_NOT_ACCEPTED");
        }
        if (snapshot.enrolledAt() != null && snapshot.enrolledAt().isAfter(now)) {
            blockers.add("ENROLLMENT_DATE_IN_FUTURE");
        }
        if (snapshot.accessExpiresAt() != null && now.isAfter(snapshot.accessExpiresAt())) {
            blockers.add("COURSE_ACCESS_EXPIRED");
        } else if (snapshot.accessExpiresAt() != null
                && Duration.between(now, snapshot.accessExpiresAt()).toDays() <= 7) {
            warnings.add("COURSE_ACCESS_EXPIRES_SOON");
        }
        if (!snapshot.identityVerified()) {
            warnings.add("IDENTITY_NOT_VERIFIED");
        }
        if (snapshot.outstandingBalance()) {
            warnings.add("OUTSTANDING_BALANCE");
        }
    }

    private boolean evaluateCertificate(
            CourseProgressSnapshot snapshot,
            boolean completed,
            List<String> blockers,
            List<String> warnings
    ) {
        if (!snapshot.certificateEnabled()) {
            warnings.add("CERTIFICATE_DISABLED");
            return false;
        }
        if (!completed) {
            return false;
        }
        if (!snapshot.identityVerified()) {
            blockers.add("CERTIFICATE_REQUIRES_IDENTITY_VERIFICATION");
            return false;
        }
        if (snapshot.outstandingBalance()) {
            blockers.add("CERTIFICATE_BLOCKED_BY_BALANCE");
            return false;
        }
        if (snapshot.certificateName() == null || snapshot.certificateName().isBlank()) {
            blockers.add("CERTIFICATE_NAME_REQUIRED");
            return false;
        }
        if (snapshot.certificateName().length() < 2 || snapshot.certificateName().length() > 80) {
            blockers.add("CERTIFICATE_NAME_LENGTH_INVALID");
            return false;
        }
        return true;
    }

    private boolean hasApprovedProject(List<LessonProgress> lessons) {
        return lessons.stream().anyMatch(lesson -> lesson.type() == LessonType.PROJECT
                && lesson.submitted()
                && lesson.approved()
                && !lesson.plagiarismFlag());
    }

    private boolean hasInstructorApproval(List<LessonProgress> lessons) {
        return lessons.stream().filter(LessonProgress::mandatory)
                .allMatch(lesson -> lesson.type() != LessonType.ASSIGNMENT || lesson.approved());
    }

    private boolean noAcademicBlocker(List<String> blockers) {
        return blockers.stream().noneMatch(blocker -> blocker.startsWith("PLAGIARISM_REVIEW")
                || blocker.startsWith("ATTEMPT_LIMIT_REACHED")
                || blocker.equals("FINAL_PROJECT_NOT_APPROVED")
                || blocker.equals("INSTRUCTOR_APPROVAL_REQUIRED"));
    }

    private void validate(CourseProgressSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (snapshot.studentId() == null || snapshot.studentId().isBlank()) {
            throw new IllegalArgumentException("studentId must not be blank");
        }
        if (snapshot.courseId() == null || snapshot.courseId().isBlank()) {
            throw new IllegalArgumentException("courseId must not be blank");
        }
        if (snapshot.requiredOverallPercent() < 0 || snapshot.requiredOverallPercent() > 100) {
            throw new IllegalArgumentException("requiredOverallPercent must be between 0 and 100");
        }
        if (snapshot.requiredQuizScore() < 0 || snapshot.requiredQuizScore() > 100) {
            throw new IllegalArgumentException("requiredQuizScore must be between 0 and 100");
        }
        Set<String> ids = new HashSet<>();
        Set<Integer> orders = new HashSet<>();
        for (LessonProgress lesson : snapshot.lessons()) {
            Objects.requireNonNull(lesson, "lesson must not be null");
            if (lesson.lessonId() == null || lesson.lessonId().isBlank()) {
                throw new IllegalArgumentException("lessonId must not be blank");
            }
            if (!ids.add(lesson.lessonId())) {
                throw new IllegalArgumentException("lessonId must be unique");
            }
            if (!orders.add(lesson.order())) {
                throw new IllegalArgumentException("lesson order must be unique");
            }
            if (lesson.type() == null) {
                throw new IllegalArgumentException("lesson type must not be null");
            }
            if (lesson.order() < 0) {
                throw new IllegalArgumentException("lesson order must not be negative");
            }
            if (lesson.completionPercent() < 0 || lesson.completionPercent() > 100) {
                throw new IllegalArgumentException("completionPercent must be between 0 and 100");
            }
            if (lesson.score() < 0 || lesson.score() > 100) {
                throw new IllegalArgumentException("score must be between 0 and 100");
            }
            if (lesson.attempts() < 0 || lesson.maximumAttempts() < 0) {
                throw new IllegalArgumentException("attempt counts must not be negative");
            }
            if (lesson.maximumAttempts() > 0 && lesson.attempts() > lesson.maximumAttempts()) {
                throw new IllegalArgumentException("attempts must not exceed maximumAttempts");
            }
            if (lesson.attendedMinutes() < 0 || lesson.requiredMinutes() < 0) {
                throw new IllegalArgumentException("attendance minutes must not be negative");
            }
        }
    }

    private static final class LessonAssessment {

        private final int weight;
        private final int earnedWeight;
        private final boolean complete;

        private LessonAssessment(int weight, int earnedWeight, boolean complete) {
            this.weight = weight;
            this.earnedWeight = earnedWeight;
            this.complete = complete;
        }

        private int weight() {
            return weight;
        }

        private int earnedWeight() {
            return earnedWeight;
        }

        private boolean complete() {
            return complete;
        }
    }
}
