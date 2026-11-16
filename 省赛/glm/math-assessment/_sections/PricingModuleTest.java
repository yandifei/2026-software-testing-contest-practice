package net.mooctest;

import net.mooctest.error.BusinessException;
import net.mooctest.error.ErrorCode;
import net.mooctest.pricing.domain.CouponType;
import net.mooctest.pricing.domain.MembershipLevel;
import net.mooctest.pricing.domain.PriceBreakdown;
import net.mooctest.pricing.domain.PriceRequest;
import net.mooctest.pricing.domain.PromotionType;
import net.mooctest.pricing.service.AdvancedPricingService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PricingModuleTest for the advanced pricing module (pricing.service.AdvancedPricingService
 * + pricing.domain.*).
 *
 * <p>组织约定（与 StudentTest 一致）:</p>
 * <ul>
 *   <li>每个 {@code @Nested} 类对应一组业务分支（Validation/Membership/Bulk/Promotion/Coupon/
 *       FirstPurchase/Nonprofit/Loyalty/Tax/Cap/Breakdown），每个条件分支的两侧都有断言。</li>
 *   <li>期望值全部为硬编码字符串（{@code money() = setScale(2, HALF_UP)}），禁止在测试里
 *       重复实现计价公式；BigDecimal 一律经 {@link #assertMoney} 以 {@code toPlainString()}
 *       比较，规避 scale 相等性陷阱。</li>
 *   <li>固定时钟 {@code 2026-06-15T02:00:00Z (UTC)}，即 {@code LocalDate.now(clock)}
 *       恒为 2026-06-15（周一）；日期窗口用 06-14/06-15/06-16 三个邻居日压边界。</li>
 *   <li>白盒测试（仅 2 个）: promotion/coupon 的 "date range is invalid" 守卫与固定时钟
 *       在逻辑上矛盾（startsOn &gt; endsOn 时，"has not started" 或 "has expired" 必然先触发），
 *       真实 record 无法到达该分支；通过按调用序返回不一致日期的 stub record 执行真实字节码，
 *       并断言真实告警输出。不篡改任何报告。</li>
 *   <li>已知不可达分支（记录备查，共 2 个，均为真实死代码）:
 *       (a) {@code calculate} 非叠加互斥条件的 {@code request.coupon() != null} false 侧 ——
 *       coupon 为 null 时 couponDiscount 必为 0，前一条件已短路；
 *       (b) {@code trimDiscounts} 循环条件 {@code index >= 0} 的 false 侧。excess =
 *       totalDiscount - cap，且 cap &ge; subtotal*0.70 &ge; membershipDiscount（INSTRUCTOR
 *       全额时 cap = subtotal = membershipDiscount），修剪完 1..5 号折扣后 excess 必已归零，
 *       循环永远经由 {@code excess.signum() > 0} 退出。</li>
 * </ul>
 */
class PricingModuleTest {

    private static final Instant NOW = Instant.parse("2026-06-15T02:00:00Z");
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final Clock PRICING_CLOCK = Clock.fixed(NOW, UTC);

    /** 无状态服务（仅持有不可变 Clock），全套件安全共享。 */
    private static final AdvancedPricingService SERVICE = new AdvancedPricingService(PRICING_CLOCK);

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final LocalDate TOMORROW = LocalDate.of(2026, 6, 16);
    private static final LocalDate YESTERDAY = LocalDate.of(2026, 6, 14);

    // ------------------------------------------------------------------
    // 请求 / 券 / 活动工厂（字段全显式，验证类测试可单字段变形）
    // ------------------------------------------------------------------

    private static BigDecimal amount(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static PriceRequest req(String courseId, String unitPrice, int quantity,
                                    MembershipLevel membershipLevel,
                                    net.mooctest.pricing.domain.Coupon coupon,
                                    net.mooctest.pricing.domain.Promotion promotion,
                                    boolean firstPurchase, boolean nonprofitCustomer, boolean taxExempt,
                                    boolean instructorOwnedCourse, int loyaltyPointsToUse,
                                    int availableLoyaltyPoints, String countryCode, String currency) {
        return new PriceRequest(courseId, amount(unitPrice), quantity, membershipLevel, coupon, promotion,
                firstPurchase, nonprofitCustomer, taxExempt, instructorOwnedCourse,
                loyaltyPointsToUse, availableLoyaltyPoints, countryCode, currency);
    }

    private static PriceRequest request(String unitPrice, int quantity) {
        return request(unitPrice, quantity, MembershipLevel.NONE);
    }

    private static PriceRequest request(String unitPrice, int quantity, MembershipLevel membershipLevel) {
        return req("course-1", unitPrice, quantity, membershipLevel, null, null,
                false, false, false, false, 0, 0, "CN", "CNY");
    }

    private static PriceRequest request(String unitPrice, int quantity, String countryCode, String currency) {
        return req("course-1", unitPrice, quantity, MembershipLevel.NONE, null, null,
                false, false, false, false, 0, 0, countryCode, currency);
    }

    private static PriceRequest request(String unitPrice, int quantity, MembershipLevel membershipLevel,
                                        net.mooctest.pricing.domain.Coupon coupon,
                                        net.mooctest.pricing.domain.Promotion promotion) {
        return req("course-1", unitPrice, quantity, membershipLevel, coupon, promotion,
                false, false, false, false, 0, 0, "CN", "CNY");
    }

    private static PriceRequest request(String unitPrice, int quantity, MembershipLevel membershipLevel,
                                        net.mooctest.pricing.domain.Coupon coupon,
                                        net.mooctest.pricing.domain.Promotion promotion,
                                        boolean firstPurchase,
                                        boolean nonprofitCustomer, boolean instructorOwnedCourse,
                                        int loyaltyPointsToUse, int availableLoyaltyPoints) {
        return req("course-1", unitPrice, quantity, membershipLevel, coupon, promotion,
                firstPurchase, nonprofitCustomer, false, instructorOwnedCourse,
                loyaltyPointsToUse, availableLoyaltyPoints, "CN", "CNY");
    }

    private static net.mooctest.pricing.domain.Coupon coupon(
            String code, CouponType type, String value, String minimumSubtotal,
            LocalDate validFrom, LocalDate validUntil, int remainingUses,
            boolean stackable, Set<String> eligibleCourseIds,
            MembershipLevel minimumMembership, int maximumQuantity) {
        return new net.mooctest.pricing.domain.Coupon(code, type, amount(value), amount(minimumSubtotal),
                validFrom, validUntil, remainingUses, stackable, eligibleCourseIds,
                minimumMembership, maximumQuantity);
    }

    /** 默认可用券: code SAVE-10, 剩余 5 次, 可叠加, 无窗口/门槛/限量。 */
    private static net.mooctest.pricing.domain.Coupon coupon(CouponType type, String value) {
        return coupon("SAVE-10", type, value, null, null, null, 5, true, Set.of(), null, 0);
    }

    private static net.mooctest.pricing.domain.Coupon coupon(CouponType type, String value, boolean stackable) {
        return coupon("SAVE-10", type, value, null, null, null, 5, stackable, Set.of(), null, 0);
    }

    /** 仅变 code/type/value 的券（其余字段取默认）。 */
    private static net.mooctest.pricing.domain.Coupon coupon(String code, CouponType type, String value) {
        return coupon(code, type, value, null, null, null, 5, true, Set.of(), null, 0);
    }

    /** 默认活动: name "Summer Sale", minimumQuantity 1, 无窗口/上限。 */
    private static net.mooctest.pricing.domain.Promotion promotion(PromotionType type, String value) {
        return promotion("Summer Sale", type, value, null, null, false, 1, null);
    }

    private static net.mooctest.pricing.domain.Promotion promotion(
            String name, PromotionType type, String value, LocalDate startsOn,
            LocalDate endsOn, boolean membersOnly, int minimumQuantity, String maximumDiscount) {
        return new net.mooctest.pricing.domain.Promotion(name, type, amount(value), startsOn, endsOn,
                membersOnly, minimumQuantity, amount(maximumDiscount));
    }

    private static void assertMoney(BigDecimal actual, String expected) {
        assertEquals(expected, actual.toPlainString());
    }

    // ------------------------------------------------------------------
    // 请求校验: validateRequest 的每个守卫及边界
    // ------------------------------------------------------------------
    @Nested
    class Validation {

        @Test
        void nullRequestThrowsNullPointerException() {
            NullPointerException ex = assertThrows(NullPointerException.class,
                    () -> SERVICE.calculate(null));
            assertEquals("request must not be null", ex.getMessage());
        }

        @Test
        void blankCourseIdIsRejected() {
            IllegalArgumentException nullId = assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req(null, "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, "CN", "CNY")));
            assertEquals("courseId must not be blank", nullId.getMessage());

            IllegalArgumentException blankId = assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("   ", "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, "CN", "CNY")));
            assertEquals("courseId must not be blank", blankId.getMessage());
        }

        @Test
        void nullUnitPriceThrowsBusinessExceptionWithInvalidPrice() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> SERVICE.calculate(req("course-1", null, 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, "CN", "CNY")));
            assertEquals(ErrorCode.INVALID_PRICE, ex.code());
            assertEquals("Unit price must be between 0 and 99999.99", ex.getMessage());
        }

        @Test
        void negativeUnitPriceThrowsBusinessException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> SERVICE.calculate(req("course-1", "-0.01", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, "CN", "CNY")));
            assertEquals(ErrorCode.INVALID_PRICE, ex.code());
            assertEquals("Unit price must be between 0 and 99999.99", ex.getMessage());
        }

        @Test
        void unitPriceBoundaryAcceptsMaximumAndRejectsBeyond() {
            PriceBreakdown atMax = SERVICE.calculate(request("99999.99", 1));
            assertMoney(atMax.subtotal(), "99999.99");
            // 99999.99 * 0.06 = 5999.9994 -> HALF_UP -> 6000.00
            assertMoney(atMax.tax(), "6000.00");
            assertMoney(atMax.total(), "105999.99");
            assertEquals(List.of("TAX_CN"), atMax.appliedRules());

            BusinessException beyond = assertThrows(BusinessException.class,
                    () -> SERVICE.calculate(request("100000.00", 1)));
            assertEquals(ErrorCode.INVALID_PRICE, beyond.code());
            assertEquals("Unit price must be between 0 and 99999.99", beyond.getMessage());
        }

        @Test
        void quantityBelowOneIsRejected() {
            assertEquals("quantity must be positive", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(request("10.00", 0))).getMessage());
            assertEquals("quantity must be positive", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(request("10.00", -3))).getMessage());
        }

        @Test
        void quantityAboveHundredIsRejected() {
            assertEquals("quantity must not exceed 100", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(request("10.00", 101))).getMessage());
        }

        @Test
        void nullMembershipLevelIsRejected() {
            assertEquals("membershipLevel must not be null", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("course-1", "10.00", 1, null, null, null,
                            false, false, false, false, 0, 0, "CN", "CNY"))).getMessage());
        }

        @Test
        void blankCurrencyIsRejected() {
            assertEquals("currency must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("course-1", "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, "CN", null))).getMessage());
            assertEquals("currency must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("course-1", "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, "CN", ""))).getMessage());
        }

        @Test
        void unsupportedCurrencyIsRejectedButLowercaseIsAccepted() {
            assertEquals("unsupported currency: JPY", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(request("10.00", 1, "CN", "JPY"))).getMessage());

            PriceBreakdown lowercase = SERVICE.calculate(request("10.00", 1, "CN", "cny"));
            assertMoney(lowercase.total(), "10.60");
            assertEquals(List.of("TAX_CN"), lowercase.appliedRules());
            assertTrue(lowercase.warnings().isEmpty());
        }

        @Test
        void blankCountryCodeIsRejected() {
            assertEquals("countryCode must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("course-1", "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, null, "CNY"))).getMessage());
            assertEquals("countryCode must not be blank", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("course-1", "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, 0, "  ", "CNY"))).getMessage());
        }

        @Test
        void negativeLoyaltyPointsToUseIsRejected() {
            assertEquals("loyaltyPointsToUse must not be negative", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("course-1", "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, -1, 0, "CN", "CNY"))).getMessage());
        }

        @Test
        void negativeAvailableLoyaltyPointsIsRejected() {
            assertEquals("availableLoyaltyPoints must not be negative", assertThrows(IllegalArgumentException.class,
                    () -> SERVICE.calculate(req("course-1", "10.00", 1, MembershipLevel.NONE, null, null,
                            false, false, false, false, 0, -5, "CN", "CNY"))).getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 会员折扣: 六个档位 + INSTRUCTOR/ENTERPRISE 双侧 + NONE 早退
    // ------------------------------------------------------------------
    @Nested
    class Membership {

        @Test
        void noneMembershipAddsNoDiscountAndNoRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 1, MembershipLevel.NONE));
            assertMoney(breakdown.subtotal(), "10.00");
            assertMoney(breakdown.membershipDiscount(), "0.00");
            assertMoney(breakdown.tax(), "0.60");
            assertMoney(breakdown.total(), "10.60");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void basicMembershipDiscountIsThreePercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 1, MembershipLevel.BASIC));
            assertMoney(breakdown.membershipDiscount(), "0.30");
            assertMoney(breakdown.total(), "10.28");
            assertEquals(List.of("MEMBERSHIP_BASIC", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void plusMembershipDiscountIsSevenPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 1, MembershipLevel.PLUS));
            assertMoney(breakdown.membershipDiscount(), "0.70");
            assertMoney(breakdown.total(), "9.86");
            assertEquals(List.of("MEMBERSHIP_PLUS", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void premiumMembershipDiscountIsTwelvePercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 1, MembershipLevel.PREMIUM));
            assertMoney(breakdown.membershipDiscount(), "1.20");
            assertMoney(breakdown.total(), "9.33");
            assertEquals(List.of("MEMBERSHIP_PREMIUM", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void instructorOwnedCourseWithInstructorLevelIsFree() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 1, MembershipLevel.INSTRUCTOR,
                    null, null, false, false, true, 0, 0));
            assertMoney(breakdown.membershipDiscount(), "10.00");
            assertMoney(breakdown.tax(), "0.00");
            assertMoney(breakdown.total(), "0.00");
            assertEquals(List.of("MEMBERSHIP_INSTRUCTOR", "FREE_ORDER"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void instructorLevelWithoutOwnershipGetsTenPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 1, MembershipLevel.INSTRUCTOR));
            assertMoney(breakdown.membershipDiscount(), "1.00");
            assertMoney(breakdown.total(), "9.54");
            assertEquals(List.of("MEMBERSHIP_INSTRUCTOR", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void enterpriseTenSeatsGetEighteenPercentWithoutWarning() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 10, MembershipLevel.ENTERPRISE));
            assertMoney(breakdown.subtotal(), "100.00");
            assertMoney(breakdown.membershipDiscount(), "18.00");
            assertMoney(breakdown.bulkDiscount(), "5.00");
            assertMoney(breakdown.total(), "81.62");
            assertEquals(List.of("MEMBERSHIP_ENTERPRISE", "BULK_5", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void enterpriseBelowTenSeatsIsReducedWithWarning() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 9, MembershipLevel.ENTERPRISE));
            assertMoney(breakdown.membershipDiscount(), "7.20");
            assertMoney(breakdown.bulkDiscount(), "4.50");
            assertMoney(breakdown.total(), "83.00");
            assertEquals(List.of("MEMBERSHIP_ENTERPRISE", "BULK_5", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Enterprise discount is reduced for orders below ten seats"),
                    breakdown.warnings());
        }
    }

    // ------------------------------------------------------------------
    // 批量折扣: 50/20/5 三档边界（49/50, 19/20, 4/5）+ 以下无规则
    // ------------------------------------------------------------------
    @Nested
    class Bulk {

        @Test
        void fourSeatsEarnNoBulkRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.15", 4));
            assertMoney(breakdown.subtotal(), "40.60");
            assertMoney(breakdown.bulkDiscount(), "0.00");
            assertMoney(breakdown.total(), "43.04");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void fiveSeatsTriggerBulkFiveWithHalfUpRounding() {
            // 50.75 * 0.05 = 2.5375 -> 2.54
            PriceBreakdown breakdown = SERVICE.calculate(request("10.15", 5));
            assertMoney(breakdown.bulkDiscount(), "2.54");
            assertMoney(breakdown.total(), "51.10");
            assertEquals(List.of("BULK_5", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void nineteenSeatsStayInBulkFive() {
            // 192.85 * 0.05 = 9.6425 -> 9.64
            PriceBreakdown breakdown = SERVICE.calculate(request("10.15", 19));
            assertMoney(breakdown.bulkDiscount(), "9.64");
            assertMoney(breakdown.total(), "194.20");
            assertEquals(List.of("BULK_5", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void twentySeatsTriggerBulkTwenty() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.15", 20));
            assertMoney(breakdown.bulkDiscount(), "20.30");
            assertMoney(breakdown.total(), "193.66");
            assertEquals(List.of("BULK_20", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void fortyNineSeatsStayInBulkTwenty() {
            // 497.35 * 0.10 = 49.7350 -> 49.74
            PriceBreakdown breakdown = SERVICE.calculate(request("10.15", 49));
            assertMoney(breakdown.bulkDiscount(), "49.74");
            assertMoney(breakdown.total(), "474.47");
            assertEquals(List.of("BULK_20", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void fiftySeatsTriggerBulkFifty() {
            // 507.50 * 0.15 = 76.125 -> 76.13
            PriceBreakdown breakdown = SERVICE.calculate(request("10.15", 50));
            assertMoney(breakdown.bulkDiscount(), "76.13");
            assertMoney(breakdown.total(), "457.25");
            assertEquals(List.of("BULK_50", "TAX_CN"), breakdown.appliedRules());
        }
    }

    // ------------------------------------------------------------------
    // 促销: 配置完整性 / 日期窗口 / 会员限定 / 最少数量 / 三种类型 / 上限
    // ------------------------------------------------------------------
    @Nested
    class Promotion {

        @Test
        void absentPromotionAddsNoRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void unnamedPromotionIsIgnored() {
            PriceBreakdown nullName = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(null, PromotionType.PERCENT, "10.00", null, null, false, 1, null)));
            assertMoney(nullName.promotionDiscount(), "0.00");
            assertEquals(List.of("TAX_CN"), nullName.appliedRules());
            assertEquals(List.of("A promotion without a name was ignored"), nullName.warnings());

            PriceBreakdown blankName = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("   ", PromotionType.PERCENT, "10.00", null, null, false, 1, null)));
            assertEquals(List.of("A promotion without a name was ignored"), blankName.warnings());
        }

        @Test
        void incompletePromotionConfigurationIsIgnored() {
            PriceBreakdown nullType = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", null, "10.00", null, null, false, 1, null)));
            assertEquals(List.of("Promotion configuration is incomplete"), nullType.warnings());
            assertMoney(nullType.promotionDiscount(), "0.00");

            PriceBreakdown nullValue = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, null, null, null, false, 1, null)));
            assertEquals(List.of("Promotion configuration is incomplete"), nullValue.warnings());
        }

        @Test
        void nonPositivePromotionValueIsRejected() {
            PriceBreakdown zero = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(PromotionType.PERCENT, "0.00")));
            assertEquals(List.of("Promotion value must be positive"), zero.warnings());

            PriceBreakdown negative = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(PromotionType.FIXED, "-5.00")));
            assertEquals(List.of("Promotion value must be positive"), negative.warnings());
        }

        @Test
        void promotionBeforeItsStartIsRejected() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", TOMORROW, null,
                            false, 1, null)));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Promotion has not started"), breakdown.warnings());
        }

        @Test
        void promotionStartingTodayIsValid() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", TODAY, null,
                            false, 1, null)));
            assertMoney(breakdown.promotionDiscount(), "10.00");
            assertMoney(breakdown.total(), "95.40");
            assertEquals(List.of("PROMOTION_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void promotionExpiredYesterdayIsRejected() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, YESTERDAY,
                            false, 1, null)));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertEquals(List.of("Promotion has expired"), breakdown.warnings());
        }

        @Test
        void promotionEndingTodayIsValid() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, TODAY,
                            false, 1, null)));
            assertMoney(breakdown.promotionDiscount(), "10.00");
            assertMoney(breakdown.total(), "95.40");
            assertEquals(List.of("PROMOTION_PERCENT", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void singleDayPromotionWindowIsStillValid() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", TODAY, TODAY,
                            false, 1, null)));
            assertMoney(breakdown.promotionDiscount(), "10.00");
            assertTrue(breakdown.warnings().isEmpty());
        }

        /**
         * 可观测行为固定: startsOn=TODAY 且 endsOn=YESTERDAY（start &gt; end）时，
         * "has expired" 先于 "date range is invalid" 触发 —— 后者在固定时钟下是死分支，
         * 只能经由下方 stub record 白盒测试执行。
         */
        @Test
        void invertedPromotionWindowFiresTheEarlierExpiryGuard() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", TODAY, YESTERDAY,
                            false, 1, null)));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertEquals(List.of("Promotion has expired"), breakdown.warnings());
            assertFalse(breakdown.appliedRules().contains("PROMOTION_PERCENT"));
        }

        /**
         * 白盒分支覆盖: "Promotion date range is invalid" 守卫要求 startsOn &gt; endsOn，
         * 但前两个窗口守卫已强制 startsOn &le; today &le; endsOn，真实 record 在固定时钟下
         * 数学上不可能到达。用按调用序返回不一致日期的 stub record 执行真实字节码:
         * startsOn 第 1/2 次调用（窗口检查）返回 06-01，第 3/4 次调用（区间检查）返回 06-30；
         * endsOn 反向（06-30, 06-30, 06-01, 06-01）。
         */
        @Test
        void stubbedRecordReachesInvertedPromotionWindowGuard() {
            net.mooctest.pricing.domain.Promotion inverted =
                    mock(net.mooctest.pricing.domain.Promotion.class);
            when(inverted.name()).thenReturn("Inverted Window");
            when(inverted.type()).thenReturn(PromotionType.PERCENT);
            when(inverted.value()).thenReturn(new BigDecimal("10"));
            when(inverted.startsOn()).thenReturn(
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 30));
            when(inverted.endsOn()).thenReturn(
                    LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 30),
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1));

            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, inverted));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Promotion date range is invalid"), breakdown.warnings());
        }

        @Test
        void membersOnlyPromotionIsBlockedForNonMembers() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, null,
                            true, 1, null)));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertEquals(List.of("Promotion is available to members only"), breakdown.warnings());
        }

        @Test
        void membersOnlyPromotionAppliesToMembers() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.BASIC,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, null,
                            true, 1, null)));
            assertMoney(breakdown.membershipDiscount(), "3.00");
            assertMoney(breakdown.promotionDiscount(), "10.00");
            assertMoney(breakdown.total(), "92.22");
            assertEquals(List.of("MEMBERSHIP_BASIC", "PROMOTION_PERCENT", "TAX_CN"),
                    breakdown.appliedRules());
        }

        @Test
        void promotionBelowMinimumQuantityIsRejected() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 2, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, null,
                            false, 3, null)));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertEquals(List.of("Order quantity is below the promotion minimum"), breakdown.warnings());
        }

        @Test
        void promotionAtExactMinimumQuantityPasses() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 3, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, null,
                            false, 3, null)));
            assertMoney(breakdown.subtotal(), "30.00");
            assertMoney(breakdown.promotionDiscount(), "3.00");
            assertMoney(breakdown.total(), "28.62");
            assertEquals(List.of("PROMOTION_PERCENT", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void minimumQuantityBelowOneOnlyRequiresSingleUnit() {
            PriceBreakdown zeroFloor = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, null,
                            false, 0, null)));
            assertMoney(zeroFloor.promotionDiscount(), "10.00");
            assertTrue(zeroFloor.warnings().isEmpty());

            PriceBreakdown negativeFloor = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "10.00", null, null,
                            false, -5, null)));
            assertMoney(negativeFloor.promotionDiscount(), "10.00");
        }

        @Test
        void percentPromotionAppliesConfiguredRate() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(PromotionType.PERCENT, "50.00")));
            assertMoney(breakdown.promotionDiscount(), "50.00");
            assertMoney(breakdown.total(), "53.00");
            assertEquals(List.of("PROMOTION_PERCENT", "TAX_CN"), breakdown.appliedRules());
        }

        /**
         * 边界 value=80 走 percent() 分支（&gt; 80 为假）。注意 80% 折扣必然超过 0.70 标准
         * 折扣上限，故随后被修剪至 70.00 —— 两条路径的最终数值与规则在结构上不可区分，
         * 由 DISCOUNT_CAP 告警钉住修剪行为。
         */
        @Test
        void percentPromotionAtEightyIsStillLimitedByStandardCap() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(PromotionType.PERCENT, "80.00")));
            assertMoney(breakdown.promotionDiscount(), "70.00");
            assertMoney(breakdown.total(), "31.80");
            assertEquals(List.of("PROMOTION_PERCENT", "DISCOUNT_CAP", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        /** value=81 &gt; 80: 先被 80% 钳制为 80.00，再被 0.70 标准折扣上限修剪为 70.00。 */
        @Test
        void percentPromotionAboveEightyIsCappedAtEightyPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(PromotionType.PERCENT, "81.00")));
            assertMoney(breakdown.promotionDiscount(), "70.00");
            assertMoney(breakdown.total(), "31.80");
            assertEquals(List.of("PROMOTION_PERCENT", "DISCOUNT_CAP", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        @Test
        void fixedPromotionAppliesValueBelowSubtotal() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(PromotionType.FIXED, "30.00")));
            assertMoney(breakdown.promotionDiscount(), "30.00");
            assertMoney(breakdown.total(), "74.20");
            assertEquals(List.of("PROMOTION_FIXED", "TAX_CN"), breakdown.appliedRules());
        }

        /** min(500, 100) = 100.00 &gt; 0.70 标准:上限 -> 再修剪至 70.00（FREE_ORDER 不再触发）。 */
        @Test
        void fixedPromotionNeverExceedsSubtotal() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion(PromotionType.FIXED, "500.00")));
            assertMoney(breakdown.promotionDiscount(), "70.00");
            assertMoney(breakdown.total(), "31.80");
            assertEquals(List.of("PROMOTION_FIXED", "DISCOUNT_CAP", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        @Test
        void buyMoreSaveMoreGivesTwelvePercentAtTwentySeats() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 20, MembershipLevel.NONE,
                    null, promotion(PromotionType.BUY_MORE_SAVE_MORE, "12.00")));
            assertMoney(breakdown.bulkDiscount(), "20.00");
            assertMoney(breakdown.promotionDiscount(), "24.00");
            assertMoney(breakdown.total(), "165.36");
            assertEquals(List.of("BULK_20", "PROMOTION_BUY_MORE_SAVE_MORE", "TAX_CN"),
                    breakdown.appliedRules());
        }

        @Test
        void buyMoreSaveMoreGivesEightPercentBelowTwentySeats() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 19, MembershipLevel.NONE,
                    null, promotion(PromotionType.BUY_MORE_SAVE_MORE, "12.00")));
            assertMoney(breakdown.promotionDiscount(), "15.20");
            assertMoney(breakdown.total(), "175.22");
            assertEquals(List.of("BULK_5", "PROMOTION_BUY_MORE_SAVE_MORE", "TAX_CN"),
                    breakdown.appliedRules());
        }

        @Test
        void buyMoreSaveMoreGivesEightPercentAtTenSeats() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 10, MembershipLevel.NONE,
                    null, promotion(PromotionType.BUY_MORE_SAVE_MORE, "12.00")));
            assertMoney(breakdown.promotionDiscount(), "8.00");
            assertMoney(breakdown.total(), "92.22");
        }

        @Test
        void buyMoreSaveMoreGivesFourPercentBelowTenSeats() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 9, MembershipLevel.NONE,
                    null, promotion(PromotionType.BUY_MORE_SAVE_MORE, "12.00")));
            assertMoney(breakdown.promotionDiscount(), "3.60");
            assertMoney(breakdown.total(), "86.81");
            assertEquals(List.of("BULK_5", "PROMOTION_BUY_MORE_SAVE_MORE", "TAX_CN"),
                    breakdown.appliedRules());
        }

        @Test
        void promotionCapClipsDiscountAndWarns() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "50.00", null, null,
                            false, 1, "20.00")));
            assertMoney(breakdown.promotionDiscount(), "20.00");
            assertMoney(breakdown.total(), "84.80");
            assertEquals(List.of("PROMOTION_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Promotion discount reached its configured cap"), breakdown.warnings());
        }

        @Test
        void promotionCapAboveDiscountDoesNotTrigger() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "30.00", null, null,
                            false, 1, "40.00")));
            assertMoney(breakdown.promotionDiscount(), "30.00");
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void negativePromotionCapIsIgnored() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "30.00", null, null,
                            false, 1, "-1.00")));
            assertMoney(breakdown.promotionDiscount(), "30.00");
            assertMoney(breakdown.total(), "74.20");
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void zeroPromotionCapZeroesDiscountAndOmitsRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, promotion("Summer Sale", PromotionType.PERCENT, "50.00", null, null,
                            false, 1, "0.00")));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Promotion discount reached its configured cap"), breakdown.warnings());
        }
    }

    // ------------------------------------------------------------------
    // 优惠券: 配置完整性 / 窗口 / 门槛 / 限定课程 / 数量上限 / 会员下限 / 类型 / 互斥
    // ------------------------------------------------------------------
    @Nested
    class Coupon {

        @Test
        void absentCouponAddsNoRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1));
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void blankCouponCodeIsRejected() {
            PriceBreakdown nullCode = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(null, CouponType.PERCENT, "10.00"), null));
            assertEquals(List.of("Coupon code is blank"), nullCode.warnings());
            assertMoney(nullCode.couponDiscount(), "0.00");

            PriceBreakdown blankCode = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("  ", CouponType.PERCENT, "10.00"), null));
            assertEquals(List.of("Coupon code is blank"), blankCode.warnings());
        }

        @Test
        void incompleteCouponConfigurationIsRejected() {
            PriceBreakdown nullType = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", null, "10.00"), null));
            assertEquals(List.of("Coupon configuration is incomplete"), nullType.warnings());

            PriceBreakdown nullValue = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, null), null));
            assertEquals(List.of("Coupon configuration is incomplete"), nullValue.warnings());
        }

        @Test
        void nonPositiveCouponValueIsRejected() {
            PriceBreakdown zero = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.PERCENT, "0.00"), null));
            assertEquals(List.of("Coupon value must be positive"), zero.warnings());

            PriceBreakdown negative = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "-10.00"), null));
            assertEquals(List.of("Coupon value must be positive"), negative.warnings());
        }

        @Test
        void exhaustedCouponIsRejected() {
            PriceBreakdown zeroUses = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 0, true,
                            Set.of(), null, 0), null));
            assertEquals(List.of("Coupon has no remaining uses"), zeroUses.warnings());

            PriceBreakdown negativeUses = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, -2, true,
                            Set.of(), null, 0), null));
            assertEquals(List.of("Coupon has no remaining uses"), negativeUses.warnings());
        }

        @Test
        void couponBeforeValidFromIsRejected() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, TOMORROW, null, 5, true,
                            Set.of(), null, 0), null));
            assertEquals(List.of("Coupon is not active yet"), breakdown.warnings());
            assertMoney(breakdown.couponDiscount(), "0.00");
        }

        @Test
        void couponValidFromTodayIsActive() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, TODAY, null, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertMoney(breakdown.total(), "95.40");
            assertEquals(List.of("COUPON_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void couponExpiredYesterdayIsRejected() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, YESTERDAY, 5, true,
                            Set.of(), null, 0), null));
            assertEquals(List.of("Coupon has expired"), breakdown.warnings());
        }

        @Test
        void couponValidUntilTodayIsValid() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, TODAY, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void singleDayCouponWindowIsValid() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, TODAY, TODAY, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertTrue(breakdown.warnings().isEmpty());
        }

        /**
         * 可观测行为固定: validFrom=TODAY 且 validUntil=YESTERDAY（start &gt; end）时，
         * "has expired" 先于 "date range is invalid" 触发（后者在固定时钟下为死分支，
         * 仅可经由下方 stub record 白盒测试执行）。
         */
        @Test
        void invertedCouponWindowFiresTheEarlierExpiryGuard() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, TODAY, YESTERDAY, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertEquals(List.of("Coupon has expired"), breakdown.warnings());
        }

        /**
         * 白盒分支覆盖: "Coupon date range is invalid" 守卫与固定时钟矛盾（同促销侧注释）。
         * validFrom 第 1/2 次调用（窗口检查）返回 06-01，第 3/4 次（区间检查）返回 06-30；
         * validUntil 反向（06-30, 06-30, 06-01, 06-01）。
         */
        @Test
        void stubbedRecordReachesInvertedCouponWindowGuard() {
            net.mooctest.pricing.domain.Coupon inverted =
                    mock(net.mooctest.pricing.domain.Coupon.class);
            when(inverted.code()).thenReturn("INVERTED");
            when(inverted.type()).thenReturn(CouponType.PERCENT);
            when(inverted.value()).thenReturn(new BigDecimal("10"));
            when(inverted.remainingUses()).thenReturn(1);
            when(inverted.validFrom()).thenReturn(
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 30));
            when(inverted.validUntil()).thenReturn(
                    LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 30),
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1));

            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    inverted, null));
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Coupon date range is invalid"), breakdown.warnings());
        }

        @Test
        void couponMinimumSubtotalBlocksSmallerOrders() {
            PriceBreakdown breakdown = SERVICE.calculate(request("99.99", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", "100.00", null, null, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.subtotal(), "99.99");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.total(), "105.99");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Order does not meet the coupon minimum subtotal"), breakdown.warnings());
        }

        @Test
        void couponMinimumSubtotalBoundaryPassesOnEquality() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", "100.00", null, null, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertMoney(breakdown.total(), "95.40");
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void couponRestrictedToOtherCoursesIsRejected() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of("course-9"), null, 0), null));
            assertEquals(List.of("Coupon is not valid for this course"), breakdown.warnings());
            assertMoney(breakdown.couponDiscount(), "0.00");
        }

        @Test
        void couponAppliesToItsEligibleCourse() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of("course-1"), null, 0), null));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void couponWithEmptyEligibilityAppliesEverywhere() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void nullEligibleCourseIdsAreNormalizedToUnrestricted() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            null, null, 0), null));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertMoney(breakdown.total(), "95.40");
            assertEquals(List.of("COUPON_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void couponQuantityLimitBlocksLargerOrders() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 6, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), null, 5), null));
            assertMoney(breakdown.bulkDiscount(), "3.00");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.total(), "60.42");
            assertEquals(List.of("BULK_5", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Order quantity exceeds the coupon limit"), breakdown.warnings());
        }

        @Test
        void couponQuantityLimitBoundaryPassesOnEquality() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 5, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), null, 5), null));
            assertMoney(breakdown.bulkDiscount(), "2.50");
            assertMoney(breakdown.couponDiscount(), "5.00");
            assertMoney(breakdown.total(), "45.05");
            assertEquals(List.of("BULK_5", "COUPON_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void couponWithoutQuantityLimitAcceptsLargeOrders() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 60, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), null, 0), null));
            assertMoney(breakdown.bulkDiscount(), "90.00");
            assertMoney(breakdown.couponDiscount(), "60.00");
            assertMoney(breakdown.total(), "477.00");
            assertEquals(List.of("BULK_50", "COUPON_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void couponMembershipFloorBlocksLowerLevels() {
            PriceBreakdown basic = SERVICE.calculate(request("100.00", 1, MembershipLevel.BASIC,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), MembershipLevel.PLUS, 0), null));
            assertEquals(List.of("Membership level is too low for this coupon"), basic.warnings());

            PriceBreakdown none = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), MembershipLevel.BASIC, 0), null));
            assertEquals(List.of("Membership level is too low for this coupon"), none.warnings());
        }

        @Test
        void couponMembershipFloorAllowsEqualAndHigherLevels() {
            PriceBreakdown equal = SERVICE.calculate(request("100.00", 1, MembershipLevel.PLUS,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), MembershipLevel.PLUS, 0), null));
            assertMoney(equal.membershipDiscount(), "7.00");
            assertMoney(equal.couponDiscount(), "10.00");
            assertMoney(equal.total(), "87.98");
            assertEquals(List.of("MEMBERSHIP_PLUS", "COUPON_PERCENT", "TAX_CN"), equal.appliedRules());

            PriceBreakdown higher = SERVICE.calculate(request("100.00", 1, MembershipLevel.PREMIUM,
                    coupon("SAVE-10", CouponType.PERCENT, "10.00", null, null, null, 5, true,
                            Set.of(), MembershipLevel.PLUS, 0), null));
            assertMoney(higher.couponDiscount(), "10.00");
            assertMoney(higher.total(), "82.68");
            assertTrue(higher.warnings().isEmpty());
        }

        @Test
        void percentCouponAboveSeventyIsCapped() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.PERCENT, "90.00"), null));
            assertMoney(breakdown.couponDiscount(), "70.00");
            assertMoney(breakdown.total(), "31.80");
            assertEquals(List.of("COUPON_PERCENT", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void percentCouponAtSeventyKeepsConfiguredRate() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.PERCENT, "70.00"), null));
            assertMoney(breakdown.couponDiscount(), "70.00");
            assertMoney(breakdown.total(), "31.80");
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void percentCouponBelowSeventyAppliesConfiguredRate() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.PERCENT, "30.00"), null));
            assertMoney(breakdown.couponDiscount(), "30.00");
            assertMoney(breakdown.total(), "74.20");
            assertEquals(List.of("COUPON_PERCENT", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void fixedAndFullReductionCouponsApplyConfiguredValue() {
            PriceBreakdown fixed = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "25.00"), null));
            assertMoney(fixed.couponDiscount(), "25.00");
            assertMoney(fixed.total(), "79.50");
            assertEquals(List.of("COUPON_FIXED", "TAX_CN"), fixed.appliedRules());

            PriceBreakdown fullReduction = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FULL_REDUCTION, "25.00"), null));
            assertMoney(fullReduction.couponDiscount(), "25.00");
            assertEquals(List.of("COUPON_FULL_REDUCTION", "TAX_CN"), fullReduction.appliedRules());
        }

        /** min(999, 100) = 100.00 &gt; 0.70 标准上限 -> 再修剪至 70.00。 */
        @Test
        void fixedCouponNeverExceedsSubtotal() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "999.00"), null));
            assertMoney(breakdown.couponDiscount(), "70.00");
            assertMoney(breakdown.total(), "31.80");
            assertEquals(List.of("COUPON_FIXED", "DISCOUNT_CAP", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        @Test
        void cashbackCouponDefersValueWithFutureRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.CASHBACK, "50.00"), null));
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("COUPON_CASHBACK_FUTURE", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Cashback is credited after payment and does not reduce this order"),
                    breakdown.warnings());
        }

        // ---- 不可叠加券与促销的互斥（双向 + 等额保促销 + 可叠加共存 + 无促销侧）----

        @Test
        void stackableCouponAndPromotionCombine() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "20.00", true), promotion(PromotionType.PERCENT, "20.00")));
            assertMoney(breakdown.promotionDiscount(), "20.00");
            assertMoney(breakdown.couponDiscount(), "20.00");
            assertMoney(breakdown.total(), "63.60");
            assertEquals(List.of("PROMOTION_PERCENT", "COUPON_FIXED", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void nonStackableCouponLosesToStrongerPromotion() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "20.00", false), promotion(PromotionType.PERCENT, "50.00")));
            assertMoney(breakdown.promotionDiscount(), "50.00");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.total(), "53.00");
            assertEquals(List.of("PROMOTION_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("The coupon was ignored because the promotion gives a greater discount"),
                    breakdown.warnings());
        }

        @Test
        void nonStackableCouponBeatsWeakerPromotion() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "20.00", false), promotion(PromotionType.PERCENT, "5.00")));
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertMoney(breakdown.couponDiscount(), "20.00");
            assertMoney(breakdown.total(), "84.80");
            assertEquals(List.of("COUPON_FIXED", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("The promotion was ignored because the coupon gives a greater discount"),
                    breakdown.warnings());
        }

        @Test
        void equalDiscountsKeepPromotionAndDropCoupon() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "20.00", false), promotion(PromotionType.PERCENT, "20.00")));
            assertMoney(breakdown.promotionDiscount(), "20.00");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.total(), "84.80");
            assertEquals(List.of("PROMOTION_PERCENT", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("The coupon was ignored because the promotion gives a greater discount"),
                    breakdown.warnings());
        }

        @Test
        void nonStackableCouponStillAppliesWithoutPromotion() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "20.00", false), null));
            assertMoney(breakdown.couponDiscount(), "20.00");
            assertMoney(breakdown.total(), "84.80");
            assertEquals(List.of("COUPON_FIXED", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }
    }

    // ------------------------------------------------------------------
    // 首购: 8% / 100 元封顶 / 讲师课程拒绝 / 不可叠加券阻断
    // ------------------------------------------------------------------
    @Nested
    class FirstPurchase {

        @Test
        void firstPurchaseDiscountAppliesEightPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("1000.00", 1, MembershipLevel.NONE,
                    null, null, true, false, false, 0, 0));
            assertMoney(breakdown.firstPurchaseDiscount(), "80.00");
            assertMoney(breakdown.total(), "975.20");
            assertEquals(List.of("FIRST_PURCHASE", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void firstPurchaseDiscountCapsAtHundred() {
            PriceBreakdown breakdown = SERVICE.calculate(request("2000.00", 1, MembershipLevel.NONE,
                    null, null, true, false, false, 0, 0));
            // 2000 * 0.08 = 160.00 -> min(160.00, 100.00) = 100.00
            assertMoney(breakdown.firstPurchaseDiscount(), "100.00");
            assertMoney(breakdown.total(), "2014.00");
            assertEquals(List.of("FIRST_PURCHASE", "TAX_CN"), breakdown.appliedRules());
        }

        @Test
        void firstPurchaseIsBlockedOnInstructorOwnedCourses() {
            PriceBreakdown breakdown = SERVICE.calculate(request("1000.00", 1, MembershipLevel.NONE,
                    null, null, true, false, true, 0, 0));
            assertMoney(breakdown.firstPurchaseDiscount(), "0.00");
            assertMoney(breakdown.total(), "1060.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("First purchase discount is not applied to instructor-owned courses"),
                    breakdown.warnings());
        }

        @Test
        void firstPurchaseIsBlockedByNonStackableCoupon() {
            PriceBreakdown breakdown = SERVICE.calculate(request("1000.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "10.00", false), null, true, false, false, 0, 0));
            assertMoney(breakdown.couponDiscount(), "10.00");
            assertMoney(breakdown.firstPurchaseDiscount(), "0.00");
            assertMoney(breakdown.total(), "1049.40");
            assertEquals(List.of("COUPON_FIXED", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("First purchase discount is blocked by a non-stackable coupon"),
                    breakdown.warnings());
        }
    }

    // ------------------------------------------------------------------
    // 非营利: 5% / 企业 8% / 未知国家不加税率减免规则
    // ------------------------------------------------------------------
    @Nested
    class Nonprofit {

        @Test
        void nonprofitDiscountIsFivePercentWithHalvedChineseTax() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, null, false, true, false, 0, 0));
            assertMoney(breakdown.nonprofitDiscount(), "5.00");
            // 95.00 * 0.03 (0.06 减半) = 2.85
            assertMoney(breakdown.tax(), "2.85");
            assertMoney(breakdown.total(), "97.85");
            assertEquals(List.of("NONPROFIT", "NONPROFIT_TAX_REDUCTION", "TAX_CN"),
                    breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void nonprofitEnterpriseDiscountIsEightPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 10, MembershipLevel.ENTERPRISE,
                    null, null, false, true, false, 0, 0));
            assertMoney(breakdown.membershipDiscount(), "18.00");
            assertMoney(breakdown.nonprofitDiscount(), "8.00");
            assertMoney(breakdown.total(), "71.07");
            assertEquals(List.of("MEMBERSHIP_ENTERPRISE", "BULK_5", "NONPROFIT_ENTERPRISE",
                    "NONPROFIT_TAX_REDUCTION", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void nonprofitUnknownCountrySkipsTaxReductionRule() {
            PriceBreakdown breakdown = SERVICE.calculate(req("course-1", "100.00", 1,
                    MembershipLevel.NONE, null, null, false, true, false, false, 0, 0, "FR", "EUR"));
            assertMoney(breakdown.nonprofitDiscount(), "5.00");
            assertMoney(breakdown.tax(), "0.00");
            assertMoney(breakdown.total(), "95.00");
            assertEquals(List.of("NONPROFIT"), breakdown.appliedRules());
            assertEquals(List.of("No tax rule is configured for country FR"), breakdown.warnings());
        }
    }

    // ------------------------------------------------------------------
    // 积分: 0 跳过 / 超额缩减 / 99 拒收 / 100 可用 / 20% 封顶双侧 / 零上限归零
    // ------------------------------------------------------------------
    @Nested
    class Loyalty {

        @Test
        void zeroRequestedPointsAreSkipped() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, null, false, false, false, 0, 500));
            assertMoney(breakdown.loyaltyDiscount(), "0.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void requestedPointsAreReducedToAvailableBalance() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, null, false, false, false, 500, 200));
            // 200 points -> 2.00，低于 20% 上限（20.00），无封顶告警
            assertMoney(breakdown.loyaltyDiscount(), "2.00");
            assertMoney(breakdown.total(), "103.88");
            assertEquals(List.of("LOYALTY_POINTS", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Requested loyalty points were reduced to the available balance"),
                    breakdown.warnings());
        }

        @Test
        void ninetyNinePointsAreRejected() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, null, false, false, false, 99, 500));
            assertMoney(breakdown.loyaltyDiscount(), "0.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("At least 100 loyalty points are required"), breakdown.warnings());
        }

        @Test
        void exactlyHundredPointsApplyOneUnitOfValue() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, null, false, false, false, 100, 1000));
            assertMoney(breakdown.loyaltyDiscount(), "1.00");
            assertMoney(breakdown.total(), "104.94");
            assertEquals(List.of("LOYALTY_POINTS", "TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void loyaltyDiscountIsCappedAtTwentyPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.NONE,
                    null, null, false, false, false, 5000, 5000));
            // 5000 points -> 50.00 > cap 100.00 * 0.20 = 20.00
            assertMoney(breakdown.loyaltyDiscount(), "20.00");
            assertMoney(breakdown.total(), "84.80");
            assertEquals(List.of("LOYALTY_POINTS", "TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Loyalty discount was limited to twenty percent"), breakdown.warnings());
        }

        @Test
        void tinyCapReducesLoyaltyToZeroWithoutRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("0.01", 1, MembershipLevel.NONE,
                    null, null, false, false, false, 100, 100));
            // cap = money(0.01 * 0.20) = 0.00 -> value 归零且不加 LOYALTY_POINTS 规则
            assertMoney(breakdown.loyaltyDiscount(), "0.00");
            assertMoney(breakdown.tax(), "0.00");
            assertMoney(breakdown.total(), "0.01");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Loyalty discount was limited to twenty percent"), breakdown.warnings());
        }
    }

    // ------------------------------------------------------------------
    // 税收: 免税 / 五国税率 / 未知国家 / 非营利减半 / 币种错配 / 小写国家码
    // ------------------------------------------------------------------
    @Nested
    class Tax {

        @Test
        void taxExemptOrderAddsExemptRuleWithoutTax() {
            PriceBreakdown breakdown = SERVICE.calculate(req("course-1", "100.00", 1,
                    MembershipLevel.NONE, null, null, false, false, true, false, 0, 0, "CN", "CNY"));
            assertMoney(breakdown.tax(), "0.00");
            assertMoney(breakdown.total(), "100.00");
            assertEquals(List.of("TAX_EXEMPT"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void chinaTaxIsSixPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "CN", "CNY"));
            assertMoney(breakdown.tax(), "6.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void usTaxIsSevenAndAHalfPercentWithHalfUpRounding() {
            // 147.00 * 0.075 = 11.0250 -> HALF_UP -> 11.03
            PriceBreakdown breakdown = SERVICE.calculate(request("147.00", 1, "US", "USD"));
            assertMoney(breakdown.tax(), "11.03");
            assertMoney(breakdown.total(), "158.03");
            assertEquals(List.of("TAX_US"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void germanTaxIsNineteenPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "DE", "EUR"));
            assertMoney(breakdown.tax(), "19.00");
            assertMoney(breakdown.total(), "119.00");
            assertEquals(List.of("TAX_DE"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void singaporeTaxIsNinePercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "SG", "SGD"));
            assertMoney(breakdown.tax(), "9.00");
            assertMoney(breakdown.total(), "109.00");
            assertEquals(List.of("TAX_SG"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void unitedKingdomTaxIsTwentyPercent() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "GB", "GBP"));
            assertMoney(breakdown.tax(), "20.00");
            assertMoney(breakdown.total(), "120.00");
            assertEquals(List.of("TAX_GB"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        @Test
        void unknownCountryWarnsWithoutTaxRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "FR", "EUR"));
            assertMoney(breakdown.tax(), "0.00");
            assertMoney(breakdown.total(), "100.00");
            assertTrue(breakdown.appliedRules().isEmpty());
            assertEquals(List.of("No tax rule is configured for country FR"), breakdown.warnings());
        }

        @Test
        void chineseTaxWarnsOnForeignCurrency() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "CN", "USD"));
            assertMoney(breakdown.tax(), "6.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertEquals(List.of("Chinese tax is calculated for a non-CNY order"), breakdown.warnings());
        }

        @Test
        void usTaxWarnsOnForeignCurrency() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "US", "CNY"));
            assertMoney(breakdown.tax(), "7.50");
            assertEquals(List.of("TAX_US"), breakdown.appliedRules());
            assertEquals(List.of("United States tax is calculated for a non-USD order"),
                    breakdown.warnings());
        }

        @Test
        void germanTaxWarnsOnForeignCurrency() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "DE", "USD"));
            assertMoney(breakdown.tax(), "19.00");
            assertEquals(List.of("TAX_DE"), breakdown.appliedRules());
            assertEquals(List.of("German tax is calculated for a non-EUR order"), breakdown.warnings());
        }

        @Test
        void lowercaseCountryCodeUsesConfiguredRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, "cn", "CNY"));
            assertMoney(breakdown.tax(), "6.00");
            assertMoney(breakdown.total(), "106.00");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }
    }

    // ------------------------------------------------------------------
    // 折扣上限: 0.70 双侧 / 讲师全额上限 / trimDiscounts 从队尾修剪且跳过零项
    // ------------------------------------------------------------------
    @Nested
    class Cap {

        /**
         * 修剪次序可观测: excess = 350.00 时 NONPROFIT(50) 与 FIRST_PURCHASE(80) 先被清零，
         * COUPON 位置为 0（触发 continue），PROMOTION 承担剩余 220 -> 580，
         * MEMBERSHIP/BULK 完全不受影响。
         */
        @Test
        void standardCapTrimsFromTheEndSkippingZeroComponents() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 10, MembershipLevel.PLUS,
                    null, promotion(PromotionType.PERCENT, "80.00"), true, true, false, 0, 0));
            assertMoney(breakdown.subtotal(), "1000.00");
            assertMoney(breakdown.membershipDiscount(), "70.00");
            assertMoney(breakdown.bulkDiscount(), "50.00");
            assertMoney(breakdown.promotionDiscount(), "580.00");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.firstPurchaseDiscount(), "0.00");
            assertMoney(breakdown.nonprofitDiscount(), "0.00");
            assertMoney(breakdown.loyaltyDiscount(), "0.00");
            // 非营利客户: 300.00 * 0.03 (0.06 减半) = 9.00
            assertMoney(breakdown.tax(), "9.00");
            assertMoney(breakdown.total(), "309.00");
            assertEquals(List.of("MEMBERSHIP_PLUS", "BULK_5", "PROMOTION_PERCENT", "FIRST_PURCHASE",
                    "NONPROFIT", "DISCOUNT_CAP", "NONPROFIT_TAX_REDUCTION", "TAX_CN"),
                    breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        /** 讲师自营 + INSTRUCTOR: cap = subtotal，折扣被压回 100%，after 为 0 触发 FREE_ORDER。 */
        @Test
        void instructorCapEqualsSubtotalAndForcesFreeOrder() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.INSTRUCTOR,
                    null, promotion(PromotionType.FIXED, "50.00"), false, false, true, 0, 0));
            assertMoney(breakdown.membershipDiscount(), "100.00");
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertMoney(breakdown.tax(), "0.00");
            assertMoney(breakdown.total(), "0.00");
            assertEquals(List.of("MEMBERSHIP_INSTRUCTOR", "PROMOTION_FIXED", "DISCOUNT_CAP",
                    "FREE_ORDER"), breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        @Test
        void discountsExactlyAtCapAreNotTrimmed() {
            // 7.00 + 63.00 = 70.00 == cap(1000... subtotal 100 * 0.70)
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.PLUS,
                    null, promotion(PromotionType.PERCENT, "63.00")));
            assertMoney(breakdown.membershipDiscount(), "7.00");
            assertMoney(breakdown.promotionDiscount(), "63.00");
            assertMoney(breakdown.total(), "31.80");
            assertEquals(List.of("MEMBERSHIP_PLUS", "PROMOTION_PERCENT", "TAX_CN"),
                    breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        /** 讲师自营但会员非 INSTRUCTOR: 仍用 0.70 标准上限（若错用 subtotal 则不会修剪）。 */
        @Test
        void instructorOwnedNonInstructorMembershipKeepsStandardCap() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.PREMIUM,
                    null, promotion(PromotionType.PERCENT, "80.00"), false, false, true, 0, 0));
            assertMoney(breakdown.membershipDiscount(), "12.00");
            assertMoney(breakdown.promotionDiscount(), "58.00");
            assertMoney(breakdown.total(), "31.80");
            assertEquals(List.of("MEMBERSHIP_PREMIUM", "PROMOTION_PERCENT", "DISCOUNT_CAP", "TAX_CN"),
                    breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        @Test
        void discountsBelowCapAccumulateFreely() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 1, MembershipLevel.PLUS,
                    null, promotion(PromotionType.PERCENT, "30.00")));
            assertMoney(breakdown.membershipDiscount(), "7.00");
            assertMoney(breakdown.promotionDiscount(), "30.00");
            assertMoney(breakdown.total(), "66.78");
            assertEquals(List.of("MEMBERSHIP_PLUS", "PROMOTION_PERCENT", "TAX_CN"),
                    breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }
    }

    // ------------------------------------------------------------------
    // 结果对象: 12 个分量全量断言（简单 + 封顶各一）、免费课程、第 100 件告警
    // ------------------------------------------------------------------
    @Nested
    class Breakdown {

        @Test
        void simpleOrderProducesCompleteBreakdown() {
            PriceBreakdown breakdown = SERVICE.calculate(request("10.00", 1));
            assertMoney(breakdown.subtotal(), "10.00");
            assertMoney(breakdown.membershipDiscount(), "0.00");
            assertMoney(breakdown.bulkDiscount(), "0.00");
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.firstPurchaseDiscount(), "0.00");
            assertMoney(breakdown.nonprofitDiscount(), "0.00");
            assertMoney(breakdown.loyaltyDiscount(), "0.00");
            assertMoney(breakdown.tax(), "0.60");
            assertMoney(breakdown.total(), "10.60");
            assertEquals(List.of("TAX_CN"), breakdown.appliedRules());
            assertTrue(breakdown.warnings().isEmpty());
        }

        /** 封顶触发样例的全量分量断言（1150 -> 修剪至 700 的完整路径；非营利税率减半）。 */
        @Test
        void cappedOrderProducesCompleteBreakdown() {
            PriceBreakdown breakdown = SERVICE.calculate(request("100.00", 10, MembershipLevel.PLUS,
                    coupon(CouponType.FIXED, "100.00"), promotion(PromotionType.PERCENT, "80.00"),
                    true, true, false, 0, 0));
            assertMoney(breakdown.subtotal(), "1000.00");
            assertMoney(breakdown.membershipDiscount(), "70.00");
            assertMoney(breakdown.bulkDiscount(), "50.00");
            assertMoney(breakdown.promotionDiscount(), "580.00");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.firstPurchaseDiscount(), "0.00");
            assertMoney(breakdown.nonprofitDiscount(), "0.00");
            assertMoney(breakdown.loyaltyDiscount(), "0.00");
            // 非营利客户: 300.00 * 0.03 (0.06 减半) = 9.00
            assertMoney(breakdown.tax(), "9.00");
            assertMoney(breakdown.total(), "309.00");
            assertEquals(List.of("MEMBERSHIP_PLUS", "BULK_5", "PROMOTION_PERCENT", "COUPON_FIXED",
                    "FIRST_PURCHASE", "NONPROFIT", "DISCOUNT_CAP", "NONPROFIT_TAX_REDUCTION", "TAX_CN"),
                    breakdown.appliedRules());
            assertEquals(List.of("Combined discounts were limited by the maximum discount policy"),
                    breakdown.warnings());
        }

        @Test
        void freeCourseProducesFreeOrderWithoutLoyaltyOrTaxRules() {
            PriceBreakdown breakdown = SERVICE.calculate(request("0.00", 1, MembershipLevel.NONE,
                    null, null, false, false, false, 500, 500));
            assertMoney(breakdown.subtotal(), "0.00");
            assertMoney(breakdown.membershipDiscount(), "0.00");
            assertMoney(breakdown.bulkDiscount(), "0.00");
            assertMoney(breakdown.promotionDiscount(), "0.00");
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.firstPurchaseDiscount(), "0.00");
            assertMoney(breakdown.nonprofitDiscount(), "0.00");
            assertMoney(breakdown.loyaltyDiscount(), "0.00");
            assertMoney(breakdown.tax(), "0.00");
            assertMoney(breakdown.total(), "0.00");
            assertEquals(List.of("FREE_ORDER"), breakdown.appliedRules());
            assertEquals(List.of("No discount value is generated for a free course"), breakdown.warnings());
        }

        @Test
        void fixedCouponOnFreeCourseYieldsNoCouponRule() {
            PriceBreakdown breakdown = SERVICE.calculate(request("0.00", 1, MembershipLevel.NONE,
                    coupon(CouponType.FIXED, "20.00"), null));
            assertMoney(breakdown.couponDiscount(), "0.00");
            assertMoney(breakdown.total(), "0.00");
            assertEquals(List.of("FREE_ORDER"), breakdown.appliedRules());
            assertEquals(List.of("No discount value is generated for a free course"), breakdown.warnings());
        }

        @Test
        void maximumQuantityWarningAppearsOnlyAtHundredUnits() {
            PriceBreakdown atLimit = SERVICE.calculate(request("2.00", 100));
            assertMoney(atLimit.subtotal(), "200.00");
            assertMoney(atLimit.bulkDiscount(), "30.00");
            assertMoney(atLimit.total(), "180.20");
            assertEquals(List.of("BULK_50", "TAX_CN"), atLimit.appliedRules());
            assertEquals(List.of("The order reached the maximum allowed quantity"), atLimit.warnings());

            PriceBreakdown belowLimit = SERVICE.calculate(request("2.00", 99));
            assertMoney(belowLimit.total(), "178.40");
            assertTrue(belowLimit.warnings().isEmpty());
        }
    }
}
