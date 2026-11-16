package net.mooctest.progress.domain;

public record LessonProgress(
        String lessonId,
        LessonType type,
        boolean mandatory,
        int order,
        int completionPercent,
        int score,
        int attempts,
        int maximumAttempts,
        boolean submitted,
        boolean approved,
        boolean plagiarismFlag,
        int attendedMinutes,
        int requiredMinutes,
        boolean unlocked
) {
}
