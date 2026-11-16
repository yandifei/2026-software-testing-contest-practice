package net.mooctest.pricing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Promotion(
        String name,
        PromotionType type,
        BigDecimal value,
        LocalDate startsOn,
        LocalDate endsOn,
        boolean membersOnly,
        int minimumQuantity,
        BigDecimal maximumDiscount
) {
}
