package net.mooctest.pricing.service;

import net.mooctest.error.BusinessException;
import net.mooctest.error.ErrorCode;
import net.mooctest.pricing.domain.Coupon;
import net.mooctest.pricing.domain.CouponType;
import net.mooctest.pricing.domain.MembershipLevel;
import net.mooctest.pricing.domain.PriceBreakdown;
import net.mooctest.pricing.domain.PriceRequest;
import net.mooctest.pricing.domain.Promotion;
import net.mooctest.pricing.domain.PromotionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class AdvancedPricingService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal MAX_UNIT_PRICE = new BigDecimal("99999.99");
    private static final BigDecimal STANDARD_DISCOUNT_CAP = new BigDecimal("0.70");
    private static final BigDecimal LOYALTY_CAP = new BigDecimal("0.20");
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("CNY", "USD", "EUR", "SGD", "GBP");

    private final Clock clock;

    public AdvancedPricingService(Clock clock) {
        this.clock = clock;
    }

    public PriceBreakdown calculate(PriceRequest request) {
        validateRequest(request);
        LocalDate today = LocalDate.now(clock);
        List<String> appliedRules = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        BigDecimal subtotal = money(request.unitPrice().multiply(BigDecimal.valueOf(request.quantity())));
        BigDecimal membershipDiscount = membershipDiscount(request, subtotal, appliedRules, warnings);
        BigDecimal bulkDiscount = bulkDiscount(request.quantity(), subtotal, appliedRules);
        BigDecimal promotionDiscount = promotionDiscount(request, subtotal, today, appliedRules, warnings);
        BigDecimal couponDiscount = couponDiscount(request, subtotal, today, appliedRules, warnings);

        if (promotionDiscount.signum() > 0
                && couponDiscount.signum() > 0
                && request.coupon() != null
                && !request.coupon().stackable()) {
            if (promotionDiscount.compareTo(couponDiscount) >= 0) {
                couponDiscount = ZERO;
                appliedRules.removeIf(rule -> rule.startsWith("COUPON_"));
                warnings.add("The coupon was ignored because the promotion gives a greater discount");
            } else {
                promotionDiscount = ZERO;
                appliedRules.removeIf(rule -> rule.startsWith("PROMOTION_"));
                warnings.add("The promotion was ignored because the coupon gives a greater discount");
            }
        }

        BigDecimal firstPurchaseDiscount = firstPurchaseDiscount(request, subtotal, appliedRules, warnings);
        BigDecimal nonprofitDiscount = nonprofitDiscount(request, subtotal, appliedRules);

        List<BigDecimal> discounts = new ArrayList<>(List.of(
                membershipDiscount,
                bulkDiscount,
                promotionDiscount,
                couponDiscount,
                firstPurchaseDiscount,
                nonprofitDiscount
        ));
        BigDecimal totalDiscount = sum(discounts);
        BigDecimal cap = request.instructorOwnedCourse()
                && request.membershipLevel() == MembershipLevel.INSTRUCTOR
                ? subtotal
                : money(subtotal.multiply(STANDARD_DISCOUNT_CAP));
        if (totalDiscount.compareTo(cap) > 0) {
            BigDecimal excess = totalDiscount.subtract(cap);
            discounts = trimDiscounts(discounts, excess);
            membershipDiscount = discounts.get(0);
            bulkDiscount = discounts.get(1);
            promotionDiscount = discounts.get(2);
            couponDiscount = discounts.get(3);
            firstPurchaseDiscount = discounts.get(4);
            nonprofitDiscount = discounts.get(5);
            totalDiscount = cap;
            warnings.add("Combined discounts were limited by the maximum discount policy");
            appliedRules.add("DISCOUNT_CAP");
        }

        BigDecimal afterDiscounts = money(subtotal.subtract(totalDiscount).max(BigDecimal.ZERO));
        BigDecimal loyaltyDiscount = loyaltyDiscount(request, afterDiscounts, appliedRules, warnings);
        BigDecimal taxableAmount = money(afterDiscounts.subtract(loyaltyDiscount).max(BigDecimal.ZERO));
        BigDecimal tax = calculateTax(request, taxableAmount, appliedRules, warnings);
        BigDecimal total = money(taxableAmount.add(tax));

        if (total.signum() == 0) {
            appliedRules.add("FREE_ORDER");
        }
        if (request.quantity() == 100) {
            warnings.add("The order reached the maximum allowed quantity");
        }
        if (request.unitPrice().signum() == 0) {
            warnings.add("No discount value is generated for a free course");
        }

        return new PriceBreakdown(
                subtotal,
                membershipDiscount,
                bulkDiscount,
                promotionDiscount,
                couponDiscount,
                firstPurchaseDiscount,
                nonprofitDiscount,
                loyaltyDiscount,
                tax,
                total,
                appliedRules,
                warnings
        );
    }

    private void validateRequest(PriceRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (request.courseId() == null || request.courseId().isBlank()) {
            throw new IllegalArgumentException("courseId must not be blank");
        }
        if (request.unitPrice() == null) {
            throw invalidPrice();
        }
        if (request.unitPrice().signum() < 0) {
            throw invalidPrice();
        }
        if (request.unitPrice().compareTo(MAX_UNIT_PRICE) > 0) {
            throw invalidPrice();
        }
        if (request.quantity() < 1) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (request.quantity() > 100) {
            throw new IllegalArgumentException("quantity must not exceed 100");
        }
        if (request.membershipLevel() == null) {
            throw new IllegalArgumentException("membershipLevel must not be null");
        }
        if (request.currency() == null || request.currency().isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        if (!SUPPORTED_CURRENCIES.contains(request.currency().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("unsupported currency: " + request.currency());
        }
        if (request.countryCode() == null || request.countryCode().isBlank()) {
            throw new IllegalArgumentException("countryCode must not be blank");
        }
        if (request.loyaltyPointsToUse() < 0) {
            throw new IllegalArgumentException("loyaltyPointsToUse must not be negative");
        }
        if (request.availableLoyaltyPoints() < 0) {
            throw new IllegalArgumentException("availableLoyaltyPoints must not be negative");
        }
    }

    private BigDecimal membershipDiscount(
            PriceRequest request,
            BigDecimal subtotal,
            List<String> rules,
            List<String> warnings
    ) {
        BigDecimal rate = switch (request.membershipLevel()) {
            case NONE -> BigDecimal.ZERO;
            case BASIC -> new BigDecimal("0.03");
            case PLUS -> new BigDecimal("0.07");
            case PREMIUM -> new BigDecimal("0.12");
            case INSTRUCTOR -> request.instructorOwnedCourse()
                    ? BigDecimal.ONE
                    : new BigDecimal("0.10");
            case ENTERPRISE -> request.quantity() >= 10
                    ? new BigDecimal("0.18")
                    : new BigDecimal("0.08");
        };
        if (rate.signum() == 0) {
            return ZERO;
        }
        if (request.membershipLevel() == MembershipLevel.ENTERPRISE && request.quantity() < 10) {
            warnings.add("Enterprise discount is reduced for orders below ten seats");
        }
        rules.add("MEMBERSHIP_" + request.membershipLevel().name());
        return money(subtotal.multiply(rate));
    }

    private BigDecimal bulkDiscount(int quantity, BigDecimal subtotal, List<String> rules) {
        BigDecimal rate;
        if (quantity >= 50) {
            rate = new BigDecimal("0.15");
            rules.add("BULK_50");
        } else if (quantity >= 20) {
            rate = new BigDecimal("0.10");
            rules.add("BULK_20");
        } else if (quantity >= 5) {
            rate = new BigDecimal("0.05");
            rules.add("BULK_5");
        } else {
            return ZERO;
        }
        return money(subtotal.multiply(rate));
    }

    private BigDecimal promotionDiscount(
            PriceRequest request,
            BigDecimal subtotal,
            LocalDate today,
            List<String> rules,
            List<String> warnings
    ) {
        Promotion promotion = request.promotion();
        if (promotion == null) {
            return ZERO;
        }
        if (promotion.name() == null || promotion.name().isBlank()) {
            warnings.add("A promotion without a name was ignored");
            return ZERO;
        }
        if (promotion.type() == null || promotion.value() == null) {
            warnings.add("Promotion configuration is incomplete");
            return ZERO;
        }
        if (promotion.value().signum() <= 0) {
            warnings.add("Promotion value must be positive");
            return ZERO;
        }
        if (promotion.startsOn() != null && today.isBefore(promotion.startsOn())) {
            warnings.add("Promotion has not started");
            return ZERO;
        }
        if (promotion.endsOn() != null && today.isAfter(promotion.endsOn())) {
            warnings.add("Promotion has expired");
            return ZERO;
        }
        if (promotion.startsOn() != null
                && promotion.endsOn() != null
                && promotion.startsOn().isAfter(promotion.endsOn())) {
            warnings.add("Promotion date range is invalid");
            return ZERO;
        }
        if (promotion.membersOnly() && request.membershipLevel() == MembershipLevel.NONE) {
            warnings.add("Promotion is available to members only");
            return ZERO;
        }
        if (request.quantity() < Math.max(1, promotion.minimumQuantity())) {
            warnings.add("Order quantity is below the promotion minimum");
            return ZERO;
        }

        BigDecimal discount = switch (promotion.type()) {
            case PERCENT -> promotion.value().compareTo(new BigDecimal("80")) > 0
                    ? money(subtotal.multiply(new BigDecimal("0.80")))
                    : percent(subtotal, promotion.value());
            case FIXED -> money(promotion.value().min(subtotal));
            case BUY_MORE_SAVE_MORE -> request.quantity() >= 20
                    ? money(subtotal.multiply(new BigDecimal("0.12")))
                    : request.quantity() >= 10
                    ? money(subtotal.multiply(new BigDecimal("0.08")))
                    : money(subtotal.multiply(new BigDecimal("0.04")));
        };
        if (promotion.maximumDiscount() != null
                && promotion.maximumDiscount().signum() >= 0
                && discount.compareTo(promotion.maximumDiscount()) > 0) {
            discount = money(promotion.maximumDiscount());
            warnings.add("Promotion discount reached its configured cap");
        }
        if (discount.signum() > 0) {
            rules.add("PROMOTION_" + promotion.type().name());
        }
        return discount;
    }

    private BigDecimal couponDiscount(
            PriceRequest request,
            BigDecimal subtotal,
            LocalDate today,
            List<String> rules,
            List<String> warnings
    ) {
        Coupon coupon = request.coupon();
        if (coupon == null) {
            return ZERO;
        }
        if (coupon.code() == null || coupon.code().isBlank()) {
            warnings.add("Coupon code is blank");
            return ZERO;
        }
        if (coupon.type() == null || coupon.value() == null) {
            warnings.add("Coupon configuration is incomplete");
            return ZERO;
        }
        if (coupon.value().signum() <= 0) {
            warnings.add("Coupon value must be positive");
            return ZERO;
        }
        if (coupon.remainingUses() <= 0) {
            warnings.add("Coupon has no remaining uses");
            return ZERO;
        }
        if (coupon.validFrom() != null && today.isBefore(coupon.validFrom())) {
            warnings.add("Coupon is not active yet");
            return ZERO;
        }
        if (coupon.validUntil() != null && today.isAfter(coupon.validUntil())) {
            warnings.add("Coupon has expired");
            return ZERO;
        }
        if (coupon.validFrom() != null
                && coupon.validUntil() != null
                && coupon.validFrom().isAfter(coupon.validUntil())) {
            warnings.add("Coupon date range is invalid");
            return ZERO;
        }
        if (coupon.minimumSubtotal() != null
                && subtotal.compareTo(coupon.minimumSubtotal()) < 0) {
            warnings.add("Order does not meet the coupon minimum subtotal");
            return ZERO;
        }
        if (!coupon.eligibleCourseIds().isEmpty()
                && !coupon.eligibleCourseIds().contains(request.courseId())) {
            warnings.add("Coupon is not valid for this course");
            return ZERO;
        }
        if (coupon.maximumQuantity() > 0 && request.quantity() > coupon.maximumQuantity()) {
            warnings.add("Order quantity exceeds the coupon limit");
            return ZERO;
        }
        if (coupon.minimumMembership() != null
                && request.membershipLevel().ordinal() < coupon.minimumMembership().ordinal()) {
            warnings.add("Membership level is too low for this coupon");
            return ZERO;
        }

        BigDecimal discount = switch (coupon.type()) {
            case PERCENT -> coupon.value().compareTo(new BigDecimal("70")) > 0
                    ? money(subtotal.multiply(new BigDecimal("0.70")))
                    : percent(subtotal, coupon.value());
            case FIXED, FULL_REDUCTION -> money(coupon.value().min(subtotal));
            case CASHBACK -> ZERO;
        };
        if (coupon.type() == CouponType.CASHBACK) {
            rules.add("COUPON_CASHBACK_FUTURE");
            warnings.add("Cashback is credited after payment and does not reduce this order");
        } else if (discount.signum() > 0) {
            rules.add("COUPON_" + coupon.type().name());
        }
        return discount;
    }

    private BigDecimal firstPurchaseDiscount(
            PriceRequest request,
            BigDecimal subtotal,
            List<String> rules,
            List<String> warnings
    ) {
        if (!request.firstPurchase()) {
            return ZERO;
        }
        if (request.instructorOwnedCourse()) {
            warnings.add("First purchase discount is not applied to instructor-owned courses");
            return ZERO;
        }
        if (request.coupon() != null && !request.coupon().stackable()) {
            warnings.add("First purchase discount is blocked by a non-stackable coupon");
            return ZERO;
        }
        rules.add("FIRST_PURCHASE");
        return money(subtotal.multiply(new BigDecimal("0.08"))).min(new BigDecimal("100.00"));
    }

    private BigDecimal nonprofitDiscount(PriceRequest request, BigDecimal subtotal, List<String> rules) {
        if (!request.nonprofitCustomer()) {
            return ZERO;
        }
        if (request.membershipLevel() == MembershipLevel.ENTERPRISE) {
            rules.add("NONPROFIT_ENTERPRISE");
            return money(subtotal.multiply(new BigDecimal("0.08")));
        }
        rules.add("NONPROFIT");
        return money(subtotal.multiply(new BigDecimal("0.05")));
    }

    private BigDecimal loyaltyDiscount(
            PriceRequest request,
            BigDecimal amount,
            List<String> rules,
            List<String> warnings
    ) {
        if (request.loyaltyPointsToUse() == 0 || amount.signum() == 0) {
            return ZERO;
        }
        int usablePoints = request.loyaltyPointsToUse();
        if (usablePoints > request.availableLoyaltyPoints()) {
            usablePoints = request.availableLoyaltyPoints();
            warnings.add("Requested loyalty points were reduced to the available balance");
        }
        if (usablePoints < 100) {
            warnings.add("At least 100 loyalty points are required");
            return ZERO;
        }
        BigDecimal value = money(BigDecimal.valueOf(usablePoints).movePointLeft(2));
        BigDecimal cap = money(amount.multiply(LOYALTY_CAP));
        if (value.compareTo(cap) > 0) {
            value = cap;
            warnings.add("Loyalty discount was limited to twenty percent");
        }
        if (value.signum() > 0) {
            rules.add("LOYALTY_POINTS");
        }
        return value;
    }

    private BigDecimal calculateTax(
            PriceRequest request,
            BigDecimal taxableAmount,
            List<String> rules,
            List<String> warnings
    ) {
        if (request.taxExempt()) {
            rules.add("TAX_EXEMPT");
            return ZERO;
        }
        if (taxableAmount.signum() == 0) {
            return ZERO;
        }
        String country = request.countryCode().toUpperCase(Locale.ROOT);
        BigDecimal rate = switch (country) {
            case "CN" -> new BigDecimal("0.06");
            case "US" -> new BigDecimal("0.075");
            case "DE" -> new BigDecimal("0.19");
            case "SG" -> new BigDecimal("0.09");
            case "GB" -> new BigDecimal("0.20");
            default -> BigDecimal.ZERO;
        };
        if (request.nonprofitCustomer() && rate.signum() > 0) {
            rate = rate.multiply(new BigDecimal("0.50"));
            rules.add("NONPROFIT_TAX_REDUCTION");
        }
        if (rate.signum() == 0) {
            warnings.add("No tax rule is configured for country " + country);
            return ZERO;
        }
        if (country.equals("CN") && !request.currency().equalsIgnoreCase("CNY")) {
            warnings.add("Chinese tax is calculated for a non-CNY order");
        }
        if (country.equals("US") && !request.currency().equalsIgnoreCase("USD")) {
            warnings.add("United States tax is calculated for a non-USD order");
        }
        if (country.equals("DE") && !request.currency().equalsIgnoreCase("EUR")) {
            warnings.add("German tax is calculated for a non-EUR order");
        }
        rules.add("TAX_" + country);
        return money(taxableAmount.multiply(rate));
    }

    private List<BigDecimal> trimDiscounts(List<BigDecimal> discounts, BigDecimal excess) {
        List<BigDecimal> result = new ArrayList<>(discounts);
        for (int index = result.size() - 1; index >= 0 && excess.signum() > 0; index--) {
            BigDecimal current = result.get(index);
            if (current.signum() == 0) {
                continue;
            }
            BigDecimal reduction = current.min(excess);
            result.set(index, money(current.subtract(reduction)));
            excess = money(excess.subtract(reduction));
        }
        return result;
    }

    private BigDecimal percent(BigDecimal amount, BigDecimal percentage) {
        return money(amount.multiply(percentage.movePointLeft(2)));
    }

    private BigDecimal sum(List<BigDecimal> values) {
        return money(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BusinessException invalidPrice() {
        return new BusinessException(
                ErrorCode.INVALID_PRICE,
                "Unit price must be between 0 and 99999.99"
        );
    }
}
