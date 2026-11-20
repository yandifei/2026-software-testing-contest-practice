package net.mooctest;

import java.util.*;

public class MatchingEngine {
    public static final class Assignment {
        private final Task task;
        private final Researcher researcher;
        private final double score;

        public Assignment(Task task, Researcher researcher, double score) {
            this.task = task;
            this.researcher = researcher;
            this.score = score;
        }

        public Task getTask() { return task; }
        public Researcher getResearcher() { return researcher; }
        public double getScore() { return score; }
    }

    public double score(Researcher r, Task t) {
        double s = Math.min(r.getCapacity(), t.getDuration()) * 0.1 + r.getRating() * 0.05;
        return s;
    }

    public List<Assignment> match(List<Researcher> researchers, List<Task> tasks) {
        List<Assignment> res = new ArrayList<>();
        if (researchers == null || tasks == null) return res;
        tasks.sort(Comparator.comparing(Task::getPriority).reversed().thenComparing(Task::getDuration).reversed());
        Set<Long> used = new HashSet<>();
        for (Task t : tasks) {
            Assignment best = null;
            for (Researcher r : researchers) {
                if (used.contains(r.getId())) continue;
                if (!r.canAssign(t)) continue;
                double sc = score(r, t);
                if (best == null || sc > best.score) best = new Assignment(t, r, sc);
            }
            if (best != null) {
                best.researcher.assignTask(best.task);
                best.task.assignTo(best.researcher.getId());
                used.add(best.researcher.getId());
                res.add(best);
            }
        }
        return res;
    }
}
