package net.mooctest;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class GraphSchedulerTest {
    @Test
    void emptyAndIndependentTasks() {
        assertTrue(GraphUtils.topologicalSort(List.of()).isEmpty());
        assertFalse(GraphUtils.hasCycle(List.of()));
        assertEquals(0, GraphUtils.longestPathDuration(List.of()));
        new Scheduler().schedule(List.of());
        Task shortTask = new Task("short", 2, null);
        Task longTask = new Task("long", 7, null);
        Task zero = new Task("zero", 0, null);
        List<Task> tasks = List.of(shortTask, longTask, zero);
        assertEquals(Set.copyOf(tasks), Set.copyOf(GraphUtils.topologicalSort(tasks)));
        assertEquals(7, GraphUtils.longestPathDuration(tasks));
        new Scheduler().schedule(tasks);
        assertSchedule(shortTask, 0, 2, 5, 7);
        assertSchedule(longTask, 0, 7, 0, 7);
        assertSchedule(zero, 0, 0, 7, 7);
        assertEquals(5, shortTask.slack());
    }

    @Test
    void characterizesReversedDependencyOrderAndTruncatedCriticalPath() {
        Task first = new Task("first", 3, null);
        Task second = new Task("second", 5, null);
        Task third = new Task("third", 2, null);
        second.addDependency(first);
        third.addDependency(second);
        List<Task> tasks = List.of(first, second, third);
        assertEquals(List.of(third, second, first), GraphUtils.topologicalSort(tasks));
        assertFalse(GraphUtils.hasCycle(tasks));
        assertEquals(5, GraphUtils.longestPathDuration(tasks),
                "Known defect: dependency chain should have duration 10");
        new Scheduler().schedule(tasks);
        assertSchedule(first, 0, 3, 2, 5);
        assertSchedule(second, 0, 5, 0, 5);
        assertSchedule(third, 0, 2, 0, 2);
        assertEquals(List.of(third, second, first), GraphUtils.topologicalSort(List.of(third, second)));
    }

    @Test
    void sharedDependencyWaitsForBothIncomingEdges() {
        Task dependency = new Task("shared", 4, null);
        Task left = new Task("left", 2, null);
        Task right = new Task("right", 6, null);
        left.addDependency(dependency);
        right.addDependency(dependency);
        List<Task> sorted = GraphUtils.topologicalSort(List.of(left, right, dependency));
        assertEquals(3, sorted.size());
        assertEquals(dependency, sorted.get(2));
        assertEquals(Set.of(left, right), Set.copyOf(sorted.subList(0, 2)));
        assertEquals(6, GraphUtils.longestPathDuration(sorted));
    }

    @Test
    void cyclesPropagateAsDomainErrors() {
        Task a = new Task("a", 1, null);
        Task b = new Task("b", 1, null);
        a.addDependency(b);
        b.addDependency(a);
        List<Task> tasks = List.of(a, b, new Task("isolated", 0, null));
        assertTrue(GraphUtils.hasCycle(tasks));
        assertEquals("cycle detected", assertThrows(DomainException.class,
                () -> GraphUtils.topologicalSort(tasks)).getMessage());
        assertThrows(DomainException.class, () -> GraphUtils.longestPathDuration(tasks));
        assertThrows(DomainException.class, () -> new Scheduler().schedule(tasks));
    }

    private static void assertSchedule(Task task, int est, int eft, int lst, int lft) {
        assertEquals(est, task.getEst(), task.getName() + " EST");
        assertEquals(eft, task.getEft(), task.getName() + " EFT");
        assertEquals(lst, task.getLst(), task.getName() + " LST");
        assertEquals(lft, task.getLft(), task.getName() + " LFT");
    }
}
