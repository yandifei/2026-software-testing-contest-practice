package net.mooctest;

import java.util.*;

public class Scheduler {
    public void schedule(Collection<Task> tasks) {
        List<Task> order = GraphUtils.topologicalSort(tasks);
        Map<Task, Integer> finish = new HashMap<>();
        for (Task t : order) {
            int est = 0;
            for (Task d : t.getDependencies()) est = Math.max(est, finish.getOrDefault(d, 0));
            int eft = est + t.getDuration();
            t.setSchedule(est, eft, est, eft);
            finish.put(t, eft);
        }
        int projectFinish = 0;
        for (int v : finish.values()) projectFinish = Math.max(projectFinish, v);
        ListIterator<Task> it = order.listIterator(order.size());
        Map<Task, Integer> startLatest = new HashMap<>();
        while (it.hasPrevious()) {
            Task t = it.previous();
            int lft = projectFinish;
            for (Task d : t.getDependencies()) lft = Math.min(lft, startLatest.getOrDefault(d, projectFinish));
            int lst = Math.max(0, lft - t.getDuration());
            t.setSchedule(t.getEst(), t.getEft(), lst, lft);
            startLatest.put(t, lst);
        }
    }
}
