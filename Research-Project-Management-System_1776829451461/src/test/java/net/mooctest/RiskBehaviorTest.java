package net.mooctest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class RiskBehaviorTest {
    @Test
    void defaultsClampingAndValueAccessors() {
        Risk risk = new Risk(null, null, -1, 2);
        assertEquals("", risk.getName());
        assertEquals("GENERAL", risk.getCategory());
        assertEquals(0, risk.getProbability());
        assertEquals(1, risk.getImpact());
        Risk named = new Risk("delay", "TIME", 0.5, 0.75);
        assertEquals("delay", named.getName());
        assertEquals("TIME", named.getCategory());
        assertEquals(0.5, named.getProbability());
        assertEquals(0.75, named.getImpact());
        assertEquals(0.375, named.score());
        assertEquals(0, named.clamp(0));
        assertEquals(1, named.clamp(1));
    }

    @ParameterizedTest
    @CsvSource({"0,0", "0.001,1", "0.249,1", "0.25,2", "0.499,2", "0.5,3", "1,3"})
    void priorityIncludesExactThresholds(double score, int expected) {
        assertEquals(expected, new Risk("r", "c", 1, score).priority());
    }

    @Test
    void comparesPriorityThenImpactThenNameInBothDirections() {
        Risk high = new Risk("high", "c", 1, 0.5);
        Risk low = new Risk("low", "c", 0.1, 0.9);
        assertTrue(high.compareTo(low) < 0);
        assertTrue(low.compareTo(high) > 0);
        Risk larger = new Risk("z", "c", 0.2, 0.8);
        Risk smaller = new Risk("a", "c", 0.2, 0.7);
        assertTrue(larger.compareTo(smaller) < 0);
        assertTrue(smaller.compareTo(larger) > 0);
        Risk a = new Risk("a", "c", 0.2, 0.8);
        assertTrue(a.compareTo(larger) < 0);
        assertTrue(larger.compareTo(a) > 0);
        assertEquals(0, a.compareTo(new Risk("a", "different", 0.2, 0.8)));
    }

    @Test
    void emptyAndNonPositiveSimulationInputs() {
        RiskAnalyzer analyzer = new RiskAnalyzer();
        assertResult(analyzer.simulate(null, 10), 0, 0, 0);
        assertResult(analyzer.simulate(List.of(), 10), 0, 0, 0);
        List<Risk> risks = List.of(new Risk("r", "c", 1, 1));
        assertResult(analyzer.simulate(risks, 0), 0, 0, 0);
        assertResult(analyzer.simulate(risks, -1), 0, 0, 0);
        assertResult(new RiskAnalyzer.SimulationResult(2, 3, 4), 2, 3, 4);
    }

    @Test
    void certainAndImpossibleEventsHaveExactStatistics() {
        List<Risk> risks = List.of(new Risk("certain", "c", 1, 0.25),
                new Risk("certain too", "c", 1, 0.5), new Risk("never", "c", 0, 1));
        assertResult(new RiskAnalyzer().simulate(risks, 10), 0.75, 0.75, 0.75);
        assertResult(new RiskAnalyzer().simulate(risks, 1), 0.75, 0.75, 0.75);
    }

    @Test
    void deterministicDrawsExerciseStrictProbabilityAndPercentileBoundaries() {
        // Ten fixed draws: equality with probability 0.5 must NOT trigger the event.
        double[] draws = {0.5, 0.8, 0.1, 0.9, 0.3, 0.7, 0.2, 0.6, 0.4, 0.99};
        RiskAnalyzer analyzer = new RiskAnalyzer() {
            private int index;
            @Override public double rnd() { return draws[index++]; }
        };
        assertResult(analyzer.simulate(List.of(new Risk("r", "c", 0.5, 0.5)), 10),
                0.2, 0.5, 0.5);
        // Nine zero scenarios followed by one event: floor(10 * .9) selects index 9.
        RiskAnalyzer percentile = new RiskAnalyzer() {
            private int index;
            @Override public double rnd() { return index++ == 9 ? 0.0 : 0.9; }
        };
        assertResult(percentile.simulate(List.of(new Risk("r", "c", 0.5, 1)), 10),
                0.1, 1, 1);
    }

    @Test
    void zeroSeedProducesZeroDrawAndStrictZeroProbabilityDoesNotTrigger() throws Exception {
        RiskAnalyzer analyzer = new RiskAnalyzer();
        Field seed = RiskAnalyzer.class.getDeclaredField("seed");
        seed.setAccessible(true);
        seed.setLong(analyzer, 0L);
        assertEquals(0, analyzer.rnd());
        assertResult(analyzer.simulate(List.of(new Risk("never", "c", 0, 1)), 1), 0, 0, 0);
    }

    @Test
    void seededGeneratorIsReproducibleAndAdvances() {
        RiskAnalyzer a = new RiskAnalyzer();
        RiskAnalyzer b = new RiskAnalyzer();
        double previous = -1;
        for (int i = 0; i < 32; i++) {
            double draw = a.rnd();
            assertEquals(draw, b.rnd());
            assertTrue(draw >= 0 && draw < 1);
            assertNotEquals(previous, draw);
            previous = draw;
        }
    }

    @Test
    void defaultSeedMatchesIndependentGoldenSequence() {
        // Numerators computed with independent 64-bit integer arithmetic.
        long[] numerators = {8442009034574139L, 4766085112791913L, 2609970339759407L,
                115724349775205L, 6582913784013087L};
        RiskAnalyzer analyzer = new RiskAnalyzer();
        for (long numerator : numerators) {
            assertEquals(numerator / 9007199254740992.0, analyzer.rnd());
        }
    }

    private static void assertResult(RiskAnalyzer.SimulationResult result,
                                     double mean, double p90, double worst) {
        assertEquals(mean, result.getMeanImpact(), 1e-12);
        assertEquals(p90, result.getP90Impact(), 1e-12);
        assertEquals(worst, result.getWorstCaseImpact(), 1e-12);
    }
}
