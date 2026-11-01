package net.mooctest;

import java.util.*;

public class Budget {
    public static final class Item {
        private final String name;
        private final double cost;
        private final double value;
        private final String category;

        public Item(String name, double cost, double value, String category) {
            this.name = name == null ? "" : name;
            this.cost = cost < 0 ? 0 : cost;
            this.value = value < 0 ? 0 : value;
            this.category = category == null ? "GENERAL" : category;
        }

        public String getName() { return name; }
        public double getCost() { return cost; }
        public double getValue() { return value; }
        public String getCategory() { return category; }
    }

    private final List<Item> items;
    private double reserveRatio;

    public Budget() {
        this.items = new ArrayList<>();
        this.reserveRatio = 0.1;
    }

    public void add(Item item) {
        if (item == null) return;
        items.add(item);
    }

    public List<Item> getItems() { return new ArrayList<>(items); }

    public double totalCost() {
        double s = 0;
        for (Item i : items) s += i.getCost();
        return s;
    }

    public double totalValue() {
        double s = 0;
        for (Item i : items) s += i.getValue();
        return s;
    }

    public double forecastCost(double inflationRate) {
        if (inflationRate < -0.5) inflationRate = -0.5;
        if (inflationRate > 1.0) inflationRate = 1.0;
        return totalCost() * (1 + inflationRate);
    }

    public double requiredReserve() {
        double base = totalCost();
        double r = base * reserveRatio;
        if (r < 1000) r = 1000;
        return r;
    }

    public void setReserveRatio(double r) {
        if (r < 0) r = 0;
        if (r > 0.5) r = 0.5;
        this.reserveRatio = r;
    }
}
