package net.mooctest.pricing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record Coupon(
        String code,
        CouponType type,
        BigDecimal value,
        BigDecimal minimumSubtotal,
        LocalDate validFrom,
        LocalDate validUntil,
        int remainingUses,
        boolean stackable,
        Set<String> eligibleCourseIds,
        MembershipLevel minimumMembership,
        int maximumQuantity
) {

    public Coupon {
        eligibleCourseIds = eligibleCourseIds == null ? Set.of() : Set.copyOf(eligibleCourseIds);
    }
}
