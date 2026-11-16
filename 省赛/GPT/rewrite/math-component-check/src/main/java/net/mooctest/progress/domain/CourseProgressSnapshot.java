package net.mooctest.progress.domain;

import java.time.Instant;
import java.util.List;

public record CourseProgressSnapshot(
        String studentId,
        String courseId,
        List<LessonProgress> lessons,
        boolean sequentialLearning,
        int requiredOverallPercent,
        int requiredQuizScore,
        boolean identityVerified,
        boolean codeOfConductAccepted,
        boolean subscriptionActive,
        boolean accountSuspended,
        boolean coursePublished,
        Instant enrolledAt,
        Instant accessExpiresAt,
        boolean certificateEnabled,
        String certificateName,
        boolean finalProjectRequired,
        boolean outstandingBalance,
        boolean instructorApprovalRequired
) {
    public CourseProgressSnapshot {
        lessons = lessons == null ? List.of() : List.copyOf(lessons);
    }
}
