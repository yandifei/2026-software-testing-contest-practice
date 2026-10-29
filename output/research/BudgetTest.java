package net.mooctest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Single-file regression suite for the Research Project Management System.
 * Normal, boundary and error paths are grouped by domain below.
 * Tests named characterizes document current behavior and known business risks.
 * Tests use public behavior; private fields and constructors are not accessed reflectively.
 */
public class BudgetTest {
    // BudgetBoundary scenarios
    @Test
    void budgetBoundary_normalizesItemsAndProtectsBudgetCollection() {
        Budget.Item item = new Budget.Item(null, -1, -2, null);
        assertEquals("", item.getName());
        assertEquals("GENERAL", item.getCategory());
        assertEquals(0, item.getCost());
        assertEquals(0, item.getValue());
        Budget budget = new Budget();
        budget.add(null);
        assertTrue(budget.getItems().isEmpty());
        assertEquals(0, budget.totalCost());
        assertEquals(0, budget.totalValue());
        assertEquals(1000, budget.requiredReserve());
        Budget.Item first = new Budget.Item("A", 12000, 7, "EQUIPMENT");
        Budget.Item second = new Budget.Item("B", 8000, 11, "LABOR");
        budget.add(first);
        budget.add(second);
        assertEquals(List.of(first, second), budget.getItems());
        budget.getItems().clear();
        assertEquals(20000, budget.totalCost());
        assertEquals(18, budget.totalValue());
        assertEquals(2000, budget.requiredReserve());
    }

    @ParameterizedTest
    @CsvSource({"-2, 100", "-0.5, 100", "0, 200", "0.25, 250", "1, 400", "2, 400"})
    void budgetBoundary_clampsInflation(double inflation, double expected) {
        Budget budget = new Budget();
        budget.add(new Budget.Item("item", 200, 1, "x"));
        assertEquals(expected, budget.forecastCost(inflation), 1e-9);
    }

    @ParameterizedTest
    @CsvSource({"-1, 1000", "0, 1000", "0.05, 1000", "0.2, 4000", "0.5, 10000", "1, 10000"})
    void budgetBoundary_clampsReserve(double ratio, double expected) {
        Budget budget = new Budget();
        budget.add(new Budget.Item("item", 20000, 1, "x"));
        budget.setReserveRatio(ratio);
        assertEquals(expected, budget.requiredReserve());
    }

    @Test
    void budgetBoundary_optimizerHandlesNullEmptyZeroAndTies() {
        BudgetOptimizer optimizer = new BudgetOptimizer();
        assertEquals("budget null", assertThrows(DomainException.class,
                () -> optimizer.optimize(null, 1)).getMessage());
        Budget budget = new Budget();
        BudgetOptimizer.Selection empty = optimizer.optimize(budget, -1);
        assertTrue(empty.getItems().isEmpty());
        assertEquals(0, empty.getTotalCost());
        assertEquals(0, empty.getTotalValue());
        Budget.Item free = new Budget.Item("free", 0, 3, null);
        Budget.Item first = new Budget.Item("first", 2, 5, null);
        budget.add(free);
        budget.add(first);
        budget.add(new Budget.Item("tie", 2, 5, null));
        budget.add(new Budget.Item("worthless", 0, 0, null));
        assertEquals(List.of(free), optimizer.optimize(budget, -2).getItems());
        BudgetOptimizer.Selection chosen = optimizer.optimize(budget, 2);
        assertEquals(List.of(free, first), chosen.getItems());
        assertEquals(2, chosen.getTotalCost());
        assertEquals(8, chosen.getTotalValue());
    }

    @Test
    void budgetBoundary_optimizerMatchesAllSubsetsForEverySmallCapacity() {
        Budget budget = new Budget();
        int[] costs = {0, 1, 2, 3, 5, 7};
        int[] values = {2, 2, 6, 7, 11, 13};
        for (int i = 0; i < costs.length; i++) {
            budget.add(new Budget.Item("item" + i, costs[i], values[i], "test"));
        }
        for (int limit = 0; limit <= 20; limit++) {
            int optimum = 0;
            for (int mask = 0; mask < (1 << costs.length); mask++) {
                int cost = 0;
                int value = 0;
                for (int bit = 0; bit < costs.length; bit++) {
                    if ((mask & (1 << bit)) != 0) {
                        cost += costs[bit];
                        value += values[bit];
                    }
                }
                if (cost <= limit) optimum = Math.max(optimum, value);
            }
            BudgetOptimizer.Selection result = new BudgetOptimizer().optimize(budget, limit);
            assertEquals(optimum, result.getTotalValue(), "capacity=" + limit);
            assertTrue(result.getTotalCost() <= limit);
            assertEquals(result.getItems().stream().mapToDouble(Budget.Item::getCost).sum(),
                    result.getTotalCost());
            assertEquals(result.getItems().stream().mapToDouble(Budget.Item::getValue).sum(),
                    result.getTotalValue());
            assertEquals(result.getItems().size(), result.getItems().stream().distinct().count());
        }
    }

    @Test
    void budgetBoundary_characterizesRoundingThatCanExceedActualLimit() {
        Budget budget = new Budget();
        Budget.Item item = new Budget.Item("fractional", 1.49, 5, null);
        budget.add(item);
        BudgetOptimizer.Selection result = new BudgetOptimizer().optimize(budget, 1.0);
        assertEquals(List.of(item), result.getItems());
        assertEquals(1.49, result.getTotalCost());
        assertTrue(result.getTotalCost() > 1.0, "Known defect: rounded cost exceeds actual limit");
        assertEquals(List.of(item), new BudgetOptimizer.Selection(List.of(item), 2, 3).getItems());
    }

    @Test
    void budgetBoundary_halfUnitsRoundUpForBothCapacityAndItemWeight() {
        Budget budget = new Budget();
        Budget.Item item = new Budget.Item("half", 2.5, 7.5, "c");
        budget.add(item);
        BudgetOptimizer optimizer = new BudgetOptimizer();
        assertTrue(optimizer.optimize(budget, 2.49).getItems().isEmpty());
        BudgetOptimizer.Selection result = optimizer.optimize(budget, 2.5);
        assertEquals(List.of(item), result.getItems());
        assertEquals(2.5, result.getTotalCost());
        assertEquals(7.5, result.getTotalValue());
    }

    // Budget scenarios
    @Test
	public void testItemConstructorWithValidParameters() {
		Budget.Item item = new Budget.Item("Test", 100.0, 0.5, "CATEGORY");
		assertEquals("Test", item.getName());
		assertEquals(100.0, item.getCost(), 0.001);
		assertEquals(0.5, item.getValue(), 0.001);
		assertEquals("CATEGORY", item.getCategory());
	}
	
	@Test
    public void budget_testItemConstructorWithNullName() {
        Budget.Item item = new Budget.Item(null, 100.0, 0.5, "CATEGORY");
        assertEquals("", item.getName());
    }

    // GraphScheduler scenarios
    @Test
    void graphScheduler_emptyAndIndependentTasks() {
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
        graphScheduler_assertSchedule(shortTask, 0, 2, 5, 7);
        graphScheduler_assertSchedule(longTask, 0, 7, 0, 7);
        graphScheduler_assertSchedule(zero, 0, 0, 7, 7);
        assertEquals(5, shortTask.slack());
    }

    @Test
    void graphScheduler_characterizesReversedDependencyOrderAndTruncatedCriticalPath() {
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
        graphScheduler_assertSchedule(first, 0, 3, 2, 5);
        graphScheduler_assertSchedule(second, 0, 5, 0, 5);
        graphScheduler_assertSchedule(third, 0, 2, 0, 2);
        assertEquals(List.of(third, second, first), GraphUtils.topologicalSort(List.of(third, second)));
    }

    @Test
    void graphScheduler_sharedDependencyWaitsForBothIncomingEdges() {
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
    void graphScheduler_cyclesPropagateAsDomainErrors() {
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

    private static void graphScheduler_assertSchedule(Task task, int est, int eft, int lst, int lft) {
        assertEquals(est, task.getEst(), task.getName() + " EST");
        assertEquals(eft, task.getEft(), task.getName() + " EFT");
        assertEquals(lst, task.getLst(), task.getName() + " LST");
        assertEquals(lft, task.getLft(), task.getName() + " LFT");
    }

    // IdGenerator scenarios
    @ParameterizedTest
    @NullAndEmptySource
    void idGenerator_rejectsMissingIds(String value) {
        DomainException exception = assertThrows(DomainException.class, () -> IdGenerator.fromString(value));
        assertEquals("id string invalid", exception.getMessage());
        assertNull(exception.getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "-1", " 1", "1 ", "18446744073709551616", "1.0"})
    void idGenerator_wrapsMalformedOrOverflowingIds(String value) {
        DomainException exception = assertThrows(DomainException.class, () -> IdGenerator.fromString(value));
        assertEquals("id parse failed", exception.getMessage());
        assertInstanceOf(NumberFormatException.class, exception.getCause());
    }

    @Test
    void idGenerator_parsesUnsignedRangeAndRejectsZero() {
        assertEquals(1, IdGenerator.fromString("1"));
        assertEquals(12345, IdGenerator.fromString("12345"));
        assertEquals(Long.MAX_VALUE, IdGenerator.fromString("9223372036854775807"));
        assertEquals(Long.MIN_VALUE, IdGenerator.fromString("9223372036854775808"));
        assertEquals(-1, IdGenerator.fromString("18446744073709551615"));
        DomainException exception = assertThrows(DomainException.class, () -> IdGenerator.fromString("0"));
        assertEquals("id zero invalid", exception.getMessage());
        assertNull(exception.getCause());
        String generated = IdGenerator.nextIdStr();
        assertTrue(IdGenerator.fromString(generated) > 0);
        assertEquals(generated, Long.toUnsignedString(IdGenerator.fromString(generated)));
    }



    @Test
    void idGenerator_domainExceptionPreservesMessageAndCause() {
        Throwable cause = new IllegalArgumentException("root");
        DomainException exception = new DomainException("context", cause);
        assertEquals("context", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals("plain", new DomainException("plain").getMessage());
    }

    // MatchingEngine scenarios
    @Test
    void matchingEngine_nullAndEmptyInputsProduceNoAssignments() {
        MatchingEngine engine = new MatchingEngine();
        assertTrue(engine.match(null, new ArrayList<>()).isEmpty());
        assertTrue(engine.match(new ArrayList<>(), null).isEmpty());
        assertTrue(engine.match(new ArrayList<>(), new ArrayList<>()).isEmpty());
        assertTrue(engine.match(List.of(), new ArrayList<>(List.of(new Task("t", 1, null)))).isEmpty());
    }

    @Test
    void matchingEngine_scoreCombinesMinimumCapacityAndRating() {
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
    void matchingEngine_bestQualifiedCapacityWinsAndEachResearcherIsUsedOnce() {
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
    void matchingEngine_characterizesAscendingPriorityAndDescendingDuration() {
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
    void matchingEngine_insufficientCapacityLeavesTaskUnassigned() {
        Researcher researcher = new Researcher("r", 1);
        Task task = new Task("large", 2, null);
        assertTrue(new MatchingEngine().match(List.of(researcher),
                new ArrayList<>(List.of(task))).isEmpty());
        assertNull(task.getAssignedResearcherId());
        assertEquals(1, researcher.getCapacity());
    }

    // MutationBoundary scenarios
    @Test
    void mutationBoundary_percentileExcludesAnOutlierInTwentyScenarios() {
        RiskAnalyzer analyzer = new RiskAnalyzer() {
            private int drawCount;

            @Override
            public double rnd() {
                return drawCount++ == 0 ? 0.0 : 0.75;
            }
        };
        RiskAnalyzer.SimulationResult result = analyzer.simulate(
                List.of(new Risk("rare", "impact", 0.5, 1.0)), 20);
        assertEquals(0.05, result.getMeanImpact(), 1e-12);
        assertEquals(0.0, result.getP90Impact());
        assertEquals(1.0, result.getWorstCaseImpact());
    }

    @Test
    void mutationBoundary_releasingNoHoursDoesNotNormalizeAnExistingExcessCapacity() {
        Researcher researcher = new Researcher("large initial capacity", 41);
        researcher.releaseHours(0);
        assertEquals(41, researcher.getCapacity());
        researcher.releaseHours(-1);
        assertEquals(41, researcher.getCapacity());
        researcher.releaseHours(1);
        assertEquals(40, researcher.getCapacity());
    }



    @Test
    void mutationBoundary_emptyMatchResultsRemainIndependentMutableLists() {
        MatchingEngine engine = new MatchingEngine();
        List<MatchingEngine.Assignment> missingResearchers = engine.match(null, new ArrayList<>());
        List<MatchingEngine.Assignment> missingTasks = engine.match(new ArrayList<>(), null);
        MatchingEngine.Assignment assignment = new MatchingEngine.Assignment(
                new Task("t", 1, null), new Researcher("r", 1), 0.1);
        missingResearchers.add(assignment);
        assertEquals(List.of(assignment), missingResearchers);
        assertTrue(missingTasks.isEmpty());
        missingTasks.add(assignment);
        assertEquals(List.of(assignment), missingTasks);
        assertTrue(engine.match(null, null).isEmpty());
    }

    @Test
    void mutationBoundary_itemRetainsSignedZeroWhileClampingStrictlyNegativeValues() {
        Budget.Item item = new Budget.Item("signed zero", -0.0, -0.0, null);
        mutationBoundary_assertNegativeZero(item.getCost());
        mutationBoundary_assertNegativeZero(item.getValue());
        Budget.Item negative = new Budget.Item("negative", -Double.MIN_VALUE, -Double.MIN_VALUE, null);
        assertEquals(0L, Double.doubleToRawLongBits(negative.getCost()));
        assertEquals(0L, Double.doubleToRawLongBits(negative.getValue()));
    }

    @Test
    void mutationBoundary_riskClampPreservesSignedZero() {
        Risk risk = new Risk("zero", "c", -0.0, -0.0);
        mutationBoundary_assertNegativeZero(risk.getProbability());
        mutationBoundary_assertNegativeZero(risk.getImpact());
        mutationBoundary_assertNegativeZero(risk.clamp(-0.0));
        assertEquals(0L, Double.doubleToRawLongBits(risk.clamp(-Double.MIN_VALUE)));
    }

    @Test
    void mutationBoundary_progressPreservesSignedZero() {
        Task task = new Task("zero", 0, null);
        task.updateProgress(-0.0);
        mutationBoundary_assertNegativeZero(task.getProgress());
        task.updateProgress(-Double.MIN_VALUE);
        assertEquals(0L, Double.doubleToRawLongBits(task.getProgress()));
    }



    private static void mutationBoundary_assertNegativeZero(double value) {
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(value),
                "Strictly negative checks must preserve IEEE-754 negative zero");
    }

    // ProjectReport scenarios
    @Test
    void projectReport_projectDefaultsAndNameBudgetUpdates() {
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
    void projectReport_collectionsAndStatusCountsReflectMembers() {
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
    void projectReport_reportContainsExactCountsCostsAndRiskStatistics() {
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

    // ResearcherBehavior scenarios
    @Test
    void researcherBehavior_namesSkillsAndCopies() {
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
    void researcherBehavior_allocationUsesExactCapacityAndReleaseCapsAtForty() {
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
    void researcherBehavior_ratingIsClampedExponentialAverage() {
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
    void researcherBehavior_assignmentAndCompletionUpdateCapacityAndRating() {
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
    void researcherBehavior_characterizesMissingSkillValidationAndZeroDurationAssignment() {
        Researcher researcher = new Researcher("unskilled", 5);
        Task task = new Task("requires skill", 1, null);
        task.requireSkill("physics", 10);
        assertTrue(researcher.canAssign(task), "Known defect: skills are not checked");
        Task zero = new Task("zero", 0, null);
        assertTrue(researcher.canAssign(zero));
        assertFalse(researcher.assignTask(zero));
        assertEquals(5, researcher.getCapacity());
    }

    // ResourceBehavior scenarios
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 10, 0);

    @Test
    void resourceBehavior_metadataDefaultsAndSetters() {
        Resource resource = new Resource(null, null);
        assertTrue(resource.getId() > 0);
        assertEquals("", resource.getName());
        assertEquals("GENERIC", resource.getType());
        resource.setName("Microscope");
        resource.setType("LAB");
        assertEquals("Microscope", resource.getName());
        assertEquals("LAB", resource.getType());
        resource.setName(null);
        resource.setType(null);
        assertEquals("", resource.getName());
        assertEquals("GENERIC", resource.getType());
        Resource named = new Resource("Room", "SPACE");
        assertEquals("Room", named.getName());
        assertEquals("SPACE", named.getType());
    }

    @Test
    void resourceBehavior_invalidIntervalsDoNotCreateBookings() {
        Resource resource = new Resource("r", "t");
        assertFalse(resource.isAvailable(null, BASE));
        assertFalse(resource.isAvailable(BASE, null));
        assertFalse(resource.isAvailable(BASE, BASE));
        assertFalse(resource.isAvailable(BASE, BASE.minusMinutes(1)));
        assertFalse(resource.book(BASE, BASE));
        assertTrue(resource.listBookings().isEmpty());
        resource.cancel(null);
        resource.cancel(BASE);
        assertTrue(resource.listBookings().isEmpty());
    }

    @Test
    void resourceBehavior_bookingsCheckPrecedingEndAndCanBeCancelled() {
        Resource resource = new Resource("r", "t");
        assertTrue(resource.isAvailable(BASE, BASE.plusHours(1)));
        assertTrue(resource.book(BASE, BASE.plusHours(1)));
        assertFalse(resource.book(BASE, BASE.plusHours(2)));
        assertFalse(resource.isAvailable(BASE.plusMinutes(30), BASE.plusHours(2)));
        assertFalse(resource.isAvailable(BASE.plusHours(1), BASE.plusHours(2)));
        assertTrue(resource.isAvailable(BASE.plusHours(1).plusSeconds(1), BASE.plusHours(2)));
        assertTrue(resource.book(BASE.plusHours(2), BASE.plusHours(3)));
        assertEquals(2, resource.listBookings().size());
        assertEquals(BASE, resource.listBookings().get(0).getKey());
        assertEquals(BASE.plusHours(1), resource.listBookings().get(0).getValue());
        resource.listBookings().clear();
        assertEquals(2, resource.listBookings().size());
        resource.cancel(BASE);
        assertEquals(1, resource.listBookings().size());
        assertTrue(resource.isAvailable(BASE, BASE.plusHours(1)));
    }

    @Test
    void resourceBehavior_characterizesIgnoredFutureOverlapAndConflictStub() {
        Resource resource = new Resource("r", "t");
        assertTrue(resource.book(BASE.plusHours(1), BASE.plusHours(2)));
        assertTrue(resource.isAvailable(BASE, BASE.plusHours(3)),
                "Known defect: later bookings are ignored");
        assertFalse(resource.conflicts(BASE.plusHours(1), BASE.plusHours(2)),
                "Known defect: conflicts always returns false");
        assertFalse(resource.conflicts(null, null));
    }

    // RiskBehavior scenarios
    @Test
    void riskBehavior_defaultsClampingAndValueAccessors() {
        Risk risk = new Risk(null, null, -1, 2);
        assertEquals("", risk.getName());
        assertEquals("GENERAL", risk.getCategory());
        assertEquals(0, risk.getProbability());
        assertEquals(1, risk.getImpact());
        Risk named = new Risk("delay", "TIME", 0.5, 0.75);
        assertEquals("delay", named.getName());
        assertEquals("TIME", named.getCategory());
        assertEquals(0.5, named.getProbability());
        assertEquals(0.75, named.getImpact());
        assertEquals(0.375, named.score());
        assertEquals(0, named.clamp(0));
        assertEquals(1, named.clamp(1));
    }

    @ParameterizedTest
    @CsvSource({"0,0", "0.001,1", "0.249,1", "0.25,2", "0.499,2", "0.5,3", "1,3"})
    void riskBehavior_priorityIncludesExactThresholds(double score, int expected) {
        assertEquals(expected, new Risk("r", "c", 1, score).priority());
    }

    @Test
    void riskBehavior_comparesPriorityThenImpactThenNameInBothDirections() {
        Risk high = new Risk("high", "c", 1, 0.5);
        Risk low = new Risk("low", "c", 0.1, 0.9);
        assertTrue(high.compareTo(low) < 0);
        assertTrue(low.compareTo(high) > 0);
        Risk larger = new Risk("z", "c", 0.2, 0.8);
        Risk smaller = new Risk("a", "c", 0.2, 0.7);
        assertTrue(larger.compareTo(smaller) < 0);
        assertTrue(smaller.compareTo(larger) > 0);
        Risk a = new Risk("a", "c", 0.2, 0.8);
        assertTrue(a.compareTo(larger) < 0);
        assertTrue(larger.compareTo(a) > 0);
        assertEquals(0, a.compareTo(new Risk("a", "different", 0.2, 0.8)));
    }

    @Test
    void riskBehavior_emptyAndNonPositiveSimulationInputs() {
        RiskAnalyzer analyzer = new RiskAnalyzer();
        riskBehavior_assertResult(analyzer.simulate(null, 10), 0, 0, 0);
        riskBehavior_assertResult(analyzer.simulate(List.of(), 10), 0, 0, 0);
        List<Risk> risks = List.of(new Risk("r", "c", 1, 1));
        riskBehavior_assertResult(analyzer.simulate(risks, 0), 0, 0, 0);
        riskBehavior_assertResult(analyzer.simulate(risks, -1), 0, 0, 0);
        riskBehavior_assertResult(new RiskAnalyzer.SimulationResult(2, 3, 4), 2, 3, 4);
    }

    @Test
    void riskBehavior_certainAndImpossibleEventsHaveExactStatistics() {
        List<Risk> risks = List.of(new Risk("certain", "c", 1, 0.25),
                new Risk("certain too", "c", 1, 0.5), new Risk("never", "c", 0, 1));
        riskBehavior_assertResult(new RiskAnalyzer().simulate(risks, 10), 0.75, 0.75, 0.75);
        riskBehavior_assertResult(new RiskAnalyzer().simulate(risks, 1), 0.75, 0.75, 0.75);
    }

    @Test
    void riskBehavior_deterministicDrawsExerciseStrictProbabilityAndPercentileBoundaries() {
        // Ten fixed draws: equality with probability 0.5 must NOT trigger the event.
        double[] draws = {0.5, 0.8, 0.1, 0.9, 0.3, 0.7, 0.2, 0.6, 0.4, 0.99};
        RiskAnalyzer analyzer = new RiskAnalyzer() {
            private int index;
            @Override public double rnd() { return draws[index++]; }
        };
        riskBehavior_assertResult(analyzer.simulate(List.of(new Risk("r", "c", 0.5, 0.5)), 10),
                0.2, 0.5, 0.5);
        // Nine zero scenarios followed by one event: floor(10 * .9) selects index 9.
        RiskAnalyzer percentile = new RiskAnalyzer() {
            private int index;
            @Override public double rnd() { return index++ == 9 ? 0.0 : 0.9; }
        };
        riskBehavior_assertResult(percentile.simulate(List.of(new Risk("r", "c", 0.5, 1)), 10),
                0.1, 1, 1);
    }



    @Test
    void riskBehavior_seededGeneratorIsReproducibleAndAdvances() {
        RiskAnalyzer a = new RiskAnalyzer();
        RiskAnalyzer b = new RiskAnalyzer();
        double previous = -1;
        for (int i = 0; i < 32; i++) {
            double draw = a.rnd();
            assertEquals(draw, b.rnd());
            assertTrue(draw >= 0 && draw < 1);
            assertNotEquals(previous, draw);
            previous = draw;
        }
    }

    @Test
    void riskBehavior_defaultSeedMatchesIndependentGoldenSequence() {
        // Numerators computed with independent 64-bit integer arithmetic.
        long[] numerators = {8442009034574139L, 4766085112791913L, 2609970339759407L,
                115724349775205L, 6582913784013087L};
        RiskAnalyzer analyzer = new RiskAnalyzer();
        for (long numerator : numerators) {
            assertEquals(numerator / 9007199254740992.0, analyzer.rnd());
        }
    }

    private static void riskBehavior_assertResult(RiskAnalyzer.SimulationResult result,
                                     double mean, double p90, double worst) {
        assertEquals(mean, result.getMeanImpact(), 1e-12);
        assertEquals(p90, result.getP90Impact(), 1e-12);
        assertEquals(worst, result.getWorstCaseImpact(), 1e-12);
    }

    // TaskBehavior scenarios
    @Test
    void taskBehavior_defaultsAndSettersPreserveValuesAndNormalizeNulls() {
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
    void taskBehavior_requirementsClampAndKeepHighestRequestedLevel() {
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
    void taskBehavior_dependenciesRejectSelfNullAndDuplicatesAndReturnCopy() {
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
    void taskBehavior_scheduleClampsBoundsAndComputesSlack() {
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
    void taskBehavior_stateTransitionsOnlyStartPlannedTasks() {
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
    void taskBehavior_progressClampsToUnitInterval(double input, double expected) {
        Task task = new Task("progress", 1, null);
        task.updateProgress(input);
        assertEquals(expected, task.getProgress());
    }

    // UtilityConstruction scenarios




    @Test
    void idGenerator_publicIdsArePositiveAndUniqueAcrossRepeatedCalls() {
        Set<Long> ids = new java.util.HashSet<>();
        for (int i = 0; i < 512; i++) {
            long id = IdGenerator.nextId();
            assertTrue(id > 0);
            assertTrue(ids.add(id), "each generated ID must be unique");
            assertEquals(id, IdGenerator.fromString(Long.toUnsignedString(id)));
        }
    }

    @Test
    void budgetReserve_publicResultExposesClampingAndReset() {
        Budget budget = new Budget();
        budget.add(new Budget.Item("equipment", 100000, 1, "lab"));
        budget.setReserveRatio(0.25);
        assertEquals(25000, budget.requiredReserve());
        budget.setReserveRatio(-0.25);
        assertEquals(1000, budget.requiredReserve());
        budget.setReserveRatio(0.75);
        assertEquals(50000, budget.requiredReserve());
        budget.setReserveRatio(0.1);
        assertEquals(10000, budget.requiredReserve());
    }

    @Test
    void riskBehavior_zeroDrawStillDoesNotTriggerZeroProbability() {
        RiskAnalyzer analyzer = new RiskAnalyzer() {
            @Override
            public double rnd() { return 0.0; }
        };
        riskBehavior_assertResult(analyzer.simulate(
                List.of(new Risk("never", "c", 0, 1)), 1), 0, 0, 0);
    }
}
