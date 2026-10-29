package net.mooctest;

import java.util.*;

public class RiskAnalyzer {
    public static final class SimulationResult {
        private final double meanImpact;
        private final double p90Impact;
        private final double worstCaseImpact;

        public SimulationResult(double meanImpact, double p90Impact, double worstCaseImpact) {
            this.meanImpact = meanImpact;
            this.p90Impact = p90Impact;
            this.worstCaseImpact = worstCaseImpact;
        }

        public double getMeanImpact() { return meanImpact; }
        public double getP90Impact() { return p90Impact; }
        public double getWorstCaseImpact() { return worstCaseImpact; }
    }

    private long seed = 2463534242L;

    public double rnd() {
        seed ^= (seed << 13);
        seed ^= (seed >>> 7);
        seed ^= (seed << 17);
        long v = seed & ((1L << 53) - 1);
        return v / (double)(1L << 53);
    }

    public SimulationResult simulate(List<Risk> risks, int iterations) {
        if (risks == null || risks.isEmpty() || iterations <= 0) return new SimulationResult(0, 0, 0);
        List<Double> impacts = new ArrayList<>();
        double sum = 0;
        double worst = 0;
        for (int i = 0; i < iterations; i++) {
            double scenario = 0;
            for (Risk r : risks) {
                double p = r.getProbability();
                double draw = rnd();
                if (draw < p) scenario += r.getImpact();
            }
            impacts.add(scenario);
            sum += scenario;
            if (scenario > worst) worst = scenario;
        }
        Collections.sort(impacts);
        double mean = sum / iterations;
        double p90 = impacts.get(Math.min(impacts.size() - 1, (int)Math.floor(impacts.size() * 0.9)));
        return new SimulationResult(mean, p90, worst);
    }
}
