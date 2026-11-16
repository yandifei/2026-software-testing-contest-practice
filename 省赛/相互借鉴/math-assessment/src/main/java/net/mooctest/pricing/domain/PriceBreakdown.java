package net.mooctest.pricing.domain;

import java.math.BigDecimal;
import java.util.List;

public record PriceBreakdown(
        BigDecimal subtotal,
        BigDecimal membershipDiscount,
        BigDecimal bulkDiscount,
        BigDecimal promotionDiscount,
        BigDecimal couponDiscount,
        BigDecimal firstPurchaseDiscount,
        BigDecimal nonprofitDiscount,
        BigDecimal loyaltyDiscount,
        BigDecimal tax,
        BigDecimal total,
        List<String> appliedRules,
        List<String> warnings
) {

    public PriceBreakdown {
        appliedRules = List.copyOf(appliedRules);
        warnings = List.copyOf(warnings);
    }
}
