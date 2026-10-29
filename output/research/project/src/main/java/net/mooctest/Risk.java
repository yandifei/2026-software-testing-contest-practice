package net.mooctest;

public class Risk implements Comparable<Risk> {
    private final String name;
    private final String category;
    private final double probability;
    private final double impact;

    public Risk(String name, String category, double probability, double impact) {
        this.name = name == null ? "" : name;
        this.category = category == null ? "GENERAL" : category;
        this.probability = clamp(probability);
        this.impact = clamp(impact);
    }

    public double clamp(double v) {
        if (v < 0) v = 0;
        if (v > 1) v = 1;
        return v;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getProbability() { return probability; }
    public double getImpact() { return impact; }

    public double score() {
        return probability * impact;
    }

    public int priority() {
        double s = score();
        if (s >= 0.5) return 3;
        if (s >= 0.25) return 2;
        return s > 0 ? 1 : 0;
    }

    @Override
    public int compareTo(Risk o) {
        int p = Integer.compare(o.priority(), this.priority());
        if (p != 0) return p;
        int i = Double.compare(o.impact, this.impact);
        if (i != 0) return i;
        return this.name.compareTo(o.name);
    }
}
