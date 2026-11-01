package net.mooctest;

import java.util.*;

public class Task {
    public enum Status { PLANNED, IN_PROGRESS, BLOCKED, DONE, CANCELLED }
    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }

    private final long id;
    private String name;
    private int duration;
    private Status status;
    private Priority priority;
    private final Set<Task> dependencies;
    private final Map<String, Integer> requiredSkills;
    private int est;
    private int eft;
    private int lst;
    private int lft;
    private double progress;
    private Long assignedResearcherId;

    public Task(String name, int duration, Priority priority) {
        this.id = IdGenerator.nextId();
        this.name = name == null ? "" : name;
        this.duration = Math.max(0, duration);
        this.priority = priority == null ? Priority.MEDIUM : priority;
        this.status = Status.PLANNED;
        this.dependencies = new HashSet<>();
        this.requiredSkills = new HashMap<>();
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public int getDuration() { return duration; }
    public Status getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public Set<Task> getDependencies() { return new HashSet<>(dependencies); }
    public Map<String, Integer> getRequiredSkills() { return new HashMap<>(requiredSkills); }
    public int getEst() { return est; }
    public int getEft() { return eft; }
    public int getLst() { return lst; }
    public int getLft() { return lft; }
    public double getProgress() { return progress; }
    public Long getAssignedResearcherId() { return assignedResearcherId; }

    public void setName(String n) { this.name = n == null ? "" : n; }
    public void setDuration(int d) { this.duration = Math.max(0, d); }
    public void setPriority(Priority p) { this.priority = p == null ? Priority.MEDIUM : p; }

    public void requireSkill(String skill, int level) {
        if (skill == null || skill.isEmpty()) return;
        if (level < 0) level = 0;
        if (level > 10) level = 10;
        Integer old = requiredSkills.get(skill);
        if (old == null) requiredSkills.put(skill, level);
        else requiredSkills.put(skill, Math.max(level, old));
    }

    public boolean addDependency(Task t) {
        if (t == null || t == this) return false;
        return dependencies.add(t);
    }

    public boolean dependsOn(Task t) {
        if (t == null) return false;
        return dependencies.contains(t);
    }

    public void setSchedule(int est, int eft, int lst, int lft) {
        this.est = Math.max(0, est);
        this.eft = Math.max(this.est, eft);
        this.lft = Math.max(this.eft, lft);
        this.lst = Math.max(0, lst);
    }

    public int slack() { return Math.max(0, lst - est); }

    public void start() { if (status == Status.PLANNED) status = Status.IN_PROGRESS; }
    public void cancel() { status = Status.CANCELLED; }
    public void complete() { status = Status.DONE; }

    public void updateProgress(double p) {
        if (p < 0) p = 0;
        if (p > 1) p = 1;
        progress = p;
    }

    public void assignTo(Long researcherId) {
        this.assignedResearcherId = researcherId;
    }
}
