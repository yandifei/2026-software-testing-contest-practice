package net.mooctest;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class ProjectReportTest {
    @Test
    void projectDefaultsAndNameBudgetUpdates() {
        Project project = new Project(null);
        assertTrue(project.getId() > 0);
        assertEquals("", project.getName());
        assertNotNull(project.getBudget());
        assertTrue(project.getTasks().isEmpty());
        assertTrue(project.getResearchers().isEmpty());
        assertTrue(project.getRisks().isEmpty());
        project.setName("study");
        assertEquals("study", project.getName());
        project.setName(null);
        assertEquals("", project.getName());
        Budget original = project.getBudget();
        project.setBudget(null);
        assertSame(original, project.getBudget());
        Budget replacement = new Budget();
        project.setBudget(replacement);
        assertSame(replacement, project.getBudget());
        assertNull(project.addTask(null));
        assertNull(project.addResearcher(null));
        project.addRisk(null);
        assertTrue(project.getRisks().isEmpty());
        assertNull(project.getTask(-1));
        assertNull(project.getResearcher(-1));
        assertTrue(project.planAssignments().isEmpty());
        assertEquals(0, project.criticalPathDuration());
        assertEquals(0, project.analyzeRisk(1).getMeanImpact());
        for (Task.Status status : Task.Status.values()) {
            assertEquals(Long.valueOf(0), project.statusCounts().get(status));
        }
    }

    @Test
    void collectionsAndStatusCountsReflectMembers() {
        Project project = new Project("study");
        Task planned = new Task("planned", 2, null);
        Task active = new Task("active", 3, null);
        active.start();
        Task done = new Task("done", 4, null);
        done.complete();
        Task cancelled = new Task("cancelled", 1, null);
        cancelled.cancel();
        for (Task task : List.of(planned, active, done, cancelled)) {
            assertSame(task, project.addTask(task));
            assertSame(task, project.getTask(task.getId()));
        }
        project.addTask(planned);
        assertEquals(List.of(planned, active, done, cancelled), List.copyOf(project.getTasks()));
        assertEquals(Map.of(Task.Status.PLANNED, 1L, Task.Status.IN_PROGRESS, 1L,
                Task.Status.DONE, 1L, Task.Status.CANCELLED, 1L, Task.Status.BLOCKED, 0L),
                project.statusCounts());
        assertEquals(4, project.criticalPathDuration());
        Researcher researcher = new Researcher("Ada", 10);
        assertSame(researcher, project.addResearcher(researcher));
        project.addResearcher(researcher);
        assertSame(researcher, project.getResearcher(researcher.getId()));
        assertEquals(List.of(researcher), List.copyOf(project.getResearchers()));
        Risk risk = new Risk("certain", "c", 1, 0.5);
        project.addRisk(risk);
        project.getRisks().clear();
        assertEquals(List.of(risk), project.getRisks());
        assertEquals(0.5, project.analyzeRisk(3).getMeanImpact());
        List<MatchingEngine.Assignment> assignments = project.planAssignments();
        assertEquals(1, assignments.size());
        assertSame(done, assignments.get(0).getTask());
        assertEquals(6, researcher.getCapacity());
    }

    @Test
    void reportContainsExactCountsCostsAndRiskStatistics() {
        ReportGenerator generator = new ReportGenerator();
        assertEquals("", generator.generate(null));
        Project project = new Project("Fusion");
        Task task = new Task("experiment", 4, Task.Priority.HIGH);
        task.start();
        project.addTask(task);
        project.getBudget().add(new Budget.Item("equipment", 12.5, 8, "LAB"));
        project.addRisk(new Risk("certain", "c", 1, 0.5));
        assertEquals("Project:Fusion\n"
                + "Status PLANNED:0\n"
                + "Status IN_PROGRESS:1\n"
                + "Status BLOCKED:0\n"
                + "Status DONE:0\n"
                + "Status CANCELLED:0\n"
                + "CriticalPath:4\n"
                + "BudgetCost:12.5\n"
                + "BudgetValue:8.0\n"
                + "RiskMean:0.5\n"
                + "RiskP90:0.5\n"
                + "RiskWorst:0.5\n", generator.generate(project));
    }
}
