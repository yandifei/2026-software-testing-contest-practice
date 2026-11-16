package net.mooctest.refund.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PurchaseSnapshot(
        String purchaseId,
        String studentId,
        String courseId,
        BigDecimal paidAmount,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        Instant paidAt,
        PaymentMethod paymentMethod,
        int progressPercent,
        int downloadedResources,
        boolean certificateIssued,
        int liveSessionsAttended,
        int totalLiveSessions,
        boolean giftPurchase,
        boolean couponUsed,
        int loyaltyPointsUsed,
        boolean chargebackOpen,
        boolean previouslyRefunded,
        boolean instructorCancelled,
        boolean courseRemoved,
        Instant accessExpiresAt
) {
}
