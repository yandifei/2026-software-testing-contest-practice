package net.mooctest.enrollment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record CourseOffering(
        String courseId,
        String instructorId,
        EnrollmentCourseStatus status,
        Instant enrollmentOpensAt,
        Instant enrollmentClosesAt,
        Instant startsAt,
        int capacity,
        int enrolledCount,
        boolean waitlistEnabled,
        int waitlistCapacity,
        int waitlistedCount,
        int minimumAge,
        int maximumAge,
        Set<String> allowedCountries,
        String requiredLanguage,
        Set<String> prerequisiteCourseIds,
        int minimumPrerequisiteScore,
        int maximumWeeklyCourses,
        int maximumFailedAttempts,
        boolean organizationOnly,
        String requiredOrganizationId,
        boolean subscriptionRequired,
        boolean liveCourse,
        String scheduleKey,
        int accessibilitySeatCapacity,
        int accessibilitySeatsUsed,
        BigDecimal price
) {

    public CourseOffering {
        allowedCountries = allowedCountries == null ? Set.of() : Set.copyOf(allowedCountries);
        prerequisiteCourseIds = prerequisiteCourseIds == null ? Set.of() : Set.copyOf(prerequisiteCourseIds);
    }
}
