package net.mooctest;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Focused regressions for gaps identified in the first mutation report. */
public class MutationBoundaryTest {
    @Test
    void percentileExcludesAnOutlierInTwentyScenarios() {
        RiskAnalyzer analyzer = new RiskAnalyzer() {
            private int drawCount;

            @Override
            public double rnd() {
                return drawCount++ == 0 ? 0.0 : 0.75;
            }
        };
        RiskAnalyzer.SimulationResult result = analyzer.simulate(
                List.of(new Risk("rare", "impact", 0.5, 1.0)), 20);
        assertEquals(0.05, result.getMeanImpact(), 1e-12);
        assertEquals(0.0, result.getP90Impact());
        assertEquals(1.0, result.getWorstCaseImpact());
    }

    @Test
    void releasingNoHoursDoesNotNormalizeAnExistingExcessCapacity() {
        Researcher researcher = new Researcher("large initial capacity", 41);
        researcher.releaseHours(0);
        assertEquals(41, researcher.getCapacity());
        researcher.releaseHours(-1);
        assertEquals(41, researcher.getCapacity());
        researcher.releaseHours(1);
        assertEquals(40, researcher.getCapacity());
    }

    @Test
    void successfulAssignmentsIncrementInternalCountAndRejectedOnesDoNot() throws Exception {
        // There is no public counter accessor; observe the state without changing it.
        Field count = Researcher.class.getDeclaredField("assignedCount");
        count.setAccessible(true);
        Researcher researcher = new Researcher("r", 5);
        Task task = new Task("two hours", 2, null);
        assertEquals(0, count.getInt(researcher));
        assertTrue(researcher.assignTask(task));
        assertEquals(1, count.getInt(researcher));
        assertTrue(researcher.assignTask(task));
        assertEquals(2, count.getInt(researcher));
        assertFalse(researcher.assignTask(task));
        assertFalse(researcher.assignTask(null));
        assertEquals(2, count.getInt(researcher));
        assertEquals(1, researcher.getCapacity());
    }

    @Test
    void emptyMatchResultsRemainIndependentMutableLists() {
        MatchingEngine engine = new MatchingEngine();
        List<MatchingEngine.Assignment> missingResearchers = engine.match(null, new ArrayList<>());
        List<MatchingEngine.Assignment> missingTasks = engine.match(new ArrayList<>(), null);
        MatchingEngine.Assignment assignment = new MatchingEngine.Assignment(
                new Task("t", 1, null), new Researcher("r", 1), 0.1);
        missingResearchers.add(assignment);
        assertEquals(List.of(assignment), missingResearchers);
        assertTrue(missingTasks.isEmpty());
        missingTasks.add(assignment);
        assertEquals(List.of(assignment), missingTasks);
        assertTrue(engine.match(null, null).isEmpty());
    }

    @Test
    void itemRetainsSignedZeroWhileClampingStrictlyNegativeValues() {
        Budget.Item item = new Budget.Item("signed zero", -0.0, -0.0, null);
        assertNegativeZero(item.getCost());
        assertNegativeZero(item.getValue());
        Budget.Item negative = new Budget.Item("negative", -Double.MIN_VALUE, -Double.MIN_VALUE, null);
        assertEquals(0L, Double.doubleToRawLongBits(negative.getCost()));
        assertEquals(0L, Double.doubleToRawLongBits(negative.getValue()));
    }

    @Test
    void riskClampPreservesSignedZero() {
        Risk risk = new Risk("zero", "c", -0.0, -0.0);
        assertNegativeZero(risk.getProbability());
        assertNegativeZero(risk.getImpact());
        assertNegativeZero(risk.clamp(-0.0));
        assertEquals(0L, Double.doubleToRawLongBits(risk.clamp(-Double.MIN_VALUE)));
    }

    @Test
    void progressPreservesSignedZero() {
        Task task = new Task("zero", 0, null);
        task.updateProgress(-0.0);
        assertNegativeZero(task.getProgress());
        task.updateProgress(-Double.MIN_VALUE);
        assertEquals(0L, Double.doubleToRawLongBits(task.getProgress()));
    }

    @Test
    void reserveRatioStoresClampedValueEvenWhenMinimumReserveMasksIt() throws Exception {
        Field ratio = Budget.class.getDeclaredField("reserveRatio");
        ratio.setAccessible(true);
        Budget budget = new Budget();
        budget.setReserveRatio(-0.25);
        assertEquals(0.0, ratio.getDouble(budget));
        budget.setReserveRatio(-0.0);
        assertNegativeZero(ratio.getDouble(budget));
        budget.setReserveRatio(0.25);
        assertEquals(0.25, ratio.getDouble(budget));
        budget.setReserveRatio(0.75);
        assertEquals(0.5, ratio.getDouble(budget));
    }

    private static void assertNegativeZero(double value) {
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(value),
                "Strictly negative checks must preserve IEEE-754 negative zero");
    }
}
