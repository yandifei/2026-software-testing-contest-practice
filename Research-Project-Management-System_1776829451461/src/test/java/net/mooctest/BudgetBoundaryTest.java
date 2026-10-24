package net.mooctest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Boundary checks and an independent exhaustive oracle for the integer knapsack. */
public class BudgetBoundaryTest {
    @Test
    void normalizesItemsAndProtectsBudgetCollection() {
        Budget.Item item = new Budget.Item(null, -1, -2, null);
        assertEquals("", item.getName());
        assertEquals("GENERAL", item.getCategory());
        assertEquals(0, item.getCost());
        assertEquals(0, item.getValue());
        Budget budget = new Budget();
        budget.add(null);
        assertTrue(budget.getItems().isEmpty());
        assertEquals(0, budget.totalCost());
        assertEquals(0, budget.totalValue());
        assertEquals(1000, budget.requiredReserve());
        Budget.Item first = new Budget.Item("A", 12000, 7, "EQUIPMENT");
        Budget.Item second = new Budget.Item("B", 8000, 11, "LABOR");
        budget.add(first);
        budget.add(second);
        assertEquals(List.of(first, second), budget.getItems());
        budget.getItems().clear();
        assertEquals(20000, budget.totalCost());
        assertEquals(18, budget.totalValue());
        assertEquals(2000, budget.requiredReserve());
    }

    @ParameterizedTest
    @CsvSource({"-2, 100", "-0.5, 100", "0, 200", "0.25, 250", "1, 400", "2, 400"})
    void clampsInflation(double inflation, double expected) {
        Budget budget = new Budget();
        budget.add(new Budget.Item("item", 200, 1, "x"));
        assertEquals(expected, budget.forecastCost(inflation), 1e-9);
    }

    @ParameterizedTest
    @CsvSource({"-1, 1000", "0, 1000", "0.05, 1000", "0.2, 4000", "0.5, 10000", "1, 10000"})
    void clampsReserve(double ratio, double expected) {
        Budget budget = new Budget();
        budget.add(new Budget.Item("item", 20000, 1, "x"));
        budget.setReserveRatio(ratio);
        assertEquals(expected, budget.requiredReserve());
    }

    @Test
    void optimizerHandlesNullEmptyZeroAndTies() {
        BudgetOptimizer optimizer = new BudgetOptimizer();
        assertEquals("budget null", assertThrows(DomainException.class,
                () -> optimizer.optimize(null, 1)).getMessage());
        Budget budget = new Budget();
        BudgetOptimizer.Selection empty = optimizer.optimize(budget, -1);
        assertTrue(empty.getItems().isEmpty());
        assertEquals(0, empty.getTotalCost());
        assertEquals(0, empty.getTotalValue());
        Budget.Item free = new Budget.Item("free", 0, 3, null);
        Budget.Item first = new Budget.Item("first", 2, 5, null);
        budget.add(free);
        budget.add(first);
        budget.add(new Budget.Item("tie", 2, 5, null));
        budget.add(new Budget.Item("worthless", 0, 0, null));
        assertEquals(List.of(free), optimizer.optimize(budget, -2).getItems());
        BudgetOptimizer.Selection chosen = optimizer.optimize(budget, 2);
        assertEquals(List.of(free, first), chosen.getItems());
        assertEquals(2, chosen.getTotalCost());
        assertEquals(8, chosen.getTotalValue());
    }

    @Test
    void optimizerMatchesAllSubsetsForEverySmallCapacity() {
        Budget budget = new Budget();
        int[] costs = {0, 1, 2, 3, 5, 7};
        int[] values = {2, 2, 6, 7, 11, 13};
        for (int i = 0; i < costs.length; i++) {
            budget.add(new Budget.Item("item" + i, costs[i], values[i], "test"));
        }
        for (int limit = 0; limit <= 20; limit++) {
            int optimum = 0;
            for (int mask = 0; mask < (1 << costs.length); mask++) {
                int cost = 0;
                int value = 0;
                for (int bit = 0; bit < costs.length; bit++) {
                    if ((mask & (1 << bit)) != 0) {
                        cost += costs[bit];
                        value += values[bit];
                    }
                }
                if (cost <= limit) optimum = Math.max(optimum, value);
            }
            BudgetOptimizer.Selection result = new BudgetOptimizer().optimize(budget, limit);
            assertEquals(optimum, result.getTotalValue(), "capacity=" + limit);
            assertTrue(result.getTotalCost() <= limit);
            assertEquals(result.getItems().stream().mapToDouble(Budget.Item::getCost).sum(),
                    result.getTotalCost());
            assertEquals(result.getItems().stream().mapToDouble(Budget.Item::getValue).sum(),
                    result.getTotalValue());
            assertEquals(result.getItems().size(), result.getItems().stream().distinct().count());
        }
    }

    @Test
    void characterizesRoundingThatCanExceedActualLimit() {
        Budget budget = new Budget();
        Budget.Item item = new Budget.Item("fractional", 1.49, 5, null);
        budget.add(item);
        BudgetOptimizer.Selection result = new BudgetOptimizer().optimize(budget, 1.0);
        assertEquals(List.of(item), result.getItems());
        assertEquals(1.49, result.getTotalCost());
        assertTrue(result.getTotalCost() > 1.0, "Known defect: rounded cost exceeds actual limit");
        assertEquals(List.of(item), new BudgetOptimizer.Selection(List.of(item), 2, 3).getItems());
    }

    @Test
    void halfUnitsRoundUpForBothCapacityAndItemWeight() {
        Budget budget = new Budget();
        Budget.Item item = new Budget.Item("half", 2.5, 7.5, "c");
        budget.add(item);
        BudgetOptimizer optimizer = new BudgetOptimizer();
        assertTrue(optimizer.optimize(budget, 2.49).getItems().isEmpty());
        BudgetOptimizer.Selection result = optimizer.optimize(budget, 2.5);
        assertEquals(List.of(item), result.getItems());
        assertEquals(2.5, result.getTotalCost());
        assertEquals(7.5, result.getTotalValue());
    }
}
