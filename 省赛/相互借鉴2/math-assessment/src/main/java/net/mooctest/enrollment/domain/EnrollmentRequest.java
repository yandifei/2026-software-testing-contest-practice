package net.mooctest.enrollment.domain;

public record EnrollmentRequest(
        StudentProfile student,
        CourseOffering course,
        int requestedSeats,
        boolean paymentAuthorized,
        boolean scholarshipApproved,
        boolean accessibilitySeatRequired,
        boolean organizationSponsored,
        String organizationId,
        boolean auditMode,
        boolean acceptWaitlist
) {
}
