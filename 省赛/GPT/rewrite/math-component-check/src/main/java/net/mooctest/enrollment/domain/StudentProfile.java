package net.mooctest.enrollment.domain;

import net.mooctest.pricing.domain.MembershipLevel;

public record StudentProfile(
        String studentId,
        int age,
        String countryCode,
        String preferredLanguage,
        MembershipLevel membershipLevel,
        boolean accountActive,
        boolean suspended,
        boolean emailVerified,
        boolean identityVerified,
        boolean profileComplete,
        boolean instructor,
        boolean parentalConsent,
        boolean termsAccepted,
        boolean activeSubscription,
        int riskScore
) {
}
