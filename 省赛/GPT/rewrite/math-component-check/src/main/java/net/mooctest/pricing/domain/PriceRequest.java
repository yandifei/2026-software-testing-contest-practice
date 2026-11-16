package net.mooctest.pricing.domain;

import java.math.BigDecimal;

public record PriceRequest(
        String courseId,
        BigDecimal unitPrice,
        int quantity,
        MembershipLevel membershipLevel,
        Coupon coupon,
        Promotion promotion,
        boolean firstPurchase,
        boolean nonprofitCustomer,
        boolean taxExempt,
        boolean instructorOwnedCourse,
        int loyaltyPointsToUse,
        int availableLoyaltyPoints,
        String countryCode,
        String currency
) {
}
