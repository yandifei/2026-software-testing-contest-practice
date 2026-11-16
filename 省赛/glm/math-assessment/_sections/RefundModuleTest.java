package net.mooctest;

import net.mooctest.refund.domain.PaymentMethod;
import net.mooctest.refund.domain.PurchaseSnapshot;
import net.mooctest.refund.domain.RefundDecision;
import net.mooctest.refund.domain.RefundReason;
import net.mooctest.refund.domain.RefundRequest;
import net.mooctest.refund.service.RefundPolicyService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RefundPolicyService + refund domain module tests (fixed clock: today = 2026-06-15 UTC).
 *
 * <p>约定：期望值全部硬编码（金额用 toPlainString + compareTo 双重断言，固定 2 位小数），
 * 每条校验规则一个独立测试并断言精确异常消息，每个 RefundDecision 断言全部 8 个字段，
 * 每个 RefundReason 枚举值至少一条端到端决策用例。</p>
 *
 * <p>DEAD branches（对照 RefundPolicyService 逐行核实，确不可达，故无测试）:</p>
 * <ul>
 *   <li>L78 requestedRate.signum() &lt;= 0: requestedPercentage 经 validate() 限定 0..100，
 *       pct&lt;=0 时返回此刻必为正的 eligibleRate，故 requestedRate 恒 &gt; 0，
 *       REQUESTED_PERCENTAGE_IS_ZERO 不可达。</li>
 *   <li>L84 fee &gt; grossRefund: fee = money(gross × rate)，rate 上限 0.17
 *       （0.05 方法费 + 0.02 下载费 + 0.10 直播费），对任意非负 gross 恒有 fee ≤ gross。</li>
 *   <li>L271 rate &gt; 0.20 封顶: 可达的最大费率为 0.17，封顶分支永不触发。</li>
 *   <li>L287 两个 true 侧（FRAUD 原因 / fraudSuspected）: evaluate() 在 L47-49 已提前返回
 *       FRAUD_REVIEW_REQUIRED 人工审核决策，calculateReturnedPoints 永远不会看到这两者。</li>
 *   <li>L309 路由 switch 的 case GIFT: 能到达 switch 的请求必满足 !giftPurchase 且
 *       method != GIFT（L305 的 if 已对两者先行返回），case GIFT 体不可达。</li>
 *   <li>L341 percentage &gt; 100: percentageOrDefault 的两个调用点均传入经校验的
 *       requestedPercentage（≤ 100），该分支不可达。</li>
 * </ul></p>
 */
class RefundModuleTest {

    private static final Instant NOW = Instant.parse("2026-06-15T02:00:00Z");

    private static final Instant H1 = NOW.minusSeconds(3600);        // 1h ago
    private static final Instant H2 = NOW.minusSeconds(7200);        // exactly 2h
    private static final Instant H3 = NOW.minusSeconds(10800);       // 3h
    private static final Instant H24 = NOW.minusSeconds(86400);      // exactly 24h
    private static final Instant H25 = NOW.minusSeconds(90000);      // 25h
    private static final Instant H168 = NOW.minusSeconds(604800);    // exactly 7d
    private static final Instant H169 = NOW.minusSeconds(608400);    // 169h
    private static final Instant H336 = NOW.minusSeconds(1209600);   // exactly 14d
    private static final Instant H337 = NOW.minusSeconds(1213200);   // 337h
    private static final Instant H720 = NOW.minusSeconds(2592000);   // exactly 30d
    private static final Instant H721 = NOW.minusSeconds(2595600);   // 721h
    private static final Instant FUTURE = NOW.plusSeconds(3600);     // 1h ahead

    private static RefundPolicyService service() {
        return new RefundPolicyService(Clock.fixed(NOW, ZoneId.of("UTC")));
    }

    /** 默认快照：P-1/S-1/C-1，paid 100.00，无证书/无券/无积分/无取消，accessExpiresAt=null。 */
    private static PurchaseSnapshot purchase(Instant paidAt, PaymentMethod method, int progress, int downloads) {
        return snapshot(paidAt, method, progress, downloads,
                "100.00", "0.00", "0.00", false, 0, 0, 0, false, false, false, false, false, false, null);
    }

    private static PurchaseSnapshot snapshot(Instant paidAt, PaymentMethod method, int progress, int downloads,
                                             String paidAmount, String tax, String discount,
                                             boolean certificateIssued, int attended, int totalSessions,
                                             int loyaltyPoints, boolean giftPurchase, boolean couponUsed,
                                             boolean chargebackOpen, boolean previouslyRefunded,
                                             boolean instructorCancelled, boolean courseRemoved,
                                             Instant accessExpiresAt) {
        return new PurchaseSnapshot("P-1", "S-1", "C-1", new BigDecimal(paidAmount), new BigDecimal(tax),
                new BigDecimal(discount), paidAt, method, progress, downloads, certificateIssued,
                attended, totalSessions, giftPurchase, couponUsed, loyaltyPoints, chargebackOpen,
                previouslyRefunded, instructorCancelled, courseRemoved, accessExpiresAt);
    }

    private static RefundRequest request(PurchaseSnapshot p, RefundReason reason, boolean fullRefund, int pct,
                                         boolean evidence, boolean techConfirmed, boolean complaintUpheld,
                                         boolean medicalVerified, boolean duplicateConfirmed, boolean bankDetails,
                                         boolean fraudSuspected, boolean adminOverride) {
        return new RefundRequest(p, reason, fullRefund, pct, evidence, techConfirmed, complaintUpheld,
                medicalVerified, duplicateConfirmed, bankDetails, fraudSuspected, adminOverride);
    }

    /** 便利构造：全额退款、无附加标记、银行账户已提供。 */
    private static RefundRequest request(PurchaseSnapshot p, RefundReason reason) {
        return request(p, reason, true, 0, false, false, false, false, false, true, false, false);
    }

    /** 全字段断言：金额同时断言 compareTo（值）与 toPlainString（固定 2 位小数刻度）。 */
    private static void assertDecision(RefundDecision d, boolean approved, boolean manualReview,
                                       String refundable, String fee, int points, String route,
                                       List<String> reasons, List<String> warnings) {
        assertEquals(approved, d.approved(), "approved");
        assertEquals(manualReview, d.manualReview(), "manualReview");
        assertNotNull(d.refundableAmount(), "refundableAmount null");
        assertEquals(0, d.refundableAmount().compareTo(new BigDecimal(refundable)), "refundableAmount value");
        assertEquals(refundable, d.refundableAmount().toPlainString(), "refundableAmount scale");
        assertNotNull(d.processingFee(), "processingFee null");
        assertEquals(0, d.processingFee().compareTo(new BigDecimal(fee)), "processingFee value");
        assertEquals(fee, d.processingFee().toPlainString(), "processingFee scale");
        assertEquals(points, d.loyaltyPointsReturned(), "loyaltyPointsReturned");
        assertEquals(route, d.refundRoute(), "refundRoute");
        assertEquals(reasons, d.reasons(), "reasons");
        assertEquals(warnings, d.warnings(), "warnings");
    }

    @Nested
    class RequestValidationTests {

        @Test
        void nullRequestIsRejected() {
            NullPointerException ex = assertThrows(NullPointerException.class, () -> service().evaluate(null));
            assertEquals("request must not be null", ex.getMessage());
        }

        @Test
        void nullPurchaseIsRejected() {
            RefundRequest r = new RefundRequest(null, RefundReason.OTHER, true, 0, false,
                    false, false, false, false, true, false, false);
            NullPointerException ex = assertThrows(NullPointerException.class, () -> service().evaluate(r));
            assertEquals("purchase must not be null", ex.getMessage());
        }

        @Test
        void nullReasonIsRejected() {
            RefundRequest r = new RefundRequest(purchase(H1, PaymentMethod.CARD, 0, 0), null, true, 0,
                    false, false, false, false, false, true, false, false);
            NullPointerException ex = assertThrows(NullPointerException.class, () -> service().evaluate(r));
            assertEquals("reason must not be null", ex.getMessage());
        }

        @Test
        void blankPurchaseIdIsRejected() {
            PurchaseSnapshot blank = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            PurchaseSnapshot p = new PurchaseSnapshot("  ", blank.studentId(), blank.courseId(),
                    blank.paidAmount(), blank.taxAmount(), blank.discountAmount(), blank.paidAt(),
                    blank.paymentMethod(), blank.progressPercent(), blank.downloadedResources(),
                    blank.certificateIssued(), blank.liveSessionsAttended(), blank.totalLiveSessions(),
                    blank.giftPurchase(), blank.couponUsed(), blank.loyaltyPointsUsed(),
                    blank.chargebackOpen(), blank.previouslyRefunded(), blank.instructorCancelled(),
                    blank.courseRemoved(), blank.accessExpiresAt());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("purchaseId must not be blank", ex.getMessage());
        }

        @Test
        void nullPurchaseIdIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot(null, "S-1", "C-1", new BigDecimal("100.00"),
                    new BigDecimal("0.00"), new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("purchaseId must not be blank", ex.getMessage());
        }

        @Test
        void blankStudentIdIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", " ", "C-1", new BigDecimal("100.00"),
                    new BigDecimal("0.00"), new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("studentId must not be blank", ex.getMessage());
        }

        @Test
        void nullCourseIdIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", null, new BigDecimal("100.00"),
                    new BigDecimal("0.00"), new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("courseId must not be blank", ex.getMessage());
        }

        @Test
        void nullPaidAmountIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", "C-1", null, new BigDecimal("0.00"),
                    new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("paidAmount must not be negative", ex.getMessage());
        }

        @Test
        void negativePaidAmountIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", "C-1", new BigDecimal("-0.01"),
                    new BigDecimal("0.00"), new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("paidAmount must not be negative", ex.getMessage());
        }

        @Test
        void negativeTaxAmountIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", "C-1", new BigDecimal("100.00"),
                    new BigDecimal("-1.00"), new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("taxAmount must not be negative", ex.getMessage());
        }

        @Test
        void negativeDiscountAmountIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", "C-1", new BigDecimal("100.00"),
                    new BigDecimal("0.00"), new BigDecimal("-0.50"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("discountAmount must not be negative", ex.getMessage());
        }

        @Test
        void nullPaidAtIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(purchase(null, PaymentMethod.CARD, 0, 0), RefundReason.OTHER)));
            assertEquals("paidAt must not be null", ex.getMessage());
        }

        @Test
        void nullPaymentMethodIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(purchase(H1, null, 0, 0), RefundReason.OTHER)));
            assertEquals("paymentMethod must not be null", ex.getMessage());
        }

        @Test
        void progressPercentBelowZeroIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(purchase(H1, PaymentMethod.CARD, -1, 0), RefundReason.OTHER)));
            assertEquals("progressPercent must be between 0 and 100", ex.getMessage());
        }

        @Test
        void progressPercentAboveHundredIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(purchase(H1, PaymentMethod.CARD, 101, 0), RefundReason.OTHER)));
            assertEquals("progressPercent must be between 0 and 100", ex.getMessage());
        }

        @Test
        void negativeDownloadedResourcesIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(purchase(H1, PaymentMethod.CARD, 0, -1), RefundReason.OTHER)));
            assertEquals("downloadedResources must not be negative", ex.getMessage());
        }

        @Test
        void negativeLiveSessionCountsAreRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(
                            snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                                    false, -1, 5, 0, false, false, false, false, false, false, null),
                            RefundReason.OTHER)));
            assertEquals("live session counts must not be negative", ex.getMessage());

            IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(
                            snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                                    false, 1, -1, 0, false, false, false, false, false, false, null),
                            RefundReason.OTHER)));
            assertEquals("live session counts must not be negative", ex2.getMessage());
        }

        @Test
        void attendedSessionsExceedingTotalIsRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(
                            snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                                    false, 2, 1, 0, false, false, false, false, false, false, null),
                            RefundReason.OTHER)));
            assertEquals("attended sessions must not exceed total sessions", ex.getMessage());
        }

        @Test
        void negativeLoyaltyPointsAreRejected() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(
                            snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                                    false, 0, 0, -1, false, false, false, false, false, false, null),
                            RefundReason.OTHER)));
            assertEquals("loyaltyPointsUsed must not be negative", ex.getMessage());
        }

        @Test
        void requestedPercentageBelowZeroIsRejected() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 0, 0), RefundReason.OTHER,
                    false, -1, false, false, false, false, false, true, false, false);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(r));
            assertEquals("requestedPercentage must be between 0 and 100", ex.getMessage());
        }

        @Test
        void requestedPercentageAboveHundredIsRejected() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 0, 0), RefundReason.OTHER,
                    false, 101, false, false, false, false, false, true, false, false);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(r));
            assertEquals("requestedPercentage must be between 0 and 100", ex.getMessage());
        }

        @Test
        void nullStudentIdIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", null, "C-1", new BigDecimal("100.00"),
                    new BigDecimal("0.00"), new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("studentId must not be blank", ex.getMessage());
        }

        @Test
        void blankCourseIdIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", "  ", new BigDecimal("100.00"),
                    new BigDecimal("0.00"), new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("courseId must not be blank", ex.getMessage());
        }

        @Test
        void nullTaxAmountIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", "C-1", new BigDecimal("100.00"),
                    null, new BigDecimal("0.00"), H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("taxAmount must not be negative", ex.getMessage());
        }

        @Test
        void nullDiscountAmountIsRejected() {
            PurchaseSnapshot p = new PurchaseSnapshot("P-1", "S-1", "C-1", new BigDecimal("100.00"),
                    new BigDecimal("0.00"), null, H1, PaymentMethod.CARD, 0, 0,
                    false, 0, 0, false, false, 0, false, false, false, false, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service().evaluate(request(p, RefundReason.OTHER)));
            assertEquals("discountAmount must not be negative", ex.getMessage());
        }

        @Test
        void validBoundaryRequestPassesValidation() {
            RefundDecision d = service().evaluate(request(purchase(H2, PaymentMethod.CARD, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COOLING_OFF_PERIOD", "REFUND_APPROVED"), List.of());
        }
    }

    @Nested
    class EligibleRateTests {

        @Test
        void instructorCancelledCourseGrantsFullRefundWithoutFee() {
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 50, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, true, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("PROVIDER_CANCELLED_COURSE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void courseRemovedGrantsFullRefundWithoutFee() {
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 50, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, true, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("PROVIDER_CANCELLED_COURSE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void duplicatePaymentConfirmedGrantsFullRefund() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 50, 0), RefundReason.DUPLICATE_PAYMENT,
                    true, 0, false, false, false, false, true, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("DUPLICATE_PAYMENT_CONFIRMED", "REFUND_APPROVED"), List.of());
        }

        @Test
        void duplicatePaymentWithEvidenceOnlyGetsHalf() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 50, 0), RefundReason.DUPLICATE_PAYMENT,
                    false, 50, true, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "50.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("DUPLICATE_PAYMENT_REQUIRES_REVIEW", "REFUND_APPROVED"), List.of());
        }

        @Test
        void duplicatePaymentUnconfirmedIsRejected() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 50, 0), RefundReason.DUPLICATE_PAYMENT,
                    true, 0, false, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("DUPLICATE_PAYMENT_NOT_CONFIRMED"), List.of());
        }

        @Test
        void confirmedTechnicalIssueAtHalfProgressGrantsFullRefund() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 50, 0), RefundReason.TECHNICAL_ISSUE,
                    true, 0, false, true, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("CONFIRMED_TECHNICAL_ISSUE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void confirmedTechnicalIssueJustPastHalfGetsSeventyFivePercent() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 51, 0), RefundReason.TECHNICAL_ISSUE,
                    false, 75, false, true, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "75.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("CONFIRMED_TECHNICAL_ISSUE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void unconfirmedTechnicalIssueWithoutEvidenceIsRejected() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 0, 0), RefundReason.TECHNICAL_ISSUE,
                    true, 0, false, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("TECHNICAL_EVIDENCE_REQUIRED"), List.of());
        }

        @Test
        void unconfirmedTechnicalIssueWithEvidenceWarnsThenFallsToCoolingOff() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 0, 0), RefundReason.TECHNICAL_ISSUE,
                    true, 0, true, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COOLING_OFF_PERIOD", "REFUND_APPROVED"), List.of("UNCONFIRMED_TECHNICAL_ISSUE"));
        }

        @Test
        void verifiedMedicalUnderEightyProgressGrantsFullRefund() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 79, 0), RefundReason.MEDICAL,
                    true, 0, false, false, false, true, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "97.00", "3.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("VERIFIED_MEDICAL_EXCEPTION", "REFUND_APPROVED"), List.of());
        }

        @Test
        void verifiedMedicalAtEightyProgressDropsToFortyPercent() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 80, 0), RefundReason.MEDICAL,
                    false, 40, false, false, false, true, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "38.80", "1.20", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("VERIFIED_MEDICAL_EXCEPTION", "REFUND_APPROVED"), List.of());
        }

        @Test
        void medicalEvidenceWithoutVerificationGetsHalf() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 30, 0), RefundReason.MEDICAL,
                    false, 50, true, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "48.50", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("MEDICAL_DOCUMENT_REQUIRES_REVIEW", "REFUND_APPROVED"), List.of());
        }

        @Test
        void medicalWithoutDocumentOrEvidenceIsRejected() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 30, 0), RefundReason.MEDICAL,
                    true, 0, false, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("MEDICAL_DOCUMENT_REQUIRED"), List.of());
        }

        @Test
        void upheldComplaintJustUnderSixtyProgressGrantsFullRefund() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 59, 0), RefundReason.CONTENT_QUALITY,
                    true, 0, false, false, true, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("CONTENT_COMPLAINT_UPHELD", "REFUND_APPROVED"), List.of());
        }

        @Test
        void upheldComplaintAtSixtyProgressDropsToSixtyPercent() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 60, 0), RefundReason.CONTENT_QUALITY,
                    false, 60, false, false, true, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "60.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("CONTENT_COMPLAINT_UPHELD", "REFUND_APPROVED"), List.of());
        }

        @Test
        void contentQualityWithoutUpheldComplaintFallsToGeneralPolicy() {
            RefundRequest r = request(purchase(H168, PaymentMethod.CARD, 59, 0), RefundReason.CONTENT_QUALITY,
                    true, 0, false, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());
        }

        @Test
        void certificateIssuedBlocksRefund() {
            PurchaseSnapshot p = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    true, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("CERTIFICATE_ALREADY_ISSUED"), List.of());
        }

        @Test
        void progressEightyBlocksRefundButSeventyNineFallsThrough() {
            RefundDecision at80 = service().evaluate(request(purchase(H721, PaymentMethod.CARD, 80, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at80, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("COURSE_SUBSTANTIALLY_COMPLETED"), List.of());

            RefundDecision at79 = service().evaluate(request(purchase(H721, PaymentMethod.CARD, 79, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at79, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());
        }

        @Test
        void tenDownloadedResourcesBlockRefundButNineStillAllowCoolingOff() {
            RefundDecision at10 = service().evaluate(request(purchase(H1, PaymentMethod.CARD, 0, 10),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at10, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("TOO_MANY_RESOURCES_DOWNLOADED"), List.of());

            RefundDecision at9 = service().evaluate(request(purchase(H1, PaymentMethod.CARD, 0, 9),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at9, true, false, "98.00", "2.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COOLING_OFF_PERIOD", "REFUND_APPROVED"), List.of("RESOURCE_DOWNLOAD_FEE_APPLIED"));
        }

        @Test
        void coolingOffCoversExactlyTwoHoursButThreeHoursShiftsToOneDayRule() {
            RefundDecision at2h = service().evaluate(request(purchase(H2, PaymentMethod.CARD, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at2h, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COOLING_OFF_PERIOD", "REFUND_APPROVED"), List.of());

            RefundDecision at3h = service().evaluate(request(purchase(H3, PaymentMethod.CARD, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at3h, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("ONE_DAY_LOW_USAGE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void coolingOffExcludesNonZeroProgressWithinTwoHours() {
            // hours <= 2 but progress 5 != 0: cooling-off window skipped, one-day rule applies instead.
            RefundDecision d = service().evaluate(request(purchase(H2, PaymentMethod.CARD, 5, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("ONE_DAY_LOW_USAGE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void oneDayRuleCoversFivePercentProgressAndExactly24h() {
            RefundDecision p5at24h = service().evaluate(request(purchase(H24, PaymentMethod.CARD, 5, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(p5at24h, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("ONE_DAY_LOW_USAGE", "REFUND_APPROVED"), List.of());

            RefundDecision p6at24h = service().evaluate(request(purchase(H24, PaymentMethod.CARD, 6, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(p6at24h, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());

            RefundDecision p5at25h = service().evaluate(request(purchase(H25, PaymentMethod.CARD, 5, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(p5at25h, true, false, "97.00", "3.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void sevenDayGuaranteeBoundaries() {
            RefundDecision at168 = service().evaluate(request(purchase(H168, PaymentMethod.CARD, 10, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at168, true, false, "97.00", "3.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());

            RefundDecision at169 = service().evaluate(request(purchase(H169, PaymentMethod.CARD, 10, 0),
                    RefundReason.ACCIDENTAL_PURCHASE, false, 50, false, false, false, false, false, true, false, false));
            assertDecision(at169, true, false, "48.50", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of());

            RefundDecision p11 = service().evaluate(request(purchase(H168, PaymentMethod.CARD, 11, 0),
                    RefundReason.ACCIDENTAL_PURCHASE, false, 70, false, false, false, false, false, true, false, false));
            assertDecision(p11, true, false, "67.90", "2.10", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of());
        }

        @Test
        void fourteenDayBoundaries() {
            RefundDecision at336 = service().evaluate(request(purchase(H336, PaymentMethod.CARD, 30, 0),
                    RefundReason.ACCIDENTAL_PURCHASE, false, 50, false, false, false, false, false, true, false, false));
            assertDecision(at336, true, false, "48.50", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of());

            RefundDecision at337 = service().evaluate(request(purchase(H337, PaymentMethod.CARD, 30, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at337, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());

            RefundDecision p31 = service().evaluate(request(purchase(H336, PaymentMethod.CARD, 31, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(p31, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());
        }

        @Test
        void thirtyDayExceptionBoundaries() {
            RefundDecision at720 = service().evaluate(request(purchase(H720, PaymentMethod.CARD, 15, 0),
                    RefundReason.ACCIDENTAL_PURCHASE, false, 25, false, false, false, false, false, true, false, false));
            assertDecision(at720, true, false, "24.25", "0.75", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("THIRTY_DAY_EXCEPTION", "REFUND_APPROVED"), List.of());

            RefundDecision at721 = service().evaluate(request(purchase(H721, PaymentMethod.CARD, 15, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(at721, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());

            RefundDecision p16 = service().evaluate(request(purchase(H720, PaymentMethod.CARD, 16, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(p16, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());
        }

        @Test
        void accidentalPurchaseAfterDownloadIsRejectedButWithoutDownloadFallsThrough() {
            RefundDecision withDownload = service().evaluate(request(purchase(H721, PaymentMethod.CARD, 50, 1),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(withDownload, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("ACCIDENTAL_PURCHASE_AFTER_DOWNLOAD"), List.of());

            RefundDecision noDownload = service().evaluate(request(purchase(H721, PaymentMethod.CARD, 50, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(noDownload, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());
        }

        @Test
        void otherReasonRequiresEvidence() {
            RefundDecision noEvidence = service().evaluate(request(purchase(H721, PaymentMethod.CARD, 50, 0),
                    RefundReason.OTHER));
            assertDecision(noEvidence, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("OTHER_REASON_REQUIRES_EVIDENCE"), List.of());

            RefundDecision withEvidence = service().evaluate(request(purchase(H721, PaymentMethod.CARD, 50, 0),
                    RefundReason.OTHER, true, 0, true, false, false, false, false, true, false, false));
            assertDecision(withEvidence, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("REFUND_POLICY_NOT_SATISFIED"), List.of());
        }
    }

    @Nested
    class DecisionPathTests {

        @Test
        void purchaseDatedInFutureIsRejected() {
            RefundDecision d = service().evaluate(request(purchase(FUTURE, PaymentMethod.CARD, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("PURCHASE_DATE_IN_FUTURE"), List.of());
        }

        @Test
        void alreadyRefundedPurchaseIsRejected() {
            PurchaseSnapshot p = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, true, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("PURCHASE_ALREADY_REFUNDED"), List.of());
        }

        @Test
        void openChargebackGoesToManualReview() {
            PurchaseSnapshot p = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, true, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, true, "0.00", "0.00", 0, "MANUAL_REVIEW",
                    List.of("OPEN_CHARGEBACK"), List.of());
        }

        @Test
        void fraudReasonGoesToManualReview() {
            RefundDecision d = service().evaluate(request(purchase(H1, PaymentMethod.CARD, 0, 0),
                    RefundReason.FRAUD));
            assertDecision(d, false, true, "0.00", "0.00", 0, "MANUAL_REVIEW",
                    List.of("FRAUD_REVIEW_REQUIRED"), List.of());
        }

        @Test
        void fraudSuspicionOnAnyReasonGoesToManualReview() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 0, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    true, 0, false, false, false, false, false, true, true, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, false, true, "0.00", "0.00", 0, "MANUAL_REVIEW",
                    List.of("FRAUD_REVIEW_REQUIRED"), List.of());
        }

        @Test
        void expiredAccessAddsWarningButApprovalContinues() {
            PurchaseSnapshot expired = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false,
                    NOW.minusSeconds(1800));
            RefundDecision d = service().evaluate(request(expired, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COOLING_OFF_PERIOD", "REFUND_APPROVED"), List.of("COURSE_ACCESS_EXPIRED"));
        }

        @Test
        void accessExpiringExactlyNowOrLaterAddsNoWarning() {
            PurchaseSnapshot exactlyNow = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, NOW);
            RefundDecision nowDecision = service().evaluate(request(exactlyNow, RefundReason.ACCIDENTAL_PURCHASE));
            assertTrue(nowDecision.warnings().isEmpty(), "expires exactly now must not warn: " + nowDecision.warnings());

            PurchaseSnapshot later = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false,
                    NOW.plusSeconds(1800));
            RefundDecision futureDecision = service().evaluate(request(later, RefundReason.ACCIDENTAL_PURCHASE));
            assertTrue(futureDecision.warnings().isEmpty(), "future expiry must not warn");
        }

        @Test
        void adminOverrideRescuesZeroRateWithFullRefund() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 80, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    true, 0, false, false, false, false, false, true, false, true);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COURSE_SUBSTANTIALLY_COMPLETED", "REFUND_APPROVED"), List.of("ADMIN_OVERRIDE_APPLIED"));
        }

        @Test
        void adminOverrideWithoutFullRequestDefaultsToFiftyPercent() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 80, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    false, 0, false, false, false, false, false, true, false, true);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "50.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COURSE_SUBSTANTIALLY_COMPLETED", "REFUND_APPROVED"), List.of("ADMIN_OVERRIDE_APPLIED"));
        }

        @Test
        void adminOverrideHonorsExplicitRequestedPercentage() {
            RefundRequest r = request(purchase(H1, PaymentMethod.CARD, 80, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    false, 25, false, false, false, false, false, true, false, true);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "25.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COURSE_SUBSTANTIALLY_COMPLETED", "REFUND_APPROVED"), List.of("ADMIN_OVERRIDE_APPLIED"));

            RefundRequest r100 = request(purchase(H1, PaymentMethod.CARD, 80, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    false, 100, false, false, false, false, false, true, false, true);
            RefundDecision d100 = service().evaluate(r100);
            assertDecision(d100, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COURSE_SUBSTANTIALLY_COMPLETED", "REFUND_APPROVED"), List.of("ADMIN_OVERRIDE_APPLIED"));
        }

        @Test
        void adminOverrideOnNonzeroRateSkipsResetAndWaivesFee() {
            // 14-day partial rate 0.50; override does not reset it, and no processing fee is charged.
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 30, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    false, 0, false, false, false, false, false, true, false, true);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "50.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of("ADMIN_OVERRIDE_APPLIED"));
        }

        @Test
        void fullRefundRequestAbovePolicyLimitIsReducedWithWarning() {
            RefundDecision d = service().evaluate(request(purchase(H336, PaymentMethod.CARD, 30, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "48.50", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"),
                    List.of("REQUESTED_PERCENTAGE_REDUCED_TO_POLICY_LIMIT"));
        }

        @Test
        void requestedPercentageWithinLimitIsHonoredWithoutWarning() {
            RefundRequest r = request(purchase(H336, PaymentMethod.CARD, 30, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    false, 25, false, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "24.25", "0.75", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of());

            RefundRequest r50 = request(purchase(H336, PaymentMethod.CARD, 30, 0), RefundReason.ACCIDENTAL_PURCHASE,
                    false, 50, false, false, false, false, false, true, false, false);
            RefundDecision d50 = service().evaluate(r50);
            assertDecision(d50, true, false, "48.50", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of());
        }

        @Test
        void zeroRateWithoutOverrideKeepsDeterminationReasons() {
            RefundDecision d = service().evaluate(request(purchase(H168, PaymentMethod.CARD, 80, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("COURSE_SUBSTANTIALLY_COMPLETED"), List.of());
        }
    }

    @Nested
    class FeeRouteAndAdjustmentTests {

        @Test
        void cardFeeIsThreePercentBeyond24h() {
            RefundDecision d = service().evaluate(request(purchase(H25, PaymentMethod.CARD, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "97.00", "3.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void alipayAndWechatFeeIsOnePercentBeyond24h() {
            RefundDecision alipay = service().evaluate(request(purchase(H25, PaymentMethod.ALIPAY, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(alipay, true, false, "99.00", "1.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());

            RefundDecision wechat = service().evaluate(request(purchase(H25, PaymentMethod.WECHAT, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(wechat, true, false, "99.00", "1.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void bankTransferFeeIsTwoPercentWithBankDetails() {
            RefundDecision d = service().evaluate(request(purchase(H25, PaymentMethod.BANK_TRANSFER, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "98.00", "2.00", 0, "BANK_TRANSFER",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void walletAndGiftChargesNoFeeAndRoutesToWalletCredit() {
            RefundDecision wallet = service().evaluate(request(purchase(H25, PaymentMethod.WALLET, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(wallet, true, false, "100.00", "0.00", 0, "WALLET_CREDIT",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());

            RefundDecision gift = service().evaluate(request(purchase(H25, PaymentMethod.GIFT, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(gift, true, false, "100.00", "0.00", 0, "WALLET_CREDIT",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of("GIFT_REFUND_ISSUED_AS_CREDIT"));
        }

        @Test
        void cryptoFeeIsFivePercentAndForcesManualReview() {
            RefundDecision d = service().evaluate(request(purchase(H25, PaymentMethod.CRYPTO, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, true, "95.00", "5.00", 0, "MANUAL_REVIEW",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"),
                    List.of("CRYPTO_REFUND_REQUIRES_MANUAL_REVIEW"));
        }

        @Test
        void feeIsZeroedWithin24hEvenForCard() {
            RefundDecision d = service().evaluate(request(purchase(H24, PaymentMethod.CARD, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("ONE_DAY_LOW_USAGE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void downloadFeeAppliesEvenWithin24hAndStacksBeyond24h() {
            RefundDecision within24 = service().evaluate(request(purchase(H24, PaymentMethod.CARD, 0, 1),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(within24, true, false, "98.00", "2.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("ONE_DAY_LOW_USAGE", "REFUND_APPROVED"), List.of("RESOURCE_DOWNLOAD_FEE_APPLIED"));

            RefundDecision beyond24 = service().evaluate(request(purchase(H25, PaymentMethod.CARD, 0, 1),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(beyond24, true, false, "95.00", "5.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of("RESOURCE_DOWNLOAD_FEE_APPLIED"));
        }

        @Test
        void liveSessionAttendanceFeeBoundaries() {
            // 2/4 = 0.5000 -> full attendance fee (+0.10) on top of card 0.03 => 0.13 of 100.00.
            RefundDecision full = service().evaluate(request(
                    snapshot(H25, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                            false, 2, 4, 0, false, false, false, false, false, false, null),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(full, true, false, "87.00", "13.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"),
                    List.of("LIVE_SESSION_ATTENDANCE_FEE_APPLIED"));

            // 4/10 = 0.4000 -> partial fee (+0.05) => rate 0.08.
            RefundDecision partial = service().evaluate(request(
                    snapshot(H25, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                            false, 4, 10, 0, false, false, false, false, false, false, null),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(partial, true, false, "92.00", "8.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"),
                    List.of("PARTIAL_LIVE_ATTENDANCE_FEE_APPLIED"));

            // 1/5 = 0.2000 exactly -> still partial fee (>= 0.20).
            RefundDecision atBoundary = service().evaluate(request(
                    snapshot(H25, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                            false, 1, 5, 0, false, false, false, false, false, false, null),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(atBoundary, true, false, "92.00", "8.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"),
                    List.of("PARTIAL_LIVE_ATTENDANCE_FEE_APPLIED"));

            // 1/10 = 0.1000 -> below 0.20, no session fee.
            RefundDecision none = service().evaluate(request(
                    snapshot(H25, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                            false, 1, 10, 0, false, false, false, false, false, false, null),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(none, true, false, "97.00", "3.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void giftPurchaseFlagRoutesToWalletCreditEvenForCard() {
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 0, true, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "97.00", "3.00", 0, "WALLET_CREDIT",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of("GIFT_REFUND_ISSUED_AS_CREDIT"));
        }

        @Test
        void bankTransferWithoutBankDetailsGoesToManualReview() {
            RefundRequest r = request(purchase(H25, PaymentMethod.BANK_TRANSFER, 0, 0),
                    RefundReason.ACCIDENTAL_PURCHASE, true, 0, false, false, false, false, false,
                    false, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, false, true, "98.00", "2.00", 0, "MANUAL_REVIEW",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of("BANK_DETAILS_REQUIRED"));
        }

        @Test
        void couponValueIsDeductedWhenRefundCoversIt() {
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "100.00", "0.00", "20.00",
                    false, 0, 0, 0, false, true, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 50, false, false, false, false, false, true, false, false));
            assertDecision(d, true, false, "38.50", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of("COUPON_VALUE_NOT_REFUNDED"));
        }

        @Test
        void couponLargerThanRefundConsumesItAll() {
            // paid 40.00, rate 0.50 -> gross 20.00, card fee 0.60, refundable 19.40;
            // coupon adjustment 40.00*0.50 = 20.00 >= 19.40 -> refundable 0.
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "40.00", "0.00", "40.00",
                    false, 0, 0, 0, false, true, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 50, false, false, false, false, false, true, false, false));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("NO_REFUNDABLE_VALUE"), List.of("COUPON_VALUE_CONSUMED_REFUND"));
        }

        @Test
        void couponIsIgnoredForProviderFaultRefunds() {
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "100.00", "0.00", "20.00",
                    false, 0, 0, 0, false, true, false, false, true, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("PROVIDER_CANCELLED_COURSE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void loyaltyPointsReturnProportionallyWithWarning() {
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 100, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 50, false, false, false, false, false, true, false, false));
            assertDecision(d, true, false, "48.50", "1.50", 50, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"),
                    List.of("LOYALTY_POINTS_RETURNED_PROPORTIONALLY"));
        }

        @Test
        void fullRateReturnsAllPointsWithoutWarning() {
            PurchaseSnapshot p = snapshot(H1, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 100, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 100, "ORIGINAL_PAYMENT_METHOD",
                    List.of("COOLING_OFF_PERIOD", "REFUND_APPROVED"), List.of());
        }

        @Test
        void pointReturnFloorsDownward() {
            // 0.50 * 99 = 49.5 -> RoundingMode.DOWN -> 49.
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 99, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertEquals(49, d.loyaltyPointsReturned(), "points must floor, not round up");
            assertTrue(d.warnings().contains("LOYALTY_POINTS_RETURNED_PROPORTIONALLY"));
        }

        @Test
        void couponOrderHalvesReturnedPoints() {
            // rate 0.50 * 100 = 50 -> halved to 25; coupon adjustment 10.00*0.50 = 5.00 < refundable 47.00.
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "100.00", "0.00", "10.00",
                    false, 0, 0, 100, false, true, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 50, false, false, false, false, false, true, false, false));
            assertDecision(d, true, false, "43.50", "1.50", 25, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"),
                    List.of("COUPON_VALUE_NOT_REFUNDED", "LOYALTY_POINTS_RETURNED_PROPORTIONALLY",
                            "POINT_RETURN_REDUCED_FOR_COUPON_ORDER"));
        }

        @Test
        void smallRefundToWalletIsPaidBelowMinimum() {
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.WALLET, 0, 0, "0.50", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "0.50", "0.00", 0, "WALLET_CREDIT",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"),
                    List.of("SMALL_REFUND_RETURNED_TO_WALLET"));
        }

        @Test
        void smallNonWalletRefundIsAbsorbedIntoFeeAndRejected() {
            // paid 1.00, card fee 0.03 -> refundable 0.97 < 1.00 -> fee absorbs it -> NO_REFUNDABLE_VALUE.
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.CARD, 0, 0, "1.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("NO_REFUNDABLE_VALUE"), List.of("REFUND_BELOW_MINIMUM_PAYOUT"));
        }

        @Test
        void refundOfExactlyOnePayoutUnitIsPaid() {
            // paid 1.03, fee 0.03 -> refundable exactly 1.00, no small-refund warning.
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.CARD, 0, 0, "1.03", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "1.00", "0.03", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void zeroRefundableAfterFeeIsRejected() {
            // paid 2.00, rate 0.25 -> gross 0.50, fee 0.06 -> 0.44 < 1.00 -> absorbed -> 0.
            PurchaseSnapshot p = snapshot(H720, PaymentMethod.CARD, 15, 0, "2.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 25, false, false, false, false, false, true, false, false));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("NO_REFUNDABLE_VALUE"), List.of("REFUND_BELOW_MINIMUM_PAYOUT"));
        }

        @Test
        void cryptoSmallRefundStaysInManualReviewInsteadOfRejecting() {
            // paid 1.00, crypto fee 0.05 -> refundable 0.95 < 1.00 -> absorbed, but
            // MANUAL_REVIEW route short-circuits the NO_REFUNDABLE_VALUE rejection.
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.CRYPTO, 0, 0, "1.00", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, false, true, "0.00", "1.00", 0, "MANUAL_REVIEW",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"),
                    List.of("CRYPTO_REFUND_REQUIRES_MANUAL_REVIEW", "REFUND_BELOW_MINIMUM_PAYOUT"));
        }

        @Test
        void manualReviewWithPositiveRefundAndPointsSkipsZeroValueRejection() {
            // refundable 98.00 > 0 and points 100 > 0 with MANUAL_REVIEW route: the
            // NO_REFUNDABLE_VALUE guard is skipped on both conditions.
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.BANK_TRANSFER, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 0, 100, false, false, false, false, false, false, null);
            RefundRequest r = request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    true, 0, false, false, false, false, false, false, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, false, true, "98.00", "2.00", 100, "MANUAL_REVIEW",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of("BANK_DETAILS_REQUIRED"));
        }

        @Test
        void absorbedSmallRefundStillReturnsPointsInsteadOfRejecting() {
            // paid 1.00, fee 0.03 -> refundable 0.97 < 1.00 -> absorbed to 0.00; because
            // points 100 > 0 the NO_REFUNDABLE_VALUE rejection is skipped and the decision
            // is approved with zero cash but full points.
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.CARD, 0, 0, "1.00", "0.00", "0.00",
                    false, 0, 0, 100, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "0.00", "1.00", 100, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"),
                    List.of("REFUND_BELOW_MINIMUM_PAYOUT"));
        }

        @Test
        void zeroAttendanceWithScheduledSessionsChargesNoSessionFee() {
            // totalLiveSessions 10 > 0 but attended 0: attendance-fee block never entered.
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.CARD, 0, 0, "100.00", "0.00", "0.00",
                    false, 0, 10, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "97.00", "3.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void couponOrderWithProviderFaultKeepsFullPointReturn() {
            // couponUsed but instructor cancelled (provider fault): coupon adjustment and
            // point halving are both skipped; full rate 1.00 returns all 100 points.
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "100.00", "0.00", "20.00",
                    false, 0, 0, 100, false, true, false, false, true, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 100, false, false, false, false, false, true, false, false));
            assertDecision(d, true, false, "100.00", "0.00", 100, "ORIGINAL_PAYMENT_METHOD",
                    List.of("PROVIDER_CANCELLED_COURSE", "REFUND_APPROVED"), List.of());
        }
    }

    @Nested
    class RefundReasonEndToEndTests {

        @Test
        void endToEndAccidentalPurchase() {
            RefundDecision d = service().evaluate(request(purchase(H3, PaymentMethod.CARD, 4, 0),
                    RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("ONE_DAY_LOW_USAGE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void endToEndTechnicalIssue() {
            RefundRequest r = request(purchase(H25, PaymentMethod.CARD, 50, 0), RefundReason.TECHNICAL_ISSUE,
                    true, 0, false, true, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("CONFIRMED_TECHNICAL_ISSUE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void endToEndContentQuality() {
            RefundRequest r = request(purchase(H25, PaymentMethod.CARD, 10, 0), RefundReason.CONTENT_QUALITY,
                    true, 0, false, false, true, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("CONTENT_COMPLAINT_UPHELD", "REFUND_APPROVED"), List.of());
        }

        @Test
        void endToEndMedical() {
            RefundRequest r = request(purchase(H25, PaymentMethod.CARD, 20, 0), RefundReason.MEDICAL,
                    true, 0, false, false, false, true, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "97.00", "3.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("VERIFIED_MEDICAL_EXCEPTION", "REFUND_APPROVED"), List.of());
        }

        @Test
        void endToEndCourseCancelled() {
            RefundDecision d = service().evaluate(request(purchase(H25, PaymentMethod.CARD, 10, 0),
                    RefundReason.COURSE_CANCELLED));
            assertDecision(d, true, false, "100.00", "0.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("PROVIDER_CANCELLED_COURSE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void endToEndDuplicatePayment() {
            RefundRequest r = request(purchase(H25, PaymentMethod.CARD, 10, 0), RefundReason.DUPLICATE_PAYMENT,
                    false, 50, true, false, false, false, false, true, false, false);
            RefundDecision d = service().evaluate(r);
            assertDecision(d, true, false, "48.50", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("DUPLICATE_PAYMENT_REQUIRES_REVIEW", "REFUND_APPROVED"), List.of());
        }

        @Test
        void endToEndFraud() {
            RefundDecision d = service().evaluate(request(purchase(H1, PaymentMethod.CARD, 0, 0),
                    RefundReason.FRAUD));
            assertDecision(d, false, true, "0.00", "0.00", 0, "MANUAL_REVIEW",
                    List.of("FRAUD_REVIEW_REQUIRED"), List.of());
        }

        @Test
        void endToEndOther() {
            RefundDecision d = service().evaluate(request(purchase(H721, PaymentMethod.WECHAT, 50, 0),
                    RefundReason.OTHER));
            assertDecision(d, false, false, "0.00", "0.00", 0, "NONE",
                    List.of("OTHER_REASON_REQUIRES_EVIDENCE"), List.of());
        }
    }

    @Nested
    class RoundingTests {

        @Test
        void grossRefundUsesHalfUp() {
            // 0.03 * 0.50 = 0.0150 -> HALF_UP -> 0.02 (not 0.01).
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.WALLET, 30, 0, "0.03", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 50, false, false, false, false, false, true, false, false));
            assertDecision(d, true, false, "0.02", "0.00", 0, "WALLET_CREDIT",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"),
                    List.of("SMALL_REFUND_RETURNED_TO_WALLET"));
        }

        @Test
        void processingFeeUsesHalfUp() {
            // 33.33 * 0.03 = 0.9999 -> HALF_UP -> 1.00; refundable 33.33 - 1.00 = 32.33.
            PurchaseSnapshot p = snapshot(H25, PaymentMethod.CARD, 0, 0, "33.33", "0.00", "0.00",
                    false, 0, 0, 0, false, false, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE));
            assertDecision(d, true, false, "32.33", "1.00", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("SEVEN_DAY_GUARANTEE", "REFUND_APPROVED"), List.of());
        }

        @Test
        void couponAdjustmentUsesHalfUp() {
            // coupon adjustment = 0.03 * 0.50 = 0.0150 -> HALF_UP -> 0.02; 47.00 - 0.02 = 46.98.
            PurchaseSnapshot p = snapshot(H336, PaymentMethod.CARD, 30, 0, "100.00", "0.00", "0.03",
                    false, 0, 0, 0, false, true, false, false, false, false, null);
            RefundDecision d = service().evaluate(request(p, RefundReason.ACCIDENTAL_PURCHASE,
                    false, 50, false, false, false, false, false, true, false, false));
            assertDecision(d, true, false, "48.48", "1.50", 0, "ORIGINAL_PAYMENT_METHOD",
                    List.of("FOURTEEN_DAY_PARTIAL_REFUND", "REFUND_APPROVED"), List.of("COUPON_VALUE_NOT_REFUNDED"));
        }
    }

    @Nested
    class RefundDecisionRecordTests {

        @Test
        void decisionCopiesReasonAndWarningListsDefensively() {
            List<String> reasons = new ArrayList<>(List.of("R1"));
            List<String> warnings = new ArrayList<>(List.of("W1"));
            RefundDecision d = new RefundDecision(true, false, new BigDecimal("1.00"),
                    new BigDecimal("0.00"), 0, "ORIGINAL_PAYMENT_METHOD", reasons, warnings);

            reasons.add("MUTANT");
            warnings.add("MUTANT");
            assertEquals(List.of("R1"), d.reasons(), "reasons must be snapshotted");
            assertEquals(List.of("W1"), d.warnings(), "warnings must be snapshotted");

            assertThrows(UnsupportedOperationException.class, () -> d.reasons().add("X"));
            assertThrows(UnsupportedOperationException.class, () -> d.warnings().add("X"));
        }

        @Test
        void decisionRejectsNullLists() {
            assertThrows(NullPointerException.class, () -> new RefundDecision(true, false,
                    new BigDecimal("1.00"), new BigDecimal("0.00"), 0, "ORIGINAL_PAYMENT_METHOD", null, List.of()));
            assertThrows(NullPointerException.class, () -> new RefundDecision(true, false,
                    new BigDecimal("1.00"), new BigDecimal("0.00"), 0, "ORIGINAL_PAYMENT_METHOD", List.of(), null));
        }
    }
}
