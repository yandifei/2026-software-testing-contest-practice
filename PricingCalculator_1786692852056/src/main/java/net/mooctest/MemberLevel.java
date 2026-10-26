package net.mooctest;

public enum MemberLevel {
    NONE(1.0),
    SILVER(0.9),
    GOLD(0.8);

    private final double discountRate;

    MemberLevel(double discountRate) {
        this.discountRate = discountRate;
    }

    public double getDiscountRate() {
        return discountRate;
    }
}