package net.mooctest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDate;
import java.time.Month;
import static org.junit.jupiter.api.Assertions.*;

class PricingCalculatorTest {

    private PricingCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PricingCalculator();
    }

    @ParameterizedTest
    @CsvSource({
            "STANDARD, 200.0",
            "DELUXE, 350.0",
            "SUITE, 600.0"
    })
    void testBasePrice(RoomType type, double expected) {
        Reservation reservation = reservation(type, MemberLevel.NONE, 3, 1, 0, 0);
        assertEquals(expected, type.getBasePrice());
        assertEquals(expected, calculator.calculate(reservation));
    }

    @ParameterizedTest
    @CsvSource({
            "1, 0.9, OFF_SEASON, 180.0",
            "2, 0.9, OFF_SEASON, 180.0",
            "3, 1.0, REGULAR, 200.0",
            "4, 1.0, REGULAR, 200.0",
            "5, 1.0, REGULAR, 200.0",
            "6, 1.2, PEAK, 240.0",
            "7, 1.2, PEAK, 240.0",
            "8, 1.2, PEAK, 240.0",
            "9, 1.0, REGULAR, 200.0",
            "10, 1.0, REGULAR, 200.0",
            "11, 0.9, OFF_SEASON, 180.0",
            "12, 0.9, OFF_SEASON, 180.0"
    })
    void testSeasonFactor(int month, double expectedFactor,
                          Season expectedSeason, double expectedPrice) {
        Season season = Season.fromMonth(Month.of(month));
        assertSame(expectedSeason, season);
        assertEquals(expectedFactor, season.getFactor());
        assertEquals(expectedPrice, calculator.calculate(
                reservation(RoomType.STANDARD, MemberLevel.NONE, month, 1, 0, 0)));
    }

    @ParameterizedTest
    @CsvSource({
            "NONE, 1.0, 400.0",
            "SILVER, 0.9, 360.0",
            "GOLD, 0.8, 320.0"
    })
    void testMemberDiscount(MemberLevel level, double expectedRate, double expectedPrice) {
        assertEquals(expectedRate, level.getDiscountRate());
        assertEquals(expectedPrice, calculator.calculate(
                reservation(RoomType.STANDARD, level, 3, 2, 0, 0)));
    }

    @Test
    void testExtraBedFee() {
        // 房费 200 * 3，加床费 2 * 100 * 3。
        assertEquals(1200.0, calculator.calculate(
                reservation(RoomType.STANDARD, MemberLevel.NONE, 3, 3, 2, 0)));
    }

    @Test
    void testBreakfastFee() {
        // 房费 200 * 2，早餐费 3 * 50 * 2。
        assertEquals(700.0, calculator.calculate(
                reservation(RoomType.STANDARD, MemberLevel.NONE, 3, 2, 0, 3)));
    }

    @Test
    void testCombinedScenario() {
        // 房费 350 * 4 * 1.2 * 0.8 = 1344；加床 800；早餐 600。
        assertEquals(2744.0, calculator.calculate(
                reservation(RoomType.DELUXE, MemberLevel.GOLD, 7, 4, 2, 3)));
    }

    @Test
    void testSeasonBoundaryFebruary() {
        Guest guest = new Guest("月末入住", MemberLevel.NONE);
        Reservation reservation = new Reservation(guest, RoomType.STANDARD,
                LocalDate.of(2026, 2, 28), 3, 0, 0);
        // 季节按入住月份决定，跨到三月仍使用二月的淡季系数。
        assertEquals(540.0, calculator.calculate(reservation));
    }

    @Test
    void testRounding() {
        assertEquals(283.50, calculator.calculate(
                reservation(RoomType.DELUXE, MemberLevel.SILVER, 11, 1, 0, 0)));
        assertEquals(756.00, calculator.calculate(
                reservation(RoomType.DELUXE, MemberLevel.GOLD, 11, 3, 0, 0)));
    }

    @ParameterizedTest
    @CsvSource({"0, 0.0", "1, 750.0", "3, 2250.0"})
    void testEveryChargeScalesWithNights(int nights, double expectedPrice) {
        assertEquals(expectedPrice, calculator.calculate(
                reservation(RoomType.SUITE, MemberLevel.NONE, 3, nights, 1, 1)));
    }

    @Test
    void testAdditionalServicesAreNotDiscounted() {
        // 淡季金卡房费 144，加床 100，早餐 50。
        assertEquals(294.0, calculator.calculate(
                reservation(RoomType.STANDARD, MemberLevel.GOLD, 1, 1, 1, 1)));
    }

    @Test
    void testGuestPreservesNameAndMembership() {
        Guest guest = new Guest("张三", MemberLevel.SILVER);
        assertEquals("张三", guest.getName());
        assertSame(MemberLevel.SILVER, guest.getLevel());
    }

    @Test
    void testReservationPreservesAllBookingDetails() {
        Guest guest = new Guest("李四", MemberLevel.GOLD);
        LocalDate date = LocalDate.of(2026, 8, 15);
        Reservation reservation = new Reservation(guest, RoomType.SUITE, date, 5, 2, 3);
        assertAll(
                () -> assertSame(guest, reservation.getGuest()),
                () -> assertSame(RoomType.SUITE, reservation.getRoomType()),
                () -> assertEquals(date, reservation.getCheckInDate()),
                () -> assertSame(Month.AUGUST, reservation.getCheckInMonth()),
                () -> assertEquals(5, reservation.getNights()),
                () -> assertEquals(2, reservation.getExtraBeds()),
                () -> assertEquals(3, reservation.getBreakfastPeople())
        );
    }

    @Test
    void testNullMonthCannotDetermineSeason() {
        assertThrows(NullPointerException.class, () -> Season.fromMonth(null));
    }

    private Reservation reservation(RoomType roomType, MemberLevel level, int month,
                                    int nights, int extraBeds, int breakfastPeople) {
        return new Reservation(new Guest("测试住客", level), roomType,
                LocalDate.of(2026, month, 15), nights, extraBeds, breakfastPeople);
    }
}

