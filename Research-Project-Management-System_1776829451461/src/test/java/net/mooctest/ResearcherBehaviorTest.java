package net.mooctest;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class ResearcherBehaviorTest {
    @Test
    void namesSkillsAndCopies() {
        Researcher researcher = new Researcher(null, -3);
        assertTrue(researcher.getId() > 0);
        assertEquals("", researcher.getName());
        assertEquals(0, researcher.getCapacity());
        assertEquals(0, researcher.getRating());
        researcher.setName("Ada");
        assertEquals("Ada", researcher.getName());
        researcher.setName(null);
        assertEquals("", researcher.getName());
        researcher.addSkill(null, 3);
        researcher.addSkill("", 3);
        assertTrue(researcher.getSkills().isEmpty());
        assertEquals(0, researcher.getSkillLevel("missing"));
        assertTrue(researcher.hasSkill("missing", -1));
        assertFalse(researcher.hasSkill("missing", 1));
        researcher.addSkill("java", -1);
        assertEquals(0, researcher.getSkillLevel("java"));
        researcher.addSkill("java", 11);
        assertEquals(10, researcher.getSkillLevel("java"));
        researcher.addSkill("java", 5);
        assertTrue(researcher.hasSkill("java", 5));
        assertFalse(researcher.hasSkill("java", 6));
        assertEquals(Set.of("java"), researcher.getSkills());
        researcher.getSkills().clear();
        assertEquals(5, researcher.getSkillLevel("java"));
    }

    @Test
    void allocationUsesExactCapacityAndReleaseCapsAtForty() {
        Researcher researcher = new Researcher("Ada", 10);
        assertEquals("Ada", researcher.getName());
        assertFalse(researcher.allocateHours(-1));
        assertFalse(researcher.allocateHours(0));
        assertFalse(researcher.allocateHours(11));
        assertEquals(10, researcher.getCapacity());
        assertTrue(researcher.allocateHours(4));
        assertEquals(6, researcher.getCapacity());
        assertTrue(researcher.allocateHours(6));
        assertEquals(0, researcher.getCapacity());
        researcher.releaseHours(-1);
        researcher.releaseHours(0);
        assertEquals(0, researcher.getCapacity());
        researcher.releaseHours(3);
        assertEquals(3, researcher.getCapacity());
        researcher.releaseHours(37);
        assertEquals(40, researcher.getCapacity());
        researcher.releaseHours(1);
        assertEquals(40, researcher.getCapacity());
    }

    @Test
    void ratingIsClampedExponentialAverage() {
        Researcher researcher = new Researcher("r", 10);
        researcher.updateRating(100);
        assertEquals(30, researcher.getRating(), 1e-12);
        researcher.updateRating(50);
        assertEquals(36, researcher.getRating(), 1e-12);
        researcher.updateRating(-1);
        assertEquals(25.2, researcher.getRating(), 1e-12);
        researcher.updateRating(101);
        assertEquals(47.64, researcher.getRating(), 1e-12);
        researcher.updateRating(0);
        assertEquals(33.348, researcher.getRating(), 1e-12);
    }

    @Test
    void assignmentAndCompletionUpdateCapacityAndRating() {
        Researcher researcher = new Researcher("r", 5);
        Task task = new Task("t", 5, null);
        assertFalse(researcher.canAssign(null));
        assertFalse(researcher.assignTask(null));
        assertFalse(researcher.completeTask(null, 100));
        assertFalse(researcher.assignTask(new Task("big", 6, null)));
        assertTrue(researcher.canAssign(task));
        assertTrue(researcher.assignTask(task));
        assertEquals(0, researcher.getCapacity());
        assertFalse(researcher.canAssign(task));
        assertTrue(researcher.completeTask(task, 80));
        assertEquals(5, researcher.getCapacity());
        assertEquals(24, researcher.getRating());
    }

    @Test
    void characterizesMissingSkillValidationAndZeroDurationAssignment() {
        Researcher researcher = new Researcher("unskilled", 5);
        Task task = new Task("requires skill", 1, null);
        task.requireSkill("physics", 10);
        assertTrue(researcher.canAssign(task), "Known defect: skills are not checked");
        Task zero = new Task("zero", 0, null);
        assertTrue(researcher.canAssign(zero));
        assertFalse(researcher.assignTask(zero));
        assertEquals(5, researcher.getCapacity());
    }
}
