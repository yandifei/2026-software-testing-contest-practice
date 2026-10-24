package net.mooctest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class TaskBehaviorTest {
    @Test
    void defaultsAndSettersPreserveValuesAndNormalizeNulls() {
        Task task = new Task(null, -1, null);
        assertTrue(task.getId() > 0);
        assertEquals("", task.getName());
        assertEquals(0, task.getDuration());
        assertEquals(Task.Priority.MEDIUM, task.getPriority());
        assertEquals(Task.Status.PLANNED, task.getStatus());
        assertEquals(0, task.getProgress());
        assertNull(task.getAssignedResearcherId());
        task.setName("study");
        task.setDuration(7);
        task.setPriority(Task.Priority.CRITICAL);
        task.assignTo(42L);
        assertEquals("study", task.getName());
        assertEquals(7, task.getDuration());
        assertEquals(Task.Priority.CRITICAL, task.getPriority());
        assertEquals(Long.valueOf(42), task.getAssignedResearcherId());
        task.setName(null);
        task.setDuration(-3);
        task.setPriority(null);
        task.assignTo(null);
        assertEquals("", task.getName());
        assertEquals(0, task.getDuration());
        assertEquals(Task.Priority.MEDIUM, task.getPriority());
        assertNull(task.getAssignedResearcherId());
        Task named = new Task("named", 4, Task.Priority.HIGH);
        assertEquals("named", named.getName());
        assertEquals(4, named.getDuration());
        assertEquals(Task.Priority.HIGH, named.getPriority());
    }

    @Test
    void requirementsClampAndKeepHighestRequestedLevel() {
        Task task = new Task("skills", 1, null);
        task.requireSkill(null, 4);
        task.requireSkill("", 4);
        assertTrue(task.getRequiredSkills().isEmpty());
        task.requireSkill("java", -1);
        assertEquals(Map.of("java", 0), task.getRequiredSkills());
        task.requireSkill("java", 5);
        task.requireSkill("java", 2);
        assertEquals(5, task.getRequiredSkills().get("java"));
        task.requireSkill("java", 11);
        task.requireSkill("python", 10);
        task.requireSkill("zero", 0);
        assertEquals(Map.of("java", 10, "python", 10, "zero", 0), task.getRequiredSkills());
        task.getRequiredSkills().clear();
        assertEquals(3, task.getRequiredSkills().size());
    }

    @Test
    void dependenciesRejectSelfNullAndDuplicatesAndReturnCopy() {
        Task task = new Task("child", 2, null);
        Task parent = new Task("parent", 3, null);
        assertFalse(task.addDependency(null));
        assertFalse(task.addDependency(task));
        assertFalse(task.dependsOn(null));
        assertFalse(task.dependsOn(parent));
        assertTrue(task.addDependency(parent));
        assertFalse(task.addDependency(parent));
        assertTrue(task.dependsOn(parent));
        assertEquals(Set.of(parent), task.getDependencies());
        task.getDependencies().clear();
        assertTrue(task.dependsOn(parent));
    }

    @Test
    void scheduleClampsBoundsAndComputesSlack() {
        Task task = new Task("timing", 1, null);
        task.setSchedule(3, 8, 6, 12);
        assertEquals(3, task.getEst());
        assertEquals(8, task.getEft());
        assertEquals(6, task.getLst());
        assertEquals(12, task.getLft());
        assertEquals(3, task.slack());
        task.setSchedule(5, 2, 1, 3);
        assertEquals(5, task.getEft());
        assertEquals(5, task.getLft());
        assertEquals(0, task.slack());
        task.setSchedule(-1, -2, -3, -4);
        assertEquals(0, task.getEst());
        assertEquals(0, task.getEft());
        assertEquals(0, task.getLst());
        assertEquals(0, task.getLft());
    }

    @Test
    void stateTransitionsOnlyStartPlannedTasks() {
        Task task = new Task("state", 1, null);
        task.start();
        assertEquals(Task.Status.IN_PROGRESS, task.getStatus());
        task.start();
        assertEquals(Task.Status.IN_PROGRESS, task.getStatus());
        task.complete();
        task.start();
        assertEquals(Task.Status.DONE, task.getStatus());
        task.cancel();
        task.start();
        assertEquals(Task.Status.CANCELLED, task.getStatus());
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "0,0", "0.4,0.4", "1,1", "2,1"})
    void progressClampsToUnitInterval(double input, double expected) {
        Task task = new Task("progress", 1, null);
        task.updateProgress(input);
        assertEquals(expected, task.getProgress());
    }
}
