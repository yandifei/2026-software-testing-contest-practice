package net.mooctest;

import java.util.*;

public class BudgetOptimizer {
    public static final class Selection {
        private final List<Budget.Item> items;
        private final double totalCost;
        private final double totalValue;

        public Selection(List<Budget.Item> items, double totalCost, double totalValue) {
            this.items = items;
            this.totalCost = totalCost;
            this.totalValue = totalValue;
        }

        public List<Budget.Item> getItems() { return items; }
        public double getTotalCost() { return totalCost; }
        public double getTotalValue() { return totalValue; }
    }

    public Selection optimize(Budget budget, double limit) {
        if (budget == null) throw new DomainException("budget null");
        if (limit < 0) limit = 0;
        List<Budget.Item> items = budget.getItems();
        int n = items.size();
        int cap = (int)Math.round(limit);
        double[][] dp = new double[n + 1][cap + 1];
        boolean[][] take = new boolean[n + 1][cap + 1];
        for (int i = 1; i <= n; i++) {
            Budget.Item it = items.get(i - 1);
            int w = (int)Math.round(it.getCost());
            double val = it.getValue();
            for (int c = 0; c <= cap; c++) {
                double best = dp[i - 1][c];
                boolean choose = false;
                if (w <= c) {
                    double cand = dp[i - 1][c - w] + val;
                    if (cand > best) { best = cand; choose = true; }
                }
                dp[i][c] = best;
                take[i][c] = choose;
            }
        }
        double totalValue = dp[n][cap];
        List<Budget.Item> picked = new ArrayList<>();
        double totalCost = 0;
        int c = cap;
        for (int i = n; i >= 1; i--) {
            if (take[i][c]) {
                Budget.Item it = items.get(i - 1);
                picked.add(it);
                int w = (int)Math.round(it.getCost());
                totalCost += it.getCost();
                c -= w;
            }
        }
        Collections.reverse(picked);
        return new Selection(picked, totalCost, totalValue);
    }
}
