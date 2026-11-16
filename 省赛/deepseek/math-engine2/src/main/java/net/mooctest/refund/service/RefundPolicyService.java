package net.mooctest.refund.service;

import net.mooctest.refund.domain.PaymentMethod;
import net.mooctest.refund.domain.PurchaseSnapshot;
import net.mooctest.refund.domain.RefundDecision;
import net.mooctest.refund.domain.RefundReason;
import net.mooctest.refund.domain.RefundRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class RefundPolicyService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final Clock clock;

    public RefundPolicyService(Clock clock) {
        this.clock = clock;
    }

    public RefundDecision evaluate(RefundRequest request) {
        validate(request);
        PurchaseSnapshot purchase = request.purchase();
        Instant now = Instant.now(clock);
        long hoursSincePurchase = Duration.between(purchase.paidAt(), now).toHours();
        List<String> reasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (hoursSincePurchase < 0) {
            return rejected("PURCHASE_DATE_IN_FUTURE", warnings);
        }
        if (purchase.previouslyRefunded()) {
            return rejected("PURCHASE_ALREADY_REFUNDED", warnings);
        }
        if (purchase.chargebackOpen()) {
            return manual("OPEN_CHARGEBACK", warnings);
        }
        if (request.fraudSuspected() || request.reason() == RefundReason.FRAUD) {
            return manual("FRAUD_REVIEW_REQUIRED", warnings);
        }
        if (purchase.accessExpiresAt() != null && now.isAfter(purchase.accessExpiresAt())) {
            warnings.add("COURSE_ACCESS_EXPIRED");
        }

        BigDecimal eligibleRate = determineEligibleRate(request, hoursSincePurchase, reasons, warnings);
        if (eligibleRate.signum() == 0 && !request.adminOverride()) {
            if (reasons.isEmpty()) {
                reasons.add("REFUND_POLICY_NOT_SATISFIED");
            }
            return new RefundDecision(false, false, ZERO, ZERO, 0, "NONE", reasons, warnings);
        }

        if (request.adminOverride()) {
            if (eligibleRate.signum() == 0) {
                eligibleRate = request.fullRefundRequested()
                        ? BigDecimal.ONE
                        : percentageOrDefault(request.requestedPercentage(), new BigDecimal("0.50"));
            }
            warnings.add("ADMIN_OVERRIDE_APPLIED");
        }

        BigDecimal requestedRate = request.fullRefundRequested()
                ? BigDecimal.ONE
                : percentageOrDefault(request.requestedPercentage(), eligibleRate);
        if (requestedRate.compareTo(eligibleRate) > 0) {
            requestedRate = eligibleRate;
            warnings.add("REQUESTED_PERCENTAGE_REDUCED_TO_POLICY_LIMIT");
        }
        if (requestedRate.signum() <= 0) {
            return rejected("REQUESTED_PERCENTAGE_IS_ZERO", warnings);
        }

        BigDecimal grossRefund = money(purchase.paidAmount().multiply(requestedRate));
        BigDecimal fee = calculateProcessingFee(request, grossRefund, hoursSincePurchase, warnings);
        if (fee.compareTo(grossRefund) > 0) {
            fee = grossRefund;
            warnings.add("PROCESSING_FEE_LIMITED_TO_REFUND_AMOUNT");
        }
        BigDecimal refundable = money(grossRefund.subtract(fee).max(BigDecimal.ZERO));

        if (purchase.couponUsed() && !isProviderFault(request)) {
            BigDecimal couponAdjustment = money(purchase.discountAmount().multiply(requestedRate));
            if (couponAdjustment.compareTo(refundable) < 0) {
                refundable = money(refundable.subtract(couponAdjustment));
                warnings.add("COUPON_VALUE_NOT_REFUNDED");
            } else {
                refundable = ZERO;
                warnings.add("COUPON_VALUE_CONSUMED_REFUND");
            }
        }

        int returnedPoints = calculateReturnedPoints(purchase, request, requestedRate, warnings);
        String route = determineRoute(request, warnings);
        boolean manualReview = route.equals("MANUAL_REVIEW");

        if (refundable.compareTo(new BigDecimal("1.00")) < 0 && refundable.signum() > 0) {
            if (purchase.paymentMethod() == PaymentMethod.WALLET) {
                warnings.add("SMALL_REFUND_RETURNED_TO_WALLET");
            } else {
                fee = money(fee.add(refundable));
                refundable = ZERO;
                warnings.add("REFUND_BELOW_MINIMUM_PAYOUT");
            }
        }
        if (refundable.signum() == 0 && returnedPoints == 0 && !manualReview) {
            return rejected("NO_REFUNDABLE_VALUE", warnings);
        }

        reasons.add("REFUND_APPROVED");
        return new RefundDecision(
                !manualReview,
                manualReview,
                refundable,
                fee,
                returnedPoints,
                route,
                reasons,
                warnings
        );
    }

    private BigDecimal determineEligibleRate(
            RefundRequest request,
            long hoursSincePurchase,
            List<String> reasons,
            List<String> warnings
    ) {
        PurchaseSnapshot purchase = request.purchase();

        if (purchase.instructorCancelled()
                || purchase.courseRemoved()
                || request.reason() == RefundReason.COURSE_CANCELLED) {
            reasons.add("PROVIDER_CANCELLED_COURSE");
            return BigDecimal.ONE;
        }
        if (request.reason() == RefundReason.DUPLICATE_PAYMENT) {
            if (request.duplicatePaymentConfirmed()) {
                reasons.add("DUPLICATE_PAYMENT_CONFIRMED");
                return BigDecimal.ONE;
            }
            if (request.evidenceProvided()) {
                reasons.add("DUPLICATE_PAYMENT_REQUIRES_REVIEW");
                return new BigDecimal("0.50");
            }
            reasons.add("DUPLICATE_PAYMENT_NOT_CONFIRMED");
            return BigDecimal.ZERO;
        }
        if (request.reason() == RefundReason.TECHNICAL_ISSUE) {
            if (request.technicalIssueConfirmed()) {
                reasons.add("CONFIRMED_TECHNICAL_ISSUE");
                return purchase.progressPercent() <= 50 ? BigDecimal.ONE : new BigDecimal("0.75");
            }
            if (!request.evidenceProvided()) {
                reasons.add("TECHNICAL_EVIDENCE_REQUIRED");
                return BigDecimal.ZERO;
            }
            warnings.add("UNCONFIRMED_TECHNICAL_ISSUE");
        }
        if (request.reason() == RefundReason.MEDICAL) {
            if (request.medicalDocumentVerified()) {
                reasons.add("VERIFIED_MEDICAL_EXCEPTION");
                return purchase.progressPercent() < 80 ? BigDecimal.ONE : new BigDecimal("0.40");
            }
            if (request.evidenceProvided()) {
                reasons.add("MEDICAL_DOCUMENT_REQUIRES_REVIEW");
                return new BigDecimal("0.50");
            }
            reasons.add("MEDICAL_DOCUMENT_REQUIRED");
            return BigDecimal.ZERO;
        }
        if (request.reason() == RefundReason.CONTENT_QUALITY && request.contentComplaintUpheld()) {
            reasons.add("CONTENT_COMPLAINT_UPHELD");
            return purchase.progressPercent() < 60 ? BigDecimal.ONE : new BigDecimal("0.60");
        }
        if (purchase.certificateIssued()) {
            reasons.add("CERTIFICATE_ALREADY_ISSUED");
            return BigDecimal.ZERO;
        }
        if (purchase.progressPercent() >= 80) {
            reasons.add("COURSE_SUBSTANTIALLY_COMPLETED");
            return BigDecimal.ZERO;
        }
        if (purchase.downloadedResources() >= 10) {
            reasons.add("TOO_MANY_RESOURCES_DOWNLOADED");
            return BigDecimal.ZERO;
        }

        if (hoursSincePurchase <= 2 && purchase.progressPercent() == 0) {
            reasons.add("COOLING_OFF_PERIOD");
            return BigDecimal.ONE;
        }
        if (hoursSincePurchase <= 24 && purchase.progressPercent() <= 5) {
            reasons.add("ONE_DAY_LOW_USAGE");
            return BigDecimal.ONE;
        }
        if (hoursSincePurchase <= 7 * 24L && purchase.progressPercent() <= 10) {
            reasons.add("SEVEN_DAY_GUARANTEE");
            return BigDecimal.ONE;
        }
        if (hoursSincePurchase <= 7 * 24L && purchase.progressPercent() <= 30) {
            reasons.add("SEVEN_DAY_PARTIAL_REFUND");
            return new BigDecimal("0.70");
        }
        if (hoursSincePurchase <= 14 * 24L && purchase.progressPercent() <= 30) {
            reasons.add("FOURTEEN_DAY_PARTIAL_REFUND");
            return new BigDecimal("0.50");
        }
        if (hoursSincePurchase <= 30 * 24L && purchase.progressPercent() <= 15) {
            reasons.add("THIRTY_DAY_EXCEPTION");
            return new BigDecimal("0.25");
        }
        if (request.reason() == RefundReason.ACCIDENTAL_PURCHASE && purchase.downloadedResources() > 0) {
            reasons.add("ACCIDENTAL_PURCHASE_AFTER_DOWNLOAD");
            return BigDecimal.ZERO;
        }
        if (request.reason() == RefundReason.OTHER && !request.evidenceProvided()) {
            reasons.add("OTHER_REASON_REQUIRES_EVIDENCE");
            return BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateProcessingFee(
            RefundRequest request,
            BigDecimal grossRefund,
            long hoursSincePurchase,
            List<String> warnings
    ) {
        PurchaseSnapshot purchase = request.purchase();
        if (isProviderFault(request)) {
            return ZERO;
        }
        if (request.adminOverride()) {
            return ZERO;
        }

        BigDecimal rate = switch (purchase.paymentMethod()) {
            case CARD -> new BigDecimal("0.03");
            case ALIPAY, WECHAT -> new BigDecimal("0.01");
            case BANK_TRANSFER -> new BigDecimal("0.02");
            case WALLET, GIFT -> BigDecimal.ZERO;
            case CRYPTO -> new BigDecimal("0.05");
        };
        if (hoursSincePurchase <= 24) {
            rate = BigDecimal.ZERO;
        }
        if (purchase.downloadedResources() > 0) {
            rate = rate.add(new BigDecimal("0.02"));
            warnings.add("RESOURCE_DOWNLOAD_FEE_APPLIED");
        }
        if (purchase.totalLiveSessions() > 0 && purchase.liveSessionsAttended() > 0) {
            BigDecimal attendedRate = BigDecimal.valueOf(purchase.liveSessionsAttended())
                    .divide(BigDecimal.valueOf(purchase.totalLiveSessions()), 4, RoundingMode.HALF_UP);
            if (attendedRate.compareTo(new BigDecimal("0.50")) >= 0) {
                rate = rate.add(new BigDecimal("0.10"));
                warnings.add("LIVE_SESSION_ATTENDANCE_FEE_APPLIED");
            } else if (attendedRate.compareTo(new BigDecimal("0.20")) >= 0) {
                rate = rate.add(new BigDecimal("0.05"));
                warnings.add("PARTIAL_LIVE_ATTENDANCE_FEE_APPLIED");
            }
        }
        if (rate.compareTo(new BigDecimal("0.20")) > 0) {
            rate = new BigDecimal("0.20");
            warnings.add("PROCESSING_FEE_RATE_CAPPED");
        }
        return money(grossRefund.multiply(rate));
    }

    private int calculateReturnedPoints(
            PurchaseSnapshot purchase,
            RefundRequest request,
            BigDecimal refundRate,
            List<String> warnings
    ) {
        if (purchase.loyaltyPointsUsed() <= 0) {
            return 0;
        }
        if (request.reason() == RefundReason.FRAUD || request.fraudSuspected()) {
            return 0;
        }
        int returned = refundRate.multiply(BigDecimal.valueOf(purchase.loyaltyPointsUsed()))
                .setScale(0, RoundingMode.DOWN)
                .intValue();
        if (returned < purchase.loyaltyPointsUsed()) {
            warnings.add("LOYALTY_POINTS_RETURNED_PROPORTIONALLY");
        }
        if (purchase.couponUsed() && !isProviderFault(request)) {
            returned = returned / 2;
            warnings.add("POINT_RETURN_REDUCED_FOR_COUPON_ORDER");
        }
        return Math.max(0, returned);
    }

    private String determineRoute(RefundRequest request, List<String> warnings) {
        PurchaseSnapshot purchase = request.purchase();
        if (purchase.giftPurchase() || purchase.paymentMethod() == PaymentMethod.GIFT) {
            warnings.add("GIFT_REFUND_ISSUED_AS_CREDIT");
            return "WALLET_CREDIT";
        }
        return switch (purchase.paymentMethod()) {
            case CARD, ALIPAY, WECHAT -> "ORIGINAL_PAYMENT_METHOD";
            case WALLET -> "WALLET_CREDIT";
            case BANK_TRANSFER -> {
                if (request.bankDetailsPresent()) {
                    yield "BANK_TRANSFER";
                }
                warnings.add("BANK_DETAILS_REQUIRED");
                yield "MANUAL_REVIEW";
            }
            case CRYPTO -> {
                warnings.add("CRYPTO_REFUND_REQUIRES_MANUAL_REVIEW");
                yield "MANUAL_REVIEW";
            }
            case GIFT -> "WALLET_CREDIT";
        };
    }

    private boolean isProviderFault(RefundRequest request) {
        PurchaseSnapshot purchase = request.purchase();
        return purchase.instructorCancelled()
                || purchase.courseRemoved()
                || request.reason() == RefundReason.COURSE_CANCELLED
                || request.technicalIssueConfirmed()
                || request.contentComplaintUpheld()
                || request.duplicatePaymentConfirmed();
    }

    private BigDecimal percentageOrDefault(int percentage, BigDecimal defaultRate) {
        if (percentage <= 0) {
            return defaultRate;
        }
        if (percentage > 100) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(percentage).movePointLeft(2);
    }

    private void validate(RefundRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(request.purchase(), "purchase must not be null");
        Objects.requireNonNull(request.reason(), "reason must not be null");
        PurchaseSnapshot purchase = request.purchase();
        if (purchase.purchaseId() == null || purchase.purchaseId().isBlank()) {
            throw new IllegalArgumentException("purchaseId must not be blank");
        }
        if (purchase.studentId() == null || purchase.studentId().isBlank()) {
            throw new IllegalArgumentException("studentId must not be blank");
        }
        if (purchase.courseId() == null || purchase.courseId().isBlank()) {
            throw new IllegalArgumentException("courseId must not be blank");
        }
        if (purchase.paidAmount() == null || purchase.paidAmount().signum() < 0) {
            throw new IllegalArgumentException("paidAmount must not be negative");
        }
        if (purchase.taxAmount() == null || purchase.taxAmount().signum() < 0) {
            throw new IllegalArgumentException("taxAmount must not be negative");
        }
        if (purchase.discountAmount() == null || purchase.discountAmount().signum() < 0) {
            throw new IllegalArgumentException("discountAmount must not be negative");
        }
        if (purchase.paidAt() == null) {
            throw new IllegalArgumentException("paidAt must not be null");
        }
        if (purchase.paymentMethod() == null) {
            throw new IllegalArgumentException("paymentMethod must not be null");
        }
        if (purchase.progressPercent() < 0 || purchase.progressPercent() > 100) {
            throw new IllegalArgumentException("progressPercent must be between 0 and 100");
        }
        if (purchase.downloadedResources() < 0) {
            throw new IllegalArgumentException("downloadedResources must not be negative");
        }
        if (purchase.liveSessionsAttended() < 0 || purchase.totalLiveSessions() < 0) {
            throw new IllegalArgumentException("live session counts must not be negative");
        }
        if (purchase.liveSessionsAttended() > purchase.totalLiveSessions()) {
            throw new IllegalArgumentException("attended sessions must not exceed total sessions");
        }
        if (purchase.loyaltyPointsUsed() < 0) {
            throw new IllegalArgumentException("loyaltyPointsUsed must not be negative");
        }
        if (request.requestedPercentage() < 0 || request.requestedPercentage() > 100) {
            throw new IllegalArgumentException("requestedPercentage must be between 0 and 100");
        }
    }

    private RefundDecision rejected(String reason, List<String> warnings) {
        return new RefundDecision(false, false, ZERO, ZERO, 0, "NONE", List.of(reason), warnings);
    }

    private RefundDecision manual(String reason, List<String> warnings) {
        return new RefundDecision(false, true, ZERO, ZERO, 0, "MANUAL_REVIEW", List.of(reason), warnings);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
