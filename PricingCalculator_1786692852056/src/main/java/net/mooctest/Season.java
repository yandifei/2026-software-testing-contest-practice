package net.mooctest;

import java.time.Month;

public enum Season {
	OFF_SEASON(0.9),   // 淡季：11,12,1,2
	REGULAR(1.0),      // 平季：3,4,5,9,10
	PEAK(1.2);         // 旺季：6,7,8

	private final double factor;

	Season(double factor) {
		this.factor = factor;
	}

	public double getFactor() {
		return factor;
	}

	/**
	 * 根据月份返回对应的季节
	 */
	public static Season fromMonth(Month month) {
		switch (month) {
			case JUNE:
			case JULY:
			case AUGUST:
				return PEAK;
			case NOVEMBER:
			case DECEMBER:
			case JANUARY:
			case FEBRUARY:
				return OFF_SEASON;
			default:
				return REGULAR;
		}
	}
}