package net.mooctest;

import net.mooctest.expression.engine.NumberUtils;
import net.mooctest.refund.domain.PaymentMethod;
import net.mooctest.refund.domain.PurchaseSnapshot;
import net.mooctest.refund.domain.RefundDecision;
import net.mooctest.refund.domain.RefundReason;
import net.mooctest.refund.domain.RefundRequest;
import net.mooctest.refund.service.RefundPolicyService;
import net.mooctest.scheduling.domain.CourseKind;
import net.mooctest.scheduling.domain.DeliveryMode;
import net.mooctest.scheduling.domain.ScheduleRequest;
import net.mooctest.scheduling.domain.SchedulingDecision;
import net.mooctest.scheduling.service.SchedulingService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MathProbeTest {

    private static final Instant MP_NOW = Instant.parse("2026-06-01T00:00:00Z");

    private static Clock mpClock() {
        return Clock.fixed(MP_NOW, ZoneOffset.UTC);
    }

    private static PurchaseSnapshot mpPurchase(
            BigDecimal paidAmount,
            long paidHoursAgo,
            int progressPercent,
            PaymentMethod method,
            int attended,
            int total
    ) {
        return new PurchaseSnapshot(
                "P-1", "S-1", "C-1",
                paidAmount,
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                MP_NOW.minus(Duration.ofHours(paidHoursAgo)),
                method,
                progressPercent,
                0,
                false,
                attended,
                total,
                false,
                false,
                0,
                false,
                false,
                false,
                false,
                null
        );
    }

    private static RefundRequest mpRefund(PurchaseSnapshot purchase) {
        return new RefundRequest(
                purchase,
                RefundReason.OTHER,
                false,
                0,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );
    }

    @Test
    void refundSevenDayPartialWindowRightEdge() {
        // 168h 整：应命中 7 天部分退款窗口（0.70），而不是滑落到 14 天窗口（0.50）
        PurchaseSnapshot purchase = mpPurchase(
                new BigDecimal("100.00"), 168L, 20, PaymentMethod.CARD, 0, 0);
        RefundDecision decision = new RefundPolicyService(mpClock()).evaluate(mpRefund(purchase));
        assertTrue(decision.approved());
        assertTrue(decision.reasons().contains("SEVEN_DAY_PARTIAL_REFUND"));
        assertEquals(new BigDecimal("70.00"), decision.refundableAmount().add(decision.processingFee()));
        assertEquals(new BigDecimal("67.90"), decision.refundableAmount());
        assertEquals(new BigDecimal("2.10"), decision.processingFee());
    }

    @Test
    void refundFourteenDayPartialWindowRightEdge() {
        // 336h 整：应命中 14 天部分退款窗口（0.50）
        PurchaseSnapshot purchase = mpPurchase(
                new BigDecimal("100.00"), 336L, 20, PaymentMethod.CARD, 0, 0);
        RefundDecision decision = new RefundPolicyService(mpClock()).evaluate(mpRefund(purchase));
        assertTrue(decision.approved());
        assertTrue(decision.reasons().contains("FOURTEEN_DAY_PARTIAL_REFUND"));
        assertEquals(new BigDecimal("48.50"), decision.refundableAmount());
        assertEquals(new BigDecimal("1.50"), decision.processingFee());
    }

    @Test
    void refundThirtyDayExceptionWindowRightEdge() {
        // 720h 整：应命中 30 天例外窗口（0.25）
        PurchaseSnapshot purchase = mpPurchase(
                new BigDecimal("100.00"), 720L, 12, PaymentMethod.CARD, 0, 0);
        RefundDecision decision = new RefundPolicyService(mpClock()).evaluate(mpRefund(purchase));
        assertTrue(decision.approved());
        assertTrue(decision.reasons().contains("THIRTY_DAY_EXCEPTION"));
        assertEquals(new BigDecimal("24.25"), decision.refundableAmount());
        assertEquals(new BigDecimal("0.75"), decision.processingFee());
    }

    @Test
    void refundSevenDayPartialProgressEdge() {
        // 7 天部分退款窗口的进度上边界：progress==30 仍应给 0.70
        PurchaseSnapshot purchase = mpPurchase(
                new BigDecimal("100.00"), 168L, 30, PaymentMethod.CARD, 0, 0);
        RefundDecision decision = new RefundPolicyService(mpClock()).evaluate(mpRefund(purchase));
        assertTrue(decision.approved());
        assertTrue(decision.reasons().contains("SEVEN_DAY_PARTIAL_REFUND"));
        assertEquals(new BigDecimal("67.90"), decision.refundableAmount());
    }

    @Test
    void refundFourteenDayProgressEdge() {
        // 14 天部分退款窗口的进度上边界：progress==30 仍应给 0.50
        PurchaseSnapshot purchase = mpPurchase(
                new BigDecimal("100.00"), 200L, 30, PaymentMethod.CARD, 0, 0);
        RefundDecision decision = new RefundPolicyService(mpClock()).evaluate(mpRefund(purchase));
        assertTrue(decision.approved());
        assertTrue(decision.reasons().contains("FOURTEEN_DAY_PARTIAL_REFUND"));
        assertEquals(new BigDecimal("48.50"), decision.refundableAmount());
    }

    @Test
    void refundThirtyDayProgressEdge() {
        // 30 天例外窗口的进度上边界：progress==15 仍应给 0.25
        PurchaseSnapshot purchase = mpPurchase(
                new BigDecimal("100.00"), 720L, 15, PaymentMethod.CARD, 0, 0);
        RefundDecision decision = new RefundPolicyService(mpClock()).evaluate(mpRefund(purchase));
        assertTrue(decision.approved());
        assertTrue(decision.reasons().contains("THIRTY_DAY_EXCEPTION"));
        assertEquals(new BigDecimal("24.25"), decision.refundableAmount());
    }

    @Test
    void refundFeeEqualsZeroGrossAddsNoCapWarning() {
        // 0 元支付单：手续费（0.00）与毛退款（0.00）相等，不得因此触发“手续费封顶”告警
        PurchaseSnapshot purchase = mpPurchase(
                new BigDecimal("0.00"), 1L, 0, PaymentMethod.CARD, 0, 0);
        RefundDecision decision = new RefundPolicyService(mpClock()).evaluate(mpRefund(purchase));
        assertFalse(decision.warnings().contains("PROCESSING_FEE_LIMITED_TO_REFUND_AMOUNT"));
    }

    @Test
    void refundProcessingFeeIgnoresZeroTotalSessions() throws Exception {
        // 畸形快照（attended=5 > total=0）：真实逻辑因 totalLiveSessions()>0 不成立而跳过出勤费率，
        // 不得进入 5/0 的除法分支。
        PurchaseSnapshot purchase = mpPurchase(
                new BigDecimal("100.00"), 48L, 0, PaymentMethod.CARD, 5, 0);
        RefundRequest request = mpRefund(purchase);
        Method m = RefundPolicyService.class.getDeclaredMethod(
                "calculateProcessingFee",
                RefundRequest.class,
                BigDecimal.class,
                long.class,
                List.class);
        m.setAccessible(true);
        List<String> warnings = new ArrayList<>();
        Object fee = m.invoke(new RefundPolicyService(mpClock()),
                request, new BigDecimal("100.00"), 48L, warnings);
        assertEquals(new BigDecimal("3.00"), fee);
    }

    @Test
    void numberSumOfRangeSameExtremeDouble() {
        assertEquals(Double.MAX_VALUE,
                NumberUtils.sumOfRange(Double.MAX_VALUE, Double.MAX_VALUE),
                0.0d);
    }

    @Test
    void numberSumOfRangeWithNaNStepSameEndpoints() {
        // start == end 时无论步长如何都应直接返回端点值
        assertEquals(5.0d, NumberUtils.sumOfRange(5.0d, 5.0d, Double.NaN), 0.0d);
    }

    @Test
    void numberMultiplyOfRangeWithNaNStepSameEndpoints() {
        assertEquals(5.0d, NumberUtils.MultiplyOfRange(5.0d, 5.0d, Double.NaN), 0.0d);
    }

    private static ScheduleRequest mpHybridRequest(String roomId) {
        Clock clock = Clock.fixed(Instant.parse("2026-11-15T08:00:00Z"), ZoneOffset.UTC);
        return new ScheduleRequest(
                "C-1", "CO-1", "I-1",
                DeliveryMode.HYBRID,
                CourseKind.LECTURE,
                Instant.parse("2026-12-01T02:00:00Z"),
                Instant.parse("2026-12-01T03:30:00Z"),
                "Asia/Shanghai",
                roomId,
                "https://meeting.example.com/x",
                10, 10, 20,
                Set.of(), Set.of(),
                false, false,
                true, false, true,
                false, 1, 30, 4,
                List.of(), List.of(), List.of(), Set.of(),
                false, false, true
        );
    }

    @Test
    void schedulingHybridWithoutRoomReportsConflictInsteadOfNullPointer() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-15T08:00:00Z"), ZoneOffset.UTC);
        SchedulingDecision decision = new SchedulingService(clock).evaluate(mpHybridRequest(null));
        assertNotNull(decision);
        assertTrue(decision.conflicts().contains("HYBRID_ROOM_REQUIRED"),
                "conflicts=" + decision.conflicts());
    }

    @Test
    void schedulingHybridWithBlankRoomReportsConflict() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-15T08:00:00Z"), ZoneOffset.UTC);
        SchedulingDecision decision = new SchedulingService(clock).evaluate(mpHybridRequest("   "));
        assertNotNull(decision);
        assertTrue(decision.conflicts().contains("HYBRID_ROOM_REQUIRED"),
                "conflicts=" + decision.conflicts());
    }

}
