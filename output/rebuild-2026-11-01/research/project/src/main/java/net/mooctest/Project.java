package net.mooctest;

import java.util.*;

public class Project {
    private final long id;
    private String name;
    private final Map<Long, Task> tasks;
    private final Map<Long, Researcher> researchers;
    private final List<Risk> risks;
    private Budget budget;

    public Project(String name) {
        this.id = IdGenerator.nextId();
        this.name = name == null ? "" : name;
        this.tasks = new LinkedHashMap<>();
        this.researchers = new LinkedHashMap<>();
        this.risks = new ArrayList<>();
        this.budget = new Budget();
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n == null ? "" : n; }
    public Budget getBudget() { return budget; }
    public void setBudget(Budget b) { if (b != null) this.budget = b; }

    public Task addTask(Task t) {
        if (t == null) return null;
        tasks.put(t.getId(), t);
        return t;
    }

    public Researcher addResearcher(Researcher r) {
        if (r == null) return null;
        researchers.put(r.getId(), r);
        return r;
    }

    public void addRisk(Risk r) {
        if (r != null) risks.add(r);
    }

    public Task getTask(long id) { return tasks.get(id); }
    public Researcher getResearcher(long id) { return researchers.get(id); }
    public Collection<Task> getTasks() { return tasks.values(); }
    public Collection<Researcher> getResearchers() { return researchers.values(); }
    public List<Risk> getRisks() { return new ArrayList<>(risks); }

    public Map<Task.Status, Long> statusCounts() {
        Map<Task.Status, Long> m = new EnumMap<>(Task.Status.class);
        for (Task.Status s : Task.Status.values()) m.put(s, 0L);
        for (Task t : tasks.values()) m.put(t.getStatus(), m.get(t.getStatus()) + 1);
        return m;
    }

    public int criticalPathDuration() {
        return GraphUtils.longestPathDuration(tasks.values());
    }

    public List<MatchingEngine.Assignment> planAssignments() {
        MatchingEngine engine = new MatchingEngine();
        return engine.match(new ArrayList<>(researchers.values()), new ArrayList<>(tasks.values()));
    }

    public RiskAnalyzer.SimulationResult analyzeRisk(int iterations) {
        RiskAnalyzer analyzer = new RiskAnalyzer();
        return analyzer.simulate(risks, iterations);
    }
}
