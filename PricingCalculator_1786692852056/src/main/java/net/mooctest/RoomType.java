package net.mooctest;

public enum RoomType {
	STANDARD(200.0),
	DELUXE(350.0),
	SUITE(600.0);

	private final double basePrice;

	RoomType(double basePrice) {
		this.basePrice = basePrice;
	}

	public double getBasePrice() {
		return basePrice;
	}
}