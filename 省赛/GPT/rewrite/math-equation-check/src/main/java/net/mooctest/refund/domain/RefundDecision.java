package net.mooctest.refund.domain;

import java.math.BigDecimal;
import java.util.List;

public record RefundDecision(
        boolean approved,
        boolean manualReview,
        BigDecimal refundableAmount,
        BigDecimal processingFee,
        int loyaltyPointsReturned,
        String refundRoute,
        List<String> reasons,
        List<String> warnings
) {

    public RefundDecision {
        reasons = List.copyOf(reasons);
        warnings = List.copyOf(warnings);
    }
}
