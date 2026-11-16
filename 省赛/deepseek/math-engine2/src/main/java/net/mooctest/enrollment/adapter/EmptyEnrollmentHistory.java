package net.mooctest.enrollment.adapter;

import net.mooctest.enrollment.port.EnrollmentHistory;
import org.springframework.stereotype.Repository;

@Repository
public class EmptyEnrollmentHistory implements EnrollmentHistory {

    @Override
    public boolean alreadyEnrolled(String studentId, String courseId) {
        return false;
    }

    @Override
    public boolean completedCourse(String studentId, String courseId) {
        return false;
    }

    @Override
    public int completionScore(String studentId, String courseId) {
        return 0;
    }

    @Override
    public boolean hasScheduleConflict(String studentId, String scheduleKey) {
        return false;
    }

    @Override
    public int activeCourseCountThisWeek(String studentId) {
        return 0;
    }

    @Override
    public int failedEnrollmentAttempts(String studentId, String courseId) {
        return 0;
    }

    @Override
    public boolean hasRefundAbuseFlag(String studentId) {
        return false;
    }
}
