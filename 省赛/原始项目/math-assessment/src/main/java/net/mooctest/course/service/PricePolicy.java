package net.mooctest.course.service;

import net.mooctest.error.BusinessException;
import net.mooctest.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PricePolicy {

    private static final BigDecimal MAX_PRICE = new BigDecimal("99999.99");
    private static final int MAX_DISCOUNT_PERCENT = 80;

    public BigDecimal normalizePrice(BigDecimal price) {
        if (price == null || price.signum() < 0 || price.compareTo(MAX_PRICE) > 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_PRICE,
                    "Price must be between 0 and 99999.99"
            );
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal discountedPrice(BigDecimal originalPrice, int discountPercent) {
        BigDecimal normalizedPrice = normalizePrice(originalPrice);
        if (discountPercent < 0 || discountPercent > MAX_DISCOUNT_PERCENT) {
            throw new BusinessException(
                    ErrorCode.INVALID_DISCOUNT,
                    "Discount percent must be between 0 and 80"
            );
        }

        BigDecimal multiplier = BigDecimal.valueOf(100L - discountPercent)
                .movePointLeft(2);
        return normalizedPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
