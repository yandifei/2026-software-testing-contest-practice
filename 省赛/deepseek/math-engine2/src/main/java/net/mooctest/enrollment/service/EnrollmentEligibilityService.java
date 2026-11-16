package net.mooctest.enrollment.service;

import net.mooctest.enrollment.domain.CourseOffering;
import net.mooctest.enrollment.domain.EnrollmentCourseStatus;
import net.mooctest.enrollment.domain.EnrollmentDecision;
import net.mooctest.enrollment.domain.EnrollmentOutcome;
import net.mooctest.enrollment.domain.EnrollmentRequest;
import net.mooctest.enrollment.domain.StudentProfile;
import net.mooctest.enrollment.port.EnrollmentHistory;
import net.mooctest.pricing.domain.MembershipLevel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class EnrollmentEligibilityService {

    private final EnrollmentHistory history;
    private final Clock clock;

    public EnrollmentEligibilityService(EnrollmentHistory history, Clock clock) {
        this.history = history;
        this.clock = clock;
    }

    public EnrollmentDecision evaluate(EnrollmentRequest request) {
        validateStructure(request);
        StudentProfile student = request.student();
        CourseOffering course = request.course();
        Instant now = Instant.now(clock);
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean manualReview = false;

        validateStudentState(student, request, blockers, warnings);
        validateCourseState(course, request, now, blockers, warnings);
        validateDemographics(student, course, blockers, warnings);
        validateOrganization(student, course, request, blockers, warnings);

        if (student.instructor() && student.studentId().equals(course.instructorId())) {
            blockers.add("INSTRUCTOR_CANNOT_ENROLL_IN_OWN_COURSE");
        }
        if (history.alreadyEnrolled(student.studentId(), course.courseId())) {
            blockers.add("ALREADY_ENROLLED");
        }
        if (history.hasRefundAbuseFlag(student.studentId())) {
            manualReview = true;
            warnings.add("REFUND_ABUSE_REVIEW");
        }
        if (student.riskScore() >= 90) {
            blockers.add("RISK_SCORE_TOO_HIGH");
        } else if (student.riskScore() >= 70) {
            manualReview = true;
            warnings.add("HIGH_RISK_MANUAL_REVIEW");
        } else if (student.riskScore() >= 50) {
            warnings.add("ELEVATED_RISK_MONITORING");
        }

        int failedAttempts = history.failedEnrollmentAttempts(student.studentId(), course.courseId());
        if (failedAttempts < 0) {
            throw new IllegalStateException("failed attempt count must not be negative");
        }
        if (course.maximumFailedAttempts() > 0 && failedAttempts >= course.maximumFailedAttempts()) {
            blockers.add("TOO_MANY_FAILED_ATTEMPTS");
        } else if (failedAttempts > 0) {
            warnings.add("PREVIOUS_FAILED_ATTEMPTS");
        }

        evaluatePrerequisites(student, course, blockers, warnings);

        if (course.liveCourse()) {
            if (course.scheduleKey() == null || course.scheduleKey().isBlank()) {
                blockers.add("LIVE_COURSE_WITHOUT_SCHEDULE");
            } else if (history.hasScheduleConflict(student.studentId(), course.scheduleKey())) {
                if (request.auditMode()) {
                    warnings.add("SCHEDULE_CONFLICT_ALLOWED_IN_AUDIT_MODE");
                } else {
                    blockers.add("SCHEDULE_CONFLICT");
                }
            }
        } else if (course.scheduleKey() != null && !course.scheduleKey().isBlank()) {
            warnings.add("SELF_PACED_COURSE_IGNORES_SCHEDULE");
        }

        int activeCourses = history.activeCourseCountThisWeek(student.studentId());
        if (activeCourses < 0) {
            throw new IllegalStateException("active course count must not be negative");
        }
        if (course.maximumWeeklyCourses() > 0 && activeCourses >= course.maximumWeeklyCourses()) {
            if (student.membershipLevel() == MembershipLevel.PREMIUM && activeCourses == course.maximumWeeklyCourses()) {
                warnings.add("PREMIUM_WEEKLY_LIMIT_GRACE");
            } else if (student.membershipLevel() == MembershipLevel.ENTERPRISE && request.organizationSponsored()) {
                warnings.add("ENTERPRISE_WEEKLY_LIMIT_OVERRIDE");
            } else {
                blockers.add("WEEKLY_COURSE_LIMIT_REACHED");
            }
        }

        boolean paymentRequired = requiresPayment(request);
        if (paymentRequired && !request.paymentAuthorized()) {
            blockers.add("PAYMENT_NOT_AUTHORIZED");
        }
        if (!paymentRequired && request.paymentAuthorized()) {
            warnings.add("PAYMENT_AUTHORIZATION_NOT_REQUIRED");
        }
        if (request.scholarshipApproved() && request.organizationSponsored()) {
            warnings.add("SCHOLARSHIP_AND_SPONSORSHIP_BOTH_PRESENT");
        }

        int availableSeats = Math.max(0, course.capacity() - course.enrolledCount());
        boolean capacityAvailable = evaluateCapacity(request, availableSeats, blockers, warnings);

        if (!blockers.isEmpty()) {
            return new EnrollmentDecision(
                    EnrollmentOutcome.REJECTED,
                    paymentRequired,
                    availableSeats,
                    blockers,
                    warnings
            );
        }

        if (!capacityAvailable) {
            if (!course.waitlistEnabled()) {
                return rejected(paymentRequired, availableSeats, "COURSE_FULL", warnings);
            }
            if (!request.acceptWaitlist()) {
                return rejected(paymentRequired, availableSeats, "WAITLIST_NOT_ACCEPTED", warnings);
            }
            if (course.waitlistCapacity() <= 0) {
                return rejected(paymentRequired, availableSeats, "WAITLIST_DISABLED_BY_CAPACITY", warnings);
            }
            if (course.waitlistedCount() >= course.waitlistCapacity()) {
                return rejected(paymentRequired, availableSeats, "WAITLIST_FULL", warnings);
            }
            if (request.requestedSeats() > 1) {
                return rejected(paymentRequired, availableSeats, "GROUP_ENROLLMENT_CANNOT_WAITLIST", warnings);
            }
            if (manualReview) {
                warnings.add("WAITLIST_AFTER_MANUAL_REVIEW");
                return new EnrollmentDecision(
                        EnrollmentOutcome.MANUAL_REVIEW,
                        paymentRequired,
                        availableSeats,
                        List.of(),
                        warnings
                );
            }
            warnings.add("PLACED_ON_WAITLIST");
            return new EnrollmentDecision(
                    EnrollmentOutcome.WAITLISTED,
                    false,
                    availableSeats,
                    List.of(),
                    warnings
            );
        }

        if (manualReview) {
            return new EnrollmentDecision(
                    EnrollmentOutcome.MANUAL_REVIEW,
                    paymentRequired,
                    availableSeats,
                    List.of(),
                    warnings
            );
        }

        return new EnrollmentDecision(
                EnrollmentOutcome.ENROLLED,
                paymentRequired,
                availableSeats,
                List.of(),
                warnings
        );
    }

    private void validateStructure(EnrollmentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(request.student(), "student must not be null");
        Objects.requireNonNull(request.course(), "course must not be null");
        if (request.student().studentId() == null || request.student().studentId().isBlank()) {
            throw new IllegalArgumentException("studentId must not be blank");
        }
        if (request.course().courseId() == null || request.course().courseId().isBlank()) {
            throw new IllegalArgumentException("courseId must not be blank");
        }
        if (request.requestedSeats() < 1) {
            throw new IllegalArgumentException("requestedSeats must be positive");
        }
        if (request.requestedSeats() > 50) {
            throw new IllegalArgumentException("requestedSeats must not exceed 50");
        }
        if (request.student().riskScore() < 0 || request.student().riskScore() > 100) {
            throw new IllegalArgumentException("riskScore must be between 0 and 100");
        }
    }

    private void validateStudentState(
            StudentProfile student,
            EnrollmentRequest request,
            List<String> blockers,
            List<String> warnings
    ) {
        if (!student.accountActive()) {
            blockers.add("ACCOUNT_INACTIVE");
        }
        if (student.suspended()) {
            blockers.add("ACCOUNT_SUSPENDED");
        }
        if (!student.emailVerified()) {
            blockers.add("EMAIL_NOT_VERIFIED");
        }
        if (!student.identityVerified() && request.course().liveCourse()) {
            blockers.add("IDENTITY_REQUIRED_FOR_LIVE_COURSE");
        } else if (!student.identityVerified()) {
            warnings.add("IDENTITY_NOT_VERIFIED");
        }
        if (!student.profileComplete()) {
            blockers.add("PROFILE_INCOMPLETE");
        }
        if (!student.termsAccepted()) {
            blockers.add("TERMS_NOT_ACCEPTED");
        }
        if (student.age() < 18 && !student.parentalConsent()) {
            blockers.add("PARENTAL_CONSENT_REQUIRED");
        }
        if (student.age() >= 18 && student.parentalConsent()) {
            warnings.add("UNNECESSARY_PARENTAL_CONSENT");
        }
    }

    private void validateCourseState(
            CourseOffering course,
            EnrollmentRequest request,
            Instant now,
            List<String> blockers,
            List<String> warnings
    ) {
        if (course.status() == null) {
            blockers.add("COURSE_STATUS_MISSING");
        } else if (course.status() == EnrollmentCourseStatus.DRAFT) {
            blockers.add("COURSE_NOT_PUBLISHED");
        } else if (course.status() == EnrollmentCourseStatus.CLOSED) {
            blockers.add("COURSE_CLOSED");
        } else if (course.status() == EnrollmentCourseStatus.ARCHIVED && !request.auditMode()) {
            blockers.add("COURSE_ARCHIVED");
        } else if (course.status() == EnrollmentCourseStatus.ARCHIVED) {
            warnings.add("ARCHIVED_COURSE_AUDIT_ONLY");
        }
        if (course.enrollmentOpensAt() != null && course.enrollmentClosesAt() != null
                && course.enrollmentOpensAt().isAfter(course.enrollmentClosesAt())) {
            throw new IllegalArgumentException("enrollment window is invalid");
        }
        if (course.enrollmentOpensAt() != null && now.isBefore(course.enrollmentOpensAt())) {
            blockers.add("ENROLLMENT_NOT_OPEN");
        }
        if (course.enrollmentClosesAt() != null && now.isAfter(course.enrollmentClosesAt())) {
            blockers.add("ENROLLMENT_CLOSED");
        }
        if (course.startsAt() != null && now.isAfter(course.startsAt())) {
            if (request.auditMode()) {
                warnings.add("LATE_AUDIT_ENROLLMENT");
            } else {
                blockers.add("COURSE_ALREADY_STARTED");
            }
        }
        if (course.capacity() < 0 || course.enrolledCount() < 0) {
            throw new IllegalArgumentException("course capacity values must not be negative");
        }
        if (course.enrolledCount() > course.capacity()) {
            warnings.add("COURSE_IS_OVERBOOKED");
        }
    }

    private void validateDemographics(
            StudentProfile student,
            CourseOffering course,
            List<String> blockers,
            List<String> warnings
    ) {
        if (student.age() < 0 || student.age() > 120) {
            blockers.add("INVALID_STUDENT_AGE");
        }
        if (course.minimumAge() > 0 && student.age() < course.minimumAge()) {
            blockers.add("BELOW_MINIMUM_AGE");
        }
        if (course.maximumAge() > 0 && student.age() > course.maximumAge()) {
            blockers.add("ABOVE_MAXIMUM_AGE");
        }
        if (course.minimumAge() > 0 && course.maximumAge() > 0
                && course.minimumAge() > course.maximumAge()) {
            throw new IllegalArgumentException("course age range is invalid");
        }
        if (student.countryCode() == null || student.countryCode().isBlank()) {
            blockers.add("COUNTRY_MISSING");
        } else if (!course.allowedCountries().isEmpty()
                && !course.allowedCountries().contains(student.countryCode().toUpperCase(Locale.ROOT))) {
            blockers.add("COUNTRY_NOT_ALLOWED");
        }
        if (course.requiredLanguage() != null && !course.requiredLanguage().isBlank()) {
            if (student.preferredLanguage() == null || student.preferredLanguage().isBlank()) {
                blockers.add("LANGUAGE_PROFILE_MISSING");
            } else if (!course.requiredLanguage().equalsIgnoreCase(student.preferredLanguage())) {
                blockers.add("LANGUAGE_REQUIREMENT_NOT_MET");
            }
        } else if (student.preferredLanguage() == null || student.preferredLanguage().isBlank()) {
            warnings.add("PREFERRED_LANGUAGE_MISSING");
        }
    }

    private void validateOrganization(
            StudentProfile student,
            CourseOffering course,
            EnrollmentRequest request,
            List<String> blockers,
            List<String> warnings
    ) {
        if (course.organizationOnly() && !request.organizationSponsored()) {
            blockers.add("ORGANIZATION_SPONSOR_REQUIRED");
        }
        if (course.organizationOnly()
                && (request.organizationId() == null || request.organizationId().isBlank())) {
            blockers.add("ORGANIZATION_ID_REQUIRED");
        }
        if (course.requiredOrganizationId() != null
                && !course.requiredOrganizationId().isBlank()
                && !course.requiredOrganizationId().equals(request.organizationId())) {
            blockers.add("ORGANIZATION_NOT_ALLOWED");
        }
        if (!course.organizationOnly() && request.organizationSponsored()) {
            warnings.add("OPTIONAL_ORGANIZATION_SPONSORSHIP");
        }
        if (course.subscriptionRequired() && !student.activeSubscription()) {
            if (request.organizationSponsored()) {
                warnings.add("SUBSCRIPTION_COVERED_BY_ORGANIZATION");
            } else if (request.scholarshipApproved()) {
                warnings.add("SUBSCRIPTION_COVERED_BY_SCHOLARSHIP");
            } else {
                blockers.add("ACTIVE_SUBSCRIPTION_REQUIRED");
            }
        }
        if (request.requestedSeats() > 1
                && student.membershipLevel() != MembershipLevel.ENTERPRISE
                && !request.organizationSponsored()) {
            blockers.add("GROUP_ENROLLMENT_REQUIRES_ENTERPRISE");
        }
    }

    private void evaluatePrerequisites(
            StudentProfile student,
            CourseOffering course,
            List<String> blockers,
            List<String> warnings
    ) {
        if (course.prerequisiteCourseIds().isEmpty()) {
            return;
        }
        int completed = 0;
        for (String prerequisite : course.prerequisiteCourseIds()) {
            if (prerequisite == null || prerequisite.isBlank()) {
                warnings.add("INVALID_PREREQUISITE_CONFIGURATION");
                continue;
            }
            if (!history.completedCourse(student.studentId(), prerequisite)) {
                blockers.add("MISSING_PREREQUISITE:" + prerequisite);
                continue;
            }
            completed++;
            int score = history.completionScore(student.studentId(), prerequisite);
            if (score < 0 || score > 100) {
                blockers.add("INVALID_PREREQUISITE_SCORE:" + prerequisite);
            } else if (course.minimumPrerequisiteScore() > 0
                    && score < course.minimumPrerequisiteScore()) {
                blockers.add("PREREQUISITE_SCORE_TOO_LOW:" + prerequisite);
            } else if (score >= 95) {
                warnings.add("EXCELLENT_PREREQUISITE_SCORE:" + prerequisite);
            }
        }
        if (completed == course.prerequisiteCourseIds().size()) {
            warnings.add("ALL_PREREQUISITES_COMPLETED");
        }
    }

    private boolean requiresPayment(EnrollmentRequest request) {
        BigDecimal price = request.course().price();
        if (price == null || price.signum() <= 0) {
            return false;
        }
        if (request.auditMode()) {
            return false;
        }
        if (request.scholarshipApproved()) {
            return false;
        }
        return !request.organizationSponsored();
    }

    private boolean evaluateCapacity(
            EnrollmentRequest request,
            int availableSeats,
            List<String> blockers,
            List<String> warnings
    ) {
        CourseOffering course = request.course();
        if (request.accessibilitySeatRequired()) {
            if (course.accessibilitySeatCapacity() <= 0) {
                blockers.add("NO_ACCESSIBILITY_SEATS");
            } else if (course.accessibilitySeatsUsed() >= course.accessibilitySeatCapacity()) {
                blockers.add("ACCESSIBILITY_SEATS_FULL");
            } else {
                warnings.add("ACCESSIBILITY_SEAT_RESERVED");
            }
        } else if (course.accessibilitySeatsUsed() > course.accessibilitySeatCapacity()) {
            warnings.add("ACCESSIBILITY_CAPACITY_CONFIGURATION_INVALID");
        }
        if (request.requestedSeats() <= availableSeats) {
            if (availableSeats - request.requestedSeats() <= 2) {
                warnings.add("COURSE_ALMOST_FULL");
            }
            return true;
        }
        return false;
    }

    private EnrollmentDecision rejected(
            boolean paymentRequired,
            int availableSeats,
            String reason,
            List<String> warnings
    ) {
        return new EnrollmentDecision(
                EnrollmentOutcome.REJECTED,
                paymentRequired,
                availableSeats,
                List.of(reason),
                warnings
        );
    }
}
