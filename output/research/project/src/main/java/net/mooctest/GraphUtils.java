package net.mooctest;

import java.util.*;

public final class GraphUtils {
    private GraphUtils() {}

    public static List<Task> topologicalSort(Collection<Task> tasks) {
        Map<Task, Integer> indeg = new HashMap<>();
        for (Task t : tasks) indeg.put(t, 0);
        for (Task t : tasks) for (Task d : t.getDependencies()) indeg.put(d, indeg.getOrDefault(d, 0) + 1);
        Deque<Task> q = new ArrayDeque<>();
        for (Map.Entry<Task, Integer> e : indeg.entrySet()) if (e.getValue() == 0) q.add(e.getKey());
        List<Task> res = new ArrayList<>();
        while (!q.isEmpty()) {
            Task u = q.removeFirst();
            res.add(u);
            for (Task d : u.getDependencies()) {
                int v = indeg.getOrDefault(d, 0) - 1;
                indeg.put(d, v);
                if (v == 0) q.add(d);
            }
        }
        if (res.size() != indeg.size()) throw new DomainException("cycle detected");
        return res;
    }

    public static boolean hasCycle(Collection<Task> tasks) {
        try {
            topologicalSort(tasks);
            return false;
        } catch (DomainException e) {
            return true;
        }
    }

    public static int longestPathDuration(Collection<Task> tasks) {
        List<Task> order = topologicalSort(tasks);
        Map<Task, Integer> dp = new HashMap<>();
        int best = 0;
        for (Task t : order) {
            int base = 0;
            for (Task d : t.getDependencies()) base = Math.max(base, dp.getOrDefault(d, 0));
            int v = base + t.getDuration();
            dp.put(t, v);
            if (v > best) best = v;
        }
        return best;
    }
}
