package net.mooctest;

import java.util.*;

public class Researcher {
    private final long id;
    private String name;
    private final Map<String, Integer> skills;
    private int capacity;
    private double rating;
    private int assignedCount;

    public Researcher(String name, int capacity) {
        this.id = IdGenerator.nextId();
        this.name = name == null ? "" : name;
        this.capacity = Math.max(0, capacity);
        this.skills = new HashMap<>();
        this.assignedCount = 0;
        this.rating = 0.0;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n == null ? "" : n; }
    public int getCapacity() { return capacity; }
    public double getRating() { return rating; }

    public void addSkill(String skill, int level) {
        if (skill == null || skill.isEmpty()) return;
        if (level < 0) level = 0;
        if (level > 10) level = 10;
        skills.put(skill, Math.max(0, Math.min(10, level)));
    }

    public int getSkillLevel(String skill) {
        Integer v = skills.get(skill);
        return v == null ? 0 : v;
    }

    public boolean hasSkill(String skill, int minLevel) {
        return getSkillLevel(skill) >= Math.max(0, minLevel);
    }

    public boolean allocateHours(int hours) {
        if (hours <= 0) return false;
        if (capacity < hours) return false;
        capacity -= hours;
        return true;
    }

    public void releaseHours(int hours) {
        if (hours <= 0) return;
        capacity += hours;
        if (capacity > 40) capacity = 40;
    }

    public void updateRating(double outcomeScore) {
        if (outcomeScore < 0) outcomeScore = 0;
        if (outcomeScore > 100) outcomeScore = 100;
        double alpha = 0.3;
        rating = rating * (1 - alpha) + outcomeScore * alpha;
    }

    public boolean canAssign(Task task) {
        if (task == null) return false;
        return capacity >= task.getDuration();
    }

    public boolean assignTask(Task task) {
        if (!canAssign(task)) return false;
        assignedCount++;
        return allocateHours(task.getDuration());
    }

    public boolean completeTask(Task task, double quality) {
        if (task == null) return false;
        releaseHours(task.getDuration());
        updateRating(quality);
        return true;
    }

    public Set<String> getSkills() { return new HashSet<>(skills.keySet()); }
}
