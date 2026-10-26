package net.mooctest;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PricingCalculator {

	private static final double EXTRA_BED_RATE = 100.0;   // 加床每晚费用
	private static final double BREAKFAST_RATE = 50.0;    // 每人每天早餐费

	/**
	 * 计算总价
	 * @param reservation 预订信息
	 * @return 保留两位小数的总价
	 */
	public double calculate(Reservation reservation) {
		// 1. 基础房价每晚
		double baseNightly = reservation.getRoomType().getBasePrice();

		// 2. 季节系数
		Season season = Season.fromMonth(reservation.getCheckInMonth());
		double seasonFactor = season.getFactor();

		// 3. 会员折扣
		double discountRate = reservation.getGuest().getLevel().getDiscountRate();

		// 4. 房间总价（含季节和会员折扣）
		double roomTotal = baseNightly * reservation.getNights() * seasonFactor * discountRate;

		// 5. 加床总费
		double extraBedTotal = reservation.getExtraBeds() * EXTRA_BED_RATE * reservation.getNights();

		// 6. 早餐总费
		double breakfastTotal = reservation.getBreakfastPeople() * BREAKFAST_RATE * reservation.getNights();

		// 7. 最终总价
		double total = roomTotal + extraBedTotal + breakfastTotal;

		// 8. 保留两位小数
		BigDecimal bd = new BigDecimal(total).setScale(2, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}
}