package net.mooctest;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class MatchingEngineTest {
    @Test
    void nullAndEmptyInputsProduceNoAssignments() {
        MatchingEngine engine = new MatchingEngine();
        assertTrue(engine.match(null, new ArrayList<>()).isEmpty());
        assertTrue(engine.match(new ArrayList<>(), null).isEmpty());
        assertTrue(engine.match(new ArrayList<>(), new ArrayList<>()).isEmpty());
        assertTrue(engine.match(List.of(), new ArrayList<>(List.of(new Task("t", 1, null)))).isEmpty());
    }

    @Test
    void scoreCombinesMinimumCapacityAndRating() {
        Researcher researcher = new Researcher("r", 4);
        researcher.updateRating(80);
        MatchingEngine engine = new MatchingEngine();
        assertEquals(1.4, engine.score(researcher, new Task("short", 2, null)), 1e-12);
        assertEquals(1.6, engine.score(researcher, new Task("long", 10, null)), 1e-12);
        assertEquals(1.6, engine.score(researcher, new Task("equal", 4, null)), 1e-12);
        MatchingEngine.Assignment assignment = new MatchingEngine.Assignment(null, researcher, 7.25);
        assertNull(assignment.getTask());
        assertSame(researcher, assignment.getResearcher());
        assertEquals(7.25, assignment.getScore());
    }

    @Test
    void bestQualifiedCapacityWinsAndEachResearcherIsUsedOnce() {
        Researcher unavailable = new Researcher("unavailable", 1);
        Researcher first = new Researcher("first", 10);
        Researcher best = new Researcher("best", 10);
        Researcher tied = new Researcher("tied", 10);
        best.updateRating(100);
        tied.updateRating(100);
        Task task = new Task("task", 3, Task.Priority.HIGH);
        List<MatchingEngine.Assignment> results = new MatchingEngine().match(
                List.of(unavailable, first, best, tied), new ArrayList<>(List.of(task)));
        assertEquals(1, results.size());
        MatchingEngine.Assignment assignment = results.get(0);
        assertSame(task, assignment.getTask());
        assertSame(best, assignment.getResearcher());
        assertEquals(1.8, assignment.getScore(), 1e-12);
        assertEquals(best.getId(), task.getAssignedResearcherId().longValue());
        assertEquals(7, best.getCapacity());
        assertEquals(10, first.getCapacity());
        assertEquals(10, tied.getCapacity());

        Researcher only = new Researcher("only", 20);
        Task a = new Task("a", 3, null);
        Task b = new Task("b", 2, null);
        results = new MatchingEngine().match(List.of(only), new ArrayList<>(List.of(b, a)));
        assertEquals(1, results.size());
        assertSame(a, results.get(0).getTask());
        assertEquals(17, only.getCapacity());
        assertNull(b.getAssignedResearcherId());
    }

    @Test
    void characterizesAscendingPriorityAndDescendingDuration() {
        Task urgent = new Task("urgent", 3, Task.Priority.CRITICAL);
        Task lowShort = new Task("short", 1, Task.Priority.LOW);
        Task lowLong = new Task("long", 2, Task.Priority.LOW);
        ArrayList<Task> tasks = new ArrayList<>(List.of(urgent, lowShort, lowLong));
        Researcher researcher = new Researcher("r", 10);
        List<MatchingEngine.Assignment> result = new MatchingEngine().match(List.of(researcher), tasks);
        assertEquals(List.of(lowLong, lowShort, urgent), tasks,
                "Known defect: critical tasks are sorted after low priority tasks");
        assertEquals(1, result.size());
        assertSame(lowLong, result.get(0).getTask());
        assertEquals(8, researcher.getCapacity());
        assertNull(urgent.getAssignedResearcherId());
    }

    @Test
    void insufficientCapacityLeavesTaskUnassigned() {
        Researcher researcher = new Researcher("r", 1);
        Task task = new Task("large", 2, null);
        assertTrue(new MatchingEngine().match(List.of(researcher),
                new ArrayList<>(List.of(task))).isEmpty());
        assertNull(task.getAssignedResearcherId());
        assertEquals(1, researcher.getCapacity());
    }
}
