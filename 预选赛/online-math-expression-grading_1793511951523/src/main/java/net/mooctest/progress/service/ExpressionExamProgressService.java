package net.mooctest.progress.service;

import net.mooctest.grading.expression.ExpressionExamGrade;
import net.mooctest.grading.domain.GradeReport;
import net.mooctest.progress.domain.CourseProgressSnapshot;
import net.mooctest.progress.domain.LessonProgress;
import net.mooctest.progress.domain.LessonType;
import net.mooctest.progress.domain.ProgressEvaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Applies a finalized expression exam grade to its quiz lesson. */
public final class ExpressionExamProgressService {
    private final LearningProgressService progressService;

    public ExpressionExamProgressService(LearningProgressService progressService) {
        this.progressService = Objects.requireNonNull(progressService, "progressService must not be null");
    }

    public ProgressEvaluation evaluate(CourseProgressSnapshot snapshot, String quizLessonId, ExpressionExamGrade grade) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(grade, "grade must not be null");
        if (quizLessonId == null || quizLessonId.isBlank()) {
            throw new IllegalArgumentException("quizLessonId must not be blank");
        }
        GradeReport report = grade.report();
        if (report.manualReview()) {
            throw new IllegalArgumentException("exam grade is pending manual review");
        }

        List<LessonProgress> updated = new ArrayList<>(snapshot.lessons().size());
        boolean found = false;
        for (LessonProgress lesson : snapshot.lessons()) {
            if (!quizLessonId.equals(lesson.lessonId())) {
                updated.add(lesson);
                continue;
            }
            if (found) {
                throw new IllegalArgumentException("quizLessonId is duplicated: " + quizLessonId);
            }
            if (lesson.type() != LessonType.QUIZ) {
                throw new IllegalArgumentException("lesson is not a quiz: " + quizLessonId);
            }
            found = true;
            int percentage = Math.max(0, Math.min(100, report.percentage()));
            updated.add(new LessonProgress(
                    lesson.lessonId(), lesson.type(), lesson.mandatory(), lesson.order(),
                    100, percentage, lesson.attempts() + 1, lesson.maximumAttempts(),
                    true, lesson.approved(), lesson.plagiarismFlag()
                            || report.failures().stream().anyMatch(failure -> failure.contains("INTEGRITY")),
                    lesson.attendedMinutes(), lesson.requiredMinutes(), lesson.unlocked()
            ));
        }
        if (!found) {
            throw new IllegalArgumentException("quiz lesson not found: " + quizLessonId);
        }

        CourseProgressSnapshot updatedSnapshot = new CourseProgressSnapshot(
                snapshot.studentId(), snapshot.courseId(), updated, snapshot.sequentialLearning(),
                snapshot.requiredOverallPercent(), snapshot.requiredQuizScore(), snapshot.identityVerified(),
                snapshot.codeOfConductAccepted(), snapshot.subscriptionActive(), snapshot.accountSuspended(),
                snapshot.coursePublished(), snapshot.enrolledAt(), snapshot.accessExpiresAt(),
                snapshot.certificateEnabled(), snapshot.certificateName(), snapshot.finalProjectRequired(),
                snapshot.outstandingBalance(), snapshot.instructorApprovalRequired()
        );
        return progressService.evaluate(updatedSnapshot);
    }
}
