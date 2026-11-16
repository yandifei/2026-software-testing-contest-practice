package net.mooctest.enrollment.port;

public interface EnrollmentHistory {

    boolean alreadyEnrolled(String studentId, String courseId);

    boolean completedCourse(String studentId, String courseId);

    int completionScore(String studentId, String courseId);

    boolean hasScheduleConflict(String studentId, String scheduleKey);

    int activeCourseCountThisWeek(String studentId);

    int failedEnrollmentAttempts(String studentId, String courseId);

    boolean hasRefundAbuseFlag(String studentId);
}
